#!/bin/bash
# Waits until the Verifier's Gradle chain (Iteration 17) has written the given step's exit line.
# Usage: bash .harness/run/evidence/v17-wait.sh <step> ; step is test, verify, lint, assemble, compile, deps, insight, colorpicker or ALLDONE.
cd /d/Valve/Personal/Kanso || exit 2
until grep -q -E "^$1(=| )" .harness/run/evidence/v17-times.txt 2>/dev/null; do sleep 5; done
cat .harness/run/evidence/v17-times.txt
