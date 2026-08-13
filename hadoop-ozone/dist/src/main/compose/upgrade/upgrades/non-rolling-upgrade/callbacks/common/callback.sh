#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

source "$TEST_DIR"/testlib.sh

### HELPER METHODS ###

## @description Generates data on the cluster.
## @param The prefix to use for data generated.
## @param All parameters after the first one are passed directly to the robot command,
##        see https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#all-command-line-options
generate() {
  execute_robot_test "$SCM" -N "${OUTPUT_NAME}-generate-${1}" -v PREFIX:"$1" ${@:2} upgrade/generate.robot
}

## @description Validates that data exists on the cluster.
## @param The prefix of the data to be validated.
## @param All parameters after the first one are passed directly to the robot command,
##        see https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#all-command-line-options
validate() {
  execute_robot_test "$SCM" -N "${OUTPUT_NAME}-validate-${1}" -v PREFIX:"$1" ${@:2} upgrade/validate.robot
}

## @description Exercises the OMRequest / OMResponse payloads that moved into the
## `oneof body` group as well as the fields that stay flat outside it, so
## their bytes appear on the wire and in the OM Ratis log during each phase.
## @param The prefix to use for data generated.
oneof_generate() {
  execute_robot_test "$SCM" -N "${OUTPUT_NAME}-oneof-generate-${1}" -v PREFIX:"$1" upgrade/oneof-workload-generate.robot
}

## @description Reads back state written by `oneof_generate` in an earlier phase and asserts
## it survived Ratis log replay under the current binary.
## @param The prefix of the data to be validated.
oneof_validate() {
  execute_robot_test "$SCM" -N "${OUTPUT_NAME}-oneof-validate-${1}" -v PREFIX:"$1" upgrade/oneof-workload-validate.robot
}

## @description Exercises the SCM admin RPCs whose payloads moved into the `oneof body` group
## of ScmContainerLocationRequest / Response so their bytes cross the wire in both directions.
oneof_admin() {
  execute_robot_test "$SCM" -N "${OUTPUT_NAME}-oneof-admin" upgrade/oneof-admin-rpcs.robot
}

### CALLBACKS ###

with_old_version() {
  execute_robot_test "$SCM" -N "${OUTPUT_NAME}-check-finalization" --include finalized upgrade/check-finalization.robot
  generate old1
  validate old1
  oneof_generate old1
  oneof_admin
}

with_this_version_pre_finalized() {
  # No check for pre-finalized status here, because the release may not have
  # added layout features to OM or HDDS.
  validate old1
  # end-to-end: the new binary replays the segment files the old binary
  # wrote for the `oneof`-migrated arms and the flat-optional pairs.
  oneof_validate old1
  oneof_admin
  # HDDS-6261: overwrite the same keys intentionally
  generate old1 --exclude create-volume-and-bucket

  generate new1
  validate new1
  oneof_generate new1
}

with_old_version_downgraded() {
  execute_robot_test "$SCM" -N "${OUTPUT_NAME}-check-finalization" --include finalized upgrade/check-finalization.robot
  validate old1
  validate new1
  # end-to-end: the old binary replays the segment files the new binary
  # wrote (for `new1` above) and applies them via its own state machine.
  oneof_validate old1
  oneof_validate new1

  generate old2
  validate old2
  oneof_generate old2
  oneof_admin

  # HDDS-6261: overwrite the same keys again to trigger the precondition check
  # that exists <= 1.1.0 OM
  generate old1 --exclude create-volume-and-bucket
}

with_this_version_finalized() {
  execute_robot_test "$SCM" -N "${OUTPUT_NAME}-check-finalization" --include finalized upgrade/check-finalization.robot
  validate old1
  validate new1
  validate old2
  # Every earlier phase's `oneof`-migrated payloads must still round-trip
  # through both replay directions after finalization.
  oneof_validate old1
  oneof_validate new1
  oneof_validate old2
  oneof_admin

  generate new2
  validate new2
  oneof_generate new2
  oneof_validate new2
}
