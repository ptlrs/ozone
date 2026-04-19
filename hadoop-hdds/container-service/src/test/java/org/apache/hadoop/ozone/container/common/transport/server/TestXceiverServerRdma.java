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

package org.apache.hadoop.ozone.container.common.transport.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.apache.hadoop.hdds.conf.OzoneConfiguration;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.DatanodeBlockID;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.RdmaReadBlockRequestProto;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.RdmaReadBlockResponseProto;
import org.apache.hadoop.ozone.common.Checksum;
import org.apache.hadoop.ozone.common.ChecksumData;
import org.apache.hadoop.ozone.container.rdma.MockRdmaTransferService;
import org.apache.hadoop.ozone.container.rdma.RdmaBufferPool;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link XceiverServerRdma} using
 * {@link MockRdmaTransferService}.
 */
class TestXceiverServerRdma {

  private static ContainerProtos.ChunkInfo syntheticChunk(long offset, long len) {
    return ContainerProtos.ChunkInfo.newBuilder()
        .setChunkName("test-chunk-" + offset)
        .setOffset(offset)
        .setLen(len)
        .setChecksumData(ContainerProtos.ChecksumData.newBuilder()
            .setType(ContainerProtos.ChecksumType.NONE)
            .setBytesPerChecksum(0)
            .build())
        .build();
  }

  private static OzoneConfiguration smallPoolConf(int bufferSize, int bufferCount) {
    OzoneConfiguration conf = new OzoneConfiguration();
    conf.setInt(RdmaBufferPool.BUFFER_SIZE_KEY, bufferSize);
    conf.setInt(RdmaBufferPool.BUFFER_COUNT_KEY, bufferCount);
    return conf;
  }

  @Test
  void handleRdmaReadSingleChunkTransfersFullBlock() throws Exception {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    ByteBuffer remote = ByteBuffer.allocateDirect(4096);
    svc.registerRemoteBuffer("tok-1", remote);
    XceiverServerRdma rdma = new XceiverServerRdma(smallPoolConf(4096, 2), svc);

    byte[] data = new byte[1024];
    Arrays.fill(data, (byte) 0xAB);
    Path tmp = Files.createTempFile("rdma-test", ".block");
    Files.write(tmp, data);

    RdmaReadBlockRequestProto request = RdmaReadBlockRequestProto.newBuilder()
        .setBlockID(DatanodeBlockID.newBuilder().setContainerID(1).setLocalID(1))
        .setRdmaToken("tok-1")
        .build();
    List<ContainerProtos.ChunkInfo> chunks =
        Collections.singletonList(syntheticChunk(0, data.length));

    try (FileChannel fc = FileChannel.open(tmp, StandardOpenOption.READ)) {
      RdmaReadBlockResponseProto resp =
          rdma.handleRdmaRead(request, fc, data.length, chunks);
      assertEquals(data.length, resp.getBytesTransferred());
      byte[] seen = new byte[data.length];
      remote.position(0);
      remote.get(seen);
      assertArrayEquals(data, seen);
    } finally {
      rdma.close();
      Files.deleteIfExists(tmp);
    }
  }

  @Test
  void handleRdmaReadHonoursDestOffset() throws Exception {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    ByteBuffer remote = ByteBuffer.allocateDirect(4096);
    svc.registerRemoteBuffer("tok-off", remote);
    XceiverServerRdma rdma = new XceiverServerRdma(smallPoolConf(4096, 2), svc);

    byte[] data = new byte[512];
    Arrays.fill(data, (byte) 0xCD);
    Path tmp = Files.createTempFile("rdma-dest", ".block");
    Files.write(tmp, data);

    RdmaReadBlockRequestProto request = RdmaReadBlockRequestProto.newBuilder()
        .setBlockID(DatanodeBlockID.newBuilder().setContainerID(1).setLocalID(2))
        .setRdmaToken("tok-off")
        .setDestOffset(1024)
        .build();
    List<ContainerProtos.ChunkInfo> chunks =
        Collections.singletonList(syntheticChunk(0, data.length));

    try (FileChannel fc = FileChannel.open(tmp, StandardOpenOption.READ)) {
      RdmaReadBlockResponseProto resp =
          rdma.handleRdmaRead(request, fc, data.length, chunks);
      assertEquals(data.length, resp.getBytesTransferred());
      byte[] seen = new byte[data.length];
      remote.position(1024);
      remote.get(seen);
      assertArrayEquals(data, seen);
    } finally {
      rdma.close();
      Files.deleteIfExists(tmp);
    }
  }

  @Test
  void handleRdmaReadTransfersMultipleStoredChunks() throws Exception {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    ByteBuffer remote = ByteBuffer.allocateDirect(16 * 1024);
    svc.registerRemoteBuffer("tok-multi", remote);
    // Buffer smaller than block total so the loop genuinely iterates.
    XceiverServerRdma rdma = new XceiverServerRdma(smallPoolConf(4096, 2), svc);

    byte[] data = new byte[16 * 1024];
    new Random(42).nextBytes(data);
    Path tmp = Files.createTempFile("rdma-multi", ".block");
    Files.write(tmp, data);

    RdmaReadBlockRequestProto request = RdmaReadBlockRequestProto.newBuilder()
        .setBlockID(DatanodeBlockID.newBuilder().setContainerID(1).setLocalID(3))
        .setRdmaToken("tok-multi")
        .build();
    List<ContainerProtos.ChunkInfo> chunks = Arrays.asList(
        syntheticChunk(0, 4096),
        syntheticChunk(4096, 4096),
        syntheticChunk(8192, 4096),
        syntheticChunk(12288, 4096));

    try (FileChannel fc = FileChannel.open(tmp, StandardOpenOption.READ)) {
      RdmaReadBlockResponseProto resp =
          rdma.handleRdmaRead(request, fc, data.length, chunks);
      assertEquals(data.length, resp.getBytesTransferred());
      assertEquals(4, resp.getPerChunkChecksumCount());
      byte[] seen = new byte[data.length];
      remote.position(0);
      remote.get(seen);
      assertArrayEquals(data, seen);
    } finally {
      rdma.close();
      Files.deleteIfExists(tmp);
    }
  }

  @Test
  void unknownRdmaTokenSurfacesIOException() throws Exception {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    // Deliberately do NOT register the remote buffer.
    XceiverServerRdma rdma = new XceiverServerRdma(smallPoolConf(4096, 2), svc);

    byte[] data = new byte[512];
    Path tmp = Files.createTempFile("rdma-unk", ".block");
    Files.write(tmp, data);

    RdmaReadBlockRequestProto request = RdmaReadBlockRequestProto.newBuilder()
        .setBlockID(DatanodeBlockID.newBuilder().setContainerID(1).setLocalID(4))
        .setRdmaToken("no-such-token")
        .build();

    try (FileChannel fc = FileChannel.open(tmp, StandardOpenOption.READ)) {
      assertThrows(IOException.class, () -> rdma.handleRdmaRead(request, fc,
          data.length, Collections.singletonList(syntheticChunk(0, data.length))));
    } finally {
      rdma.close();
      Files.deleteIfExists(tmp);
    }
  }

  @Test
  void storedChecksumMismatchAbortsBeforeRdmaWrite() throws Exception {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    ByteBuffer remote = ByteBuffer.allocateDirect(4096);
    svc.registerRemoteBuffer("tok-bad-crc", remote);
    XceiverServerRdma rdma = new XceiverServerRdma(smallPoolConf(4096, 2), svc);

    byte[] onDisk = new byte[1024];
    Arrays.fill(onDisk, (byte) 0x11);
    Path tmp = Files.createTempFile("rdma-bad-crc", ".block");
    Files.write(tmp, onDisk);

    // Build a stored ChunkInfo whose CRC32C corresponds to all-zero bytes.
    byte[] fakeBytes = new byte[1024];
    ChecksumData fakeCs =
        new Checksum(ContainerProtos.ChecksumType.CRC32C, 1024)
            .computeChecksum(fakeBytes);
    ContainerProtos.ChunkInfo chunk = ContainerProtos.ChunkInfo.newBuilder()
        .setChunkName("bad-crc")
        .setOffset(0)
        .setLen(1024)
        .setChecksumData(fakeCs.getProtoBufMessage())
        .build();

    RdmaReadBlockRequestProto request = RdmaReadBlockRequestProto.newBuilder()
        .setBlockID(DatanodeBlockID.newBuilder().setContainerID(1).setLocalID(5))
        .setRdmaToken("tok-bad-crc")
        .build();

    try (FileChannel fc = FileChannel.open(tmp, StandardOpenOption.READ)) {
      assertThrows(IOException.class, () -> rdma.handleRdmaRead(
          request, fc, onDisk.length, Collections.singletonList(chunk)));
    } finally {
      rdma.close();
      Files.deleteIfExists(tmp);
    }

    // Remote buffer must still be zero — no partial RDMA_WRITE leaked out.
    byte[] seen = new byte[1024];
    remote.position(0);
    remote.get(seen);
    assertArrayEquals(new byte[1024], seen);
  }
}
