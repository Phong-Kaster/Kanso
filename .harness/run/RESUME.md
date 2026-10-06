# RESUME BLOCK

> Derived cache. On any disagreement with task files or git, this file is wrong.

- **Stage:** Phase 3 done (T-001..T-007, T-010 complete); DoD approved
- **Next Phase:** 3
  - T-006 - scope: `K/ui/camera/`, `T/camera/`, `T/screenshots/CameraRefusedScreenshotTest.kt`
  - T-007 - scope: `K/data/photo/PhotoSaver.kt`, `K/data/photo/UprightJpeg.kt`, `K/ui/tool/` (Save action only), `T/save/`
- **Created in Phase 2:** `data/media/{Photo,Album,PhotoSource,MediaStorePhotoSource}`; `GalleryViewModel(tool, probe, photoSource)`, `GalleryScreen(viewModel,onBack,onPhotoChosen,modifier)`; `data/photo/PhotoLoader` (open class, `load(uri,maxEdgePx)`), `ToolViewModel(tool,photoUri,loader,probe)`, `ToolScreen(viewModel,onBack)` with disabled Save (`TOOL_SAVE_TAG`), `TOOL_IMAGE_TAG` + `TOOL_PHOTO_URI_KEY`; nav key `ToolPhoto` wired; Koin bindings for PhotoSource/PhotoLoader/ToolViewModel; Coil + ExifInterface in build; test fakes `FakePhotoSource`, `FakePhotoLoader`. Camera tile still opens Placeholder.
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` (+ `:app:verifyRoborazziDebug`; new pictures only through foreman-record-baselines.ps1) | lint: `./gradlew :app:lintDebug`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
- **Phase 3:** T-006, T-007; **Phase 4:** T-008, T-009
- **Shared files the Iteration edits itself:** see PLAN.md
