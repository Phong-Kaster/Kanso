# RESUME BLOCK

> Derived cache; on any disagreement with the task files or git, this file is wrong.

- **Stage:** ended DONE_PARTIAL (Verifier Iteration 12). T-001..T-011 complete; every machine criterion re-proved. Criteria 7, 10, 12 await a person (`.harness/ISSUES.md`).
- **Next Phase:** none
- **Queued decisions:** 0 (D-001 answered)
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` and `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` (heap 3g; each full test run ~2 min). Never run Gradle batches in background past 10 min: a killed batch leaves Gradle running and fails the next run.
- **Base commit for "unchanged" checks:** `9a61275`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
- **Flakes:** none open.
