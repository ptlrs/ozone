#!/usr/bin/env python3
"""v2: eliminate 'misc' buckets by adding fallback rules for observed paths."""
import json, os, re, sys
from pathlib import Path
from collections import Counter

RAW = Path('study/atlas/tmp/raw_classes.tsv')
OUT_JSONL = Path('study/atlas/tmp/classes.jsonl')
OUT_FEATURES = Path('study/atlas/tmp/features.tsv')
OUT_ANCHORS = Path('study/atlas/tmp/anchors.tsv')

RULES = [
    ('hadoop-hdds/managed-rocksdb/',       'RocksDB',   'managed-rocksdb'),
    ('hadoop-hdds/rocksdb-checkpoint-differ/', 'RocksDB','checkpoint-differ'),
    ('hadoop-hdds/rocks-native/',          'RocksDB',   'rocks-native'),
    ('hadoop-ozone/s3gateway/',            'Interfaces','s3gateway'),
    ('hadoop-ozone/s3-secret-store/',      'Interfaces','s3-secret-store'),
    ('hadoop-ozone/httpfsgateway/',        'Interfaces','httpfs'),
    ('hadoop-ozone/ozonefs-common/',       'Interfaces','ozonefs-common'),
    ('hadoop-ozone/ozonefs/',              'Interfaces','ozonefs-hadoop-current'),
    ('hadoop-ozone/ozonefs-hadoop2/',      'Interfaces','ozonefs-hadoop2'),
    ('hadoop-ozone/ozonefs-hadoop3/',      'Interfaces','ozonefs-hadoop3'),
    ('hadoop-ozone/iceberg/',              'Interfaces','iceberg'),
    ('hadoop-ozone/multitenancy-ranger/',  'Interfaces','multitenancy-ranger'),
    ('hadoop-ozone/cli-admin/',            'Admin CLIs', 'admin'),
    ('hadoop-ozone/cli-shell/',            'Admin CLIs', 'shell'),
    ('hadoop-ozone/cli-interactive/',      'Admin CLIs', 'interactive-shell'),
    ('hadoop-ozone/cli-debug/',            'Debug & Repair', 'debug'),
    ('hadoop-ozone/cli-repair/',           'Debug & Repair', 'repair'),
    ('hadoop-hdds/cli-common/',            'Admin CLIs', 'cli-common'),
    ('hadoop-ozone/freon/',                'Bench & Insight', 'freon'),
    ('hadoop-ozone/insight/',              'Bench & Insight', 'insight'),
    ('hadoop-ozone/vapor/',                'Bench & Insight', 'vapor'),
    ('hadoop-ozone/tools/',                'Bench & Insight', 'ozone-tools'),
    ('hadoop-hdds/client/',                'Client',    'hdds-client'),
    ('hadoop-ozone/client/',               'Client',    'ozone-client'),
    ('hadoop-hdds/server-scm/',            'SCM',       ''),
    ('hadoop-hdds/container-service/',     'DN',        ''),
    ('hadoop-hdds/erasurecode/',           'DN',        'erasure-coding'),
    ('hadoop-ozone/ozone-manager/',        'OM',        ''),
    ('hadoop-ozone/interface-storage/',    'OM',        'interface-storage'),
    ('hadoop-ozone/recon/',                'Recon',     ''),
    ('hadoop-ozone/recon-codegen/',        'Recon',     'recon-codegen'),
    ('hadoop-hdds/framework/',             'Framework', ''),
    ('hadoop-hdds/common/',                'HddsCommon',''),
    ('hadoop-hdds/config/',                'HddsCommon','config-annotations'),
    ('hadoop-hdds/annotations/',           'HddsCommon','annotations'),
    ('hadoop-hdds/interface-admin/',       'HddsCommon','interface-admin'),
    ('hadoop-hdds/interface-client/',      'HddsCommon','interface-client'),
    ('hadoop-hdds/interface-server/',      'HddsCommon','interface-server'),
    ('hadoop-ozone/common/',               'OzoneCommon',''),
    ('hadoop-ozone/interface-client/',     'OzoneCommon','interface-client'),
]

FEATURE_RULES = {
  'SCM': [
    ('/container/replication/',            'container-replication'),
    ('/container/balancer/',               'container-balancer'),
    ('/container/reconciliation/',         'container-reconciliation'),
    ('/container/',                        'container-manager'),
    ('/pipeline/choose/',                  'pipeline-choose-policy'),
    ('/pipeline/',                         'pipeline-manager'),
    ('/block/',                            'block-manager'),
    ('/node/',                             'node-manager'),
    ('/safemode/',                         'safemode'),
    ('/security/',                         'scm-security'),
    ('/ha/',                               'scm-ha'),
    ('/ratis/',                            'scm-ha'),
    ('/upgrade/',                          'upgrade'),
    ('/metadata/',                         'scm-metadata'),
    ('/protocol/',                         'scm-protocol'),
    ('/protocolPB/',                       'scm-protocol'),
    ('/server/',                           'scm-server'),
    ('/events/',                           'scm-events'),
    ('/net/',                              'network-topology'),
    ('/crl/',                              'scm-security'),
    ('/cert/',                             'scm-security'),
    ('/certificate/',                      'scm-security'),
    ('/audit/',                            'scm-audit'),
    ('/command/',                          'scm-commands'),
    ('StorageContainerManager',            'scm-server'),
    ('/scm/',                              'scm-server'),
  ],
  'DN': [
    ('/keyvalue/impl/',                    'kv-container-impl'),
    ('/keyvalue/statemachine/',            'ratis-statemachine-dn'),
    ('/keyvalue/',                         'kv-container'),
    ('/transport/server/ratis/',           'ratis-statemachine-dn'),
    ('/transport/server/',                 'grpc-server-dn'),
    ('/transport/',                        'grpc-server-dn'),
    ('/chunk/',                            'chunk-manager'),
    ('/block/',                            'block-manager-dn'),
    ('/replication/',                      'container-replication-dn'),
    ('/reconciliation/',                   'container-reconciliation-dn'),
    ('/checksum/',                         'container-checksum'),
    ('/scanner/',                          'container-scanner'),
    ('/diskbalancer/',                     'disk-balancer'),
    ('/volume/',                           'hdds-volume'),
    ('/upgrade/',                          'dn-upgrade'),
    ('/statemachine/',                     'dn-statemachine'),
    ('/report/',                           'dn-reports'),
    ('/commandhandler/',                   'dn-command-handlers'),
    ('/interfaces/',                       'container-interfaces'),
    ('/utils/',                            'dn-utils'),
    ('/metadata/',                         'dn-rocksdb'),
    ('/helpers/',                          'dn-helpers'),
    ('/ec/reconstruction/',                'erasure-coding'),
    ('/ozoneimpl/',                        'dn-service'),
    ('/stream/',                           'dn-streaming'),
    ('/protocol/commands/',                'dn-scm-commands'),
    ('/protocol/',                         'dn-protocol'),
    ('/protocolPB/',                       'dn-protocol'),
    ('/hdds/scm/',                         'dn-scm-client'),
    ('/hdds/freon/',                       'dn-freon'),
    ('/ozone/audit/',                      'dn-audit'),
    ('/ozone/',                            'dn-service'),
    ('HddsDatanodeService',                'dn-service'),
    ('/container/common/',                 'container-common'),
    ('/container/',                        'container-common'),
  ],
  'OM': [
    ('/request/s3/',                       'om-request-s3'),
    ('/request/key/',                      'om-request-key'),
    ('/request/file/',                     'om-request-file'),
    ('/request/bucket/',                   'om-request-bucket'),
    ('/request/volume/',                   'om-request-volume'),
    ('/request/snapshot/',                 'om-request-snapshot'),
    ('/request/tenant/',                   'om-request-tenant'),
    ('/request/upgrade/',                  'om-request-upgrade'),
    ('/request/',                          'om-request'),
    ('/response/',                         'om-response'),
    ('/ratis/',                            'om-ratis'),
    ('/ratis_snapshot/',                   'om-ratis'),
    ('/snapshot/',                         'om-snapshot'),
    ('/service/',                          'om-background-services'),
    ('/lock/',                             'om-locking'),
    ('/upgrade/',                          'om-upgrade'),
    ('/security/',                         'om-security'),
    ('/multitenant/',                      'om-multitenant'),
    ('/codec/',                            'om-codecs'),
    ('/execution/',                        'om-execution'),
    ('/cache/',                            'om-cache'),
    ('/audit/',                            'om-audit'),
    ('/protocolPB/',                       'om-protocol'),
    ('/protocol/',                         'om-protocol'),
    ('/fs/',                               'om-fs'),
    ('/helpers/',                          'om-helpers'),
    ('/exceptions/',                       'om-exceptions'),
    ('/utils/',                            'om-utils'),
    ('OMMetadataManager',                  'om-metadata'),
    ('KeyManager',                         'om-key-manager'),
    ('BucketManager',                      'om-bucket-manager'),
    ('VolumeManager',                      'om-volume-manager'),
    ('OzoneManager',                       'om-server'),
    ('/om/',                               'om-server'),
  ],
  'Recon': [
    ('/tasks/',                            'recon-tasks'),
    ('/scm/',                              'recon-scm'),
    ('/api/',                              'recon-api'),
    ('/persistence/',                      'recon-persistence'),
    ('/scheduler/',                        'recon-scheduler'),
    ('/spi/',                              'recon-spi'),
    ('/security/',                         'recon-security'),
    ('/recovery/',                         'recon-recovery'),
    ('/schema/',                           'recon-schema'),
    ('/heatmap/',                          'recon-heatmap'),
    ('/upgrade/',                          'recon-upgrade'),
    ('/fsck/',                             'recon-fsck'),
    ('/logging/',                          'recon-logging'),
    ('/metrics/',                          'recon-metrics'),
    ('ReconServer',                        'recon-server'),
    ('/recon/',                            'recon-server'),
  ],
  'Framework': [
    ('/ratis/',                            'ratis-integration',       'Ratis-integration'),
    ('/scm/ha/',                           'ratis-integration',       'Ratis-integration'),
    ('/security/token/',                   'security-tokens',         'Security'),
    ('/security/x509/',                    'security-x509',           'Security'),
    ('/security/symmetric/',               'security-symmetric',      'Security'),
    ('/security/ssl/',                     'security-ssl',            'Security'),
    ('/security/',                         'security-framework',      'Security'),
    ('/http/',                             'http-server',             'HddsCommon'),
    ('/db/',                               'hdds-db-utils',           'HddsCommon'),
    ('/hdds/utils/db/',                    'hdds-db-utils',           'HddsCommon'),
    ('/metrics/',                          'metrics-utils',           'HddsCommon'),
    ('/utils/',                            'framework-utils',         'HddsCommon'),
    ('/tracing/',                          'tracing',                 'HddsCommon'),
    ('/audit/',                            'audit',                   'HddsCommon'),
    ('/server/',                           'framework-server',        'HddsCommon'),
    ('/conf/',                             'config-runtime',          'HddsCommon'),
    ('/protocol/',                         'framework-protocol',      'HddsCommon'),
    ('/protocolPB/',                       'framework-protocol',      'HddsCommon'),
    ('/upgrade/',                          'upgrade-framework',       'HddsCommon'),
    ('/util/',                             'framework-utils',         'HddsCommon'),
    ('/scm/net/',                          'network-topology',        'HddsCommon'),
    ('/scm/metadata/',                     'hdds-db-utils',           'HddsCommon'),
    ('/scm/proxy/',                        'scm-client-proxy',        'HddsCommon'),
    ('/scm/client/',                       'scm-client-proxy',        'HddsCommon'),
    ('/scm/container/common/',             'container-common',        'HddsCommon'),
    ('/fs/',                               'fs-utils',                'HddsCommon'),
    ('/hdds/freon/',                       'framework-freon',         'HddsCommon'),
    ('/ozone/lease/',                      'lease-manager',           'HddsCommon'),
    ('/ozone/common/',                     'ozone-common-primitives', 'HddsCommon'),
    ('/hdds/',                             'hdds-primitives',         'HddsCommon'),
  ],
  'HddsCommon': [
    ('/security/token/',                   'security-tokens'),
    ('/security/',                         'security-common'),
    ('/security_/',                        'hadoop-shaded'),
    ('/io_/',                              'hadoop-shaded'),
    ('/ipc_/',                             'hadoop-shaded'),
    ('/protocol/',                         'protocol-common'),
    ('/protocolPB/',                       'protocol-common'),
    ('/upgrade/',                          'upgrade-common'),
    ('/scm/pipeline/',                     'pipeline-common'),
    ('/scm/container/common/',             'container-common'),
    ('/scm/container/',                    'container-common'),
    ('/scm/net/',                          'network-topology'),
    ('/scm/ha/',                           'ratis-integration'),
    ('/scm/storage/',                      'storage-common'),
    ('/scm/',                              'scm-common'),
    ('/ratis/',                            'ratis-integration'),
    ('/utils/',                            'hdds-utils'),
    ('/audit/',                            'audit-common'),
    ('/tracing/',                          'tracing-common'),
    ('/conf/',                             'config-common'),
    ('/ozone/',                            'ozone-common-primitives'),
    ('/hdds/',                             'hdds-primitives'),
    ('/client/',                           'hdds-client-common'),
    ('/annotation/',                       'annotations'),
  ],
  'OzoneCommon': [
    ('/audit/',                            'audit-common'),
    ('/security/',                         'security-common'),
    ('/upgrade/',                          'upgrade-common'),
    ('/protocol/',                         'protocol-common'),
    ('/protocolPB/',                       'protocol-common'),
    ('/om/helpers/',                       'om-helpers-common'),
    ('/om/',                               'om-common'),
    ('/s3/',                               's3-common'),
    ('/snapshot/',                         'snapshot-common'),
    ('/client/',                           'client-common'),
    ('/util/',                             'ozone-utils'),
    ('/fs/',                               'ozone-fs-common'),
    ('/freon/',                            'freon-common'),
    ('/ozone/',                            'ozone-common-primitives'),
  ],
  'Interfaces': [
    ('/s3/endpoint/',                      's3-endpoint'),
    ('/s3/signature/',                     's3-signature'),
    ('/s3/exception/',                     's3-errors'),
    ('/s3/util/',                          's3-utils'),
    ('/s3/metrics/',                       's3-metrics'),
    ('/s3/io/',                            's3-io'),
    ('/s3/audit/',                         's3-audit'),
    ('/s3/commontypes/',                   's3-common-types'),
    ('/s3/awssdk/',                        's3-awssdk'),
    ('/s3secret/',                         's3-secrets'),
    ('/httpfs/',                           'httpfs'),
    ('/ranger/',                           'multitenancy-ranger'),
    ('/iceberg/',                          'iceberg'),
    ('/ozonefs/',                          'ozonefs'),
  ],
  'Admin CLIs': [
    ('/admin/scm/',                        'admin-scm'),
    ('/admin/om/',                         'admin-om'),
    ('/admin/datanode/',                   'admin-datanode'),
    ('/admin/reconfig/',                   'admin-reconfig'),
    ('/admin/security/',                   'admin-security'),
    ('/admin/nssummary/',                  'admin-nssummary'),
    ('/admin/container/',                  'admin-container'),
    ('/admin/replication/',                'admin-replication'),
    ('/admin/ratis/',                      'admin-ratis'),
    ('/admin/',                            'admin'),
    ('/shell/',                            'shell'),
    ('/interactive/',                      'interactive'),
  ],
  'Debug & Repair': [
    ('/debug/ldb',                         'debug-ldb'),
    ('/debug/container/',                  'debug-container'),
    ('/debug/replicas/',                   'debug-replicas'),
    ('/debug/',                            'debug'),
    ('/repair/om/',                        'repair-om'),
    ('/repair/scm/',                       'repair-scm'),
    ('/repair/dn/',                        'repair-dn'),
    ('/repair/',                           'repair'),
  ],
  'Bench & Insight': [
    ('/freon/',                            'freon'),
    ('/insight/',                          'insight'),
    ('/vapor/',                            'vapor'),
    ('/tools/',                            'ozone-tools'),
  ],
  'Client': [
    ('/client/rpc/',                       'client-rpc'),
    ('/client/io/',                        'client-io'),
    ('/client/ec/',                        'client-ec'),
    ('/client/replication/',               'client-replication'),
    ('/client/checksum/',                  'client-checksum'),
    ('/client/protocol/',                  'client-protocol'),
    ('/client/',                           'client-core'),
  ],
  'RocksDB': [
    ('/checkpoint/differ/',                'checkpoint-differ'),
    ('/rocksdiff/',                        'checkpoint-differ'),
    ('/rocksnative/',                      'rocks-native'),
    ('/rocksdb/native/',                   'rocks-native'),
    ('/managed/',                          'managed-rocksdb'),
    ('/rocksdb/',                          'managed-rocksdb'),
  ],
}

KIND_HINTS = [
  ('Exception',                'exception'),
  ('Metrics',                  'metrics'),
  ('Config.java',              'config'),
  ('ConfigKeys',               'config'),
  ('Codec.java',               'util'),
  ('Factory',                  'factory'),
  ('Builder',                  'data'),
  ('ProtocolPB',               'rpc-stub'),
  ('ProtocolTranslatorPB',     'rpc-stub'),
  ('ProtocolServerSideTranslatorPB','rpc-stub'),
  ('CLI.java',                 'cli'),
  ('Command.java',             'cli'),
  ('Subcommand',               'cli'),
]

CONCURRENCY_MARKERS = [
  (re.compile(r'@ThreadSafe|Concurrent|ReadWriteLock|synchronized\s*\('), 'thread-safe'),
  (re.compile(r'ExecutorService|newSingleThreadExecutor|newFixedThreadPool|ScheduledExecutor|BlockingQueue'), 'actor/queue'),
  (re.compile(r'extends StateMachine|applyTransaction|takeSnapshot\('), 'ratis-applied'),
]

def classify(path: str):
    comp = feat = ''
    for sub, c, f in RULES:
        if sub in path:
            comp, feat = c, f
            break
    if not comp:
        comp = 'Unclassified'
    if comp in FEATURE_RULES:
        for tup in FEATURE_RULES[comp]:
            if len(tup) == 2:
                sub, f = tup
                if sub in path:
                    if not feat: feat = f
                    break
            elif len(tup) == 3:
                sub, f, comp_over = tup
                if sub in path:
                    feat, comp = f, comp_over
                    break
    if not feat: feat = 'misc'
    return comp, feat

def kind_of(fqcn, path, body):
    b = body[:8000]
    for suf, k in KIND_HINTS:
        if fqcn.endswith('.' + suf.replace('.java','')) or suf in os.path.basename(path):
            return k
    if re.search(r'\bpublic\s+(final\s+)?enum\s+', b): return 'data'
    if re.search(r'\binterface\s+\w+', b) and '@FunctionalInterface' not in b:
        return 'interface'
    if re.search(r'\babstract\s+class\s+', b): return 'abstract'
    if '@Path(' in b or '@Provider' in b: return 'service'
    if 'extends StateMachine' in b or 'implements StateMachine' in b: return 'state-machine'
    if 'BackgroundService' in b or 'implements Runnable' in b or 'implements Callable' in b: return 'service'
    if re.search(r'@Command\(', b): return 'cli'
    cls = fqcn.rsplit('.',1)[-1]
    if 'DTO' in fqcn or cls.endswith('Info') or cls.endswith('Response') or cls.endswith('Request'):
        return 'dto'
    return 'service'

def logic_weight_of(kind, loc_code):
    if kind in ('data','dto','exception','config','rpc-stub'): return 'data-only'
    if loc_code < 60: return 'mixed'
    if loc_code >= 200: return 'logic-heavy'
    return 'mixed'

def concurrency_of(body):
    for rx, tag in CONCURRENCY_MARKERS:
        if rx.search(body): return tag
    return 'single-threaded'

def persistence_of(body):
    m = re.findall(r'(?:get|open)Table\("(\w+)"', body)
    if m:
        return 'RocksDB:'+','.join(sorted(set(m))[:4])
    if 'RocksDB' in body or 'ManagedRocksDB' in body: return 'RocksDB'
    if re.search(r'FileOutputStream|Files\.write|writeToDisk', body): return 'on-disk'
    return 'in-memory'

JAVADOC_RE = re.compile(r'/\*\*(.*?)\*/', re.DOTALL)
FIRST_SENT_RE = re.compile(r'([^.\n]{5,200}\.)')

def role_of(body, cls):
    for m in JAVADOC_RE.finditer(body[:8000]):
        block = m.group(1)
        lines = [re.sub(r'^\s*\*\s?','',l).strip() for l in block.splitlines()]
        joined = ' '.join(l for l in lines if l and not l.startswith('@'))
        sm = FIRST_SENT_RE.search(joined)
        if sm:
            s = sm.group(1).strip()
            if len(s) > 120: s = s[:117].rstrip() + '...'
            return s
    return f'inferred: {cls} — role not documented.'

rows = []
with open(RAW) as f:
    for line in f:
        line = line.rstrip('\n')
        if not line: continue
        fqcn, path, loc_total, loc_code_r = line.split('\t')
        loc_code = int(loc_code_r.rstrip('~'))
        try:
            body = Path(path).read_text(errors='replace')
        except Exception:
            body = ''
        comp, feat = classify(path)
        kind = kind_of(fqcn, path, body)
        lw = logic_weight_of(kind, loc_code)
        conc = concurrency_of(body)
        pers = persistence_of(body)
        role = role_of(body, fqcn.rsplit('.',1)[-1])
        if kind in ('data','dto','exception'): diff, mins = 1, 10
        elif kind in ('config','rpc-stub','util','factory','metrics','cli','interface'): diff, mins = 2, 20
        elif kind in ('abstract','service','coordinator','algorithm'):
            if loc_code < 150: diff, mins = 3, 30
            elif loc_code < 400: diff, mins = 4, 45
            else: diff, mins = 5, 60
        elif kind == 'state-machine': diff, mins = 5, 90
        else: diff, mins = 3, 30
        rows.append({
            'fqcn': fqcn, 'path': path, 'loc_total': int(loc_total), 'loc_code': f'{loc_code}~',
            'component': comp, 'feature': feat, 'kind': kind, 'logic_weight': lw,
            'concurrency': conc, 'persistence': pers, 'role_one_liner': role,
            'key_collaborators': [], 'entry_points': [], 'invariants': [],
            'test_exemplar': '', 'difficulty': diff, 'study_minutes': mins,
            'prereq_fqcns': [], 'read_order_hint': 0, 'sharp_edges': [],
        })

with open(OUT_JSONL, 'w') as f:
    for r in rows: f.write(json.dumps(r) + '\n')

c = Counter((r['component'], r['feature']) for r in rows)
with open(OUT_FEATURES, 'w') as f:
    for (comp, feat), n in sorted(c.items(), key=lambda x: (x[0][0], -x[1])):
        f.write(f'{comp}\t{feat}\t{n}\n')

by_group = {}
for r in rows: by_group.setdefault((r['component'], r['feature']), []).append(r)
anchors = []
seen_a = set()
for grp, items in by_group.items():
    items.sort(key=lambda x: -int(x['loc_code'].rstrip('~')))
    cutoff = max(1, len(items) // 5)
    for r in items[:cutoff]:
        if int(r['loc_code'].rstrip('~')) >= 200 or r['kind'] == 'state-machine':
            if r['fqcn'] not in seen_a:
                anchors.append(r); seen_a.add(r['fqcn'])
    for r in items:
        if r['kind'] == 'state-machine' and r['fqcn'] not in seen_a:
            anchors.append(r); seen_a.add(r['fqcn'])
with open(OUT_ANCHORS, 'w') as f:
    for r in anchors:
        f.write(f'{r["component"]}\t{r["feature"]}\t{r["fqcn"]}\t{r["path"]}\t{r["loc_code"]}\n')

print(f'rows: {len(rows)}')
print(f'features: {len(c)}')
print(f'anchors: {len(anchors)}')
misc = [k for k in c if k[1]=='misc']
print(f'misc groups: {misc}')
