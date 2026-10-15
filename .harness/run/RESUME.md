# RESUME BLOCK

> Derived cache; loses to task files and git.

- **Stage:** executing; Phase 4 (T-004) complete
- **Next Phase:** T-005 - scope in `TASKS/T-005.md` (slice in <=6 files; Worker Briefs need the Constraints: copy at `.harness/knowledge/reference/constraints-verbatim.md`)
- **Queued decisions:** 0
- **Abandoned / unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && timeout 1500 ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | pictures: `verifyRoborazziDebug`; record only via `.harness/loop/bin/foreman-record-baselines.ps1 -Module app -Tests <filter>`
- **Criteria T-005 cites:** see TASKS/T-005.md (quote from DoD.md with Grep when dispatching)
- **T-004 facts:** `EditCropper(bitmap, settings, enabled, onCropFinished)` shown instead of the photo when Crop is open (`EditToolContent(cropper=...)`, wired in ToolScreen); `ToolViewModel.commitCrop`; tag `edit-cropper`; baselines tool_edit_free_crop_* stale until T-009; README line 39/150 updated, docs/features/edit.md still old (T-007). **T-003 facts:** Edit photo = `LivePreviewImage(state.filterPreview ?: original, state.settings)` (Canvas: crop, turn, flip, ColorFilter matrix from `EditColourMatrix`); semantics keys `LivePreviewMatrix`/`LivePreviewGeometry`, tag `live-preview`; filter = 640px conflated render (`FilterPreviewRenderer`); `state.edited` stays null on Edit; Save full-renders; `renderFinal()` is for Done (T-006); FilterPreviewRenderer runs on Dispatchers.Default so VM tests poll in real time. **Older notes:** `ToolViewModel` renders every settings change from the original via `renderSettings` (latest wins, generation counter); `EditProcessor` now stops early on cancel (`checkActive`); EditPanel callbacks carry `processorFor`; design in `reference/t002-design.md`. Sheet controls stay enabled during a render.
- **Phase 1-2 facts:** on Edit `TOOL_CARD_TAG` = the 60% photo area; `EditSession` is immutable (change/preview/commit/undo); `EditSettings.isNeutral` = all four tool-neutral; tests of Edit go through `viewModel.changeSettings/previewSettings/commitSettings/chooseCropRatio`; `ToolPanel` takes `onEditChange/onEditPreview/onEditCommit/onEditRatio` (no onApply).
- **Reference topics:** `reference/engine-edit.md`, `reference/run9-facts.md`
- **Model tiers:** fast: `sonnet` | capable: `opus`
