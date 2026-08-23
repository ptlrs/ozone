# Enrichment scope for component: Recon

## Directory to edit

`/Users/rpatel/Github/ozone/study/atlas/components/recon/`

## Feature files to enrich (one at a time)

### `recon-api.md` — 102 classes (6 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-api`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.recon.api.OMDBInsightEndpoint` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/OMDBInsightEndpoint.java, 475~)
  - `org.apache.hadoop.ozone.recon.api.NodeEndpoint` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/NodeEndpoint.java, 300~)
  - `org.apache.hadoop.ozone.recon.api.StorageDistributionEndpoint` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/StorageDistributionEndpoint.java, 275~)
  - `org.apache.hadoop.ozone.recon.api.ExportJobManager` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/ExportJobManager.java, 250~)
  - `org.apache.hadoop.ozone.recon.chatbot.api.ChatbotEndpoint` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/chatbot/api/ChatbotEndpoint.java, 200~)
  - `org.apache.hadoop.ozone.recon.api.DataNodeMetricsService` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/api/DataNodeMetricsService.java, 250~)

### `recon-codegen.md` — 10 classes (0 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-codegen`

### `recon-fsck.md` — 7 classes (1 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-fsck`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.recon.fsck.ReconReplicationManager` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/fsck/ReconReplicationManager.java, 425~)

### `recon-heatmap.md` — 4 classes (1 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-heatmap`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.recon.heatmap.HeatMapUtil` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/heatmap/HeatMapUtil.java, 300~)

### `recon-metrics.md` — 8 classes (0 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-metrics`

### `recon-persistence.md` — 8 classes (1 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-persistence`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.recon.persistence.ContainerHealthSchemaManager` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/persistence/ContainerHealthSchemaManager.java, 375~)

### `recon-recovery.md` — 2 classes (1 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-recovery`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.recon.recovery.ReconOmMetadataManagerImpl` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/recovery/ReconOmMetadataManagerImpl.java, 200~)

### `recon-scm.md` — 22 classes (4 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-scm`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.recon.scm.ReconStorageContainerManagerFacade` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/scm/ReconStorageContainerManagerFacade.java, 825~)
  - `org.apache.hadoop.ozone.recon.scm.ReconStorageContainerSyncHelper` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/scm/ReconStorageContainerSyncHelper.java, 375~)
  - `org.apache.hadoop.ozone.recon.scm.ReconContainerManager` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/scm/ReconContainerManager.java, 325~)
  - `org.apache.hadoop.ozone.recon.scm.ReconNodeManager` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/scm/ReconNodeManager.java, 200~)

### `recon-security.md` — 2 classes (0 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-security`

### `recon-server.md` — 32 classes (6 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-server`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.recon.ReconUtils` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/ReconUtils.java, 475~)
  - `org.apache.hadoop.ozone.recon.chatbot.agent.ChatbotAgent` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/chatbot/agent/ChatbotAgent.java, 375~)
  - `org.apache.hadoop.ozone.recon.chatbot.llm.LangChain4jDispatcher` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/chatbot/llm/LangChain4jDispatcher.java, 325~)
  - `org.apache.hadoop.ozone.recon.ReconServer` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/ReconServer.java, 300~)
  - `org.apache.hadoop.ozone.recon.chatbot.recon.ReconEndpointRouter` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/chatbot/recon/ReconEndpointRouter.java, 200~)
  - `org.apache.hadoop.ozone.recon.chatbot.agent.LlmToolSpecFactory` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/chatbot/agent/LlmToolSpecFactory.java, 200~)

### `recon-spi.md` — 21 classes (1 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-spi`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.recon.spi.impl.ReconContainerMetadataManagerImpl` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/spi/impl/ReconContainerMetadataManagerImpl.java, 400~)

### `recon-tasks.md` — 37 classes (7 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-tasks`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.recon.tasks.ContainerKeyMapperHelper` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/tasks/ContainerKeyMapperHelper.java, 325~)
  - `org.apache.hadoop.ozone.recon.tasks.ReconTaskControllerImpl` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/tasks/ReconTaskControllerImpl.java, 650~)
  - `org.apache.hadoop.ozone.recon.tasks.NSSummaryTaskWithFSO` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/tasks/NSSummaryTaskWithFSO.java, 275~)
  - `org.apache.hadoop.ozone.recon.tasks.NSSummaryTaskWithLegacy` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/tasks/NSSummaryTaskWithLegacy.java, 275~)
  - `org.apache.hadoop.ozone.recon.tasks.OmTableInsightTask` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/tasks/OmTableInsightTask.java, 275~)
  - `org.apache.hadoop.ozone.recon.tasks.NSSummaryTaskDbEventHandler` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/tasks/NSSummaryTaskDbEventHandler.java, 275~)
  - `org.apache.hadoop.ozone.recon.tasks.NSSummaryTaskWithOBS` (hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/tasks/NSSummaryTaskWithOBS.java, 200~)

### `recon-upgrade.md` — 10 classes (0 anchors)

- feature key in atlas.json: `component=Recon, feature=recon-upgrade`
