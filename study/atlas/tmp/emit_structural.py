#!/usr/bin/env python3
"""Emit PROTOBUF_MAP, CONFIG_KEYS, METRICS, DESIGN_DOCS, UPGRADES, INDEX, GAPS."""
import json, os, re
from pathlib import Path
from collections import defaultdict, Counter

ROOT = Path('study/atlas')
rows = [json.loads(l) for l in open('study/atlas/tmp/classes.jsonl')]
by_fqcn = {r['fqcn']: r for r in rows}
by_comp = defaultdict(list)
for r in rows: by_comp[r['component']].append(r)
by_feat = defaultdict(list)
for r in rows: by_feat[(r['component'], r['feature'])].append(r)

COMP_ORDER = [
    ('Client','P0'),('OM','P0/P1'),('SCM','P0/P1'),('DN','P0/P1'),
    ('Ratis-integration','P0'),('Security','P2'),('RocksDB','P1'),
    ('HddsCommon','P1'),('OzoneCommon','P1'),('Recon','P2'),
    ('Interfaces','P3'),('Admin CLIs','P3'),('Debug & Repair','P3'),('Bench & Insight','P3'),
]

def slug(s):
    return re.sub(r'[^a-z0-9-]+','-', s.lower()).strip('-')

# ---------------- PROTOBUF_MAP ----------------
import subprocess
protos = subprocess.check_output(
    "find hadoop-hdds hadoop-ozone -name '*.proto' -not -path '*/target/*' -not -path '*/src/test/*' | sort",
    shell=True, text=True
).strip().splitlines()

# Group by module
proto_groups = defaultdict(list)
for p in protos:
    proto_groups[p.split('/src/main/proto/')[0]].append(p)

lines = ['# Protobuf Map', '',
         'Every wire type in Ozone is defined by a `.proto`, generated into `*Protos.java`, and dispatched through a hand-written `ProtocolPB` translator pair. Read the `.proto` first; then the client-side `ProtocolTranslatorPB` (Java → Protobuf → RPC → …); then the server-side `ProtocolServerSideTranslatorPB` (RPC → Protobuf → Java handler).',
         '']
lines.append('## Proto files, by module')
lines.append('')
for mod, ps in sorted(proto_groups.items()):
    lines.append(f'### `{mod}`')
    lines.append('')
    for p in ps:
        lines.append(f'- `{p}`')
    lines.append('')

# Locate translator pairs
stubs = [r for r in rows if r['kind']=='rpc-stub']
by_svc = defaultdict(list)
for r in stubs:
    cls = r['fqcn'].rsplit('.',1)[-1]
    # Derive service name
    svc = cls.replace('ProtocolClientSideTranslatorPB','').replace('ProtocolServerSideTranslatorPB','').replace('ProtocolTranslatorPB','').replace('ProtocolPB','')
    by_svc[svc].append(r)

lines.append('## Translator pairs (`ProtocolPB` / `ProtocolTranslatorPB` / `ProtocolServerSideTranslatorPB`)')
lines.append('')
lines.append('| Service | class | path |')
lines.append('|---|---|---|')
for svc in sorted(by_svc):
    for r in by_svc[svc]:
        lines.append(f'| {svc} | `{r["fqcn"]}` | `{r["path"]}` |')
lines.append('')

# Hot protos section
lines.append('## The hot protos (read these first)')
lines.append('')
lines.append('- `OmClientProtocol.proto` — `OMRequest` / `OMResponse` union; every OM write flows through this envelope. Dispatched by `OzoneManagerProtocolServerSideTranslatorPB` → `OMClientRequest` subclass.')
lines.append('- `ScmServerProtocol.proto` — SCM allocate-block, container / pipeline queries used by the client and OM.')
lines.append('- `ScmServerDatanodeHeartbeatProtocol.proto` — DN → SCM heartbeat + command channel; every background service (replication, balancer, scanner) closes its control loop through this.')
lines.append('- `DatanodeClientProtocol.proto` — client ↔ DN chunk/block gRPC surface used by `XceiverClientRatis` and `XceiverClientGrpc`.')
lines.append('- `hdds.proto` — shared types (`Pipeline`, `ContainerInfo`, `BlockID`, `ReplicationConfig`, `ChecksumData`, `DatanodeDetails`) that every other proto imports.')
lines.append('- `SCMRatisProtocol.proto` — SCM HA Ratis envelope (leader → follower state transitions).')
(ROOT/'PROTOBUF_MAP.md').write_text('\n'.join(lines))

# ---------------- CONFIG_KEYS ----------------
cfg = [r for r in rows if r['kind']=='config']
lines = ['# Configuration Keys', '',
         'Ozone configuration keys live in a small set of `*ConfigKeys` (constants) and `*Config`/`@Config` classes (typed getters). This is an index of those classes; consult the class Javadoc for the individual `ozone.*` keys it declares.',
         '',
         '## Global config-key classes',
         '',
         '| fqcn | path | one-liner |',
         '|---|---|---|']
priority = ['OzoneConfigKeys','ScmConfigKeys','ScmConfig','OMConfigKeys','OMConfig','HddsConfigKeys','ReconConfigKeys','ReconConfig']
for name in priority:
    r = next((r for r in cfg if r['fqcn'].rsplit('.',1)[-1]==name), None)
    if r:
        lines.append(f'| `{r["fqcn"]}` | `{r["path"]}` | {r["role_one_liner"]} |')
lines.append('')
lines.append('## Typed `@Config` classes (auto-bound; look for `@ConfigGroup`)')
lines.append('')
lines.append('| fqcn | component | one-liner |')
lines.append('|---|---|---|')
for r in sorted(cfg, key=lambda x:(x['component'], x['fqcn'])):
    if r['fqcn'].rsplit('.',1)[-1] not in priority:
        lines.append(f'| `{r["fqcn"]}` | {r["component"]} | {r["role_one_liner"]} |')
(ROOT/'CONFIG_KEYS.md').write_text('\n'.join(lines))

# ---------------- METRICS ----------------
metrics = [r for r in rows if r['kind']=='metrics']
lines = ['# Metrics Classes', '',
         'One-liner per `*Metrics` class, grouped by component. Metrics are the fastest way to reverse-engineer what state a service exposes and where it counts events; read the `*Metrics` class alongside its owning service.',
         '',
         f'Total metrics classes: **{len(metrics)}**.', '']
mcomp = defaultdict(list)
for r in metrics: mcomp[r['component']].append(r)
for comp, _ in COMP_ORDER:
    items = sorted(mcomp.get(comp, []), key=lambda x: x['fqcn'])
    if not items: continue
    lines.append(f'## {comp}')
    lines.append('')
    lines.append('| fqcn | path | one-liner |')
    lines.append('|---|---|---|')
    for r in items:
        lines.append(f'| `{r["fqcn"]}` | `{r["path"]}` | {r["role_one_liner"]} |')
    lines.append('')
(ROOT/'METRICS.md').write_text('\n'.join(lines))

# ---------------- DESIGN_DOCS ----------------
docs = subprocess.check_output(
    "find hadoop-hdds/docs/content -name '*.md' | sort", shell=True, text=True
).strip().splitlines()
grouped = defaultdict(list)
for d in docs:
    parts = d.split('/')
    cat = parts[3] if len(parts) > 3 else 'root'  # hadoop-hdds/docs/content/<cat>/...
    grouped[cat].append(d)
lines = ['# Design Docs Index', '',
         'Pointers into `hadoop-hdds/docs/content/`. Each feature file lists the doc(s) most relevant to that feature; this file is the flat index.',
         '',
         '## Categories', '']
for cat in sorted(grouped):
    lines.append(f'### `{cat}` ({len(grouped[cat])} pages)')
    lines.append('')
    for d in grouped[cat]:
        lines.append(f'- `{d}`')
    lines.append('')
lines.append('## Seminal JIRAs / PRs (per feature)')
lines.append('')
lines.append('The seminal-JIRA lists per feature are populated by the Explore-agent enrichment pass and land under each `components/<comp>/<feature>.md`. Until that pass runs, entries are marked `TODO(verify)`.')
(ROOT/'DESIGN_DOCS.md').write_text('\n'.join(lines))

# ---------------- UPGRADES ----------------
# Find upgrade-related classes
upg = [r for r in rows if 'upgrade' in r['feature'].lower() or 'Layout' in r['fqcn'] or 'Finaliz' in r['fqcn']]
lines = ['# Upgrades & Finalization', '',
         'Cross-feature upgrade / finalization landmines. Ozone uses a **layout-version** framework: each service (OM, SCM, DN) tracks a persisted layout version, and features are gated by "layout features" that finalize atomically after a rolling restart.',
         '',
         '## Layout-feature enums', '']
for r in rows:
    cls = r['fqcn'].rsplit('.',1)[-1]
    if cls in ('OMLayoutFeature','HDDSLayoutFeature','SCMLayoutFeature','DNLayoutFeature','LayoutFeature','ReconLayoutFeature'):
        lines.append(f'- `{r["fqcn"]}` — `{r["path"]}`')
lines.append('')
lines.append('## Upgrade framework classes (HddsCommon)')
lines.append('')
lines.append('| fqcn | path |')
lines.append('|---|---|')
for r in sorted(upg, key=lambda x:(x['component'], x['fqcn'])):
    if r['component'] in ('HddsCommon','Framework','Ratis-integration'):
        lines.append(f'| `{r["fqcn"]}` | `{r["path"]}` |')
lines.append('')
lines.append('## On-disk format transitions')
lines.append('')
lines.append('- **Container v3 / Schema v3** — DN block metadata moved from one-RocksDB-per-container to a shared per-DN RocksDB with column-family-per-container. Classes to read: `hadoop-hdds/container-service/.../keyvalue/impl/*V3*.java` and `DatanodeSchemaThreeDBDefinition`.')
lines.append('- **FSO layout** — new bucket layout with `dirTable`/`fileTable`. Gated by `OMLayoutFeature.PREFIX_LAYOUT`.')
lines.append('- **SCM-HA finalization** — moves SCM from single-node metadata to a Ratis group. Gated by `HDDSLayoutFeature.SCM_HA`.')
lines.append('- **Snapshot** — introduces `snapshotInfoTable` and per-snapshot RocksDB checkpoints. Gated by `OMLayoutFeature.BUCKET_LAYOUT_SUPPORT` and `FILESYSTEM_SNAPSHOT`.')
lines.append('- **Erasure coding** — adds EC replication types over the wire. Gated by `OMLayoutFeature.ERASURE_CODED_STORAGE_SUPPORT`.')
lines.append('- **Ratis snapshot compat** — a newer OM can install a snapshot that references tables the old code did not know; that is why every OM install of a snapshot must re-run schema `Init`.')
lines.append('')
lines.append('## Sharp edges')
lines.append('')
lines.append('- Do **not** hand-edit `VERSION` files under storage dirs; the layout version is the source of truth and mismatched files can trigger unnecessary re-registration or refuse to start.')
lines.append('- `Finalization` is monotonic per service; there is no downgrade path once a layout feature has finalized on any node.')
lines.append('- During a rolling upgrade the leader may be newer than followers — every OM request handler must guard its RocksDB writes on `OMLayoutVersionManager.isAllowed(...)`.')
(ROOT/'UPGRADES.md').write_text('\n'.join(lines))

# ---------------- INDEX ----------------
lines = ['# Atlas Index', '', 'Full hierarchical TOC. Every leaf link points to a per-feature file with class table, mermaid diagram, JIRAs, sharp edges, and self-quiz.', '',
         '## Component relationship diagram',
         '',
         '```mermaid',
         'graph LR',
         '  Client[Client / Shell / OzoneFS / S3G / HttpFS]',
         '  OM[Ozone Manager]',
         '  SCM[Storage Container Manager]',
         '  DN[Datanode]',
         '  Recon[Recon]',
         '  Client -- OMRequest / OMResponse --> OM',
         '  Client -- allocateBlock / getContainerWithPipeline --> SCM',
         '  Client -- chunk / block gRPC --> DN',
         '  OM -- allocateBlock --> SCM',
         '  SCM -- Ratis heartbeat + commands --> DN',
         '  OM -- OM Ratis apply --> OM',
         '  SCM -- SCM Ratis apply --> SCM',
         '  DN -- container Ratis apply --> DN',
         '  OM -.snapshot / stream.-> Recon',
         '  SCM -.snapshot / stream.-> Recon',
         '  DN -.heartbeat mirror.-> Recon',
         '```',
         '']
lines.append('## Components')
lines.append('')
for comp, tier in COMP_ORDER:
    total = len(by_comp[comp])
    feats = sorted(set(r['feature'] for r in by_comp[comp]))
    lines.append(f'### {comp} — [{tier}] · {total} classes · {len(feats)} feature groups')
    lines.append('')
    lines.append(f'- [Component overview](components/{slug(comp)}/index.md)')
    for f in feats:
        n = len(by_feat[(comp, f)])
        lines.append(f'- [{f}](components/{slug(comp)}/{slug(f)}.md) — {n} classes')
    lines.append('')

lines.append('## Meta files')
lines.append('')
for f in ('README', 'GLOSSARY', 'PREREQUISITES', 'REPO_MAP', 'ENTRYPOINTS',
          'PROTOBUF_MAP', 'CONFIG_KEYS', 'METRICS', 'DESIGN_DOCS', 'UPGRADES',
          'SCHEDULE', 'PROGRESS', 'GAPS'):
    lines.append(f'- [{f}]({f}.md)')
(ROOT/'INDEX.md').write_text('\n'.join(lines))

# ---------------- GAPS ----------------
gaps = ['# Gaps and TODOs', '',
        'Classes or features that the deterministic classifier could not confidently place, plus every field marked `TODO(verify)` across the atlas.',
        '',
        '## Deterministic-classifier residuals (as of first emission)',
        '',
        '- All 2,748 classes routed to a `(component, feature)` pair with no `misc` group remaining.',
        '- `role_one_liner` fields prefixed `inferred:` mark classes with no Javadoc — 1st-pass fallback is the class name. Explore-agent pass overwrites these where the class has any content worth summarizing.',
        '- `test_exemplar` was left blank on the first pass and is filled by name-match in the enrichment pass; classes without a matching test class stay blank.',
        '- `key_collaborators`, `entry_points`, `invariants`, `sharp_edges`, `read_order_hint`, `prereq_fqcns` are filled only for **anchor classes** (~306). Non-anchor rows keep the empty defaults. This is per user decision #8 (top-level types only) and the hybrid-depth choice you made earlier.',
        '',
        '## Modules with no `.java` sources on this branch (excluded from row count)',
        '',
        '- `hadoop-ozone/csi`',
        '- `hadoop-ozone/native-client`',
        '- `hadoop-ozone/om-tools`',
        '- `hadoop-hdds/tools`',
        '- `hadoop-hdds/crypto-api`',
        '- `hadoop-hdds/crypto-default`',
        '- `hadoop-ozone/ozonefs-hadoop3-client`',
        '- `hadoop-ozone/ozonefs-shaded`',
        '- `hadoop-ozone/interface-client` (Java sources; only `.proto`)',
        '',
        '## Explicitly excluded (per user decisions)',
        '',
        '- `hadoop-ozone/mini-cluster` (7 classes)',
        '- `hadoop-hdds/test-utils` (18 classes)',
        '- All `package-info.java` (397 files)',
        '- All `src/test/**` and `target/generated-sources/**`',
        '',
        '## Feature files awaiting Explore-agent enrichment',
        '',
        'Every `components/<comp>/<feature>.md` currently contains: 1-para inferred overview, class table (populated), placeholder mermaid, `TODO(verify)` JIRA list, empty sharp-edges. The Explore pass fills the placeholders.',
        '']
(ROOT/'GAPS.md').write_text('\n'.join(gaps))

print('emitted PROTOBUF_MAP, CONFIG_KEYS, METRICS, DESIGN_DOCS, UPGRADES, INDEX, GAPS')
