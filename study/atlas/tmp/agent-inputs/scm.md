# Enrichment scope for component: SCM

## Directory to edit

`/Users/rpatel/Github/ozone/study/atlas/components/scm/`

## Feature files to enrich (one at a time)

### `block-manager.md` — 11 classes (3 anchors)

- feature key in atlas.json: `component=SCM, feature=block-manager`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.block.DeletedBlockLogImpl` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/block/DeletedBlockLogImpl.java, 350~)
  - `org.apache.hadoop.hdds.scm.block.SCMBlockDeletingService` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/block/SCMBlockDeletingService.java, 225~)
  - `org.apache.hadoop.hdds.scm.block.ScmBlockDeletingServiceMetrics` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/block/ScmBlockDeletingServiceMetrics.java, 300~)

### `container-balancer.md` — 20 classes (5 anchors)

- feature key in atlas.json: `component=SCM, feature=container-balancer`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.container.balancer.ContainerBalancerTask` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/balancer/ContainerBalancerTask.java, 900~)
  - `org.apache.hadoop.hdds.scm.container.balancer.ContainerBalancer` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/balancer/ContainerBalancer.java, 475~)
  - `org.apache.hadoop.hdds.scm.container.balancer.MoveManager` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/balancer/MoveManager.java, 375~)
  - `org.apache.hadoop.hdds.scm.container.balancer.ContainerBalancerSelectionCriteria` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/balancer/ContainerBalancerSelectionCriteria.java, 225~)
  - `org.apache.hadoop.hdds.scm.container.balancer.ContainerBalancerMetrics` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/balancer/ContainerBalancerMetrics.java, 200~)

### `container-manager.md` — 31 classes (5 anchors)

- feature key in atlas.json: `component=SCM, feature=container-manager`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.container.placement.algorithms.SCMContainerPlacementRackAware` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/placement/algorithms/SCMContainerPlacementRackAware.java, 425~)
  - `org.apache.hadoop.hdds.scm.container.AbstractContainerReportHandler` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/AbstractContainerReportHandler.java, 300~)
  - `org.apache.hadoop.hdds.scm.container.ContainerStateManagerImpl` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/ContainerStateManagerImpl.java, 400~)
  - `org.apache.hadoop.hdds.scm.container.placement.algorithms.SCMContainerPlacementRackScatter` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/placement/algorithms/SCMContainerPlacementRackScatter.java, 400~)
  - `org.apache.hadoop.hdds.scm.container.ContainerManagerImpl` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/ContainerManagerImpl.java, 350~)

### `container-reconciliation.md` — 2 classes (0 anchors)

- feature key in atlas.json: `component=SCM, feature=container-reconciliation`

### `container-replication.md` — 48 classes (9 anchors)

- feature key in atlas.json: `component=SCM, feature=container-replication`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.container.replication.ReplicationManager` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/replication/ReplicationManager.java, 1025~)
  - `org.apache.hadoop.hdds.scm.container.replication.ECUnderReplicationHandler` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/replication/ECUnderReplicationHandler.java, 525~)
  - `org.apache.hadoop.hdds.scm.container.replication.RatisContainerReplicaCount` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/replication/RatisContainerReplicaCount.java, 325~)
  - `org.apache.hadoop.hdds.scm.container.replication.ContainerReplicaPendingOps` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/replication/ContainerReplicaPendingOps.java, 325~)
  - `org.apache.hadoop.hdds.scm.container.replication.ECContainerReplicaCount` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/replication/ECContainerReplicaCount.java, 300~)
  - `org.apache.hadoop.hdds.scm.container.replication.RatisUnderReplicationHandler` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/replication/RatisUnderReplicationHandler.java, 300~)
  - `org.apache.hadoop.hdds.scm.container.replication.ReplicationManagerUtil` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/replication/ReplicationManagerUtil.java, 250~)
  - `org.apache.hadoop.hdds.scm.container.replication.ContainerHealthResult` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/replication/ContainerHealthResult.java, 200~)
  - `org.apache.hadoop.hdds.scm.container.replication.ReplicationManagerMetrics` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/container/replication/ReplicationManagerMetrics.java, 400~)

### `node-manager.md` — 34 classes (6 anchors)

- feature key in atlas.json: `component=SCM, feature=node-manager`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.node.SCMNodeManager` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/node/SCMNodeManager.java, 1400~)
  - `org.apache.hadoop.hdds.scm.node.NodeDecommissionManager` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/node/NodeDecommissionManager.java, 475~)
  - `org.apache.hadoop.hdds.scm.node.NodeStateManager` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/node/NodeStateManager.java, 450~)
  - `org.apache.hadoop.hdds.scm.node.DatanodeAdminMonitorImpl` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/node/DatanodeAdminMonitorImpl.java, 375~)
  - `org.apache.hadoop.hdds.scm.node.states.NodeStateMap` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/node/states/NodeStateMap.java, 225~)
  - `org.apache.hadoop.hdds.scm.node.NodeDecommissionMetrics` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/node/NodeDecommissionMetrics.java, 200~)

### `pipeline-choose-policy.md` — 5 classes (0 anchors)

- feature key in atlas.json: `component=SCM, feature=pipeline-choose-policy`

### `pipeline-manager.md` — 28 classes (8 anchors)

- feature key in atlas.json: `component=SCM, feature=pipeline-manager`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.pipeline.PipelineManagerImpl` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/pipeline/PipelineManagerImpl.java, 725~)
  - `org.apache.hadoop.hdds.scm.pipeline.PipelinePlacementPolicy` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/pipeline/PipelinePlacementPolicy.java, 375~)
  - `org.apache.hadoop.hdds.scm.pipeline.PipelineStateManagerImpl` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/pipeline/PipelineStateManagerImpl.java, 275~)
  - `org.apache.hadoop.hdds.scm.pipeline.PipelineStateMap` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/pipeline/PipelineStateMap.java, 250~)
  - `org.apache.hadoop.hdds.scm.pipeline.WritableECContainerProvider` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/pipeline/WritableECContainerProvider.java, 225~)
  - `org.apache.hadoop.hdds.scm.pipeline.BackgroundPipelineCreator` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/pipeline/BackgroundPipelineCreator.java, 225~)
  - `org.apache.hadoop.hdds.scm.pipeline.RatisPipelineProvider` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/pipeline/RatisPipelineProvider.java, 200~)
  - `org.apache.hadoop.hdds.scm.pipeline.SortedList` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/pipeline/SortedList.java, 200~)

### `safemode.md` — 13 classes (2 anchors)

- feature key in atlas.json: `component=SCM, feature=safemode`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.safemode.SCMSafeModeManager` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/safemode/SCMSafeModeManager.java, 275~)
  - `org.apache.hadoop.hdds.scm.safemode.HealthyPipelineSafeModeRule` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/safemode/HealthyPipelineSafeModeRule.java, 250~)

### `scm-audit.md` — 1 classes (0 anchors)

- feature key in atlas.json: `component=SCM, feature=scm-audit`

### `scm-commands.md` — 1 classes (0 anchors)

- feature key in atlas.json: `component=SCM, feature=scm-commands`

### `scm-events.md` — 1 classes (0 anchors)

- feature key in atlas.json: `component=SCM, feature=scm-events`

### `scm-ha.md` — 59 classes (7 anchors)

- feature key in atlas.json: `component=SCM, feature=scm-ha`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.ha.SequenceIdGenerator` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/ha/SequenceIdGenerator.java, 250~)
  - `org.apache.hadoop.hdds.scm.ha.SCMStateMachine` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/ha/SCMStateMachine.java, 375~)
  - `org.apache.hadoop.hdds.scm.ha.SCMHAManagerImpl` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/ha/SCMHAManagerImpl.java, 300~)
  - `org.apache.hadoop.hdds.scm.ha.SCMRatisServerImpl` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/ha/SCMRatisServerImpl.java, 300~)
  - `org.apache.hadoop.hdds.scm.ha.invoker.ContainerStateManagerInvoker` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/ha/invoker/ContainerStateManagerInvoker.java, 250~)
  - `org.apache.hadoop.hdds.scm.ha.SCMHANodeDetails` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/ha/SCMHANodeDetails.java, 225~)
  - `org.apache.hadoop.hdds.scm.ha.invoker.PipelineStateManagerInvoker` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/ha/invoker/PipelineStateManagerInvoker.java, 200~)

### `scm-metadata.md` — 4 classes (0 anchors)

- feature key in atlas.json: `component=SCM, feature=scm-metadata`

### `scm-protocol.md` — 5 classes (0 anchors)

- feature key in atlas.json: `component=SCM, feature=scm-protocol`

### `scm-security.md` — 6 classes (1 anchors)

- feature key in atlas.json: `component=SCM, feature=scm-security`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.security.RootCARotationManager` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/security/RootCARotationManager.java, 575~)

### `scm-server.md` — 25 classes (8 anchors)

- feature key in atlas.json: `component=SCM, feature=scm-server`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.SCMCommonPlacementPolicy` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/SCMCommonPlacementPolicy.java, 375~)
  - `org.apache.hadoop.hdds.scm.server.StorageContainerManager` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/server/StorageContainerManager.java, 1475~)
  - `org.apache.hadoop.hdds.scm.server.SCMClientProtocolServer` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/server/SCMClientProtocolServer.java, 1350~)
  - `org.apache.hadoop.hdds.scm.server.SCMDatanodeProtocolServer` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/server/SCMDatanodeProtocolServer.java, 350~)
  - `org.apache.hadoop.hdds.scm.server.SCMSecurityProtocolServer` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/server/SCMSecurityProtocolServer.java, 350~)
  - `org.apache.hadoop.hdds.scm.server.SCMBlockProtocolServer` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/server/SCMBlockProtocolServer.java, 350~)
  - `org.apache.hadoop.hdds.scm.server.SCMDatanodeHeartbeatDispatcher` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/server/SCMDatanodeHeartbeatDispatcher.java, 275~)
  - `org.apache.hadoop.hdds.scm.server.ContainerReportQueue` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/server/ContainerReportQueue.java, 275~)

### `upgrade.md` — 8 classes (1 anchors)

- feature key in atlas.json: `component=SCM, feature=upgrade`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.hdds.scm.server.upgrade.FinalizationStateManagerImpl` (hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/server/upgrade/FinalizationStateManagerImpl.java, 200~)
