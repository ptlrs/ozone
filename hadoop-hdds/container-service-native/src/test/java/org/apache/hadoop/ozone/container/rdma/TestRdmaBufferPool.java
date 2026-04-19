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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.hadoop.hdds.conf.ConfigurationSource;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RdmaBufferPool}. Uses an in-memory
 * ConfigurationSource stub so the test does not depend on
 * OzoneConfiguration's XML machinery (woodstox, hadoop-common) — neither
 * of which is on this module's test classpath by design.
 */
class TestRdmaBufferPool {

  /**
   * Minimal key/value config backed by a map. Only implements the
   * abstract methods on {@link ConfigurationSource}; the default methods
   * do the rest (getInt/getLong parse the string via {@link #get}).
   */
  private static final class MapConfig implements ConfigurationSource {
    private final Map<String, String> values = new HashMap<>();

    MapConfig set(String key, String value) {
      values.put(key, value);
      return this;
    }

    MapConfig setInt(String key, int value) {
      return set(key, Integer.toString(value));
    }

    MapConfig setLong(String key, long value) {
      return set(key, Long.toString(value));
    }

    @Override
    public String get(String key) {
      return values.get(key);
    }

    @Override
    public Collection<String> getConfigKeys() {
      return values.keySet();
    }

    @Override
    public char[] getPassword(String key) {
      String v = values.get(key);
      return v == null ? null : v.toCharArray();
    }
  }

  @Test
  void acquireAndReleaseCycleEmptiesAndRefillsPool() throws Exception {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    svc.initialize(null, 0);
    MapConfig conf = new MapConfig()
        .setInt(RdmaBufferPool.BUFFER_SIZE_KEY, 1024)
        .setInt(RdmaBufferPool.BUFFER_COUNT_KEY, 4);
    try (RdmaBufferPool pool = new RdmaBufferPool(conf, svc)) {
      List<RdmaBuffer> buffers = new ArrayList<>();
      for (int i = 0; i < 4; i++) {
        RdmaBuffer b = pool.acquire();
        assertNotNull(b);
        assertNotNull(b.getBuffer());
        assertEquals(1024, b.getBuffer().capacity());
        buffers.add(b);
      }
      assertEquals(0, pool.getAvailableCount());
      assertEquals(4, pool.getTotalCount());
      buffers.forEach(pool::release);
      assertEquals(4, pool.getAvailableCount());
    }
    svc.close();
  }

  @Test
  void acquireBlocksUntilReleaseOrThrowsOnTimeout() throws Exception {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    svc.initialize(null, 0);
    MapConfig conf = new MapConfig()
        .setInt(RdmaBufferPool.BUFFER_SIZE_KEY, 256)
        .setInt(RdmaBufferPool.BUFFER_COUNT_KEY, 1)
        .setLong(RdmaBufferPool.ACQUIRE_TIMEOUT_KEY, 50);
    try (RdmaBufferPool pool = new RdmaBufferPool(conf, svc)) {
      RdmaBuffer held = pool.acquire();
      assertNotNull(held);
      RdmaBufferPoolExhaustedException ex = assertThrows(
          RdmaBufferPoolExhaustedException.class,
          () -> pool.acquire(20, TimeUnit.MILLISECONDS));
      assertNotNull(ex.getMessage());
      pool.release(held);
      RdmaBuffer again = pool.acquire();
      assertSame(held, again);
      pool.release(again);
    }
    svc.close();
  }

  @Test
  void acquirePreservesBufferCapacityAndResetsPosition() throws Exception {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    svc.initialize(null, 0);
    MapConfig conf = new MapConfig()
        .setInt(RdmaBufferPool.BUFFER_SIZE_KEY, 512)
        .setInt(RdmaBufferPool.BUFFER_COUNT_KEY, 2);
    try (RdmaBufferPool pool = new RdmaBufferPool(conf, svc)) {
      RdmaBuffer b = pool.acquire();
      ByteBuffer bb = b.getBuffer();
      bb.position(100);
      bb.limit(200);
      pool.release(b);
      RdmaBuffer b2 = pool.acquire();
      assertEquals(0, b2.getBuffer().position());
      assertEquals(512, b2.getBuffer().limit());
      pool.release(b2);
    }
    svc.close();
  }

  @Test
  void initFailureReleasesAllPinnedBuffers() throws Exception {
    final int maxRegistrations = 2;
    RdmaTransferService throwingAfterN = new MockRdmaTransferService() {
      private int registered;

      @Override
      public long registerBuffer(ByteBuffer buffer) throws IOException {
        if (++registered > maxRegistrations) {
          throw new IOException("simulated registerBuffer failure");
        }
        return super.registerBuffer(buffer);
      }
    };
    throwingAfterN.initialize(null, 0);

    MapConfig conf = new MapConfig()
        .setInt(RdmaBufferPool.BUFFER_SIZE_KEY, 128)
        .setInt(RdmaBufferPool.BUFFER_COUNT_KEY, 4);

    IOException ex = assertThrows(IOException.class,
        () -> new RdmaBufferPool(conf, throwingAfterN));
    assertEquals("simulated registerBuffer failure", ex.getMessage());
    throwingAfterN.close();
  }
}
