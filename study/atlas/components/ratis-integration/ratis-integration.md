# Ratis-integration / ratis-integration

**Classes:** 4    **Kinds:** interface:1, service:1, data:1, metrics:1

## Overview

This feature group collects the low-level glue between SCM's HA layer and Apache Ratis. `SCMHandler` is a single-method interface that every SCM request handler must implement; it binds each handler to one `RequestType` enum value so the SCM state machine can dispatch Raft-applied commands to the right handler. `SCMHAUtils` is the runtime companion: a static utility class that encodes retry policy decisions (which exceptions trigger failover vs. in-place retry vs. hard failure), resolves the primordial SCM node, and maps configuration keys to Ratis storage and snapshot directories. `SequenceIdType` is the RocksDB-persisted enum used by `SequenceIdGenerator` to identify monotonic counter slots (`localId`, `delTxnId`, `containerId`, `CertificateId`); its embedded `Codec` serialises enum names to bytes using only the first distinct byte as a lookup key. `RatisMetricsUtils` is a thin adapter interface that bridges Ratis's `RatisMetricRegistry` to the Dropwizard 3 `MetricRegistry` and exposes JMX reporter lifecycle hooks.

## Diagram

```mermaid
classDiagram
  class SCMHandler {
    <<interface>>
    +getType() RequestType
  }
  class SCMHAUtils {
    <<utility>>
    +getPrimordialSCM(conf) String
    +isRetriableWithNoFailoverException(e) boolean
    +checkNonRetriableException(e) boolean
    +getRetryAction(failovers, retry, e, max, interval) RetryAction
    +getSCMRatisDirectory(conf) String
    +getSCMRatisSnapshotDirectory(conf) String
  }
  class SequenceIdType {
    <<enum>>
    localId
    delTxnId
    containerId
    CertificateId
    rootCertificateId
    +getCodec() Codec~SequenceIdType~
    +getByteArray() byte[]
  }
  class RatisMetricsUtils {
    <<interface>>
    +getDropWizardMetricRegistry(r) MetricRegistry
    +jmxReporter() Consumer~RatisMetricRegistry~
    +stopJmxReporter() Consumer~RatisMetricRegistry~
  }
  SCMHAUtils ..> SCMHandler : classifies handlers via RequestType
  SequenceIdType --> "Codec" : embeds
```

## Class table

### Sub-feature: `metrics.dropwizard3`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 282 | `org.apache.ratis.metrics.dropwizard3.RatisMetricsUtils` | metrics | mixed | 25~ | 20 | Utilities for ratis metrics dropwizard3. |

### Sub-feature: `scm.ha`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 283 | `org.apache.hadoop.hdds.scm.ha.SCMHandler` | interface | mixed | 25~ | 20 | Base interface for SCM handlers participating in Ratis-based HA. |
| 284 | `org.apache.hadoop.hdds.scm.ha.SCMHAUtils` | service | mixed | 150~ | 45 | Utility class used by SCM HA. |
| 285 | `org.apache.hadoop.hdds.scm.ha.SequenceIdType` | data | data-only | 75~ | 10 | Represents the sequence ID types managed by org.apache.hadoop.hdds.scm.ha.SequenceIdGenerator The enum constant names... |



## Anchor details

No class in this feature is marked `logic-heavy`; the most substantial logic lives in `SCMHAUtils`. Key observations:

- **SCMHAUtils.getRetryAction**: the method contains a four-branch decision tree that checks, in order, `AccessControlException`, `shouldNotFailoverOnRpcException`, `checkRetriableWithNoFailoverException`, `checkNonRetriableException`, then defaults to `FAILOVER_AND_RETRY`. Callers that skip this method and hardcode retry logic will silently diverge from the canonical policy.
- **SCMHAUtils.unwrapException / getExceptionForClass**: both methods walk the cause chain, but `getExceptionForClass` additionally unwraps `RemoteException` from the Hadoop RPC layer before walking — omitting that step causes `NotLeaderException` detection to fail in remote-call paths.
- **SequenceIdType (embedded Codec)**: the codec uses only the first byte of each enum name as the lookup key (`SEQUENCE_ID_TYPES` map). The static initializer enforces uniqueness at class-load time with an `IllegalStateException`; adding a new enum constant whose name starts with the same letter as an existing one will break startup.

## Design docs

- `hadoop-hdds/docs/content/design/scmha.md` — SCM HA design doc; covers the Ratis-backed state machine, handler dispatch, and sequence ID generation that this feature group implements.
- `hadoop-hdds/docs/content/design/omha.md` — OM HA design; provides useful contrast because OM and SCM share the same Ratis integration pattern.
- `hadoop-hdds/docs/content/design/upgrade-dev-primer.md` — relevant for understanding finalization hooks that interact with `SequenceIdType` persisted counters during layout upgrades.

## Seminal JIRAs / PRs

- HDDS-15370. Change sequenceIdTable to use SequenceIdType
- HDDS-14919. Add a base class for all the SCM handler interfaces
- HDDS-13866. Use component-specific default directory for Ratis
- HDDS-12351. Move SCMHAUtils and ServerUtils to hdds-server-framework
- HDDS-13753. Use forked Hadoop RPC
- HDDS-9192. Update Ratis to 3.0.0

## Sharp edges

- `SequenceIdType` enum constant names are their persisted RocksDB keys; renaming or reordering constants is a silent data-corruption hazard. The codec's first-byte lookup would silently decode a renamed constant to the wrong type. See `SequenceIdType.java` static initializer and the comment: "The enum constant names are kept exactly as their persisted RocksDB keys."
- `SCMHAUtils.isNonRetriableException` checks for `StateMachineException` by walking the cause chain but does NOT check the `NON_RETRIABLE_EXCEPTION_LIST`; that list is checked by the separate method `checkNonRetriableException`. Callers that use the wrong method will either silently retry non-retriable errors or fail-fast on retriable ones. See `SCMHAUtils.java` lines 149-167.
- `rootCertificateId` in `SequenceIdType` is `@Deprecated` in favour of `CertificateId`, but both constants still exist and both have active RocksDB entries in older clusters. Any code that generates a new `rootCertificateId` sequence ID rather than `CertificateId` after the migration point will silently create a diverging counter. (HDDS-15370 introduced the migration; verify upgrade finalization guards before touching this.)

## Related features

- `components/scm/scm-ha.md` — SCM HA service layer that directly implements `SCMHandler` and relies on `SCMHAUtils` for retry decisions.
- `components/scm/scm-metadata.md` — SCM RocksDB schema; the `sequenceIdTable` persists `SequenceIdType` keys.
- `components/om/om-ratis.md` — OM's parallel Ratis integration; same handler-dispatch pattern applied to OM.
- `components/scm/scm-server.md` — SCM server bootstrap, which wires `SCMHandler` implementations into the Ratis group.
- `components/interfaces/` — Protobuf `SCMRatisProtocol.RequestType` that `SCMHandler.getType()` returns.

## Self-quiz

1. `SCMHandler` declares a single method. What is it, what does it return, and why is this sufficient for dispatch in the SCM state machine?
2. `SCMHAUtils.getRetryAction` distinguishes three retry outcomes. Name them and the exception class that triggers each.
3. `SequenceIdType` persists to RocksDB using only the first byte of each enum name as a key. What invariant does the static initializer enforce, and what happens at runtime if it is violated?
4. `SCMHAUtils.getExceptionForClass` unwraps `RemoteException` before walking the cause chain. Why is this step necessary, and which two callers rely on it?
5. `RatisMetricsUtils` is an interface with only static methods. What Dropwizard type does `getDropWizardMetricRegistry` return, and which concrete class does it cast `RatisMetricRegistry` to internally?

<details>
<summary>Answers</summary>

Answer 1: `getType()` returns `SCMRatisProtocol.RequestType`. The SCM Ratis state machine maps each incoming command's `RequestType` to the matching `SCMHandler` implementation; a single-method interface is enough because all handler routing is done by this one enum value.

Answer 2: `FAILOVER_AND_RETRY` for unknown exceptions; `RETRY` (no failover) for `LeaderNotReadyException`, `ReconfigurationInProgressException`, `ReconfigurationTimeoutException`, `ResourceUnavailableException`; `FAIL` for `SCMException`, `NonRetriableException`, `PipelineNotFoundException`, `ContainerNotFoundException`, and for `AccessControlException` or RPC exceptions where retrying on another server would not help.

Answer 3: The static initializer builds `SEQUENCE_ID_TYPES` keyed by first byte and throws `IllegalStateException` if two constants share the same first byte. If violated, the JVM fails to load the class, which crashes the SCM at startup.

Answer 4: In Hadoop RPC, the server wraps application exceptions inside `RemoteException` before serialising them on the wire; unwrapping recovers the original exception class so `instanceof` checks on `NotLeaderException` or `StateMachineException` work. `getNotLeaderException` and `getServerNotLeaderException` both delegate to `getExceptionForClass`.

Answer 5: It returns `com.codahale.metrics.MetricRegistry`. Internally it casts the `RatisMetricRegistry` argument to `Dm3RatisMetricRegistryImpl` (a package-private class in the same package) and calls `getDropWizardMetricRegistry()` on it.

</details>
