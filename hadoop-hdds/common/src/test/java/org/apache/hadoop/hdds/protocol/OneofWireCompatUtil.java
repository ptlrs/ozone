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

package org.apache.hadoop.hdds.protocol;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reflection helpers used by the wire-compat every-arm scan tests in the
 * OM/SCM oneof migration branch. Lives in {@code hdds-common}'s test-jar so
 * both {@code hadoop-hdds/server-scm} and {@code hadoop-ozone/ozone-manager}
 * can share it without duplicating the class per module.
 *
 * <p>Every fixture proto is compiled against the plain
 * {@code com.google.protobuf} runtime, so both descriptors and
 * {@link DynamicMessage} come from that runtime. Production
 * {@code ContainerProtos} is generated against Ratis-thirdparty; the
 * bridge across runtimes is byte[], as elsewhere in these tests.
 */
public final class OneofWireCompatUtil {

  private OneofWireCompatUtil() {
  }

  /**
   * @return every optional non-repeated message-typed field number on
   *     {@code descriptor} whose number falls in {@code [minTag, maxTag]}
   *     (inclusive). Preserves declaration order.
   */
  public static Set<Integer> messageFieldNumbersInRange(Descriptor descriptor, int minTag, int maxTag) {
    Set<Integer> out = new LinkedHashSet<>();
    for (FieldDescriptor f : descriptor.getFields()) {
      if (f.getJavaType() != FieldDescriptor.JavaType.MESSAGE) {
        continue;
      }
      if (f.isRepeated()) {
        continue;
      }
      int n = f.getNumber();
      if (n < minTag || n > maxTag) {
        continue;
      }
      out.add(n);
    }
    return out;
  }

  /**
   * Build a minimal well-formed instance of {@code descriptor}: every
   * required field is populated with the JavaType-default (0/""/first
   * enum value/recursively-built required submessage). Optional fields
   * are left unset.
   */
  public static Message buildMinimalMessage(Descriptor descriptor) {
    DynamicMessage.Builder b = DynamicMessage.newBuilder(descriptor);
    for (FieldDescriptor f : descriptor.getFields()) {
      if (f.isRequired()) {
        b.setField(f, minimalValueFor(f));
      }
    }
    return b.build();
  }

  private static Object minimalValueFor(FieldDescriptor f) {
    switch (f.getJavaType()) {
    case INT:         return 0;
    case LONG:        return 0L;
    case FLOAT:       return 0.0f;
    case DOUBLE:      return 0.0;
    case BOOLEAN:     return false;
    case STRING:      return "";
    case BYTE_STRING: return ByteString.EMPTY;
    case ENUM:        return f.getEnumType().getValues().get(0);
    case MESSAGE:     return buildMinimalMessage(f.getMessageType());
    default:
      throw new IllegalStateException("Unsupported JavaType " + f.getJavaType());
    }
  }

  /**
   * Serialize a wrapper (via {@link DynamicMessage} built from
   * {@code descriptor}) that carries every required outer field
   * populated with minimal values plus a minimal instance of the
   * message-typed field at {@code fieldNumber}.
   */
  public static byte[] buildBytesWithMessageField(Descriptor descriptor, int fieldNumber) {
    FieldDescriptor arm = descriptor.findFieldByNumber(fieldNumber);
    if (arm == null || arm.getJavaType() != FieldDescriptor.JavaType.MESSAGE) {
      throw new IllegalArgumentException("Not a message field: " + fieldNumber);
    }
    DynamicMessage.Builder b = DynamicMessage.newBuilder(descriptor);
    for (FieldDescriptor f : descriptor.getFields()) {
      if (f.isRequired()) {
        b.setField(f, minimalValueFor(f));
      }
    }
    b.setField(arm, buildMinimalMessage(arm.getMessageType()));
    return b.build().toByteArray();
  }

  /**
   * Serialize an "empty-body" wrapper: every required outer field is
   * populated with a minimal value; no oneof arm (or optional payload
   * field) is set. Legal on the wire (a heartbeat before the datanode
   * has any command, etc.).
   */
  public static byte[] buildEmptyBodyBytes(Descriptor descriptor) {
    DynamicMessage.Builder b = DynamicMessage.newBuilder(descriptor);
    for (FieldDescriptor f : descriptor.getFields()) {
      if (f.isRequired()) {
        b.setField(f, minimalValueFor(f));
      }
    }
    return b.build().toByteArray();
  }
}
