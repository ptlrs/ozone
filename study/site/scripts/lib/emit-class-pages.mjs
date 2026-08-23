// One MDX page per atlas row under docs/class/<slug>.mdx.
//
// Template selection:
//   full    - logic_weight in {mixed, logic-heavy} OR non-empty invariants
//             OR non-empty sharp_edges OR non-empty entry_points
//   minimal - everything else
//
// Author notes: if docs/class-notes/<slug>.mdx exists, it is imported and
// rendered under an "Author notes" section on the full template.
//
// The generated MDX is post-processed by autolinkFqcnsInMdx before being
// written — so cross-references inside role_one_liner, collaborator lists,
// etc. become <Fqcn>…</Fqcn> chips.

import fs from 'node:fs/promises';
import path from 'node:path';
import {docsRoot, atlasRoot, studyRoot} from './paths.mjs';
import {shortName} from './load-atlas.mjs';
import {autolinkFqcnsInMdx} from './remark-fqcn-autolink.mjs';
import {info, warn} from './log.mjs';
import {rewriteMermaidClicks} from './mermaid-clickable.mjs';

// repoRoot = the checkout root (parent of study/). Source files are looked up
// relative to it so we can resolve line numbers for entry-point methods.
const repoRoot = path.resolve(studyRoot, '..');
const sourceLineCache = new Map();

async function readSourceLines(relPath) {
  if (sourceLineCache.has(relPath)) return sourceLineCache.get(relPath);
  try {
    const abs = path.join(repoRoot, relPath);
    const text = await fs.readFile(abs, 'utf8');
    const lines = text.split('\n');
    sourceLineCache.set(relPath, lines);
    return lines;
  } catch {
    sourceLineCache.set(relPath, null);
    return null;
  }
}

/**
 * Best-effort: return the 1-based line number of a method declaration named
 * `method` in the given Java source, or null when it cannot be found.
 *
 * Matches lines that look like a Java declaration for `method(`:
 *   public|private|protected|package + optional modifiers + return type +
 *   `method(`. Ignores `.method(` (calls). Falls back to any line
 *   containing `method(` when the strict match misses.
 */
async function findMethodLine(relPath, method) {
  const lines = await readSourceLines(relPath);
  if (!lines) return null;
  const strict = new RegExp(
    `^\\s*(?:public|private|protected|static|final|abstract|synchronized|@[A-Za-z]+\\s*(?:\\([^)]*\\))?\\s+)+[^;]*\\b${method}\\s*\\(`,
  );
  for (let i = 0; i < lines.length; i++) {
    if (strict.test(lines[i])) return i + 1;
  }
  const loose = new RegExp(`(?<![.\\w])${method}\\s*\\(`);
  for (let i = 0; i < lines.length; i++) {
    if (loose.test(lines[i]) && !lines[i].includes('.')) return i + 1;
  }
  return null;
}

const NON_TRIVIAL = new Set(['mixed', 'logic-heavy']);

/**
 * Escape a free-text string from atlas.json so MDX treats it as plain text.
 * atlas.json roles often contain fragments of raw javadoc HTML (`<br>`,
 * `<pre>`, `<code>`, unmatched `<`). Encoding `<` → `&lt;` and `>` → `&gt;`
 * makes MDX render them literally.
 */
function escapeText(s) {
  if (typeof s !== 'string') return '';
  return s
    .replace(/\{/g, '&#123;')
    .replace(/\}/g, '&#125;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function isFull(row) {
  if (NON_TRIVIAL.has(row.logic_weight)) return true;
  if ((row.invariants ?? []).length > 0) return true;
  if ((row.sharp_edges ?? []).length > 0) return true;
  if ((row.entry_points ?? []).length > 0) return true;
  return false;
}

function fqcnList(items, prefix = '') {
  if (!items || items.length === 0) return '_none_';
  // Emit bare fqcns and let autolinkFqcnsInMdx wrap them once. Pre-wrapping
  // in <Fqcn> here causes a double wrap ([object Object] on the page).
  return items.map((f) => `${prefix}${f}`).join(' · ');
}

function renderInvariants(items) {
  if (!items || items.length === 0) return '';
  return ['## Invariants', '', ...items.map((s) => `- ${escapeText(s)}`), ''].join('\n');
}

function renderSharpEdges(items) {
  if (!items || items.length === 0) return '';
  const out = ['## Sharp edges', ''];
  for (const s of items) {
    // Escape first, then unescape TODO(verify) so we can wrap it in a
    // badge. This avoids raw `<` / `>` from atlas prose reaching MDX.
    const escaped = escapeText(s);
    const withBadge = escaped.replace(
      /TODO\(verify\)/g,
      '<span className="atlas-todo-verify" title="Unverified — needs a source citation.">TODO(verify)</span>',
    );
    out.push(`- ${withBadge}`);
  }
  out.push('');
  return out.join('\n');
}

function renderMetadataTable(row) {
  // kind / logic_weight / concurrency / difficulty / study_minutes are already
  // shown by <ClassBadge> at the top. Keep only the fields the badges do not
  // cover, and render them as a compact chip strip so we do not push the
  // interesting content (role, entry points, invariants) off-screen.
  const chips = [];
  if (row.persistence) chips.push(['persistence', escapeText(row.persistence)]);
  if (row.loc_total != null) chips.push(['loc', `${row.loc_total}`]);
  if (row.reading_order != null) chips.push(['reading_order', String(row.reading_order)]);
  if (row.test_exemplar) chips.push(['test', row.test_exemplar.split('/').pop()]);
  if (chips.length === 0) return '';
  const cells = chips
    .map(([k, v]) => `<span className="atlas-meta-chip"><span className="atlas-meta-chip__k">${k}</span> ${v}</span>`)
    .join(' ');
  return ['<div className="atlas-meta-row">', cells, '</div>', ''].join('\n');
}

function renderStudySummary(row) {
  const role = escapeText(row.role_one_liner).replace(/\n/g, ' ');
  const persistence = row.persistence ? escapeText(row.persistence) : 'not classified';
  const featureUrl = `/component/${slugifyComponent(row.component)}/${row.feature}`;
  return [
    '<div className="atlas-class-summary">',
    '<div>',
    '<div className="atlas-eyebrow">Role</div>',
    `<p>${role}</p>`,
    '</div>',
    '<div className="atlas-class-summary__facts">',
    `<a href="/component/${slugifyComponent(row.component)}">${row.component}</a> / <a href="${featureUrl}">${row.feature}</a>`,
    `<span>${row.kind} · ${row.logic_weight}</span>`,
    `<span>${row.concurrency} · ${persistence}</span>`,
    '</div>',
    '</div>',
    '',
  ].join('\n');
}

function renderMentalModel(row) {
  const persistence = row.persistence ? escapeText(row.persistence) : 'not classified';
  return [
    '## Mental model',
    '',
    '<div className="atlas-study-grid">',
    '<div className="atlas-study-card">',
    '<strong>What to look for</strong>',
    `<p>Read this as a <code>${row.kind}</code> in <code>${row.component}/${row.feature}</code>. Focus on the code paths that make the role statement true.</p>`,
    '</div>',
    '<div className="atlas-study-card">',
    '<strong>State and execution</strong>',
    `<p>Concurrency is classified as <code>${row.concurrency}</code>; persistence is <code>${persistence}</code>. Use those two facts to decide which fields and mutations deserve extra attention.</p>`,
    '</div>',
    '<div className="atlas-study-card">',
    '<strong>Depth target</strong>',
    `<p>Budget <code>${row.study_minutes ?? 0} min</code>. Difficulty is <code>${row.difficulty ?? '?'}/5</code>, so skim for shape first and then read the entry points.</p>`,
    '</div>',
    '</div>',
    '',
  ].join('\n');
}

function renderSourceAndTest(row) {
  const test = row.test_exemplar
    ? `<li><SourceLink path="${row.test_exemplar}">${row.test_exemplar}</SourceLink></li>`
    : '<li><em>No exemplar test classified yet.</em></li>';
  return [
    '## Source and test',
    '',
    '<div className="atlas-source-card">',
    '<strong>Source</strong>',
    `<p><SourceLink path="${row.path}">${row.path}</SourceLink></p>`,
    '<strong>Best test to read</strong>',
    '<ul>',
    test,
    '</ul>',
    '</div>',
    '',
  ].join('\n');
}

async function renderEntryPoints(row) {
  const eps = row.entry_points ?? [];
  if (eps.length === 0) return '';
  const fileName = row.path.split('/').pop();
  const items = [];
  for (const m of eps) {
    // Try to resolve a line number so the SourceLink jumps directly to the
    // method on GitHub / IDEA. Falls back gracefully when the source is not
    // reachable or the pattern is missed. The line number only travels in
    // the href; showing it in the visible label was noise (the reader
    // clicks to jump, and the target editor scrolls to the method anyway).
    const line = await findMethodLine(row.path, m);
    const lineAttr = line != null ? ` line={${line}}` : '';
    const label = `${fileName} · ${m}()`;
    items.push(`- <SourceLink path="${row.path}"${lineAttr}>${label}</SourceLink>`);
  }
  return ['## Start in source', '', items.join('\n'), ''].join('\n');
}

function renderReadNext(row, byComponentFeature) {
  const key = `${row.component}/${row.feature}`;
  const peers = byComponentFeature.get(key) ?? [];
  const idx = peers.findIndex((p) => p.fqcn === row.fqcn);
  if (idx === -1 || idx === peers.length - 1) return '';
  const next = peers[idx + 1];
  return [
    '## Read next',
    '',
    `<div className="atlas-read-next">In this feature (${row.component}/${row.feature}): ${next.fqcn}</div>`,
    '',
  ].join('\n');
}

async function classNoteImport(slug) {
  const p = path.join(docsRoot, 'class-notes', `${slug}.mdx`);
  try {
    await fs.access(p);
    // Docusaurus MDX supports `import` from other docs pages via a
    // site-relative alias. We do NOT include the class-notes file in the
    // sidebar; it lives outside the docs tree only in appearance — but it
    // is under docsRoot so the docs plugin still picks it up. Users can
    // link directly to /class-notes/<slug> if they want.
    return `import ClassNote from '@site/docs/class-notes/${slug}.mdx';\n`;
  } catch {
    return '';
  }
}

function renderClassNoteSlot(hasNote) {
  if (!hasNote) return '';
  return ['## Author notes', '', '<ClassNote />', ''].join('\n');
}

async function renderFull(row, ctx) {
  const {byComponentFeature, hasNote} = ctx;
  const entryPoints = await renderEntryPoints(row);
  return [
    renderStudySummary(row),
    renderMetadataTable(row),
    renderMentalModel(row),
    renderSourceAndTest(row),
    entryPoints,
    renderInvariants(row.invariants),
    renderSharpEdges(row.sharp_edges),
    row.prereq_fqcns?.length ? `## Read first\n\n${fqcnList(row.prereq_fqcns)}\n` : '',
    row.key_collaborators?.length ? `## Key collaborators\n\n${fqcnList(row.key_collaborators)}\n` : '',
    `## Where it appears`,
    '',
    `<Backlinks fqcn="${row.fqcn}" />`,
    '',
    renderReadNext(row, byComponentFeature),
    renderClassNoteSlot(hasNote),
    `## Progress`,
    '',
    `<ProgressCheckbox id="${ctx.slug}" />`,
    '',
  ]
    .filter(Boolean)
    .join('\n');
}

function renderMinimal(row, ctx) {
  return [
    renderStudySummary(row),
    renderMetadataTable(row),
    renderMentalModel(row),
    renderSourceAndTest(row),
    `## Where it appears`,
    '',
    `<Backlinks fqcn="${row.fqcn}" />`,
    '',
    `## Progress`,
    '',
    `<ProgressCheckbox id="${ctx.slug}" />`,
    '',
  ]
    .filter(Boolean)
    .join('\n');
}

function slugifyComponent(comp) {
  return comp.toLowerCase().replace(/[^a-z0-9]+/g, '-');
}

function fqcnKeyIndex(idx) {
  // Precompute a lookup table for the auto-linker.
  const out = {};
  for (const k of Object.keys(idx)) out[k] = 1;
  return out;
}

export async function emitClassPages({rows, slugByFqcn, fqcnIndex, byComponent}) {
  const outDir = path.join(docsRoot, 'class');
  await fs.mkdir(outDir, {recursive: true});
  const fqcnKeys = fqcnKeyIndex(fqcnIndex);
  const byComponentFeature = new Map();
  for (const [comp, feats] of byComponent) {
    for (const [feat, list] of feats) {
      const sorted = [...list].sort(
        (a, b) => (a.reading_order ?? 0) - (b.reading_order ?? 0),
      );
      byComponentFeature.set(`${comp}/${feat}`, sorted);
    }
  }

  let count = 0;
  const unresolved = new Set();
  for (const row of rows) {
    const slug = slugByFqcn.get(row.fqcn);
    if (!slug) {
      warn(`no slug for fqcn ${row.fqcn}`);
      continue;
    }
    const hasNote =
      (await classNoteImport(slug)) !== '' ? true : false;
    const importLine = await classNoteImport(slug);
    const short = shortName(row.fqcn);
    const template = isFull(row) ? renderFull : renderMinimal;
    const ctx = {slug, hasNote, byComponentFeature};
    const body = await template(row, ctx);

    // The doc's default id is derived from its path (docs/class/<slug>.mdx →
    // "class/<slug>"). We DON'T set id in front-matter — Docusaurus rejects
    // slashes in explicit ids, and letting it derive the id lets the sidebar
    // reference the doc by its path-shaped id consistently.
    const header = [
      '---',
      `slug: /class/${slug}`,
      `title: ${JSON.stringify(short)}`,
      `sidebar_label: ${JSON.stringify(short)}`,
      'atlasGenerated: true',
      '---',
      '',
      importLine,
      `# ${short}`,
      '',
      `<code className="atlas-fqcn-full">${row.fqcn}</code>`,
      '',
      `<ClassBadge kind="${row.kind}" logicWeight="${row.logic_weight}" concurrency="${row.concurrency}" difficulty={${row.difficulty ?? 0}} studyMinutes={${row.study_minutes ?? 0}} />`,
      '',
    ].join('\n');

    // Auto-link fqcns in the emitted MDX (skipping the header we just wrote,
    // which has no bare fqcns anyway).
    const perPage = {unresolved: new Set()};
    const linked = autolinkFqcnsInMdx(body, fqcnKeys, perPage);
    // Mermaid rewrite is a no-op here (class pages don't have mermaid), but
    // running the pass keeps the pipeline consistent.
    const withClicks = rewriteMermaidClicks(linked, fqcnIndex);
    for (const u of perPage.unresolved) unresolved.add(u);

    const finalContent = header + withClicks;
    await fs.writeFile(path.join(outDir, `${slug}.mdx`), finalContent);
    count++;
  }
  info(`emit ${count} class pages`);
  return {unresolved};
}
