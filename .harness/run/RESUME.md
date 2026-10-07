# RESUME BLOCK

> Derived cache; on any disagreement with the task files or git, this file is wrong.

- **Stage:** execute; D-001 approved; Phase 1 (T-001) complete
- **Next Phase (2):** T-002 `ui/settings/SettingsScreen.kt`; T-003 `ui/permission/AccessRefusedPanel.kt`, `ui/permission/PermissionGate.kt`; T-008 `ui/camera/CameraScreen.kt` (scopes under `app/src/main/java/com/phongkaster/kanso/`). Pattern to copy: `HomeContent` + `HomePreview`, `KansoThemePreview`.
- **Later phases:** 3 = T-005 (tool, Fast), T-006 (saved, Fast), T-007 (gallery); 4 = T-004 (nav host); 5 = T-009 (coverage test). Scopes in PLAN.md / task files.
- **Queued decisions:** 0 (D-001 answered)
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` and `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` (from `.harness/knowledge/PROJECT.md`; heap 3g, run long ones in the background)
- **Base commit for "unchanged" checks:** `9a61275`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
- **Flakes:** full runs on Windows sometimes fail one Robolectric UI test (CameraScreenTest retake, ThemeColourSettingsTest wallpaper); rerun alone before treating as a defect.
