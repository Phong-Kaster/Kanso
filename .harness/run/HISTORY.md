# HISTORY

## Iteration 1 (2026-10-15) - Bootstrap
Read PRD Run 9; four analysts (survey, DoD, decomposition, critique); wrote DoD, PLAN, 9 tasks, ASSUMPTIONS A-001..A-008; created branch loop/kanso-real-editor; queued D-001.

## Iteration 2 (2026-10-15)
Consumed D-001 (approved, APPROVED ticked). Started Phase 1 (T-001).
Phase 1 (T-001) done in 5 slices; Edit is now fixed photo + row of four tools + non-modal sheets + Undo/Compare pill, no Replace. Reviewer findings: panel state lost on sheet switch (KDoc corrected; T-002 fixes), README (T-007), dead closeTool (T-002), Save while comparing (T-006). Renamed 3 ReplaceNavigationTest tests (DoD 35 count note).

## Iteration 3 (2026-10-15)
Phase 2 (T-002) done in 4 slices + Iteration wiring: one EditSession (undo stack of actions), EditProcessor renders full settings from the original (filter before colour), sliders -100..100 that stay, filters one at a time, dots, Discard dialog and Back. Review found 4 blocking issues (neutral too strict, uncancellable render, predictive back lost, Discard not distinct) - all fixed; ratio-after-turn re-fit carried to T-004.

## Iteration 4 (2026-10-15)
Phase 3 (T-003) done in 4 slices + wiring: sliders/crop/turns/flips drawn live by Compose from one EditColourMatrix, filter as a conflated 640px render, parity test vs Android ColorMatrixColorFilter passes; Save on Edit full-renders. Reviewer blocking finding (filter drag frames dropped) fixed; 7 approved baselines re-recorded.

## Iteration - Phase 4 (T-004)
Real cropper (core/imagecrop) replaces range sliders; two Workers (production, tests), review fixes applied; tests/lint/assemble green; free-crop baselines pending T-009.

## Iteration - Phase 5 (T-005, T-006)
Iteration pre-wired Result key, RenderedPhotoStore, FileProvider, DI, nav; two Workers each wrote production then tests. Review fixes: untouched photo saved from its own file, controls locked while Done renders, store cleared when Result is left, Saved tag names renamed to Result. 19 baselines re-recorded (approved). verifyRoborazziDebug, lint, assemble green.

## Iteration - Phase 6 (T-007, T-008, T-009)
Recovered a crashed invocation's coherent debris (docs + 14 new picture tests), recorded baselines 55-68 (judged by opening 55, 57, 61, 63-65), three Workers wrote CONTEXT.md, ADR-003, retouch/artistic/my-edits/camera docs and verified README/edit.md against the code. Review: no blocking; fixed docs (failure message not drawn with Crop open), stale test KDocs, poll for filter chip. verifyRoborazziDebug, lint, assemble green. All tasks complete -> DONE-candidate.

## Verifier Iteration (2026-10-19)
Fresh whole set green (812 tests, lint, assemble, verifyRoborazziDebug; 7m 45s). Every machine criterion holds except R6 (`saved_*` string ids and "Saved screen" prose remain; Tier-3 A-009 reads R6 as the screen's name) and criterion 34's Verifier read (four imprecise doc sentences, three stale KDocs, found by a fresh Capable reader). Filed T-010, T-011 as Phase 7; DONE-candidate cleared. Devices not driven (gaps first). Tried to archive Runs 1-8 of ISSUES.md to `.harness/archive/` with Write: denied by the permission rules, nothing moved.
