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

// JNI bridge to NVIDIA cuObjServer. Each extern "C" function below
// is the native implementation of one of the CuObjServerBridge Java
// methods. Mapping between Java name and function name follows the
// JNI convention: Java_<mangled-package>_<class>_<method>.
//
// Verified against /usr/include/cuobjserver.h from libcuobjserver-dev
// 1.0.0.26-1 (Apr 2026). Signatures match spec v12.5 §4.6.

#include <jni.h>
#include <cstdint>
#include <cstdlib>   // free() in nativeFreeHostBuffer
#include <cstring>
#include <string>
#include <stdexcept>

#include <cuobjserver.h>  // NVIDIA cuObject Server SDK

namespace {

void throwIOException(JNIEnv *env, const char *msg) {
  jclass ioExClass = env->FindClass("java/io/IOException");
  if (ioExClass != nullptr) {
    env->ThrowNew(ioExClass, msg);
  }
}

std::string jstringToStdString(JNIEnv *env, jstring jstr) {
  if (jstr == nullptr) {
    return "";
  }
  const char *chars = env->GetStringUTFChars(jstr, nullptr);
  std::string result(chars);
  env->ReleaseStringUTFChars(jstr, chars);
  return result;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_org_apache_hadoop_ozone_container_rdma_CuObjServerBridge_nativeInit(
    JNIEnv *env, jclass cls,
    jstring bindAddress, jint port, jint proto) {
  try {
    std::string ip = jstringToStdString(env, bindAddress);
    auto *server = new cuObjServer(
        ip.c_str(),
        static_cast<unsigned short>(port),
        static_cast<unsigned>(proto));
    return reinterpret_cast<jlong>(server);
  } catch (const std::exception &e) {
    throwIOException(env, e.what());
    return 0;
  }
}

extern "C" JNIEXPORT void JNICALL
Java_org_apache_hadoop_ozone_container_rdma_CuObjServerBridge_nativeShutdown(
    JNIEnv *env, jclass cls, jlong handle) {
  try {
    delete reinterpret_cast<cuObjServer *>(handle);
  } catch (const std::exception &e) {
    throwIOException(env, e.what());
  }
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_apache_hadoop_ozone_container_rdma_CuObjServerBridge_nativeAllocHostBuffer(
    JNIEnv *env, jclass cls, jlong handle, jlong size) {
  auto *server = reinterpret_cast<cuObjServer *>(handle);
  try {
    void *ptr = server->allocHostBuffer(static_cast<size_t>(size));
    if (ptr == nullptr) {
      throwIOException(env, "allocHostBuffer returned null");
      return nullptr;
    }
    return env->NewDirectByteBuffer(ptr, size);
  } catch (const std::exception &e) {
    throwIOException(env, e.what());
    return nullptr;
  }
}

extern "C" JNIEXPORT void JNICALL
Java_org_apache_hadoop_ozone_container_rdma_CuObjServerBridge_nativeFreeHostBuffer(
    JNIEnv *env, jclass cls, jobject buffer) {
  void *ptr = env->GetDirectBufferAddress(buffer);
  if (ptr == nullptr) {
    throwIOException(env, "Buffer is not a direct ByteBuffer");
    return;
  }
  std::free(ptr);
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_apache_hadoop_ozone_container_rdma_CuObjServerBridge_nativeRegisterBuffer(
    JNIEnv *env, jclass cls, jlong handle, jobject buffer) {
  auto *server = reinterpret_cast<cuObjServer *>(handle);
  void *ptr = env->GetDirectBufferAddress(buffer);
  if (ptr == nullptr) {
    throwIOException(env, "Buffer is not a direct ByteBuffer");
    return 0;
  }
  jlong capacity = env->GetDirectBufferCapacity(buffer);
  try {
    struct rdma_buffer *rdma_buf = server->registerBuffer(
        ptr, static_cast<size_t>(capacity));
    return reinterpret_cast<jlong>(rdma_buf);
  } catch (const std::exception &e) {
    throwIOException(env, e.what());
    return 0;
  }
}

extern "C" JNIEXPORT void JNICALL
Java_org_apache_hadoop_ozone_container_rdma_CuObjServerBridge_nativeDeRegisterBuffer(
    JNIEnv *env, jclass cls, jlong handle, jlong rdmaBuffHandle) {
  auto *server = reinterpret_cast<cuObjServer *>(handle);
  try {
    server->deRegisterBuffer(
        reinterpret_cast<struct rdma_buffer *>(rdmaBuffHandle));
  } catch (const std::exception &e) {
    throwIOException(env, e.what());
  }
}

extern "C" JNIEXPORT jint JNICALL
Java_org_apache_hadoop_ozone_container_rdma_CuObjServerBridge_nativeAllocateChannelId(
    JNIEnv *env, jclass cls, jlong handle) {
  auto *server = reinterpret_cast<cuObjServer *>(handle);
  try {
    uint16_t channelId = server->allocateChannelId();
    return static_cast<jint>(channelId);
  } catch (const std::exception &e) {
    throwIOException(env, e.what());
    return -1;
  }
}

extern "C" JNIEXPORT void JNICALL
Java_org_apache_hadoop_ozone_container_rdma_CuObjServerBridge_nativeFreeChannelId(
    JNIEnv *env, jclass cls, jlong handle, jint channelId) {
  auto *server = reinterpret_cast<cuObjServer *>(handle);
  try {
    server->freeChannelId(static_cast<uint16_t>(channelId));
  } catch (const std::exception &e) {
    throwIOException(env, e.what());
  }
}

extern "C" JNIEXPORT jlong JNICALL
Java_org_apache_hadoop_ozone_container_rdma_CuObjServerBridge_nativeHandleGetObject(
    JNIEnv *env, jclass cls,
    jlong handle, jstring objectKey,
    jlong rdmaBuffHandle, jlong remoteOffset, jlong size,
    jstring rdmaDescriptor, jint channelId, jlong localOffset) {
  auto *server = reinterpret_cast<cuObjServer *>(handle);
  std::string key = jstringToStdString(env, objectKey);
  std::string rdma_descr = jstringToStdString(env, rdmaDescriptor);
  auto *rdma_buf = reinterpret_cast<struct rdma_buffer *>(rdmaBuffHandle);
  try {
    ssize_t bytesWritten = server->handleGetObject(
        key,
        rdma_buf,
        static_cast<uint64_t>(remoteOffset),
        static_cast<size_t>(size),
        rdma_descr,
        static_cast<uint16_t>(channelId),
        static_cast<uint64_t>(localOffset),
        nullptr,   // ibv_wc_status* (sync mode)
        nullptr);  // async_handle (sync mode)
    if (bytesWritten < 0) {
      throwIOException(env, "handleGetObject returned negative value");
      return -1;
    }
    return static_cast<jlong>(bytesWritten);
  } catch (const std::exception &e) {
    throwIOException(env, e.what());
    return -1;
  }
}
