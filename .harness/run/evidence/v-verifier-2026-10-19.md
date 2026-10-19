# Run 9 Verifier, 2026-10-19 (tree 3d67d3e)

Fresh run: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug verifyRoborazziDebug --continue` -> BUILD SUCCESSFUL in 7m 45s, EXIT=0 (`verifier-final.log`). 812 unit tests, 0 failed, 0 errors, 0 skipped; every test class the DoD names has a result file.

| Criterion | Result |
|---|---|
| 1-19, 24, 26-28 | named test classes pass in the fresh run |
| 25 | `grep "one change"` prints nothing |
| 29 | merged debug manifest: no INTERNET / ACCESS_NETWORK_STATE; `git diff eccbf95 -- reference/` empty; catalog and app build script unchanged; no lottie/shimmer in app/src/main |
| 30 | lint exit 0; APK holds lib/arm64-v8a, armeabi-v7a, x86_64 |
| 31 | `test-edits.md` present; no assumeTrue/@Ignore/Assume. |
| 32 | four docs changed since eccbf95; Editing features + How each editing feature works present; every backticked path resolves (test paths under app/src/test, lookup png inside libs/imageprocessor.aar); forbidden-phrase grep empty |
| 33 | ADR-003 present and names ADR-002/Done/Result/strip; CONTEXT.md defines the five terms |
| 22 (machine part) | verifyRoborazziDebug passes; `git diff --name-status eccbf95` of screenshots: 19 M, all on the D-001 Baselines list, 14 A (55-68); old pictures kept in `baseline-diffs/` |
| R1-R5 | greps print nothing |
| **R6** | **gap**: `grep -rn "saved_" res/values` prints 10 ids; `-w Saved` finds "Saved screen" prose in 4 main and ~14 test files, plus SaveResult.Saved / SaveState.Saved / the "Saved" label (A-009, Tier 3). Filed T-010. |
| **34 (Verifier read)** | **gap**: fresh-context read found no false step/size/format/folder/failure claim, but edit.md:101-102, the untouched-photo Save sentence (README:143-145, edit.md:224-225, 255-256), edit.md:45, edit.md:17/37 say more or less than the code; KDoc ToolScreen.kt:310-311, EditPanel.kt:125-126, EditCropper.kt:72 stale. Filed T-011. |
| 20, 21, 22, 23, 34 (person) | not driven this Iteration: gaps found first, so the next Verifier drives them (§11 step 3) |
