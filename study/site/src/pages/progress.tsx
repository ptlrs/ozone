import React, {useEffect, useMemo, useState} from 'react';
import Layout from '@theme/Layout';
import BrowserOnly from '@docusaurus/BrowserOnly';
import Link from '@docusaurus/Link';
import atlasMin from '@site/docs/_data/atlas.min.json';
import {PROGRESS_EVENT, readAllProgress} from '@site/src/components/ProgressCheckbox';

interface MinRow {
  fqcn: string;
  slug: string;
  shortName: string;
  role: string;
  component: string;
  feature: string;
  study_minutes: number;
  read_order_hint: number;
  difficulty: number;
}

const ROWS: MinRow[] = atlasMin as MinRow[];

function computeSummary(entries: Record<string, {read: boolean; date?: string}>) {
  const readSlugs = new Set(
    Object.entries(entries).filter(([, v]) => v.read).map(([k]) => k),
  );
  const byComponent = new Map<string, {total: number; read: number; minutes: number; minutesRead: number}>();
  for (const r of ROWS) {
    if (!byComponent.has(r.component)) {
      byComponent.set(r.component, {total: 0, read: 0, minutes: 0, minutesRead: 0});
    }
    const b = byComponent.get(r.component)!;
    b.total += 1;
    b.minutes += r.study_minutes ?? 0;
    if (readSlugs.has(r.slug)) {
      b.read += 1;
      b.minutesRead += r.study_minutes ?? 0;
    }
  }
  // Streak = consecutive unique dates ending today.
  const dates = new Set<string>();
  for (const v of Object.values(entries)) if (v.read && v.date) dates.add(v.date);
  const streak = computeStreak(dates);
  const nextRecs = ROWS.filter((r) => !readSlugs.has(r.slug))
    .sort((a, b) => (a.read_order_hint ?? 0) - (b.read_order_hint ?? 0))
    .slice(0, 3);
  return {byComponent, streak, nextRecs, totalMinutes: ROWS.reduce((s, r) => s + (r.study_minutes ?? 0), 0), readCount: readSlugs.size};
}

function computeStreak(dates: Set<string>): number {
  if (dates.size === 0) return 0;
  const today = new Date();
  let streak = 0;
  for (let i = 0; i < 60; i++) {
    const d = new Date(today);
    d.setDate(today.getDate() - i);
    const key = d.toISOString().slice(0, 10);
    if (dates.has(key)) {
      streak += 1;
    } else if (i === 0) {
      // allow yesterday to still start a streak
      continue;
    } else {
      break;
    }
  }
  return streak;
}

function formatSnippet(entries: Record<string, {read: boolean; date?: string}>): string {
  const bySlug = new Map(ROWS.map((r) => [r.slug, r]));
  const lines: string[] = ['# Progress export (paste into study/atlas/PROGRESS.md)', ''];
  const readEntries = Object.entries(entries).filter(([, v]) => v.read);
  readEntries.sort((a, b) => (a[1].date ?? '').localeCompare(b[1].date ?? ''));
  for (const [slug, v] of readEntries) {
    const r = bySlug.get(slug);
    if (!r) continue;
    lines.push(`- [x] ${v.date ?? ''} ${r.fqcn} (${r.study_minutes ?? 0}) notes:`);
  }
  return lines.join('\n');
}

function ProgressInner() {
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
  const {byComponent, streak, nextRecs, totalMinutes, readCount} = useMemo(() => computeSummary(entries), [entries]);

  const onExport = () => {
    const text = formatSnippet(entries);
    const blob = new Blob([text], {type: 'text/markdown'});
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'progress.md';
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 200);
  };

  return (
    <div className="container margin-vert--lg">
      <h1>Progress</h1>
      <p>
        <strong>{readCount}</strong> of {ROWS.length} classes read · streak {streak} day{streak === 1 ? '' : 's'} · {totalMinutes} total minutes budgeted
      </p>
      <button className="button button--primary button--sm" onClick={onExport}>Export progress.md</button>

      <h2>Per component</h2>
      <table>
        <thead>
          <tr>
            <th>Component</th>
            <th>Read</th>
            <th>Total</th>
            <th>%</th>
            <th>Minutes read / budget</th>
          </tr>
        </thead>
        <tbody>
          {[...byComponent.entries()].sort().map(([comp, s]) => {
            const pct = s.total === 0 ? 0 : Math.round((s.read / s.total) * 100);
            return (
              <tr key={comp}>
                <td>{comp}</td>
                <td>{s.read}</td>
                <td>{s.total}</td>
                <td>{pct}%</td>
                <td>{s.minutesRead} / {s.minutes}</td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <h2>Next 3 recommended</h2>
      {nextRecs.length === 0 ? (
        <p>You've read everything in the atlas. That is not a small thing.</p>
      ) : (
        <ol>
          {nextRecs.map((r) => (
            <li key={r.fqcn}>
              <Link to={`/class/${r.slug}`}>{r.shortName}</Link> — {r.component}/{r.feature} · {r.study_minutes ?? 0} min · <em>{r.role}</em>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

export default function ProgressPage(): JSX.Element {
  return (
    <Layout title="Progress" description="Local reading progress across the atlas">
      <BrowserOnly fallback={<div className="container margin-vert--lg">Loading progress…</div>}>
        {() => <ProgressInner />}
      </BrowserOnly>
    </Layout>
  );
}
