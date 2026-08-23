#!/usr/bin/env python3
"""Assign sub-feature + sub-sub-feature to every row and re-emit atlas + indexes.

Method:
  1. sub_feature: derived from purpose overrides first (name-pattern matches on
     the class' short name and its collaborators), else falls back to the
     package tail (last two segments).
  2. sub_sub_feature: package tail (last two segments) when it disagrees with
     sub-feature, else ''.
  3. reading_order: monotonically increasing integer across the whole atlas,
     computed by walking components in the schedule's priority order, then
     features in the schedule order (curated features first), then
     sub-features alphabetically, then classes in read_order_hint.
  4. Emits per-component index.md refreshed with the sub-feature TOC and
     per-feature files refreshed with sub-feature grouping in the class table.
"""
import json, re, os
from pathlib import Path
from collections import defaultdict, OrderedDict

ROOT = Path('study/atlas')
COMPS = ROOT / 'components'
rows = json.load(open(ROOT / 'atlas.json'))

# ---- purpose-based sub-feature overrides ----
def name_pattern(cls):
    """Return purpose tags from a class's short name."""
    tags = set()
    if cls.startswith('EC') or 'Erasure' in cls: tags.add('ec')
    if cls.startswith('OzoneS3') or cls.startswith('S3') or 'S3Secret' in cls: tags.add('s3-consumer')
    if cls.startswith('Basic') and 'Ozone' in cls and 'FileSystem' in cls: tags.add('ofs-consumer')
    if cls.endswith('OutputStream') or cls.endswith('OutputStreamEntry') or cls.endswith('OutputStreamEntryPool') or cls.endswith('DataStreamOutput'):
        tags.add('write-path')
    if cls.endswith('InputStream') or cls.endswith('InputStreamFactory') or cls.endswith('InputStreamFactoryImpl') or cls.endswith('InputStreamProxy'):
        tags.add('read-path')
    if 'Checksum' in cls: tags.add('checksum')
    if cls.startswith('XceiverClient'): tags.add('rpc-transport')
    if cls.endswith('CommitWatcher') or 'CommitWatcher' in cls: tags.add('write-path')
    if cls.startswith('Ozone') and cls.endswith('Configuration'): tags.add('config')
    if cls in ('OzoneClient','OzoneClientFactory','OzoneClientUtils','OzoneClientException'):
        tags.add('client-facade')
    if cls == 'ObjectStore' or cls == 'OzoneVolume' or cls == 'OzoneBucket' or cls.startswith('OzoneMultipart') or cls == 'OzoneKey' or cls == 'OzoneKeyDetails' or cls == 'OzoneKeyLocation' or cls == 'OzoneSnapshot' or cls == 'OzoneSnapshotDiff' or cls == 'OzoneLifecycleConfiguration' or cls.endswith('Args'):
        tags.add('user-api')
    if cls in ('RpcClient','ClientProtocol'):
        tags.add('client-facade')
    return tags

# ---- feature-specific sub-feature routing ----
# Given a (component, feature) and a class row, return a sub-feature label.
# Falls back to package-tail if no rule matches.
def package_tail(fqcn):
    parts = fqcn.split('.')[:-1]  # drop class
    while parts and parts[0] in ('org','apache','hadoop','ozone','hdds','ratis'):
        # keep 'ozone'/'hdds' if that's all we have; strip only leading company noise
        if parts[0] in ('org','apache','hadoop'): parts.pop(0)
        else: break
    return '.'.join(parts[-2:]) if len(parts) >= 2 else '.'.join(parts) or 'root'

SUBFEATURE_RULES = {
  ('Client','ozone-client'): [
    (lambda r,c: 'ec' in name_pattern(c),         'ec-client'),
    (lambda r,c: 'checksum' in name_pattern(c),   'checksum'),
    (lambda r,c: 'write-path' in name_pattern(c) or 'InputStream' in c or 'OutputStream' in c or c in ('KeyOutputStream','KeyInputStream','KeyDataStreamOutput'), 'io-streams'),
    (lambda r,c: 'client-facade' in name_pattern(c) or 'RpcClient' in c or 'ClientProtocol' in c, 'client-facade'),
    (lambda r,c: 'user-api' in name_pattern(c),   'user-api-types'),
  ],
  ('Client','hdds-client'): [
    (lambda r,c: 'ec' in name_pattern(c),                                'ec-transport-read'),
    (lambda r,c: c.startswith('XceiverClient') or c in ('ContainerClientMetrics','ErrorInjector','StreamBufferArgs','XceiverClientCreator','XceiverClientFactory','XceiverClientManager','XceiverClientMetrics'), 'xceiver-clients'),
    (lambda r,c: 'InputStream' in c or c in ('BlockExtendedInputStream','ChunkInputStream','BadDataLocationException','InsufficientLocationsException'), 'read-streams'),
    (lambda r,c: 'OutputStream' in c or 'DataStreamOutput' in c or 'CommitWatcher' in c or c in ('BufferPool',), 'write-streams'),
    (lambda r,c: c in ('OzoneClientConfig','HddsClientUtils','BoundedElasticByteBufferPool','ByteArrayStreamOutput','ByteBufferOutputStream','ByteArrayReader','ByteBufferReader','ByteReaderStrategy','ByteBufferStreamOutput','DomainPeer'), 'client-utils'),
  ],
  ('Interfaces','s3gateway'): [
    (lambda r,c: '/endpoint/' in r['path'],       's3-endpoints'),
    (lambda r,c: '/signature/' in r['path'],      's3-signature'),
    (lambda r,c: '/exception/' in r['path'] or c.endswith('Exception'), 's3-errors'),
    (lambda r,c: '/util/' in r['path'],           's3-utils'),
    (lambda r,c: '/commontypes/' in r['path'],    's3-common-types'),
    (lambda r,c: '/metrics/' in r['path'] or 'Metrics' in c, 's3-metrics'),
    (lambda r,c: '/s3secret/' in r['path'] or 'S3Secret' in c, 's3-secret-mgmt'),
    (lambda r,c: '/audit/' in r['path'],          's3-audit'),
    (lambda r,c: '/awssdk/' in r['path'],         's3-awssdk-compat'),
  ],
  ('Interfaces','ozonefs-common'): [
    (lambda r,c: c.startswith('BasicRootedOzone') or 'Rooted' in c, 'ofs-rooted'),      # ofs:// = rooted
    (lambda r,c: c.startswith('BasicOzone') and 'FileSystem' in c,  'o3fs-bucket'),
    (lambda r,c: 'Adapter' in c,                                    'client-adapter'),
    (lambda r,c: 'DataStreamOutput' in c or 'InputStream' in c or 'OutputStream' in c,  'io-streams'),
    (lambda r,c: 'FileStatus' in c or 'PathCapabilities' in c,      'fs-types'),
    (lambda r,c: 'StorageStatistics' in c,                          'metrics'),
  ],
  ('OM','om-request-key'): [
    (lambda r,c: '/key/acl/' in r['path'] or '/key/acl' in r['path'], 'key-acl'),
    (lambda r,c: '/acl/prefix/' in r['path'],   'prefix-acl'),
    (lambda r,c: 'Rename' in c,                 'rename'),
    (lambda r,c: 'Delete' in c or 'Purge' in c, 'delete'),
    (lambda r,c: 'Commit' in c or 'Create' in c or 'Allocate' in c or 'Open' in c, 'create-commit'),
  ],
  ('SCM','container-replication'): [
    (lambda r,c: '/health/' in r['path'] or c.endswith('HealthCheck'), 'health-checks'),
    (lambda r,c: 'EC' in c,                     'ec-replication'),
    (lambda r,c: 'Under' in c,                  'under-replication'),
    (lambda r,c: 'Over' in c,                   'over-replication'),
    (lambda r,c: 'Mis' in c,                    'mis-replication'),
    (lambda r,c: 'Quasi' in c or 'Closed' in c or 'Unhealthy' in c, 'lifecycle-transitions'),
  ],
  ('DN','erasure-coding'): [
    (lambda r,c: 'Reconstruction' in c or 'Coordinator' in c, 'reconstruction'),
    (lambda r,c: 'Coder' in c or 'RS' in c or 'Encoder' in c or 'Decoder' in c, 'coder'),
    (lambda r,c: 'Chunk' in c,                  'ec-chunk'),
  ],
}

def sub_feature_of(r):
    key = (r['component'], r['feature'])
    cls = r['fqcn'].rsplit('.',1)[-1]
    if key in SUBFEATURE_RULES:
        for pred, tag in SUBFEATURE_RULES[key]:
            try:
                if pred(r, cls): return tag
            except Exception:
                continue
    return package_tail(r['fqcn'])

def sub_sub_of(r, sub):
    tail = package_tail(r['fqcn'])
    return tail if tail != sub else ''

for r in rows:
    r['sub_feature'] = sub_feature_of(r)
    r['sub_sub_feature'] = sub_sub_of(r, r['sub_feature'])

# ---- global reading order ----
# The 26-week SCHEDULE curates specific features first; extract that order.
# Then within each feature, order by sub-feature -> read_order_hint.
CURATED_ORDER = [
    ('Client','ozone-client'), ('OzoneCommon','client-common'),
    ('OM','om-protocol'), ('OM','om-request'),
    ('Client','hdds-client'), ('SCM','block-manager'), ('SCM','pipeline-manager'),
    ('DN','kv-container'), ('DN','kv-container-impl'),
    ('DN','container-interfaces'),
    ('DN','erasure-coding'), ('OM','om-request-key'),
    ('Ratis-integration','ratis-integration'), ('HddsCommon','ratis-integration'),
    ('OM','om-ratis'),
    ('OM','om-response'), ('OM','om-execution'),
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

# Component priority for tail features not on the curated list
COMPONENT_PRIORITY = ['Client','OM','SCM','DN','Ratis-integration','Security','RocksDB',
                      'HddsCommon','OzoneCommon','Recon','Interfaces',
                      'Admin CLIs','Debug & Repair','Bench & Insight']

# Build (component, feature) ordering
feature_order = OrderedDict()
for cf in CURATED_ORDER:
    if cf not in feature_order: feature_order[cf] = len(feature_order)

# Append remaining features by component priority, then alpha
all_feats = sorted({(r['component'], r['feature']) for r in rows},
                   key=lambda cf: (COMPONENT_PRIORITY.index(cf[0]) if cf[0] in COMPONENT_PRIORITY else 99, cf[1]))
for cf in all_feats:
    if cf not in feature_order: feature_order[cf] = len(feature_order)

# Group rows by (feature, sub_feature), then assign a monotonic global reading_order
by_fs = defaultdict(list)
for r in rows:
    key = (r['component'], r['feature'])
    by_fs[key].append(r)

reading_counter = 0
for cf, order_idx in feature_order.items():
    if cf not in by_fs: continue
    items = by_fs[cf]
    # sub-feature: alphabetize (or use rule-declared order for known features)
    subs = sorted({r['sub_feature'] for r in items})
    for sub in subs:
        sub_items = [r for r in items if r['sub_feature'] == sub]
        sub_items.sort(key=lambda r: (r.get('read_order_hint') or 999999, r['fqcn']))
        for r in sub_items:
            reading_counter += 1
            r['reading_order'] = reading_counter

# ---- write outputs ----
with open('study/atlas/tmp/classes.jsonl', 'w') as f:
    for r in rows: f.write(json.dumps(r) + '\n')
with open(ROOT / 'atlas.json', 'w') as f:
    json.dump(rows, f, indent=1)

# ---- rewrite component/<comp>/index.md with sub-feature TOC + reading order ----
def slug(s): return re.sub(r'[^a-z0-9-]+','-', s.lower()).strip('-')

from collections import Counter
by_comp = defaultdict(list)
for r in rows: by_comp[r['component']].append(r)

for comp in by_comp:
    cdir = COMPS / slug(comp)
    cdir.mkdir(parents=True, exist_ok=True)
    items = by_comp[comp]
    features = sorted({r['feature'] for r in items})
    # feature -> [(sub_feature, [rows])]
    feat_to_subs = {}
    for f in features:
        rows_f = [r for r in items if r['feature'] == f]
        subs = {}
        for r in rows_f:
            subs.setdefault(r['sub_feature'], []).append(r)
        # order by read-anchored min reading_order
        subs_ordered = sorted(subs.items(), key=lambda x: min(rr['reading_order'] for rr in x[1]))
        feat_to_subs[f] = subs_ordered
    # feature order = min reading_order across all sub-rows
    features_ordered = sorted(features, key=lambda f: min(r['reading_order'] for r in items if r['feature']==f))

    lines = [f'# Component: {comp}', '',
             f'**Classes:** {len(items)}    **Features:** {len(features)}',
             '',
             '## Feature and sub-feature index (in reading order)',
             '',
             '| # | Feature | Sub-feature | Classes | Anchors | Range of reading_order |',
             '|--:|---|---|--:|--:|---|']
    row_i = 0
    for f in features_ordered:
        for sub, srows in feat_to_subs[f]:
            row_i += 1
            n = len(srows)
            anch = sum(1 for r in srows if r['logic_weight']=='logic-heavy')
            lo = min(r['reading_order'] for r in srows)
            hi = max(r['reading_order'] for r in srows)
            lines.append(f'| {row_i} | [{f}]({slug(f)}.md) | `{sub}` | {n} | {anch} | {lo}–{hi} |')
    lines.append('')
    lines.append('_Reading order is a global monotonic index across the entire atlas so you can always answer "what do I read next" by looking up `reading_order + 1`._')
    (cdir / 'index.md').write_text('\n'.join(lines))
print('component indexes refreshed with sub-feature TOC + reading_order')

# ---- rewrite class table in each feature file to insert sub-feature dividers ----
CLASS_TABLE_RE = re.compile(r'(## Class table\s*\n\s*\n)(.*?)(\n\n## )', re.DOTALL)

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

def render_table_with_subs(feat_rows):
    # group by sub_feature preserving reading_order
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
            out.append(
                f'| {r["reading_order"]} | `{r["fqcn"]}` | {r["kind"]} | {r["logic_weight"]} | '
                f'{r["loc_code"]} | {r["study_minutes"]} | {escape_md(r["role_one_liner"])} |'
            )
        out.append('')
    return '\n'.join(out)

tables_rewritten = 0
for cf, srows in by_fs.items():
    comp, feat = cf
    md = COMPS / slug(comp) / f'{slug(feat)}.md'
    if not md.exists(): continue
    text = md.read_text()
    new_body = render_table_with_subs(srows) + '\n'
    def repl(m): return m.group(1) + new_body + m.group(3)
    new_text, n = CLASS_TABLE_RE.subn(repl, text, count=1)
    if n:
        md.write_text(new_text)
        tables_rewritten += 1
print(f'feature files with sub-feature tables: {tables_rewritten}')
