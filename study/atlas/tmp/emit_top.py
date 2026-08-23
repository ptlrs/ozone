#!/usr/bin/env python3
"""Emit atlas.json + top-level Markdown files from classes.jsonl."""
import json, os, re
from pathlib import Path
from collections import defaultdict, Counter

ROOT = Path('study/atlas')
rows = [json.loads(l) for l in open('study/atlas/tmp/classes.jsonl')]

# --- atlas.json ---
with open(ROOT / 'atlas.json', 'w') as f:
    json.dump(rows, f, indent=1)

# --- indexes for markdown emission ---
by_comp = defaultdict(list)
for r in rows: by_comp[r['component']].append(r)
by_feat = defaultdict(list)
for r in rows: by_feat[(r['component'], r['feature'])].append(r)

COMP_ORDER = [
    # priority-aligned ordering per user's TOP-LEVEL PRIORITIZATION
    ('Client',           'P0', 'client write/read paths (RPC, IO, EC, replication)'),
    ('OM',               'P0 / P1', 'namespace, keys/buckets/volumes, snapshots, Ratis apply'),
    ('SCM',              'P0 / P1', 'containers, pipelines, replication mgr, HA, safemode'),
    ('DN',               'P0 / P1', 'container v3, chunk manager, RocksDB, Ratis state machine, scanner, disk balancer'),
    ('Ratis-integration','P0', 'Ratis client/server helpers shared by OM and SCM'),
    ('Security',         'P2', 'certificates (x509), tokens, symmetric secrets, SSL helpers'),
    ('RocksDB',          'P1', 'managed-rocksdb wrappers, checkpoint differ, native RocksDB'),
    ('HddsCommon',       'P1', 'framework, protobuf-common, config, tracing, HTTP, DB utils'),
    ('OzoneCommon',      'P1', 'shared om-helpers, s3, snapshot, ozone-fs common types'),
    ('Recon',            'P2', 'observability, derived views, background tasks'),
    ('Interfaces',       'P3', 'S3 gateway, HttpFS, OzoneFS, Iceberg, multi-tenancy'),
    ('Admin CLIs',       'P3', 'ozone admin + ozone sh + shell scaffolding'),
    ('Debug & Repair',   'P3', 'ozone debug (ldb, container, replicas) and ozone repair'),
    ('Bench & Insight',  'P3', 'freon, insight, vapor, tools'),
]
assert set(c for c,_,_ in COMP_ORDER) == set(by_comp.keys()), set(by_comp.keys()) ^ set(c for c,_,_ in COMP_ORDER)

# --- README.md ---
(ROOT / 'README.md').write_text('''# Class Study Atlas — Apache Ozone

This atlas is the single source of truth for the "learn Ozone in 6 months" plan.

## Layout

- `README.md`           — this file (how to use the atlas, legend, generation pipeline)
- `GLOSSARY.md`         — Ozone acronyms and vocabulary
- `PREREQUISITES.md`    — external concepts to skim before Week 1
- `REPO_MAP.md`         — module layout, `hadoop-hdds/*` vs `hadoop-ozone/*`, dependency direction
- `ENTRYPOINTS.md`      — every daemon `main()` and CLI entry point with a call-stack starter map
- `PROTOBUF_MAP.md`     — the hot Protobuf definitions and the Java classes that ser/de/dispatch them
- `CONFIG_KEYS.md`      — index of `ozone.*` configuration keys and their consumers
- `METRICS.md`          — one-liner per `*Metrics` class, grouped by component
- `DESIGN_DOCS.md`      — pointers into `hadoop-hdds/docs/` and seminal JIRAs per feature
- `UPGRADES.md`         — cross-feature upgrade & finalization landmines
- `INDEX.md`            — full hierarchical TOC + component-relationship diagram
- `SCHEDULE.md`         — 26-week plan (Gantt + per-week class lists)
- `PROGRESS.md`         — daily checkbox log + cumulative-progress chart
- `GAPS.md`             — classes/features that could not be confidently classified
- `components/<comp>/index.md`             — component overview + feature index
- `components/<comp>/<feature>.md`         — one file per feature (overview, mermaid, class table, JIRAs, sharp edges, quiz)
- `atlas.json`          — machine-readable copy of every class row

## Class row legend

Every class row across the atlas carries the same field set:

| Field | Meaning |
|---|---|
| `fqcn` | fully-qualified class name |
| `path` | repo-relative Java source path |
| `loc_total` | total lines in the file |
| `loc_code` | non-blank, non-comment, non-import, non-package lines (rounded to nearest 25, `~` suffix) |
| `kind` | one of `data`, `dto`, `config`, `interface`, `abstract`, `service`, `state-machine`, `coordinator`, `algorithm`, `util`, `factory`, `rpc-stub`, `metrics`, `cli`, `exception` |
| `logic_weight` | `data-only` \\| `mixed` \\| `logic-heavy` |
| `role_one_liner` | active-voice, ≤120 chars, what the class DOES |
| `key_collaborators` | 3–7 fqcns it calls or is called by |
| `entry_points` | methods that are the "start here" for readers |
| `invariants` | 0–3 bullets on what must always hold |
| `concurrency` | `single-threaded` \\| `thread-safe` \\| `externally synchronized` \\| `actor/queue` \\| `ratis-applied` |
| `persistence` | RocksDB table / on-disk file / `in-memory` |
| `test_exemplar` | the ONE test class that best teaches this class |
| `difficulty` | 1–5 (1 = 15 min; 5 = multi-day) |
| `study_minutes` | rough budget to reach working understanding |
| `prereq_fqcns` | classes to read FIRST |
| `read_order_hint` | integer within feature group (1 = read first) |
| `sharp_edges` | 0–2 bullets on production landmines / bugs that shaped this class |

## How to use daily

1. Open `SCHEDULE.md`, find today (Wnn Dn).
2. Read the 1–3 listed classes; consult the feature file for context.
3. Tick the boxes in `PROGRESS.md`.
4. On Friday, do the weekly recap (no new classes).
5. Milestones (M1..M7) live in `SCHEDULE.md`.

## Priority tiers

| Tier | Meaning |
|---|---|
| P0 | client write path, client read path, consensus path (Ratis, OM apply, DN apply) |
| P1 | metadata management, storage engine |
| P2 | background jobs, security |
| P3 | interfaces, ops & tooling |

## How the atlas was generated

1. `study/atlas/tmp/enumerate.sh` sweeps `hadoop-hdds/` and `hadoop-ozone/` for production `.java` files. Excludes `src/test/**`, `target/**`, `generated-sources/**`, `mini-cluster/**`, `test-utils/**`, and `package-info.java`.
2. `study/atlas/tmp/classify_v2.py` derives `component`, `feature`, `kind`, `logic_weight`, `concurrency`, `persistence`, `role_one_liner`, `difficulty`, `study_minutes` deterministically from the source. This produces `study/atlas/tmp/classes.jsonl`.
3. Anchor classes (top 20% by lines within each feature, plus every state-machine) are flagged in `study/atlas/tmp/anchors.tsv` for deep enrichment.
4. Explore subagents (one per component) enrich the anchor rows with `key_collaborators`, `entry_points`, `invariants`, `sharp_edges`, `prereq_fqcns`, `read_order_hint` and author each `<feature>.md` narrative (mermaid diagram, JIRAs, quiz).
5. `study/atlas/tmp/emit_top.py` renders `atlas.json` and every Markdown file from the merged data.

To regenerate after a repo update:

```bash
bash study/atlas/tmp/enumerate.sh
python3 study/atlas/tmp/classify_v2.py
python3 study/atlas/tmp/emit_top.py
```

## Rules the atlas obeys

- No hallucinated fqcns. Every row is a file that exists on this branch.
- Every claim about a class is derived from a file the classifier or Explore agent read; inferred fields are prefixed `inferred:`.
- Every mermaid diagram references real fqcns from `atlas.json`.
- JIRA / PR references not verified against Apache JIRA are marked `TODO(verify)`.
- No emoji, no marketing tone, Markdown per repo conventions.
''')

# --- GLOSSARY.md ---
(ROOT / 'GLOSSARY.md').write_text('''# Glossary

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
''')

# --- PREREQUISITES.md ---
(ROOT / 'PREREQUISITES.md').write_text('''# Prerequisites

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
''')

# --- REPO_MAP.md ---
comp_lines = []
for comp, tier, blurb in COMP_ORDER:
    n = len(by_comp[comp])
    comp_lines.append(f'| {comp} | {tier} | {n} | {blurb} |')
(ROOT / 'REPO_MAP.md').write_text(f'''# Repository Map

## Two aggregators

- `hadoop-hdds/` — storage layer and shared infrastructure. Modules cannot depend on `hadoop-ozone/`.
- `hadoop-ozone/` — Ozone services and clients. Modules here depend on `hadoop-hdds/*`.

## Module dependency direction

```mermaid
graph LR
    annot[hadoop-hdds/annotations]
    cfg[hadoop-hdds/config]
    common[hadoop-hdds/common]
    ifadmin[hadoop-hdds/interface-admin]
    ifclient[hadoop-hdds/interface-client]
    ifserver[hadoop-hdds/interface-server]
    mrdb[hadoop-hdds/managed-rocksdb]
    rdiff[hadoop-hdds/rocksdb-checkpoint-differ]
    fw[hadoop-hdds/framework]
    hclient[hadoop-hdds/client]
    scm[hadoop-hdds/server-scm]
    dn[hadoop-hdds/container-service]
    ec[hadoop-hdds/erasurecode]

    ocommon[hadoop-ozone/common]
    oclient[hadoop-ozone/client]
    om[hadoop-ozone/ozone-manager]
    recon[hadoop-ozone/recon]
    s3g[hadoop-ozone/s3gateway]
    tools[hadoop-ozone/tools + freon + insight]
    ofs[hadoop-ozone/ozonefs*]
    admin[hadoop-ozone/cli-admin + cli-shell]

    annot --> common
    cfg --> common
    ifadmin --> common
    ifclient --> common
    ifserver --> common
    mrdb --> fw
    rdiff --> fw
    common --> fw
    common --> hclient
    fw --> scm
    fw --> dn
    fw --> hclient
    ec --> dn
    ec --> hclient

    hclient --> ocommon
    ocommon --> oclient
    oclient --> om
    oclient --> recon
    oclient --> s3g
    oclient --> ofs
    oclient --> admin
    oclient --> tools
    om --> recon
    scm --> recon
    dn --> recon
```

## Components in this atlas

Priority tiers align with the top-level prioritization in the atlas prompt (P0 = read first). Count is number of production classes in the component (test / generated / package-info excluded).

| Component | Tier | Classes | Scope |
|---|---|---|---|
''' + '\n'.join(comp_lines) + f'''

Total production classes: **{sum(len(v) for v in by_comp.values())}**.

## Notable module facts

- `hadoop-hdds/framework` is not a "domain" component; it is a grab-bag of shared server-side plumbing that this atlas re-slices by feature (`ratis-integration` → Ratis-integration component; `security/*` → Security component; the rest → HddsCommon).
- `hadoop-hdds/common` contains **Hadoop-shaded** copies of `org.apache.hadoop.io.retry`, `org.apache.hadoop.ipc`, and `org.apache.hadoop.security` under packages `io_`, `ipc_`, `security_`. These are surfaced as feature `HddsCommon / hadoop-shaded` and are lower priority for study — they are effectively imported code.
- `hadoop-ozone/mini-cluster` and `hadoop-hdds/test-utils` are production `main/` source trees but exist only to support tests; they are **excluded** from this atlas per user decision.
- `hadoop-ozone/csi`, `hadoop-ozone/native-client`, `hadoop-ozone/om-tools`, `hadoop-hdds/tools`, `hadoop-hdds/crypto-*` currently contain **no `.java` sources** on this branch (only assembly / native shells).
''')

# --- ENTRYPOINTS.md ---
def find_class(fqcn_suffix):
    for r in rows:
        if r['fqcn'].endswith('.' + fqcn_suffix): return r
    return None

def find_by_name(name):
    matches = [r for r in rows if r['fqcn'].rsplit('.',1)[-1] == name]
    return matches[0] if matches else None

def rowline(name):
    r = find_by_name(name)
    if not r: return f'| {name} | NOT FOUND | | |'
    return f'| `{r["fqcn"]}` | `{r["path"]}` | {r["kind"]} | {r["role_one_liner"]} |'

entrypoints_names = [
    ('OM daemon',          ['OzoneManagerStarter', 'OzoneManager', 'OzoneManagerServiceProviderImpl']),
    ('SCM daemon',         ['StorageContainerManagerStarter', 'StorageContainerManager']),
    ('DN daemon',          ['HddsDatanodeService']),
    ('Recon daemon',       ['ReconServer']),
    ('S3 Gateway',         ['Gateway', 'S3GatewayHttpServer']),
    ('HttpFS Gateway',     ['HttpFSServerWebServer']),
    ('ozone sh (shell CLI)',      ['OzoneShell']),
    ('ozone admin CLI',           ['OzoneAdmin']),
    ('ozone debug CLI',           ['OzoneDebug']),
    ('ozone repair CLI',          ['RepairTool']),
    ('ozone freon (bench CLI)',   ['Freon']),
    ('ozone insight CLI',         ['Insight']),
]
sections = ['# Entry Points', '', 'Every daemon and CLI listed here has a `main()` (or Java-service equivalent) that starts a service. Read these first to have call-stack anchors.', '']
for group, names in entrypoints_names:
    sections.append(f'## {group}')
    sections.append('')
    sections.append('| fqcn | path | kind | role |')
    sections.append('|---|---|---|---|')
    for n in names:
        sections.append(rowline(n))
    sections.append('')

sections.append('## Call-stack starter map — client write path')
sections.append('')
sections.append('The next atlas pass will populate the concrete traces per component. See:')
sections.append('- `components/Client/ozone-client.md` for the OzoneClient entry and RPC to OM')
sections.append('- `components/OM/om-request-key.md` for the OM request handling')
sections.append('- `components/SCM/block-manager.md` for block allocation')
sections.append('- `components/Client/hdds-client.md` for XceiverClientRatis and chunk-write path')
sections.append('- `components/DN/ratis-statemachine-dn.md` for the DN Ratis state-machine apply')
sections.append('- `components/DN/chunk-manager.md` for chunk-file writes')
sections.append('- `components/OM/om-request-key.md` (commit) for CommitKey')
(ROOT / 'ENTRYPOINTS.md').write_text('\n'.join(sections))
print('emitted README, GLOSSARY, PREREQUISITES, REPO_MAP, ENTRYPOINTS')
