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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.hadoop.hdds.conf.ConfigurationSource;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.RdmaReadBlockRequestProto;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.RdmaReadBlockResponseProto;
import org.apache.hadoop.ozone.common.Checksum;
import org.apache.hadoop.ozone.common.ChecksumData;
import org.apache.hadoop.ozone.common.OzoneChecksumException;
import org.apache.hadoop.ozone.container.rdma.RdmaBuffer;
import org.apache.hadoop.ozone.container.rdma.RdmaBufferPool;
import org.apache.hadoop.ozone.container.rdma.RdmaTransferException;
import org.apache.hadoop.ozone.container.rdma.RdmaTransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RDMA transfer engine for the datanode — Phase 1: read path only.
 * Owns the {@link RdmaTransferService} and {@link RdmaBufferPool}; called
 * by {@code KeyValueHandler.handleRdmaReadBlock} for each RdmaReadBlock
 * request.
 */
public class XceiverServerRdma implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(XceiverServerRdma.class);

  public static final String RDMA_ENABLED_KEY = "hdds.datanode.rdma.enabled";
  public static final boolean RDMA_ENABLED_DEFAULT = false;
  public static final String RDMA_BIND_ADDRESS_KEY = "hdds.datanode.rdma.bind.address";
  public static final String RDMA_BIND_ADDRESS_DEFAULT = "0.0.0.0";
  public static final String RDMA_PORT_KEY = "hdds.datanode.rdma.port";
  public static final int RDMA_PORT_DEFAULT = 19870;
  public static final String RDMA_MAX_CONCURRENT_TRANSFERS_KEY =
      "hdds.datanode.rdma.max.concurrent.transfers";
  public static final int RDMA_MAX_CONCURRENT_TRANSFERS_DEFAULT = 32;
  public static final String RDMA_CHECKSUM_ENABLED_KEY =
      "hdds.datanode.rdma.checksum.enabled";
  public static final boolean RDMA_CHECKSUM_ENABLED_DEFAULT = true;
  public static final String RDMA_TRANSFER_SERVICE_CLASS_KEY =
      "hdds.datanode.rdma.transfer.service.class";
  public static final String RDMA_TRANSFER_SERVICE_CLASS_DEFAULT =
      "org.apache.hadoop.ozone.container.rdma.CuObjRdmaTransferService";

  private static final long MAX_RDMA_OP_SIZE = 1L * 1024 * 1024 * 1024;
  private static final long JAVA_SIDE_BLOCK_TIMEOUT_MS = 5L * 60 * 1000;

  private final RdmaTransferService transferService;
  private final RdmaBufferPool bufferPool;
  private final int rdmaPort;
  private final Semaphore transferSemaphore;
  private final boolean checksumVerificationEnabled;

  public XceiverServerRdma(ConfigurationSource conf,
      RdmaTransferService transferService) throws IOException {
    this.transferService = transferService;
    this.rdmaPort = conf.getInt(RDMA_PORT_KEY, RDMA_PORT_DEFAULT);

    int maxConcurrentTransfers = conf.getInt(
        RDMA_MAX_CONCURRENT_TRANSFERS_KEY,
        RDMA_MAX_CONCURRENT_TRANSFERS_DEFAULT);
    this.transferSemaphore = new Semaphore(maxConcurrentTransfers);
    this.checksumVerificationEnabled = conf.getBoolean(
        RDMA_CHECKSUM_ENABLED_KEY, RDMA_CHECKSUM_ENABLED_DEFAULT);

    String bindAddress = conf.get(RDMA_BIND_ADDRESS_KEY, RDMA_BIND_ADDRESS_DEFAULT);

    transferService.initialize(bindAddress, rdmaPort);
    this.bufferPool = new RdmaBufferPool(conf, transferService);

    LOG.info("XceiverServerRdma started: bind={}, port={}, checksumVerification={}",
        bindAddress, rdmaPort, checksumVerificationEnabled);
  }

  /**
   * Handle an RDMA read (S3 GET): read block data from disk, verify each
   * stored chunk's checksum, and RDMA_WRITE to the client. Per stored chunk:
   * (1) read the whole chunk into the RDMA buffer, (2) verify stored CRC
   * (skipped when {@code hdds.datanode.rdma.checksum.enabled=false} or when
   * the chunk's ChecksumType is NONE), (3) RDMA_WRITE the requested range
   * subset of the chunk, (4) append the stored ChecksumData to
   * perChunkChecksum so the client can re-verify post-DMA.
   *
   * <p>Requirement: RDMA buffer size must be ≥ the largest stored chunk.
   * Phase 1 does not support reads that span a stored chunk across multiple
   * RDMA-buffer reads.
   *
   * @param storedChunks block's write-time chunks in order; pass an empty
   *     list only for blocks written with CHECKSUM_NONE. Must not be null.
   */
  public RdmaReadBlockResponseProto handleRdmaRead(
      RdmaReadBlockRequestProto request,
      FileChannel blockFile,
      long blockSize,
      List<ContainerProtos.ChunkInfo> storedChunks)
      throws IOException, InterruptedException {

    final String rdmaDescriptor = request.getRdmaToken();
    final long offset = request.hasOffset() ? request.getOffset() : 0;
    final long requestedLength = request.hasLength() && request.getLength() > 0
        ? request.getLength()
        : (blockSize - offset);
    final long transferLength = Math.min(requestedLength, blockSize - offset);
    final long destOffset = request.hasDestOffset() ? request.getDestOffset() : 0;

    if (transferLength <= 0) {
      return RdmaReadBlockResponseProto.newBuilder()
          .setBytesTransferred(0)
          .build();
    }

    if (!transferSemaphore.tryAcquire(
        bufferPool.getAcquireTimeoutMs(), TimeUnit.MILLISECONDS)) {
      // Kept as IOException: semaphore exhaustion is load shedding, not a
      // transfer failure, and maps to IO_EXCEPTION at the handler boundary.
      throw new IOException("RDMA transfer semaphore exhausted");
    }

    int channelId = -1;
    RdmaBuffer rdmaBuffer = null;
    try {
      channelId = transferService.allocateChannel();
      rdmaBuffer = bufferPool.acquire();
      final ByteBuffer buffer = rdmaBuffer.getBuffer();
      final int bufSize = buffer.capacity();
      long totalTransferred = 0;

      final long rangeStart = offset;
      final long rangeEnd = offset + transferLength;
      final long deadline = System.nanoTime()
          + TimeUnit.MILLISECONDS.toNanos(JAVA_SIDE_BLOCK_TIMEOUT_MS);

      final RdmaReadBlockResponseProto.Builder responseBuilder =
          RdmaReadBlockResponseProto.newBuilder();

      for (ContainerProtos.ChunkInfo storedChunk : storedChunks) {
        final long chunkStart = storedChunk.getOffset();
        final long chunkEnd = chunkStart + storedChunk.getLen();

        if (chunkEnd <= rangeStart) {
          continue;
        }
        if (chunkStart >= rangeEnd) {
          break;
        }

        if (System.nanoTime() > deadline) {
          throw new IOException("RDMA transfer timed out after "
              + JAVA_SIDE_BLOCK_TIMEOUT_MS + "ms for block "
              + request.getBlockID());
        }

        final int chunkLen = Math.toIntExact(storedChunk.getLen());
        if (chunkLen > bufSize) {
          throw new IOException("RDMA buffer size (" + bufSize
              + " B) is smaller than a stored chunk (" + chunkLen
              + " B). Raise " + RdmaBufferPool.BUFFER_SIZE_KEY
              + " to at least the block's bytesPerChunk.");
        }
        if (chunkLen > MAX_RDMA_OP_SIZE) {
          throw new IOException("Stored chunk (" + chunkLen
              + " B) exceeds cuObjServer per-op limit (" + MAX_RDMA_OP_SIZE
              + " B).");
        }

        buffer.clear();
        buffer.limit(chunkLen);
        int bytesRead = 0;
        while (bytesRead < chunkLen) {
          int n = blockFile.read(buffer, chunkStart + bytesRead);
          if (n < 0) {
            break;
          }
          bytesRead += n;
        }
        if (bytesRead != chunkLen) {
          throw new IOException("Short read on stored chunk for block "
              + request.getBlockID() + " at offset " + chunkStart
              + ": expected " + chunkLen + " got " + bytesRead);
        }

        final ContainerProtos.ChecksumData storedCsProto =
            storedChunk.getChecksumData();
        if (checksumVerificationEnabled
            && storedCsProto.getType() != ContainerProtos.ChecksumType.NONE) {
          buffer.flip();
          try {
            Checksum.verifyChecksum(
                buffer, ChecksumData.getFromProtoBuf(storedCsProto), 0);
          } catch (OzoneChecksumException e) {
            throw new IOException(
                "Stored-checksum verification FAILED for block "
                    + request.getBlockID() + " chunk offset " + chunkStart
                    + " — disk corruption suspected; RDMA_WRITE skipped",
                e);
          }
          buffer.position(0).limit(chunkLen);
        }

        final long clipFrom = Math.max(chunkStart, rangeStart);
        final long clipTo = Math.min(chunkEnd, rangeEnd);
        final long bufClipOffset = clipFrom - chunkStart;
        final long bufClipLen = clipTo - clipFrom;

        long rdmaSent = 0;
        while (rdmaSent < bufClipLen) {
          long written = transferService.rdmaWriteToRemote(
              request.getBlockID().toString(),
              rdmaDescriptor,
              rdmaBuffer.getRegistrationHandle(),
              bufClipOffset + rdmaSent,
              destOffset + totalTransferred + rdmaSent,
              bufClipLen - rdmaSent,
              channelId);
          if (written <= 0) {
            throw new RdmaTransferException(
                "handleGetObject returned " + written);
          }
          rdmaSent += written;
        }

        responseBuilder.addPerChunkChecksum(storedCsProto);
        totalTransferred += bufClipLen;
      }

      responseBuilder.setBytesTransferred(totalTransferred);
      responseBuilder.setClientBufferOffset(destOffset);
      return responseBuilder.build();
    } finally {
      if (rdmaBuffer != null) {
        try {
          bufferPool.release(rdmaBuffer);
        } catch (RuntimeException e) {
          LOG.warn("Failed to release RDMA buffer", e);
        }
      }
      if (channelId >= 0) {
        try {
          transferService.freeChannel(channelId);
        } catch (RuntimeException | IOException e) {
          LOG.warn("Failed to free RDMA channel", e);
        }
      }
      transferSemaphore.release();
    }
  }

  public int getRdmaPort() {
    return rdmaPort;
  }

  public boolean isAvailable() {
    return transferService.isAvailable();
  }

  public RdmaBufferPool getBufferPool() {
    return bufferPool;
  }

  public RdmaTransferService getTransferService() {
    return transferService;
  }

  @Override
  public void close() throws IOException {
    bufferPool.close();
    transferService.close();
    LOG.info("XceiverServerRdma closed");
  }
}
