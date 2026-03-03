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

import java.io.File;
import java.io.IOException;
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
import org.apache.hadoop.hdds.client.DefaultReplicationConfig;
import org.apache.hadoop.hdds.client.ECReplicationConfig;
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
import org.apache.hadoop.ozone.container.common.utils.DatanodeStoreCache;
import org.apache.hadoop.ozone.container.keyvalue.KeyValueContainer;
import org.apache.hadoop.ozone.container.keyvalue.KeyValueContainerData;
import org.apache.hadoop.ozone.container.ozoneimpl.ContainerScannerConfiguration;
import org.apache.hadoop.ozone.container.ozoneimpl.OnDemandContainerScanner;
import org.apache.hadoop.ozone.container.ozoneimpl.OzoneContainer;
import org.apache.ozone.test.GenericTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
 * <p>EC 3-2 containers use Schema V3: all containers on a given volume share a
 * single RocksDB instance, managed by {@link DatanodeStoreCache} (a JVM
 * singleton). {@code DatanodeStoreCache.removeDB()} calls
 * {@code db.getStore().stop()} which closes the native DB immediately — no
 * reference counting, no iterator protection. Any active iterator opened via a
 * prior {@code getDB()} call on the same path will SIGSEGV on its next
 * {@code next()} invocation.
 *
 * <p>Two scanner sources run simultaneously to maximise the race window:
 * <ol>
 *   <li><b>Background scanner</b> — starts automatically with
 *       {@code dataScanInterval=0}; sweeps ALL containers on a volume
 *       sequentially in a single continuous loop, holding iterators open for
 *       seconds at a time (largest race window).</li>
 *   <li><b>On-demand scan loop</b> — fires {@code scanContainerWithoutGap()}
 *       for every container on every datanode, fire-and-forget, adding
 *       additional in-flight iterators.</li>
 * </ol>
 *
 * <p>A third thread per datanode (<b>RocksDB close loop</b>) continuously
 * collects unique Schema V3 DB paths from that datanode's containers and calls
 * {@code DatanodeStoreCache.getInstance().removeDB(path)} while both scanners
 * have active iterators.  This reproduces the crash in seconds/minutes rather
 * than the days it takes in production.
 *
 * <p>Expected outcome: the JVM crashes with SIGSEGV and produces a new
 * {@code hs_err_pid*.log} in the working directory, matching the pattern of
 * the existing production crash logs.
 *
 * <p>This test never terminates on its own — run it manually and observe.
 */
public class TestRocksDBIteratorCrashRepro {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestRocksDBIteratorCrashRepro.class);

  private static final int DATANODE_COUNT = 5;
  private static final int MIN_SEED_KEY_COUNT = 60;
  private static final int MIN_SEED_CONTAINER_COUNT = 10;
  private static final int KEY_SIZE_MB = 8;
  private static final long PROGRESS_LOG_INTERVAL_MS = 2_000L;
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

  private static MiniOzoneCluster cluster;
  private static OzoneClient client;
  private static OzoneBucket bucket;
  private static volatile boolean stopWorkers = false;
  private static ExecutorService workers;
  private static ECReplicationConfig ecReplication;

  @BeforeAll
  public static void setUp() throws Exception {
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

    cluster = MiniOzoneCluster.newBuilder(conf)
        .setNumDatanodes(DATANODE_COUNT)
        .build();
    cluster.waitForClusterToBeReady();

    client = OzoneClientFactory.getRpcClient(conf);
    ObjectStore store = client.getObjectStore();
    String volumeName = UUID.randomUUID().toString();
    String bucketName = volumeName;
    store.createVolume(volumeName);
    OzoneVolume volume = store.getVolume(volumeName);

    ecReplication = new ECReplicationConfig(3, 2,
        ECReplicationConfig.EcCodec.RS, (int) OzoneConsts.MB);
    BucketArgs bucketArgs = BucketArgs.newBuilder()
        .setDefaultReplicationConfig(new DefaultReplicationConfig(ecReplication))
        .build();
    volume.createBucket(bucketName, bucketArgs);
    bucket = volume.getBucket(bucketName);

    seedKeysAndContainers();
    closeSeededContainers();
    logProgress("seed complete");
  }

  @AfterAll
  public static void tearDown() throws IOException {
    stopWorkers = true;
    if (workers != null) {
      workers.shutdownNow();
    }
    if (client != null) {
      client.close();
    }
    if (cluster != null) {
      cluster.shutdown();
    }
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
          keyName, payload.length, ecReplication, new HashMap<>())) {
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
  private static void runRocksDBCloseLoop(HddsDatanodeService dn) {
    OzoneContainer ozoneContainer =
        dn.getDatanodeStateMachine().getContainer();
    DatanodeStoreCache storeCache = DatanodeStoreCache.getInstance();

    try {
      // Give both scanners time to open iterators before the first close.
      Thread.sleep(CLOSE_THREAD_STARTUP_DELAY_MS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    while (!stopWorkers) {
      try {
        // Collect the unique per-volume DB paths for this datanode.
        // Multiple containers on the same volume share one path (Schema V3).
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

        for (String path : dbPaths) {
          // removeDB calls db.getStore().stop() — closes the native RocksDB
          // DB* immediately with no reference counting and no regard for
          // live iterators.  Any scanner thread mid-iteration will SIGSEGV
          // on its next RocksIterator.next0() call.
          storeCache.removeDB(path);
        }

        closeRounds.incrementAndGet();

        // Brief pause to let scanners call getDB() and re-open the DB,
        // establishing fresh iterators before the next close round.
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

  private static void logProgress(String prefix) {
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

  @Test
  public void runInfiniteRocksDBCloseDuringScan() throws Exception {
    List<HddsDatanodeService> datanodes = cluster.getHddsDatanodes();
    // 2 threads per datanode: one on-demand scan loop + one RocksDB close loop.
    // The background scanner (dataScanInterval=0) runs for free in its own
    // per-volume threads started by OzoneContainer during setUp().
    workers = Executors.newFixedThreadPool(2 * datanodes.size(),
        new NamedDaemonFactory("crash-repro"));

    for (HddsDatanodeService dn : datanodes) {
      workers.submit(() -> runOnDemandScanLoop(dn));
      workers.submit(() -> runRocksDBCloseLoop(dn));
    }

    while (true) {
      Thread.sleep(PROGRESS_LOG_INTERVAL_MS);
      logProgress("running");
    }
  }

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
