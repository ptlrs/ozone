import React, {useEffect, useState} from 'react';
import BrowserOnly from '@docusaurus/BrowserOnly';
import {
  buildHref,
  readConfig,
  readMode,
  subscribeMode,
  type LinkMode,
} from './atlas-config';

export interface SourceLinkProps {
  /** Repo-relative path, e.g. `hadoop-ozone/ozone-manager/.../X.java`. */
  path: string;
  /** Optional line number. Rendered as `#Ln` on GitHub or `&line=n` in IDEA. */
  line?: number;
  /** Visible label. Defaults to the last path segment (with `:line` suffix). */
  children?: React.ReactNode;
  /** Passed through to the anchor. */
  className?: string;
  /** Passed through to the anchor's title attribute. */
  title?: string;
}

function defaultLabel(path: string, line?: number): string {
  const base = path.split('/').pop() ?? path;
  return line != null ? `${base}:${line}` : base;
}

/**
 * A source-file link whose target is resolved at render time from the current
 * window.__ATLAS_CONFIG and localStorage mode. Wrapped in <BrowserOnly> so it
 * always reflects the client-side mode (no SSR/CSR flicker on hover).
 */
export default function SourceLink(props: SourceLinkProps): JSX.Element {
  const {path, line, children, className, title} = props;
  return (
    <BrowserOnly fallback={<code className={className}>{children ?? defaultLabel(path, line)}</code>}>
      {() => <SourceLinkInner {...props} />}
    </BrowserOnly>
  );
}

function SourceLinkInner({path, line, children, className, title}: SourceLinkProps): JSX.Element {
  const cfg = readConfig();
  const [mode, setMode] = useState<LinkMode>(() => readMode(cfg));
  useEffect(() => subscribeMode(setMode), []);
  const href = buildHref(cfg, mode, path, line);
  const rel = mode === 'github' ? 'noopener noreferrer' : undefined;
  const target = mode === 'github' ? '_blank' : undefined;
  return (
    <a
      className={className}
      title={title ?? `${mode === 'jetbrains' ? 'IDEA' : 'GitHub'} → ${path}${line != null ? `:${line}` : ''}`}
      href={href}
      rel={rel}
      target={target}
    >
      {children ?? defaultLabel(path, line)}
    </a>
  );
}
