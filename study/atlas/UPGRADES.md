# Upgrades & Finalization

Cross-feature upgrade / finalization landmines. Ozone uses a **layout-version** framework: each service (OM, SCM, DN) tracks a persisted layout version, and features are gated by "layout features" that finalize atomically after a rolling restart.

## Layout-feature enums

- `org.apache.hadoop.ozone.upgrade.LayoutFeature` — `hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/upgrade/LayoutFeature.java`
- `org.apache.hadoop.hdds.upgrade.HDDSLayoutFeature` — `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/upgrade/HDDSLayoutFeature.java`
- `org.apache.hadoop.ozone.om.upgrade.OMLayoutFeature` — `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/upgrade/OMLayoutFeature.java`
- `org.apache.hadoop.ozone.recon.upgrade.ReconLayoutFeature` — `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/upgrade/ReconLayoutFeature.java`

## Upgrade framework classes (HddsCommon)

| fqcn | path |
|---|---|
| `org.apache.hadoop.hdds.upgrade.BelongsToHDDSLayoutVersion` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/upgrade/BelongsToHDDSLayoutVersion.java` |
| `org.apache.hadoop.hdds.upgrade.HDDSLayoutFeature` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/upgrade/HDDSLayoutFeature.java` |
| `org.apache.hadoop.hdds.upgrade.HDDSLayoutVersionManager` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/upgrade/HDDSLayoutVersionManager.java` |
| `org.apache.hadoop.hdds.upgrade.HDDSUpgradeAction` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/upgrade/HDDSUpgradeAction.java` |
| `org.apache.hadoop.ozone.upgrade.AbstractLayoutVersionManager` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/upgrade/AbstractLayoutVersionManager.java` |
| `org.apache.hadoop.ozone.upgrade.BasicUpgradeFinalizer` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/upgrade/BasicUpgradeFinalizer.java` |
| `org.apache.hadoop.ozone.upgrade.DefaultUpgradeFinalizationExecutor` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/upgrade/DefaultUpgradeFinalizationExecutor.java` |
| `org.apache.hadoop.ozone.upgrade.LayoutFeature` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/upgrade/LayoutFeature.java` |
| `org.apache.hadoop.ozone.upgrade.LayoutVersionManager` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/upgrade/LayoutVersionManager.java` |
| `org.apache.hadoop.ozone.upgrade.LayoutVersionManagerMXBean` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/upgrade/LayoutVersionManagerMXBean.java` |
| `org.apache.hadoop.ozone.upgrade.UpgradeActionHdds` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/upgrade/UpgradeActionHdds.java` |
| `org.apache.hadoop.ozone.upgrade.UpgradeException` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/upgrade/UpgradeException.java` |
| `org.apache.hadoop.ozone.upgrade.UpgradeFinalization` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/upgrade/UpgradeFinalization.java` |
| `org.apache.hadoop.ozone.upgrade.UpgradeFinalizationExecutor` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/upgrade/UpgradeFinalizationExecutor.java` |
| `org.apache.hadoop.ozone.upgrade.UpgradeFinalizer` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/ozone/upgrade/UpgradeFinalizer.java` |

## On-disk format transitions

- **Container v3 / Schema v3** — DN block metadata moved from one-RocksDB-per-container to a shared per-DN RocksDB with column-family-per-container. Classes to read: `hadoop-hdds/container-service/.../keyvalue/impl/*V3*.java` and `DatanodeSchemaThreeDBDefinition`.
- **FSO layout** — new bucket layout with `dirTable`/`fileTable`. Gated by `OMLayoutFeature.PREFIX_LAYOUT`.
- **SCM-HA finalization** — moves SCM from single-node metadata to a Ratis group. Gated by `HDDSLayoutFeature.SCM_HA`.
- **Snapshot** — introduces `snapshotInfoTable` and per-snapshot RocksDB checkpoints. Gated by `OMLayoutFeature.BUCKET_LAYOUT_SUPPORT` and `FILESYSTEM_SNAPSHOT`.
- **Erasure coding** — adds EC replication types over the wire. Gated by `OMLayoutFeature.ERASURE_CODED_STORAGE_SUPPORT`.
- **Ratis snapshot compat** — a newer OM can install a snapshot that references tables the old code did not know; that is why every OM install of a snapshot must re-run schema `Init`.

## Sharp edges

- Do **not** hand-edit `VERSION` files under storage dirs; the layout version is the source of truth and mismatched files can trigger unnecessary re-registration or refuse to start.
- `Finalization` is monotonic per service; there is no downgrade path once a layout feature has finalized on any node.
- During a rolling upgrade the leader may be newer than followers — every OM request handler must guard its RocksDB writes on `OMLayoutVersionManager.isAllowed(...)`.
