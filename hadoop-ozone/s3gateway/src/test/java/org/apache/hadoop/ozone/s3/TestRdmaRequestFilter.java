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

package org.apache.hadoop.ozone.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.container.ContainerRequestContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link RdmaRequestFilter}.
 */
class TestRdmaRequestFilter {

  @Test
  void headerPresentStashesTokenAndCapableFlag() {
    ContainerRequestContext ctx = mock(ContainerRequestContext.class);
    when(ctx.getHeaderString("x-amz-rdma-token")).thenReturn("abc-123");

    new RdmaRequestFilter().filter(ctx);

    verify(ctx).setProperty(
        RdmaRequestFilter.RDMA_TOKEN_PROPERTY, "abc-123");
    verify(ctx).setProperty(
        RdmaRequestFilter.RDMA_CAPABLE_PROPERTY, Boolean.TRUE);
  }

  @Test
  void headerAbsentDoesNotSetProperties() {
    ContainerRequestContext ctx = mock(ContainerRequestContext.class);
    when(ctx.getHeaderString("x-amz-rdma-token")).thenReturn(null);

    new RdmaRequestFilter().filter(ctx);

    verify(ctx, never()).setProperty(
        ArgumentCaptor.forClass(String.class).capture(),
        ArgumentCaptor.forClass(Object.class).capture());
  }

  @Test
  void emptyHeaderDoesNotSetProperties() {
    ContainerRequestContext ctx = mock(ContainerRequestContext.class);
    when(ctx.getHeaderString("x-amz-rdma-token")).thenReturn("");

    new RdmaRequestFilter().filter(ctx);

    verify(ctx, never()).setProperty(
        ArgumentCaptor.forClass(String.class).capture(),
        ArgumentCaptor.forClass(Object.class).capture());
  }

  @Test
  void propertyConstantsAreStable() {
    // Consumers (ObjectEndpoint.handleRdmaGet) read these by name; changes
    // here would silently disable the RDMA detection path. Surface the
    // exact strings so any rename is an explicit, reviewed change.
    assertEquals("ozone.s3.rdma.token",
        RdmaRequestFilter.RDMA_TOKEN_PROPERTY);
    assertEquals("ozone.s3.rdma.capable",
        RdmaRequestFilter.RDMA_CAPABLE_PROPERTY);
    assertTrue(RdmaRequestFilter.PRIORITY > 0);
    assertNull(System.getProperty(
        "RdmaRequestFilter.PRIORITY-nonexistent"));
  }
}
