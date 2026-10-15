#!/usr/bin/env bash
# Checks that every repository path README.md names in its editing-feature sections exists.
#
# Scanned regions, found by their headings (never by line number, so the check follows the
# README as it grows):
#   1. from "### Editing features" to the next heading of any level (the editing features table);
#   2. from "## How each editing feature works" to the next level-2 ("## ") heading: the whole
#      section, with every "###" subsection in it.
#
# A backticked token counts as a path when it has no space, "(", "=" or ":" (so no code, no
# Gradle project names and no "content://" Uris) and it contains "/" or ends in a file
# extension. Locations on the phone and MIME types (Pictures/Kanso, image/png, ...) are not
# repository paths and are skipped (is_runtime).
#
# A path resolves when it exists under one of:
#   - the repository root;
#   - app/src/main/java/com/phongkaster/kanso/ (the base the README states for its paths);
#   - app/src/main/ (assets/...);
#   - for a bare file name, anywhere under app/src/main, core or opencv (build folders skipped);
#   - as an asset inside one of libs/*.aar ("assets/<path>" in the zip's file list, which zip
#     stores uncompressed, so grep finds it).
# A "<...>" placeholder, "*" and "..." match any characters, across folders too.
#
# Exits 0 when every path resolves; 1 when one does not (each is listed), a heading is missing,
# or too few paths were found to trust the scan.
set -u
cd "$(dirname "$0")/../../.." || exit 2
README=README.md
[ -f "$README" ] || { echo "README.md not found"; exit 2; }

# Prints the two regions; exits 3 when either heading is missing.
regions() {
  awk '
    /^#+ / {
      if (in1) in1 = 0
      if (in2 && /^## /) in2 = 0
      if ($0 ~ /^### Editing features[[:space:]]*$/) { in1 = 1; found1 = 1; next }
      if ($0 ~ /^## How each editing feature works[[:space:]]*$/) { in2 = 1; found2 = 1; next }
    }
    in1 || in2 { print }
    END { if (!found1 || !found2) exit 3 }
  ' "$README"
}

is_candidate() {
  case "$1" in '' | *' '* | *'('* | *'='* | *':'*) return 1 ;; esac
  case "$1" in */*) return 0 ;; esac
  printf '%s' "$1" | grep -qE '\.(kt|kts|java|xml|gradle|properties|webp|png|jpg|aar|task|tflite|md|sh|json)$'
}

is_runtime() {
  case "$1" in image/* | Pictures/* | /storage/* | filesDir/* | Kanso_* | .png | .jpg) return 0 ;; esac
  return 1
}

# Turns placeholders into "*" and drops a trailing "/".
to_glob() {
  printf '%s' "$1" | sed -e 's/<[^>]*>/*/g' -e 's/\.\.\./*/g' -e 's/…/*/g' -e 's#/$##'
}

exists_under() {
  local full="$1$2" base
  case "$full" in
    *'*'*)
      base="${full%%\**}"
      case "$base" in */*) base="${base%/*}" ;; *) return 1 ;; esac
      [ -d "$base" ] || return 1
      find "$base" -path "$full" -print -quit 2>/dev/null | grep -q .
      ;;
    *) [ -e "$full" ] ;;
  esac
}

in_aar() {
  local p="${1#assets/}" re aar
  re=$(printf '%s' "$p" | sed -e 's/[][\.^$+?(){}|]/\\&/g' -e 's/\*/[A-Za-z0-9_.\/-]*/g')
  for aar in libs/*.aar; do
    grep -a -q -E "assets/$re" "$aar" && return 0
  done
  return 1
}

resolves() {
  local pat="$1" root
  for root in "" "app/src/main/java/com/phongkaster/kanso/" "app/src/main/"; do
    exists_under "$root" "$pat" && return 0
  done
  case "$pat" in
    */*) ;;
    *)
      find app/src/main core opencv -name build -prune -o -name "$pat" -print -quit 2>/dev/null \
        | grep -q . && return 0
      ;;
  esac
  in_aar "$pat"
}

text=$(regions) || { echo "README.md lacks '### Editing features' or '## How each editing feature works'"; exit 1; }

missing=0
checked=0
while IFS= read -r tok; do
  is_candidate "$tok" || continue
  is_runtime "$tok" && continue
  checked=$((checked + 1))
  if ! resolves "$(to_glob "$tok")"; then
    echo "MISSING: $tok"
    missing=$((missing + 1))
  fi
done < <(printf '%s\n' "$text" | grep -o '`[^`]*`' | tr -d '`' | sort -u)

echo "readme-paths: checked $checked paths, $missing missing"
if [ "$checked" -lt 20 ]; then
  echo "Too few paths found ($checked): the region scan is probably broken."
  exit 1
fi
[ "$missing" -eq 0 ]
