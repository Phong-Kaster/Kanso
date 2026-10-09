# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** Verifier done (Iteration 5, on `9fa9d89`). Run Mode: Autonomous. Branch `loop/kanso-one-class-per-file`, base `4d5f239`. Every task T-001..T-007 is complete, nothing is abandoned, and every `machine` criterion was re-proved with fresh evidence. The run ended `DONE_PARTIAL`: DoD 6 and 15 await a person (`.harness/ISSUES.md`, Awaiting a person, Run 4). No Cleanup Commit; `.harness/run/` stays.
- **If invoked again:** only a human act changes anything (a signature on DoD 6 or 15, or a new goal). With no new decision in `DECISIONS.md`, re-running the Verifier would repeat the same result. Re-verify before reporting if the tree moved past `9fa9d89`.
- **Next Phase:** none; no executable task remains.
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | UI: `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` (all verified 2026-10-09 on `9fa9d89`: one combined run `cd /d/Valve/Personal/Kanso && ./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:verifyRoborazziDebug :app:lintDebug :app:assembleDebug --continue` took 5 min 10 s cold; redirect to `.harness/run/evidence/`, run in the background, wait inside the turn, never leave the shell inside `app/build`)
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` — pass `model` explicitly on every dispatch; the Reviewer is always Capable.
- **Tests now:** 231 `@Test`, all pass. 16 baselines untouched. `OneTypePerFileTest.ALLOWED` is `emptySet()`.
- **Device:** two real phones attach (`R58W1120QVA` the owner's Galaxy A13, `HJ609L045411` another); name one with `-Serial`. The debug build of `9fa9d89` is installed on the A13 with photo access granted. Stop driving when `-Op focus` says `NotificationShade`.
