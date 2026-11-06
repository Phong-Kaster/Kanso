# Test edits (Run 13)

Each test added, edited, renamed or removed, with its reason. Appended per Phase.

## Phase 1 (T-001, Colour group)

All under `app/src/test/java/com/phongkaster/kanso/`.

- `tool/edit/ColourGroupTest` (added, 11 tests): DoD 1-8 and 11 for the new two-layer row.
- `tool/edit/ToolRowScrolling.scrollToolRowTo` (edited helper): RGB, HSL and Curves are no longer on the first row, so it taps Colour first; any other tool taps Back first if the Colour group row is showing.
- `tool/edit/EditToolRowTest` (edited): `the_row_shows_eight_tools_in_order_each_an_icon_over_its_name` (counts the 7 first-row tools), `tapping_a_tool_opens_its_sheet_and_tapping_it_again_closes_it` (only visits the open tool's own layer), `the_row_lists_eight_tools_in_order_left_to_right` (Colour sits where RGB was; 7 tools), `the_first_five_entries_fit_and_the_last_three_are_one_swipe_away` (fitting set is Crop, Rotate, Adjust, Colour; swipe-away set is Filters, Stickers, Text, Date). Reason: the row's entries changed.
- `tool/edit/EditLayoutTest.the_stickers_sheet_replaces_the_row_and_the_photo_keeps_its_size` (edited): always-shown set is Crop, Rotate, Adjust plus Colour; colour tools reached through Colour.
- `tool/edit/CompareTest.compare_sits_in_the_tool_row_and_undo_beside_done` (edited): for RGB, HSL, Curves it anchors on the tool's own entry (scrolling to Filters would tap Back and close the sheet).
- `tool/edit/CompareTest.assertEditingEnabled` (edited helper): checks the Colour entry's disabled state in place of RGB, HSL and Curves, which cannot be reached while Colour is disabled.
- `tool/edit/HslSheetTest.the_hsl_entry_sits_between_rgb_and_filters` (RENAMED `the_hsl_entry_sits_between_rgb_and_curves`): HSL no longer neighbours Filters; checked on layer 2 after tapping Colour.
- `screenshots/ToolScreenshotTest` (edited): `edit_tool_row_hsl_light/dark` (pictures 83/84) now show the Colour group; added `edit_colour_row_light/dark`, `edit_colour_group_light/dark` (pictures 101-104, DoD 11) and helper `setHslThenBackToFirstRow`.
