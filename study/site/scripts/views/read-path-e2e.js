module.exports = {
  id: 'read-path-e2e',
  title: 'Read path end-to-end',
  description:
    'Follow one key read from OzoneBucket down to the datanode chunk read, including ' +
    'pipeline resolution and EC-vs-Ratis divergence at the input-stream layer.',
  tags: ['read-path', 'hot-path'],
  overview: {
    prose:
      'Reads split at the input-stream layer: RATIS keys route through KeyInputStream; ' +
      'EC keys route through ECKeyInputStream and rebuild missing cells via the reconstruction ' +
      'input stream. Both funnel through XceiverClientGrpc.',
    mermaid: [
      'sequenceDiagram',
      '  participant App as OzoneBucket',
      '  participant Client as RpcClient',
      '  participant OM',
      '  participant SCM',
      '  participant DN as XceiverClientGrpc',
      '  App->>Client: getKey()',
      '  Client->>OM: lookupKey',
      '  Client->>SCM: getContainerWithPipeline',
      '  Client->>DN: readChunk',
    ].join('\n'),
  },
  selectors: [
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.OzoneBucket'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.rpc.RpcClient'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.io.KeyInputStream'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.io.ECKeyInputStream'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.client.io.ECBlockReconstructedStripeInputStream'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.hdds.scm.storage.BlockInputStream'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.hdds.scm.storage.ChunkInputStream'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.hdds.scm.XceiverClientGrpc'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.om.request.key.OMKeyLookupRequest'},
    {kind: 'fqcn', fqcn: 'org.apache.hadoop.ozone.container.keyvalue.KeyValueHandler'},
  ],
};
