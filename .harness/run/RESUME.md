# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** Phase 3 complete at Iteration 4 (checkpoint `loop(phase-3)`); next is Phase 4. Autonomous mode: Loop Branch `loop/kanso-m3-expressive-blue`, base `76522d7`. D-001 answered.
- **Next Phase:** Phase 4 = three tasks (T-001 to T-007, T-009 and T-010 are complete; read only the task files below):
  - T-008 - scope: `navigation/KansoTransitions.kt` (new, top-level functions only; never write the annotation text `@Composable` in it), `NavTransitionTest.kt` (new) - Capable
  - T-011 - scope: new `a11y/AccessibilityWalkTest.kt`, `a11y/TouchTargetCheck.kt`, `theme/ThemeFollowTest.kt` (test files only) - Capable; its Notes list the Phase 3 facts (fixed-colour swatches, the Saved mark's place, which states need a paused clock)
  - T-012 - scope: `ui/settings/SettingsUiState.kt` (a defaulted field), `SettingsViewModel.kt`, `data/settings/SettingsRepository.kt`, one line of `SettingsScreen.kt`, `settings/ThemeSwatchTest.kt`, `settings/SettingsRepositoryTest.kt` (and `SettingsUiStateTest.kt` only if needed) - Capable (added from the Phase 3 review; the Custom swatch shows the last picked seed)
  - The Iteration wires after the Workers (PLAN.md, Phase Grouping): `KansoNavHost.kt` (hands the specs to `NavDisplay`: `transitionSpec`, `popTransitionSpec`, `predictivePopTransitionSpec`), `AndroidManifest.xml` (`android:enableOnBackInvokedCallback="true"`), `PreviewsRenderTest.kt` if a preview is added. Then three fresh full runs (`cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue`) to look for flakes. A defect T-011's tests find in a screen becomes a Tier-1 fix task (Phase 5).
  - Careful: `Espresso.pressBack()` tests (`NavigationTest`, `SavedNavigationTest`, `PhotoToToolNavigationTest`, `ThemeModeSettingsTest`) may react to the manifest flag (the Phase 1 spike says they pass unchanged); Home's text invariants and `home-tool-<TOOL_NAME>` tags and the other binding Constraints in PROJECT.md; a Worker holds no build capability, so a compile error in its tests is found by the Iteration's first combined run (do not count it as an attempt).
  - Later: the Verifier (PARTIAL-candidate in Autonomous mode, since criteria 13, 20, 21 and 30 need a person): before it, write the evidence files the DoD names (`expressive-scan.md`, `fixed-colours.md`, `baseline-diffs/before` and `after`, `head-badging.txt`, `head-testnames.txt`, `test-edits.md` complete, `symbols/` check), add the criterion 20 and 21 unsigned items to `.harness/ISSUES.md` (carry every earlier `- [ ]` item word for word), and re-run everything fresh.
- **Queued decisions:** 0.
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` (compare: `:app:compareRoborazziDebug`; record new/approved: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`) | the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (2 to 3 min warm, 7 min cold). Always under `timeout`, output to `.harness/run/evidence/`, begin with `cd /d/Valve/Personal/Kanso &&`. A background Gradle started with `( … ) &` is not tracked by the harness: wait for it in the same turn with a bounded `for i in $(seq 1 N); do [ -f /tmp/<name>.exit ] && break; sleep 15; done` loop (a bare `sleep` and a python heredoc both hang or are blocked here).
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` (dispatch every Worker with `model` set explicitly; the Reviewer and the Verifier are always `opus`)
