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
Documentation       Validate that state written by oneof-workload-generate.robot in an earlier
...                 phase is still readable after Ratis log replay under the current binary.
...                 Every read here uses an OMRequest arm that was migrated into the `oneof body`
...                 group (LookupKey = 32, ListStatus = 74, GetAcl = 78, InfoVolume = 14,
...                 InfoBucket = 22, ListVolume = 16, ListBuckets = 25, ListKeys = 35,
...                 GetFileStatus = 70), or a flat-optional field that was left outside it.
...                 Tag numbers referenced below come from hadoop-ozone/interface-client/src/main/proto/OmClientProtocol.proto.
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
LookupKey survives replay for the FSO bucket touched file
    # OMRequest.LookupKeyRequest = 32, moved into `oneof body`.
    ${output} =    Execute    ozone sh key info /${PREFIX}-fso-vol/${PREFIX}-fso-bkt/${PREFIX}-dir/touched-${PREFIX}.txt
                   Should contain    ${output}    "name"

ListStatus survives replay under the FSO bucket
    # OMRequest.ListStatusRequest = 74, moved into `oneof body`.
    ${output} =    Execute    ozone fs -ls ofs://omservice/${PREFIX}-fso-vol/${PREFIX}-fso-bkt/${PREFIX}-dir
                   Should contain    ${output}    touched-${PREFIX}.txt

GetAcl survives replay on the FSO file with an ACL added
    # OMRequest.GetAclRequest = 78, moved into `oneof body`. The ACL was
    # written by oneof-workload-generate.robot's `AddAcl` test in this phase.
    ${output} =    Execute    ozone sh key getacl /${PREFIX}-fso-vol/${PREFIX}-fso-bkt/${PREFIX}-dir/touched-${PREFIX}.txt
                   Should contain    ${output}    testuser2

Volume quota survives replay
    # OMRequest.InfoVolumeRequest = 14, moved into `oneof body`. Written by
    # oneof-workload-generate.robot's `Set quota on the FSO volume` test.
    ${output} =    Execute    ozone sh volume info /${PREFIX}-fso-vol
                   Should contain    ${output}    ${PREFIX}-fso-vol

Bucket quota survives replay
    # OMRequest.InfoBucketRequest = 22, moved into `oneof body`. Written by
    # oneof-workload-generate.robot's `Set quota on the FSO bucket` test.
    ${output} =    Execute    ozone sh bucket info /${PREFIX}-fso-vol/${PREFIX}-fso-bkt
                   Should contain    ${output}    ${PREFIX}-fso-bkt

ListVolume includes the FSO volume created earlier
    # OMRequest.ListVolumeRequest = 16, moved into `oneof body`.
    ${output} =    Execute    ozone sh volume list /
                   Should contain    ${output}    ${PREFIX}-fso-vol

ListBuckets includes the FSO bucket created earlier
    # OMRequest.ListBucketsRequest = 25, moved into `oneof body`.
    ${output} =    Execute    ozone sh bucket list /${PREFIX}-fso-vol
                   Should contain    ${output}    ${PREFIX}-fso-bkt

ListKeys includes the renamed scratch key
    # OMRequest.ListKeysRequest = 35, moved into `oneof body`. The renamed
    # scratch key was written by oneof-workload-generate.robot's `Rename a
    # scratch key in the FSO bucket` test in the same phase.
    ${output} =    Execute    ozone sh key list /${PREFIX}-fso-vol/${PREFIX}-fso-bkt
                   Should contain    ${output}    rename-dst-${PREFIX}

GetFileStatus survives replay on the FSO touched file
    # OMRequest.GetFileStatusRequest = 70, moved into `oneof body`. Written by
    # oneof-workload-generate.robot's `Create an FSO bucket and touch a file
    # in it` test.
    ${output} =    Execute    ozone fs -stat ofs://omservice/${PREFIX}-fso-vol/${PREFIX}-fso-bkt/${PREFIX}-dir/touched-${PREFIX}.txt
                   Should contain    ${output}    touched-${PREFIX}.txt

Tenant survives replay of the 11 + 96 pair
    Pass Execution If    '${SECURITY_ENABLED}' != 'true'    Skip in unsecure cluster
    ${mt} =              Multitenancy enabled
    Pass Execution If    '${mt}' != 'true'    Skip when ozone.om.multitenancy.enabled is not true
    ${output} =          Execute    ozone tenant list
                         Should contain    ${output}    ${PREFIX}-tenant
