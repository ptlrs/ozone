# Configuration Keys

Ozone configuration keys live in a small set of `*ConfigKeys` (constants) and `*Config`/`@Config` classes (typed getters). This is an index of those classes; consult the class Javadoc for the individual `ozone.*` keys it declares.

## Global config-key classes

| fqcn | path | one-liner |
|---|---|---|
| `org.apache.hadoop.ozone.OzoneConfigKeys` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/ozone/OzoneConfigKeys.java` | This class contains constants for configuration keys used in Ozone. |
| `org.apache.hadoop.hdds.scm.ScmConfigKeys` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/scm/ScmConfigKeys.java` | This class contains constants for configuration keys used in SCM. |
| `org.apache.hadoop.hdds.scm.ScmConfig` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/scm/ScmConfig.java` | The configuration class for the SCM service. |
| `org.apache.hadoop.ozone.om.OMConfigKeys` | `hadoop-ozone/common/src/main/java/org/apache/hadoop/ozone/om/OMConfigKeys.java` | Ozone Manager Constants. |
| `org.apache.hadoop.hdds.HddsConfigKeys` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/HddsConfigKeys.java` | This class contains constants for configuration keys and default values used in hdds. |
| `org.apache.hadoop.hdds.recon.ReconConfigKeys` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/recon/ReconConfigKeys.java` | This class contains constants for Recon related configuration keys used in SCM and Datanode. |
| `org.apache.hadoop.hdds.recon.ReconConfig` | `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/recon/ReconConfig.java` | The configuration class for the Recon service. |

## Typed `@Config` classes (auto-bound; look for `@ConfigGroup`)

| fqcn | component | one-liner |
|---|---|---|
| `org.apache.hadoop.ozone.local.LocalOzoneClusterConfig` | Bench & Insight | Configuration for a local Ozone cluster runtime. |
| `org.apache.hadoop.hdds.scm.OzoneClientConfig` | Client | Configuration values for Ozone Client. |
| `org.apache.hadoop.hdds.client.DefaultReplicationConfig` | HddsCommon | Replication configuration for EC replication. |
| `org.apache.hadoop.hdds.client.ECReplicationConfig` | HddsCommon | Replication configuration for EC replication. |
| `org.apache.hadoop.hdds.client.RatisReplicationConfig` | HddsCommon | Replication configuration for Ratis replication. |
| `org.apache.hadoop.hdds.client.ReplicatedReplicationConfig` | HddsCommon | Interface extension to denote replication configurations that work by copying the data replicationFactor times, like... |
| `org.apache.hadoop.hdds.client.ReplicationConfig` | HddsCommon | Replication configuration for any ReplicationType with all the required parameters. |
| `org.apache.hadoop.hdds.client.StandaloneReplicationConfig` | HddsCommon | Replication configuration for STANDALONE replication. |
| `org.apache.hadoop.hdds.conf.Config` | HddsCommon | Mark field to be configurable from ozone-site. |
| `org.apache.hadoop.hdds.conf.DatanodeRatisGrpcConfig` | HddsCommon | Ratis Grpc Config Keys. |
| `org.apache.hadoop.hdds.conf.DatanodeRatisServerConfig` | HddsCommon | Datanode Ratis server Configuration. |
| `org.apache.hadoop.hdds.conf.HddsPrometheusConfig` | HddsCommon | The configuration class for the Prometheus endpoint. |
| `org.apache.hadoop.hdds.conf.ReconfigurableConfig` | HddsCommon | Base class for config with reconfigurable properties. |
| `org.apache.hadoop.hdds.ratis.conf.RatisClientConfig` | HddsCommon | Configuration related to Ratis Client. |
| `org.apache.hadoop.hdds.scm.ScmRatisServerConfig` | HddsCommon | SCM Ratis Server config. |
| `org.apache.hadoop.hdds.scm.proxy.SCMClientConfig` | HddsCommon | Config for SCM Block Client. |
| `org.apache.hadoop.hdds.security.SecurityConfig` | HddsCommon | A class that deals with all Security related configs in HDDS. |
| `org.apache.hadoop.hdds.server.http.HttpConfig` | HddsCommon | Singleton to get access to Http related configuration. |
| `org.apache.hadoop.hdds.tracing.TracingConfig` | HddsCommon | OpenTelemetry tracing configuration for Ozone services. |
| `org.apache.hadoop.hdds.utils.db.TableConfig` | HddsCommon | Class that maintains Table Configuration. |
| `org.apache.hadoop.ozone.conf.OzoneServiceConfig` | HddsCommon | This class is used to define Ozone service level configs which are needed for all the ozone services. |
| `org.apache.hadoop.ozone.s3.S3GatewayConfigKeys` | Interfaces | This class contains constants for configuration keys used in S3G. |
| `org.apache.hadoop.ozone.s3secret.S3SecretConfigKeys` | Interfaces | This class contains constants for configuration keys used in S3 secret endpoint. |
| `org.apache.hadoop.ozone.om.ratis.OzoneManagerRatisServerConfig` | OM | Class which defines OzoneManager Ratis Server config. |
| `org.apache.hadoop.ozone.conf.OMClientConfig` | OzoneCommon | Config for OM Client. |
| `org.apache.hadoop.ozone.om.OmConfig` | OzoneCommon | Ozone Manager configuration. |
| `org.apache.hadoop.ozone.recon.ReconServerConfigKeys` | Recon | This class contains constants for Recon configuration keys. |
| `org.apache.hadoop.ozone.recon.ReconSqlDbConfig` | Recon | The configuration class for the Recon SQL DB. |
| `org.apache.hadoop.ozone.recon.chatbot.ChatbotConfigKeys` | Recon | Configuration keys for Recon Chatbot service. |
| `org.apache.hadoop.ozone.recon.scm.ReconStorageConfig` | Recon | Recon's extension of SCMStorageConfig. |
| `org.apache.hadoop.ozone.recon.tasks.ReconTaskConfig` | Recon | The configuration class for the Recon tasks. |
| `org.apache.hadoop.hdds.utils.db.managed.ManagedBlockBasedTableConfig` | RocksDB | Managed BlockBasedTableConfig. |
| `org.apache.hadoop.hdds.scm.server.SCMHTTPServerConfig` | SCM | SCM HTTP Server configuration in Java style configuration class. |
| `org.apache.hadoop.hdds.scm.server.SCMStorageConfig` | SCM | SCMStorageConfig is responsible for management of the StorageDirectories used by the SCM. |
| `org.apache.hadoop.hdds.security.symmetric.SecretKeyConfig` | Security | Configurations related to SecretKeys lifecycle management. |
