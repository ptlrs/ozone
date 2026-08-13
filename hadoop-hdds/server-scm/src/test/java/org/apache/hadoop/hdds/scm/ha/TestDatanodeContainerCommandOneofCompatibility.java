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
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.ContainerCommandRequestProto;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.ContainerCommandResponseProto;
import org.apache.hadoop.hdds.protocol.datanode.proto.testing.Proto2DatanodeClientProtocolForOneofMigrationTesting;
import org.junit.jupiter.api.Test;

/**
 * Wire-compatibility tests for the `oneof` migration of
 * `ContainerCommandRequestProto` and `ContainerCommandResponseProto`.
 *
 * <p>The fixture proto
 * (`Proto2DatanodeClientProtocolForOneofMigrationTesting.proto`) is a verbatim
 * copy of upstream/master's `DatanodeClientProtocol.proto` before the
 * `oneof` migration; only the outer container names differ so the
 * generated class does not collide with `ContainerProtos`.
 *
 * <p>Production `ContainerProtos` is generated against the
 * `org.apache.ratis.thirdparty.com.google.protobuf` runtime while the
 * fixture uses plain `com.google.protobuf`; `parseFrom(byte[])` /
 * `toByteArray()` bridges the two runtimes.
 */
class TestDatanodeContainerCommandOneofCompatibility {

  // ---------------------------------------------------------------------
  // ContainerCommandRequestProto
  // ---------------------------------------------------------------------

  @Test
  void fixtureRequestParsesUnderProductionSchema() throws Exception {
    assertFixtureRequestArmParses(6);
    assertFixtureRequestArmParses(16);
    assertFixtureRequestArmParses(28);
  }

  private static void assertFixtureRequestArmParses(int tag) throws Exception {
    Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandRequestProto.Builder fb =
        Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandRequestProto.newBuilder()
            .setCmdType(fixtureRequestType(tag))
            .setTraceID("tr-" + tag)
            .setContainerID(200L + tag)
            .setDatanodeUuid("dn-" + tag)
            .setPipelineID("pipe-" + tag)
            .setEncodedToken("tok-" + tag)  // trailer, tag 23
            .setVersion(1 + tag);           // trailer, tag 24
    switch (tag) {
      case 6:  fb.setCreateContainer(fixtureCreateContainer()); break;
      case 16: fb.setReadChunk(fixtureReadChunk()); break;
      case 28: fb.setReadBlock(fixtureReadBlock()); break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
    Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandRequestProto fixture = fb.build();
    byte[] onWire = fixture.toByteArray();
    ContainerCommandRequestProto prod = ContainerCommandRequestProto.parseFrom(onWire);

    assertEquals(prodRequestType(tag), prod.getCmdType());
    assertEquals("tr-" + tag, prod.getTraceID());
    assertEquals(200L + tag, prod.getContainerID());
    assertEquals("dn-" + tag, prod.getDatanodeUuid());
    assertEquals("pipe-" + tag, prod.getPipelineID());
    // Trailer fields at tags 23 and 24, positioned AFTER the oneof block,
    // must remain parseable at their original tags.
    assertEquals("tok-" + tag, prod.getEncodedToken());
    assertEquals(1 + tag, prod.getVersion());

    switch (tag) {
      case 6:
        assertTrue(prod.hasCreateContainer());
        assertArrayEquals(fixture.getCreateContainer().toByteArray(),
            prod.getCreateContainer().toByteArray());
        break;
      case 16:
        assertTrue(prod.hasReadChunk());
        assertArrayEquals(fixture.getReadChunk().toByteArray(),
            prod.getReadChunk().toByteArray());
        break;
      case 28:
        assertTrue(prod.hasReadBlock());
        assertArrayEquals(fixture.getReadBlock().toByteArray(),
            prod.getReadBlock().toByteArray());
        break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
    assertArrayEquals(onWire, prod.toByteArray());
  }

  @Test
  void productionRequestParsesUnderFixtureSchema() throws Exception {
    for (int tag : new int[]{6, 12, 17, 22, 25, 28}) {
      ContainerCommandRequestProto prod = buildProdRequestForArm(tag);
      byte[] onWire = prod.toByteArray();
      Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandRequestProto fixture =
          Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandRequestProto.parseFrom(onWire);

      assertEquals(prod.getCmdType().getNumber(), fixture.getCmdType().getNumber());
      assertEquals(prod.getContainerID(), fixture.getContainerID());
      assertEquals(prod.getDatanodeUuid(), fixture.getDatanodeUuid());
      assertEquals(prod.getPipelineID(), fixture.getPipelineID());
      assertEquals(prod.getEncodedToken(), fixture.getEncodedToken());
      assertEquals(prod.getVersion(), fixture.getVersion());
      assertPayloadArmMatches(prod, fixture, tag);
      assertArrayEquals(onWire, fixture.toByteArray());
    }
  }

  // ---------------------------------------------------------------------
  // ContainerCommandResponseProto
  // ---------------------------------------------------------------------

  @Test
  void fixtureResponseParsesUnderProductionSchema() throws Exception {
    assertFixtureResponseArmParses(15);
    assertFixtureResponseArmParses(25);
  }

  private static void assertFixtureResponseArmParses(int tag) throws Exception {
    Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandResponseProto.Builder fb =
        Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandResponseProto.newBuilder()
            .setCmdType(fixtureResponseType(tag))
            .setTraceID("tr-r-" + tag)
            .setResult(Proto2DatanodeClientProtocolForOneofMigrationTesting.Result.SUCCESS)
            .setMessage("ok-" + tag);
    switch (tag) {
      case 15: fb.setWriteChunk(fixtureWriteChunkResp()); break;
      case 25: fb.setReadBlock(fixtureReadBlockResp()); break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
    Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandResponseProto fixture = fb.build();
    byte[] onWire = fixture.toByteArray();
    ContainerCommandResponseProto prod = ContainerCommandResponseProto.parseFrom(onWire);
    assertEquals(prodResponseType(tag), prod.getCmdType());
    assertEquals("tr-r-" + tag, prod.getTraceID());
    assertEquals(ContainerProtos.Result.SUCCESS, prod.getResult());
    assertEquals("ok-" + tag, prod.getMessage());
    switch (tag) {
      case 15:
        assertTrue(prod.hasWriteChunk());
        assertArrayEquals(fixture.getWriteChunk().toByteArray(), prod.getWriteChunk().toByteArray());
        break;
      case 25:
        assertTrue(prod.hasReadBlock());
        assertArrayEquals(fixture.getReadBlock().toByteArray(), prod.getReadBlock().toByteArray());
        break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
    assertArrayEquals(onWire, prod.toByteArray());
  }

  @Test
  void productionResponseParsesUnderFixtureSchema() throws Exception {
    for (int tag : new int[]{5, 11, 15, 19, 22, 25}) {
      ContainerCommandResponseProto prod = buildProdResponseForArm(tag);
      byte[] onWire = prod.toByteArray();
      Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandResponseProto fixture =
          Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandResponseProto.parseFrom(onWire);
      assertEquals(prod.getCmdType().getNumber(), fixture.getCmdType().getNumber());
      assertEquals(prod.getResult().getNumber(), fixture.getResult().getNumber());
      assertEquals(prod.getMessage(), fixture.getMessage());
      assertResponseArmMatches(prod, fixture, tag);
      assertArrayEquals(onWire, fixture.toByteArray());
    }
  }

  // ---------------------------------------------------------------------
  // Every-arm scans, empty-body cases, and explicit cross-runtime check.
  // ---------------------------------------------------------------------

  /**
   * Every-arm scan for `ContainerCommandRequestProto`. The scan uses
   * plain-runtime reflection on the fixture descriptor to build a
   * minimal message for each arm, then hands the bytes to the
   * Ratis-thirdparty production parser (`ContainerCommandRequestProto`)
   * and asserts byte-identical round-trip. Payload arm tags live in
   * [6, 22] and [25, 28]; trailer scalars occupy 23/24.
   */
  @Test
  void everyRequestArmRoundTripsAgainstProductionSchema() throws Exception {
    Descriptor fixtureDesc =
        Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandRequestProto.getDescriptor();
    java.util.Set<Integer> arms = OneofWireCompatUtil.messageFieldNumbersInRange(fixtureDesc, 6, 40);
    assertFalse(arms.isEmpty(), "Fixture should declare at least one arm");
    for (int fieldNumber : arms) {
      byte[] onWire = OneofWireCompatUtil.buildBytesWithMessageField(fixtureDesc, fieldNumber);
      ContainerCommandRequestProto prod = ContainerCommandRequestProto.parseFrom(onWire);
      assertArrayEquals(onWire, prod.toByteArray(),
          "Wire mismatch for ContainerCommandRequestProto arm at tag " + fieldNumber);
    }
  }

  @Test
  void everyResponseArmRoundTripsAgainstProductionSchema() throws Exception {
    Descriptor fixtureDesc =
        Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandResponseProto.getDescriptor();
    java.util.Set<Integer> arms = OneofWireCompatUtil.messageFieldNumbersInRange(fixtureDesc, 5, 40);
    assertFalse(arms.isEmpty(), "Fixture should declare at least one arm");
    for (int fieldNumber : arms) {
      byte[] onWire = OneofWireCompatUtil.buildBytesWithMessageField(fixtureDesc, fieldNumber);
      ContainerCommandResponseProto prod = ContainerCommandResponseProto.parseFrom(onWire);
      assertArrayEquals(onWire, prod.toByteArray(),
          "Wire mismatch for ContainerCommandResponseProto arm at tag " + fieldNumber);
    }
  }

  @Test
  void emptyBodyRequestRoundTrips() throws Exception {
    Descriptor fixtureDesc =
        Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandRequestProto.getDescriptor();
    byte[] onWire = OneofWireCompatUtil.buildEmptyBodyBytes(fixtureDesc);
    ContainerCommandRequestProto prod = ContainerCommandRequestProto.parseFrom(onWire);
    assertArrayEquals(onWire, prod.toByteArray());
  }

  @Test
  void emptyBodyResponseRoundTrips() throws Exception {
    Descriptor fixtureDesc =
        Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandResponseProto.getDescriptor();
    byte[] onWire = OneofWireCompatUtil.buildEmptyBodyBytes(fixtureDesc);
    ContainerCommandResponseProto prod = ContainerCommandResponseProto.parseFrom(onWire);
    assertArrayEquals(onWire, prod.toByteArray());
  }

  /**
   * Explicit cross-runtime `data`-byte round-trip check. The `data`
   * field on `ReadChunkResponseProto` (`oneof responseData { bytes data = 3; ... }`
   * in production, `optional bytes data = 3` in the fixture) is the
   * only place a raw byte payload crosses the plain-runtime →
   * Ratis-thirdparty boundary in these tests. `assertArrayEquals` on
   * the outer wire already proves it, but this explicit check gives a
   * future reviewer a single line to look at.
   */
  @Test
  void readChunkResponseDataBytesSurviveCrossRuntimeRoundTrip() throws Exception {
    byte[] payload = "chunk-data-☃".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    // Build on the fixture (plain-runtime) side, hand to production.
    Proto2DatanodeClientProtocolForOneofMigrationTesting.ReadChunkResponseProto fixtureReadChunk =
        Proto2DatanodeClientProtocolForOneofMigrationTesting.ReadChunkResponseProto.newBuilder()
            .setBlockID(Proto2DatanodeClientProtocolForOneofMigrationTesting.DatanodeBlockID.newBuilder()
                .setContainerID(1L).setLocalID(1L).build())
            .setChunkData(Proto2DatanodeClientProtocolForOneofMigrationTesting.ChunkInfo.newBuilder()
                .setChunkName("c").setOffset(0L).setLen(payload.length)
                .setChecksumData(Proto2DatanodeClientProtocolForOneofMigrationTesting.ChecksumData.newBuilder()
                    .setType(Proto2DatanodeClientProtocolForOneofMigrationTesting.ChecksumType.NONE)
                    .setBytesPerChecksum(0).build())
                .build())
            .setData(com.google.protobuf.ByteString.copyFrom(payload))
            .build();
    ContainerProtos.ReadChunkResponseProto prodParsed =
        ContainerProtos.ReadChunkResponseProto.parseFrom(fixtureReadChunk.toByteArray());
    assertArrayEquals(payload, prodParsed.getData().toByteArray(),
        "data bytes must survive plain-runtime -> Ratis-thirdparty round-trip");

    // Round-trip the other direction: production -> fixture.
    ContainerProtos.ReadChunkResponseProto prodBuilt =
        ContainerProtos.ReadChunkResponseProto.newBuilder()
            .setBlockID(ContainerProtos.DatanodeBlockID.newBuilder()
                .setContainerID(1L).setLocalID(1L).build())
            .setChunkData(ContainerProtos.ChunkInfo.newBuilder()
                .setChunkName("c").setOffset(0L).setLen(payload.length)
                .setChecksumData(ContainerProtos.ChecksumData.newBuilder()
                    .setType(ContainerProtos.ChecksumType.NONE).setBytesPerChecksum(0).build())
                .build())
            .setData(org.apache.ratis.thirdparty.com.google.protobuf.ByteString.copyFrom(payload))
            .build();
    Proto2DatanodeClientProtocolForOneofMigrationTesting.ReadChunkResponseProto fixtureParsed =
        Proto2DatanodeClientProtocolForOneofMigrationTesting.ReadChunkResponseProto.parseFrom(
            prodBuilt.toByteArray());
    assertArrayEquals(payload, fixtureParsed.getData().toByteArray(),
        "data bytes must survive Ratis-thirdparty -> plain-runtime round-trip");
  }

  // ---------------------------------------------------------------------
  // Fixture-side sub-message builders (plain com.google.protobuf runtime).
  // ---------------------------------------------------------------------

  private static Proto2DatanodeClientProtocolForOneofMigrationTesting.DatanodeBlockID fixtureBlockId() {
    return Proto2DatanodeClientProtocolForOneofMigrationTesting.DatanodeBlockID.newBuilder()
        .setContainerID(1L).setLocalID(1L).build();
  }

  private static Proto2DatanodeClientProtocolForOneofMigrationTesting.ChecksumData fixtureChecksumData() {
    return Proto2DatanodeClientProtocolForOneofMigrationTesting.ChecksumData.newBuilder()
        .setType(Proto2DatanodeClientProtocolForOneofMigrationTesting.ChecksumType.NONE)
        .setBytesPerChecksum(0).build();
  }

  private static Proto2DatanodeClientProtocolForOneofMigrationTesting.ChunkInfo fixtureChunkInfo() {
    return Proto2DatanodeClientProtocolForOneofMigrationTesting.ChunkInfo.newBuilder()
        .setChunkName("c").setOffset(0L).setLen(0L)
        .setChecksumData(fixtureChecksumData()).build();
  }

  private static Proto2DatanodeClientProtocolForOneofMigrationTesting.CreateContainerRequestProto
      fixtureCreateContainer() {
    return Proto2DatanodeClientProtocolForOneofMigrationTesting.CreateContainerRequestProto
        .newBuilder().build();
  }

  private static Proto2DatanodeClientProtocolForOneofMigrationTesting.ReadChunkRequestProto
      fixtureReadChunk() {
    return Proto2DatanodeClientProtocolForOneofMigrationTesting.ReadChunkRequestProto.newBuilder()
        .setBlockID(fixtureBlockId()).setChunkData(fixtureChunkInfo()).build();
  }

  private static Proto2DatanodeClientProtocolForOneofMigrationTesting.ReadBlockRequestProto
      fixtureReadBlock() {
    return Proto2DatanodeClientProtocolForOneofMigrationTesting.ReadBlockRequestProto.newBuilder()
        .setBlockID(fixtureBlockId()).setOffset(0L).build();
  }

  private static Proto2DatanodeClientProtocolForOneofMigrationTesting.WriteChunkResponseProto
      fixtureWriteChunkResp() {
    return Proto2DatanodeClientProtocolForOneofMigrationTesting.WriteChunkResponseProto
        .newBuilder().build();
  }

  private static Proto2DatanodeClientProtocolForOneofMigrationTesting.ReadBlockResponseProto
      fixtureReadBlockResp() {
    return Proto2DatanodeClientProtocolForOneofMigrationTesting.ReadBlockResponseProto.newBuilder()
        .setChecksumData(fixtureChecksumData())
        .setOffset(0L)
        .setData(com.google.protobuf.ByteString.copyFrom("d".getBytes()))
        .build();
  }

  // ---------------------------------------------------------------------
  // Production-side sub-message builders (Ratis-thirdparty runtime).
  // ---------------------------------------------------------------------

  private static ContainerProtos.DatanodeBlockID blockId() {
    return ContainerProtos.DatanodeBlockID.newBuilder()
        .setContainerID(1L).setLocalID(1L).build();
  }

  private static ContainerProtos.ChecksumData checksumData() {
    return ContainerProtos.ChecksumData.newBuilder()
        .setType(ContainerProtos.ChecksumType.NONE).setBytesPerChecksum(0).build();
  }

  private static ContainerProtos.ChunkInfo chunkInfo() {
    return ContainerProtos.ChunkInfo.newBuilder()
        .setChunkName("c").setOffset(0L).setLen(0L)
        .setChecksumData(checksumData()).build();
  }

  private static ContainerCommandRequestProto buildProdRequestForArm(int tag) {
    ContainerCommandRequestProto.Builder b = ContainerCommandRequestProto.newBuilder()
        .setCmdType(prodRequestType(tag))
        .setTraceID("tr-" + tag)
        .setContainerID(200L + tag)
        .setDatanodeUuid("dn-" + tag)
        .setPipelineID("pipe-" + tag)
        .setEncodedToken("tok-" + tag)
        .setVersion(1 + tag);
    switch (tag) {
      case 6:
        b.setCreateContainer(ContainerProtos.CreateContainerRequestProto.newBuilder().build());
        break;
      case 12:
        b.setPutBlock(ContainerProtos.PutBlockRequestProto.newBuilder()
            .setBlockData(ContainerProtos.BlockData.newBuilder().setBlockID(blockId()).build())
            .build());
        break;
      case 17:
        b.setWriteChunk(ContainerProtos.WriteChunkRequestProto.newBuilder()
            .setBlockID(blockId()).setChunkData(chunkInfo()).build());
        break;
      case 22:
        b.setGetCommittedBlockLength(ContainerProtos.GetCommittedBlockLengthRequestProto.newBuilder()
            .setBlockID(blockId()).build());
        break;
      case 25:
        b.setFinalizeBlock(ContainerProtos.FinalizeBlockRequestProto.newBuilder()
            .setBlockID(blockId()).build());
        break;
      case 28:
        b.setReadBlock(ContainerProtos.ReadBlockRequestProto.newBuilder()
            .setBlockID(blockId()).setOffset(0L).build());
        break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
    return b.build();
  }

  private static ContainerCommandResponseProto buildProdResponseForArm(int tag) {
    ContainerCommandResponseProto.Builder b = ContainerCommandResponseProto.newBuilder()
        .setCmdType(prodResponseType(tag))
        .setTraceID("tr-r-" + tag)
        .setResult(ContainerProtos.Result.SUCCESS)
        .setMessage("ok-" + tag);
    switch (tag) {
      case 5:
        b.setCreateContainer(ContainerProtos.CreateContainerResponseProto.newBuilder().build());
        break;
      case 11:
        b.setPutBlock(ContainerProtos.PutBlockResponseProto.newBuilder()
            .setCommittedBlockLength(ContainerProtos.GetCommittedBlockLengthResponseProto.newBuilder()
                .setBlockID(blockId()).setBlockLength(0L).build())
            .build());
        break;
      case 15:
        b.setWriteChunk(ContainerProtos.WriteChunkResponseProto.newBuilder().build());
        break;
      case 19:
        b.setPutSmallFile(ContainerProtos.PutSmallFileResponseProto.newBuilder()
            .setCommittedBlockLength(ContainerProtos.GetCommittedBlockLengthResponseProto.newBuilder()
                .setBlockID(blockId()).setBlockLength(0L).build())
            .build());
        break;
      case 22:
        b.setFinalizeBlock(ContainerProtos.FinalizeBlockResponseProto.newBuilder()
            .setBlockData(ContainerProtos.BlockData.newBuilder().setBlockID(blockId()).build())
            .build());
        break;
      case 25:
        b.setReadBlock(ContainerProtos.ReadBlockResponseProto.newBuilder()
            .setChecksumData(checksumData())
            .setOffset(0L)
            .setData(org.apache.ratis.thirdparty.com.google.protobuf.ByteString.copyFrom("d".getBytes()))
            .build());
        break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
    return b.build();
  }

  // ---------------------------------------------------------------------
  // Enum bridges.
  // ---------------------------------------------------------------------

  private static ContainerProtos.Type prodRequestType(int tag) {
    switch (tag) {
      case 6:  return ContainerProtos.Type.CreateContainer;
      case 12: return ContainerProtos.Type.PutBlock;
      case 16: return ContainerProtos.Type.ReadChunk;
      case 17: return ContainerProtos.Type.WriteChunk;
      case 22: return ContainerProtos.Type.GetCommittedBlockLength;
      case 25: return ContainerProtos.Type.FinalizeBlock;
      case 28: return ContainerProtos.Type.ReadBlock;
      default: throw new IllegalArgumentException("tag " + tag);
    }
  }

  private static Proto2DatanodeClientProtocolForOneofMigrationTesting.Type fixtureRequestType(int tag) {
    switch (tag) {
      case 6:  return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.CreateContainer;
      case 12: return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.PutBlock;
      case 16: return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.ReadChunk;
      case 17: return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.WriteChunk;
      case 22: return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.GetCommittedBlockLength;
      case 25: return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.FinalizeBlock;
      case 28: return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.ReadBlock;
      default: throw new IllegalArgumentException("tag " + tag);
    }
  }

  private static ContainerProtos.Type prodResponseType(int tag) {
    switch (tag) {
      case 5:  return ContainerProtos.Type.CreateContainer;
      case 11: return ContainerProtos.Type.PutBlock;
      case 15: return ContainerProtos.Type.WriteChunk;
      case 19: return ContainerProtos.Type.PutSmallFile;
      case 22: return ContainerProtos.Type.FinalizeBlock;
      case 25: return ContainerProtos.Type.ReadBlock;
      default: throw new IllegalArgumentException("tag " + tag);
    }
  }

  private static Proto2DatanodeClientProtocolForOneofMigrationTesting.Type fixtureResponseType(int tag) {
    switch (tag) {
      case 5:  return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.CreateContainer;
      case 11: return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.PutBlock;
      case 15: return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.WriteChunk;
      case 19: return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.PutSmallFile;
      case 22: return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.FinalizeBlock;
      case 25: return Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.ReadBlock;
      default: throw new IllegalArgumentException("tag " + tag);
    }
  }

  private static void assertPayloadArmMatches(
      ContainerCommandRequestProto prod,
      Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandRequestProto fixture,
      int tag) {
    switch (tag) {
      case 6:
        assertTrue(fixture.hasCreateContainer());
        assertArrayEquals(prod.getCreateContainer().toByteArray(),
            fixture.getCreateContainer().toByteArray());
        break;
      case 12:
        assertTrue(fixture.hasPutBlock());
        assertArrayEquals(prod.getPutBlock().toByteArray(),
            fixture.getPutBlock().toByteArray());
        break;
      case 17:
        assertTrue(fixture.hasWriteChunk());
        assertArrayEquals(prod.getWriteChunk().toByteArray(),
            fixture.getWriteChunk().toByteArray());
        break;
      case 22:
        assertTrue(fixture.hasGetCommittedBlockLength());
        assertArrayEquals(prod.getGetCommittedBlockLength().toByteArray(),
            fixture.getGetCommittedBlockLength().toByteArray());
        break;
      case 25:
        assertTrue(fixture.hasFinalizeBlock());
        assertArrayEquals(prod.getFinalizeBlock().toByteArray(),
            fixture.getFinalizeBlock().toByteArray());
        break;
      case 28:
        assertTrue(fixture.hasReadBlock());
        assertArrayEquals(prod.getReadBlock().toByteArray(),
            fixture.getReadBlock().toByteArray());
        break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
  }

  private static void assertResponseArmMatches(
      ContainerCommandResponseProto prod,
      Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandResponseProto fixture,
      int tag) {
    switch (tag) {
      case 5:
        assertTrue(fixture.hasCreateContainer());
        assertArrayEquals(prod.getCreateContainer().toByteArray(),
            fixture.getCreateContainer().toByteArray());
        break;
      case 11:
        assertTrue(fixture.hasPutBlock());
        assertArrayEquals(prod.getPutBlock().toByteArray(),
            fixture.getPutBlock().toByteArray());
        break;
      case 15:
        assertTrue(fixture.hasWriteChunk());
        assertArrayEquals(prod.getWriteChunk().toByteArray(),
            fixture.getWriteChunk().toByteArray());
        break;
      case 19:
        assertTrue(fixture.hasPutSmallFile());
        assertArrayEquals(prod.getPutSmallFile().toByteArray(),
            fixture.getPutSmallFile().toByteArray());
        break;
      case 22:
        assertTrue(fixture.hasFinalizeBlock());
        assertArrayEquals(prod.getFinalizeBlock().toByteArray(),
            fixture.getFinalizeBlock().toByteArray());
        break;
      case 25:
        assertTrue(fixture.hasReadBlock());
        assertArrayEquals(prod.getReadBlock().toByteArray(),
            fixture.getReadBlock().toByteArray());
        break;
      default: throw new IllegalArgumentException("tag " + tag);
    }
  }
}
