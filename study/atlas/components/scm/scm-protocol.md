# SCM / scm-protocol

**Classes:** 5    **Kinds:** rpc-stub:4, service:1

## Overview

The scm-protocol feature contains the server-side Protobuf translators that bridge the gRPC/Hadoop-RPC wire protocol to the internal SCM service interfaces. `StorageContainerLocationProtocolServerSideTranslatorPB` handles client requests (pipeline and container queries, safe mode, container balancer control). `ScmBlockLocationProtocolServerSideTranslatorPB` handles block allocation and deletion requests from OM. `SCMSecurityProtocolServerSideTranslatorPB` handles certificate sign requests. `SecretKeyProtocolServerSideTranslatorPB` handles secret key fetch requests from datanodes. Each translator deserialises the incoming proto, calls the corresponding method on the backing protocol server (`SCMClientProtocolServer`, `SCMBlockProtocolServer`, `SCMSecurityProtocolServer`), and serialises the response. `RetriableDatanodeEventWatcher` is a utility that pairs start events with completion events and handles timeout-based retry for datanode-targeted operations.

## Diagram

```mermaid
sequenceDiagram
  participant Client
  participant StorageContainerLocationProtocolServerSideTranslatorPB
  participant SCMClientProtocolServer
  participant PipelineManagerImpl
  Client->>StorageContainerLocationProtocolServerSideTranslatorPB: listPipelines(ListPipelineRequestProto)
  StorageContainerLocationProtocolServerSideTranslatorPB->>SCMClientProtocolServer: listPipelines()
  SCMClientProtocolServer->>PipelineManagerImpl: getPipelines()
  PipelineManagerImpl-->>SCMClientProtocolServer: List&lt;Pipeline&gt;
  SCMClientProtocolServer-->>StorageContainerLocationProtocolServerSideTranslatorPB: ListPipelineResponseProto
  StorageContainerLocationProtocolServerSideTranslatorPB-->>Client: response
```

## Class table

### Sub-feature: `protocol.commands`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1481 | `org.apache.hadoop.ozone.protocol.commands.RetriableDatanodeEventWatcher` | service | mixed | 25~ | 30 | EventWatcher for start events and completion events with payload of type RetriablePayload and RetriableCompletionPayl... |

### Sub-feature: `scm.protocol`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 1482 | `org.apache.hadoop.hdds.scm.protocol.StorageContainerLocationProtocolServerSideTranslatorPB` | rpc-stub | logic-heavy | 1175~ | 20 | This class is the server-side translator that forwards requests received on StorageContainerLocationProtocolPB to the... |
| 1483 | `org.apache.hadoop.hdds.scm.protocol.SCMSecurityProtocolServerSideTranslatorPB` | rpc-stub | logic-heavy | 250~ | 20 | This class is the server-side translator that forwards requests received on SCMSecurityProtocolPB to the SCMSecurityP... |
| 1484 | `org.apache.hadoop.hdds.scm.protocol.ScmBlockLocationProtocolServerSideTranslatorPB` | rpc-stub | logic-heavy | 200~ | 20 | This class is the server-side translator that forwards requests received on StorageContainerLocationProtocolPB to the... |
| 1485 | `org.apache.hadoop.hdds.scm.protocol.SecretKeyProtocolServerSideTranslatorPB` | rpc-stub | data-only | 100~ | 20 | This class is the server-side translator that forwards requests received on SecretKeyProtocolDatanodePB to the server... |



## Anchor details

_No logic-heavy anchors in this feature; the classes are primarily data / dto / config / cli._

## Design docs

- no dedicated design doc under hadoop-hdds/docs/content/ on this branch

## Seminal JIRAs / PRs

- HDDS-14103. Create an option to suppress/unsuppress containers from report — adds new RPC to `StorageContainerLocationProtocolServerSideTranslatorPB`.
- HDDS-14618. Support including only specified containers in Container Balancer — extends balancer RPC.
- HDDS-3262133beeb HDDS-14618 also adds include/exclude container list support to the translator.

## Sharp edges

- `StorageContainerLocationProtocolServerSideTranslatorPB` at ~1175 LOC is one of the largest translator classes in SCM; it is effectively a boilerplate fan-out class. Adding a new protocol method requires changes in the Protobuf definition, this translator, the `SCMClientProtocolServer`, and the `SCMClientProtocol` interface — four separate files for one new operation.

## Related features

- `components/scm/scm-server.md` — `SCMClientProtocolServer`, `SCMBlockProtocolServer`, and `SCMSecurityProtocolServer` are the backing services for these translators
- `components/scm/scm-ha.md` — translators on a follower SCM forward mutating requests to the leader via `SCMRatisServerImpl`

## Self-quiz

1. `StorageContainerLocationProtocolServerSideTranslatorPB` is marked `rpc-stub` in the atlas. What does this kind mean, and what does this class do that a pure stub would not?
2. `ScmBlockLocationProtocolServerSideTranslatorPB` handles `allocateBlock`. On a follower SCM, what happens when this translator receives an allocate request?
3. `RetriableDatanodeEventWatcher` watches for start and completion events. What determines the retry timeout, and what happens when the timeout expires?
4. `SCMSecurityProtocolServerSideTranslatorPB` handles `getCertificate`. Where does the actual certificate store lookup happen — in the translator or in the backing server?
5. Adding a new SCM admin command requires changes in how many distinct Java files, and which four are they?

<details>
<summary>Answers</summary>

Answer 1: `rpc-stub` means the class is primarily boilerplate proto serialization/deserialization. Unlike a pure stub, it also handles error mapping (converting Java exceptions to proto error codes) and audit logging via the backing server.
Answer 2: On a follower, `SCMBlockProtocolServer.allocateBlock()` checks `scmContext.isLeader()` and, if false, throws `NotLeaderException`. The gRPC framework converts this to a `StatusRuntimeException` with `UNAVAILABLE` status, and the client retries on the leader.
Answer 3: The retry timeout is configured via `ozone.scm.event.timeout` (or similar). When the timeout expires, `RetriableDatanodeEventWatcher` fires a timeout event to the registered handler, which may retry the command or report failure.
Answer 4: The actual lookup happens in `SCMSecurityProtocolServer`, which delegates to `SCMCertStore`. The translator only handles serialization.
Answer 5: Four files: (1) the Protobuf `.proto` file, (2) the translator class (`StorageContainerLocationProtocolServerSideTranslatorPB`), (3) the protocol server (`SCMClientProtocolServer`), and (4) the protocol interface (`SCMClientProtocol` or `StorageContainerLocationProtocol`).

</details>
