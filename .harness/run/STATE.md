# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.

## Current

- **Stage:** build
- **Run Mode:** Autonomous
- **Loop Branch:** loop/kanso-curves (from `main` at `069b5c3`)
- **Next Phase:** Phase 4 - T-004 (Lightness)
- **DONE-candidate:** no
- **PARTIAL-candidate:** no

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | done | data/edit/edit curve model + EditTool/EditSettings/EditProcessor, EditPanel, EditToolRow, curvespanel/*, 9 tests, README, docs (24 files) | - |
| T-002 | done | FilterPreviewRenderer, ToolViewModel, 2 tests, README, edit.md | p2-build2.txt |
| T-003 | done | ToolUiState, ToolViewModel, ToolPanel, ToolScreen, EditPanel, curvespanel/*, tests, README, edit.md (15 files) | - |
| T-004 | pending | CurveChannel, CurveAdjustments, EditCurves, CurveChips, CurveGraph, strings, tests, README, edit.md (12 files) | - |

## Assumptions

- PRD read as a new Curves tool after HSL with RGB/Red/Green/Blue/Lightness curves; put to the human in D-001, not assumed.
