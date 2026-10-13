# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.
>
> Keep it small. Every iteration pays to read it, and unlike ENGINE.md it is not served from a
> prompt cache. Never let history accumulate here.

- **Stage:** Iteration 16 finished T-016 (four preview names added to `requiredPreviewNames`, whole set green, `evidence/p8-run16.log`). PARTIAL-candidate recorded. **Next: the Verifier (Capable, `opus`)**: re-prove every machine criterion, look at all 41 `baseline-diffs/after/` pictures and fill `index.md`'s "Verifier look" column ("did not show a defect in ..."), drive the Galaxy A13 if `-Op focus -Serial R58W1120QVA` names Kanso (two devices attached: always pass `-Serial`; emulator `emulator-5554` has the build), then report `DONE_PARTIAL` (A-025, A-026, A-027 and unsigned criteria stand) unless a gap is found.
- **DoD text the Phase cites:** none (no Phase pending).
- **Open ISSUES items naming these files:** none.
- **Queued decisions:** 0 (D-001 answered).
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` (compare: `:app:compareRoborazziDebug`; record new/approved: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`) | the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (about 3 to 8 min). Always under `timeout`, output to `.harness/run/evidence/`, begin with `cd /d/Valve/Personal/Kanso &&`. A shell here rejects some multi-line `cat <<EOF` heredocs: write files with the Write tool; a `( ... ) &` subshell with `run_in_background` returns at once, so wait for its exit file in a bounded foreground loop.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` (dispatch every Worker with `model` set explicitly; the Reviewer and the Verifier are always `opus`)
- **Time note:** the Runtime kills an Iteration at 90 minutes; the whole set takes about 3.5 min warm. Iteration 12 found that the shell's `cat <<'EOF'` heredocs work here.
