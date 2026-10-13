#!/bin/bash
# Waits until the Verifier's Gradle chain (Iteration 15) has written the given step's exit line.
# Usage: bash .harness/run/evidence/v15-wait.sh <step> ; step is test, verify, lint, assemble, compile or ALLDONE.
cd /d/Valve/Personal/Kanso || exit 2
until grep -q -E "^$1(=|$)" .harness/run/evidence/v15-times.txt; do sleep 5; done
cat .harness/run/evidence/v15-times.txt
