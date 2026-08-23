// Post-process mermaid fenced blocks: for each node whose label OR id
// exactly matches a known fqcn or short name in the fqcn-index, append a
// `click <NodeId> "/class/<slug>"` directive at the bottom of the block.
//
// Runs on the generated MDX string. Never touches user-authored content
// under study/atlas/ — build-atlas writes its own copies first.

/**
 * Extract candidate (nodeId, label) pairs from one mermaid block body.
 * Supports the shapes we actually emit in the atlas:
 *   NodeId["Label"]        -- graph
 *   NodeId[Label]          -- graph
 *   NodeId((Label))        -- graph (circle)
 *   NodeId(Label)          -- graph (rounded)
 *   NodeId{Label}          -- graph (rhombus)
 *   class NodeId {         -- classDiagram
 * If a NodeId itself matches a known short name it is used as the label too.
 */
function collectNodes(body) {
  const seen = new Map(); // nodeId -> label
  const shapes = [
    /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*\[\s*"([^"]+)"\s*\]/gm, // Id["Label"]
    /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*\[\s*([^\]"\n]+?)\s*\]/gm, // Id[Label]
    /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*\(\(\s*([^)\n]+?)\s*\)\)/gm, // Id((Label))
    /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*\(\s*([^)\n]+?)\s*\)/gm, // Id(Label)
    /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*\{\s*([^}\n]+?)\s*\}/gm, // Id{Label}
    /^\s*class\s+([A-Za-z_][A-Za-z0-9_]*)(?:\s+as\s+([A-Za-z_][A-Za-z0-9_]*))?/gm, // classDiagram
  ];
  for (const re of shapes) {
    let m;
    while ((m = re.exec(body)) !== null) {
      const id = m[1];
      const label = m[2] ?? m[1];
      if (!seen.has(id)) seen.set(id, label);
    }
  }
  return seen;
}

/**
 * Given fqcnIndex (fqcn -> entry with {slug, shortName}), try to resolve
 * a node label or id to a slug.
 */
function resolve(idOrLabel, fqcnIndex, shortNameIndex) {
  const trimmed = idOrLabel.trim().replace(/^"|"$/g, '');
  const e = fqcnIndex[trimmed];
  if (e) return e.slug;
  const s = shortNameIndex.get(trimmed);
  if (s) return s;
  return null;
}

function buildShortNameIndex(fqcnIndex) {
  const m = new Map();
  for (const fqcn of Object.keys(fqcnIndex)) {
    const e = fqcnIndex[fqcn];
    if (!m.has(e.shortName)) m.set(e.shortName, e.slug);
  }
  return m;
}

/** Rewrite every ```mermaid block in `mdx` with click directives. */
export function rewriteMermaidClicks(mdx, fqcnIndex) {
  if (typeof mdx !== 'string' || !mdx.includes('```mermaid')) return mdx;
  const shortNameIndex = buildShortNameIndex(fqcnIndex);
  return mdx.replace(/```mermaid\n([\s\S]*?)```/g, (whole, body) => {
    const nodes = collectNodes(body);
    if (nodes.size === 0) return whole;
    const clicks = [];
    for (const [id, label] of nodes) {
      const slug = resolve(label, fqcnIndex, shortNameIndex) ??
        resolve(id, fqcnIndex, shortNameIndex);
      if (!slug) continue;
      // Avoid duplicating click lines already present in the source.
      const existing = new RegExp(`^\\s*click\\s+${id}\\b`, 'm');
      if (existing.test(body)) continue;
      // Newer mermaid parsers require the explicit `href` callback name.
      // `click X "url" _self` fails with 'Expecting CALLBACK_NAME|HREF, got STR'.
      clicks.push(`click ${id} href "/class/${slug}" "Open ${id}"`);
    }
    if (clicks.length === 0) return whole;
    const trimmed = body.replace(/\s*$/, '');
    return '```mermaid\n' + trimmed + '\n' + clicks.join('\n') + '\n```';
  });
}
