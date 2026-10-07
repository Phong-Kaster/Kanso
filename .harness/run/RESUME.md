# RESUME BLOCK

> Derived cache; on any disagreement with the task files or git, this file is wrong.

- **Stage:** execute; D-001 approved; Phases 1-2 (T-001, T-002, T-003, T-008) complete
- **Next Phase (3):** T-005 `ui/tool/ToolScreen.kt` (Fast); T-006 `ui/saved/SavedScreen.kt` (Fast); T-007 `ui/gallery/GalleryScreen.kt` (scopes under `app/src/main/java/com/phongkaster/kanso/`). Copy `SettingsContent`/`PermissionGateContent`/`CameraContent` pattern. Preview names: ToolPhotoPreview, ToolUnreadablePreview, SavedPreview, GalleryPhotosPreview, GalleryPartialAccessPreview, GalleryRefusedPreview, GalleryEmptyPreview.
- **Later phases:** 4 = T-004 (nav host); 5 = T-009 (coverage test; exemptions per A-003 in its task file).
- **Queued decisions:** 0 (D-001 answered)
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` and `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` (from `.harness/knowledge/PROJECT.md`; heap 3g, run long ones in the background)
- **Base commit for "unchanged" checks:** `9a61275`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
- **Flakes:** full runs on Windows sometimes fail one Robolectric UI test (CameraScreenTest retake, ThemeColourSettingsTest wallpaper); rerun alone before treating as a defect.
