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

package org.apache.hadoop.ozone.container.keyvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.ContainerCommandRequestProto;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.ContainerCommandResponseProto;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.DatanodeBlockID;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.RdmaReadBlockRequestProto;
import org.apache.hadoop.ozone.container.common.transport.server.XceiverServerRdma;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Phase 5b additions to {@link KeyValueHandler}: the
 * post-construction {@code setXceiverServerRdma} setter and the
 * {@code handleRdmaReadBlock} fallback when no RDMA engine is wired.
 *
 * <p>KeyValueHandler's full constructor is heavy (needs ContainerSet,
 * VolumeSet, metrics, etc.) and adds nothing to these specific tests, so
 * we use a Mockito bare instance with {@code CALLS_REAL_METHODS} — the
 * constructor isn't invoked, and only the methods under test execute.
 * Broader integration coverage comes from {@code TestRdmaReadBlock}
 * (Phase 8) once MiniOzoneCluster is wired up.
 */
class TestKeyValueHandlerRdma {

  private static ContainerCommandRequestProto rdmaReadBlockRequest() {
    return ContainerCommandRequestProto.newBuilder()
        .setCmdType(ContainerProtos.Type.RdmaReadBlock)
        .setContainerID(1)
        .setDatanodeUuid("dn-0")
        .setRdmaReadBlock(RdmaReadBlockRequestProto.newBuilder()
            .setBlockID(DatanodeBlockID.newBuilder()
                .setContainerID(1).setLocalID(1).build())
            .setRdmaToken("tok")
            .build())
        .build();
  }

  private static KeyValueHandler bareHandler() {
    return mock(KeyValueHandler.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  void setterAndGetterRoundTrip() {
    KeyValueHandler handler = bareHandler();
    assertNull(handler.getXceiverServerRdma());
    XceiverServerRdma mockRdma = mock(XceiverServerRdma.class);
    handler.setXceiverServerRdma(mockRdma);
    assertSame(mockRdma, handler.getXceiverServerRdma());
    // Setter accepts null (OzoneContainer calls unconditionally).
    handler.setXceiverServerRdma(null);
    assertNull(handler.getXceiverServerRdma());
  }

  @Test
  void handleRdmaReadBlockReturnsNotAvailableWhenEngineUnset() {
    KeyValueHandler handler = bareHandler();
    // No setXceiverServerRdma call — mirrors a datanode where
    // hdds.datanode.rdma.enabled=false.
    ContainerCommandResponseProto response = handler.handleRdmaReadBlock(
        rdmaReadBlockRequest(), mock(KeyValueContainer.class));
    assertNotNull(response);
    assertEquals(ContainerProtos.Result.RDMA_NOT_AVAILABLE,
        response.getResult());
    assertEquals(ContainerProtos.Type.RdmaReadBlock, response.getCmdType());
  }

  @Test
  void handleRdmaReadBlockReturnsNotAvailableWhenEngineNotAvailable() {
    KeyValueHandler handler = bareHandler();
    XceiverServerRdma mockRdma = mock(XceiverServerRdma.class);
    when(mockRdma.isAvailable()).thenReturn(false);
    handler.setXceiverServerRdma(mockRdma);

    ContainerCommandResponseProto response = handler.handleRdmaReadBlock(
        rdmaReadBlockRequest(), mock(KeyValueContainer.class));
    assertEquals(ContainerProtos.Result.RDMA_NOT_AVAILABLE,
        response.getResult());
  }
}
