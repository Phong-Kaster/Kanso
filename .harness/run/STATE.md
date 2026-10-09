# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.
> Execution history lives in HISTORY.md, not here — this file must not grow with the run.

## Current

- **Stage:** Phase 1 done (the six screens); Phase 2 next
- **Loop Branch:** loop/kanso-one-class-per-file (started from `4d5f239`)
- **Next Phase:** 2 - T-004, T-005
- **DONE-candidate:** no
- **Run Mode:** Autonomous

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete (Phase 1, attempt 1) | ui/home, ui/gallery, ui/settings UiState + ViewModel + Screen; two new tests | `evidence/p1-verify.log`; Phase 1 commit |
| T-002 | complete (Phase 1, attempt 1) | ui/camera types + ViewModel + Screen; ui/tool UiState + ViewModel + Screen | `evidence/p1-verify.log`; Phase 1 commit |
| T-003 | complete (Phase 1, attempt 1) | ui/saved/*; T/saved tests; T/screenshots/SavedScreenshotTest.kt | `evidence/p1-verify.log`; Phase 1 commit |
| T-004 | pending (after T-001..T-003, now met) | new T/OneTypePerFileTest.kt | - |
| T-005 | pending (after T-001..T-003, now met) | new T/ScreenStateTest.kt | - |
| T-006 | pending (after T-004) | main-code splits, navigation keys | - |
| T-007 | pending (after T-004) | test-code splits | - |

## Signed off

None yet. Criteria 6 and 15 need a person; Run 3's unsigned items stay in `.harness/ISSUES.md`.

## Assumptions

Autonomous mode: see `ASSUMPTIONS.md` (A-001 to A-012). None is Tier 3, so no criterion is assumed. A-004 to A-010 were applied in Phase 1.
