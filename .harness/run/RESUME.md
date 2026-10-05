# RESUME BLOCK

> A derived cache, never a source of truth. On any disagreement with task files or git, this file is wrong.

- **Stage:** Verifier (Iteration 8) cleared DONE-candidate: the 287 MB debug APK fails to install on `emulator-5554`, so DoD 2 (emulator half), 19, 22 cannot be driven. Every other machine criterion re-proved fresh.
- **Next Phase:** 7 (again) - T-006 attempt 2 (Capable). Attempt 1 fixed the install (debug ABIs x86_64+arm64-v8a); `-Op dump` is refused because the focused window is `Application` (likely an ANR dialog, ~3 s first frame). Diagnose, then fix start-up work; widen scope as a Tier-1 amendment if needed. The Iteration itself drives the emulator via `foreman-device.ps1` after the build.
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` plus `:app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug`. Verified 2026-10-05 (clean build 28 min). Baselines in `app/src/test/screenshots/` (nine, never re-record).
- **Device:** `powershell -NoProfile -File .harness/loop/bin/foreman-device.ps1 -Op <op>`; `emulator-5554` is `sdk_gphone64_x86_64`.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
