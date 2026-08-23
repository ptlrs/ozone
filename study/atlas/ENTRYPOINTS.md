# Entry Points

Every daemon and CLI listed here has a `main()` (or Java-service equivalent) that starts a service. Read these first to have call-stack anchors.

## OM daemon

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.hadoop.ozone.om.OzoneManagerStarter` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OzoneManagerStarter.java` | interface | This class provides a command line interface to start the OM using Picocli. |
| `org.apache.hadoop.ozone.om.OzoneManager` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/om/OzoneManager.java` | service | inferred: OzoneManager — role not documented. |
| `org.apache.hadoop.ozone.recon.spi.impl.OzoneManagerServiceProviderImpl` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/spi/impl/OzoneManagerServiceProviderImpl.java` | data | Implementation of the OzoneManager Service provider. |

## SCM daemon

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.hadoop.hdds.scm.server.StorageContainerManagerStarter` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/server/StorageContainerManagerStarter.java` | interface | This class provides a command line interface to start the SCM using Picocli. |
| `org.apache.hadoop.hdds.scm.server.StorageContainerManager` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/server/StorageContainerManager.java` | service | inferred: StorageContainerManager — role not documented. |

## DN daemon

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.hadoop.ozone.HddsDatanodeService` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/HddsDatanodeService.java` | service | Datanode service plugin to start the HDDS container services. |

## Recon daemon

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.hadoop.ozone.recon.ReconServer` | `hadoop-ozone/recon/src/main/java/org/apache/hadoop/ozone/recon/ReconServer.java` | service | Recon server main class that stops and starts recon services. |

## S3 Gateway

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.hadoop.ozone.s3.Gateway` | `hadoop-ozone/s3gateway/src/main/java/org/apache/hadoop/ozone/s3/Gateway.java` | service | This class is used to start/stop S3 compatible rest server. |
| `org.apache.hadoop.ozone.s3.S3GatewayHttpServer` | `hadoop-ozone/s3gateway/src/main/java/org/apache/hadoop/ozone/s3/S3GatewayHttpServer.java` | service | Http server to provide S3-compatible API. |

## HttpFS Gateway

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.ozone.fs.http.server.HttpFSServerWebServer` | `hadoop-ozone/httpfsgateway/src/main/java/org/apache/ozone/fs/http/server/HttpFSServerWebServer.java` | service | The HttpFS web server. |

## ozone sh (shell CLI)

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.hadoop.ozone.shell.OzoneShell` | `hadoop-ozone/cli-shell/src/main/java/org/apache/hadoop/ozone/shell/OzoneShell.java` | cli | Shell commands for native rpc object manipulation. |

## ozone admin CLI

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.hadoop.ozone.admin.OzoneAdmin` | `hadoop-ozone/cli-admin/src/main/java/org/apache/hadoop/ozone/admin/OzoneAdmin.java` | service | Ozone Admin Command line tool. |

## ozone debug CLI

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.hadoop.ozone.debug.OzoneDebug` | `hadoop-ozone/cli-debug/src/main/java/org/apache/hadoop/ozone/debug/OzoneDebug.java` | service | Ozone Debug Command line tool. |

## ozone repair CLI

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.hadoop.ozone.repair.RepairTool` | `hadoop-ozone/cli-repair/src/main/java/org/apache/hadoop/ozone/repair/RepairTool.java` | data | Parent class for all actionable repair commands. |

## ozone freon (bench CLI)

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.hadoop.ozone.freon.Freon` | `hadoop-ozone/freon/src/main/java/org/apache/hadoop/ozone/freon/Freon.java` | cli | Ozone data generator and performance test tool. |

## ozone insight CLI

| fqcn | path | kind | role |
|---|---|---|---|
| `org.apache.hadoop.ozone.insight.Insight` | `hadoop-ozone/insight/src/main/java/org/apache/hadoop/ozone/insight/Insight.java` | service | Command line utility to check logs/metrics of internal ozone components. |

## Call-stack starter map — client write path

The next atlas pass will populate the concrete traces per component. See:
- `components/Client/ozone-client.md` for the OzoneClient entry and RPC to OM
- `components/OM/om-request-key.md` for the OM request handling
- `components/SCM/block-manager.md` for block allocation
- `components/Client/hdds-client.md` for XceiverClientRatis and chunk-write path
- `components/DN/ratis-statemachine-dn.md` for the DN Ratis state-machine apply
- `components/DN/chunk-manager.md` for chunk-file writes
- `components/OM/om-request-key.md` (commit) for CommitKey
