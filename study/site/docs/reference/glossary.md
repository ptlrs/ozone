---
slug: /reference/glossary
sidebar_label: "Glossary"
format: md
---

# Glossary

## Services / roles

| Term | Meaning |
|---|---|
| OM | Ozone Manager — owns namespace (volumes, buckets, keys), snapshots, S3 secrets, most user-visible metadata. Runs Ratis for HA. |
| SCM | Storage Container Manager — owns containers, pipelines, block allocation, replication, safemode. Runs Ratis for HA. |
| DN | Datanode — serves container data, chunk I/O, participates in Ratis pipelines. |
| Recon | Observability and derived-metadata service; runs its own OM/SCM read replica, tasks, and REST API. |
| S3G | S3 Gateway — S3-compatible REST façade over OM. |
| OzoneFS / OFS / O3FS | Hadoop-compatible FileSystem clients over Ozone. `ofs://` is root-mounted; `o3fs://` is bucket-mounted. |
| HttpFS | REST proxy over the OzoneFS client. |
| CSI | Kubernetes Container Storage Interface plugin. |
| Ratis | Apache Ratis — Java Raft implementation used by OM, SCM, and DN pipelines. |

## Storage vocabulary

| Term | Meaning |
|---|---|
| Volume | Top-level namespace object. Owner + quotas. |
| Bucket | Container of keys; carries layout (FSO / OBS / LEGACY), encryption, versioning, replication config. |
| Key | Object; either an OBS-style flat key or an FSO-style file. |
| FSO | File System Optimized bucket layout. Uses `dirTable` + `fileTable` with prefix ids for path resolution. |
| OBS | Object Store bucket layout. Uses `keyTable` with full-path keys. |
| LEGACY | Pre-FSO/OBS bucket layout (mixed semantics). |
| Container | Unit of replication managed by SCM; a fixed-size holder of blocks on datanodes. |
| Pipeline | Ratis group (or standalone) of datanodes across which a container is replicated. RATIS/THREE, RATIS/ONE, EC. |
| Block | Ordered set of chunks; addressed by `{containerId, localId}`. |
| Chunk | On-disk range within a container's block file; addressed by offset + length. |
| Container v3 / Schema v3 | Current on-disk container format: block metadata in a per-DN RocksDB (column-family-per-container). |
| EC | Erasure-coded replication (e.g. RS-3-2-1024k, RS-6-3-1024k). |
| Snapshot | Immutable point-in-time copy of a bucket. |
| Snapshot diff | The delta between two snapshots computed by SST-file diffing. |

## Consensus / apply-loop vocabulary

| Term | Meaning |
|---|---|
| StateMachine | Ratis state-machine callback surface. OM has one; each DN Ratis pipeline has one per container group. |
| Double buffer | OM's write-side accumulator that batches applied transactions before flushing to RocksDB. |
| OMClientRequest | Server-side request handler encoding the pre-execute / validate / apply lifecycle. |
| Snapshot install | Follower catch-up by receiving a full state snapshot from the leader. |

## RocksDB tables (OM)

`volumeTable`, `bucketTable`, `keyTable`, `fileTable`, `dirTable`, `openKeyTable`, `openFileTable`, `deletedTable`, `deletedDirTable`, `multipartInfoTable`, `s3SecretTable`, `prefixTable`, `snapshotInfoTable`, `snapshotRenamedTable`, `delegationTokenTable`, `principalToAccessIdsTable`, `tenantStateTable`, `tenantAccessIdTable`, `meta`.

## Background services

Replication Manager (SCM), Container Balancer (SCM), Disk Balancer (DN), Container Scanner (DN, data + metadata + on-demand), Merkle-tree Reconciliation, Key Deleting Service (OM), Directory Deleting Service (OM), Snapshot Deep-clean (OM), Snapshot Diff Cleanup, SST Filtering, Open Key Cleanup, Multipart Upload Cleanup.
