import React, {useEffect, useState} from 'react';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import BrowserOnly from '@docusaurus/BrowserOnly';
import viewsJson from '@site/docs/_data/views.json';
import atlasMin from '@site/docs/_data/atlas.min.json';
import {readAllProgress} from '@site/src/components/ProgressCheckbox';

interface ViewMeta {id: string; title: string; description: string; rowFqcns?: string[]}
const VIEWS: ViewMeta[] = (viewsJson as {views: ViewMeta[]}).views;
const LEARNING_PATHS = [
  {
    id: 'write-path-e2e',
    label: 'Write Path',
    outcome: 'Follow a key write from the client API through OM, SCM, Ratis, and DN chunk storage.',
  },
  {
    id: 'read-path-e2e',
    label: 'Read Path',
    outcome: 'Trace reads from bucket APIs down to block and chunk readers, including EC divergence.',
  },
  {
    id: 'ratis-consensus',
    label: 'Ratis Consensus',
    outcome: 'Understand the OM, SCM, and DN state machines that apply replicated operations.',
  },
  {
    id: 'metadata-om',
    label: 'OM Metadata',
    outcome: 'Learn the request, response, lock, table, and double-buffer layers behind namespace state.',
  },
  {
    id: 'metadata-scm',
    label: 'SCM Metadata',
    outcome: 'Study container, pipeline, node, replication, safemode, and SCM HA state.',
  },
  {
    id: 'ec-e2e',
    label: 'Erasure Coding',
    outcome: 'Connect client-side EC encoding to cell dispatch, recovery, and reconstruction.',
  },
  {
    id: 'snapshot-lifecycle',
    label: 'Snapshots',
    outcome: 'Follow snapshot creation, diff, deep-clean, and purge across OM and RocksDB helpers.',
  },
  {
    id: 'security',
    label: 'Security',
    outcome: 'Map certificates, tokens, S3 secrets, Kerberos, and authorizer integration.',
  },
  {
    id: 'background-jobs',
    label: 'Background Services',
    outcome: 'See the scheduled control loops that repair, balance, scan, delete, and synchronize state.',
  },
  {
    id: 'upgrade-finalize',
    label: 'Upgrade & Finalization',
    outcome: 'Understand layout versions, finalization checkpoints, and cross-service upgrade gates.',
  },
];

interface MinRow {
  fqcn: string;
  slug: string;
  shortName: string;
  component: string;
  feature: string;
  role: string;
  study_minutes?: number;
}
const ROWS: MinRow[] = atlasMin as MinRow[];

function ContinueCard() {
  const [last, setLast] = useState<{slug: string; date: string} | null>(null);
  useEffect(() => {
    const entries = readAllProgress();
    const read = Object.entries(entries).filter(([, v]) => v.read && v.date);
    read.sort((a, b) => (b[1].date ?? '').localeCompare(a[1].date ?? ''));
    if (read.length > 0) setLast({slug: read[0][0], date: read[0][1].date ?? ''});
  }, []);
  if (!last) return null;
  const row = ROWS.find((r) => r.slug === last.slug);
  if (!row) return null;
  return (
    <div className="atlas-continue">
      <strong>Continue</strong> — you last read <Link to={`/class/${row.slug}`}>{row.shortName}</Link> on {last.date}. <Link to="/progress">Open progress</Link> for the next three recommendations.
    </div>
  );
}

function viewStats(view?: ViewMeta): {count: number; minutes: number} {
  if (!view?.rowFqcns) return {count: 0, minutes: 0};
  const byFqcn = new Map(ROWS.map((r) => [r.fqcn, r]));
  let minutes = 0;
  for (const fqcn of view.rowFqcns) {
    minutes += byFqcn.get(fqcn)?.study_minutes ?? 0;
  }
  return {count: view.rowFqcns.length, minutes};
}

export default function Home(): JSX.Element {
  return (
    <Layout title="Ozone Class Study Atlas" description="A reading map over the Apache Ozone codebase.">
      <div className="container margin-vert--lg">
        <div className="atlas-hero">
          <p className="atlas-eyebrow">Apache Ozone learning paths</p>
          <h1>Learn Ozone by following the flows</h1>
          <p>Start with an end-to-end path, then drill into the classes, invariants, tests, and reference indexes behind it. Search still works when you are using the site as developer reference.</p>
          <div className="atlas-hero__ctas">
            <Link className="button button--primary button--lg" to="/view/">Browse all learning paths</Link>
            <Link className="button button--secondary button--lg" to="/reference/repo-map">Open reference map</Link>
            <Link className="button button--secondary button--lg" to="/view/builder">View builder</Link>
          </div>
        </div>

        <BrowserOnly>{() => <ContinueCard />}</BrowserOnly>

        <h2>Learning paths</h2>
        <p className="atlas-section-lede">Pick a flow first. Each path explains the design, then gives you the classes to read in order.</p>
        <div className="atlas-path-grid">
          {LEARNING_PATHS.map((path) => {
            const v = VIEWS.find((x) => x.id === path.id);
            const stats = viewStats(v);
            return (
              <Link key={path.id} to={`/view/${path.id}`} className="atlas-path-card">
                <div className="atlas-path-card__label">{path.label}</div>
                <div className="atlas-path-card__title">{v?.title ?? path.label}</div>
                <div className="atlas-path-card__desc">{path.outcome}</div>
                <div className="atlas-path-card__meta">{stats.count} classes · {stats.minutes} min</div>
              </Link>
            );
          })}
        </div>

        <h2>Reference while coding</h2>
        <div className="atlas-reference-grid">
          <Link to="/reference/entrypoints" className="atlas-reference-card">
            <strong>Entrypoints</strong>
            <span>Daemons, CLIs, and call-stack starters.</span>
          </Link>
          <Link to="/reference/protobuf-map" className="atlas-reference-card">
            <strong>Protobuf map</strong>
            <span>Wire definitions and the classes that translate them.</span>
          </Link>
          <Link to="/reference/config-keys" className="atlas-reference-card">
            <strong>Config keys</strong>
            <span>Configuration constants and typed config classes.</span>
          </Link>
          <Link to="/firehose/sharp-edges" className="atlas-reference-card">
            <strong>Sharp edges</strong>
            <span>Production pitfalls surfaced across the atlas.</span>
          </Link>
        </div>
      </div>
    </Layout>
  );
}
