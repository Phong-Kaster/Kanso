# AMENDMENTS

## Iteration 2 - D-001 answered (owner, 2026-11-06)

- DoD approved as written, with the engine's reading: (a) Compare pinned at the right on layer 2; (b) tapping Colour only switches the row; (c) system Back keeps today's behaviour on layer 2; (d) Back entry and slide become shared pieces used by Beautify too.
- Icon: Colour takes `ic_palette`; RGB gets Material Symbols Rounded `instant_mix` (`ic_instant_mix.xml`, `tint` attribute removed).
- Pictures: the 43 listed under `## Baselines` in DECISIONS.md may be re-recorded; any other changed picture is a defect.
- Capabilities: Gradle assemble/testDebugUnitTest/lintDebug (permanent) and the one-file `instant_mix` download (goal) granted. Run Mode switched to Autonomous.
- T-001 unblocked.

## Iteration 2 - Tier 1: scope grown after review

Review found `ToolRowEntry`/`CompareEntry` shared composables inside `EditToolRow.kt` (house rule: one composable per file) and stale text in `CONTEXT.md`. T-001's scope gained `ToolRowEntry.kt`, `CompareEntry.kt`, `GroupBackEntry.kt` (KDoc) and `CONTEXT.md`. PRD, DoD and architecture unchanged.
