/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.hadoop.ozone.container.keyvalue.helpers;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.hdds.client.BlockID;
import org.apache.hadoop.hdds.conf.ConfigurationSource;
import org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos;
import org.apache.hadoop.hdds.scm.container.common.helpers.StorageContainerException;
import org.apache.hadoop.ozone.OzoneConsts;
import org.apache.hadoop.ozone.container.common.helpers.BlockData;
import org.apache.hadoop.ozone.container.common.interfaces.Container;
import org.apache.hadoop.ozone.container.common.interfaces.DBHandle;
import org.apache.hadoop.ozone.container.common.utils.ContainerCache;
import org.apache.hadoop.ozone.container.common.utils.DatanodeStoreCache;
import org.apache.hadoop.ozone.container.common.utils.RawDB;
import org.apache.hadoop.ozone.container.common.utils.ReferenceCountedDB;
import org.apache.hadoop.ozone.container.keyvalue.KeyValueContainer;
import org.apache.hadoop.ozone.container.keyvalue.KeyValueContainerData;

import com.google.common.base.Preconditions;
import org.apache.hadoop.ozone.container.metadata.DatanodeStore;
import org.apache.hadoop.ozone.container.metadata.DatanodeStoreSchemaOneImpl;
import org.apache.hadoop.ozone.container.metadata.DatanodeStoreSchemaThreeImpl;
import org.apache.hadoop.ozone.container.metadata.DatanodeStoreSchemaTwoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.hadoop.hdds.HddsConfigKeys.HDDS_CONTAINER_EXPORT_TMPDIR;
import static org.apache.hadoop.hdds.HddsConfigKeys.HDDS_CONTAINER_EXPORT_TMPDIR_DEFAULT;
import static org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.Result.CONTAINER_NOT_FOUND;
import static org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.Result.EXPORT_CONTAINER_METADATA_FAILED;
import static org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.Result.IMPORT_CONTAINER_METADATA_FAILED;
import static org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.Result.NO_SUCH_BLOCK;
import static org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.Result.UNABLE_TO_READ_METADATA_DB;
import static org.apache.hadoop.hdds.protocol.datanode.proto.ContainerProtos.Result.UNKNOWN_BCSID;
import static org.apache.hadoop.ozone.container.common.utils.StorageVolumeUtil.onFailure;
import static org.apache.hadoop.ozone.container.keyvalue.helpers.KeyValueContainerUtil.isSameSchemaVersion;

/**
 * Utils functions to help block functions.
 */
public final class BlockUtils {
  private static final Logger LOG =
      LoggerFactory.getLogger(KeyValueContainer.class);

  /** Never constructed. **/
  private BlockUtils() {

  }

  /**
   * Obtain a DB handler for a given container or the underlying volume.
   * This handler is not cached and the caller must close it after using it.
   * If another thread attempts to open the same container when it is already
   * opened by this thread, the other thread will get a RocksDB exception.
   * @param containerDBPath The absolute path to the container database folder
   * @param schemaVersion The Container Schema version
   * @param conf Configuration
   * @param readOnly open DB in read-only mode or not
   * @return Handler to the given container.
   * @throws IOException
   */
  public static DatanodeStore getUncachedDatanodeStore(
      String containerDBPath, String schemaVersion,
      ConfigurationSource conf, boolean readOnly) throws IOException {

    DatanodeStore store;
    if (isSameSchemaVersion(schemaVersion, OzoneConsts.SCHEMA_V1)) {
      store = new DatanodeStoreSchemaOneImpl(conf, containerDBPath, readOnly);
    } else if (isSameSchemaVersion(schemaVersion, OzoneConsts.SCHEMA_V2)) {
      store = new DatanodeStoreSchemaTwoImpl(conf, containerDBPath, readOnly);
    } else if (isSameSchemaVersion(schemaVersion, OzoneConsts.SCHEMA_V3)) {
      store = new DatanodeStoreSchemaThreeImpl(conf, containerDBPath,
          readOnly);
    } else {
      throw new IllegalArgumentException(
          "Unrecognized database schema version: " + schemaVersion);
    }
    return store;
  }

  /**
   * Obtain a DB handler for a given container. This handler is not cached and
   * the caller must close it after using it.
   * If another thread attempts to open the same container when it is already
   * opened by this thread, the other thread will get a RocksDB exception.
   * @param containerData The container data
   * @param conf Configuration
   * @throws IOException
   */
  public static DatanodeStore getUncachedDatanodeStore(
      KeyValueContainerData containerData, ConfigurationSource conf,
      boolean readOnly) throws IOException {
    return getUncachedDatanodeStore(
        containerData.getDbFile().getAbsolutePath(),
        containerData.getSchemaVersion(), conf, readOnly);
  }

  /**
   * Get a DB handler for a given container.
   * If the handler doesn't exist in cache yet, first create one and
   * add into cache. This function is called with containerManager
   * ReadLock held.
   *
   * @param containerData containerData.
   * @param conf configuration.
   * @return DB handle.
   * @throws StorageContainerException
   */
  public static DBHandle getDB(KeyValueContainerData containerData,
      ConfigurationSource conf) throws StorageContainerException {
    Preconditions.checkNotNull(containerData);
    Preconditions.checkNotNull(containerData.getDbFile());

    String containerDBPath = containerData.getDbFile().getAbsolutePath();
    try {
      if (containerData.hasSchema(OzoneConsts.SCHEMA_V3)) {
        DatanodeStoreCache cache = DatanodeStoreCache.getInstance();
        Preconditions.checkNotNull(cache);
        return cache.getDB(containerDBPath, conf);
      } else {
        ContainerCache cache = ContainerCache.getInstance(conf);
        Preconditions.checkNotNull(cache);
        return cache.getDB(containerData.getContainerID(), containerData
                .getContainerDBType(), containerDBPath,
            containerData.getSchemaVersion(), conf);
      }
    } catch (IOException ex) {
      onFailure(containerData.getVolume());
      String message = String.format("Error opening DB. Container:%s " +
          "ContainerPath:%s", containerData.getContainerID(), containerData
          .getDbFile().getPath());
      throw new StorageContainerException(message, UNABLE_TO_READ_METADATA_DB);
    }
  }
  /**
   * Remove a DB handler from cache.
   *
   * @param container - Container data.
   * @param conf - Configuration.
   */
  public static void removeDB(KeyValueContainerData container,
      ConfigurationSource conf) {
    Preconditions.checkNotNull(container);
    Preconditions.checkNotNull(container.getDbFile());
    Preconditions.checkState(!container.hasSchema(OzoneConsts.SCHEMA_V3));

    ContainerCache cache = ContainerCache.getInstance(conf);
    Preconditions.checkNotNull(cache);
    cache.removeDB(container.getDbFile().getAbsolutePath());
  }

  /**
   * Shutdown all DB Handles.
   *
   * @param config
   */
  public static void shutdownCache(ConfigurationSource config) {
    ContainerCache.getInstance(config).shutdownCache();
    DatanodeStoreCache.getInstance().shutdownCache();
  }

  /**
   * Add a DB handler into cache.
   *
   * @param store - low-level DatanodeStore for DB.
   * @param containerDBPath - DB path of the container.
   * @param conf configuration.
   * @param schemaVersion schemaVersion.
   */
  public static void addDB(DatanodeStore store, String containerDBPath,
      ConfigurationSource conf, String schemaVersion) {
    if (isSameSchemaVersion(schemaVersion, OzoneConsts.SCHEMA_V3)) {
      DatanodeStoreCache cache = DatanodeStoreCache.getInstance();
      Preconditions.checkNotNull(cache);
      cache.addDB(containerDBPath, new RawDB(store, containerDBPath));
    } else {
      ContainerCache cache = ContainerCache.getInstance(conf);
      Preconditions.checkNotNull(cache);
      cache.addDB(containerDBPath,
          new ReferenceCountedDB(store, containerDBPath));
    }
  }

  /**
   * Parses the {@link BlockData} from a bytes array.
   *
   * @param bytes Block data in bytes.
   * @return Block data.
   * @throws IOException if the bytes array is malformed or invalid.
   */
  public static BlockData getBlockData(byte[] bytes) throws IOException {
    try {
      ContainerProtos.BlockData blockData = ContainerProtos.BlockData.parseFrom(
          bytes);
      return BlockData.getFromProtoBuf(blockData);
    } catch (IOException e) {
      throw new StorageContainerException("Failed to parse block data from " +
          "the bytes array.", NO_SUCH_BLOCK);
    }
  }

  /**
   * Verify if request block BCSID is supported.
   *
   * @param container container object.
   * @param blockID requested block info
   * @throws IOException if cannot support block's blockCommitSequenceId
   */
  public static void verifyBCSId(Container container, BlockID blockID)
      throws IOException {
    long bcsId = blockID.getBlockCommitSequenceId();
    Preconditions.checkNotNull(blockID,
        "BlockID cannot be null");
    Preconditions.checkNotNull(container,
        "Container cannot be null");

    long containerBCSId = container.getBlockCommitSequenceId();
    if (containerBCSId < bcsId) {
      throw new StorageContainerException(
          "Unable to find the block with bcsID " + bcsId + " .Container "
              + container.getContainerData().getContainerID() + " bcsId is "
              + containerBCSId + ".", UNKNOWN_BCSID);
    }
  }

  /**
   * Verify if request's replicaIndex matches with containerData.
   *
   * @param container container object.
   * @param blockID requested block info
   * @throws IOException if replicaIndex mismatches.
   */
  public static void verifyReplicaIdx(Container container, BlockID blockID)
      throws IOException {
    Integer containerReplicaIndex = container.getContainerData().getReplicaIndex();
    Integer blockReplicaIndex = blockID.getReplicaIndex();
    if (containerReplicaIndex > 0 && blockReplicaIndex != null && blockReplicaIndex != 0 &&
        !containerReplicaIndex.equals(blockReplicaIndex)) {
      throw new StorageContainerException(
          "Unable to find the Container with replicaIdx " + blockID.getReplicaIndex() + ". Container "
              + container.getContainerData().getContainerID() + " replicaIdx is "
              + containerReplicaIndex + ".", CONTAINER_NOT_FOUND);
    }
  }

  /**
   * Remove container KV metadata from per-disk db store.
   * @param containerData
   * @param conf
   * @throws IOException
   */
  public static void removeContainerFromDB(KeyValueContainerData containerData,
      ConfigurationSource conf) throws IOException {
    try (DBHandle db = getDB(containerData, conf)) {
      Preconditions.checkState(db.getStore()
          instanceof DatanodeStoreSchemaThreeImpl);

      ((DatanodeStoreSchemaThreeImpl) db.getStore()).removeKVContainerData(
          containerData.getContainerID());
    }
  }

  /**
   * Dump container KV metadata to external files.
   * @param containerData
   * @param conf
   * @throws StorageContainerException
   */
  public static void dumpKVContainerDataToFiles(
      KeyValueContainerData containerData,
      ConfigurationSource conf) throws IOException {
    try (DBHandle db = getDB(containerData, conf)) {
      Preconditions.checkState(db.getStore()
          instanceof DatanodeStoreSchemaThreeImpl);

      DatanodeStoreSchemaThreeImpl store = (DatanodeStoreSchemaThreeImpl)
          db.getStore();
      long containerID = containerData.getContainerID();
      //File metaDir = new File(containerData.getMetadataPath());
      //File dumpDir = DatanodeStoreSchemaThreeImpl.getDumpDir(metaDir);
      File tarExportPath = new File(conf.get(HDDS_CONTAINER_EXPORT_TMPDIR,
          HDDS_CONTAINER_EXPORT_TMPDIR_DEFAULT), String.valueOf(containerID));
      File dumpDir = DatanodeStoreSchemaThreeImpl.getDumpDir(tarExportPath);
      LOG.debug("ATTENTION! Attempting to clean up old dump files from directory: {}", dumpDir.getAbsolutePath());
      // cleanup old files first
      deleteAllDumpFiles(dumpDir);

      try {
        LOG.debug("ATTENTION! Attempting to create the dump directory: {}", dumpDir.getAbsolutePath());
        if (!dumpDir.mkdirs() && !dumpDir.exists()) {
          throw new IOException("Failed to create dir "
              + dumpDir.getAbsolutePath() + " for container " + containerID +
              " to dump metadata to files");
        }
        LOG.debug("ATTENTION! Successfully created the dump directory: {}", dumpDir.getAbsolutePath());
        LOG.debug("ATTENTION! Creating dir {} for container {} to dump metadata to files",
            dumpDir.getAbsolutePath(), containerID);
        LOG.debug("ATTENTION! Starting to dump metadata for container ID {} into directory: {}", containerID,
            dumpDir.getAbsolutePath());
        store.dumpKVContainerData(containerID, dumpDir);
        store.dumpKVContainerData(containerID, dumpDir);
      } catch (IOException e) {
        LOG.error(
            "ATTENTION! Failed to dump metadata for container ID {}. Cleaning up partially dumped files from directory: {}",
            containerID, dumpDir.getAbsolutePath(), e);
        // cleanup partially dumped files
        deleteAllDumpFiles(dumpDir);
        throw new StorageContainerException("Failed to dump metadata" +
            " for container " + containerID, e,
            EXPORT_CONTAINER_METADATA_FAILED);
      }
    }
  }

  /**
   * Load container KV metadata from external files.
   *
   * @param containerData
   * @param conf
   * @throws StorageContainerException
   */
  public static void loadKVContainerDataFromFiles(
      KeyValueContainerData containerData,
      ConfigurationSource conf) throws IOException {
    LOG.debug("ATTENTION! Starting to load container KV metadata from external files for container ID {}.",
        containerData.getContainerID());
    try (DBHandle db = getDB(containerData, conf)) {
      Preconditions.checkState(db.getStore()
          instanceof DatanodeStoreSchemaThreeImpl);

      DatanodeStoreSchemaThreeImpl store = (DatanodeStoreSchemaThreeImpl)
          db.getStore();
      long containerID = containerData.getContainerID();
      LOG.debug("ATTENTION! Container ID {} retrieved. Preparing dump directory.", containerID);

      File tarExportPath = new File(conf.get(HDDS_CONTAINER_EXPORT_TMPDIR,
          HDDS_CONTAINER_EXPORT_TMPDIR_DEFAULT), String.valueOf(containerID));
      File dumpDir = DatanodeStoreSchemaThreeImpl.getDumpDir(tarExportPath);
      LOG.debug("ATTENTION! Dump directory determined as {} for container ID {}.", dumpDir.getAbsolutePath(),
          containerID);
      try {
        store.loadKVContainerData(dumpDir);
        LOG.debug("ATTENTION! Successfully loaded KV metadata from dump directory {} for container ID {}.",
            dumpDir.getAbsolutePath(), containerID);
      } catch (IOException e) {
        throw new IOException(e);
//        LOG.error("ATTENTION! Failed to load KV metadata from dump directory {} for container ID {}.",
//            dumpDir.getAbsolutePath(), containerID, e);
//        store.removeKVContainerData(containerID);
//        throw new StorageContainerException("Failed to load metadata " +
//            "from files for container " + containerID, e,
//            IMPORT_CONTAINER_METADATA_FAILED);
      } finally {
        LOG.info("ATTENTION! Deleting dump directory {} for container ID {} after loading metadata.",
            dumpDir.getAbsolutePath(), containerID);
        deleteAllDumpFiles(dumpDir);
      }
    }
    LOG.debug("ATTENTION! Finished loading KV metadata for container ID {}.", containerData.getContainerID());
  }

  public static void deleteAllDumpFiles(File dumpDir) throws IOException {
    try {
      FileUtils.deleteDirectory(dumpDir);
    } catch (IOException e) {
      throw new IOException("Failed to delete dump files under "
          + dumpDir.getAbsolutePath(), e);
    }
  }
}
