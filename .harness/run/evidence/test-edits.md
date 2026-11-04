# Test edits (Run 11)

Each test added, edited, renamed or removed, with its reason (PROJECT.md Constraint; DoD 35).

| Phase | Test file | Change | Reason |
|---|---|---|---|
| 1 | `app/src/test/java/com/phongkaster/kanso/tool/edit/HslRangeWeightTest.kt` | added (10 tests) | T-001: DoD 2 (range order and centres), DoD 7 (range weights, colourfulness gate) |
| 1 | `app/src/test/java/com/phongkaster/kanso/tool/edit/HslFormulaTest.kt` | added (21 tests) | T-001: DoD 3 (27 values, clamping), 4 (hue), 5 (saturation), 6 (lightness), 8 (no banding), 9 (Blue leaves skin, red, greys) |
| 2 | `.../tool/edit/EditProcessorTest.kt` | added 2 tests (+ helpers `hueRamp`, `matrixPixel`) | T-002: DoD 15 (HSL after filter, before colour matrix), DoD 27 (neutral HSL pixel-identical) |
| 2 | `.../tool/edit/FilterPreviewRendererTest.kt` | added 1 test | T-002: DoD 17 (HSL-only change gives a changed preview) |
| 2 | `.../tool/edit/HslParityTest.kt` | added (3 tests) | T-002: DoD 16 (preview and Done agree within 1/255) |
