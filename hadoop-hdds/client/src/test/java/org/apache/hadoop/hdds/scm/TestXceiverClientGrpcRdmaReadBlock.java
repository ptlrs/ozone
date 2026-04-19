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

package org.apache.hadoop.hdds.scm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.ContainerCommandRequestProto;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.DatanodeBlockID;
import org.junit.jupiter.api.Test;

/**
 * Covers the package-private {@link XceiverClientGrpc#getRequestBlockID} for
 * the RdmaReadBlock branch added alongside the §4.13-pre E helper update.
 */
class TestXceiverClientGrpcRdmaReadBlock {

  @Test
  void getRequestBlockIDExtractsBlockIDFromRdmaReadBlock() {
    DatanodeBlockID expected = DatanodeBlockID.newBuilder()
        .setContainerID(17).setLocalID(29).build();
    ContainerCommandRequestProto request =
        ContainerCommandRequestProto.newBuilder()
            .setCmdType(ContainerProtos.Type.RdmaReadBlock)
            .setContainerID(17)
            .setDatanodeUuid("dn-0")
            .setRdmaReadBlock(
                ContainerProtos.RdmaReadBlockRequestProto.newBuilder()
                    .setBlockID(expected)
                    .setRdmaToken("tok")
                    .build())
            .build();
    DatanodeBlockID actual = XceiverClientGrpc.getRequestBlockID(request);
    assertEquals(expected, actual);
  }

  @Test
  void getRequestBlockIDReturnsNullForUnrelatedCmd() {
    ContainerCommandRequestProto request =
        ContainerCommandRequestProto.newBuilder()
            .setCmdType(ContainerProtos.Type.CreateContainer)
            .setContainerID(1)
            .setDatanodeUuid("dn-0")
            .build();
    assertNull(XceiverClientGrpc.getRequestBlockID(request));
  }
}
