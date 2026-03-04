# `runV3ShutdownCacheDuringScan` — Race Condition Analysis

## Overview

`runV3ShutdownCacheDuringScan` (Scenario 3 in `TestRocksDBIteratorCrashRepro`) reproduces a
use-after-free race condition specific to Schema V3 datanodes: the shared per-volume RocksDB
instance is closed during datanode shutdown **before** all scanner threads that hold live
`rocksdb::Iterator*` objects have finished their JNI calls. The result is a JVM-level SIGSEGV
crash inside `RocksIterator.next0()`.

---

## Background: Schema V3 RocksDB Architecture

Under Schema V3, all containers on a single HDDS volume share **one** RocksDB instance. This
instance is managed by `DatanodeStoreCache`, a JVM-global singleton that maps disk paths to
`RawDB` wrappers.

```
HddsVolume (e.g. /data/disk1)
  └── DatanodeStoreCache
        └── RawDB (DatanodeStore backed by native rocksdb::DB*)
              ├── Container 1 (column family)
              ├── Container 2 (column family)
              └── Container N (column family)
```

**There is no reference counting on `RawDB`.**  Any call to `DatanodeStoreCache.removeDB()` or
`DatanodeStoreCache.shutdownCache()` calls `store.stop()` immediately — it does not check whether
any thread has an open `rocksdb::Iterator*` against that DB.

```java
// DatanodeStoreCache.java
public void removeDB(String containerDBPath) {
    RawDB db = datanodeStoreMap.remove(containerDBPath);   // (1) removed from map
    if (db == null) { return; }
    db.getStore().stop();   // (2) native rocksdb::DB* freed — no iterator check
}

public void shutdownCache() {
    if (miniClusterMode) { return; }          // skipped in unit-test clusters
    for (RawDB db : datanodeStoreMap.values()) {
        db.getStore().stop();                 // same unconditional close
    }
    datanodeStoreMap.clear();
}
```

By contrast, Schema V2 uses `ReferenceCountedDB`: the refcount is incremented inside the scanner's
try-with-resources block, and `ContainerCache.removeDB()` refuses to close the DB when the
refcount is `> 0`. That is why Schema V2 paths produce an `IllegalArgumentException` rather than
a SIGSEGV.

---

## The Race Condition — Step by Step

### Actors

| Thread              | Role                                                                                                                                                                              |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Scanner thread**  | Background or on-demand container scanner; calls `container.scanData()` → `KeyValueContainerCheck.scanData()` → `KeyValueBlockIterator.hasNext()` → `RocksIterator.next0()` (JNI) |
| **Shutdown thread** | `OzoneContainer.stop()` on the main service-shutdown path                                                                                                                         |

### Timeline

```
T=0   stopContainerScrub()
        ├─ metadataScanner.shutdown()        (blocks until metadata scanner exits)
        ├─ dataScanners[i].shutdown()        (blocks until each background scanner exits)
        └─ OnDemandContainerDataScanner.shutdown()
              ├─ canceler.cancel()            ← sets a Java-level flag; JNI doesn't poll it
              ├─ scanExecutor.shutdown()      ← no new tasks accepted
              └─ awaitTermination(5 seconds)  ← waits up to 5 s for in-flight futures
                    ... 5 s pass; one scan future is still inside RocksIterator.next0() ...
              └─ scanExecutor.shutdownNow()   ← sends Thread.interrupt() to worker threads
                    [interrupt is NOT delivered while thread executes JNI native code]
              ← returns; shutdown code continues with scanner thread STILL in JNI

T=1   (shutdown continues past stopContainerScrub, scanner still in native rocksdb code)

T=2   volumeSet.shutdown()
        └─ HddsVolume.shutdown()
              └─ closeDbStore()
                    └─ DatanodeStoreCache.getInstance().removeDB(containerDBPath)
                          ├─ datanodeStoreMap.remove(path)
                          └─ db.getStore().stop()   ← frees the native rocksdb::DB*
                                                       and all associated C++ heap

T=3   [scanner thread returns from one JNI frame and calls RocksIterator.next0() again
        — now the underlying rocksdb::DB* and its block-cache/iterator state are freed]
        → SIGSEGV / hs_err_pid*.log
             #  rocksdb::BinaryHeap<...>::downheap  (MergingIterator use-after-free)
             #  rocksdb::BlockBasedTable::UpdateCacheMissMetrics (null block-cache ptr)
             #  RocksIterator.next0 (JNI boundary)
             #  KeyValueBlockIterator.hasNext
             #  KeyValueContainerCheck.scanData

T=4   blockDeletingService.shutdown()
        [If a block-deleting worker also had an open iterator at T=2, it SIGSEGVs here too]
```

### Why `interrupt()` Does Not Help

Java's `Thread.interrupt()` only sets a Java-level flag. The JVM cannot interrupt a thread that
is executing inside a native (JNI) method. The interrupt is only observed when the thread returns
to Java bytecode. A single `RocksIterator.next0()` call over a large container (millions of
blocks, compaction in progress) can stay inside native code far longer than the 5-second
`awaitTermination` window.

### Why `canceler.cancel()` Does Not Help

The `Canceler` that is passed to `container.scanData()` is a Java object with a volatile boolean
flag. Container scan code checks it between blocks (`if (canceler.isCancelled()) break`). While
the thread is executing RocksDB JNI code, the check never runs. Cancellation only prevents
*future* iterations; it cannot abort an in-progress `next0()`.

---

## What `runV3ShutdownCacheDuringScan` Reproduces

The test bypasses the `miniClusterMode` guard (which makes `shutdownCache()` a no-op in unit
tests) and calls `DatanodeStoreCache.shutdownCache()` in a tight loop while on-demand scan futures
are continuously submitted and kept in-flight:

```java
// runV3ShutdownCacheLoop — one thread
DatanodeStoreCache.setMiniClusterMode(false);
DatanodeStoreCache.getInstance().shutdownCache();   // ← unconditional DB close
DatanodeStoreCache.setMiniClusterMode(true);        // re-enable so scanner can re-open

// runOnDemandScanLoop — one thread per datanode
scanner.scanContainerWithoutGap(container, "crash-repro");  // fire-and-forget; never awaited
```

`shutdownCache()` closes the same V3 `RawDB` that the scanner's live `rocksdb::Iterator*` is
iterating over, reproducing the exact use-after-free that appears in production `hs_err_pid*.log`
files.

---

## CDH-7.1.9.1069 Production Scenario

### Triggering Event: Datanode Shutdown / Rolling Restart

CDH-7.1.9.1069 datanodes use Schema V3 by default (shared per-volume RocksDB). Every graceful
datanode shutdown — including rolling restarts during maintenance, upgrades, or automated cluster
operations — executes `OzoneContainer.stop()`:

```java
// OzoneContainer.stop() — CDH-7.1.9.1069
public void stop() {
    stopContainerScrub();                          // (a) stops scanners; 5-s JNI window
    replicationServer.stop();
    writeChannel.stop();
    readChannel.stop();
    this.handlers.values().forEach(Handler::stop); // (b) KeyValueHandler: no DB close
    hddsDispatcher.shutdown();
    volumeChecker.shutdownAndWait(0, TimeUnit.SECONDS);
    volumeSet.shutdown();                          // (c) HddsVolume.shutdown()
                                                   //     → closeDbStore()
                                                   //     → removeDB() → store.stop()
    metaVolumeSet.shutdown();
    if (dbVolumeSet != null) { dbVolumeSet.shutdown(); }
    blockDeletingService.shutdown();               // (d) AFTER V3 DBs are already closed
    recoveringContainerScrubbingService.shutdown();
}
```

The production race is identical to what the test reproduces:

| Test simulation                      | Production CDH-7.1.9.1069 equivalent                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| `DatanodeStoreCache.shutdownCache()` | `volumeSet.shutdown()` → `HddsVolume.shutdown()` → `closeDbStore()` → `DatanodeStoreCache.removeDB()` |
| On-demand scan fire-and-forget       | On-demand scanner futures submitted by Recon, SCM heartbeat, or client read paths                     |
| 5-second `awaitTermination` gap      | Same 5-second `OnDemandContainerDataScanner.shutdownScanner()` window                                 |

### Two Distinct Hazard Windows in CDH-7.1.9.1069

**Window 1 — On-demand scanner still in JNI when `volumeSet.shutdown()` fires**

```
stopContainerScrub()
  → OnDemandContainerDataScanner.shutdown()
      canceler.cancel()
      scanExecutor.shutdown()
      awaitTermination(5 s)   ← thread A still in RocksIterator.next0()
      shutdownNow()           ← interrupt not effective in JNI; returns immediately
  ← stopContainerScrub() returns
  ...
volumeSet.shutdown()
  → HddsVolume.shutdown()
      → closeDbStore()
          → DatanodeStoreCache.removeDB()
              → store.stop()   ← frees rocksdb::DB* and iterator state
  ← thread A resurfaces from JNI, calls next0() again → SIGSEGV
```

**Window 2 — Block-deleting service holds an iterator when `volumeSet.shutdown()` fires**

The `blockDeletingService` runs workers that open RocksDB iterators to enumerate pending-deletion
blocks. Because `blockDeletingService.shutdown()` is called *after* `volumeSet.shutdown()` (steps
(c) and (d) above), a worker thread that is mid-iteration when `closeDbStore()` is called will
crash identically.

### Why It Is Rare in Normal Operation but Observed in Practice

- Under **heavy replication load** (many under-replicated EC containers being rebuilt) the on-demand
  scanner is continuously triggered. Futures pile up in `scanExecutor`; the 5-second
  `awaitTermination` window is frequently exceeded.
- Under **high write throughput** the block-deleting service has large pending queues; its worker
  threads stay in RocksDB iteration for extended periods.
- **Rolling restarts** during active data movement guarantee that both the scanner and the
  block-deleting service are busy at the moment shutdown is initiated.

### Observed Crash Signatures (from production `hs_err_pid*.log`)

All three production crashes recorded in CDH-7.1.9.1069 environments show:

```
# Signal: SIGSEGV (0xb), code=1, address=<freed heap>
# Java frames:
J  org.rocksdb.RocksIterator.next0()V
J  org.apache.hadoop.ozone.container.keyvalue.helpers.KeyValueBlockIterator.hasNext()Z
J  org.apache.hadoop.ozone.container.keyvalue.KeyValueContainerCheck.scanData(...)
```

with native frames landing in one of two RocksDB code paths:

| Crash pattern | Native frame                                                               | Root cause                                            |
|---------------|----------------------------------------------------------------------------|-------------------------------------------------------|
| **A**         | `rocksdb::BinaryHeap::downheap` inside `MergingIterator::NextAndGetResult` | Heap item pointing into freed `IteratorWrapper`       |
| **B**         | `rocksdb::BlockBasedTable::UpdateCacheMissMetrics` or `GetEntryFromCache`  | Null/dangling block-cache pointer after `DB::Close()` |

Both are canonical use-after-free signatures that arise when `rocksdb::DB::Close()` (called via
`store.stop()`) is invoked while a `rocksdb::Iterator` created from that DB is still alive on
another thread.

---

## Why Schema V2 Does Not Crash

Schema V2 wraps each per-container RocksDB in a `ReferenceCountedDB`. The scanner acquires a
reference inside its try-with-resources block:

```java
try (ReferenceCountedDB db = BlockUtils.getDB(containerData, conf)) {
    // refCount > 0 here
    // ... iterate ...
}   // refCount decremented; if 0, close permitted
```

`ContainerCache.removeDB()` calls `cleanup()`, which checks `refCount > 0` and returns `false`
without closing.  `Preconditions.checkArgument(cleanup())` then throws
`IllegalArgumentException` — the DB is NOT closed, and no SIGSEGV occurs.

---

## Test Harness Summary

| Scenario                     | Test method                         | Production trigger                                           | Expected              |
|------------------------------|-------------------------------------|--------------------------------------------------------------|-----------------------|
| 1 — V3 Direct RemoveDB       | `runV3DirectRemoveDB`               | `DatanodeStoreCache.removeDB()` (import/export, volume fail) | **SIGSEGV**           |
| 2 — V3 Volume Failure        | `runV3VolumeFailureDuringScan`      | `StorageVolumeChecker` I/O error → `HddsVolume.failVolume()` | **SIGSEGV**           |
| **3 — V3 Shutdown Ordering** | **`runV3ShutdownCacheDuringScan`**  | **Datanode shutdown / rolling restart**                      | **SIGSEGV**           |
| 4 — V3 DbVolume Failure      | _(covered by Scenario 1 mechanism)_ | `DbVolume.failVolume()` → `closeAllDbStore()`                | **SIGSEGV**           |
| 5 — V2 Export Race           | `runV2ExportDuringScan`             | Container export during scan                                 | No SIGSEGV (refCount) |
| 6 — V2 Deletion Race         | `runV2DeletionDuringScan`           | Container deletion during scan                               | No SIGSEGV (refCount) |

---

## Fix Directions

The root cause is the absence of reference counting (or equivalent lifecycle tracking) on the
shared Schema V3 `RawDB`. Any correct fix must ensure that `store.stop()` is deferred until
**all** threads that obtained a `RawDB` reference via `getDB()` have finished using it. Options
include:

1. **Reference counting on `RawDB`** — increment on `getDB()`, decrement on caller close;
   `removeDB()` / `shutdownCache()` wait until refcount reaches zero before calling `store.stop()`.
2. **Shutdown ordering fix** — ensure `blockDeletingService.shutdown()` and a full scanner drain
   (not just `shutdownNow()`) complete *before* `volumeSet.shutdown()` closes any DB.
3. **Cooperative cancellation in JNI** — check the `Canceler` flag inside the RocksDB iteration
   loop at the Java level so that tasks self-terminate faster; reduce the window between scanner
   stop and DB close.
