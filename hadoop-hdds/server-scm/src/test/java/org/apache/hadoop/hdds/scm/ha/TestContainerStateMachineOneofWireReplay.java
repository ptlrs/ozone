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

package org.apache.hadoop.hdds.scm.ha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.hadoop.hdds.conf.OzoneConfiguration;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos;
import org.apache.hadoop.hdds.protocol.datanode.proto.testing.Proto2DatanodeClientProtocolForOneofMigrationTesting;
import org.apache.hadoop.ozone.container.common.interfaces.ContainerDispatcher;
import org.apache.hadoop.ozone.container.common.transport.server.ratis.ContainerStateMachine;
import org.apache.hadoop.ozone.container.common.transport.server.ratis.DispatcherContext;
import org.apache.hadoop.ozone.container.common.transport.server.ratis.XceiverServerRatis;
import org.apache.hadoop.ozone.container.ozoneimpl.ContainerController;
import org.apache.ratis.proto.RaftProtos;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.server.DivisionInfo;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * End-to-end wire-compat check for the DN Ratis-log replay path.
 *
 * <p>Encodes a {@code ContainerCommandRequestProto} under the fixture
 * schema ({@code Proto2DatanodeClientProtocolForOneofMigrationTesting}, the
 * verbatim pre-{@code oneof} layout) and hands the raw bytes to
 * {@code ContainerStateMachine.startTransaction(...)} +
 * {@code applyTransaction(...)} running against the production schema.
 *
 * <p>This exercises the same wire-crossing that a follower on a mixed-
 * version cluster performs: it parses log bytes with the production
 * {@code ContainerCommandRequestProto} parser and dispatches based on
 * the resulting {@code cmdType}. If any tag ever moved inside the
 * migrated {@code oneof}, either parse would fail or dispatch would
 * land on the wrong branch.
 */
class TestContainerStateMachineOneofWireReplay {

  private ContainerDispatcher dispatcher;
  private ContainerStateMachine stateMachine;
  private List<ThreadPoolExecutor> executors;

  @BeforeEach
  public void setup() throws Exception {
    OzoneConfiguration conf = new OzoneConfiguration();
    dispatcher = mock(ContainerDispatcher.class);
    ContainerController controller = mock(ContainerController.class);
    XceiverServerRatis ratisServer = mock(XceiverServerRatis.class);
    RaftServer raftServer = mock(RaftServer.class);
    RaftServer.Division division = mock(RaftServer.Division.class);
    RaftGroup raftGroup = mock(RaftGroup.class);
    DivisionInfo info = mock(DivisionInfo.class);
    RaftPeer raftPeer = mock(RaftPeer.class);
    when(ratisServer.getServer()).thenReturn(raftServer);
    when(raftServer.getDivision(any())).thenReturn(division);
    when(division.getGroup()).thenReturn(raftGroup);
    when(raftGroup.getPeer(any())).thenReturn(raftPeer);
    when(division.getInfo()).thenReturn(info);
    when(info.isLeader()).thenReturn(false);
    when(ratisServer.getServerDivision(any())).thenReturn(division);

    executors = IntStream.range(0, 2).mapToObj(i -> new ThreadPoolExecutor(1, 1,
        0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryBuilder()
        .setDaemon(true).setNameFormat("ChunkWriter-" + i + "-%d").build()))
        .collect(Collectors.toList());

    RaftGroupId groupId = RaftGroupId.randomId();
    stateMachine = new ContainerStateMachine(null, groupId, dispatcher,
        controller, executors, ratisServer, conf, "containerOp");
    try {
      stateMachine.initialize(raftServer, groupId, null);
    } catch (Exception ignored) {
      // Ratis storage init throws with a null RaftStorage; state machine is
      // still usable for the wire-crossing path exercised below (groupId is
      // set by super.initialize() before the storage init throws).
    }
  }

  @AfterEach
  public void tearDown() {
    stateMachine.close();
    executors.forEach(ThreadPoolExecutor::shutdown);
  }

  /**
   * Pre-migration bytes replay through the production state machine and
   * reach the container dispatcher with the correct {@code cmdType}.
   */
  @Test
  void applyTransactionRoutesFixtureEncodedCloseContainerRequest() throws Exception {
    when(dispatcher.dispatch(any(), any())).thenReturn(
        ContainerProtos.ContainerCommandResponseProto.newBuilder()
            .setCmdType(ContainerProtos.Type.CloseContainer)
            .setResult(ContainerProtos.Result.SUCCESS)
            .build());

    String datanodeUuid = UUID.randomUUID().toString();
    // Build a CloseContainer request under the pre-`oneof` fixture schema.
    // CloseContainer has an empty body (see the fixture .proto), so it
    // exercises the wrapper + cmdType routing without needing to seed
    // additional state.
    Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandRequestProto fixture =
        Proto2DatanodeClientProtocolForOneofMigrationTesting.ContainerCommandRequestProto.newBuilder()
            .setCmdType(Proto2DatanodeClientProtocolForOneofMigrationTesting.Type.CloseContainer)
            .setContainerID(42L)
            .setDatanodeUuid(datanodeUuid)
            .setTraceID("trace-1")
            .setCloseContainer(Proto2DatanodeClientProtocolForOneofMigrationTesting
                .CloseContainerRequestProto.newBuilder().build())
            .build();
    byte[] onWire = fixture.toByteArray();

    // Wrap the bytes as a Ratis StateMachineLogEntry, exactly the shape
    // ContainerStateMachine.startTransaction expects on the follower path.
    RaftProtos.LogEntryProto entry = RaftProtos.LogEntryProto.newBuilder()
        .setTerm(1)
        .setIndex(5)
        .setStateMachineLogEntry(RaftProtos.StateMachineLogEntryProto.newBuilder()
            .setLogData(ByteString.copyFrom(onWire))
            .build())
        .build();

    // startTransaction is the wire-crossing point: it parses logData with
    // the production ContainerCommandRequestProto parser and stashes a
    // Context on the transaction. applyTransaction then dispatches from
    // that Context.
    TransactionContext trx = stateMachine.startTransaction(entry, RaftProtos.RaftPeerRole.FOLLOWER);
    Message result = stateMachine.applyTransaction(trx).get();
    assertNotNull(result);

    ArgumentCaptor<ContainerProtos.ContainerCommandRequestProto> captor =
        ArgumentCaptor.forClass(ContainerProtos.ContainerCommandRequestProto.class);
    verify(dispatcher).dispatch(captor.capture(), any(DispatcherContext.class));
    ContainerProtos.ContainerCommandRequestProto dispatched = captor.getValue();
    assertEquals(ContainerProtos.Type.CloseContainer, dispatched.getCmdType());
    assertEquals(42L, dispatched.getContainerID());
    assertEquals(datanodeUuid, dispatched.getDatanodeUuid());
    assertEquals("trace-1", dispatched.getTraceID());
  }
}
