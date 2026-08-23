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

# Enumerate every production .java file with fqcn, path, loc_total, loc_code (~).
# Drop test sources, generated sources, target/, mini-cluster, test-utils.
set -euo pipefail

OUT=study/atlas/tmp/raw_classes.tsv
: > "$OUT"

find hadoop-hdds hadoop-ozone -name '*.java' \
  -not -path '*/src/test/*' \
  -not -path '*/target/*' \
  -not -path '*/generated-sources/*' \
  -not -path 'hadoop-ozone/mini-cluster/*' \
  -not -path 'hadoop-hdds/test-utils/*' \
  -not -name 'package-info.java' \
  | while read -r f; do
      # Extract package + primary type name; skip if we cannot infer fqcn.
      pkg=$(awk '/^package /{sub(";",""); print $2; exit}' "$f" || true)
      cls=$(basename "$f" .java)
      loc_total=$(wc -l < "$f" | tr -d ' ')
      # Rough code line count: strip blank, // and /* */ block comments (line-oriented heuristic), imports, package.
      loc_code=$(awk '
        BEGIN{in_block=0; count=0}
        {
          line=$0
          if (in_block) { if (line ~ /\*\//) in_block=0; next }
          if (line ~ /^[[:space:]]*\/\*/) { if (line !~ /\*\//) in_block=1; next }
          if (line ~ /^[[:space:]]*\*/) next
          if (line ~ /^[[:space:]]*\/\//) next
          if (line ~ /^[[:space:]]*$/) next
          if (line ~ /^[[:space:]]*import /) next
          if (line ~ /^[[:space:]]*package /) next
          count++
        }
        END{print count}
      ' "$f")
      # Round to nearest 25, suffix ~
      rounded=$(( (loc_code + 12) / 25 * 25 ))
      [ "$rounded" -lt 25 ] && rounded=25
      if [ -n "$pkg" ]; then
        printf '%s.%s\t%s\t%d\t%d~\n' "$pkg" "$cls" "$f" "$loc_total" "$rounded" >> "$OUT"
      fi
    done

echo "rows: $(wc -l < "$OUT")"
