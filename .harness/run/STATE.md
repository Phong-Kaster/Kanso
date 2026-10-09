# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.
> Execution history lives in HISTORY.md, not here — this file must not grow with the run.

## Current

- **Stage:** verifying (Phases 1, 2 and 3 complete; no executable task remains; the Verifier is next)
- **Loop Branch:** loop/kanso-agp9-toolchain (started from `76ecb97`)
- **Next Phase:** none. The next invocation is the Verifier (ENGINE section 14.4): it re-proves every `machine` criterion, drives criterion 3 and reports `DONE_PARTIAL`.
- **DONE-candidate:** no
- **PARTIAL-candidate:** yes (Autonomous mode, ENGINE section 14.4; recorded in Iteration 4). What stops `DONE`: criterion 11 is assumed (A-007), and criteria 3 and 7 await a person's signature.
- **Run Mode:** Autonomous

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete (Iteration 2, `loop(phase-1)`; 2 of 3 attempts: attempt 1 failed at the app's manifest merge) | wrapper, catalog, root/app/`core/*`/`opencv` build scripts, `gradle.properties` | `evidence/phase1-*.log`, `phase1-apk-vs-base.txt`; 231 tests, 16/16 pictures unchanged, lint, debug and release green, native libraries identical to base |
| T-002 | complete (Iteration 3, `loop(phase-2)`; 1 of 3 attempts) | catalog (four version lines); no app source, test, module script or baseline edited | `evidence/phase2-*`, `buildenv.txt`; 231 tests, 16/16 pictures unchanged (nothing re-recorded), lint, debug and the nine modules in both variants green on Kotlin 2.4.21 |
| T-003 | complete (Iteration 4, `loop(phase-3)`; 2 of 3 attempts: round 1 passed the build, round 2 answered a review finding) | catalog (`compose-material3-app` version and alias), one line of `app/build.gradle.kts`, new `ExpressiveApiTest.kt`; no existing app source, test, module script or baseline edited | `evidence/phase3-*`, `material3.txt`, `versions.md`, `catalog-coordinates.md`; 238 tests (231 + 7), 16/16 pictures unchanged (nothing re-recorded), lint 0 errors, debug and the nine modules in both variants green |

## Signed off

None yet. Criteria 3 and 7 need a person (criterion 7 will be "nothing moved": no baseline moved in any Phase); Runs 1-4's unsigned items stay in `.harness/ISSUES.md`.

## Assumptions

Autonomous mode: see `ASSUMPTIONS.md` (A-001 to A-008).

- **Assumed criteria (Tier 3):** **11** (A-007): the four empty wrapper modules `core/editimage`, `overlaybrush`, `imageprocessor` and `localml` got their own namespaces in their build scripts, where DoD 11's sentence says a namespace shared by a prebuilt library and its module makes the criterion "unmet and reported". Every proof command of criterion 11 passes (build both variants, no flag, engine source untouched), but the run cannot end `DONE` while this stands.
- Tier 2 and below: A-001 to A-006 (as drafted at bootstrap), A-008 (`core/localml` declares `litert-support-api` directly; DoD 14's coordinate check passes textually).
