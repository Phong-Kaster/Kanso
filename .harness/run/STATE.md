# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.

## Current

- **Stage:** bootstrap (awaiting D-001)
- **Run Mode:** Collaborative
- **Loop Branch:** loop/kanso-curves (from `main` at `069b5c3`)
- **Next Phase:** Phase 1 — Wiring (`ic_show_chart.xml`, ADR-009), then T-001
- **DONE-candidate:** no
- **PARTIAL-candidate:** no

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | deferred (D-001) | data/edit/edit curve model + EditTool/EditSettings/EditProcessor, EditPanel, EditToolRow, curvespanel/*, 9 tests, README, docs (24 files) | - |
| T-002 | deferred (D-001) | FilterPreviewRenderer, ToolViewModel, 2 tests, README, edit.md | - |
| T-003 | deferred (D-001) | ToolUiState, ToolViewModel, ToolPanel, ToolScreen, EditPanel, curvespanel/*, tests, README, edit.md (15 files) | - |
| T-004 | deferred (D-001) | CurveChannel, CurveAdjustments, EditCurves, CurveChips, CurveGraph, strings, tests, README, edit.md (12 files) | - |
| T-005 | deferred (D-001; optional) | hue curves (13 files) | - |

## Assumptions

- PRD read as a new Curves tool after HSL with RGB/Red/Green/Blue/Lightness curves; put to the human in D-001, not assumed.
