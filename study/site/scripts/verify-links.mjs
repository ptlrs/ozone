#!/usr/bin/env node
// Walks docs/ and flags:
//   - unresolved fqcns (bare `org.apache…` matches not present in fqcn-index)
//   - broken internal /class/… and /view/… links
// Step 1: bare skeleton that reads warnings emitted by build-atlas.mjs and
// exits non-zero if any are present. Steps 3+ add fqcn / link scanning.

import fs from 'node:fs/promises';
import path from 'node:path';
import {dataRoot} from './lib/paths.mjs';

async function readWarnings() {
  const p = path.join(dataRoot, 'unresolved.json');
  try {
    return JSON.parse(await fs.readFile(p, 'utf8'));
  } catch {
    return [];
  }
}

async function main() {
  const warnings = await readWarnings();
  if (!warnings.length) {
    process.stdout.write('[verify] no issues in docs/_data/unresolved.json\n');
    process.exit(0);
  }
  for (const w of warnings) {
    process.stderr.write(`[verify] ${w.msg}\n`);
  }
  process.exit(1);
}

main().catch((err) => {
  process.stderr.write(`[verify] fatal: ${err.stack || err}\n`);
  process.exit(1);
});
