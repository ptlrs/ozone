# Prerequisites

External concepts to skim before Week 1. Each entry lists a "just enough" reading target and why it matters for Ozone.

## Consensus

- **Raft** — read Diego Ongaro's Raft paper §5. Focus on: leader election, log replication, safety property, snapshot install.
  Why: Ratis (OM, SCM, DN pipelines) implements Raft.

## Storage engine

- **LSM trees / RocksDB** — Facebook RocksDB overview + column-family concept.
  Why: OM metadata, DN Container v3 block metadata, SCM metadata all sit on RocksDB. `managed-rocksdb` and Container Schema v3 assume you know CFs.

## RPC & wire

- **Protobuf 2 syntax** — read `descriptor.proto` and any `.proto` under `hadoop-hdds/interface-*/src/main/proto/`.
- **Hadoop RPC / ProtocolTranslatorPB pattern** — one client-side + one server-side translator wraps a Protobuf service. Every service in Ozone follows this shape.
- **gRPC (Netty)** — data-plane between client and DN uses gRPC; control-plane uses Hadoop RPC. Know the two paths.

## Erasure coding

- **Reed-Solomon fundamentals** — data blocks, parity blocks, systematic vs non-systematic codes.
- **Hadoop HDFS EC design doc** — Ozone reuses parts of the Hadoop `ErasureCoder` API surface.

## Security (skim only until Week 20)

- **Kerberos** — principals, keytabs, service tickets. Ozone uses `HADOOP_SECURITY_AUTHENTICATION=kerberos` in secure mode.
- **X.509 / mTLS basics** — SCM runs an internal CA that issues certificates to OM, DN, Recon.
- **Delegation tokens** — Hadoop-style token issuance for long-running clients.

## Filesystem semantics

- **`FileSystem` interface (Hadoop)** — `rename`, `atime`, `getFileStatus` semantics. FSO layout was designed to make `rename` and `delete` O(1) at the directory level.

## Recommended reading order

1. Raft §5 (30 min)
2. RocksDB CF overview (20 min)
3. One Ozone Protobuf file, e.g. `OzoneManagerProtocol.proto` (20 min)
4. Skim `hadoop-hdds/docs/content/` for existing design docs (30 min)

Total ~2 hours before Week 1.
