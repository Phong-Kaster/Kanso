# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** Phase 4 complete at Iteration 5 (checkpoint `loop(phase-4)`); next is Phase 5. Autonomous mode: Loop Branch `loop/kanso-m3-expressive-blue`, base `76522d7`. D-001 answered.
- **Next Phase:** Phase 5 = three tasks, disjoint scopes, all Capable (T-001 to T-012 are complete; read only these task files):
  - T-013 - scope: `app/src/test/java/com/phongkaster/kanso/tool/ToolSelectionAnimationTest.kt` (one test added; DoD 13's "Remove animations" pass, modelled on `NavTransitionTest.with_animations_removed_a_push_finishes_at_once_if_the_host_carries_the_setting`)
  - T-014 - scope: `app/src/main/java/com/phongkaster/kanso/navigation/KansoTransitions.kt` (only `kansoPredictivePopTransition` and its KDoc: the leaving screen stays opaque, slide only; never write the annotation text `@Composable` in it)
  - T-015 - scope: `app/src/test/java/com/phongkaster/kanso/a11y/AccessibilityWalkTest.kt` (the `camera refused` state through the real `CameraScreen`, as `ThemeFollowTest.checkCameraRefused` does)
  - The Iteration wires nothing (no shared file); a Worker holds no build capability, so a compile error in its test is found by the Iteration's first combined run (not an attempt). Briefs: tell each Worker to read `.harness/knowledge/PROJECT.md` in full (every "Never" in it, wherever it sits, is binding) and quote the ones that bite; save each Worker's report to `evidence/phase5-reports/` as it returns (S-006). After the review, three fresh full runs.
  - Then the Verifier (PARTIAL-candidate in Autonomous mode, since criteria 13, 20, 21 and 30 need a person): before it, write the evidence files the DoD names (`expressive-scan.md`, `fixed-colours.md` (the grep finds `PickerSurfaceColor` and lines inside `sample…` declarations of `CameraScreen.kt`), `baseline-diffs/before` and `after`, `head-badging.txt`, `head-testnames.txt`, `test-edits.md` complete (Phase 4 section written), `symbols/` check), add the criterion 20 and 21 unsigned items to `.harness/ISSUES.md` (carry every earlier `- [ ]` item word for word: 21 exist), and re-run everything fresh. Say aloud in the Verifier's report: the touch-target size half cannot fail for a tappable control (Compose widens it), "one frame" was two (A-014), "Remove animations" and the gesture preview are "not driven by machine".
- **Queued decisions:** 0.
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` (compare: `:app:compareRoborazziDebug`; record new/approved: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`) | the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (2.5 to 3 min warm, 7 min cold; 363 tests at Phase 4). Always under `timeout`, output to `.harness/run/evidence/`, begin with `cd /d/Valve/Personal/Kanso &&`. A background Gradle started with `( … ) &` is not tracked by the harness (its notification fires when the launcher shell exits): wait for it in the same turn with a bounded `for i in $(seq 1 N); do [ -f /tmp/<name>.exit ] && break; sleep 15; done` loop (a bare `sleep` is blocked, and a python heredoc hangs).
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` (dispatch every Worker with `model` set explicitly; the Reviewer and the Verifier are always `opus`)
