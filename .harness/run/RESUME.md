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

- **Stage:** The Verifier (Iteration 15) found one gap; all else re-proved (`evidence/v15-verifier.md`). **Next: Phase 8 = T-016** (Fast; no Worker: the Iteration edits the shared files itself). Scope: `app/src/test/java/com/phongkaster/kanso/previews/PreviewsRenderTest.kt` (`requiredPreviewNames` only: add `"ToolNarrowPhotoPreview"`, `"PhotoViewerPreview"`, `"HomeEmptyPreview"`, `"HomeLoadingPreview"` after `"MyEditsLoadingPreview"`, about line 266) and `.harness/run/evidence/test-edits.md` (one reason line). Run the whole set; a review of a four-string list edit can be the Iteration's own read of the diff. Then record the PARTIAL-candidate again (A-025, A-026, A-027 still stand) and the Verifier re-runs: it should also look at all 41 `baseline-diffs/after/` pictures and fill `index.md`'s "Verifier look" column ("did not show a defect in ..."), and drive the Galaxy A13 if `-Op focus -Serial R58W1120QVA` names Kanso (two devices are attached: always pass `-Serial`; the emulator `emulator-5554` has this build installed).
- **DoD text the Phase cites:** DoD 36: "`PreviewsRenderTest.requiredPreviewNames` holds every base name except those `test-edits.md` lists as removed with an R-number, plus one preview per new screen state: `KansoTabBarPreview`, `KansoTabShellPreview`, `MyEditsPreview`, `MyEditsEmptyPreview`, `MyEditsLoadingPreview`, `ToolNarrowPhotoPreview`, `PhotoViewerPreview`, `HomeEmptyPreview`, `HomeLoadingPreview`." and "No preview shows `LoadingIndicator` or a wavy indicator."
- **Open ISSUES items naming these files:** none.
- **Queued decisions:** 0 (D-001 answered).
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` (compare: `:app:compareRoborazziDebug`; record new/approved: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`) | the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (about 3 to 8 min). Always under `timeout`, output to `.harness/run/evidence/`, begin with `cd /d/Valve/Personal/Kanso &&`. A shell here rejects some multi-line `cat <<EOF` heredocs: write files with the Write tool; a `( ... ) &` subshell with `run_in_background` returns at once, so wait for its exit file in a bounded foreground loop.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` (dispatch every Worker with `model` set explicitly; the Reviewer and the Verifier are always `opus`)
- **Time note:** the Runtime kills an Iteration at 90 minutes; the whole set takes about 3.5 min warm. Iteration 12 found that the shell's `cat <<'EOF'` heredocs work here.
