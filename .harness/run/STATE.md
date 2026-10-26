# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.
> Execution history lives in HISTORY.md, not here — this file must not grow with the run.

## Current

- **Stage:** escalated (bootstrap; waiting on D-001, the DoD approval)
- **Loop Branch:** loop/kanso-stickers
- **Next Phase:** Phase 1 - T-001 (after D-001)
- **DONE-candidate:** no
- **PARTIAL-candidate:** no

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | blocked (D-001) | `data/edit/edit/` Sticker, StickerPictures, EditSettings, EditSession, EditProcessor; 3 tests; README, edit.md | - |
| T-002 | blocked (D-001) | EditTool, catalogue, StickerMapping, row, Stickers panel, screen state, limit message; tests; docs | - |
| T-003 | blocked (D-001) | `photocard/stickeroverlay/`, PhotoCard, ToolUiState/ViewModel/Screen; tests; docs | - |
| T-004 | blocked (D-001) | FilterPreviewRenderer, LivePreviewImage, EditCropper, ViewModel; tests; docs | - |
| T-005 | blocked (D-001) | `screenshots/StickerScreenshotTest.kt` | - |
| T-006 | blocked (D-001) | README, edit.md, tool-screen.md, CONTEXT.md | - |

## Signed-off human criteria

- none

## Assumptions

- none (the four open readings are DoD questions Q1-Q4, decided at the D-001 gate)
