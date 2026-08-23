# Metrics Classes

One-liner per `*Metrics` class, grouped by component. Metrics are the fastest way to reverse-engineer what state a service exposes and where it counts events; read the `*Metrics` class alongside its owning service.

Total metrics classes: **93**.

## Client

| fqcn | path | one-liner |
|---|---|---|
| `org.apache.hadoop.hdds.scm.ContainerClientMetrics` | `hadoop-hdds/client/src/main/java/org/apache/hadoop/hdds/scm/ContainerClientMetrics.java` | Container client metrics that describe how data writes are distributed to pipelines. |
| `org.apache.hadoop.hdds.scm.XceiverClientMetrics` | `hadoop-hdds/client/src/main/java/org/apache/hadoop/hdds/scm/XceiverClientMetrics.java` | The client metrics for the Storage Container protocol. |

## OM

| fqcn | path | one-liner |
|---|---|---|
| `org.apache.hadoop.ozone.om.BucketUtilizationMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/BucketUtilizationMetrics.java` | A class for collecting and reporting bucket utilization metrics. |
| `org.apache.hadoop.ozone.om.DeletingServiceMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/DeletingServiceMetrics.java` | Class contains metrics related to the OM Deletion services. |
| `org.apache.hadoop.ozone.om.OMMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OMMetrics.java` | This class is for maintaining Ozone Manager statistics. |
| `org.apache.hadoop.ozone.om.OMPerformanceMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OMPerformanceMetrics.java` | Including OM performance related metrics. |
| `org.apache.hadoop.ozone.om.OmMetadataReaderMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OmMetadataReaderMetrics.java` | Interface OM Metadata Reading metrics classes. |
| `org.apache.hadoop.ozone.om.OmMetricsInfo` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OmMetricsInfo.java` | OmMetricsInfo stored in a file, which will be used during OM restart to initialize the metrics. |
| `org.apache.hadoop.ozone.om.OmSnapshotInternalMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OmSnapshotInternalMetrics.java` | This class contains internal Snapshot Operation metrics. |
| `org.apache.hadoop.ozone.om.OmSnapshotMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OmSnapshotMetrics.java` | This class is for maintaining Snapshot Manager statistics. |
| `org.apache.hadoop.ozone.om.ha.OMHAMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/ha/OMHAMetrics.java` | Class to maintain metrics and info related to OM HA. |
| `org.apache.hadoop.ozone.om.ha.OMPeriodicMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/ha/OMPeriodicMetrics.java` | Generic framework for metrics that need to get updated on a specified interval. |
| `org.apache.hadoop.ozone.om.lock.OMLockMetrics` | `hadoop-ozone/interface-storage/src/main/java/org/apache/hadoop/ozone/om/lock/OMLockMetrics.java` | This class is for maintaining the various Ozone Manager Lock Metrics. |
| `org.apache.hadoop.ozone.om.ratis.OzoneManagerDoubleBufferMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/ratis/OzoneManagerDoubleBufferMetrics.java` | Class which maintains metrics related to OzoneManager DoubleBuffer. |
| `org.apache.hadoop.ozone.om.service.KeyLifecycleServiceMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/service/KeyLifecycleServiceMetrics.java` | Class contains metrics related to the OM KeyLifeCycle services. |
| `org.apache.hadoop.ozone.om.snapshot.OMSnapshotDirectoryMetrics` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/snapshot/OMSnapshotDirectoryMetrics.java` | Metrics for tracking db. |

## SCM

| fqcn | path | one-liner |
|---|---|---|
| `org.apache.hadoop.hdds.scm.FetchMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/FetchMetrics.java` | Class used to fetch metrics from MBeanServer. |
| `org.apache.hadoop.hdds.scm.block.ScmBlockDeletingServiceMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/block/ScmBlockDeletingServiceMetrics.java` | Metrics related to Block Deleting Service running in SCM. |
| `org.apache.hadoop.hdds.scm.container.balancer.ContainerBalancerMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/balancer/ContainerBalancerMetrics.java` | Metrics related to Container Balancer running in SCM. |
| `org.apache.hadoop.hdds.scm.container.metrics.SCMContainerManagerMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/metrics/SCMContainerManagerMetrics.java` | Class contains metrics related to ContainerManager. |
| `org.apache.hadoop.hdds.scm.container.placement.algorithms.SCMContainerPlacementMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/placement/algorithms/SCMContainerPlacementMetrics.java` | This class is for maintaining Topology aware container placement statistics. |
| `org.apache.hadoop.hdds.scm.container.placement.metrics.SCMMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/placement/metrics/SCMMetrics.java` | This class is for maintaining StorageContainerManager statistics. |
| `org.apache.hadoop.hdds.scm.container.placement.metrics.SCMPerformanceMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/placement/metrics/SCMPerformanceMetrics.java` | Including SCM performance related metrics. |
| `org.apache.hadoop.hdds.scm.container.replication.ReplicationManagerMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/replication/ReplicationManagerMetrics.java` | Class contains metrics related to ReplicationManager. |
| `org.apache.hadoop.hdds.scm.ha.SCMHAMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/ha/SCMHAMetrics.java` | SCM HA metrics. |
| `org.apache.hadoop.hdds.scm.node.NodeDecommissionMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/node/NodeDecommissionMetrics.java` | Class contains metrics related to the NodeDecommissionManager. |
| `org.apache.hadoop.hdds.scm.node.SCMNodeMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/node/SCMNodeMetrics.java` | This class maintains Node related metrics. |
| `org.apache.hadoop.hdds.scm.pipeline.SCMPipelineMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/pipeline/SCMPipelineMetrics.java` | This class maintains Pipeline related metrics. |
| `org.apache.hadoop.hdds.scm.safemode.SafeModeMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/safemode/SafeModeMetrics.java` | This class is used for maintaining SafeMode metric information, which can be used for monitoring during SCM startup w... |
| `org.apache.hadoop.hdds.scm.security.RootCARotationMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/security/RootCARotationMetrics.java` | Metrics related to Root CA rotation in SCM. |
| `org.apache.hadoop.hdds.scm.server.SCMContainerMetrics` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/server/SCMContainerMetrics.java` | Metrics source to report number of containers in different states. |

## DN

| fqcn | path | one-liner |
|---|---|---|
| `org.apache.hadoop.ozone.container.checksum.ContainerMerkleTreeMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/checksum/ContainerMerkleTreeMetrics.java` | Class to collect metrics related to container merkle tree. |
| `org.apache.hadoop.ozone.container.common.helpers.BlockDeletingServiceMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/helpers/BlockDeletingServiceMetrics.java` | Metrics related to Block Deleting Service running on Datanode. |
| `org.apache.hadoop.ozone.container.common.helpers.CommandHandlerMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/helpers/CommandHandlerMetrics.java` | This class collects and exposes metrics for CommandHandlerMetrics. |
| `org.apache.hadoop.ozone.container.common.helpers.ContainerMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/helpers/ContainerMetrics.java` | This class is for maintaining  the various Storage Container DataNode statistics and publishing them through the metr... |
| `org.apache.hadoop.ozone.container.common.statemachine.DatanodeQueueMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/statemachine/DatanodeQueueMetrics.java` | Class contains metrics related to Datanode queues. |
| `org.apache.hadoop.ozone.container.common.transport.server.ratis.CSMMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/transport/server/ratis/CSMMetrics.java` | This class is for maintaining Container State Machine statistics. |
| `org.apache.hadoop.ozone.container.common.utils.ContainerCacheMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/utils/ContainerCacheMetrics.java` | Metrics for the usage of ContainerDB. |
| `org.apache.hadoop.ozone.container.common.volume.BackgroundVolumeScannerMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/volume/BackgroundVolumeScannerMetrics.java` | This class captures the Background Storage Volume Scanner Metrics. |
| `org.apache.hadoop.ozone.container.common.volume.VolumeHealthMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/volume/VolumeHealthMetrics.java` | This class is used to track Volume Health metrics for all volumes on a datanode. |
| `org.apache.hadoop.ozone.container.common.volume.VolumeInfoMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/volume/VolumeInfoMetrics.java` | This class is used to track Volume Info stats for each HDDS Volume. |
| `org.apache.hadoop.ozone.container.diskbalancer.DiskBalancerServiceMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/diskbalancer/DiskBalancerServiceMetrics.java` | Metrics related to DiskBalancer Service running on Datanode. |
| `org.apache.hadoop.ozone.container.ec.reconstruction.ECReconstructionMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/ec/reconstruction/ECReconstructionMetrics.java` | Metrics class for EC Reconstruction. |
| `org.apache.hadoop.ozone.container.ozoneimpl.AbstractContainerScannerMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/ozoneimpl/AbstractContainerScannerMetrics.java` | Base class for container scanner metrics. |
| `org.apache.hadoop.ozone.container.ozoneimpl.ContainerDataScannerMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/ozoneimpl/ContainerDataScannerMetrics.java` | This class captures the container data scanner metrics on the data-node. |
| `org.apache.hadoop.ozone.container.ozoneimpl.ContainerMetadataScannerMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/ozoneimpl/ContainerMetadataScannerMetrics.java` | This class captures the container meta-data scanner metrics on the data-node. |
| `org.apache.hadoop.ozone.container.ozoneimpl.OnDemandScannerMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/ozoneimpl/OnDemandScannerMetrics.java` | This class captures the on-demand container data scanner metrics. |
| `org.apache.hadoop.ozone.container.replication.ReplicationSupervisorMetrics` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/replication/ReplicationSupervisorMetrics.java` | Metrics source to report number of replication tasks. |

## Ratis-integration

| fqcn | path | one-liner |
|---|---|---|
| `org.apache.ratis.metrics.dropwizard3.RatisMetricsUtils` | `hadoop-hdds/framework/src/main/java/org/apache/ratis/metrics/dropwizard3/RatisMetricsUtils.java` | Utilities for ratis metrics dropwizard3. |

## RocksDB

| fqcn | path | one-liner |
|---|---|---|
| `org.apache.hadoop.hdds.utils.db.managed.ManagedRocksObjectMetrics` | `hadoop-hdds/managed-rocksdb/src/main/java/org/apache/hadoop/hdds/utils/db/managed/ManagedRocksObjectMetrics.java` | Metrics about managed RockObjects. |
| `org.apache.ozone.rocksdiff.SSTFilePruningMetrics` | `hadoop-hdds/rocksdb-checkpoint-differ/src/main/java/org/apache/ozone/rocksdiff/SSTFilePruningMetrics.java` | Class contains metrics for monitoring SST file pruning operations in RocksDBCheckpointDiffer. |

## HddsCommon

| fqcn | path | one-liner |
|---|---|---|
| `org.apache.hadoop.hdds.server.events.EventExecutorMetrics` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/server/events/EventExecutorMetrics.java` | Metrics source for EventExecutor implementations. |
| `org.apache.hadoop.hdds.server.events.EventWatcherMetrics` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/server/events/EventWatcherMetrics.java` | Metrics for any event watcher. |
| `org.apache.hadoop.hdds.server.http.HttpServer2Metrics` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/server/http/HttpServer2Metrics.java` | Metrics related to HttpServer threadPool. |
| `org.apache.hadoop.hdds.server.http.PrometheusMetricsSink` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/server/http/PrometheusMetricsSink.java` | Metrics sink for prometheus exporter. |
| `org.apache.hadoop.hdds.utils.CpuMetrics` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/CpuMetrics.java` | Expose the next JMX metrics. |
| `org.apache.hadoop.hdds.utils.DBCheckpointMetrics` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/DBCheckpointMetrics.java` | This interface is for maintaining DB checkpoint statistics. |
| `org.apache.hadoop.hdds.utils.NettyMetrics` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/NettyMetrics.java` | This class emits Netty metrics. |
| `org.apache.hadoop.hdds.utils.PrometheusMetricsSinkUtil` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/PrometheusMetricsSinkUtil.java` | Util class for {@link org. |
| `org.apache.hadoop.hdds.utils.ProtocolMessageMetrics` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/ProtocolMessageMetrics.java` | Metrics to count all the subtypes of a specific message. |
| `org.apache.hadoop.hdds.utils.RocksDBStoreMetrics` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/RocksDBStoreMetrics.java` | All Rocksdb metrics. |
| `org.apache.hadoop.hdds.utils.TableCacheMetrics` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/TableCacheMetrics.java` | This class emits table level cache metrics. |
| `org.apache.hadoop.hdds.utils.UgiMetricsUtil` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/UgiMetricsUtil.java` | Util class for UGI metrics. |
| `org.apache.hadoop.hdds.utils.db.RDBMetrics` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/utils/db/RDBMetrics.java` | Class to hold RocksDB metrics. |
| `org.apache.hadoop.ipc_.metrics.RpcDetailedMetrics` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/ipc_/metrics/RpcDetailedMetrics.java` | Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. |
| `org.apache.hadoop.ipc_.metrics.RpcMetrics` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/ipc_/metrics/RpcMetrics.java` | Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. |
| `org.apache.hadoop.ozone.grpc.metrics.GrpcMetrics` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/grpc/metrics/GrpcMetrics.java` | Class which maintains metrics related to using GRPC. |
| `org.apache.hadoop.ozone.grpc.metrics.GrpcMetricsServerRequestInterceptor` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/grpc/metrics/GrpcMetricsServerRequestInterceptor.java` | Interceptor to gather metrics based on grpc server request. |
| `org.apache.hadoop.ozone.grpc.metrics.GrpcMetricsServerResponseInterceptor` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/grpc/metrics/GrpcMetricsServerResponseInterceptor.java` | Interceptor to gather metrics based on grpc server response. |
| `org.apache.hadoop.ozone.grpc.metrics.GrpcMetricsServerTransportFilter` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/grpc/metrics/GrpcMetricsServerTransportFilter.java` | Transport filter class for tracking active client connections. |
| `org.apache.hadoop.ozone.util.CacheMetrics` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/util/CacheMetrics.java` | Reusable component that emits cache metrics for a particular cache. |
| `org.apache.hadoop.ozone.util.PerformanceMetrics` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/util/PerformanceMetrics.java` | The {@code PerformanceMetrics} class encapsulates a collection of related metrics including a MutableStat, MutableQua... |
| `org.apache.hadoop.ozone.util.PerformanceMetricsInitializer` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/util/PerformanceMetricsInitializer.java` | Utility class for initializing PerformanceMetrics in a MetricsSource. |

## Recon

| fqcn | path | one-liner |
|---|---|---|
| `org.apache.hadoop.ozone.recon.MetricsServiceProviderFactory` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/MetricsServiceProviderFactory.java` | Factory class that is used to get the instance of configured Metrics Service Provider. |
| `org.apache.hadoop.ozone.recon.api.DataNodeMetricsService` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/DataNodeMetricsService.java` | Service for collecting and managing DataNode pending deletion metrics. |
| `org.apache.hadoop.ozone.recon.api.MetricsProxyEndpoint` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/MetricsProxyEndpoint.java` | Endpoint to fetch metrics data from Prometheus HTTP endpoint. |
| `org.apache.hadoop.ozone.recon.api.ReconGlobalMetricsService` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/ReconGlobalMetricsService.java` | Service for getting global storage metric values. |
| `org.apache.hadoop.ozone.recon.api.types.DataNodeMetricsCompleteResponse` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/types/DataNodeMetricsCompleteResponse.java` | Response returned when metrics collection is complete. |
| `org.apache.hadoop.ozone.recon.api.types.DataNodeMetricsProgressResponse` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/types/DataNodeMetricsProgressResponse.java` | Response returned while metrics collection is still in progress. |
| `org.apache.hadoop.ozone.recon.api.types.DatanodeMetrics` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/types/DatanodeMetrics.java` | Class that represents the datanode metrics captured during decommissioning. |
| `org.apache.hadoop.ozone.recon.api.types.DatanodePendingDeletionMetrics` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/types/DatanodePendingDeletionMetrics.java` | Represents pending deletion metrics for a datanode. |
| `org.apache.hadoop.ozone.recon.metrics.ContainerHealthTaskMetrics` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/metrics/ContainerHealthTaskMetrics.java` | Runtime metrics for ContainerHealthTask execution. |
| `org.apache.hadoop.ozone.recon.metrics.OzoneManagerSyncMetrics` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/metrics/OzoneManagerSyncMetrics.java` | Class for tracking metrics related to Ozone manager sync operations. |
| `org.apache.hadoop.ozone.recon.metrics.ReconScmContainerSyncMetrics` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/metrics/ReconScmContainerSyncMetrics.java` | Metrics for Recon SCM container sync execution. |
| `org.apache.hadoop.ozone.recon.metrics.ReconSyncMetrics` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/metrics/ReconSyncMetrics.java` | Metrics for Recon OM synchronization operations. |
| `org.apache.hadoop.ozone.recon.metrics.ReconTaskControllerMetrics` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/metrics/ReconTaskControllerMetrics.java` | Metrics for Recon Task Controller operations. |
| `org.apache.hadoop.ozone.recon.metrics.ReconTaskMetrics` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/metrics/ReconTaskMetrics.java` | Per-task metrics for Recon task delta processing and reprocess operations. |
| `org.apache.hadoop.ozone.recon.metrics.ReconTaskStatusMetrics` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/metrics/ReconTaskStatusMetrics.java` | Ship ReconTaskStatus table on persistent DB as a metrics. |
| `org.apache.hadoop.ozone.recon.spi.MetricsServiceProvider` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/spi/MetricsServiceProvider.java` | Interface to access Ozone metrics. |
| `org.apache.hadoop.ozone.recon.tasks.DataNodeMetricsCollectionTask` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/tasks/DataNodeMetricsCollectionTask.java` | Task for collecting pending deletion metrics from a DataNode using JMX. |

## Interfaces

| fqcn | path | one-liner |
|---|---|---|
| `org.apache.hadoop.ozone.s3.metrics.S3GatewayMetrics` | `hadoop-ozone/s3gateway/src/main/java/org/apache/hadoop/ozone/s3/metrics/S3GatewayMetrics.java` | This class maintains S3 Gateway related metrics. |
| `org.apache.ozone.fs.http.server.metrics.HttpFSServerMetrics` | `hadoop-ozone/httpfsgateway/src/main/java/org/apache/ozone/fs/http/server/metrics/HttpFSServerMetrics.java` | This class is for maintaining  the various HttpFSServer statistics and publishing them through the metrics interfaces. |

## Bench & Insight

| fqcn | path | one-liner |
|---|---|---|
| `org.apache.hadoop.ozone.insight.MetricsSubCommand` | `hadoop-ozone/insight/src/main/java/org/apache/hadoop/ozone/insight/MetricsSubCommand.java` | Command line interface to show metrics for a specific component. |
