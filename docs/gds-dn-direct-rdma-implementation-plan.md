# GPUDirect RDMA Phase 1 — Implementation Plan

**Companion to:** `docs/gds-dn-direct-rdma-implementation-spec.md` (≥v12.5)
**Analysis input:** `docs/gds-dn-direct-rdma-ai-implementation-readiness.md`
**Grep audit input:** `docs/gds-dn-direct-rdma-implementation-spec-v12.4-grep-audit.md`
**Target branch:** `gpudirect` on Apache Ozone 2.2.0-SNAPSHOT (merges to `master`)
**Plan version:** 1.0 (2026-04-18)

This plan is mutable. The spec is the product description; this plan
is the *process* to get there. Update it as implementation progresses;
do not update the spec to match plan reality (the spec changes only
when the product description changes).

---

## 1. Scope & objectives

Phase 1 delivers the **S3 GET (read) path** from `ObjectEndpoint` →
`XceiverServerRdma` → `CuObjServerBridge` over RDMA, with a TCP
fallback that is never silently unsafe. The code must compile and pass
all mock-backed tests on `master` merge; the native build is gated on
the NVIDIA cuObjServer SDK being available locally, and end-to-end
real-RDMA testing is gated on operator-provisioned hardware.

### Explicit non-goals (Phase 1 only)

- **No write path.** `RdmaWriteBlock` is reserved in the proto (§4.1.2)
  so the wire format is future-stable, but no dispatcher case, handler
  body, or S3G PUT integration is written in this phase. Appendix B of
  the spec captures the Phase 2 reasons.
- **No Erasure Coded (EC) keys.** Stripe-aware offset math is out of
  scope; EC keys trigger TCP fallback (§4.15.5 first early-return).
- **No TDE fast path.** Transparent Data Encryption decrypts in the
  JVM on the TCP path; RDMA would bypass that contract. TDE buckets
  fall back to TCP unless the operator opts in via
  `ozone.s3g.rdma.allow.encrypted.buckets` (§4.15.5).
- **No multipart `?partNumber=N` RDMA path.** Multipart GETs fall back
  to TCP (§4.15.5 second early-return).
- **No custom libibverbs client, no DiSNI, no Ratis changes, no OM or
  SCM changes.** Appendix B of the spec enumerates Phase 2 items.

---

## 2. Success criteria (end-state for the code-only milestone)

1. **Clean reactor build on `master` after merge:**
   ```
   mvn -pl hadoop-ozone/dist -am -DskipTests install
   ```
   must exit 0 with no new warnings at `-Werror` targets.

2. **Mock-backed test suites green.** Enumerated from spec §8.1–§8.2:
   - `TestRdmaBufferPool` (§8.1)
   - `TestMockRdmaTransferService` (§8.1)
   - `TestXceiverServerRdma` (§8.1)
   - `TestKeyValueHandlerRdma` (§8.1 — `handleRdmaReadBlock` unit)
   - `TestOzoneContainer` (regression: existing class, must still pass)
   - `TestRdmaReadBlock` (§8.2 — MiniOzoneCluster integration)
   - `TestObjectGet*` (regression in s3gateway: must still pass)

3. **Native build green when `CUOBJSERVER_HOME` is set:**
   ```
   mvn -Pnative -pl hadoop-hdds/container-service-native package
   ```
   must emit
   `hadoop-hdds/container-service-native/target/native/install/lib/libcuobj_server_bridge.so`.

4. **Distribution tarball contains the `.so`:**
   ```
   mvn -Pdist,native -pl hadoop-ozone/dist package
   ```
   must stage the library at
   `hadoop-ozone/dist/target/ozone-*/share/ozone/lib/native/libcuobj_server_bridge.so`,
   and `bin/ozone-env.sh` must export `JAVA_LIBRARY_PATH` so the datanode
   start script picks it up (spec §4.7.2).

5. **At least one E2E C++ client test succeeds** against a Crusoe A100
   (or equivalent) cluster transferring a 256 MiB object via
   `x-amz-rdma-token`. **Gated on hardware and user approval** — not
   required for the code-only milestone.

---

## 3. Staged plan — 10 phases

Each phase specifies: spec sections, files touched, atomicity, compile
checkpoint, test checkpoint, dependencies, and gates. Phase 0 is
read-only (no code).

### Phase 0 — Read and summarize

- **Spec sections:** entire spec (≥v12.5), whole readiness analysis,
  whole grep audit.
- **Files touched:** none.
- **Atomic commit:** N/A (no code).
- **Checkpoint:** produce a one-paragraph summary of what was
  understood, a questions list (blocking vs. non-blocking), and the
  empty starting diff (`git status` clean on `gpudirect`).
- **Dependencies:** none.
- **Gates:** none. Always runs.

### Phase 1 — Proto + shared helpers (single atomic commit)

- **Spec sections:** §4.1.0, §4.1.1, §4.1.2, §4.1.3, §4.1.4;
  §4.13-pre A, B, C, D (enum + `getAuditAction` case only — not the
  handler dispatch yet); §4.13-pre E.
  `getAuditParams` (§4.13-pre G, v12.5 R-2 fix) is kept in this
  commit because it depends on the same proto message type.
- **Files touched:**
  - `hadoop-hdds/interface-client/src/main/proto/DatanodeClientProtocol.proto`
  - `hadoop-hdds/common/.../HddsUtils.java`
  - `hadoop-hdds/container-service/.../audit/DNAction.java`
  - `hadoop-hdds/container-service/.../common/impl/HddsDispatcher.java`
  - `hadoop-hdds/client/.../scm/XceiverClientGrpc.java`
- **Atomic commit — required.** **Trap 2 from readiness §3.1:** adding
  `Type.RdmaReadBlock` to the proto without updating
  `HddsUtils.isReadOnly/requireBlockToken/getBlockID` *silently
  misclassifies every new request* (mis-classified as write-like, fails
  block-token verification, extracts a null block ID). The proto
  generator's output plus every helper update must ship together.
- **Compile checkpoint:**
  ```
  mvn -pl hadoop-hdds/interface-client,hadoop-hdds/common,hadoop-hdds/client,hadoop-hdds/container-service \
      -am -DskipTests install
  ```
  Expected: BUILD SUCCESS, all modules `install`ed to local repo.
- **Test checkpoint:**
  ```
  mvn -pl hadoop-hdds/common -Dtest='TestHddsUtils#*RdmaReadBlock*' test
  ```
  (Tests can be named placeholders if this is the first commit — the
  enforcement is that no *existing* test regresses.)
- **Dependencies:** none.
- **Gates:** none.

### Phase 2 — `DatanodeDetails.Port.Name.RDMA` + `HDDSLayoutFeature`

- **Spec sections:** §4.2 (both the `Port.Name`/`IO_PORTS` additions and
  the `HDDSLayoutFeature.RDMA_PORT_IN_DATANODEDETAILS(11, ...)` entry).
- **Files touched:**
  - `hadoop-hdds/common/.../DatanodeDetails.java`
  - `hadoop-hdds/common/.../HDDSLayoutFeature.java`
- **Atomic commit — required.** **Trap 1 from readiness §3.1:**
  `Port.Name.RDMA` carries a `@BelongsToHDDSLayoutVersion(
  RDMA_PORT_IN_DATANODEDETAILS)` annotation; the enum value referenced
  by the annotation must already exist in `HDDSLayoutFeature` when the
  `Port.Name` line is compiled. Add the layout feature *first in the
  file edit, then* add the port — or, equivalently, commit both edits
  together.
- **Compile checkpoint:**
  ```
  mvn -pl hadoop-hdds/common -Dtest='*Layout*' test
  ```
  Expected: `TestHDDSLayoutVersionManager` (and any related) green.
- **Test checkpoint:** same command above; also re-run phase 1's
  reactor compile to confirm nothing regressed.
- **Dependencies:** Phase 1 (proto `Type.RdmaReadBlock` must exist,
  though `Port.Name` does not directly reference it).
- **Gates:** none.

### Phase 3 — `container-service-native` module skeleton

- **Spec sections:** §4.3 (module layout, pom deps including the v12.5
  R-6 fix adding `hdds-config`; reactor entry in `hadoop-hdds/pom.xml`).
- **Files touched:**
  - `hadoop-hdds/container-service-native/pom.xml` (NEW)
  - `hadoop-hdds/container-service-native/src/main/java/.../rdma/package-info.java` (NEW, optional)
  - `hadoop-hdds/pom.xml` (reactor `<modules>`)
  - `hadoop-hdds/container-service/pom.xml` (add dep on the new module)
- **Atomic commit — required.** The reactor list and the new module's
  pom must be committed together; otherwise `mvn -am` picks up an
  orphan module. No Java classes yet — the skeleton compiles empty.
- **Compile checkpoint:**
  ```
  mvn -pl hadoop-hdds -am -DskipTests install
  ```
  Expected: full HDDS reactor builds; the new module appears in
  Maven's build order and produces an (empty-but-valid) jar.
- **Test checkpoint:** none (no code to test).
- **Dependencies:** Phases 1, 2 (for compile).
- **Gates:** none.

### Phase 4 — Java RDMA API + Mock + Buffer pool

- **Spec sections:** §4.4 (`RdmaTransferService`), §4.9
  (`MockRdmaTransferService`), §4.10 (`RdmaBuffer`, `RdmaBufferPool`).
  No cuObjServer, no JNI, no `XceiverServerRdma` yet.
- **Files touched:**
  - `.../rdma/RdmaTransferService.java` (NEW)
  - `.../rdma/MockRdmaTransferService.java` (NEW)
  - `.../rdma/RdmaBuffer.java` (NEW)
  - `.../rdma/RdmaBufferPool.java` (NEW)
  - `.../rdma/RdmaBufferPoolExhaustedException.java` (NEW)
  - Test: `.../rdma/TestRdmaBufferPool.java`,
    `.../rdma/TestMockRdmaTransferService.java` (NEW)
- **Atomic commit — can split.** Safe to stage as (4a) interface +
  Mock + tests, then (4b) buffer pool + tests. Both halves compile
  independently and the tests in each half cover that half only.
- **Compile checkpoint:**
  ```
  mvn -pl hadoop-hdds/container-service-native -am -DskipTests install
  ```
- **Test checkpoint:**
  ```
  mvn -pl hadoop-hdds/container-service-native \
      -Dtest='TestRdmaBufferPool,TestMockRdmaTransferService' test
  ```
  Expected: both test classes green. All §8.1 entries for buffer pool
  and Mock must pass.
- **Dependencies:** Phase 3 (the module must exist).
- **Gates:** none.

### Phase 5 — `XceiverServerRdma` + exception types + `KeyValueHandler` setter + dispatch case

- **Spec sections:** §4.11 (`XceiverServerRdma`, including the v12.5
  R-3a `RDMA_TRANSFER_SERVICE_CLASS_KEY` constant and the R-8 inline
  comments at the two throw sites), §4.14.1 (setter), §4.14.2
  (dispatch case), §4.14.3 (`handleRdmaReadBlock` + new
  `RdmaTransferException`).
- **Files touched:**
  - `.../common/transport/server/XceiverServerRdma.java` (NEW, in
    `hadoop-hdds/container-service`)
  - `.../rdma/RdmaTransferException.java` (NEW, in
    `hadoop-hdds/container-service-native`)
  - `.../keyvalue/KeyValueHandler.java` (edit)
  - Tests: `TestXceiverServerRdma`, `TestKeyValueHandlerRdma` (NEW)
- **Atomic commit — split recommended.** §8.1's `TestXceiverServerRdma`
  needs §4.11 to compile before it can exist. Split into (5a)
  `RdmaTransferException` + `XceiverServerRdma` + `TestXceiverServerRdma`,
  then (5b) `KeyValueHandler` setter + dispatch case +
  `TestKeyValueHandlerRdma`. **Rationale:** a single fat commit here
  mixes the engine (new code, low risk) with the handler surgery (edit
  of a 2000-line file, high risk); split gives cleaner git blame and
  lets the engine land even if handler work needs rework.
- **Compile checkpoint (5a):**
  ```
  mvn -pl hadoop-hdds/container-service-native,hadoop-hdds/container-service \
      -am -DskipTests install
  ```
- **Test checkpoint (5a):** `TestXceiverServerRdma` green.
- **Compile checkpoint (5b):** same as 5a.
- **Test checkpoint (5b):** `TestKeyValueHandlerRdma` green.
- **Dependencies:** Phases 1–4.
- **Gates:** none (Mock path only).

### Phase 6 — `OzoneContainer` wiring + `HddsDispatcher` audit-action

- **Spec sections:** §4.12 (wiring; including the v12.5 R-3a
  `RDMA_TRANSFER_SERVICE_CLASS_KEY` reference update), §4.13 (narrative
  only — the code is in §4.13-pre D/G, already landed in Phase 1).
- **Files touched:**
  - `.../ozoneimpl/OzoneContainer.java` (edit)
- **Atomic commit — required.** **Trap 3 from readiness §3.1:**
  `OzoneContainer` calls `((KeyValueHandler) h).setXceiverServerRdma(rdma)`
  in its handler-map loop. The setter on `KeyValueHandler` must exist
  at compile time. Phase 5 added the setter; Phase 6 consumes it.
  Ordering between the two phases is fixed: Phase 6 cannot merge
  without Phase 5.
- **Compile checkpoint:**
  ```
  mvn -pl hadoop-hdds/container-service -am -DskipTests install
  ```
- **Test checkpoint:**
  ```
  mvn -pl hadoop-hdds/container-service \
      -Dtest='TestOzoneContainer,TestXceiverServerRdma,TestKeyValueHandlerRdma' test
  ```
  Expected: all three green; the full §8.1 suite on
  `hadoop-hdds/container-service-native` should also re-pass here.
- **Dependencies:** Phases 1, 4, 5.
- **Gates:** none.

### Phase 7 — S3G `RdmaRequestFilter` + `ObjectEndpoint` modifications

- **Spec sections:** §4.15.1 (incl. v12.5 R-3b/c new constants in
  `S3GatewayConfigKeys`), §4.15.2, §4.15.3, §4.15.4, §4.15.5
  (`handleRdmaGet` + `handleRdmaFailure`), §4.16 (`RdmaRequestFilter`).
- **Files touched:**
  - `.../s3/RdmaRequestFilter.java` (NEW — package
    `org.apache.hadoop.ozone.s3`, matching the filter-convention fix
    from v12.4)
  - `.../s3/S3GatewayConfigKeys.java` (edit — add R-3b/c constants)
  - `.../s3/endpoint/ObjectEndpoint.java` (edit — new fields, new
    import, detection block, `handleRdmaGet`, `handleRdmaFailure`)
- **Atomic commit — single commit preferred.** The filter, the
  endpoint detection block, and the endpoint fields must all be
  internally consistent (shared property-name constant
  `RDMA_TOKEN_PROPERTY`, shared header names). A split would need a
  dummy intermediate state.
- **Compile checkpoint:**
  ```
  mvn -pl hadoop-ozone/s3gateway -am -DskipTests install
  ```
- **Test checkpoint:**
  ```
  mvn -pl hadoop-ozone/s3gateway test
  ```
  Expected: all existing `TestObjectEndpoint*` green (regression
  check); new `TestRdmaRequestFilter` / `TestObjectEndpointRdma`
  (if added) green.
- **Dependencies:** Phases 1 (proto) and 6 (handler wired — though
  S3G does not call the handler directly, the filter's behavior is
  only meaningful when the datanode side is end-to-end ready).
- **Gates:** none (Mock path is sufficient).

### Phase 8 — Integration test (MiniOzoneCluster, Mock)

- **Spec sections:** §8.2 (`TestRdmaReadBlock` in
  `hadoop-ozone/integration-test`).
- **Files touched:**
  - `hadoop-ozone/integration-test/.../TestRdmaReadBlock.java` (NEW)
- **Atomic commit — single commit.** One test class.
- **Compile checkpoint:**
  ```
  mvn -pl hadoop-ozone/integration-test -am -DskipTests install
  ```
- **Test checkpoint:**
  ```
  mvn -pl hadoop-ozone/integration-test -Dtest='TestRdmaReadBlock' test
  mvn -pl hadoop-ozone/integration-test -Dtest='TestObjectGet*' test
  ```
  Expected: `TestRdmaReadBlock` green; `TestObjectGet*` still green
  (TCP path unchanged).
- **Dependencies:** Phases 1–7.
- **Gates:** none.

### Phase 9 — Native JNI (GATED on `CUOBJSERVER_HOME`)

- **Spec sections:** §4.5 (`CuObjServerBridge.java`), §4.6 (native
  C++ `cuobj_server_bridge.cc`), §4.7.0 (Maven↔CMake integration
  with v12.5 R-7 pluginManagement note), §4.7.1 (`CMakeLists.txt`),
  §4.7.2 (distribution packaging), §4.8 (`CuObjRdmaTransferService`).
- **Files touched:**
  - `.../rdma/CuObjServerBridge.java` (NEW)
  - `.../rdma/CuObjRdmaTransferService.java` (NEW)
  - `src/main/native/CMakeLists.txt` (NEW)
  - `src/main/native/cuobj_server_bridge.cc` (NEW)
  - `hadoop-hdds/container-service-native/pom.xml` (edit — add
    `<profile id="native">`)
  - `hadoop-ozone/dist/src/main/assemblies/*.xml` (edit — include
    the `.so` under `share/ozone/lib/native/`)
  - `hadoop-ozone/dist/src/shell/ozone/ozone-env.sh` (edit — export
    `JAVA_LIBRARY_PATH`)
- **Atomic commit — split permitted.** (9a) Java-side JNI bindings
  + `CuObjRdmaTransferService` (compiles without the `.so`; runtime
  throws `UnsatisfiedLinkError` if invoked without the native library);
  (9b) C++ bridge + CMake + Maven profile; (9c) dist packaging + shell
  env. Splitting reduces review surface per commit and lets the Java
  side land without gating on native toolchain availability on every
  reviewer's machine.
- **Compile checkpoint (Java-only, 9a):**
  ```
  mvn -pl hadoop-hdds/container-service-native -am -DskipTests install
  ```
- **Compile checkpoint (native build, 9b/c) — GATED:**
  ```
  [ -n "$CUOBJSERVER_HOME" ] || { echo "SKIPPED: CUOBJSERVER_HOME unset"; exit 0; }
  mvn -Pnative -pl hadoop-hdds/container-service-native,hadoop-ozone/dist \
      -am package
  ```
  Expected (when gate passes):
  - `hadoop-hdds/container-service-native/target/native/install/lib/libcuobj_server_bridge.so` exists.
  - `hadoop-ozone/dist/target/ozone-*/share/ozone/lib/native/libcuobj_server_bridge.so` exists.
  - `bin/ozone-env.sh` exports `JAVA_LIBRARY_PATH` pointing at the
    native dir.
- **Test checkpoint:** no unit tests for the C++ bridge in Phase 1
  (unit-testing JNI is covered by Phase 10 E2E). The `MockRdmaTransferService`
  tests from Phase 4/5 still pass unchanged.
- **Dependencies:** Phases 1–6 for Java; Phase 8 optional (integration
  test uses Mock and is independent of native).
- **Gates — IMPORTANT:**
  - **`CUOBJSERVER_HOME` must be set** to the root of an installed
    NVIDIA cuObjServer SDK (ref-15). If unset, mark the native phase
    SKIPPED and finish Phase 1 with Mock-only. Do not attempt
    workarounds like stubbing `cuObjServer.h`; that produces a library
    that links but segfaults at runtime.
  - **CUDA Toolkit 13.1.1+** on the build host (public download OK).
  - **MLNX_OFED 5.8+** on the build host (public download OK).
  - **Physical NIC and bringup** NOT needed for the build — only the
    headers and libraries. Bringup is Phase 10.

### Phase 10 — End-to-end on real hardware (GATED)

- **Spec sections:** §8.5 (E2E test script), §9 (benchmarking), §7.4
  A/B/C/E/F (per-provider provisioning stanzas).
- **Files touched:** none in the repo (tests and scripts already live
  in the spec's §7.4.Z and §7.12). Any new scripts are operator
  artifacts, not code.
- **Atomic commit:** N/A (no source changes).
- **Checkpoint:** at least one of §7.4.A/B/C/E/F stanzas run
  end-to-end, producing:
  - A 256 MiB S3 GET over RDMA with `x-amz-rdma-reply: SUCCESS`.
  - Wire throughput within spec §9.1 tolerance for the chosen NIC
    (e.g., ~12 GiB/s single-stream on a 200 Gb IB link — real numbers,
    not Mock).
- **Dependencies:** Phases 1–9 complete, native `.so` packaged in the
  dist tarball, user has produced `CUOBJSERVER_HOME` + an `$VM_IP`.
- **Gates — IMPORTANT:**
  - **Real-hardware cost meter.** Crusoe A100 is $15.60/hr 8-GPU min;
    H100 $31.20/hr; Vultr/Lambda/OCI/IBM similar order of magnitude
    (spec §7.3.1). **Do not provision cloud VMs autonomously.** Ask the
    user, and quote the approximate hourly cost before starting.
  - **SDK access already satisfied** by Phase 9's gate — Phase 10 just
    exercises what Phase 9 built.
  - **AWS EFA is not a substitute for InfiniBand.** Spec §7.3.3
    documents that EFA does not expose the RDMA verbs `cuObjServer`
    requires; do not attempt Phase 10 on AWS `p4d`/`p5`.

---

## 4. Atomic-commit callouts (readiness §3.1 traps, in plan terms)

**Trap 1 — `Port.Name.RDMA` precedes `HDDSLayoutFeature` in file order
but depends on it at compile time.** This is Phase 2's single-commit
rule. Both `DatanodeDetails.java` and `HDDSLayoutFeature.java` must
land in the same commit, with the `HDDSLayoutFeature` edit applied
first within that commit if the developer is editing serially.

**Trap 2 — Proto `Type.RdmaReadBlock` must ship with every shared-helper
update in the same commit.** This is Phase 1's single-commit rule. If
the proto change landed alone, every caller of `HddsUtils.isReadOnly`,
`.requireBlockToken`, `.getBlockID`, plus the two `getAuditAction` /
`getAuditParams` switches and `XceiverClientGrpc.getRequestBlockID`,
would all misbehave for the new request type — not compile-breaks,
runtime misbehavior. One atomic commit closes every case site
together.

**Trap 3 — `OzoneContainer`'s handler loop calls
`KeyValueHandler.setXceiverServerRdma`, which must exist at compile
time.** Handled by the Phase 5 → Phase 6 ordering. Phase 6 depends on
Phase 5 having merged; do not reverse. If a developer is building
locally across both phases, the setter edit must precede the caller
edit.

---

## 5. Phase skip / resume policy

If phase N's checkpoint fails:

1. **Do not proceed to N+1.** A broken checkpoint means the artifact
   contract for later phases is not met; extending onto broken ground
   compounds the problem and will force a revert anyway.
2. **Diagnose the root cause before touching code.** "Try rebuilding"
   is not a diagnosis. Read the failure, identify the specific file +
   line + expected-vs-actual, and only then decide the scope of the
   fix.
3. **Scope the fix:**
   - *Typo / missing import / minor logic slip* → fix in the same
     commit (amend locally before push; never amend a pushed commit).
   - *Larger refactor or missed spec section* → follow-up commit on
     the same phase, retaining the phase-N commit message prefix.
   - *The spec is wrong (not a typo — actually describes code that
     cannot compile or would not work)* → **STOP and ask the user**.
     Do not patch the spec mid-implementation without explicit
     approval; the spec is the product description and review target.
4. **Never skip tests with `@Disabled` or `-DskipTests` as a
   workaround.** A test that is genuinely obsolete for Phase 1 needs a
   documented rationale in the PR description, not a silent skip.
5. **Never `git reset --hard` or `git clean -f`.** These destroy
   uncommitted work. Use `git stash` if you need a clean tree; use
   `git revert` to undo a bad commit on a shared branch.

---

## 6. Hardware / SDK gate table

| Resource | Phase | Required for | Self-procurable by an AI? | Ask user? | Notes |
|---|---|---|---|---|---|
| NVIDIA cuObjServer SDK | 9 | Native build | **NO** — requires NVIDIA developer account; enterprise-license status unclear (spec Appendix A.12 marks `NEEDS TESTING`) | Yes | Download ref-15. If unavailable, Phase 9 is SKIPPED and Phase 1 ships with Mock-only. |
| CUDA Toolkit 13.1.1+ | 9 | Native build | YES — public download from developer.nvidia.com | No | Bundled on most GPU cloud images already. |
| Mellanox MLNX_OFED 5.8+ | 9 | Native build | YES — public download from nvidia.com | No | Includes ibverbs headers + RDMA core utilities. |
| Real ConnectX NIC (host) | 10 | E2E RDMA | **NO** | Yes | Physical hardware. Only meaningful via cloud. |
| Crusoe Cloud A100 8-GPU | 10 | E2E (cheapest IB) | **NO** — $15.60/hr, user credit card | Yes | Spec §7.4.A. Default recommendation for Phase 10. |
| Crusoe Cloud H100 8-GPU | 10 | E2E (benchmarking) | **NO** — $31.20/hr | Yes | Reserve for publishable throughput runs. |
| Oracle Cloud BM.GPU.H100.8 | 10 | E2E (real RoCEv2) | **NO** — user account required | Yes | Spec §7.4.E. Only VERIFIED credit-card-accessible RoCEv2. |
| Vultr / Lambda / IBM / AWS | 10 | E2E alternates | **NO** | Yes | §7.4.B/C/F/D. AWS EFA does **not** satisfy the `cuObjServer` requirement — §7.3.3. |

---

## 7. Phase 1 exit criteria (code-only milestone)

The **code-only milestone** is independent of Phase 10 E2E and is what
"Phase 1 done" means for a merge to `master`:

1. Success-criteria 1–4 from §2 are all green. In particular, a green
   reactor build on `master` after the last phase merges, and all
   Mock-backed test classes enumerated in §2 item 2 passing.
2. Every `R-1..R-8` grep-audit item is closed — either in the v12.5
   spec patch (which landed before implementation started) or in the
   code that implements the corresponding spec section.
3. No `@Ignore` / `@Disabled` on newly added tests. No `-DskipTests`
   in CI scripts introduced by this change.
4. `git log --oneline master..gpudirect | wc -l` shows one commit per
   plan phase (10 commits, minus Phase 10 which is hardware-only).
   Split commits within a phase are acceptable when §3 calls them out.
5. The dist tarball built with `mvn -Pdist,native` contains
   `libcuobj_server_bridge.so` *if* Phase 9's gate was passed.
   Otherwise, the dist tarball builds cleanly without the `.so` and
   the datanode logs a clear warning at startup when `rdma.enabled=true`
   but the library is absent (§4.11 `XceiverServerRdma` constructor
   path or its wiring in §4.12).
6. The readiness analysis's 8-axis matrix is re-scored as ≥4 on every
   axis, with explicit evidence (grep output, test results) for each
   score ≥4.
7. No secrets (`.env`, `~/.ssh/*`, cloud-provider credentials,
   `CUOBJSERVER_HOME`-relative absolute paths) committed.

Phase 10 (E2E) is an orthogonal, operator-driven milestone: it
validates that the code actually moves bytes on real hardware at the
expected throughput, but its failure or success does not block the
code-only milestone from being declared done.
