# ASSUMPTIONS

> Autonomous mode only (ENGINE.md §14.1, ADR-027). Every decision the engine made alone, where a
> Collaborative run would have stopped to ask. The Runtime renders this file into the This run tab of
> `FOREMAN.html` after every iteration, so each entry must make sense to someone who has read nothing else.
>
> To overturn one: answer its id in `.harness/run/DECISIONS.md` and re-run. A human answer always
> outranks an assumption — the engine reverts what depended on it. Or run the revert command below
> yourself, on the Loop Branch, after the run has stopped.

---

## A-001 - "Named after it" is enforced, not only "one type per file"

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** pending - the Iteration after 1 fills in the commit SHA
- **Revert:** `git revert <sha>`

### Question

The PRD counts 8 main and 6 test files that hold more than one type. `CLAUDE.md`, which the PRD says applies in full, also says every type lives in its own file "named after it". Does this run also fix single-type files whose name differs from the type?

### Options Considered

1. Count only files with two or more types - consequences: `PermissionGate.kt` (holds `PermissionAccess`), `UprightJpeg.kt` (`FullSizeDecoder`), `SettingsStore.kt` (`RetryingMoveFileSystem`), `SavedScreen.kt` (`SavedPhoto`) and the test file `ScreenPixels.kt` (`ScreenSample`) stay as they are; the guard test would have to accept a misnamed file.
2. Enforce the name too - consequences: those five files are split or renamed; the guard test fails on a misnamed file.

### Taken, and why

Option 2. `CLAUDE.md` is the owner's standing rule and the PRD says it applies in full; the PRD's counts are examples ("for example"), not a limit.

---

## A-002 - Private top-level types count; the one in `SettingsStore.kt` becomes `internal`

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** pending
- **Revert:** `git revert <sha>`

### Question

`RetryingMoveFileSystem` is a `private class` in `SettingsStore.kt`. Does the one-class-per-file rule cover it, given that moving it forces a visibility change?

### Options Considered

1. Exempt `private` top-level types - consequences: a loophole the guard test would have to special-case.
2. Count them and widen to `internal` - consequences: a one-word visibility change, no behaviour change.

### Taken, and why

Option 2: `CLAUDE.md` says "every top-level class", with no visibility exception; `internal` keeps it out of the public API.

---

## A-003 - Navigation keys become six files, not one sealed type

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** pending
- **Revert:** `git revert <sha>`

### Question

The PRD says the navigation keys "may live as cases of one sealed type". Six small files, or one sealed type with nested cases?

### Options Considered

1. One sealed type - consequences: the keys' full class names change (`Destination.Home` …), `KansoNavHost.kt` and `ToolPhotoKeyTest` need edits, saved back stacks written by the old build no longer read, and whether Navigation 3 1.1.7 restores nested keys is unverified.
2. Six files in the same package - consequences: class names stay the same, so nothing else changes; the Navigation 3 explanation moves to `Home.kt`.

### Taken, and why

Option 2: the PRD says "may", not "must"; it needs no edit outside the six files and avoids the unverified serialisation question.

---

## A-004 - Tool's two state flows merge into one `ToolUiState`

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 2, the Phase 1 checkpoint (the next Iteration replaces this with its commit SHA)
- **Revert:** `git revert <sha>`

### Question

`ToolViewModel` exposes `state` (a sealed `ToolUiState`) and `saveState` (a sealed `SaveUiState`). The PRD wants each ViewModel to expose exactly one StateFlow of its UiState. How does Tool comply without changing behaviour?

### Options Considered

1. Keep `SaveUiState` as a second flow - consequences: breaks the PRD's "exactly one".
2. One `ToolUiState` data class holding `tool`, `photoUri`, the photo state and the save state, the two old sealed types nested inside as `PhotoState` and `SaveState` - consequences: one flow; no test refers to the old names, so renaming costs nothing; writes use `update {}` so a late decode cannot erase the save state.

### Taken, and why

Option 2: the only reading that satisfies "exactly one" and "UiStates move to their own files".

---

## A-005 - Camera's controller state merges into `CameraUiState`

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 2, the Phase 1 checkpoint (the next Iteration replaces this with its commit SHA)
- **Revert:** `git revert <sha>`

### Question

`CameraViewModel` exposes `cameraState` (the controller's state) and `state` (its own). The PRD does not mention Camera's two flows, but demands exactly one per ViewModel.

### Options Considered

1. Read the rule as allowing other StateFlows - consequences: Camera keeps two; the guard test cannot enforce "exactly one".
2. Fold `CameraState` into `CameraUiState` as a field, fed by a collector of `controller.state`; every guard keeps reading the source it reads today - consequences: one flow; a small timing risk the task spells out.

### Taken, and why

Option 2: consistent with Gallery and Tool, and with the PRD's literal text.

---

## A-006 - One-shot events and plain read-only values may sit beside the one StateFlow

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 2, the Phase 1 checkpoint (the next Iteration replaces this with its commit SHA)
- **Revert:** `git revert <sha>`

### Question

Tool's `savedEvents: Flow<String>` (a Channel) announces "saved" once. Does "exactly one StateFlow" forbid it?

### Options Considered

1. Fold it into the state - consequences: after a rotation the screen would react to the saved photo a second time.
2. Keep it: it is a one-shot event, not a StateFlow - consequences: the guard test counts only public methods returning `StateFlow`.

### Taken, and why

Option 2. Plain read-only values (`tool`, `photoUri`) may stay too where something outside the screen still reads them.

---

## A-007 - A `…Content` may take more than the UiState and callbacks

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 2, the Phase 1 checkpoint (the next Iteration replaces this with its commit SHA)
- **Revert:** `git revert <sha>`

### Question

The PRD says the stateless `…Content` takes the UiState and its callbacks. Gallery's `access`/`answered` (the permission answer), Settings' `wallpaperAvailable` (an SDK check), Saved's `sharing`, and the picture slots are not produced by a ViewModel.

### Options Considered

1. Force every parameter into the UiState - consequences: a ViewModel would have to know the permission answer and the Activity, a design change the PRD does not ask for.
2. Rule: whatever the ViewModel holds goes into the UiState; the drawing part may also take callbacks, a modifier, slots and facts only it can know - consequences: the UiState stays an honest snapshot of the ViewModel.

### Taken, and why

Option 2. Gallery's tool name, which the ViewModel does hold, moves into `GalleryUiState`.

---

## A-008 - Saved's ViewModel only loads the photo; the share block stays in the screen

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 2, the Phase 1 checkpoint (the next Iteration replaces this with its commit SHA)
- **Revert:** `git revert <sha>`

### Question

The PRD moves "the loading" of Saved into a ViewModel. What about the share lookup, the `sharing` flag, and the ViewModel's constructor?

### Options Considered

1. Move the share logic too - consequences: `startActivity` needs an Activity context, and the test asserts the intent's package, component and flags; forcing `FLAG_ACTIVITY_NEW_TASK` would weaken it.
2. Move only the decode - consequences: smaller change; the share path is byte-identical.

### Taken, and why

Option 2. The ViewModel is `SavedViewModel(photoUri, loader, probe)`: no `tool` (nothing on the screen reads it; the unused `SavedScreen` parameter is dropped), and the probe like every other entry-scoped screen ViewModel except Settings. It is built by the navigation entry through Koin with `parametersOf(key.photoUri)`.

---

## A-009 - The UiState pattern is explained once, in `HomeUiState.kt`

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 2, the Phase 1 checkpoint (the next Iteration replaces this with its commit SHA)
- **Revert:** `git revert <sha>`

### Question

The PRD wants the UiState pattern explained "where it is first used". Home or Gallery (where a UiState first existed)?

### Options Considered

1. Gallery - consequences: the oldest UiState, but Home is where a reader starts.
2. Home - consequences: Home is the first screen and gets a UiState in this run; the other five `…UiState.kt` files point back to it.

### Taken, and why

Option 2. Criterion 15 has a person read the three Home files.

---

## A-010 - Every ViewModel's flow is called `state`; `GalleryUiState` gains a `tool` field

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 2, the Phase 1 checkpoint (the next Iteration replaces this with its commit SHA)
- **Revert:** `git revert <sha>`

### Question

What is the one public flow called, and where does the Gallery's tool name come from?

### Options Considered

1. `uiState` everywhere - consequences: renames `GalleryViewModel.state`, which `GalleryGridTest` and `GalleryGridScreenshotTest` read.
2. `state` everywhere - consequences: matches Gallery, Camera and Tool today; no test changes.

### Taken, and why

Option 2. `GalleryUiState` gets one extra field, `tool`, so `GalleryContent` takes the name from the state (A-007).

---

## A-011 - "Fails the build" means failing the unit tests; the guard ships with an allow-list; KDoc coverage keeps scanning main

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** pending
- **Revert:** `git revert <sha>`

### Question

PRD item 4 wants a later run that puts two classes in one file to "fail the build". How, and over which files?

### Options Considered

1. A Gradle task that fails `assembleDebug` - consequences: a build-script change the PRD does not ask for and DoD 9 forbids.
2. A unit test (`OneTypePerFileTest`), run by `testDebugUnitTest`, `check` and `build` - consequences: `assembleDebug` alone does not run it.

### Taken, and why

Option 2, following the repository's existing scanning tests. To prove it on the real tree it ships in Phase 2 with a path-keyed allow-list of the 14 files still breaking the rule, plus a stale-entry check; Phase 3 splits them and empties the list. `KdocCoverageTest` stays unchanged (DoD 3c forbids loosening it and extending it to the test tree would add risk); the Reviewer reads the new and moved test files for KDoc.

---

## A-012 - Types nested inside another type stay nested, in tests too

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** pending
- **Revert:** `git revert <sha>`

### Question

`CLAUDE.md` says a test's own fake or data class gets its own file; the PRD says "types nested inside another stay with it". `CustomColourDebounceTest.CountingPreferencesStore` and `FakeMediaProvider.Row` are nested.

### Options Considered

1. Move nested test helpers out - consequences: more churn, and the PRD's count of 6 test files would be wrong.
2. Leave them nested - consequences: follows the PRD's sentence and its count.

### Taken, and why

Option 2.
