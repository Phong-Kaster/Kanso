# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this is wrong.

- **Stage:** build - Phase 6 (T-006). The Verifier (Iteration 7) found assemble + lint green, tests 1212 with 2 failed (`StickerScreenshotTest` 81/82, `AssertionError at StickerScreenshotTest.kt:304`), and that pictures 69, 70, 81, 82 ARE on the owner's `## Baselines` list. After Phase 6: full tests, lint, `verifyRoborazziDebug`, then set PARTIAL-candidate again (criteria 23-26 need a person).
- **Next Phase:** Phase 6 - T-006 (Fast). Scope: `app/src/test/java/com/phongkaster/kanso/screenshots/StickerScreenshotTest.kt` (Worker); baselines `app/src/test/screenshots/{69_edit_stickers_row_light,70_edit_stickers_row_dark,81_edit_stickers_crop_filter_light,82_edit_stickers_crop_filter_dark}.png` (Iteration, via the record wrapper with `-Tests "*StickerScreenshotTest*"`; copy old ones to `evidence/baseline-diffs/` first). See `TASKS/T-006.md` for steps.
- **DoD cited by T-006:**
  - 23. [machine-then-human] Pictures that changed only because the row gained a tool really differ only in the row.
  - 28. [machine] The other eight tools render exactly as before and every test that existed before this run still passes.
  - 30. [machine] The app builds, opens, and the Edit screen with the HSL sheet open does not crash; tests, lint and pictures pass.
  - 37. [machine] Pictures were recorded only through the approved route, and every existing picture that changed is on the owner's list.
- **Open ISSUES items in scope:** "[ ] Run 11 Four Sticker pictures are red (Iteration 6) ..." (corrected in ISSUES.md: they are on the list).
- **Carry-over (outside any scope, in ISSUES.md):** stale `ToolViewModel.kt` KDocs and `FilterPreviewRenderer.kt:63` comment; README "Known limits" row-width sentence (nine tools) unverified.
- **Queued decisions:** 0 | **Abandoned:** none | **Unreachable:** none
- **Verified commands:** build `./gradlew :app:assembleDebug` | test `./gradlew :app:testDebugUnitTest --continue` (~13 min; run in background with a timeout >= 2400000 ms, the default 10-minute background limit kills it) | lint `./gradlew :app:lintDebug` | pictures `./gradlew :app:verifyRoborazziDebug` | record `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app -Tests "<filter>"` | Worker compile `powershell -NoProfile -File D:/Valve/Personal/Kanso/.harness/loop/bin/foreman-compile.ps1`. Never run stdin-reading commands in Bash. Test logs need `grep -a`.
- **Reference topics:** `.harness/knowledge/reference/run11-facts.md`
- **Model tiers:** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
