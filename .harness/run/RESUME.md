# RESUME BLOCK

- **Stage:** PARTIAL-candidate recorded (Iteration 8). T-008 complete. The next invocation is the Verifier: re-prove every machine criterion (DoD 31 and R2 now should pass), drive device criteria 7, 9, 11, 13 if the A13 is free (`-Op focus` first), then report `DONE_PARTIAL` if only the 17 unrecorded pictures (DoD 27) remain.
- **Next Phase:** none
- **Queued decisions:** 0
- **Abandoned / unreachable:** none
- **Open red:** 17 `ToolScreenshotTest` pictures (45-54 new without baseline; 16,17,22,23,40,41,42 differ), `recordRoborazziDebug` denied; listed in ISSUES.md.
- **Verified commands:** build `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug` | lint `./gradlew :app:lintDebug` (under `timeout`); README: `bash .harness/run/evidence/readme-paths.sh`.
- **Model tiers:** fast: `sonnet` | capable: `opus`
