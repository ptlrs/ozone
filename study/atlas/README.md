# Class Study Atlas — Apache Ozone

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
| `logic_weight` | `data-only` \| `mixed` \| `logic-heavy` |
| `role_one_liner` | active-voice, ≤120 chars, what the class DOES |
| `key_collaborators` | 3–7 fqcns it calls or is called by |
| `entry_points` | methods that are the "start here" for readers |
| `invariants` | 0–3 bullets on what must always hold |
| `concurrency` | `single-threaded` \| `thread-safe` \| `externally synchronized` \| `actor/queue` \| `ratis-applied` |
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
