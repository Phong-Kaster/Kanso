# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** escalated (bootstrap done at Iteration 1; D-001 pending). Autonomous mode: the Loop Branch is `loop/kanso-m3-expressive-blue`, base `76522d7`.
- **Next Phase:** Phase 1, once `D-001` is answered in `DECISIONS.md` (read it in §6.2; also read its `## Baselines` list)
  - T-001 - scope: `ui/theme/KansoTheme.kt`, `ui/theme/SeedColorScheme.kt`, `data/settings/ThemeColour.kt`, `data/settings/SettingsRepository.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsUiState.kt` (KDoc), `ui/settings/SettingsScreen.kt` (colour rows, previews), `res/values/strings_theme.xml` (new); tests `theme/KansoThemeColourTest`, `theme/KansoBlueThemeTest` (new), `theme/KansoBlueContrastTest` (new), `settings/ThemeColourStoreTest`, `settings/ThemeColourSettingsTest`, `settings/ThemeColourLabelsTest`, `settings/SettingsUiStateTest`, `settings/CustomColourDebounceTest`, `screenshots/ScreenshotTest` (chooseThemeMode) - Capable
  - T-002 - scope: `ui/tool/ToolScreen.kt`, `ToolUiState.kt`, `ToolViewModel.kt`, `res/values/strings_tool.xml` (new); tests `tool/ToolScreenTest`, `tool/ToolPhotoKeyTest`, `tool/ToolViewModelTest` (new), `tool/ToolSelectionAnimationTest` (new), `save/SaveButtonTest` (if signatures change), `screenshots/ToolScreenshotTest`, `navigation/SavedNavigationTest` - Capable
  - T-003 - scope: `ui/saved/SavedScreen.kt`, `res/values/strings_saved.xml` (new), `saved/SavedScreenTest` - **Fast**
  - Phase 1 preparation by the Iteration first (PLAN.md): fetch the Symbols drawables; `Tool.kt` `iconRes` and `editingTools`; API digest into `.harness/knowledge/api-digest.md`; the `enableOnBackInvokedCallback` spike; fix the stale PROJECT.md line. After the Workers: `KansoNavHost.kt` Saved key from the selected tool; both `colors.xml` from the failing `KansoBlueThemeTest`; `strings.xml` (`theme_colour_default` out); `PreviewsRenderTest.kt`; `compareRoborazziDebug`, then `foreman-record-baselines.ps1 -Module app`.
  - Later: Phase 2 = T-004 (Home), T-005 (Gallery), T-009 (editor loading/Save); Phase 3 = T-006 (Camera), T-007 (Settings), T-010 (Saved mark); Phase 4 = T-008 (motion), T-011 (cross-screen proofs); then the Verifier.
- **Queued decisions:** 1 (D-001, the DoD approval and the screenshot list) - blocks T-001 to T-011.
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` (compare: `:app:compareRoborazziDebug`; record new/approved: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`) | the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (5 to 8 min). Always under `timeout`, output to `.harness/run/evidence/`, begin with `cd /d/Valve/Personal/Kanso &&`.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` (dispatch every Worker with `model` set explicitly; the Reviewer and the Verifier are always `opus`)
