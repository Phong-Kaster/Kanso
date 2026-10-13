#!/bin/bash
# The Verifier's Gradle chain (Iteration 17): each Proof's own command, one after another.
# Each step appends "<step>=<exit> <time>" to v17-times.txt; "ALLDONE" ends it.
cd /d/Valve/Personal/Kanso || exit 2
E=.harness/run/evidence
T=$E/v17-times.txt
: > $T
echo "start $(date +%T)" >> $T
set -o pipefail
timeout 2400 ./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest > $E/test.log 2>&1; echo "test=$? $(date +%T)" >> $T
timeout 1200 ./gradlew :app:verifyRoborazziDebug > $E/verify-roborazzi.log 2>&1; echo "verify=$? $(date +%T)" >> $T
cp app/build/test-results/roborazzi/debug/results-summary.json $E/v17-roborazzi-summary.json 2>/dev/null
timeout 1200 ./gradlew :app:lintAnalyzeDebug --rerun :app:lintReportDebug --rerun :app:lintDebug > $E/lint.log 2>&1; echo "lint=$? $(date +%T)" >> $T
timeout 1200 ./gradlew :app:assembleDebug > $E/assemble.log 2>&1; echo "assemble=$? $(date +%T)" >> $T
timeout 1200 ./gradlew :app:compileDebugKotlin --rerun-tasks > $E/compile.log 2>&1; echo "compile=$? $(date +%T)" >> $T
timeout 600 ./gradlew :app:dependencies --configuration debugRuntimeClasspath > $E/head-deps.txt 2>&1; echo "deps=$? $(date +%T)" >> $T
timeout 600 ./gradlew :app:dependencyInsight --dependency androidx.compose.material3:material3 --configuration debugRuntimeClasspath > $E/v17-m3-insight.txt 2>&1; echo "insight=$? $(date +%T)" >> $T
timeout 1200 ./gradlew :core:colorpicker:assembleDebug :core:colorpicker:assembleRelease > $E/v17-colorpicker.log 2>&1; echo "colorpicker=$? $(date +%T)" >> $T
echo "ALLDONE $(date +%T)" >> $T
