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

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

/**
 * Request filter that detects the {@code x-amz-rdma-token} header on
 * incoming S3 GETs and stashes the descriptor on the JAX-RS request
 * context. {@code ObjectEndpoint.handleGetRequest} reads the property
 * to decide whether to attempt the RDMA path. No header → absent
 * property → unchanged TCP behavior.
 */
@Provider
@Priority(RdmaRequestFilter.PRIORITY)
public class RdmaRequestFilter implements ContainerRequestFilter {

  public static final String RDMA_TOKEN_PROPERTY = "ozone.s3.rdma.token";
  public static final String RDMA_CAPABLE_PROPERTY = "ozone.s3.rdma.capable";

  // Runs after the body-header preprocessor so signing-aware filters have
  // already had a chance to adjust headers, but before the endpoint
  // resource reads the property. The exact value doesn't matter relative
  // to the other filters in this module — it just needs to be later than
  // anything that could rewrite the x-amz-rdma-token header.
  public static final int PRIORITY = HeaderPreprocessor.PRIORITY + 50;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String rdmaToken = requestContext.getHeaderString("x-amz-rdma-token");
    if (rdmaToken != null && !rdmaToken.isEmpty()) {
      requestContext.setProperty(RDMA_CAPABLE_PROPERTY, Boolean.TRUE);
      requestContext.setProperty(RDMA_TOKEN_PROPERTY, rdmaToken);
    }
  }
}
