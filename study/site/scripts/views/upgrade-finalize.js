module.exports = {
  id: 'upgrade-finalize',
  title: 'Upgrade & finalization',
  description:
    'Layout version bumps, upgrade actions, finalization state machine, and the classes that ' +
    'gate cross-service format changes.',
  tags: ['upgrade'],
  selectors: [
    {kind: 'match', feature: 'upgrade'},
    {kind: 'match', feature: 'om-upgrade'},
    {kind: 'match', feature: 'dn-upgrade'},
  ],
};
