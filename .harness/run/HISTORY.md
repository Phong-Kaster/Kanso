# HISTORY

> Append-only audit log. Never read during Orient.

### Iteration 1 - 2026-11-06 (bootstrap)
- **Phase:** none (bootstrap)
- Attempted: read the one-line PRD; four Capable analysts (survey, DoD proposal, decomposition, critique with tiers and conflicts); wrote DoD, PLAN, T-001..T-005, D-001; created `loop/kanso-curves` from `main` `069b5c3`.
- Learned: the Edit sheet is 220 dp (run11-facts says 136); highest picture is 94; curves cannot join the GPU colour matrix, so they go on the CPU path after HSL; `EditHsl.hslToRgb` is private (so Lightness copies the conversion); `EditPanel` scrolls vertically for every tool but Stickers and HSL (Curves must be exempt); the Galaxy A13 is attached (`foreman-device.ps1 -Op devices`); no ledger grants Gradle assemble/test/lint.
- Reconciled: scope ambiguity and build capability queued as D-001 (blocks all tasks); hue curves made an optional, deletable DoD section with T-005.

## Archived Decisions

### Iteration 2 - 2026-11-06 (Phase 1, T-001)
- Consumed D-001 (approved, option 1, Autonomous). Removed T-005. Wiring Worker wrote the icon and ADR-009; baseline verify was green before any change.
- T-001 in 5 slices; fixed 7 test failures then 1 more; recorded pictures 95/96; pictures 63/64 now red (D-002).
- Learned: `KdocCoverageTest` demands KDoc on local functions; the tool row is lazy so tests must `scrollToolRowTo`; the scope checker cannot parse `...` shorthand scopes; Read refuses files over about 10 KB to a Worker, so the Constraints were split into 4 temp files.

### Iteration 3 - 2026-11-06 (Phase 2, T-002)
- Consumed D-002 (approved): pictures 63/64 re-recorded through the wrapper.
- T-002 done: curves in the live preview (FilterPreviewRenderer, ToolViewModel), CurvesPreviewTest, CurveParityTest, README/edit.md. Build, 1 unit-test run, lint and screenshot check green.
- Reviewer: fixed a crash in `ToneCurve.moved` (empty coerceIn range) with a test, README Curves row corrected, test-edits.md completed. A change making diagonal-point curves neutral was tried and reverted (it would skip Undo and disable Reset).
