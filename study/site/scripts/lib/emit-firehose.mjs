// Aggregates every sharp_edges / invariants bullet across atlas.json into
// two firehose pages. Each bullet links back to its class page.

import fs from 'node:fs/promises';
import path from 'node:path';
import {docsRoot} from './paths.mjs';
import {info} from './log.mjs';

function groupByComponent(rows, field) {
  const out = new Map();
  for (const r of rows) {
    const items = r[field];
    if (!items || items.length === 0) continue;
    if (!out.has(r.component)) out.set(r.component, []);
    out.get(r.component).push({row: r, items});
  }
  return out;
}

function renderPage({title, field, rows, slugByFqcn, id, slug}) {
  const grouped = groupByComponent(rows, field);
  const sections = [];
  for (const [comp, entries] of [...grouped.entries()].sort()) {
    const body = entries
      .sort((a, b) => a.row.fqcn.localeCompare(b.row.fqcn))
      .map(({row, items}) => {
        const slugForClass = slugByFqcn.get(row.fqcn);
        const heading = slugForClass
          ? `[${row.fqcn.split('.').pop()}](/class/${slugForClass})`
          : row.fqcn;
        const bullets = items.map((s) => `  - ${s}`).join('\n');
        return `- ${heading} — ${row.component}/${row.feature}\n${bullets}`;
      })
      .join('\n');
    sections.push(`## ${comp}\n\n${body}\n`);
  }
  return [
    '---',
    `slug: ${slug}`,
    `title: ${JSON.stringify(title)}`,
    'atlasGenerated: true',
    '---',
    '',
    `# ${title}`,
    '',
    `Every \`${field}\` bullet across \`atlas.json\`, grouped by component. ${
      grouped.size === 0 ? 'None found.' : ''
    }`,
    '',
    ...sections,
  ].join('\n');
}

export async function emitFirehose({rows, slugByFqcn}) {
  const dstDir = path.join(docsRoot, 'firehose');
  await fs.mkdir(dstDir, {recursive: true});
  await fs.writeFile(
    path.join(dstDir, 'sharp-edges.md'),
    renderPage({
      title: 'Sharp edges',
      field: 'sharp_edges',
      rows,
      slugByFqcn,
      id: 'firehose/sharp-edges',
      slug: '/firehose/sharp-edges',
    }),
  );
  await fs.writeFile(
    path.join(dstDir, 'invariants.md'),
    renderPage({
      title: 'Invariants',
      field: 'invariants',
      rows,
      slugByFqcn,
      id: 'firehose/invariants',
      slug: '/firehose/invariants',
    }),
  );
  info('emit firehose sharp-edges + invariants');
}
