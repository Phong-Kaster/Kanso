#!/bin/bash
# DoD 28: every stored Material Symbols drawable equals its upstream file except for the removed
# android:tint attribute. Fetches each upstream file again, strips the tint the same way, compares the
# bytes (so the pathData is equal too) and prints one line per file. Exit 1 if any differs.
# Run from the repository root:  bash .harness/run/evidence/symbols/check.sh
set -u
BASE="https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android"
DIR="app/src/main/res/drawable"
NAMES="edit background_replace auto_fix_high face_retouching_natural brush photo_camera settings save share arrow_back home check refresh add_photo_alternate flash_on flash_off flash_auto cameraswitch dark_mode palette contrast"
TMP="$(mktemp -d)"
bad=0
for n in $NAMES; do
  curl -s -f -o "$TMP/$n.xml" "$BASE/$n/materialsymbolsrounded/${n}_24px.xml" || { echo "FETCH FAILED $n"; bad=1; continue; }
  sed -z -E 's/\n[[:space:]]*android:tint="[^"]*"//' "$TMP/$n.xml" > "$TMP/$n.stripped.xml"
  stored="$DIR/ic_$n.xml"
  if [ ! -f "$stored" ]; then echo "MISSING $stored"; bad=1; continue; fi
  up_path=$(grep -o 'android:pathData="[^"]*"' "$TMP/$n.xml" | sort | sha256sum | cut -c1-16)
  st_path=$(grep -o 'android:pathData="[^"]*"' "$stored" | sort | sha256sum | cut -c1-16)
  if cmp -s "$TMP/$n.stripped.xml" "$stored" && [ "$up_path" = "$st_path" ]; then
    echo "OK   ic_$n.xml (pathData $st_path; no android:tint: $(grep -c 'android:tint' "$stored"))"
  else
    echo "DIFF ic_$n.xml"; bad=1
  fi
done
rm -rf "$TMP"
exit $bad
