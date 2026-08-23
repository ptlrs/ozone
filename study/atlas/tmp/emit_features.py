#!/usr/bin/env python3
"""Emit components/<comp>/index.md and components/<comp>/<feature>.md files."""
import json, re, os
from pathlib import Path
from collections import defaultdict, Counter

ROOT = Path('study/atlas')
COMPS = ROOT / 'components'
rows = [json.loads(l) for l in open('study/atlas/tmp/classes.jsonl')]

by_comp = defaultdict(list)
by_feat = defaultdict(list)
for r in rows:
    by_comp[r['component']].append(r)
    by_feat[(r['component'], r['feature'])].append(r)

def slug(s): return re.sub(r'[^a-z0-9-]+','-', s.lower()).strip('-')

# ---- test exemplar name-match ----
import subprocess
test_files = subprocess.check_output(
    "find hadoop-hdds hadoop-ozone -name 'Test*.java' -path '*/src/test/*' -not -path '*/target/*'",
    shell=True, text=True
).strip().splitlines()
test_by_name = {}
for tf in test_files:
    n = os.path.basename(tf).replace('.java','')
    stripped = n.replace('Test','',1)
    # prefer integration tests
    is_int = '/integration-test' in tf
    prev = test_by_name.get(stripped)
    if prev is None or (is_int and '/integration-test' not in prev):
        test_by_name[stripped] = tf

# populate test_exemplar
for r in rows:
    cls = r['fqcn'].rsplit('.',1)[-1]
    tf = test_by_name.get(cls)
    if tf:
        r['test_exemplar'] = tf

# key_collaborators heuristic: scan file for imports from ozone/hdds namespaces of other classes with kind != rpc-stub
IMPORT_RE = re.compile(r'^import\s+(org\.apache\.hadoop\.(?:hdds|ozone|ratis)\.[a-zA-Z0-9_.]+);', re.MULTILINE)
by_fqcn = {r['fqcn']:r for r in rows}
for r in rows:
    try:
        src = Path(r['path']).read_text(errors='replace')
    except Exception:
        continue
    imps = IMPORT_RE.findall(src)
    # keep those we know about
    known = [i for i in imps if i in by_fqcn and i != r['fqcn']]
    # rank by same-feature preference
    same_feat = [k for k in known if (by_fqcn[k]['component'], by_fqcn[k]['feature']) == (r['component'], r['feature'])]
    cross = [k for k in known if k not in same_feat]
    r['key_collaborators'] = (same_feat + cross)[:6]

# entry_points heuristic: public methods with name in {start,init,run,call,apply,handle,execute,process,onMessage,submit,build,create,open,close,write,read,commit,allocate,addContainer,replicate,heartbeat,applyTransaction,takeSnapshot,notifyLeaderChanged,pauseState}
ENTRY_METHODS = re.compile(r'^\s*public\s+(?:static\s+|final\s+|synchronized\s+|abstract\s+)*[\w<>\[\]?, ]+?\s+(start|init|run|call|apply|handle|execute|process|onMessage|submit|build|create|open|close|write|read|commit|allocate|replicate|heartbeat|applyTransaction|takeSnapshot|notifyLeaderChanged|pauseState|preExecute|validateAndUpdateCache|onMessageInternal|onRatisApply)\s*\(', re.MULTILINE)
for r in rows:
    try:
        src = Path(r['path']).read_text(errors='replace')
    except Exception:
        continue
    m = ENTRY_METHODS.findall(src)
    seen = []
    for name in m:
        if name not in seen: seen.append(name)
    r['entry_points'] = seen[:4]

# read_order_hint per feature: sort by (kind_rank, -loc_code)
KIND_RANK = {
    'exception': 9, 'data':7, 'dto':7, 'config':5, 'util':6, 'factory':6, 'metrics':8,
    'rpc-stub':6, 'interface':2, 'abstract':3, 'state-machine':1, 'service':4,
    'coordinator':4,'algorithm':4,'cli':7,
}
for key, items in by_feat.items():
    def sort_key(r):
        rank = KIND_RANK.get(r['kind'], 5)
        loc = -int(r['loc_code'].rstrip('~'))
        return (rank, loc)
    items.sort(key=sort_key)
    for i, r in enumerate(items, 1):
        r['read_order_hint'] = i

# rewrite classes.jsonl with the enriched rows and atlas.json
with open('study/atlas/tmp/classes.jsonl','w') as f:
    for r in rows: f.write(json.dumps(r)+'\n')
with open(ROOT/'atlas.json','w') as f:
    json.dump(rows, f, indent=1)

# ---- write component index and feature files ----
COMPS.mkdir(exist_ok=True)

COMP_META = {
    'Client': ('P0', 'The client is the on-ramp for every read/write. Read this component first: `OzoneClient` orchestrates the OM RPC + SCM block allocation + DN chunk I/O.'),
    'OM': ('P0/P1', 'OM owns the namespace and the write-side apply loop. Every write is an `OMClientRequest` transformed through preExecute → validate → apply → response.'),
    'SCM': ('P0/P1', 'SCM owns the container/pipeline plane and safemode. The replication manager decides who holds which container copy.'),
    'DN': ('P0/P1', 'Datanodes hold the actual data. Read the Ratis state machine, the KeyValue container, chunk manager, and the container scanner.'),
    'Ratis-integration': ('P0', 'Small module of Ratis client/server helpers shared by OM, SCM, and DN pipelines.'),
    'Security': ('P2', 'Certificates (X.509), symmetric secret keys, tokens, SSL helpers. All secure-mode-only.'),
    'RocksDB': ('P1', 'Managed-rocksdb wrappers (memory-managed handles), the checkpoint-differ used for snapshot diff, and rocks-native bindings.'),
    'HddsCommon': ('P1', 'The plumbing every server uses: HTTP server, config framework, table codecs, tracing, protocol-common types, ozone-common primitives.'),
    'OzoneCommon': ('P1', 'Shared Ozone-layer types: `OmKeyInfo`, `OmBucketInfo`, snapshot common types, s3 common types.'),
    'Recon': ('P2', 'Observability service with its own OM/SCM shadow database and REST API.'),
    'Interfaces': ('P3', 'External protocol surfaces: S3 Gateway, HttpFS, OzoneFS (Hadoop2/3), Iceberg, multi-tenancy Ranger sync.'),
    'Admin CLIs': ('P3', '`ozone admin` and `ozone sh` command trees + shared picocli scaffolding.'),
    'Debug & Repair': ('P3', '`ozone debug` (ldb, container, replicas) and `ozone repair` operator tools.'),
    'Bench & Insight': ('P3', 'Freon (benchmark), Insight (state introspection), Vapor, misc tools.'),
}

def escape_md(s):
    s = s.replace('|', '\\|').replace('\n', ' ').strip()
    return mdx_safe(s)


# Sanitize javadoc / raw-HTML fragments that leak in from source-file
# comments, so downstream MDX consumers do not choke on `<br>`, `<code>`,
# `{@link ...}`, or unbalanced `<`/`{`. Callers still see plain readable
# text; the substitutions are visible but harmless.
def mdx_safe(s):
    import re as _re
    # Unwrap `{@link Foo}` / `{@code Foo}` to their inner reference.
    s = _re.sub(r'\{@link\s+([^}]+)\}', r'\1', s)
    s = _re.sub(r'\{@code\s+([^}]+)\}', r'\1', s)
    # Escape remaining `<`, `>`, `{`, `}` as HTML entities so MDX renders
    # them as plain characters instead of interpreting them as JSX.
    s = s.replace('&', '&amp;')
    s = s.replace('<', '&lt;').replace('>', '&gt;')
    s = s.replace('{', '&#123;').replace('}', '&#125;')
    return s

for comp, tier_blurb in COMP_META.items():
    tier, blurb = tier_blurb
    cdir = COMPS / slug(comp)
    cdir.mkdir(parents=True, exist_ok=True)
    feats = sorted(set(r['feature'] for r in by_comp[comp]))
    # component index
    lines = [f'# Component: {comp}', '',
             f'**Tier:** {tier}    **Classes:** {len(by_comp[comp])}    **Features:** {len(feats)}',
             '', blurb, '',
             '## Feature groups',
             '',
             '| Feature | Classes | Anchors (logic-heavy) |',
             '|---|---:|---:|']
    for f in feats:
        items = by_feat[(comp, f)]
        anch = sum(1 for r in items if r['logic_weight']=='logic-heavy')
        lines.append(f'| [{f}]({slug(f)}.md) | {len(items)} | {anch} |')
    (cdir / 'index.md').write_text('\n'.join(lines))

    # feature files
    for f in feats:
        items = list(by_feat[(comp, f)])
        # already sorted by read_order_hint via sort above
        overview = f'The `{f}` feature group in {comp} contains **{len(items)}** classes. '
        anch_names = [r['fqcn'].rsplit('.',1)[-1] for r in items[:6]]
        overview += f'Read in this order (kind + line-count derived): {", ".join(anch_names[:4])}.'
        # kind mix
        km = Counter(r['kind'] for r in items)
        mix = ', '.join(f'{k}:{v}' for k,v in km.most_common())

        # placeholder mermaid — sequenceDiagram for request-shaped features, else classDiagram of top 4 classes
        shape = 'classDiagram' if not any(w in f for w in ('request','ratis','write','read','commit','apply','replication','scanner','background','service')) else 'flowchart'
        # Guard: pick a diagram that always renders
        top4 = anch_names[:4] if len(anch_names)>=2 else anch_names + ['<see class table>']
        mermaid = ['```mermaid', 'flowchart LR']
        prev=None
        for n in top4:
            safe = re.sub(r'[^\w]', '', n)
            mermaid.append(f'  {safe}["{n}"]')
        for i in range(len(top4)-1):
            a = re.sub(r'[^\w]', '', top4[i])
            b = re.sub(r'[^\w]', '', top4[i+1])
            mermaid.append(f'  {a} --> {b}')
        mermaid.append('```')

        # class table
        header = ('| # | fqcn | kind | logic | loc | difficulty | study (min) | role |'
                  '\n|--:|---|---|---|--:|--:|--:|---|')
        table_lines = [header]
        for i, r in enumerate(items, 1):
            table_lines.append(
                f'| {i} | `{r["fqcn"]}` | {r["kind"]} | {r["logic_weight"]} | '
                f'{r["loc_code"]} | {r["difficulty"]} | {r["study_minutes"]} | '
                f'{escape_md(r["role_one_liner"])} |'
            )
        # collaborators + entry points for anchors only (to keep files digestible)
        anchor_block = []
        anchors_here = [r for r in items if r['logic_weight']=='logic-heavy']
        for r in anchors_here[:12]:  # cap per file to prevent unbounded growth
            anchor_block.append(f'### `{r["fqcn"].rsplit(".",1)[-1]}`')
            anchor_block.append('')
            anchor_block.append(f'- **path:** `{r["path"]}`')
            anchor_block.append(f'- **loc:** {r["loc_code"]}    **difficulty:** {r["difficulty"]}    **study:** {r["study_minutes"]} min    **concurrency:** {r["concurrency"]}    **persistence:** {mdx_safe(r["persistence"])}')
            if r['entry_points']:
                anchor_block.append(f'- **entry points:** {", ".join("`"+e+"`" for e in r["entry_points"])}')
            if r['key_collaborators']:
                anchor_block.append(f'- **key collaborators:** ' + ', '.join(f'`{c}`' for c in r['key_collaborators']))
            if r['test_exemplar']:
                anchor_block.append(f'- **test exemplar:** `{r["test_exemplar"]}`')
            anchor_block.append(f'- **role:** {mdx_safe(r["role_one_liner"])}')
            anchor_block.append('')

        # Quiz — 5 questions, mostly templated, one derived from a real class name
        first_service = next((r for r in items if r['kind'] in ('service','state-machine','abstract')), items[0])
        first_data = next((r for r in items if r['kind'] in ('data','dto')), items[0])
        fs_name = first_service['fqcn'].rsplit('.',1)[-1]
        fd_name = first_data['fqcn'].rsplit('.',1)[-1]
        quiz = [
            f'1. Identify the class in this feature group that owns the primary lifecycle callback (start/apply/handle). Justify from `entry_points`.',
            f'2. What thread-safety guarantee does `{fs_name}` provide, and what evidence in its `concurrency` field supports that?',
            f'3. Which class(es) here persist to a RocksDB table, and which table(s)?',
            f'4. Pick one `data`/`dto` class (e.g. `{fd_name}`) and describe the on-wire and on-disk representation.',
            f'5. Trace the call from any client-facing entry point (see `components/Client/*.md`) into a method of a class in this feature. Name every intermediate class.',
        ]
        # Answers
        answers = [
            f'Answer 1: the class whose kind is `service` or `state-machine` and whose `entry_points` includes `start`/`apply`/`handle`. See the "Anchor details" section above.',
            f'Answer 2: read the `concurrency` field in the class row of the anchor table. `ratis-applied` means single apply thread; `thread-safe` means locking/atomics.',
            f'Answer 3: look for the `persistence` field starting with `RocksDB:` in the class table.',
            f'Answer 4: check the class body for a matching Protobuf builder (`toProto()` / `fromProto()`) and a `Codec` under `hadoop-hdds/framework/.../db/`.',
            f'Answer 5: use the call-stack starter map in `ENTRYPOINTS.md`; augment with the `key_collaborators` field of each intermediate anchor.',
        ]

        lines = [
            f'# {comp} / {f}', '',
            f'**Classes:** {len(items)}    **Kinds:** {mix}', '',
            '## Overview',
            '',
            overview,
            '',
            '## Diagram',
            '',
            *mermaid,
            '',
            '_Diagram is a placeholder wiring of the top read-order-hint classes; the Explore-agent enrichment pass replaces this with a purpose-fit sequence / state / class / flowchart diagram._',
            '',
            '## Class table',
            '',
            *table_lines,
            '',
            '## Anchor details',
            '',
            '\n'.join(anchor_block) if anchor_block else '_No logic-heavy anchors in this feature; the classes are primarily data / dto / config / cli._',
            '',
            '## Design docs',
            '',
            '- See `DESIGN_DOCS.md` for the category index of `hadoop-hdds/docs/content/*` pages. The pass that binds specific pages to this feature is Explore-agent driven; expect `TODO(verify)` here for the next generation.',
            '',
            '## Seminal JIRAs / PRs',
            '',
            '- TODO(verify) — populated by Explore-agent enrichment via `git log --grep` and code-comment mining.',
            '',
            '## Sharp edges',
            '',
            '- TODO(verify) — populated by the enrichment pass.',
            '',
            '## Related features',
            '',
            '- See `components/' + slug(comp) + '/index.md` for sibling features in this component and `INDEX.md` for cross-component links.',
            '',
            '## Self-quiz',
            '',
            '\n'.join(quiz),
            '',
            '<details>',
            '<summary>Answers</summary>',
            '',
            '\n'.join(answers),
            '',
            '</details>',
            '',
        ]
        (cdir / f'{slug(f)}.md').write_text('\n'.join(lines))

print(f'emitted {sum(1 for c in COMP_META)} component indexes and {sum(len(set(r["feature"] for r in by_comp[c])) for c in COMP_META)} feature files')
