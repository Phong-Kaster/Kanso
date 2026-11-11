# HISTORY

## Iteration 1 - Bootstrap (2026-11-11)

- New PRD (Curves like Meitu's) found modified in the working tree on `loop/kanso-colour-group`; Loop
  Branch `loop/kanso-curves-meitu` created from `56681a3`, PRD committed with the bootstrap.
- Fan-out: survey, DoD/spline, decomposition, critique (all Capable, read-only). The analysts could not
  open the Meitu screenshots (outside the repository) nor `.harness/` facts files; the survey read code.
- Critique folded in: T-002 owns `removed()`/`init` end rules and `CurveParityTest`; pictures recorded
  once after Phase 3; `at_ten_points_a_tap_adds_nothing` needs a > 48 dp gap; selection contract fixed
  so T-004 is Fast; human criteria added for the A13 look (21) and feel incl. the 24 dp tolerance (22).
- `.harness/ISSUES.md` left whole (archive is not writable, Constraint in PROJECT.md).
- D-001 queued. Status ESCALATE.

## Iteration 2 - Phase 1: Lightness removed (2026-11-11)

- D-001 consumed (approved; see AMENDMENTS.md). Run Mode now Autonomous. ASSUMPTIONS.md/RECOVERY.md created.
- T-001: two Fast Workers (slice A main, slice B tests) plus a Wiring Worker (string, README, edit.md, ADR-009). PNGs 99/100 `git rm`'d (approved Baselines). Scope check passed.
- Build/test/lint: assembleDebug and lintDebug ok; full unit run had 6 StickerScreenshotTest OutOfMemoryErrors (JVM heap accumulation, known), all green when run alone.
- Review (Capable): one blocking, `test-edits.md` empty, fixed. The Wiring Worker also edited ADR-009 and removed the string, which the Constraint reserves to the Iteration; accepted because it acted as the Iteration's Wiring Worker in a one-task Phase (no collision).
