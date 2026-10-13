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

- **Stage:** Phase 7 done (Iteration 14: T-014 wrote `test-edits.md`, `orphans.sh`, `m3-scan.md`, `fixed-colours.md`; no code changed). **Next: the Verifier** (PARTIAL-candidate, section 14.4): re-prove every machine criterion, drive the A13, check the `KansoTabShell.kt:110` title; `evidence/home-reach.md` is not written, check whether a criterion needs it. (Superseded plan text follows; ignore:) Phase 7 = T-014 (Capable; scope: `.harness/run/evidence/{test-edits.md,orphans.sh,m3-scan.md,fixed-colours.md}`; the Iteration first writes `head-testnames.txt`, the `comm -23` output and the name-status list into `evidence/`; note that pictures 4, 5, 6, 24, 25 were re-recorded in Iteration 13, a baseline change, no test file edited). Then the Verifier again (drive the A13; check `KansoTabShell.kt:110` title).
- **DoD text the Phase cites:** see `DoD.md` criteria 16, 19, 23, 35, 36, R8 (Worker needs their text in its Brief; Grep by number).
- **Open ISSUES items naming these files:** none.
- **Queued decisions:** 0 (D-001 answered).
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` (compare: `:app:compareRoborazziDebug`; record new/approved: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`) | the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (about 3 to 8 min). Always under `timeout`, output to `.harness/run/evidence/`, begin with `cd /d/Valve/Personal/Kanso &&`. A shell here rejects some multi-line `cat <<EOF` heredocs: write files with the Write tool; a `( ... ) &` subshell with `run_in_background` returns at once, so wait for its exit file in a bounded foreground loop.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` (dispatch every Worker with `model` set explicitly; the Reviewer and the Verifier are always `opus`)
- **Time note:** the Runtime kills an Iteration at 90 minutes; the whole set takes about 3.5 min warm. Iteration 12 found that the shell's `cat <<'EOF'` heredocs work here.
