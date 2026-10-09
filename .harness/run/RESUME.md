# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** Phase 3 complete and checkpointed (the last file splits). Every task T-001..T-007 is complete. Run Mode: Autonomous. Branch `loop/kanso-one-class-per-file`, base `4d5f239`.
- **PARTIAL-candidate recorded in `STATE.md`** (ENGINE §14.4): the next invocation is the **Verifier**, acting at the Capable tier. It reads `DoD.md`, and:
  1. re-proves every `machine` criterion with fresh evidence (run the build, tests, Roborazzi and lint itself, plus the DoD's grep and diff checks, including the six Removals R1-R6);
  2. drives every `machine-then-human` criterion it can (emulator or the device wrapper, §12; the repository's emulator history is in `knowledge/PROJECT.md`), stating the state it drove from, and writes "did not fail when driven from X", never "works";
  3. writes the criteria still awaiting a person (DoD 6 and 15, and Run 3's unsigned items) into `.harness/ISSUES.md` under **Awaiting a person** as tickable `- [ ]` items, carrying over every existing unsigned item word for word (17 now);
  4. files tasks for any gap it can close (then clear the flag and report `CONTINUE`), otherwise reports `DONE_PARTIAL`, with no Cleanup Commit (`.harness/run/` stays).
- **Next Phase:** none; no executable task remains.
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | UI: `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` (all verified 2026-10-09 at the Phase 3 tree: one combined run `cd /d/Valve/Personal/Kanso && ./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:verifyRoborazziDebug :app:lintDebug :app:assembleDebug --continue` takes 2 to 5 minutes; redirect to `.harness/run/evidence/`, run in the background, wait inside the turn, never pipe an exit code, never leave the shell inside `app/build`)
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` — pass `model` explicitly on every dispatch; the Reviewer is always Capable.
- **Tests now:** 231 `@Test` (names identical to the base `4d5f239` list in `evidence/p3-testnames-before.txt`); all pass. Baselines (16) untouched. `OneTypePerFileTest.ALLOWED` is `emptySet()`; `Destinations.kt` is gone.
