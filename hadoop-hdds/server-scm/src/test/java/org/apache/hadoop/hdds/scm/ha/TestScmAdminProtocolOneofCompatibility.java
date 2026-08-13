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

import com.google.protobuf.Descriptors.Descriptor;
import org.apache.hadoop.hdds.protocol.OneofWireCompatUtil;
import org.apache.hadoop.hdds.protocol.proto.HddsProtos;
import org.apache.hadoop.hdds.protocol.proto.StorageContainerLocationProtocolProtos;
import org.apache.hadoop.hdds.protocol.proto.StorageContainerLocationProtocolProtos.ScmContainerLocationRequest;
import org.apache.hadoop.hdds.protocol.proto.StorageContainerLocationProtocolProtos.ScmContainerLocationResponse;
import org.apache.hadoop.hdds.protocol.proto.testing.Proto2ScmAdminProtocolForOneofMigrationTesting;
import org.junit.jupiter.api.Test;

/**
 * Wire-compatibility tests for the `oneof` migration of
 * `ScmContainerLocationRequest` / `ScmContainerLocationResponse`.
 *
 * <p>The fixture proto
 * (`Proto2ScmAdminProtocolForOneofMigrationTesting.proto`) is a verbatim
 * copy of upstream/master's `ScmAdminProtocol.proto` as it existed
 * before the `oneof` migration — only the outer container names
 * (java_package, java_outer_classname, protobuf package) are
 * renamed so its generated Java class does not collide with the
 * production class on the classpath. No field, tag, cardinality,
 * or type inside the file has been changed.
 *
 * <p>So the pre-migration schema really is the fixture, byte-for-byte.
 * If a future edit ever renumbers a tag or changes a field's
 * cardinality inside the production `oneof`, this test breaks.
 */
class TestScmAdminProtocolOneofCompatibility {

  // ---------------------------------------------------------------------
  // Request
  // ---------------------------------------------------------------------

  @Test
  void fixtureRequestParsesUnderProductionSchema() throws Exception {
    // Boundary tags: low (6), pre-16 varint boundary (11), mid (24),
    // cross-package sub-message (42, from HddsProtos), high (52).
    assertFixtureRequestArmAtTag6();
    assertFixtureRequestArmAtTag11();
    assertFixtureRequestArmAtTag24();
    assertFixtureRequestArmAtTag42();
    assertFixtureRequestArmAtTag52();
  }

  private static void assertFixtureRequestArmAtTag6() throws Exception {
    Proto2ScmAdminProtocolForOneofMigrationTesting.ContainerRequestProto arm =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ContainerRequestProto.newBuilder()
            .setReplicationType(HddsProtos.ReplicationType.STAND_ALONE)
            .setOwner("owner")
            .build();
    Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest fixture =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest.newBuilder()
            .setCmdType(Proto2ScmAdminProtocolForOneofMigrationTesting.Type.AllocateContainer)
            .setTraceID("t-6")
            .setVersion(7)
            .setContainerRequest(arm)
            .build();
    byte[] onWire = fixture.toByteArray();

    ScmContainerLocationRequest prod = ScmContainerLocationRequest.parseFrom(onWire);
    assertEquals(StorageContainerLocationProtocolProtos.Type.AllocateContainer, prod.getCmdType());
    assertEquals("t-6", prod.getTraceID());
    assertEquals(7, prod.getVersion());
    assertTrue(prod.hasContainerRequest());
    assertArrayEquals(arm.toByteArray(), prod.getContainerRequest().toByteArray());
    assertFalse(prod.hasGetPipelineRequest());
    assertArrayEquals(onWire, prod.toByteArray());
  }

  private static void assertFixtureRequestArmAtTag11() throws Exception {
    Proto2ScmAdminProtocolForOneofMigrationTesting.NodeQueryRequestProto arm =
        Proto2ScmAdminProtocolForOneofMigrationTesting.NodeQueryRequestProto.newBuilder()
            .setScope(HddsProtos.QueryScope.CLUSTER)
            .build();
    Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest fixture =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest.newBuilder()
            .setCmdType(Proto2ScmAdminProtocolForOneofMigrationTesting.Type.QueryNode)
            .setTraceID("t-11")
            .setNodeQueryRequest(arm)
            .build();
    byte[] onWire = fixture.toByteArray();

    ScmContainerLocationRequest prod = ScmContainerLocationRequest.parseFrom(onWire);
    assertTrue(prod.hasNodeQueryRequest());
    assertArrayEquals(arm.toByteArray(), prod.getNodeQueryRequest().toByteArray());
    assertArrayEquals(onWire, prod.toByteArray());
  }

  private static void assertFixtureRequestArmAtTag24() throws Exception {
    Proto2ScmAdminProtocolForOneofMigrationTesting.GetPipelineRequestProto arm =
        Proto2ScmAdminProtocolForOneofMigrationTesting.GetPipelineRequestProto.newBuilder()
            .setPipelineID(HddsProtos.PipelineID.newBuilder().setId("pipe-1").build())
            .build();
    Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest fixture =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest.newBuilder()
            .setCmdType(Proto2ScmAdminProtocolForOneofMigrationTesting.Type.GetPipeline)
            .setTraceID("t-24")
            .setGetPipelineRequest(arm)
            .build();
    byte[] onWire = fixture.toByteArray();

    ScmContainerLocationRequest prod = ScmContainerLocationRequest.parseFrom(onWire);
    assertTrue(prod.hasGetPipelineRequest());
    assertArrayEquals(arm.toByteArray(), prod.getGetPipelineRequest().toByteArray());
    assertArrayEquals(onWire, prod.toByteArray());
  }

  private static void assertFixtureRequestArmAtTag42() throws Exception {
    // The arm at tag 42 is `HddsProtos.TransferLeadershipRequestProto` —
    // a type that lives OUTSIDE the fixture's package because the fixture
    // imports `hdds.proto` unchanged. Setting it exercises cross-package
    // sub-message wire compat.
    HddsProtos.TransferLeadershipRequestProto arm =
        HddsProtos.TransferLeadershipRequestProto.newBuilder().setNewLeaderId("scm-1").build();
    Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest fixture =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest.newBuilder()
            .setCmdType(Proto2ScmAdminProtocolForOneofMigrationTesting.Type.TransferLeadership)
            .setTraceID("t-42")
            .setTransferScmLeadershipRequest(arm)
            .build();
    byte[] onWire = fixture.toByteArray();

    ScmContainerLocationRequest prod = ScmContainerLocationRequest.parseFrom(onWire);
    assertTrue(prod.hasTransferScmLeadershipRequest());
    assertArrayEquals(arm.toByteArray(), prod.getTransferScmLeadershipRequest().toByteArray());
    assertArrayEquals(onWire, prod.toByteArray());
  }

  private static void assertFixtureRequestArmAtTag52() throws Exception {
    Proto2ScmAdminProtocolForOneofMigrationTesting.SuppressContainerRequestProto arm =
        Proto2ScmAdminProtocolForOneofMigrationTesting.SuppressContainerRequestProto.newBuilder().build();
    Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest fixture =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest.newBuilder()
            .setCmdType(Proto2ScmAdminProtocolForOneofMigrationTesting.Type.SuppressContainer)
            .setTraceID("t-52")
            .setSuppressContainerRequest(arm)
            .build();
    byte[] onWire = fixture.toByteArray();

    ScmContainerLocationRequest prod = ScmContainerLocationRequest.parseFrom(onWire);
    assertTrue(prod.hasSuppressContainerRequest());
    assertArrayEquals(arm.toByteArray(), prod.getSuppressContainerRequest().toByteArray());
    assertArrayEquals(onWire, prod.toByteArray());
  }

  @Test
  void productionRequestParsesUnderFixtureSchema() throws Exception {
    for (int tag : new int[]{6, 11, 24, 42, 52}) {
      ScmContainerLocationRequest prod = buildProductionRequestForArm(tag);
      byte[] onWire = prod.toByteArray();
      Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest fixture =
          Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest.parseFrom(onWire);

      assertEquals(prod.getCmdType().getNumber(), fixture.getCmdType().getNumber());
      assertEquals(prod.getTraceID(), fixture.getTraceID());
      assertPayloadArmMatches(prod, fixture, tag);
      assertArrayEquals(onWire, fixture.toByteArray());
    }
  }

  /**
   * Every-arm scan: for every message-typed payload field in the fixture
   * wrapper, build a minimal fixture message reflectively, hand the
   * bytes to the production parser, and assert byte-identical round-trip.
   * Catches any arm that gets retagged or dropped without being added to
   * the spot-check list above.
   */
  @Test
  void everyRequestArmRoundTripsAgainstProductionSchema() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest.getDescriptor();
    // Payload arms live in tags 6..(highest); exclude the header (1..5).
    java.util.Set<Integer> arms = OneofWireCompatUtil.messageFieldNumbersInRange(
        fixtureDesc, 6, Integer.MAX_VALUE);
    assertFalse(arms.isEmpty(), "Fixture should declare at least one payload arm");
    for (int fieldNumber : arms) {
      byte[] onWire = OneofWireCompatUtil.buildBytesWithMessageField(fixtureDesc, fieldNumber);
      ScmContainerLocationRequest prod = ScmContainerLocationRequest.parseFrom(onWire);
      assertArrayEquals(onWire, prod.toByteArray(),
          "Wire mismatch for ScmContainerLocationRequest arm at tag " + fieldNumber);
    }
  }

  /**
   * Empty-body case: a wrapper with only required header fields
   * populated (no payload arm at all). Legal on the wire and produced
   * by real code paths (e.g. a probing request). Both parsers must
   * accept it and re-emit identical bytes.
   */
  @Test
  void emptyBodyRequestRoundTrips() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest.getDescriptor();
    byte[] onWire = OneofWireCompatUtil.buildEmptyBodyBytes(fixtureDesc);
    ScmContainerLocationRequest prod = ScmContainerLocationRequest.parseFrom(onWire);
    assertArrayEquals(onWire, prod.toByteArray());
  }

  // ---------------------------------------------------------------------
  // Response
  // ---------------------------------------------------------------------

  @Test
  void fixtureResponseParsesUnderProductionSchema() throws Exception {
    Proto2ScmAdminProtocolForOneofMigrationTesting.SCMListContainerResponseProto arm =
        Proto2ScmAdminProtocolForOneofMigrationTesting.SCMListContainerResponseProto.newBuilder()
            .setContainerCount(42L).build();

    Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationResponse fixture =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationResponse.newBuilder()
            .setCmdType(Proto2ScmAdminProtocolForOneofMigrationTesting.Type.ListContainer)
            .setTraceID("t2")
            .setSuccess(false)
            .setMessage("SCM not leader")
            .setStatus(Proto2ScmAdminProtocolForOneofMigrationTesting
                .ScmContainerLocationResponse.Status.SCM_NOT_LEADER)
            .setScmListContainerResponse(arm)  // tag 9
            .build();

    ScmContainerLocationResponse prod =
        ScmContainerLocationResponse.parseFrom(fixture.toByteArray());
    assertEquals(StorageContainerLocationProtocolProtos.Type.ListContainer, prod.getCmdType());
    assertEquals("t2", prod.getTraceID());
    assertFalse(prod.getSuccess());
    assertEquals("SCM not leader", prod.getMessage());
    assertEquals(ScmContainerLocationResponse.Status.SCM_NOT_LEADER, prod.getStatus());
    assertTrue(prod.hasScmListContainerResponse());
    assertArrayEquals(arm.toByteArray(), prod.getScmListContainerResponse().toByteArray());
    assertArrayEquals(fixture.toByteArray(), prod.toByteArray());
  }

  @Test
  void productionResponseParsesUnderFixtureSchema() throws Exception {
    ScmContainerLocationResponse prod = ScmContainerLocationResponse.newBuilder()
        .setCmdType(StorageContainerLocationProtocolProtos.Type.ListContainer)
        .setTraceID("t3")
        .setSuccess(true)
        .setMessage("ok")
        .setStatus(ScmContainerLocationResponse.Status.OK)
        .setScmListContainerResponse(   // tag 9
            StorageContainerLocationProtocolProtos.SCMListContainerResponseProto.newBuilder()
                .setContainerCount(42L))
        .build();

    Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationResponse fixture =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationResponse.parseFrom(
            prod.toByteArray());

    assertEquals(prod.getCmdType().getNumber(), fixture.getCmdType().getNumber());
    assertEquals("t3", fixture.getTraceID());
    assertTrue(fixture.getSuccess());
    assertEquals("ok", fixture.getMessage());
    assertEquals(prod.getStatus().getNumber(), fixture.getStatus().getNumber());
    assertTrue(fixture.hasScmListContainerResponse());
    assertArrayEquals(
        prod.getScmListContainerResponse().toByteArray(),
        fixture.getScmListContainerResponse().toByteArray());
    assertArrayEquals(prod.toByteArray(), fixture.toByteArray());
  }

  @Test
  void everyResponseArmRoundTripsAgainstProductionSchema() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationResponse.getDescriptor();
    // Payload arms start at tag 6 (see the proto file); header is 1..5.
    java.util.Set<Integer> arms = OneofWireCompatUtil.messageFieldNumbersInRange(
        fixtureDesc, 6, Integer.MAX_VALUE);
    assertFalse(arms.isEmpty(), "Fixture should declare at least one payload arm");
    for (int fieldNumber : arms) {
      byte[] onWire = OneofWireCompatUtil.buildBytesWithMessageField(fixtureDesc, fieldNumber);
      ScmContainerLocationResponse prod = ScmContainerLocationResponse.parseFrom(onWire);
      assertArrayEquals(onWire, prod.toByteArray(),
          "Wire mismatch for ScmContainerLocationResponse arm at tag " + fieldNumber);
    }
  }

  @Test
  void emptyBodyResponseRoundTrips() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationResponse.getDescriptor();
    byte[] onWire = OneofWireCompatUtil.buildEmptyBodyBytes(fixtureDesc);
    ScmContainerLocationResponse prod = ScmContainerLocationResponse.parseFrom(onWire);
    assertArrayEquals(onWire, prod.toByteArray());
  }

  // ---------------------------------------------------------------------
  // Production-side builder helpers
  // ---------------------------------------------------------------------

  private static ScmContainerLocationRequest buildProductionRequestForArm(int tag) {
    ScmContainerLocationRequest.Builder b = ScmContainerLocationRequest.newBuilder()
        .setCmdType(StorageContainerLocationProtocolProtos.Type.AllocateContainer)
        .setTraceID("t-" + tag);
    switch (tag) {
      case 6:
        b.setContainerRequest(StorageContainerLocationProtocolProtos.ContainerRequestProto
            .newBuilder()
            .setReplicationType(HddsProtos.ReplicationType.STAND_ALONE)
            .setOwner("owner")
            .build());
        break;
      case 11:
        b.setNodeQueryRequest(StorageContainerLocationProtocolProtos.NodeQueryRequestProto
            .newBuilder()
            .setScope(HddsProtos.QueryScope.CLUSTER)
            .build());
        break;
      case 24:
        b.setGetPipelineRequest(StorageContainerLocationProtocolProtos.GetPipelineRequestProto
            .newBuilder()
            .setPipelineID(HddsProtos.PipelineID.newBuilder().setId("pipe-1").build())
            .build());
        break;
      case 42:
        b.setTransferScmLeadershipRequest(HddsProtos.TransferLeadershipRequestProto.newBuilder()
            .setNewLeaderId("scm-1").build());
        break;
      case 52:
        b.setSuppressContainerRequest(StorageContainerLocationProtocolProtos
            .SuppressContainerRequestProto.newBuilder().build());
        break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
    return b.build();
  }

  private static void assertPayloadArmMatches(
      ScmContainerLocationRequest prod,
      Proto2ScmAdminProtocolForOneofMigrationTesting.ScmContainerLocationRequest fixture,
      int tag) {
    switch (tag) {
      case 6:
        assertTrue(fixture.hasContainerRequest());
        assertArrayEquals(prod.getContainerRequest().toByteArray(),
            fixture.getContainerRequest().toByteArray());
        break;
      case 11:
        assertTrue(fixture.hasNodeQueryRequest());
        assertArrayEquals(prod.getNodeQueryRequest().toByteArray(),
            fixture.getNodeQueryRequest().toByteArray());
        break;
      case 24:
        assertTrue(fixture.hasGetPipelineRequest());
        assertArrayEquals(prod.getGetPipelineRequest().toByteArray(),
            fixture.getGetPipelineRequest().toByteArray());
        break;
      case 42:
        assertTrue(fixture.hasTransferScmLeadershipRequest());
        assertArrayEquals(prod.getTransferScmLeadershipRequest().toByteArray(),
            fixture.getTransferScmLeadershipRequest().toByteArray());
        break;
      case 52:
        assertTrue(fixture.hasSuppressContainerRequest());
        assertArrayEquals(prod.getSuppressContainerRequest().toByteArray(),
            fixture.getSuppressContainerRequest().toByteArray());
        break;
      default:
        throw new IllegalArgumentException("tag " + tag);
    }
  }
}
