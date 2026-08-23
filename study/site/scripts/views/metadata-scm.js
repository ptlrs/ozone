module.exports = {
  id: 'metadata-scm',
  title: 'SCM metadata',
  description:
    'SCM container/pipeline management, replication decisions, node management, and the SCM ' +
    'RocksDB tables. Includes the HA path.',
  tags: ['metadata-scm'],
  selectors: [
    {kind: 'match', component: 'SCM'},
  ],
};
