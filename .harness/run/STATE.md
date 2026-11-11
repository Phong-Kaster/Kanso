# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.

## Current

- **Stage:** escalated (bootstrap; waiting for D-001)
- **Loop Branch:** loop/kanso-curves-meitu
- **Run Mode:** Collaborative
- **Next Phase:** Phase 1 - T-001
- **DONE-candidate:** no
- **PARTIAL-candidate:** no

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | deferred (D-001) | Curve model + chips/panel + 5 tests (2 slices) | - |
| T-002 | deferred (D-001) | `ToneCurve.kt`, CurveFormula/CurveParity/CurvesTool tests | - |
| T-003 | deferred (D-001) | curvespanel graph/chips/reset/panel/dot, strings, CurveChipRow + contrast tests (2 slices) | - |
| T-004 | deferred (D-001) | CurveGraph, CurvesPanel, CurveDeleteButton, strings, CurvesSheet/contrast/screenshot tests (2 slices) | - |

## Assumptions

- MAX_POINTS = 10 and MIN_GAP = 0.02 stay (not in the PRD; listed as a DoD question).
- New Curves files stay in `curvespanel/`, not a `components/` restructure (DoD question).
