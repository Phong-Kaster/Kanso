# RESUME BLOCK

> Regenerated every iteration. A derived cache, never a source of truth: on any disagreement with the task
> files or git, this file is wrong. Keep it small.

- **Stage:** Phase 4 complete (T-004); Phase 5 next
- **Next Phase:** Phase 5 - T-005 (one slice: `screenshots/StickerScreenshotTest.kt` new, optional `StickerScreenshotSamples.kt`; Follow `ToolScreenshotTest.kt`; Fast). Pictures 69-82 named in `TASKS/T-005.md`; the Iteration records them with `.harness/loop/bin/foreman-record-baselines.ps1` and judges each. Stickers now draw in the live preview (async small copy): screenshot tests must wait for the render to land.
- **Queued decisions:** 0
- **Abandoned:** none | **Unreachable:** none
- **Verified commands:** build `timeout 3600 ./gradlew :app:assembleDebug` | test `timeout 3600 ./gradlew :app:testDebugUnitTest` | lint `timeout 3600 ./gradlew :app:lintDebug`. NOTE `verifyRoborazziDebug` already fails 33 stale pictures at HEAD (ISSUES.md); do not treat those as regressions, compare counts. Worker compile: `powershell -NoProfile -File .harness/loop/bin/foreman-compile.ps1 -Module app` (run alone, no pipes). Constraints for Briefs: `.harness/knowledge/reference/constraints-verbatim.md` (Workers read it with an explicit `limit`; the guard refuses whole-file reads).
- **Phase 4 left:** `LivePreviewImage(touchedSticker, touchedPicture)` + semantics `LivePreviewTouchedSticker`; `ToolUiState.touchedStickerKey`; `EditCropper(stickers)` bakes stickers via `withStickersDrawn`; `StickerPlace.kt` (invisible overlay node). Sticker pixels in tests must be polled (see `StickerOverlayTest.pollPixel`).
- **Criterion T-005 cites:** DoD 27: [machine] The new pictures the owner approved are recorded and checked on every run: the row with Stickers open, the Stickers sheet on each category, a selected sticker with its four corner buttons, and a photo with stickers after a crop and a filter, each in light and in dark.
- **Open issues touching the next Phase's files:** Run 8 item "README matches the app" (`README.md`) open, awaits a person; do not drop. none in the new test files.
- **Reference topics:** `reference/run9-facts.md`, `reference/engine-edit.md`
- **Model tiers:** fast: `sonnet` | capable: `opus`
