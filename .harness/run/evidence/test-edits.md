# Test edits (Run 10)

Existing tests changed or removed, each with its reason (DoD 35). New test files are listed for completeness.

## Phase 1 (T-001)
- No existing test file edited, moved or removed.
- New: `tool/edit/StickerSessionTest.kt` (undo entries per sticker action, 20-sticker limit, gesture coalescing),
  `tool/edit/EditProcessorStickerTest.kt` (render order, crop, turn/flip, filter, output size, 1:1 checkerboard),
  `tool/edit/SolidStickerPictures.kt` (fake `StickerPictures`).

## Phase 2 (T-002)
- `tool/edit/EditToolRowTest.kt` - `the_row_shows_four_tools_in_order_each_an_icon_over_its_name`: expected list is now the five tools (Stickers added); `nameOf` gained the STICKERS case (exhaustive `when`).
- `tool/edit/EditToolRowTest.kt` - `tapping_a_tool_opens_its_sheet_and_tapping_it_again_closes_it`: the Stickers sheet replaces the row (criterion 41), so no entry exists to mark selected; for STICKERS the test checks the five entries are absent and closes with the sheet's cross; `controlTagOf` gained STICKERS; helper `showEdit` routes STICKERS to a wired `EditPanel`.
- `tool/edit/EditLayoutTest.kt` - `the_row_of_tools_is_at_the_bottom_and_the_photo_fills_the_rest` and `each_panel_opens_just_above_the_row_of_tools`: with STICKERS open there is no row, so the sheet's bounds stand in for it; helper `showEdit` routes STICKERS as above.
- `tool/edit/CompareTest.kt` - `compare_sits_in_the_tool_row_and_undo_beside_done`: the loop skips STICKERS, whose sheet hides the whole row, Compare included.
- `tool/edit/EditToolTest.kt`: not changed.
- New: `StickerMappingTest`, `StickerCatalogTest`, `StickersPanelTest`, `StickerAddTest`; new tests in `EditToolRowTest` (`the_row_lists_five_tools_in_order_left_to_right`, `all_five_entries_fit_inside_the_row_without_scrolling`, `tapping_stickers_opens_the_sheet_in_place_of_the_row`, `the_stickers_dot_is_absent_then_present_after_one_sticker_then_absent_after_undo`) and `EditLayoutTest` (`the_stickers_sheet_replaces_the_row_and_leaves_the_photo_alone`).
- Baselines re-recorded (D-001): 51, 52, 55-68 (old copies in `evidence/baseline-diffs/`). Their reference pictures were already stale before this Phase (HEAD `verifyRoborazziDebug` fails 33 tests, identical count); non-listed stale pictures were left untouched.
- `StickerAddTest.kt` (new this Phase): import of `STICKER_LIMIT_MESSAGE_TAG` follows `StickerLimitMessage` to `ui.tool.components` (review B4).

## Phase 3 (T-003)
- No existing test file edited, moved or removed.
- New: `tool/edit/StickerGeometryTest.kt` (handle, hit-test and pinch maths; DoD 8), `StickerFrameTest.kt` (corner buttons delete, duplicate, flip, resize handle, content descriptions, 48 dp; DoD 5, 6, 7, 8, 22), `StickerGestureTest.kt` (swipe, pinch, twist on a turned and flipped photo, edge clamp, flip mid-drag; DoD 9), `StickerSelectionTest.kt` (select, overlap, closed-sheet inertness, undo order, pruning; DoD 10, 11, 12, 21), `StickerOverlayTest.kt` (Compare shows the original; DoD 14).
