// Reads study/atlas/atlas.json and returns a normalized bundle used by every
// downstream emitter. Everything is derived here so no other module has to
// touch the raw JSON.

import fs from 'node:fs/promises';
import path from 'node:path';
import {atlasRoot} from './paths.mjs';
import {buildSlugMap} from './slug.mjs';
import {info} from './log.mjs';

/**
 * @typedef {Object} AtlasRow
 * @property {string} fqcn
 * @property {string} path
 * @property {number} loc_total
 * @property {string} loc_code
 * @property {string} kind
 * @property {string} role_one_liner
 * @property {string} logic_weight
 * @property {string} concurrency
 * @property {string} persistence
 * @property {string} component
 * @property {string} feature
 * @property {string[]} key_collaborators
 * @property {string[]} entry_points
 * @property {string[]} invariants
 * @property {string} test_exemplar
 * @property {number} difficulty
 * @property {number} study_minutes
 * @property {string[]} prereq_fqcns
 * @property {number} read_order_hint
 * @property {string[]} sharp_edges
 * @property {string} sub_feature
 * @property {string} sub_sub_feature
 * @property {number} reading_order
 */

/**
 * @returns {Promise<{
 *   rows: AtlasRow[],
 *   byFqcn: Map<string, AtlasRow>,
 *   bySlug: Map<string, AtlasRow>,
 *   slugByFqcn: Map<string, string>,
 *   byComponent: Map<string, Map<string, AtlasRow[]>>,
 * }>}
 */
export async function loadAtlas() {
  const raw = await fs.readFile(path.join(atlasRoot, 'atlas.json'), 'utf8');
  /** @type {AtlasRow[]} */
  const rows = JSON.parse(raw);
  info(`load atlas: ${rows.length} rows`);

  const fqcns = rows.map((r) => r.fqcn);
  const slugByFqcn = buildSlugMap(fqcns);

  const byFqcn = new Map();
  const bySlug = new Map();
  const byComponent = new Map();
  for (const r of rows) {
    byFqcn.set(r.fqcn, r);
    const slug = slugByFqcn.get(r.fqcn);
    if (slug) bySlug.set(slug, r);
    if (!byComponent.has(r.component)) byComponent.set(r.component, new Map());
    const compMap = byComponent.get(r.component);
    if (!compMap.has(r.feature)) compMap.set(r.feature, []);
    compMap.get(r.feature).push(r);
  }
  return {rows, byFqcn, bySlug, slugByFqcn, byComponent};
}

/** Short name = last dot segment (or the fqcn itself if unqualified). */
export function shortName(fqcn) {
  return fqcn.includes('.') ? fqcn.slice(fqcn.lastIndexOf('.') + 1) : fqcn;
}
