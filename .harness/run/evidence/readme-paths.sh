#!/bin/bash
# Checks that every file path named in README.md's editing sections exists in the repository.
# A path is a backticked token ending in a source/asset extension; it may be relative to the
# repo root, to app/src/main/java/com/phongkaster/kanso/, or just a file name found anywhere.
cd "$(dirname "$0")/../../.." || exit 2
base=app/src/main/java/com/phongkaster/kanso
missing=0
for p in $(sed -n '24,160p' README.md | grep -oE '`[A-Za-z0-9_./-]+\.(kt|json|md|tflite|task|png|xml|kts|ps1|sh)`' | tr -d '`' | sort -u); do
  if [ -e "$p" ] || [ -e "$base/$p" ] || [ -n "$(find . -path ./.git -prune -o -path ./reference -prune -o -name "$(basename "$p")" -print -quit)" ]; then :; else
    echo "MISSING: $p"; missing=1
  fi
done
[ $missing = 0 ] && echo "readme-paths: all paths exist"
exit $missing
