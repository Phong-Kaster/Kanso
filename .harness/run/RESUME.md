# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** escalated (waiting for D-001). Run Mode: Autonomous. Branch `loop/kanso-one-class-per-file`, base `4d5f239`.
- **Next Phase (1):** after D-001 is answered in `DECISIONS.md` (consume it, §6.2, then execute)
  - T-001 - scope: `ui/home/{HomeUiState,HomeViewModel,HomeScreen}.kt`, `ui/gallery/{GalleryUiState,GalleryViewModel,GalleryScreen}.kt`, `ui/settings/{SettingsUiState,SettingsViewModel,SettingsScreen}.kt`, new `test/home/HomeViewModelTest.kt`, `test/settings/SettingsUiStateTest.kt` - Capable
  - T-002 - scope: `ui/camera/{CameraStatus,CameraLens,FlashMode,CameraState,CameraUiState,CameraController,CameraViewModel,CameraScreen}.kt`, `ui/tool/{ToolUiState,ToolViewModel,ToolScreen}.kt` - Capable
  - T-003 - scope: `ui/saved/{SavedViewModel,SavedUiState,SavedPhoto,SavedScreen}.kt`, `test/saved/{SavedScreenTest,SavedViewModelTest}.kt`, `test/screenshots/SavedScreenshotTest.kt` - Capable
  - the Iteration wires: `navigation/KansoNavHost.kt` (preview's `HomeContent`, `entry<Saved>`), `di/AppModule.kt` (SavedViewModel recipe)
  - after Phase 1: Phase 2 = T-004, T-005 (Capable); Phase 3 = T-006, T-007 (Fast), then delete `Destinations.kt`, empty `ALLOWED`, update `knowledge/PROJECT.md`
- **Queued decisions:** 1 - D-001 (DoD approval) blocks T-001..T-007
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | UI: `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` (verified in Run 3 at `177b1f8`; redirect to `.harness/run/evidence/`, run long ones in the background, never pipe an exit code)
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` — pass `model` explicitly on every dispatch; the Reviewer is always Capable.
- **Test names at base:** 198 `@Test`; the name pipeline of DoD 3(a) was verified on 2026-10-09 (no duplicates).
