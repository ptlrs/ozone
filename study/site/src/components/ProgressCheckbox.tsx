import React, {useEffect, useState} from 'react';
import BrowserOnly from '@docusaurus/BrowserOnly';

export interface ProgressCheckboxProps {
  /** Unique per class. Usually the slug. */
  id: string;
  /** Optional label suffix; default is the current date on save. */
  label?: string;
}

const STORAGE_PREFIX = 'atlasRead:';
export const PROGRESS_EVENT = 'atlas-progress-changed';

interface Entry {
  read: boolean;
  date?: string; // yyyy-mm-dd
}

export function readEntry(id: string): Entry {
  if (typeof window === 'undefined') return {read: false};
  const raw = window.localStorage.getItem(STORAGE_PREFIX + id);
  if (!raw) return {read: false};
  try {
    return JSON.parse(raw);
  } catch {
    return {read: false};
  }
}

export function writeEntry(id: string, entry: Entry): void {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(STORAGE_PREFIX + id, JSON.stringify(entry));
  window.dispatchEvent(new CustomEvent(PROGRESS_EVENT, {detail: {id, entry}}));
}

export function readAllProgress(): Record<string, Entry> {
  if (typeof window === 'undefined') return {};
  const out: Record<string, Entry> = {};
  for (let i = 0; i < window.localStorage.length; i++) {
    const key = window.localStorage.key(i);
    if (!key || !key.startsWith(STORAGE_PREFIX)) continue;
    const id = key.slice(STORAGE_PREFIX.length);
    out[id] = readEntry(id);
  }
  return out;
}

function today(): string {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
}

export default function ProgressCheckbox({id, label}: ProgressCheckboxProps): JSX.Element {
  return (
    <BrowserOnly fallback={<span className="atlas-progress atlas-progress--placeholder">…</span>}>
      {() => <Inner id={id} label={label} />}
    </BrowserOnly>
  );
}

function Inner({id, label}: ProgressCheckboxProps): JSX.Element {
  const [entry, setEntry] = useState<Entry>(() => readEntry(id));
  useEffect(() => {
    const onChange = (e: Event) => {
      const d = (e as CustomEvent<{id: string; entry: Entry}>).detail;
      if (d && d.id === id) setEntry(d.entry);
    };
    window.addEventListener(PROGRESS_EVENT, onChange);
    return () => window.removeEventListener(PROGRESS_EVENT, onChange);
  }, [id]);

  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const next: Entry = e.target.checked
      ? {read: true, date: today()}
      : {read: false};
    writeEntry(id, next);
    setEntry(next);
  };

  return (
    <label className="atlas-progress">
      <input type="checkbox" checked={!!entry.read} onChange={onChange} />
      <span>
        {label ?? 'Mark as read'}
        {entry.read && entry.date ? <span className="atlas-progress__date"> · {entry.date}</span> : null}
      </span>
    </label>
  );
}
