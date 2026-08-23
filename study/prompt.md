
ROLE
You are a senior Apache Ozone contributor and technical educator. I am an
engineer who has just started reading the Ozone codebase (repo root:
/Users/rpatel/Github/ozone, branch upstream/master). I want to learn the
entire codebase within 6 months, ~1–2 hours/day, and I will use your output
as my single source of truth for what to read, in what order, and why.

GOAL
Produce a **Class Study Atlas** for Ozone: a hierarchical, queryable index
of every non-test production class, grouped by component → feature →
sub-feature, annotated with the metadata I need to prioritize, sequence,
and track daily progress over 26 weeks. Exhaustiveness beats brevity.

TOP-LEVEL PRIORITIZATION (order the Atlas so I encounter these first)
P0  Client write path (key create → block allocation → chunk write → commit)
P0  Client read path  (key lookup → block/pipeline resolve → chunk read)
P0  Consensus path    (Ratis pipeline, log, snapshot, leader election,
state-machine apply on OM and on DN containers)
P1  Metadata mgmt     (OM RocksDB tables, SCM container/pipeline mgmt, HA)
P1  Storage engine    (DN Container v3 / Schema v3, chunk manager, RocksDB
per-DN, block manager)
P2  Background jobs   (replication manager, container balancer, disk
balancer, scanners, merkle-tree / on-demand
reconciliation, key deleting service, snapshot diff)
P2  Security          (SCM CA, block tokens, S3 secrets, delegation tokens,
Kerberos, Ranger/authorizer)
P3  Interfaces        (S3 gateway, Ofs/O3fs, CSI, Recon, HttpFS)
P3  Ops & tooling     (admin CLIs, freon, debug ldb, upgrade framework,
metrics, tracing)

DELIVERABLE FORMAT — write to disk under
/Users/rpatel/Github/ozone/study/atlas/
as follows:

atlas/README.md               — how to use the atlas + legend
atlas/GLOSSARY.md             — acronyms & Ozone-specific terms
atlas/PREREQUISITES.md        — external concepts to skim first
(Raft, LSM/RocksDB, protobuf, Hadoop RPC,
erasure coding basics, gRPC, Kerberos)
atlas/REPO_MAP.md             — hadoop-hdds/* vs hadoop-ozone/* layers,
common vs interface-client vs
interface-storage, module dependency graph
atlas/ENTRYPOINTS.md          — every daemon's main() with a call-stack
starter map: OzoneManagerStarter,
StorageContainerManagerStarter,
HddsDatanodeService, S3GatewayStarter,
ReconServer, and CLI entry points
atlas/PROTOBUF_MAP.md         — the 4–5 hot protos and the Java classes
that serialize/deserialize/dispatch them
(OMRequest/Response, SCM protocols, DN
container proto, Ratis proto)
atlas/CONFIG_KEYS.md          — index of ozone.* keys from
OzoneConfigKeys / ScmConfigKeys /
OMConfigKeys → consumers
atlas/METRICS.md              — one-liner per *Metrics class, grouped by
component; metrics teach the state model
atlas/DESIGN_DOCS.md          — pointers into hadoop-hdds/docs/ and links
to seminal JIRAs / PRs per feature
atlas/UPGRADES.md             — cross-feature upgrade & finalization
landmines: layout versions, on-disk
format changes (Container v3 / Schema
v3), rolling-upgrade constraints, Ratis
snapshot-compat notes, and the classes
that gate each transition
atlas/INDEX.md                — full hierarchical TOC with links + a
top-level component-relationship mermaid
diagram
atlas/SCHEDULE.md             — 26-week plan (see SCHEDULE spec below)
atlas/PROGRESS.md             — checkbox log I tick daily
atlas/GAPS.md                 — classes you couldn't confidently classify
atlas/components/<comp>/<feature>.md   — one file per feature group;
must contain, in this order:
1. one-paragraph overview
2. mermaid diagram (see
DIAGRAM spec)
3. class table (all rows for
this feature)
4. **Design docs** — links
into hadoop-hdds/docs/
and any Confluence / wiki
pages that shaped the
feature
5. **Seminal JIRAs / PRs** —
3–7 links that teach the
design rationale
(creation JIRA + notable
refactors + biggest bug
fixes)
6. **Sharp edges** —
production landmines,
subtle invariants,
upgrade / finalization
gotchas, cross-version
compat traps
7. **Related features** —
see-also links to other
feature files
8. 5-Q self-quiz in a
<details> block
atlas/atlas.json              — machine-readable copy of every class row
(so I can grep/sort/jq later)

For EACH class row include these fields (JSON keys shown; mirror in Markdown
tables):
fqcn                : fully-qualified class name
path                : repo-relative file path
loc_total           : total lines in file
loc_code            : lines excluding blank, license header, imports,
and /* … */ or // comments
(if exact count is expensive, estimate to nearest 25
and suffix "~"; never leave blank)
kind                : one of {data, dto, config, interface, abstract,
service, state-machine, coordinator, algorithm,
util, factory, rpc-stub, metrics, cli, exception}
logic_weight        : "data-only" | "mixed" | "logic-heavy"
(data-only = getters/setters/builders/protobuf
adapters; logic-heavy = non-trivial algorithms,
state transitions, I/O orchestration)
role_one_liner      : ≤120 chars, active voice, what it DOES not what it IS
key_collaborators   : 3–7 fqcns it calls or is called by
entry_points        : methods that are the "start here" for readers
invariants          : 0–3 bullets on what must always hold (esp. for
state-machines and consensus classes)
concurrency         : "single-threaded" | "thread-safe" | "externally
synchronized" | "actor/queue" | "ratis-applied"
persistence         : which RocksDB table / on-disk file / in-memory only
test_exemplar       : the ONE test class that best teaches this class
(integration test preferred over unit test when it
exercises real flow)
difficulty          : 1–5 (1 = read in 15 min, 5 = multi-day)
study_minutes       : rough budget to reach working understanding
prereq_fqcns        : classes to read FIRST
read_order_hint     : integer within its feature group (1 = read first)
sharp_edges         : 0–2 bullets on known production landmines / bugs
that shaped this class (link JIRA where applicable)

GROUPING RULES
- Group by component (OM, SCM, DN, Client, S3G, Recon, HddsCommon,
  OzoneCommon, Ratis-integration, Security, Tools).
- Under each component, group by feature; under feature, by sub-feature
  when ≥5 classes justify it. Examples of sub-features you must call out
  explicitly if present:
  * container replication (RM, under/over-rep handlers, move scheduler)
  * container scanner (data scanner, metadata scanner, on-demand scanner)
  * merkle tree / container reconciliation
  * disk balancer (DN-local) vs container balancer (cluster-level)
  * snapshot (create, diff, deep-clean, chain, SST filtering)
  * EC (encoder/decoder, EC reconstruction coordinator, EC client)
  * pipeline (creation, state, choose-policy, close, destroy)
  * upgrade / layout / finalization
  * quota (namespace + space, propagation)
  * key deleting service / directory deleting service
  * multi-tenant (tenant mgr, Ranger sync)
- If a class legitimately belongs to two groups, list it in its primary
  group and add a "see also" pointer in the secondary group. Do NOT
  duplicate rows.

CLASS INCLUSION SCOPE
Include every production `.java` under `hadoop-hdds/` and `hadoop-ozone/`,
excluding `**/src/test/**`, generated protobuf classes under
`target/generated-sources/`, and shaded-jar duplicates. Include
package-info files only if they carry design docs.

DIAGRAM spec (mermaid, GitHub-flavored)
- INDEX.md: one top-level `graph LR` of components and their primary
  dependencies (Client → OM/SCM/DN, etc.).
- Every feature file: at least ONE mermaid diagram. Pick the type that
  fits the content — do not force one shape:
  * `sequenceDiagram` for request flows (write path, read path, Ratis
    apply, snapshot diff)
  * `stateDiagram-v2` for lifecycle machines (container states,
    pipeline states, replication states, upgrade states)
  * `classDiagram` for inheritance / interface hierarchies with ≥4 types
  * `flowchart` for decision-heavy background jobs (RM decisions,
    balancer selection)
  * `erDiagram` for RocksDB column-family relationships in a component
- SCHEDULE.md: a mermaid `gantt` chart of the 26-week plan, one row per
  feature group, colored by priority tier.
- PROGRESS.md: a mermaid `pie` or `xychart-beta` cumulative-progress chart
  I can regenerate weekly.
- All diagrams must render on GitHub without extensions. No emoji in
  labels. Keep node counts ≤25 per diagram; split if larger.

SCHEDULE spec (atlas/SCHEDULE.md)
- 26 weeks, 5 study-days/week, ~90 min/day = ~2340 min total budget.
- Fit classes into days by summing `study_minutes` per day ≤ 90.
- Sequence respects `prereq_fqcns` and top-level prioritization.
- Every Friday is a "connect the dots" day: no new classes; instead, one
  sequence diagram + one written recap of the week's flow, plus running
  the `test_exemplar` you found most illuminating.
- End of each month: a milestone check (see MILESTONES below).
- Include the gantt chart described in DIAGRAM spec.

PROGRESS spec (atlas/PROGRESS.md)
- One line per class: `- [ ] YYYY-MM-DD  <fqcn>  (<minutes>)  notes:`
- One line per weekly recap and monthly milestone.
- A top summary block auto-fillable by me: classes read / total, %
  complete, streak, hours logged.
- Include the cumulative-progress chart described in DIAGRAM spec.

MILESTONES (put in SCHEDULE.md)
M1 (wk 4)  Explain end-to-end write path on a whiteboard from
OzoneClient down to DN chunk file, naming every class on the
path.
M2 (wk 8)  Explain end-to-end read path incl. pipeline selection & EC
read.
M3 (wk 12) Explain OM Ratis apply loop and one non-trivial
OMClientRequest lifecycle (double-buffer, cache, response).
M4 (wk 16) Explain SCM container lifecycle + replication manager
decisions.
M5 (wk 20) Explain snapshot create + snapshot diff + deep-clean.
M6 (wk 24) Explain one full background service of choice, top to
bottom.
M7 (wk 26) Contribute a docs PR or a bug-fix PR touching ≥2 components.

SELF-QUIZ spec (per feature file)
- 5 questions, mix of "identify the class that…", "what invariant does X
  preserve?", and "trace the call from A to B".
- Answers hidden in a `<details>` block below the questions.

METHOD (how you build this)
1. Enumerate candidate classes with a fast `find … -name '*.java' | grep
   -v /test/` sweep; count with `wc -l` and a comment-stripping heuristic
   (strip lines matching `^\s*(//|\*|/\*|\*/)` and blank; not perfect,
   mark loc_code with `~`).
2. Fan out with parallel `Explore` subagents (one per top-level
   component) to classify kind/logic_weight/role from the class body and
   Javadoc.
3. Merge, dedupe, wire cross-refs, then emit files.
4. Do NOT summarize into prose paragraphs — dense tables, bullets, and
   mermaid diagrams only.

CONSTRAINTS
- No hallucinated fqcns. If you're not sure a class exists on this
  branch, omit it and note the gap in atlas/GAPS.md.
- Every claim about a class must be traceable to a file+line you actually
  read; when you infer, prefix with "inferred:".
- Every mermaid diagram must reference real fqcns from atlas.json.
- Every JIRA / PR link must be a real Apache JIRA (HDDS-*) or GitHub PR
  URL. If you cannot verify one, put "TODO(verify)" — do not fabricate.
- Sharp-edges bullets must cite either a JIRA, a code comment, or a
  concrete file+line; no folklore.
- Match the surrounding docs style — Markdown, GitHub-flavored, no emoji,
  no marketing tone.

FIRST OUTPUT
Before writing anything to disk, print:
(a) your class-count estimate per top-level component,
(b) any scope ambiguities you want me to resolve.
Then wait for my "go" before generating the atlas.

CONSIDERED AND DROPPED (do NOT reintroduce unless I ask)
- Tiering by priority (full detail for P0/P1 only). Superseded by the
  "exhaustiveness beats brevity" stance.
- "Actionable over exhaustive" preference. Same reason.
