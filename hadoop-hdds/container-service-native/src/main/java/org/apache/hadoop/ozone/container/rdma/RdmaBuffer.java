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

import java.nio.ByteBuffer;

/**
 * A single RDMA-registered buffer paired with its registration handle.
 * Backed by pinned host memory allocated via
 * {@code cuObjServer::allocHostBuffer()} in the production transfer
 * service; backed by a plain direct ByteBuffer in the mock path.
 */
public class RdmaBuffer {

  private final ByteBuffer buffer;
  private final long registrationHandle;

  RdmaBuffer(ByteBuffer buffer, long registrationHandle) {
    this.buffer = buffer;
    this.registrationHandle = registrationHandle;
  }

  public ByteBuffer getBuffer() {
    return buffer;
  }

  public long getRegistrationHandle() {
    return registrationHandle;
  }

  public void reset() {
    buffer.clear();
  }
}
