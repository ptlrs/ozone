// Deterministic slug for a fqcn.
// - base = lowercase last segment
// - on collision (multiple fqcns share the last segment) append `-<hash6>`
//   where hash6 is the first 6 hex chars of sha256(package).
//
// Callers pass in the full fqcn list once so we can pre-detect collisions.

import crypto from 'node:crypto';

/** first 6 hex chars of sha256(pkg). */
function packageHash6(pkg) {
  return crypto.createHash('sha256').update(pkg).digest('hex').slice(0, 6);
}

function baseSlug(fqcn) {
  const last = fqcn.split('.').pop() ?? fqcn;
  return last
    .replace(/[^A-Za-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase();
}

/**
 * Build a Map<fqcn, slug> that resolves collisions deterministically.
 * Sort by fqcn first so the map is stable across builds.
 * @param {string[]} fqcns
 * @returns {Map<string, string>}
 */
export function buildSlugMap(fqcns) {
  const sorted = [...fqcns].sort();
  // count how many fqcns share a base
  const byBase = new Map();
  for (const f of sorted) {
    const b = baseSlug(f);
    if (!byBase.has(b)) byBase.set(b, []);
    byBase.get(b).push(f);
  }
  const out = new Map();
  for (const [base, group] of byBase) {
    if (group.length === 1) {
      out.set(group[0], base);
    } else {
      for (const f of group) {
        const pkg = f.slice(0, f.lastIndexOf('.'));
        out.set(f, `${base}-${packageHash6(pkg)}`);
      }
    }
  }
  return out;
}

export const _internal = {baseSlug, packageHash6};
