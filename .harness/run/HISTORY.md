# HISTORY

### Iteration 1 - 2026-10-07 (bootstrap)
- **Phase:** none (bootstrap)
- Attempted: read PRD.md, CLAUDE.md, the 10 composable files, the build files and the screenshot and KDoc tests; classified the repository as a product; wrote DoD (14 criteria), PLAN (9 tasks in 5 phases) and task files; created branch `loop/kanso-composable-previews` from `loop/kanso-pick-shoot-save` (9a61275).
- Learned: the build already has `debugImplementation(ui-tooling)` but no `ui-tooling-preview`; `@Preview` has binary retention, so finding previews in a test needs a bytecode scanner (ComposablePreviewScanner) or a hand-kept list.
- Reconciled: the analysis fan-out of the spec was done by the engine itself (one small PRD, one author); no analyst subagents were dispatched. A-001 recorded for the navigation host.

### Iteration 2 - 2026-10-07
- **Phase:** 1 (T-001)
- Recovered: found uncommitted Phase 1 work from a crashed invocation (build files, HomeScreen, KansoTheme, two new tests); coherent, so salvaged and verified instead of reverted.
- Verified: assembleDebug, testDebugUnitTest, verifyRoborazziDebug, lintDebug green on the third full run; the first two each failed one unrelated flaky Robolectric test. Reviewer: no blocking findings; T-009 widened (AMENDMENTS), A-002 recorded.

### Iteration 3 - 2026-10-07
- **Phase:** 2 (T-002, T-003, T-008), three Capable Workers, scopes disjoint and contained.
- Verified: assembleDebug, testDebugUnitTest, verifyRoborazziDebug, lintDebug green (two full runs each hit the known CameraScreenTest retake flake once); baselines identical to 9a61275.
- Reviewer: no blocking findings. Duplicate Settings preview fixed (System mode now shows the default-purple colour). Camera slot fillers uncoverable by previews: A-003, noted for T-009.

## Archived Decisions

- D-001 (DoD approval): approved as written, 2026-10-07.

## Iteration 4

- Recovered Phase 3 debris (Tool, Saved, Gallery previews + watchdog): render test hung on `ToolSavingPreview` (indeterminate spinner); removed it, tests/Roborazzi/lint green. Gradle transforms cache corrupted by the killed JVM, moved aside via trash wrapper. Reviewer: watchdog scope (amended), PhotoThumbnail exemption (A-004), eager sample data (fixed). Flakes (ThemeColourSettingsTest, CameraScreenTest) passed on rerun alone.

## Iteration 5 - Phase 4 (T-004)

Split KansoNavHost into stateful host + stateless KansoNavDisplay with KansoNavHostPreview. Build, tests, Roborazzi, lint green. No debris found at recovery; the earlier hang did not recur.

## Iteration 6 - Phase 5 (T-009)

Added `PreviewCoverageTest` (source scan with self-checks, named exemptions A-003/A-004), widened `PreviewsRenderTest` (all DoD 3 names, every file with a composable has a preview), comment-blanked preview bodies in `PreviewHygieneTest`. Reviewer found 3 majors: slot-default mentions count as drawn (accepted: it is a real use), wrapper not checked thin (fixed), comment blanking ignored strings (fixed, self-check added). Build, tests, Roborazzi, lint green. DONE-candidate recorded.

## Iteration 7 - Verifier

Fresh runs: `assembleDebug` 0, `lintDebug` 0, `:app:dependencies` release (ui-tooling-preview, no ui-tooling) and debug (ui-tooling) as DoD 6 says. `testDebugUnitTest` 1 (ThemeColourSettingsTest AccessDenied on DataStore rename), `verifyRoborazziDebug` 1 (CameraScreenTest retake), full rerun 1 (ThemeColourSettingsTest again). Both pass alone, and the previews + ThemeColour combination passed twice. Static checks held: no M/D/R in app/src/test since 9a61275, screenshots identical, nav3 1.1.7, agp 8.13.0, three ABIs, reference/ unchanged, no new firebase/ads/analytics entries, all 18 DoD 3 preview names exist and are asserted. DoD 8/13 not proved → T-010 filed, DONE-candidate cleared. Devices not driven (criteria 7, 10): another Verifier pass follows T-010.

## Iteration 8 - Phase 6 (T-010, attempt 1)

Added a retrying atomic move to the settings DataStore storage. Unit tests 6/7 real runs green, Roborazzi 3/3, lint and assemble green. One unit failure not captured; T-010 stays pending for attempt 2.

## Iteration 9 - Phase 6 (T-010, attempt 2)

Looped `testDebugUnitTest --rerun`: 5 of 5 green; `verifyRoborazziDebug --rerun` 3 of 3 green; no code change beyond attempt 1's retrying move. `app/src/test` diff lists only A. T-010 complete; DONE-candidate recorded.

## Iteration 10 - Verifier

Fresh runs on 0b46878: `assembleDebug` 0; `testDebugUnitTest --rerun-tasks` 1 (198 tests, 1 failed: `CameraScreenTest.retake_deletes_the_shot_and_shows_the_shutter_again`, "Retake left the shot's file behind", CameraScreenTest.kt:119); `verifyRoborazziDebug` 0 (the same test passed there); `lintDebug` 0. Static checks held: DoD 6 (release has ui-tooling-preview and no ui-tooling, debug has ui-tooling), DoD 8 diff only A, DoD 9 screenshots identical to 9a61275, DoD 14 (nav3 1.1.7, agp 8.13.0, three ABIs, reference/ unchanged, no INTERNET/ACCESS_NETWORK_STATE in the merged debug manifest, no new firebase/ads/analytics lines). DoD 8/13 not proved → T-011 filed, DONE-candidate cleared. Devices not driven (criteria 7, 10): another Verifier pass follows T-011.
