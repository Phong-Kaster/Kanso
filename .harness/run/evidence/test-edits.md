# Test edits (Run 11)

Each test added, edited, renamed or removed, with its reason (PROJECT.md Constraint; DoD 35).

| Phase | Test file | Change | Reason |
|---|---|---|---|
| 1 | `app/src/test/java/com/phongkaster/kanso/tool/edit/HslRangeWeightTest.kt` | added (10 tests) | T-001: DoD 2 (range order and centres), DoD 7 (range weights, colourfulness gate) |
| 1 | `app/src/test/java/com/phongkaster/kanso/tool/edit/HslFormulaTest.kt` | added (21 tests) | T-001: DoD 3 (27 values, clamping), 4 (hue), 5 (saturation), 6 (lightness), 8 (no banding), 9 (Blue leaves skin, red, greys) |
| 2 | `.../tool/edit/EditProcessorTest.kt` | added 2 tests (+ helpers `hueRamp`, `matrixPixel`) | T-002: DoD 15 (HSL after filter, before colour matrix), DoD 27 (neutral HSL pixel-identical) |
| 2 | `.../tool/edit/FilterPreviewRendererTest.kt` | added 1 test | T-002: DoD 17 (HSL-only change gives a changed preview) |
| 2 | `.../tool/edit/HslParityTest.kt` | added (3 tests) | T-002: DoD 16 (preview and Done agree within 1/255) |
| 3 | `.../tool/edit/HslToolTest.kt` | added | T-003: DoD 10 (first open, remembered), 11 (choosing changes nothing, no Undo), 12 (one drag one Undo step, Redo), 13 (dot), 14 (Compare, Back asks to discard, no reset) |
| 3 | `.../tool/edit/HslSheetTest.kt` | added (incl. `a_disabled_sheet_cannot_be_used`) | T-003: DoD 2 (nine circles in order), 21 (136 dp, three rows, >= 48 dp), selection/radio roles, Compare/Done disable |
| 3 | `.../a11y/ColourRangeMarkContrastTest.kt` | added | T-003: DoD 22 (mark contrast on all nine colours, light and dark) |
| 3 | `.../tool/edit/EditToolRowTest.kt` | edited (8 -> 9 tools, `SWIPE_AWAY` gains HSL and FILTERS) | T-003: DoD 1; with HSL the row is 428 dp so Filters is no longer in view on 411 dp |
| 3 | `.../tool/edit/RgbMatrixTest.kt` | edited (`rgb_sits_right_after_adjust`, counts) | T-003: ninth tool after RGB |
| 3 | `.../tool/edit/EditLayoutTest.kt`, `ToolRowScrolling.kt` | edited | T-003: row has nine entries; scroll before tapping an entry that moved out of view |
| 3 | `.../tool/edit/Sticker{Add,Compare,Frame,Gesture,Overlay,Selection}Test.kt`, `StickersPanelTest.kt` | edited (taps only: scroll to the Stickers entry first) | T-003: HSL pushes the Stickers entry off the 411 dp row |
| 3 | `.../screenshots/ToolScreenshotTest.kt`, `StickerScreenshotTest.kt` | edited (taps only) | T-003: same, scroll before tapping a row entry |
| 5 | `.../screenshots/ToolScreenshotTest.kt` | edited (14 new tests: pictures 83-94; no existing test changed) | T-005: DoD 18, 19, 20 |
