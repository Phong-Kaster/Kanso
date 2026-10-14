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


## Iteration 4 (Phase 3 attempted: T-005, T-006) - no progress, reverted
- Both Capable Workers died on an API session limit (HTTP 429, resets 6:10pm Bangkok) before finishing. Their partial files and the Iteration's ViewModel/ToolScreen edits did not compile together and were unverified, so all were reverted to the last checkpoint; no attempt is counted against T-005 or T-006 (environment failure, not task failure).
- Plan kept for the retry: processors expose `@Volatile var strength` (Retouch) and `style`/`strength` (Artistic); `ToolViewModel.apply(processor, fromOriginal)` and `runRegistered(fromOriginal, configure)` so slider tools mix against the untouched photo; `ToolPanel` gets `onRunConfigured`. Artistic style images: draw 5 256x256 WebP with Python/PIL (available) into `app/src/main/assets/styles/`. Engine facts: DetectFaceLandmarkUC.invoke(Bitmap) -> List<List<UnifyPoint>>; StyleTransferInceptionV3BlendUC.invoke(photo, styleBitmap, ratio).
