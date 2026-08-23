module.exports = {
  id: 'ratis-consensus',
  title: 'Ratis consensus (OM + SCM + DN)',
  description:
    'The three state machines that sit on top of Apache Ratis in Ozone: OM apply loop, ' +
    'SCM HA state machine, DN container state machine, plus the shared retry / snapshot glue.',
  tags: ['ratis', 'hot-path'],
  overview: {
    prose:
      'OM applies OMClientRequest logs, DN applies container-op logs, SCM applies HA logs. ' +
      'Each has its own state machine class; the plumbing (retry policy, snapshot info, ' +
      'transport) is shared across all three via hadoop-hdds/framework.',
    mermaid: [
      'flowchart LR',
      '  R[Ratis log] --> OM[OzoneManagerStateMachine]',
      '  R --> SCM[SCMHAServer state machine]',
      '  R --> DN[ContainerStateMachine]',
    ].join('\n'),
  },
  selectors: [
    {kind: 'match', component: 'OM', feature: 'om-ratis'},
    {kind: 'match', component: 'DN', feature: 'ratis-statemachine-dn'},
    {kind: 'match', component: 'Ratis-integration'},
    {kind: 'match', component: 'HddsCommon', feature: 'ratis-integration'},
  ],
};
