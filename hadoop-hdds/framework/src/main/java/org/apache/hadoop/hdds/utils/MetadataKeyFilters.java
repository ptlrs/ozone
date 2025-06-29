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

package org.apache.hadoop.hdds.utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import org.apache.hadoop.hdds.StringUtils;
import org.apache.hadoop.ozone.OzoneConsts;

/**
 * An utility class to filter levelDB keys.
 */
public final class MetadataKeyFilters {
  private MetadataKeyFilters() { }

  @Deprecated
  public static KeyPrefixFilter getDeletingKeyFilter() {
    return new MetadataKeyFilters.KeyPrefixFilter()
            .addFilter(OzoneConsts.DELETING_KEY_PREFIX);
  }

  /**
   * @return A {@link KeyPrefixFilter} that ignores all keys beginning with
   * #. This uses the convention that key prefixes are surrounded by
   * # to ignore keys with any prefix currently used or that will be
   * added in the future.
   */
  public static KeyPrefixFilter getUnprefixedKeyFilter() {
    return new MetadataKeyFilters.KeyPrefixFilter()
            .addFilter("#", true);
  }

  /**
   * Interface for RocksDB key filters.
   */
  public interface MetadataKeyFilter {
    /**
     * Filter levelDB key with a certain condition.
     *
     * @param currentKey current key.
     * @return true if a certain condition satisfied, return false otherwise.
     */
    boolean filterKey(byte[] currentKey);

    default int getKeysScannedNum() {
      return 0;
    }

    default int getKeysHintedNum() {
      return 0;
    }
  }

  /**
   * Utility class to filter key by a string prefix. This filter
   * assumes keys can be parsed to a string.
   */
  public static final class KeyPrefixFilter implements MetadataKeyFilter {
    private List<String> positivePrefixList = new ArrayList<>();
    private List<String> negativePrefixList = new ArrayList<>();
    private int keysScanned = 0;
    private int keysHinted = 0;

    private KeyPrefixFilter() { }

    public KeyPrefixFilter addFilter(String keyPrefix) {
      addFilter(keyPrefix, false);
      return this;
    }

    public KeyPrefixFilter addFilter(String keyPrefix, boolean negative) {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(keyPrefix),
          "KeyPrefix is null or empty: %s", keyPrefix);
      // keyPrefix which needs to be added should not be prefix of any opposing
      // filter already present. If keyPrefix is a negative filter it should not
      // be a prefix of any positive filter. Nor should any opposing filter be
      // a prefix of keyPrefix.
      // For example if b0 is accepted b can not be rejected and
      // if b is accepted b0 can not be rejected. If these scenarios need to be
      // handled we need to add priorities.
      if (negative) {
        Preconditions.checkArgument(positivePrefixList.stream().noneMatch(
            prefix -> prefix.startsWith(keyPrefix) || keyPrefix
                .startsWith(prefix)),
            "KeyPrefix: " + keyPrefix + " already accepted.");
        this.negativePrefixList.add(keyPrefix);
      } else {
        Preconditions.checkArgument(negativePrefixList.stream().noneMatch(
            prefix -> prefix.startsWith(keyPrefix) || keyPrefix
                .startsWith(prefix)),
            "KeyPrefix: " + keyPrefix + " already rejected.");
        this.positivePrefixList.add(keyPrefix);
      }
      return this;
    }

    @Override
    public boolean filterKey(byte[] currentKey) {
      keysScanned++;
      if (currentKey == null) {
        return false;
      }
      boolean accept;

      // There are no filters present
      if (positivePrefixList.isEmpty() && negativePrefixList.isEmpty()) {
        return true;
      }

      accept = !positivePrefixList.isEmpty() && positivePrefixList.stream()
          .anyMatch(prefix -> {
            byte[] prefixBytes = StringUtils.string2Bytes(prefix);
            return prefixMatch(prefixBytes, currentKey);
          });
      if (accept) {
        keysHinted++;
        return true;
      }

      accept = !negativePrefixList.isEmpty() && negativePrefixList.stream()
          .allMatch(prefix -> {
            byte[] prefixBytes = StringUtils.string2Bytes(prefix);
            return !prefixMatch(prefixBytes, currentKey);
          });
      if (accept) {
        keysHinted++;
        return true;
      }

      return false;
    }

    @Override
    public int getKeysScannedNum() {
      return keysScanned;
    }

    @Override
    public int getKeysHintedNum() {
      return keysHinted;
    }

    private static boolean prefixMatch(byte[] prefix, byte[] key) {
      Preconditions.checkNotNull(prefix);
      Preconditions.checkNotNull(key);
      if (key.length < prefix.length) {
        return false;
      }
      for (int i = 0; i < prefix.length; i++) {
        if (key[i] != prefix[i]) {
          return false;
        }
      }
      return true;
    }

    public static KeyPrefixFilter newFilter(String prefix) {
      return newFilter(prefix, false);
    }

    public static KeyPrefixFilter newFilter(String prefix, boolean negative) {
      return new KeyPrefixFilter().addFilter(prefix, negative);
    }
  }
}
