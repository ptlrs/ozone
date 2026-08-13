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

package org.apache.hadoop.ozone.om.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.ByteString;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.CreateTenantRequest;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.CreateVolumeRequest;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.GetS3SecretRequest;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.OMRequest;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.TenantAssignUserAccessIdRequest;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.Type;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.UpdateGetS3SecretRequest;
import org.apache.hadoop.ozone.protocol.proto.OzoneManagerProtocolProtos.VolumeInfo;
import org.junit.jupiter.api.Test;

/**
 * Multi-payload guard for the six fields §7.1 of {@code UPGRADE_PROTO_ONEOF.md}
 * holds back as flat {@code optional}s outside the {@code body} oneof on
 * {@code OMRequest}.
 *
 * <p>Three production call sites set two payloads on the same
 * {@code OMRequest.Builder} — the second call must not clear the first:
 * <ul>
 *   <li>{@code OMTenantCreateRequest} sets tag 11
 *       ({@code createVolumeRequest}) and tag 96 ({@code CreateTenantRequest})
 *       — see {@code OMTenantCreateRequest.java:207-220}.</li>
 *   <li>{@code S3GetSecretRequest} sets tag 49 ({@code getS3SecretRequest})
 *       and tag 82 ({@code updateGetS3SecretRequest}) — see
 *       {@code S3GetSecretRequest.java:86-123}.</li>
 *   <li>{@code OMTenantAssignUserAccessIdRequest} sets tag 82
 *       ({@code updateGetS3SecretRequest}) and tag 100
 *       ({@code TenantAssignUserAccessIdRequest}) — see
 *       {@code OMTenantAssignUserAccessIdRequest.java:150-186}.</li>
 * </ul>
 *
 * <p>If any two fields in the same pair were ever moved into the
 * {@code body} oneof, protoc would generate mutually-exclusive setters —
 * the second {@code setX(...)} on the same builder would silently clear
 * the first, and half of the intended state would be dropped from the
 * Ratis transaction with no visible error. This test round-trips each
 * pair through {@code build().toByteString()} + {@code parseFrom(...)}
 * and asserts both payloads survive. It fails the day someone moves one
 * of the six paired fields into the oneof.
 */
class TestOmRequestMultiPayloadGuard {

  private static final String CLIENT_ID = "multi-payload-test-client";
  private static final String TRACE_ID = "multi-payload-trace";

  /**
   * Mirrors {@code OMTenantCreateRequest#preExecute} at
   * {@code OMTenantCreateRequest.java:207-220}, which sets both
   * {@code createVolumeRequest} (tag 11) and {@code CreateTenantRequest}
   * (tag 96) on the same {@code OMRequest.Builder} so a tenant's volume
   * and the tenant itself are created atomically in one Ratis transaction.
   */
  @Test
  void createVolumeAndCreateTenantSurviveRoundTrip() throws Exception {
    CreateVolumeRequest createVolume = CreateVolumeRequest.newBuilder()
        .setVolumeInfo(VolumeInfo.newBuilder()
            .setAdminName("admin")
            .setOwnerName("owner")
            .setVolume("tenant-volume")
            .build())
        .build();
    CreateTenantRequest createTenant = CreateTenantRequest.newBuilder()
        .setTenantId("tenant-1")
        .setVolumeName("tenant-volume")
        .setUserRoleName("tenant-1-user")
        .setAdminRoleName("tenant-1-admin")
        .setForceCreationWhenVolumeExists(false)
        .build();

    OMRequest original = OMRequest.newBuilder()
        .setCmdType(Type.CreateTenant)
        .setClientId(CLIENT_ID)
        .setTraceID(TRACE_ID)
        .setCreateVolumeRequest(createVolume)
        .setCreateTenantRequest(createTenant)
        .build();

    ByteString onWire = original.toByteString();
    OMRequest parsed = OMRequest.parseFrom(onWire);

    assertEquals(Type.CreateTenant, parsed.getCmdType());
    assertEquals(CLIENT_ID, parsed.getClientId());
    assertEquals(TRACE_ID, parsed.getTraceID());
    assertTrue(parsed.hasCreateVolumeRequest(),
        "tag 11 (createVolumeRequest) must survive alongside tag 96");
    assertTrue(parsed.hasCreateTenantRequest(),
        "tag 96 (CreateTenantRequest) must survive alongside tag 11");
    assertArrayEquals(createVolume.toByteArray(),
        parsed.getCreateVolumeRequest().toByteArray());
    assertArrayEquals(createTenant.toByteArray(),
        parsed.getCreateTenantRequest().toByteArray());
    assertArrayEquals(onWire.toByteArray(), parsed.toByteArray());
  }

  /**
   * Mirrors {@code S3GetSecretRequest#preExecute} at
   * {@code S3GetSecretRequest.java:86-123}, which sets both
   * {@code getS3SecretRequest} (tag 49) and
   * {@code updateGetS3SecretRequest} (tag 82) on the same
   * {@code OMRequest.Builder} — the client-issued get is replicated to
   * followers alongside the leader-generated secret update.
   */
  @Test
  void getS3SecretAndUpdateGetS3SecretSurviveRoundTrip() throws Exception {
    GetS3SecretRequest getSecret = GetS3SecretRequest.newBuilder()
        .setKerberosID("access-id-1")
        .setCreateIfNotExist(true)
        .build();
    UpdateGetS3SecretRequest updateSecret = UpdateGetS3SecretRequest.newBuilder()
        .setKerberosID("access-id-1")
        .setAwsSecret("aws-secret-hex")
        .build();

    OMRequest original = OMRequest.newBuilder()
        .setCmdType(Type.GetS3Secret)
        .setClientId(CLIENT_ID)
        .setTraceID(TRACE_ID)
        .setGetS3SecretRequest(getSecret)
        .setUpdateGetS3SecretRequest(updateSecret)
        .build();

    ByteString onWire = original.toByteString();
    OMRequest parsed = OMRequest.parseFrom(onWire);

    assertEquals(Type.GetS3Secret, parsed.getCmdType());
    assertEquals(CLIENT_ID, parsed.getClientId());
    assertEquals(TRACE_ID, parsed.getTraceID());
    assertTrue(parsed.hasGetS3SecretRequest(),
        "tag 49 (getS3SecretRequest) must survive alongside tag 82");
    assertTrue(parsed.hasUpdateGetS3SecretRequest(),
        "tag 82 (updateGetS3SecretRequest) must survive alongside tag 49");
    assertArrayEquals(getSecret.toByteArray(),
        parsed.getGetS3SecretRequest().toByteArray());
    assertArrayEquals(updateSecret.toByteArray(),
        parsed.getUpdateGetS3SecretRequest().toByteArray());
    assertArrayEquals(onWire.toByteArray(), parsed.toByteArray());
  }

  /**
   * Mirrors {@code OMTenantAssignUserAccessIdRequest#preExecute} at
   * {@code OMTenantAssignUserAccessIdRequest.java:150-186}, which sets
   * both {@code updateGetS3SecretRequest} (tag 82) and
   * {@code TenantAssignUserAccessIdRequest} (tag 100) on the same
   * {@code OMRequest.Builder} so an accessId is assigned and its S3
   * secret is generated in one Ratis transaction.
   */
  @Test
  void updateGetS3SecretAndTenantAssignUserAccessIdSurviveRoundTrip() throws Exception {
    UpdateGetS3SecretRequest updateSecret = UpdateGetS3SecretRequest.newBuilder()
        .setKerberosID("tenant-1$user")
        .setAwsSecret("aws-secret-hex")
        .build();
    TenantAssignUserAccessIdRequest assignAccessId =
        TenantAssignUserAccessIdRequest.newBuilder()
            .setUserPrincipal("user")
            .setTenantId("tenant-1")
            .setAccessId("tenant-1$user")
            .build();

    OMRequest original = OMRequest.newBuilder()
        .setCmdType(Type.TenantAssignUserAccessId)
        .setClientId(CLIENT_ID)
        .setTraceID(TRACE_ID)
        .setUpdateGetS3SecretRequest(updateSecret)
        .setTenantAssignUserAccessIdRequest(assignAccessId)
        .build();

    ByteString onWire = original.toByteString();
    OMRequest parsed = OMRequest.parseFrom(onWire);

    assertEquals(Type.TenantAssignUserAccessId, parsed.getCmdType());
    assertEquals(CLIENT_ID, parsed.getClientId());
    assertEquals(TRACE_ID, parsed.getTraceID());
    assertTrue(parsed.hasUpdateGetS3SecretRequest(),
        "tag 82 (updateGetS3SecretRequest) must survive alongside tag 100");
    assertTrue(parsed.hasTenantAssignUserAccessIdRequest(),
        "tag 100 (TenantAssignUserAccessIdRequest) must survive alongside tag 82");
    assertArrayEquals(updateSecret.toByteArray(),
        parsed.getUpdateGetS3SecretRequest().toByteArray());
    assertArrayEquals(assignAccessId.toByteArray(),
        parsed.getTenantAssignUserAccessIdRequest().toByteArray());
    assertArrayEquals(onWire.toByteArray(), parsed.toByteArray());
  }
}
