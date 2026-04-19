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

package org.apache.hadoop.ozone.container.rdma;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Abstraction for RDMA data transfer operations.
 *
 * <p>Production implementation uses cuObjServer via JNI
 * ({@code CuObjRdmaTransferService}). Test implementation uses in-process
 * memory copies ({@code MockRdmaTransferService}).
 *
 * <p>Phase 1: read path only ({@link #rdmaWriteToRemote} for GET).
 * Phase 2 will add {@code rdmaReadFromRemote} for PUT.
 *
 * <p>Thread safety: implementations must be thread-safe for concurrent
 * calls to {@link #rdmaWriteToRemote} on different buffers and channels.
 * Concurrent operations on the SAME buffer are NOT safe.
 */
public interface RdmaTransferService extends Closeable {

  /**
   * Initialize the RDMA transfer engine. Maps to
   * {@code new cuObjServer(ip, port, CUOBJ_PROTO_RDMA_DC_V1)}
   * (see cuObjServer API reference).
   *
   * @param bindAddress IP address to bind the cuObjServer listener
   * @param port        cuObjServer listen port for DC transport
   * @throws IOException if initialization fails
   */
  void initialize(String bindAddress, int port) throws IOException;

  /**
   * Allocate pinned (page-locked) host memory suitable for RDMA operations.
   * Maps to {@code cuObjServer::allocHostBuffer(size_t size) -> void*}.
   *
   * @param size number of bytes to allocate
   * @return direct ByteBuffer backed by pinned memory
   * @throws IOException if allocation fails
   */
  ByteBuffer allocHostBuffer(long size) throws IOException;

  /**
   * Free pinned host memory previously allocated by
   * {@link #allocHostBuffer(long)}. Must be called AFTER
   * {@link #deregisterBuffer(long)} for this buffer.
   *
   * @param buffer the direct ByteBuffer returned by allocHostBuffer
   * @throws IOException if the free fails
   */
  void freeHostBuffer(ByteBuffer buffer) throws IOException;

  /**
   * Register a direct ByteBuffer for RDMA operations. Maps to
   * {@code cuObjServer::registerBuffer(void* ptr, size_t size) ->
   * struct rdma_buffer*}.
   *
   * @param buffer direct ByteBuffer to register (must be pinned memory)
   * @return opaque registration handle (wraps the native rdma_buffer pointer)
   */
  long registerBuffer(ByteBuffer buffer) throws IOException;

  /**
   * Deregister a previously registered buffer. Maps to
   * {@code cuObjServer::deRegisterBuffer(struct rdma_buffer*)}.
   */
  void deregisterBuffer(long registrationHandle) throws IOException;

  /**
   * Allocate a channel for concurrent RDMA operations. Maps to
   * {@code cuObjServer::allocateChannelId() -> uint16_t}.
   *
   * @return channel ID in the range 0..65534
   */
  int allocateChannel() throws IOException;

  /**
   * Release a previously allocated channel. Maps to
   * {@code cuObjServer::freeChannelId(uint16_t)}.
   */
  void freeChannel(int channelId) throws IOException;

  /**
   * RDMA_WRITE: push data from a local buffer to remote GPU or host memory.
   * Used for GET operations (datanode sends data to client). Maps to
   * {@code cuObjServer::handleGetObject(...)}; max 1 GiB per call.
   *
   * @param objectKey      S3 object key (used for telemetry in cuObjServer)
   * @param rdmaDescriptor RDMA descriptor string from x-amz-rdma-token header
   * @param bufferHandle   registered buffer handle from registerBuffer
   * @param localOffset    offset within the local RDMA buffer
   * @param remoteOffset   byte offset in the remote buffer (destOffset)
   * @param length         number of bytes to transfer (max 1 GiB)
   * @param channelId      channel ID from allocateChannel
   * @return number of bytes transferred
   * @throws IOException if the RDMA operation fails
   */
  long rdmaWriteToRemote(String objectKey, String rdmaDescriptor,
      long bufferHandle, long localOffset, long remoteOffset,
      long length, int channelId) throws IOException;

  /**
   * @return true if the RDMA transfer engine initialized successfully
   */
  boolean isAvailable();
}
