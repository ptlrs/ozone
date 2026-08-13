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

package org.apache.hadoop.hdds.scm.ha;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import org.apache.hadoop.hdds.protocol.OneofWireCompatUtil;
import org.apache.hadoop.hdds.protocol.proto.HddsProtos;
import org.apache.hadoop.hdds.protocol.proto.StorageContainerDatanodeProtocolProtos;
import org.apache.hadoop.hdds.protocol.proto.StorageContainerDatanodeProtocolProtos.SCMCommandProto;
import org.apache.hadoop.hdds.protocol.proto.StorageContainerDatanodeProtocolProtos.SCMDatanodeRequest;
import org.apache.hadoop.hdds.protocol.proto.StorageContainerDatanodeProtocolProtos.SCMDatanodeResponse;
import org.apache.hadoop.hdds.protocol.proto.testing.Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting;
import org.junit.jupiter.api.Test;

/**
 * Wire-compatibility tests for the `oneof` migration of
 * `SCMDatanodeRequest`, `SCMDatanodeResponse`, and `SCMCommandProto`.
 *
 * <p>The fixture proto
 * (`Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.proto`)
 * is a verbatim copy of upstream/master's
 * `ScmServerDatanodeHeartbeatProtocol.proto` before the `oneof`
 * migration — only the outer container names (java_package,
 * java_outer_classname, protobuf package) differ so the generated
 * class does not collide with the production class on the classpath.
 * Every submessage, tag, and cardinality inside is unchanged.
 */
class TestScmDatanodeHeartbeatOneofCompatibility {

  // ---------------------------------------------------------------------
  // SCMDatanodeRequest — three arms at tags 3, 4, 5
  // ---------------------------------------------------------------------

  @Test
  void fixtureDatanodeRequestParsesUnderProductionSchema() throws Exception {
    assertFixtureRequestArmParses(3);
    assertFixtureRequestArmParses(4);
    assertFixtureRequestArmParses(5);
  }

  private static void assertFixtureRequestArmParses(int tag) throws Exception {
    Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeRequest.Builder fb =
        Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeRequest.newBuilder()
            .setCmdType(fixtureRequestType(tag))
            .setTraceID("t-" + tag);
    switch (tag) {
      case 3:
        fb.setGetVersionRequest(
            Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting
                .SCMVersionRequestProto.newBuilder().build());
        break;
      case 4:
        fb.setRegisterRequest(fixtureRegisterRequest());
        break;
      case 5:
        fb.setSendHeartbeatRequest(fixtureHeartbeatRequest());
        break;
      default: throw new IllegalStateException();
    }
    byte[] onWire = fb.build().toByteArray();
    SCMDatanodeRequest prod = SCMDatanodeRequest.parseFrom(onWire);

    assertEquals(prodRequestType(tag), prod.getCmdType());
    assertEquals("t-" + tag, prod.getTraceID());
    switch (tag) {
      case 3:
        assertTrue(prod.hasGetVersionRequest());
        assertFalse(prod.hasRegisterRequest());
        assertFalse(prod.hasSendHeartbeatRequest());
        break;
      case 4:
        assertTrue(prod.hasRegisterRequest());
        assertFalse(prod.hasGetVersionRequest());
        assertFalse(prod.hasSendHeartbeatRequest());
        break;
      case 5:
        assertTrue(prod.hasSendHeartbeatRequest());
        assertFalse(prod.hasGetVersionRequest());
        assertFalse(prod.hasRegisterRequest());
        break;
      default: throw new IllegalStateException();
    }
    assertArrayEquals(onWire, prod.toByteArray());
  }

  @Test
  void productionDatanodeRequestParsesUnderFixtureSchema() throws Exception {
    for (int tag : new int[]{3, 4, 5}) {
      SCMDatanodeRequest.Builder pb = SCMDatanodeRequest.newBuilder()
          .setCmdType(prodRequestType(tag))
          .setTraceID("t-" + tag);
      switch (tag) {
        case 3:
          pb.setGetVersionRequest(StorageContainerDatanodeProtocolProtos
              .SCMVersionRequestProto.newBuilder().build());
          break;
        case 4: pb.setRegisterRequest(prodRegisterRequest()); break;
        case 5: pb.setSendHeartbeatRequest(prodHeartbeatRequest()); break;
        default: throw new IllegalStateException();
      }
      SCMDatanodeRequest prod = pb.build();
      byte[] onWire = prod.toByteArray();

      Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeRequest fixture =
          Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeRequest.parseFrom(
              onWire);

      assertEquals(prod.getCmdType().getNumber(), fixture.getCmdType().getNumber());
      assertEquals(prod.getTraceID(), fixture.getTraceID());
      switch (tag) {
        case 3:
          assertTrue(fixture.hasGetVersionRequest());
          assertArrayEquals(prod.getGetVersionRequest().toByteArray(),
              fixture.getGetVersionRequest().toByteArray());
          break;
        case 4:
          assertTrue(fixture.hasRegisterRequest());
          assertArrayEquals(prod.getRegisterRequest().toByteArray(),
              fixture.getRegisterRequest().toByteArray());
          break;
        case 5:
          assertTrue(fixture.hasSendHeartbeatRequest());
          assertArrayEquals(prod.getSendHeartbeatRequest().toByteArray(),
              fixture.getSendHeartbeatRequest().toByteArray());
          break;
        default: throw new IllegalStateException();
      }
      assertArrayEquals(onWire, fixture.toByteArray());
    }
  }

  // ---------------------------------------------------------------------
  // SCMDatanodeResponse — three arms at tags 6, 7, 8
  //
  // The `oneof body { ... }` covers only the payload arms; the four
  // metadata fields (traceID=2, success=3, message=4, status=5) stay
  // flat and must still co-exist with any arm on the wire.
  // ---------------------------------------------------------------------

  @Test
  void fixtureDatanodeResponseParsesUnderProductionSchema() throws Exception {
    assertFixtureResponseArmParses(6);
    assertFixtureResponseArmParses(7);
    assertFixtureResponseArmParses(8);
  }

  private static void assertFixtureResponseArmParses(int tag) throws Exception {
    Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeResponse.Builder fb =
        Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeResponse.newBuilder()
            .setCmdType(fixtureRequestType(responseArmToRequestTag(tag)))
            .setTraceID("t-" + tag)
            .setSuccess(true)
            .setMessage("ok-" + tag)
            .setStatus(Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.Status.OK);
    switch (tag) {
      case 6:
        fb.setGetVersionResponse(fixtureVersionResponse());
        break;
      case 7:
        fb.setRegisterResponse(fixtureRegisteredResponse());
        break;
      case 8:
        fb.setSendHeartbeatResponse(fixtureHeartbeatResponse());
        break;
      default: throw new IllegalStateException();
    }
    byte[] onWire = fb.build().toByteArray();
    SCMDatanodeResponse prod = SCMDatanodeResponse.parseFrom(onWire);

    assertEquals(prodRequestType(responseArmToRequestTag(tag)), prod.getCmdType());
    assertEquals("t-" + tag, prod.getTraceID());
    assertTrue(prod.getSuccess());
    assertEquals("ok-" + tag, prod.getMessage());
    assertEquals(StorageContainerDatanodeProtocolProtos.Status.OK, prod.getStatus());
    switch (tag) {
      case 6:
        assertTrue(prod.hasGetVersionResponse());
        assertFalse(prod.hasRegisterResponse());
        assertFalse(prod.hasSendHeartbeatResponse());
        break;
      case 7:
        assertTrue(prod.hasRegisterResponse());
        assertFalse(prod.hasGetVersionResponse());
        assertFalse(prod.hasSendHeartbeatResponse());
        break;
      case 8:
        assertTrue(prod.hasSendHeartbeatResponse());
        assertFalse(prod.hasGetVersionResponse());
        assertFalse(prod.hasRegisterResponse());
        break;
      default: throw new IllegalStateException();
    }
    assertArrayEquals(onWire, prod.toByteArray());
  }

  @Test
  void productionDatanodeResponseParsesUnderFixtureSchema() throws Exception {
    for (int tag : new int[]{6, 7, 8}) {
      SCMDatanodeResponse.Builder pb = SCMDatanodeResponse.newBuilder()
          .setCmdType(prodRequestType(responseArmToRequestTag(tag)))
          .setTraceID("t-" + tag)
          .setSuccess(true)
          .setMessage("ok-" + tag)
          .setStatus(StorageContainerDatanodeProtocolProtos.Status.OK);
      switch (tag) {
        case 6: pb.setGetVersionResponse(prodVersionResponse()); break;
        case 7: pb.setRegisterResponse(prodRegisteredResponse()); break;
        case 8: pb.setSendHeartbeatResponse(prodHeartbeatResponse()); break;
        default: throw new IllegalStateException();
      }
      SCMDatanodeResponse prod = pb.build();
      byte[] onWire = prod.toByteArray();

      Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeResponse fixture =
          Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeResponse.parseFrom(
              onWire);

      assertEquals(prod.getCmdType().getNumber(), fixture.getCmdType().getNumber());
      assertEquals(prod.getTraceID(), fixture.getTraceID());
      assertEquals(prod.getSuccess(), fixture.getSuccess());
      assertEquals(prod.getMessage(), fixture.getMessage());
      assertEquals(prod.getStatus().getNumber(), fixture.getStatus().getNumber());
      switch (tag) {
        case 6:
          assertTrue(fixture.hasGetVersionResponse());
          assertArrayEquals(prod.getGetVersionResponse().toByteArray(),
              fixture.getGetVersionResponse().toByteArray());
          break;
        case 7:
          assertTrue(fixture.hasRegisterResponse());
          assertArrayEquals(prod.getRegisterResponse().toByteArray(),
              fixture.getRegisterResponse().toByteArray());
          break;
        case 8:
          assertTrue(fixture.hasSendHeartbeatResponse());
          assertArrayEquals(prod.getSendHeartbeatResponse().toByteArray(),
              fixture.getSendHeartbeatResponse().toByteArray());
          break;
        default: throw new IllegalStateException();
      }
      assertArrayEquals(onWire, fixture.toByteArray());
    }
  }

  /**
   * The four metadata fields (traceID=2, success=3, message=4, status=5)
   * are outside the `oneof body` and must still travel on the wire
   * alongside whichever arm is set. Regression guard against a future
   * edit that accidentally folds any of them into the `oneof`.
   */
  @Test
  void datanodeResponseMetadataCoexistsWithBodyArm() throws Exception {
    SCMDatanodeResponse prod = SCMDatanodeResponse.newBuilder()
        .setCmdType(StorageContainerDatanodeProtocolProtos.Type.Register)
        .setTraceID("trace-abc")
        .setSuccess(false)
        .setMessage("failure-detail")
        .setStatus(StorageContainerDatanodeProtocolProtos.Status.ERROR)
        .setRegisterResponse(prodRegisteredResponse())
        .build();
    byte[] onWire = prod.toByteArray();

    // Fixture (pre-oneof) parser sees every field exactly as production
    // emitted it.
    Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeResponse fixture =
        Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeResponse.parseFrom(
            onWire);
    assertEquals("trace-abc", fixture.getTraceID());
    assertFalse(fixture.getSuccess());
    assertEquals("failure-detail", fixture.getMessage());
    assertEquals(Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.Status.ERROR,
        fixture.getStatus());
    assertTrue(fixture.hasRegisterResponse());
    assertFalse(fixture.hasGetVersionResponse());
    assertFalse(fixture.hasSendHeartbeatResponse());
    assertArrayEquals(onWire, fixture.toByteArray());
  }

  // ---------------------------------------------------------------------
  // SCMCommandProto — 12 oneof arms (tags 2..13) + trailer fields 15/16/17
  // ---------------------------------------------------------------------

  @Test
  void fixtureCommandProtoParsesUnderProductionSchema() throws Exception {
    Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.DeleteBlocksCommandProto deleteBlocks =
        Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.DeleteBlocksCommandProto
            .newBuilder().setCmdId(5L).build();

    Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMCommandProto fixture =
        Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMCommandProto.newBuilder()
            .setCommandType(Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting
                .SCMCommandProto.Type.deleteBlocksCommand)
            .setDeleteBlocksCommandProto(deleteBlocks)
            .setTerm(42L)
            .setEncodedToken("token-XYZ")
            .setDeadlineMsSinceEpoch(999L)
            .build();

    SCMCommandProto prod = SCMCommandProto.parseFrom(fixture.toByteArray());
    assertEquals(SCMCommandProto.Type.deleteBlocksCommand, prod.getCommandType());
    assertTrue(prod.hasDeleteBlocksCommandProto());
    assertArrayEquals(deleteBlocks.toByteArray(), prod.getDeleteBlocksCommandProto().toByteArray());
    assertEquals(42L, prod.getTerm());
    assertEquals("token-XYZ", prod.getEncodedToken());
    assertEquals(999L, prod.getDeadlineMsSinceEpoch());
    assertArrayEquals(fixture.toByteArray(), prod.toByteArray());
  }

  @Test
  void productionCommandProtoParsesUnderFixtureSchema() throws Exception {
    for (int tag : new int[]{2, 3, 4, 6, 9, 10, 12, 13}) {
      SCMCommandProto.Builder b = SCMCommandProto.newBuilder()
          .setCommandType(prodCommandType(tag))
          .setTerm(1L + tag)
          .setEncodedToken("tok-" + tag)
          .setDeadlineMsSinceEpoch(1000L + tag);
      switch (tag) {
        case 2:
          b.setReregisterCommandProto(StorageContainerDatanodeProtocolProtos
              .ReregisterCommandProto.newBuilder().build());
          break;
        case 3:
          b.setDeleteBlocksCommandProto(StorageContainerDatanodeProtocolProtos
              .DeleteBlocksCommandProto.newBuilder().setCmdId(5L).build());
          break;
        case 4:
          b.setCloseContainerCommandProto(StorageContainerDatanodeProtocolProtos
              .CloseContainerCommandProto.newBuilder()
              .setContainerID(100L).setCmdId(6L)
              .setPipelineID(HddsProtos.PipelineID.newBuilder().setId("pipe-1").build())
              .build());
          break;
        case 6:
          b.setReplicateContainerCommandProto(StorageContainerDatanodeProtocolProtos
              .ReplicateContainerCommandProto.newBuilder()
              .setContainerID(101L).setCmdId(7L).build());
          break;
        case 9:
          b.setSetNodeOperationalStateCommandProto(StorageContainerDatanodeProtocolProtos
              .SetNodeOperationalStateCommandProto.newBuilder()
              .setCmdId(8L)
              .setNodeOperationalState(HddsProtos.NodeOperationalState.IN_SERVICE)
              .setStateExpiryEpochSeconds(0L)
              .build());
          break;
        case 10:
          b.setFinalizeNewLayoutVersionCommandProto(StorageContainerDatanodeProtocolProtos
              .FinalizeNewLayoutVersionCommandProto.newBuilder()
              .setFinalizeNewLayoutVersion(false)
              .setDataNodeLayoutVersion(StorageContainerDatanodeProtocolProtos
                  .LayoutVersionProto.newBuilder()
                  .setMetadataLayoutVersion(1).setSoftwareLayoutVersion(1).build())
              .setCmdId(9L).build());
          break;
        case 12:
          b.setReconstructECContainersCommandProto(StorageContainerDatanodeProtocolProtos
              .ReconstructECContainersCommandProto.newBuilder()
              .setContainerID(102L)
              .setMissingContainerIndexes(ByteString.copyFrom(new byte[]{1}))
              .setEcReplicationConfig(HddsProtos.ECReplicationConfig.newBuilder()
                  .setData(3).setParity(2).setCodec("RS").setEcChunkSize(1024).build())
              .setCmdId(10L).build());
          break;
        case 13:
          b.setReconcileContainerCommandProto(StorageContainerDatanodeProtocolProtos
              .ReconcileContainerCommandProto.newBuilder().setContainerID(103L).build());
          break;
        default: throw new IllegalStateException("tag " + tag);
      }
      SCMCommandProto prod = b.build();

      Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMCommandProto fixture =
          Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMCommandProto.parseFrom(
              prod.toByteArray());

      assertEquals(prod.getCommandType().getNumber(), fixture.getCommandType().getNumber());
      assertEquals(prod.getTerm(), fixture.getTerm());
      assertEquals(prod.getEncodedToken(), fixture.getEncodedToken());
      assertEquals(prod.getDeadlineMsSinceEpoch(), fixture.getDeadlineMsSinceEpoch());
      assertArrayEquals(prod.toByteArray(), fixture.toByteArray());
    }
  }

  // ---------------------------------------------------------------------
  // Every-arm scans and empty-body cases.
  // ---------------------------------------------------------------------

  /**
   * For every message-typed payload field on the fixture's
   * `SCMDatanodeRequest` (i.e. every arm at tag 3..5), build a minimal
   * fixture message reflectively and assert the production parser
   * re-emits identical bytes. Catches a retagged/dropped arm at
   * compile time via 100% arm coverage.
   */
  @Test
  void everyDatanodeRequestArmRoundTripsAgainstProductionSchema() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeRequest.getDescriptor();
    java.util.Set<Integer> arms = OneofWireCompatUtil.messageFieldNumbersInRange(
        fixtureDesc, 3, Integer.MAX_VALUE);
    assertFalse(arms.isEmpty(), "Fixture should declare at least one payload arm");
    for (int fieldNumber : arms) {
      byte[] onWire = OneofWireCompatUtil.buildBytesWithMessageField(fixtureDesc, fieldNumber);
      SCMDatanodeRequest prod = SCMDatanodeRequest.parseFrom(onWire);
      assertArrayEquals(onWire, prod.toByteArray(),
          "Wire mismatch for SCMDatanodeRequest arm at tag " + fieldNumber);
    }
  }

  /**
   * Every-arm scan for `SCMCommandProto` (tags 2..13).
   */
  @Test
  void everyCommandProtoArmRoundTripsAgainstProductionSchema() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMCommandProto.getDescriptor();
    // Arms live at tags 2..13; trailer scalars start at tag 15. Cap the
    // scan at 14 so trailer additions never accidentally get treated as
    // arms.
    java.util.Set<Integer> arms = OneofWireCompatUtil.messageFieldNumbersInRange(fixtureDesc, 2, 14);
    assertFalse(arms.isEmpty(), "Fixture should declare at least one command arm");
    for (int fieldNumber : arms) {
      byte[] onWire = OneofWireCompatUtil.buildBytesWithMessageField(fixtureDesc, fieldNumber);
      SCMCommandProto prod = SCMCommandProto.parseFrom(onWire);
      assertArrayEquals(onWire, prod.toByteArray(),
          "Wire mismatch for SCMCommandProto arm at tag " + fieldNumber);
    }
  }

  /**
   * Every-arm scan for `SCMDatanodeResponse` (tags 6..8). Cap the scan
   * to skip the flat metadata fields at tags 2..5, which are outside
   * the `oneof body` and are covered by
   * {@link #datanodeResponseMetadataCoexistsWithBodyArm}.
   */
  @Test
  void everyDatanodeResponseArmRoundTripsAgainstProductionSchema() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeResponse.getDescriptor();
    java.util.Set<Integer> arms = OneofWireCompatUtil.messageFieldNumbersInRange(fixtureDesc, 6, 8);
    assertFalse(arms.isEmpty(), "Fixture should declare at least one payload arm");
    for (int fieldNumber : arms) {
      byte[] onWire = OneofWireCompatUtil.buildBytesWithMessageField(fixtureDesc, fieldNumber);
      SCMDatanodeResponse prod = SCMDatanodeResponse.parseFrom(onWire);
      assertArrayEquals(onWire, prod.toByteArray(),
          "Wire mismatch for SCMDatanodeResponse arm at tag " + fieldNumber);
    }
  }

  @Test
  void emptyBodyDatanodeRequestRoundTrips() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeRequest.getDescriptor();
    byte[] onWire = OneofWireCompatUtil.buildEmptyBodyBytes(fixtureDesc);
    SCMDatanodeRequest prod = SCMDatanodeRequest.parseFrom(onWire);
    assertArrayEquals(onWire, prod.toByteArray());
  }

  @Test
  void emptyBodyDatanodeResponseRoundTrips() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMDatanodeResponse.getDescriptor();
    byte[] onWire = OneofWireCompatUtil.buildEmptyBodyBytes(fixtureDesc);
    SCMDatanodeResponse prod = SCMDatanodeResponse.parseFrom(onWire);
    assertArrayEquals(onWire, prod.toByteArray());
  }

  @Test
  void emptyBodyCommandProtoRoundTrips() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMCommandProto.getDescriptor();
    byte[] onWire = OneofWireCompatUtil.buildEmptyBodyBytes(fixtureDesc);
    SCMCommandProto prod = SCMCommandProto.parseFrom(onWire);
    assertArrayEquals(onWire, prod.toByteArray());
  }

  // ---------------------------------------------------------------------
  // Fixture-side sub-message builders — every required field satisfied.
  // ---------------------------------------------------------------------

  private static HddsProtos.DatanodeDetailsProto datanodeDetailsSubMsg() {
    return HddsProtos.DatanodeDetailsProto.newBuilder()
        .setUuid("dn-uuid").setIpAddress("127.0.0.1").setHostName("localhost").build();
  }

  private static Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMRegisterRequestProto
      fixtureRegisterRequest() {
    return Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMRegisterRequestProto
        .newBuilder()
        .setExtendedDatanodeDetails(HddsProtos.ExtendedDatanodeDetailsProto.newBuilder()
            .setDatanodeDetails(datanodeDetailsSubMsg()).build())
        .setNodeReport(Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting
            .NodeReportProto.newBuilder().build())
        .setContainerReport(Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting
            .ContainerReportsProto.newBuilder().build())
        .setPipelineReports(Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting
            .PipelineReportsProto.newBuilder().build())
        .build();
  }

  private static Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMHeartbeatRequestProto
      fixtureHeartbeatRequest() {
    return Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMHeartbeatRequestProto
        .newBuilder().setDatanodeDetails(datanodeDetailsSubMsg()).build();
  }

  private static Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMVersionResponseProto
      fixtureVersionResponse() {
    return Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMVersionResponseProto
        .newBuilder().setSoftwareVersion(1).build();
  }

  private static Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMRegisteredResponseProto
      fixtureRegisteredResponse() {
    return Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMRegisteredResponseProto
        .newBuilder()
        .setErrorCode(Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting
            .SCMRegisteredResponseProto.ErrorCode.success)
        .setDatanodeUUID("dn-uuid").setClusterID("cluster-1").build();
  }

  private static Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMHeartbeatResponseProto
      fixtureHeartbeatResponse() {
    return Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.SCMHeartbeatResponseProto
        .newBuilder().setDatanodeUUID("dn-uuid").build();
  }

  // ---------------------------------------------------------------------
  // Production-side sub-message builders.
  // ---------------------------------------------------------------------

  private static StorageContainerDatanodeProtocolProtos.SCMRegisterRequestProto
      prodRegisterRequest() {
    return StorageContainerDatanodeProtocolProtos.SCMRegisterRequestProto.newBuilder()
        .setExtendedDatanodeDetails(HddsProtos.ExtendedDatanodeDetailsProto.newBuilder()
            .setDatanodeDetails(datanodeDetailsSubMsg()).build())
        .setNodeReport(StorageContainerDatanodeProtocolProtos.NodeReportProto.newBuilder().build())
        .setContainerReport(StorageContainerDatanodeProtocolProtos.ContainerReportsProto.newBuilder().build())
        .setPipelineReports(StorageContainerDatanodeProtocolProtos.PipelineReportsProto.newBuilder().build())
        .build();
  }

  private static StorageContainerDatanodeProtocolProtos.SCMHeartbeatRequestProto
      prodHeartbeatRequest() {
    return StorageContainerDatanodeProtocolProtos.SCMHeartbeatRequestProto.newBuilder()
        .setDatanodeDetails(datanodeDetailsSubMsg()).build();
  }

  private static StorageContainerDatanodeProtocolProtos.SCMVersionResponseProto
      prodVersionResponse() {
    return StorageContainerDatanodeProtocolProtos.SCMVersionResponseProto.newBuilder()
        .setSoftwareVersion(1).build();
  }

  private static StorageContainerDatanodeProtocolProtos.SCMRegisteredResponseProto
      prodRegisteredResponse() {
    return StorageContainerDatanodeProtocolProtos.SCMRegisteredResponseProto.newBuilder()
        .setErrorCode(StorageContainerDatanodeProtocolProtos
            .SCMRegisteredResponseProto.ErrorCode.success)
        .setDatanodeUUID("dn-uuid").setClusterID("cluster-1").build();
  }

  private static StorageContainerDatanodeProtocolProtos.SCMHeartbeatResponseProto
      prodHeartbeatResponse() {
    return StorageContainerDatanodeProtocolProtos.SCMHeartbeatResponseProto.newBuilder()
        .setDatanodeUUID("dn-uuid").build();
  }

  // ---------------------------------------------------------------------
  // Enum bridges.
  // ---------------------------------------------------------------------

  private static Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.Type
      fixtureRequestType(int tag) {
    switch (tag) {
      case 3: return Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.Type.GetVersion;
      case 4: return Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.Type.Register;
      case 5: return Proto2ScmServerDatanodeHeartbeatProtocolForOneofMigrationTesting.Type.SendHeartbeat;
      default: throw new IllegalArgumentException("tag " + tag);
    }
  }

  /**
   * Response body arms live at tags 6..8; the enum `cmdType` values
   * mirror those of the request (`GetVersion`, `Register`,
   * `SendHeartbeat`), which are keyed by request tags 3..5. Map from
   * response arm tag to the corresponding request tag so the same enum
   * helpers work for both sides.
   */
  private static int responseArmToRequestTag(int responseTag) {
    switch (responseTag) {
      case 6: return 3;  // getVersionResponse   ↔ GetVersion
      case 7: return 4;  // registerResponse     ↔ Register
      case 8: return 5;  // sendHeartbeatResponse ↔ SendHeartbeat
      default: throw new IllegalArgumentException("response tag " + responseTag);
    }
  }

  private static StorageContainerDatanodeProtocolProtos.Type prodRequestType(int tag) {
    switch (tag) {
      case 3: return StorageContainerDatanodeProtocolProtos.Type.GetVersion;
      case 4: return StorageContainerDatanodeProtocolProtos.Type.Register;
      case 5: return StorageContainerDatanodeProtocolProtos.Type.SendHeartbeat;
      default: throw new IllegalArgumentException("tag " + tag);
    }
  }

  private static SCMCommandProto.Type prodCommandType(int tag) {
    switch (tag) {
      case 2:  return SCMCommandProto.Type.reregisterCommand;
      case 3:  return SCMCommandProto.Type.deleteBlocksCommand;
      case 4:  return SCMCommandProto.Type.closeContainerCommand;
      case 5:  return SCMCommandProto.Type.deleteContainerCommand;
      case 6:  return SCMCommandProto.Type.replicateContainerCommand;
      case 7:  return SCMCommandProto.Type.createPipelineCommand;
      case 8:  return SCMCommandProto.Type.closePipelineCommand;
      case 9:  return SCMCommandProto.Type.setNodeOperationalStateCommand;
      case 10: return SCMCommandProto.Type.finalizeNewLayoutVersionCommand;
      case 11: return SCMCommandProto.Type.refreshVolumeUsageInfo;
      case 12: return SCMCommandProto.Type.reconstructECContainersCommand;
      case 13: return SCMCommandProto.Type.reconcileContainerCommand;
      default: throw new IllegalArgumentException("tag " + tag);
    }
  }
}
