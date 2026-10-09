# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** Phase 2 complete at Iteration 3 (checkpoint `loop(phase-2)`); next is Phase 3. Autonomous mode: Loop Branch `loop/kanso-m3-expressive-blue`, base `76522d7`. D-001 answered.
- **Next Phase:** Phase 3 = three tasks (T-001 to T-005 and T-009 are complete; read only the task files below):
  - T-006 - scope: `ui/camera/CameraScreen.kt`, `res/values/strings_camera.xml` (new); tests `camera/CameraControlsTest` (its gallery-hosting test stays green untouched), `camera/CameraScreenTest`, `camera/CameraFlowTest` and `camera/CameraPermissionTest` (only if a tag or text must change), `screenshots/CameraRefusedScreenshotTest`, `screenshots/CameraControlsScreenshotTest` (new) - Capable
  - T-007 - scope: `ui/settings/SettingsScreen.kt`, `res/values/strings_settings.xml` (new); tests `settings/ThemeColourSettingsTest`, `ThemeModeSettingsTest`, `ThemeModeRestartTest`, `ThemeColourRestartTest`, `PickerNoAlphaTest`, `ThemeSwatchTest` (new), `screenshots/ScreenshotTest` - Capable
  - T-010 - scope: `ui/saved/SavedScreen.kt`, `res/values/strings_saved.xml` (add names only); tests `saved/SavedScreenTest`, `screenshots/SavedScreenshotTest`, `ExpressiveApiTest` (only the two KDoc sentences "Nothing in the app uses them yet") - Capable
  - The Iteration wires after the Workers (PLAN.md, Phase Grouping): `git rm` of `ui/camera/CameraIcons.kt` and `ui/common/BackArrowIcon.kt` once `git grep -w` shows no user (DoD R4; the gallery stopped using both in Phase 2), `PreviewsRenderTest.kt` (`requiredPreviewNames`) if a preview is added, drawables, then `compareRoborazziDebug`, comparison images into `.harness/run/evidence/baseline-diffs/phase3/`, `foreman-record-baselines.ps1 -Module app` (pictures 4, 5, 6, 14, 15, 18, 19 re-recorded; 20, 21, 24, 25 new). Open and judge each picture first.
  - Careful: Home's text invariants and `home-tool-<TOOL_NAME>` tags are binding Constraints in PROJECT.md (T-006's camera tests tap Home's "Camera" text; `CameraControlsTest` hosts the gallery and reads `gallery_open_camera`); a Worker holds no build capability, so a compile error in its tests is found by the Iteration's first combined run (do not count it as an attempt).
  - Later: Phase 4 = T-008 (motion), T-011 (cross-screen proofs); then the Verifier. DoD 16's grep still hits `CameraScreen.kt` (black picture area, sample colours), `CameraIcons.kt`, `BackArrowIcon.kt` and `SettingsScreen.kt` (`Color.Black`): Phase 3 clears them except the picker card.
- **Queued decisions:** 0.
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` (compare: `:app:compareRoborazziDebug`; record new/approved: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`) | the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (2 to 4 min warm, 7 min cold). Always under `timeout`, output to `.harness/run/evidence/`, begin with `cd /d/Valve/Personal/Kanso &&`. A background Gradle started with `( … ) &` is not tracked by the harness: wait for it in the same turn with an `until grep -q '^exit ' <log>` loop.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` (dispatch every Worker with `model` set explicitly; the Reviewer and the Verifier are always `opus`)
