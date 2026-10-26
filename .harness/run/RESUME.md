# RESUME BLOCK

> Regenerated every iteration. A derived cache, never a source of truth.

- **Stage:** Ended `DONE_PARTIAL` (Verifier, Iteration 14). All tasks complete; every machine criterion holds except 27 and 34 (A-001, Tier 3: 15 stale screenshot references). Criteria 23, 24, 25, 28, 31 and 40 await a person (`.harness/ISSUES.md`, "Awaiting a person, Run 10"). A later invocation acts only on a human answer in `DECISIONS.md` (for example, re-record the 15 pictures under A-001).
- **Next Phase:** none
- **Queued decisions:** 0 | **Abandoned:** none | **Unreachable:** none | **Assumptions:** A-001
- **Verified commands:** build `timeout 3600 ./gradlew :app:assembleDebug` | test `timeout 3600 ./gradlew :app:testDebugUnitTest` | lint `timeout 3600 ./gradlew :app:lintDebug` | screenshots `timeout 3600 ./gradlew :app:verifyRoborazziDebug` (fails A-001's 15 pictures).
- **Open issues in scope:** none
- **Model tiers:** fast: `sonnet` | capable: `opus`
