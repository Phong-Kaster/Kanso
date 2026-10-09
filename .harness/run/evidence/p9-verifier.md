# Verifier, Iteration 9 (2026-10-10), on `a118896`

Capable, Autonomous mode. Wrote none of the implementation. Every result below is from a run made in this
Iteration; files from earlier Iterations were re-run, not trusted.

## Fresh runs

| Run | Command | Result | Log |
|---|---|---|---|
| clean, then the whole set | `:app:clean`, then `cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` | exit 0, 3 min 30 s; 368 tests, 0 failed, 0 errors, 0 skipped; 24 pictures, 0 changed; lint 0 errors, 54 warnings | `p9-clean.log`, `p9-full.log` |
| DoD 1 | `:app:testDebugUnitTest --tests '*KansoBlueThemeTest*'` | exit 0, `BUILD SUCCESSFUL`; 13 tests (SDK 27, 30, 31, 35, light and night) | `c1.log` |
| DoD 25 | `:app:cleanTestDebugUnitTest :app:testDebugUnitTest` | exit 0, 2 min 41 s; XML totals `tests 368`, `failures 0`, `errors 0`, `skipped 0`; `@Test` at HEAD: 368 | `test.log` |
| DoD 20 (d), 26 | `:app:verifyRoborazziDebug` | exit 0; `results-summary.json`: total 24, changed 0, unchanged 24 | `verify-roborazzi.log` |
| DoD 26 | `:app:lintAnalyzeDebug --rerun :app:lintReportDebug --rerun :app:lintDebug` | exit 0; `:app:lintAnalyzeDebug` executed (not up to date); 0 errors, 54 warnings | `lint.log` |
| DoD 26 | `:app:compileDebugKotlin --rerun-tasks` | exit 0, 74 tasks executed; no `deprecat` line for `app/src/main` (2 lines, both outside the app) | `compile.log` |
| DoD 24 | `:app:assembleDebug` | exit 0, `BUILD SUCCESSFUL` | `assemble.log` |
| DoD 27 | `:app:dependencies --configuration debugRuntimeClasspath` | exit 0; the normalised module set (633 lines) equals the base's | `head-deps.txt` |
| DoD 27 | `:app:dependencyInsight --dependency androidx.compose.material3:material3` | selects `1.5.0-beta01` | `p9-dep-insight.log` |

Every test class a Proof names passed, with its count: `p9-per-class.txt`. The searches and git diffs:
`p9-greps.txt`, `p9-greps-2.txt`, `p9-greps-3.txt`. The APK checks: `p9-apk.txt`. The Gradle checks:
`p9-gradle-checks.txt`.

## The three clauses Phase 6 closed

A fresh-context, read-only audit (Capable) read the Phase 6 tests against their Proofs. All three hold.

- **DoD 29.** `git grep -n 'reference/' -- settings.gradle.kts app` prints nothing (exit 1).
  - `OneTypePerFileTest.ALLOWED` is still `emptySet()` (`:759`).
  - The reworded KDoc (`:393-394`) says the same as before. Only that comment line changed.
- **DoD 2.** `ThemeSwatchTest.choose` taps wallpaper, blue, wallpaper and then custom. Before each tap it checks that the app shows a different `primary`, so no tap lands on a scheme that is already applied.
  - Each tap is compared with that choice's own scheme in the seven roles of DoD 1 (`rolesOf`, `:447-455`).
  - The 10 s is one wall-clock budget, taken just before the click and checked again after the waits (`:278-294`).
  - Custom on a fresh install expects Kanso blue's scheme. The green test taps Custom from wallpaper and requires the green scheme (`:207-211`).
- **DoD 4.** The layout test uses `ToolFixtures.gradient(600, 400)` at `w411dp-h891dp-xxhdpi` (`ToolScreenTest.kt:113`, `:318`).
  - "The photo" is the drawn `Image`. "Its area" is the separate `PhotoArea` box, so the fill check can fail.
  - The layout is in whole pixels, so the +0.5 px slack on the stacking checks is exact in practice.

## Every other Proof, re-run

- **1, 3, 5, 7, 12, 15, 17, 18, 19, 22, 23:** every named test passed in the fresh runs (`p9-per-class.txt`).
  - A second fresh-context audit (Capable) compared 1, 6, 7, 8, 12, 16, 17 and 23 clause by clause with their tests.
  - It found 1, 7, 12, 17 and 23 hold. 6, 8 and 16 are weighed below.
- **6: holds.** The audit asked whether the centre clause is loose. The test accepts the centre pixel within 8 per channel of the thumbnail colour (`GalleryGridTest.kt:380-383`, `COLOUR_TOLERANCE = 8` at `:554`).
  - The Proof itself makes "more than 8 per channel apart" the line between the thumbnail colour and not the thumbnail colour.
  - So "within 8" is "the thumbnail colour" by the Proof's own measure. Not a gap.
  - The corner check is stricter than written: every channel is more than 8 from the thumbnail, and the corner is within 8 of the gap's background.
- **8: holds.** The Proof calls the node it means "the inner shutter node". The test draws the bounds of `camera-shutter-inner`, inside the `camera-shutter` button.
  - A difference inside those bounds is a difference in the button.
  - For "drawn as before" after the release: the 56 dp square's corners lie outside the 72 dp resting circle. So a button stuck in its pressed shape would show there.
  - The release check is exact equality, stricter than "or in its taking state".
- **16: holds.** The test compares the background pixel with the scheme's `background`, and the Proof names `surface` or the top-bar container. For Kanso blue the two are one colour:
  - The committed pictures read `#F9F9FF` at the top bar (`surface`) and on the page (`background`) in light, and `#111318` for both in dark (`4_settings_light.png`, `5_settings_dark.png`, `10_gallery_light.png`, `11_gallery_dark.png`).
  - These are the window backgrounds DoD 1 (d) proves equal to `background`.
  - Home has no top bar, as recorded earlier.
- **9, 10, 11, 13 (machine part), 14 (Proof), 16 (grep), 26 (greps), R1 to R6:** the searches print what the Proofs ask for (`p9-greps.txt`, `p9-greps-2.txt`).
  - 11: a fresh search finds the same 21 button calls as `expressive-scan.md` (18 in screens, 3 in previews), at the same lines, and each passes `shapes =`. `ButtonGroup(`, `HorizontalFloatingToolbar(`, `Morph(`, `OverflowIndicator` and `RadioButton(` are at the lines the scan names.
  - 16: the same 8 fixed-colour lines as `fixed-colours.md`. `SampleLivePicture` and `sampleShotBitmap` are still declared at `CameraScreen.kt:858` and `:884`.
  - 26: the two `@Suppress("DEPRECATION")` in `app/src/main` were there at `76522d7`, and none was added.
- **13:** `NavTransitionTest` and `ToolSelectionAnimationTest` print "not driven by machine": this host carries no animation scale into Compose. Not a failure. No View-system animator in `app/src/main`.
- **14:** `KansoNavHost.kt:241-243` passes the three specs, built in `KansoTransitions.kt` from `motionScheme`.
  - `enableOnBackInvokedCallback=true` is on `<application>` in the APK's manifest (`aapt2 dump xmltree`).
  - Still **assumed** (A-015): the predictive slide is a straight line, not a spring.
- **15:** the badging of the HEAD APK equals `base-badging.txt` exactly, with no `INTERNET` or `ACCESS_NETWORK_STATE`.
- **20 (machine part):**
  - `before/` (16) and `after/` (24) were rewritten from git, and `git status` shows them unchanged, so they equal the committed files byte for byte.
  - `git diff --name-status 76522d7 HEAD -- app/src/test/screenshots` shows `M` for the 16 and `A` for the 8, with no `D`.
  - Total 24, changed 0.
  - The Verifier opened all 24 `after/` pictures. What it saw is written per group in `.harness/ISSUES.md` ("did not show a defect in …").
- **24:**
  - The APK shows `native-code: 'arm64-v8a' 'armeabi-v7a' 'x86_64'`, `minSdkVersion:'27'` (A-016), `targetSdkVersion:'36'` and `compileSdkVersion='37'`.
  - `app/build.gradle.kts` is unchanged since the base.
  - `LaunchTest` 3 of 3.
- **25:**
  - The test-name script of `p7-verifier.md` rebuilds `base-testnames.txt` (238) and `head-testnames.txt` (368, no duplicates) exactly.
  - The 9 names `comm -23` prints and all 29 `M` test files are in `test-edits.md`. `ThemeSwatchTest` is new, so it needs no row.
  - The guards are unchanged since the base: `ScreenStateTest`, `PreviewHygieneTest`, `PreviewCoverageTest` and `KdocCoverageTest`. `EXEMPT` has the same three entries.
  - All 19 base `requiredPreviewNames` are still there (23 now). No `@Ignore` or `assume`.
- **27:** nothing changed outside the app (`git diff --exit-code` exit 0).
  - No dependency line added.
  - No Material 2 import, no opt-in in the build script, no `@file:OptIn` in `app/src/main`.
- **28:** the check script, run from a copy without its last `rm -rf` line, fetched all 21 upstream files again: all `OK`, with no `android:tint` (`p9-symbols-check.txt`).
  - Each of the ten has a `<vector` root and at least one reference in `app/src/main/java`.
  - `ic_arrow_back` is auto-mirrored, and `aapt2 dump resources` lists all ten.
  - Every one of the 21 added drawables is in `SOURCES.md`. `ic_launcher_foreground` was there before the run.
- **29:** every part holds.
  - `reference/` is unchanged since `a0d9a79`.
  - `lottie` and `airbnb` are only in the catalog, as at the base.
  - No `lightroom` or `adobe`, and the `reference/` search prints nothing.
- **30 (machine part):** every word the Proof names is in a comment of its file (`p9-c30-precheck.txt`).
- **21, device pre-check: not driven.**
  - The only device is the owner's Galaxy A13 (`R58W1120QVA`).
  - The first check (05:57) was refused, because `:app:clean` had removed the debug APK and the wrapper drives only a built one.
  - At 05:59, 06:02, 06:06 and 06:08, after the build, another app had focus (a person using the phone). So nothing was installed, started or read. The other app is not named here.

## Result

No gap. Every `machine` criterion holds on fresh evidence. The run cannot be `DONE`:

- criteria 13, 20, 21 and 30 await a person (the "Awaiting a person, Run 6" items in `.harness/ISSUES.md`);
- criterion 14 is assumed (A-015, Tier 3).

Autonomous mode: `DONE_PARTIAL`, with no Cleanup Commit.
