import React from 'react';
import Link from '@docusaurus/Link';
import clsx from 'clsx';
import {lookupFqcn} from './fqcn-index';

export interface FqcnProps {
  /** Fully-qualified class name, e.g. `org.apache.hadoop.ozone.…RpcClient`. */
  children: string;
  /** If `long`, render the full fqcn. Default: short name. */
  form?: 'short' | 'long';
  /** Optional label override — takes precedence over `form`. */
  label?: string;
}

/**
 * Render a fqcn as a chip that links to /class/<slug>. When the fqcn is not
 * in the index we fall back to a warning-bordered <code> so authors notice
 * dead references at review time.
 */
export default function Fqcn({children, form = 'short', label}: FqcnProps): JSX.Element {
  const fqcn = typeof children === 'string' ? children.trim() : String(children);
  const entry = lookupFqcn(fqcn);
  const text = label ?? (entry ? (form === 'long' ? fqcn : entry.shortName) : fqcn);
  if (!entry) {
    return (
      <code
        className="atlas-fqcn atlas-fqcn--unknown"
        title={`Unknown fqcn (not in atlas.json): ${fqcn}`}
      >
        {text}
      </code>
    );
  }
  return (
    <Link
      className={clsx('atlas-fqcn')}
      to={`/class/${entry.slug}`}
      title={entry.role ? `${fqcn} — ${entry.role}` : fqcn}
    >
      {text}
    </Link>
  );
}
