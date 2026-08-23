# OM / om-response

**Classes:** 84    **Kinds:** dto:62, service:17, abstract:4, interface:1

## Overview

The `om-response` feature contains the 84 `OMClientResponse` implementation classes — one per request type, plus abstract bases. Every `OMClientResponse` subclass implements `checkAndUpdateDB(OMMetadataManager, BatchOperation)`, which is called by `OzoneManagerDoubleBuffer` to apply the request's side effects to RocksDB. The `@CleanupTableInfo` annotation on each class lists the column family names that the response writes to, enabling `OzoneManagerDoubleBuffer` to invalidate the correct table caches after flush. Abstract bases factor out shared logic: `AbstractOMKeyDeleteResponse` handles the common pattern of moving keys from any table to `deletedTable`; `AbstractS3MultipartAbortResponse` handles moving MPU part keys to the deleted table. The 62 `dto` responses simply call `super.checkAndUpdateDB` with an empty batch (read-only request responses); the 17 `service` responses contain actual write logic.

## Diagram

```mermaid
classDiagram
  class OMClientResponse {
    <<abstract>>
    +checkAndUpdateDB(metadataMgr, batchOp)
    +getOMResponse() OMResponse
  }
  class CleanupTableInfo {
    <<interface>>
    +cleanupTables() String[]
  }
  class AbstractOMKeyDeleteResponse {
    <<abstract>>
    #addDeletesToBatch(keyList, batchOp)
  }
  class AbstractS3MultipartAbortResponse {
    <<abstract>>
    #addMPUPartsToBatch(partList, batchOp)
  }
  class OmKeyResponse {
    <<abstract>>
  }
  OMClientResponse <|-- AbstractOMKeyDeleteResponse
  OMClientResponse <|-- AbstractS3MultipartAbortResponse
  OMClientResponse <|-- OmKeyResponse
  OMClientResponse --> CleanupTableInfo
```

## Class table

### Sub-feature: `acl.prefix`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 304 | `org.apache.hadoop.ozone.om.response.key.acl.prefix.OMPrefixAclResponse` | dto | data-only | 25~ | 10 | Response for Prefix Acl request. |

### Sub-feature: `bucket.acl`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 305 | `org.apache.hadoop.ozone.om.response.bucket.acl.OMBucketAclResponse` | dto | data-only | 25~ | 10 | Response for Bucket acl request. |

### Sub-feature: `key.acl`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 306 | `org.apache.hadoop.ozone.om.response.key.acl.OMKeyAclResponseWithFSO` | service | mixed | 25~ | 30 | Response for Bucket acl request for prefix layout. |
| 307 | `org.apache.hadoop.ozone.om.response.key.acl.OMKeyAclResponse` | dto | data-only | 25~ | 10 | Response for Bucket acl request. |

### Sub-feature: `om.response`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 308 | `org.apache.hadoop.ozone.om.response.CleanupTableInfo` | interface | mixed | 25~ | 20 | Annotation to provide information about clean up table information for OMClientResponse. |
| 309 | `org.apache.hadoop.ozone.om.response.OMClientResponse` | abstract | mixed | 25~ | 30 | Interface for OM Responses, each OM response should implement this interface. |
| 310 | `org.apache.hadoop.ozone.om.response.DummyOMClientResponse` | dto | data-only | 25~ | 10 | A dummy OMClientResponse implementation. |

### Sub-feature: `response.bucket`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 311 | `org.apache.hadoop.ozone.om.response.bucket.OMBucketDeleteResponse` | dto | data-only | 50~ | 10 | Response for DeleteBucket request. |
| 312 | `org.apache.hadoop.ozone.om.response.bucket.OMBucketCreateResponse` | dto | data-only | 50~ | 10 | Response for CreateBucket request. |
| 313 | `org.apache.hadoop.ozone.om.response.bucket.OMBucketSetOwnerResponse` | dto | data-only | 25~ | 10 | Response for set owner request. |
| 314 | `org.apache.hadoop.ozone.om.response.bucket.OMBucketSetPropertyResponse` | dto | data-only | 25~ | 10 | Response for SetBucketProperty request. |

### Sub-feature: `response.file`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 315 | `org.apache.hadoop.ozone.om.response.file.OMDirectoryCreateResponseWithFSO` | service | mixed | 75~ | 30 | Response for create directory request. |
| 316 | `org.apache.hadoop.ozone.om.response.file.OMFileCreateResponseWithFSO` | service | mixed | 50~ | 30 | Response for create file request - prefix layout. |
| 317 | `org.apache.hadoop.ozone.om.response.file.OMDirectoryCreateResponse` | dto | data-only | 50~ | 10 | Response for create directory request. |
| 318 | `org.apache.hadoop.ozone.om.response.file.OMRecoverLeaseResponse` | dto | data-only | 25~ | 10 | Performs tasks for RecoverLease request responses. |
| 319 | `org.apache.hadoop.ozone.om.response.file.OMFileCreateResponse` | dto | data-only | 25~ | 10 | Response for crate file request. |

### Sub-feature: `response.key`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 320 | `org.apache.hadoop.ozone.om.response.key.AbstractOMKeyDeleteResponse` | abstract | mixed | 50~ | 30 | Base class for responses that need to move keys from an arbitrary table to the deleted table. |
| 321 | `org.apache.hadoop.ozone.om.response.key.OmKeyResponse` | abstract | mixed | 25~ | 30 | OmKeyResponse. |
| 322 | `org.apache.hadoop.ozone.om.response.key.OMDirectoriesPurgeResponseWithFSO` | service | mixed | 125~ | 30 | Response for OMDirectoriesPurgeRequestWithFSO request. |
| 323 | `org.apache.hadoop.ozone.om.response.key.OMKeysDeleteResponseWithFSO` | service | mixed | 75~ | 30 | Response for DeleteKeys request. |
| 324 | `org.apache.hadoop.ozone.om.response.key.OMKeyRenameResponseWithFSO` | service | mixed | 75~ | 30 | Response for RenameKey request - prefix layout. |
| 325 | `org.apache.hadoop.ozone.om.response.key.OMKeyDeleteResponseWithFSO` | service | mixed | 75~ | 30 | Response for DeleteKey request. |
| 326 | `org.apache.hadoop.ozone.om.response.key.OMKeyCommitResponseWithFSO` | service | mixed | 50~ | 30 | Response for CommitKey request - prefix layout1. |
| 327 | `org.apache.hadoop.ozone.om.response.key.OMKeySetTimesResponseWithFSO` | service | mixed | 25~ | 30 | Response for Bucket acl request for prefix layout. |
| 328 | `org.apache.hadoop.ozone.om.response.key.OMAllocateBlockResponseWithFSO` | service | mixed | 25~ | 30 | Response for AllocateBlock request - prefix layout. |
| 329 | `org.apache.hadoop.ozone.om.response.key.OMKeyCreateResponseWithFSO` | service | mixed | 25~ | 30 | Response for CreateKey request - prefix layout. |
| 330 | `org.apache.hadoop.ozone.om.response.key.OMKeyCommitResponse` | dto | data-only | 100~ | 10 | Response for CommitKey request. |
| 331 | `org.apache.hadoop.ozone.om.response.key.OMKeyPurgeResponse` | dto | data-only | 75~ | 10 | Response for OMKeyPurgeRequest request. |
| 332 | `org.apache.hadoop.ozone.om.response.key.OMKeysDeleteResponse` | dto | data-only | 75~ | 10 | Response for DeleteKey request. |
| 333 | `org.apache.hadoop.ozone.om.response.key.OMKeyRenameResponse` | dto | data-only | 50~ | 10 | Response for RenameKey request. |
| 334 | `org.apache.hadoop.ozone.om.response.key.OMKeysRenameResponse` | dto | data-only | 50~ | 10 | Response for RenameKeys request. |
| 335 | `org.apache.hadoop.ozone.om.response.key.OMKeyCreateResponse` | dto | data-only | 50~ | 10 | Response for CreateKey request. |
| 336 | `org.apache.hadoop.ozone.om.response.key.OMKeyDeleteResponse` | dto | data-only | 50~ | 10 | Response for DeleteKey request. |
| 337 | `org.apache.hadoop.ozone.om.response.key.OMOpenKeysDeleteResponse` | dto | data-only | 25~ | 10 | Handles responses to move open keys from the open key table to the delete table. |
| 338 | `org.apache.hadoop.ozone.om.response.key.OMAllocateBlockResponse` | dto | data-only | 25~ | 10 | Response for AllocateBlock request. |
| 339 | `org.apache.hadoop.ozone.om.response.key.OMKeySetTimesResponse` | dto | data-only | 25~ | 10 | Response for Bucket acl request. |

### Sub-feature: `response.lifecycle`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 340 | `org.apache.hadoop.ozone.om.response.lifecycle.OMLifecycleSetServiceStatusResponse` | dto | data-only | 25~ | 10 | Response for SetLifecycleServiceStatus request. |
| 341 | `org.apache.hadoop.ozone.om.response.lifecycle.OMLifecycleConfigurationDeleteResponse` | dto | data-only | 25~ | 10 | Response for SetLifecycleConfiguration request. |
| 342 | `org.apache.hadoop.ozone.om.response.lifecycle.OMLifecycleSaveScanStateResponse` | dto | data-only | 25~ | 10 | Response for SaveLifecycleScanState request. |
| 343 | `org.apache.hadoop.ozone.om.response.lifecycle.OMLifecycleConfigurationSetResponse` | dto | data-only | 25~ | 10 | Response for SetLifecycleConfiguration request. |

### Sub-feature: `response.security`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 344 | `org.apache.hadoop.ozone.om.response.security.OMGetDelegationTokenResponse` | dto | data-only | 25~ | 10 | Handle response for GetDelegationToken request. |
| 345 | `org.apache.hadoop.ozone.om.response.security.OMRenewDelegationTokenResponse` | dto | data-only | 25~ | 10 | Handle response for RenewDelegationToken request. |
| 346 | `org.apache.hadoop.ozone.om.response.security.OMCancelDelegationTokenResponse` | dto | data-only | 25~ | 10 | Handle response for CancelDelegationToken request. |

### Sub-feature: `response.snapshot`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 347 | `org.apache.hadoop.ozone.om.response.snapshot.OMSnapshotMoveDeletedKeysResponse` | dto | data-only | 175~ | 10 | Response for OMSnapshotMoveDeletedKeysRequest. |
| 348 | `org.apache.hadoop.ozone.om.response.snapshot.OMSnapshotMoveTableKeysResponse` | dto | data-only | 100~ | 10 | Response for OMSnapshotMoveDeletedKeysRequest. |
| 349 | `org.apache.hadoop.ozone.om.response.snapshot.OMSnapshotPurgeResponse` | dto | data-only | 75~ | 10 | Response for OMSnapshotPurgeRequest. |
| 350 | `org.apache.hadoop.ozone.om.response.snapshot.OMSnapshotRenameResponse` | dto | data-only | 25~ | 10 | Response for OMSnapshotRenameRequest. |
| 351 | `org.apache.hadoop.ozone.om.response.snapshot.OMSnapshotSetPropertyResponse` | dto | data-only | 25~ | 10 | Response for OMSnapshotSetPropertyRequest. |
| 352 | `org.apache.hadoop.ozone.om.response.snapshot.OMSnapshotCreateResponse` | dto | data-only | 25~ | 10 | Response for OMSnapshotCreateRequest. |
| 353 | `org.apache.hadoop.ozone.om.response.snapshot.OMSnapshotDeleteResponse` | dto | data-only | 25~ | 10 | Response for OMSnapshotDeleteRequest. |

### Sub-feature: `response.upgrade`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 354 | `org.apache.hadoop.ozone.om.response.upgrade.OMPrepareResponse` | dto | data-only | 25~ | 10 | Response for prepare request. |
| 355 | `org.apache.hadoop.ozone.om.response.upgrade.OMCancelPrepareResponse` | dto | data-only | 25~ | 10 | Response for cancel prepare. |
| 356 | `org.apache.hadoop.ozone.om.response.upgrade.OMFinalizeUpgradeResponse` | dto | data-only | 25~ | 10 | Response for finalizeUpgrade request. |

### Sub-feature: `response.util`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 357 | `org.apache.hadoop.ozone.om.response.util.OMEchoRPCWriteResponse` | dto | data-only | 25~ | 10 | Response for EchoRPC request (write). |

### Sub-feature: `response.volume`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 358 | `org.apache.hadoop.ozone.om.response.volume.OMVolumeSetOwnerResponse` | dto | data-only | 50~ | 10 | Response for set owner request. |
| 359 | `org.apache.hadoop.ozone.om.response.volume.OMVolumeCreateResponse` | dto | data-only | 25~ | 10 | Response for CreateVolume request. |
| 360 | `org.apache.hadoop.ozone.om.response.volume.OMVolumeAclOpResponse` | dto | data-only | 25~ | 10 | Response for om volume acl operation request. |
| 361 | `org.apache.hadoop.ozone.om.response.volume.OMVolumeSetQuotaResponse` | dto | data-only | 25~ | 10 | Response for set quota request. |
| 362 | `org.apache.hadoop.ozone.om.response.volume.OMVolumeDeleteResponse` | dto | data-only | 25~ | 10 | Response for DeleteVolume request. |
| 363 | `org.apache.hadoop.ozone.om.response.volume.OMQuotaRepairResponse` | dto | data-only | 25~ | 10 | Response for OMQuotaRepairRequest request. |

### Sub-feature: `s3.multipart`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 364 | `org.apache.hadoop.ozone.om.response.s3.multipart.AbstractS3MultipartAbortResponse` | abstract | mixed | 75~ | 30 | Base class for responses that need to move multipart info part keys to the deleted table. |
| 365 | `org.apache.hadoop.ozone.om.response.s3.multipart.S3MultipartUploadCompleteResponseWithFSO` | service | mixed | 75~ | 30 | Response for Multipart Upload Complete request. |
| 366 | `org.apache.hadoop.ozone.om.response.s3.multipart.S3InitiateMultipartUploadResponseWithFSO` | service | mixed | 50~ | 30 | Response for S3 Initiate Multipart Upload request for prefix layout. |
| 367 | `org.apache.hadoop.ozone.om.response.s3.multipart.S3MultipartUploadCommitPartResponseWithFSO` | service | mixed | 25~ | 30 | Response for S3MultipartUploadCommitPartWithFSO request. |
| 368 | `org.apache.hadoop.ozone.om.response.s3.multipart.S3MultipartUploadAbortResponseWithFSO` | service | mixed | 25~ | 30 | Response for Multipart Abort Request - prefix layout. |
| 369 | `org.apache.hadoop.ozone.om.response.s3.multipart.S3MultipartUploadCompleteResponse` | dto | data-only | 75~ | 10 | Response for Multipart Upload Complete request. |
| 370 | `org.apache.hadoop.ozone.om.response.s3.multipart.S3MultipartUploadCommitPartResponse` | dto | data-only | 75~ | 10 | Response for S3MultipartUploadCommitPart request. |
| 371 | `org.apache.hadoop.ozone.om.response.s3.multipart.S3MultipartUploadAbortResponse` | dto | data-only | 50~ | 10 | Response for Multipart Abort Request. |
| 372 | `org.apache.hadoop.ozone.om.response.s3.multipart.S3InitiateMultipartUploadResponse` | dto | data-only | 50~ | 10 | Response for S3 Initiate Multipart Upload request. |
| 373 | `org.apache.hadoop.ozone.om.response.s3.multipart.S3ExpiredMultipartUploadsAbortResponse` | dto | data-only | 25~ | 10 | Handles response to abort expired MPUs. |

### Sub-feature: `s3.security`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 374 | `org.apache.hadoop.ozone.om.response.s3.security.S3RevokeSecretResponse` | dto | data-only | 25~ | 10 | Response for RevokeS3Secret request. |
| 375 | `org.apache.hadoop.ozone.om.response.s3.security.S3GetSecretResponse` | dto | data-only | 25~ | 10 | Response for GetS3Secret request. |
| 376 | `org.apache.hadoop.ozone.om.response.s3.security.OMSetSecretResponse` | dto | data-only | 25~ | 10 | Response for SetSecret request. |

### Sub-feature: `s3.tagging`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 377 | `org.apache.hadoop.ozone.om.response.s3.tagging.S3DeleteObjectTaggingResponseWithFSO` | service | mixed | 25~ | 30 | Response for delete object tagging request for FSO bucket. |
| 378 | `org.apache.hadoop.ozone.om.response.s3.tagging.S3PutObjectTaggingResponseWithFSO` | service | mixed | 25~ | 30 | Response for put object tagging request for FSO bucket. |
| 379 | `org.apache.hadoop.ozone.om.response.s3.tagging.S3DeleteObjectTaggingResponse` | dto | data-only | 25~ | 10 | Response for delete object tagging request. |
| 380 | `org.apache.hadoop.ozone.om.response.s3.tagging.S3PutObjectTaggingResponse` | dto | data-only | 25~ | 10 | Response for put object tagging request. |

### Sub-feature: `s3.tenant`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 381 | `org.apache.hadoop.ozone.om.response.s3.tenant.OMTenantRevokeUserAccessIdResponse` | dto | data-only | 50~ | 10 | Response for OMTenantRevokeUserAccessIdRequest. |
| 382 | `org.apache.hadoop.ozone.om.response.s3.tenant.OMTenantCreateResponse` | dto | data-only | 50~ | 10 | Response for OMTenantCreate request. |
| 383 | `org.apache.hadoop.ozone.om.response.s3.tenant.OMTenantAssignUserAccessIdResponse` | dto | data-only | 50~ | 10 | Response for OMAssignUserToTenantRequest. |
| 384 | `org.apache.hadoop.ozone.om.response.s3.tenant.OMTenantDeleteResponse` | dto | data-only | 25~ | 10 | Response for DeleteTenant request. |
| 385 | `org.apache.hadoop.ozone.om.response.s3.tenant.OMTenantAssignAdminResponse` | dto | data-only | 25~ | 10 | Response for OMTenantAssignAdminRequest. |
| 386 | `org.apache.hadoop.ozone.om.response.s3.tenant.OMTenantRevokeAdminResponse` | dto | data-only | 25~ | 10 | Response for OMTenantAssignAdminRequest. |
| 387 | `org.apache.hadoop.ozone.om.response.s3.tenant.OMSetRangerServiceVersionResponse` | dto | data-only | 25~ | 10 | Response for OMSetRangerServiceVersionRequest. |



## Anchor details

_No logic-heavy anchors in this feature; the classes are primarily data / dto / config / cli._

## Design docs

- no dedicated design doc for the response layer under `hadoop-hdds/docs/content/` on this branch; response classes are tightly coupled to their corresponding request classes

## Seminal JIRAs / PRs

- HDDS-13919. S3 Conditional Writes (PutObject) — new response classes for conditional commit
- HDDS-14661. Handle split table writes for Multipart Upload (changed MPU response classes)
- HDDS-15650. Fix snapshotUsedNamespace underflow in `OMDirectoriesPurgeResponseWithFSO`

## Sharp edges

- The `@CleanupTableInfo` annotation on each response class must list all tables that `checkAndUpdateDB` writes to. Missing a table causes `OzoneManagerDoubleBuffer` to not invalidate the corresponding cache after flush, leading to stale cache reads. Adding a new table write to a response without updating the annotation is a silent correctness bug.
- `DummyOMClientResponse` (not in the class table but present in the codebase) is returned when `applyTransaction` encounters an error before dispatching; it has an empty `checkAndUpdateDB` and an error `OMResponse`.

## Related features

- `components/om/om-ratis.md` — `OzoneManagerDoubleBuffer.add(OMClientResponse)` is called with every response
- `components/om/om-request-key.md` — request classes produce the corresponding `WithFSO` response classes
- `components/om/om-codecs.md` — `OMDBDefinition` table name constants used in `@CleanupTableInfo`

## Self-quiz

1. What is the contract of `OMClientResponse.checkAndUpdateDB(OMMetadataManager, BatchOperation)`?
2. `AbstractOMKeyDeleteResponse.addDeletesToBatch` moves keys. From which table to which table?
3. `@CleanupTableInfo` lists table names. What does `OzoneManagerDoubleBuffer` do with this information after a flush?
4. A `dto` response has an empty `checkAndUpdateDB`. What does this mean for the response's effect on RocksDB?
5. `OMDirectoriesPurgeResponseWithFSO` also updates snapshot exclusive-size. What table stores this and which JIRA fixed the underflow?

<details>
<summary>Answers</summary>

Answer 1: The method applies the response's DB mutations to the provided `BatchOperation`. The mutations are committed atomically when `OzoneManagerDoubleBuffer` calls `metadataMgr.commitBatchOperation(batch)`.
Answer 2: From `deletedKeyTable` source entries to `deletedTable` (the active deleted-key table).
Answer 3: It calls `omMetadataManager.clearTableCache(tableName)` (or the equivalent) for each listed table to evict stale cache entries.
Answer 4: A `dto` response with empty `checkAndUpdateDB` means the request was either read-only (no DB side effect) or the response carries a failed status where no DB writes should occur.
Answer 5: `snapshotInfoTable` stores `SnapshotInfo` which holds the `exclusiveSize` field. The underflow was fixed in HDDS-15650 by correctly accounting for namespace size when FSO directories are deleted and purged.

</details>
