# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** the run ended `DONE_PARTIAL` (Iteration 9, the Verifier, 2026-10-10).
  - Every `machine` criterion was re-proved fresh on `a118896`, and two audits found no gap (`evidence/p9-verifier.md`).
  - Criteria 13, 20, 21 and 30 await a person: 18 unsigned items under "Awaiting a person, Run 6" in `.harness/ISSUES.md`.
  - Criterion 14 is assumed (A-015).
  - No Cleanup Commit. Loop Branch `loop/kanso-m3-expressive-blue`, base `76522d7`. D-001 is answered; nothing is queued.
- **Next Phase:** none. Nothing executable remains.
- **If invoked again:**
  - Consume `DECISIONS.md` and any ticks a person made in `.harness/ISSUES.md`.
  - A ticked item is recorded in `STATE.md` § Signed off, with its date.
  - A failed item (a line under it saying what the person saw) is a discovery: reconcile it into a task.
  - If every one of criteria 13, 20, 21 and 30 is signed, criterion 14 still blocks `DONE` (A-015, Tier 3) until a person accepts the straight-line slide or the DoD changes.
  - Never drop an unsigned item. 39 are open: 21 from earlier runs and 18 from Run 6.
- **Already weighed, not tasks:**
  - The audit points in `ISSUES.md` § "Run 6, other problems": DoD 6's tolerance of 8, DoD 8's inner ring, DoD 16's `background` = `surface`, DoD 2's `positionInRoot`, and the 1 500/3 000 ms loop caveat.
  - Iteration 7's lesser points.
  - A-016 (`minSdkVersion:'27'`).
- **Queued decisions:** 0.
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:**
  - Every Gradle command starts with `cd /d/Valve/Personal/Kanso &&`, runs under `timeout`, and sends its output to `.harness/run/evidence/`.
  - build: `./gradlew :app:assembleDebug`
  - test: `./gradlew :app:testDebugUnitTest`
  - lint: `./gradlew :app:lintDebug`
  - screenshots: `./gradlew :app:verifyRoborazziDebug`
    - compare: `:app:compareRoborazziDebug`
    - record new or approved pictures: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`
  - the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue`
    - 3 min 30 s after `:app:clean`; 368 tests.
  - Waiting on Gradle: run it with the Bash tool's `run_in_background`, write an exit file, and wait in a bounded loop.
  - Device: check `-Op focus` only after `assembleDebug`, because the wrapper refuses while no debug APK exists.
  - Shell traps:
    - Never `python -` or a python heredoc: it waits on stdin until the timeout. Write a script file and run `python -I <file>`.
    - `rm -rf` is refused: use `mktemp -d`. The symbols `check.sh` ends with `rm -rf`, so run a copy without that line.
  - The Runtime ends an invocation at 5400 s: checkpoint early.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`. Dispatch every Worker with `model` set explicitly. The Reviewer and the Verifier are always `opus`.
