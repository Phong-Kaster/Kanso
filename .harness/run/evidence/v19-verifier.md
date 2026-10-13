# The Verifier, Iteration 19 (2026-10-13), on the tree of `45d4023`

Fresh runs, each its own Gradle command (times in `v19-times.txt`: 22:12:53 to 22:21:33):

| Command | Log | Result |
|---|---|---|
| `:app:cleanTestDebugUnitTest :app:testDebugUnitTest` | `test.log` | exit 0, `BUILD SUCCESSFUL`; 571 tests, failures 0, errors 0, skipped 0 (`v19-c35.txt`); 571 `@Test` lines |
| `:app:verifyRoborazziDebug` | `verify-roborazzi.log` | exit 0; `v19-roborazzi-summary.json` total 41, changed 0, added 0 |
| `:app:lintAnalyzeDebug --rerun :app:lintReportDebug --rerun :app:lintDebug` | `lint.log` | exit 0; 0 issues of severity Error |
| `:app:assembleDebug` | `assemble.log` | exit 0 |
| `:app:compileDebugKotlin --rerun-tasks` | `compile.log` | exit 0; no deprecation line names `app/src/main` |
| `:app:dependencies --configuration debugRuntimeClasspath` | `head-deps.txt` | normalised list equals the base's |
| `:app:dependencyInsight ... material3` | `v19-m3-insight.txt` | 1.5.0-beta01 |
| `:core:colorpicker:assembleDebug :core:colorpicker:assembleRelease` | `v19-colorpicker.log` | exit 0 |

Greps and diffs: `v19-greps.sh` -> `v19-greps.txt` (the same output as Iteration 17's).

## Per criterion

- **1-5, 7, 9-15, 17, 18, 21 (tests), 24-26, 32, 34 (`LaunchTest`)**: the whole suite ran green, every class the DoD names included.
- **6**: `HomeReachTest` green; `home-reach.md` stands (no app change since it was copied).
- **8**: `EditsLoadingTest` green; the indicator grep prints nothing.
- **16, 19**: 15 button calls in `app/src/main`, each passing `shapes =`; `MaterialExpressiveTheme(` at `KansoTheme.kt:138`.
- **20 (assumed, A-025)**: (a) `app/src/main` greps print nothing; the catalog's `compose-shimmer` and `lottie` lines are the base's. (b)-(e) green.
- **21**: badging equals the base; no INTERNET or ACCESS_NETWORK_STATE.
- **22**: `NavBarColourTest`, `KansoBlueContrastTest` green. Pictures 28, 29, 34, 35 (re-recorded by T-017) now show the bar: did not show a defect in the bar, its pill or the layout. The gap Iteration 17 found is closed.
- **23**: fixed-colour grep finds the 8 lines of `fixed-colours.md`; `colors.xml` unchanged; no bitmap added.
- **27, 28**: `ToolBackdropTest`, `HomeScreenTest` green.
- **29 (assumed, A-027)**: 41 total, 0 changed; 24 M, 17 A, no D; `after/` (41) equals HEAD, `before/` (24) equals the base byte for byte.
- **30**: not driven: `-Op focus` named `NotificationShade` at 22:22 (someone using the phone).
- **33 (assumed, A-026)**: tests green; the settings-names grep differs from the base only in `AppModule.kt` 83 -> 99.
- **34**: `native-code: 'arm64-v8a' 'armeabi-v7a' 'x86_64'`, `minSdkVersion:'27'`, `targetSdkVersion:'36'`, `compileSdkVersion='37'`.
- **35**: the assume/ignore grep exits 1; 45 base names gone (`v19-gone.txt`, both lists sorted with `LC_ALL=C`), each named in `test-edits.md`; all 32 M/D test files listed there.
- **36**: guard files unchanged against the base; `ScreenStateTest` adds only `myedits/MyEditsViewModel`; `requiredPreviewNames` adds exactly the nine; `MaterialTheme(` and Material 2 greps print nothing.
- **37**: every comment grep finds its words.
- **38**: verify, lint, compile as above; no `@Suppress("DEPRECATION")` added.
- **39**: `core` diff names only `HSVColorPicker.kt`, one hunk at base lines 66-70; colorpicker builds.
- **40**: build files unchanged; dependencies equal; `reference/` unchanged and not referenced; five drawables are vectors, used and in `SOURCES.md`.
- **41**: ADR-002 exists; ADR-001 carries the note.
- **R1-R9**: every grep prints nothing; `ic_add_photo_alternate.xml` absent; `orphans.sh` prints nothing; unused resources are the base's two.

Verdict: every `machine` criterion holds. `DONE` is out of reach: criteria 20, 29 and 33 rest on Tier-3 assumptions (A-025..A-027), and criteria 20, 27-31 and 37 await a person. Report: `DONE_PARTIAL`.
