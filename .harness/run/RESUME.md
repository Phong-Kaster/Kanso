# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** Phase 1 complete at Iteration 2 (checkpoint `loop(phase-1)`); next is Phase 2. Autonomous mode: Loop Branch `loop/kanso-m3-expressive-blue`, base `76522d7`. D-001 answered.
- **Next Phase:** Phase 2 = three tasks (T-001 to T-003 are complete; read only the task files below):
  - T-004 - scope: `ui/home/HomeScreen.kt`, `HomeUiState.kt`, `HomeViewModel.kt`, `res/values/strings_home.xml` (new); tests `home/HomeViewModelTest`, `navigation/NavigationTest`, `navigation/PhotoToToolNavigationTest`, `settings/ThemeModeSettingsTest`, `settings/ThemeModeRestartTest`, `LaunchTest` and `screenshots/ScreenshotTest` only if a Home flow must change - Capable
  - T-005 - scope: `ui/gallery/GalleryScreen.kt`, `res/values/strings_gallery.xml` (new); tests `gallery/GalleryGridTest`, `gallery/GalleryPermissionTest`, `gallery/FakePhotoSource` (additions only), `screenshots/GalleryGridScreenshotTest`, `GalleryRefusedScreenshotTest`, `GalleryLoadingScreenshotTest` (new) - Capable
  - T-009 - scope: `ui/tool/ToolScreen.kt`, `res/values/strings_tool.xml` (add names only), `save/SaveButtonTest`, `screenshots/ToolScreenshotTest` only if a flow must change - Capable
  - The Iteration wires after the Workers (PLAN.md, Phase Grouping): `KansoNavHost.kt` (Home's new callbacks and the preview that builds `HomeUiState`), `Tool.kt` (`CAMERA` out once Home no longer lists it; `editingTools` becomes `entries`), `PreviewsRenderTest.kt` (`requiredPreviewNames`), drawables, then `compareRoborazziDebug`, comparison images into `.harness/run/evidence/baseline-diffs/phase2/`, `foreman-record-baselines.ps1 -Module app` (pictures 1, 2, 3, 10-13 re-recorded, 26 and 27 new). Open and judge each picture first.
  - Careful: `ToolScreenTest`'s whole-app walk and `SavedNavigationTest` tap Home's Edit entry by tag `home-tool-EDIT` when it exists and by the "Edit" row until then, so T-004's cards must carry `home-tool-<TOOL_NAME>`; Home's text invariants are binding Constraints in PROJECT.md.
  - Later: Phase 3 = T-006 (Camera), T-007 (Settings), T-010 (Saved mark); Phase 4 = T-008 (motion), T-011 (cross-screen proofs); then the Verifier.
- **Queued decisions:** 0.
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` (compare: `:app:compareRoborazziDebug`; record new/approved: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`) | the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (2 to 4 min warm, 7 min cold). Always under `timeout`, output to `.harness/run/evidence/`, begin with `cd /d/Valve/Personal/Kanso &&`. A background Gradle started with `( … ) &` is not tracked by the harness: wait for it in the same turn with an `until grep -q '^exit ' <log>` loop.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` (dispatch every Worker with `model` set explicitly; the Reviewer and the Verifier are always `opus`)
