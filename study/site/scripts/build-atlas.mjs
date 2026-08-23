#!/usr/bin/env node
// Regenerates study/site/docs/ from study/atlas/.
// Idempotent: rerunning with no source changes produces byte-identical output
// (modulo timestamp-free stdout).
//
// Pipeline (see study/site/README.md and study/docusaurus-prompt.md):
//   load atlas
//     └─ slug map + byComponent index
//   emit fqcn-index.json
//   load view modules → resolve → emit view MDX + views.json
//   compute tag overlay (rule-based + view-derived) → tags.overlay.json
//     └─ tagIndex per fqcn
//   emit atlas.min.json
//   build backlinks → backlinks.json
//   emit class pages
//   emit feature pages (with BEGIN/END merge) + component indexes
//   emit firehose pages
//   emit reference pages + guide page (verbatim from atlas .md)
//   emit intro.mdx
//   emit sidebars.generated.js
//   persist warnings to _data/unresolved.json

import fs from 'node:fs/promises';
import path from 'node:path';
import {docsRoot, dataRoot, tagsOverlayPath} from './lib/paths.mjs';
import {loadAtlas} from './lib/load-atlas.mjs';
import {emitFqcnIndex} from './lib/fqcn-index.mjs';
import {emitReferencePages} from './lib/emit-reference-pages.mjs';
import {emitFirehose} from './lib/emit-firehose.mjs';
import {emitClassPages} from './lib/emit-class-pages.mjs';
import {emitFeaturePages, emitComponentIndexes} from './lib/emit-feature-pages.mjs';
import {emitViews} from './lib/emit-views.mjs';
import {emitAtlasMin} from './lib/emit-atlas-min.mjs';
import {buildBacklinks, emitBacklinks} from './lib/backlinks.mjs';
import {emitSidebars} from './lib/emit-sidebars.mjs';
import {buildTagOverlay, tagRulesMeta} from './lib/tag-seed.mjs';
import {info, warn, warningCount, drainWarnings} from './lib/log.mjs';

async function ensureDir(p) {
  await fs.mkdir(p, {recursive: true});
}

async function emitIntroDoc() {
  const dst = path.join(docsRoot, 'intro.mdx');
  const body = [
    '---',
    'id: intro-hidden',
    'slug: /intro-hidden',
    'sidebar_label: "Welcome"',
    '---',
    '',
    '# Ozone Class Study Atlas',
    '',
    'The landing page lives at [/](/) as a React page. This document exists to keep the docs plugin happy.',
    '',
  ].join('\n');
  await fs.writeFile(dst, body);
  info('emit intro.mdx (hidden)');

  // Remove any stale intro.md from prior runs.
  try {
    await fs.rm(path.join(docsRoot, 'intro.md'));
  } catch {
    /* ignore */
  }
  try {
    await fs.rm(path.join(docsRoot, 'progress-stub.md'));
  } catch {
    /* ignore */
  }
}

async function loadOrInitTagOverlay(rows, resolvedViews) {
  const derived = buildTagOverlay(rows, resolvedViews);
  const meta = tagRulesMeta();
  let existing = null;
  try {
    existing = JSON.parse(await fs.readFile(tagsOverlayPath, 'utf8'));
  } catch {
    existing = null;
  }
  const hasUserEntries =
    existing &&
    Object.keys(existing).some((k) => k !== '_meta');
  const out = {_meta: meta};
  if (hasUserEntries) {
    // Keep user's hand-authored entries; merge (union) with derived.
    for (const [k, v] of Object.entries(existing)) {
      if (k === '_meta') continue;
      out[k] = Array.isArray(v) ? [...v] : [];
    }
    for (const [k, v] of Object.entries(derived)) {
      const combined = new Set([...(out[k] ?? []), ...v]);
      out[k] = [...combined].sort();
    }
  } else {
    for (const [k, v] of Object.entries(derived)) out[k] = v;
  }
  await fs.writeFile(tagsOverlayPath, JSON.stringify(out, null, 2) + '\n');
  info(`emit tags.overlay.json (${Object.keys(out).length - 1} tagged fqcns)`);
  const tagIndex = new Map();
  for (const [k, v] of Object.entries(out)) {
    if (k === '_meta') continue;
    tagIndex.set(k, v);
  }
  return tagIndex;
}

async function main() {
  await ensureDir(docsRoot);
  await ensureDir(dataRoot);

  const atlas = await loadAtlas();
  const fqcnIndex = await emitFqcnIndex(atlas.rows, atlas.slugByFqcn);

  // Views need the atlas but their resolved rows feed the tag overlay,
  // which in turn feeds atlas.min.json and (a re-resolution of) the views.
  // We resolve twice: once with an empty tag index (so tag-based selectors
  // in shipped views work only via view-derived tags on the *second* pass),
  // then rebuild with the real tag index.
  const emptyTags = new Map();
  const firstPass = await emitViews({
    rows: atlas.rows,
    slugByFqcn: atlas.slugByFqcn,
    fqcnIndex,
    tagIndex: emptyTags,
  });

  const tagIndex = await loadOrInitTagOverlay(atlas.rows, firstPass);

  // Re-resolve views now that tag-based selectors have data to match.
  const resolvedViews = await emitViews({
    rows: atlas.rows,
    slugByFqcn: atlas.slugByFqcn,
    fqcnIndex,
    tagIndex,
  });

  await emitAtlasMin({rows: atlas.rows, slugByFqcn: atlas.slugByFqcn, tagIndex});

  const backlinks = buildBacklinks(atlas.rows, resolvedViews);
  await emitBacklinks(backlinks);

  const classOut = await emitClassPages({
    rows: atlas.rows,
    slugByFqcn: atlas.slugByFqcn,
    fqcnIndex,
    byComponent: atlas.byComponent,
  });

  const featureOut = await emitFeaturePages({
    rows: atlas.rows,
    byComponent: atlas.byComponent,
    fqcnIndex,
  });
  await emitComponentIndexes({byComponent: atlas.byComponent});

  await emitFirehose({rows: atlas.rows, slugByFqcn: atlas.slugByFqcn});
  await emitReferencePages({fqcnIndex});
  await emitIntroDoc();

  await emitSidebars({
    byComponent: atlas.byComponent,
    resolvedViews,
    rows: atlas.rows,
    slugByFqcn: atlas.slugByFqcn,
  });

  // Aggregate unresolved-fqcn warnings.
  const unresolvedOut = {
    classes: [...classOut.unresolved],
    features: featureOut.unresolvedByFeature,
    views: Object.fromEntries(
      resolvedViews.filter((v) => v.missing.length > 0).map((v) => [v.id, v.missing]),
    ),
  };
  await fs.writeFile(
    path.join(dataRoot, 'unresolved.json'),
    JSON.stringify({warnings: drainWarnings(), unresolved: unresolvedOut}, null, 2) + '\n',
  );

  info(`done. warnings=${warningCount()}`);
}

main().catch((err) => {
  warn(`fatal: ${err.stack || err.message || err}`);
  process.exit(1);
});
