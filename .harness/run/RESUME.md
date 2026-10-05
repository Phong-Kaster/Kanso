# RESUME BLOCK

> A derived cache, never a source of truth. On any disagreement with task files or git, this file is wrong.

- **Stage:** DONE-candidate; T-001 to T-005 complete. Next invocation is the Verifier (Capable): re-prove every machine criterion, drive machine-then-human ones.
- **Next Phase:** none. Baselines in `app/src/test/screenshots/` (nine, never re-record).
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` plus `:app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug`. Verified 2026-10-05. AGP 8.13 pins nav3 1.1.7 / lifecycle 2.10 (see PROJECT.md).
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
