#!/usr/bin/env python3
"""Emit SCHEDULE.md (26 weeks, curated) and PROGRESS.md."""
import json, re
from pathlib import Path
from collections import defaultdict, OrderedDict

ROOT = Path('study/atlas')
rows = [json.loads(l) for l in open('study/atlas/tmp/classes.jsonl')]
by_feat = defaultdict(list)
for r in rows: by_feat[(r['component'], r['feature'])].append(r)

# Curated week plan. Each week is a list of (component, feature) tuples.
# Fridays are recap days (no new reads). The plan follows the top-level
# prioritization P0 -> P1 -> P2 -> P3 with an early one-week onboarding.
WEEKS = [
    # ---------- P0 CORE ----------
    ('W01 Onboarding', [('Client','ozone-client'), ('OzoneCommon','client-common')]),
    ('W02 Client write path (RPC)',   [('Client','ozone-client'), ('OM','om-protocol'), ('OM','om-request')]),
    ('W03 Client write path (blocks)',[('Client','hdds-client'), ('SCM','block-manager'), ('SCM','pipeline-manager')]),
    ('W04 Client write path (chunks)',[('Client','hdds-client'), ('DN','kv-container'), ('DN','kv-container-impl')]),
    # -- M1 milestone --
    ('W05 Client read path',          [('Client','ozone-client'), ('Client','hdds-client'), ('DN','container-interfaces')]),
    ('W06 Client read path (EC)',     [('DN','erasure-coding'), ('OM','om-request-key')]),
    ('W07 Consensus: Ratis integration', [('Ratis-integration','ratis-integration'), ('HddsCommon','ratis-integration')]),
    ('W08 Consensus: OM apply',       [('OM','om-ratis'), ('OM','om-request-key')]),
    # -- M2 milestone --
    ('W09 Consensus: OM response + double buffer', [('OM','om-response'), ('OM','om-execution')]),
    ('W10 Consensus: DN state machine',[('DN','ratis-statemachine-dn'), ('DN','kv-container')]),

    # ---------- P1 METADATA + STORAGE ----------
    ('W11 OM key manager & metadata', [('OM','om-server'), ('OM','om-key-manager'), ('OM','interface-storage')]),
    ('W12 OM bucket/volume manager', [('OM','om-bucket-manager'), ('OM','om-volume-manager'), ('OM','om-request-bucket'), ('OM','om-request-volume')]),
    # -- M3 milestone --
    ('W13 OM locking + codecs',       [('OM','om-locking'), ('OM','om-codecs'), ('OM','interface-storage')]),
    ('W14 SCM containers',            [('SCM','container-manager'), ('HddsCommon','container-common')]),
    ('W15 SCM pipelines',             [('SCM','pipeline-manager'), ('SCM','pipeline-choose-policy'), ('HddsCommon','pipeline-common')]),
    ('W16 SCM replication manager',   [('SCM','container-replication')]),
    # -- M4 milestone --
    ('W17 SCM HA + safemode + node',  [('SCM','scm-ha'), ('SCM','safemode'), ('SCM','node-manager')]),
    ('W18 DN volumes + rocksdb',      [('DN','hdds-volume'), ('DN','dn-rocksdb'), ('RocksDB','managed-rocksdb')]),
    ('W19 DN state machine + reports',[('DN','dn-statemachine'), ('DN','dn-reports'), ('DN','dn-command-handlers' if ('DN','dn-command-handlers') in by_feat else 'dn-scm-commands')]),

    # ---------- P2 BG + SECURITY ----------
    ('W20 OM snapshot',               [('OM','om-snapshot'), ('OM','om-request-snapshot'), ('OzoneCommon','snapshot-common')]),
    # -- M5 milestone --
    ('W21 RocksDB checkpoint differ', [('RocksDB','checkpoint-differ'), ('RocksDB','rocks-native')]),
    ('W22 DN background: scanner + reconciliation + balancer',
        [('DN','container-replication-dn'), ('SCM','container-balancer'), ('DN','disk-balancer')]),
    ('W23 OM background services',    [('OM','om-background-services'), ('OM','om-upgrade')]),
    # -- M6 milestone --
    ('W24 Security: certs + tokens',  [('Security','security-x509'), ('Security','security-tokens'), ('OM','om-security'), ('SCM','scm-security')]),

    # ---------- P3 INTERFACES + TOOLS ----------
    ('W25 Interfaces: S3 + OzoneFS + Recon glance',
        [('Interfaces','s3gateway'), ('Interfaces','ozonefs-common'), ('Recon','recon-server')]),
    ('W26 Tools + wrap-up + M7 PR',
        [('Admin CLIs','admin'), ('Debug & Repair','debug'), ('Bench & Insight','freon')]),
]

def slug(s): return re.sub(r'[^a-z0-9-]+','-', s.lower()).strip('-')

# Milestone map: week number -> milestone text
MILESTONES = {
    4:  ('M1', 'Explain end-to-end write path on a whiteboard from `OzoneClient` down to DN chunk file, naming every class on the path.'),
    8:  ('M2', 'Explain end-to-end read path, including pipeline selection and EC read.'),
    12: ('M3', 'Explain OM Ratis apply loop and one non-trivial `OMClientRequest` lifecycle (double-buffer, cache, response).'),
    16: ('M4', 'Explain SCM container lifecycle + replication manager decisions.'),
    20: ('M5', 'Explain snapshot create + snapshot diff + deep-clean.'),
    23: ('M6', 'Explain one full background service of choice, top to bottom.'),
    26: ('M7', 'Contribute a docs PR or a bug-fix PR touching >=2 components.'),
}

# Build per-week daily plan (D1..D4 = new reads, D5 = recap).
# Each new-read day gets ~90 min of anchor reads from the week's features,
# ranked by read_order_hint (already sorted). If features run out, backfill
# with any remaining anchors in the feature.

GLOBAL_USED = set()  # de-dupe reads across the entire 26-week plan

def budget_days(week_features, per_feature_cap=4):
    """Round-robin over features so every feature in the week is represented.
    Each feature contributes up to `per_feature_cap` classes; then we pack days
    with sum(study_minutes) <= 90.
    """
    lanes = []
    for cf in week_features:
        if not isinstance(cf, tuple) or cf not in by_feat: continue
        items = by_feat[cf]
        def rk(r):
            return (0 if r['logic_weight']=='logic-heavy' else 1,
                    -r['difficulty'],
                    -int(r['loc_code'].rstrip('~')))
        ranked = [r for r in sorted(items, key=rk) if r['fqcn'] not in GLOBAL_USED]
        lanes.append(ranked[:per_feature_cap])

    # Round-robin merge to guarantee feature balance
    pool = []
    i = 0
    while any(lanes):
        lane = lanes[i % len(lanes)]
        if lane:
            pool.append(lane.pop(0))
        else:
            # remove empty lane
            del lanes[i % len(lanes)]
            if not lanes: break
            continue
        i += 1

    days = [[] for _ in range(4)]
    daily_budget = 90
    for r in pool:
        placed = False
        for d in range(4):
            if sum(x['study_minutes'] for x in days[d]) + r['study_minutes'] <= daily_budget:
                days[d].append(r); GLOBAL_USED.add(r['fqcn']); placed = True; break
        if not placed:
            continue
    return days

# Emit SCHEDULE.md
lines = ['# 26-Week Study Schedule', '',
         '5 days/week, ~90 minutes/day. Monday–Thursday are new reads (D1..D4). Friday (D5) is a **connect-the-dots** day: no new classes; do the weekly recap (one sequence diagram + prose) and run the most illuminating `test_exemplar` you found this week.',
         '',
         '## Gantt', '',
         '```mermaid',
         'gantt',
         '  title Ozone 26-week class-study atlas',
         '  dateFormat  X',
         '  axisFormat  W%w',
         '']

gantt_phases = [
    ('P0 Client write path',                 'client-write',  1, 4),
    ('P0 Client read path',                  'client-read',   5, 6),
    ('P0 Consensus Ratis+OM+DN',             'consensus',     7, 10),
    ('P1 Metadata management OM SCM DN',     'metadata',     11, 19),
    ('P2 Background + snapshot + security',  'bg',           20, 24),
    ('P3 Interfaces + tooling + contribution','p3',          25, 26),
]
lines.append('  section Phases')
for name, tag, s, e in gantt_phases:
    lines.append(f'  {name} :{tag}, {s}, {e - s + 1}d')
lines.append('```')
lines.append('')
lines.append('## Weekly plan')
lines.append('')

for idx, (title, feats) in enumerate(WEEKS, 1):
    lines.append(f'### {title}  (Week {idx:02d})')
    lines.append('')
    lines.append(f'**Feature focus:** ' + ', '.join(f'`{c}/{f}`' if isinstance(f,str) and (c,f) in by_feat else f'`{c}/{f}`?' for c,f in feats if isinstance(f,str)))
    lines.append('')
    days = budget_days(feats)
    lines.append('| Day | Class | study (min) | feature |')
    lines.append('|---|---|--:|---|')
    for d, day in enumerate(days, 1):
        if not day:
            lines.append(f'| D{d} | _(pool exhausted for this week; catch up on prior reads or extend a feature file)_ | | |')
            continue
        for r in day:
            lines.append(f'| D{d} | `{r["fqcn"]}` | {r["study_minutes"]} | {r["component"]}/{r["feature"]} |')
    lines.append(f'| D5 | **Weekly recap** — write a sequence diagram (see `INDEX.md` legend) for one flow you learned. Run the test exemplar you found most illuminating. | 90 |  |')
    if idx in MILESTONES:
        mid, txt = MILESTONES[idx]
        lines.append('')
        lines.append(f'**Milestone {mid} at end of Week {idx}:** {txt}')
    lines.append('')

lines.append('## Scope note')
lines.append('')
lines.append(f'The atlas indexes **2,748** classes; the naive read-time budget for all of them at 30 min/class is well over 1,000 hours. This 26-week schedule curates approximately 15% of the classes — the anchor rows across the P0/P1/P2 features — so the daily budget fits ~90 minutes. Every class not in the schedule is still catalogued in `components/` and `atlas.json`; the schedule is a reading order, not a coverage guarantee. When you have finished the 26 weeks, use `atlas.json` to pick further reading by feature or by `logic_weight=logic-heavy` filter.')
(ROOT / 'SCHEDULE.md').write_text('\n'.join(lines))

# ---- PROGRESS.md ----
lines = ['# Progress Log', '',
         'One line per class read; one line per weekly recap; one line per milestone.',
         '',
         '## Summary (fill in as you go)',
         '',
         '- Classes read: __ / 2748 total (only anchor rows expected on the 26-week schedule).',
         '- Weeks complete: __ / 26',
         '- Current streak: __ days',
         '- Hours logged: __',
         '',
         '## Cumulative-progress chart (regenerate weekly)',
         '',
         '```mermaid',
         'xychart-beta',
         '  title "Weekly reading progress (classes)"',
         '  x-axis [W01, W02, W03, W04, W05, W06, W07, W08, W09, W10, W11, W12, W13, W14, W15, W16, W17, W18, W19, W20, W21, W22, W23, W24, W25, W26]',
         '  y-axis "classes read" 0 --> 200',
         '  bar [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]',
         '  line [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]',
         '```',
         '',
         '_Replace the two zero-lists as you progress; the `bar` array is weekly counts and `line` array is cumulative._',
         '',
         '## Daily log',
         '',
         'Format: `- [ ] Wnn Dn  <fqcn>  (<minutes>)  notes:`',
         '']

# Pre-fill checkboxes with the schedule so you can just tick them
for idx, (title, feats) in enumerate(WEEKS, 1):
    lines.append(f'### Week {idx:02d} — {title}')
    lines.append('')
    days = budget_days(feats)
    for d, day in enumerate(days, 1):
        for r in day:
            lines.append(f'- [ ] W{idx:02d} D{d}  `{r["fqcn"]}`  ({r["study_minutes"]})  notes:')
    lines.append(f'- [ ] W{idx:02d} D5  **Weekly recap: sequence diagram + test-exemplar run**  (90)  notes:')
    if idx in MILESTONES:
        mid, txt = MILESTONES[idx]
        lines.append(f'- [ ] **Milestone {mid}** — {txt}')
    lines.append('')

(ROOT / 'PROGRESS.md').write_text('\n'.join(lines))
print('emitted SCHEDULE.md and PROGRESS.md')
