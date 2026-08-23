// Emits docs/component/<comp>/<feature>.mdx by transforming the atlas source
// file at study/atlas/components/<comp>/<feature>.md:
//   1. front-matter with slug + atlasGenerated: true
//   2. auto-link fqcns to <Fqcn>
//   3. rewrite mermaid blocks with click directives
//   4. rewrite class-table rows so each fqcn cell becomes a <Fqcn>
//   5. splice into BEGIN/END markers so hand-authored prose survives
//      regeneration.

import fs from 'node:fs/promises';
import path from 'node:path';
import {atlasComponentsRoot, docsRoot} from './paths.mjs';
import {autolinkFqcnsInMdx} from './remark-fqcn-autolink.mjs';
import {rewriteMermaidClicks} from './mermaid-clickable.mjs';
import {writeWithGeneratedBlock} from './merge-block.mjs';
import {info} from './log.mjs';

function slugifyComponent(comp) {
  return comp.toLowerCase().replace(/[^a-z0-9]+/g, '-');
}

/**
 * Turn backticked fqcn table cells into <Fqcn> chips so the class-table rows
 * link to the class pages. The autolinker skips inline code spans by design,
 * so we do this before it runs and unprotect the cell.
 *
 * Only rewrites cells whose entire content is a single `..fqcn..` token, so
 * we don't accidentally touch prose that mentions a fqcn.
 */
function linkifyBacktickedFqcnCells(md, fqcnKeys) {
  // Match a backticked fqcn token flanked by pipes / whitespace so the
  // substitution only fires inside table cells whose entire content is one
  // fqcn — not inline prose that happens to mention one.
  return md.replace(
    /(?<=\|\s)`([a-z][a-zA-Z0-9_.]*\.[A-Z][A-Za-z0-9_$]*)`(?=\s*\|)/g,
    (whole, fqcn) => (fqcnKeys[fqcn] ? `<Fqcn>${fqcn}</Fqcn>` : whole),
  );
}

async function listFeatureFiles() {
  const out = [];
  const components = await fs.readdir(atlasComponentsRoot, {withFileTypes: true});
  for (const c of components) {
    if (!c.isDirectory()) continue;
    const dir = path.join(atlasComponentsRoot, c.name);
    const files = await fs.readdir(dir);
    for (const f of files) {
      if (!f.endsWith('.md')) continue;
      if (f === 'index.md') continue;
      out.push({comp: c.name, feature: f.replace(/\.md$/, ''), src: path.join(dir, f)});
    }
  }
  return out;
}

export async function emitFeaturePages({rows, byComponent, fqcnIndex}) {
  const featureFiles = await listFeatureFiles();
  const fqcnKeys = {};
  for (const k of Object.keys(fqcnIndex)) fqcnKeys[k] = 1;

  // Precompute row-lookup by component + feature so we can emit our own
  // class-table if the source file's table is stale.
  const byCompFeat = new Map();
  for (const [comp, feats] of byComponent) {
    for (const [feat, list] of feats) {
      byCompFeat.set(`${comp}/${feat}`, list);
    }
  }

  let count = 0;
  const unresolvedByFeature = {};
  for (const {comp, feature, src} of featureFiles) {
    // Drop the first H1 line — Docusaurus will render title from front-matter.
    const raw = await fs.readFile(src, 'utf8');
    const compSlugLocal = slugifyComponent(comp);
    // Rewrite sibling `foo.md` references (Related features section) to
    // absolute /component/<comp>/<feat> URLs so no relative link is left
    // behind after we move the file into docs/component/<comp>/.
    const rewritten = raw.replace(
      /\]\(([a-z0-9._-]+)\.md(#[^)]*)?\)/g,
      (_whole, name, anchor) => `](/component/${compSlugLocal}/${name}${anchor ?? ''})`,
    );
    const body = rewritten.replace(/^# .*\n?/m, '').trim();

    const perFile = {unresolved: new Set()};
    const cellLinked = linkifyBacktickedFqcnCells(body, fqcnKeys);
    const linked = autolinkFqcnsInMdx(cellLinked, fqcnKeys, perFile);
    const withClicks = rewriteMermaidClicks(linked, fqcnIndex);
    if (perFile.unresolved.size > 0) {
      unresolvedByFeature[`${comp}/${feature}`] = [...perFile.unresolved];
    }

    const compSlug = slugifyComponent(comp);
    const dst = path.join(docsRoot, 'component', compSlug, `${feature}.mdx`);
    await fs.mkdir(path.dirname(dst), {recursive: true});
    const header = [
      '---',
      `slug: /component/${compSlug}/${feature}`,
      `sidebar_label: ${JSON.stringify(feature)}`,
      `atlasGenerated: true`,
      '---',
      '',
      `# ${comp} / ${feature}`,
      '',
    ].join('\n');

    const generatedBlock =
      withClicks +
      '\n\n' +
      (perFile.unresolved.size > 0
        ? `<UnresolvedNote context="this feature page" items={${JSON.stringify([...perFile.unresolved])}} />`
        : '');

    const stubAbove = `_This feature page was generated from \`study/atlas/components/${comp}/${feature}.md\`. Author prose belongs above or below the BEGIN/END markers._`;

    // Delete the step-1 stub .md if present so Docusaurus doesn't see two docs.
    const stubPath = path.join(docsRoot, 'component', compSlug, `${feature}.md`);
    try {
      await fs.rm(stubPath);
    } catch {
      /* ignore */
    }

    await writeWithGeneratedBlock(dst, {
      header,
      generated: generatedBlock,
      stubAbove,
    });
    count++;
  }
  info(`emit ${count} feature pages`);
  return {unresolvedByFeature};
}

export async function emitComponentIndexes({byComponent}) {
  const compRoot = path.join(docsRoot, 'component');
  await fs.mkdir(compRoot, {recursive: true});
  let count = 0;
  for (const [comp, feats] of byComponent) {
    const compSlug = slugifyComponent(comp);
    const dst = path.join(compRoot, compSlug, 'index.mdx');
    await fs.mkdir(path.dirname(dst), {recursive: true});
    const items = [...feats.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([feat, list]) => {
        const classes = list.length;
        return `- [${feat}](/component/${compSlug}/${feat}) — ${classes} class${classes === 1 ? '' : 'es'}`;
      });
    const body = [
      '---',
      `slug: /component/${compSlug}/`,
      `sidebar_label: ${JSON.stringify(comp)}`,
      `atlasGenerated: true`,
      '---',
      '',
      `# ${comp}`,
      '',
      `${[...feats.keys()].length} features · ${[...feats.values()].reduce((s, l) => s + l.length, 0)} classes.`,
      '',
      ...items,
      '',
    ].join('\n');
    await fs.writeFile(dst, body);
    count++;
  }
  info(`emit ${count} component indexes`);
}
