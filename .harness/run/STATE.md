# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.
> Execution history lives in HISTORY.md, not here — this file must not grow with the run.

## Current

- **Stage:** executing (Phase 1 complete; D-001 answered in Iteration 2)
- **Loop Branch:** loop/kanso-agp9-toolchain (started from `76ecb97`)
- **Next Phase:** 2 - T-002 (Compose 1.12.1, Navigation 3 1.2.0, lifecycle 2.11.0, Kotlin 2.4.21)
- **DONE-candidate:** no
- **Run Mode:** Autonomous

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete (Iteration 2, `loop(phase-1)`; 2 of 3 attempts: attempt 1 failed at the app's manifest merge) | wrapper, catalog, root/app/`core/*`/`opencv` build scripts, `gradle.properties` | `evidence/phase1-*.log`, `phase1-apk-vs-base.txt`; 231 tests, 16/16 pictures unchanged, lint, debug and release green, native libraries identical to base |
| T-002 | pending (executable; next Phase) | catalog, `app/build.gradle.kts`, `app/src` only as forced, baselines through the wrapper | - |
| T-003 | pending (after T-002) | catalog, `app/build.gradle.kts`, new `ExpressiveApiTest.kt`, baselines through the wrapper | - |

## Signed off

None yet. Criteria 3 and 7 need a person; Runs 1-4's unsigned items stay in `.harness/ISSUES.md`.

## Assumptions

Autonomous mode: see `ASSUMPTIONS.md` (A-001 to A-008).

- **Assumed criteria (Tier 3):** **11** (A-007): the four empty wrapper modules `core/editimage`, `overlaybrush`, `imageprocessor` and `localml` got their own namespaces in their build scripts, where DoD 11's sentence says a namespace shared by a prebuilt library and its module makes the criterion "unmet and reported". Every proof command of criterion 11 passes (build both variants, no flag, engine source untouched), but the run cannot end `DONE` while this stands.
- Tier 2 and below: A-001 to A-006 (as drafted at bootstrap), A-008 (`core/localml` declares `litert-support-api` directly; DoD 14's coordinate check passes textually).
