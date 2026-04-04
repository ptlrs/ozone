# Prompt: Generate Detailed Implementation Spec for DN-Direct RDMA in Apache Ozone

## Instructions to the AI

You are a senior systems engineer specializing in NVIDIA GPUDirect Storage, RDMA networking, and distributed storage
systems. Generate a **complete, detailed implementation specification** for integrating NVIDIA's cuObject SDK into
Apache Ozone to enable DN-Direct RDMA — where Ozone datanodes RDMA-write/read data directly to/from GPU client memory,
with the S3 Gateway acting only as a control plane.

**Dual audience**: This spec serves both (a) AI systems that will generate implementation code with tests in one shot,
and (b) human engineers and architects who need to understand the design, trace decisions back to sources, and review
the architecture. Write with both audiences in mind.

**Source attribution**: For every technical claim, SDK API detail, hardware requirement, or design decision, include the
source URL or documentation reference. Collect all sources in a "References" appendix at the end, and cite them inline
using `[ref-N]` notation. This allows engineers to verify claims and trace decisions.

The spec must be detailed enough that another AI can consume it and generate the full implementation code with tests in
one shot. Use markdown formatting throughout. Use mermaid diagrams for all architecture and flow illustrations — never
use ASCII art.

---

## Context: What We Are Building

Apache Ozone is a distributed object store with an S3-compatible gateway. Today, all data flows through TCP:

```
S3 Client → (HTTP/TCP) → S3 Gateway → (gRPC/TCP) → Datanodes → NVMe
```

We are integrating NVIDIA's cuObject SDK to enable GPU clients to receive data directly from datanodes via RDMA,
bypassing the gateway for the data plane:

```
GPU Client ← (RDMA direct) ← Datanodes (cuObjServer)
GPU Client → (HTTP control) → S3 Gateway → (gRPC control) → Datanodes
```

NVIDIA's cuObject architecture is a two-part system:

- **cuObjClient** (on GPU machine): Registers GPU memory for RDMA, generates `x-amz-rdma-token`, sends S3 request with
  token in HTTP header
- **cuObjServer** (embedded in storage data nodes as a library): Parses RDMA tokens, performs RDMA READ/WRITE directly
  to/from client GPU memory using Dynamically Connected (DC) InfiniBand/RoCEv2 transport

NVIDIA's own documentation describes this flow: "Gateway parses token and instructs data nodes → Data nodes allocate
local buffers and register for RDMA using cuObjServer APIs → DC connection established between data node and client →
RDMA_READ or RDMA_WRITE executes directly to/from client GPU memory." This is exactly what we are implementing.

---

## Section 1: Development Environment Setup

Generate detailed, step-by-step instructions for setting up the development environment. Cover BOTH Lambda Cloud and
Vultr. Prioritize the cheapest option for development.

### 1.1 Cloud Instance Selection

**Requirements for development:**

- We want a **single-node development setup** for speed and cost. One machine that runs:
  - An Ozone cluster (OM + SCM + 3 datanodes in-process via MiniOzoneCluster)
  - A GPU for testing cuObjClient
  - RDMA loopback for testing RDMA data path on a single machine
- For integration testing, we need a **2-node setup**: 1 GPU client + 1 storage node (running Ozone + cuObjServer)

**Generate:**

- Exact instance types on Lambda Cloud (e.g., gpu_1x_a100, gpu_1x_h100_sxm5) with pricing
- Exact instance types on Vultr (e.g., vbm-12c-256gb-6-a100) with pricing
- Comparison table: GPU type, vCPUs, RAM, storage, networking, price/hr
- Recommendation for cheapest viable option
- Explain whether RDMA loopback testing is possible on a single cloud GPU instance (i.e., can we test cuObjServer →
  cuObjClient RDMA on the same machine?)
- Explain what networking is available on these instances — do Lambda/Vultr instances have ConnectX NICs? Can we do RoCE
  between instances in the same region?
- If RDMA between cloud instances is not available, explain the fallback: can cuObject work in TCP compatibility mode?
  What do we lose?

### 1.2 Operating System and Base Packages

Generate exact commands for Ubuntu 22.04 or 24.04:

- System update, build essentials, kernel headers
- Java 8 and 11 (Ozone requires both)
- Maven 3.9+
- Protocol Buffers compiler (protoc)
- CMake 3.20+
- Git, Docker (for containerized testing)

### 1.3 NVIDIA Driver and CUDA Installation

Generate exact commands:

- NVIDIA driver installation (latest stable, 550+ series)
- CUDA Toolkit 13.1.1+ installation (required for cuObject v1.0.0)
- If CUDA 13.1.1 is not yet available, explain what CUDA version to use and any limitations
- Environment variable setup (PATH, LD_LIBRARY_PATH, CUDA_HOME)
- Validation: `nvidia-smi`, `nvcc --version`

### 1.4 MLNX_OFED / DOCA-OFED Installation

Generate exact commands:

- Download and install MLNX_OFED (latest LTS, Oct 2024) or DOCA-OFED
- Kernel module loading and verification
- RDMA device verification: `ibstat`, `ibv_devinfo`
- For cloud instances without InfiniBand: explain RoCE configuration over Ethernet
- Loopback RDMA testing: `rping -s` / `rping -c -a 127.0.0.1`
- If RDMA is not available on the cloud instance, explain SoftRoCE (rxe) setup for development

### 1.5 GPUDirect Storage Installation

Generate exact commands:

- GDS packages (part of CUDA toolkit)
- Kernel configuration: IOMMU off (`intel_iommu=off` or `amd_iommu=off` in GRUB)
- cuFile configuration: `/etc/cufile.json`
- Validation: `gdscheck -p` (or `gdscheck.py`)
- Benchmark: `gdsio` basic test

### 1.6 cuObject SDK Installation

Generate exact commands:

- Download cuObject client and server libraries
- Installation (headers, shared libraries)
- Verify: show how to compile and run a minimal cuObjClient + cuObjServer hello-world test
- List all header files and library files that will be used

### 1.7 Apache Ozone Build Validation

Generate exact commands:

- Clone and build Ozone from master branch
- Run a quick smoke test with MiniOzoneCluster
- Verify S3 Gateway works: `aws s3 --endpoint-url http://localhost:9878 ls`
- Build the existing native module (`-Drocks_tools_native`) to verify native build toolchain works

### 1.8 Environment Setup and Validation Scripts

Generate **complete, runnable shell scripts** (not just instructions) for:

**`setup-dev-env.sh`** — A single script that performs the entire environment setup:

- Detects OS and architecture
- Installs all system packages
- Installs NVIDIA drivers, CUDA toolkit, GDS, MLNX_OFED
- Installs cuObject SDK
- Configures SoftRoCE if no hardware RDMA is detected
- Configures IOMMU, cuFile, etc.
- Builds Apache Ozone from source
- Accepts flags like `--skip-ozone-build`, `--soft-roce-only`, `--cuda-version=12.6`
- Logs all output to a file for debugging
- Is idempotent (can be run multiple times safely)

**`validate-env.sh`** — A comprehensive validation script that checks:

- GPU accessible and correct compute capability (`nvidia-smi`)
- CUDA toolkit version (`nvcc --version`)
- RDMA device present and functional (`ibv_devinfo`, or SoftRoCE `rdma link`)
- GDS installed and verified (`gdscheck` or equivalent)
- cuObject libraries present (`ldconfig -p | grep cuobj`)
- RDMA loopback works (`rping` test)
- Ozone builds successfully
- MiniOzoneCluster starts and S3 Gateway is accessible
- Produces a summary table: PASS/FAIL for each check
- Returns non-zero exit code if any critical check fails

**`setup-docker-cluster.sh`** — For integration testing, generate a Docker Compose setup:

- Container 1: Ozone OM + SCM + S3 Gateway
- Container 2-4: Ozone Datanodes with cuObjServer
- Container 5: GPU client with cuObjClient
- Explain how GPU passthrough works in Docker (`--gpus`, `--device`, `--cap-add`)
- Explain how RDMA device passthrough works in Docker (`--device /dev/infiniband/`)
- Include `docker-compose.yml` and `Dockerfile` specs

The scripts should be complete enough that another AI can generate them directly from the spec.

---

## Section 2: Architecture

### 2.1 Overall Architecture

Generate mermaid diagrams showing:

- The complete system architecture with all components
- Which components run on which nodes
- Network connections between components (RDMA data plane vs TCP control plane)
- Single-node development topology
- Multi-node production topology

### 2.2 Data Flow: S3 GET (Read Path)

Generate a detailed mermaid sequence diagram showing every step:

1. GPU application calls cuObjClient to GET an S3 object
2. cuObjClient registers GPU memory, generates RDMA token
3. cuObjClient sends HTTP GET with `x-amz-rdma-token` to S3 Gateway
4. S3 Gateway contacts Ozone Manager for key metadata (block locations)
5. OM returns `OmKeyInfo` with `List<OmKeyLocationInfo>`, each containing `Pipeline` with `DatanodeDetails` (including
   RDMA port)
6. For each block, S3 Gateway sends an `RdmaReadBlock` gRPC command to the owning datanode, passing the RDMA token and
   destination offset
7. Datanode receives command, reads block data from local NVMe via `FileChannel.read()` into RDMA-registered buffer
8. Datanode uses cuObjServer to RDMA WRITE data directly to client's GPU memory
9. Datanode returns completion to S3 Gateway
10. S3 Gateway returns HTTP 200 with metadata to cuObjClient
11. cuObjClient returns GPU buffer to application

### 2.3 Data Flow: S3 PUT (Write Path)

Generate a detailed mermaid sequence diagram showing:

1. GPU application calls cuObjClient to PUT an S3 object
2. cuObjClient registers GPU buffer, generates RDMA token
3. cuObjClient sends HTTP PUT with `x-amz-rdma-token` to S3 Gateway
4. S3 Gateway contacts OM to create key and allocate blocks (OM contacts SCM for pipeline)
5. SCM returns pipeline with DatanodeDetails (including RDMA ports)
6. S3 Gateway sends `RdmaWriteBlock` gRPC command to the primary datanode, passing RDMA token and source offset
7. Primary datanode uses cuObjServer to RDMA READ from client's GPU memory into RDMA-registered buffer
8. Datanode writes data to local NVMe via existing `ChunkManager.writeChunk()` path
9. Datanode replicates to other datanodes via Ratis (unchanged)
10. Datanode returns completion
11. S3 Gateway commits key with OM
12. S3 Gateway returns HTTP 200 with ETag

### 2.4 Fallback Path

Generate a diagram showing what happens when:

- The datanode does not have an RDMA NIC (graceful fallback to existing TCP path)
- The client does not send `x-amz-rdma-token` (standard S3 path, zero change)
- RDMA transfer fails mid-way (retry on alternate replica, or fall back to TCP)

### 2.5 Component Interaction Map

Generate a mermaid diagram showing all Ozone components and which ones are modified:

- Ozone Manager (unchanged)
- SCM (unchanged)
- S3 Gateway (modified: orchestration logic)
- Datanode (modified: new XceiverServerRdma, new port type, cuObjServer JNI)
- Existing gRPC/Ratis channels (unchanged)
- ChunkManager / FilePerBlockStrategy (unchanged — disk I/O path is the same)

---

## Section 2B: GPU Memory Semantics and Data Consumption Model

This is a critical section. Generate extensive, detailed explanations with mermaid diagrams for each topic. This section
must be clear enough for engineers unfamiliar with GPU programming to understand exactly how data flows to and is
consumed by GPU applications.

### 2B.1 Transfer Model: Complete vs Streaming

cuObject operates on a **complete-transfer model**, not streaming. Explain in detail:

- When cuObjClient issues a GET, the entire requested range must complete before the application can access the data
- The application **cannot** read partially-transferred data — doing so would read uninitialized GPU memory
- The GET call is synchronous from the application's perspective — it blocks until all RDMA writes from all datanodes
  are complete
- Generate a mermaid sequence diagram showing: application calls GET → cuObjClient registers memory → RDMA transfers
  happen → all complete → application accesses buffer
- Contrast with how a TCP-based S3 client works (streaming: process bytes as they arrive)
- Explain implications: the GPU application must allocate the full buffer upfront

**cuObject does not integrate with CUDA streams for asynchronous overlap.** The application cannot overlap a cuObject
GET with a CUDA kernel on the same data. However, it CAN overlap a GET on buffer A with computation on buffer B (
double-buffering pattern). Explain this pattern in detail with a diagram.

### 2B.2 Small Files (< 1 MB)

Explain the small file performance characteristics:

- RDMA has per-transfer overhead: memory registration (10µs-1ms), DC connection setup, completion polling
- For very small files (< 64KB), this overhead dominates and RDMA may be **slower** than TCP
- cuObject uses Dynamic Connection (DC) transport which reduces but does not eliminate setup cost
- Generate a conceptual performance graph (describe it, since mermaid can't do arbitrary charts) showing: X-axis =
  object size, Y-axis = throughput, two lines (RDMA vs TCP) crossing over at ~256KB-1MB
- **Recommendation for the implementation**: Below a configurable threshold (default 256KB), the S3 Gateway should fall
  back to the TCP path even if the client sends an RDMA token. Add config key `ozone.s3g.rdma.min.object.size`
- Explain how Ozone knows the object size before deciding the path: OM returns `OmKeyInfo.getDataSize()` which is
  available before any data transfer

### 2B.3 Files Larger Than GPU Memory

Explain how to handle objects larger than available GPU memory:

- cuObjClient has a **4 GiB per-call limit** on memory registration via `cuMemObjGetDescriptor()`
- For objects larger than GPU memory, the application must use **RANGE_GET** (S3 Range header) to read chunks
- Pattern: allocate GPU buffer of size N → GET range [0, N) → process → GET range [N, 2N) → process → ...
- The S3 Gateway already supports Range headers (existing `RangeHeaderParserUtil` in ObjectEndpoint)
- For RDMA path: the Range header determines which block(s) and byte ranges to read, and the RDMA transfer covers only
  that range
- Generate a mermaid diagram showing the chunked-read loop for a 20GB file on a GPU with 8GB memory
- Explain: for the RDMA path, each range request is an independent S3 GET with its own RDMA token (cuObjClient
  re-registers or reuses the same buffer)
- **For writes**: same chunking applies. Application must PUT in parts (S3 Multipart Upload) if the GPU tensor is larger
  than 4GiB

### 2B.4 Multiple Concurrent Requests

Explain concurrency in detail:

- **Multiple threads on one GPU**: cuObjClient supports concurrent GET/PUT on **different** buffers. NOT on the same
  buffer.
- **Multiple applications on one GPU**: NVIDIA MPS (Multi-Process Service) or CUDA MIG (Multi-Instance GPU) allows
  multiple processes to share a GPU. Each process can independently use cuObjClient.
- **Multiple GPUs on one node**: Each GPU has its own memory space. cuObjClient registers memory on a specific GPU. An
  application targeting GPU 0 and GPU 1 must make separate GET calls for each GPU's buffer.
- Generate a mermaid diagram showing: 2 PyTorch training processes, each on a different GPU, each making concurrent GET
  requests through the same S3 Gateway to different datanodes
- **On the datanode side**: cuObjServer is multi-threaded and handles concurrent RDMA operations from multiple clients.
  XceiverServerRdma must also be thread-safe — use the existing thread pool pattern from XceiverServerGrpc.
- **Rate limiting**: Explain whether we need to limit concurrent RDMA operations per datanode (to avoid exhausting RDMA
  resources). Recommend a configurable limit: `ozone.datanode.rdma.max.concurrent.transfers`

### 2B.5 GPU Application Consumption Pattern

Explain the end-to-end flow from a GPU application's perspective with concrete examples:

**Example 1: PyTorch Training Data Loading**

```
Training loop:
  for each batch:
    1. Allocate GPU buffer (or reuse from pool)
    2. cuObjClient.get("s3://training-data/batch-042.parquet") → GPU buffer
    3. Parse parquet in GPU memory (cuDF or custom CUDA kernel)
    4. Feed tensor to model.forward()
    5. Backprop, update weights
    6. Loop
```

- Generate mermaid sequence diagram for this loop
- Show where the RDMA transfer happens relative to training
- Show the double-buffering optimization: while batch N is training, batch N+1 is being RDMA-fetched into a second
  buffer

**Example 2: RAPIDS cuDF Analytics**

```
df = cudf.read_parquet("s3://datalake/table.parquet")  # uses kvikIO → cuObjClient
result = df.groupby("col").agg({"val": "sum"})
```

- Explain how kvikIO integrates with cuObjClient under the hood
- Explain that cuDF allocates the GPU buffer internally and cuObjClient fills it

**Example 3: Checkpoint Save (PUT)**

```
1. model.state_dict() → GPU tensors
2. Serialize to buffer (torch.save to GPU buffer)
3. cuObjClient.put("s3://checkpoints/epoch-5.pt", gpuBuffer)
4. Datanode RDMA-reads from GPU → writes to NVMe
```

### 2B.6 What Happens If Application Reads Before Transfer Completes?

Explain clearly:

- cuObjClient GET is a **blocking call** — the function does not return until all data is in GPU memory
- If the application somehow circumvents this (e.g., reading GPU memory from a different thread before GET returns): *
  *undefined behavior** — the memory may contain partial data, zeros, or stale data
- This is not unique to RDMA — the same applies to any DMA operation
- The cuObjClient API is designed to prevent this: it only returns the buffer pointer after completion
- **No special handling needed in our Ozone implementation** — cuObjClient manages synchronization on the client side

### 2B.7 Scatter-Gather Support

Explain:

- cuObjServer supports scatter-gather I/O (up to 10 entries per operation)
- This means a single RDMA operation can write to non-contiguous GPU memory regions
- Use case: writing a block's data into a pre-allocated tensor with strides, or filling multiple small buffers from one
  block
- **For our initial implementation**: we use contiguous buffers only (simpler). Scatter-gather is a future optimization.
- Document this as a known limitation and future work item

---

## Section 2C: Security and Encryption

This section explains Ozone's security layers, how they interact with the RDMA data path, their performance
implications, and how to configure them for maximum GPUDirect performance.

### 2C.1 Ozone Security Layers Overview

Ozone has four independent security layers. Generate a mermaid diagram showing each layer, which components enforce it,
and where it sits in the data path:

| Layer                                 | What It Does                                                       | Default State    | Config Key                                          | Impact on RDMA                                                                                                                                                                              |
|---------------------------------------|--------------------------------------------------------------------|------------------|-----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Kerberos Authentication**           | Client→OM/SCM/S3GW authentication                                  | OFF              | `ozone.security.enabled`                            | Control plane only — no RDMA impact                                                                                                                                                         |
| **Block Tokens**                      | Short-lived tokens authorizing datanode access for specific blocks | OFF              | `hdds.block.token.enabled`                          | Must be validated in `XceiverServerRdma` if enabled — CPU overhead per request, but no data path change                                                                                     |
| **gRPC TLS**                          | Encrypts gRPC traffic between gateway↔datanode and client↔datanode | OFF              | `hdds.grpc.tls.enabled`                             | Only affects the gRPC control channel (RdmaReadBlock/RdmaWriteBlock commands), NOT the RDMA data plane. RDMA data travels on a separate InfiniBand/RoCE path that does NOT go through gRPC. |
| **Transparent Data Encryption (TDE)** | Encrypts object data at rest using AES-CTR                         | OFF (per-bucket) | Bucket-level: `--encryption-key` at bucket creation | **Critical RDMA impact** — see 2C.3                                                                                                                                                         |

**Key insight for the spec:** By default, ALL security layers are OFF. For development and benchmarking, no security
configuration is needed. In production, each layer can be enabled independently.

### 2C.2 Security Architecture in the RDMA Path

Generate a mermaid sequence diagram showing the complete request flow with all security layers enabled:

1. Client authenticates to OM via Kerberos → gets delegation token
2. Client requests key from OM → OM returns `OmKeyInfo` with block locations AND a **block token** (signed JWT
   containing containerID, blockID, expiry, permissions)
3. Client sends S3 GET with `x-amz-rdma-token` + `Authorization` header to S3 Gateway
4. S3 Gateway validates delegation token, gets block locations + block tokens from OM
5. S3 Gateway sends `RdmaReadBlock` command to datanode via **gRPC with TLS** — the command includes the block token and
   RDMA token
6. Datanode validates block token using `TokenVerifier` (CPU operation, ~0.1ms per token)
7. If valid, datanode reads data from NVMe into RDMA-registered buffer
8. Datanode RDMA-WRITEs data to GPU memory — **this RDMA transfer is NOT encrypted by gRPC TLS** (it's a separate RDMA
   path)
9. Datanode returns completion to gateway via gRPC/TLS

**Important:** The RDMA data plane is inherently **not encrypted**. RDMA bypasses the kernel network stack and gRPC
entirely. Data travels as plaintext over InfiniBand/RoCEv2. This is by design — encryption would defeat the purpose of
zero-copy GPU transfers (would require CPU/host memory bounce for encryption). NVIDIA's cuObject does not support
encryption on the RDMA path.

### 2C.3 TDE (Transparent Data Encryption) and RDMA

This is the most important security consideration. Generate a detailed explanation with mermaid diagrams:

**How TDE works in Ozone today:**

- TDE is enabled per-bucket by associating a KMS encryption key at bucket creation
- When a client writes data, `OzoneOutputStream` wraps the write stream with `CryptoOutputStream` (see
  `hadoop-ozone/client/src/main/java/org/apache/hadoop/ozone/client/io/OzoneOutputStream.java`)
- The encryption happens **at the client or S3 Gateway level, BEFORE data reaches the datanode**
- Datanodes store **already-encrypted bytes** — they never see plaintext
- On read, the S3 Gateway wraps the read stream with `CryptoInputStream` to decrypt before returning to the client

**TDE interaction with RDMA — two scenarios:**

**Scenario A: TDE Disabled (Recommended for GDS)**

```
GPU Memory ←(RDMA)← Datanode NVMe
Data is plaintext everywhere. Maximum performance.
```

- No encryption overhead
- Data flows: NVMe → RDMA-registered buffer → RDMA WRITE → GPU memory
- This is the recommended configuration for GPUDirect workloads (AI training data, analytics)

**Scenario B: TDE Enabled (Performance Penalty)**
Generate a mermaid diagram showing this altered flow:

```
GPU Memory ←(RDMA)← Datanode NVMe [encrypted bytes]
```

- Datanodes store encrypted bytes. When RDMA-written to GPU, the GPU receives **encrypted data**
- The GPU application would need to **decrypt on the GPU** — but Ozone's decryption is Java-based (`CryptoInputStream`
  uses `javax.crypto`)
- **This breaks the intended data flow.** Options:
  1. **Don't use TDE with RDMA** (recommended): AI training data typically doesn't need at-rest encryption on the
     storage side — the GPU cluster is trusted
  2. **Decrypt at the gateway** (defeats purpose): Gateway reads encrypted data via TCP, decrypts in CPU memory, then
     RDMA-writes plaintext to GPU — adds the TCP hop back, negating RDMA benefit
  3. **Decrypt on GPU** (future work): Implement AES-CTR decryption as a CUDA kernel. This is possible but requires the
     encryption key to be available on the GPU — significant security and implementation complexity. Not recommended for
     initial implementation.

**Generate clear recommendation:** For initial implementation, document that TDE-enabled buckets are **not compatible**
with the RDMA path. When S3 Gateway detects an RDMA token AND the bucket has TDE enabled, it should fall back to the TCP
path and log a warning. Add config key: `ozone.s3g.rdma.allow.encrypted.buckets` (default: `false`; if set to `true`,
RDMA transfers encrypted bytes to GPU and the application is responsible for decryption).

### 2C.4 Block Token Validation in XceiverServerRdma

Explain the existing pattern and how to replicate it:

- `HddsDispatcher` (line ~117) uses `TokenVerifier` to validate block tokens
- When security is disabled, it uses `NoopTokenVerifier` (line ~136-137) which always succeeds
- `XceiverServerRdma` must follow the same pattern:
  - Accept block token in `RdmaReadBlockRequestProto` and `RdmaWriteBlockRequestProto`
  - Call `tokenVerifier.verify(token, command)` before performing RDMA
  - Use `NoopTokenVerifier` when `hdds.block.token.enabled=false`
- Performance impact: Token verification is a CPU operation (~0.1ms, involves signature verification). For large RDMA
  transfers (100MB+), this is negligible. For small transfers near the RDMA/TCP crossover point, it adds to the overhead
  that already makes RDMA less efficient for small files.

### 2C.5 RDMA Transport Security

Explain the security posture of the RDMA data plane:

- **RDMA data is NOT encrypted in transit.** InfiniBand and RoCEv2 do not provide encryption by default.
- **RDMA tokens provide authorization.** The `x-amz-rdma-token` contains the RDMA memory region key (rkey) and memory
  address. Without a valid rkey, a remote host cannot RDMA into GPU memory. cuObjServer validates tokens before
  executing transfers.
- **Network-level isolation:** RDMA traffic should run on a dedicated fabric (InfiniBand subnet or VLAN for RoCE). This
  provides physical network isolation.
- **No eavesdropping protection:** If an attacker has access to the RDMA fabric, they could potentially sniff data. For
  environments requiring wire encryption, IPsec over RoCE is possible but adds latency and CPU overhead that defeats GDS
  benefits.
- **Recommendation for the spec:** Document that RDMA transfers are plaintext and rely on network-level isolation for
  security. Production deployments requiring data-in-transit encryption should either (a) use TDE (encrypted at rest,
  but see 2C.3 for RDMA implications) or (b) not use the RDMA path.

### 2C.6 Disabling Security for Maximum RDMA Performance

Generate exact configuration for a "maximum performance, no security" deployment:

```xml
<!-- ozone-site.xml -->
<!-- Disable all security layers -->
<property>
  <name>ozone.security.enabled</name>
  <value>false</value>
</property>
<property>
  <name>hdds.block.token.enabled</name>
  <value>false</value>
</property>
<property>
  <name>hdds.grpc.tls.enabled</name>
  <value>false</value>
</property>
<!-- Do NOT create buckets with --encryption-key to avoid TDE -->
```

Explain:

- With these settings, `TokenVerifier` becomes `NoopTokenVerifier` — zero overhead
- gRPC control channel is plaintext — no TLS handshake overhead
- No encryption on data at rest or in transit
- This is appropriate for: development, benchmarking, isolated GPU clusters, non-sensitive AI training data
- This is NOT appropriate for: multi-tenant clusters, sensitive data, regulatory environments

### 2C.7 Security Configuration Matrix

Generate a table showing recommended security settings for different deployment scenarios:

| Scenario                                    | `ozone.security.enabled` | `hdds.block.token.enabled` | `hdds.grpc.tls.enabled` | TDE      | RDMA Performance                          |
|---------------------------------------------|--------------------------|----------------------------|-------------------------|----------|-------------------------------------------|
| Development / Benchmarking                  | `false`                  | `false`                    | `false`                 | Disabled | **Maximum**                               |
| Isolated GPU Cluster (trusted network)      | `true`                   | `true`                     | `false`                 | Disabled | **High** (token validation ~0.1ms)        |
| Shared Cluster (untrusted network)          | `true`                   | `true`                     | `true`                  | Disabled | **High** (gRPC TLS on control plane only) |
| Regulated Environment (encryption required) | `true`                   | `true`                     | `true`                  | Enabled  | **TCP fallback** (RDMA incompatible)      |

---

## Section 3: Ozone Code Changes

For each change below, specify:

- Exact file path in the Ozone codebase
- What to add/modify
- Code structure (class names, method signatures, key logic)
- How it integrates with existing code

### 3.1 Protocol Buffer Changes

**File:** `hadoop-hdds/interface-client/src/main/proto/DatanodeClientProtocol.proto`

Specify:

- New command types to add to the `Type` enum (e.g., `RdmaReadBlock = 25`, `RdmaWriteBlock = 26`)
- New protobuf message definitions:
  - `RdmaReadBlockRequestProto`: blockID, containerID, rdmaToken (bytes), destOffset (int64), length (int64), security
    token
  - `RdmaReadBlockResponseProto`: bytesTransferred, status
  - `RdmaWriteBlockRequestProto`: blockID, containerID, pipelineID, rdmaToken (bytes), srcOffset (int64), length (
    int64), security token
  - `RdmaWriteBlockResponseProto`: bytesTransferred, status
- How these are added to `ContainerCommandRequestProto` and `ContainerCommandResponseProto`
- Generate the complete protobuf additions

### 3.2 DatanodeDetails Port Type

**File:** `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/protocol/DatanodeDetails.java`

Specify:

- Add `RDMA` to `Port.Name` enum
- Version annotation (if needed for rolling upgrades)
- How the port gets serialized in protobuf and registered with SCM

### 3.3 New Maven Module: container-service-native

**Location:** `hadoop-hdds/container-service-native/`

This is a new Maven module for the JNI bridge to cuObjServer. Specify:

- Complete `pom.xml` following the pattern from `hadoop-hdds/rocks-native/pom.xml`
- Maven profile activation (e.g., `-Dgds_native`)
- CMake configuration for building the JNI shared library
- How JNI headers are generated from Java classes
- How the .so is packaged into the JAR
- Dependencies: CUDA headers, cuObjServer headers, libibverbs headers

### 3.4 JNI Bridge: CuObjServerBridge.java

**Location:** `hadoop-hdds/container-service-native/src/main/java/.../gds/CuObjServerBridge.java`

Specify the complete Java class with:

- `native` method declarations:
  - `static native long init(String rdmaDevice, int maxConnections)` → returns handle
  - `static native void shutdown(long handle)`
  - `static native long registerBuffer(long handle, ByteBuffer buffer)` → returns MR handle
  - `static native void deregisterBuffer(long handle, long mrHandle)`
  -
  `static native int rdmaWriteToRemote(long handle, byte[] rdmaToken, ByteBuffer localBuffer, long destOffset, int length)` →
  bytes transferred
  -
  `static native int rdmaReadFromRemote(long handle, byte[] rdmaToken, ByteBuffer localBuffer, long srcOffset, int length)` →
  bytes transferred
- Static block that loads the native library using `NativeLibraryLoader`
- Availability check method: `static boolean isAvailable()`
- Error handling: how native errors map to Java exceptions

### 3.5 Native C++ Implementation

**Location:** `hadoop-hdds/container-service-native/src/main/native/CuObjServerBridge.cpp`

Specify:

- JNI function implementations for each native method
- How to include cuObjServer headers and link against the library
- cuObjServer initialization and lifecycle management
- RDMA buffer registration pattern
- RDMA WRITE implementation (for GET/read path)
- RDMA READ implementation (for PUT/write path)
- Error handling and cleanup
- Thread safety considerations

### 3.6 CMakeLists.txt for Native Build

**Location:** `hadoop-hdds/container-service-native/src/main/CMakeLists.txt`

Specify:

- Find CUDA toolkit
- Find cuObjServer headers and libraries
- Find JNI headers
- Find libibverbs
- Compile flags
- Shared library output configuration

### 3.7 XceiverServerRdma.java

**Location:** `hadoop-hdds/container-service/src/main/java/.../transport/server/XceiverServerRdma.java`

This implements `XceiverServerSpi`. Specify:

- Constructor: takes `DatanodeDetails`, config, `ContainerDispatcher`, cert client
- `start()`: Initialize cuObjServer via JNI, register RDMA port on DatanodeDetails
- `stop()`: Shutdown cuObjServer, release RDMA resources
- How it handles `RdmaReadBlock` commands:
  1. Validate security token
  2. Get container/block from dispatcher
  3. Read block data from disk into RDMA-registered buffer (reuse existing `ChunkManager.readChunk()` or
     `KeyValueHandler.readBlockImpl()` logic)
  4. Call `CuObjServerBridge.rdmaWriteToRemote()` with the RDMA token from the request
  5. Return completion
- How it handles `RdmaWriteBlock` commands:
  1. Validate security token
  2. Call `CuObjServerBridge.rdmaReadFromRemote()` to get data from GPU
  3. Write data using existing `ChunkManager.writeChunk()` path
  4. Trigger Ratis replication (existing path)
  5. Return completion
- RDMA buffer pool management (pre-allocated pool of RDMA-registered ByteBuffers)
- Fallback: if cuObjServer is not available (`CuObjServerBridge.isAvailable() == false`), this server doesn't start —
  graceful degradation
- Configuration keys: `ozone.datanode.rdma.enabled`, `ozone.datanode.rdma.port`, `ozone.datanode.rdma.device`,
  `ozone.datanode.rdma.buffer.size`, `ozone.datanode.rdma.buffer.count`

### 3.8 OzoneContainer.java Modifications

**File:** `hadoop-hdds/container-service/src/main/java/.../ozoneimpl/OzoneContainer.java`

Specify:

- Add `XceiverServerSpi rdmaChannel` field alongside existing `writeChannel` and `readChannel`
- In constructor: create `XceiverServerRdma` if RDMA is enabled and `CuObjServerBridge.isAvailable()`
- In `start()`: call `rdmaChannel.start()` (after line 575 where readChannel starts)
- In `stop()`: call `rdmaChannel.stop()`
- Port registration in `DatanodeDetails`

### 3.9 HddsDispatcher Modifications

**File:** `hadoop-hdds/container-service/src/main/java/.../impl/HddsDispatcher.java`

Specify:

- Add handlers for new `RdmaReadBlock` and `RdmaWriteBlock` command types
- How these dispatch to `KeyValueHandler` (or a new handler method)

### 3.10 KeyValueHandler Modifications

**File:** `hadoop-hdds/container-service/src/main/java/.../keyvalue/KeyValueHandler.java`

Specify:

- New methods: `handleRdmaReadBlock()` and `handleRdmaWriteBlock()`
- These reuse existing disk I/O logic from `readBlockImpl()` and `writeChunk()` but interface with RDMA instead of gRPC
  streaming
- Block token verification (reuse existing `checkBlockToken()`)

### 3.11 S3 Gateway: ObjectEndpoint Modifications

**File:** `hadoop-ozone/s3gateway/src/main/java/.../endpoint/ObjectEndpoint.java`

Specify:

- Detection of `x-amz-rdma-token` header in `handleGetRequest()` and `handlePutRequest()`
- When RDMA token is present:
  - Get block locations from OM (existing code)
  - Extract `DatanodeDetails` with RDMA port from each `Pipeline`
  - For each block, send `RdmaReadBlock`/`RdmaWriteBlock` command to the datanode via gRPC
  - Pass the RDMA token, block offset, and length
  - Wait for all completions
  - Return HTTP 200 with metadata (no data body for GET, no body consumed for PUT)
- When RDMA token is absent: existing path, zero change
- How to send the `RdmaReadBlock` command to a datanode: use existing `XceiverClientGrpc` or a new lightweight RPC

### 3.12 S3 Gateway: RdmaRequestFilter

**File:** `hadoop-ozone/s3gateway/src/main/java/.../filter/RdmaRequestFilter.java`

Specify:

- JAX-RS `ContainerRequestFilter` implementation
- Extracts `x-amz-rdma-token` header
- Stores parsed token in request context for downstream use by ObjectEndpoint
- Validates token format

---

## Section 4: C/C++ Integration Architecture

Generate a dedicated section with mermaid diagrams explaining:

### 4.1 JNI Architecture

- How Java calls native code via JNI
- Memory management: who allocates, who frees
- Thread model: which Java threads call JNI, thread safety of cuObjServer
- ByteBuffer handling: direct ByteBuffers for zero-copy between Java and native
- Error propagation: native exceptions → Java exceptions

### 4.2 Native Library Lifecycle

- When is `libgds_jni.so` loaded? (via `NativeLibraryLoader` in static initializer)
- How is cuObjServer initialized? (once per datanode process, in `XceiverServerRdma.start()`)
- How are RDMA resources cleaned up? (in `XceiverServerRdma.stop()`, before JVM shutdown)
- Shutdown hook ordering

### 4.3 RDMA Buffer Management

- Pre-allocated pool of RDMA-registered direct ByteBuffers
- Registration with cuObjServer at startup
- Allocation/return pattern during request handling
- Sizing: how many buffers, what size each (configurable)

### 4.4 Build System Integration

- Mermaid diagram showing the Maven build pipeline:
  - Java compilation → JNI header generation → CMake configure → make → package .so into JAR
- Conditional build: only when `-Dgds_native` is set
- Platform support: Linux x86_64 only (cuObject is Linux-only)
- CI considerations: how to run tests without GPU hardware (mock native layer)

### 4.5 Compiler and Toolchain Requirements

Generate a detailed section specifying all compiler and toolchain requirements for building the native JNI bridge. This
is critical because CUDA and cuObject have strict compiler compatibility constraints.

**CUDA Toolkit and Host Compiler Compatibility:**

NVIDIA nvcc delegates host-side compilation to GCC (on Linux). Not all GCC versions are compatible with all CUDA
versions. Generate the compatibility matrix:

| CUDA Toolkit Version | Maximum GCC Version | Minimum GCC Version | C++ Standard            |
|----------------------|---------------------|---------------------|-------------------------|
| CUDA 12.0–12.2       | GCC 12.2            | GCC 6               | C++17                   |
| CUDA 12.3–12.6       | GCC 12.3            | GCC 6               | C++17                   |
| CUDA 13.0+           | GCC 13.x (verify)   | GCC 7               | C++17 or C++20 (verify) |

**Note:** cuObject v1.0.0 requires CUDA 13.1.1+. If CUDA 13.1.1 is not yet available at implementation time, document
the exact CUDA version to use (likely 12.6.x) and any cuObject API differences.

Cite the CUDA release notes and compatibility guide for exact version
constraints: https://docs.nvidia.com/cuda/cuda-toolkit-release-notes/index.html
and https://docs.nvidia.com/cuda/cuda-installation-guide-linux/#host-compiler-support-policy

**Required toolchain components:**

| Component             | Minimum Version               | Recommended Version   | Notes                                                          |
|-----------------------|-------------------------------|-----------------------|----------------------------------------------------------------|
| GCC (g++)             | 11.x                          | 12.x                  | Must match CUDA compatibility; used as nvcc host compiler      |
| CMake                 | 3.20                          | 3.28+                 | Required for `FindCUDAToolkit`, `FindJNI`, modern CUDA support |
| CUDA Toolkit (nvcc)   | 12.4 (or 13.1.1 if available) | Latest compatible     | Includes nvcc compiler, CUDA headers, runtime libraries        |
| cuObject Server SDK   | 1.0.0                         | 1.0.0                 | Headers: `cuobjserver.h`; Library: `libcuobjserver.so`         |
| JDK (for JNI headers) | 8 or 11                       | 11                    | `javac -h` generates JNI headers; match Ozone's build JDK      |
| libibverbs-dev        | 1.14+                         | Latest from MLNX_OFED | InfiniBand verbs headers (transitive dep of cuObjServer)       |
| librdmacm-dev         | 1.3+                          | Latest from MLNX_OFED | RDMA connection manager headers                                |
| pkg-config            | Any                           | Latest                | For locating library paths                                     |

**C++ standard and compile flags:**

```cmake
# CMakeLists.txt requirements
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)  # Required for shared library loaded by JVM

# CUDA-specific flags
set(CMAKE_CUDA_STANDARD 17)
set(CMAKE_CUDA_ARCHITECTURES "70;80;90")  # Volta, Ampere, Hopper (match target GPUs)

# Warning and optimization flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -O2")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -O0")
```

**CUDA architecture targets:**

| GPU Generation | Compute Capability | Architecture Name | Instances Using It         |
|----------------|--------------------|-------------------|----------------------------|
| V100           | 7.0                | Volta             | Lambda 1x_v100             |
| A100           | 8.0                | Ampere            | Lambda 1x_a100, Vultr A100 |
| H100           | 9.0                | Hopper            | Lambda 1x_h100             |

Generate the CMake flag `CMAKE_CUDA_ARCHITECTURES` to target all three, with explanation of binary vs PTX compilation
and the `-gencode` flags nvcc uses.

**Platform verification script:**

Generate the spec for a `check-toolchain.sh` script that:

- Checks GCC version and validates against CUDA compatibility
- Checks CMake version
- Checks nvcc version and CUDA toolkit path
- Checks for cuObjServer headers and libraries (`pkg-config --cflags --libs cuobjserver` or manual path check)
- Checks for JNI headers (`$JAVA_HOME/include/jni.h`)
- Checks for libibverbs headers
- Reports any version incompatibilities
- Exits with clear error message if any required component is missing or incompatible

**Cross-compilation note:** The JNI `.so` is built only on Linux x86_64. It is NOT cross-compiled. When building Ozone
on macOS or other platforms, the native module is skipped (Maven profile activation:
`<os><name>Linux</name><arch>amd64</arch></os>`). All CI builds for the RDMA module must run on Linux x86_64 with CUDA
installed. Tests without GPU hardware should mock the native layer.

---

## Section 5: Testing

### 5.1 Unit Tests

Specify tests for each new Java class:

- `CuObjServerBridgeTest`: Test that native library loads (or gracefully reports unavailable). Mock native layer for CI
  without GPU.
- `XceiverServerRdmaTest`: Test lifecycle (start/stop), port registration, configuration parsing. Use mock
  `CuObjServerBridge`.
- `RdmaRequestFilterTest`: Test header extraction, missing header, malformed header.
- `ObjectEndpoint` RDMA path test: Test that when RDMA token is present, the gateway sends `RdmaReadBlock` to datanodes
  instead of reading data itself.

### 5.2 Integration Tests with MiniOzoneCluster

Specify tests that:

- Start MiniOzoneCluster with RDMA-enabled datanodes (mocked RDMA for in-JVM testing)
- PUT an object via S3 Gateway with RDMA token header
- GET an object via S3 Gateway with RDMA token header
- Verify that the correct `RdmaReadBlock`/`RdmaWriteBlock` commands were dispatched to the correct datanodes
- Verify fallback: PUT/GET without RDMA token uses existing TCP path
- Verify error handling: RDMA failure triggers fallback
- Small file test: object below `ozone.s3g.rdma.min.object.size` should use TCP even with RDMA token
- Large file test: object spanning multiple blocks triggers parallel RDMA to multiple datanodes

### 5.3 Integration Tests with Docker Cluster

For testing with real RDMA (or SoftRoCE), specify tests using the Docker Compose cluster from Section 1.8:

- `docker-compose up` starts the full Ozone cluster with GPU/RDMA passthrough
- A test script (Python or shell) runs against the Dockerized cluster:
  - Creates a bucket
  - PUTs objects of various sizes (1KB, 1MB, 100MB, 1GB) via both TCP and RDMA paths
  - GETs them back via both paths
  - Verifies data integrity (MD5/SHA256 match)
  - Verifies that RDMA path was actually used (check datanode metrics/logs for RDMA transfer counts)
- Explain when to use MiniOzoneCluster vs Docker cluster:
  - MiniOzoneCluster: unit/integration tests in IDE, mocked RDMA, fast iteration, CI
  - Docker cluster: end-to-end tests with real RDMA or SoftRoCE, closer to production

### 5.4 End-to-End RDMA Tests (Requires Hardware)

Specify tests that require actual GPU + RDMA hardware:

- Start real Ozone cluster (1 OM, 1 SCM, 1-3 datanodes)
- cuObjClient writes 1GB GPU buffer to Ozone via S3 PUT + RDMA
- cuObjClient reads it back via S3 GET + RDMA
- Verify data integrity (checksum match)
- Verify data landed in GPU memory (not CPU memory) — use `nvidia-smi` or CUDA memory query to confirm
- Test large file (>4GiB) via Range GET chunking
- Test concurrent transfers from multiple GPU processes
- Test failover: kill one datanode, verify retry on replica

### 5.5 MiniOzone RDMA Integration Test Pattern

Show the exact test class structure following Ozone's existing patterns:

- Extend `ClusterForTests<MiniOzoneCluster>` or use `@TestInstance(PER_CLASS)`
- Configure MiniOzoneCluster builder with RDMA settings
- Use mock RDMA layer for CI
- Include the mock `CuObjServerBridge` implementation that records calls instead of performing RDMA

---

## Section 6: Performance Benchmarking and Verification

The spec for this section must be detailed enough that an AI can generate all benchmark scripts directly.

### 6.1 Benchmark Architecture

Generate a mermaid diagram showing:

- GPU client running benchmark tool
- Ozone cluster (configurable: 1 or 3 datanodes)
- Metrics collection points (gateway, datanode, GPU client)
- What is measured at each point

### 6.2 Benchmark Tool: `ozone-rdma-bench`

Generate the **complete specification** for a Python benchmark tool (using `boto3` for baseline, `cuObjClient` Python
bindings or C wrapper for RDMA) that:

**Modes:**

- `baseline-write`: Write N objects using standard boto3 S3 client (TCP path)
- `baseline-read`: Read N objects using standard boto3 S3 client (TCP path)
- `rdma-write`: Write N objects using cuObjClient (RDMA path)
- `rdma-read`: Read N objects using cuObjClient (RDMA path)
- `compare`: Run all four modes and produce comparison

**Parameters:**

- `--endpoint`: S3 Gateway URL
- `--bucket`: Bucket name
- `--num-objects`: Number of objects (default 100)
- `--object-sizes`: Comma-separated sizes to test (e.g., "64KB,1MB,10MB,100MB,1GB")
- `--gpu-id`: GPU device ID (default 0)
- `--concurrent`: Number of concurrent transfers (default 1)
- `--output`: Output format (table, csv, json)
- `--verify`: Enable data integrity verification (checksum comparison)

**Metrics collected:**

- Per-operation latency (ns)
- Aggregate throughput (MB/s)
- p50, p95, p99 latency
- CPU utilization on client, gateway, datanode (via `psutil` or `/proc/stat`)
- GPU memory bandwidth utilization (via `nvidia-smi dmon`)
- Number of RDMA vs TCP transfers (from datanode metrics)

**Output:**

- Summary table comparing baseline vs RDMA for each object size
- CSV file with per-operation data for detailed analysis
- Sanity check: verify that RDMA reads return identical data to baseline reads

Generate the complete class/function structure, CLI argument parsing, and measurement logic. The spec should be detailed
enough to generate the full Python script.

### 6.3 Expected Results

Explain what speedup we expect and why, with specific numbers where possible:

- For large objects (100MB+): 2-8x throughput improvement (cite NVIDIA published benchmarks)
- For medium objects (1-10MB): 1.5-3x improvement
- For small objects (<256KB): Likely slower due to RDMA overhead — below crossover point
- CPU utilization: 30-60% reduction on gateway (no data plane), 10-20% reduction on datanode (RDMA offloads TCP stack)
- Latency: First-byte latency may be higher (RDMA setup), but total transfer time significantly lower for large objects
- Include references to NVIDIA's published MinIO + cuObject benchmarks for context

### 6.4 Complete Benchmark Script: `run-benchmark.sh`

Generate the spec for a **complete shell script** that:

1. Validates environment (calls `validate-env.sh`)
2. Starts Ozone cluster (MiniOzoneCluster or Docker, configurable)
3. Creates test bucket
4. Runs `ozone-rdma-bench --mode=compare` with all object sizes
5. Collects metrics from all nodes
6. Produces a formatted comparison report
7. Validates data integrity (writes via RDMA, reads via TCP, compares checksums — and vice versa)
8. Tears down cluster
9. Exits with pass/fail status

The script should be fully autonomous — no manual steps.

### 6.5 Regression Test Script: `ci-rdma-test.sh`

Generate the spec for a CI-friendly script that:

- Runs with mocked RDMA (no GPU hardware required)
- Uses MiniOzoneCluster
- Verifies all RDMA code paths are exercised (via mock assertions)
- Runs in < 5 minutes
- Returns exit code 0 on pass, non-zero on failure
- Suitable for GitHub Actions or Jenkins

---

## Section 7: Configuration Reference

Generate a complete configuration reference table:

- All new Ozone configuration keys
- Default values
- Description
- Which component uses each key

---

## Section 8: Deployment Guide

### 8.1 Single-Node Development

How to run everything on one machine for development:

- MiniOzoneCluster in IDE with RDMA mocked
- MiniOzoneCluster with SoftRoCE for local RDMA testing

### 8.2 Multi-Node Test Cluster

How to deploy on Lambda/Vultr:

- Node 1: GPU client (cuObjClient, benchmark tool)
- Node 2: Ozone cluster (OM + SCM + Datanode + S3 Gateway + cuObjServer)
- Network configuration for RDMA between nodes
- Security: block tokens, TLS

---

## Key Constraints and Decisions

When generating the spec, adhere to these constraints:

1. **Use cuObjServer library from NVIDIA for all RDMA operations.** Do not implement raw libibverbs. cuObjServer handles
   RDMA connection management, DC transport, buffer registration, and memory operations internally.

2. **cuObject requires CUDA 13.1.1+.** If this is not yet available, document the workaround (use CUDA 12.x with earlier
   GDS APIs and note what changes when 13.1.1 ships).

3. **The S3 Gateway is control plane only for RDMA requests.** It resolves block locations and dispatches commands to
   datanodes. Data never passes through the gateway for RDMA requests.

4. **The disk I/O path on datanodes does NOT change.** Data is read/written via the existing `FileChannel`/
   `ChunkManager` path. Only the transport between datanode and client changes (gRPC → RDMA).

5. **Graceful degradation is mandatory.** If cuObjServer is not available (no GPU, no RDMA NIC, native lib not loaded),
   the datanode works exactly as today. If the client doesn't send RDMA token, the request goes through the normal TCP
   path.

6. **Target Ozone master branch** (current HEAD).

7. **Follow existing Ozone patterns.** The JNI module follows `hadoop-hdds/rocks-native/`. The server follows
   `XceiverServerGrpc`. The port type follows existing `DatanodeDetails.Port.Name` enum. Tests follow MiniOzoneCluster
   patterns.

8. **Single-node development with MiniOzoneCluster** is the primary development workflow. Multi-node is for integration
   testing.

---

## Ozone Codebase Reference

These are the key existing files and patterns to follow:

| What                         | File                                                                                                                                                                           |
|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| S3 GET handler               | `hadoop-ozone/s3gateway/src/main/java/org/apache/hadoop/ozone/s3/endpoint/ObjectEndpoint.java` (line 389: `handleGetRequest`)                                                  |
| S3 PUT handler               | Same file, `handlePutRequest`                                                                                                                                                  |
| S3 streaming PUT             | `hadoop-ozone/s3gateway/src/main/java/org/apache/hadoop/ozone/s3/endpoint/ObjectEndpointStreaming.java`                                                                        |
| S3 Gateway main              | `hadoop-ozone/s3gateway/src/main/java/org/apache/hadoop/ozone/s3/Gateway.java`                                                                                                 |
| Datanode transport interface | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/transport/server/XceiverServerSpi.java`                                                  |
| gRPC transport impl          | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/transport/server/XceiverServerGrpc.java`                                                 |
| gRPC service                 | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/transport/server/GrpcXceiverService.java`                                                |
| Container startup            | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/ozoneimpl/OzoneContainer.java` (line 237: writeChannel, line 250: readChannel, line 574: start) |
| Datanode main                | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/HddsDatanodeService.java`                                                                                 |
| Port types                   | `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/protocol/DatanodeDetails.java` (line 863: Port.Name enum)                                                             |
| Protobuf definitions         | `hadoop-hdds/interface-client/src/main/proto/DatanodeClientProtocol.proto` (line 37: Type enum, line 160: request message)                                                     |
| Container dispatcher         | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/common/impl/HddsDispatcher.java`                                                                |
| Block handler                | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/KeyValueHandler.java` (line 2070: readBlock)                                           |
| Block read from disk         | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/impl/FilePerBlockStrategy.java`                                                        |
| Native library loader        | `hadoop-hdds/rocks-native/src/main/java/org/apache/hadoop/hdds/utils/NativeLibraryLoader.java`                                                                                 |
| Native Maven POM             | `hadoop-hdds/rocks-native/pom.xml` (full CMake + JNI build pattern)                                                                                                            |
| Key location info            | `hadoop-ozone/common/src/main/java/org/apache/hadoop/ozone/om/helpers/OmKeyLocationInfo.java`                                                                                  |
| Pipeline (block locations)   | `hadoop-hdds/common/src/main/java/org/apache/hadoop/hdds/scm/pipeline/Pipeline.java`                                                                                           |
| Block input stream           | `hadoop-hdds/client/src/main/java/org/apache/hadoop/hdds/scm/storage/BlockInputStream.java`                                                                                    |
| Client connection manager    | `hadoop-hdds/client/src/main/java/org/apache/hadoop/hdds/scm/XceiverClientManager.java`                                                                                        |
| MiniOzoneCluster             | `hadoop-ozone/mini-cluster/src/main/java/org/apache/hadoop/ozone/MiniOzoneCluster.java`                                                                                        |
| MiniOzoneCluster impl        | `hadoop-ozone/mini-cluster/src/main/java/org/apache/hadoop/ozone/MiniOzoneClusterImpl.java`                                                                                    |
| S3 integration tests         | `hadoop-ozone/integration-test-s3/src/test/java/org/apache/hadoop/ozone/s3/`                                                                                                   |
| ChunkManager interface       | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/interfaces/ChunkManager.java`                                                          |
| Chunk utilities              | `hadoop-hdds/container-service/src/main/java/org/apache/hadoop/ozone/container/keyvalue/helpers/ChunkUtils.java`                                                               |

---

---

## Section 9: References Appendix

Generate a numbered references appendix containing all sources used. Include at minimum:

- [ref-1] NVIDIA cuObject Documentation: https://docs.nvidia.com/gpudirect-storage/cuobject/index.html
- [ref-2] NVIDIA cuObject Client v1.0.0 Release
  Notes: https://docs.nvidia.com/gpudirect-storage/cuobject/cuobject-client-release-notes/index.html
- [ref-3] NVIDIA cuObject Server v1.0.0 Release
  Notes: https://docs.nvidia.com/gpudirect-storage/cuobject/pdf/cuobject-server-release-notes.pdf
- [ref-4] GPUDirect Storage Overview Guide: https://docs.nvidia.com/gpudirect-storage/overview-guide/index.html
- [ref-5] GPUDirect Storage Installation and
  Troubleshooting: https://docs.nvidia.com/gpudirect-storage/troubleshooting-guide/index.html
- [ref-6] GPUDirect RDMA Documentation: https://docs.nvidia.com/cuda/gpudirect-rdma/
- [ref-7] GDS cuFile API Reference: https://docs.nvidia.com/gpudirect-storage/api-reference-guide/index.html
- [ref-8] NVIDIA MLNX_OFED
  Documentation: https://docs.nvidia.com/networking/display/nvidia-mlnx-ofed-documentation-v24-10-2-1-8-0-lts
- [ref-9] MLNX_OFED Firmware-Driver Compatibility Matrix: https://network.nvidia.com/support/mlnx-ofed-matrix/
- [ref-10] MinIO AIStor with NVIDIA GPUDirect
  RDMA: https://www.min.io/blog/minio-aistor-with-nvidia-gpudirect-r-rdma-for-s3-compatible-storage-unlocking-performance-for-ai-factory-workloads
- [ref-11] PyTorch GPUDirect Storage Tutorial: https://docs.pytorch.org/tutorials/unstable/gpu_direct_storage.html
- [ref-12] CUDA Programming Guide - Asynchronous
  Execution: https://docs.nvidia.com/cuda/cuda-programming-guide/02-basics/asynchronous-execution.html
- [ref-13] How to Overlap Data Transfers in CUDA: https://developer.nvidia.com/blog/how-overlap-data-transfers-cuda-cc/
- [ref-14] KvikIO GitHub Repository: https://github.com/rapidsai/kvikio
- [ref-15] Apache Ozone S3 Gateway Documentation: https://ozone.apache.org/docs/edge/interface/s3.html
- [ref-16] NVIDIA Developer Forums - cuObject
  Example: https://forums.developer.nvidia.com/t/unable-to-reproduce-cuobject-example/360539
- [ref-17] DOCA RDMA Programming Guide: https://docs.nvidia.com/doca/archive/doca-v2.2.0/pdf/rdma-programming-guide.pdf
- [ref-18] Lambda Cloud GPU Instances: https://lambdalabs.com/service/gpu-cloud
- [ref-19] Vultr GPU Instances: https://www.vultr.com/products/cloud-gpu/

Add any additional sources discovered during spec generation. Every technical claim in the spec should be traceable to a
reference.

---

## Output Format

Generate the spec as a single markdown document with:

- Table of contents
- Numbered sections matching the structure above
- Mermaid diagrams (```mermaid blocks) for all architecture and flow illustrations
- Code blocks with language tags for all code snippets
- Tables for configuration, hardware requirements, and comparisons
- Inline source citations using `[ref-N]` notation
- Exact file paths for every code change
- Complete protobuf definitions (not just descriptions)
- Complete Java class skeletons (not just method names)
- Complete CMakeLists.txt
- Complete pom.xml for the new native module
- Complete test class structures
- Complete benchmark tool structure (function signatures, CLI args, measurement logic)
- Complete shell script specs for setup, validation, and benchmarking
- References appendix with all sources numbered and linked
