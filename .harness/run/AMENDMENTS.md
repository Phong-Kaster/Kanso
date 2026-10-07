
## Iteration 2 - 2026-10-07

- D-001 consumed: the DoD (14 criteria, no Removals), ComposablePreviewScanner 0.9.3 as `testImplementation` and `ui-tooling-preview` as `implementation` approved as written. A-001 not overturned. No plan change.
- Tier 1: T-009's description now says to replace `PreviewsRenderTest`'s fixed `filesExpectedToHavePreviews` list with a source scan of every file holding a `@Composable` (reviewer finding; DoD 2).

## Iteration 3 - 2026-10-07

- Tier 1: `PreviewsRenderTest` gained a 60 s watchdog (halts the test JVM and prints the preview's name) on the operator's note in DECISIONS.md that a hung preview ran 35 min unbounded. Outside T-005..T-007 scope by necessity; T-009 owns the file and keeps it. Known cost: a hang writes no test report, only stderr and a non-zero exit.
- Tier 1: `ToolSavingPreview` removed. Cause of the hang is probably the indeterminate `CircularProgressIndicator` (infinite animation); removing it made the render test pass in 3 min. Not proven against the thread dump (which shows re-composition in one frame). The Saving state has no preview; T-009/future previews must not use indeterminate indicators.
- Tier 1: preview sample data in Saved and Gallery made lazy/function so the shipping app does not build it (reviewer finding).
- Environment: Gradle `transforms` cache was corrupt after the killed JVM; moved aside with the trash wrapper (RECOVERY R-265).

## Iteration 7 - Tier 1: T-010 added (Verifier gap)

The full unit-test suite fails in fresh runs on Windows. ThemeColourSettingsTest hit a DataStore rename AccessDenied twice, including on the one rerun DoD 8 allows. CameraScreenTest retake failed once. Added T-010 (Phase 6, Capable) to make full runs reliable without changing existing tests. PRD, DoD and architecture are unchanged.

## Iteration 10 - Tier 1: T-011 added (Verifier gap)

The first fresh full `testDebugUnitTest` failed on `CameraScreenTest.retake_deletes_the_shot_and_shows_the_shutter_again` ("Retake left the shot's file behind"). DoD 8's one rerun covers only the ThemeColourSettingsTest flake, so DoD 8 and 13 are not proved. Added T-011 (Phase 7, Capable): make retake surely delete the file without changing an existing test. PRD, DoD and architecture are unchanged.
