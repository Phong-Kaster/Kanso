# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** ended `DONE_PARTIAL` (Verifier, Iteration 5, 2026-10-09). Run Mode: Autonomous. Branch `loop/kanso-agp9-toolchain`, base `76ecb97`. T-001 to T-003 complete (`loop(phase-1)` `d816bde`, `loop(phase-2)` `58d6efb`, `loop(phase-3)` `c0871f3`); every `machine` criterion re-proved by the Verifier (`evidence/verifier.md`). `DONE` is stopped by A-007 (criterion 11 assumed, Tier 3) and by criteria 3 (not driven: the Galaxy A13 was in use) and 7 ("nothing moved"), unsigned in `.harness/ISSUES.md` under "Awaiting a person, Run 5". No Cleanup Commit; `.harness/run/` stays.
- **Next Phase:** none. A later invocation has nothing to execute unless a human answer arrives in `DECISIONS.md` (for example reversing A-007 or another assumption) or a signature fails: consume it (ENGINE 6.2, 14.1), file tasks for what it changes, and verify again. A ticked item in `ISSUES.md` or `DECISIONS.md` is recorded in `STATE.md` "Signed off"; with 3 and 7 signed, only A-007 still stops `DONE`.
- **Driving criterion 3 later:** `powershell -NoProfile -File .harness/loop/bin/foreman-device.ps1 -Op <op> -Serial R58W1120QVA`; `-Op devices` first; stop at once if `-Op focus` reports anything but `com.phongkaster.kanso` or its permission dialog; keep raw dumps outside the repository (PROJECT.md Constraint). Run 4's debug build is still installed on the phone.
- **Queued decisions:** 0 (D-001 answered). **Abandoned:** none. **Unreachable:** none.
- **Verified commands (AGP 9.4.1, Gradle 9.6.0):** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | UI: `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` (fresh analysis: `:app:lintAnalyzeDebug --rerun :app:lintReportDebug --rerun :app:lintDebug`) | release: `./gradlew :app:assembleRelease` (about 2-4 min) | modules: the DoD 11 command. Start each with `cd /d/Valve/Personal/Kanso &&`, redirect to `.harness/run/evidence/<name>.log`, run long ones with `Bash` `run_in_background` writing an exit file under `/tmp`, and wait inside the turn.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` — pass `model` explicitly on every dispatch; the Reviewer is always Capable; the Verifier runs at Capable.
- **Assumed criteria (Tier 3):** 11 (A-007). Tier 2 and below: A-001 to A-006, A-008.
