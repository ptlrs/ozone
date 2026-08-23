# Enrichment scope for component: OM

## Directory to edit

`/Users/rpatel/Github/ozone/study/atlas/components/om/`

## Feature files to enrich (one at a time)

### `interface-storage.md` — 11 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=interface-storage`

### `om-audit.md` — 2 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-audit`

### `om-background-services.md` — 13 classes (6 anchors)

- feature key in atlas.json: `component=OM, feature=om-background-services`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.om.service.KeyLifecycleService` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/service/KeyLifecycleService.java, 1625~)
  - `org.apache.hadoop.ozone.om.service.QuotaRepairTask` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/service/QuotaRepairTask.java, 625~)
  - `org.apache.hadoop.ozone.om.service.DirectoryDeletingService` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/service/DirectoryDeletingService.java, 575~)
  - `org.apache.hadoop.ozone.om.service.OMRangerBGSyncService` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/service/OMRangerBGSyncService.java, 525~)
  - `org.apache.hadoop.ozone.om.service.KeyDeletingService` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/service/KeyDeletingService.java, 525~)
  - `org.apache.hadoop.ozone.om.service.SnapshotDeletingService` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/service/SnapshotDeletingService.java, 300~)

### `om-bucket-manager.md` — 2 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-bucket-manager`

### `om-codecs.md` — 2 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-codecs`

### `om-execution.md` — 2 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-execution`

### `om-fs.md` — 1 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-fs`

### `om-helpers.md` — 2 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-helpers`

### `om-key-manager.md` — 2 classes (1 anchors)

- feature key in atlas.json: `component=OM, feature=om-key-manager`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.om.KeyManagerImpl` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/KeyManagerImpl.java, 1850~)

### `om-locking.md` — 11 classes (1 anchors)

- feature key in atlas.json: `component=OM, feature=om-locking`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.om.lock.OzoneManagerLock` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/lock/OzoneManagerLock.java, 375~)

### `om-multitenant.md` — 5 classes (1 anchors)

- feature key in atlas.json: `component=OM, feature=om-multitenant`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.om.multitenant.MultiTenantAccessController` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/multitenant/MultiTenantAccessController.java, 375~)

### `om-protocol.md` — 5 classes (1 anchors)

- feature key in atlas.json: `component=OM, feature=om-protocol`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.protocolPB.OzoneManagerRequestHandler` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/protocolPB/OzoneManagerRequestHandler.java, 1300~)

### `om-ratis.md` — 7 classes (5 anchors)

- feature key in atlas.json: `component=OM, feature=om-ratis`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.om.ratis.OzoneManagerRatisServer` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/ratis/OzoneManagerRatisServer.java, 600~)
  - `org.apache.hadoop.ozone.om.ratis.OzoneManagerStateMachine` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/ratis/OzoneManagerStateMachine.java, 500~)
  - `org.apache.hadoop.ozone.om.ratis.OzoneManagerDoubleBuffer` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/ratis/OzoneManagerDoubleBuffer.java, 400~)
  - `org.apache.hadoop.ozone.om.ratis.utils.OzoneManagerRatisUtils` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/ratis/utils/OzoneManagerRatisUtils.java, 350~)
  - `org.apache.hadoop.ozone.om.ratis_snapshot.OmRatisSnapshotProvider` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/ratis_snapshot/OmRatisSnapshotProvider.java, 325~)

### `om-request.md` — 25 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-request`

### `om-request-bucket.md` — 8 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-request-bucket`

### `om-request-file.md` — 6 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-request-file`

### `om-request-key.md` — 31 classes (4 anchors)

- feature key in atlas.json: `component=OM, feature=om-request-key`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.om.request.key.OMKeyRequest` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/request/key/OMKeyRequest.java, 850~)
  - `org.apache.hadoop.ozone.om.request.key.OMKeyRenameRequestWithFSO` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/request/key/OMKeyRenameRequestWithFSO.java, 275~)
  - `org.apache.hadoop.ozone.om.request.key.OMKeyCommitRequestWithFSO` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/request/key/OMKeyCommitRequestWithFSO.java, 250~)
  - `org.apache.hadoop.ozone.om.request.key.OMDirectoriesPurgeRequestWithFSO` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/request/key/OMDirectoriesPurgeRequestWithFSO.java, 225~)

### `om-request-s3.md` — 27 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-request-s3`

### `om-request-snapshot.md` — 8 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-request-snapshot`

### `om-request-upgrade.md` — 3 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-request-upgrade`

### `om-request-volume.md` — 10 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-request-volume`

### `om-response.md` — 84 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-response`

### `om-security.md` — 10 classes (1 anchors)

- feature key in atlas.json: `component=OM, feature=om-security`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.security.OzoneDelegationTokenSecretManager` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/security/OzoneDelegationTokenSecretManager.java, 450~)

### `om-server.md` — 70 classes (18 anchors)

- feature key in atlas.json: `component=OM, feature=om-server`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.om.ListIterator` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/ListIterator.java, 225~)
  - `org.apache.hadoop.ozone.om.OzoneManager` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OzoneManager.java, 4050~)
  - `org.apache.hadoop.ozone.om.OmMetadataManagerImpl` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OmMetadataManagerImpl.java, 1375~)
  - `org.apache.hadoop.ozone.om.OmSnapshotManager` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OmSnapshotManager.java, 775~)
  - `org.apache.hadoop.ozone.om.OMMultiTenantManagerImpl` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OMMultiTenantManagerImpl.java, 725~)
  - `org.apache.hadoop.ozone.om.OmMetadataReader` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OmMetadataReader.java, 525~)
  - `org.apache.hadoop.ozone.om.OMDBCheckpointServlet` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OMDBCheckpointServlet.java, 475~)
  - `org.apache.hadoop.ozone.om.TrashOzoneFileSystem` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/TrashOzoneFileSystem.java, 475~)
  - `org.apache.hadoop.ozone.om.SnapshotChainManager` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/SnapshotChainManager.java, 450~)
  - `org.apache.hadoop.ozone.om.OMDBCheckpointServletInodeBasedXfer` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OMDBCheckpointServletInodeBasedXfer.java, 375~)
  - `org.apache.hadoop.ozone.om.PrefixManagerImpl` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/PrefixManagerImpl.java, 250~)
  - `org.apache.hadoop.ozone.om.OmSnapshot` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OmSnapshot.java, 225~)

### `om-snapshot.md` — 37 classes (6 anchors)

- feature key in atlas.json: `component=OM, feature=om-snapshot`
- anchor fqcns to read (in read-order):
  - `org.apache.hadoop.ozone.om.snapshot.SnapshotDiffManager` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/snapshot/SnapshotDiffManager.java, 1250~)
  - `org.apache.hadoop.ozone.om.snapshot.OmSnapshotLocalDataManager` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/snapshot/OmSnapshotLocalDataManager.java, 825~)
  - `org.apache.hadoop.ozone.om.snapshot.defrag.SnapshotDefragService` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/snapshot/defrag/SnapshotDefragService.java, 525~)
  - `org.apache.hadoop.ozone.om.snapshot.SnapshotDiffValueParser` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/snapshot/SnapshotDiffValueParser.java, 275~)
  - `org.apache.hadoop.ozone.om.snapshot.SnapshotUtils` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/snapshot/SnapshotUtils.java, 225~)
  - `org.apache.hadoop.ozone.om.snapshot.OMSnapshotDirectoryMetrics` (hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/snapshot/OMSnapshotDirectoryMetrics.java, 200~)

### `om-upgrade.md` — 9 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-upgrade`

### `om-volume-manager.md` — 2 classes (0 anchors)

- feature key in atlas.json: `component=OM, feature=om-volume-manager`
