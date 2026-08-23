import React from 'react';
import Link from '@docusaurus/Link';
import backlinksJson from '@site/docs/_data/backlinks.json';
import viewsJson from '@site/docs/_data/views.json';
import {lookupFqcn} from './fqcn-index';

interface Entry {
  byPrereq: string[];
  byCollab: string[];
  byView: string[];
}
interface ViewMeta {
  id: string;
  title: string;
}

const INDEX: Record<string, Entry> = backlinksJson as Record<string, Entry>;
const VIEWS: Record<string, ViewMeta> = (() => {
  const list = (viewsJson as {views?: ViewMeta[]}).views ?? [];
  const out: Record<string, ViewMeta> = {};
  for (const v of list) out[v.id] = v;
  return out;
})();

export interface BacklinksProps {
  fqcn: string;
}

/** Render a "Referenced by" panel for a class page. */
export default function Backlinks({fqcn}: BacklinksProps): JSX.Element | null {
  const entry = INDEX[fqcn];
  if (!entry) return null;
  const total =
    entry.byPrereq.length + entry.byCollab.length + entry.byView.length;
  if (total === 0) return null;
  return (
    <div className="atlas-backlinks">
      {entry.byPrereq.length > 0 && (
        <FqcnList label="Listed as prereq by" items={entry.byPrereq} />
      )}
      {entry.byCollab.length > 0 && (
        <FqcnList label="Listed as collaborator by" items={entry.byCollab} />
      )}
      {entry.byView.length > 0 && (
        <div className="atlas-backlinks__group">
          <div className="atlas-backlinks__label">Appears in views</div>
          <ul className="atlas-backlinks__list">
            {entry.byView.map((id) => (
              <li key={id}>
                <Link to={`/view/${id}`}>{VIEWS[id]?.title ?? id}</Link>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

function FqcnList({label, items}: {label: string; items: string[]}) {
  return (
    <div className="atlas-backlinks__group">
      <div className="atlas-backlinks__label">{label}</div>
      <ul className="atlas-backlinks__list">
        {items.map((f) => {
          const e = lookupFqcn(f);
          const to = e ? `/class/${e.slug}` : undefined;
          const text = e ? e.shortName : f;
          return (
            <li key={f}>
              {to ? <Link to={to} title={f}>{text}</Link> : <code>{f}</code>}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
