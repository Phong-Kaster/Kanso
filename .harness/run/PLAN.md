# PLAN — Run 14: Curves that handle like Meitu's

> Machine-owned execution strategy. Evolves through Tier-1 amendments (AMENDMENTS.md) and queued decisions.

## Strategy

Three Phases, in order, because nearly every task writes the same Curves files:

1. Remove Lightness first (smallest change, makes later scopes smaller).
2. The curve maths (T-002: free end points, flat ends, natural spline, `valueAt`) and the look (T-003)
   together — their scopes are disjoint.
3. The touch model and selection (T-004) on top of both.

Paths: `M:` = `app/src/main/java/com/phongkaster/kanso/`, `T:` = `app/src/test/java/com/phongkaster/kanso/`,
`cp/` = `M:ui/tool/component/toolpanel/editpanel/curvespanel/`.

**Fixed contracts (so every task is Fast):**
- Spline: natural cubic (zero second derivative at the first and last point) through all points; flat
  at the first/last point's y outside them; clipped to 0..1. One `internal fun valueAt(x: Float): Float`
  on `ToneCurve` feeds both `table()` and the drawn line.
- End points: first point's x in `[0, next.x - MIN_GAP]`, last mirrored; `removed(i)` any index while
  size > 2; `init` requires ≥ 2 points, sorted, within 0..1. `MAX_POINTS = 10`, `MIN_GAP = 0.02` stay.
- Selection lives in `CurvesPanel` (`rememberSaveable(channel)`), never in `ToolUiState`/`ToolViewModel`.
  `CurveGraph(curve, selected: Int?, onSelect: (Int?) -> Unit, …)`; "add and select" reports the new
  point's index after sorting. The panel remembers the last curve it handed up; when the incoming curve
  differs from it (Undo, Redo, Reset, channel change) the selection clears. Delete = one `onChange`
  (one Undo step), like Reset.
- On the line: distance from the finger to the drawn line ≤ 24 dp (`HIT_RADIUS_DP`); a point within its
  24 dp radius wins. A drag starting on the line consumes its pointer changes like a drag on a point.
- Colours: RGB line `onSurface`; R/G/B lines `RgbChannel.tintOn(...)`. Unselected point: fill = line
  colour, ring = `surfaceContainerLowest`. Selected: fill = `surfaceContainerLowest`, thicker ring = line
  colour. Frame and three verticals: `outlineVariant`.
- Labels: visible "RGB", "R", "G", "B" (new keys); spoken names stay `edit_curves_channel_*`.
- Layout: graph `weight(1f)` beside a column (delete above Reset), both 48 dp icon buttons.

**Screenshots:** pictures 95–98 change in Phases 2 and 3. They are re-recorded **once**, after Phase 3,
with the owner's approval listed under `## Baselines` in `DECISIONS.md`; 99/100 are deleted after
Phase 1 under the same approval. Until then a Phase's `verifyRoborazziDebug` is expected to fail on
95–98 only, and the Phase records which pictures differ — any other picture failing is a defect.

**Docs:** README.md, `docs/features/edit.md`, `docs/features/tool-screen.md`, `docs/adr/*` are shared
files: the Wiring Worker of each Phase updates them for that Phase's behaviour, in the same checkpoint.

## Task Graph

- T-001 - Curves offers RGB, R, G, B only; no Lightness pass (depends on: -) - scope: 10 files, 2 slices - tier: Fast
- T-002 - End points move freely, line flat beyond them, natural spline clipped (depends on: T-001) - scope: `M:data/edit/edit/ToneCurve.kt`, `T:tool/edit/CurveFormulaTest.kt`, `T:tool/edit/CurveParityTest.kt`, `T:tool/edit/CurvesToolTest.kt` - tier: Fast
- T-003 - The sheet looks like Meitu's in Kanso's colours (depends on: T-001) - scope: 8 files, 2 slices - tier: Fast
- T-004 - Tap/drag on the line adds and selects; trash deletes; no long press (depends on: T-002, T-003) - scope: 7 files, 2 slices - tier: Fast

## Phase Grouping

| Phase | Tasks | Shared files the Wiring Worker writes |
|---|---|---|
| 1 | T-001 | after: `app/src/main/res/values/strings_edit_curves.xml` (remove `edit_curves_channel_lightness`), README.md, `docs/features/edit.md`, `docs/features/tool-screen.md`; Iteration deletes PNGs 99/100 once approved |
| 2 | T-002, T-003 | after: README.md, `docs/features/edit.md`, `docs/features/tool-screen.md`, `docs/adr/ADR-011-…md` (new), `docs/adr/ADR-009-…md` |
| 3 | T-004 | before: `app/src/main/res/drawable/ic_delete.xml` (new, Material Symbols Rounded "delete"); after: README.md, `docs/features/*`; Iteration records 95–98 (approved) and 105–108 |

## Known Risks

- Deleting `CurveChannel.LIGHTNESS` breaks compilation of four test files until slice B lands: build
  only after both slices.
- `CurveChipRowTest` checks Reset's text and the long chip labels: T-003 rewrites those checks.
- Tap-off-the-line helpers (`CurvesSheetTest` :125/:141/:174, `ToolScreenshotTest.drawSCurve`) stop
  adding points under T-004; `at_ten_points_a_tap_adds_nothing` must use uneven spacing with a > 48 dp
  gap or it passes for the wrong reason.
- Natural spline moves every segment when one point moves; a natural spline with close points swings
  hard (clipped) — matches Meitu per PRD.
- No trash drawable exists; Material Symbols path must be fetched or supplied (D-001 asks).
- No ledger grants Gradle assemble/test/lint (D-001 proposes them).
