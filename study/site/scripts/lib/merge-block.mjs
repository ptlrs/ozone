// Splice a generated block into an existing MDX file, preserving anything
// above BEGIN and below END. First-run: create the file with an author
// stub above the block.

import fs from 'node:fs/promises';

const BEGIN = '{/* BEGIN atlas-generated */}';
const END = '{/* END atlas-generated */}';

export async function writeWithGeneratedBlock(dst, {header, generated, stubAbove}) {
  let existing = '';
  try {
    existing = await fs.readFile(dst, 'utf8');
  } catch {
    existing = '';
  }
  if (!existing) {
    const first = [header, '', stubAbove ?? '', '', BEGIN, '', generated, '', END, ''].join('\n');
    await fs.writeFile(dst, first);
    return;
  }
  const bi = existing.indexOf(BEGIN);
  const ei = existing.indexOf(END);
  if (bi === -1 || ei === -1 || ei < bi) {
    // The file exists but the markers are absent or malformed; overwrite
    // with a fresh scaffold that keeps whatever prose the file had at the
    // top (best-effort) but rebuilds the generated block cleanly.
    const preserved = existing.trim();
    const next = [
      header,
      '',
      '{/* preserved from previous version */}',
      preserved,
      '',
      BEGIN,
      '',
      generated,
      '',
      END,
      '',
    ].join('\n');
    await fs.writeFile(dst, next);
    return;
  }
  // Replace only the block between the markers.
  const before = existing.slice(0, bi + BEGIN.length);
  const after = existing.slice(ei);
  const next = before + '\n\n' + generated + '\n\n' + after;
  await fs.writeFile(dst, next);
}
