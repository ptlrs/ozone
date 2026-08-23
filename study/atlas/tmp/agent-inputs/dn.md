# Enrichment scope for component: DN

## Directory to edit

`/Users/rpatel/Github/ozone/study/atlas/components/dn/`

## Feature files to enrich (one at a time)

### `container-checksum.md` — 6 classes (1 anchors)

- feature key in atlas.json: `component=DN, feature=container-checksum`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.checksum.ContainerChecksumTreeManager` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/checksum/ContainerChecksumTreeManager.java, 250~)

### `container-interfaces.md` — 14 classes (0 anchors)

- feature key in atlas.json: `component=DN, feature=container-interfaces`

### `container-replication-dn.md` — 20 classes (2 anchors)

- feature key in atlas.json: `component=DN, feature=container-replication-dn`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.replication.ReplicationSupervisor` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/replication/ReplicationSupervisor.java, 525~)
  - `org.apache.hadoop.ozone.container.replication.ReplicationServer` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/replication/ReplicationServer.java, 250~)

### `disk-balancer.md` — 12 classes (2 anchors)

- feature key in atlas.json: `component=DN, feature=disk-balancer`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.diskbalancer.DiskBalancerService` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/diskbalancer/DiskBalancerService.java, 600~)
  - `org.apache.hadoop.ozone.container.diskbalancer.DiskBalancerConfiguration` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/diskbalancer/DiskBalancerConfiguration.java, 275~)

### `dn-audit.md` — 1 classes (0 anchors)

- feature key in atlas.json: `component=DN, feature=dn-audit`

### `dn-freon.md` — 1 classes (0 anchors)

- feature key in atlas.json: `component=DN, feature=dn-freon`

### `dn-helpers.md` — 8 classes (2 anchors)

- feature key in atlas.json: `component=DN, feature=dn-helpers`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.common.helpers.ContainerUtils` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/helpers/ContainerUtils.java, 275~)
  - `org.apache.hadoop.ozone.container.common.helpers.DatanodeIdYaml` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/helpers/DatanodeIdYaml.java, 200~)

### `dn-protocol.md` — 8 classes (0 anchors)

- feature key in atlas.json: `component=DN, feature=dn-protocol`

### `dn-reports.md` — 8 classes (0 anchors)

- feature key in atlas.json: `component=DN, feature=dn-reports`

### `dn-rocksdb.md` — 21 classes (2 anchors)

- feature key in atlas.json: `component=DN, feature=dn-rocksdb`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.metadata.AbstractDatanodeStore` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/metadata/AbstractDatanodeStore.java, 225~)
  - `org.apache.hadoop.ozone.container.metadata.DatanodeStoreSchemaThreeImpl` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/metadata/DatanodeStoreSchemaThreeImpl.java, 200~)

### `dn-scm-client.md` — 1 classes (0 anchors)

- feature key in atlas.json: `component=DN, feature=dn-scm-client`

### `dn-scm-commands.md` — 17 classes (0 anchors)

- feature key in atlas.json: `component=DN, feature=dn-scm-commands`

### `dn-service.md` — 42 classes (10 anchors)

- feature key in atlas.json: `component=DN, feature=dn-service`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.common.impl.ContainerData` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/impl/ContainerData.java, 400~)
  - `org.apache.hadoop.ozone.container.common.impl.HddsDispatcher` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/impl/HddsDispatcher.java, 800~)
  - `org.apache.hadoop.ozone.HddsDatanodeService` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/HddsDatanodeService.java, 575~)
  - `org.apache.hadoop.ozone.container.ozoneimpl.OzoneContainer` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/ozoneimpl/OzoneContainer.java, 525~)
  - `org.apache.hadoop.ozone.container.common.impl.ContainerSet` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/impl/ContainerSet.java, 375~)
  - `org.apache.hadoop.ozone.container.common.states.endpoint.HeartbeatEndpointTask` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/states/endpoint/HeartbeatEndpointTask.java, 350~)
  - `org.apache.hadoop.ozone.container.common.impl.StorageLocationReport` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/impl/StorageLocationReport.java, 300~)
  - `org.apache.hadoop.ozone.container.common.impl.BlockDeletingService` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/impl/BlockDeletingService.java, 250~)
  - `org.apache.hadoop.ozone.container.ozoneimpl.ContainerReader` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/ozoneimpl/ContainerReader.java, 250~)
  - `org.apache.hadoop.ozone.container.common.impl.ContainerDataYaml` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/impl/ContainerDataYaml.java, 200~)

### `dn-statemachine.md` — 21 classes (5 anchors)

- feature key in atlas.json: `component=DN, feature=dn-statemachine`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.common.statemachine.DatanodeConfiguration` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/statemachine/DatanodeConfiguration.java, 1000~)
  - `org.apache.hadoop.ozone.container.common.statemachine.StateContext` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/statemachine/StateContext.java, 650~)
  - `org.apache.hadoop.ozone.container.common.statemachine.commandhandler.DeleteBlocksCommandHandler` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/statemachine/commandhandler/DeleteBlocksCommandHandler.java, 575~)
  - `org.apache.hadoop.ozone.container.common.statemachine.DatanodeStateMachine` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/statemachine/DatanodeStateMachine.java, 475~)
  - `org.apache.hadoop.ozone.container.common.statemachine.SCMConnectionManager` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/statemachine/SCMConnectionManager.java, 200~)

### `dn-streaming.md` — 9 classes (0 anchors)

- feature key in atlas.json: `component=DN, feature=dn-streaming`

### `dn-upgrade.md` — 7 classes (0 anchors)

- feature key in atlas.json: `component=DN, feature=dn-upgrade`

### `dn-utils.md` — 11 classes (0 anchors)

- feature key in atlas.json: `component=DN, feature=dn-utils`

### `erasure-coding.md` — 41 classes (3 anchors)

- feature key in atlas.json: `component=DN, feature=erasure-coding`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.ec.reconstruction.ECReconstructionCoordinator` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/ec/reconstruction/ECReconstructionCoordinator.java, 400~)
  - `org.apache.ozone.erasurecode.rawcoder.util.GaloisField` (hadoop-hdds/erasurecode/src/main/java/org/apache/ozone/erasurecode/rawcoder/util/GaloisField.java, 350~)
  - `org.apache.ozone.erasurecode.rawcoder.util.GF256` (hadoop-hdds/erasurecode/src/main/java/org/apache/ozone/erasurecode/rawcoder/util/GF256.java, 250~)

### `grpc-server-dn.md` — 5 classes (2 anchors)

- feature key in atlas.json: `component=DN, feature=grpc-server-dn`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.common.transport.server.Receiver` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/transport/server/Receiver.java, 250~)
  - `org.apache.hadoop.ozone.container.common.transport.server.XceiverServerDomainSocket` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/transport/server/XceiverServerDomainSocket.java, 225~)

### `hdds-volume.md` — 24 classes (3 anchors)

- feature key in atlas.json: `component=DN, feature=hdds-volume`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.common.volume.HddsVolume` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/volume/HddsVolume.java, 450~)
  - `org.apache.hadoop.ozone.container.common.volume.StorageVolumeChecker` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/volume/StorageVolumeChecker.java, 300~)
  - `org.apache.hadoop.ozone.container.common.volume.MutableVolumeSet` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/volume/MutableVolumeSet.java, 275~)

### `kv-container.md` — 13 classes (6 anchors)

- feature key in atlas.json: `component=DN, feature=kv-container`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.keyvalue.KeyValueHandler` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/KeyValueHandler.java, 1825~)
  - `org.apache.hadoop.ozone.container.keyvalue.KeyValueContainer` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/KeyValueContainer.java, 650~)
  - `org.apache.hadoop.ozone.container.keyvalue.KeyValueContainerCheck` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/KeyValueContainerCheck.java, 325~)
  - `org.apache.hadoop.ozone.container.keyvalue.helpers.KeyValueContainerUtil` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/helpers/KeyValueContainerUtil.java, 325~)
  - `org.apache.hadoop.ozone.container.keyvalue.helpers.ChunkUtils` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/helpers/ChunkUtils.java, 325~)
  - `org.apache.hadoop.ozone.container.keyvalue.KeyValueContainerData` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/KeyValueContainerData.java, 250~)

### `kv-container-impl.md` — 10 classes (3 anchors)

- feature key in atlas.json: `component=DN, feature=kv-container-impl`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.keyvalue.impl.FilePerBlockStrategy` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/impl/FilePerBlockStrategy.java, 275~)
  - `org.apache.hadoop.ozone.container.keyvalue.impl.BlockManagerImpl` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/impl/BlockManagerImpl.java, 275~)
  - `org.apache.hadoop.ozone.container.keyvalue.impl.FilePerChunkStrategy` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/impl/FilePerChunkStrategy.java, 225~)

### `ratis-statemachine-dn.md` — 8 classes (3 anchors)

- feature key in atlas.json: `component=DN, feature=ratis-statemachine-dn`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.container.common.transport.server.ratis.ContainerStateMachine` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/transport/server/ratis/ContainerStateMachine.java, 1000~)
  - `org.apache.hadoop.ozone.container.common.transport.server.ratis.XceiverServerRatis` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/transport/server/ratis/XceiverServerRatis.java, 675~)
  - `org.apache.hadoop.ozone.container.keyvalue.statemachine.background.BlockDeletingTask` (hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/statemachine/background/BlockDeletingTask.java, 400~)
