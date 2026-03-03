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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.FileUtils;
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
import org.apache.hadoop.ozone.client.io.OzoneInputStream;
import org.apache.hadoop.ozone.client.io.OzoneOutputStream;
import org.apache.hadoop.ozone.container.common.interfaces.Container;
import org.apache.hadoop.ozone.container.common.volume.HddsVolume;
import org.apache.hadoop.ozone.container.common.volume.VolumeChoosingPolicyFactory;
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
 * Manual, non-terminating integration repro harness for scanner + import/export
 * concurrency in a 10-node MiniOzone cluster.
 */
public class TestInfiniteScannerImportExportRepro {

  private static final Logger LOG =
      LoggerFactory.getLogger(TestInfiniteScannerImportExportRepro.class);

  private static final int DATANODE_COUNT = 10;
  private static final int MIN_SEED_KEY_COUNT = 96;
  private static final int MIN_SEED_CONTAINER_COUNT = 12;
  private static final int KEY_SIZE_MB = 8;
  private static final int READ_BUFFER_SIZE = 8 * 1024;
  private static final long PROGRESS_LOG_INTERVAL_MS = 2_000L;
  private static final List<String> keyNames = new CopyOnWriteArrayList<>();
  private static final Set<Long> containerIds = ConcurrentHashMap.newKeySet();
  private static final AtomicLong importExportIterations = new AtomicLong();
  private static final AtomicLong importExportFailures = new AtomicLong();
  private static final AtomicLong readIterations = new AtomicLong();
  private static final AtomicLong readFailures = new AtomicLong();
  private static final AtomicLong onDemandScanIterations = new AtomicLong();
  private static final AtomicLong onDemandScanFailures = new AtomicLong();
  private static MiniOzoneCluster cluster;
  private static OzoneClient client;
  private static OzoneBucket bucket;
  private static Path tempDir;
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

    tempDir = Files.createTempDirectory("ozone-infinite-repro-");
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
    if (tempDir != null) {
      FileUtils.deleteDirectory(tempDir.toFile());
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

  private static void ensureSufficientContainerLoad() throws Exception {
    if (containerIds.size() >= MIN_SEED_CONTAINER_COUNT) {
      return;
    }
    LOG.info("Container count dropped below target. Seeding more keys.");
    seedKeysAndContainers();
    closeSeededContainers();
  }

  private static void closeSeededContainers() throws Exception {
    for (Long containerId : containerIds) {
      try {
        cluster.getStorageContainerLocationClient().closeContainer(containerId);
      } catch (Exception ignored) {
        // Containers may already be closed. Ignore and continue.
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

  private static void runImportExportLoop() {
    while (!stopWorkers) {
      try {
        Long containerId = pickRandomContainerId();
        if (containerId == null) {
          continue;
        }
        executeImportExportCycle(containerId);
        importExportIterations.incrementAndGet();
      } catch (Exception ex) {
        long failures = importExportFailures.incrementAndGet();
        if (failures % 100 == 0) {
          LOG.warn("Import/export failures so far: {}", failures, ex);
        }
      }
    }
  }

  private static void executeImportExportCycle(long containerId)
      throws Exception {
    List<HddsDatanodeService> holders = getReplicaHolders(containerId);
    if (holders.isEmpty()) {
      return;
    }

    HddsDatanodeService sourceDn =
        holders.get((int) (System.nanoTime() % holders.size()));
    HddsDatanodeService targetDn = chooseNonHolder(holders);
    if (targetDn == null) {
      return;
    }

    Path tarPath = Files.createTempFile(tempDir,
        "container-" + containerId + "-", ".tar");
    try {
      OzoneContainer sourceContainer =
          sourceDn.getDatanodeStateMachine().getContainer();
      OnDemandContainerReplicationSource source =
          new OnDemandContainerReplicationSource(sourceContainer.getController());
      source.prepare(containerId);
      try (OutputStream out = Files.newOutputStream(tarPath)) {
        source.copyData(containerId, out, NO_COMPRESSION);
      }

      OzoneContainer targetContainer =
          targetDn.getDatanodeStateMachine().getContainer();
      ContainerImporter importer = new ContainerImporter(
          targetDn.getConf(),
          targetContainer.getContainerSet(),
          targetContainer.getController(),
          targetContainer.getVolumeSet(),
          VolumeChoosingPolicyFactory.getPolicy(targetDn.getConf()));
      HddsVolume targetVolume =
          importer.chooseNextVolume(importer.getDefaultReplicationSpace());
      importer.importContainer(containerId, tarPath, targetVolume,
          NO_COMPRESSION);

      // Remove imported replica so the same container can be churned repeatedly.
      targetContainer.getController().deleteContainer(containerId, true);
    } finally {
      Files.deleteIfExists(tarPath);
    }
  }

  private static List<HddsDatanodeService> getReplicaHolders(long containerId) {
    List<HddsDatanodeService> holders = new ArrayList<>();
    for (HddsDatanodeService dn : cluster.getHddsDatanodes()) {
      OzoneContainer container = dn.getDatanodeStateMachine().getContainer();
      if (container.getContainerSet().getContainer(containerId) != null) {
        holders.add(dn);
      }
    }
    return holders;
  }

  private static HddsDatanodeService chooseNonHolder(
      List<HddsDatanodeService> holders) {
    Map<String, HddsDatanodeService> holderByUuid = new HashMap<>();
    holders.forEach(dn -> holderByUuid.put(
        dn.getDatanodeDetails().getUuidString(), dn));
    List<HddsDatanodeService> candidates = new ArrayList<>();
    for (HddsDatanodeService dn : cluster.getHddsDatanodes()) {
      if (!holderByUuid.containsKey(dn.getDatanodeDetails().getUuidString())) {
        candidates.add(dn);
      }
    }
    if (candidates.isEmpty()) {
      return null;
    }
    return candidates.get((int) (System.nanoTime() % candidates.size()));
  }

  private static void runReadLoop() {
    byte[] buffer = new byte[READ_BUFFER_SIZE];
    while (!stopWorkers) {
      String keyName = pickRandomKeyName();
      if (keyName == null) {
        continue;
      }
      try (OzoneInputStream in = bucket.readKey(keyName)) {
        while (in.read(buffer, 0, buffer.length) >= 0) {
          // Continue reading to EOF.
        }
        readIterations.incrementAndGet();
      } catch (Exception ex) {
        long failures = readFailures.incrementAndGet();
        if (failures % 100 == 0) {
          LOG.warn("Read failures so far: {}", failures, ex);
        }
      }
    }
  }

  private static void runOnDemandScanLoop() {
    while (!stopWorkers) {
      try {
        Long containerId = pickRandomContainerId();
        if (containerId == null) {
          continue;
        }
        List<HddsDatanodeService> holders = getReplicaHolders(containerId);
        if (holders.isEmpty()) {
          continue;
        }
        HddsDatanodeService holder =
            holders.get((int) (System.nanoTime() % holders.size()));
        OzoneContainer ozoneContainer =
            holder.getDatanodeStateMachine().getContainer();
        Container<?> container = ozoneContainer.getContainerSet()
            .getContainer(containerId);
        OnDemandContainerScanner scanner = ozoneContainer.getOnDemandScanner();
        if (container != null && scanner != null) {
          scanner.scanContainerWithoutGap(container, "manual-repro-loop");
        }
        onDemandScanIterations.incrementAndGet();
      } catch (Exception ex) {
        long failures = onDemandScanFailures.incrementAndGet();
        if (failures % 100 == 0) {
          LOG.warn("On-demand scan loop failures so far: {}", failures, ex);
        }
      }
    }
  }

  private static Long pickRandomContainerId() {
    if (containerIds.isEmpty()) {
      return null;
    }
    int targetIndex = (int) (System.nanoTime() % containerIds.size());
    int i = 0;
    for (Long id : containerIds) {
      if (i == targetIndex) {
        return id;
      }
      i++;
    }
    return null;
  }

  private static String pickRandomKeyName() {
    int size = keyNames.size();
    if (size == 0) {
      return null;
    }
    int targetIndex = (int) (System.nanoTime() % size);
    return keyNames.get(targetIndex);
  }

  private static void logProgress(String prefix) {
    LOG.info(
        "[{}] keys={}, containers={}, importExport={}, importExportFailures={}, "
            + "reads={}, readFailures={}, onDemandScans={}, onDemandScanFailures={}",
        prefix,
        keyNames.size(),
        containerIds.size(),
        importExportIterations.get(),
        importExportFailures.get(),
        readIterations.get(),
        readFailures.get(),
        onDemandScanIterations.get(),
        onDemandScanFailures.get());
  }

  @Test
  public void runInfiniteImportExportWhileScannerRuns() throws Exception {
    workers = Executors.newFixedThreadPool(3,
        new NamedDaemonFactory("repro-loop"));
    workers.submit(TestInfiniteScannerImportExportRepro::runImportExportLoop);
    workers.submit(TestInfiniteScannerImportExportRepro::runReadLoop);
    workers.submit(TestInfiniteScannerImportExportRepro::runOnDemandScanLoop);

    while (true) {
      Thread.sleep(PROGRESS_LOG_INTERVAL_MS);
      logProgress("running");
      ensureSufficientContainerLoad();
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
