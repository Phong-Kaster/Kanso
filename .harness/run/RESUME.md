# RESUME BLOCK

> Derived cache; on any disagreement with the task files or git, this file is wrong.

- **Stage:** execute; Phases 1-4 (T-001..T-008) complete
- **Next Phase (5):** T-009 (coverage test; exemptions per A-003, A-004 in its task file).
- **Queued decisions:** 0 (D-001 answered)
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` and `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` (from `.harness/knowledge/PROJECT.md`; heap 3g, run long ones in the background)
- **Base commit for "unchanged" checks:** `9a61275`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
- **Flakes:** full runs on Windows sometimes fail one Robolectric UI test (CameraScreenTest retake, ThemeColourSettingsTest wallpaper); rerun alone before treating as a defect.
