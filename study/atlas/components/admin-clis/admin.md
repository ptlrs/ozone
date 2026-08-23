# Admin CLIs / admin

**Classes:** 101    **Kinds:** cli:80, service:20, dto:1

## Overview

The `admin` feature group implements `ozone admin`, the operator-facing CLI for SCM and OM management. Its 101 classes split into two layers: a thin RPC facade (`ContainerOperationClient` implements `ScmClient` and delegates every call to `StorageContainerLocationProtocol`) and a large set of Picocli subcommands that call through that facade. Subcommands cover containers, pipelines, datanodes, disk balancing, container balancing, safe mode, certificates, replication manager, OM lifecycle, upgrade finalization, and namespace summary. `AbstractDiskBalancerSubCommand` adds a batch-execution loop that resolves UUID-based node IDs through SCM before dispatching to individual datanode gRPC endpoints. `ContainerBalancerStatusSubcommand` reads rich iteration-history protos from SCM and formats them for human or JSON output. The OM-facing subcommands (under `org.apache.hadoop.ozone.admin.om`) connect via the OM admin protocol rather than SCM, keeping the two service paths independent.

## Diagram

```mermaid
classDiagram
  class ScmClient {
    <<interface>>
  }
  class ContainerOperationClient {
    +execute(ScmClient)
  }
  class ScmSubcommand {
    <<abstract>>
    +execute(ScmClient)
  }
  class AbstractDiskBalancerSubCommand {
    <<abstract>>
    +call()
    +executeCommand(hostName)
    +displayResults(success, failed)
  }
  class InfoSubcommand
  class TopologySubcommand
  class ContainerBalancerStatusSubcommand
  class UsageInfoSubcommand
  class DiskBalancerStartSubcommand
  class DiskBalancerStatusSubcommand
  class DiskBalancerReportSubcommand

  ScmClient <|.. ContainerOperationClient
  ScmSubcommand <|-- InfoSubcommand
  ScmSubcommand <|-- TopologySubcommand
  ScmSubcommand <|-- ContainerBalancerStatusSubcommand
  ScmSubcommand <|-- UsageInfoSubcommand
  AbstractDiskBalancerSubCommand <|-- DiskBalancerStartSubcommand
  AbstractDiskBalancerSubCommand <|-- DiskBalancerStatusSubcommand
  AbstractDiskBalancerSubCommand <|-- DiskBalancerReportSubcommand
  ContainerOperationClient ..> AbstractDiskBalancerSubCommand : used by
```

## Class table

### Sub-feature: `admin.nssummary`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1207 | `org.apache.hadoop.ozone.admin.nssummary.NSSummaryCLIUtils` | service | mixed | 100~ | 30 | Utility class to support Namespace CLI. |
| 1208 | `org.apache.hadoop.ozone.admin.nssummary.NSSummaryAdmin` | service | mixed | 50~ | 30 | Subcommand for admin operations related to OM. |
| 1209 | `org.apache.hadoop.ozone.admin.nssummary.DiskUsageSubCommand` | cli | mixed | 150~ | 20 | Disk Usage Subcommand. |
| 1210 | `org.apache.hadoop.ozone.admin.nssummary.SummarySubCommand` | cli | mixed | 75~ | 20 | Namespace Summary Subcommand. |
| 1211 | `org.apache.hadoop.ozone.admin.nssummary.QuotaUsageSubCommand` | cli | mixed | 75~ | 20 | Quota Usage Subcommand. |
| 1212 | `org.apache.hadoop.ozone.admin.nssummary.FileSizeDistSubCommand` | cli | mixed | 75~ | 20 | File Size Distribution Subcommand. |

### Sub-feature: `admin.om`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1213 | `org.apache.hadoop.ozone.admin.om.OmAddressOptions` | service | mixed | 100~ | 30 | Defines command-line options for OM address, whether service or single host. |
| 1214 | `org.apache.hadoop.ozone.admin.om.OMAdmin` | service | mixed | 75~ | 30 | Subcommand for admin operations related to OM. |
| 1215 | `org.apache.hadoop.ozone.admin.om.ListOpenFilesSubCommand` | cli | mixed | 175~ | 20 | inferred: ListOpenFilesSubCommand — role not documented. |
| 1216 | `org.apache.hadoop.ozone.admin.om.PrepareSubCommand` | cli | mixed | 125~ | 20 | Handler of ozone admin om prepare command. |
| 1217 | `org.apache.hadoop.ozone.admin.om.DecommissionOMSubcommand` | cli | mixed | 125~ | 20 | inferred: DecommissionOMSubcommand — role not documented. |
| 1218 | `org.apache.hadoop.ozone.admin.om.FinalizeUpgradeSubCommand` | cli | mixed | 100~ | 20 | Handler of ozone admin om finalizeUpgrade command. |
| 1219 | `org.apache.hadoop.ozone.admin.om.GetServiceRolesSubcommand` | cli | mixed | 75~ | 20 | Handler of om roles command. |
| 1220 | `org.apache.hadoop.ozone.admin.om.LifecycleStatusSubCommand` | cli | mixed | 50~ | 20 | Handler of ozone admin om lifecycle status command. |
| 1221 | `org.apache.hadoop.ozone.admin.om.LifecycleSuspendSubCommand` | cli | mixed | 50~ | 20 | Handler of ozone admin om lifecycle suspend command. |
| 1222 | `org.apache.hadoop.ozone.admin.om.TransferOmLeaderSubCommand` | cli | mixed | 25~ | 20 | Handler of ozone admin om transfer command. |
| 1223 | `org.apache.hadoop.ozone.admin.om.CancelPrepareSubCommand` | cli | mixed | 25~ | 20 | Handler of ozone admin om cancelprepare command. |
| 1224 | `org.apache.hadoop.ozone.admin.om.FetchKeySubCommand` | cli | mixed | 25~ | 20 | Handler of ozone admin om fetch-key command. |
| 1225 | `org.apache.hadoop.ozone.admin.om.LifecycleSubCommand` | cli | mixed | 25~ | 20 | Subcommand to admin operations related to Lifecycle Service. |
| 1226 | `org.apache.hadoop.ozone.admin.om.FinalizationStatusSubCommand` | cli | mixed | 25~ | 20 | Handler of ozone admin om finalizationstatus command. |
| 1227 | `org.apache.hadoop.ozone.admin.om.UpdateRangerSubcommand` | cli | mixed | 25~ | 20 | Handler of om updateranger command. |
| 1228 | `org.apache.hadoop.ozone.admin.om.LifecycleResumeSubCommand` | cli | mixed | 25~ | 20 | Handler of ozone admin om lifecycle resume command. |

### Sub-feature: `admin.reconfig`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1229 | `org.apache.hadoop.ozone.admin.reconfig.ReconfigureSubCommandUtil` | service | mixed | 75~ | 30 | Reconfigure subcommand utils. |
| 1230 | `org.apache.hadoop.ozone.admin.reconfig.ReconfigureCommands` | cli | mixed | 50~ | 20 | Subcommand to group reconfigure OM related operations. |
| 1231 | `org.apache.hadoop.ozone.admin.reconfig.ReconfigureStatusSubcommand` | cli | mixed | 50~ | 20 | Handler of ozone admin reconfig status command. |
| 1232 | `org.apache.hadoop.ozone.admin.reconfig.AbstractReconfigureSubCommand` | cli | mixed | 25~ | 20 | An abstract Class use to ReconfigureSubCommand. |
| 1233 | `org.apache.hadoop.ozone.admin.reconfig.ReconfigurePropertiesSubcommand` | cli | mixed | 25~ | 20 | Handler of ozone admin reconfig properties command. |
| 1234 | `org.apache.hadoop.ozone.admin.reconfig.ReconfigureStartSubcommand` | cli | mixed | 25~ | 20 | Handler of ozone admin reconfig start command. |

### Sub-feature: `admin.scm`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1235 | `org.apache.hadoop.ozone.admin.scm.DeletedBlocksTxnCommands` | service | mixed | 25~ | 30 | Subcommand to group container related operations. |
| 1236 | `org.apache.hadoop.ozone.admin.scm.ScmAdmin` | service | mixed | 25~ | 30 | Subcommand for admin operations related to SCM. |
| 1237 | `org.apache.hadoop.ozone.admin.scm.FinalizeScmUpgradeSubcommand` | cli | mixed | 100~ | 20 | Handler of Finalize SCM command. |
| 1238 | `org.apache.hadoop.ozone.admin.scm.GetScmRatisRolesSubcommand` | cli | mixed | 75~ | 20 | Handler of scm status command. |
| 1239 | `org.apache.hadoop.ozone.admin.scm.DecommissionScmSubcommand` | cli | mixed | 25~ | 20 | Handler of ozone admin scm decommission command. |
| 1240 | `org.apache.hadoop.ozone.admin.scm.RotateKeySubCommand` | cli | mixed | 25~ | 20 | inferred: RotateKeySubCommand — role not documented. |
| 1241 | `org.apache.hadoop.ozone.admin.scm.TransferScmLeaderSubCommand` | cli | mixed | 25~ | 20 | Handler of ozone admin scm transfer command. |
| 1242 | `org.apache.hadoop.ozone.admin.scm.GetDeletedBlockSummarySubcommand` | cli | mixed | 25~ | 20 | Handler of getting deleted blocks summary from SCM side. |
| 1243 | `org.apache.hadoop.ozone.admin.scm.FinalizationScmStatusSubcommand` | cli | mixed | 25~ | 20 | Handler of FinalizationStatus SCM command. |

### Sub-feature: `cli.cert`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1244 | `org.apache.hadoop.hdds.scm.cli.cert.ListSubcommand` | cli | mixed | 125~ | 20 | This is the handler that process certificate list command. |
| 1245 | `org.apache.hadoop.hdds.scm.cli.cert.CertCommands` | cli | mixed | 25~ | 20 | Sub command for certificate related operations. |
| 1246 | `org.apache.hadoop.hdds.scm.cli.cert.InfoSubcommand` | cli | mixed | 25~ | 20 | This is the handler that process certificate info command. |
| 1247 | `org.apache.hadoop.hdds.scm.cli.cert.CleanExpiredCertsSubcommand` | cli | mixed | 25~ | 20 | This is the handler to clean SCM database from expired certificates. |
| 1248 | `org.apache.hadoop.hdds.scm.cli.cert.ScmCertSubcommand` | cli | mixed | 25~ | 20 | Base class for admin commands that connect via SCM security client. |

### Sub-feature: `cli.container`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1249 | `org.apache.hadoop.hdds.scm.cli.container.ContainerIDParameters` | service | mixed | 50~ | 30 | Parameter for specifying list of container IDs. |
| 1250 | `org.apache.hadoop.hdds.scm.cli.container.InfoSubcommand` | cli | logic-heavy | 225~ | 20 | This is the handler that process container info command. |
| 1251 | `org.apache.hadoop.hdds.scm.cli.container.ReconcileSubcommand` | cli | mixed | 175~ | 20 | Handle the container reconcile CLI command. |
| 1252 | `org.apache.hadoop.hdds.scm.cli.container.ReportSubcommand` | cli | mixed | 125~ | 20 | This is the handler to process the container report command. |
| 1253 | `org.apache.hadoop.hdds.scm.cli.container.ListSubcommand` | cli | mixed | 100~ | 20 | The ListSubcommand class represents a command to list containers in a structured way. |
| 1254 | `org.apache.hadoop.hdds.scm.cli.container.UpgradeSubcommand` | cli | mixed | 25~ | 20 | inferred: UpgradeSubcommand — role not documented. |
| 1255 | `org.apache.hadoop.hdds.scm.cli.container.ContainerCommands` | cli | mixed | 25~ | 20 | Subcommand to group container related operations. |
| 1256 | `org.apache.hadoop.hdds.scm.cli.container.CreateSubcommand` | cli | mixed | 25~ | 20 | This is the handler that process container creation command. |
| 1257 | `org.apache.hadoop.hdds.scm.cli.container.CloseSubcommand` | cli | mixed | 25~ | 20 | The handler of close container command. |

### Sub-feature: `cli.datanode`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1258 | `org.apache.hadoop.hdds.scm.cli.datanode.DiskBalancerSubCommandUtil` | service | mixed | 125~ | 30 | DiskBalancer subcommand utilities. |
| 1259 | `org.apache.hadoop.hdds.scm.cli.datanode.NodeSelectionMixin` | service | mixed | 25~ | 30 | Picocli mixin providing standardized datanode selection options for consistent CLI usage across commands. |
| 1260 | `org.apache.hadoop.hdds.scm.cli.datanode.DatanodeCommands` | service | mixed | 25~ | 30 | Subcommand for datanode related operations. |
| 1261 | `org.apache.hadoop.hdds.scm.cli.datanode.DatanodeParameters` | service | mixed | 25~ | 30 | Parameter for specifying list of datanode addresses. |
| 1262 | `org.apache.hadoop.hdds.scm.cli.datanode.DiskBalancerCommonOptions` | service | mixed | 25~ | 30 | Common options for DiskBalancer commands. |
| 1263 | `org.apache.hadoop.hdds.scm.cli.datanode.HostNameParameters` | service | mixed | 25~ | 30 | Parameter for specifying list of hostnames. |
| 1264 | `org.apache.hadoop.hdds.scm.cli.datanode.UsageInfoSubcommand` | cli | logic-heavy | 250~ | 20 | Command to list the usage info of a datanode. |
| 1265 | `org.apache.hadoop.hdds.scm.cli.datanode.AbstractDiskBalancerSubCommand` | cli | logic-heavy | 225~ | 20 | Abstract base class for DiskBalancer subcommands. |
| 1266 | `org.apache.hadoop.hdds.scm.cli.datanode.BasicDatanodeInfo` | dto | data-only | 175~ | 10 | Represents filtered Datanode information for json use. |
| 1267 | `org.apache.hadoop.hdds.scm.cli.datanode.ListInfoSubcommand` | cli | mixed | 175~ | 20 | Handler of list datanodes info command. |
| 1268 | `org.apache.hadoop.hdds.scm.cli.datanode.DiskBalancerReportSubcommand` | cli | mixed | 175~ | 20 | Handler to get disk balancer report. |
| 1269 | `org.apache.hadoop.hdds.scm.cli.datanode.DiskBalancerStatusSubcommand` | cli | mixed | 150~ | 20 | Handler to get disk balancer status. |
| 1270 | `org.apache.hadoop.hdds.scm.cli.datanode.DiskBalancerUpdateSubcommand` | cli | mixed | 125~ | 20 | Handler to update disk balancer configuration. |
| 1271 | `org.apache.hadoop.hdds.scm.cli.datanode.DecommissionStatusSubCommand` | cli | mixed | 125~ | 20 | Handler to print decommissioning nodes status. |
| 1272 | `org.apache.hadoop.hdds.scm.cli.datanode.DiskBalancerStartSubcommand` | cli | mixed | 125~ | 20 | Handler to start disk balancer. |
| 1273 | `org.apache.hadoop.hdds.scm.cli.datanode.DiskBalancerStopSubcommand` | cli | mixed | 50~ | 20 | Handler to stop disk balancer. |
| 1274 | `org.apache.hadoop.hdds.scm.cli.datanode.StatusSubCommand` | cli | mixed | 25~ | 20 | View status of one or more datanodes. |
| 1275 | `org.apache.hadoop.hdds.scm.cli.datanode.DiskBalancerCommands` | cli | mixed | 25~ | 20 | DiskBalancer command group for managing disk space balancing on Ozone datanodes. |
| 1276 | `org.apache.hadoop.hdds.scm.cli.datanode.MaintenanceSubCommand` | cli | mixed | 25~ | 20 | Place one or more datanodes into Maintenance Mode. |
| 1277 | `org.apache.hadoop.hdds.scm.cli.datanode.DecommissionSubCommand` | cli | mixed | 25~ | 20 | Decommission one or more datanodes. |
| 1278 | `org.apache.hadoop.hdds.scm.cli.datanode.RecommissionSubCommand` | cli | mixed | 25~ | 20 | Recommission one or more datanodes. |

### Sub-feature: `cli.pipeline`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1279 | `org.apache.hadoop.hdds.scm.cli.pipeline.FilterPipelineOptions` | service | mixed | 50~ | 30 | Defines command-line option for filtering pipelines. |
| 1280 | `org.apache.hadoop.hdds.scm.cli.pipeline.ClosePipelineSubcommand` | cli | mixed | 50~ | 20 | Handler of close pipeline command. |
| 1281 | `org.apache.hadoop.hdds.scm.cli.pipeline.ListPipelinesSubcommand` | cli | mixed | 50~ | 20 | Handler of list pipelines command. |
| 1282 | `org.apache.hadoop.hdds.scm.cli.pipeline.CreatePipelineSubcommand` | cli | mixed | 25~ | 20 | Handler of createPipeline command. |
| 1283 | `org.apache.hadoop.hdds.scm.cli.pipeline.ActivatePipelineSubcommand` | cli | mixed | 25~ | 20 | Handler of activate pipeline command. |
| 1284 | `org.apache.hadoop.hdds.scm.cli.pipeline.PipelineCommands` | cli | mixed | 25~ | 20 | Subcommand to group pipeline related operations. |
| 1285 | `org.apache.hadoop.hdds.scm.cli.pipeline.DeactivatePipelineSubcommand` | cli | mixed | 25~ | 20 | Handler of deactivate pipeline command. |

### Sub-feature: `hdds.util`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1286 | `org.apache.hadoop.hdds.util.DurationUtil` | service | mixed | 25~ | 30 | Pretty duration string representation. |

### Sub-feature: `om.lease`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1287 | `org.apache.hadoop.ozone.admin.om.lease.LeaseRecoverer` | service | mixed | 25~ | 30 | CLI to recover the lease of a specified file. |
| 1288 | `org.apache.hadoop.ozone.admin.om.lease.LeaseSubCommand` | cli | mixed | 25~ | 20 | Handler of ozone admin om lease command. |

### Sub-feature: `om.snapshot`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1289 | `org.apache.hadoop.ozone.admin.om.snapshot.DefragSubCommand` | cli | mixed | 75~ | 20 | Handler of ozone admin om snapshot defrag command. |
| 1290 | `org.apache.hadoop.ozone.admin.om.snapshot.SnapshotSubCommand` | cli | mixed | 25~ | 20 | Handler of ozone admin om snapshot command. |

### Sub-feature: `ozone.admin`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1291 | `org.apache.hadoop.ozone.admin.OzoneAdmin` | service | mixed | 25~ | 30 | Ozone Admin Command line tool. |

### Sub-feature: `scm.cli`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1292 | `org.apache.hadoop.hdds.scm.cli.ContainerOperationClient` | service | logic-heavy | 475~ | 60 | This class provides the client-facing APIs of container operations. |
| 1293 | `org.apache.hadoop.hdds.scm.cli.ScmOption` | service | mixed | 50~ | 30 | Defines command-line option for SCM address. |
| 1294 | `org.apache.hadoop.hdds.scm.cli.TopologySubcommand` | cli | logic-heavy | 225~ | 20 | Handler of printTopology command. |
| 1295 | `org.apache.hadoop.hdds.scm.cli.ContainerBalancerStatusSubcommand` | cli | logic-heavy | 200~ | 20 | Handler to query status of container balancer. |
| 1296 | `org.apache.hadoop.hdds.scm.cli.SafeModeCheckSubcommand` | cli | mixed | 150~ | 20 | This is the handler that process safe mode check command. |
| 1297 | `org.apache.hadoop.hdds.scm.cli.ContainerBalancerStartSubcommand` | cli | mixed | 100~ | 20 | Handler to start container balancer. |
| 1298 | `org.apache.hadoop.hdds.scm.cli.SafeModeWaitSubcommand` | cli | mixed | 50~ | 20 | This is the handler that process safe mode wait command. |
| 1299 | `org.apache.hadoop.hdds.scm.cli.ScmSubcommand` | cli | mixed | 25~ | 20 | Base class for admin commands that connect via SCM client. |
| 1300 | `org.apache.hadoop.hdds.scm.cli.SafeModeCommands` | cli | mixed | 25~ | 20 | Subcommand to group safe mode related operations. |
| 1301 | `org.apache.hadoop.hdds.scm.cli.ReplicationManagerCommands` | cli | mixed | 25~ | 20 | Subcommand to group replication manager related operations. |
| 1302 | `org.apache.hadoop.hdds.scm.cli.ReplicationManagerStatusSubcommand` | cli | mixed | 25~ | 20 | Handler to query status of replication manager. |
| 1303 | `org.apache.hadoop.hdds.scm.cli.ContainerBalancerStopSubcommand` | cli | mixed | 25~ | 20 | Handler to stop container balancer. |
| 1304 | `org.apache.hadoop.hdds.scm.cli.ReplicationManagerStartSubcommand` | cli | mixed | 25~ | 20 | Handler to start replication manager. |
| 1305 | `org.apache.hadoop.hdds.scm.cli.SafeModeExitSubcommand` | cli | mixed | 25~ | 20 | This is the handler that process safe mode exit command. |
| 1306 | `org.apache.hadoop.hdds.scm.cli.ReplicationManagerStopSubcommand` | cli | mixed | 25~ | 20 | Handler to stop replication manager. |
| 1307 | `org.apache.hadoop.hdds.scm.cli.ContainerBalancerCommands` | cli | mixed | 25~ | 20 | Subcommand to group container balancer related operations. |



## Anchor details

### `ContainerOperationClient`

- **path:** `hadoop-ozone/cli-admin/src/main/java/org/apache/hadoop/hdds/scm/cli/ContainerOperationClient.java`
- **loc:** 475~    **difficulty:** 5    **study:** 60 min    **concurrency:** single-threaded    **persistence:** in-memory
- **entry points:** `close`
- **key collaborators:** `org.apache.hadoop.hdds.client.ReplicationConfig`, `org.apache.hadoop.hdds.conf.ConfigurationSource`, `org.apache.hadoop.hdds.conf.OzoneConfiguration`, `org.apache.hadoop.hdds.protocol.DatanodeDetails`, `org.apache.hadoop.hdds.protocol.SecretKeyProtocolScm`, `org.apache.hadoop.hdds.scm.DatanodeAdminError`
- **role:** This class provides the client-facing APIs of container operations.

`createContainer` allocates via SCM then immediately issues a datanode `ContainerProtocolCalls.createContainer` RPC on the pipeline leader — two-phase create in one method. The `XceiverClientManager` is lazily initialized and guarded by `synchronized getXceiverClientManager()` to avoid creating it for read-only commands that never touch datanodes. `listContainer` silently caps the requested count at `OZONE_SCM_CONTAINER_LIST_MAX_COUNT` and logs a warning rather than throwing, which can confuse callers expecting a full page.

### `UsageInfoSubcommand`

- **path:** `hadoop-ozone/cli-admin/src/main/java/org/apache/hadoop/hdds/scm/cli/datanode/UsageInfoSubcommand.java`
- **loc:** 250~    **difficulty:** 2    **study:** 20 min    **concurrency:** single-threaded    **persistence:** in-memory
- **entry points:** `execute`
- **key collaborators:** `org.apache.hadoop.hdds.scm.cli.ScmSubcommand`, `org.apache.hadoop.hdds.cli.HddsVersionProvider`, `org.apache.hadoop.hdds.protocol.DatanodeDetails`, `org.apache.hadoop.hdds.scm.client.ScmClient`, `org.apache.hadoop.hdds.server.JsonUtils`
- **test exemplar:** `hadoop-ozone/cli-admin/src/test/java/org/apache/hadoop/hdds/scm/cli/datanode/TestUsageInfoSubcommand.java`
- **role:** Command to list the usage info of a datanode.

The `execute` method branches on whether a specific node address/UUID is given or a most/least-used count query: node-specific calls `getDatanodeUsageInfo(address, uuid)` while the sorted query calls `getDatanodeUsageInfo(mostUsed, count)`. The inner `DatanodeUsage` class holds both "Filesystem" stats (raw OS metrics, optional — guarded by `hasFsCapacity && hasFsAvailable`) and "Ozone" stats (capacity after reserved-space deduction), printed as separate labeled blocks to help diagnose disk-pressure situations.

### `AbstractDiskBalancerSubCommand`

- **path:** `hadoop-ozone/cli-admin/src/main/java/org/apache/hadoop/hdds/scm/cli/datanode/AbstractDiskBalancerSubCommand.java`
- **loc:** 225~    **difficulty:** 2    **study:** 20 min    **concurrency:** single-threaded    **persistence:** in-memory
- **entry points:** `call`
- **key collaborators:** `org.apache.hadoop.hdds.scm.cli.ContainerOperationClient`, `org.apache.hadoop.hdds.HddsConfigKeys`, `org.apache.hadoop.hdds.conf.OzoneConfiguration`, `org.apache.hadoop.hdds.scm.client.ScmClient`, `org.apache.hadoop.hdds.server.JsonUtils`
- **role:** Abstract base class for DiskBalancer subcommands.

`call()` checks `HDDS_DATANODE_DISK_BALANCER_ENABLED_KEY` from a locally loaded `OzoneConfiguration` before doing any RPC — if disk balancer is disabled in config the command exits immediately with a message rather than returning an error code. UUID-to-address resolution opens a short-lived `ContainerOperationClient` only when `--node-id` args are present or `--in-service-datanodes` is set, then closes it in a `finally` block. The `datanodeDisplayNames` map preserves pre-fetched SCM metadata for pretty-printing even when the per-node RPC later fails.

### `InfoSubcommand`

- **path:** `hadoop-ozone/cli-admin/src/main/java/org/apache/hadoop/hdds/scm/cli/container/InfoSubcommand.java`
- **loc:** 225~    **difficulty:** 2    **study:** 20 min    **concurrency:** single-threaded    **persistence:** in-memory
- **entry points:** `execute`
- **key collaborators:** `org.apache.hadoop.hdds.scm.cli.ScmSubcommand`, `org.apache.hadoop.hdds.HddsUtils`, `org.apache.hadoop.hdds.cli.HddsVersionProvider`, `org.apache.hadoop.hdds.client.ReplicationConfig`, `org.apache.hadoop.hdds.protocol.DatanodeDetails`, `org.apache.hadoop.hdds.scm.client.ScmClient`
- **role:** This is the handler that process container info command.

When a container's pipeline is empty (all associated datanodes are down or pipeline is deleted), `InfoSubcommand` wraps the output in `ContainerWithoutDatanodes` rather than `ContainerWithPipelineAndReplicas` to avoid serializing null datanode lists. For text output it attempts a second RPC (`getPipeline`) to show write-pipeline state, but catches `PipelineNotFoundException` and prints "CLOSED" — a deliberate graceful-degradation branch at line ~153 that makes the command safe to run on closed containers.

### `TopologySubcommand`

- **path:** `hadoop-ozone/cli-admin/src/main/java/org/apache/hadoop/hdds/scm/cli/TopologySubcommand.java`
- **loc:** 225~    **difficulty:** 2    **study:** 20 min    **concurrency:** single-threaded    **persistence:** in-memory
- **entry points:** `execute`
- **key collaborators:** `org.apache.hadoop.hdds.cli.AdminSubcommand`, `org.apache.hadoop.hdds.cli.HddsVersionProvider`, `org.apache.hadoop.hdds.protocol.DatanodeDetails`, `org.apache.hadoop.hdds.scm.client.ScmClient`, `org.apache.hadoop.hdds.server.JsonUtils`
- **role:** Handler of printTopology command.

`execute` iterates the fixed `STATES` list (HEALTHY, STALE, DEAD) and calls `queryNode` once per state, applying `--operational-state` and `--node-state` string filters client-side rather than server-side. The `--order` flag switches between `printOrderedByLocation` (groups by `networkLocation`, sorts alphabetically) and `printNodesWithLocation` (flat list with location column). Invalid state strings throw `InvalidPropertiesFormatException` rather than a picocli validation error — a rough edge in the user experience.

### `ContainerBalancerStatusSubcommand`

- **path:** `hadoop-ozone/cli-admin/src/main/java/org/apache/hadoop/hdds/scm/cli/ContainerBalancerStatusSubcommand.java`
- **loc:** 200~    **difficulty:** 2    **study:** 20 min    **concurrency:** single-threaded    **persistence:** in-memory
- **entry points:** `execute`
- **key collaborators:** `org.apache.hadoop.hdds.cli.HddsVersionProvider`, `org.apache.hadoop.hdds.scm.client.ScmClient`, `org.apache.hadoop.ozone.OzoneConsts`
- **role:** Handler to query status of container balancer.

`execute` calls `getContainerBalancerStatusInfo()` which returns a `ContainerBalancerStatusInfoResponseProto` containing the full iteration history. The `-v` flag gates the verbose path; `-H` (history) only has effect combined with `-v` — passing `-H` alone prints a warning but does not fail. Distinguishing the "currently running iteration" from completed iterations is done by filtering `iterationResult.isEmpty()` (empty string = in-progress) vs non-empty (finished).


## Design docs

- `hadoop-hdds/docs/content/tools/Admin.md` — operator reference for `ozone admin` subcommands.
- `hadoop-hdds/docs/content/design/diskbalancer.md` — design doc for the DiskBalancer feature that the disk-balancer subcommand group drives.
- `hadoop-hdds/docs/content/feature/ContainerBalancer.md` — ContainerBalancer feature doc, background for `ContainerBalancerStatusSubcommand` and `ContainerBalancerStartSubcommand`.

## Seminal JIRAs / PRs

- HDDS-14195. [DiskBalancer] Improve DiskBalancer CLI Output Readability and Usability
- HDDS-15306. Expose Disk Balancer CLI in top-level datanode help and improve usability
- HDDS-15490. [DiskBalancer] Align batch CLI success messages with HEALTHY IN_SERVICE datanode selection
- HDDS-15667. `containerbalancer status`: show stop reason and iteration details after stop
- HDDS-15690. Support Node-ID for DiskBalancer Commands
- HDDS-14108. Provide option in 'scm safemode status' to show status of all SCM nodes
- HDDS-14103. Create an option to suppress/unsuppress containers from report

## Sharp edges

- `ContainerOperationClient.listContainer` silently caps the page size at `OZONE_SCM_CONTAINER_LIST_MAX_COUNT` (default 50 000) with only a WARN log — callers expecting the full count get fewer results without an error (see `ContainerOperationClient.java` lines 368-375 and 383-390).
- `TopologySubcommand` filters operational state and node state strings client-side using raw string comparison. An unrecognized state string throws `InvalidPropertiesFormatException` rather than a picocli `ParameterException`, so the error message is less user-friendly (TopologySubcommand.java lines 102-122).
- `AbstractDiskBalancerSubCommand.call()` reads `HDDS_DATANODE_DISK_BALANCER_ENABLED_KEY` from a fresh local `OzoneConfiguration`, not from the cluster config fetched via SCM — if the operator has enabled disk balancer only on the server, the CLI may still refuse to run (AbstractDiskBalancerSubCommand.java lines 76-82).

## Related features

- `components/admin-clis/cli-common.md` — base classes (`GenericCli`, `ScmSubcommand`) that all admin subcommands extend.
- `components/admin-clis/shell.md` — user-facing `ozone sh` commands that share the same Picocli scaffolding.
- `components/admin-clis/interactive-shell.md` — `ozone interactive` wraps both admin and shell commands.
- `components/SCM/container-balancer.md` — server-side ContainerBalancer that the `containerbalancer` subcommands control.
- `components/Datanode/disk-balancer.md` — server-side DiskBalancer that the `diskbalancer` subcommands drive.

## Self-quiz

1. `ContainerOperationClient.createContainer` performs a two-phase operation. Name the two RPCs it issues and the order they occur.
2. `AbstractDiskBalancerSubCommand.call()` guards execution with a local config check before opening any SCM connection. Which config key does it read, and what happens if the check fails?
3. `InfoSubcommand.printDetails` uses two different JSON wrapper classes depending on pipeline state. What condition triggers `ContainerWithoutDatanodes` vs `ContainerWithPipelineAndReplicas`?
4. `TopologySubcommand.execute` queries SCM three times. What drives this loop, and what are the three values?
5. `ContainerBalancerStatusSubcommand` distinguishes an in-progress iteration from a completed one without a separate boolean field. What property of the proto message is used for this test?

<details>
<summary>Answers</summary>

Answer 1: First `storageContainerLocationClient.allocateContainer` (SCM RPC to register the container and get a pipeline), then `ContainerProtocolCalls.createContainer` (datanode RPC on the pipeline leader to physically create the container). SCM allocation always precedes datanode creation.

Answer 2: It reads `HddsConfigKeys.HDDS_DATANODE_DISK_BALANCER_ENABLED_KEY`. If the key evaluates to false it prints an error to stderr and returns null, aborting execution without contacting SCM.

Answer 3: `container.getPipeline().isEmpty()` — when the pipeline has no nodes (all datanodes unreachable or pipeline deleted), `ContainerWithoutDatanodes` is used; otherwise `ContainerWithPipelineAndReplicas` is used.

Answer 4: The static `STATES` list: `[HEALTHY, STALE, DEAD]`. `execute` calls `scmClient.queryNode(null, state, CLUSTER, "")` once for each of those three values.

Answer 5: `iterationResult.isEmpty()` on `ContainerBalancerTaskIterationStatusInfoProto`. An empty string means the iteration is still running; a non-empty string (e.g. "ITERATION_COMPLETED") means it has finished.

</details>
