#!/bin/bash
# R8: for every non-*Test.kt file under app/src/test/java, print it when none of its top-level
# declarations is used by another test file. Prints nothing when there are none.
# Usage (from the repository root): bash .harness/run/evidence/orphans.sh
root=app/src/test/java
find "$root" -name '*.kt' ! -name '*Test.kt' | while read -r f; do
  names=$(grep -h -o -E '^(internal |private |public )?(data |sealed |enum |abstract |open |object |class |interface |fun |val |const val )+(<[^>]*> )?([A-Za-z_.]+\.)?[A-Za-z_][A-Za-z0-9_]*' "$f" | awk '{print $NF}' | sed 's/.*\.//' | sort -u)
  used=0
  for n in $names; do
    if grep -r -l -w --include='*.kt' "$n" "$root" | grep -v -x "$f" | grep -q .; then used=1; break; fi
  done
  [ "$used" = 0 ] && echo "$f"
done
exit 0
