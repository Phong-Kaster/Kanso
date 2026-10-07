# HISTORY

> Append-only audit log. Never read during Orient.

### Iteration 1 - 2026-10-06 (bootstrap, Run 2)
- **Phase:** none
- Attempted: read PRD, classified repository (product), fan-out analysis (DoD proposal, decomposition, critique), wrote DoD (37 criteria + 3 Removals), PLAN, 9 tasks, ASSUMPTIONS A-001..A-006, D-001.
- Learned: `MediaRepository` is synchronous with no interface (needs an app-side wrapper); approved baselines can only change through the human's `## Baselines` listing.
- Reconciled: baseline dependency recorded as A-006.

## Archived Decisions

### Iteration 2 - 2026-10-06
- **Phase:** 1 (T-001, T-002, T-003)
- Attempted: consumed D-001; moved BackArrowIcon to ui/common; strings, manifest read permissions, Gallery key, nav wiring, Koin binding; removed placeholder screenshot tests + pictures 7-9; 3 Workers (T-001 Fast, T-002/T-003 Capable); reviewer; recorded 12_/13_.
- Learned: backgroundScope is skipped by advanceUntilIdle; lint forbids constructing a ViewModel inside a composable; ThemeColourSettingsTest wallpaper test flakes once with AccessDeniedException on the DataStore rename (Windows file lock), passes on rerun.
- Reconciled: all fixed in-iteration; flake recorded in PROJECT.md.

### Iteration 3 - 2026-10-06
- **Phase:** 2 (T-004, T-005, T-010)
- Attempted: wired Coil 2.7.0 + ExifInterface (already in catalog), `ToolPhoto` key, strings, Koin (`PhotoSource`, `PhotoLoader`, ToolViewModel), nav entry; 3 Capable Workers; fixed old gallery tests' signatures; recorded 10_/11_/16_/17_ and (approved, D-001) re-recorded 6_; reviewer; added `PhotoToToolNavigationTest`.
- Learned: Compose `waitUntil` does not run what a background read posts to the main looper under Robolectric: idle the main looper inside the condition. Workers' first-try code compiled.
- Reconciled: reviewer criterion-3 gap closed by a nav-host test; EXIF-read failure and stale-thumbnail cache key fixed; recreate test on a copy of the host and rotation re-decode noted in ISSUES.

### Iteration 4 - 2026-10-06
- **Phase:** 3 (T-006, T-007)
- Attempted: CameraX 1.6.2 in catalog/build, manifest (CAMERA, WRITE<=28, uses-feature), `Camera` key + Home route, deleted Placeholder (R1, R2), `PhotoSaver` contract, strings, Koin; 2 Capable Workers; updated old tool tests for `saver`/`onSaved` and Save enabled; recorded 14_/15_, re-recorded 16_/17_ (A-007); reviewer.
- Learned: the whole unit-test JVM ran out of heap once camera/save tests joined (exit 1 / apparent hang): test heap now 3g with a fork every 40 classes. `RecordingRegistry` casts to Array, so single-permission requests need `SinglePermissionRegistry` (T/save).
- Reconciled: kept-shot cleanup and publish-row check fixed from review; file:// Uri on API 27-28 passed to T-009 as a note.

### Iteration 5 - 2026-10-07
- **Phase:** 4 (T-008, T-009)
- Attempted: Saved key, strings and nav wiring (Saved replaces the stack above Home; Edit another -> [Home, Gallery]); 2 Capable Workers in parallel (camera controls + gallery button; Saved screen + share); gallery camera button wired; recorded 10-13 (approved in D-001 baselines) and new 18/19 after viewing them; SavedNavigationTest added after review.
- Learned: `CameraViewModel.retake` raced its own test (state update before file delete): file now deleted first. Reviewer found criteria 6 (gallery path) and 15 (back stack) unproven through the real nav host: added SavedNavigationTest (4 tests).
- Reconciled: black viewfinder (`Color.Black`, Phase 3) and `onShare = {}` / default `onOpenCamera` recorded in ISSUES, not fixed.

## Iteration 6 - Verifier

Re-ran assemble, unit tests, Roborazzi verify, lint (all exit 0); checked reference/, manifest, dependencies, removals. No emulator attached (only a real phone), so machine-then-human criteria were not driven. Reported DONE_PARTIAL.
