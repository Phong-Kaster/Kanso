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
