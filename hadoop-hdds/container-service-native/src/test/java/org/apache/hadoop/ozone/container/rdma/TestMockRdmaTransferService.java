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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MockRdmaTransferService}.
 */
class TestMockRdmaTransferService {

  @Test
  void initializeFlipsAvailability() throws IOException {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    assertFalse(svc.isAvailable());
    svc.initialize(null, 0);
    assertTrue(svc.isAvailable());
    svc.close();
    assertFalse(svc.isAvailable());
  }

  @Test
  void registerAndDeregisterBufferRoundTrip() throws IOException {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    svc.initialize(null, 0);
    ByteBuffer buf = svc.allocHostBuffer(64);
    long handle = svc.registerBuffer(buf);
    assertNotEquals(0L, handle);
    svc.deregisterBuffer(handle);
    svc.close();
  }

  @Test
  void allocateChannelReturnsUniqueIds() throws IOException {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    svc.initialize(null, 0);
    int c1 = svc.allocateChannel();
    int c2 = svc.allocateChannel();
    assertNotEquals(c1, c2);
    svc.freeChannel(c1);
    svc.freeChannel(c2);
    svc.close();
  }

  @Test
  void rdmaWriteToRemoteCopiesBytes() throws IOException {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    svc.initialize(null, 0);

    ByteBuffer local = svc.allocHostBuffer(32);
    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8};
    local.position(0);
    local.put(data);
    long handle = svc.registerBuffer(local);

    ByteBuffer remote = ByteBuffer.allocateDirect(32);
    svc.registerRemoteBuffer("tok-1", remote);

    long written = svc.rdmaWriteToRemote(
        "obj", "tok-1", handle, 0, 4, data.length, svc.allocateChannel());
    assertEquals(data.length, written);

    byte[] seen = new byte[data.length];
    remote.position(4);
    remote.get(seen);
    assertArrayEquals(data, seen);

    // Bytes outside the written range must stay zero.
    byte[] zeros = new byte[4];
    remote.position(0);
    byte[] head = new byte[4];
    remote.get(head);
    assertArrayEquals(zeros, head);
    svc.close();
  }

  @Test
  void rdmaWriteUnknownDescriptorFails() throws IOException {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    svc.initialize(null, 0);
    ByteBuffer local = svc.allocHostBuffer(16);
    long handle = svc.registerBuffer(local);
    int channel = svc.allocateChannel();
    assertThrows(IOException.class, () ->
        svc.rdmaWriteToRemote("obj", "no-such-token",
            handle, 0, 0, 4, channel));
    svc.close();
  }

  @Test
  void rdmaWriteUnknownBufferHandleFails() throws IOException {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    svc.initialize(null, 0);
    svc.registerRemoteBuffer("tok", ByteBuffer.allocateDirect(8));
    int channel = svc.allocateChannel();
    assertThrows(IOException.class, () ->
        svc.rdmaWriteToRemote("obj", "tok", 0xDEADBEEFL, 0, 0, 4, channel));
    svc.close();
  }

  @Test
  void closeClearsRegisteredAndRemoteBuffers() throws IOException {
    MockRdmaTransferService svc = new MockRdmaTransferService();
    svc.initialize(null, 0);
    ByteBuffer local = svc.allocHostBuffer(8);
    long handle = svc.registerBuffer(local);
    svc.registerRemoteBuffer("tok", ByteBuffer.allocateDirect(8));
    svc.close();
    // After close, the mock drops all registrations — another write must
    // fail with the "no registered buffer" message rather than silently
    // succeeding against stale state.
    IOException ex = assertThrows(IOException.class, () ->
        svc.rdmaWriteToRemote("obj", "tok", handle, 0, 0, 4, 1));
    assertTrue(ex.getMessage().contains("No registered buffer"),
        "unexpected message: " + ex.getMessage());
  }
}
