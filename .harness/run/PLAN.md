# PLAN

## Strategy

Option A (kept from the PRD: `Tool` still means five tools with one screen each): Edit stays inside `ToolScreen`/`ToolViewModel`/`ToolUiState`; the new Edit layout lives in new files that `ToolScreen` hands Edit to. Phases 1-4 are serial because every Edit feature runs through the same three files. Phase 5 pairs Result (new `ui/result/*`) with Done (`ui/tool/*`), disjoint, after the Iteration pre-wires the shared files. Phase 6 writes the record and the pictures. One checkpoint commit per Phase on `loop/kanso-real-editor` (A-001). README and the touched feature docs get a small true delta in each Phase's commit (CLAUDE.md), fully rewritten in Phase 6.

## Task Graph

- T-001 Edit layout: fixed photo, row, sheets, pill, compare (depends on: -) - tier: Capable
- T-002 Session settings, undo stack, one filter, Back asks (T-001) - Capable
- T-003 Live preview + parity test (T-002) - Capable
- T-004 Real cropper (T-003) - Capable
- T-005 Result screen, save flow moves (pre-wiring by Iteration) - Capable
- T-006 Done on all five tools (pre-wiring by Iteration) - Capable
- T-007 README, edit.md, CONTEXT.md (T-005, T-006) - Capable
- T-008 Other feature docs (T-005, T-006) - Capable
- T-009 New picture tests (T-001..T-006) - Fast
(Scopes and tiers: `TASKS/T-00x.md`. No task is Fast except T-009, bounded and mechanical; Capable elsewhere because each embeds an architecture decision.)

## Phase Grouping

| Phase | Tasks | Shared files the Iteration wires itself |
|---|---|---|
| 1 | T-001 | new drawables (crop, rotate, tune, filter, compare, undo icons from Material Symbols), `PreviewsRenderTest.requiredPreviewNames`, `PreviewCoverageTest`, `AccessibilityWalkTest`/`ThemeFollowTest` call sites, README delta |
| 2 | T-002 | PreviewsRenderTest names (dialog), README delta |
| 3 | T-003 | README delta |
| 4 | T-004 | `tool_edit_free_crop` baselines (approved), README delta |
| 5 | T-005, T-006 | `navigation/Result.kt` (replaces `Saved.kt`), `data/edit/RenderedPhotoStore.kt`, `di/AppModule.kt`, `KansoNavHost.kt`, `KansoTransitions.kt`, `AndroidManifest.xml` + `res/xml/file_paths.xml` (FileProvider), `strings*.xml` removals, `ScreenStateTest`, `PreviewsRenderTest`, `PreviewCoverageTest`, nav tests (`SavedNavigationTest`, `MyEditsViewerNavigationTest`, `HomeRecentTest`, `BackStackActionsTest`, `NavStateTest`, `NavigationTest`), `AccessibilityWalkTest`, `ThemeFollowTest`, all changed baselines |
| 6 | T-007, T-008, T-009 | `docs/adr/ADR-003-*.md`, `docs/features/my-edits.md`, recording baselines with the wrapper, `evidence/baseline-diffs/` |
| 7 | T-010 (4 slices), T-011 | before dispatch: `git mv` `res/values/strings_saved.xml` to `strings_result_mark.xml` and `strings_saved_layout.xml` to `strings_result_layout.xml`; after: `evidence/test-edits.md` for any test whose code changed |
| 8 | T-012 | none (comments and one doc sentence) |
| 9 | T-013 | none (README and edit.md sentences, three KDocs) |

## Known Risks

- Robolectric cannot do GPU/native graphics easily: the colour-parity test (DoD 17) may need `@GraphicsMode(NATIVE)`; unproven here.
- 32-bit A13 smoothness of the filter preview is unmeasurable without the device (DoD 20).
- Process death empties the hand-off store (A-004): Result shows Nothing to show.
- Roborazzi recording needs the wrapper; changed baselines beyond the two the PRD names are listed in DoD 22 for approval.
- Phase 5 must keep one green build across two tasks that split the save flow; the Iteration builds once after both.
