// Loads scripts/views/*.js and emits:
//   docs/view/index.mdx         — landing list
//   docs/view/<id>.mdx          — one MDX page per view
//   docs/_data/views.json       — {views: [{id, title, description, ...}], resolved: {...}}
//
// A view is:
//   {id, title, description, tags, selectors, ordering?, overview?}
// A selector:
//   {kind:'fqcn', fqcn:'...'}
//   {kind:'tag',  tag:'...'}
//   {kind:'match', component?, feature?, logic_weight?}

import fs from 'node:fs/promises';
import path from 'node:path';
import {pathToFileURL} from 'node:url';
import {viewsRoot, dataRoot, docsRoot} from './paths.mjs';
import {rewriteMermaidClicks} from './mermaid-clickable.mjs';
import {info, warn} from './log.mjs';

async function loadViewModules() {
  const files = await fs.readdir(viewsRoot);
  const mods = [];
  for (const f of files) {
    if (!f.endsWith('.js')) continue;
    const p = path.join(viewsRoot, f);
    // The view modules are written as CommonJS (module.exports). Node's ESM
    // loader can import them via createRequire; we use dynamic import()
    // against a file:// URL which works for both cjs and esm.
    // eslint-disable-next-line no-await-in-loop
    const mod = await import(pathToFileURL(p).href);
    mods.push(mod.default ?? mod);
  }
  return mods;
}

function selectorMatch(row, sel, tagIndex) {
  if (sel.kind === 'fqcn') return row.fqcn === sel.fqcn;
  if (sel.kind === 'tag') return (tagIndex.get(row.fqcn) ?? []).includes(sel.tag);
  if (sel.kind === 'match') {
    if (sel.component && row.component !== sel.component) return false;
    if (sel.feature && row.feature !== sel.feature) return false;
    if (sel.logic_weight && row.logic_weight !== sel.logic_weight) return false;
    return true;
  }
  return false;
}

function resolveView(view, rows, tagIndex) {
  const explicitFqcns = view.selectors
    .filter((s) => s.kind === 'fqcn')
    .map((s) => s.fqcn);
  const byFqcn = new Map(rows.map((r) => [r.fqcn, r]));

  const explicit = [];
  const missing = [];
  for (const f of explicitFqcns) {
    const r = byFqcn.get(f);
    if (r) explicit.push(r);
    else missing.push(f);
  }

  const otherSelectors = view.selectors.filter((s) => s.kind !== 'fqcn');
  const explicitSet = new Set(explicit.map((r) => r.fqcn));
  const matched = [];
  if (otherSelectors.length > 0) {
    for (const r of rows) {
      if (explicitSet.has(r.fqcn)) continue;
      for (const s of otherSelectors) {
        if (selectorMatch(r, s, tagIndex)) {
          matched.push(r);
          break;
        }
      }
    }
    matched.sort((a, b) => {
      if (a.component !== b.component) return a.component.localeCompare(b.component);
      if (a.feature !== b.feature) return a.feature.localeCompare(b.feature);
      return (a.read_order_hint ?? 0) - (b.read_order_hint ?? 0);
    });
  }

  const rowsOut = [...explicit, ...matched];
  return {rowsOut, missing};
}

function renderRowList(rows, slugByFqcn) {
  if (rows.length === 0) return '_No classes match the selectors._';
  // Group by component → feature so the reader can see the layering of a
  // cross-cutting view (Client → OM → SCM → DN) instead of a wall of items.
  const byCompFeat = new Map();
  for (const r of rows) {
    if (!byCompFeat.has(r.component)) byCompFeat.set(r.component, new Map());
    const feats = byCompFeat.get(r.component);
    if (!feats.has(r.feature)) feats.set(r.feature, []);
    feats.get(r.feature).push(r);
  }
  const parts = [];
  for (const [comp, feats] of [...byCompFeat.entries()].sort()) {
    // Component-level totals help the reader budget time before drilling in.
    const compTotal = [...feats.values()].reduce(
      (s, list) => s + list.reduce((s2, r) => s2 + (r.study_minutes ?? 0), 0),
      0,
    );
    const compCount = [...feats.values()].reduce((s, list) => s + list.length, 0);
    parts.push(`### ${comp} — ${compCount} class${compCount === 1 ? '' : 'es'} · ${compTotal} min`);
    for (const [feat, list] of [...feats.entries()].sort()) {
      const featTotal = list.reduce((s, r) => s + (r.study_minutes ?? 0), 0);
      parts.push(`\n**[${feat}](/component/${comp.toLowerCase().replace(/[^a-z0-9]+/g, '-')}/${feat})** · ${list.length} · ${featTotal} min\n`);
      const items = [...list]
        .sort((a, b) => (a.reading_order ?? 0) - (b.reading_order ?? 0))
        .map((r) => {
          const slug = slugByFqcn.get(r.fqcn);
          const link = slug ? `[${r.fqcn.split('.').pop()}](/class/${slug})` : `\`${r.fqcn}\``;
          const min = r.study_minutes ?? 0;
          return `- ${link} — ${escapeRole(r.role_one_liner ?? '')} <span className="atlas-mono-hint">${min} min</span>`;
        });
      parts.push(items.join('\n'));
    }
    parts.push('');
  }
  return parts.join('\n');
}

function renderSpotlight(rows, slugByFqcn) {
  if (rows.length === 0) return '';
  const items = rows.slice(0, 10).map((r, i) => {
    const slug = slugByFqcn.get(r.fqcn);
    const short = r.fqcn.split('.').pop();
    const link = slug ? `<a href="/class/${slug}">${short}</a>` : `<code>${r.fqcn}</code>`;
    return [
      `<div className="atlas-step-card">`,
      `<div className="atlas-step-card__num">${i + 1}</div>`,
      `<div>`,
      `<strong>${link}</strong>`,
      `<p>${escapeRole(r.role_one_liner ?? '')}</p>`,
      `<span className="atlas-mono-hint">${r.component}/${r.feature} · ${r.study_minutes ?? 0} min</span>`,
      `</div>`,
      `</div>`,
    ].join('\n');
  });
  return [
    '## Key classes on this path',
    '',
    '<div className="atlas-step-grid">',
    ...items,
    '</div>',
    '',
  ].join('\n');
}

function renderUnderstandChecklist(view, rowsOut) {
  const components = [...new Set(rowsOut.map((r) => r.component))].slice(0, 5);
  const componentText = components.length > 0 ? components.join(' -> ') : 'the participating services';
  return [
    '## What to understand before moving on',
    '',
    `- Explain the service boundary crossings in this path: ${componentText}.`,
    '- Name the entry-point classes and the class that owns each durable state change.',
    '- Identify the retry, failure, or cleanup behavior that keeps the path correct when a service is slow or unavailable.',
    `- Open the individual class pages for invariants, sharp edges, source links, and test exemplars.`,
    view.tags?.length ? `- Use these tags as anchors when searching later: ${view.tags.map((t) => `\`${t}\``).join(', ')}.` : '',
    '',
  ].filter(Boolean).join('\n');
}

function escapeRole(s) {
  if (!s) return '';
  const oneLine = s.replace(/\n/g, ' ').trim();
  // Trim so a very long javadoc doesn't push the study-minutes off the row.
  const clipped = oneLine.length > 140 ? oneLine.slice(0, 137) + '…' : oneLine;
  return clipped
    .replace(/\{/g, '&#123;')
    .replace(/\}/g, '&#125;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function renderView(view, rowsOut, missing, slugByFqcn, fqcnIndex) {
  const readMin = rowsOut.reduce((s, r) => s + (r.study_minutes ?? 0), 0);
  const overviewMermaid = view.overview?.mermaid
    ? '```mermaid\n' + view.overview.mermaid + '\n```'
    : '';
  const withClicks = overviewMermaid
    ? rewriteMermaidClicks(overviewMermaid, fqcnIndex)
    : '';
  const parts = [
    '---',
    `slug: /view/${view.id}`,
    `title: ${JSON.stringify(view.title)}`,
    `sidebar_label: ${JSON.stringify(view.title)}`,
    'atlasGenerated: true',
    '---',
    '',
    `# ${view.title}`,
    '',
    '<div className="atlas-flow-hero">',
    `<p>${escapeRole(view.description ?? '')}</p>`,
    `<div className="atlas-flow-hero__meta">${readMin} minutes · ${rowsOut.length} classes</div>`,
    '</div>',
    '',
    '## Flow walkthrough',
    '',
    view.overview?.prose ?? '',
    '',
    withClicks,
    '',
    renderSpotlight(rowsOut, slugByFqcn),
    renderUnderstandChecklist(view, rowsOut),
    '## Full reading list',
    '',
    renderRowList(rowsOut, slugByFqcn),
    '',
    missing.length > 0
      ? `<UnresolvedNote context="this view's selectors" items={${JSON.stringify(missing)}} />`
      : '',
    '',
  ];
  return parts.filter(Boolean).join('\n');
}

export async function emitViews({rows, slugByFqcn, fqcnIndex, tagIndex}) {
  const modules = await loadViewModules();
  const viewsDir = path.join(docsRoot, 'view');
  await fs.mkdir(viewsDir, {recursive: true});

  const resolved = [];
  for (const v of modules) {
    const {rowsOut, missing} = resolveView(v, rows, tagIndex);
    if (missing.length > 0) {
      warn(`view "${v.id}": ${missing.length} unresolved fqcns in selectors`, {missing});
    }
    resolved.push({id: v.id, title: v.title, description: v.description, tags: v.tags ?? [], rows: rowsOut, missing});
    const dst = path.join(viewsDir, `${v.id}.mdx`);
    await fs.writeFile(dst, renderView(v, rowsOut, missing, slugByFqcn, fqcnIndex));
  }

  // Landing index.
  const indexBody = [
    '---',
    'slug: /view/',
    'sidebar_label: "Views"',
    'atlasGenerated: true',
    '---',
    '',
    '# Views',
    '',
    'Parallel reading orders over the same class set. Each view is a small module under `scripts/views/`.',
    '',
    ...resolved.map(
      (v) =>
        `## [${v.title}](/view/${v.id})\n\n${v.description ?? ''}\n\n${v.rows.length} classes · ${v.rows.reduce((s, r) => s + (r.study_minutes ?? 0), 0)} minutes\n`,
    ),
    '',
  ].join('\n');
  await fs.writeFile(path.join(viewsDir, 'index.mdx'), indexBody);

  // Data side-car for Backlinks.tsx and ViewBuilder.tsx.
  const dataOut = {
    views: resolved.map((v) => ({
      id: v.id,
      title: v.title,
      description: v.description,
      tags: v.tags,
      rowFqcns: v.rows.map((r) => r.fqcn),
    })),
  };
  await fs.writeFile(path.join(dataRoot, 'views.json'), JSON.stringify(dataOut) + '\n');
  info(`emit ${resolved.length} view pages`);
  return resolved;
}
