# PLAN — Run 12: Curves on the Edit screen

> Machine-owned execution strategy. Evolves through Tier-1 amendments (AMENDMENTS.md) and Tier-2
> queued decisions. Paths: `M/` = `app/src/main/java/com/phongkaster/kanso/`, `T/` =
> `app/src/test/java/com/phongkaster/kanso/`, `P/` = `M/ui/tool/component/toolpanel/editpanel/curvespanel/`.

## Strategy

A tenth Edit tool, `CURVES`, placed right after `HSL`, built in a strict chain of single-task Phases:
the master curve end to end with Done (T-001), the live preview (T-002), the Red/Green/Blue chips
(T-003), the Lightness curve (T-004), and, only if the human keeps DoD 23-24, hue curves (T-005).

Design fixed here, so no Worker makes an architecture decision (critique M2; recorded in ADR-009 by the
Phase 1 Wiring Worker):

- **Model** (`M/data/edit/edit/`): `CurvePoint(x: Float, y: Float)` in 0..1; `ToneCurve(points:
  List<CurvePoint>)` — immutable, sorted by x, first x = 0 and last x = 1, 2..10 points, neighbouring x
  at least `MIN_GAP = 0.02` apart, `NEUTRAL` = (0,0),(1,1), `isNeutral`, `table(): IntArray(256)` by
  monotone cubic (Fritsch–Carlson), clamped to 0..255; `CurveChannel` enum (RGB, RED, GREEN, BLUE; T-004
  adds LIGHTNESS); `CurveAdjustments(rgb, red, green, blue[, lightness])` with `curve(channel)`,
  `with(channel, curve)`, `isNeutral`, `NEUTRAL`; `EditSettings.curves: CurveAdjustments` and
  `hasCurveChange`. Lists, never arrays, so `==` works for Undo and `PreviewKey`
  (`EditSession.kt:58-60`).
- **Formula** (`EditCurves.apply(IntArray, CurveAdjustments)`, no Android types, like `EditHsl.apply`):
  per pixel `r' = rgbT[redT[r]]` (likewise g, b), then T-004's Lightness: RGB→HSL, `L' = lightT(L)`,
  HSL→RGB, greys included, full 0..1 range. The RGB↔HSL conversion is **copied** into the curves code,
  never extracted from `EditHsl.kt` (critique B2: `hslToRgb` is private and HSL's parity and pictures
  must not move; DoD 21).
- **Pipeline:** Done `EditProcessor.render`: … filter → HSL → **curves** → colour matrix. Live preview:
  the CPU stage `FilterPreviewRenderer` includes curves in `filterOnly` and its early return; the
  ViewModel's preview gates (`ToolViewModel` photo-loaded request, `requestPreview` early exit,
  `PreviewKey`, `stillWanted`, survey lines :166, :1424-1471, :1518-1524) add `hasCurveChange`.
- **Sheet** (220 dp, 188 dp of content): one 48 dp row of chips with Reset at its end, the graph full
  width below (~132 dp tall). `EditPanel`'s `verticalScroll` exemption list (`EditPanel.kt:239-242`)
  gains CURVES so a vertical drag moves a point, not the sheet (critique B3).
- **Gestures:** one `pointerInput` on the graph picks the nearest point within 24 dp; tap on empty
  space adds a point at the tapped (x, y); drag moves (clamped between neighbours ± `MIN_GAP`; end points
  vertical only); `onLongPress` removes a middle point. No `onDoubleTap` (it delays every tap by the
  double-tap timeout, critique M4). Each gesture: `onPreview` while moving, `onCommit` once at the end.
- **Colours** of the line per channel from `MaterialTheme.colorScheme` or `Color.hsl(...)`, never
  `Color(0x` (PROJECT.md Constraint).
- **Icon:** a Material Symbols drawable `ic_show_chart.xml`, white fill, no `android:tint` (Constraint),
  written by the Phase 1 Wiring Worker (drawables are Wiring-only).

Verification: unit + Robolectric tests per task; new pictures numbered from 95, test code by the
Worker, baselines recorded by the Iteration through `foreman-record-baselines.ps1`; existing pictures
that move because the row gained an entry only from the human's `## Baselines` list. The A13 is driven by
the Verifier (DoD 16), not a task.

## Task Graph

- T-001 - The user can bend the photo with a master (RGB) curve and save it (depends on: D-001, Phase 1 wiring) - scope: see task file (24 files, 4 slices) - tier: Fast
- T-002 - The photo follows the curve live while dragging (depends on: T-001) - scope: `M/data/edit/edit/FilterPreviewRenderer.kt`, `M/ui/tool/ToolViewModel.kt`, `T/tool/edit/CurvesPreviewTest.kt`, `T/tool/edit/CurveParityTest.kt`, `README.md`, `docs/features/edit.md` - tier: Fast
- T-003 - The user can curve Red, Green and Blue separately (depends on: T-002) - scope: see task file (15 files, 3 slices) - tier: Fast
- T-004 - The user can curve Lightness (HSL L) (depends on: T-003) - scope: see task file (12 files, 2 slices) - tier: Fast
- T-005 - Optional: hue curves (depends on: T-004; deferred unless D-001 keeps DoD 23-24) - scope: see task file - tier: Fast

## Phase Grouping

Every task shares `README.md`, `docs/features/edit.md` and `P/CurvesPanel.kt` with the next one, so no
two can share a Phase.

| Phase | Tasks | Shared files the Wiring Worker writes |
|---|---|---|
| 1 | T-001 | before T-001: `app/src/main/res/drawable/ic_show_chart.xml`, `docs/adr/ADR-009-curves-run-on-the-cpu-after-hsl.md`; after, only if a guard fires: `T/previews/PreviewsRenderTest.kt`, `PreviewCoverageTest.kt` |
| 2 | T-002 | - |
| 3 | T-003 | - |
| 4 | T-004 | - |
| 5 | T-005 (if kept) | - |

The Iteration itself writes `.harness/run/evidence/test-edits.md` and records baselines.

## Known Risks

- `verifyRoborazziDebug` may be red at the start for pictures that show the row (83, 84 and any row
  window that reaches past HSL). Measure in Phase 1 before T-001 changes anything visible; list exact
  paths in a decision for the human's `## Baselines`.
- Robolectric drags on a Canvas: copy `HslSheetTest`/`StickerGestureTest`; never `performScrollTo()` in a
  row that fits (PROJECT.md:102).
- Preview cost on the A13: curves add a table lookup per pixel on the ≤ 640 px copy after HSL; unmeasured
  (DoD 17 is a person's).
- The A13 is driven by resource id or text only (`foreman-device.ps1`); the graph may not be reachable
  by id, in which case DoD 16 is reported "not driven" for that step.
