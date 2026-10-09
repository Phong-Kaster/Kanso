# PLAN

> Machine-owned execution strategy. The human never reviews this file - only the Definition of Done.
> Evolves through Tier-1 amendments (logged in AMENDMENTS.md) and Tier-2 queued decisions.

## Strategy

Run 4 brings the code of Runs 1-3 up to CLAUDE.md's "one class per file, one UiState per screen" without changing a pixel or a
behaviour. Order of attack, chosen so that the checker can be proved red on the real tree and so that every Phase builds:

1. **Phase 1 - the six screens.** Each screen gets `…UiState`, one `state` StateFlow and a `…Content` that takes the UiState; Saved gets a
   ViewModel. These are the only tasks with behaviour risk (merging two flows into one on Camera, Tool, Settings; moving Saved's decode), so
   they go first and in parallel; their scopes are disjoint. The Iteration wires the two shared files itself.
2. **Phase 2 - the two guards.** `OneTypePerFileTest` ships with a path-keyed allow-list of the 14 files that still break the rule, plus a
   stale-entry check (a listed file that no longer breaks the rule fails the test). Because every listed file really does violate, the green
   run on the real tree is the proof that the scanner detects violations. `ScreenStateTest` proves "exactly one StateFlow of its UiState" by
   Java reflection. They depend on Phase 1 only (the allow-list names what Phase 1 leaves behind) and write one new test file each.
3. **Phase 3 - the mechanical splits.** Main-side and test-side splits are word-for-word moves into new files in the same package (Fast tier).
   The Iteration then deletes `Destinations.kt`, empties `ALLOWED`, and updates `knowledge/PROJECT.md`.

Verification each Phase: build and test the combined tree once - `./gradlew :app:testDebugUnitTest`, `:app:verifyRoborazziDebug`,
`:app:lintDebug`, `:app:assembleDebug` - redirected to files under `.harness/run/evidence/`, read back in summary. Baselines are never
re-recorded (DoD 2); a failing screenshot test writes `<name>_compare.png`, which is opened before anything is changed.

## Task Graph

- T-001 - Home, Gallery, Settings draw from one UiState (depends on: -) - scope: `ui/home/*`, `ui/gallery/{GalleryUiState,GalleryViewModel,GalleryScreen}.kt`, `ui/settings/{SettingsUiState,SettingsViewModel,SettingsScreen}.kt`, new `T/home/HomeViewModelTest.kt`, `T/settings/SettingsUiStateTest.kt` - tier: Capable
- T-002 - Camera and Tool expose one UiState each (depends on: -) - scope: `ui/camera/{CameraStatus,CameraLens,FlashMode,CameraState,CameraUiState,CameraController,CameraViewModel,CameraScreen}.kt`, `ui/tool/{ToolUiState,ToolViewModel,ToolScreen}.kt` - tier: Capable
- T-003 - Saved gets SavedViewModel + SavedUiState (depends on: -) - scope: `ui/saved/*`, `T/saved/{SavedScreenTest,SavedViewModelTest}.kt`, `T/screenshots/SavedScreenshotTest.kt` - tier: Capable
- T-004 - `OneTypePerFileTest` with allow-list (depends on: T-001, T-002, T-003) - scope: new `T/OneTypePerFileTest.kt` - tier: Capable
- T-005 - `ScreenStateTest` (depends on: T-001, T-002, T-003) - scope: new `T/ScreenStateTest.kt` - tier: Capable
- T-006 - main-code splits, six NavKey files (depends on: T-004) - scope: `data/media`, `data/photo`, `data/settings/{RetryingMoveFileSystem,SettingsStore}.kt`, `ui/permission/{PermissionAccess,PermissionGate}.kt`, `navigation/{Home,Camera,Gallery,ToolPhoto,Saved,Settings,Destinations}.kt` - tier: Fast
- T-007 - test-code splits (depends on: T-004) - scope: the nine new and seven edited test files listed in the task - tier: Fast

(M = `app/src/main/java/com/phongkaster/kanso`, T = `app/src/test/java/com/phongkaster/kanso`; full lists in `TASKS/`.)

## Phase Grouping

| Phase | Tasks | Shared files the Iteration wires itself |
|---|---|---|
| 1 | T-001, T-002, T-003 | `M/navigation/KansoNavHost.kt` (`KansoNavHostPreview` calls `HomeContent(state = HomeUiState(sampleHomeTools), …)`; `entry<Saved>` builds `SavedViewModel` with `koinViewModel` + `parametersOf(key.photoUri)` and calls `SavedScreen(viewModel, …)`), `M/di/AppModule.kt` (`viewModel { params -> SavedViewModel(photoUri = params.get(), loader = get(), probe = get()) }`) |
| 2 | T-004, T-005 | none; the Iteration fixes any stray offender the checker reports in a Phase 1 file |
| 3 | T-006, T-007 | `git rm` of `M/navigation/Destinations.kt`; `ALLOWED` emptied in `T/OneTypePerFileTest.kt`; `.harness/knowledge/PROJECT.md` (fake locations, Saved screen's loader) |

Phase scopes are pairwise disjoint (checked against the critique, 2026-10-09). Workers hold no git, build or test capability.

## Known Risks

- **Camera merge timing.** A merged copy read right after a shot or a zoom can lag on a device while a Robolectric test passes. T-002 spells out which source each guard reads.
- **Tool merge.** A late decode must not erase the save state (`update { }`); `ToolScreenTest` asserts the photo is asked for exactly once.
- **Settings `combine`.** The first value must still be `System` plus the phone's default colour with `Eagerly`; the debounce code is untouched. Watch `ThemeColourRestartTest`, `ThemeModeRestartTest`, `CustomColourDebounceTest` and the DataStore rename flake (known, fixed in Run 3, T-010).
- **Saved decode in a ViewModel.** `SavedScreenTest`/`SavedScreenshotTest` wait without `idle()` today; T-003 adds it. No dispatcher hop in `load`.
- **Lint `ViewModelConstructorInComposable`.** Build every ViewModel before `setContent`, in tests and in code.
- **`@Composable` text rule** (`PreviewsRenderTest.kt:97-98`): a new non-composable main file that mentions the annotation in KDoc fails `every_file_with_a_composable_has_a_preview`. In every main-code brief.
- **Phase 1 Workers adding a top-level type** the checker then flags in a file no Phase 2 task may touch: in every brief, and the Iteration greps for it before Phase 2.
- **Roborazzi byte-for-byte.** Only how state reaches `…Content` may change; any layout change moves a pixel and is a defect here, never an approval to ask for.
- **Workers cannot delete files**; the Iteration deletes `Destinations.kt` (and any other file a task empties).
- **Full runs are slow** (tens of minutes with Roborazzi and Robolectric): run Gradle in the background redirected to a file and wait inside the turn; a hang is a failure (POLICIES, Evidence).
