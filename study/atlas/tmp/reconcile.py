#!/usr/bin/env python3
"""Post-enrichment reconciliation.

Fixes:
  1. Role extractor: strip {@code ...}/{@link ...}, terminator is '. ' / '.\\n' / EOF.
  2. Role extractor: only accept a Javadoc block that IMMEDIATELY precedes a
     type declaration (class/interface/enum/@interface). Skip license blocks.
  3. Kind classifier scoped to primary type declaration line.
  4. logic_weight post-pass: loc_code >= 200 cannot be 'data-only'.
  5. Harvest 'inferred:' roles from feature-file Anchor Details.
  6. Link validator over components/**/*.md.
  7. Regenerate atlas.json + class tables in all feature files with the new rows.
"""
import json, re, os
from pathlib import Path
from collections import defaultdict, Counter

ROOT = Path('study/atlas')
COMPS = ROOT / 'components'

# ---------------------------------------------------------------- helpers
INLINE_TAG = re.compile(r'\{@\w+\s+([^{}]*?)\}')
def strip_inline(s): return INLINE_TAG.sub(r'\1', s)

# Match a doc block '/** ... */' immediately followed by whitespace then a type declaration.
DOC_BLOCK_RE = re.compile(
    r'/\*\*\s*(.*?)\s*\*/\s*'
    r'(?:@[\w.]+(?:\([^)]*\))?\s*)*'                     # optional annotations
    r'(?:public\s+|private\s+|protected\s+|static\s+|final\s+|abstract\s+)*'
    r'(?:class|interface|enum|@interface|record)\s+(\w+)',
    re.DOTALL
)
FIRST_SENT_RE = re.compile(r'([^\n]{5,240}?\.)(?:\s|$)')

def role_of(body, cls):
    """Extract role_one_liner: only from a Javadoc block that precedes the
    primary type declaration. Skip license headers. Strip inline @tags.
    """
    best = None
    for m in DOC_BLOCK_RE.finditer(body[:16000]):
        raw = m.group(1)
        matched_cls = m.group(2)
        # Only use blocks whose target type name matches this class
        if matched_cls != cls:
            # Still keep as a fallback if we don't find a name-matched block
            if best is None:
                best = raw
            continue
        best = raw
        break
    if best is None:
        return f'inferred: {cls} — role not documented.'
    lines = [re.sub(r'^\s*\*\s?','',l).strip() for l in best.splitlines()]
    joined = strip_inline(' '.join(l for l in lines if l and not l.startswith('@')))
    m = FIRST_SENT_RE.search(joined)
    if m:
        s = m.group(1).strip()
        if len(s) > 120: s = s[:117].rstrip() + '...'
        # sanity: reject Apache license first sentence
        low = s.lower()
        if 'licensed to the apache' in low or 'copyright' in low:
            return f'inferred: {cls} — role not documented.'
        return s
    return f'inferred: {cls} — role not documented.'

# ---------------------------------------------------------------- kind
KIND_HINTS = [
  ('Exception',                'exception'),
  ('Metrics',                  'metrics'),
  ('Config.java',              'config'),
  ('ConfigKeys',               'config'),
  ('Codec.java',               'util'),
  ('Factory',                  'factory'),
  ('Builder',                  'data'),
  ('ProtocolPB',               'rpc-stub'),
  ('ProtocolTranslatorPB',     'rpc-stub'),
  ('ProtocolServerSideTranslatorPB','rpc-stub'),
  ('CLI.java',                 'cli'),
  ('Command.java',             'cli'),
  ('Subcommand',               'cli'),
]

# scoped type-decl match: only look at the PRIMARY type declaration line
PRIMARY_TYPE_RE_TEMPLATE = re.compile(
    r'^(?P<indent>\s*)'
    r'(?:(?:public|private|protected)\s+)?'
    r'(?:(?:static|final|sealed|non-sealed)\s+)*'
    r'(?P<mods>(?:abstract\s+)?)'
    r'(?P<kw>class|interface|enum|@interface|record)\s+'
    r'{cls}\b',
    re.MULTILINE
)

def kind_of(fqcn, path, body):
    cls = fqcn.rsplit('.',1)[-1]
    # 1. Name-based rules (override for well-known suffix conventions)
    for suf, k in KIND_HINTS:
        if fqcn.endswith('.' + suf.replace('.java','')) or suf in os.path.basename(path):
            return k
    # 2. Scoped primary-type detection
    pattern = re.compile(
        r'^\s*(?:(?:public|private|protected)\s+)?(?:(?:static|final|sealed|non-sealed)\s+)*'
        r'(?P<mods>(?:abstract\s+)?)(?P<kw>class|interface|enum|@interface|record)\s+' +
        re.escape(cls) + r'\b',
        re.MULTILINE
    )
    m = pattern.search(body)
    if m:
        kw = m.group('kw')
        mods = m.group('mods')
        if kw == 'interface':
            # exclude @FunctionalInterface annotated one-methods from `interface` heuristic?
            # Kind of interface either way.
            return 'interface'
        if kw == 'enum': return 'data'
        if kw == '@interface': return 'interface'
        if kw == 'record': return 'dto'
        # class
        if 'abstract' in mods: return 'abstract'
        # Deeper class classification
        if 'extends StateMachine' in body or 'implements StateMachine' in body:
            return 'state-machine'
        if re.search(r'@Command\(', body): return 'cli'
        if 'BackgroundService' in body or 'implements Runnable' in body or 'implements Callable' in body:
            return 'service'
        if '@Path(' in body or '@Provider' in body: return 'service'
        # DTO by name suffix (concrete class only)
        if cls.endswith('Info') or cls.endswith('Response') or cls.endswith('Request') or 'DTO' in fqcn:
            # But if it's clearly logic-heavy (large file), it's a service
            return 'dto'
        return 'service'
    # 3. Fallback if we can't find the type decl (shouldn't happen for well-formed files)
    if cls.endswith('Info') or cls.endswith('Response') or cls.endswith('Request'):
        return 'dto'
    return 'service'

CONCURRENCY_MARKERS = [
  (re.compile(r'@ThreadSafe|Concurrent|ReadWriteLock|synchronized\s*\('), 'thread-safe'),
  (re.compile(r'ExecutorService|newSingleThreadExecutor|newFixedThreadPool|ScheduledExecutor|BlockingQueue'), 'actor/queue'),
  (re.compile(r'extends StateMachine|applyTransaction|takeSnapshot\('), 'ratis-applied'),
]
def concurrency_of(body):
    for rx, tag in CONCURRENCY_MARKERS:
        if rx.search(body): return tag
    return 'single-threaded'

def persistence_of(body):
    m = re.findall(r'(?:get|open)Table\("(\w+)"', body)
    if m:
        return 'RocksDB:'+','.join(sorted(set(m))[:4])
    if 'RocksDB' in body or 'ManagedRocksDB' in body: return 'RocksDB'
    if re.search(r'FileOutputStream|Files\.write|writeToDisk', body): return 'on-disk'
    return 'in-memory'

def logic_weight_of(kind, loc_code):
    if kind in ('data','dto','exception','config','rpc-stub') and loc_code >= 200:
        # override: 200+ LOC cannot be data-only regardless of kind
        return 'logic-heavy'
    if kind in ('data','dto','exception','config','rpc-stub'): return 'data-only'
    if loc_code < 60: return 'mixed'
    if loc_code >= 200: return 'logic-heavy'
    return 'mixed'

# ---------------------------------------------------------------- reload & re-derive
RAW = Path('study/atlas/tmp/raw_classes.tsv')
rows = []
by_fqcn = {}
with open(RAW) as f:
    for line in f:
        line = line.rstrip('\n')
        if not line: continue
        fqcn, path, loc_total, loc_code_r = line.split('\t')
        loc_code = int(loc_code_r.rstrip('~'))
        try:
            body = Path(path).read_text(errors='replace')
        except Exception:
            body = ''
        cls = fqcn.rsplit('.',1)[-1]
        kind = kind_of(fqcn, path, body)
        role = role_of(body, cls)
        lw = logic_weight_of(kind, loc_code)
        conc = concurrency_of(body)
        pers = persistence_of(body)
        row = {
            'fqcn': fqcn, 'path': path,
            'loc_total': int(loc_total), 'loc_code': f'{loc_code}~',
            'kind': kind, 'role_one_liner': role,
            'logic_weight': lw, 'concurrency': conc, 'persistence': pers,
        }
        rows.append(row); by_fqcn[fqcn] = row

# Merge in the enriched fields from the old classes.jsonl (component/feature/entry_points/etc.)
old = {}
for line in open('study/atlas/tmp/classes.jsonl'):
    r = json.loads(line); old[r['fqcn']] = r
for r in rows:
    o = old.get(r['fqcn'])
    if not o: continue
    for k in ('component','feature','key_collaborators','entry_points','invariants',
              'test_exemplar','difficulty','study_minutes','prereq_fqcns',
              'read_order_hint','sharp_edges'):
        r[k] = o.get(k, [] if k in ('key_collaborators','entry_points','invariants','prereq_fqcns','sharp_edges') else '' if k=='test_exemplar' else 0 if k in ('difficulty','study_minutes','read_order_hint') else '')

# Report kind/logic_weight/role changes
changes = {'kind':0,'lw':0,'role':0}
for r in rows:
    o = old.get(r['fqcn'])
    if not o: continue
    if r['kind'] != o.get('kind'): changes['kind'] += 1
    if r['logic_weight'] != o.get('logic_weight'): changes['lw'] += 1
    if r['role_one_liner'] != o.get('role_one_liner'): changes['role'] += 1
print(f'reclassification changes: {changes}')

# ---------------------------------------------------------------- Harvest inferred roles from feature files
inferred_rows = [r for r in rows if r['role_one_liner'].startswith('inferred:')]
harvested = 0
if inferred_rows:
    inferred_by_cls = {r['fqcn'].rsplit('.',1)[-1]: r for r in inferred_rows}
    # Scan every feature file, look for ### <ClassName> under ## Anchor details
    ANCHOR_HDR_RE = re.compile(r'^### `?([A-Za-z_][A-Za-z0-9_]*)`?\s*$', re.MULTILINE)
    for md in COMPS.rglob('*.md'):
        if md.name == 'index.md': continue
        text = md.read_text()
        anch_idx = text.find('## Anchor details')
        if anch_idx < 0: continue
        rel_idx = text.find('\n## Design docs', anch_idx)
        section = text[anch_idx: rel_idx if rel_idx>0 else len(text)]
        for m in ANCHOR_HDR_RE.finditer(section):
            cls = m.group(1)
            if cls not in inferred_by_cls: continue
            # Extract the block until next ### or end
            start = m.end()
            nx = ANCHOR_HDR_RE.search(section, start)
            block = section[start: nx.start() if nx else len(section)]
            # Prefer a '- **role:**' line if present; else first sentence of the block
            role_line = re.search(r'\*\*role:\*\*\s*(.+)', block)
            if role_line:
                role = role_line.group(1).strip()
            else:
                # find first bullet or sentence that is not metadata (path/loc/study/entry/test/collaborators)
                lines = [l.strip() for l in block.strip().splitlines() if l.strip()]
                text_lines = [l for l in lines if not l.startswith('- **')]
                candidate = ' '.join(text_lines).strip()
                m2 = FIRST_SENT_RE.search(candidate)
                role = m2.group(1).strip() if m2 else candidate[:117]
            if not role: continue
            if len(role) > 120: role = role[:117].rstrip() + '...'
            inferred_by_cls[cls]['role_one_liner'] = 'inferred(from-md): ' + role
            harvested += 1
print(f'inferred: roles harvested from feature MDs: {harvested}')

# ---------------------------------------------------------------- write outputs
with open('study/atlas/tmp/classes.jsonl','w') as f:
    for r in rows: f.write(json.dumps(r)+'\n')
with open(ROOT/'atlas.json','w') as f:
    json.dump(rows, f, indent=1)
print(f'atlas.json rewritten: {len(rows)} rows')

# ---------------------------------------------------------------- class-table refresh
# Rewrite ONLY the "## Class table" block in each feature file so class-table
# rows reflect the corrected kind/logic_weight/role. Preserve everything else.
def slug(s): return re.sub(r'[^a-z0-9-]+','-', s.lower()).strip('-')

by_feat = defaultdict(list)
for r in rows: by_feat[(r['component'], r['feature'])].append(r)

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

TABLE_HDR = ('| # | fqcn | kind | logic | loc | difficulty | study (min) | role |'
             '\n|--:|---|---|---|--:|--:|--:|---|')

def render_table(items):
    lines = [TABLE_HDR]
    # Preserve the existing read_order_hint ordering
    items_sorted = sorted(items, key=lambda r: (r.get('read_order_hint') or 999999, r['fqcn']))
    for i, r in enumerate(items_sorted, 1):
        lines.append(
            f'| {i} | `{r["fqcn"]}` | {r["kind"]} | {r["logic_weight"]} | '
            f'{r["loc_code"]} | {r["difficulty"]} | {r["study_minutes"]} | '
            f'{escape_md(r["role_one_liner"])} |'
        )
    return '\n'.join(lines)

CLASS_TABLE_RE = re.compile(
    r'(## Class table\s*\n\s*\n)(.*?)(\n\n## )',
    re.DOTALL
)

tables_rewritten = 0
for (comp, feat), items in by_feat.items():
    md = COMPS / slug(comp) / f'{slug(feat)}.md'
    if not md.exists(): continue
    text = md.read_text()
    new_table = render_table(items) + '\n'
    def repl(m):
        return m.group(1) + new_table + m.group(3)
    new_text, n = CLASS_TABLE_RE.subn(repl, text, count=1)
    if n:
        md.write_text(new_text)
        tables_rewritten += 1
print(f'class tables rewritten: {tables_rewritten}')

# ---------------------------------------------------------------- Link validator
broken = []
LINK_RE = re.compile(r'\]\((?!http|#)([^)]+\.md)(?:#[^)]*)?\)')
for md in COMPS.rglob('*.md'):
    text = md.read_text()
    for m in LINK_RE.finditer(text):
        tgt = m.group(1)
        # Resolve relative to md's parent (or atlas root if starts with /)
        if tgt.startswith('/'):
            resolved = Path(tgt)
        else:
            resolved = (md.parent / tgt).resolve()
        if not resolved.exists():
            broken.append((md.relative_to(ROOT), tgt))
# also check top-level docs
for md in ROOT.glob('*.md'):
    text = md.read_text()
    for m in LINK_RE.finditer(text):
        tgt = m.group(1)
        resolved = (md.parent / tgt).resolve()
        if not resolved.exists():
            broken.append((md.relative_to(ROOT), tgt))
print(f'broken relative links: {len(broken)}')
Path('study/atlas/tmp/broken_links.tsv').write_text(
    '\n'.join(f'{a}\t{b}' for a,b in broken)
)

# ---------------------------------------------------------------- GAPS.md refresh
gaps_lines = ['# Gaps and TODOs', '',
              'Residuals after enrichment. Regenerated by `study/atlas/tmp/reconcile.py`.',
              '',
              '## Reclassification pass changes',
              '',
              f'- kind changes: **{changes["kind"]}**',
              f'- logic_weight changes: **{changes["lw"]}**',
              f'- role_one_liner changes: **{changes["role"]}**',
              f'- inferred: roles harvested from feature-file anchor details: **{harvested}**',
              '',
              '## Broken relative links',
              '',
              f'- **{len(broken)}** broken `[text](target.md)` links across the atlas. See `study/atlas/tmp/broken_links.tsv` for the full list.',
              '',
              '## Classes still with `inferred:` role',
              '']
still_inferred = [r for r in rows if r['role_one_liner'].startswith('inferred:') and not r['role_one_liner'].startswith('inferred(from-md):')]
gaps_lines.append(f'{len(still_inferred)} classes have no Javadoc AND no anchor-details description. Examples:')
gaps_lines.append('')
for r in still_inferred[:20]:
    gaps_lines.append(f'- `{r["fqcn"]}` — `{r["path"]}`')
gaps_lines.append('')
gaps_lines.append('## Modules with no `.java` sources on this branch (excluded from row count)')
gaps_lines.append('')
for m in ('hadoop-ozone/csi','hadoop-ozone/native-client','hadoop-ozone/om-tools',
          'hadoop-hdds/tools','hadoop-hdds/crypto-api','hadoop-hdds/crypto-default',
          'hadoop-ozone/ozonefs-hadoop3-client','hadoop-ozone/ozonefs-shaded',
          'hadoop-ozone/interface-client'):
    gaps_lines.append(f'- `{m}`')
gaps_lines.append('')
gaps_lines.append('## Explicitly excluded (per user decisions)')
gaps_lines.append('')
gaps_lines.append('- `hadoop-ozone/mini-cluster` (7 classes)')
gaps_lines.append('- `hadoop-hdds/test-utils` (18 classes)')
gaps_lines.append('- All `package-info.java` (397 files)')
gaps_lines.append('- All `src/test/**` and `target/generated-sources/**`')
gaps_lines.append('')
gaps_lines.append('## Features with unresolved JIRA history')
gaps_lines.append('')
gaps_lines.append('Enrichment agents flagged the following features as having no confident founding-JIRA in the recent git log window:')
gaps_lines.append('')
for e in ('SCM/pipeline-choose-policy','SCM/scm-audit','OM/om-audit','OM/om-codecs','OM/om-helpers','OM/om-fs','Recon/recon-codegen'):
    gaps_lines.append(f'- `{e}`')
(ROOT/'GAPS.md').write_text('\n'.join(gaps_lines))
print('GAPS.md refreshed')
