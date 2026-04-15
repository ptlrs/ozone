# Review of Audit Findings Against Spec v9.0

**Date:** 2026-04-14
**Scope:** Independent validation of every claim in `gds-dn-direct-rdma-implementation-spec-audit.md` against the actual Ozone codebase and spec text.

---

## Executive Summary

The audit identified 5 blocking issues and 8 semantic gaps. After cross-referencing each against the codebase:

- **4 of 5 blocking issues are VALID** and need spec fixes
- **1 blocking issue is OVERSTATED** (Phase 1/2 mixing)
- **7 of 8 semantic gaps are VALID** (ranging from critical to minor)
- **1 semantic gap is PARTIALLY VALID** (client acquisition API)

The audit is substantially correct. These are real issues that would break compilation or produce incorrect S3 behavior. The spec needs targeted fixes before it's safe for implementation.

---

## Blocking Issues — Verdict Per Finding

### 1. "Phase 1 and Phase 2 are mixed together"

**Verdict: OVERSTATED — the spec already has clear boundaries.**

The audit claims "later sections still define `RdmaWriteBlock`, `rdmaReadFromRemote`, and other PUT-side code as if they were part of the implementation." After checking every occurrence:

| Location | What | Phase Label |
|----------|------|-------------|
| Spec line 138 | Phase scope table | Explicitly says "deferred → Phase 2" |
| Spec line 751 | `RdmaWriteBlock = 26` proto enum | Comment: `// Reserved for Phase 2` |
| Spec lines 782-793 | `RdmaWriteBlockRequestProto` / `ResponseProto` | Header: `// Phase 2 (reserved)` |
| Spec line 802 | `rdmaWriteBlock = 30` request field | Comment: `// Phase 2` |
| Spec line 808 | `rdmaWriteBlock = 27` response field | Comment: `// Phase 2` |
| Spec line 870 | `RdmaTransferService` docstring | "Phase 2 will add rdmaReadFromRemote for PUT." |

Every Phase 2 artifact is clearly annotated. Including reserved protobuf field numbers is good practice — it prevents field-number collisions when Phase 2 is eventually implemented. The `rdmaReadFromRemote` reference is a docstring note, not code.

**No spec change needed.** An implementation agent reading the Phase 1 scope table (lines 124-141) would not accidentally build Phase 2 pieces. However, adding a bold note at the top of Section 4.1 reinforcing "implement only `RdmaReadBlock`; write-side messages are reserved field numbers only" would be a minor improvement.

### 2. "The spec uses Java syntax that does not compile under this repo's build"

**Verdict: VALID — two separate issues confirmed.**

**Evidence:**
- Root `pom.xml` line 155: `<maven.compiler.release>8</maven.compiler.release>`
- Only exception: `hadoop-ozone/iceberg/pom.xml` overrides to Java 11 for Iceberg API

**Issue 2a: `var` keyword (Java 10+)**

The spec uses `var` in three places in `handleRdmaGet()`:
- Line 2224: `for (var f : futures)`
- Line 2225: `var response = f.get();`
- Line 2232: `var csData = response.getRdmaReadBlock().getChecksumData();`

These will not compile with `maven.compiler.release = 8`. The codebase has zero uses of `var` anywhere.

**Fix:** Replace with explicit types.

**Issue 2b: `java.util.zip.CRC32C` (Java 9+)**

The spec uses `new java.util.zip.CRC32C()` directly at line 1804 in `XceiverServerRdma.handleRdmaRead()`.

The existing codebase handles this carefully via `ChecksumByteBufferFactory` which uses MethodHandles reflection to load `CRC32C` at runtime with fallback to `PureJavaCrc32CByteBuffer`. Direct usage of `java.util.zip.CRC32C` will compile (the bytecode target allows referencing JDK 9+ classes at compile time with `--release 8` only if they're not used) — **actually, `--release 8` will reject it at compile time because `--release` checks the JDK 8 API surface.** This is a real compile blocker.

**Fix:** Use the existing `ChecksumByteBufferFactory` or `Checksum` utility classes from `org.apache.hadoop.ozone.common`.

### 3. "The native bootstrap snippet calls the wrong NativeLibraryLoader API"

**Verdict: VALID.**

**Evidence from `NativeLibraryLoader.java`:**
```java
public synchronized boolean loadLibrary(
    final String libraryName, final List<String> dependentFiles)
```

The only `loadLibrary` method requires **two parameters**: `(String, List<String>)`.

The spec at line 1020 calls:
```java
NativeLibraryLoader.getInstance().loadLibrary(LIBRARY_NAME);
```

This is a **one-argument call** that will not compile — no such overload exists.

**Fix:** Change to `NativeLibraryLoader.getInstance().loadLibrary(LIBRARY_NAME, Collections.emptyList())` or use `System.loadLibrary()` directly (which is what other native modules in Ozone do).

### 4. "The gateway RDMA filter handoff is coded against the wrong object"

**Verdict: VALID, but the audit's description is slightly imprecise.**

The audit says "`ObjectRequestContext` does not expose `getProperty()`." This is correct — `ObjectRequestContext` is a custom inner class extending `S3RequestContext` with no `getProperty()` method.

However, the spec at line 2084 uses:
```java
String rdmaToken = (String) context
    .getProperty(RdmaRequestFilter.RDMA_TOKEN_PROPERTY);
```

The variable name `context` is ambiguous. In `ObjectEndpoint`, the word "context" could refer to:
- The `ContainerRequestContext` (JAX-RS), accessed via `getContext()` from `EndpointBase` — this **does** have `getProperty()`
- The `ObjectRequestContext` parameter passed to internal methods — this does **not** have `getProperty()`

The spec's code appears in `handleGetRequest()` where the JAX-RS `ContainerRequestContext` is accessible. If `context` here refers to the inherited field from `EndpointBase`, the call would be `getContext().getProperty(...)` (since the field is private, accessed via getter).

**Fix:** Change to `getContext().getProperty(RdmaRequestFilter.RDMA_TOKEN_PROPERTY)` to use the correct accessor.

### 5. "The `maxConnections` knob is not mapped to a documented cuObjServer parameter"

**Verdict: VALID.**

The spec defines `hdds.datanode.rdma.max.connections` (line 1709) and passes it to `RdmaTransferService.initialize()` as the third parameter. But the cuObjServer constructor takes `(ip, port, proto)` — there is no `maxConnections` parameter.

Appendix D (line 3296) documents `cuObjRDMATunableParam` with fields like `num_dcis`, `cq_depth`, `max_sge`, but these are separate from the constructor. The spec never shows `maxConnections` being mapped to any of these tunables.

In the C++ implementation (line 1105), `nativeInit` passes `(ip, port, proto)` to the cuObjServer constructor. The `maxConnections` value from Java is never used.

**Fix:** Either (a) remove `maxConnections` from the interface and config, or (b) map it to a real cuObjServer tunable like `num_dcis` in `cuObjRDMATunableParam`, or (c) document that it's reserved for future use and currently ignored.

---

## Semantic Gaps — Verdict Per Finding

### 6. "RDMA GET response does not preserve the normal GET response shape"

**Verdict: VALID — this is the most critical semantic gap.**

The normal `ObjectEndpoint.handleGetRequest()` response includes:

| Header/Feature | Normal GET | RDMA GET (spec) |
|----------------|-----------|-----------------|
| `Accept-Ranges: bytes` | YES (line 493) | **MISSING** |
| `ETag` (quoted) | YES — `wrapInQuotes(eTag)` (line 552) | **UNQUOTED** (line 2254) |
| `Last-Modified` | YES — RFC1123 format (lines 524-533) | **MISSING** |
| `x-amz-tagging-count` | YES if tags exist (lines 535-544) | **MISSING** |
| Custom metadata headers | Present in HEAD, inconsistent in GET | **MISSING** |
| `Content-Type` override via query params | YES (lines 499-508) | **MISSING** |
| Success metrics | YES (`incGetKeySuccessLength`, etc.) | **MISSING** |
| 200 vs 206 logic | Based on `rangeHeader.isReadFull()` | Based on `rangeHeader != null` (wrong) |
| Response body | Streaming data | Empty (correct for RDMA) |

The 200/206 distinction is particularly wrong: a range request for the full object (e.g., `Range: bytes=0-`) should return 200, not 206. The normal path uses `rangeHeader.isReadFull()` for this check; the RDMA path treats any non-null rangeHeader as partial.

**Fix:** Build the RDMA response using the same header-setting helpers the normal path uses (`addEntityTagHeader()`, `addLastModifiedDate()`, `addTagCountIfAny()`), and use `rangeHeader.isReadFull()` for status code selection.

### 7. "RDMA GET path does not state what to do about EC buckets or multipart part-number requests"

**Verdict: VALID.**

The normal GET path handles `partNumber` query parameter (line 407 of ObjectEndpoint). The RDMA `handleRdmaGet()` method never accesses `partNumber`. If a client requests a specific part via `?partNumber=3`, RDMA would ignore it and attempt to read the entire object.

EC (Erasure Coding) buckets have different block layout semantics. The RDMA path does not check for EC replication and may produce incorrect block-to-offset mappings.

**Fix:** Add explicit fallback to TCP for both cases in Phase 1:
```java
// At the top of handleRdmaGet():
if (partNumber != null) return null;  // fallback to TCP
if (keyDetails.getReplicationConfig().getReplicationType() == EC) return null;
```

### 8. "Fallback after a partial RDMA transfer is unsafe"

**Verdict: VALID.**

The spec's `handleRdmaGet()` at line 2242 catches exceptions from `CompletableFuture.allOf(futures).join()` and returns `null` to fall back to TCP. But some futures may have already completed successfully, meaning RDMA_WRITE has already pushed bytes into the client's buffer. A TCP fallback would then stream the *entire* object into the normal HTTP response body, and the client would have:
- Partial data in RDMA buffer (from completed blocks)
- Full data in HTTP response body (from TCP fallback)

The client has no way to reconcile these. This is a data integrity risk.

**Fix:** Once any RDMA block transfer has succeeded, do NOT fall back to TCP. Return an error response instead and let the client retry the entire request. Modify the join/catch logic:
```java
// Track whether any transfer completed before failure
boolean anyCompleted = false;
for (CompletableFuture<?> f : futures) {
  if (f.isDone() && !f.isCompletedExceptionally()) {
    anyCompleted = true;
    break;
  }
}
if (anyCompleted) {
  // Cannot safely fall back — some data already written to client buffer
  return Response.status(503).header("Retry-After", "1").build();
}
return null; // safe to fall back to TCP — no data transferred
```

### 9. "Datanode checksum path computes CRC32C but does not verify against stored checksum metadata"

**Verdict: VALID.**

The spec's `XceiverServerRdma.handleRdmaRead()` (lines 1802-1812) computes a fresh CRC32C over the data read from disk, but never compares it against the checksums stored in the block's metadata (which were computed at write time). This means disk corruption that happens to produce valid-looking data would not be caught.

The existing TCP read path verifies data against stored checksums via `ChunkManager` / `BlockChecksumHelper`. The RDMA path bypasses this entirely.

**Fix:** Before RDMA_WRITE, verify each chunk's CRC32C against the stored `ChecksumData` from the block's metadata. If verification fails, return an error rather than silently sending corrupted data.

### 10. "Datanode checksum size is wrong in the snippet"

**Verdict: VALID.**

Line 1844: `setBytesPerChecksum(bufSize)` uses the RDMA buffer capacity (default 4 MiB) as the checksum granularity. But the block's actual checksum granularity (set at write time) is typically 256 KiB or 1 MiB, controlled by `ozone.client.checksum.chunk.size`. Using the wrong granularity means:
- The checksums returned to the gateway don't match the block's stored checksums
- A client attempting to verify checksums would get mismatches

**Fix:** Read the block's actual `ChecksumData` metadata to get the real `bytesPerChecksum`, or compute checksums at the RDMA buffer granularity and document that these are transfer-integrity checksums (not block-metadata checksums).

### 11. "Gateway read path uses the wrong client-acquisition API"

**Verdict: PARTIALLY VALID.**

The audit says existing read flows use `acquireClientForReadData()` / `releaseClientForReadData()`, but the spec uses `acquireClient()` / `releaseClient()`.

Both methods exist in `XceiverClientFactory`:
- `acquireClient(Pipeline)` — generic client acquisition (line 38)
- `acquireClientForReadData(Pipeline)` — read-specific (line 55)

In the `XceiverClientCreator` base class, `acquireClientForReadData()` delegates to `acquireClient()` anyway, but also sets `topologyAwareRead` flag in the release path. The functional difference is topology-aware node selection for reads.

For RDMA, the gateway is dispatching to a *specific* datanode (the one holding the block), so topology-aware read routing is less relevant — the target is already determined by block location. However, using the read-specific API is still more correct semantically and future-proofs against changes in the client manager.

**Fix:** Change to `acquireClientForReadData()` / `releaseClientForReadData()` for consistency.

### 12. "RpcClient.getKeyInfo helper drifts from existing behavior"

**Verdict: VALID.**

`RpcClient.java` already has at lines 1896-1911:
```java
public OmKeyInfo getKeyInfo(String volumeName, String bucketName,
    String keyName, boolean forceUpdateContainerCache) throws IOException
```

This existing 4-argument overload correctly sets `topologyAwareReadEnabled`, `latestVersionLocation`, and `forceUpdateContainerCacheFromSCM`. The spec's proposed 3-argument helper (lines 2047-2057) hard-codes `setSortDatanodes(true)` and `setLatestVersionLocation(true)` but misses `topologyAwareReadEnabled` and uses a different code path.

**Fix:** Remove the spec's 3-argument helper. Use the existing 4-argument method: `((RpcClient) proxy).getKeyInfo(volumeName, bucketName, keyName, false)`.

### 13. "Block-range math uses reference equality"

**Verdict: VALID — latent bug.**

Spec line 2154:
```java
if (loc == locations.get(0)) break;
```

This uses `==` (reference equality) to find where the first selected block sits in the full block list. This works *only* if `locations` is a sublist of `allLocations` sharing the same object references. Since `locations` is built by `locations.add(loc)` where `loc` comes from iterating `allLocations`, the references *do* match in this specific code. However:
- It's fragile — any refactoring that copies or wraps location objects would break it
- It's confusing to read — index comparison or `blockID` comparison is clearer

**Fix:** Replace with index-based or blockID-based comparison:
```java
long blockStartGlobal = 0;
BlockID firstSelectedId = locations.get(0).getBlockID();
for (OmKeyLocationInfo loc : allLocations) {
  if (loc.getBlockID().equals(firstSelectedId)) break;
  blockStartGlobal += loc.getLength();
}
```

---

## Summary Table

| # | Finding | Verdict | Severity | Spec Fix Needed |
|---|---------|---------|----------|-----------------|
| 1 | Phase 1/2 mixed | OVERSTATED | Low | Minor clarification |
| 2a | `var` keyword (Java 10+) | VALID | **Compile blocker** | Replace with explicit types |
| 2b | `java.util.zip.CRC32C` (Java 9+) | VALID | **Compile blocker** | Use existing checksum utils |
| 3 | NativeLibraryLoader wrong API | VALID | **Compile blocker** | Add `Collections.emptyList()` param |
| 4 | Gateway context.getProperty() | VALID | **Compile blocker** | Use `getContext().getProperty()` |
| 5 | maxConnections unmapped | VALID | Medium | Remove or map to real tunable |
| 6 | RDMA response shape mismatch | VALID | **S3 behavior break** | Reuse existing response helpers |
| 7 | EC / part-number not handled | VALID | **S3 behavior break** | Add fallback to TCP |
| 8 | Unsafe partial fallback | VALID | **Data integrity** | Block fallback after partial transfer |
| 9 | Checksum not verified against stored | VALID | **Data integrity** | Verify against block metadata |
| 10 | Checksum granularity wrong | VALID | Medium | Use block's actual bytesPerChecksum |
| 11 | Wrong client-acquisition API | PARTIALLY VALID | Low | Use read-specific API |
| 12 | getKeyInfo drifts from existing | VALID | Medium | Use existing 4-arg overload |
| 13 | Reference equality in block math | VALID | Low (latent bug) | Use blockID comparison |

---

## Why Didn't Previous Audits Catch These?

The spec went through multiple rounds of validation (v6.0 through v9.0) that focused on:

1. **Factual correctness of external claims** — NVIDIA API signatures, cloud pricing, file paths, proto field numbers. These were verified well.
2. **Architectural coherence** — the overall design is sound.
3. **Hallucination detection** — several hallucinated facts (PyPI packages, wrong instance types, incorrect pricing) were caught and fixed.

But the audits missed a different class of problems:

### What the previous audits DID well
- Verified NVIDIA API signatures match documentation
- Verified codebase facts (file paths, proto field numbers, enum values)
- Caught hallucinated vendor/pricing claims
- Validated architecture and data flow correctness

### What the previous audits MISSED and why

**Category 1: Build-system compatibility (issues 2a, 2b, 3)**
Previous audits verified *what* APIs exist but not *whether the project's build constraints allow calling them*. This is a cross-cutting concern that requires checking the build system (`pom.xml`) against every code snippet — it's easy to miss because the code *looks* correct in isolation.

**Category 2: Behavioral parity with existing code paths (issues 6, 7, 11, 12)**
Previous audits verified that the *new* code is internally consistent, but didn't diff it against the *existing* code it's supposed to be equivalent to. The RDMA GET response *works* — it just doesn't match the existing S3 GET response. Catching this requires reading `ObjectEndpoint.handleGetRequest()` line by line and checking every header, status code, and side effect against the RDMA path.

**Category 3: Safety invariants at boundaries (issues 8, 9, 13)**
These are subtle correctness issues that only emerge when you think about failure modes and edge cases. "What happens if 3 of 5 blocks succeed and the 4th fails?" or "Does the checksum path actually verify or just compute?" require adversarial thinking about the code, not just checking that it compiles and runs the happy path.

**Category 4: API surface mismatches (issues 3, 4, 5)**
The spec calls `NativeLibraryLoader.getInstance().loadLibrary(LIBRARY_NAME)` — this looks perfectly reasonable if you assume `loadLibrary(String)` exists. The audit caught it by actually reading the class and discovering there's only a two-argument overload. Previous audits verified the *existence* of `NativeLibraryLoader` but not the *exact signature* of every method called.

---

## What Prompt Would Have Caught These Issues?

The previous audit prompts were essentially: *"Verify the facts in this spec against the codebase and NVIDIA docs."* This is good for catching hallucinations but insufficient for catching integration issues.

A more effective prompt would be structured in three phases:

### Phase 1: Compilation Feasibility Audit (catches issues 2a, 2b, 3, 4)

```
You are auditing a Java implementation specification for compilation feasibility
against the Apache Ozone codebase.

For EVERY Java code snippet in the spec:
1. Check the project's maven.compiler.release / source / target level in pom.xml.
   Flag any Java language features or JDK APIs that exceed this level.
2. For every method call on an existing class (not a new class defined in the spec),
   grep for the class in the codebase and verify the EXACT method signature exists —
   including parameter count, parameter types, and return type. Do not assume
   overloads exist.
3. For every @Context injection or inherited field access, verify the exact type
   and available methods by reading the actual base class.
4. For every import, verify the class exists and is accessible at the project's
   Java level.

Report each violation with: file, line, the call/syntax used, and what the codebase
actually provides.
```

### Phase 2: Behavioral Parity Audit (catches issues 6, 7, 11, 12)

```
You are auditing whether the RDMA code path produces S3-compatible responses that
match the existing TCP code path.

For the RDMA GET response in handleRdmaGet():
1. Read ObjectEndpoint.handleGetRequest() end-to-end. List every HTTP header set,
   every status code branch, every metric call, and every special-case handler
   (EC, multipart, part-number, TDE, conditional requests).
2. For each item in that list, check whether the RDMA path produces the same
   behavior. Flag any omission as a semantic gap.
3. Check that the RDMA path uses the same client-acquisition and key-info APIs
   that the existing read path uses. If it uses different methods on the same
   interfaces, explain the behavioral difference.
4. Check that response headers match S3 spec requirements (e.g., ETag must be
   quoted per RFC 7232).
```

### Phase 3: Failure Mode and Safety Audit (catches issues 8, 9, 10, 13)

```
You are auditing the RDMA implementation for safety under failure conditions
and edge cases.

1. PARTIAL FAILURE: The RDMA GET dispatches N blocks in parallel. What happens
   if blocks 1-3 succeed (data written to client buffer via RDMA_WRITE) but
   block 4 fails? Can the client recover? Does the fallback-to-TCP path
   leave the client in a consistent state?
2. DATA INTEGRITY: The spec says checksums are computed and verified. Trace
   the actual code: does it (a) compute a new checksum, (b) read the stored
   checksum from block metadata, and (c) compare them? If it only computes
   without comparing, that's a verification gap.
3. CHECKSUM GRANULARITY: Does the RDMA checksum use the same bytesPerChecksum
   as the stored block metadata? If not, the checksums are incomparable.
4. REFERENCE SAFETY: Flag any use of reference equality (==) on objects that
   should use .equals(). Flag any implicit assumptions about object identity
   across collection operations.
5. PARAMETER MAPPING: For every config key that's passed to a native API, verify
   that the native API actually accepts that parameter. Flag any config keys
   that are defined but never used, or used but not mapped to a real parameter.
```

### Why Three Separate Prompts?

Each phase requires a different mode of thinking:
- **Phase 1** is mechanical — grep and compare signatures
- **Phase 2** requires reading two code paths side by side and diffing their behavior
- **Phase 3** requires adversarial reasoning about what goes wrong

Combining all three into a single prompt produces a review that's shallow on all axes. Separating them lets the auditor focus deeply on one concern at a time.

### Key Principle the Previous Prompts Lacked

> **"Don't verify that the new code works. Verify that it works identically to what it replaces, fails safely when it can't, and compiles under the project's actual build constraints."**

The previous audits asked "is this correct?" when they should have asked "is this correct *in the context of this specific codebase, its build system, its existing behavior, and its failure modes*?"
