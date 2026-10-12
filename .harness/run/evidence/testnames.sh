#!/bin/bash
# Usage: testnames.sh <rev>  -> File.kt:testName, sorted, one per @Test
git grep -I -E '@Test|^\s*(private |internal )?fun ' "$1" -- app/src/test/java | tr -d '\r' | awk '
{ i = index($0, ":"); rest = substr($0, i+1); j = index(rest, ":"); path = substr(rest, 1, j-1); line = substr(rest, j+1);
  n = split(path, parts, "/"); file = parts[n]; }
line ~ /^[[:space:]]*@Test/ { pend[file] = 1; next }
pend[file] && line ~ /fun / { s = line; sub(/.*fun +/, "", s); sub(/\(.*/, "", s); gsub(/`/, "", s); print file ":" s; pend[file] = 0 }
' | sort
