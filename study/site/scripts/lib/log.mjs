// Tiny logger + warning accumulator. Warnings are surfaced by build-atlas.mjs
// at the end of a run and (for view / class contexts) written into
// docs/_data/unresolved.json so pages can render a footer note.

const warnings = [];

export function info(msg) {
  process.stdout.write(`[atlas] ${msg}\n`);
}

export function warn(msg, meta) {
  warnings.push({msg, meta: meta ?? null});
  process.stderr.write(`[atlas][warn] ${msg}\n`);
}

export function warningCount() {
  return warnings.length;
}

export function drainWarnings() {
  const out = warnings.slice();
  warnings.length = 0;
  return out;
}
