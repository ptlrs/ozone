import React from 'react';
import clsx from 'clsx';

export interface ClassBadgeProps {
  kind?: string;
  difficulty?: number;
  studyMinutes?: number;
  logicWeight?: string;
  tier?: string;
  concurrency?: string;
  className?: string;
}

/**
 * Small chip row on class pages. Colors come from src/css/custom.css tokens.
 * Any prop that is empty/undefined is skipped so the row stays compact.
 */
export default function ClassBadge(props: ClassBadgeProps): JSX.Element {
  const chips: JSX.Element[] = [];
  if (props.kind) {
    chips.push(
      <span key="kind" className={clsx('atlas-chip', `atlas-chip--kind-${props.kind}`)}>
        {props.kind}
      </span>,
    );
  }
  if (props.logicWeight) {
    chips.push(
      <span key="logic" className="atlas-chip atlas-chip--logic">
        {props.logicWeight}
      </span>,
    );
  }
  if (props.concurrency) {
    chips.push(
      <span key="conc" className="atlas-chip atlas-chip--conc">
        {props.concurrency}
      </span>,
    );
  }
  if (props.difficulty != null) {
    chips.push(
      <span key="diff" className="atlas-chip atlas-chip--diff" title={`Difficulty ${props.difficulty}/5`}>
        {'●'.repeat(props.difficulty)}
        <span className="atlas-chip--diff-fade">{'●'.repeat(Math.max(0, 5 - props.difficulty))}</span>
      </span>,
    );
  }
  if (props.studyMinutes != null) {
    chips.push(
      <span key="min" className="atlas-chip atlas-chip--min">
        {props.studyMinutes} min
      </span>,
    );
  }
  if (props.tier) {
    chips.push(
      <span key="tier" className={clsx('atlas-chip', `atlas-chip--tier-${props.tier}`)}>
        {props.tier}
      </span>,
    );
  }
  return <div className={clsx('atlas-chip-row', props.className)}>{chips}</div>;
}
