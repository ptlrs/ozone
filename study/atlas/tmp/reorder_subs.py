#!/usr/bin/env python3
"""Recompute reading_order with per-feature sub-feature priority."""
import json, re
from pathlib import Path
from collections import OrderedDict, defaultdict

ROOT = Path('study/atlas')
COMPS = ROOT / 'components'
rows = json.load(open(ROOT / 'atlas.json'))

# Sub-feature priority per feature. Anything not listed sorts alpha AFTER the list.
SUB_PRIO = {
    ('Client','ozone-client'): ['client-facade','user-api-types','io-streams','client.io','client.rpc','ec-client','checksum'],
    ('Client','hdds-client'): ['xceiver-clients','write-streams','scm.storage','read-streams','ec-transport-read','client-utils'],
    ('Interfaces','s3gateway'): ['s3-endpoints','s3-signature','s3-secret-mgmt','s3-common-types','s3-errors','s3-utils','s3-metrics','s3-audit','s3-awssdk-compat'],
    ('Interfaces','ozonefs-common'): ['client-adapter','ofs-rooted','o3fs-bucket','io-streams','fs-types','metrics'],
    ('OM','om-request-key'): ['create-commit','delete','rename','key-acl','prefix-acl'],
    ('SCM','container-replication'): ['under-replication','over-replication','mis-replication','lifecycle-transitions','ec-replication','health-checks'],
    ('DN','erasure-coding'): ['reconstruction','coder','ec-chunk'],
}

# Curated feature order (unchanged from prior pass)
CURATED_ORDER = [
    ('Client','ozone-client'), ('OzoneCommon','client-common'),
    ('OM','om-protocol'), ('OM','om-request'),
    ('Client','hdds-client'), ('SCM','block-manager'), ('SCM','pipeline-manager'),
    ('DN','kv-container'), ('DN','kv-container-impl'), ('DN','container-interfaces'),
    ('DN','erasure-coding'), ('OM','om-request-key'),
    ('Ratis-integration','ratis-integration'), ('HddsCommon','ratis-integration'),
    ('OM','om-ratis'), ('OM','om-response'), ('OM','om-execution'),
    ('DN','ratis-statemachine-dn'),
    ('OM','om-server'), ('OM','om-key-manager'), ('OM','interface-storage'),
    ('OM','om-bucket-manager'), ('OM','om-volume-manager'), ('OM','om-request-bucket'), ('OM','om-request-volume'),
    ('OM','om-locking'), ('OM','om-codecs'),
    ('SCM','container-manager'), ('HddsCommon','container-common'),
    ('SCM','pipeline-choose-policy'), ('HddsCommon','pipeline-common'),
    ('SCM','container-replication'),
    ('SCM','scm-ha'), ('SCM','safemode'), ('SCM','node-manager'),
    ('DN','hdds-volume'), ('DN','dn-rocksdb'), ('RocksDB','managed-rocksdb'),
    ('DN','dn-statemachine'), ('DN','dn-reports'), ('DN','dn-scm-commands'),
    ('OM','om-snapshot'), ('OM','om-request-snapshot'), ('OzoneCommon','snapshot-common'),
    ('RocksDB','checkpoint-differ'), ('RocksDB','rocks-native'),
    ('DN','container-replication-dn'), ('SCM','container-balancer'), ('DN','disk-balancer'),
    ('OM','om-background-services'), ('OM','om-upgrade'),
    ('Security','security-x509'), ('Security','security-tokens'),
    ('OM','om-security'), ('SCM','scm-security'),
    ('Interfaces','s3gateway'), ('Interfaces','ozonefs-common'), ('Recon','recon-server'),
    ('Admin CLIs','admin'), ('Debug & Repair','debug'), ('Bench & Insight','freon'),
]
COMPONENT_PRIORITY = ['Client','OM','SCM','DN','Ratis-integration','Security','RocksDB',
                      'HddsCommon','OzoneCommon','Recon','Interfaces',
                      'Admin CLIs','Debug & Repair','Bench & Insight']

feature_order = OrderedDict()
for cf in CURATED_ORDER: feature_order.setdefault(cf, len(feature_order))
all_feats = sorted({(r['component'], r['feature']) for r in rows},
                   key=lambda cf: (COMPONENT_PRIORITY.index(cf[0]) if cf[0] in COMPONENT_PRIORITY else 99, cf[1]))
for cf in all_feats: feature_order.setdefault(cf, len(feature_order))

def sub_key(cf, sub):
    prio = SUB_PRIO.get(cf, [])
    if sub in prio: return (0, prio.index(sub))
    return (1, sub)  # alpha after prio list

by_fs = defaultdict(list)
for r in rows: by_fs[(r['component'], r['feature'])].append(r)

reading = 0
for cf in feature_order:
    if cf not in by_fs: continue
    items = by_fs[cf]
    subs = sorted({r['sub_feature'] for r in items}, key=lambda s: sub_key(cf, s))
    for sub in subs:
        srows = [r for r in items if r['sub_feature'] == sub]
        srows.sort(key=lambda r: (r.get('read_order_hint') or 999999, r['fqcn']))
        for r in srows:
            reading += 1
            r['reading_order'] = reading

with open('study/atlas/tmp/classes.jsonl','w') as f:
    for r in rows: f.write(json.dumps(r)+'\n')
with open(ROOT/'atlas.json','w') as f:
    json.dump(rows, f, indent=1)

# rewrite per-feature class tables to reflect new order
CLASS_TABLE_RE = re.compile(r'(## Class table\s*\n\s*\n)(.*?)(\n\n## )', re.DOTALL)
def slug(s): return re.sub(r'[^a-z0-9-]+','-', s.lower()).strip('-')
def mdx_safe(s):
    import re as _re
    s = _re.sub(r'\{@link\s+([^}]+)\}', r'\1', s)
    s = _re.sub(r'\{@code\s+([^}]+)\}', r'\1', s)
    s = s.replace('&', '&amp;')
    s = s.replace('<', '&lt;').replace('>', '&gt;')
    s = s.replace('{', '&#123;').replace('}', '&#125;')
    return s


def escape_md(s):
    s = s.replace('|', '\\|').replace('\n', ' ').strip()
    return mdx_safe(s)

def render(feat_rows, cf):
    groups = OrderedDict()
    for r in sorted(feat_rows, key=lambda r: r['reading_order']):
        groups.setdefault(r['sub_feature'], []).append(r)
    out = []
    for sub, srows in groups.items():
        out.append(f'### Sub-feature: `{sub}`')
        out.append('')
        out.append('| reading_order | fqcn | kind | logic | loc | study (min) | role |')
        out.append('|--:|---|---|---|--:|--:|---|')
        for r in srows:
            out.append(f'| {r["reading_order"]} | `{r["fqcn"]}` | {r["kind"]} | {r["logic_weight"]} | {r["loc_code"]} | {r["study_minutes"]} | {escape_md(r["role_one_liner"])} |')
        out.append('')
    return '\n'.join(out)

for cf, srows in by_fs.items():
    comp, feat = cf
    md = COMPS / slug(comp) / f'{slug(feat)}.md'
    if not md.exists(): continue
    t = md.read_text()
    new_body = render(srows, cf) + '\n'
    t2, n = CLASS_TABLE_RE.subn(lambda m: m.group(1)+new_body+m.group(3), t, count=1)
    if n: md.write_text(t2)

# rewrite component indexes
by_comp = defaultdict(list)
for r in rows: by_comp[r['component']].append(r)

for comp, items in by_comp.items():
    cdir = COMPS / slug(comp)
    feats = sorted({r['feature'] for r in items})
    feats_ordered = sorted(feats, key=lambda f: min(r['reading_order'] for r in items if r['feature']==f))
    lines = [f'# Component: {comp}', '',
             f'**Classes:** {len(items)}    **Features:** {len(feats)}',
             '',
             '## Feature and sub-feature index (in reading order)',
             '',
             '| # | Feature | Sub-feature | Classes | Anchors | reading_order range |',
             '|--:|---|---|--:|--:|---|']
    i = 0
    for f in feats_ordered:
        rows_f = [r for r in items if r['feature']==f]
        subs = {}
        for r in rows_f: subs.setdefault(r['sub_feature'], []).append(r)
        subs_ordered = sorted(subs.items(), key=lambda x: min(rr['reading_order'] for rr in x[1]))
        for sub, srows in subs_ordered:
            i += 1
            anch = sum(1 for r in srows if r['logic_weight']=='logic-heavy')
            lo = min(r['reading_order'] for r in srows)
            hi = max(r['reading_order'] for r in srows)
            lines.append(f'| {i} | [{f}]({slug(f)}.md) | `{sub}` | {len(srows)} | {anch} | {lo}–{hi} |')
    lines.append('')
    lines.append('_reading_order is a global monotonic index. Look up `reading_order + 1` in atlas.json for the next class._')
    (cdir/'index.md').write_text('\n'.join(lines))

# INDEX.md sub-feature TOC section
by_comp_feat_sub = defaultdict(lambda: defaultdict(list))
for r in rows:
    by_comp_feat_sub[r['component']][r['feature']].append(r)

# Regenerate INDEX.md
lines_idx = open(ROOT/'INDEX.md').read().split('\n')
# preserve first block up to '## Components'
try:
    cutoff = lines_idx.index('## Components')
    prefix = lines_idx[:cutoff]
except ValueError:
    prefix = lines_idx

new = prefix + ['## Components (reading-order)', '',
                '| # | Component | Feature | Sub-feature | Classes | reading_order range |',
                '|--:|---|---|---|--:|---|']
i = 0
for cf in feature_order:
    if cf not in by_fs: continue
    comp, feat = cf
    subs = {}
    for r in by_fs[cf]: subs.setdefault(r['sub_feature'], []).append(r)
    subs_ordered = sorted(subs.items(), key=lambda x: min(rr['reading_order'] for rr in x[1]))
    for sub, srows in subs_ordered:
        i += 1
        lo = min(r['reading_order'] for r in srows)
        hi = max(r['reading_order'] for r in srows)
        new.append(f'| {i} | {comp} | [{feat}](components/{slug(comp)}/{slug(feat)}.md) | `{sub}` | {len(srows)} | {lo}–{hi} |')
new.append('')
new.append('## Meta files')
new.append('')
for f in ('README','GLOSSARY','PREREQUISITES','REPO_MAP','ENTRYPOINTS',
          'PROTOBUF_MAP','CONFIG_KEYS','METRICS','DESIGN_DOCS','UPGRADES',
          'SCHEDULE','PROGRESS','GAPS'):
    new.append(f'- [{f}]({f}.md)')
(ROOT/'INDEX.md').write_text('\n'.join(new))

print(f'rows: {len(rows)}')
print(f'features: {sum(1 for _ in feature_order)}')
sub_count = len({(r['component'],r['feature'],r['sub_feature']) for r in rows})
print(f'sub-features: {sub_count}')
