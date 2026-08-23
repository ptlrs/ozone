module.exports = {
  id: 'ec-e2e',
  title: 'Erasure coding end-to-end',
  description:
    'Client-side EC encode → per-cell dispatch → DN reconstruction. Covers the ' +
    'Rs/Xor raw coders and the reconstruction coordinator.',
  tags: ['ec', 'hot-path'],
  overview: {
    prose:
      'Writes: ECKeyOutputStream drives a RawErasureEncoder over the incoming stripes, then ' +
      'hands each data-and-parity cell to ECBlockOutputStreamEntry. Reads: ECKeyInputStream ' +
      'goes through ECBlockReconstructedStripeInputStream when cells are missing.',
    mermaid: [
      'flowchart LR',
      '  Enc[RawErasureEncoder] --> KOS[ECKeyOutputStream]',
      '  KOS --> BOSE[ECBlockOutputStreamEntry]',
      '  Rec[ECReconstructionCoordinator] --> KIS[ECKeyInputStream]',
    ].join('\n'),
  },
  selectors: [
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.io.ECKeyOutputStream'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.io.ECBlockOutputStreamEntry'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.io.ECKeyInputStream'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.io.ECBlockReconstructedStripeInputStream'},
    {kind: 'match', component: 'DN', feature: 'erasure-coding'},
  ],
};
