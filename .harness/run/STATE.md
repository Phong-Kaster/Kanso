# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.

## Current

- **Stage:** build
- **Run Mode:** Autonomous
- **Loop Branch:** loop/kanso-curves (from `main` at `069b5c3`)
- **Next Phase:** Phase 2 - T-002 (live preview)
- **DONE-candidate:** no
- **PARTIAL-candidate:** no

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | done (63, 64 red: D-002) | data/edit/edit curve model + EditTool/EditSettings/EditProcessor, EditPanel, EditToolRow, curvespanel/*, 9 tests, README, docs (24 files) | - |
| T-002 | pending | FilterPreviewRenderer, ToolViewModel, 2 tests, README, edit.md | - |
| T-003 | pending | ToolUiState, ToolViewModel, ToolPanel, ToolScreen, EditPanel, curvespanel/*, tests, README, edit.md (15 files) | - |
| T-004 | pending | CurveChannel, CurveAdjustments, EditCurves, CurveChips, CurveGraph, strings, tests, README, edit.md (12 files) | - |

## Assumptions

- PRD read as a new Curves tool after HSL with RGB/Red/Green/Blue/Lightness curves; put to the human in D-001, not assumed.
