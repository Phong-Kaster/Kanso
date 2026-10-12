#!/bin/bash
# DoD 40: every Material Symbols drawable this run adds equals its upstream file except for the removed
# android:tint attribute. Fetches each upstream file again, strips the tint the same way, compares the bytes
# (so the pathData is equal too), checks that the file is a <vector> root and that app/src/main/java uses it,
# and prints one line per file. Exit 1 if any check fails. Run from the repository root:
#   bash .harness/run/evidence/symbols/check.sh
# A drawable added later in the run is appended to NAMES (and to SOURCES.md).
set -u
BASE="https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android"
DIR="app/src/main/res/drawable"
NAMES="photo_library arrow_forward close swap_horiz hide_image"
TMP="$(mktemp -d)"
bad=0
for n in $NAMES; do
  curl -s -f -o "$TMP/$n.xml" "$BASE/$n/materialsymbolsrounded/${n}_24px.xml" || { echo "FETCH FAILED $n"; bad=1; continue; }
  sed -z -E 's/\n[[:space:]]*android:tint="[^"]*"//' "$TMP/$n.xml" > "$TMP/$n.stripped.xml"
  stored="$DIR/ic_$n.xml"
  if [ ! -f "$stored" ]; then echo "MISSING $stored"; bad=1; continue; fi
  up_path=$(grep -o 'android:pathData="[^"]*"' "$TMP/$n.xml" | sort | sha256sum | cut -c1-16)
  st_path=$(grep -o 'android:pathData="[^"]*"' "$stored" | sort | sha256sum | cut -c1-16)
  root=$(head -c 7 "$stored")
  used=$(git grep -c -w "R.drawable.ic_$n" -- app/src/main/java | wc -l)
  if cmp -s "$TMP/$n.stripped.xml" "$stored" && [ "$up_path" = "$st_path" ] && [ "$root" = "<vector" ]; then
    echo "OK   ic_$n.xml (pathData $st_path; android:tint count: $(grep -c 'android:tint' "$stored"); files in app/src/main/java using it: $used)"
  else
    echo "DIFF ic_$n.xml"; bad=1
  fi
done
rm -rf "$TMP"
exit $bad
