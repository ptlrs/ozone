# Bench &amp; Insight / vapor

**Classes:** 14    **Kinds:** cli:7, service:6, abstract:1

## Overview

Vapor is the SCM- and datanode-level stress-test submodule that was split from Freon by HDDS-14771. It targets scenarios where the bottleneck is not OM throughput but rather SCM capacity or individual datanode write pipelines. `SCMThroughputBenchmark` registers fake datanodes against a real SCM, force-exits safe mode, optionally activates pipelines, then hammers SCM with block allocations, container allocations, or container-report processing using multi-threaded inner benchmark classes. `DatanodeSimulator` takes a longer-running approach: it registers thousands of simulated datanode identities, drives SCM heartbeats via a `ScheduledExecutorService`, grows containers to a target count, then moves all simulated nodes to read-only mode. Per-simulated-datanode state is encapsulated in `DatanodeSimulationState` (pipelines, containers, registration status) and serialised to a JSON file on shutdown so a run can be resumed with `--reload`. `FollowerAppendLogEntryGenerator` isolates a single follower datanode and drives Ratis `AppendEntries` RPCs directly via gRPC, bypassing the normal pipeline write path. `GeneratorDatanode` generates offline container metadata and data on disk, useful for populating a datanode's storage without going through Ratis.

## Class table

### Sub-feature: `freon.containergenerator`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2735 | `org.apache.hadoop.ozone.freon.containergenerator.BaseGenerator` | abstract | mixed | 50~ | 30 | Common options of data generators for fast scale test. |
| 2736 | `org.apache.hadoop.ozone.freon.containergenerator.GeneratorDatanode` | cli | logic-heavy | 225~ | 20 | inferred(from-md): Offline container metadata and data generator for datanode storage directories. |
| 2737 | `org.apache.hadoop.ozone.freon.containergenerator.GeneratorOm` | cli | mixed | 150~ | 20 | Container generator for OM metadata. |
| 2738 | `org.apache.hadoop.ozone.freon.containergenerator.GeneratorScm` | cli | mixed | 25~ | 20 | Container generator for SCM metadata. |

### Sub-feature: `ozone.freon`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2739 | `org.apache.hadoop.ozone.freon.SCMThroughputBenchmark` | service | logic-heavy | 650~ | 60 | Benchmark for scm throughput. |
| 2740 | `org.apache.hadoop.ozone.freon.DatanodeSimulator` | service | logic-heavy | 375~ | 45 | This command simulates a number of datanodes and coordinates with SCM to create a number of containers on the said da... |
| 2741 | `org.apache.hadoop.ozone.freon.DatanodeSimulationState` | service | logic-heavy | 275~ | 45 | Encapsulates states of a simulated datanode instance. |
| 2742 | `org.apache.hadoop.ozone.freon.FollowerAppendLogEntryGenerator` | cli | logic-heavy | 275~ | 45 | inferred(from-md): Drives a single follower datanode with synthetic Ratis AppendEntries RPCs from a fake leader. |
| 2743 | `org.apache.hadoop.ozone.freon.StreamingGenerator` | service | mixed | 100~ | 30 | Freon test for streaming service. |
| 2744 | `org.apache.hadoop.ozone.freon.BaseAppendLogGenerator` | service | mixed | 25~ | 30 | Generic utility for leader/follower specific isolated tests. |
| 2745 | `org.apache.hadoop.ozone.freon.LeaderAppendLogEntryGenerator` | cli | mixed | 150~ | 20 | inferred: LeaderAppendLogEntryGenerator — role not documented. |
| 2746 | `org.apache.hadoop.ozone.freon.ChunkManagerDiskWrite` | cli | mixed | 100~ | 20 | inferred: ChunkManagerDiskWrite — role not documented. |
| 2747 | `org.apache.hadoop.ozone.freon.VaporSubcommand` | cli | mixed | 25~ | 20 | Marker interface for subcommands to be registered for ozone vapor. |
| 2748 | `org.apache.hadoop.ozone.freon.Vapor` | cli | mixed | 25~ | 20 | Ozone data generator and performance test tool. |



## Diagram

```mermaid
flowchart TD
  Vapor["Vapor ("ozone vapor")"] --> SCMThroughputBenchmark
  Vapor --> DatanodeSimulator
  Vapor --> FollowerAppendLogEntryGenerator
  Vapor --> LeaderAppendLogEntryGenerator
  Vapor --> ChunkManagerDiskWrite
  Vapor --> StreamingGenerator

  subgraph ContainerGenerator["ozone vapor cgdn / cgom / cgscm"]
    GeneratorDatanode
    GeneratorOm
    GeneratorScm
    BaseGenerator --> GeneratorDatanode
    BaseGenerator --> GeneratorOm
    BaseGenerator --> GeneratorScm
  end

  DatanodeSimulator --> DatanodeSimulationState
  SCMThroughputBenchmark -->|fake heartbeats| SCM[(SCM)]
  DatanodeSimulator -->|heartbeat loop| SCM
  DatanodeSimulator -->|heartbeat| Recon[(Recon)]
  FollowerAppendLogEntryGenerator -->|gRPC AppendEntries| DN[(Datanode)]
```

## Anchor details

### `SCMThroughputBenchmark`

- **path:** `hadoop-ozone/vapor/src/main/java/org/apache/hadoop/ozone/freon/SCMThroughputBenchmark.java`
- **loc:** 650~    **difficulty:** 5    **study:** 60 min    **concurrency:** actor/queue    **persistence:** in-memory
- **entry points:** `call`, `run`
- **key collaborators:** `org.apache.hadoop.hdds.cli.HddsVersionProvider`, `org.apache.hadoop.hdds.client.RatisReplicationConfig`, `org.apache.hadoop.hdds.client.ReplicationConfig`, `org.apache.hadoop.hdds.conf.OzoneConfiguration`, `org.apache.hadoop.hdds.protocol.DatanodeDetails`, `org.apache.hadoop.hdds.protocol.DatanodeID`
- **role:** Benchmark for SCM throughput: block allocation, container allocation, or container-report processing.
- `initCluster()` calls `activatePipelines()` which sleeps for 60 seconds unconditionally (`Thread.sleep(Duration.ofSeconds(60).toMillis())`) to wait for SCM to create pipelines before force-activating them. This hardcoded delay is a known limitation: if pipeline creation is slow, the benchmark proceeds with zero pipelines and all block-allocation calls fail. The `ProcessReports` benchmark path drives SCM with fake container-report heartbeats, making it the right tool for measuring SCM report-processing throughput without real datanode churn.

### `DatanodeSimulator`

- **path:** `hadoop-ozone/vapor/src/main/java/org/apache/hadoop/ozone/freon/DatanodeSimulator.java`
- **loc:** 375~    **difficulty:** 4    **study:** 45 min    **concurrency:** actor/queue    **persistence:** in-memory
- **entry points:** `call`
- **key collaborators:** `org.apache.hadoop.hdds.DatanodeVersion`, `org.apache.hadoop.hdds.HddsUtils`, `org.apache.hadoop.hdds.cli.HddsVersionProvider`, `org.apache.hadoop.hdds.conf.ConfigurationSource`, `org.apache.hadoop.hdds.protocol.DatanodeDetails`, `org.apache.hadoop.hdds.protocol.DatanodeID`
- **role:** Simulates thousands of datanodes against a real SCM to stress-test node management and container placement.
- The `--reload` flag deserialises previously saved `DatanodeSimulationState` objects from a JSON file via Jackson so a simulation run can be resumed across process restarts. The `growContainers()` loop allocates containers via `scmContainerClient` and records SCM-assigned container IDs in each `DatanodeSimulationState`; once the target count is reached, `moveDatanodesToReadonly()` closes all pipelines for simulated nodes and sets a `volatile boolean readOnly` flag so subsequent heartbeats report no active pipelines to SCM.

### `DatanodeSimulationState`

- **path:** `hadoop-ozone/vapor/src/main/java/org/apache/hadoop/ozone/freon/DatanodeSimulationState.java`
- **loc:** 275~    **difficulty:** 4    **study:** 45 min    **concurrency:** single-threaded    **persistence:** in-memory
- **key collaborators:** `org.apache.hadoop.hdds.conf.StorageUnit`, `org.apache.hadoop.hdds.protocol.DatanodeDetails`, `org.apache.hadoop.ozone.container.common.impl.StorageLocationReport`
- **role:** Encapsulates per-simulated-datanode state: registration status, pipeline set, container map, and read-only flag.
- The constant `CONTAINER_SIZE = 5 GB` (line 60) drives the storage-capacity numbers reported in heartbeats to SCM. Each simulated datanode reports exactly one storage location with capacity derived from `targetContainersCount * CONTAINER_SIZE`, so SCM's capacity tracking reflects the simulation scale. The `volatile boolean readOnly` field is checked on every heartbeat to decide whether to include pipeline-reports or omit them, letting SCM see the nodes as passive replicas.

### `FollowerAppendLogEntryGenerator`

- **path:** `hadoop-ozone/vapor/src/main/java/org/apache/hadoop/ozone/freon/FollowerAppendLogEntryGenerator.java`
- **loc:** 275~    **difficulty:** 4    **study:** 45 min    **concurrency:** actor/queue    **persistence:** in-memory
- **entry points:** `call`
- **key collaborators:** `org.apache.hadoop.hdds.cli.HddsVersionProvider`, `org.apache.hadoop.hdds.conf.OzoneConfiguration`
- **role:** Drives a single follower datanode with synthetic Ratis AppendEntries RPCs from a fake leader.
- The test requires the target datanode to be started with `OZONE_DATANODE_STANDALONE_TEST=follower`. It opens a gRPC `RaftServerProtocolServiceStub` directly, constructs `AppendEntriesRequestProto` messages containing synthetic `WriteChunkRequestProto` payloads, and measures the `AppendEntriesReplyProto` latency. A `TimedSemaphore` (from Commons Lang) caps the RPC send rate to the configured throughput, making this the only Freon/Vapor tool that tests Ratis pipeline write latency in isolation from the full Ozone stack.

### `GeneratorDatanode`

- **path:** `hadoop-ozone/vapor/src/main/java/org/apache/hadoop/ozone/freon/containergenerator/GeneratorDatanode.java`
- **loc:** 225~    **difficulty:** 2    **study:** 20 min    **concurrency:** single-threaded    **persistence:** in-memory
- **entry points:** `call`
- **key collaborators:** `org.apache.hadoop.ozone.freon.VaporSubcommand`, `org.apache.hadoop.hdds.cli.HddsVersionProvider`, `org.apache.hadoop.hdds.client.BlockID`, `org.apache.hadoop.hdds.conf.ConfigurationSource`, `org.apache.hadoop.hdds.scm.OzoneClientConfig`, `org.apache.hadoop.hdds.scm.container.common.helpers.StorageContainerException`
- **test exemplar:** `hadoop-ozone/vapor/src/test/java/org/apache/hadoop/ozone/freon/containergenerator/TestGeneratorDatanode.java`
- **role:** Offline container metadata and data generator for datanode storage directories.
- `GeneratorDatanode` writes directly to `KeyValueContainer`/`BlockManagerImpl`/`ChunkManager` without involving any RPC. It uses `MutableVolumeSet` and `RoundRobinVolumeChoosingPolicy` to distribute containers across configured data dirs, and calls `DispatcherContext.WriteChunkStage.WRITE_DATA` then `COMMIT_DATA` to match the two-phase write path that the real XCeiverServer uses. The `--index` / `--datanodes` sharding parameters allow parallelising generation across hosts: each host generates a disjoint container-ID range.


## Design docs

- `hadoop-hdds/docs/content/design/scmha.md` — SCM HA design; `SCMThroughputBenchmark` and `DatanodeSimulator` stress SCM's container and node management paths described here.
- `hadoop-hdds/docs/content/design/multiraft.md` — multi-Raft pipeline design; `FollowerAppendLogEntryGenerator` isolates a single follower node in this topology.
- no dedicated vapor design doc under `hadoop-hdds/docs/content/` on this branch.

## Seminal JIRAs / PRs

- HDDS-14771. Split server-side load testers from freon (created the vapor submodule, moved SCMThroughputBenchmark, DatanodeSimulator, FollowerAppendLogEntryGenerator, and container generators).
- HDDS-15614. Remove Datanode download/pull container replication (touched DatanodeSimulator heartbeat logic).
- HDDS-15682. Use the original configured host and port to identify SCM nodes (fixed SCMThroughputBenchmark SCM address resolution).
- HDDS-13199. Remove DatanodeDetails#getUuid and DatanodeID#getUuid methods (required updates in DatanodeSimulator).
- HDDS-15274. Fork RetryPolicies from Hadoop (affected SCMThroughputBenchmark's RPC retry setup).

## Sharp edges

- `SCMThroughputBenchmark.activatePipelines()` contains an unconditional `Thread.sleep(60_000)` before activating pipelines. If SCM allocates pipelines in less than 60 seconds the benchmark wastes time; if allocation takes longer the benchmark proceeds with zero pipelines and all block-allocation tasks fail immediately, printing no useful diagnostic.
- `DatanodeSimulator` uses `OZONE_SCM_HEARTBEAT_RPC_TIMEOUT` and `HDDS_CONTAINER_REPORT_INTERVAL` from config, but the Javadoc comment at line 97 warns that `ozone.scm.heartbeat.thread.interval`, `hdds.heartbeat.interval`, `ozone.scm.stale.node.interval`, and `ozone.scm.dead.node.interval` must be manually lengthened in `ozone-site.xml` before running the simulation — otherwise SCM marks simulated datanodes as stale/dead within minutes.
- `GeneratorDatanode` writes container chunks using `WriteChunkStage.WRITE_DATA` followed by `COMMIT_DATA`. If the process is killed between the two stages, the container on disk will be in a partially-committed state and the datanode will fail to load it on next start, requiring manual cleanup.

## Related features

- `components/bench-insight/freon.md` — Freon covers OM/client-level load; vapor covers SCM/datanode-level load.
- `components/bench-insight/ozone-tools.md` — `ozone local` provides a convenient single-JVM target for light vapor testing.
- `components/SCM/scm-replication.md` — `DatanodeSimulator` exercises the SCM replication manager and node management.
- `components/Container/container-service.md` — `GeneratorDatanode` writes directly to the container storage layer.

## Self-quiz

1. `SCMThroughputBenchmark.initCluster()` calls four methods in sequence. Name them in order and explain what would go wrong if `exitSafeMode()` were skipped for a `BlockBenchmark` run.
2. `DatanodeSimulator` saves datanode state to a file on shutdown via a JVM shutdown hook. Which Jackson annotations on `DatanodeSimulationState` enable serialization of `InetSocketAddress` fields, and why is custom serialization needed there?
3. `DatanodeSimulationState.CONTAINER_SIZE` is 5 GB. How does this constant affect what SCM's capacity model believes about the simulated cluster, and what happens when `targetContainersCount * CONTAINER_SIZE` exceeds the storage value in the `NodeReportProto`?
4. `FollowerAppendLogEntryGenerator` sends `AppendEntriesRequestProto` messages directly via gRPC. What special environment variable must be set on the target datanode, and which Ratis gRPC service stub does the generator use?
5. `GeneratorDatanode` uses `--index` and `--datanodes` to shard container-ID ranges. If you have 3 datanodes and 900 containers per datanode, what container-ID range would `--index=2 --datanodes=3` generate, and how are range boundaries computed?

<details>
<summary>Answers</summary>

Answer 1: The four methods are `initSCMClients()`, `registerFakeDatanodes()`, `activatePipelines()` (only if the benchmark requires pipelines), and `exitSafeMode()`. If `exitSafeMode()` were skipped for `BlockBenchmark`, SCM would still be in safe mode and all `allocateBlock()` calls would be rejected with a safe-mode exception, producing 100% failure counts with no meaningful throughput measurement.

Answer 2: `DatanodeSimulationState` uses `@JsonSerialize(using = InetSocketAddressSerializer.class)` and `@JsonDeserialize(using = InetSocketAddressDeserializer.class)` (custom inner classes). Custom serialization is needed because Jackson's default serialization of `InetSocketAddress` does not produce a stable, deserializable JSON form — it would serialize internal fields that change across JVM versions.

Answer 3: Each simulated datanode reports storage capacity as `targetContainersCount * CONTAINER_SIZE` bytes in its `StorageReportProto`. SCM aggregates these values to compute cluster-total capacity. If the product exceeds the `StorageReportProto`'s long capacity field (unlikely at normal scales), it would overflow. In practice this is how the simulation creates the impression of an exabyte-scale cluster.

Answer 4: The target datanode must be started with `OZONE_DATANODE_STANDALONE_TEST=follower` (documented in the Javadoc at line 80). The generator uses `RaftServerProtocolServiceGrpc.RaftServerProtocolServiceStub` from the Ratis gRPC library to send `AppendEntriesRequestProto` messages directly to the datanode's Ratis server port.

Answer 5: The generator assigns container IDs as `index + k * datanodes` for each container k. With `--index=2 --datanodes=3` and 900 containers, the range is IDs `{2, 5, 8, ..., 2 + (900-1)*3}` = IDs 2, 5, 8, ... 2699. Each host generates every 3rd container ID starting from its own index, ensuring no overlap across the three hosts.

</details>
