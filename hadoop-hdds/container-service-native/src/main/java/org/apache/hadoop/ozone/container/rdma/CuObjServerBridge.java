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
import java.util.Collections;
import org.apache.hadoop.hdds.utils.NativeLibraryLoader;

/**
 * JNI bridge to libcuobjserver (NVIDIA cuObject Server SDK). Every native
 * method maps to a cuObjServer API documented at
 * <a href="https://docs.nvidia.com/gpudirect-storage/cuobject/cuObjServer-api/index.html">[ref-18]</a>.
 *
 * <p>The native library {@code libcuobj_server_bridge.so} is produced by
 * the {@code -Pnative} Maven profile (§4.7.0). When the library is absent
 * (default build, or SDK not installed), {@link #loadLibrary()} returns
 * false and every native method throws {@link UnsatisfiedLinkError} if
 * called.
 */
final class CuObjServerBridge {

  private static final String LIBRARY_NAME = "cuobj_server_bridge";
  private static volatile boolean loaded;
  private static volatile boolean available;

  private CuObjServerBridge() {
  }

  static synchronized boolean loadLibrary() {
    if (loaded) {
      return available;
    }
    loaded = true;
    // NativeLibraryLoader.loadLibrary returns boolean and never throws
    // UnsatisfiedLinkError — it catches Throwable internally (see
    // hadoop-hdds/rocks-native/.../NativeLibraryLoader.java:116-142). We
    // MUST assign the return value; a thrown-exception pattern is wrong.
    available = NativeLibraryLoader.getInstance()
        .loadLibrary(LIBRARY_NAME, Collections.emptyList());
    return available;
  }

  static boolean isAvailable() {
    return available;
  }

  // cuObjServer(ip, port, proto). proto = CUOBJ_PROTO_RDMA_DC_V1 = 1001.
  static native long nativeInit(String bindAddress, int port, int proto)
      throws IOException;

  // ~cuObjServer() destructor.
  static native void nativeShutdown(long handle) throws IOException;

  // cuObjServer::allocHostBuffer(size) -> void*
  static native ByteBuffer nativeAllocHostBuffer(long handle, long size)
      throws IOException;

  // free(ptr) on the pointer returned by allocHostBuffer.
  static native void nativeFreeHostBuffer(ByteBuffer buffer) throws IOException;

  // cuObjServer::registerBuffer(ptr, size) -> struct rdma_buffer*
  static native long nativeRegisterBuffer(long handle, ByteBuffer buffer)
      throws IOException;

  // cuObjServer::deRegisterBuffer(struct rdma_buffer*)  (capital R)
  static native void nativeDeRegisterBuffer(long handle, long rdmaBuffHandle)
      throws IOException;

  // cuObjServer::allocateChannelId() -> uint16_t
  static native int nativeAllocateChannelId(long handle) throws IOException;

  // cuObjServer::freeChannelId(uint16_t)
  static native void nativeFreeChannelId(long handle, int channelId)
      throws IOException;

  // cuObjServer::handleGetObject(key, rdma_buf, remote_offset, size,
  //   rdma_descr, channel, local_offset, nullptr, nullptr)
  static native long nativeHandleGetObject(long handle, String objectKey,
      long rdmaBuffHandle, long remoteOffset, long size,
      String rdmaDescriptor, int channelId, long localOffset)
      throws IOException;
}
