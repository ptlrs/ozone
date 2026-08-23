// Build inverse indexes over the atlas so class pages can render
// "Referenced by" lists in step 4.
//
// Sources of edges:
//   - prereq_fqcns  → each target's inbound list gets the source added
//   - key_collaborators → same
//   - feature membership → a class listing implies its feature file
//   - view membership → a fqcn in a resolved view

import fs from 'node:fs/promises';
import path from 'node:path';
import {dataRoot} from './paths.mjs';
import {info} from './log.mjs';

/**
 * @param {import('./load-atlas.mjs').AtlasRow[]} rows
 * @param {{id:string,title:string,rows:import('./load-atlas.mjs').AtlasRow[]}[]} resolvedViews
 */
export function buildBacklinks(rows, resolvedViews) {
  /** fqcn -> {byPrereq: fqcn[], byCollab: fqcn[], byView: string[], feature: string} */
  const map = new Map();
  const ensure = (f) => {
    if (!map.has(f)) map.set(f, {byPrereq: [], byCollab: [], byView: []});
    return map.get(f);
  };

  for (const r of rows) {
    for (const p of r.prereq_fqcns || []) {
      ensure(p).byPrereq.push(r.fqcn);
    }
    for (const c of r.key_collaborators || []) {
      ensure(c).byCollab.push(r.fqcn);
    }
  }
  for (const v of resolvedViews) {
    for (const r of v.rows) {
      ensure(r.fqcn).byView.push(v.id);
    }
  }
  // dedupe + sort for stable output
  for (const [, entry] of map) {
    entry.byPrereq = [...new Set(entry.byPrereq)].sort();
    entry.byCollab = [...new Set(entry.byCollab)].sort();
    entry.byView = [...new Set(entry.byView)].sort();
  }
  return map;
}

export async function emitBacklinks(map) {
  const out = {};
  for (const [k, v] of map) out[k] = v;
  const dst = path.join(dataRoot, 'backlinks.json');
  await fs.writeFile(dst, JSON.stringify(out) + '\n');
  info(`emit _data/backlinks.json (${map.size} entries)`);
}
