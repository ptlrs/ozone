// docs/_data/atlas.min.json — trimmed rows for the client-side view builder.

import fs from 'node:fs/promises';
import path from 'node:path';
import {dataRoot} from './paths.mjs';
import {shortName} from './load-atlas.mjs';
import {info} from './log.mjs';

export async function emitAtlasMin({rows, slugByFqcn, tagIndex}) {
  const minRows = rows.map((r) => ({
    fqcn: r.fqcn,
    slug: slugByFqcn.get(r.fqcn),
    shortName: shortName(r.fqcn),
    role: r.role_one_liner,
    component: r.component,
    feature: r.feature,
    kind: r.kind,
    logic_weight: r.logic_weight,
    difficulty: r.difficulty,
    study_minutes: r.study_minutes,
    read_order_hint: r.read_order_hint,
    tags: tagIndex.get(r.fqcn) ?? [],
  }));
  await fs.writeFile(path.join(dataRoot, 'atlas.min.json'), JSON.stringify(minRows) + '\n');
  info(`emit _data/atlas.min.json (${minRows.length} rows)`);
}
