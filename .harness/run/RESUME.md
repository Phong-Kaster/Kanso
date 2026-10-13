# RESUME BLOCK

> A derived cache; git and task files win on disagreement.

- **Stage:** Iteration 18 completed T-017 (pictures 28, 29, 34, 35 re-recorded with the bar, whole set green). PARTIAL-candidate recorded in STATE.md. **Next: the Verifier** (§11 + §14.4): re-prove every machine criterion with the whole-set command, look again at pictures 28, 29, 34, 35 (`baseline-diffs/after/`), drive the Galaxy A13 if `-Op focus -Serial R58W1120QVA` names Kanso; then `DONE_PARTIAL` (A-025..A-027 are Tier-3 assumptions). Write A-029's SHA and revert command in ASSUMPTIONS.md (commit of Iteration 18).
- **Next Phase:** none
- **Queued decisions:** 0. **Abandoned:** none. **Unreachable:** none.
- **Open ISSUES items naming Phase files:** none.
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` | whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (about 4-8 min; run as `evidence/v18-chain.sh`).
- **Model tiers:** fast: `sonnet` | capable: `opus` (Reviewer and Verifier always `opus`)
- **Time note:** the Runtime kills an Iteration at 90 minutes.
