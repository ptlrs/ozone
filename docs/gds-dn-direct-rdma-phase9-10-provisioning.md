# Phase 9 & 10 — Cloud Provisioning Plan (Cost-Tiered)

**Purpose:** three tiers of cloud hardware that together cover every
remaining phase — dev, host-memory RDMA E2E, and GPU RDMA E2E — with
costs matched to what each stage actually needs.

**Key insight:** the dev loop and the RDMA test loop are fundamentally
different workloads. Paying $15/hr to sit idle while you compile C++ is
waste. The plan below puts dev on a **$24/month** cloud VM and only
burns per-hour bare-metal when we're actually pushing bytes across an
RDMA fabric.

---

## What each phase *actually* needs

| Phase | CPU | RAM | Disk | RDMA NIC | GPU | SDK | Tier |
|---|---|---|---|---|---|---|---|
| 9b build (`.cc` + CMake + `-Pnative`) | 2-4 vCPU | 4-8 GB | 20 GB | **no** | **no** | yes | **Tier 1** |
| 9c dist packaging | 2-4 vCPU | 4-8 GB | 20 GB | **no** | **no** | yes | **Tier 1** |
| 9 library-load smoke test (`System.loadLibrary`) | 2-4 vCPU | 4-8 GB | 20 GB | **no** | **no** | yes | **Tier 1** |
| 10 host-memory E2E (real bytes over RDMA) | 16+ vCPU | 32+ GB | 100 GB | **yes** (2 nodes) | **no** | yes | **Tier 2** |
| 10 GPU-mode E2E (GPUDirect to VRAM) | 16+ vCPU | 64+ GB | 200 GB | **yes** (2 nodes) | **yes** (client) | yes | **Tier 3** |

The Phase 9 column is clear: **no NIC, no GPU, no kernel privileges.**
`System.loadLibrary` resolving `libcuobj_server_bridge.so` calls
`dlopen(2)`, which maps the ELF and resolves dependent symbols. It
doesn't probe hardware. (The *constructor* of `cuObjServer` would
probe hardware — but a loading smoke test doesn't need to get that
far.) So a $0.03/hr Linux VM covers everything up to and including
Phase 9.

---

## Tier 1 — Dev VM (Phase 9b/c + smoke test) — ~$24/month

Any cheap x86_64 Ubuntu 22.04 cloud VM. The top three options by
price, all with the same capability for our purposes:

| Provider | SKU | Price | Notes |
|---|---|---|---|
| **Vultr** | High Frequency 4 vCPU / 8 GB / 50 GB NVMe | **$24/mo** ($0.036/hr) | Cheapest solid option; fast AMD CPU |
| **Hetzner** | CPX31 / AX41-CPU | €13-17/mo | Even cheaper, EU-based |
| **DigitalOcean** | Premium Intel 4 vCPU / 8 GB | $48/mo ($0.07/hr) | Fine, slightly pricier |

**Recommendation: Vultr HF 4 vCPU / 8 GB** (same CLI you'd use for
Tier 2 Vultr bare-metal later, so the tooling path is consistent).

### Provision (once)

```bash
# Local setup
brew install vultr/vultr-cli/vultr-cli                          # macOS
# or: go install github.com/vultr/vultr-cli/v3@latest           # Linux
export VULTR_API_KEY=<from my.vultr.com → Account → API>

# SSH keypair for Claude's future access
ssh-keygen -t ed25519 -f ~/.ssh/ozone_rdma_ed25519 -N ""

# Upload the public key to Vultr (one-time)
vultr-cli ssh-key create \
  --name ozone-rdma-key \
  --ssh-key "$(cat ~/.ssh/ozone_rdma_ed25519.pub)"
export SSH_KEY_ID=$(vultr-cli ssh-key list | grep ozone-rdma-key | awk '{print $1}')

# Find a High Frequency 4 vCPU / 8 GB plan
vultr-cli plans list | grep 'vhf-4c'     # pick the cheapest match

# Find an Ubuntu 22.04 OS id
vultr-cli os list | grep 22.04

# Provision
vultr-cli instance create \
  --region sjc \
  --plan   vhf-4c-8gb \
  --os     1743 \
  --hostname ozone-rdma-dev \
  --ssh-keys $SSH_KEY_ID

# Grab the IP (takes ~60 seconds after create)
export DEV_IP=$(vultr-cli instance list | grep ozone-rdma-dev | awk '{print $2}')
echo "Dev VM IP: $DEV_IP"
```

### Cost reality

- Running 24/7: **$24/month** — leave it running during the Phase 9
  sprint without worrying about the meter
- Active dev work: same $24/month; there's no per-hour to optimize
- Tear down when Phase 9 merges: `vultr-cli instance delete $INSTANCE_ID`

### What we install on it

```bash
ssh ozone-rdma-dev bash -s <<'EOF'
set -euo pipefail
sudo apt-get update
sudo apt-get install -y \
  build-essential cmake git curl jq \
  openjdk-17-jdk maven \
  libibverbs-dev librdmacm-dev libnuma-dev \
  pkg-config

# cuObjServer SDK — you drop the installer onto the VM out of band
# (scp the tarball you got from developer.nvidia.com), install under
# /opt/cuobjserver:
#   sudo mkdir -p /opt/cuobjserver && sudo tar -xzf cuobjserver-*.tar.gz -C /opt/cuobjserver --strip-components=1
echo 'export CUOBJSERVER_HOME=/opt/cuobjserver'                       >> ~/.bashrc
echo 'export LD_LIBRARY_PATH=$CUOBJSERVER_HOME/lib64:$LD_LIBRARY_PATH' >> ~/.bashrc
source ~/.bashrc

git clone <your-fork-or-remote> ~/ozone
cd ~/ozone && git checkout gpudirect
mvn -pl hadoop-hdds/container-service-native -am -DskipTests install
EOF
```

**Tier 1 covers:** every commit in Phases 9b and 9c. When this tier is
green, we've proven the `.so` compiles, links, ships in the dist
tarball, and loads correctly into a JVM.

---

## Tier 2 — Non-GPU RDMA pair (Phase 10 host-memory E2E)

Only spin this up **when we're ready to run the first E2E test**,
which happens once Tier 1 is green. Keep it running for the duration
of the test session (typically 1-3 hrs), then tear down.

**Yes — you don't need a GPU for the first round of E2E testing.**
Spec §1 is emphatic: cuObjServer's `handleGetObject` RDMA_WRITEs to
whatever address the client's token encodes, GPU or host. The
datanode code path is identical. First-round E2E runs cuObjClient in
`CUOBJ_MEMORY_SYSTEM` mode (pinned host memory, no CUDA required on
the client) and proves the full read path end-to-end.

### Options, cheapest first

| Provider | SKU | Per node | Pair total | Fabric | Provisioning time | Notes |
|---|---|---|---|---|---|---|
| **Vultr** | CPU bare-metal w/ Mellanox ConnectX-6 | ~$0.30-0.55/hr | **~$0.60-1.10/hr** | InfiniBand (if in stock) | 5-15 min | Cheapest real option; stock is **not guaranteed** — check `vultr-cli bare-metal plan list` before committing |
| **Azure** | HBv3 / HBv4 (ConnectX-6/7 HDR IB, no GPU) | ~$3.50/hr | ~$7/hr | InfiniBand HDR | 1-3 business days first time | Quota-gated: default quota is 0, needs approval ticket |
| **OCI** | BM.HPC2.36 (dual ConnectX-5, no GPU) | ~$2.70/hr | ~$5.40/hr | RDMA over ConnectX | Quota-gated | Non-GPU HPC shape; RoCE or IB depending on region |
| **Crusoe A100 IB** | `a100-80gb-sxm-ib.8x` | $15.60/hr | $31.20/hr | InfiniBand 200 Gbps | 5 min | **Overkill for non-GPU testing**, but same hardware serves Tier 3 — worth considering only if Tier 2 → Tier 3 is a one-shot plan |

**Recommendation: Vultr CPU bare-metal with ConnectX if in stock; fall
back to Crusoe A100 otherwise** (the A100 GPUs will simply sit idle —
paying for hardware you don't use, but the Crusoe path is a known
quantity and doesn't need a quota ticket).

### Vultr bare-metal check + provision

```bash
# List all bare-metal plans
vultr-cli bare-metal plan list

# Filter to ones with Mellanox NICs — look at the Features column
# or consult docs.vultr.com for which plan IDs have ConnectX NICs
# Cross-reference vbm-* plan IDs with:
# https://docs.vultr.com/products/compute/bare-metal/explore-plan-variants

# Provision two nodes in the SAME region so they share the same switch
for name in ozone-rdma-dn ozone-rdma-client; do
  vultr-cli bare-metal create \
    --region <same-region-both-times> \
    --plan   <connectx-plan-id> \
    --os     1743 \
    --host   $name \
    --sshkey $SSH_KEY_ID
done

# Wait for state=active (5-15 min)
vultr-cli bare-metal list
```

### Cost reality

- 1-hour non-GPU E2E session: **~$1-2** (Vultr in stock) or ~$7 (Azure)
- Leave it idle between test runs: same per-hour — tear down aggressively
- Run the full Phase 10 host-memory validation in a 2-3 hour burst for
  < $10 total (Vultr) or ~$20 (Azure)

### Teardown

```bash
vultr-cli bare-metal list | awk '/ozone-rdma/{print $1}' \
  | xargs -n1 vultr-cli bare-metal delete
```

**Tier 2 covers:** Phase 10 host-memory mode. Proves bytes actually
cross a real RDMA fabric between two hosts and land in the right
client-side buffer. This is the *first* real proof that the stack
works end-to-end.

---

## Tier 3 — GPU + IB pair (Phase 10 GPU-mode E2E)

Only if you want to validate `CUOBJ_MEMORY_CUDA_DEVICE` specifically
(GPUDirect RDMA into VRAM). Same two-node pattern, different SKU.

### Options

| Provider | SKU | Pair total | Fabric | GPU | Notes |
|---|---|---|---|---|---|
| **Crusoe** | `a100-80gb-sxm-ib.8x` × 2 | **$31.20/hr** | IB ConnectX-6 200 Gbps | 8× A100 80GB per node | Recommended: preinstalled drivers, `nvidia_peermem` loaded |
| **Crusoe** | `h100-80gb-sxm-ib.8x` × 2 | $62.40/hr | IB ConnectX-7 400 Gbps | 8× H100 80GB per node | Reserve for publishable throughput numbers; 2× A100 price |
| **Vultr** | H100 bare-metal × 2 | ~$35-45/hr | IB Quantum-2 3200 Gbps | 8× H100 per node | Adds ~60 min MLNX_OFED install per node |

### Cost reality

- 1-hour GPU E2E session on Crusoe A100: ~$31
- Full benchmarking sweep (spec §9): 2-3 hours = ~$60-100
- H100 only if you need the throughput numbers for a publication

**Tier 3 covers:** Phase 10 GPU mode + §9 benchmarking. Optional — the
code-only milestone is complete after Tier 2 passes.

---

## Recommended workflow

```
[ Tier 1 dev VM: $24/mo, keep running for the Phase 9 sprint ]
  │
  │  Phases 9b + 9c commit, then PR
  │  Library-load smoke test green
  │
  ▼
[ Tier 2 non-GPU RDMA pair: spin up, run Phase 10 host-memory E2E,
  tear down. ~$1-10 total ]
  │
  │  Validate: 256 MiB S3 GET over RDMA to host memory, checksum OK
  │
  ▼
[ OPTIONAL Tier 3 GPU+IB pair: spin up, run Phase 10 GPU-mode E2E,
  tear down. ~$30-60 total ]
  │
  │  Validate: same 256 MiB GET but target is CUDA device memory
  │  Optional: §9 benchmarking
  ▼
[ Done. Phase 1 E2E-verified end to end. ]
```

**Total expected spend:** $24 (Tier 1 month) + $5 (Tier 2 run) + $60
(optional Tier 3 run) = **~$30-90** across the remaining phases. Down
from the ~$200+ the previous plan would have burned.

---

## Claude's SSH access setup

Same as before, just one entry per node we're currently using.
`~/.ssh/config`:

```
Host ozone-rdma-dev
  HostName <TIER1_IP>
  User root             # Vultr default; ubuntu on Crusoe
  IdentityFile ~/.ssh/ozone_rdma_ed25519
  StrictHostKeyChecking accept-new
```

`.claude/settings.json`:

```json
{
  "permissions": {
    "allow": [
      "Bash(ssh ozone-rdma-dev:*)",
      "Bash(scp:*)",
      "Bash(rsync * ozone-rdma-dev:*)"
    ]
  }
}
```

When we add Tier 2 / Tier 3 nodes later, append `ozone-rdma-dn` and
`ozone-rdma-client` entries to both files.

---

## Things I will explicitly ask before executing

- Any `create` that spends money (`vultr-cli instance create`,
  `bare-metal create`, `crusoe compute vms create`)
- Any `delete` / `terminate`
- Pushing commits from the remote to a git origin
- Running binaries downloaded from outside the repo (e.g., the SDK
  installer — you'll upload that yourself)

Reads, SSH within pre-authorized aliases, Maven/CMake/gcc, and
local-only operations run without prompts.

---

## Decision points for you right now

1. **cuObjServer SDK licensing** (still the gating unknown): can you
   download it from developer.nvidia.com with a standard developer
   account, or does it require enterprise licensing? This blocks
   everything.

2. **Tier 1 provider:** Vultr HF (recommended), Hetzner, or DigitalOcean?

3. **Tier 1 region:** `sjc` (US west) is typical for Mac-based SSH,
   low latency. Confirm you want `sjc` or pick another Vultr region.

Once (1) and (2) are resolved, `vultr-cli instance create` puts a VM
online in ~60 seconds and we can start Phase 9b within the same
session. Tier 2 / Tier 3 happen days or weeks later, only when we're
ready to push bytes over real RDMA — and only for the hours we
actually need.
