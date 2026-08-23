# Component: DN

**Classes:** 308    **Features:** 23

## Feature and sub-feature index (in reading order)

| # | Feature | Sub-feature | Classes | Anchors | reading_order range |
|--:|---|---|--:|--:|---|
| 1 | [kv-container](kv-container.md) | `container.keyvalue` | 7 | 5 | 173–179 |
| 2 | [kv-container](kv-container.md) | `keyvalue.helpers` | 4 | 2 | 180–183 |
| 3 | [kv-container](kv-container.md) | `keyvalue.interfaces` | 2 | 0 | 184–185 |
| 4 | [kv-container-impl](kv-container-impl.md) | `keyvalue.impl` | 10 | 3 | 186–195 |
| 5 | [container-interfaces](container-interfaces.md) | `common.interfaces` | 14 | 0 | 196–209 |
| 6 | [erasure-coding](erasure-coding.md) | `reconstruction` | 4 | 1 | 210–213 |
| 7 | [erasure-coding](erasure-coding.md) | `coder` | 22 | 0 | 214–235 |
| 8 | [erasure-coding](erasure-coding.md) | `ec-chunk` | 1 | 0 | 236–236 |
| 9 | [erasure-coding](erasure-coding.md) | `ec.reconstruction` | 1 | 0 | 237–237 |
| 10 | [erasure-coding](erasure-coding.md) | `erasurecode.rawcoder` | 8 | 0 | 238–245 |
| 11 | [erasure-coding](erasure-coding.md) | `ozone.erasurecode` | 1 | 0 | 246–246 |
| 12 | [erasure-coding](erasure-coding.md) | `rawcoder.util` | 4 | 2 | 247–250 |
| 13 | [ratis-statemachine-dn](ratis-statemachine-dn.md) | `server.ratis` | 6 | 2 | 390–395 |
| 14 | [ratis-statemachine-dn](ratis-statemachine-dn.md) | `statemachine.background` | 2 | 1 | 396–397 |
| 15 | [hdds-volume](hdds-volume.md) | `common.volume` | 24 | 4 | 732–755 |
| 16 | [dn-rocksdb](dn-rocksdb.md) | `container.metadata` | 21 | 2 | 756–776 |
| 17 | [dn-statemachine](dn-statemachine.md) | `common.statemachine` | 8 | 4 | 807–814 |
| 18 | [dn-statemachine](dn-statemachine.md) | `statemachine.commandhandler` | 13 | 1 | 815–827 |
| 19 | [dn-reports](dn-reports.md) | `common.report` | 8 | 0 | 828–835 |
| 20 | [dn-scm-commands](dn-scm-commands.md) | `protocol.commands` | 17 | 0 | 836–852 |
| 21 | [container-replication-dn](container-replication-dn.md) | `container.replication` | 20 | 2 | 925–944 |
| 22 | [disk-balancer](disk-balancer.md) | `container.diskbalancer` | 9 | 3 | 965–973 |
| 23 | [disk-balancer](disk-balancer.md) | `diskbalancer.policy` | 3 | 0 | 974–976 |
| 24 | [container-checksum](container-checksum.md) | `container.checksum` | 6 | 1 | 1519–1524 |
| 25 | [dn-audit](dn-audit.md) | `ozone.audit` | 1 | 0 | 1525–1525 |
| 26 | [dn-freon](dn-freon.md) | `hdds.freon` | 1 | 0 | 1526–1526 |
| 27 | [dn-helpers](dn-helpers.md) | `common.helpers` | 8 | 2 | 1527–1534 |
| 28 | [dn-protocol](dn-protocol.md) | `ozone.protocol` | 4 | 0 | 1535–1538 |
| 29 | [dn-protocol](dn-protocol.md) | `ozone.protocolPB` | 4 | 0 | 1539–1542 |
| 30 | [dn-scm-client](dn-scm-client.md) | `hdds.scm` | 1 | 0 | 1543–1543 |
| 31 | [dn-service](dn-service.md) | `common.impl` | 10 | 6 | 1544–1553 |
| 32 | [dn-service](dn-service.md) | `common.states` | 1 | 0 | 1554–1554 |
| 33 | [dn-service](dn-service.md) | `container.common` | 2 | 0 | 1555–1556 |
| 34 | [dn-service](dn-service.md) | `container.ozoneimpl` | 17 | 2 | 1557–1573 |
| 35 | [dn-service](dn-service.md) | `ozone` | 7 | 1 | 1574–1580 |
| 36 | [dn-service](dn-service.md) | `states.datanode` | 2 | 0 | 1581–1582 |
| 37 | [dn-service](dn-service.md) | `states.endpoint` | 3 | 1 | 1583–1585 |
| 38 | [dn-streaming](dn-streaming.md) | `container.stream` | 9 | 0 | 1586–1594 |
| 39 | [dn-upgrade](dn-upgrade.md) | `container.upgrade` | 7 | 0 | 1595–1601 |
| 40 | [dn-utils](dn-utils.md) | `common.utils` | 10 | 0 | 1602–1611 |
| 41 | [dn-utils](dn-utils.md) | `utils.db` | 1 | 0 | 1612–1612 |
| 42 | [grpc-server-dn](grpc-server-dn.md) | `transport.server` | 5 | 2 | 1613–1617 |

_reading_order is a global monotonic index. Look up `reading_order + 1` in atlas.json for the next class._
