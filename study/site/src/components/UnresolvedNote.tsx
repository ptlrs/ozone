import React from 'react';

export interface UnresolvedNoteProps {
  items?: string[];
  context: string;
}

/**
 * Small footer note on view / class pages when the build surfaced
 * unresolved fqcns for that page. The list is embedded by the emitter.
 */
export default function UnresolvedNote({items, context}: UnresolvedNoteProps): JSX.Element | null {
  if (!items || items.length === 0) return null;
  return (
    <div className="atlas-unresolved-note">
      <strong>{items.length} unresolved fqcn{items.length === 1 ? '' : 's'}</strong> in {context}:
      <ul>
        {items.map((f) => (
          <li key={f}><code>{f}</code></li>
        ))}
      </ul>
    </div>
  );
}
