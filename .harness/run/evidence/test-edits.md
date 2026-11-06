# Test edits (Run 12)
- `RgbMatrixTest.rgb_sits_right_after_adjust`: ten tools, CURVES between HSL and FILTERS.
- `EditToolRowTest` (`nameOf`, `controlTagOf`, the order tests, the swipe-away test): CURVES added; count 9 → 10.
- `CompareTest.compare_sits_in_the_tool_row_and_undo_beside_done`: scroll the row to FILTERS first (lazy row, ten entries).
- `EditToolTest.a_dot_marks_only_the_tools_whose_settings_are_not_neutral` / `assertDots`: scroll each entry into view; scroll back to ADJUST before reading its dot.
- `HslSheetTest.the_hsl_entry_sits_between_rgb_and_filters`: asserts HSL → CURVES → FILTERS.
- `StickerAddTest.assertSheetClosedAndFiveToolsShown`: tool-entry count 9 → 10.
- `ToolScreenshotTest`: pictures 95 and 96 added.
