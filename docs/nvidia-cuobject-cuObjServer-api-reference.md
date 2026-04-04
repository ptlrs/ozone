# NVIDIA cuObjServer API Specification v1.0.0 - Complete Content

## Overview

The cuObjServer library provides server-side APIs for RDMA-based object operations with GPUDirect Storage support, enabling high-performance data transfers through RDMA protocols, specifically RDMA Dynamically Connected (DC) transports.

### Key Features
- Server-side RDMA connection management
- Memory registration for RDMA transfers (single buffer and scatter-gather)
- Synchronous and asynchronous RDMA operations (GET and PUT)
- Multi-threaded operation support with channel management
- Configurable RDMA tuning parameters
- Telemetry and logging capabilities
- Maximum operation size: 1 GiB per handleGetObject and handlePutObject call

### Protocol Support
- CUOBJ_PROTO_RDMA_DC_V1 (RDMA Dynamically Connected version 1)

## Core Types and Enumerations

### Error Types
```c
typedef enum cuObjErr_enum {
    CU_OBJ_SUCCESS = 0,
    CU_OBJ_FAIL = 1
} cuObjErr_t;
```

### Protocol Types
```c
typedef enum cuObjProto_enum {
    CUOBJ_PROTO_RDMA_DC_V1 = 1001,
    CUOBJ_PROTO_MAX
} cuObjProto_t;
```

### Operation Types
```c
typedef enum cuObjOpType_enum {
    CUOBJ_GET = 0,
    CUOBJ_PUT = 1,
    CUOBJ_INVALID = 9999
} cuObjOpType_t;
```

### Channel ID States
```c
typedef enum cuObjChannelIdState_enum {
    CHANNEL_ID_FREE = 0,
    CHANNEL_ID_ALLOCATED = 1,
    CHANNEL_ID_IN_USE = 2,
    INVALID_CHANNEL_ID = UINT16_MAX
} cuObjChannelIdState_t;
```

### Delay Modes
```c
typedef enum cuObjDelayMode {
    CUOBJ_DELAY_NONE = 0,
    CUOBJ_DELAY_BATCH = 1,
    CUOBJ_DELAY_ENTRY = 2,
    CUOBJ_DELAY_ADAPTIVE = 3,
    CUOBJ_DELAY_INVALID = 4
} cuObjDelayMode_t;
```

## RDMAConnection Class

### Class Declaration
```c
class RDMAConnection {
public:
    cuObjRDMATunable params;

    RDMAConnection(const char* ip, unsigned short port);
    ~RDMAConnection();

    int startRDMASession();
    void initRDMAConfigParams(cuObjRDMATunable config_params);
    void closeRDMASession();
    void handleDisconnectEvent(struct rdma_cm_id* id);

    char* getChannelIP(struct rdma_cm_id* id);
    int getChannelPort(struct rdma_cm_id* id);
    void getConfigTunableParam(struct cuObjRDMATunableParam* param);

protected:
    const char* myip;
    struct sockaddr_in addr;
    struct rdma_device* rdma_dev;
};
```

## cuObjServer Class API

### Class Declaration
```c
class cuObjServer : public RDMAConnection {
public:
    cuObjServer(const char* ip, unsigned short port, unsigned proto);
    cuObjServer(const char* ip, unsigned short port, unsigned proto,
                cuObjRDMATunable params);
    cuObjServer() = default;
    ~cuObjServer();

    // Memory management
    void* allocHostBuffer(size_t size);
    struct rdma_buffer* registerBuffer(void* ptr, size_t size);
    struct rdma_buffer* registerBuffer(std::vector<cuObjScatterGatherEntry_t> sglist);
    void deRegisterBuffer(struct rdma_buffer* rdma_buff);

    // Scatter-gather support
    struct rdma_buffer* getRDMABufferFromSgList(
        struct rdma_buffer* rdma_buffer,
        std::vector<cuObjScatterGatherEntryWithOffset_t> sglist);
    void putRDMABufferFromSgList(struct rdma_buffer* rdma_buffer);

    // I/O operations (synchronous)
    ssize_t handleGetObject(const std::string& key,
                            struct rdma_buffer* local_rdma_buff,
                            uint64_t remote_buf_start, size_t size,
                            const std::string& rdma_descr,
                            uint16_t channel = 0, uint64_t local_offset = 0,
                            ibv_wc_status* status = nullptr,
                            void* async_handle = nullptr);

    ssize_t handlePutObject(const std::string& key,
                            struct rdma_buffer* local_rdma_buff,
                            uint64_t remote_buf_start, size_t size,
                            const std::string& rdma_descr,
                            uint16_t channel = 0, uint64_t local_offset = 0,
                            ibv_wc_status* status = nullptr,
                            void* async_handle = nullptr);

    // I/O operations (with poll delay)
    ssize_t handleGetObject(const std::string& key,
                            struct rdma_buffer* local_rdma_buff,
                            uint64_t remote_buf_start, size_t size,
                            const std::string& rdma_descr,
                            uint32_t poll_delay,
                            uint16_t channel = 0, uint64_t local_offset = 0,
                            ibv_wc_status* status = nullptr,
                            void* async_handle = nullptr);

    ssize_t handlePutObject(const std::string& key,
                            struct rdma_buffer* local_rdma_buff,
                            uint64_t remote_buf_start, size_t size,
                            const std::string& rdma_descr,
                            uint32_t poll_delay,
                            uint16_t channel = 0, uint64_t local_offset = 0,
                            ibv_wc_status* status = nullptr,
                            void* async_handle = nullptr);

    // Asynchronous operations
    int poll(cuObjAsyncEvent_t* events, int max_events, uint16_t channel = 0);

    // Channel management
    uint16_t allocateChannelId();
    void freeChannelId(uint16_t channel_id);

    // Connection status
    bool isConnected();

    // Static telemetry
    static void setupTelemetry(bool use_OTEL, std::ostream* os);
    static void shutdownTelemetry();
    static void setTelemFlags(unsigned flags);
};
```

## Memory Management APIs

### allocHostBuffer
Allocates a 4 KB aligned host buffer suitable for RDMA operations.
- **Parameters**: size_t size
- **Returns**: Pointer to allocated buffer (NULL on failure)
- **Notes**: Caller responsible for freeing with free()

### registerBuffer (single buffer)
Registers a contiguous host buffer for RDMA operations.
- **Parameters**: void* ptr, size_t size
- **Returns**: Opaque RDMA handle (NULL on failure)
- **Notes**: Buffer must remain valid until deRegisterBuffer() is called

### registerBuffer (scatter-gather list)
Registers a scatter-gather list of non-contiguous host buffers.
- **Parameters**: std::vector<cuObjScatterGatherEntry_t> sglist
- **Returns**: Opaque RDMA handle (NULL on failure)
- **Notes**: Maximum 10 entries; local_offset is ignored

### getRDMABufferFromSgList
Creates an RDMA buffer view from a registered buffer using scatter-gather offsets.
- **Parameters**: struct rdma_buffer* rdma_buffer, std::vector<cuObjScatterGatherEntryWithOffset_t> sglist
- **Returns**: New opaque RDMA handle (NULL on failure)
- **Notes**: Creates derived view; must call putRDMABufferFromSgList() to free

### putRDMABufferFromSgList
Frees an RDMA buffer view created by getRDMABufferFromSgList.
- **Parameters**: struct rdma_buffer* rdma_buffer
- **Notes**: Does not deregister underlying buffer

### deRegisterBuffer
Deregisters an RDMA buffer and releases associated resources.
- **Parameters**: struct rdma_buffer* rdma_buff
- **Notes**: Does not free underlying memory

## I/O Operations

### handleGetObject
Handles GET operation by performing RDMA_WRITE to client memory.

**Parameters**:
- key: Request identifier
- local_rdma_buff: Registered local memory handle (source data)
- remote_buf_start: Starting address in remote client memory
- size: Size of operation in bytes (max 1 GiB)
- rdma_descr: RDMA descriptor string from client
- channel: Channel ID from allocateChannelId() (default: 0)
- local_offset: Offset into local buffer (ignored for scatter-gather)
- status: Optional pointer to InfiniBand verbs completion status
- async_handle: User handle for async operation

**Returns**:
- Positive: Bytes transferred (synchronous success)
- Negative: Error code
- 0: Successful async submission (when async_handle is non-NULL)

### handlePutObject
Handles PUT operation by performing RDMA_READ from client memory.

**Parameters**:
- key: Request identifier
- local_rdma_buff: Registered local memory handle (destination)
- remote_buf_start: Starting address in remote client memory
- size: Size of operation in bytes (max 1 GiB)
- rdma_descr: RDMA descriptor string from client
- channel: Channel ID (default: 0)
- local_offset: Offset into local buffer
- status: Optional pointer to InfiniBand verbs completion status
- async_handle: User handle for async operation

**Returns**:
- Positive: Bytes transferred
- Negative: Error code
- 0: Successful async submission (when async_handle is non-NULL)

## Asynchronous Operations

### cuObjAsyncEvent_t Structure
```c
typedef struct cuObjAsyncEvent_s {
    void* async_handle;
    ssize_t status;
} cuObjAsyncEvent_t;
```

### poll
Polls for completion of async handleGetObject and handlePutObject operations.

**Parameters**:
- events: Array to store completion events
- max_events: Maximum number of events to return
- channel: Channel ID to poll (must match async operation channel, default: 0)

**Returns**:
- Number of completed events (0 to max_events)
- Negative value on error
- -EINVAL: events is nullptr or channel is invalid
- -EIO: polled entry does not complete successfully

## Channel Management

### allocateChannelId
Allocates a unique channel ID for multi-threaded applications.
- **Returns**: Allocated channel ID (INVALID_CHANNEL_ID on failure)

### freeChannelId
Releases a previously allocated channel ID.
- **Parameters**: uint16_t channel_id
- **Note**: Do not free default channel (0)

## Connection Management

### isConnected
Checks if the server is connected and ready for operations.
- **Returns**: true if connected and operational, false otherwise

## Telemetry Management

### setupTelemetry
Configures telemetry output stream for logging and monitoring.
- **Parameters**: bool use_OTEL, std::ostream* os
- **Note**: Output stream must remain valid until shutdownTelemetry() is called

### shutdownTelemetry
```c
static void shutdownTelemetry();
```

### setTelemFlags
```c
static void setTelemFlags(unsigned flags);
```

## Supporting Structures

### cuObjScatterGatherEntry_t
```c
typedef struct cuObjScatterGatherEntry {
    void* addr;
    size_t size;
} cuObjScatterGatherEntry_t;
```

### cuObjScatterGatherEntryWithOffset_t
```c
typedef struct cuObjScatterGatherEntryWithOffset {
    loff_t offset;
    size_t size;
} cuObjScatterGatherEntryWithOffset_t;
```

### cuObjRDMATunableParam
```c
struct cuObjRDMATunableParam {
    int num_dcis;                    // default: 128
    unsigned cq_depth;               // default: 640
    unsigned long dc_key;            // default: 0xffeeddcc
    int ibv_poll_max_comp_event;     // unused
    int service_level;               // default: 0
    uint8_t timeout;                 // default: 16
    unsigned hop_limit;              // default: 4
    int pkey_index;                  // default: 0
    int max_wr;                      // unused
    int max_sge;                     // default: 10
    uint32_t delay_interval;         // default: 5000 ns
    cuObjDelayMode_t delay_mode;     // default: CUOBJ_DELAY_BATCH
    bool qp_reset_on_failure;        // default: true
    uint8_t retry_cnt;               // default: 7
    unsigned traffic_class;          // default: 96
};
```

## Error Handling

### Return Value Conventions
- I/O operations return ssize_t: positive (bytes), negative (error), 0 (async)
- Memory management returns pointers (NULL on error) or void
- Channel management returns channel ID (INVALID_CHANNEL_ID on error)
- Connection status returns boolean

### InfiniBand Verbs Status
The optional ibv_wc_status* status parameter provides:
- IBV_WC_SUCCESS
- IBV_WC_LOC_LEN_ERR
- IBV_WC_LOC_PROT_ERR
- IBV_WC_REM_ACCESS_ERR
- IBV_WC_RETRY_EXC_ERR
- IBV_WC_RNR_RETRY_EXC_ERR

## Constants and Limits

### Operation Limits
- Maximum operation size: 1 GiB per call
- Maximum scatter-gather entries: 10 per registerBuffer(sglist)
- Maximum SGE per operation: configurable via max_sge (default: 10)

### Channel Limits
- Default channel: 0 (always available)
- Channel ID range: 0 to 65534
- INVALID_CHANNEL_ID: 65535

### RDMA Defaults
- Number of DCIs: 128
- CQ depth: 640
- DC key: 0xffeeddcc
- Service level: 0
- Timeout: 16
- Hop limit: 4
- Partition key index: 0
- Retry count: 7
- Polling delay: 5000 ns

### Memory Requirements
- Buffer alignment: 4 KB recommended
- Memory type: host memory (system RAM)
- Registered buffers are pinned for RDMA

## Thread Safety and Channel Management

### Thread Safety Model
- Constructors/destructors: not thread-safe
- Memory registration: thread-safe for different buffers
- I/O operations: thread-safe only with unique channel IDs
- Channel management: thread-safe with internal synchronization
- Static methods: thread-safe with internal synchronization

### Channel Usage Rules
- Default channel (0): always available, suitable for single-threaded
- Allocated channels: each thread must allocate its own channel
- Async operations: poll() must be called on same channel as submission

## Best Practices

- Use allocHostBuffer() for 4 KB aligned buffers
- Set qp_reset_on_failure = true for automatic recovery
- Tune delay_interval based on workload characteristics
- Use async for batch operations; sync for simplicity
- Limit concurrent operations based on cq_depth
- Reuse buffers after completion
- Follow cleanup order: complete operations, deregister buffers, free channels
- Verify RDMA devices are configured and subnet manager is running
