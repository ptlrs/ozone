// Shared absolute paths for build-atlas.mjs and its helpers.
// Every script imports from here so nothing hard-codes ../.. chains.
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));

/** study/site/ */
export const siteRoot = path.resolve(here, '..', '..');

/** study/ */
export const studyRoot = path.resolve(siteRoot, '..');

/** study/atlas/ (read-only source). */
export const atlasRoot = path.join(studyRoot, 'atlas');

/** study/atlas/components/ */
export const atlasComponentsRoot = path.join(atlasRoot, 'components');

/** study/site/docs/ (generated output). */
export const docsRoot = path.join(siteRoot, 'docs');

/** study/site/docs/_data/ (side-car JSON). */
export const dataRoot = path.join(docsRoot, '_data');

/** study/site/scripts/views/ */
export const viewsRoot = path.join(siteRoot, 'scripts', 'views');

/** study/site/data/tags.overlay.json */
export const tagsOverlayPath = path.join(siteRoot, 'data', 'tags.overlay.json');

/** study/site/sidebars.generated.js */
export const sidebarsGeneratedPath = path.join(siteRoot, 'sidebars.generated.js');
