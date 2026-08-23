# HddsCommon / ratis-integration

**Classes:** 11    **Kinds:** service:4, exception:4, interface:1, config:1, dto:1

## Overview

This feature group bridges Ozone's service model to Apache Ratis (Raft), providing the factory methods, retry policies, configuration bindings, and exception hierarchy that all Ozone Ratis clients share. `RatisHelper` is the central factory: it builds `RaftGroup`, `RaftPeer`, and `RaftGroupId` objects from an Ozone `Pipeline`, and assembles a `RaftClient` with Ozone-specific retry and timeout configuration from `RatisClientConfig`. HDDS-15768 added IPv6 bracket-wrapping in `RatisHelper.toRaftPeer()` so peer addresses like `[::1]:9858` are parsed correctly by Ratis. The retry subsystem uses `RetryPolicyCreator` as a strategy interface, with `RequestTypeDependentRetryPolicyCreator` mapping exception types (retriable-with-failover, retriable-without-failover, non-retriable) to policy instances via `RetriableWithFailOverException`, `RetriableWithNoFailoverException`, and `NonRetriableException`. `ContainerCommandRequestMessage` wraps `ContainerCommandRequestProto` in the Ratis `Message` interface so it can be submitted to a `RaftClient` directly. `SCMNodeInfo` and the HA exception types complete the group.

## Diagram

```mermaid
flowchart TD
  Pipeline["Pipeline"] -->|toRaftGroup| RatisHelper
  RatisHelper -->|newRaftClient| RaftClient["RaftClient (Ratis)"]
  RatisHelper -->|reads| RatisClientConfig
  RatisHelper -->|uses| RetryPolicyCreator
  RetryPolicyCreator <|.. RequestTypeDependentRetryPolicyCreator
  RetryPolicyCreator <|.. RetryLimitedPolicyCreator
  RequestTypeDependentRetryPolicyCreator -->|maps| RetriableWithFailOverException
  RequestTypeDependentRetryPolicyCreator -->|maps| RetriableWithNoFailoverException
  RequestTypeDependentRetryPolicyCreator -->|maps| NonRetriableException
  ContainerCommandRequestMessage -->|wraps| ContainerCommandRequestProto["ContainerCommandRequestProto (proto)"]
  ContainerCommandRequestMessage -->|submitted to| RaftClient
  ServerNotLeaderException -->|thrown by| RaftClient
```

## Class table

### Sub-feature: `hdds.ratis`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 286 | `org.apache.hadoop.hdds.ratis.RatisHelper` | service | logic-heavy | 450~ | 60 | Ratis helper methods. |
| 287 | `org.apache.hadoop.hdds.ratis.ContainerCommandRequestMessage` | service | mixed | 75~ | 30 | Implementing the Message interface for ContainerCommandRequestProto. |
| 288 | `org.apache.hadoop.hdds.ratis.ServerNotLeaderException` | exception | data-only | 75~ | 10 | Exception thrown when a server is not a leader for Ratis group. |

### Sub-feature: `ratis.conf`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 289 | `org.apache.hadoop.hdds.ratis.conf.RatisClientConfig` | config | data-only | 175~ | 20 | Configuration related to Ratis Client. |

### Sub-feature: `ratis.retrypolicy`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 290 | `org.apache.hadoop.hdds.ratis.retrypolicy.RetryPolicyCreator` | interface | mixed | 25~ | 20 | The interface of RetryLimited policy creator. |
| 291 | `org.apache.hadoop.hdds.ratis.retrypolicy.RequestTypeDependentRetryPolicyCreator` | service | mixed | 50~ | 30 | Table mapping exception type to retry policy used for the exception in write and watch request. |
| 292 | `org.apache.hadoop.hdds.ratis.retrypolicy.RetryLimitedPolicyCreator` | service | mixed | 25~ | 30 | The creator of RetryLimited policy. |

### Sub-feature: `scm.ha`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 293 | `org.apache.hadoop.hdds.scm.ha.SCMNodeInfo` | dto | data-only | 150~ | 10 | Class which builds SCM Node Information. |
| 294 | `org.apache.hadoop.hdds.scm.ha.RetriableWithNoFailoverException` | exception | data-only | 25~ | 10 | This exception indicates that the request can be retried, but only on the same server, without failover. |
| 295 | `org.apache.hadoop.hdds.scm.ha.RetriableWithFailOverException` | exception | data-only | 25~ | 10 | This exception indicates that the request can be retried, and client need to retry on the next server. |
| 296 | `org.apache.hadoop.hdds.scm.ha.NonRetriableException` | exception | data-only | 25~ | 10 | exception for which there should be no retry. |



## Anchor details

### `RatisHelper`

- **path:** `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/ratis/RatisHelper.java`
- **loc:** 450~    **difficulty:** 5    **study:** 60 min    **concurrency:** single-threaded    **persistence:** in-memory
- **key collaborators:** `org.apache.hadoop.hdds.ratis.conf.RatisClientConfig`, `org.apache.hadoop.hdds.ratis.retrypolicy.RetryPolicyCreator`, `org.apache.hadoop.hdds.HddsConfigKeys`, `org.apache.hadoop.hdds.HddsUtils`, `org.apache.hadoop.hdds.conf.ConfigurationSource`, `org.apache.hadoop.hdds.conf.OzoneConfiguration`
- **test exemplar:** `hadoop-hdds/common/src/test/java/org/apache/hadoop/hdds/ratis/TestRatisHelper.java`
- **role:** Ratis helper methods. `newRaftClient()` assembles a `RaftClient` wiring together the `RaftGroup` derived from `Pipeline`, the retry policy from `RequestTypeDependentRetryPolicyCreator`, and timeouts from `RatisClientConfig`. HDDS-15444 adjusted the default Ratis client retry and timeout values here to reduce false-positive leader-not-found errors under transient GC pauses. HDDS-15768 added bracket escaping in `toRaftPeer()` for IPv6 literal addresses.


## Design docs

- `hadoop-hdds/docs/content/design/scmha.md` — SCM HA Raft group topology that `RatisHelper` serves
- `hadoop-hdds/docs/content/design/omha.md` — OM HA Ratis setup; same `RatisHelper` factory used
- `hadoop-hdds/docs/content/design/listener-om.md` — Listener OM support (HDDS-11523) requires distinguishing listener peers in `RatisHelper`

## Seminal JIRAs / PRs

- HDDS-15768. Bracket IPv6 literals in Ratis peer addresses
- HDDS-15444. Adjusted Ratis client retry, timeout configs
- HDDS-13739. Make error messages less verbose for failed PutBlock or WriteChunk
- HDDS-11523. Support Listener OM
- HDDS-12772. Configure initial heartbeat and first election time for quicker MiniOzoneCluster startup

## Sharp edges

- `RatisHelper.newRaftClient()` produces a new `RaftClient` instance on every call; callers are responsible for closing it. Failure to close leaks a Netty event-loop thread group and associated off-heap buffers (related to HDDS-13739 verbose error noise masking close failures).
- IPv6 peer address handling added in HDDS-15768 is applied only in `RatisHelper.toRaftPeer()`; code paths that construct peer addresses outside `RatisHelper` will still break on IPv6 hosts.

## Related features

- [`pipeline-common.md`](pipeline-common.md) — `Pipeline` is the Ozone input to `RatisHelper.toRaftGroup()`
- [`protocol-common.md`](protocol-common.md) — `DatanodeDetails` ports are read by `RatisHelper` to build `RaftPeer` addresses
- [`scm-common.md`](scm-common.md) — SCM HA uses `RatisHelper` for its own Raft group
- [`container-common.md`](container-common.md) — container write pipeline submits `ContainerCommandRequestMessage` via `RaftClient`
- [`config-runtime.md`](config-runtime.md) — `RatisClientConfig` is a typed config bound from `OzoneConfiguration`
- [`tracing-common.md`](tracing-common.md) — distributed tracing spans cross the Ratis client boundary

## Self-quiz

1. What three Ratis objects does `RatisHelper` build from an Ozone `Pipeline`, and which method on `Pipeline` provides the node list?
2. Describe the three exception types in the HA retry hierarchy (`RetriableWithFailOverException`, `RetriableWithNoFailoverException`, `NonRetriableException`) and give a concrete example of when each is thrown.
3. What change did HDDS-15768 make to `RatisHelper.toRaftPeer()`, and why was it necessary?
4. `ContainerCommandRequestMessage` implements which Ratis interface, and what does its `getContent()` method return?
5. `RatisClientConfig` is a typed config class. Which Ozone configuration prefix does it bind to, and which two timeout values were adjusted by HDDS-15444?

<details>
<summary>Answers</summary>

Answer 1: `RatisHelper` builds a `RaftGroupId` (from `PipelineID`), a list of `RaftPeer` objects (one per `DatanodeDetails`), and a `RaftGroup` combining the two. The node list comes from `Pipeline.getNodes()`.
Answer 2: `RetriableWithFailOverException` is thrown when the client should retry on a different server (e.g., `ServerNotLeaderException` — Ratis redirected to a non-leader); `RetriableWithNoFailoverException` when the same server should be retried without switching (e.g., transient overload); `NonRetriableException` when the error is permanent and retrying is pointless (e.g., container not found).
Answer 3: HDDS-15768 added `[` and `]` brackets around IPv6 address literals when forming the `host:port` string for `RaftPeer`. Without brackets, Ratis's address parser misinterprets the colons in an IPv6 address as port delimiters.
Answer 4: `ContainerCommandRequestMessage` implements `org.apache.ratis.protocol.Message`. Its `getContent()` returns the serialized bytes of `ContainerCommandRequestProto` as a `ByteString`, which Ratis forwards to the state machine's `applyTransaction` on the leader.
Answer 5: `RatisClientConfig` binds to the `hdds.ratis.client` configuration prefix. HDDS-15444 adjusted `rpc.request.timeout` and `rpc.watch.timeout` to reduce spurious `TimeoutIOException` failures during GC pauses and slow followers.

</details>
