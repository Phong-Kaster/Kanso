# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.

## Current

- **Stage:** build (Verifier, Iteration 7, found a second DoD 26 doc gap; T-006 filed)
- **Loop Branch:** loop/kanso-curves-meitu
- **Run Mode:** Autonomous
- **Next Phase:** Phase 5 - T-006
- **DONE-candidate:** no
- **PARTIAL-candidate:** no

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete | Lightness removed (10 files, 2 slices) + strings/docs wiring | evidence/phase1-build.txt |
| T-002 | complete | `ToneCurve.kt`, CurveFormula/CurveParity/CurvesTool tests | - |
| T-003 | complete | curvespanel graph/chips/reset/panel/dot, strings, CurveChipRow + contrast tests (2 slices) | evidence/phase3-*.txt |
| T-004 | complete | CurveGraph, CurvesPanel, CurveDeleteButton, strings, CurvesSheet/contrast/screenshot tests (2 slices) | evidence/phase3-*.txt |
| T-005 | complete | `README.md`, `docs/features/edit.md` | grep clean |
| T-006 | pending | `README.md`, `docs/features/tool-screen.md`, `docs/adr/ADR-009-...md` | - |

## Assumptions

- MAX_POINTS = 10 and MIN_GAP = 0.02 stay (not in the PRD; listed as a DoD question).
- New Curves files stay in `curvespanel/`, not a `components/` restructure (DoD question).

## Verifier, Iteration 5 (2026-11-11)

- Fresh `assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug`: exit 0, 1297 tests, 0 failures (evidence/verify-gradle.txt); the only lint hit in a changed file (PluralsCandidate, `edit_curves_graph_description`) predates the run.
- Removals R1-R10 and DoD 25 greps clean; DoD 13 (curves after HSL in EditProcessor) holds.
- Gap: DoD 26 - README Curves row (:102), README :304, docs/features/edit.md :548 describe the old touch model -> T-005.
- DoD 21/22 not driven: the gap was found first, and the next Verifier drives them on the final tree.

## Verifier, Iteration 7 (2026-11-11)

- `assembleDebug testDebugUnitTest lintDebug verifyRoborazziDebug`: exit 0, 1297 tests, 0 failures (evidence/verify2-gradle.txt; most tasks up to date, since only docs changed after Iteration 5's full run).
- R1-R10 and DoD 25 greps clean; every `app/src/` path the README names exists; ADR-011 exists and the README ADR table lists it.
- Gap: DoD 26 - the Proof's grep finds "long press" about Curves in README.md:102 and docs/features/tool-screen.md:91 ("a long press does nothing"); ADR-009:30 Fritsch bullet marked only via its Status note -> T-006.
- DoD 21/22 not driven: the gap was found first; the next Verifier drives them (journeys/ has none for Run 14 yet).
