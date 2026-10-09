# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.
> Execution history lives in HISTORY.md, not here — this file must not grow with the run.

## Current

- **Stage:** Phase 3 done (the last splits); PARTIAL-candidate recorded, the next invocation is the Verifier
- **Loop Branch:** loop/kanso-one-class-per-file (started from `4d5f239`)
- **Next Phase:** none (every task complete); the Verifier runs next
- **DONE-candidate:** no
- **PARTIAL-candidate:** yes (Autonomous mode, ENGINE §14.4): every task is complete and nothing is abandoned, but DoD 6 and 15 need a person, so the run cannot end `DONE`; the Verifier re-proves every `machine` criterion, drives every `machine-then-human` one, and reports `DONE_PARTIAL` unless it finds a gap it can close
- **Run Mode:** Autonomous

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete (Phase 1, attempt 1) | ui/home, ui/gallery, ui/settings UiState + ViewModel + Screen; two new tests | `evidence/p1-verify.log`; Phase 1 commit |
| T-002 | complete (Phase 1, attempt 1) | ui/camera types + ViewModel + Screen; ui/tool UiState + ViewModel + Screen | `evidence/p1-verify.log`; Phase 1 commit |
| T-003 | complete (Phase 1, attempt 1) | ui/saved/*; T/saved tests; T/screenshots/SavedScreenshotTest.kt | `evidence/p1-verify.log`; Phase 1 commit |
| T-004 | complete (Phase 2, attempt 1) | new T/OneTypePerFileTest.kt | `evidence/p2-verify.log`, `p2-new-tests-2.log`, mutation logs |
| T-005 | complete (Phase 2, attempt 1) | new T/ScreenStateTest.kt | `evidence/p2-verify.log`, `p2-new-tests-2.log`, mutation log |
| T-006 | complete (Phase 3, attempt 1) | 13 new + 7 edited main files; `Destinations.kt` removed | `evidence/p3-verify.log`, `p3-diff.patch`; Phase 3 commit |
| T-007 | complete (Phase 3, attempt 1) | 9 new + 7 edited test files | `evidence/p3-verify.log`, `p3-testnames-before/after.txt`; Phase 3 commit |

## Signed off

None yet. Criteria 6 and 15 need a person; Run 3's unsigned items stay in `.harness/ISSUES.md`.

## Assumptions

Autonomous mode: see `ASSUMPTIONS.md` (A-001 to A-012). None is Tier 3, so no criterion is assumed. A-004 to A-010 were applied in Phase 1 (`adbdbe0`); A-001 and A-011 in Phase 2 (`e50a0a5`); A-002, A-003 and A-012 in Phase 3 (the commit that follows `e50a0a5`).
