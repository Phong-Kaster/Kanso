#!/bin/bash
# Run 7: fetch the Material Symbols Rounded drawables this run adds into app/src/main/res/drawable/ as
# ic_<name>.xml. The only change made to an upstream file is removing its android:tint attribute (the app's
# window theme does not define ?attr/colorControlNormal; see PROJECT.md). Prints the SHA-256 of the upstream
# and of the stored file for SOURCES.md. Run from the repository root:
#   bash .harness/run/evidence/symbols/fetch.sh
set -u
BASE="https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android"
DIR="app/src/main/res/drawable"
NAMES="photo_library arrow_forward close swap_horiz hide_image"
TMP="$(mktemp -d)"
for n in $NAMES; do
  curl -s -f -o "$TMP/$n.xml" "$BASE/$n/materialsymbolsrounded/${n}_24px.xml" || { echo "FETCH FAILED $n"; continue; }
  sed -z -E 's/\n[[:space:]]*android:tint="[^"]*"//' "$TMP/$n.xml" > "$DIR/ic_$n.xml"
  echo "| ic_$n.xml | $BASE/$n/materialsymbolsrounded/${n}_24px.xml | $(sha256sum "$TMP/$n.xml" | cut -d' ' -f1) | $(sha256sum "$DIR/ic_$n.xml" | cut -d' ' -f1) |"
done
rm -rf "$TMP"
