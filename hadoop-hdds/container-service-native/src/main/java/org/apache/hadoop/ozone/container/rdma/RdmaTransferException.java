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

/**
 * Wire-level RDMA transfer failure: cuObjServer returned a non-positive
 * byte count, channel allocation failed, or an ibverbs status propagated
 * up. Mapped to {@code ContainerProtos.Result.RDMA_TRANSFER_FAILED} at
 * the handler boundary so the gateway can skip an RDMA retry and go
 * directly to TCP.
 */
public class RdmaTransferException extends IOException {

  private static final long serialVersionUID = 1L;

  public RdmaTransferException(String message) {
    super(message);
  }

  public RdmaTransferException(String message, Throwable cause) {
    super(message, cause);
  }
}
