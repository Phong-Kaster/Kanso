# RESUME BLOCK

> A derived cache, never a source of truth. On any disagreement with task files or git, this file is wrong.

- **Stage:** DONE_PARTIAL (Iteration 11). Nothing left to run.
- **Next Phase:** none (all tasks complete)
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` plus `:app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug`. Verified 2026-10-05 (clean build 28 min). Baselines in `app/src/test/screenshots/` (nine, never re-record).
- **Device:** `powershell -NoProfile -File .harness/loop/bin/foreman-device.ps1 -Op <op>`; `emulator-5554` is `sdk_gphone64_x86_64`; a system window `Application` covers it (A-005).
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
