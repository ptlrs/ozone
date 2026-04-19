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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.hadoop.hdds.conf.ConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pool of pre-allocated, RDMA-registered direct ByteBuffers.
 *
 * <p>Config keys:
 * <ul>
 *   <li>{@code hdds.datanode.rdma.buffer.size} (default: 4 MiB)
 *   <li>{@code hdds.datanode.rdma.buffer.count} (default: 64)
 *   <li>{@code hdds.datanode.rdma.buffer.acquire.timeout.ms} (default: 5000)
 * </ul>
 */
public class RdmaBufferPool implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(RdmaBufferPool.class);

  public static final String BUFFER_SIZE_KEY = "hdds.datanode.rdma.buffer.size";
  public static final int BUFFER_SIZE_DEFAULT = 4 * 1024 * 1024;
  public static final String BUFFER_COUNT_KEY = "hdds.datanode.rdma.buffer.count";
  public static final int BUFFER_COUNT_DEFAULT = 64;
  public static final String ACQUIRE_TIMEOUT_KEY =
      "hdds.datanode.rdma.buffer.acquire.timeout.ms";
  public static final long ACQUIRE_TIMEOUT_DEFAULT = 5000;

  private final BlockingQueue<RdmaBuffer> pool;
  private final List<RdmaBuffer> allBuffers;
  private final RdmaTransferService transferService;
  private final int bufferSize;
  private final int bufferCount;
  private final long acquireTimeoutMs;

  public RdmaBufferPool(ConfigurationSource conf,
      RdmaTransferService transferService) throws IOException {
    this.transferService = transferService;
    this.bufferSize = conf.getInt(BUFFER_SIZE_KEY, BUFFER_SIZE_DEFAULT);
    this.bufferCount = conf.getInt(BUFFER_COUNT_KEY, BUFFER_COUNT_DEFAULT);
    this.acquireTimeoutMs = conf.getLong(ACQUIRE_TIMEOUT_KEY, ACQUIRE_TIMEOUT_DEFAULT);
    this.pool = new ArrayBlockingQueue<>(bufferCount);
    this.allBuffers = new ArrayList<>(bufferCount);

    // Partial-initialization cleanup: if registerBuffer throws after
    // allocHostBuffer succeeded, the just-allocated pinned buffer — and
    // every fully-registered buffer before it — must be released. Without
    // this, a failure at buffer N leaks N-1 pinned pages into the kernel.
    ByteBuffer lastAllocated = null;
    try {
      for (int i = 0; i < bufferCount; i++) {
        lastAllocated = null;
        ByteBuffer buf = transferService.allocHostBuffer(bufferSize);
        lastAllocated = buf;
        long regHandle = transferService.registerBuffer(buf);
        lastAllocated = null;
        RdmaBuffer rdmaBuffer = new RdmaBuffer(buf, regHandle);
        allBuffers.add(rdmaBuffer);
        pool.offer(rdmaBuffer);
      }
    } catch (RuntimeException | IOException e) {
      if (lastAllocated != null) {
        try {
          transferService.freeHostBuffer(lastAllocated);
        } catch (IOException ex) {
          LOG.warn("Failed to free partial buffer", ex);
        }
      }
      for (RdmaBuffer rb : allBuffers) {
        try {
          transferService.deregisterBuffer(rb.getRegistrationHandle());
        } catch (IOException ex) {
          LOG.warn("Failed to deregister during cleanup", ex);
        }
        try {
          transferService.freeHostBuffer(rb.getBuffer());
        } catch (IOException ex) {
          LOG.warn("Failed to free during cleanup", ex);
        }
      }
      allBuffers.clear();
      pool.clear();
      throw e;
    }
    LOG.info("RdmaBufferPool initialized: {} buffers x {} bytes = {} MiB total",
        bufferCount, bufferSize, (long) bufferCount * bufferSize / (1024 * 1024));
  }

  public RdmaBuffer acquire(long timeout, TimeUnit unit)
      throws IOException, InterruptedException {
    RdmaBuffer buf = pool.poll(timeout, unit);
    if (buf == null) {
      throw new RdmaBufferPoolExhaustedException(
          "RDMA buffer pool exhausted: all " + bufferCount + " buffers in use.");
    }
    buf.reset();
    return buf;
  }

  public RdmaBuffer acquire() throws IOException, InterruptedException {
    return acquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
  }

  public void release(RdmaBuffer buffer) {
    if (buffer != null) {
      buffer.reset();
      pool.offer(buffer);
    }
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public int getAvailableCount() {
    return pool.size();
  }

  public int getTotalCount() {
    return bufferCount;
  }

  public long getAcquireTimeoutMs() {
    return acquireTimeoutMs;
  }

  @Override
  public void close() throws IOException {
    for (RdmaBuffer buf : allBuffers) {
      try {
        transferService.deregisterBuffer(buf.getRegistrationHandle());
      } catch (IOException e) {
        LOG.warn("Failed to deregister RDMA buffer", e);
      }
      try {
        transferService.freeHostBuffer(buf.getBuffer());
      } catch (IOException e) {
        LOG.warn("Failed to free pinned host buffer", e);
      }
    }
    allBuffers.clear();
    pool.clear();
  }
}
