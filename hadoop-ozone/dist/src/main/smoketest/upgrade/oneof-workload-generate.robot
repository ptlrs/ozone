# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

*** Settings ***
Documentation       Exercise OMRequest payload fields that moved into the `oneof body` group as
...                 well as the fields that stay flat outside it, so their bytes appear on
...                 the wire and in the OM Ratis log during upgrade / downgrade replay.
...                 Runs alongside upgrade/generate.robot;
...                 both are invoked once per phase with the same PREFIX so this suite can rely on
...                 the volume/bucket/key that generate.robot created for the same PREFIX.
Library             OperatingSystem
Library             BuiltIn
Resource            ../commonlib.robot
Suite Setup         Get Security Enabled From Config
Test Setup          Run Keyword if    '${SECURITY_ENABLED}' == 'true'    Kinit test user     testuser     testuser.keytab
Test Timeout        5 minutes

*** Variables ***

*** Keywords ***
Multitenancy enabled
    ${rc}    ${value} =    Run And Return Rc And Output    ozone getconf confKey ozone.om.multitenancy.enabled
    RETURN    ${value}

*** Test Cases ***
Create an FSO bucket and touch a file in it
    # OMRequest.CreateFileRequest = 72 and OMRequest.CreateDirectoryRequest = 71
    # both moved into the `oneof body` group. `ozone fs -touch` on an
    # FILE_SYSTEM_OPTIMIZED bucket routes through both.
    ${output} =    Execute And Ignore Error    ozone sh volume create /${PREFIX}-fso-vol
                   Log    ${output}
    ${output} =    Execute And Ignore Error    ozone sh bucket create /${PREFIX}-fso-vol/${PREFIX}-fso-bkt --layout FILE_SYSTEM_OPTIMIZED
                   Log    ${output}
                   Execute    ozone fs -mkdir -p ofs://omservice/${PREFIX}-fso-vol/${PREFIX}-fso-bkt/${PREFIX}-dir
                   Execute    ozone fs -touch ofs://omservice/${PREFIX}-fso-vol/${PREFIX}-fso-bkt/${PREFIX}-dir/touched-${PREFIX}.txt

AddAcl on the FSO file created above
    # OMRequest.AddAclRequest = 75, moved into `oneof body`. Using a file this
    # suite created itself avoids having the ACL wiped by generate.robot's
    # `key put` rewriting the shared old1 key in a later phase.
    ${output} =    Execute And Ignore Error    ozone sh key addacl /${PREFIX}-fso-vol/${PREFIX}-fso-bkt/${PREFIX}-dir/touched-${PREFIX}.txt -a user:testuser2:rwxy
                   Log    ${output}

Set quota on the FSO volume
    # OMRequest.SetVolumePropertyRequest = 12, moved into `oneof body`. The
    # matching read is `Volume quota survives replay` in oneof-workload-validate.
                   Execute    ozone sh volume setquota /${PREFIX}-fso-vol --space-quota 10TB --namespace-quota 100

Set quota on the FSO bucket
    # OMRequest.SetBucketPropertyRequest = 23, moved into `oneof body`.
                   Execute    ozone sh bucket setquota /${PREFIX}-fso-vol/${PREFIX}-fso-bkt --space-quota 1TB --namespace-quota 1000

Rename a scratch key in the FSO bucket
    # OMRequest.RenameKeyRequest = 33, moved into `oneof body`. The scratch key
    # is created and renamed within this phase's PREFIX namespace so nothing
    # generate.robot depends on is affected.
                   Execute and checkrc    echo "${PREFIX}: scratch key for rename" > /tmp/rename-src-${PREFIX}    0
                   Execute    ozone sh key put /${PREFIX}-fso-vol/${PREFIX}-fso-bkt/rename-src-${PREFIX} /tmp/rename-src-${PREFIX}
                   Execute    ozone sh key rename /${PREFIX}-fso-vol/${PREFIX}-fso-bkt rename-src-${PREFIX} rename-dst-${PREFIX}
                   Execute and checkrc    rm /tmp/rename-src-${PREFIX}    0

Delete a scratch key in the FSO bucket
    # OMRequest.DeleteKeyRequest = 34, moved into `oneof body`. The scratch key
    # is created and deleted within this phase's PREFIX namespace.
                   Execute and checkrc    echo "${PREFIX}: scratch key for delete" > /tmp/delete-src-${PREFIX}    0
                   Execute    ozone sh key put /${PREFIX}-fso-vol/${PREFIX}-fso-bkt/delete-src-${PREFIX} /tmp/delete-src-${PREFIX}
                   Execute    ozone sh key delete /${PREFIX}-fso-vol/${PREFIX}-fso-bkt/delete-src-${PREFIX}
                   Execute and checkrc    rm /tmp/delete-src-${PREFIX}    0

GetS3Secret exercises the flat-optional pair 49 + 82
    # preExecute of S3GetSecretRequest sets BOTH
    # OMRequest.getS3SecretRequest (49) AND OMRequest.updateGetS3SecretRequest (82)
    # on the same builder. Both fields stay flat outside the `oneof`.
    Pass Execution If    '${SECURITY_ENABLED}' != 'true'    Skip in unsecure cluster (49+82 pair requires kerberos)
                        Execute And Ignore Error    ozone s3 revokesecret -y -u testuser
    ${output} =         Execute    ozone s3 getsecret -u testuser
                        Should contain    ${output}    awsAccessKey
                        Should contain    ${output}    awsSecret

Create tenant exercises the flat-optional pair 11 + 96
    # OMTenantCreateRequest builds an OMRequest with BOTH
    # createVolumeRequest (11) AND CreateTenantRequest (96) set on the same
    # builder. Both fields stay flat outside the `oneof`.
    Pass Execution If    '${SECURITY_ENABLED}' != 'true'    Skip in unsecure cluster (tenant CLI requires kerberos)
    ${mt} =              Multitenancy enabled
    Pass Execution If    '${mt}' != 'true'    Skip when ozone.om.multitenancy.enabled is not true
    ${rc}  ${listed} =   Run And Return Rc And Output    ozone tenant list
    ${exists} =          Evaluate    '${PREFIX}-tenant' in '''${listed}'''
    IF    not ${exists}
        ${output} =      Execute    ozone tenant --verbose create ${PREFIX}-tenant
                         Should contain    ${output}    "tenantId" : "${PREFIX}-tenant"
    END

Assign tenant user exercises the flat-optional pair 82 + 100
    # OMTenantAssignUserAccessIdRequest preExecute sets BOTH
    # OMRequest.updateGetS3SecretRequest (82) AND
    # OMRequest.TenantAssignUserAccessIdRequest (100). Both stay flat.
    # Each phase uses a distinct PREFIX, so each phase creates a fresh tenant
    # and assigns testuser to it — the "already assigned" reject path never
    # fires under normal driver execution.
    Pass Execution If    '${SECURITY_ENABLED}' != 'true'    Skip in unsecure cluster
    ${mt} =              Multitenancy enabled
    Pass Execution If    '${mt}' != 'true'    Skip when ozone.om.multitenancy.enabled is not true
    ${rc}  ${output} =   Run And Return Rc And Output    ozone tenant --verbose user assign testuser --tenant=${PREFIX}-tenant
                         Log    ${output}
                         Should Contain Any    ${output}    Assigned 'testuser' to '${PREFIX}-tenant'    is already assigned to tenant
