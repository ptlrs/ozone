# HddsCommon / scm-common

**Classes:** 17    **Kinds:** service:6, dto:4, interface:3, config:3, exception:1

## Overview

The scm-common feature collects the shared DTOs, interfaces, config holders, and client utilities that span SCM server code, datanodes, and Ozone clients. `XceiverClientSpi` is the SPI that all container I/O clients implement; it abstracts over the Ratis and gRPC transport variants. `ContainerPlacementStatus` is the interface checked after container replication changes to verify that placement policy constraints are still satisfied. `ScmConfigKeys` is the authoritative constant-holder for every `ozone.scm.*` and `hdds.scm.*` key string, while `ScmConfig` and `ScmRatisServerConfig` are the typed `@ConfigGroup` POJOs. `ClientTrustManager` implements `javax.net.ssl.X509TrustManager` and uses a `CACertificateProvider` to verify SCM-issued certificates on gRPC channels. `ByteStringConversion` selects the zero-copy or copying `ByteBuffer`-to-`ByteString` conversion path based on an Ozone config key. DTO classes such as `AddSCMRequest`, `RemoveSCMRequest`, and `ScmInfo` carry the data for SCM HA membership operations.

## Diagram

```mermaid
classDiagram
  class XceiverClientSpi {
    <<interface>>
    +sendCommand()
    +close()
  }
  class XceiverClientReply {
    +getCommandResponse()
    +getLogIndex()
  }
  class StreamingReaderSpi {
    <<interface>>
    +setStreamingReadResponse(StreamingReadResponse)
  }
  class StreamingReadResponse {
    +datanodeDetails
    +requestObserver
  }
  class ContainerPlacementStatus {
    <<interface>>
    +isPolicySatisfied() bool
    +misReplicationCount() int
  }
  class ClientTrustManager {
    +checkServerTrusted()
  }
  class ByteStringConversion {
    +createByteBufferConversion() Function
  }
  class ScmConfigKeys {
    +OZONE_SCM_DEADNODE_INTERVAL
    +OZONE_SCM_HEARTBEAT_RPC_TIMEOUT
  }
  class ScmConfig {
    +serviceIds
    +serviceAddresses
  }
  class ScmRatisServerConfig {
    +requestTimeoutInMs
    +leaderReadyCheckIntervalInMs
  }
  class ScmInfo {
    +clusterId
    +scmId
  }
  class AddSCMRequest
  class RemoveSCMRequest
  XceiverClientSpi ..> XceiverClientReply : returns
  StreamingReaderSpi ..> StreamingReadResponse : sets
  ClientTrustManager --> "CACertificateProvider" : delegates trust check
  ScmConfig --> ScmConfigKeys : uses constants from
```

## Class table

### Sub-feature: `hdds.scm`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2038 | `org.apache.hadoop.hdds.scm.XceiverClientSpi` | abstract | mixed | 100~ | 20 | A Client for the storageContainer protocol. |
| 2039 | `org.apache.hadoop.hdds.scm.ContainerPlacementStatus` | interface | mixed | 25~ | 20 | Interface to allow container placement status to be queried to ensure a container meets its placement policy (number... |
| 2040 | `org.apache.hadoop.hdds.scm.StreamingReaderSpi` | interface | mixed | 25~ | 20 | SPI for streaming reader to set the streaming read response. |
| 2041 | `org.apache.hadoop.hdds.scm.XceiverClientReply` | service | mixed | 25~ | 30 | This class represents the reply from XceiverClient. |
| 2042 | `org.apache.hadoop.hdds.scm.DatanodeAdminError` | service | mixed | 25~ | 30 | Simple class to wrap a datanode admin host and error message. |
| 2043 | `org.apache.hadoop.hdds.scm.ByteStringConversion` | service | mixed | 25~ | 30 | Helper class to create a conversion function from ByteBuffer to ByteString based on the property OzoneConfigKeys#OZON... |
| 2044 | `org.apache.hadoop.hdds.scm.PipelineRequestInformation` | service | mixed | 25~ | 30 | The information of the request of pipeline. |
| 2045 | `org.apache.hadoop.hdds.scm.ScmConfigKeys` | config | logic-heavy | 450~ | 20 | This class contains constants for configuration keys used in SCM. |
| 2046 | `org.apache.hadoop.hdds.scm.ScmConfig` | config | data-only | 150~ | 20 | The configuration class for the SCM service. |
| 2047 | `org.apache.hadoop.hdds.scm.ScmRatisServerConfig` | config | data-only | 25~ | 20 | SCM Ratis Server config. |
| 2048 | `org.apache.hadoop.hdds.scm.ScmInfo` | dto | data-only | 50~ | 10 | ScmInfo wraps the result returned from SCM#getScmInfo which contains clusterId and the SCM Id. |
| 2049 | `org.apache.hadoop.hdds.scm.AddSCMRequest` | dto | data-only | 50~ | 10 | Class for ADD SCM request to be sent by Bootstrapping SCM to existing leader SCM. |
| 2050 | `org.apache.hadoop.hdds.scm.StreamingReadResponse` | dto | data-only | 25~ | 10 | Streaming read response holding datanode details and request observer to send read requests. |
| 2051 | `org.apache.hadoop.hdds.scm.RemoveSCMRequest` | dto | data-only | 25~ | 10 | Request class using which SCM can be removed form the HA Ring. |

### Sub-feature: `scm.client`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2052 | `org.apache.hadoop.hdds.scm.client.ClientTrustManager` | service | mixed | 125~ | 30 | A javax.net.ssl.TrustManager implementation for gRPC and Ratis clients. |

### Sub-feature: `scm.exceptions`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2053 | `org.apache.hadoop.hdds.scm.exceptions.SCMException` | exception | data-only | 75~ | 10 | Exception thrown by SCM. |

### Sub-feature: `scm.utils`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2054 | `org.apache.hadoop.hdds.scm.utils.ClientCommandsUtils` | service | mixed | 25~ | 30 | These methods should be merged with other similar utility classes. |



## Anchor details

_No logic-heavy anchors in this feature; the classes are primarily data / dto / config / cli._

## Design docs

- `hadoop-hdds/docs/content/design/scmha.md` — SCM HA membership operations (AddSCMRequest, RemoveSCMRequest are the DTOs for these operations).
- `hadoop-hdds/docs/content/design/topology.md` — network topology used by placement policies that check `ContainerPlacementStatus`.

## Seminal JIRAs / PRs

- HDDS-15535. Container Balancer should validate configuration and report startup failures
- HDDS-15533. DNS refresh on heartbeat failure for DN to SCM
- HDDS-15634. Avoid updating container delete transaction ID on SCM delete log append
- HDDS-15581. ozone debug replicas chunk-info reports wrong block size for EC keys
- HDDS-15093. Make RackScatter intra-rack placement capacity-aware
- HDDS-12563. Cache and reuse ContainerID object

## Sharp edges

- `ByteStringConversion` chooses the zero-copy UnsafeByteOperations path only when `ozone.unsafeByteBufferConversion` is true; enabling it without compatible Protobuf-compatible native libraries on the classpath can cause silent data corruption.
- `ScmConfigKeys` contains constants for both the deprecated `ozone.scm.*` prefix and the current `hdds.scm.*` prefix; callers that read only one prefix will silently miss overrides set under the other (HDDS-15533 context).

## Related features

- [`scm-client-proxy.md`](scm-client-proxy.md) — proxy providers that use `ScmConfigKeys` constants and `ScmInfo` results
- [`pipeline-common.md`](pipeline-common.md) — pipelines linked to the xceivers that implement `XceiverClientSpi`
- [`container-common.md`](container-common.md) — container lifecycle that checks `ContainerPlacementStatus`
- [`security-common.md`](security-common.md) — `SecurityConfig` consumed by `ClientTrustManager`
- [`network-topology.md`](network-topology.md) — topology abstractions consulted by placement policies returning `ContainerPlacementStatus`

## Self-quiz

1. `XceiverClientSpi` is an interface. Name one concrete implementation in the codebase (outside this feature group) and describe which transport it uses: Ratis or standalone gRPC?
2. `ContainerPlacementStatus.isPolicySatisfied()` returns a boolean. What class in the SCM server calls this method to decide whether a container needs re-replication?
3. `ByteStringConversion` selects between two conversion paths at runtime. What config key controls the selection, and where is that key defined?
4. `ScmInfo` carries a `clusterId` and `scmId`. Which RPC call returns a `ScmInfo`, and what is it used for on the client side?
5. `ClientTrustManager` delegates certificate validation to a `CACertificateProvider`. In what scenario is a custom `CACertificateProvider` injected rather than the default one?

<details>
<summary>Answers</summary>

Answer 1: `XceiverClientRatis` (in `hadoop-hdds/container-service`) implements `XceiverClientSpi` over the Ratis transport; `XceiverClientGrpc` implements it over a direct gRPC channel (standalone pipeline).
Answer 2: `ReplicationManager` in the SCM server evaluates `ContainerPlacementStatus` via the configured placement policy to determine which containers are under- or mis-replicated.
Answer 3: The key is `OzoneConfigKeys.OZONE_UNSAFEBYTEBUFFERCONVERSION` (defined in `hadoop-ozone/common`); `ByteStringConversion.createByteBufferConversion()` reads it at construction time.
Answer 4: `StorageContainerLocationProtocol.getScmInfo()` returns the `ScmInfo`; clients use it to verify cluster identity on first connection and to detect accidental cross-cluster writes.
Answer 5: A custom `CACertificateProvider` is injected when a component (e.g., datanode) has loaded its own certificate chain from disk and the trust anchor is the locally cached SCM root CA, rather than the system trust store.

</details>
