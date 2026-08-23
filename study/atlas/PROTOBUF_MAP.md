# Protobuf Map

Every wire type in Ozone is defined by a `.proto`, generated into `*Protos.java`, and dispatched through a hand-written `ProtocolPB` translator pair. Read the `.proto` first; then the client-side `ProtocolTranslatorPB` (Java → Protobuf → RPC → …); then the server-side `ProtocolServerSideTranslatorPB` (RPC → Protobuf → Java handler).

## Proto files, by module

### `hadoop-hdds/interface-admin`

- `hadoop-hdds/interface-admin/src/main/proto/ScmAdminProtocol.proto`

### `hadoop-hdds/interface-client`

- `hadoop-hdds/interface-client/src/main/proto/DatanodeClientProtocol.proto`
- `hadoop-hdds/interface-client/src/main/proto/DiskBalancerProtocol.proto`
- `hadoop-hdds/interface-client/src/main/proto/hdds.proto`
- `hadoop-hdds/interface-client/src/main/proto/IpcConnectionContext.proto`
- `hadoop-hdds/interface-client/src/main/proto/ProtobufRpcEngine.proto`
- `hadoop-hdds/interface-client/src/main/proto/ReconfigureProtocol.proto`
- `hadoop-hdds/interface-client/src/main/proto/RpcHeader.proto`

### `hadoop-hdds/interface-server`

- `hadoop-hdds/interface-server/src/main/proto/InterSCMProtocol.proto`
- `hadoop-hdds/interface-server/src/main/proto/SCMRatisProtocol.proto`
- `hadoop-hdds/interface-server/src/main/proto/ScmSecretKeyProtocol.proto`
- `hadoop-hdds/interface-server/src/main/proto/ScmServerDatanodeHeartbeatProtocol.proto`
- `hadoop-hdds/interface-server/src/main/proto/ScmServerProtocol.proto`
- `hadoop-hdds/interface-server/src/main/proto/ScmServerSecurityProtocol.proto`
- `hadoop-hdds/interface-server/src/main/proto/SCMUpdateProtocol.proto`

### `hadoop-ozone/interface-client`

- `hadoop-ozone/interface-client/src/main/proto/OMAdminProtocol.proto`
- `hadoop-ozone/interface-client/src/main/proto/OmClientProtocol.proto`
- `hadoop-ozone/interface-client/src/main/proto/OmInterServiceProtocol.proto`
- `hadoop-ozone/interface-client/src/main/proto/Security.proto`

### `hadoop-ozone/interface-storage`

- `hadoop-ozone/interface-storage/src/main/proto/OmStorageProtocol.proto`

## Translator pairs (`ProtocolPB` / `ProtocolTranslatorPB` / `ProtocolServerSideTranslatorPB`)

| Service | class | path |
|---|---|---|
| DiskBalancer | `org.apache.hadoop.hdds.protocolPB.DiskBalancerProtocolPB` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/protocolPB/DiskBalancerProtocolPB.java` |
| DiskBalancer | `org.apache.hadoop.hdds.protocolPB.DiskBalancerProtocolServerSideTranslatorPB` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/protocolPB/DiskBalancerProtocolServerSideTranslatorPB.java` |
| OMAdmin | `org.apache.hadoop.ozone.om.protocolPB.OMAdminProtocolPB` | `hadoop-ozone/common/src/main/java/org/apache/hadoop/ozone/om/protocolPB/OMAdminProtocolPB.java` |
| OMInterService | `org.apache.hadoop.ozone.om.protocolPB.OMInterServiceProtocolPB` | `hadoop-ozone/common/src/main/java/org/apache/hadoop/ozone/om/protocolPB/OMInterServiceProtocolPB.java` |
| OzoneManager | `org.apache.hadoop.ozone.om.protocolPB.OzoneManagerProtocolPB` | `hadoop-ozone/common/src/main/java/org/apache/hadoop/ozone/om/protocolPB/OzoneManagerProtocolPB.java` |
| OzoneManager | `org.apache.hadoop.ozone.protocolPB.OzoneManagerProtocolServerSideTranslatorPB` | `hadoop-ozone/ozone-manager/src/main/java/org/apache/hadoop/ozone/protocolPB/OzoneManagerProtocolServerSideTranslatorPB.java` |
| ReconDatanode | `org.apache.hadoop.ozone.protocolPB.ReconDatanodeProtocolPB` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/protocolPB/ReconDatanodeProtocolPB.java` |
| Reconfigure | `org.apache.hadoop.hdds.protocolPB.ReconfigureProtocolPB` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/protocolPB/ReconfigureProtocolPB.java` |
| Reconfigure | `org.apache.hadoop.hdds.protocolPB.ReconfigureProtocolServerSideTranslatorPB` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/protocolPB/ReconfigureProtocolServerSideTranslatorPB.java` |
| SCMSecurity | `org.apache.hadoop.hdds.protocolPB.SCMSecurityProtocolPB` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/protocolPB/SCMSecurityProtocolPB.java` |
| SCMSecurity | `org.apache.hadoop.hdds.scm.protocol.SCMSecurityProtocolServerSideTranslatorPB` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/protocol/SCMSecurityProtocolServerSideTranslatorPB.java` |
| ScmBlockLocation | `org.apache.hadoop.hdds.scm.protocolPB.ScmBlockLocationProtocolPB` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/scm/protocolPB/ScmBlockLocationProtocolPB.java` |
| ScmBlockLocation | `org.apache.hadoop.hdds.scm.protocol.ScmBlockLocationProtocolServerSideTranslatorPB` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/protocol/ScmBlockLocationProtocolServerSideTranslatorPB.java` |
| SecretKey | `org.apache.hadoop.hdds.scm.protocol.SecretKeyProtocolServerSideTranslatorPB` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/protocol/SecretKeyProtocolServerSideTranslatorPB.java` |
| StorageContainerDatanode | `org.apache.hadoop.ozone.protocolPB.StorageContainerDatanodeProtocolServerSideTranslatorPB` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/protocolPB/StorageContainerDatanodeProtocolServerSideTranslatorPB.java` |
| StorageContainerDatanode | `org.apache.hadoop.ozone.protocolPB.StorageContainerDatanodeProtocolPB` | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/protocolPB/StorageContainerDatanodeProtocolPB.java` |
| StorageContainerLocation | `org.apache.hadoop.hdds.scm.protocolPB.StorageContainerLocationProtocolPB` | `hadoop-hdds/framework/src/main/java/org/apache/hadoop/hdds/scm/protocolPB/StorageContainerLocationProtocolPB.java` |
| StorageContainerLocation | `org.apache.hadoop.hdds.scm.protocol.StorageContainerLocationProtocolServerSideTranslatorPB` | `hadoop-hdds/server-scm/src/main/java/org/apache/hadoop/hdds/scm/protocol/StorageContainerLocationProtocolServerSideTranslatorPB.java` |

## The hot protos (read these first)

- `OmClientProtocol.proto` — `OMRequest` / `OMResponse` union; every OM write flows through this envelope. Dispatched by `OzoneManagerProtocolServerSideTranslatorPB` → `OMClientRequest` subclass.
- `ScmServerProtocol.proto` — SCM allocate-block, container / pipeline queries used by the client and OM.
- `ScmServerDatanodeHeartbeatProtocol.proto` — DN → SCM heartbeat + command channel; every background service (replication, balancer, scanner) closes its control loop through this.
- `DatanodeClientProtocol.proto` — client ↔ DN chunk/block gRPC surface used by `XceiverClientRatis` and `XceiverClientGrpc`.
- `hdds.proto` — shared types (`Pipeline`, `ContainerInfo`, `BlockID`, `ReplicationConfig`, `ChecksumData`, `DatanodeDetails`) that every other proto imports.
- `SCMRatisProtocol.proto` — SCM HA Ratis envelope (leader → follower state transitions).
