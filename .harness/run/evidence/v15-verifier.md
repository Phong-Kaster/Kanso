# The Verifier, Iteration 15 (2026-10-13), on the tree of `31b8402`

Fresh runs, each its own Gradle command (`v15-times.txt`: 21:29:53 to 21:38:50):

| Command | Log | Result |
|---|---|---|
| `:app:cleanTestDebugUnitTest :app:testDebugUnitTest` | `test.log` | exit 0, `BUILD SUCCESSFUL`; 571 tests, failures 0, errors 0, skipped 0; 571 `@Test` lines (`v15-c35.txt`) |
| `:app:verifyRoborazziDebug` | `verify-roborazzi.log` | exit 0; `results-summary.json` total 41, changed 0, added 0 |
| `:app:lintAnalyzeDebug --rerun :app:lintReportDebug --rerun :app:lintDebug` | `lint.log` | exit 0; 0 issues of severity Error |
| `:app:assembleDebug` | `assemble.log` | exit 0 |
| `:app:compileDebugKotlin --rerun-tasks` | `compile.log` | exit 0; no deprecation line names `app/src/main` |
| `:app:dependencies --configuration debugRuntimeClasspath` | `head-deps.txt` | normalised list equals the base's (`v15-deps-diff.txt` empty) |
| `:app:dependencyInsight ... material3` | `v15-m3-insight.txt` | 1.5.0-beta01 |
| `:core:colorpicker:assembleDebug :core:colorpicker:assembleRelease` | `v15-colorpicker.log` | exit 0 |

Greps and diffs: `v15-greps.txt`, `v15-c37.txt`, `v15-build.txt`, `v15-symbols.txt`.

## Per criterion

- **1-5, 7, 9-15, 17, 18, 21 (tests), 22, 24-26, 32, 34 (`LaunchTest`)**: every named class exists and ran green in `test.log`.
- **6**: `HomeReachTest` green (5 tests); its gesture table copied to `home-reach.md`, every tool shortcut "1 tap".
- **8**: `EditsLoadingTest` green; the indicator grep prints nothing; pictures 32, 33, 36, 37 unchanged.
- **16**: camera tests green; the five button calls in `CameraScreen.kt` pass `shapes =`; `test-edits.md` lists the camera test files.
- **19**: `m3-scan.md` present; all 15 button calls in `app/src/main` pass `shapes =` (checked again here, call by call).
- **20 (machine-then-human, assumed by A-025)**: (a) the `app/src/main` greps print nothing; the catalog lines `compose-shimmer` and `lottie` are the base's (A-025). (b), (c), (d) green; (e) `NavTransitionTest` prints "not driven by machine".
- **21**: `head-badging.txt` equals `base-badging.txt`; no INTERNET or ACCESS_NETWORK_STATE.
- **23**: `ThemeFollowTest` green; the fixed-colour grep finds the 8 lines of `fixed-colours.md` and no other; `colors.xml` unchanged; no bitmap added.
- **27**: `ToolBackdropTest` green; the SDK 35 control prints "blur not driven by machine on this host", so the SDK 35 pass asserts only that the space is filled; SDK 30 asserted in full.
- **28**: `HomeScreenTest` swipe checks green.
- **29 (assumed by A-027)**: verify 41 total, 0 changed; `git diff --name-status` 24 M, 17 A, no D. `after/` was stale for pictures 4, 5, 6, 24, 25 (re-recorded in Iteration 13): refreshed from HEAD here, and all 41 `after/` and 24 `before/` files now equal HEAD and the base byte for byte. The per-picture look is not recorded yet.
- **30**: not driven on the Galaxy A13: at 21:40 its focused window was `NotificationShade`, so a person was using it. Supplementary, on the emulator `emulator-5554` (not the criterion's starting state): the debug APK installed, a cold start reached Home in 4.4 s, the crash buffer has no `FATAL EXCEPTION`, one dump (only `com.phongkaster.kanso`) holds "Home", "My edits", "Settings", "What will you edit today?", "Try now", "Recent edits" and not "Open a photo". Pictures `v15-emu-home.png`, `v15-emu-myedits.png`, `v15-emu-settings.png` (an emulator with no personal data).
- **33 (assumed by A-026)**: tests green; the settings-names grep differs from the base only in a line number (`AppModule.kt` 83 -> 99).
- **34**: `assembleDebug` exit 0; badging: `native-code: 'arm64-v8a' 'armeabi-v7a' 'x86_64'`, `minSdkVersion:'27'`, `targetSdkVersion:'36'`, `compileSdkVersion='37'`.
- **35**: totals as above; the assume/ignore grep exits 1; 45 base names gone (C-locale `comm`), every one listed in `test-edits.md`; all 31 `M` test files listed with a reason (the file's own count says 30: a miscount, the list has 31) and the one `D`.
- **36 - GAP**: `requiredPreviewNames` lacks `ToolNarrowPhotoPreview`, `PhotoViewerPreview`, `HomeEmptyPreview`, `HomeLoadingPreview`, which the DoD names. The previews exist in the app. Filed as T-016. The other guard clauses hold (`OneTypePerFileTest` `ALLOWED` empty, the `MaterialTheme(` and Material 2 greps print nothing).
- **37**: every comment grep finds its words (`v15-c37.txt`).
- **38**: verify, lint, compile as above; no `@Suppress("DEPRECATION")` added.
- **39**: `core` diff names only `HSVColorPicker.kt`, one hunk at base lines 66-70; colorpicker builds; `PickerNoAlphaTest`, `PickerDefaultAlphaTest` green.
- **40**: build files unchanged; dependencies equal; material3 1.5.0-beta01; `reference/` unchanged against `a0d9a79` and not referenced; the five new drawables check OK against upstream (`v15-symbols.txt`) and are used.
- **41**: ADR-002 exists and names 2026-10-12 and ADR-001; ADR-001 carries the "superseded in part" note.
- **R1-R7**: every grep prints nothing; `ic_add_photo_alternate.xml` is absent.
- **R8**: `ToolSelectionAnimationTest.kt` is not tracked; `orphans.sh` prints nothing.
- **R9**: `head-unused-resources.txt` is `gallery_album_label` and `gallery_body_placeholder`, the base's two; the five named resources are absent.
- **Looked at, not a criterion**: `KansoTabShell.kt:110`'s `headlineSmallEmphasized` is the stand-in text of `KansoTabShellPreview`, not a screen's title. Home and Settings both use `headlineLargeEmphasized`. No action.
