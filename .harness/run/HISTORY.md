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
