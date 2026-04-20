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

cmake_minimum_required(VERSION 3.10)
project(cuobj_server_bridge CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

# --------------------------------------------------------------------
# Locate cuObjServer SDK
# --------------------------------------------------------------------
# Supports two install layouts:
#   (a) system package: Debian/Ubuntu `libcuobjserver-dev` places
#       headers at /usr/include and the shared lib at
#       /usr/lib/x86_64-linux-gnu. Detected via pkg-config module
#       `cuobjserver-13.1` (name carries CUDA release suffix; probe a
#       few variants in case NVIDIA bumps the suffix in future SDK
#       releases).
#   (b) tarball install: headers under $CUOBJSERVER_HOME/include and
#       libs under $CUOBJSERVER_HOME/lib64 (or lib). Used when NVIDIA
#       ships the SDK as a standalone tarball instead of apt.
#
# Precedence: if $CUOBJSERVER_HOME is set, trust it. Otherwise fall
# back to pkg-config, then to a bare find_library search.

if(NOT CUOBJSERVER_HOME)
  set(CUOBJSERVER_HOME $ENV{CUOBJSERVER_HOME})
endif()

set(CUOBJSERVER_FOUND FALSE)

if(CUOBJSERVER_HOME AND EXISTS "${CUOBJSERVER_HOME}/include/cuobjserver.h")
  message(STATUS "cuObjServer: using CUOBJSERVER_HOME=${CUOBJSERVER_HOME}")
  set(CUOBJSERVER_INCLUDE_DIRS "${CUOBJSERVER_HOME}/include")
  find_library(CUOBJSERVER_LIBRARY
    NAMES cuobjserver
    PATHS
      "${CUOBJSERVER_HOME}/lib64"
      "${CUOBJSERVER_HOME}/lib"
      "${CUOBJSERVER_HOME}/lib/x86_64-linux-gnu"
    NO_DEFAULT_PATH)
  if(NOT CUOBJSERVER_LIBRARY)
    # CUOBJSERVER_HOME=/usr case — fall through to standard paths below.
    find_library(CUOBJSERVER_LIBRARY NAMES cuobjserver)
  endif()
  if(CUOBJSERVER_LIBRARY)
    set(CUOBJSERVER_FOUND TRUE)
  endif()
endif()

if(NOT CUOBJSERVER_FOUND)
  find_package(PkgConfig QUIET)
  if(PkgConfig_FOUND)
    foreach(_pc_name cuobjserver cuobjserver-13.1 cuobjserver-13.2 cuobjserver-13.3)
      pkg_check_modules(_CUOBJSERVER_PC QUIET ${_pc_name})
      if(_CUOBJSERVER_PC_FOUND)
        message(STATUS "cuObjServer: resolved via pkg-config ${_pc_name}")
        set(CUOBJSERVER_INCLUDE_DIRS ${_CUOBJSERVER_PC_INCLUDE_DIRS})
        set(CUOBJSERVER_LIBRARY ${_CUOBJSERVER_PC_LIBRARIES})
        list(APPEND CUOBJSERVER_LDFLAGS ${_CUOBJSERVER_PC_LDFLAGS})
        set(CUOBJSERVER_FOUND TRUE)
        break()
      endif()
    endforeach()
  endif()
endif()

if(NOT CUOBJSERVER_FOUND)
  # Last-resort raw search across standard system paths.
  find_path(CUOBJSERVER_INCLUDE_DIRS NAMES cuobjserver.h)
  find_library(CUOBJSERVER_LIBRARY NAMES cuobjserver)
  if(CUOBJSERVER_INCLUDE_DIRS AND CUOBJSERVER_LIBRARY)
    set(CUOBJSERVER_FOUND TRUE)
    message(STATUS "cuObjServer: resolved via system paths")
  endif()
endif()

if(NOT CUOBJSERVER_FOUND)
  message(FATAL_ERROR
    "cuObjServer SDK not found. Install the `libcuobjserver-dev` apt "
    "package from the NVIDIA CUDA repo, or set CUOBJSERVER_HOME to a "
    "tarball install root that contains include/cuobjserver.h.")
endif()

message(STATUS "cuObjServer include: ${CUOBJSERVER_INCLUDE_DIRS}")
message(STATUS "cuObjServer library: ${CUOBJSERVER_LIBRARY}")

include_directories(${CUOBJSERVER_INCLUDE_DIRS})

# --------------------------------------------------------------------
# Build the JNI bridge
# --------------------------------------------------------------------
add_library(cuobj_server_bridge SHARED cuobj_server_bridge.cc)

target_link_libraries(cuobj_server_bridge PRIVATE ${CUOBJSERVER_LIBRARY})

# Export only the JNI entry points to keep the ABI minimal.
set_target_properties(cuobj_server_bridge PROPERTIES
  LINK_FLAGS "-Wl,--no-undefined")

install(TARGETS cuobj_server_bridge
  LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib)
