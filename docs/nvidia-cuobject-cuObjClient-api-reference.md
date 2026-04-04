# cuObjClient API Specification v1.0.0 - Complete Documentation

Source: https://docs.nvidia.com/gpudirect-storage/cuobject/cuObjClient-api/index.html

## Overview

The `cuObjClient` library enables "high performance GET and PUT operations with RDMA acceleration and GPUDirect Storage support." Key capabilities include memory registration for RDMA transfers, synchronous I/O operations with callbacks, support for system/CUDA memory types, and telemetry logging.

## Core Type Definitions

### Error Types
```c
typedef enum cuObjErr_enum {
    CU_OBJ_SUCCESS = 0,    // Operation successfully completed
    CU_OBJ_FAIL = 1        // Operation failed
} cuObjErr_t;
```

### Protocol Types
```c
typedef enum cuObjProto_enum {
    CUOBJ_PROTO_RDMA_DC_V1 = 1001,    // RDMA Dynamically Connected v1
    CUOBJ_PROTO_MAX
} cuObjProto_t;
```

### Operation Types
```c
typedef enum cuObjOpType_enum {
    CUOBJ_GET = 0,         // GET operation (read from server)
    CUOBJ_PUT = 1,         // PUT operation (write to server)
    CUOBJ_INVALID = 9999
} cuObjOpType_t;
```

### Memory Types
```c
typedef enum cuObjMemoryType_enum {
    CUOBJ_MEMORY_SYSTEM = 0,        // System (host) memory
    CUOBJ_MEMORY_CUDA_MANAGED = 1,  // CUDA unified/managed memory
    CUOBJ_MEMORY_CUDA_DEVICE = 2,   // CUDA device memory
    CUOBJ_MEMORY_UNKNOWN = 3,       // Unknown memory type
    CUOBJ_MEMORY_INVALID = 4        // Invalid memory type
} cuObjMemoryType_t;
```

## cuObjClient Class API

### Class Declaration
```cpp
class cuObjClient {
public:
    // Constructor & Destructor
    cuObjClient(CUObjOps_t& ops, cuObjProto_t proto = CUOBJ_PROTO_RDMA_DC_V1);
    ~cuObjClient();

    // Memory Management
    cuObjErr_t cuMemObjGetDescriptor(void* ptr, size_t size);
    cuObjErr_t cuMemObjPutDescriptor(void* ptr);
    ssize_t cuMemObjGetMaxRequestCallbackSize(void* ptr);
    cuObjErr_t cuMemObjGetRDMAToken(void* ptr, size_t size, size_t buffer_offset,
                                     cuObjOpType_t operation, char** desc_str_out);
    cuObjErr_t cuMemObjPutRDMAToken(char* desc_str);

    // I/O Operations
    ssize_t cuObjGet(void* ctx, void* ptr, size_t size, loff_t offset = 0,
                     loff_t buf_offset = 0);
    ssize_t cuObjPut(void* ctx, void* ptr, size_t size, loff_t offset = 0,
                     loff_t buf_offset = 0);

    // Connection Management
    bool isConnected(void);

    // Static Utilities
    static void* getCtx(const void* handle);
    static cuObjMemoryType_t getMemoryType(const void* ptr);

    // Telemetry Management
    static void setupTelemetry(bool use_OTEL, std::ostream* os);
    static void shutdownTelemetry();
    static void setTelemFlags(unsigned flags);
};
```

## Memory Management APIs

### cuMemObjGetDescriptor
Registers memory for RDMA operations. Maximum size: 4 GiB per buffer. Supports system memory, CUDA managed memory, and CUDA device memory.

```cpp
cuObjErr_t cuMemObjGetDescriptor(void* ptr, size_t size);
```
- Returns `CU_OBJ_SUCCESS` or `CU_OBJ_FAIL`

### cuMemObjPutDescriptor
Unregisters previously registered memory. Pointer must match `cuMemObjGetDescriptor` call.

```cpp
cuObjErr_t cuMemObjPutDescriptor(void* ptr);
```

### cuMemObjGetMaxRequestCallbackSize
Returns maximum callback size in bytes for registered memory. Multiple callbacks invoked if requested I/O exceeds this limit.

```cpp
ssize_t cuMemObjGetMaxRequestCallbackSize(void* ptr);
```

### cuMemObjGetRDMAToken
Generates RDMA descriptor string for registered buffer. Memory must be pre-registered.

```cpp
cuObjErr_t cuMemObjGetRDMAToken(void* ptr, size_t size, size_t buffer_offset,
                                 cuObjOpType_t operation, char** desc_str_out);
```
- Parameters: `operation` accepts `CUOBJ_GET` or `CUOBJ_PUT`
- Constraint: `buffer_offset + size` must not exceed originally registered buffer size
- Free string using `cuMemObjPutRDMAToken()`

### cuMemObjPutRDMAToken
Frees RDMA descriptor string allocated by `cuMemObjGetRDMAToken`. Calls underlying `cuFileRDMADescStrPut()`.

```cpp
cuObjErr_t cuMemObjPutRDMAToken(char* desc_str);
```

## I/O Operations

### cuObjGet
Performs GET operation to read data from remote server. Synchronous operation. Returns data size or negative error code.

```cpp
ssize_t cuObjGet(void* ctx, void* ptr, size_t size, loff_t offset = 0,
                 loff_t buf_offset = 0);
```
- Memory must be registered via `cuMemObjGetDescriptor()` first
- GET callback invoked one or more times during operation
- Multiple callbacks if size exceeds `MaxRequestCallbackSize`

### cuObjPut
Performs PUT operation to write data to remote server. Synchronous operation. Returns data size or negative error code.

```cpp
ssize_t cuObjPut(void* ctx, void* ptr, size_t size, loff_t offset = 0,
                 loff_t buf_offset = 0);
```
- Memory must be registered via `cuMemObjGetDescriptor()` first
- PUT callback invoked one or more times during operation
- Multiple callbacks if size exceeds `MaxRequestCallbackSize`

## Connection Management

### isConnected
Checks if client is connected and ready for operations.

```cpp
bool isConnected(void);
```
- Returns `true` if operational, `false` otherwise

## Static Utility Methods

### getCtx
Extracts user context from callback handle. Call from within callback.

```cpp
static void* getCtx(const void* handle);
```
- Parameter: `handle` is first parameter passed to callback
- Returns: User context pointer passed to `cuObjGet()` or `cuObjPut()`

### getMemoryType
Determines memory type of given pointer.

```cpp
static cuObjMemoryType_t getMemoryType(const void* ptr);
```
- Returns: `CUOBJ_MEMORY_SYSTEM`, `CUOBJ_MEMORY_CUDA_MANAGED`, `CUOBJ_MEMORY_CUDA_DEVICE`, `CUOBJ_MEMORY_UNKNOWN`, or `CUOBJ_MEMORY_INVALID`

## Telemetry Management

### setupTelemetry
Configures telemetry output stream for logging and monitoring. Affects all `cuObjClient` instances. Output stream must remain valid until `shutdownTelemetry()` called.

```cpp
static void setupTelemetry(bool use_OTEL, std::ostream* os);
```
- Parameter `use_OTEL`: Enable OpenTelemetry integration

### shutdownTelemetry
Shuts down telemetry and resets to default output stream. Call before closing custom output streams.

```cpp
static void shutdownTelemetry();
```

### setTelemFlags
Configures telemetry logging level and flags.

```cpp
static void setTelemFlags(unsigned flags);
```

## Callback Interface

### CUObjIOOps Structure
User-defined callback operations structure.

```cpp
typedef struct CUObjIOOps {
    ssize_t (*get)(const void* handle, char* ptr, size_t size, loff_t offset,
                   const cufileRDMAInfo_t* rdma_info);
    ssize_t (*put)(const void* handle, const char* ptr, size_t size, loff_t offset,
                   const cufileRDMAInfo_t* rdma_info);
} CUObjOps_t;
```

### GET Callback
Called during `cuObjGet()` to handle data retrieval.

```cpp
ssize_t (*get)(const void* handle, char* ptr, size_t size, loff_t offset,
               const cufileRDMAInfo_t* rdma_info);
```
- Returns: Size of data read on success, `-1` on failure
- Note: `offset` is 0 when `MaxRequestCallbackSize >= total request size`
- Multiple callbacks invoked for large requests
- May be called from different thread than caller
- Must communicate with `cuObjServer` using RDMA descriptor information

### PUT Callback
Called during `cuObjPut()` to handle data transmission.

```cpp
ssize_t (*put)(const void* handle, const char* ptr, size_t size, loff_t offset,
               const cufileRDMAInfo_t* rdma_info);
```
- Returns: Size of data written on success, `-1` on failure
- Multiple callbacks invoked for large requests
- May be called from different thread than caller
- Must communicate with `cuObjServer` using RDMA descriptor information

## Error Handling

### Error Codes
- `CU_OBJ_SUCCESS` (0): Operation completed successfully
- `CU_OBJ_FAIL` (1): Operation failed

### Return Value Conventions
- Memory management APIs return `cuObjErr_t`
- I/O operations return `ssize_t` (positive = bytes transferred, negative = error)
- Connection status returns boolean values
- Utility functions return pointers or specific types as documented

### Best Practices
- Always check return values for error conditions
- Ensure proper cleanup in error paths
- Callbacks should return `-1` on errors
- Verify memory is registered before I/O operations
- Keep RDMA tokens valid only as long as needed

## Constants and Limits

### Memory Limits
```c
#define CUOBJ_MAX_MEMORY_REG_SIZE (4ULL * 1024 * 1024 * 1024)  // 4GiB
```
- Maximum memory registration: 4 GiB per `cuMemObjGetDescriptor()` call
- No explicit limit on number of registered buffers
- Total RDMA resources limited by system capabilities

### Protocol Constants
```c
#define OBJ_RDMA_V1 "CUOBJ"
```
- Protocol identifier: `"CUOBJ"`
- Default protocol: `CUOBJ_PROTO_RDMA_DC_V1` (value: 1001)

### Operation Limits
- I/O operations split into chunks if exceeding `MaxRequestCallbackSize`
- Each callback receives contiguous memory chunk
- Total I/O size limited by registered buffer size

## Thread Safety

### Client Thread Safety
- Constructor and destructor not thread-safe; single-threaded creation/destruction required
- Memory registration not thread-safe for same buffer; synchronize if multiple threads register/deregister
- I/O operations thread-safe for different buffers; multiple threads can call `cuObjGet()` and `cuObjPut()` concurrently with different registered buffers
- Callbacks may be invoked from different threads; synchronize access to shared resources
- Each callback invocation serialized for single I/O operation
- Static methods thread-safe with internal synchronization

### Synchronization Guidelines
- Safe: Multiple threads with different buffers
- Unsafe: Multiple threads accessing same buffer without external synchronization

## Best Practices

- Memory alignment: Page-aligned buffers (4 KB) provide optimal performance
- RDMA capability: Not all memory types support RDMA; device memory requires GPUDirect RDMA support
- Error recovery: If callbacks return `-1`, I/O operation fails immediately
- Context lifetime: User context must remain valid until operation completes
- Descriptor lifetime: RDMA descriptors valid only during callback invocation
- Connection status: Check `isConnected()` after construction
- Cleanup order: Deregister memory before freeing buffers
- Callback performance: Keep callbacks fast; avoid heavy processing
