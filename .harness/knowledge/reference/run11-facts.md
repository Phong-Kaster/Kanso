# Run 11 facts — HSL on the Edit screen (bootstrap survey, 2026-11-04)

Survey of the code at `8b2b0f2` (after the RGB tool, `72e7174`). Line numbers are that commit's.

## How an Edit tool is wired (RGB as the template)

- Row order = `EditTool` case order (`data/edit/edit/EditTool.kt:12-40`).
- Exhaustive `when (tool)` with no `else`: `EditSettings.isNeutral` (:123-132), `EditToolRow.toolIcon`
  (:159-168) and `toolName` (:172-181), `EditPanel.EditPanelContent` (:230-314),
  `EditToolRowTest.kt:501,513`. A new case fails to compile until all are written.
- "Changed" dot: `!settings.isNeutral(tool)` (`EditToolRow.kt:120`); discard-on-Back
  (`ToolViewModel.onBackPressed` :1312) reads the same.
- Remembered chip: `ToolUiState.rgbChannel` (:102-106), `ToolViewModel.chooseRgbChannel` (:574),
  passed through `ToolPanel.kt:85,110-111` and `ToolScreen.kt:407`.
- One drag = one Undo step: `ToolViewModel.previewSettings` (:1270, `session.preview`) while dragging,
  `commitSettings` (:1283, `session.commit()`) on release.
- Sheet height 136 dp: `editbottombar/EditSheet.kt:34-38` (sized for one 48 dp chip row + one 48 dp
  slider row). `EditFeatureTile` chips are ≥ 56 dp (`EditFeatureTile.kt:80`). `EditPanel` scrolls
  (:221).

## Render pipeline

- Done: `EditProcessor.render` (:90-131): crop and layers, turn, flip, filter (:113-116, pure
  `filterPixel` :422), colour matrix (:122, `EditColourMatrix.of`).
- Live preview, CPU part: `FilterPreviewRenderer.render` (:60-86) runs the real `EditProcessor` with
  only filter, intensity and layers (`filterOnly` :69-73) on a ≤ 640 px copy (`PREVIEW_EDGE` :109);
  early `return null` at :64 when there is no filter and no layer. Driven by
  `ToolViewModel.requestPreview` (:1426; early exit :1432; `PreviewKey` :1402; repeat skip :1445),
  `renderPreview` (:1465; `stillWanted` :1488-1493), and the photo-loaded request (:162-163).
- Live preview, GPU part: `LivePreviewImage.kt:125-128` draws
  `ColorFilter.colorMatrix(EditColourMatrix.of(settings))` over `state.filterPreview ?: bitmap`
  (`PhotoCard.kt:174`). A sticker under the finger is drawn with the matrix but no filter
  (`LivePreviewImage` KDoc :102-109).
- `minSdk` 27 (`app/build.gradle.kts:26`): AGSL `RuntimeShader` (API 33) is not available everywhere.

## Tests a ninth tool breaks

- `RgbMatrixTest.kt:131-132` (`RGB.ordinal + 1 == FILTERS.ordinal`, 8 entries), `StickerAddTest.kt:338`
  (8 entries), `EditToolRowTest.kt:135,281` (eight tools listed), `ToolRowScrolling.kt:15` (KDoc).
- `EditLayoutTest.kt:196-200` asserts every tool but Text and Date is on screen.
- Taps on row entries without scrolling: `ToolScreenshotTest.openSheet` (:332),
  `StickerScreenshotTest:219`, `EditToolRowTest:344,361`. Use `scrollToolRowTo`.
- Loops over `EditTool.entries` (should pass if HSL's sheet opens normally): `EditLayoutTest`
  (158, 198, 243, 332), `CompareTest` (241, 391), `EditToolTest:433`.
- Pictures showing the row: 16, 17, 40, 41, 42, 51, 52, 55–82 (`ToolScreenshotTest`,
  `StickerScreenshotTest`). Highest picture number is 82.

## Traps

- `ColourCircle.kt:37-38` and `RgbChannel.kt:29-35` contain `Color(0x` literals; the PROJECT.md
  Constraint forbids new ones in `app/src/main` outside samples and previews. Compute range colours
  with `Color.hsl(...)`.
- RGB added its strings to `strings_edit.xml`; the PROJECT.md Constraint says new names go in a new
  `strings_<area>.xml` with a prefix. Run 11 uses `strings_edit_hsl.xml`, prefix `edit_hsl_`.
- Existing drawables `ic_tune` (Adjust), `ic_palette` (RGB), `ic_contrast`, `ic_saturation` are taken.
