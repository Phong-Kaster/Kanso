# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.

## Current

- **Stage:** build (Verifier, Iteration 5, found a DoD 26 doc gap; T-005 filed)
- **Loop Branch:** loop/kanso-curves-meitu
- **Run Mode:** Autonomous
- **Next Phase:** 4 - T-005
- **DONE-candidate:** no
- **PARTIAL-candidate:** no

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete | Lightness removed (10 files, 2 slices) + strings/docs wiring | evidence/phase1-build.txt |
| T-002 | complete | `ToneCurve.kt`, CurveFormula/CurveParity/CurvesTool tests | - |
| T-003 | complete | curvespanel graph/chips/reset/panel/dot, strings, CurveChipRow + contrast tests (2 slices) | evidence/phase3-*.txt |
| T-004 | complete | CurveGraph, CurvesPanel, CurveDeleteButton, strings, CurvesSheet/contrast/screenshot tests (2 slices) | evidence/phase3-*.txt |
| T-005 | pending | `README.md`, `docs/features/edit.md` | - |

## Assumptions

- MAX_POINTS = 10 and MIN_GAP = 0.02 stay (not in the PRD; listed as a DoD question).
- New Curves files stay in `curvespanel/`, not a `components/` restructure (DoD question).

## Verifier, Iteration 5 (2026-11-11)

- Fresh `assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug`: exit 0, 1297 tests, 0 failures (evidence/verify-gradle.txt); the only lint hit in a changed file (PluralsCandidate, `edit_curves_graph_description`) predates the run.
- Removals R1-R10 and DoD 25 greps clean; DoD 13 (curves after HSL in EditProcessor) holds.
- Gap: DoD 26 - README Curves row (:102), README :304, docs/features/edit.md :548 describe the old touch model -> T-005.
- DoD 21/22 not driven: the gap was found first, and the next Verifier drives them on the final tree.
