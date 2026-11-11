# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this file is wrong.

- **Stage:** build - all tasks T-001..T-004 complete; STATE records a DONE-candidate. Next invocation is the Verifier (ENGINE §11).
- **Next Phase:** none.
- **Queued decisions:** 0. Mode: Autonomous. **Abandoned / Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && timeout 2400 ./gradlew :app:assembleDebug` | test: `... :app:testDebugUnitTest` (OOM on StickerScreenshotTest possible; rerun alone; delete `app/hs_err_pid*.log`) | lint: `... :app:lintDebug` | pictures: `... :app:verifyRoborazziDebug`.
- **Open issues:** none touching files; see .harness/ISSUES.md.
- **Reference topics:** `reference/run14-facts.md`, `reference/run12-facts.md`; Constraints: `reference/constraints-verbatim.md`
- **Model tiers:** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
