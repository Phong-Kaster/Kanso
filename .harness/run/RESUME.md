# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this is wrong.

- **Stage:** build - Phase 5 done (T-005, pictures recorded). PARTIAL-candidate set: full tests ran, only 4 StickerScreenshotTest pictures (69,70,81,82) fail, not recordable without the owner listing them. Next invocation is the Verifier (§14.4): rerun lint + verifyRoborazziDebug (not reached last time), then DONE_PARTIAL.
- **Next Phase:** none left.
- **Carry-over (outside any scope, in ISSUES.md):** stale `ToolViewModel.kt` KDocs and `FilterPreviewRenderer.kt:63` comment; README "Known limits" row-width sentence (nine tools) unverified.
- **Queued decisions:** 0 | **Abandoned:** none | **Unreachable:** none
- **Verified commands:** build `./gradlew :app:assembleDebug` | test `./gradlew :app:testDebugUnitTest` (~6 min; run `HslSheetTest` alone first) | lint `./gradlew :app:lintDebug` | pictures `./gradlew :app:verifyRoborazziDebug` | record `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app` | Worker compile `powershell -NoProfile -File D:/Valve/Personal/Kanso/.harness/loop/bin/foreman-compile.ps1`. Never run stdin-reading commands in Bash.
- **Reference topics:** `.harness/knowledge/reference/run11-facts.md`
- **Model tiers:** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
