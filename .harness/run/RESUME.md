# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** Phase 1 complete and checkpointed (D-001 approved 2026-10-09). Run Mode: Autonomous. Branch `loop/kanso-one-class-per-file`, base `4d5f239`.
- **Next Phase (2):** T-004, T-005 (both Capable, no dependency left, scopes disjoint)
  - T-004 - scope: new `test/OneTypePerFileTest.kt` only. Ships with a path-keyed `ALLOWED` of the 14 files that still break the rule (listed in the task) plus a stale-entry check.
  - T-005 - scope: new `test/ScreenStateTest.kt` only (exactly one public StateFlow per ViewModel by Java reflection; `…Content` takes the UiState; no var/Mutable in `…UiState.kt`).
  - the Iteration wires nothing; it greps Phase 1 files for stray offenders before the Phase and fixes any the checker reports.
  - after Phase 2: Phase 3 = T-006, T-007 (Fast), then `git rm` of `navigation/Destinations.kt`, empty `ALLOWED`, update `knowledge/PROJECT.md`
- **Queued decisions:** 0 (D-001 answered)
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | UI: `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` (all verified 2026-10-09 at the Phase 1 tree: one combined run `:app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:verifyRoborazziDebug :app:lintDebug :app:assembleDebug --continue` takes about 5.5 minutes; redirect to `.harness/run/evidence/`, run in the background, wait inside the turn, never pipe an exit code)
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` — pass `model` explicitly on every dispatch; the Reviewer is always Capable.
- **Tests now:** 207 `@Test` (198 at base + 9 new: Home 1, Settings 3, Saved 5); all pass. Baselines (16) untouched.
