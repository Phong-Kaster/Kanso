# Run 13 facts — the Edit row's Colour group (bootstrap survey, 2026-11-06, HEAD `772e6dd`)

Paths under `app/src/main/java/com/phongkaster/kanso/` (main) or `app/src/test/java/com/phongkaster/kanso/` (test).

- Edit row: `ui/tool/component/editbottombar/edittoolrow/EditToolRow.kt` — outer `Row` `EDIT_TOOL_ROW_TAG`
  (:36), `LazyRow` `EDIT_TOOL_LIST_TAG` over `EditTool.entries` (:105-135), entries 60 dp (:54), divider
  + Compare 68 dp pinned right (:137-153); tags `editToolTag` (:45), `editToolDotTag` (:51),
  `EDIT_COMPARE_TAG` (:42); `toolIcon` (:159-170: RGB `ic_palette`, HSL `ic_invert_colors`, Curves
  `ic_show_chart`), `toolName` (:174-185). Dot = `!settings.isNeutral(tool)` (:120). Host:
  `EditBottomBar.kt:151-162` (`enabled = !comparing && !finishing`); Stickers/Text/Date sheets replace
  the row (:105-143). The row has only `onOpen(EditTool)`, a toggle (`ToolViewModel.openTool` :536-564);
  `closeTool()` at :614.
- Back: `ToolViewModel.onBackPressed` :1344-1362 (layer sheet → open sheet → discard prompt if not
  neutral); wired at `ToolScreen.kt:257` (top bar) and a `BackHandler` :428-430; dialog :431-439.
  Compare: `ToolViewModel.setComparing` :1619-1624.
- Beautify two-level row: `ui/tool/component/toolpanel/beautifypanel/BeautifyToolRow.kt` (public row +
  private `GroupRow`, `ToolEntry`, `GroupEntry`, `BackEntry` in one file — not a precedent for file
  layout); `AnimatedContent` on `openGroup` :72-114; layer 2 `LazyRow` + divider + **Back pinned right**
  :132-155; group dot `group.tools.any(changed)` :97; tags `BEAUTIFY_BACK_TAG` :40, `beautifyGroupTag`
  :46, `beautifyToolTag` :43. State is panel-local `rememberSaveable` (`BeautifyPanel.kt:114-115`);
  opening a group opens its first tool (:299-303); Back clears group and tool, keeps values (:306-310);
  `BeautifyToolTest` :158-194 pins it. Models: `ui/tool/model/BeautifyGroup.kt`, `BeautifyEntry.kt`.
- Strings: RGB `edit_layout_rgb` (`strings_edit_layout.xml:8`), HSL `edit_hsl_label`, Curves
  `edit_curves_tool_name`, Back `back` (`strings.xml:80`). "Colour" exists only as `text_colour_title`
  (Text tool) — do not reuse.
- Tests that walk the flat row: `tool/edit/ToolRowScrolling.kt:21` (central helper),
  `EditToolRowTest.kt` (:132-163, :167-247, :279-335, `SWIPE_AWAY` :612, `isRowEntry` :504, `when`s
  :512/:526), `HslSheetTest.kt:391-419`, `EditLayoutTest.kt:196-221`, `CompareTest.kt:390-402`
  (and :241-258 opens via ViewModel then scrolls to Filters), `EditToolTest.kt:433-441`,
  `StickerAddTest.kt:335-344`, `screenshots/ToolScreenshotTest.kt` (`openSheet` :550-555, pictures 83/84
  :346-358). VM-level tool tests open tools with `viewModel.openTool` and are unaffected.
  `RgbMatrixTest.kt:130-134` asserts `EditTool` has 10 cases in order.
- Guards: `ScreenStateTest.kt:51` expects seven ViewModels; `a11y/BottomClearanceTest.kt:291` keys on
  `EDIT_TOOL_ROW_TAG`; `AccessibilityWalkTest` walks the Edit screen (:353, :383, :416).
- Highest picture number is 100; next are 101-104. Pictures 71-80 (sticker sheets) hide the row.
- Docs mentioning the row: README :56, :99-102, :116-129, :154-157, :212, :239, :285, :431;
  `docs/features/edit.md` :1, :36-76; `docs/features/tool-screen.md` :31, :311; `docs/features/beautify.md`
  :107, :185. Next ADR number is 010.

## Built in Run 13 (Iteration 2)

- `scrollToolRowTo(tool)` (test/tool/edit/ToolRowScrolling.kt) now changes layers: RGB/HSL/CURVES tap Colour first; any other tool taps Back first if the group row shows (that closes an open colour sheet).
- `StateRestorationTester` for a compose rule is `androidx.compose.ui.test.junit4.StateRestorationTester`.
- `EditToolEntry` sets `maxLines = 1` with ellipsis, so a one-line check must also assert `!isLineEllipsized(0)`.
- A Worker's shell allows only the compile command (no grep/sed/python): use Edit and Grep.
- Files: `ui/tool/model/EditToolGroup.kt`, `EditRowEntry.kt`; `ui/tool/component/GroupBackEntry.kt`, `GroupRowTransition.kt`; `edittoolrow/EditToolRow.kt`, `ToolRowEntry.kt`, `CompareEntry.kt`, `EditColourGroupRow.kt`. Pictures 101-104 added; 43 re-recorded. Full suite 1282 tests green.
