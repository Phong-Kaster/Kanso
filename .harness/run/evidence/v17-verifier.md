# The Verifier, Iteration 17 (2026-10-13), on the tree of `cc4125e`

Fresh runs, each its own Gradle command (`v17-chain.sh`, times in `v17-times.txt`: 21:48:18 to 21:56:49):

| Command | Log | Result |
|---|---|---|
| `:app:cleanTestDebugUnitTest :app:testDebugUnitTest` | `test.log` | exit 0, `BUILD SUCCESSFUL`; 571 tests, failures 0, errors 0, skipped 0; 571 `@Test` lines; every class the DoD names ran green (`v17-c35.txt`) |
| `:app:verifyRoborazziDebug` | `verify-roborazzi.log` | exit 0; `v17-roborazzi-summary.json` total 41, changed 0, added 0 |
| `:app:lintAnalyzeDebug --rerun :app:lintReportDebug --rerun :app:lintDebug` | `lint.log` | exit 0; 0 issues of severity Error |
| `:app:assembleDebug` | `assemble.log` | exit 0 |
| `:app:compileDebugKotlin --rerun-tasks` | `compile.log` | exit 0; no deprecation line names `app/src/main` |
| `:app:dependencies --configuration debugRuntimeClasspath` | `head-deps.txt` | normalised list equals the base's (`v17-deps-diff.txt` empty) |
| `:app:dependencyInsight ... material3` | `v17-m3-insight.txt` | 1.5.0-beta01 |
| `:core:colorpicker:assembleDebug :core:colorpicker:assembleRelease` | `v17-colorpicker.log` | exit 0 |

Greps and diffs: `v17-greps.sh` -> `v17-greps.txt`; symbols: `v17-symbols.txt` (all five OK).

## Per criterion

- **1-5, 7, 9-15, 17, 18, 21 (tests), 24-26, 32, 34 (`LaunchTest`)**: every named class ran green.
- **6**: `HomeReachTest` green. The app code is unchanged since `home-reach.md` was copied (Iteration 15).
- **8**: `EditsLoadingTest` green; the indicator grep prints nothing; pictures 32, 33, 36, 37 did not show a defect.
- **16, 19**: all 15 button calls in `app/src/main` (5 in `CameraScreen.kt`) pass `shapes =`, checked call by call; `MaterialExpressiveTheme(` at `KansoTheme.kt:138`.
- **20 (assumed, A-025)**: (a) `app/src/main` greps print nothing; the catalog lines are the base's. (b)-(e) green.
- **21**: badging equals the base; no INTERNET or ACCESS_NETWORK_STATE.
- **22 - GAP**: `NavBarColourTest`, `KansoBlueContrastTest` green, but the clause "Pictures 1, 2, 28, 29, 34 and 35 show the bar" fails for 28, 29, 34, 35 (`MyEditsScreenshotTest.showPhotos` and `HomeScreenshotTest.showEmpty` host the content without `KansoTabShell`). Filed as T-017.
- **23**: `ThemeFollowTest` green; the fixed-colour grep finds exactly the 8 lines of `fixed-colours.md`; `colors.xml` unchanged; no bitmap added.
- **27**: `ToolBackdropTest` green; pictures 40-42 did not show a defect (soft copy beside sharp stripes).
- **28**: `HomeScreenTest` swipe checks green.
- **29 (assumed, A-027)**: 41 total, 0 changed; 24 M, 17 A, no D; `after/` (41) equals HEAD and `before/` (24) equals the base byte for byte. Every picture looked at and recorded in `baseline-diffs/index.md`: 37 "did not show a defect", 4 defects against DoD 22 (T-017).
- **30**: not driven: the Galaxy A13 showed `NotificationShade` at 21:51 and 21:53 (someone using it).
- **33 (assumed, A-026)**: tests green; the settings-names grep differs from the base only in `AppModule.kt` 83 -> 99.
- **34**: `native-code: 'arm64-v8a' 'armeabi-v7a' 'x86_64'`, `minSdkVersion:'27'`, `targetSdkVersion:'36'`, `compileSdkVersion='37'`.
- **35**: totals above; the assume/ignore grep exits 1; 45 base names gone, each named in `test-edits.md`; every M/D test file listed there.
- **36**: guard files unchanged against the base (`ALLOWED` empty, `EXEMPT` three entries); `ScreenStateTest` adds only `myedits/MyEditsViewModel`; `requiredPreviewNames` adds exactly the nine and removes nothing; each of the nine previews exists; `MaterialTheme(` and Material 2 greps print nothing.
- **37**: every comment grep finds its words (`v17-greps.txt`, C37).
- **38**: verify, lint, compile as above; no `@Suppress("DEPRECATION")` added.
- **39**: `core` diff names only `HSVColorPicker.kt`, one hunk at base lines 66-70; colorpicker builds; picker tests green.
- **40**: build files unchanged; dependencies equal; `reference/` unchanged against `a0d9a79` and not referenced; five drawables OK and used.
- **41**: ADR-002 exists, names 2026-10-12 and ADR-001; ADR-001 carries the note.
- **R1-R9**: every grep prints nothing; `ic_add_photo_alternate.xml` absent; `orphans.sh` prints nothing; unused resources are the base's two.
