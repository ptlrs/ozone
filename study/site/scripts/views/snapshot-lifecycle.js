module.exports = {
  id: 'snapshot-lifecycle',
  title: 'Snapshot lifecycle',
  description:
    'Snapshot create → chain / manager → diff → deep-clean → purge. Follows both the OM ' +
    'request path and the background jobs that clean up snapshot data.',
  tags: ['snapshot'],
  overview: {
    prose:
      'Snapshots live on the OM. The request-side is a family of OMSnapshot*Request classes. ' +
      'Background maintenance is the deep-clean, chain manager, diff manager, and SST filtering path.',
  },
  selectors: [
    {kind: 'match', component: 'OM', feature: 'om-snapshot'},
    {kind: 'match', component: 'OM', feature: 'om-request-snapshot'},
    {kind: 'match', component: 'OM', feature: 'om-response'},
  ],
};
