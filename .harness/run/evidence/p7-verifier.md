# Verifier, Iteration 7 (2026-10-10), on `1e82f8d`

Capable, Autonomous mode. Wrote none of the implementation. Every result below is from a run made in this
Iteration; files from earlier Iterations were re-run, not trusted.

## Fresh runs

| Run | Command | Result | Log |
|---|---|---|---|
| clean, then the whole set | `:app:clean`, then `testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` | exit 0, 3 min 33 s; 368 tests, 0 failed, 0 errors, 0 skipped; 24 pictures, 0 changed | `p7-clean.log`, `p7-verify-full.log` |
| DoD 26 compile | `:app:compileDebugKotlin --rerun-tasks` | exit 0, 74 tasks executed; no `deprecat` line for `app/src/main` (3 warnings, all outside the app: a compiler flag and two in `core/common`) | `compile.log` |
| DoD 24 | `:app:assembleDebug` | exit 0, `BUILD SUCCESSFUL` | `assemble.log` |
| DoD 25 | `:app:cleanTestDebugUnitTest :app:testDebugUnitTest` | exit 0; XML totals `tests 368`, `failures 0`, `errors 0`, `skipped 0`; `@Test` at HEAD: 368 | `test.log` |
| DoD 20 (d), 26 | `:app:verifyRoborazziDebug` | exit 0; `results-summary.json`: total 24, changed 0, unchanged 24 | `verify-roborazzi.log` |
| DoD 26 | `:app:lintAnalyzeDebug --rerun :app:lintReportDebug --rerun :app:lintDebug` | exit 0; `:app:lintAnalyzeDebug` executed (not up to date); 0 errors, 54 warnings, 2 hints | `lint.log` |
| DoD 27 | `:app:dependencies --configuration debugRuntimeClasspath` | exit 0; the normalised module set (633 lines) equals the base's | `head-deps.txt` |
| DoD 27 | `:app:dependencyInsight --dependency androidx.compose.material3:material3` | selects `1.5.0-beta01` | `p7-dep-insight.log` |
| DoD 1 | `:app:testDebugUnitTest --tests '*KansoBlueThemeTest*'` | exit 0, `BUILD SUCCESSFUL`; 13 tests at SDK 27, 30, 31 and 35, light and night | `c1.log` |

Every test class a Proof names passed, with its count: `p7-per-class.txt`. The searches and git diffs: `p7-greps.txt`.

## Per criterion

- **1, 2, 3, 4, 5, 6, 7, 8, 12, 15, 16, 17, 18, 19, 22, 23**: every named test passed in the fresh runs. A fresh-context, read-only audit compared each test with its Proof. It found no test that cannot fail, and two places where the test checks less than the Proof promises:
  - **2 (gap, T-020):** a tap is proved by `primary` only, not by "that choice's scheme".
  - **4 (gap, T-021):** the fill check runs on a 400x200 JPEG, not on the 600x400 gradient the Proof names.
  - Checked by reading the code instead, and holding:
    - **22:** the stored names `theme_mode`, `theme_colour_source`, `theme_colour_seed` and the file `datastore/settings.preferences_pb` are the same at `76522d7` and HEAD (`SettingsRepository.kt:147-153`, `AppModule.kt:83`). `setThemeColour(Wallpaper)` stores the raw string `"Wallpaper"` (`SOURCE_WALLPAPER`), so the SDK 30 test reads that raw value.
    - **3, 5 and 7:** the exact back stacks follow from `KansoNavHost.kt:93-178`. Home pushes only from `Home`, Keep replaces `Camera` with `ToolPhoto`, Save clears to `[Home]` and adds `Saved`, Edit another replaces `Saved` with `Gallery(tool)`, and Home pops `Saved`. The tests prove this from where Back leads.
  - Minor and recorded, not gaps:
    - **18:** `TouchTargetCheck` also counts `node.size` and skips nodes that are not placed.
    - **19:** the icon check accepts any node that has no description.
    - **5:** "inside the column" accepts any scrollable ancestor.
- **9, 10, 11, 13 (machine part), 14 (Proof), 16 (grep), 26 (greps), R1 to R6**: the searches print what the Proofs ask for (`p7-greps.txt`).
  - 11: `expressive-scan.md` re-checked against a fresh search. The same 20 calls, at the same lines, and every one passes `shapes =` or gives its reason.
  - 16: `fixed-colours.md` re-checked. The same 8 lines.
- **13:** `NavTransitionTest` and `ToolSelectionAnimationTest` print "not driven by machine": this host carries no animation scale into Compose. Not a failure.
- **14:** the Proof holds (both screens are composed 2 frames after a push or pop, A-014). `enableOnBackInvokedCallback=true` is on `<application>` in the APK's manifest. The criterion is still **assumed** (A-015): the preview slides on a straight line, not a spring.
- **15:** the badging of the HEAD APK equals `base-badging.txt` exactly, and has no `INTERNET` or `ACCESS_NETWORK_STATE`.
- **20 (machine part):**
  - `before/` has the 16 old pictures and `after/` the 24 new ones, all rewritten from git.
  - The `after/` files equal the committed ones byte for byte.
  - `git diff --name-status 76522d7 HEAD -- app/src/test/screenshots` shows `M` for the 16 and `A` for the 8, with no `D`.
  - Total 24, changed 0.
- **24:**
  - The APK has `native-code: 'arm64-v8a' 'armeabi-v7a' 'x86_64'`, `targetSdkVersion:'36'` and `compileSdkVersion='37'`.
  - The `abiFilters` line is unchanged.
  - `LaunchTest` passes at SDK 27, 30 and 35.
  - `aapt2` 36.0.0 names the minimum-version line `minSdkVersion:'27'`, not `sdkVersion:'27'` as the Proof writes it. The minimum version is 27, and no code can change the tool's wording (A-016).
- **25:**
  - The awk (`File.kt:name`, written in this Iteration) rebuilds `base-testnames.txt` at `76522d7` exactly (238 names).
  - At HEAD it gives 368 names with no duplicates (`head-testnames.txt`).
  - The 9 names `comm -23` prints, and all 28 `M` test files, are each in `test-edits.md`.
  - The guards are unchanged:
    - `OneTypePerFileTest.ALLOWED = emptySet()`.
    - `PreviewCoverageTest.EXEMPT` has the same three entries.
    - `ScreenStateTest` and `PreviewHygieneTest` are untouched since the base.
    - All 19 base `requiredPreviewNames` are still there (23 now).
  - No `@Ignore` or `assume`.
- **27:**
  - Nothing changed outside the app (`git diff --exit-code` exit 0).
  - `app/build.gradle.kts` is unchanged.
  - No Material 2 import, and no opt-in in the build script.
- **28:**
  - `check.sh` fetched all 21 upstream files again: all `OK`, and no `android:tint` (`p7-symbols-check.txt`).
  - Each of the ten has a `<vector` root and at least one reference in `app/src/main/java`.
  - `ic_arrow_back` is auto-mirrored.
  - `aapt2 dump resources` lists all ten.
- **29 (gap, T-019):** every part holds except one. `git grep -n 'reference/' -- settings.gradle.kts app` prints one line: a KDoc sentence in `OneTypePerFileTest.kt:394`, there since `76522d7`. It is a comment, not a build or an import, but the Proof says "prints nothing".
  - The other parts hold:
    - `reference/` is unchanged since `a0d9a79`.
    - `lottie` and `airbnb` appear only in the catalog, as they did at the base.
    - No `lightroom` or `adobe`.
- **30 (machine part):** every word the Proof names appears in a comment of its file (`p7-c30-precheck.txt`).
- **21, device pre-check: not driven.** The only device is the owner's Galaxy A13 (`R58W1120QVA`). At both checks this Iteration another app had focus (a person using the phone), so nothing was installed, started or read. The other app is not named here.

## Result

Three gaps, each closable by a test-only edit, became Phase 6: T-019, T-020, T-021. The PARTIAL-candidate flag is cleared. The next Verifier re-proves everything after Phase 6, drives the phone if it is free, and writes the unsigned "Awaiting a person, Run 6" items. This Verifier wrote none of them, so the next one can word them from what it actually drove.

## The test-name awk (criterion 25)

Run from the repository root with a revision (`76522d7` rebuilds `base-testnames.txt` exactly; `HEAD` makes `head-testnames.txt`):

```bash
#!/bin/bash
# Usage: testnames.sh <rev>  -> File.kt:testName, sorted, one per @Test
git grep -I -E '@Test|^\s*(private |internal )?fun ' "$1" -- app/src/test/java | tr -d '\r' | awk '
{ i = index($0, ":"); rest = substr($0, i+1); j = index(rest, ":"); path = substr(rest, 1, j-1); line = substr(rest, j+1);
  n = split(path, parts, "/"); file = parts[n]; }
line ~ /^[[:space:]]*@Test/ { pend[file] = 1; next }
pend[file] && line ~ /fun / { s = line; sub(/.*fun +/, "", s); sub(/\(.*/, "", s); gsub(/`/, "", s); print file ":" s; pend[file] = 0 }
' | sort
```
