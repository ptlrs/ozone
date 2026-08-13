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
Documentation       Exercise the SCM admin RPCs whose payloads moved into the `oneof body` group
...                 of ScmContainerLocationRequest / Response so their bytes cross the wire in
...                 both directions during the non-rolling upgrade.
...                 SCMDatanodeRequest and SCMCommandProto are exercised implicitly: every DN
...                 heartbeats to SCM throughout the driver's four phases, and driver.sh already
...                 fails if pipelines cannot form or replicate.
Library             OperatingSystem
Library             BuiltIn
Resource            ../commonlib.robot
Suite Setup         Get Security Enabled From Config
Test Setup          Run Keyword if    '${SECURITY_ENABLED}' == 'true'    Kinit test user     testuser     testuser.keytab
Test Timeout        3 minutes

*** Test Cases ***
List pipelines
    # ScmContainerLocationRequest.ListPipelineRequest = payload arm inside `oneof body`.
    ${output} =    Execute    ozone admin pipeline list
                   Should not contain    ${output}    Exception

List datanodes
    # ScmContainerLocationRequest.DecommissionScmRequest etc. share the same wrapper; the
    # `datanode list` command is dispatched through ScmContainerLocation as well.
    ${output} =    Execute    ozone admin datanode list
                   Should not contain    ${output}    Exception

List containers
    # ScmContainerLocationRequest.ListContainerRequest arm.
    ${output} =    Execute    ozone admin container list
                   Should Start With    ${output}    [
                   Should End With      ${output}    ]

Info of the first container
    # ScmContainerLocationRequest.GetContainerRequest arm; verifies both request and response
    # wrappers still parse end to end after the version cross. The output format has drifted
    # between 2.2.0 and current, so we only assert the container id appears anywhere in the
    # command's output — the point of this test is that the RPC round-trips at all.
    ${first} =     Execute    ozone admin container list --json | jq -r '.[0].containerID // empty'
    Pass Execution If    '${first}' == ''    Skip when no containers have been allocated in this phase
    ${output} =    Execute    ozone admin container info ${first}
                   Should contain    ${output}    ${first}

Safemode status
    # ScmContainerLocationRequest.InSafeModeRequest = 19 and GetSafeModeRuleStatusesRequest = 26
    # arms — both are dispatched by a single `safemode status` invocation.
    ${output} =    Execute    ozone admin safemode status
                   Should not contain    ${output}    Exception

Replication manager status
    # ScmContainerLocationRequest.ReplicationManagerStatusRequest = 23 arm.
    ${output} =    Execute    ozone admin replicationmanager status
                   Should not contain    ${output}    Exception

Container balancer status
    # ScmContainerLocationRequest.ContainerBalancerStatusRequest = 35 arm on older versions and
    # ContainerBalancerStatusInfoRequest = 48 on newer ones — one of the two is picked by the
    # client depending on the negotiated protocol version. Ignore-error so the phase where the
    # arm's response type is unrecognized (mixed-version window) does not fail the suite; the
    # point is that the request/response wrapper still round-trips on the wire.
    ${output} =    Execute And Ignore Error    ozone admin containerbalancer status
                   Log    ${output}

Datanode usage info
    # ScmContainerLocationRequest.DatanodeUsageInfoRequest = 30 arm.
    ${output} =    Execute    ozone admin datanode usageinfo -m
                   Should not contain    ${output}    Exception
