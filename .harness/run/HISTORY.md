# HISTORY

### Iteration 1 - 2026-10-14
- **Phase:** bootstrap
- Attempted: classified the repo (product), surveyed engine, Retica calls and the app with three analysts, wrote DoD, PLAN, 7 tasks.
- Learned: Retica's Retouch was object removal; Artistic style images missing; ML Kit subject segmentation downloads; engine cannot run on the host JVM.
- Reconciled: recorded as DoD readings Q1-Q4 and PLAN risks.

## Archived Decisions

## Iteration 2 (Phase 1: T-001)
- Consumed D-001. Five sequential Workers (data/edit, saver+share, ViewModel/state/panel, screen, tests); Iteration wired AppModule, SavedScreen share MIME, removed editor_panel strings, re-recorded 7 baselines, README note.
- Review (Capable) found: undo lock on non-cancel CancellationException, process() not forced off Main, opaque PNG risk (now in ToolProcessor contract), stale KDoc: all fixed. Late-progress-from-previous-run edge left (minor).
- Evidence: p1-full-2.log green. Temporary brief files under knowledge/reference were deleted before commit.

## Iteration 3 (Phase 2: T-002, T-003, T-004)
- Three parallel Capable Workers (Edit, Remove background, Enhance); Iteration wired ToolPanel, ToolScreen (checkerboard behind a cut-out), AppModule, README rows, fixed EngineUpscaler compile error, a KDoc gap, and two test setups (EditToolTest dispatcher, CutoutPreviewTest sampling).
- Review (Capable) found no blocker beyond README (done) and listed majors in ISSUES.md.
- Evidence p2-full-2.log: 648 tests, 7 failed (ToolScreenshotTest baselines, re-record denied). Lint ran without a reported failure.

