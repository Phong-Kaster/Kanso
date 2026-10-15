# RESUME BLOCK

> Derived cache; loses to task files and git.

- **Stage:** executing; Phase 1 (T-001) complete
- **Next Phase:** T-002 - scope in `TASKS/T-002.md` (slice in <=6 files; Worker Briefs need the Constraints: copy at `.harness/knowledge/reference/constraints-verbatim.md`, regenerate from PROJECT.md)
- **Queued decisions:** 0
- **Abandoned / unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && timeout 1500 ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | pictures: `verifyRoborazziDebug`; record only via `.harness/loop/bin/foreman-record-baselines.ps1 -Module app -Tests <filter>`
- **Criteria T-002 cites:** DoD 2 (dot), 5, 6, 7, 12, 14, 15, 16, 18, 25, R1, R2 (quote from DoD.md with Grep when dispatching)
- **Carried review notes for T-002:** EditPanel state lives in a composable that AnimatedContent recreates per sheet (move into the session in the ViewModel); `closeTool()` unused in production (wire to Back); old tests assuming stacking/spring-back rewritten with reasons; renamed tests in test-edits.md.
- **Phase 1 facts:** on Edit `TOOL_CARD_TAG` = the 60% photo area; `EDITOR_PANEL_TAG`/`EDIT_PANEL_TAG` exist only while a sheet is open; `ToolContent(editSheet=…)`; Edit's Undo is in `EDIT_PILL_TAG` (still `TOOL_UNDO_TAG`); `ToolScreenshotTest.openFreeCrop` opens the Crop sheet first.
- **Reference topics:** `reference/engine-edit.md`, `reference/run9-facts.md`
- **Model tiers:** fast: `sonnet` | capable: `opus`
