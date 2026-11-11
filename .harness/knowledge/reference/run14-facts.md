# Run 14 facts — Curves like Meitu's (bootstrap survey, 2026-11-11, HEAD `56681a3`)

Paths: `data/` = `app/src/main/java/com/phongkaster/kanso/data/edit/edit/`, `cp/` =
`app/src/main/java/com/phongkaster/kanso/ui/tool/component/toolpanel/editpanel/curvespanel/`, `t/` =
`app/src/test/java/com/phongkaster/kanso/`.

## Code
- Model: `data/CurvePoint.kt`, `data/CurveChannel.kt` (RGB, RED, GREEN, BLUE, LIGHTNESS :23),
  `data/CurveAdjustments.kt` (five curves; `curve`/`with` :24-39; `isNeutral` :42-43),
  `data/EditSettings.kt:87-92,151` (`curves`, `hasCurveChange`).
- `data/ToneCurve.kt`: `init` :26-36 (x = 0 / x = 1 at :28-29), `table()` :54-80 Fritsch-Carlson with
  per-segment clamp :75-77, flat ends already via `coerceIn` :66, `added` :87-93, `moved` :104-118 (end
  x pinned :107-108), `removed` :121-124 (refuses ends), `MAX_POINTS = 10` :129, `MIN_GAP = 0.02` :132,
  `tangents` :150-175.
- `data/EditCurves.kt:24-42` apply; Lightness pass :41, `applyLightness` :49-78, `pack` :81-86,
  `hslToRgb` :89-102. `data/EditProcessor.kt:128-133` runs curves after HSL.
- Undo: the graph previews on every move and commits once on lift (`cp/CurveGraph.kt:177-189`);
  `cp/CurvesPanel.kt:105-111` turns it into `onPreview` + `onCommit`; Reset is one `onChange` (:116-119).
  No ViewModel callback is Curves-specific except `chooseCurveChannel` (`ToolViewModel.kt:589-591`).
- `cp/CurveGraph.kt`: `CURVE_GRAPH_TAG` :44, `CURVE_GRAPH_PADDING_DP` :50, `HIT_RADIUS_DP = 24` :53,
  gestures :125-207 (tap adds :165-170, drag :171-190, long press removes :196-204, only point drags
  consume :155), grid :211-217, diagonal :219-224, line :226-232, points :234-238.
- `cp/CurvesPanel.kt` `curveLineColor` :129-140 uses `Color.hsv`; `cp/CurveChips.kt` FilterChips with
  `horizontalScroll`; `cp/CurveResetButton.kt` `CURVE_RESET_TAG` :22, TextButton.
- Strings: `app/src/main/res/values/strings_edit_curves.xml` (channel names :6-12, graph description
  :14, reset :16).
- R/G/B tints: `ui/tool/model/RgbChannel.kt:33-47` (`tintOn(surface)`), contrast guard
  `t/a11y/RgbTintContrastTest.kt`.
- Icons: no trash/delete drawable; `ic_refresh.xml` exists (used by Crop "Original" and camera retake).

## Tests and pictures
- `t/tool/edit/CurveFormulaTest.kt`, `CurvesSheetTest.kt` (sdk 35, 411 dp; `spot()` :298-304; `show()`
  :307-345), `CurveChipRowTest.kt` (NATIVE graphics; Reset text check :211-217), `CurvesToolTest.kt`,
  `CurveParityTest.kt`, `CurvesPreviewTest.kt`; `EditToolRowTest` imports `CURVE_GRAPH_TAG`.
- Pictures 95-100 drawn by `t/screenshots/ToolScreenshotTest.kt` :447-491; `drawSCurve` :546-559 taps
  OFF the line (stops adding points under the new touch model). Next free picture: 105. Next ADR: 011.
- Docs on Curves: README row :102, "Curves, end to end" :298-346, ADR table :692;
  `docs/adr/ADR-009-curves-run-on-the-cpu-after-hsl.md` :4, :27, :28, :30; `docs/features/edit.md`
  :51-63, :126-132, :436-444, :543-544; `docs/features/tool-screen.md:85-93` (already stale).

## Traps
- Deleting `CurveChannel.LIGHTNESS` breaks compilation of 4 test files until they are edited too.
- `at_ten_points_a_tap_adds_nothing`: on evenly spaced ten points every line tap is within 24 dp of a
  point, so the test passes for the wrong reason; use uneven spacing with a > 48 dp gap.
- A drag starting on the line must consume its pointer changes, or the sheet takes the gesture.
- Analysts (read-only role) cannot read `.harness/` facts files or files outside the repository.
