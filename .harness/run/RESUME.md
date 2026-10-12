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

- **Stage:** escalated (bootstrap done at Iteration 1; D-001 pending). Autonomous mode: the Loop Branch is `loop/kanso-influx-layout`, base `a720960`.
- **Next Phase:** Phase 1, once `D-001` is answered in `DECISIONS.md` (read it in §6.2; also read its `## Baselines` list). Paths: `M` = `app/src/main/java/com/phongkaster/kanso/`, `T` = `app/src/test/java/com/phongkaster/kanso/`, `R` = `app/src/main/res/`; the exact scopes are in each `TASKS/T-00x.md`.
  - T-001 - the tab shell: `M navigation/{KansoTab,MyEdits,KansoTabBar,KansoTabShell,KansoTabTransitions,BackStackActions}.kt`, `R values/strings_tabs.xml`; Home's Camera/Settings entries removed, Settings without Back (`M ui/home/*`, `M ui/settings/SettingsScreen.kt`); tests `NavBarTest`, `NavBarColourTest`, `BackStackActionsTest`, `TabTaps`, edits to `NavigationTest`, `NavTransitionTest`, `CameraFlowTest`, `CameraPermissionTest`, six settings/picker tests, `ScreenshotTest`, `KansoBlueContrastTest` - Capable
  - T-002 - My edits: `M data/media/{SavedPhotoSource,MediaStoreSavedPhotoSource}.kt`, `M ui/myedits/*`, `M ui/common/SkeletonBlock.kt`, `PhotoThumbnail` made `internal` in `M ui/gallery/GalleryScreen.kt`, `R values/strings_myedits.xml`; tests `T myedits/*`, `MyEditsScreenshotTest` - Capable
  - T-003 - the tool screen: `M ui/tool/*`, `M ui/common/{BlurredBackdrop,RoundIconButton}.kt`, `R values/strings_tool_screen.xml`; tests `T tool/*`, `ToolScreenshotTest`, `SavedNavigationTest`, `PhotoToToolNavigationTest`, `HomeViewModelTest`, `HomeTaps`, `ReplaceNavigationTest` (a) - Capable
  - Phase 1 preparation by the Iteration first (PLAN.md): fetch the five Symbols drawables (`photo_library`, `arrow_forward`, `close`, `swap_horiz`, `hide_image`); write ADR-002 and the ADR-001 note; the `rememberInfiniteTransition` spike (answer into PROJECT.md). After the Workers: `KansoNavHost.kt` (the shell around `KansoNavDisplay`, tab fade metadata, `entry<MyEdits>`, Camera button, Replace, Saved key, clearance padding), `Tool.kt` (`editingTools` out), `AppModule.kt`, `PreviewsRenderTest.kt`, `ScreenStateTest.kt`, call sites and broken premises in `AccessibilityWalkTest.kt` and `ThemeFollowTest.kt`, `git rm ToolSelectionAnimationTest.kt`, then `compareRoborazziDebug` and `foreman-record-baselines.ps1 -Module app` for the Phase 1 pictures (1-6, 24, 25, 16, 17, 22, 23, new 28-33, 40-42).
  - Later: Phase 2 = T-004 (Saved, viewer), T-005 (Home carousel and pills), T-006 (gallery and camera, Fast); Phase 3 = T-007 (Settings rows), T-008 (Recent edits strip), T-009 (hex fix); Phase 4 = T-010, T-011 (cross-screen walks); then the Verifier.
- **Queued decisions:** 1 (D-001, the DoD approval and the screenshot list) - blocks T-001 to T-011.
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` (compare: `:app:compareRoborazziDebug`; record new/approved: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`) | the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (about 3 to 8 min). Always under `timeout`, output to `.harness/run/evidence/`, begin with `cd /d/Valve/Personal/Kanso &&`. A shell here rejects a multi-line `cat <<EOF` heredoc: write files with the Write tool.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` (dispatch every Worker with `model` set explicitly; the Reviewer and the Verifier are always `opus`)
