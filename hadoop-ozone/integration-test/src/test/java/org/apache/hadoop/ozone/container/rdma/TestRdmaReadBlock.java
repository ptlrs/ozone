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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import org.apache.hadoop.hdds.client.ReplicationConfig;
import org.apache.hadoop.hdds.client.ReplicationFactor;
import org.apache.hadoop.hdds.client.ReplicationType;
import org.apache.hadoop.hdds.conf.OzoneConfiguration;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.ContainerCommandRequestProto;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.ContainerCommandResponseProto;
import org.apache.hadoop.hdds.scm.XceiverClientSpi;
import org.apache.hadoop.hdds.utils.IOUtils;
import org.apache.hadoop.ozone.HddsDatanodeService;
import org.apache.hadoop.ozone.MiniOzoneCluster;
import org.apache.hadoop.ozone.client.ObjectStore;
import org.apache.hadoop.ozone.client.OzoneClient;
import org.apache.hadoop.ozone.client.OzoneClientFactory;
import org.apache.hadoop.ozone.client.io.OzoneOutputStream;
import org.apache.hadoop.ozone.client.rpc.RpcClient;
import org.apache.hadoop.ozone.container.common.transport.server.XceiverServerRdma;
import org.apache.hadoop.ozone.om.helpers.OmKeyArgs;
import org.apache.hadoop.ozone.om.helpers.OmKeyInfo;
import org.apache.hadoop.ozone.om.helpers.OmKeyLocationInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * End-to-end integration test for the RDMA read path against a
 * MiniOzoneCluster running with {@link MockRdmaTransferService}. Writes a
 * 1 MiB key through the normal TCP write path, then issues a direct
 * RdmaReadBlock request per block via {@code XceiverClient.sendCommand}
 * and asserts the bytes land in the registered mock remote buffer.
 *
 * <p>Exercises the full datanode-side dispatch chain from proto + shared
 * helpers (Phase 1) through KeyValueHandler dispatch (Phase 5b) and the
 * OzoneContainer wiring (Phase 6) — without any S3 Gateway HTTP layer.
 * S3G integration comes separately via
 * {@code TestObjectGet} + {@code TestRdmaRequestFilter} in the s3gateway
 * module.
 */
class TestRdmaReadBlock {

  private static final String VOLUME_NAME = "rdmavol";
  private static final String BUCKET_NAME = "rdmabucket";
  private static final String KEY_NAME = "testkey";
  private static final int OBJECT_SIZE = 1024 * 1024;

  private static OzoneConfiguration conf;
  private static MiniOzoneCluster cluster;
  private static OzoneClient client;

  @BeforeAll
  static void setUp() throws Exception {
    conf = new OzoneConfiguration();
    conf.setBoolean(XceiverServerRdma.RDMA_ENABLED_KEY, true);
    conf.set(XceiverServerRdma.RDMA_TRANSFER_SERVICE_CLASS_KEY,
        MockRdmaTransferService.class.getName());
    conf.setQuietMode(false);
    cluster = MiniOzoneCluster.newBuilder(conf).setNumDatanodes(1).build();
    cluster.waitForClusterToBeReady();

    client = OzoneClientFactory.getRpcClient(conf);
    ObjectStore objectStore = client.getObjectStore();
    objectStore.createVolume(VOLUME_NAME);
    objectStore.getVolume(VOLUME_NAME).createBucket(BUCKET_NAME);
  }

  @AfterAll
  static void tearDown() {
    IOUtils.closeQuietly(client);
    if (cluster != null) {
      cluster.shutdown();
    }
  }

  @Test
  void rdmaReadBlockEndToEnd() throws Exception {
    // 1. Write a 1 MiB key via the normal TCP path.
    byte[] data = new byte[OBJECT_SIZE];
    new Random(42).nextBytes(data);

    try (OzoneOutputStream out = client.getObjectStore()
        .getVolume(VOLUME_NAME).getBucket(BUCKET_NAME)
        .createKey(KEY_NAME, data.length,
            ReplicationConfig.fromTypeAndFactor(
                ReplicationType.RATIS, ReplicationFactor.ONE), null)) {
      out.write(data);
    }

    // 2. Look up block locations through OM.
    OmKeyArgs keyArgs = new OmKeyArgs.Builder()
        .setVolumeName(VOLUME_NAME)
        .setBucketName(BUCKET_NAME)
        .setKeyName(KEY_NAME)
        .setSortDatanodesInPipeline(true)
        .build();
    OmKeyInfo keyInfo = cluster.getOzoneManager().lookupKey(keyArgs);
    List<OmKeyLocationInfo> locations = keyInfo
        .getLatestVersionLocations().getBlocksLatestVersionOnly();
    assertNotNull(locations);
    assertEquals(data.length,
        locations.stream().mapToLong(OmKeyLocationInfo::getLength).sum());

    // 3. Register a mock remote buffer keyed by an arbitrary token. Every
    //    datanode running in the MiniOzoneCluster has its own Mock instance
    //    (one datanode here, so we register on that one).
    HddsDatanodeService dn = cluster.getHddsDatanodes().get(0);
    XceiverServerRdma rdmaServer = dn.getDatanodeStateMachine()
        .getContainer().getXceiverServerRdma();
    assertNotNull(rdmaServer, "RDMA engine must be wired on the datanode");

    String fakeToken = "test-rdma-token-" + System.nanoTime();
    ByteBuffer remoteBuffer = ByteBuffer.allocateDirect(data.length);
    ((MockRdmaTransferService) rdmaServer.getTransferService())
        .registerRemoteBuffer(fakeToken, remoteBuffer);

    // 4. Dispatch one RdmaReadBlock request per block. The S3 Gateway
    //    would do this concurrently via its dispatch pool; for the test
    //    we go sequentially to make failure-mode debugging simpler.
    long gpuOffset = 0;
    for (OmKeyLocationInfo loc : locations) {
      ContainerCommandRequestProto.Builder reqBuilder =
          ContainerCommandRequestProto.newBuilder()
              .setCmdType(ContainerProtos.Type.RdmaReadBlock)
              .setRdmaReadBlock(
                  ContainerProtos.RdmaReadBlockRequestProto.newBuilder()
                      .setBlockID(loc.getBlockID().getDatanodeBlockIDProtobuf())
                      .setRdmaToken(fakeToken)
                      .setLength(loc.getLength())
                      .setDestOffset(gpuOffset)
                      .build())
              .setContainerID(loc.getContainerID())
              .setDatanodeUuid(
                  loc.getPipeline().getClosestNode().getUuidString());
      if (loc.getToken() != null) {
        reqBuilder.setEncodedToken(loc.getToken().encodeToUrlString());
      }

      RpcClient rpc = (RpcClient) ((OzoneClient) client).getProxy();
      XceiverClientSpi xcClient = rpc.getXceiverClientManager()
          .acquireClientForReadData(loc.getPipeline());
      try {
        ContainerCommandResponseProto response =
            xcClient.sendCommand(reqBuilder.build());
        assertEquals(ContainerProtos.Result.SUCCESS, response.getResult(),
            () -> "unexpected result: " + response.getMessage());
        assertEquals(loc.getLength(),
            response.getRdmaReadBlock().getBytesTransferred());
      } finally {
        rpc.getXceiverClientManager().releaseClientForReadData(xcClient, false);
      }
      gpuOffset += loc.getLength();
    }

    // 5. Bytes that left the mock RDMA engine must match the original.
    byte[] received = new byte[data.length];
    remoteBuffer.position(0);
    remoteBuffer.get(received);
    assertArrayEquals(data, received);
  }
}
