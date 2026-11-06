# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.

## Current

- **Stage:** build
- **Run Mode:** Autonomous
- **Loop Branch:** loop/kanso-curves (from `main` at `069b5c3`)
- **Next Phase:** none; Verifier next
- **DONE-candidate:** no
- **PARTIAL-candidate:** yes - pictures 99/100 (Lightness S-curve) differ from their baselines (the old ones show the D-004 bug, Reset missing) and are not on the owner's `## Baselines` list, so the wrapper cannot re-record them; `verifyRoborazziDebug` is red on them until the owner lists them. Criteria 15-17 and 22 await a person.

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | done | data/edit/edit curve model + EditTool/EditSettings/EditProcessor, EditPanel, EditToolRow, curvespanel/*, 9 tests, README, docs (24 files) | - |
| T-002 | done | FilterPreviewRenderer, ToolViewModel, 2 tests, README, edit.md | p2-build2.txt |
| T-003 | done | ToolUiState, ToolViewModel, ToolPanel, ToolScreen, EditPanel, curvespanel/*, tests, README, edit.md (15 files) | - |
| T-004 | done | CurveChannel, CurveAdjustments, EditCurves, CurveChips, CurveGraph, strings, tests, README, edit.md (12 files) | - |
| T-006 | done | CurveChips, CurveResetButton, CurvesPanel, strings_edit_curves.xml, CurveChipRowTest (new), README (6 files) | p5-build4.log |

## Assumptions

- PRD read as a new Curves tool after HSL with RGB/Red/Green/Blue/Lightness curves; put to the human in D-001, not assumed.
