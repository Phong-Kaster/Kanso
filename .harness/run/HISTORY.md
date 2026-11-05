# HISTORY

> Append-only audit log. Written every iteration, **never read during Orient**.

<!-- Newest first. One entry per iteration. -->

### Iteration 3 - 2026-11-04
- **Phase:** 2 (T-002)
- Attempted: slice 1 (production) and slice 2 (tests) Workers, Fast; full tests + lint green; Capable review found a README "eight ranges" error (fixed) and carry-overs for T-003.
- Learned: a Worker cannot Read a whole constraints copy (read guard); Grep works. Constraints were copied to a temp reference file and deleted before commit.
- Reconciled: no failed attempt; T-002 complete.


### Iteration 2 - 2026-11-04
- **Phase:** 1 (T-001)
- Recovered: the previous invocation crashed after consuming D-001 (uncommitted AMENDMENTS/DoD/ESCALATION/STATE edits, kept) and writing slice 1 of T-001 (compiled clean, kept); two stray `_*-copy.md` files in `knowledge/reference/` removed.
- Consumed D-001: DoD approved, A-001..A-004 stand, 35 baselines may be re-recorded (AMENDMENTS).
- Attempted: slice 2 tests (Worker, Fast); review (Capable) found a missing `test-edits.md` entry and perf/robustness issues in the formula; a fix Worker added the per-hue table, removed per-pixel allocation, size check and NaN guard.
- Learned: `foreman-check-scope` does not expand `.../` in task scopes (task files T-001, T-002 rewritten with full paths, Tier 1); a full `testDebugUnitTest` takes ~10 min.
- Reconciled: no failed attempt; T-001 complete.

### Iteration 1 (Bootstrap) - 2026-11-04
- **Phase:** none (bootstrap)
- Attempted: read PRD (HSL tool, uncommitted edit to `PRD.md` at `8b2b0f2`, committed here); classed the repository a product (PROJECT.md); created `loop/kanso-hsl` from `feat/autumn-filter` at `8b2b0f2`. Fan-out at Capable: conventions survey, DoD proposal, decomposition, conflict/tier analysis, then an independent critique of the draft.
- Learned: the live preview already runs `EditProcessor` on a ≤ 640 px CPU copy (filter + layers) with the colour matrix on the GPU on top, so HSL slots between the two with one formula; `EditTool` has four exhaustive `when`s, so `EditTool.HSL` waits for the UI task; three 48 dp rows do not fit the 136 dp sheet; the row pictures 16, 17, 40-42, 51, 52, 55-82 move with a ninth tool and the recording script needs the owner's `## Baselines` list.
- Reconciled: critique B1 (row baselines) → asked in D-001; B2 (sheet height) → A-001; B3 (four preview code spots) → T-002 description; B4 (`EditLayoutTest`, screenshot taps) → T-003 scope. README timing → A-002. Knowledge: `reference/run11-facts.md` added and indexed.
- ISSUES.md: the settled Run 10 section stays (the archive cannot be written from here, PROJECT.md Constraint on `.harness/archive/`); a Run 11 section added on top.

## Archived Decisions

## Iteration 5 (2026-11-05) — Phase 3, T-003
- Recovered Iteration 4's debris (owner-stopped, coherent). Ran `HslSheetTest` alone (ends, 47 s), then the full suite and lint (green). Review at Capable: B1 test-edits.md (fixed), B2 HSL controls ignored `enabled` (fixed, test added), B3 touch-box overlap (A-005), README Filters-in-view claim (fixed), minor comments/tag constant (fixed). Re-run: full tests + lint green (`p3-full-7.log`).
- `verifyRoborazziDebug` fails 25 pictures (the row moved). The record script refused all of them: DECISIONS.md `## Baselines` uses bare names, the script wants `app/src/test/screenshots/<name>`. Left unrecorded.
- Learned: foreman-compile.ps1 must not be piped. Not done: M3 (helpers in HslSlider.kt), FilterPreviewRenderer.kt:63 comment, ToolViewModel stale KDocs (T-004).

## Iteration 5 — Phase 4 (T-004)
Worker (Fast) wrote ADR-008, edit.md, tool-screen.md, CONTEXT.md, README additions. Scope check passed. A13 cost recorded as "not measured". Docs only: no build run. Carry-overs left in ISSUES.md.

## Iteration 6 — Phase 5 (T-005)
Fast Worker wrote 14 tests; recording stalled twice (performScrollTo on a non-scrolling row; now a Constraint). Capable Worker fixed it; 12 new pictures and the listed baselines recorded (old copies in evidence/baseline-diffs). Full suite/lint not re-run (test-only change; compile passed).

## Iteration 6
Full test run: 1212 tests, 4 failed (StickerScreenshotTest 69,70,81,82, nine-entry row). Not on Baselines list; not re-recordable. PARTIAL-candidate set. Evidence: .harness/run/evidence/p5-full.log

## Iteration 7 — Verifier (PARTIAL-candidate), gap found
Assemble + lint exit 0 (`evidence/v-build-lint.log`; 0 lint errors). Full tests (`evidence/v-test.log`, 13 min): 1212, 2 failed, `StickerScreenshotTest` 81/82 `AssertionError at StickerScreenshotTest.kt:304`. Iteration 6's claim that 69/70/81/82 are not on the Baselines list was wrong: all four are listed. Filed T-006, cleared PARTIAL-candidate. `verifyRoborazziDebug` not run (known red until T-006). Machine-then-human criteria 23, 24 not driven (reachable only after T-006). First test run was killed at the 10-minute background limit and rerun.
