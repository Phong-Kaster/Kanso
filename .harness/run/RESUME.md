# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** Phase 6 (Iteration 8) is built and checked: T-019, T-020 and T-021 are complete, and one fresh whole run of the finished tree is green (`evidence/p8-full-2.log`: exit 0 in 3 min 6 s, 368 tests with 0 failed, 0 errors and 0 skipped, `verifyRoborazziDebug` 24 of 24 unchanged, lint 0 errors, 54 warnings, 2 hints, the debug APK built). The **PARTIAL-candidate** is recorded again, so **the next invocation is the Verifier** (Capable). Loop Branch `loop/kanso-m3-expressive-blue`, base `76522d7`. D-001 is answered; nothing is queued.
- **Next Phase:** none. Nothing executable remains.
  - What Phase 6 changed (test files only, no app code, no picture): the `reference/` text left a KDoc sentence in `OneTypePerFileTest.kt` (DoD 29's search prints nothing); `ThemeSwatchTest.choose` waits for and asserts seven roles of the chosen scheme (DoD 2); the gradient layout test in `ToolScreenTest.kt` asserts the photo fills its area on one side (DoD 4).
  - `evidence/test-edits.md` has the Phase 6 row and the `ToolScreenTest.kt` line. `evidence/p8-diff.patch` is the diff the reviewer read.
- **For the next Verifier:**
  - First re-prove the three clauses Phase 6 closed, from the Proof's own words: DoD 29 (`git grep -n 'reference/' -- settings.gradle.kts app` prints nothing; `ALLOWED` is `emptySet()`), DoD 2 (read `ThemeSwatchTest.choose`: seven roles for each of the three choices it taps), DoD 4 (the gradient test asserts the fill on the 600x400 photo). Then audit whether any other Proof clause is looser than its test, as Iteration 7's audit did.
  - Re-run every proof. Files are already on disk, but re-run them, never trust them: the `p7-*` logs, `expressive-scan.md`, `fixed-colours.md`, `head-*.txt`, `baseline-diffs/before` and `after`, `symbols/check.sh`.
  - The test-name awk is in `p7-verifier.md` (criterion 25); it reproduces `base-testnames.txt` exactly. After Phase 6, `head-testnames.txt` is unchanged unless a test is renamed.
  - Drive criteria 13, 20, 21 and 30 with `foreman-device.ps1` only when `-Op focus` names Kanso. The owner's phone `R58W1120QVA` was in use at both of Iteration 7's checks; if a person is using it, write "not driven".
  - Then write the unsigned "Awaiting a person, Run 6" items in `.harness/ISSUES.md`:
    - criterion 13;
    - criterion 20, one item per group of pictures, naming `baseline-diffs/after` and `before`;
    - criterion 21, the nine steps;
    - criterion 30.
    - Each item leads with what was driven, from which state, and what is still unlooked-at.
  - **Carry all 21 earlier `- [ ]` items word for word** (Runs 1, 3, 4, 5 and the Galaxy A13 list).
- **Already weighed, not tasks:**
  - The audit's lesser points: the exact back stacks of 3, 5 and 7 are read from `KansoNavHost.kt`; criterion 22's names and file are checked by diff; `TouchTargetCheck` also counts `node.size`; the Settings icon check accepts any node that has no description; "inside the column" accepts any scrollable ancestor.
  - A-016: `aapt2` 36 prints DoD 24's `sdkVersion` line as `minSdkVersion:'27'`.
  - Say aloud in the final report:
    - The touch-target half of criterion 18 cannot fail.
    - "One frame" in criterion 14 is two (A-014).
    - "Remove animations" is not driven by machine.
    - The back gesture is machine-driven only as far as positions and pixels.
    - Criterion 14 is assumed (A-015). Ask the person, in criterion 21 step 8, to swipe from both edges and to release a short swipe early.
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
  - the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (2 min 40 s warm; 3 min 33 s after `:app:clean`; 368 tests).
  - Waiting on Gradle: run it with the Bash tool's `run_in_background` and poll an exit file with a bounded `for i in $(seq 1 N); do [ -f /tmp/<name>.exit ] && break; sleep 15; done`.
  - Shell traps:
    - Never `python -` or a python heredoc: it waits on stdin until the timeout.
    - `rm -rf` is refused: use `mktemp -d`.
  - The Runtime ends an invocation at 5400 s: checkpoint early.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`. Dispatch every Worker with `model` set explicitly. The Reviewer and the Verifier are always `opus`.
