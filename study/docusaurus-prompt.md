ROLE
You are a senior full-stack engineer building a local Docusaurus 3 site over
the existing Apache Ozone "Class Study Atlas" at
/Users/rpatel/Github/worktrees/ozone/ozone-learning/study/atlas/.

READ FIRST (do not skip; print a 10-line summary before writing code):
  study/prompt.md
  study/atlas/README.md
  study/atlas/INDEX.md
  study/atlas/SCHEDULE.md      (skim)
  study/atlas/atlas.json       (skim; ~2.6 MB — read the schema + sample rows)
  one sample feature file, e.g. study/atlas/components/client/ozone-client.md

HARD RULES
  - Do NOT edit anything under study/atlas/. Treat it as read-only source.
  - Do NOT fabricate fqcns, JIRA ids, PR numbers, or design-doc links.
  - Every class in atlas.json gets its own /class/<slug> detail page.
  - Every source path or fqcn mentioned anywhere in generated MDX is a link.
  - Regenerating the site must preserve hand-authored MDX sections
    (see MERGE RULES).

GOAL
Deliver study/site/, a Docusaurus 3 project that runs with `npm run start`
and includes: full-text offline search, dark mode, mermaid, per-class detail
pages, backlinks, parallel "views" of the same data, and a source-link
toggle between opening files in JetBrains IDEA locally vs. on GitHub.

DELIVERABLES  (all under study/site/)
  package.json, docusaurus.config.js, sidebars.js, tsconfig.json
  scripts/
    build-atlas.mjs          — reads ../atlas/atlas.json + ../atlas/**.md,
                               emits docs/**/*.mdx, sidebar entries, tag &
                               view indexes, and docs/_data/fqcn-index.json.
                               Idempotent. Splits into small modules by
                               concern (atlas load, class pages, feature
                               pages, views, mermaid rewrite, backlink graph,
                               sidebar emit).
    verify-links.mjs         — walks docs/ and prints unresolved fqcns and
                               broken internal links; non-zero exit on any.
    views/<view-id>.js       — per-view selectors (see VIEWS).
    config.local.example.js  — user copies to config.local.js.
  src/
    theme/                   — swizzled bits as needed (e.g. Footer).
    components/
      SourceLink.tsx         — one <a> whose href is resolved at click time
                               from window.__ATLAS_CONFIG.
      Fqcn.tsx               — <Fqcn>org.apache…RpcClient</Fqcn> shows short
                               name, tooltip = role_one_liner, links to
                               /class/<slug>. Falls back to raw text with a
                               warning-style border if the fqcn is unknown.
      Backlinks.tsx          — "Referenced by" list computed at build time.
      LinkModeToggle.tsx     — header toggle: 'jetbrains' | 'github'.
      ClassBadge.tsx         — chips: kind, difficulty, tier, minutes.
      ProgressCheckbox.tsx   — localStorage-backed per-class read state.
      MermaidClickable.tsx   — post-process wrapper if runtime rewrite needed
                               (prefer build-time rewrite).
      ViewBuilder.tsx        — /view/builder UI (see VIEW BUILDER).
    css/custom.css           — light + dark tokens; consistent tag colors.
  docs/                      — generated + hand-authored MDX; see LAYOUT.
  static/
  screenshots/               — one after each implementation step.
  README.md                  — run, regenerate, configure, add view/tag/note.

URL LAYOUT
  /                              landing (see below)
  /guide/                        reading-order guide (from INDEX.md + SCHEDULE.md)
  /reference/repo-map            from REPO_MAP.md
  /reference/entrypoints         from ENTRYPOINTS.md
  /reference/protobuf-map        from PROTOBUF_MAP.md
  /reference/config-keys         from CONFIG_KEYS.md
  /reference/metrics             from METRICS.md
  /reference/glossary            from GLOSSARY.md
  /reference/prerequisites       from PREREQUISITES.md
  /reference/design-docs         from DESIGN_DOCS.md
  /reference/upgrades            from UPGRADES.md
  /reference/gaps                from GAPS.md
  /component/<comp>/             component index (auto-generated)
  /component/<comp>/<feature>    feature page (atlas source + hand-authored)
  /class/<slug>                  per class (auto-generated, EVERY class)
  /view/                         index of all views
  /view/<view-id>                one page per view
  /view/builder                  DIY view builder (see below)
  /firehose/sharp-edges          all sharp_edges bullets, filterable
  /firehose/invariants           all invariants bullets, filterable
  /progress                      localStorage-backed tracker

LANDING PAGE
  - Hero: one-line pitch; primary CTAs to Reading Guide, Views, Search.
  - Component graph (import INDEX.md's mermaid); nodes clickable to
    /component/<comp>.
  - "Continue where you left off" card (reads localStorage).
  - Featured views: write-path-e2e, read-path-e2e, ec-e2e, ratis-consensus.
  - Search box (uses the offline search plugin).

SOURCE LINKS (JetBrains navigate scheme; GitHub as toggle)
  config.local.js shape (gitignored; commit only config.local.example.js):
    module.exports = {
      repoRoot: '/Users/rpatel/Github/ozone',
      githubRepo: 'apache/ozone',
      githubRef: 'master',            // sha preferred for stability
      jetbrainsProject: 'ozone',      // must match IDEA's project name
      defaultMode: 'jetbrains',       // 'jetbrains' | 'github'
    }
  <SourceLink path="hadoop-ozone/.../X.java" line={123}/> resolves at click:
    jetbrains mode:
      jetbrains://idea/navigate/reference?project=<project>&path=<path>&line=<line>
    github mode:
      https://github.com/<repo>/blob/<ref>/<path>#L<line>
  Header LinkModeToggle overrides defaultMode; state persisted in localStorage.
  If config.local.js is absent, run in github mode and show a top banner
  linking to config.local.example.js.

CLASS DETAIL PAGE (/class/<slug>)  — generated for EVERY class in atlas.json
  Slug: lowercase last segment; on collision append -<packageHash6>. The map
  slug -> fqcn is written to docs/_data/fqcn-index.json.
  Template selection:
    - full template: logic_weight in {mixed, logic-heavy} OR non-empty
      invariants OR non-empty sharp_edges OR non-empty entry_points.
    - minimal template: everything else (mostly data-only DTOs / config).
  Full template sections (in order):
    Header: short name, full fqcn (mono, copy button), ClassBadge row.
    Role (role_one_liner, prominent block).
    Source (SourceLink to file; entry_points list, each a SourceLink w/ line).
    Metadata table (kind, logic_weight, concurrency, persistence, difficulty,
                    study_minutes, loc_total, loc_code, test_exemplar).
    Invariants.
    Sharp edges (TODO(verify) rendered as a red badge inline).
    Prereqs (chips linking to class pages).
    Key collaborators (chips).
    Referenced by (backlinks: any class listing this fqcn in
      prereq_fqcns or key_collaborators; feature pages that include this row;
      views that include this fqcn).
    Feature (link) and component.
    Read next (next class in same feature by read_order_hint).
    Views this class appears in.
    Author notes (imported from study/site/docs/class-notes/<slug>.mdx if
      the file exists).
    ProgressCheckbox.
  Minimal template omits: invariants, sharp edges, read next, author notes.
  It always has header, role, source, metadata, backlinks, feature/component.

FQCN AUTO-LINKING & MERMAID CLICKABILITY
  build-atlas.mjs emits docs/_data/fqcn-index.json:
    { "<fqcn>": { "slug": "<slug>", "shortName": "X", "role": "..." }, ... }
  A remark plugin auto-links bare fqcns in generated MDX only (do not rewrite
  hand-written prose — authors use <Fqcn> explicitly).
  A separate mermaid post-processor scans every ```mermaid block in generated
  MDX and, for each node whose label (or id) exactly matches a known fqcn or
  short name, inserts `click <NodeId> "/class/<slug>"`. Never edit atlas .md;
  the pass runs on the copy that build-atlas emits into study/site/docs/.

MERGE RULES  (regeneration safety)
  Each feature MDX under docs/component/<comp>/<feature>.mdx is emitted with:
    {/* BEGIN atlas-generated */}
    ...content copied/derived from study/atlas/components/<comp>/<feature>.md
    plus class-table auto-linking and mermaid click passes...
    {/* END atlas-generated */}
  On re-run, only the block between markers is rewritten. Anything above
  or below is preserved. First run creates the file with a hand-authored
  stub h1 + intro line above the block.
  Class pages are fully generated. Author prose lives in
  docs/class-notes/<slug>.mdx and is imported into the class page.

VIEWS  (parallel indexes over the same class set)
  Ship these ten out of the box, each as scripts/views/<id>.js exporting:
    { id, title, description, tags: [], selectors: [...], ordering: [...],
      overview: { mermaid?: string, prose?: string } }
  A selector is one of:
    { kind:'fqcn',  fqcn:'org.apache...' }
    { kind:'tag',   tag:'ec' }
    { kind:'match', component?:'OM', feature?:'om-ratis', logic_weight?:... }
  Ordering:
    - explicit array of fqcns first, in order, then
    - remaining matches by (component, feature, read_order_hint).
  Views to ship:
    write-path-e2e, read-path-e2e, ec-e2e, ratis-consensus,
    snapshot-lifecycle, background-jobs, security, upgrade-finalize,
    metadata-om, metadata-scm.
  Each view page renders: title, prose, mermaid overview, linear reading
  list (auto-linked to /class/<slug>), and a "read time" total (sum of
  study_minutes). Missing fqcns → build warning, never a hard failure.

TAG OVERLAY
  study/site/data/tags.overlay.json (hand-edited, checked in):
    { "<fqcn>": ["ec","write-path"], ... }
  Views can reference tags via { kind:'tag', ... }. The overlay never
  modifies atlas.json.

VIEW BUILDER  (/view/builder)
  A client-only page powered by ViewBuilder.tsx:
    - Left pane: predicate builder (AND of tag chips + component select +
      feature select + logic_weight filter + free-text fqcn contains).
    - Right pane: live-updating class list (same rendering as view pages),
      sortable by read_order_hint / study_minutes / difficulty / component.
    - Save-locally button: persists the query to localStorage keyed by name.
    - Export button: downloads a scripts/views/<id>.js module the user can
      drop into the repo to promote a saved query into a first-class view.
    - Bootstraps from docs/_data/atlas.min.json (emitted by build-atlas.mjs
      with just the fields the builder needs: fqcn, slug, shortName, role,
      component, feature, kind, logic_weight, difficulty, study_minutes,
      read_order_hint, tags — merged from atlas.json + tags.overlay.json).

FIREHOSE PAGES
  /firehose/sharp-edges — every sharp_edges bullet across atlas, grouped by
     component, filterable by tag; each entry links to its class page.
  /firehose/invariants  — same shape for invariants.

SEARCH
  Use @easyops-cn/docusaurus-search-local. Index page bodies, frontmatter,
  and — for class pages — the fqcn and short name and every collaborator
  short name. Boost class pages over reference pages.

PROGRESS
  /progress reads localStorage; shows per-component percent, streak, and
  the next 3 recommended classes (next unread by read_order_hint across all
  features). Export button dumps a PROGRESS.md snippet the user pastes into
  study/atlas/PROGRESS.md.

QUALITY BARS
  - `npm run build` (docusaurus build) exits clean.
  - `npm run verify` (verify-links.mjs) prints 0 unresolved fqcns or exits 1.
  - Every class in atlas.json has a class detail page.
  - Every view page compiles; missing-fqcn warnings are surfaced in
    build output and on the affected view page as a small footer note.
  - Light and dark themes both look intentional; one screenshot each.

STYLE
  - TypeScript for React components. ESM JavaScript for build scripts.
  - Small, single-purpose modules. No god-file build script.
  - Match Docusaurus idioms; do not invent config formats when a plugin exists.
  - No emoji. No marketing tone.
  - Consistent tag / kind color tokens defined in css/custom.css.

FIRST OUTPUT  (before writing any code)
Print:
  (a) the exact file tree you plan to create under study/site/,
  (b) the shape of build-atlas.mjs's pipeline (inputs → intermediates →
      outputs), and where each concern lives,
  (c) assumptions you want confirmed: Docusaurus 3.x version, Node/npm
      version, whether tags.overlay.json starts empty or seeded, whether
      to `npm install` now vs. print the command.
Then WAIT for "go".

AFTER "GO"
Implement in this order, running `npm run start` after each step and saving
one screenshot to study/site/screenshots/step-<n>.png:
  1. Docusaurus skeleton + sidebars + verbatim import of atlas .md files.
  2. SourceLink + LinkModeToggle + config.local.js plumbing.
  3. build-atlas.mjs skeleton + fqcn-index + Fqcn component + auto-link
     transformation for generated pages.
  4. Class detail pages (both templates) + backlinks + ProgressCheckbox.
  5. Mermaid click-rewrite pass.
  6. Views: start with write-path-e2e + ec-e2e; then the remaining eight.
  7. Tag overlay + View builder page.
  8. Firehose pages.
  9. Progress page.
  10. Search plugin + tuning.
  11. Landing page polish.

After each step print: files changed, new URLs, and screenshot path.

CONSTRAINTS
  - No edits under study/atlas/.
  - No fabricated fqcns, JIRAs, or PR links.
  - No new third-party deps beyond Docusaurus core, @docusaurus/theme-mermaid,
    @easyops-cn/docusaurus-search-local, and small remark/rehype plugins,
    without first explaining why and waiting for confirmation.
