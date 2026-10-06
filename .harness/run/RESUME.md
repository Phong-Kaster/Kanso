# RESUME BLOCK

> Derived cache. On any disagreement with task files or git, this file is wrong.

- **Stage:** Phase 1 done (T-001..T-003 complete); DoD approved
- **Next Phase:** 2
  - T-004 - scope: `K/ui/gallery/` (all), `K/data/media/`, `T/gallery/GalleryGridTest.kt`, `T/screenshots/GalleryGridScreenshotTest.kt` (K, T as before)
  - T-005 - scope: `K/ui/tool/`, `K/data/photo/PhotoLoader.kt`, `T/tool/`, `T/screenshots/ToolScreenshotTest.kt`
  - T-010 - scope: `core/colorpicker/.../HSVColorPicker.kt`, `K/ui/settings/SettingsScreen.kt` (call site), `T/settings/PickerNoAlphaTest.kt`
- **Created in Phase 1:** `GalleryScreen(viewModel, onBack)`, `GalleryViewModel(tool, probe)`, tags GALLERY_*_TAG in `K/ui/gallery/GalleryScreen.kt`; `K/ui/permission/{PermissionGate,AccessRefusedPanel,PhotoPermissions}.kt`; `GalleryScreen` granted body is a placeholder (`GALLERY_BODY_TAG`) T-004 replaces; test helper `RecordingRegistry` in `T/gallery/GalleryPermissionTest.kt`; nav entry `Gallery(tool)` wired; Camera tile still opens Placeholder.
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` (+ `:app:verifyRoborazziDebug`; new pictures only through foreman-record-baselines.ps1) | lint: `./gradlew :app:lintDebug`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
- **Phase 2:** T-004, T-005, T-010; **Phase 3:** T-006, T-007; **Phase 4:** T-008, T-009
- **Shared files the Iteration edits itself:** see PLAN.md
