#!/bin/bash
# Fetches the Material Symbols Rounded drawables (FILL 0, weight 400, optical size 24) from
# github.com/google/material-design-icons (Apache 2.0), removes only android:tint, stores each as
# app/src/main/res/drawable/ic_<name>.xml and writes SOURCES.md. Run from the repository root.
set -eu
BASE="https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android"
DIR="app/src/main/res/drawable"
OUT=".harness/run/evidence/symbols/SOURCES.md"
TEN="edit background_replace auto_fix_high face_retouching_natural brush photo_camera settings save share arrow_back"
EXTRA="home check refresh add_photo_alternate flash_on flash_off flash_auto cameraswitch dark_mode palette contrast"
TMP="$(mktemp -d)"
{
  echo "# Material Symbols provenance (DoD 28)"
  echo
  echo "Fetched $(date -u +%Y-%m-%d) with .harness/run/evidence/symbols/fetch.sh from github.com/google/material-design-icons (Apache 2.0). Style: Rounded, FILL 0, weight 400, optical size 24. The only change to a stored file is the removal of the android:tint attribute (the app's window theme does not define ?attr/colorControlNormal); check.sh proves it."
  echo
  echo "| Stored file | Upstream URL | SHA-256 upstream | SHA-256 stored |"
  echo "|---|---|---|---|"
} > "$OUT"
for group in TEN EXTRA; do
  if [ "$group" = TEN ]; then list="$TEN"; else list="$EXTRA"; echo "| _extra drawables the screens need (A-008)_ | | | |" >> "$OUT"; fi
  for n in $list; do
    url="$BASE/$n/materialsymbolsrounded/${n}_24px.xml"
    curl -s -f -o "$TMP/$n.xml" "$url"
    sed -z -E 's/\n[[:space:]]*android:tint="[^"]*"//' "$TMP/$n.xml" > "$DIR/ic_$n.xml"
    echo "| ic_$n.xml | $url | $(sha256sum "$TMP/$n.xml" | cut -d' ' -f1) | $(sha256sum "$DIR/ic_$n.xml" | cut -d' ' -f1) |" >> "$OUT"
  done
done
rm -rf "$TMP"
