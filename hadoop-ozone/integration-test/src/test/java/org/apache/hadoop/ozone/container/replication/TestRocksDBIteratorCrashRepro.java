/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.ozone.container.replication;

import static org.apache.hadoop.ozone.container.replication.CopyContainerCompression.NO_COMPRESSION;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hadoop.hdds.client.DefaultReplicationConfig;
import org.apache.hadoop.hdds.client.ECReplicationConfig;
import org.apache.hadoop.hdds.client.RatisReplicationConfig;
import org.apache.hadoop.hdds.client.ReplicationConfig;
import org.apache.hadoop.hdds.conf.OzoneConfiguration;
import org.apache.hadoop.hdds.conf.StorageUnit;
import org.apache.hadoop.hdds.protocol.proto.HddsProtos;
import org.apache.hadoop.hdds.scm.ScmConfigKeys;
import org.apache.hadoop.hdds.scm.container.ContainerID;
import org.apache.hadoop.ozone.HddsDatanodeService;
import org.apache.hadoop.ozone.MiniOzoneCluster;
import org.apache.hadoop.ozone.OzoneConfigKeys;
import org.apache.hadoop.ozone.OzoneConsts;
import org.apache.hadoop.ozone.client.BucketArgs;
import org.apache.hadoop.ozone.client.ObjectStore;
import org.apache.hadoop.ozone.client.OzoneBucket;
import org.apache.hadoop.ozone.client.OzoneClient;
import org.apache.hadoop.ozone.client.OzoneClientFactory;
import org.apache.hadoop.ozone.client.OzoneVolume;
import org.apache.hadoop.ozone.client.io.OzoneOutputStream;
import org.apache.hadoop.ozone.container.common.interfaces.Container;
import org.apache.hadoop.ozone.container.common.statemachine.DatanodeConfiguration;
import org.apache.hadoop.ozone.container.common.utils.DatanodeStoreCache;
import org.apache.hadoop.ozone.container.common.volume.HddsVolume;
import org.apache.hadoop.ozone.container.common.volume.MutableVolumeSet;
import org.apache.hadoop.ozone.container.common.volume.StorageVolume;
import org.apache.hadoop.ozone.container.keyvalue.KeyValueContainer;
import org.apache.hadoop.ozone.container.keyvalue.KeyValueContainerData;
import org.apache.hadoop.ozone.container.keyvalue.TarContainerPacker;
import org.apache.hadoop.ozone.container.keyvalue.helpers.BlockUtils;
import org.apache.hadoop.ozone.container.ozoneimpl.ContainerScannerConfiguration;
import org.apache.hadoop.ozone.container.ozoneimpl.OnDemandContainerScanner;
import org.apache.hadoop.ozone.container.ozoneimpl.OzoneContainer;
import org.apache.ozone.test.GenericTestUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manual, non-terminating integration repro harness for the RocksDB
 * SIGSEGV crash seen in production (hs_err_pid*.log files).
 *
 * <p>All three production crashes occur in {@code RocksIterator.next0()} during
 * container data scanning, with the JVM dying in native code at either
 * {@code rocksdb::BinaryHeap::downheap} (MergingIterator) or
 * {@code BlockBasedTable::UpdateCacheMissMetrics} — both are use-after-free
 * crashes caused by the underlying RocksDB {@code DB*} being closed while a
 * live C++ {@code rocksdb::Iterator*} is still mid-iteration.
 *
 * <h3>Schema V3 vs V2 Protection</h3>
 *
 * <p><b>Schema V3</b> (shared per-volume DB, managed by
 * {@link DatanodeStoreCache}): NO protection at all.
 * {@code DatanodeStoreCache.removeDB()} calls {@code db.getStore().stop()}
 * immediately — no reference counting, no iterator tracking.  Any active
 * iterator opened via a prior {@code getDB()} call WILL SIGSEGV.
 *
 * <p><b>Schema V2</b> (per-container DB, managed by {@code ContainerCache}):
 * PROTECTED by {@code ReferenceCountedDB} refCount.  The scanner holds
 * refCount &gt; 0 during iteration (within its try-with-resources block).
 * {@code ContainerCache.removeDB()}, {@code shutdownCache()}, and LRU
 * eviction all check refCount before closing and refuse to close when
 * refCount &gt; 0 (throwing {@code IllegalArgumentException} or skipping
 * eviction).  This means V2 scanner paths do NOT produce SIGSEGV in
 * production — the race exists but is caught by the refCount assertion.
 *
 * <h3>Scenarios covered by this harness</h3>
 * <ol>
 *   <li><b>V3 Direct RemoveDB</b> ({@link #runV3DirectRemoveDB()}) —
 *       calls {@code DatanodeStoreCache.removeDB()} directly.
 *       <b>SIGSEGV expected.</b></li>
 *   <li><b>V3 Volume Failure (broken simulation)</b>
 *       ({@link #runV3VolumeFailureDuringScan()}) —
 *       calls {@code HddsVolume.failVolume()} directly in a loop.
 *       <b>Does NOT reliably reproduce the crash</b> because after the
 *       first call {@code HddsVolume.dbLoaded} is set to {@code false};
 *       subsequent calls to {@code closeDbStore()} return immediately as a
 *       no-op.  The race can only fire once per volume.
 *       See {@link #runV3ActualVolumeFailureDuringScan()} for the correct
 *       version that exercises the full production code path.</li>
 *   <li><b>V3 Shutdown Ordering</b>
 *       ({@link #runV3ShutdownCacheDuringScan()}) — bypasses
 *       miniClusterMode and calls {@code DatanodeStoreCache.shutdownCache()},
 *       simulating the shutdown ordering bug where
 *       {@code volumeSet.shutdown()} closes all V3 DBs before
 *       blockDeletingService is stopped.
 *       <b>SIGSEGV expected.</b></li>
 *   <li><b>V3 DbVolume Failure</b> — DbVolume.failVolume() calls
 *       {@code closeAllDbStore()} which calls {@code removeDB()} for every
 *       HDDS volume mapped to that DB volume.  This is the same
 *       {@code removeDB()} mechanism as Scenario 1.  Not separately testable
 *       in MiniOzoneCluster (no dedicated DB volumes configured), but the
 *       underlying close mechanism is identical.</li>
 *   <li><b>V2 Export Race</b> ({@link #runV2ExportDuringScan()}) —
 *       concurrent export + scan on Schema V2 containers.  Export calls
 *       {@code BlockUtils.removeDB()} which calls
 *       {@code ContainerCache.removeDB()}.  Scanner holds refCount &gt; 0,
 *       so {@code cleanup()} returns false and the
 *       {@code Preconditions.checkArgument} throws IAE — DB is NOT closed.
 *       <b>No SIGSEGV; demonstrates refCount protection.</b></li>
 *   <li><b>V2 Deletion Race</b> ({@link #runV2DeletionDuringScan()}) —
 *       concurrent deletion + scan on Schema V2 containers.  Same refCount
 *       protection as Scenario 5.  Deletion calls
 *       {@code KeyValueContainerUtil.removeContainerDB()} →
 *       {@code BlockUtils.removeDB()} which checks refCount.
 *       <b>No SIGSEGV; demonstrates refCount protection.</b></li>
 *   <li><b>V3 Actual Volume Failure</b>
 *       ({@link #runV3ActualVolumeFailureDuringScan()}) — exercises the full
 *       {@code StorageVolumeChecker} production path without calling
 *       {@code removeDB()} or {@code failVolume()} directly: makes the DB
 *       directory temporarily unreadable so {@code HddsVolume.check()}
 *       returns {@code FAILED}, then calls
 *       {@code MutableVolumeSet.checkAllVolumes()} which drives
 *       {@code handleVolumeFailures()} → {@code MutableVolumeSet.failVolume()}
 *       → {@code HddsVolume.failVolume()} → {@code closeDbStore()} →
 *       {@code DatanodeStoreCache.removeDB()}.
 *       <b>SIGSEGV expected.</b></li>
 *   <li><b>V3 Actual Rolling Restart</b>
 *       ({@link #runV3ActualRollingRestartDuringScan()}) — exercises the
 *       actual production shutdown ordering by calling
 *       {@code cluster.restartHddsDatanode()} in a loop.  Each restart
 *       drives {@code HddsDatanodeService.stop()} →
 *       {@code DatanodeStateMachine.stopDaemon()} →
 *       {@code DatanodeStateMachine.close()} →
 *       {@code OzoneContainer.stop()} with the real shutdown ordering:
 *       {@code stopContainerScrub()} (5-second JNI window), then
 *       {@code volumeSet.shutdown()} → {@code HddsVolume.shutdown()} →
 *       {@code closeDbStore()} → {@code DatanodeStoreCache.removeDB()}.
 *       <b>SIGSEGV expected.</b></li>
 * </ol>
 *
 * <p>Run individual scenarios:
 * <pre>
 * mvn test -pl hadoop-ozone/integration-test \
 *   -Dtest=TestRocksDBIteratorCrashRepro#runV3DirectRemoveDB
 * </pre>
 *
 * <p>V2 scenarios (5, 6) automatically create a Schema V2 cluster with
 * RATIS THREE containers — no special parameters required.  V3 scenarios
 * (1-3) create a Schema V3 cluster with EC 3-2 containers.
 *
 * <p>Each test runs for a configurable duration (default 10 minutes,
 * override with {@code -Drepro.timeout.minutes=N}).  If no SIGSEGV occurs
 * within the timeout the test exits normally.
 *
 * <p>To reduce log noise, the test suppresses INFO-level logging from
 * {@code DatanodeStoreCache} and container scanner classes which otherwise
 * produce thousands of lines per second from the tight add/remove/scan
 * loops.
 */
public class TestRocksDBIteratorCrashRepro {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestRocksDBIteratorCrashRepro.class);

  private static final long TIMEOUT_MINUTES =
      Long.getLong("repro.timeout.minutes", 10L);
  private static final long PROGRESS_LOG_INTERVAL_MS = 15_000L;

  private static final int DATANODE_COUNT = 5;
  private static final int MIN_SEED_KEY_COUNT = 60;
  private static final int MIN_SEED_CONTAINER_COUNT = 10;
  private static final int KEY_SIZE_MB = 8;
  private static final AtomicLong v2ExportAttempts = new AtomicLong();
  // Give both scanners time to establish iterators before the first close.
  private static final long CLOSE_THREAD_STARTUP_DELAY_MS = 1_000L;
  // Short sleep between close rounds: lets scanners re-open the DB and start
  // new iterators before the next close, maintaining a continuous race.
  private static final long CLOSE_THREAD_SLEEP_MS = 50L;

  private static final List<String> keyNames = new CopyOnWriteArrayList<>();
  private static final Set<Long> containerIds = ConcurrentHashMap.newKeySet();
  private static final AtomicLong onDemandScanRounds = new AtomicLong();
  private static final AtomicLong onDemandScanFailures = new AtomicLong();
  private static final AtomicLong closeRounds = new AtomicLong();
  private static final AtomicLong closeFailures = new AtomicLong();
  private static final AtomicLong v2ExportRefCountBlocked = new AtomicLong();
  private static final AtomicLong v2DeleteAttempts = new AtomicLong();
  private static final AtomicLong v2DeleteRefCountBlocked = new AtomicLong();
  private static boolean v2Mode;

  private static MiniOzoneCluster cluster;
  private static OzoneClient client;
  private static OzoneBucket bucket;
  private static OzoneConfiguration clusterConf;
  private static volatile boolean stopWorkers = false;
  private static ExecutorService workers;
  private static ReplicationConfig replicationConfig;

  private static void setUpCluster(boolean v2) throws Exception {
    v2Mode = v2;
    resetCounters();

    // Suppress the high-volume logs that overwhelm the IDE console.
    // The tight close/reopen loop triggers INFO and WARN from many classes:
    //   DatanodeStoreCache: "Added db"/"Removed db" on every add/remove
    //   HddsVolume: "SchemaV3 db is stopped/loaded" on every close/reopen
    //   OnDemandContainerScanner: "Unexpected exception" + stack trace
    //   BlockUtils/RDBStore: errors on DB open during the race
    //   StorageVolume: warnings during volume state changes
    // Suppress the entire container package to ERROR — for a crash repro
    // test we only care about the SIGSEGV, not container lifecycle logs.
    GenericTestUtils.setLogLevel(
        LoggerFactory.getLogger("org.apache.hadoop.ozone.container"),
        org.slf4j.event.Level.ERROR);
    // Also suppress RocksDB native wrapper logs.
    GenericTestUtils.setLogLevel(
        LoggerFactory.getLogger("org.rocksdb"),
        org.slf4j.event.Level.ERROR);

    OzoneConfiguration conf = new OzoneConfiguration();
    conf.setBoolean(ContainerScannerConfiguration.HDDS_CONTAINER_SCRUB_ENABLED,
        true);
    conf.setBoolean(
        ContainerScannerConfiguration.HDDS_CONTAINER_SCRUB_DEV_DATA_ENABLED,
        true);
    conf.setBoolean(
        ContainerScannerConfiguration.HDDS_CONTAINER_SCRUB_DEV_METADATA_ENABLED,
        false);
    // Zero interval: background scanner sweeps containers non-stop with no
    // pause between iterations — the largest possible iterator hold window.
    conf.setTimeDuration(ContainerScannerConfiguration.DATA_SCAN_INTERVAL_KEY,
        0, TimeUnit.SECONDS);
    conf.setTimeDuration(ContainerScannerConfiguration.CONTAINER_SCAN_MIN_GAP,
        0, TimeUnit.SECONDS);
    conf.set(ScmConfigKeys.OZONE_SCM_CONTAINER_SIZE, "32MB");
    conf.setStorageSize(OzoneConfigKeys.OZONE_SCM_BLOCK_SIZE, 4,
        StorageUnit.MB);

    if (v2) {
      // Disable Schema V3 to force per-container RocksDB (Schema V2).
      conf.setBoolean(DatanodeConfiguration.CONTAINER_SCHEMA_V3_ENABLED,
          false);
      LOG.info("V2 MODE: Schema V3 disabled, creating RATIS THREE containers");
    }

    cluster = MiniOzoneCluster.newBuilder(conf)
        .setNumDatanodes(DATANODE_COUNT)
        .build();
    cluster.waitForClusterToBeReady();
    clusterConf = conf;

    client = OzoneClientFactory.getRpcClient(conf);
    ObjectStore store = client.getObjectStore();
    String volumeName = UUID.randomUUID().toString();
    String bucketName = volumeName;
    store.createVolume(volumeName);
    OzoneVolume volume = store.getVolume(volumeName);

    if (v2) {
      replicationConfig = RatisReplicationConfig.getInstance(
          HddsProtos.ReplicationFactor.THREE);
      BucketArgs bucketArgs = BucketArgs.newBuilder()
          .setDefaultReplicationConfig(
              new DefaultReplicationConfig(replicationConfig))
          .build();
      volume.createBucket(bucketName, bucketArgs);
    } else {
      ECReplicationConfig ecConfig = new ECReplicationConfig(3, 2,
          ECReplicationConfig.EcCodec.RS, (int) OzoneConsts.MB);
      replicationConfig = ecConfig;
      BucketArgs bucketArgs = BucketArgs.newBuilder()
          .setDefaultReplicationConfig(new DefaultReplicationConfig(ecConfig))
          .build();
      volume.createBucket(bucketName, bucketArgs);
    }
    bucket = volume.getBucket(bucketName);

    seedKeysAndContainers();
    closeSeededContainers();
    logProgress("seed complete");
  }

  private static void tearDownCluster() {
    stopWorkers = true;
    if (workers != null) {
      workers.shutdownNow();
    }
    try {
      if (client != null) {
        client.close();
      }
    } catch (Exception ignored) {
    }
    if (cluster != null) {
      cluster.shutdown();
    }
    cluster = null;
    client = null;
    bucket = null;
    clusterConf = null;
    workers = null;
    replicationConfig = null;
  }

  private static void resetCounters() {
    keyNames.clear();
    containerIds.clear();
    onDemandScanRounds.set(0);
    onDemandScanFailures.set(0);
    closeRounds.set(0);
    closeFailures.set(0);
    v2ExportAttempts.set(0);
    v2ExportRefCountBlocked.set(0);
    v2DeleteAttempts.set(0);
    v2DeleteRefCountBlocked.set(0);
    stopWorkers = false;
  }

  private static void seedKeysAndContainers() throws Exception {
    byte[] payload = new byte[KEY_SIZE_MB * (int) OzoneConsts.MB];
    for (int i = 0; i < payload.length; i++) {
      payload[i] = (byte) (i % 251);
    }

    int writeAttempt = 0;
    while (keyNames.size() < MIN_SEED_KEY_COUNT
        || containerIds.size() < MIN_SEED_CONTAINER_COUNT) {
      writeAttempt++;
      String keyName = "repro-key-" + UUID.randomUUID();
      try (OzoneOutputStream out = bucket.createKey(
          keyName, payload.length, replicationConfig, new HashMap<>())) {
        out.write(payload);
      }
      keyNames.add(keyName);
      bucket.getKey(keyName).getOzoneKeyLocations().forEach(
          location -> containerIds.add(location.getContainerID()));

      if (writeAttempt % 16 == 0) {
        LOG.info("Seeding progress: keys={}, containers={}",
            keyNames.size(), containerIds.size());
      }
    }
  }

  private static void closeSeededContainers() throws Exception {
    for (Long containerId : containerIds) {
      try {
        cluster.getStorageContainerLocationClient().closeContainer(containerId);
      } catch (Exception ignored) {
        // Containers may already be closed.
      }
    }

    GenericTestUtils.waitFor(() -> {
      int closed = 0;
      for (Long containerId : containerIds) {
        final HddsProtos.LifeCycleState state;
        try {
          state = cluster.getStorageContainerManager().getContainerManager()
              .getContainer(ContainerID.valueOf(containerId)).getState();
        } catch (Exception ex) {
          return false;
        }
        if (state == HddsProtos.LifeCycleState.CLOSED
            || state == HddsProtos.LifeCycleState.QUASI_CLOSED) {
          closed++;
        }
      }
      return closed >= Math.max(1, containerIds.size() / 2);
    }, 1000, 60_000);
  }

  // ========================================================================
  //  Common worker loops
  // ========================================================================

  /**
   * Fires {@code scanContainerWithoutGap()} for every container on the given
   * datanode in a tight loop, never waiting for the returned futures.  This
   * keeps the on-demand scanner's thread pool continuously saturated with
   * in-flight iterators, layered on top of the background scanner.
   */
  private static void runOnDemandScanLoop(HddsDatanodeService dn) {
    OzoneContainer ozoneContainer =
        dn.getDatanodeStateMachine().getContainer();
    OnDemandContainerScanner scanner = ozoneContainer.getOnDemandScanner();

    while (!stopWorkers) {
      try {
        Iterator<Container<?>> iter =
            ozoneContainer.getContainerSet().iterator();
        while (iter.hasNext() && !stopWorkers) {
          Container<?> container = iter.next();
          if (scanner != null) {
            // Fire-and-forget: do NOT wait for the future.  Leaving futures
            // in-flight ensures iterators remain open when the close thread
            // calls removeDB().
            scanner.scanContainerWithoutGap(container, "crash-repro");
          }
        }
        onDemandScanRounds.incrementAndGet();
      } catch (Exception ex) {
        long failures = onDemandScanFailures.incrementAndGet();
        if (failures % 100 == 0) {
          LOG.warn("On-demand scan loop failures so far: {}", failures, ex);
        }
      }
    }
  }

  /**
   * Continuously closes every Schema V3 RocksDB instance held by the given
   * datanode's containers by calling
   * {@link DatanodeStoreCache#removeDB(String)}.
   *
   * <p>For Schema V3 all containers on a volume share one RocksDB; collecting
   * unique {@code dbFile} paths and calling {@code removeDB} on each closes
   * the shared native DB while both the background scanner and on-demand
   * scanner may have live iterators open on it, reproducing the use-after-free
   * that causes the SIGSEGV in production.
   */
  private static void runV3RemoveDBLoop(HddsDatanodeService dn) {
    OzoneContainer ozoneContainer =
        dn.getDatanodeStateMachine().getContainer();
    DatanodeStoreCache storeCache = DatanodeStoreCache.getInstance();

    try {
      Thread.sleep(CLOSE_THREAD_STARTUP_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    while (!stopWorkers) {
      try {
        Set<String> dbPaths = collectV3DbPaths(ozoneContainer);
        for (String path : dbPaths) {
          storeCache.removeDB(path);
        }
        closeRounds.incrementAndGet();
        Thread.sleep(CLOSE_THREAD_SLEEP_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception ex) {
        long failures = closeFailures.incrementAndGet();
        if (failures % 100 == 0) {
          LOG.warn("RocksDB close loop failures so far: {}", failures, ex);
        }
      }
    }
  }

  /**
   * Continuously calls {@link HddsVolume#failVolume()} on every HDDS volume
   * of the given datanode.  This exercises the full production path:
   * {@code StorageVolumeChecker} detects I/O error →
   * {@code HddsVolume.failVolume()} → {@code closeDbStore()} →
   * {@code DatanodeStoreCache.removeDB()}.
   *
   * <p>After failing a volume, the DB is removed from the cache.  The next
   * scanner iteration will call {@code BlockUtils.getDB()} which calls
   * {@code DatanodeStoreCache.getDB()}, which re-opens the DB and adds it
   * back to the cache.  This allows the loop to continue producing the race.
   */
  private static void runV3VolumeFailureLoop(HddsDatanodeService dn) {
    OzoneContainer ozoneContainer =
        dn.getDatanodeStateMachine().getContainer();

    try {
      Thread.sleep(CLOSE_THREAD_STARTUP_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    while (!stopWorkers) {
      try {
        List<StorageVolume> volumes =
            ozoneContainer.getVolumeSet().getVolumesList();
        for (StorageVolume vol : volumes) {
          if (vol instanceof HddsVolume) {
            // failVolume() → closeDbStore() → removeDB() — the production
            // crash path.  This closes the shared V3 DB for this volume.
            ((HddsVolume) vol).failVolume();
          }
        }
        closeRounds.incrementAndGet();
        Thread.sleep(CLOSE_THREAD_SLEEP_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception ex) {
        long failures = closeFailures.incrementAndGet();
        if (failures % 100 == 0) {
          LOG.warn("Volume failure loop failures so far: {}", failures, ex);
        }
      }
    }
  }

  /**
   * Bypasses miniClusterMode and calls
   * {@link DatanodeStoreCache#shutdownCache()} while scanners are running.
   *
   * <p>In production, {@code OzoneContainer.stop()} calls
   * {@code KeyValueHandler.stop()} → {@code BlockUtils.shutdownCache()} →
   * {@code DatanodeStoreCache.shutdownCache()} at step 5, BEFORE scanners
   * are stopped (step 8+) and BEFORE blockDeletingService.shutdown() (step
   * 10).  This test simulates that ordering bug.
   *
   * <p>In MiniOzoneCluster, {@code DatanodeStoreCache.shutdownCache()} is a
   * no-op because miniClusterMode is true.  We bypass that by calling
   * {@code setMiniClusterMode(false)} before each shutdownCache() call.
   */
  private static void runV3ShutdownCacheLoop(HddsDatanodeService dn) {
    try {
      Thread.sleep(CLOSE_THREAD_STARTUP_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    while (!stopWorkers) {
      try {
        // Bypass the miniClusterMode guard so shutdownCache() actually
        // closes the DBs.
        DatanodeStoreCache.setMiniClusterMode(false);
        DatanodeStoreCache.getInstance().shutdownCache();
        // Re-enable miniClusterMode so that normal datanode operations
        // (scanner getDB() re-opening DBs) work correctly.
        DatanodeStoreCache.setMiniClusterMode(true);

        closeRounds.incrementAndGet();
        Thread.sleep(CLOSE_THREAD_SLEEP_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception ex) {
        long failures = closeFailures.incrementAndGet();
        if (failures % 100 == 0) {
          LOG.warn("ShutdownCache loop failures so far: {}", failures, ex);
        }
      }
    }
  }

  /**
   * Continuously exports every container on the given datanode using
   * {@link KeyValueContainer#exportContainerData}.  For Schema V2,
   * export calls {@code BlockUtils.removeDB()} which goes through
   * {@code ContainerCache.removeDB()} → {@code cleanup()} → checks refCount.
   *
   * <p>When the scanner holds refCount &gt; 0, {@code cleanup()} returns
   * false and the Preconditions assertion throws {@code IllegalArgumentException}
   * — the DB is NOT closed and no SIGSEGV occurs.  This test demonstrates
   * that the race window exists but is caught by the refCount check.
   */
  private static void runV2ExportLoop(HddsDatanodeService dn) {
    OzoneContainer ozoneContainer =
        dn.getDatanodeStateMachine().getContainer();
    TarContainerPacker packer = new TarContainerPacker(NO_COMPRESSION);

    try {
      Thread.sleep(CLOSE_THREAD_STARTUP_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    while (!stopWorkers) {
      try {
        Iterator<Container<?>> iter =
            ozoneContainer.getContainerSet().iterator();
        while (iter.hasNext() && !stopWorkers) {
          Container<?> container = iter.next();
          if (container instanceof KeyValueContainer) {
            KeyValueContainer kvContainer = (KeyValueContainer) container;
            KeyValueContainerData data = kvContainer.getContainerData();
            // Only V2 containers have per-container DB that export removes.
            if (!data.hasSchema(OzoneConsts.SCHEMA_V3)) {
              v2ExportAttempts.incrementAndGet();
              try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                kvContainer.exportContainerData(out, packer);
              } catch (IllegalArgumentException iae) {
                // Expected: refCount > 0 because scanner holds a reference.
                // This proves the race window exists but refCount prevents
                // the DB from being closed.
                v2ExportRefCountBlocked.incrementAndGet();
              } catch (IllegalStateException ise) {
                // Container may not be in CLOSED/QUASI_CLOSED state.
              }
            }
          }
        }
        closeRounds.incrementAndGet();
        Thread.sleep(CLOSE_THREAD_SLEEP_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception ex) {
        long failures = closeFailures.incrementAndGet();
        if (failures % 100 == 0) {
          LOG.warn("V2 export loop failures so far: {}", failures, ex);
        }
      }
    }
  }

  /**
   * Continuously attempts to delete every container on the given datanode
   * by calling {@code BlockUtils.removeDB()} directly on V2 containers.
   *
   * <p>We don't call the full {@code KeyValueHandler.deleteInternal()} because
   * that acquires the container write lock, marks the container for delete,
   * and removes it from the container set — all of which would prevent the
   * scanner from finding the container again.  Instead, we directly call
   * {@code BlockUtils.removeDB()} to exercise only the V2 DB close path.
   *
   * <p>Same outcome as export: refCount &gt; 0 prevents the close.
   */
  private static void runV2DeletionLoop(HddsDatanodeService dn) {
    OzoneContainer ozoneContainer =
        dn.getDatanodeStateMachine().getContainer();

    try {
      Thread.sleep(CLOSE_THREAD_STARTUP_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    while (!stopWorkers) {
      try {
        Iterator<Container<?>> iter =
            ozoneContainer.getContainerSet().iterator();
        while (iter.hasNext() && !stopWorkers) {
          Container<?> container = iter.next();
          if (container instanceof KeyValueContainer) {
            KeyValueContainerData data =
                ((KeyValueContainer) container).getContainerData();
            if (!data.hasSchema(OzoneConsts.SCHEMA_V3)) {
              v2DeleteAttempts.incrementAndGet();
              try {
                BlockUtils.removeDB(data, clusterConf);
              } catch (IllegalArgumentException iae) {
                // Expected: refCount > 0 blocks the close.
                v2DeleteRefCountBlocked.incrementAndGet();
              } catch (IllegalStateException ise) {
                // Preconditions.checkState for schema version.
              }
            }
          }
        }
        closeRounds.incrementAndGet();
        Thread.sleep(CLOSE_THREAD_SLEEP_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception ex) {
        long failures = closeFailures.incrementAndGet();
        if (failures % 100 == 0) {
          LOG.warn("V2 deletion loop failures so far: {}", failures, ex);
        }
      }
    }
  }

  // ========================================================================
  //  Helpers
  // ========================================================================

  private static Set<String> collectV3DbPaths(OzoneContainer ozoneContainer) {
    Set<String> dbPaths = new HashSet<>();
    Iterator<Container<?>> iter =
        ozoneContainer.getContainerSet().iterator();
    while (iter.hasNext()) {
      Container<?> container = iter.next();
      if (container instanceof KeyValueContainer) {
        KeyValueContainerData data =
            ((KeyValueContainer) container).getContainerData();
        File dbFile = data.getDbFile();
        if (dbFile != null) {
          dbPaths.add(dbFile.getAbsolutePath());
        }
      }
    }
    return dbPaths;
  }

  private static void startWorkers(int threadsPerDn,
      List<HddsDatanodeService> datanodes, String name) {
    workers = Executors.newFixedThreadPool(threadsPerDn * datanodes.size(),
        new NamedDaemonFactory(name));
  }

  private static void logProgress(String prefix) {
    if (v2Mode) {
      LOG.info(
          "[{}] keys={}, containers={}, onDemandScanRounds={}, "
              + "onDemandScanFailures={}, closeRounds={}, closeFailures={}, "
              + "v2ExportAttempts={}, v2ExportBlocked={}, "
              + "v2DeleteAttempts={}, v2DeleteBlocked={}",
          prefix,
          keyNames.size(),
          containerIds.size(),
          onDemandScanRounds.get(),
          onDemandScanFailures.get(),
          closeRounds.get(),
          closeFailures.get(),
          v2ExportAttempts.get(),
          v2ExportRefCountBlocked.get(),
          v2DeleteAttempts.get(),
          v2DeleteRefCountBlocked.get());
    } else {
      LOG.info(
          "[{}] keys={}, containers={}, onDemandScanRounds={}, "
              + "onDemandScanFailures={}, closeRounds={}, closeFailures={}",
          prefix,
          keyNames.size(),
          containerIds.size(),
          onDemandScanRounds.get(),
          onDemandScanFailures.get(),
          closeRounds.get(),
          closeFailures.get());
    }
  }

  private static void runProgressLoop() throws InterruptedException {
    long deadlineMs = System.currentTimeMillis()
        + TimeUnit.MINUTES.toMillis(TIMEOUT_MINUTES);
    LOG.info("Race loop started. Timeout in {} minutes. "
        + "Override with -Drepro.timeout.minutes=N", TIMEOUT_MINUTES);
    while (System.currentTimeMillis() < deadlineMs) {
      Thread.sleep(PROGRESS_LOG_INTERVAL_MS);
      logProgress("running");
    }
    logProgress("timeout reached — no SIGSEGV within "
        + TIMEOUT_MINUTES + " minutes");
  }

  // ========================================================================
  //  Scenario 1: V3 Direct RemoveDB (original test)
  // ========================================================================

  /**
   * <b>Scenario 1: V3 Direct RemoveDB.</b>
   *
   * <p>Calls {@code DatanodeStoreCache.removeDB()} directly for each unique
   * V3 DB path while both scanners have active iterators.  This is the most
   * general V3 crash scenario — every other V3 scenario ultimately calls
   * {@code removeDB()}.
   *
   * <p><b>Expected: SIGSEGV.</b>
   */
  @Test
  public void runV3DirectRemoveDB() throws Exception {
    setUpCluster(false);
    try {
      List<HddsDatanodeService> datanodes = cluster.getHddsDatanodes();
      startWorkers(2, datanodes, "v3-removedb");

      for (HddsDatanodeService dn : datanodes) {
        workers.submit(() -> runOnDemandScanLoop(dn));
        workers.submit(() -> runV3RemoveDBLoop(dn));
      }

      runProgressLoop();
    } finally {
      tearDownCluster();
    }
  }

  /**
   * Backwards-compatible alias for {@link #runV3DirectRemoveDB()}.
   */
  @Test
  public void runInfiniteRocksDBCloseDuringScan() throws Exception {
    runV3DirectRemoveDB();
  }

  // ========================================================================
  //  Scenario 2: V3 Volume Failure (broken simulation — see Scenario 7)
  // ========================================================================

  /**
   * On-demand scan loop that reads its target DN from an
   * {@link AtomicReference}.  When the reference is {@code null} (e.g.
   * while the target DN is being restarted), the thread sleeps briefly and
   * retries.  This allows a single scan thread to follow a DN through
   * repeated restarts without the restart thread needing to create new scan
   * threads each cycle.
   */
  private static void runOnDemandScanLoopRef(
      AtomicReference<HddsDatanodeService> targetRef) {
    while (!stopWorkers) {
      try {
        HddsDatanodeService dn = targetRef.get();
        if (dn == null) {
          Thread.sleep(100);
          continue;
        }
        OzoneContainer ozoneContainer =
            dn.getDatanodeStateMachine().getContainer();
        OnDemandContainerScanner scanner = ozoneContainer.getOnDemandScanner();
        Iterator<Container<?>> iter =
            ozoneContainer.getContainerSet().iterator();
        while (iter.hasNext() && !stopWorkers) {
          Container<?> container = iter.next();
          if (scanner != null) {
            scanner.scanContainerWithoutGap(container, "crash-repro");
          }
        }
        onDemandScanRounds.incrementAndGet();
      } catch (Exception ex) {
        long failures = onDemandScanFailures.incrementAndGet();
        if (failures % 100 == 0) {
          LOG.warn("On-demand scan loop (ref) failures: {}", failures, ex);
        }
      }
    }
  }

  // ========================================================================
  //  Scenario 3: V3 Shutdown Ordering
  // ========================================================================

  /**
   * <b>Scenario 3: V3 Shutdown Ordering race.</b>
   *
   * <p>Simulates the shutdown ordering bug in {@code OzoneContainer.stop()}:
   * <ol>
   *   <li>Step 5: {@code handlers.forEach(Handler::stop)} →
   *       {@code KeyValueHandler.stop()} → {@code BlockUtils.shutdownCache()}
   *       → {@code DatanodeStoreCache.shutdownCache()} — closes ALL V3 DBs
   *       </li>
   *   <li>Step 8: {@code volumeSet.shutdown()} — closes V3 DBs again via
   *       HddsVolume.shutdown()</li>
   *   <li>Step 10: {@code blockDeletingService.shutdown()} — AFTER DBs are
   *       already closed!</li>
   * </ol>
   *
   * <p>In MiniOzoneCluster, {@code DatanodeStoreCache.shutdownCache()} skips
   * clearing in miniClusterMode.  This test bypasses that guard to exercise
   * the production code path.
   *
   * <p><b>Expected: SIGSEGV.</b>
   */
  @Test
  public void runV3ShutdownCacheDuringScan() throws Exception {
    setUpCluster(false);
    try {
      List<HddsDatanodeService> datanodes = cluster.getHddsDatanodes();
      // Only need 1 shutdown thread globally (shutdownCache is static/global).
      workers = Executors.newFixedThreadPool(datanodes.size() + 1,
          new NamedDaemonFactory("v3-shutdown"));

      for (HddsDatanodeService dn : datanodes) {
        workers.submit(() -> runOnDemandScanLoop(dn));
      }
      // One thread to call shutdownCache() in a loop.
      workers.submit(() -> runV3ShutdownCacheLoop(datanodes.get(0)));

      runProgressLoop();
    } finally {
      tearDownCluster();
    }
  }

  // ========================================================================
  //  Scenario 5: V2 Export Race
  // ========================================================================

  /**
   * <b>Scenario 5: V2 Export Race (Schema V2 only).</b>
   *
   * <p>Creates a Schema V2 cluster with RATIS THREE containers.
   *
   * <p>Concurrent export + scan on V2 containers.  Export calls
   * {@code BlockUtils.removeDB()} → {@code ContainerCache.removeDB()} →
   * {@code cleanup()}.  When the scanner holds refCount &gt; 0 (within its
   * try-with-resources block in {@code KeyValueContainerCheck.scanData()}),
   * {@code cleanup()} returns false and {@code Preconditions.checkArgument}
   * throws {@code IllegalArgumentException}.
   *
   * <p>Watch the log for {@code v2ExportBlocked} incrementing — this proves
   * the race window is being hit but the refCount check is preventing the
   * DB from being closed.
   *
   * <p><b>Expected: No SIGSEGV.  IAE logged when refCount blocks the close.
   * </b>
   */
  @Test
  public void runV2ExportDuringScan() throws Exception {
    setUpCluster(true);
    try {
      List<HddsDatanodeService> datanodes = cluster.getHddsDatanodes();
      startWorkers(2, datanodes, "v2-export");

      for (HddsDatanodeService dn : datanodes) {
        workers.submit(() -> runOnDemandScanLoop(dn));
        workers.submit(() -> runV2ExportLoop(dn));
      }

      runProgressLoop();
    } finally {
      tearDownCluster();
    }
  }

  // ========================================================================
  //  Scenario 6: V2 Deletion Race
  // ========================================================================

  /**
   * <b>Scenario 6: V2 Deletion Race (Schema V2 only).</b>
   *
   * <p>Creates a Schema V2 cluster with RATIS THREE containers.
   *
   * <p>Concurrent deletion + scan on V2 containers.  Directly calls
   * {@code BlockUtils.removeDB()} to exercise the V2 DB close path without
   * actually deleting the container (which would remove it from the container
   * set and prevent future scanning).
   *
   * <p>Same outcome as export: refCount &gt; 0 prevents the close.
   *
   * <p><b>Expected: No SIGSEGV.  IAE logged when refCount blocks the close.
   * </b>
   */
  @Test
  public void runV2DeletionDuringScan() throws Exception {
    setUpCluster(true);
    try {
      List<HddsDatanodeService> datanodes = cluster.getHddsDatanodes();
      startWorkers(2, datanodes, "v2-delete");

      for (HddsDatanodeService dn : datanodes) {
        workers.submit(() -> runOnDemandScanLoop(dn));
        workers.submit(() -> runV2DeletionLoop(dn));
      }

      runProgressLoop();
    } finally {
      tearDownCluster();
    }
  }

  // ========================================================================
  //  Scenario 7: V3 Actual Volume Failure (via StorageVolumeChecker path)
  // ========================================================================

  /**
   * Continuously triggers the actual production volume-failure path on the
   * datanode at {@code dnIndex}:
   *
   * <ol>
   *   <li>Collects the V3 RocksDB directories for every HDDS volume on the
   *       target DN — the same paths that {@link HddsVolume#check(Boolean)}
   *       inspects.</li>
   *   <li>Makes those directories temporarily unreadable so that
   *       {@code check()} returns {@code VolumeCheckResult.FAILED}.</li>
   *   <li>Calls {@link MutableVolumeSet#checkAllVolumes()} which drives:
   *       {@code StorageVolumeChecker} → {@code handleVolumeFailures()} →
   *       {@code MutableVolumeSet.failVolume()} →
   *       {@code HddsVolume.failVolume()} → {@code closeDbStore()} →
   *       {@code DatanodeStoreCache.removeDB()} → {@code store.stop()}.</li>
   *   <li>Restores directory permissions.</li>
   *   <li>Restarts the DN so the next iteration starts with fresh
   *       {@code HddsVolume} instances ({@code dbLoaded == true}).  This
   *       is the key fix over {@link #runV3VolumeFailureDuringScan()}: after
   *       the restart {@code dbLoaded} is {@code true} again, so
   *       {@code closeDbStore()} is not a no-op.</li>
   * </ol>
   */
  private static void runActualVolumeFailureLoop(
      int dnIndex, AtomicReference<HddsDatanodeService> targetRef) {
    try {
      Thread.sleep(CLOSE_THREAD_STARTUP_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    while (!stopWorkers) {
      try {
        HddsDatanodeService dn = targetRef.get();
        if (dn == null) {
          Thread.sleep(100);
          continue;
        }
        OzoneContainer ozoneContainer =
            dn.getDatanodeStateMachine().getContainer();
        MutableVolumeSet volumeSet = ozoneContainer.getVolumeSet();

        // Build the list of V3 RocksDB directories for this DN.
        // HddsVolume.check() inspects: new File(dbParentDir, CONTAINER_DB_NAME)
        // which is the same path stored in DatanodeStoreCache and returned
        // by KeyValueContainerData.getDbFile().
        List<File> dbDirs = new ArrayList<>();
        for (StorageVolume vol : volumeSet.getVolumesList()) {
          if (vol instanceof HddsVolume) {
            HddsVolume hddsVol = (HddsVolume) vol;
            File dbDir = new File(
                hddsVol.getDbParentDir(), OzoneConsts.CONTAINER_DB_NAME);
            if (dbDir.exists()) {
              dbDirs.add(dbDir);
            }
          }
        }
        if (dbDirs.isEmpty()) {
          Thread.sleep(100);
          continue;
        }

        // Make the DB directories unreadable.  Active scanner iterators
        // already hold open native file descriptors and are NOT evicted by
        // the permission change — they continue running in JNI.
        // HddsVolume.check() line 314: !dbFile.canRead() → FAILED.
        for (File dbDir : dbDirs) {
          dbDir.setReadable(false);
        }
        try {
          // Invoke the actual production StorageVolumeChecker path.
          // MutableVolumeSet.checkAllVolumes() uses the real volumeChecker
          // that was configured during datanode startup (not a mock):
          //   StorageVolumeChecker.checkAllVolumes(volumes)
          //   → HddsVolume.check() → VolumeCheckResult.FAILED
          //   → MutableVolumeSet.handleVolumeFailures()
          //   → MutableVolumeSet.failVolume(volumeRoot)
          //   → HddsVolume.failVolume() → closeDbStore()
          //   → DatanodeStoreCache.removeDB() → store.stop()
          // Any scan future executing RocksIterator.next0() at this moment
          // dereferences the freed rocksdb::DB* → SIGSEGV.
          volumeSet.checkAllVolumes();
        } finally {
          for (File dbDir : dbDirs) {
            dbDir.setReadable(true);
          }
        }
        closeRounds.incrementAndGet();

        // After failVolume() the volume is in failedVolumeMap; the next
        // checkAllVolumes() would see an empty volumeMap and be a no-op.
        // Restart the DN to get a fresh HddsVolume with dbLoaded == true.
        targetRef.set(null);
        cluster.restartHddsDatanode(dnIndex, false);
        targetRef.set(cluster.getHddsDatanodes().get(dnIndex));

        // Allow scan futures to re-saturate the new DN's executor before
        // the next failure injection.
        Thread.sleep(CLOSE_THREAD_SLEEP_MS * 20);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception ex) {
        long failures = closeFailures.incrementAndGet();
        if (failures % 10 == 0) {
          LOG.warn("Actual volume failure loop failures: {}", failures, ex);
        }
      }
    }
  }

  // ========================================================================
  //  Scenario 8: V3 Actual Rolling Restart (via OzoneContainer.stop() path)
  // ========================================================================

  /**
   * Continuously restarts the datanode at {@code dnIndex} to exercise the
   * actual production shutdown ordering in {@code OzoneContainer.stop()}:
   *
   * <pre>
   *   stopContainerScrub()   — awaitTermination(5 s), then shutdownNow()
   *                            JNI threads cannot be interrupted; they
   *                            continue executing native code.
   *   volumeSet.shutdown()   — HddsVolume.shutdown() → closeDbStore()
   *                            → DatanodeStoreCache.removeDB() → store.stop()
   *                            frees rocksdb::DB* while JNI may be running.
   *   blockDeletingService.shutdown()  — called AFTER DBs are closed.
   * </pre>
   *
   * <p>The scan thread keeps submitting futures against the target DN right
   * up until {@code stopContainerScrub()} disables new submissions.
   * In-flight futures that entered native code before {@code shutdownNow()}
   * are still executing when {@code volumeSet.shutdown()} fires → SIGSEGV.
   *
   * <p>After each restart, {@code targetRef} is updated to the new
   * {@code HddsDatanodeService} so the scan thread switches targets
   * automatically.
   */
  private static void runRollingRestartLoop(
      int dnIndex, AtomicReference<HddsDatanodeService> targetRef) {
    try {
      Thread.sleep(CLOSE_THREAD_STARTUP_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    while (!stopWorkers) {
      try {
        // Give the scan thread time to accumulate in-flight futures on the
        // target DN before triggering the shutdown.
        Thread.sleep(CLOSE_THREAD_SLEEP_MS * 20);

        // Pause the scan thread's target reference slightly before the stop
        // so newly submitted futures land in the executor queue (not yet
        // running) — they are the ones racing with volumeSet.shutdown().
        targetRef.set(null);

        // cluster.restartHddsDatanode() calls stopDatanode() synchronously:
        //   HddsDatanodeService.stop()
        //   → DatanodeStateMachine.stopDaemon()
        //   → DatanodeStateMachine.close()
        //   → OzoneContainer.stop()  ← REAL production shutdown ordering
        //       1. stopContainerScrub(): sends shutdownNow() after 5 s;
        //          returns even if JNI threads are still running.
        //       2. volumeSet.shutdown() → HddsVolume.shutdown()
        //          → closeDbStore() → DatanodeStoreCache.removeDB()
        //          → store.stop() — frees rocksdb::DB*
        //       3. blockDeletingService.shutdown() — AFTER DBs closed.
        // Futures in JNI at step (2) → SIGSEGV.
        cluster.restartHddsDatanode(dnIndex, false);

        // Update reference so scan thread uses the fresh DN instance.
        targetRef.set(cluster.getHddsDatanodes().get(dnIndex));

        closeRounds.incrementAndGet();
        Thread.sleep(CLOSE_THREAD_SLEEP_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception ex) {
        long failures = closeFailures.incrementAndGet();
        if (failures % 10 == 0) {
          LOG.warn("Rolling restart loop failures: {}", failures, ex);
        }
      }
    }
  }

  // ========================================================================
  //  Helpers for Scenarios 7 and 8
  // ========================================================================

  /**
   * <b>Scenario 2: V3 Volume Failure during scanning.</b>
   *
   * <p><b>WHY THIS TEST DOES NOT RELIABLY REPRODUCE THE CRASH:</b>
   *
   * <p>This test calls {@link HddsVolume#failVolume()} directly in a loop.
   * The first call works: {@code HddsVolume.closeDbStore()} sees
   * {@code dbLoaded.get() == true}, calls
   * {@code DatanodeStoreCache.removeDB()}, then sets
   * {@code dbLoaded.set(false)}.
   *
   * <p>After the first call the scanner's {@code BlockUtils.getDB()} call
   * re-opens the shared DB (via {@code DatanodeStoreCache.getDB()}), putting
   * a fresh {@code RawDB} back in the cache.  However, {@code HddsVolume.dbLoaded}
   * stays {@code false} — the volume object does not know the DB was
   * reopened by the scanner.
   *
   * <p>Every subsequent loop iteration calls {@code failVolume()} again.
   * {@code closeDbStore()} checks {@code dbLoaded.get()} first and returns
   * immediately as a <b>no-op</b>.  The loop fires the race at most once per
   * volume.  If that single window is missed (very common), the test runs
   * forever without reproducing the crash.
   *
   * <p>See {@link #runV3ActualVolumeFailureDuringScan()} (Scenario 7) for
   * the correct version that goes through the full
   * {@code StorageVolumeChecker} production path and is reliably continuous.
   *
   * <p><b>Expected: SIGSEGV on first iteration only; subsequent iterations
   * are no-ops.</b>
   */
  @Test
  public void runV3VolumeFailureDuringScan() throws Exception {
    setUpCluster(false);
    try {
      List<HddsDatanodeService> datanodes = cluster.getHddsDatanodes();
      startWorkers(2, datanodes, "v3-volFail");

      for (HddsDatanodeService dn : datanodes) {
        workers.submit(() -> runOnDemandScanLoop(dn));
        workers.submit(() -> runV3VolumeFailureLoop(dn));
      }

      runProgressLoop();
    } finally {
      tearDownCluster();
    }
  }

  /**
   * <b>Scenario 7: V3 Actual Volume Failure — full production path.</b>
   *
   * <p>Triggers the crash through the real production call chain WITHOUT
   * calling {@code removeDB()} or {@code failVolume()} directly:
   *
   * <ol>
   *   <li>The on-demand scanner fires fire-and-forget futures against all
   *       containers on DN 0, keeping the shared V3 {@code rocksdb::Iterator*}
   *       continuously active in native code.</li>
   *   <li>The DB directories for every HDDS volume on DN 0 are made
   *       temporarily unreadable ({@code File.setReadable(false)}).  Active
   *       scanner iterators already hold open file descriptors and are
   *       unaffected by the permission change at the OS level.</li>
   *   <li>{@code MutableVolumeSet.checkAllVolumes()} is called.  This drives
   *       the actual production check path:
   *       {@code StorageVolumeChecker.checkAllVolumes(volumes)} →
   *       {@code HddsVolume.check()} (line 314: {@code !dbFile.canRead()})
   *       → {@code VolumeCheckResult.FAILED} →
   *       {@code MutableVolumeSet.handleVolumeFailures()} →
   *       {@code MutableVolumeSet.failVolume()} →
   *       {@code HddsVolume.failVolume()} → {@code closeDbStore()} →
   *       {@code DatanodeStoreCache.removeDB()} → {@code store.stop()}.</li>
   *   <li>Any scanner future still inside {@code RocksIterator.next0()} at
   *       step (3) dereferences the now-freed {@code rocksdb::DB*} →
   *       <b>SIGSEGV</b>.</li>
   *   <li>DB directories are restored and DN 0 is restarted to reset
   *       {@code HddsVolume.dbLoaded} to {@code true}, allowing the loop
   *       to fire again.  (This reset is the fix for the broken Scenario 2
   *       which cannot loop because {@code dbLoaded} is never reset.)</li>
   * </ol>
   *
   * <p>Unlike {@link #runV3VolumeFailureDuringScan()} (Scenario 2), this
   * test reliably produces a continuous race because each restart gives the
   * DN fresh {@code HddsVolume} instances with {@code dbLoaded == true}.
   *
   * <p><b>Expected: SIGSEGV.</b>
   */
  @Test
  public void runV3ActualVolumeFailureDuringScan() throws Exception {
    setUpCluster(false);
    try {
      List<HddsDatanodeService> datanodes = cluster.getHddsDatanodes();
      // DN 0 is the restart target; all other DNs run scan loops continuously.
      AtomicReference<HddsDatanodeService> targetRef =
          new AtomicReference<>(datanodes.get(0));

      // threadCount = 1 per non-target DN + 1 for the target DN scan + 1 for
      // the volume-failure loop.
      workers = Executors.newFixedThreadPool(datanodes.size() + 1,
          new NamedDaemonFactory("v3-actualVolFail"));

      for (int i = 1; i < datanodes.size(); i++) {
        final HddsDatanodeService dn = datanodes.get(i);
        workers.submit(() -> runOnDemandScanLoop(dn));
      }
      workers.submit(() -> runOnDemandScanLoopRef(targetRef));
      workers.submit(() -> runActualVolumeFailureLoop(0, targetRef));

      runProgressLoop();
    } finally {
      tearDownCluster();
    }
  }

  /**
   * <b>Scenario 8: V3 Actual Rolling Restart — full production shutdown path.</b>
   *
   * <p>Triggers the crash through the actual datanode restart path WITHOUT
   * calling {@code removeDB()} or {@code shutdownCache()} directly.  This
   * replicates the production rolling-restart scenario that caused all three
   * observed {@code hs_err_pid*.log} crashes.
   *
   * <p>Each call to {@code cluster.restartHddsDatanode()} drives the real
   * production code path:
   * <pre>
   *   HddsDatanodeService.stop()
   *   → DatanodeStateMachine.stopDaemon()
   *   → DatanodeStateMachine.close()
   *   → OzoneContainer.stop()
   *       1. stopContainerScrub()
   *            → OnDemandContainerScanner shutdown: awaitTermination(5 s)
   *              then shutdownNow() — JNI threads are NOT interrupted
   *       2. handlers.forEach(Handler::stop)
   *       3. volumeSet.shutdown()
   *            → HddsVolume.shutdown()
   *            → closeDbStore()
   *            → DatanodeStoreCache.removeDB()
   *            → store.stop()            ← frees rocksdb::DB*
   *       4. blockDeletingService.shutdown()  ← AFTER DBs already closed
   * </pre>
   *
   * <p>The race: scan futures submitted just before {@code stopContainerScrub()}
   * may still be executing {@code RocksIterator.next0()} (a blocking JNI
   * call) when step (3) frees the underlying {@code rocksdb::DB*}.  The
   * next {@code next0()} invocation dereferences freed memory → SIGSEGV.
   *
   * <p>After each restart DN 0 is replaced with a fresh
   * {@code HddsDatanodeService} (new {@code HddsVolume} instances,
   * {@code dbLoaded == true}), and the scan thread switches to the new
   * instance via the {@code AtomicReference}, making the loop continuous.
   *
   * <p><b>Expected: SIGSEGV.</b>
   */
  @Test
  public void runV3ActualRollingRestartDuringScan() throws Exception {
    setUpCluster(false);
    try {
      List<HddsDatanodeService> datanodes = cluster.getHddsDatanodes();
      // DN 0 is the restart target; all other DNs run scan loops continuously.
      AtomicReference<HddsDatanodeService> targetRef =
          new AtomicReference<>(datanodes.get(0));

      // threadCount = 1 per non-target DN + 1 for the target DN scan + 1 for
      // the rolling-restart loop.
      workers = Executors.newFixedThreadPool(datanodes.size() + 1,
          new NamedDaemonFactory("v3-actualRestart"));

      for (int i = 1; i < datanodes.size(); i++) {
        final HddsDatanodeService dn = datanodes.get(i);
        workers.submit(() -> runOnDemandScanLoop(dn));
      }
      workers.submit(() -> runOnDemandScanLoopRef(targetRef));
      workers.submit(() -> runRollingRestartLoop(0, targetRef));

      runProgressLoop();
    } finally {
      tearDownCluster();
    }
  }

  // ========================================================================
  //  Thread factory
  // ========================================================================

  private static final class NamedDaemonFactory implements ThreadFactory {
    private final AtomicLong sequence = new AtomicLong();
    private final String prefix;

    private NamedDaemonFactory(String prefix) {
      this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable runnable) {
      Thread t = new Thread(runnable,
          prefix + "-" + sequence.incrementAndGet());
      t.setDaemon(true);
      return t;
    }
  }
}
