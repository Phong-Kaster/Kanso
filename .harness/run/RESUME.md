# RESUME BLOCK

> Derived cache; loses to task files and git.

- **Stage:** executing; Phase 2 (T-002) complete
- **Next Phase:** T-003 - scope in `TASKS/T-003.md` (slice in <=6 files; Worker Briefs need the Constraints: copy at `.harness/knowledge/reference/constraints-verbatim.md`, regenerate from PROJECT.md)
- **Queued decisions:** 0
- **Abandoned / unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && timeout 1500 ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | pictures: `verifyRoborazziDebug`; record only via `.harness/loop/bin/foreman-record-baselines.ps1 -Module app -Tests <filter>`
- **Criteria T-003 cites:** DoD 4, 6, 17 (quote from DoD.md with Grep when dispatching)
- **Carried notes for T-003:** `ToolViewModel` renders every settings change from the original via `renderSettings` (latest wins, generation counter); `EditProcessor` now stops early on cancel (`checkActive`); EditPanel callbacks carry `processorFor`; design in `reference/t002-design.md`. Sheet controls stay enabled during a render.
- **Phase 1-2 facts:** on Edit `TOOL_CARD_TAG` = the 60% photo area; `EditSession` is immutable (change/preview/commit/undo); `EditSettings.isNeutral` = all four tool-neutral; tests of Edit go through `viewModel.changeSettings/previewSettings/commitSettings/chooseCropRatio`; `ToolPanel` takes `onEditChange/onEditPreview/onEditCommit/onEditRatio` (no onApply).
- **Reference topics:** `reference/engine-edit.md`, `reference/run9-facts.md`
- **Model tiers:** fast: `sonnet` | capable: `opus`
