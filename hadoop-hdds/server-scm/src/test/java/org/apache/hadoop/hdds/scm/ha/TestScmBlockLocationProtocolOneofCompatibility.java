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
import org.apache.hadoop.hdds.protocol.proto.ScmBlockLocationProtocolProtos;
import org.apache.hadoop.hdds.protocol.proto.ScmBlockLocationProtocolProtos.SCMBlockLocationRequest;
import org.apache.hadoop.hdds.protocol.proto.ScmBlockLocationProtocolProtos.SCMBlockLocationResponse;
import org.apache.hadoop.hdds.protocol.proto.testing.Proto2ScmBlockLocationProtocolForOneofMigrationTesting;
import org.junit.jupiter.api.Test;

/**
 * Wire-compatibility tests for the `oneof` migration of
 * `SCMBlockLocationRequest` / `SCMBlockLocationResponse`.
 *
 * <p>The fixture proto
 * (`Proto2ScmBlockLocationProtocolForOneofMigrationTesting.proto`) is a
 * verbatim copy of upstream/master's `ScmServerProtocol.proto` as it
 * existed before the `oneof` migration — only the outer container
 * names (java_package, java_outer_classname, protobuf package) are
 * renamed so its generated Java class does not collide with the
 * production class on the classpath. No field, tag, cardinality, or
 * type inside the wrapper messages has been changed.
 *
 * <p>So the pre-migration schema really is the fixture, byte-for-byte.
 * If a future edit ever renumbers a tag or changes a field's
 * cardinality inside the production `oneof`, this test breaks.
 */
class TestScmBlockLocationProtocolOneofCompatibility {

  // ---------------------------------------------------------------------
  // Request
  // ---------------------------------------------------------------------

  @Test
  void fixtureRequestParsesUnderProductionSchema() throws Exception {
    // Boundary tags: low (11, first arm and pre-16 varint boundary),
    // cross-package sub-message (13, from HddsProtos GetScmInfoRequestProto),
    // last-arm (16). Together these cover the varint tag-encoding
    // boundaries and the mix of in-package / cross-package arms.
    assertFixtureRequestArmAtTag11();
    assertFixtureRequestArmAtTag13();
    assertFixtureRequestArmAtTag16();
  }

  private static void assertFixtureRequestArmAtTag11() throws Exception {
    Proto2ScmBlockLocationProtocolForOneofMigrationTesting.AllocateScmBlockRequestProto arm =
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.AllocateScmBlockRequestProto.newBuilder()
            .setSize(1024L)
            .setNumBlocks(1)
            .setType(HddsProtos.ReplicationType.STAND_ALONE)
            .setOwner("owner")
            .build();
    Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationRequest fixture =
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationRequest.newBuilder()
            .setCmdType(Proto2ScmBlockLocationProtocolForOneofMigrationTesting.Type.AllocateScmBlock)
            .setTraceID("t-11")
            .setVersion(7)
            .setAllocateScmBlockRequest(arm)
            .build();
    byte[] onWire = fixture.toByteArray();

    SCMBlockLocationRequest prod = SCMBlockLocationRequest.parseFrom(onWire);
    assertEquals(ScmBlockLocationProtocolProtos.Type.AllocateScmBlock, prod.getCmdType());
    assertEquals("t-11", prod.getTraceID());
    assertEquals(7, prod.getVersion());
    assertTrue(prod.hasAllocateScmBlockRequest());
    assertArrayEquals(arm.toByteArray(), prod.getAllocateScmBlockRequest().toByteArray());
    assertFalse(prod.hasDeleteScmKeyBlocksRequest());
    assertArrayEquals(onWire, prod.toByteArray());
  }

  private static void assertFixtureRequestArmAtTag13() throws Exception {
    // The arm at tag 13 is `HddsProtos.GetScmInfoRequestProto` —
    // a type that lives OUTSIDE the fixture's package because the fixture
    // imports `hdds.proto` unchanged. Setting it exercises cross-package
    // sub-message wire compat.
    HddsProtos.GetScmInfoRequestProto arm =
        HddsProtos.GetScmInfoRequestProto.newBuilder().setTraceID("inner-t").build();
    Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationRequest fixture =
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationRequest.newBuilder()
            .setCmdType(Proto2ScmBlockLocationProtocolForOneofMigrationTesting.Type.GetScmInfo)
            .setTraceID("t-13")
            .setGetScmInfoRequest(arm)
            .build();
    byte[] onWire = fixture.toByteArray();

    SCMBlockLocationRequest prod = SCMBlockLocationRequest.parseFrom(onWire);
    assertTrue(prod.hasGetScmInfoRequest());
    assertArrayEquals(arm.toByteArray(), prod.getGetScmInfoRequest().toByteArray());
    assertArrayEquals(onWire, prod.toByteArray());
  }

  private static void assertFixtureRequestArmAtTag16() throws Exception {
    Proto2ScmBlockLocationProtocolForOneofMigrationTesting.GetClusterTreeRequestProto arm =
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.GetClusterTreeRequestProto.newBuilder()
            .build();
    Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationRequest fixture =
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationRequest.newBuilder()
            .setCmdType(Proto2ScmBlockLocationProtocolForOneofMigrationTesting.Type.GetClusterTree)
            .setTraceID("t-16")
            .setGetClusterTreeRequest(arm)
            .build();
    byte[] onWire = fixture.toByteArray();

    SCMBlockLocationRequest prod = SCMBlockLocationRequest.parseFrom(onWire);
    assertTrue(prod.hasGetClusterTreeRequest());
    assertArrayEquals(arm.toByteArray(), prod.getGetClusterTreeRequest().toByteArray());
    assertArrayEquals(onWire, prod.toByteArray());
  }

  @Test
  void productionRequestParsesUnderFixtureSchema() throws Exception {
    for (int tag : new int[]{11, 13, 16}) {
      SCMBlockLocationRequest prod = buildProductionRequestForArm(tag);
      byte[] onWire = prod.toByteArray();
      Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationRequest fixture =
          Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationRequest.parseFrom(onWire);

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
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationRequest.getDescriptor();
    // Payload arms start at tag 11 (see the proto file); header is 1..4.
    java.util.Set<Integer> arms = OneofWireCompatUtil.messageFieldNumbersInRange(
        fixtureDesc, 11, Integer.MAX_VALUE);
    assertFalse(arms.isEmpty(), "Fixture should declare at least one payload arm");
    for (int fieldNumber : arms) {
      byte[] onWire = OneofWireCompatUtil.buildBytesWithMessageField(fixtureDesc, fieldNumber);
      SCMBlockLocationRequest prod = SCMBlockLocationRequest.parseFrom(onWire);
      assertArrayEquals(onWire, prod.toByteArray(),
          "Wire mismatch for SCMBlockLocationRequest arm at tag " + fieldNumber);
    }
  }

  /**
   * Empty-body case: a wrapper with only required header fields
   * populated (no payload arm at all). Legal on the wire. Both parsers
   * must accept it and re-emit identical bytes.
   */
  @Test
  void emptyBodyRequestRoundTrips() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationRequest.getDescriptor();
    byte[] onWire = OneofWireCompatUtil.buildEmptyBodyBytes(fixtureDesc);
    SCMBlockLocationRequest prod = SCMBlockLocationRequest.parseFrom(onWire);
    assertArrayEquals(onWire, prod.toByteArray());
  }

  // ---------------------------------------------------------------------
  // Response
  // ---------------------------------------------------------------------

  @Test
  void fixtureResponseParsesUnderProductionSchema() throws Exception {
    Proto2ScmBlockLocationProtocolForOneofMigrationTesting.AllocateScmBlockResponseProto arm =
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.AllocateScmBlockResponseProto.newBuilder()
            .build();

    Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationResponse fixture =
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationResponse.newBuilder()
            .setCmdType(Proto2ScmBlockLocationProtocolForOneofMigrationTesting.Type.AllocateScmBlock)
            .setTraceID("t2")
            .setSuccess(false)
            .setMessage("SCM not leader")
            .setStatus(Proto2ScmBlockLocationProtocolForOneofMigrationTesting.Status.SCM_NOT_LEADER)
            .setLeaderSCMNodeId("scm-1")
            .setAllocateScmBlockResponse(arm)  // tag 11
            .build();

    SCMBlockLocationResponse prod =
        SCMBlockLocationResponse.parseFrom(fixture.toByteArray());
    assertEquals(ScmBlockLocationProtocolProtos.Type.AllocateScmBlock, prod.getCmdType());
    assertEquals("t2", prod.getTraceID());
    assertFalse(prod.getSuccess());
    assertEquals("SCM not leader", prod.getMessage());
    assertEquals(ScmBlockLocationProtocolProtos.Status.SCM_NOT_LEADER, prod.getStatus());
    assertEquals("scm-1", prod.getLeaderSCMNodeId());
    assertTrue(prod.hasAllocateScmBlockResponse());
    assertArrayEquals(arm.toByteArray(), prod.getAllocateScmBlockResponse().toByteArray());
    assertArrayEquals(fixture.toByteArray(), prod.toByteArray());
  }

  @Test
  void productionResponseParsesUnderFixtureSchema() throws Exception {
    SCMBlockLocationResponse prod = SCMBlockLocationResponse.newBuilder()
        .setCmdType(ScmBlockLocationProtocolProtos.Type.AllocateScmBlock)
        .setTraceID("t3")
        .setSuccess(true)
        .setMessage("ok")
        .setStatus(ScmBlockLocationProtocolProtos.Status.OK)
        .setLeaderSCMNodeId("scm-2")
        // tag 11
        .setAllocateScmBlockResponse(
            ScmBlockLocationProtocolProtos.AllocateScmBlockResponseProto.newBuilder())
        .build();

    Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationResponse fixture =
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationResponse.parseFrom(
            prod.toByteArray());

    assertEquals(prod.getCmdType().getNumber(), fixture.getCmdType().getNumber());
    assertEquals("t3", fixture.getTraceID());
    assertTrue(fixture.getSuccess());
    assertEquals("ok", fixture.getMessage());
    assertEquals(prod.getStatus().getNumber(), fixture.getStatus().getNumber());
    assertEquals("scm-2", fixture.getLeaderSCMNodeId());
    assertTrue(fixture.hasAllocateScmBlockResponse());
    assertArrayEquals(
        prod.getAllocateScmBlockResponse().toByteArray(),
        fixture.getAllocateScmBlockResponse().toByteArray());
    assertArrayEquals(prod.toByteArray(), fixture.toByteArray());
  }

  @Test
  void everyResponseArmRoundTripsAgainstProductionSchema() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationResponse.getDescriptor();
    // Payload arms start at tag 11 (see the proto file); header is 1..7.
    java.util.Set<Integer> arms = OneofWireCompatUtil.messageFieldNumbersInRange(
        fixtureDesc, 11, Integer.MAX_VALUE);
    assertFalse(arms.isEmpty(), "Fixture should declare at least one payload arm");
    for (int fieldNumber : arms) {
      byte[] onWire = OneofWireCompatUtil.buildBytesWithMessageField(fixtureDesc, fieldNumber);
      SCMBlockLocationResponse prod = SCMBlockLocationResponse.parseFrom(onWire);
      assertArrayEquals(onWire, prod.toByteArray(),
          "Wire mismatch for SCMBlockLocationResponse arm at tag " + fieldNumber);
    }
  }

  @Test
  void emptyBodyResponseRoundTrips() throws Exception {
    Descriptor fixtureDesc =
        Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationResponse.getDescriptor();
    byte[] onWire = OneofWireCompatUtil.buildEmptyBodyBytes(fixtureDesc);
    SCMBlockLocationResponse prod = SCMBlockLocationResponse.parseFrom(onWire);
    assertArrayEquals(onWire, prod.toByteArray());
  }

  // ---------------------------------------------------------------------
  // Production-side builder helpers
  // ---------------------------------------------------------------------

  private static SCMBlockLocationRequest buildProductionRequestForArm(int tag) {
    SCMBlockLocationRequest.Builder b = SCMBlockLocationRequest.newBuilder()
        .setCmdType(ScmBlockLocationProtocolProtos.Type.AllocateScmBlock)
        .setTraceID("t-" + tag);
    switch (tag) {
    case 11:
      b.setCmdType(ScmBlockLocationProtocolProtos.Type.AllocateScmBlock);
      b.setAllocateScmBlockRequest(ScmBlockLocationProtocolProtos.AllocateScmBlockRequestProto
          .newBuilder()
          .setSize(1024L)
          .setNumBlocks(1)
          .setType(HddsProtos.ReplicationType.STAND_ALONE)
          .setOwner("owner")
          .build());
      break;
    case 13:
      b.setCmdType(ScmBlockLocationProtocolProtos.Type.GetScmInfo);
      b.setGetScmInfoRequest(HddsProtos.GetScmInfoRequestProto.newBuilder()
          .setTraceID("inner-t").build());
      break;
    case 16:
      b.setCmdType(ScmBlockLocationProtocolProtos.Type.GetClusterTree);
      b.setGetClusterTreeRequest(ScmBlockLocationProtocolProtos.GetClusterTreeRequestProto
          .newBuilder().build());
      break;
    default: throw new IllegalArgumentException("tag " + tag);
    }
    return b.build();
  }

  private static void assertPayloadArmMatches(
      SCMBlockLocationRequest prod,
      Proto2ScmBlockLocationProtocolForOneofMigrationTesting.SCMBlockLocationRequest fixture,
      int tag) {
    switch (tag) {
    case 11:
      assertTrue(fixture.hasAllocateScmBlockRequest());
      assertArrayEquals(prod.getAllocateScmBlockRequest().toByteArray(),
          fixture.getAllocateScmBlockRequest().toByteArray());
      break;
    case 13:
      assertTrue(fixture.hasGetScmInfoRequest());
      assertArrayEquals(prod.getGetScmInfoRequest().toByteArray(),
          fixture.getGetScmInfoRequest().toByteArray());
      break;
    case 16:
      assertTrue(fixture.hasGetClusterTreeRequest());
      assertArrayEquals(prod.getGetClusterTreeRequest().toByteArray(),
          fixture.getGetClusterTreeRequest().toByteArray());
      break;
    default:
      throw new IllegalArgumentException("tag " + tag);
    }
  }
}
