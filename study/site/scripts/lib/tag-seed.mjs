// Deterministic tag rules. Every rule is a function (row) -> tag[].
// Documented at top of tags.overlay.json in a _meta block so reviewers can
// see exactly why a class carries a tag.

const RULES = [
  {
    tag: 'hot-path',
    reason: 'appears in write-path-e2e, read-path-e2e, or ratis-consensus (post-resolution)',
    apply: () => [], // populated in seedFromViews
  },
  {
    tag: 'invariant-carrier',
    reason: 'non-empty invariants[] OR concurrency in {ratis-applied, actor/queue}',
    apply: (r) =>
      (r.invariants && r.invariants.length > 0) ||
      r.concurrency === 'ratis-applied' ||
      r.concurrency === 'actor/queue',
  },
  {
    tag: 'background-loop',
    reason: 'role matches scheduler|service|scanner|balancer|reconciliation|periodic|background AND kind in {service, coordinator}',
    apply: (r) =>
      /(scheduler|service|scanner|balancer|reconciliation|periodic|background)/i.test(r.role_one_liner ?? '') &&
      (r.kind === 'service' || r.kind === 'coordinator'),
  },
  {
    tag: 'state-machine',
    reason: 'kind === state-machine OR concurrency === ratis-applied',
    apply: (r) => r.kind === 'state-machine' || r.concurrency === 'ratis-applied',
  },
  {
    tag: 'rocksdb-touching',
    reason: 'persistence names a RocksDB table (contains "RocksDB" or "Table")',
    apply: (r) => /rocksdb|table/i.test(r.persistence ?? '') && r.persistence !== 'in-memory',
  },
  {
    tag: 'proto-boundary',
    reason: 'kind === rpc-stub OR fqcn ends with RequestHandler / ProtocolServerSideImpl / ProtocolClientSideImpl',
    apply: (r) =>
      r.kind === 'rpc-stub' ||
      /(RequestHandler|ProtocolServerSideImpl|ProtocolClientSideImpl)$/.test(r.fqcn),
  },
  {
    tag: 'sharp-edge-carrier',
    reason: 'non-empty sharp_edges[]',
    apply: (r) => (r.sharp_edges && r.sharp_edges.length > 0),
  },
  {
    tag: 'test-anchor',
    reason: 'non-empty test_exemplar',
    apply: (r) => !!(r.test_exemplar && r.test_exemplar.length > 0),
  },
];

const VIEW_TAG_MAP = {
  'write-path-e2e': 'write-path',
  'read-path-e2e': 'read-path',
  'ec-e2e': 'ec',
  'ratis-consensus': 'ratis',
  'snapshot-lifecycle': 'snapshot',
  'background-jobs': 'background',
  'security': 'security',
  'upgrade-finalize': 'upgrade',
  'metadata-om': 'metadata-om',
  'metadata-scm': 'metadata-scm',
};
const HOT_PATH_VIEWS = new Set(['write-path-e2e', 'read-path-e2e', 'ratis-consensus']);

export function buildTagOverlay(rows, resolvedViews) {
  const overlay = {};
  const attach = (fqcn, tag) => {
    if (!overlay[fqcn]) overlay[fqcn] = [];
    if (!overlay[fqcn].includes(tag)) overlay[fqcn].push(tag);
  };
  // Rule-based tags.
  for (const r of rows) {
    for (const rule of RULES) {
      if (rule.tag === 'hot-path') continue; // handled below
      if (rule.apply(r)) attach(r.fqcn, rule.tag);
    }
  }
  // View-derived tags.
  for (const v of resolvedViews) {
    const t = VIEW_TAG_MAP[v.id];
    if (!t) continue;
    for (const r of v.rows) {
      attach(r.fqcn, t);
      if (HOT_PATH_VIEWS.has(v.id)) attach(r.fqcn, 'hot-path');
    }
  }
  // Stabilize ordering.
  for (const k of Object.keys(overlay)) overlay[k].sort();
  return overlay;
}

export function tagRulesMeta() {
  return {
    generated: 'by scripts/lib/tag-seed.mjs',
    rules: RULES.map((r) => ({tag: r.tag, reason: r.reason})),
    view_derived: VIEW_TAG_MAP,
  };
}
