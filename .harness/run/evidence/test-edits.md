# Test edits (Run 12)
- `RgbMatrixTest.rgb_sits_right_after_adjust`: ten tools, CURVES between HSL and FILTERS.
- `EditToolRowTest` (`nameOf`, `controlTagOf`, the order tests, the swipe-away test): CURVES added; count 9 → 10.
- `CompareTest.compare_sits_in_the_tool_row_and_undo_beside_done`: scroll the row to FILTERS first (lazy row, ten entries).
- `EditToolTest.a_dot_marks_only_the_tools_whose_settings_are_not_neutral` / `assertDots`: scroll each entry into view; scroll back to ADJUST before reading its dot.
- `HslSheetTest.the_hsl_entry_sits_between_rgb_and_filters`: asserts HSL → CURVES → FILTERS.
- `StickerAddTest.assertSheetClosedAndFiveToolsShown`: tool-entry count 9 → 10.
- `ToolScreenshotTest`: pictures 95 and 96 added.
- New test classes (no existing test changed): `CurveFormulaTest`, `CurvesSheetTest`, `CurvesToolTest`, `CurveParityTest` (preview vs Done, DoD 7/8), `CurvesPreviewTest` (curve-only preview, DoD 7).
- Baselines `63_edit_filters_1977_light.png` and `64_edit_filters_1977_dark.png` re-recorded (D-002, owner-approved 2026-11-06): only the tool row differs (Curves entry before Filters; the row scrolls so Filters stays in view).
- `CurveFormulaTest`: added `moving_a_point_squeezed_between_pinned_neighbours_never_throws` (reviewer crash finding) and `a_point_on_the_diagonal_counts_as_a_change`.

## T-004
- CurvesSheetTest `tapping_a_chip_chooses_it_and_the_graph_names_it`: added `CurveChannel.LIGHTNESS -> "Lightness"` branch (when must be exhaustive). Nothing weakened.
- CurvesSheetTest: renamed `the_four_chips_...` to `the_five_chips_...`, added assertion on the channel list. No check removed.
- ToolScreenshotTest: `drawRedSCurve()` now delegates to `drawSCurve(channel)`, shared by pictures 97-100.

## T-006
- Added `CurveChipRowTest` (4 tests: 360 dp and 411 dp, light and dark; every curve bent, then for each of the five curves: each chip label is the whole name on one line, not ellipsized, its drawn line inside its box; at 411 dp every chip inside the sheet; Reset displayed, clickable, enabled, inside sheet and window, and tapping it straightens that curve). Reason: DoD 23 (D-004). `@GraphicsMode(NATIVE)` so text is measured with real fonts. Width judged by `getLineLeft/Right(0)`, not `didOverflowWidth`/`hasVisualOverflow` (with `softWrap = false` the paragraph is laid out 2904 px wide, so those are true for every label). No existing test changed.
- `CurveChipRowTest` (review fix): added Reset >= 48 x 48 dp (touch and laid-out), Reset label whole on one line, each chip touch area >= 48 dp. Reason: reviewer showed a squeezed Reset would pass.
