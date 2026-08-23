# Admin CLIs / shell

**Classes:** 112    **Kinds:** service:62, cli:36, abstract:11, interface:3

## Overview

The `shell` feature group implements `ozone sh`, the user-facing CLI for managing volumes, buckets, keys, snapshots, tokens, tenants, prefixes, and ACLs. Its 112 classes follow a strict hierarchy: `OzoneAddress` parses and validates an `o3://[host]/vol/bucket/key` URI and manufactures the appropriate `OzoneClient` (plain, HA service-ID, or host+port); `Handler` is the abstract base that opens a client via `OzoneAddress` and delegates to a concrete handler's `execute(ObjectStore, OzoneVolume, ...)` method; domain-specific abstract classes (`VolumeHandler`, `BucketHandler`, `KeyHandler`, etc.) narrow the open client to the appropriate Ozone object before passing control to the leaf handler. The `Shell` interface and `OzoneShell` class are the Picocli parent commands that group sub-trees by object type. The snapshot, tenant, S3-secret, token, and prefix namespaces each contribute their own handler and command-grouping classes following the same pattern. All handlers are single-use, single-threaded CLI invocations; none hold persistent state.

## Diagram

```mermaid
classDiagram
  class Handler {
    <<abstract>>
    +run()
    +execute(client, volume, bucket, key)
  }
  class VolumeHandler {
    <<abstract>>
  }
  class BucketHandler {
    <<abstract>>
  }
  class KeyHandler {
    <<abstract>>
  }
  class TenantHandler {
    <<abstract>>
  }
  class BucketSnapshotHandler {
    <<abstract>>
  }
  class OzoneAddress {
    +createClient(conf)
    +ensureKeyAddress()
    +ensureBucketAddress()
    +ensureVolumeAddress()
  }
  class CreateVolumeHandler
  class DeleteVolumeHandler
  class PutKeyHandler
  class DeleteKeyHandler
  class CreateBucketHandler
  class SnapshotDiffHandler

  Handler <|-- VolumeHandler
  Handler <|-- BucketHandler
  Handler <|-- KeyHandler
  Handler <|-- TenantHandler
  BucketHandler <|-- BucketSnapshotHandler
  VolumeHandler <|-- CreateVolumeHandler
  VolumeHandler <|-- DeleteVolumeHandler
  KeyHandler <|-- PutKeyHandler
  KeyHandler <|-- DeleteKeyHandler
  BucketHandler <|-- CreateBucketHandler
  BucketSnapshotHandler <|-- SnapshotDiffHandler
  Handler ..> OzoneAddress : parses URI
```

## Class table

### Sub-feature: `ozone.shell`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2561 | `org.apache.hadoop.ozone.shell.Shell` | abstract | mixed | 75~ | 20 | Ozone user interface commands. |
| 2562 | `org.apache.hadoop.ozone.shell.Handler` | abstract | mixed | 50~ | 30 | inferred: Handler — role not documented. |
| 2563 | `org.apache.hadoop.ozone.shell.ReplicationOptions` | abstract | mixed | 50~ | 30 | Common options for specifying replication config: specialized for Ozone Shell and Freon commands. |
| 2564 | `org.apache.hadoop.ozone.shell.OzoneAddress` | service | logic-heavy | 325~ | 45 | Address of an ozone object for ozone shell. |
| 2565 | `org.apache.hadoop.ozone.shell.OzoneInteractiveWelcome` | service | mixed | 50~ | 30 | Startup banner for ozone interactive. |
| 2566 | `org.apache.hadoop.ozone.shell.REPL` | service | mixed | 50~ | 30 | Interactive shell for Ozone commands. |
| 2567 | `org.apache.hadoop.ozone.shell.StoreTypeOption` | service | mixed | 25~ | 30 | Option for org.apache.hadoop.ozone.security.acl.OzoneObj.StoreType. |
| 2568 | `org.apache.hadoop.ozone.shell.MandatoryReplicationOptions` | service | mixed | 25~ | 30 | Options for requiring replication config in 'ozone shell' commands. |
| 2569 | `org.apache.hadoop.ozone.shell.SetSpaceQuotaOptions` | service | mixed | 25~ | 30 | Common options for 'quota' commands. |
| 2570 | `org.apache.hadoop.ozone.shell.PrefixFilterOption` | service | mixed | 25~ | 30 | Option for filtering lists by prefix. |
| 2571 | `org.apache.hadoop.ozone.shell.ShellReplicationOptions` | service | mixed | 25~ | 30 | Options for specifying replication config in 'ozone shell' commands. |
| 2572 | `org.apache.hadoop.ozone.shell.ListLimitOptions` | service | mixed | 25~ | 30 | Options for limiting the size of lists. |
| 2573 | `org.apache.hadoop.ozone.shell.ListPaginationOptions` | service | mixed | 25~ | 30 | Options to provide pagination of lists. |
| 2574 | `org.apache.hadoop.ozone.shell.ClearSpaceQuotaOptions` | service | mixed | 25~ | 30 | Common options for 'clrquota' commands. |
| 2575 | `org.apache.hadoop.ozone.shell.OzoneShell` | cli | mixed | 25~ | 20 | Shell commands for native rpc object manipulation. |

### Sub-feature: `shell.acl`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2576 | `org.apache.hadoop.ozone.shell.acl.AclHandler` | abstract | mixed | 25~ | 30 | Base class for ACL-related commands. |
| 2577 | `org.apache.hadoop.ozone.shell.acl.GetAclHandler` | abstract | mixed | 25~ | 30 | Get ACLs. |
| 2578 | `org.apache.hadoop.ozone.shell.acl.AclOption` | service | mixed | 50~ | 30 | Defines command-line option for specifying one or more ACLs. |

### Sub-feature: `shell.bucket`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2579 | `org.apache.hadoop.ozone.shell.bucket.BucketHandler` | abstract | mixed | 25~ | 30 | Base class for bucket command handlers. |
| 2580 | `org.apache.hadoop.ozone.shell.bucket.GetAclBucketHandler` | service | mixed | 50~ | 30 | Get ACL of bucket. |
| 2581 | `org.apache.hadoop.ozone.shell.bucket.SetReplicationConfigHandler` | service | mixed | 25~ | 30 | set replication configuration of the bucket. |
| 2582 | `org.apache.hadoop.ozone.shell.bucket.SetEncryptionKey` | service | mixed | 25~ | 30 | inferred: SetEncryptionKey — role not documented. |
| 2583 | `org.apache.hadoop.ozone.shell.bucket.BucketUri` | service | mixed | 25~ | 30 | URI parameter for bucket-specific commands. |
| 2584 | `org.apache.hadoop.ozone.shell.bucket.AddAclBucketHandler` | service | mixed | 25~ | 30 | Add ACL to bucket. |
| 2585 | `org.apache.hadoop.ozone.shell.bucket.SetAclBucketHandler` | service | mixed | 25~ | 30 | Set ACL on bucket. |
| 2586 | `org.apache.hadoop.ozone.shell.bucket.RemoveAclBucketHandler` | service | mixed | 25~ | 30 | Remove ACL from bucket. |
| 2587 | `org.apache.hadoop.ozone.shell.bucket.DeleteBucketHandler` | cli | mixed | 100~ | 20 | Delete bucket Handler. |
| 2588 | `org.apache.hadoop.ozone.shell.bucket.CreateBucketHandler` | cli | mixed | 100~ | 20 | create bucket handler. |
| 2589 | `org.apache.hadoop.ozone.shell.bucket.SetQuotaHandler` | cli | mixed | 50~ | 20 | set quota of the bucket. |
| 2590 | `org.apache.hadoop.ozone.shell.bucket.InfoBucketHandler` | cli | mixed | 50~ | 20 | Executes Info bucket. |
| 2591 | `org.apache.hadoop.ozone.shell.bucket.UpdateBucketHandler` | cli | mixed | 25~ | 20 | Executes update bucket calls. |
| 2592 | `org.apache.hadoop.ozone.shell.bucket.ListBucketHandler` | cli | mixed | 25~ | 20 | Executes List Bucket. |
| 2593 | `org.apache.hadoop.ozone.shell.bucket.BucketCommands` | cli | mixed | 25~ | 20 | Subcommands for the bucket related operations. |
| 2594 | `org.apache.hadoop.ozone.shell.bucket.LinkBucketHandler` | cli | mixed | 25~ | 20 | Creates a symlink to another bucket. |
| 2595 | `org.apache.hadoop.ozone.shell.bucket.ClearQuotaHandler` | cli | mixed | 25~ | 20 | clean quota of the bucket. |

### Sub-feature: `shell.common`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2596 | `org.apache.hadoop.ozone.shell.common.VolumeBucketHandler` | abstract | mixed | 25~ | 30 | Base class for commands that accept both volume and bucket. |
| 2597 | `org.apache.hadoop.ozone.shell.common.VolumeBucketUri` | service | mixed | 25~ | 30 | URI parameter for volume or bucket specific commands. |

### Sub-feature: `shell.keys`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2598 | `org.apache.hadoop.ozone.shell.keys.DeleteKeyHandler` | cli | mixed | 100~ | 20 | Executes Delete Key. |
| 2599 | `org.apache.hadoop.ozone.shell.keys.KeyHandler` | abstract | mixed | 25~ | 30 | Base class for key command handlers. |
| 2600 | `org.apache.hadoop.ozone.shell.keys.ChecksumKeyHandler` | service | mixed | 50~ | 30 | Class to display checksum information about an existing key. |
| 2601 | `org.apache.hadoop.ozone.shell.keys.KeyUri` | service | mixed | 25~ | 30 | URI parameter for key-specific commands. |
| 2602 | `org.apache.hadoop.ozone.shell.keys.RewriteKeyHandler` | service | mixed | 25~ | 30 | Rewrite a key with different replication. |
| 2603 | `org.apache.hadoop.ozone.shell.keys.GetAclKeyHandler` | service | mixed | 25~ | 30 | Get ACL of key. |
| 2604 | `org.apache.hadoop.ozone.shell.keys.SetAclKeyHandler` | service | mixed | 25~ | 30 | Set ACL on keys. |
| 2605 | `org.apache.hadoop.ozone.shell.keys.AddAclKeyHandler` | service | mixed | 25~ | 30 | Add ACL to key. |
| 2606 | `org.apache.hadoop.ozone.shell.keys.RemoveAclKeyHandler` | service | mixed | 25~ | 30 | Remove ACL from keys. |
| 2607 | `org.apache.hadoop.ozone.shell.keys.ListKeyHandler` | cli | mixed | 100~ | 20 | Executes List Keys for a bucket or snapshot. |
| 2608 | `org.apache.hadoop.ozone.shell.keys.PutKeyHandler` | cli | mixed | 100~ | 20 | Puts a file into an ozone bucket. |
| 2609 | `org.apache.hadoop.ozone.shell.keys.GetKeyHandler` | cli | mixed | 50~ | 20 | Gets an existing key. |
| 2610 | `org.apache.hadoop.ozone.shell.keys.CopyKeyHandler` | cli | mixed | 50~ | 20 | Copy an existing key to another one within the same bucket. |
| 2611 | `org.apache.hadoop.ozone.shell.keys.KeyCommands` | cli | mixed | 25~ | 20 | Subcommand to group key related operations. |
| 2612 | `org.apache.hadoop.ozone.shell.keys.CatKeyHandler` | cli | mixed | 25~ | 20 | Cat an existing key. |
| 2613 | `org.apache.hadoop.ozone.shell.keys.InfoKeyHandler` | cli | mixed | 25~ | 20 | Executes Info Object. |
| 2614 | `org.apache.hadoop.ozone.shell.keys.RenameKeyHandler` | cli | mixed | 25~ | 20 | Renames an existing key. |

### Sub-feature: `shell.prefix`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2615 | `org.apache.hadoop.ozone.shell.prefix.AddAclPrefixHandler` | service | mixed | 25~ | 30 | Add ACL to prefix. |
| 2616 | `org.apache.hadoop.ozone.shell.prefix.SetAclPrefixHandler` | service | mixed | 25~ | 30 | Set ACL on prefix. |
| 2617 | `org.apache.hadoop.ozone.shell.prefix.RemoveAclPrefixHandler` | service | mixed | 25~ | 30 | Remove ACL from prefix. |
| 2618 | `org.apache.hadoop.ozone.shell.prefix.GetAclPrefixHandler` | service | mixed | 25~ | 30 | Get ACL of prefix. |
| 2619 | `org.apache.hadoop.ozone.shell.prefix.PrefixUri` | service | mixed | 25~ | 30 | URI parameter for prefix-specific commands. |
| 2620 | `org.apache.hadoop.ozone.shell.prefix.PrefixCommands` | cli | mixed | 25~ | 20 | Subcommands for the prefix related operations. |

### Sub-feature: `shell.s3`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2621 | `org.apache.hadoop.ozone.shell.s3.S3Handler` | abstract | mixed | 25~ | 20 | Common interface for S3 command handling. |
| 2622 | `org.apache.hadoop.ozone.shell.s3.RevokeS3SecretHandler` | cli | mixed | 25~ | 20 | Executes revokesecret calls. |
| 2623 | `org.apache.hadoop.ozone.shell.s3.GetS3SecretHandler` | cli | mixed | 25~ | 20 | Executes getsecret calls. |
| 2624 | `org.apache.hadoop.ozone.shell.s3.SetS3SecretHandler` | cli | mixed | 25~ | 20 | ozone s3 setsecret. |
| 2625 | `org.apache.hadoop.ozone.shell.s3.S3Shell` | cli | mixed | 25~ | 20 | Shell for s3 related operations. |

### Sub-feature: `shell.snapshot`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2626 | `org.apache.hadoop.ozone.shell.snapshot.BucketSnapshotHandler` | abstract | mixed | 25~ | 30 | Base class for bucket commands that require a snapshot URI. |
| 2627 | `org.apache.hadoop.ozone.shell.snapshot.SnapshotDiffHandler` | service | mixed | 150~ | 45 | ozone sh snapshot diff. |
| 2628 | `org.apache.hadoop.ozone.shell.snapshot.InfoSnapshotHandler` | service | mixed | 25~ | 30 | ozone sh snapshot info. |
| 2629 | `org.apache.hadoop.ozone.shell.snapshot.ListSnapshotDiffHandler` | service | mixed | 25~ | 30 | ozone sh snapshot listDiff. |
| 2630 | `org.apache.hadoop.ozone.shell.snapshot.RenameSnapshotHandler` | service | mixed | 25~ | 30 | ozone sh snapshot rename. |
| 2631 | `org.apache.hadoop.ozone.shell.snapshot.ListSnapshotHandler` | service | mixed | 25~ | 30 | ozone sh snapshot list. |
| 2632 | `org.apache.hadoop.ozone.shell.snapshot.CreateSnapshotHandler` | service | mixed | 25~ | 30 | ozone sh snapshot create. |
| 2633 | `org.apache.hadoop.ozone.shell.snapshot.SnapshotUri` | service | mixed | 25~ | 30 | URI parameter for snapshot-specific commands. |
| 2634 | `org.apache.hadoop.ozone.shell.snapshot.DeleteSnapshotHandler` | service | mixed | 25~ | 30 | ozone sh snapshot delete. |
| 2635 | `org.apache.hadoop.ozone.shell.snapshot.SnapshotCommands` | cli | mixed | 25~ | 20 | Subcommands for the snapshot related operations. |

### Sub-feature: `shell.tenant`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2636 | `org.apache.hadoop.ozone.shell.tenant.TenantHandler` | abstract | mixed | 25~ | 30 | Base class for tenant command handlers. |
| 2637 | `org.apache.hadoop.ozone.shell.tenant.GetUserInfoHandler` | service | mixed | 50~ | 30 | ozone tenant user info. |
| 2638 | `org.apache.hadoop.ozone.shell.tenant.TenantListHandler` | service | mixed | 25~ | 30 | ozone tenant list. |
| 2639 | `org.apache.hadoop.ozone.shell.tenant.TenantUserCommands` | service | mixed | 25~ | 30 | Subcommand to group tenant user related operations. |
| 2640 | `org.apache.hadoop.ozone.shell.tenant.TenantListUsersHandler` | service | mixed | 25~ | 30 | Command to list users in a tenant along with corresponding accessId. |
| 2641 | `org.apache.hadoop.ozone.shell.tenant.TenantAssignUserAccessIdHandler` | service | mixed | 25~ | 30 | ozone tenant user assign. |
| 2642 | `org.apache.hadoop.ozone.shell.tenant.TenantBucketLinkHandler` | service | mixed | 25~ | 30 | ozone tenant linkbucket. |
| 2643 | `org.apache.hadoop.ozone.shell.tenant.TenantDeleteHandler` | service | mixed | 25~ | 30 | ozone tenant delete. |
| 2644 | `org.apache.hadoop.ozone.shell.tenant.TenantGetSecretHandler` | service | mixed | 25~ | 30 | ozone tenant user get-secret. |
| 2645 | `org.apache.hadoop.ozone.shell.tenant.TenantCreateHandler` | service | mixed | 25~ | 30 | ozone tenant create. |
| 2646 | `org.apache.hadoop.ozone.shell.tenant.TenantAssignAdminHandler` | service | mixed | 25~ | 30 | ozone tenant user assign-admin. |
| 2647 | `org.apache.hadoop.ozone.shell.tenant.TenantRevokeAdminHandler` | service | mixed | 25~ | 30 | ozone tenant user revoke-admin. |
| 2648 | `org.apache.hadoop.ozone.shell.tenant.TenantSetSecretHandler` | service | mixed | 25~ | 30 | ozone tenant user set-secret. |
| 2649 | `org.apache.hadoop.ozone.shell.tenant.TenantRevokeUserAccessIdHandler` | service | mixed | 25~ | 30 | ozone tenant user revoke. |
| 2650 | `org.apache.hadoop.ozone.shell.tenant.TenantShell` | cli | mixed | 25~ | 20 | Shell for multi-tenant related operations. |

### Sub-feature: `shell.token`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2651 | `org.apache.hadoop.ozone.shell.token.TokenHandler` | abstract | mixed | 25~ | 30 | Handler for requests with an existing token. |
| 2652 | `org.apache.hadoop.ozone.shell.token.TokenOption` | service | mixed | 50~ | 30 | Option for token file. |
| 2653 | `org.apache.hadoop.ozone.shell.token.PrintTokenHandler` | cli | mixed | 25~ | 30 | inferred: PrintTokenHandler — role not documented. |
| 2654 | `org.apache.hadoop.ozone.shell.token.RenewerOption` | service | mixed | 25~ | 30 | Option for token renewer. |
| 2655 | `org.apache.hadoop.ozone.shell.token.TokenCommands` | cli | mixed | 25~ | 20 | Sub-command to group token related operations. |
| 2656 | `org.apache.hadoop.ozone.shell.token.GetTokenHandler` | cli | mixed | 25~ | 20 | Executes getDelegationToken api. |
| 2657 | `org.apache.hadoop.ozone.shell.token.RenewTokenHandler` | cli | mixed | 25~ | 20 | Executes renewDelegationToken api. |
| 2658 | `org.apache.hadoop.ozone.shell.token.CancelTokenHandler` | cli | mixed | 25~ | 20 | Executes cancelDelegationToken api. |

### Sub-feature: `shell.volume`

| reading_order | fqcn | kind | logic | loc | study (min) | role |
|--:|---|---|---|--:|--:|---|
| 2659 | `org.apache.hadoop.ozone.shell.volume.VolumeHandler` | abstract | mixed | 25~ | 30 | Base class for volume command handlers. |
| 2660 | `org.apache.hadoop.ozone.shell.volume.DeleteVolumeHandler` | cli | mixed | 150~ | 45 | Executes deleteVolume call for the shell. |
| 2661 | `org.apache.hadoop.ozone.shell.volume.VolumeUri` | service | mixed | 25~ | 30 | URI parameter for volume-specific commands. |
| 2662 | `org.apache.hadoop.ozone.shell.volume.RemoveAclVolumeHandler` | service | mixed | 25~ | 30 | Remove ACL from volume. |
| 2663 | `org.apache.hadoop.ozone.shell.volume.SetAclVolumeHandler` | service | mixed | 25~ | 30 | Set ACL on volume. |
| 2664 | `org.apache.hadoop.ozone.shell.volume.AddAclVolumeHandler` | service | mixed | 25~ | 30 | Add ACL to volume. |
| 2665 | `org.apache.hadoop.ozone.shell.volume.GetAclVolumeHandler` | service | mixed | 25~ | 30 | Get ACL of volume. |
| 2666 | `org.apache.hadoop.ozone.shell.volume.ListVolumeHandler` | cli | mixed | 50~ | 20 | Executes List Volume call. |
| 2667 | `org.apache.hadoop.ozone.shell.volume.InfoVolumeHandler` | cli | mixed | 25~ | 20 | Executes volume Info calls. |
| 2668 | `org.apache.hadoop.ozone.shell.volume.CreateVolumeHandler` | cli | mixed | 25~ | 20 | Executes the create volume call for the shell. |
| 2669 | `org.apache.hadoop.ozone.shell.volume.SetQuotaHandler` | cli | mixed | 25~ | 20 | Executes set volume quota calls. |
| 2670 | `org.apache.hadoop.ozone.shell.volume.ClearQuotaHandler` | cli | mixed | 25~ | 20 | clear quota of the volume. |
| 2671 | `org.apache.hadoop.ozone.shell.volume.VolumeCommands` | cli | mixed | 25~ | 20 | Subcommand to group volume related operations. |
| 2672 | `org.apache.hadoop.ozone.shell.volume.UpdateVolumeHandler` | cli | mixed | 25~ | 20 | Executes update volume calls. |



## Anchor details

### `OzoneAddress`

- **path:** `hadoop-ozone/cli-shell/src/main/java/org/apache/hadoop/ozone/shell/OzoneAddress.java`
- **loc:** 325~    **difficulty:** 4    **study:** 45 min    **concurrency:** single-threaded    **persistence:** in-memory
- **key collaborators:** `org.apache.hadoop.hdds.conf.ConfigurationSource`, `org.apache.hadoop.hdds.conf.MutableConfigurationSource`, `org.apache.hadoop.hdds.conf.OzoneConfiguration`, `org.apache.hadoop.ozone.OmUtils`, `org.apache.hadoop.ozone.client.OzoneClient`, `org.apache.hadoop.ozone.client.OzoneClientException`
- **test exemplar:** `hadoop-ozone/cli-shell/src/test/java/org/apache/hadoop/ozone/shell/TestOzoneAddress.java`
- **role:** Address of an ozone object for ozone shell.

`createClient` contains a three-way branch on the URI host: (a) if the host matches a configured OM HA service ID, it calls `createRpcClientFromServiceId` and rejects any port; (b) if the host is a plain hostname without port, it calls `createRpcClientFromHostPort` using `OmUtils.getOmRpcPort(conf)` as the default port; (c) if no host is given, it falls back to `OZONE_OM_INTERNAL_SERVICE_ID` then `OZONE_OM_SERVICE_IDS_KEY`. The private `stringToUri` method custom-parses the path to avoid `?` and `#` being URL-encoded by `java.net.URI`, which is important for key names containing those characters. The `EMPTY_HOST` sentinel (`"___DEFAULT___"`) is used to detect the host-omitted case after URI construction.


## Design docs

- `hadoop-hdds/docs/content/interface/Cli.md` — user-facing CLI reference covering `ozone sh` commands.
- `hadoop-hdds/docs/content/design/topology.md` — design doc for network topology, referenced by snapshot and key placement commands.
- no dedicated design doc for the shell command layer under `hadoop-hdds/docs/content/` on this branch.

## Seminal JIRAs / PRs

- HDDS-12155. Create new submodule for ozone shell
- HDDS-12686. Remove output of OzoneAddress in --verbose mode CLI
- HDDS-12687. Avoid ambiguity in URI descriptions
- HDDS-13376. Add server-side limit note to ozone sh snapshot diff --page-size option
- HDDS-7956. Deprecate camelCase and under_score style long options
- HDDS-15877. Extend getFileStatus head-op optimization to o3fs and remaining type-only callers
- HDDS-5306. Add `ozone sh * getacl` option to match `setacl` input format

## Sharp edges

- `OzoneAddress.createClient` throws `OzoneClientException` with the message "Service ID or host name must not be omitted when multiple ozone.om.service.ids is defined" when more than one OM service ID is configured and the URI has no host and `OZONE_OM_INTERNAL_SERVICE_ID` is not set — this is a silent runtime failure for multi-OM clusters where the user forgets to specify the service ID in the URI (OzoneAddress.java lines 163-167).
- The HTTP REST scheme (`o3://` vs old `http://`) throws `UnsupportedOperationException` with a message about AWS S3 — operators who were using HTTP REST will hit this immediately on upgrade (OzoneAddress.java lines 130-133).

## Related features

- `components/admin-clis/cli-common.md` — `Handler`, `GenericCli`, and base mixin classes that `OzoneAddress` collaborates with.
- `components/admin-clis/admin.md` — `ozone admin` commands that share the same Picocli launch infrastructure.
- `components/admin-clis/interactive-shell.md` — `ozone interactive` includes `OzoneShell` as the `sh` subtree.
- `components/OM/namespace.md` — server-side OM request handling for the volume/bucket/key operations triggered by these handlers.
- `components/OM/snapshot.md` — server-side snapshot logic driven by `SnapshotDiffHandler`, `CreateSnapshotHandler`, etc.

## Self-quiz

1. `OzoneAddress.createClient` has three client-creation paths for the `o3://` scheme. Describe the condition for each path and which factory method it calls.
2. `OzoneAddress` uses a custom `stringToUri` method rather than `new URI(string)` directly. What parsing problem does it solve, and for what type of Ozone object names does it matter?
3. `DeleteVolumeHandler` has ~150 LOC despite a conceptually simple delete operation. What extra logic does it contain beyond calling `objectStore.deleteVolume(name)`?
4. `SnapshotDiffHandler` is the most complex handler in this feature. What does it do to handle large diffs, and which proto field enables it?
5. `OzoneAddress.ensureSnapshotAddress` accepts a bucket path with a snapshot indicator but rejects a key path. What method does it call to determine whether a trailing path segment is a valid snapshot indicator?

<details>
<summary>Answers</summary>

Answer 1: (a) Host matches an OM HA service ID (`OmUtils.isOmHAServiceId`) → `createRpcClientFromServiceId`; port in URI throws. (b) Host is present but not an HA service ID → `createRpcClientFromHostPort` (using `OmUtils.getOmRpcPort` if no port). (c) No host → fall through `OZONE_OM_INTERNAL_SERVICE_ID` / `OZONE_OM_SERVICE_IDS_KEY` to `createRpcClientFromServiceId` or `createRpcClient`.

Answer 2: `java.net.URI` percent-encodes `?` and `#` in path segments, which would corrupt key names that legitimately contain those characters. `stringToUri` manually splits scheme, authority, and path before passing them to the `URI(scheme, authority, path, null, null)` constructor, which does not re-encode the path.

Answer 3: inferred: it checks whether the volume is empty before deleting, and handles errors such as the volume not existing or the user lacking permission; it may also have a `--force` flag to skip the emptiness check.

Answer 4: `SnapshotDiffHandler` uses pagination (`--page-size`, `--page-token`) to handle large diffs by submitting a job and then retrieving pages of results via separate RPC calls. The `pageToken` field from the response is passed as input to the next call to retrieve the next page.

Answer 5: `OmUtils.isBucketSnapshotIndicator(keyName)` — if true, the trailing segment is treated as a snapshot indicator and stored in `snapshotNameWithIndicator`; if false, `OzoneClientException` is thrown.

</details>
