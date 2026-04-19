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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock RdmaTransferService for tests that do not have RDMA hardware or a
 * cuObjServer SDK. All operations are simulated as local memory copies
 * between heap ByteBuffers; no NIC DMA, no pinned memory, no RDMA verbs
 * are involved. Performance numbers from this impl are meaningless.
 */
public class MockRdmaTransferService implements RdmaTransferService {

  private static final Logger LOG =
      LoggerFactory.getLogger(MockRdmaTransferService.class);

  private final AtomicLong handleCounter = new AtomicLong(1);
  private final AtomicInteger channelCounter = new AtomicInteger(1);
  private final Map<Long, ByteBuffer> registeredBuffers =
      new ConcurrentHashMap<>();
  private final Map<String, ByteBuffer> remoteBuffers =
      new ConcurrentHashMap<>();
  private volatile boolean initialized;

  /**
   * Register a simulated remote buffer for testing. Callers associate an
   * arbitrary rdma descriptor string with a ByteBuffer; rdmaWriteToRemote
   * later copies data into this buffer when asked to write to that
   * descriptor.
   */
  public void registerRemoteBuffer(String rdmaDescriptor, ByteBuffer buffer) {
    remoteBuffers.put(rdmaDescriptor, buffer);
  }

  @Override
  public void initialize(String bindAddress, int port) throws IOException {
    initialized = true;
    LOG.info("MockRdmaTransferService initialized (SIMULATION - no real RDMA)");
  }

  @Override
  public ByteBuffer allocHostBuffer(long size) throws IOException {
    return ByteBuffer.allocateDirect(Math.toIntExact(size));
  }

  @Override
  public void freeHostBuffer(ByteBuffer buffer) throws IOException {
    // Heap-allocated direct buffers are reclaimed by the JVM.
  }

  @Override
  public long registerBuffer(ByteBuffer buffer) throws IOException {
    long handle = handleCounter.getAndIncrement();
    registeredBuffers.put(handle, buffer);
    return handle;
  }

  @Override
  public void deregisterBuffer(long registrationHandle) throws IOException {
    registeredBuffers.remove(registrationHandle);
  }

  @Override
  public int allocateChannel() throws IOException {
    return channelCounter.getAndIncrement();
  }

  @Override
  public void freeChannel(int channelId) throws IOException {
    // No persistent channel state in the mock.
  }

  @Override
  public long rdmaWriteToRemote(String objectKey, String rdmaDescriptor,
      long bufferHandle, long localOffset, long remoteOffset,
      long length, int channelId) throws IOException {
    ByteBuffer local = registeredBuffers.get(bufferHandle);
    if (local == null) {
      throw new IOException("No registered buffer for handle: " + bufferHandle);
    }
    ByteBuffer remote = remoteBuffers.get(rdmaDescriptor);
    if (remote == null) {
      throw new IOException("No remote buffer registered for RDMA descriptor: "
          + rdmaDescriptor + ". In tests, call registerRemoteBuffer() first.");
    }
    int len = Math.toIntExact(length);
    byte[] data = new byte[len];
    ByteBuffer localView = local.duplicate();
    localView.position(Math.toIntExact(localOffset));
    localView.get(data, 0, len);
    synchronized (remote) {
      ByteBuffer remoteView = remote.duplicate();
      remoteView.position(Math.toIntExact(remoteOffset));
      remoteView.put(data, 0, len);
    }
    return len;
  }

  @Override
  public boolean isAvailable() {
    return initialized;
  }

  @Override
  public void close() throws IOException {
    initialized = false;
    registeredBuffers.clear();
    remoteBuffers.clear();
  }
}
