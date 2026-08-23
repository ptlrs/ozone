import React, {useEffect, useState} from 'react';
import BrowserOnly from '@docusaurus/BrowserOnly';
import clsx from 'clsx';
import {readConfig, readMode, subscribeMode, writeMode, type LinkMode} from './atlas-config';

/**
 * Header toggle between JetBrains-IDEA and GitHub source links. Renders as a
 * two-segment pill in the navbar. State lives in localStorage so it survives
 * reloads and syncs across tabs.
 */
export default function LinkModeToggle(): JSX.Element {
  return (
    <BrowserOnly fallback={<span className="atlas-link-mode-toggle atlas-link-mode-toggle--placeholder" />}>
      {() => <LinkModeToggleInner />}
    </BrowserOnly>
  );
}

function LinkModeToggleInner(): JSX.Element {
  const cfg = readConfig();
  const [mode, setMode] = useState<LinkMode>(() => readMode(cfg));
  useEffect(() => subscribeMode(setMode), []);

  const onClick = (next: LinkMode) => (e: React.MouseEvent) => {
    e.preventDefault();
    writeMode(next);
  };

  return (
    <div
      className="atlas-link-mode-toggle"
      role="group"
      aria-label="Source-link click mode"
      title={
        cfg.localMissing
          ? 'config.local.js missing — JetBrains links will not work locally until you copy config.local.example.js.'
          : 'Choose whether class source links open in JetBrains IDEA or on GitHub.'
      }
    >
      <button
        type="button"
        className={clsx('atlas-link-mode-toggle__btn', mode === 'jetbrains' && 'is-active')}
        onClick={onClick('jetbrains')}
        aria-pressed={mode === 'jetbrains'}
      >
        IDEA
      </button>
      <button
        type="button"
        className={clsx('atlas-link-mode-toggle__btn', mode === 'github' && 'is-active')}
        onClick={onClick('github')}
        aria-pressed={mode === 'github'}
      >
        GitHub
      </button>
    </div>
  );
}
