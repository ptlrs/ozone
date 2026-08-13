/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.ozone.om.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import org.apache.hadoop.hdds.protocol.OneofWireCompatUtil;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.OMRequest;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.OMResponse;
import org.apache.hadoop.ozone.protocol.proto.testing.Proto2OmClientProtocolForOneofMigrationTesting;
import org.junit.jupiter.api.Test;

/**
 * Wire-compatibility tests for the `oneof` migration of `OMRequest` and
 * `OMResponse` (see
 * `hadoop-ozone/interface-client/src/main/proto/OmClientProtocol.proto`).
 *
 * <p>The fixture proto
 * `Proto2OmClientProtocolForOneofMigrationTesting.proto` is a verbatim copy
 * of upstream/master's `OmClientProtocol.proto` before the `oneof`
 * migration; only the outer container names (java_package,
 * java_outer_classname, protobuf package) differ so the generated class
 * does not collide with `OzoneManagerProtocolProtos`.
 *
 * <p>Because the fixture declares every payload arm as a real typed
 * sub-message (identical to production's pre-migration schema), the
 * fixture and production sub-message types are the same shape — the
 * bridge is just `parseFrom(byte[])`/`toByteArray()` at the wrapper
 * level.
 */
class TestOmClientProtocolOneofCompatibility {

  // ---------------------------------------------------------------------
  // OMRequest — fixture → production
  // ---------------------------------------------------------------------

  /**
   * Prove a fixture-encoded OMRequest is byte-identical to what the
   * production schema would emit for the same field values. This
   * covers the seven fields excluded from the `body` oneof (see §7 of
   * UPGRADE_PROTO_ONEOF.md), including {@code readConsistencyHint} at
   * tag 7 which is a message (not an int/enum).
   */
  @Test
  void fixtureRequestParsesUnderProductionSchema_flatExcludedFields() throws Exception {
    Proto2OmClientProtocolForOneofMigrationTesting.OMRequest fixture =
        Proto2OmClientProtocolForOneofMigrationTesting.OMRequest.newBuilder()
            .setCmdType(Proto2OmClientProtocolForOneofMigrationTesting.Type.CreateVolume)
            .setTraceID("trace-1")
            .setClientId("client-1")
            .setUserInfo(fixtureUserInfoSub())
            .setVersion(1)
            .setLayoutVersion(fixtureLayoutVersionSub())
            .setReadConsistencyHint(fixtureReadConsistencyHintSub())            // 7
            .setCreateVolumeRequest(fixtureCreateVolumeRequestSub())            // 11
            .setGetS3SecretRequest(fixtureGetS3SecretRequestSub())              // 49
            .setUpdateGetS3SecretRequest(fixtureUpdateGetS3SecretRequestSub())  // 82
            .setS3Authentication(fixtureS3AuthenticationSub())                  // 95
            .setCreateTenantRequest(fixtureCreateTenantRequestSub())            // 96
            .setTenantAssignUserAccessIdRequest(fixtureTenantAssignUserAccessIdSub()) // 100
            .addSetSnapshotPropertyRequests(fixtureSetSnapshotPropertyRequestSub("k1")) // 143 (repeated)
            .addSetSnapshotPropertyRequests(fixtureSetSnapshotPropertyRequestSub("k2"))
            .build();
    byte[] onWire = fixture.toByteArray();

    OMRequest prod = OMRequest.parseFrom(onWire);
    assertEquals(OzoneManagerProtocolProtos.Type.CreateVolume, prod.getCmdType());
    assertEquals("trace-1", prod.getTraceID());
    assertEquals("client-1", prod.getClientId());
    assertArrayEquals(fixture.getUserInfo().toByteArray(), prod.getUserInfo().toByteArray());
    assertEquals(1, prod.getVersion());
    assertArrayEquals(fixture.getLayoutVersion().toByteArray(),
        prod.getLayoutVersion().toByteArray());

    // readConsistencyHint at tag 7 is a message with a nested oneof; wire
    // bytes must survive round-trip.
    assertTrue(prod.hasReadConsistencyHint());
    assertArrayEquals(fixture.getReadConsistencyHint().toByteArray(),
        prod.getReadConsistencyHint().toByteArray());

    // Every one of the seven excluded fields survives at its original tag.
    assertTrue(prod.hasCreateVolumeRequest());
    assertArrayEquals(fixture.getCreateVolumeRequest().toByteArray(),
        prod.getCreateVolumeRequest().toByteArray());
    assertTrue(prod.hasGetS3SecretRequest());
    assertArrayEquals(fixture.getGetS3SecretRequest().toByteArray(),
        prod.getGetS3SecretRequest().toByteArray());
    assertTrue(prod.hasUpdateGetS3SecretRequest());
    assertArrayEquals(fixture.getUpdateGetS3SecretRequest().toByteArray(),
        prod.getUpdateGetS3SecretRequest().toByteArray());
    assertTrue(prod.hasS3Authentication());
    assertArrayEquals(fixture.getS3Authentication().toByteArray(),
        prod.getS3Authentication().toByteArray());
    assertTrue(prod.hasCreateTenantRequest());
    assertArrayEquals(fixture.getCreateTenantRequest().toByteArray(),
        prod.getCreateTenantRequest().toByteArray());
    assertTrue(prod.hasTenantAssignUserAccessIdRequest());
    assertArrayEquals(fixture.getTenantAssignUserAccessIdRequest().toByteArray(),
        prod.getTenantAssignUserAccessIdRequest().toByteArray());
    // The repeated field: both entries must arrive in order.
    assertEquals(2, prod.getSetSnapshotPropertyRequestsCount());
    assertArrayEquals(fixture.getSetSnapshotPropertyRequests(0).toByteArray(),
        prod.getSetSnapshotPropertyRequests(0).toByteArray());
    assertArrayEquals(fixture.getSetSnapshotPropertyRequests(1).toByteArray(),
        prod.getSetSnapshotPropertyRequests(1).toByteArray());

    assertArrayEquals(onWire, prod.toByteArray());
  }

  @Test
  void fixtureRequestParsesUnderProductionSchema_oneofArms() throws Exception {
    assertFixtureRequestArmParses(12);
    assertFixtureRequestArmParses(31);
    assertFixtureRequestArmParses(110);
    assertFixtureRequestArmParses(112);
  }

  private static void assertFixtureRequestArmParses(int tag) throws Exception {
    Proto2OmClientProtocolForOneofMigrationTesting.OMRequest.Builder fb =
        Proto2OmClientProtocolForOneofMigrationTesting.OMRequest.newBuilder()
            .setCmdType(fixtureRequestType(tag))
            .setClientId("c-" + tag);
    switch (tag) {
      case 12:
        fb.setSetVolumePropertyRequest(fixtureSetVolumePropertyRequestSub());
        break;
      case 31:
        fb.setCreateKeyRequest(fixtureCreateKeyRequestSub());
        break;
      case 110:
        fb.setEchoRPCRequest(fixtureEchoRPCRequestSub());
        break;
      case 112:
        fb.setCreateSnapshotRequest(fixtureCreateSnapshotRequestSub());
        break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
    Proto2OmClientProtocolForOneofMigrationTesting.OMRequest fixture = fb.build();
    byte[] onWire = fixture.toByteArray();
    OMRequest prod = OMRequest.parseFrom(onWire);
    assertEquals(prodRequestType(tag), prod.getCmdType());
    assertEquals("c-" + tag, prod.getClientId());
    switch (tag) {
      case 12:
        assertTrue(prod.hasSetVolumePropertyRequest());
        assertArrayEquals(fixture.getSetVolumePropertyRequest().toByteArray(),
            prod.getSetVolumePropertyRequest().toByteArray());
        break;
      case 31:
        assertTrue(prod.hasCreateKeyRequest());
        assertArrayEquals(fixture.getCreateKeyRequest().toByteArray(),
            prod.getCreateKeyRequest().toByteArray());
        break;
      case 110:
        assertTrue(prod.hasEchoRPCRequest());
        assertArrayEquals(fixture.getEchoRPCRequest().toByteArray(),
            prod.getEchoRPCRequest().toByteArray());
        break;
      case 112:
        assertTrue(prod.hasCreateSnapshotRequest());
        assertArrayEquals(fixture.getCreateSnapshotRequest().toByteArray(),
            prod.getCreateSnapshotRequest().toByteArray());
        break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
    assertArrayEquals(onWire, prod.toByteArray());
  }

  // ---------------------------------------------------------------------
  // OMRequest — production → fixture
  // ---------------------------------------------------------------------

  @Test
  void productionRequestParsesUnderFixtureSchema_flatExcludedFields() throws Exception {
    OMRequest prod = OMRequest.newBuilder()
        .setCmdType(OzoneManagerProtocolProtos.Type.CreateVolume)
        .setClientId("client-1")
        .setReadConsistencyHint(prodReadConsistencyHintSub())
        .setCreateVolumeRequest(prodCreateVolumeRequestSub())
        .setGetS3SecretRequest(prodGetS3SecretRequestSub())
        .setUpdateGetS3SecretRequest(prodUpdateGetS3SecretRequestSub())
        .setS3Authentication(prodS3AuthenticationSub())
        .setCreateTenantRequest(prodCreateTenantRequestSub())
        .setTenantAssignUserAccessIdRequest(prodTenantAssignUserAccessIdSub())
        .addSetSnapshotPropertyRequests(prodSetSnapshotPropertyRequestSub("k1"))
        .addSetSnapshotPropertyRequests(prodSetSnapshotPropertyRequestSub("k2"))
        .build();

    byte[] onWire = prod.toByteArray();
    Proto2OmClientProtocolForOneofMigrationTesting.OMRequest fixture =
        Proto2OmClientProtocolForOneofMigrationTesting.OMRequest.parseFrom(onWire);

    assertEquals(prod.getCmdType().getNumber(), fixture.getCmdType().getNumber());
    assertEquals("client-1", fixture.getClientId());
    assertTrue(fixture.hasReadConsistencyHint());
    assertArrayEquals(prod.getReadConsistencyHint().toByteArray(),
        fixture.getReadConsistencyHint().toByteArray());
    assertTrue(fixture.hasCreateVolumeRequest());
    assertArrayEquals(prod.getCreateVolumeRequest().toByteArray(),
        fixture.getCreateVolumeRequest().toByteArray());
    assertTrue(fixture.hasGetS3SecretRequest());
    assertTrue(fixture.hasUpdateGetS3SecretRequest());
    assertTrue(fixture.hasS3Authentication());
    assertTrue(fixture.hasCreateTenantRequest());
    assertTrue(fixture.hasTenantAssignUserAccessIdRequest());
    assertEquals(2, fixture.getSetSnapshotPropertyRequestsCount());
    assertArrayEquals(prod.getSetSnapshotPropertyRequests(0).toByteArray(),
        fixture.getSetSnapshotPropertyRequests(0).toByteArray());
    assertArrayEquals(prod.getSetSnapshotPropertyRequests(1).toByteArray(),
        fixture.getSetSnapshotPropertyRequests(1).toByteArray());

    assertArrayEquals(onWire, fixture.toByteArray());
  }

  @Test
  void productionRequestParsesUnderFixtureSchema_oneofArms() throws Exception {
    assertProductionRequestArmToFixture(12,
        b -> b.setSetVolumePropertyRequest(prodSetVolumePropertyRequestSub()));
    assertProductionRequestArmToFixture(31,
        b -> b.setCreateKeyRequest(prodCreateKeyRequestSub()));
    assertProductionRequestArmToFixture(110,
        b -> b.setEchoRPCRequest(prodEchoRPCRequestSub()));
    assertProductionRequestArmToFixture(112,
        b -> b.setCreateSnapshotRequest(prodCreateSnapshotRequestSub()));
  }

  @FunctionalInterface
  private interface RequestArmSetter {
    OMRequest.Builder set(OMRequest.Builder b);
  }

  private static void assertProductionRequestArmToFixture(int tag, RequestArmSetter setter)
      throws Exception {
    OMRequest.Builder b = OMRequest.newBuilder()
        .setCmdType(OzoneManagerProtocolProtos.Type.EchoRPC)
        .setClientId("c-" + tag);
    setter.set(b);
    OMRequest prod = b.build();
    byte[] onWire = prod.toByteArray();
    Proto2OmClientProtocolForOneofMigrationTesting.OMRequest fixture =
        Proto2OmClientProtocolForOneofMigrationTesting.OMRequest.parseFrom(onWire);
    assertEquals(prod.getCmdType().getNumber(), fixture.getCmdType().getNumber());
    assertEquals("c-" + tag, fixture.getClientId());
    switch (tag) {
      case 12:
        assertTrue(fixture.hasSetVolumePropertyRequest());
        assertArrayEquals(prod.getSetVolumePropertyRequest().toByteArray(),
            fixture.getSetVolumePropertyRequest().toByteArray());
        break;
      case 31:
        assertTrue(fixture.hasCreateKeyRequest());
        assertArrayEquals(prod.getCreateKeyRequest().toByteArray(),
            fixture.getCreateKeyRequest().toByteArray());
        break;
      case 110:
        assertTrue(fixture.hasEchoRPCRequest());
        assertArrayEquals(prod.getEchoRPCRequest().toByteArray(),
            fixture.getEchoRPCRequest().toByteArray());
        break;
      case 112:
        assertTrue(fixture.hasCreateSnapshotRequest());
        assertArrayEquals(prod.getCreateSnapshotRequest().toByteArray(),
            fixture.getCreateSnapshotRequest().toByteArray());
        break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
    assertArrayEquals(onWire, fixture.toByteArray());
  }

  // ---------------------------------------------------------------------
  // OMResponse
  // ---------------------------------------------------------------------

  @Test
  void fixtureResponseParsesUnderProductionSchema() throws Exception {
    Proto2OmClientProtocolForOneofMigrationTesting.EchoRPCResponse echoArm =
        Proto2OmClientProtocolForOneofMigrationTesting.EchoRPCResponse.newBuilder()
            .setPayload(ByteString.copyFromUtf8("pong")).build();
    Proto2OmClientProtocolForOneofMigrationTesting.OMLockDetailsProto lockDetails =
        Proto2OmClientProtocolForOneofMigrationTesting.OMLockDetailsProto.newBuilder()
            .setIsLockAcquired(true).setReadLockNanos(1234L).build();

    Proto2OmClientProtocolForOneofMigrationTesting.OMResponse fixture =
        Proto2OmClientProtocolForOneofMigrationTesting.OMResponse.newBuilder()
            .setCmdType(Proto2OmClientProtocolForOneofMigrationTesting.Type.EchoRPC)
            .setTraceID("t-r")
            .setSuccess(true)
            .setMessage("ok")
            .setStatus(Proto2OmClientProtocolForOneofMigrationTesting.Status.OK)
            .setLeaderOMNodeId("om-1")
            .setEchoRPCResponse(echoArm)   // 110 (oneof)
            .setOmLockDetails(lockDetails) // 131 (flat cross-cutting)
            .build();

    OMResponse prod = OMResponse.parseFrom(fixture.toByteArray());
    assertEquals(OzoneManagerProtocolProtos.Type.EchoRPC, prod.getCmdType());
    assertEquals("t-r", prod.getTraceID());
    assertTrue(prod.getSuccess());
    assertEquals("ok", prod.getMessage());
    assertEquals(OzoneManagerProtocolProtos.Status.OK, prod.getStatus());
    assertEquals("om-1", prod.getLeaderOMNodeId());
    assertTrue(prod.hasEchoRPCResponse());
    assertArrayEquals(echoArm.toByteArray(), prod.getEchoRPCResponse().toByteArray());
    assertTrue(prod.hasOmLockDetails());
    assertArrayEquals(lockDetails.toByteArray(), prod.getOmLockDetails().toByteArray());
    // Confirm other oneof arms not surprisingly present.
    assertFalse(prod.hasCreateVolumeResponse());
    assertArrayEquals(fixture.toByteArray(), prod.toByteArray());
  }

  @Test
  void productionResponseParsesUnderFixtureSchema() throws Exception {
    OMResponse prod = OMResponse.newBuilder()
        .setCmdType(OzoneManagerProtocolProtos.Type.EchoRPC)
        .setStatus(OzoneManagerProtocolProtos.Status.OK)
        .setLeaderOMNodeId("om-1")
        .setEchoRPCResponse(OzoneManagerProtocolProtos.EchoRPCResponse.newBuilder()
            .setPayload(ByteString.copyFromUtf8("pong")).build())
        .setOmLockDetails(OzoneManagerProtocolProtos.OMLockDetailsProto.newBuilder()
            .setIsLockAcquired(true).setReadLockNanos(1234L).build())
        .build();
    byte[] onWire = prod.toByteArray();
    Proto2OmClientProtocolForOneofMigrationTesting.OMResponse fixture =
        Proto2OmClientProtocolForOneofMigrationTesting.OMResponse.parseFrom(onWire);

    assertEquals(prod.getCmdType().getNumber(), fixture.getCmdType().getNumber());
    assertEquals(prod.getStatus().getNumber(), fixture.getStatus().getNumber());
    assertEquals("om-1", fixture.getLeaderOMNodeId());
    assertTrue(fixture.hasEchoRPCResponse());
    assertArrayEquals(prod.getEchoRPCResponse().toByteArray(),
        fixture.getEchoRPCResponse().toByteArray());
    assertTrue(fixture.hasOmLockDetails());
    assertArrayEquals(prod.getOmLockDetails().toByteArray(),
        fixture.getOmLockDetails().toByteArray());
    assertArrayEquals(onWire, fixture.toByteArray());
  }

  // ---------------------------------------------------------------------
  // Every-arm scans and empty-body cases.
  // ---------------------------------------------------------------------

  /**
   * Every-arm scan for `OMRequest`. Iterates every message-typed
   * optional field in the fixture (which is the pre-migration schema
   * verbatim) and asserts byte-identical round-trip through the
   * production parser. This covers both the flat-excluded fields
   * (see §7 of `UPGRADE_PROTO_ONEOF.md`) and every arm the migration
   * moved into the `body` oneof — so a retagged or dropped arm shows
   * up here at test time, not at customer-cluster time.
   *
   * <p>Only single-instance message fields are scanned; the repeated
   * `SetSnapshotPropertyRequests` field at tag 143 has its
   * count-and-order behavior covered by the direction-specific tests
   * above.
   */
  @Test
  void everyRequestMessageFieldRoundTripsAgainstProductionSchema() throws Exception {
    Descriptor fixtureDesc = Proto2OmClientProtocolForOneofMigrationTesting.OMRequest.getDescriptor();
    // Payload fields start at tag 7 (`readConsistencyHint`); header is 1..6.
    java.util.Set<Integer> arms = OneofWireCompatUtil.messageFieldNumbersInRange(
        fixtureDesc, 7, Integer.MAX_VALUE);
    assertFalse(arms.isEmpty(), "Fixture should declare at least one payload field");
    for (int fieldNumber : arms) {
      byte[] onWire = OneofWireCompatUtil.buildBytesWithMessageField(fixtureDesc, fieldNumber);
      OMRequest prod = OMRequest.parseFrom(onWire);
      assertArrayEquals(onWire, prod.toByteArray(),
          "Wire mismatch for OMRequest field at tag " + fieldNumber);
    }
  }

  /**
   * Every-arm scan for `OMResponse`. Header is 1..6; payload arms
   * start at tag 11.
   */
  @Test
  void everyResponseMessageFieldRoundTripsAgainstProductionSchema() throws Exception {
    Descriptor fixtureDesc = Proto2OmClientProtocolForOneofMigrationTesting.OMResponse.getDescriptor();
    java.util.Set<Integer> arms = OneofWireCompatUtil.messageFieldNumbersInRange(
        fixtureDesc, 7, Integer.MAX_VALUE);
    assertFalse(arms.isEmpty(), "Fixture should declare at least one payload field");
    for (int fieldNumber : arms) {
      byte[] onWire = OneofWireCompatUtil.buildBytesWithMessageField(fixtureDesc, fieldNumber);
      OMResponse prod = OMResponse.parseFrom(onWire);
      assertArrayEquals(onWire, prod.toByteArray(),
          "Wire mismatch for OMResponse field at tag " + fieldNumber);
    }
  }

  @Test
  void emptyBodyRequestRoundTrips() throws Exception {
    Descriptor fixtureDesc = Proto2OmClientProtocolForOneofMigrationTesting.OMRequest.getDescriptor();
    byte[] onWire = OneofWireCompatUtil.buildEmptyBodyBytes(fixtureDesc);
    OMRequest prod = OMRequest.parseFrom(onWire);
    assertArrayEquals(onWire, prod.toByteArray());
  }

  @Test
  void emptyBodyResponseRoundTrips() throws Exception {
    Descriptor fixtureDesc = Proto2OmClientProtocolForOneofMigrationTesting.OMResponse.getDescriptor();
    byte[] onWire = OneofWireCompatUtil.buildEmptyBodyBytes(fixtureDesc);
    OMResponse prod = OMResponse.parseFrom(onWire);
    assertArrayEquals(onWire, prod.toByteArray());
  }

  // ---------------------------------------------------------------------
  // Fixture-side sub-message builders.
  // ---------------------------------------------------------------------

  private static Proto2OmClientProtocolForOneofMigrationTesting.UserInfo fixtureUserInfoSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.UserInfo.newBuilder()
        .setUserName("u").setHostName("h").build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.LayoutVersion fixtureLayoutVersionSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.LayoutVersion.newBuilder()
        .setVersion(1L).build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.ReadConsistencyHint
      fixtureReadConsistencyHintSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.ReadConsistencyHint.newBuilder()
        .setReadConsistency(
            Proto2OmClientProtocolForOneofMigrationTesting.ReadConsistencyProto
                .READ_CONSISTENCY_UNSPECIFIED)
        .build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.CreateVolumeRequest
      fixtureCreateVolumeRequestSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.CreateVolumeRequest.newBuilder()
        .setVolumeInfo(Proto2OmClientProtocolForOneofMigrationTesting.VolumeInfo.newBuilder()
            .setAdminName("a").setOwnerName("o").setVolume("v").build())
        .build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.GetS3SecretRequest
      fixtureGetS3SecretRequestSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.GetS3SecretRequest.newBuilder()
        .setKerberosID("k").build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.UpdateGetS3SecretRequest
      fixtureUpdateGetS3SecretRequestSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.UpdateGetS3SecretRequest.newBuilder()
        .setKerberosID("k").setAwsSecret("s").build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.S3Authentication
      fixtureS3AuthenticationSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.S3Authentication.newBuilder()
        .setStringToSign("sts").setSignature("sig").setAccessId("aid").build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.CreateTenantRequest
      fixtureCreateTenantRequestSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.CreateTenantRequest.newBuilder()
        .setTenantId("t").build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.TenantAssignUserAccessIdRequest
      fixtureTenantAssignUserAccessIdSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.TenantAssignUserAccessIdRequest.newBuilder()
        .setUserPrincipal("u").setTenantId("t").setAccessId("a").build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.SetSnapshotPropertyRequest
      fixtureSetSnapshotPropertyRequestSub(String key) {
    return Proto2OmClientProtocolForOneofMigrationTesting.SetSnapshotPropertyRequest.newBuilder()
        .setSnapshotKey(key).build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.SetVolumePropertyRequest
      fixtureSetVolumePropertyRequestSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.SetVolumePropertyRequest.newBuilder()
        .setVolumeName("v").build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.CreateKeyRequest
      fixtureCreateKeyRequestSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.CreateKeyRequest.newBuilder()
        .setKeyArgs(Proto2OmClientProtocolForOneofMigrationTesting.KeyArgs.newBuilder()
            .setVolumeName("v").setBucketName("b").setKeyName("k").build())
        .build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.EchoRPCRequest
      fixtureEchoRPCRequestSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.EchoRPCRequest.newBuilder()
        .setPayloadReq(ByteString.copyFromUtf8("ping")).build();
  }

  private static Proto2OmClientProtocolForOneofMigrationTesting.CreateSnapshotRequest
      fixtureCreateSnapshotRequestSub() {
    return Proto2OmClientProtocolForOneofMigrationTesting.CreateSnapshotRequest.newBuilder()
        .setVolumeName("v").setBucketName("b").build();
  }

  // ---------------------------------------------------------------------
  // Production-side sub-message builders.
  // ---------------------------------------------------------------------

  private static OzoneManagerProtocolProtos.ReadConsistencyHint prodReadConsistencyHintSub() {
    return OzoneManagerProtocolProtos.ReadConsistencyHint.newBuilder()
        .setReadConsistency(
            OzoneManagerProtocolProtos.ReadConsistencyProto.READ_CONSISTENCY_UNSPECIFIED)
        .build();
  }

  private static OzoneManagerProtocolProtos.CreateVolumeRequest prodCreateVolumeRequestSub() {
    return OzoneManagerProtocolProtos.CreateVolumeRequest.newBuilder()
        .setVolumeInfo(OzoneManagerProtocolProtos.VolumeInfo.newBuilder()
            .setAdminName("a").setOwnerName("o").setVolume("v").build())
        .build();
  }

  private static OzoneManagerProtocolProtos.GetS3SecretRequest prodGetS3SecretRequestSub() {
    return OzoneManagerProtocolProtos.GetS3SecretRequest.newBuilder()
        .setKerberosID("k").build();
  }

  private static OzoneManagerProtocolProtos.UpdateGetS3SecretRequest
      prodUpdateGetS3SecretRequestSub() {
    return OzoneManagerProtocolProtos.UpdateGetS3SecretRequest.newBuilder()
        .setKerberosID("k").setAwsSecret("s").build();
  }

  private static OzoneManagerProtocolProtos.S3Authentication prodS3AuthenticationSub() {
    return OzoneManagerProtocolProtos.S3Authentication.newBuilder()
        .setStringToSign("sts").setSignature("sig").setAccessId("aid").build();
  }

  private static OzoneManagerProtocolProtos.CreateTenantRequest prodCreateTenantRequestSub() {
    return OzoneManagerProtocolProtos.CreateTenantRequest.newBuilder()
        .setTenantId("t").build();
  }

  private static OzoneManagerProtocolProtos.TenantAssignUserAccessIdRequest
      prodTenantAssignUserAccessIdSub() {
    return OzoneManagerProtocolProtos.TenantAssignUserAccessIdRequest.newBuilder()
        .setUserPrincipal("u").setTenantId("t").setAccessId("a").build();
  }

  private static OzoneManagerProtocolProtos.SetSnapshotPropertyRequest
      prodSetSnapshotPropertyRequestSub(String key) {
    return OzoneManagerProtocolProtos.SetSnapshotPropertyRequest.newBuilder()
        .setSnapshotKey(key).build();
  }

  private static OzoneManagerProtocolProtos.SetVolumePropertyRequest
      prodSetVolumePropertyRequestSub() {
    return OzoneManagerProtocolProtos.SetVolumePropertyRequest.newBuilder()
        .setVolumeName("v").build();
  }

  private static OzoneManagerProtocolProtos.CreateKeyRequest prodCreateKeyRequestSub() {
    return OzoneManagerProtocolProtos.CreateKeyRequest.newBuilder()
        .setKeyArgs(OzoneManagerProtocolProtos.KeyArgs.newBuilder()
            .setVolumeName("v").setBucketName("b").setKeyName("k").build())
        .build();
  }

  private static OzoneManagerProtocolProtos.EchoRPCRequest prodEchoRPCRequestSub() {
    return OzoneManagerProtocolProtos.EchoRPCRequest.newBuilder()
        .setPayloadReq(ByteString.copyFromUtf8("ping")).build();
  }

  private static OzoneManagerProtocolProtos.CreateSnapshotRequest prodCreateSnapshotRequestSub() {
    return OzoneManagerProtocolProtos.CreateSnapshotRequest.newBuilder()
        .setVolumeName("v").setBucketName("b").build();
  }

  // ---------------------------------------------------------------------
  // Enum bridges.
  // ---------------------------------------------------------------------

  private static Proto2OmClientProtocolForOneofMigrationTesting.Type fixtureRequestType(int tag) {
    switch (tag) {
      case 12:  return Proto2OmClientProtocolForOneofMigrationTesting.Type.SetVolumeProperty;
      case 31:  return Proto2OmClientProtocolForOneofMigrationTesting.Type.CreateKey;
      case 110: return Proto2OmClientProtocolForOneofMigrationTesting.Type.EchoRPC;
      case 112: return Proto2OmClientProtocolForOneofMigrationTesting.Type.CreateSnapshot;
      default: throw new IllegalArgumentException("tag " + tag);
    }
  }

  private static OzoneManagerProtocolProtos.Type prodRequestType(int tag) {
    switch (tag) {
      case 12:  return OzoneManagerProtocolProtos.Type.SetVolumeProperty;
      case 31:  return OzoneManagerProtocolProtos.Type.CreateKey;
      case 110: return OzoneManagerProtocolProtos.Type.EchoRPC;
      case 112: return OzoneManagerProtocolProtos.Type.CreateSnapshot;
      default: throw new IllegalArgumentException("tag " + tag);
    }
  }
}
