// Loads docs/_data/fqcn-index.json at build time (via Webpack JSON import)
// so <Fqcn> can resolve entries synchronously.
import indexJson from '@site/docs/_data/fqcn-index.json';

export interface FqcnEntry {
  slug: string;
  shortName: string;
  role: string;
  component: string;
  feature: string;
}

const INDEX: Record<string, FqcnEntry> = indexJson as Record<string, FqcnEntry>;

export function lookupFqcn(fqcn: string): FqcnEntry | undefined {
  return INDEX[fqcn];
}

/** Look up by short name — first match wins. Ambiguous by design. */
export function lookupByShortName(shortName: string): FqcnEntry | undefined {
  for (const key of Object.keys(INDEX)) {
    if (INDEX[key].shortName === shortName) return INDEX[key];
  }
  return undefined;
}

export function knownFqcns(): string[] {
  return Object.keys(INDEX);
}
