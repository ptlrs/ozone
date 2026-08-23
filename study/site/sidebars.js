// @ts-check
// The sidebar has two halves:
//   1. A hand-written spine (Guide, Reference, Firehose, Progress).
//   2. Everything else — component/*, class/*, view/* — is pulled in from
//      the generated sidebars.generated.js that scripts/build-atlas.mjs emits.
//      If the file does not exist yet (very first `npm run start` before the
//      preinstall hook has run), we fall back to an empty generated block so
//      Docusaurus still boots.

const fs = require('fs');
const path = require('path');

/** @type {any} */
const staticSidebar = {
  main: [
    {
      type: 'category',
      label: 'Guide',
      collapsed: false,
      items: [
        {type: 'doc', id: 'guide/reading-order', label: 'Reading order'},
      ],
    },
    {
      type: 'category',
      label: 'Reference',
      collapsed: true,
      items: [
        {type: 'doc', id: 'reference/repo-map', label: 'Repo map'},
        {type: 'doc', id: 'reference/entrypoints', label: 'Entry points'},
        {type: 'doc', id: 'reference/protobuf-map', label: 'Protobuf map'},
        {type: 'doc', id: 'reference/config-keys', label: 'Config keys'},
        {type: 'doc', id: 'reference/metrics', label: 'Metrics'},
        {type: 'doc', id: 'reference/glossary', label: 'Glossary'},
        {type: 'doc', id: 'reference/prerequisites', label: 'Prerequisites'},
        {type: 'doc', id: 'reference/design-docs', label: 'Design docs'},
        {type: 'doc', id: 'reference/upgrades', label: 'Upgrades'},
        {type: 'doc', id: 'reference/gaps', label: 'Gaps'},
      ],
    },
    {
      type: 'category',
      label: 'Firehose',
      collapsed: true,
      items: [
        {type: 'doc', id: 'firehose/sharp-edges', label: 'Sharp edges'},
        {type: 'doc', id: 'firehose/invariants', label: 'Invariants'},
      ],
    },
  ],
};

/** @type {any} */
let generated = {main: []};
const genPath = path.join(__dirname, 'sidebars.generated.js');
if (fs.existsSync(genPath)) {
  // eslint-disable-next-line global-require, import/no-dynamic-require
  generated = require(genPath);
}

// Splice generated groups (Components, Views, Classes) into the main sidebar.
// Docusaurus rejects categories with zero items, so we skip empty ones — this
// lets step 1 boot before class/view emission comes online in later steps.
const mergedMain = [...staticSidebar.main];
if (Array.isArray(generated.components) && generated.components.length > 0) {
  mergedMain.push({
    type: 'category',
    label: 'Components',
    collapsed: true,
    items: generated.components,
  });
}
if (Array.isArray(generated.views) && generated.views.length > 0) {
  mergedMain.push({
    type: 'category',
    label: 'Views',
    collapsed: true,
    items: generated.views,
  });
}
// Class pages are intentionally omitted from the sidebar. With thousands of
// entries, an A-Z tree overwhelms the learning-path and reference workflows.
// They remain reachable from search, views, feature tables, and backlinks.

module.exports = {main: mergedMain};
