import React, {useEffect, useMemo, useState} from 'react';
import Link from '@docusaurus/Link';
import BrowserOnly from '@docusaurus/BrowserOnly';
import atlasMin from '@site/docs/_data/atlas.min.json';
import {PROGRESS_EVENT, readAllProgress} from './ProgressCheckbox';

// The atlas min payload is a flat list of {slug, ...} rows. We only need the
// slug set to compute progress across every class page in the site.
interface MinRow {
  slug: string;
}
const ROWS: MinRow[] = atlasMin as MinRow[];

/**
 * Slim progress indicator shown in the navbar. Reads from the same
 * localStorage keys as <ProgressCheckbox> so ticking a class page updates
 * the bar immediately (via PROGRESS_EVENT) and persists across sessions.
 *
 * Hidden on narrow viewports (see .atlas-progress-navbar in custom.css).
 */
function Inner(): JSX.Element {
  const [entries, setEntries] = useState(() => readAllProgress());
  useEffect(() => {
    const onChange = () => setEntries(readAllProgress());
    window.addEventListener(PROGRESS_EVENT, onChange);
    window.addEventListener('storage', onChange);
    return () => {
      window.removeEventListener(PROGRESS_EVENT, onChange);
      window.removeEventListener('storage', onChange);
    };
  }, []);
  const {readCount, total, pct} = useMemo(() => {
    const readSlugs = new Set(
      Object.entries(entries).filter(([, v]) => v.read).map(([k]) => k),
    );
    const t = ROWS.length;
    const r = ROWS.reduce((s, row) => s + (readSlugs.has(row.slug) ? 1 : 0), 0);
    return {readCount: r, total: t, pct: t === 0 ? 0 : (r / t) * 100};
  }, [entries]);
  return (
    <Link to="/progress" className="atlas-progress-navbar" title="Open progress">
      <span className="atlas-progress-navbar__label">
        {readCount} / {total}
      </span>
      <span className="atlas-progress-navbar__bar" aria-hidden="true">
        <span
          className="atlas-progress-navbar__fill"
          style={{width: `${pct.toFixed(1)}%`}}
        />
      </span>
    </Link>
  );
}

export default function ProgressNavbar(): JSX.Element {
  return (
    <BrowserOnly fallback={<span className="atlas-progress-navbar" aria-hidden="true" />}>
      {() => <Inner />}
    </BrowserOnly>
  );
}
