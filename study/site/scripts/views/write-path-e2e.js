// Client-write path end-to-end: OzoneBucket → KeyOutputStream →
// BlockOutputStream → OM (create+commit) → SCM (allocateBlock) → DN chunks.
module.exports = {
  id: 'write-path-e2e',
  title: 'Write path end-to-end',
  description:
    'Follow one key write from the OzoneBucket API down to a chunk on disk on a datanode, ' +
    'crossing the client, OM, SCM and DN service boundaries.',
  tags: ['write-path', 'hot-path'],
  overview: {
    prose:
      'The linear list below is a reading order that mirrors the actual RPC / data ' +
      'flow. It does not include every helper each stage uses — for that, follow the ' +
      'backlinks on the individual class pages.',
    mermaid: [
      'sequenceDiagram',
      '  participant App as OzoneBucket',
      '  participant Client as RpcClient',
      '  participant KOS as KeyOutputStream',
      '  participant OM',
      '  participant SCM',
      '  participant DN as XceiverClientRatis',
      '  App->>Client: createKey()',
      '  Client->>OM: createKey (open)',
      '  loop chunks',
      '    KOS->>SCM: allocateBlock()',
      '    KOS->>DN: write chunk',
      '  end',
      '  Client->>OM: commitKey()',
    ].join('\n'),
  },
  selectors: [
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.OzoneBucket'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.rpc.RpcClient'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.io.KeyOutputStream'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.io.BlockOutputStreamEntryPool'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.io.BlockOutputStreamEntry'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.hdds.scm.storage.BlockOutputStream'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.hdds.scm.XceiverClientRatis'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.hdds.scm.XceiverClientGrpc'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.om.request.key.OMKeyCreateRequest'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.om.request.key.OMKeyCommitRequest'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.hdds.scm.block.BlockManagerImpl'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.container.keyvalue.KeyValueHandler'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.container.keyvalue.impl.FilePerBlockStrategy'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.container.keyvalue.impl.BlockManagerImpl'},
  ],
};
