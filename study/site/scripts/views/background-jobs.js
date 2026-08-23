module.exports = {
  id: 'background-jobs',
  title: 'Background services',
  description:
    'The scheduled services that keep the cluster healthy: replication manager, container ' +
    'balancer, disk balancer, scanners, key/directory deleters.',
  tags: ['background', 'background-loop'],
  selectors: [
    // SCM: replication manager, container/pipeline lifecycle, block deleting, etc.
    {kind: 'match', component: 'SCM'},
    // DN: on-datanode background jobs (scanners live under dn-service; disk
    // balancer + container replication have their own features).
    {kind: 'match', component: 'DN', feature: 'dn-service'},
    {kind: 'match', component: 'DN', feature: 'disk-balancer'},
    {kind: 'match', component: 'DN', feature: 'container-replication-dn'},
    // OM: the om-background-services feature holds the KeyDeleting /
    // DirectoryDeleting / SnapshotDeleting / KeyLifecycle / OpenKeyCleanup /
    // MultipartUploadCleanup / Compaction / RangerBGSync / QuotaRepair
    // services. `tag: background-loop` pulls in any other OM service that
    // matches the scheduler|scanner|balancer role heuristic (see
    // scripts/lib/tag-seed.mjs).
    {kind: 'match', component: 'OM', feature: 'om-background-services'},
    {kind: 'tag', tag: 'background-loop'},
  ],
};
