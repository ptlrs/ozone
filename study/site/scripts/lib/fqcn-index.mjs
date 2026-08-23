// Emits docs/_data/fqcn-index.json:
//   { "<fqcn>": {"slug": "<slug>", "shortName": "X", "role": "…"} }
// Consumed by Fqcn.tsx (runtime) and by the mermaid click-rewrite pass
// (build-time). Also used by remark-fqcn-autolink for generated MDX.

import fs from 'node:fs/promises';
import path from 'node:path';
import {dataRoot} from './paths.mjs';
import {shortName} from './load-atlas.mjs';
import {info} from './log.mjs';

/** @param {import('./load-atlas.mjs').AtlasRow[]} rows */
export async function emitFqcnIndex(rows, slugByFqcn) {
  const idx = {};
  for (const r of rows) {
    idx[r.fqcn] = {
      slug: slugByFqcn.get(r.fqcn),
      shortName: shortName(r.fqcn),
      role: r.role_one_liner,
      component: r.component,
      feature: r.feature,
    };
  }
  await fs.mkdir(dataRoot, {recursive: true});
  const dst = path.join(dataRoot, 'fqcn-index.json');
  await fs.writeFile(dst, JSON.stringify(idx) + '\n');
  info(`emit _data/fqcn-index.json (${Object.keys(idx).length} entries)`);
  return idx;
}
