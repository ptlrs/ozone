# Enrichment scope for component: HddsCommon

## Directory to edit

`/Users/rpatel/Github/ozone/study/atlas/components/hddscommon/`

## Feature files to enrich (one at a time)

### `annotations.md` — 4 classes (0 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=annotations`

### `audit.md` — 7 classes (0 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=audit`

### `audit-common.md` — 1 classes (0 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=audit-common`

### `config-annotations.md` — 16 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=config-annotations`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.conf.ConfigurationReflectionUtil` (hadoop-hdds/config/src/main/java/org/apache/hadoop/hdds/conf/ConfigurationReflectionUtil.java, 225~)

### `config-common.md` — 5 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=config-common`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.conf.OzoneConfiguration` (hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/conf/OzoneConfiguration.java, 350~)

### `config-runtime.md` — 7 classes (0 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=config-runtime`

### `container-common.md` — 21 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=container-common`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.container.balancer.ContainerBalancerConfiguration` (hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/scm/container/balancer/ContainerBalancerConfiguration.java, 375~)

### `framework-freon.md` — 3 classes (0 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=framework-freon`

### `framework-protocol.md` — 27 classes (3 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=framework-protocol`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.protocolPB.ScmBlockLocationProtocolClientSideTranslatorPB` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/scm/protocolPB/ScmBlockLocationProtocolClientSideTranslatorPB.java, 250~)
  - `org.apache.hadoop.hdds.scm.protocolPB.StorageContainerLocationProtocolClientSideTranslatorPB` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/scm/protocolPB/StorageContainerLocationProtocolClientSideTranslatorPB.java, 925~)
  - `org.apache.hadoop.hdds.protocolPB.SCMSecurityProtocolClientSideTranslatorPB` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/protocolPB/SCMSecurityProtocolClientSideTranslatorPB.java, 200~)

### `framework-server.md` — 20 classes (3 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=framework-server`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.server.ServerUtils` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/server/ServerUtils.java, 225~)
  - `org.apache.hadoop.hdds.server.events.EventQueue` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/server/events/EventQueue.java, 225~)
  - `org.apache.hadoop.hdds.server.events.FixedThreadPoolWithAffinityExecutor` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/server/events/FixedThreadPoolWithAffinityExecutor.java, 200~)

### `framework-utils.md` — 31 classes (6 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=framework-utils`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.utils.HddsServerUtil` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/HddsServerUtil.java, 450~)
  - `org.apache.hadoop.hdds.utils.HAUtils` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/HAUtils.java, 275~)
  - `org.apache.hadoop.hdds.utils.DBCheckpointServlet` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/DBCheckpointServlet.java, 275~)
  - `org.apache.hadoop.hdds.utils.LogLevel` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/LogLevel.java, 250~)
  - `org.apache.hadoop.hdds.utils.Archiver` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/Archiver.java, 200~)
  - `org.apache.hadoop.hdds.utils.RocksDBStoreMetrics` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/RocksDBStoreMetrics.java, 200~)

### `fs-utils.md` — 12 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=fs-utils`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.fs.CachingSpaceUsageSource` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/fs/CachingSpaceUsageSource.java, 200~)

### `hadoop-shaded.md` — 54 classes (9 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=hadoop-shaded`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ipc_.ProtobufRpcEngine` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ipc_/ProtobufRpcEngine.java, 300~)
  - `org.apache.hadoop.ipc_.Server` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ipc_/Server.java, 2425~)
  - `org.apache.hadoop.io_.retry.RetryPolicies` (hadoop-hdds/common/src/main/java/org/apache/hadoop/io_/retry/RetryPolicies.java, 250~)
  - `org.apache.hadoop.ipc_.Client` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ipc_/Client.java, 1150~)
  - `org.apache.hadoop.ipc_.DecayRpcScheduler` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ipc_/DecayRpcScheduler.java, 700~)
  - `org.apache.hadoop.security_.SaslRpcClient` (hadoop-hdds/common/src/main/java/org/apache/hadoop/security_/SaslRpcClient.java, 475~)
  - `org.apache.hadoop.io_.retry.RetryInvocationHandler` (hadoop-hdds/common/src/main/java/org/apache/hadoop/io_/retry/RetryInvocationHandler.java, 325~)
  - `org.apache.hadoop.ipc_.FairCallQueue` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ipc_/FairCallQueue.java, 250~)
  - `org.apache.hadoop.ipc_.CallQueueManager` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ipc_/CallQueueManager.java, 250~)

### `hdds-db-utils.md` — 48 classes (5 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=hdds-db-utils`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.utils.db.RocksDatabase` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/db/RocksDatabase.java, 675~)
  - `org.apache.hadoop.hdds.utils.db.TypedTable` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/db/TypedTable.java, 425~)
  - `org.apache.hadoop.hdds.utils.db.RDBStore` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/db/RDBStore.java, 350~)
  - `org.apache.hadoop.hdds.utils.db.RDBBatchOperation` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/db/RDBBatchOperation.java, 300~)
  - `org.apache.hadoop.hdds.utils.db.RDBTable` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/db/RDBTable.java, 250~)

### `hdds-primitives.md` — 28 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=hdds-primitives`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.HddsUtils` (hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/HddsUtils.java, 525~)

### `hdds-utils.md` — 39 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=hdds-utils`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.utils.db.CodecBuffer` (hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/utils/db/CodecBuffer.java, 300~)

### `http-server.md` — 14 classes (3 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=http-server`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.server.http.BaseHttpServer` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/server/http/BaseHttpServer.java, 300~)
  - `org.apache.hadoop.hdds.server.http.HttpServer2` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/server/http/HttpServer2.java, 1175~)
  - `org.apache.hadoop.hdds.server.http.ProfileServlet` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/server/http/ProfileServlet.java, 350~)

### `lease-manager.md` — 8 classes (0 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=lease-manager`

### `metrics-utils.md` — 4 classes (0 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=metrics-utils`

### `network-topology.md` — 12 classes (3 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=network-topology`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.net.NetworkTopologyImpl` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/scm/net/NetworkTopologyImpl.java, 600~)
  - `org.apache.hadoop.hdds.scm.net.InnerNodeImpl` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/scm/net/InnerNodeImpl.java, 425~)
  - `org.apache.hadoop.hdds.scm.net.NodeSchemaLoader` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/scm/net/NodeSchemaLoader.java, 350~)

### `ozone-common-primitives.md` — 48 classes (7 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=ozone-common-primitives`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.common.PureJavaCrc32ByteBuffer` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/common/PureJavaCrc32ByteBuffer.java, 525~)
  - `org.apache.hadoop.ozone.common.PureJavaCrc32CByteBuffer` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/common/PureJavaCrc32CByteBuffer.java, 525~)
  - `org.apache.hadoop.ozone.OzoneConsts` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/OzoneConsts.java, 325~)
  - `org.apache.hadoop.ozone.common.IncrementalChunkBuffer` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/common/IncrementalChunkBuffer.java, 225~)
  - `org.apache.hadoop.ozone.util.ShutdownHookManager` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/util/ShutdownHookManager.java, 200~)
  - `org.apache.hadoop.ozone.common.ChunkBufferImplWithByteBufferList` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/common/ChunkBufferImplWithByteBufferList.java, 200~)
  - `org.apache.hadoop.ozone.common.Checksum` (hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/common/Checksum.java, 200~)

### `pipeline-common.md` — 5 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=pipeline-common`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.pipeline.Pipeline` (hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/scm/pipeline/Pipeline.java, 475~)

### `protocol-common.md` — 4 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=protocol-common`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.protocol.DatanodeDetails` (hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/protocol/DatanodeDetails.java, 650~)

### `ratis-integration.md` — 11 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=ratis-integration`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.ratis.RatisHelper` (hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/ratis/RatisHelper.java, 450~)

### `scm-client-proxy.md` — 10 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=scm-client-proxy`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.proxy.SCMFailoverProxyProviderBase` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/scm/proxy/SCMFailoverProxyProviderBase.java, 325~)

### `scm-common.md` — 17 classes (0 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=scm-common`

### `security-common.md` — 12 classes (0 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=security-common`

### `security-tokens.md` — 3 classes (0 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=security-tokens`

### `storage-common.md` — 2 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=storage-common`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.storage.ContainerProtocolCalls` (hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/scm/storage/ContainerProtocolCalls.java, 700~)

### `tracing-common.md` — 8 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=tracing-common`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.tracing.TracingUtil` (hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/tracing/TracingUtil.java, 375~)

### `upgrade-common.md` — 6 classes (0 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=upgrade-common`

### `upgrade-framework.md` — 9 classes (1 anchors)

- feature key in atlas.json: `component=HddsCommon, feature=upgrade-framework`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.upgrade.BasicUpgradeFinalizer` (hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/upgrade/BasicUpgradeFinalizer.java, 225~)
