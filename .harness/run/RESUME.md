# RESUME BLOCK

- **Stage:** phase 2 done (T-001..T-004 complete); next Phase 3
- **Next Phase:** T-005 (Retouch), T-006 (Artistic) - scopes in each task file. The Iteration wires `ui/tool/ToolPanel.kt` (replace the Retouch/Artistic branches; `ToolPanel(tool, modifier, state, onApply, onRun)`, with `state == null` giving empty tagged slots for previews), `di/AppModule.kt` (`ProcessorRegistry(mapOf(Tool.REMOVE_BACKGROUND to ..., Tool.ENHANCE to ...))`) and README rows.
- **Seam (pointers):** `data/edit/{ToolProcessor,EditResult,EditFailure,WorkingSize,ProcessorRegistry}.kt`; examples to copy: `data/edit/enhance/*` (interface + Engine* wrapper + fake-tested processor), `ui/tool/EnhancePanel.kt`; engine facts `knowledge/reference/engine-digest-run8.md` (javap in `/c/Program Files/Java/jdk-24/bin/javap.exe -p -cp <unzipped classes.jar>`); Workers cannot unzip AARs, so the Iteration extracts what they need first (`/tmp/aar/<name>/cls`). Test tips: `ToolViewModel(..., workDispatcher = Dispatchers.Unconfined)` and `shadowOf(Looper.getMainLooper()).idle()` inside `waitUntil`.
- **Queued decisions:** 0
- **Abandoned / unreachable:** none
- **Open red:** 7 `ToolScreenshotTest` pictures (16,17,22,23,40,41,42) differ: baselines not re-recorded (command denied); listed in ISSUES.md.
- **Verified commands:** build `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug` | lint `./gradlew :app:lintDebug` (run under `timeout`)
- **Criteria:** read DoD 10-16, 17, 22 with Grep by number when briefing Phase 3 (`.harness/run/DoD.md`).
- **Model tiers:** fast: `sonnet` | capable: `opus`
- **Notes:** Workers read `.harness/` only under `knowledge/reference/`; for the verbatim Constraints Brief copy PROJECT.md Constraints (lines 3-67) and shared rules into temporary `reference/_brief-*.md` and delete them before the commit.
