# Ozone Class Study Atlas — site

A local Docusaurus 3 site over `study/atlas/`. Read-only view of the atlas
with per-class detail pages, backlinks, views, offline search, and a toggle
between JetBrains-IDEA and GitHub source links.

## Run

Developed against Node 26 / npm 11; minimum Node 22 / npm 10 (see
`engines` in `package.json`).

```bash
cd study/site
npm install     # first time only
npm run atlas   # regenerate docs/ from ../atlas/  (also runs on prestart)
npm run start   # dev server on http://localhost:3000
```

From the repo root, use:

```bash
npm --prefix study/site run start
```

If port 3000 is already in use, choose another port:

```bash
npm --prefix study/site run start -- --port 3001
```

`npm run start` invokes `scripts/build-atlas.mjs` via its `prestart` hook,
so the manual `npm run atlas` step is only needed after editing generator
code or the atlas source.

### Search in dev

The offline search plugin (`@easyops-cn/docusaurus-search-local`) only
indexes at production `build` time — under `docusaurus start` the search
box exists but returns no results. When you want the search bar to work
locally, use `npm run start:with-search`, which runs a full production
build and serves it (search index included). Iterating on prose is
faster under plain `npm run start`.

## Regenerate

```bash
npm run atlas     # emits docs/**/*.{md,mdx}, sidebars.generated.js, _data/*.json
npm run verify    # exits non-zero if there are unresolved fqcns or broken links
npm run build     # docusaurus build; exits clean on a green atlas
```

Nothing under `../atlas/` is ever modified.

## Configure source links

```bash
cp config.local.example.js config.local.js
# edit repoRoot / githubRepo / githubRef / jetbrainsProject / defaultMode
```

`config.local.js` is gitignored. Without it the site runs in `github` mode
and shows a banner pointing at the example.

## Add a view

Drop a module under `scripts/views/<view-id>.js` that exports
`{id, title, description, selectors, ordering, overview}`. The next
`npm run atlas` picks it up and emits `/view/<view-id>`.

## Add class notes

Author `docs/class-notes/<slug>.mdx` next to a class you've read. The class
page imports it if present (step 4). The atlas source never sees your notes.

## Layout

```
scripts/          — build-atlas.mjs + helpers, verify-links.mjs, views
src/              — React components (TSX) + theme + custom.css
docs/             — 100% generated; do not hand-edit generated files
data/             — hand-edited tags.overlay.json
static/           — assets + atlas-config.js runtime bootstrap
screenshots/      — one PNG per implementation step
```
