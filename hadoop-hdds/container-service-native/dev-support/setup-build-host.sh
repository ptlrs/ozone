#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# =============================================================================
# Ozone GPUDirect RDMA build-host bootstrap (Ubuntu 22.04)
# =============================================================================
#
# Idempotent. Re-running is safe — every step is either a no-op on a
# configured host or picks up where it left off. Designed for a fresh
# Vultr / Hetzner / DigitalOcean x86_64 Ubuntu 22.04 VM that will host
# the Phase 9b/c native build + the Phase 9 library-load smoke test.
#
# What it does:
#   1. Base build toolchain (JDK 17, Maven 3.9, build-essential, cmake,
#      ibverbs / rdma-cm / numa user-space headers).
#   2. NVIDIA apt repo keyring.
#   3. CUDA Toolkit 13.2 (user-space only — no GPU required on this host
#      because we skip the kernel driver packages by default).
#   4. cuObjServer SDK (`cuobjserver` package from the NVIDIA apt repo).
#
# What it does NOT do:
#   - Install GPU kernel drivers (`nvidia-open` / `cuda-drivers`). The
#     build host has no GPU; drivers would fail to load and aren't
#     needed to compile or link against libcudart / libcuobjserver. If
#     you're running this on a future GPU host, pass `--with-drivers`
#     (installs the open-source kernel module, NVIDIA-licensed variant
#     of the driver — cleanest from a compliance standpoint).
#   - Clone / update the Ozone repo (rsync from the developer host
#     instead, or arrange a GitHub fork + pull).
#
# Prerequisites:
#   - Ubuntu 22.04 x86_64 (Jammy)
#   - Run as root, or as a user with passwordless sudo.
#   - ~120 GB disk (CUDA Toolkit alone is ~5 GB; the full apt cache
#     after CUDA install can reach 15-20 GB).
#
# Usage:
#   curl -fsSL <url> -o setup-build-host.sh && chmod +x setup-build-host.sh
#   ./setup-build-host.sh                   # dev-host flavor (default)
#   ./setup-build-host.sh --with-drivers    # also install GPU driver
#
# =============================================================================

set -euo pipefail

# -- Option parsing -----------------------------------------------------------

WITH_DRIVERS=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-drivers) WITH_DRIVERS=1 ; shift ;;
    -h|--help)
      sed -n 's/^# \{0,1\}//p' "$0" | sed -n '/^===/,/^===$/p' | head -60
      exit 0 ;;
    *) echo "unknown arg: $1" ; exit 2 ;;
  esac
done

# -- sudo wrapper (passthrough if already root) -------------------------------

if [[ $EUID -eq 0 ]]; then
  SUDO=""
else
  SUDO="sudo"
fi

log() { printf "\n\033[1;34m== %s ==\033[0m\n" "$*"; }

# -- Step 1: OS sanity check --------------------------------------------------

log "Verifying Ubuntu 22.04 x86_64"
. /etc/os-release
if [[ "${ID:-}" != "ubuntu" || "${VERSION_ID:-}" != "22.04" ]]; then
  echo "This script targets Ubuntu 22.04 (Jammy). Detected: ${ID:-?} ${VERSION_ID:-?}." >&2
  echo "Adjust pkg names / repo URLs for other distros before re-running." >&2
  exit 1
fi
if [[ "$(uname -m)" != "x86_64" ]]; then
  echo "This script targets x86_64. Detected: $(uname -m)." >&2
  exit 1
fi

# -- Step 2: Base toolchain ---------------------------------------------------

log "Installing base build toolchain"
${SUDO} apt-get update -y
${SUDO} env DEBIAN_FRONTEND=noninteractive apt-get install -y \
    openjdk-17-jdk \
    maven \
    build-essential \
    cmake \
    pkg-config \
    git \
    curl \
    jq \
    ca-certificates \
    libibverbs-dev \
    librdmacm-dev \
    libnuma-dev

# -- Step 3: NVIDIA apt repo --------------------------------------------------

log "Installing NVIDIA CUDA apt keyring"
# cuda-keyring is idempotent — dpkg -i skips unchanged versions.
KEYRING_DEB="/tmp/cuda-keyring_1.1-1_all.deb"
if ! dpkg -s cuda-keyring >/dev/null 2>&1; then
  curl -fsSL -o "$KEYRING_DEB" \
    https://developer.download.nvidia.com/compute/cuda/repos/ubuntu2204/x86_64/cuda-keyring_1.1-1_all.deb
  ${SUDO} dpkg -i "$KEYRING_DEB"
  rm -f "$KEYRING_DEB"
  ${SUDO} apt-get update -y
else
  echo "cuda-keyring already installed; skipping."
fi

# -- Step 4: CUDA Toolkit (user-space) ---------------------------------------

log "Installing CUDA Toolkit 13.2 (user-space libraries + headers)"
${SUDO} env DEBIAN_FRONTEND=noninteractive apt-get install -y cuda-toolkit-13-2

# -- Step 5: Kernel driver (optional) ----------------------------------------

if [[ $WITH_DRIVERS -eq 1 ]]; then
  log "Installing NVIDIA open-source kernel driver (nvidia-open)"
  ${SUDO} env DEBIAN_FRONTEND=noninteractive apt-get install -y nvidia-open
else
  log "Skipping kernel driver (no GPU on this host — use --with-drivers if wrong)"
fi

# -- Step 6: cuObjServer SDK --------------------------------------------------

log "Installing cuObjServer SDK"
${SUDO} env DEBIAN_FRONTEND=noninteractive apt-get install -y cuobjserver

# -- Step 7: Environment file -------------------------------------------------

log "Writing /etc/profile.d/ozone-gds.sh (CUDA / cuObjServer env)"
# Idempotent: overwrite every time with the canonical contents.
${SUDO} tee /etc/profile.d/ozone-gds.sh >/dev/null <<'PROFILE'
# Managed by hadoop-hdds/container-service-native/dev-support/setup-build-host.sh
# Safe to sync across runs.

# JDK (Maven 3.6+ + CMake's FindJNI both want JAVA_HOME)
if [ -d /usr/lib/jvm/java-17-openjdk-amd64 ]; then
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
fi

# CUDA Toolkit (user-space compile + link)
if [ -d /usr/local/cuda ]; then
  export CUDA_HOME=/usr/local/cuda
  case ":${PATH:-}:" in *":${CUDA_HOME}/bin:"*) ;; *) export PATH="${CUDA_HOME}/bin:${PATH:-}" ;; esac
  case ":${LD_LIBRARY_PATH:-}:" in
    *":${CUDA_HOME}/lib64:"*) ;;
    *) export LD_LIBRARY_PATH="${CUDA_HOME}/lib64${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}" ;;
  esac
fi

# cuObjServer — the Debian/Ubuntu package installs under /usr by default,
# but the spec's CMakeLists.txt expects $CUOBJSERVER_HOME. Point it at
# /usr so find_package / include_directories resolve against the
# system-package layout (headers in /usr/include, libs in
# /usr/lib/x86_64-linux-gnu). If a future release moves to /opt, update
# here.
if [ -f /usr/include/cuobjserver.h ]; then
  export CUOBJSERVER_HOME=/usr
elif [ -d /opt/cuobjserver ]; then
  export CUOBJSERVER_HOME=/opt/cuobjserver
fi
PROFILE

# -- Step 8: Verification -----------------------------------------------------

log "Verifying installation"
# shellcheck source=/dev/null
. /etc/profile.d/ozone-gds.sh

echo "-- tool versions --"
java -version
mvn -version | head -3
cmake --version | head -1
gcc --version | head -1

echo "-- CUDA headers + libs --"
ls -d "${CUDA_HOME:-/usr/local/cuda}/include/cuda_runtime.h" \
      "${CUDA_HOME:-/usr/local/cuda}/lib64/libcudart.so" 2>/dev/null \
  || echo "WARN: CUDA userspace files not at expected paths; inspect /usr/local/cuda*"

echo "-- cuObjServer headers + libs --"
for f in /usr/include/cuobjserver.h \
         /usr/lib/x86_64-linux-gnu/libcuobjserver.so \
         /usr/lib/libcuobjserver.so; do
  [ -e "$f" ] && echo "  found: $f"
done

echo "-- ibverbs / rdma-cm --"
dpkg -l libibverbs-dev librdmacm-dev libnuma-dev 2>/dev/null \
  | awk '/^ii/{printf "  %-22s %s\n", $2, $3}'

log "Bootstrap complete."
echo "Next: new shells will pick up CUDA / cuObjServer env automatically."
echo "For this shell:   . /etc/profile.d/ozone-gds.sh"
echo "Then:             echo \$CUOBJSERVER_HOME   # should print /usr or /opt/cuobjserver"
