# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this is wrong.

- **Stage:** build — Phase 4 done (T-004 docs); next Phase 5.
- **Next Phase:** Phase 5: T-005 (`TASKS/T-005.md`; `screenshots/ToolScreenshotTest.kt`, HSL pictures). Read the task file for scope and DoD text.
- **Pictures:** `verifyRoborazziDebug` fails 25 pictures (the row moved). The record script refuses them: `## Baselines` in DECISIONS.md lists bare names, the script wants `app/src/test/screenshots/<name>`. Only the owner can fix the list; do not route around.
- **Carry-over (outside any scope, in ISSUES.md):** stale `ToolViewModel.kt` KDocs and `FilterPreviewRenderer.kt:63` comment; README "Known limits" row-width sentence (nine tools) unverified.
- **Queued decisions:** 0 | **Abandoned:** none | **Unreachable:** none
- **Verified commands:** build `./gradlew :app:assembleDebug` | test `./gradlew :app:testDebugUnitTest` (~6 min; run `HslSheetTest` alone first) | lint `./gradlew :app:lintDebug` | pictures `./gradlew :app:verifyRoborazziDebug` | record `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app` | Worker compile `powershell -NoProfile -File D:/Valve/Personal/Kanso/.harness/loop/bin/foreman-compile.ps1`. Never run stdin-reading commands in Bash.
- **Reference topics:** `.harness/knowledge/reference/run11-facts.md`
- **Model tiers:** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
