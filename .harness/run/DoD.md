# Definition of Done — Run 14: Curves that handle like Meitu's

> Derived from `PRD.md` at bootstrap (2026-11-11). Human-owned after approval: the engine may propose
> changes (Tier 3) but never apply them.

## Status

- [ ] APPROVED — approve via `D-001` in `.harness/run/ESCALATION.md`; edit criteria freely before approving.
      Approval covers the Verification Class of each criterion, not only its wording.

Verification classes: `[machine]` a command or a named file proves it; `[machine-then-human]` the
Verifier drives it (phone, journey, picture) and a person still looks and signs; `[human-only]` nothing
can drive it.

Short paths in Proof lines: `cp/` = `app/src/main/java/com/phongkaster/kanso/ui/tool/component/toolpanel/editpanel/curvespanel/`,
`data/` = `app/src/main/java/com/phongkaster/kanso/data/edit/edit/`, `t/` = `app/src/test/java/com/phongkaster/kanso/`.

## Acceptance Criteria

### Behaviour

1. [machine] Tapping on the curve's line adds a point exactly on the line where you tapped, at the line's own height (not the finger's), and that point becomes the selected one.
   Proof: `t/tool/edit/CurvesSheetTest.kt`, sheet open on RGB with a straight curve: a tap 10 dp above the line at x = 0.4 gives 3 points, the new one at x = 0.4 ± 0.01 with y = the line's value there before the tap ± 0.01, selected, and exactly one commit. A tap on the flat part left of the first point or right of the last also counts as on the line and adds a point there, which becomes the new end point.
2. [machine] A tap away from the line and away from every point adds nothing and only clears the selection.
   Proof: `CurvesSheetTest`, with a point selected, a tap 40 dp from the line and from every point: point count unchanged, no point selected, no commit.
3. [machine] A tap counts as "on the line" when it lands within 24 dp of the drawn line — the same reach as grabbing a point today; when a point is also within 24 dp, the point wins and is selected instead.
   Proof: `CurvesSheetTest`, straight curve: a tap 22 dp from the line adds a point; a tap 26 dp from it adds none; a tap 10 dp from an existing point selects that point and adds none.
4. [machine] A drag that starts on the line away from any point adds a point there and drags it; the photo follows the finger, and lifting the finger makes one Undo step.
   Proof: `CurvesSheetTest`, straight curve: press on the line at x = 0.5, move 40 dp up, lift: 3 points, preview calls during the move, exactly one commit; the sheet does not scroll.
5. [machine] Tapping or dragging a point selects it; only one point is selected at a time; switching channel, Undo, Redo and Reset clear the selection.
   Proof: `CurvesSheetTest`, four points: tap point 1 → selected 1; drag point 2 → selected 2; choose another channel → none; back, select, then change the curve from outside the panel (as Undo does) → none.
6. [machine] The delete button removes the selected point, then the neighbour to its left becomes selected (or to its right if it has none); end points can be deleted too.
   Proof: `CurvesSheetTest`, points at x 0, 0.3, 0.7, 1: select 0.7 and delete → 0, 0.3, 1 with 0.3 selected, one commit; select x = 0 and delete → 0.3, 1 with 0.3 selected, and the line is flat to the left of 0.3.
7. [machine] The delete button is disabled when no point is selected and when only two points remain.
   Proof: `CurvesSheetTest`: the delete button's node is not enabled on a straight curve with nothing selected, nor with two points and one selected; it is enabled with three points and one selected.
8. [machine] The first and last points move sideways as well as up and down; left of the first point and right of the last the line is flat at that point's height; a point still cannot pass its neighbour, and dragging past the frame holds the point at the edge.
   Proof: `t/tool/edit/CurveFormulaTest.kt`: moving the first point to (0.2, 0.1) and the last to (0.8, 0.9) gives table entries 0..51 equal to 26 ± 1 and 204..255 equal to 230 ± 1; an end point moved past its neighbour stops a minimum gap short of it; a move outside 0..1 is held at 0 or 1. `CurvesSheetTest`: dragging the first point right moves its x.
9. [machine] The line is a smooth natural spline through every point that may overshoot between points, cut off at pure black and pure white — so one raised point bends the whole line more than today and can make a flat white top.
   Proof: `CurveFormulaTest`: for points (0,0), (0.5,0.75), (1,1) the table at x = 0.125, 0.25, 0.375, 0.625, 0.75, 0.875 equals round(255 × 0.2168, 0.4219, 0.6035, 0.8535, 0.9219, 0.9668) ± 1; for (0,0), (0.25,0.15), (0.75,0.85), (1,1) it equals round(255 × 0.0563, 0.3063, 0.6938, 0.9438) at x = 0.125, 0.375, 0.625, 0.875 ± 1; for (0,0), (0.25,0.75), (1,1) every entry from x = 0.40 to 1 is 255 and none is outside 0..255. The drawn line and the table come from one function.
10. [machine] A curve still holds at most ten points: at ten points a tap on the line adds nothing.
    Proof: `CurvesSheetTest` with ten unevenly spaced points leaving a gap wider than 48 dp: a tap on the line in the middle of the gap adds no point and makes no commit.
11. [machine] Every drag is one Undo step, and adding and deleting a point are one Undo step each; Undo and Redo work across channels as before.
    Proof: `t/tool/edit/CurvesToolTest.kt`: add, drag and delete on Red, then add on Blue: four Undos return to a straight curve in reverse order and four Redos replay them; existing tests unedited and green.
12. [machine] Reset is an icon that straightens only the open channel and is disabled while that channel is straight.
    Proof: `CurvesSheetTest`: bend Red and Blue, open Red, press Reset → Red straight, Blue kept, one commit; Reset then not enabled; it is not enabled on a straight channel.
13. [machine] Curves still run on the phone's processor after HSL, and the saved photo matches the preview — including a curve that overshoots and a curve whose end points were moved.
    Proof: `t/tool/edit/CurveParityTest.kt` gains those two cases and stays green; `data/EditProcessor.kt` still applies curves after HSL (ADR-009).
14. [machine] RGB, HSL and the Colour group with its "changed" dots work exactly as after Run 13.
    Proof: `RgbMatrixTest`, `RgbSheetTest`, `HslSheetTest`, `HslParityTest`, `ColourGroupTest` unedited and green; every screenshot picture other than 95–100 and 105–108 passes `verifyRoborazziDebug` unchanged.

### Appearance and reachability

15. [machine] The graph is wide — about two to one — and shows only a frame and three vertical lines, with no diagonal guide and no horizontal lines.
    Proof: a UI test at 360 dp and 411 dp screen widths measures the plot's width : height between 1.8 and 2.6; pictures `app/src/test/screenshots/95_edit_curves_master_straight_light.png` and `96_..._dark.png`, judged once by the engine against this sentence, then pinned by `verifyRoborazziDebug`.
16. [machine] The channels read `RGB · R · G · B`, each with a colour dot (RGB's dot shows all three colours); a screen reader still says "RGB", "Red", "Green" and "Blue"; each entry is easy to tap.
    Proof: `t/tool/edit/CurveChipRowTest.kt` at 360 and 411 dp, light and dark: four entries, each visible label on one line, at least 48 dp to touch, spoken names as listed; pictures 95–98 show the dots.
17. [machine] The line takes the channel's colour — the normal text colour for RGB, and for Red, Green and Blue the same tints the RGB sheet already uses — and stands out at least 3 : 1 from the graph's background in light and dark.
    Proof: a contrast test (shape of `t/a11y/RgbTintContrastTest.kt`) over the five seeds it uses, light and dark: RGB line = theme `onSurface`, R/G/B = `RgbChannel.tintOn(...)`, each ≥ 3 : 1 against `surfaceContainerLowest`.
18. [machine] The selected point looks clearly different from the others, using only theme colours: the others are filled with the line's colour inside a ring of the background colour; the selected one is filled with the background colour inside a thicker ring of the line's colour.
    Proof: the contrast test of 17 checks the ring against the background ≥ 3 : 1; pictures 97/98 and 105/106 show a selected point, judged once, then pinned.
19. [machine] The delete (trash) and Reset buttons are icons in a column beside the graph, delete above Reset, each at least 48 dp to touch, with spoken names, and both on screen at 360 dp and 411 dp.
    Proof: `CurveChipRowTest`-style bounds checks for the delete tag and `CURVE_RESET_TAG`: inside the sheet and the window, ≥ 48 dp, delete's top above Reset's top, both right of the graph.
20. [machine] The Curves pictures show the new look: RGB straight with nothing selected (95, 96), Red S-curve with a selected point (97, 98), RGB with one point pulled high so the line overshoots into a flat white top (105, 106), and Blue with both end points moved in so the line is flat at both ends (107, 108); the two Lightness pictures (99, 100) are gone.
    Proof: `app/src/test/screenshots/95_…` to `98_…` re-recorded and `105_edit_curves_rgb_overshoot_{light,dark}.png`, `107_edit_curves_blue_moved_ends_{light,dark}.png` recorded once after the last Phase; the engine opens each and judges it against this sentence and criteria 15–19 before committing; `verifyRoborazziDebug` green afterwards.
21. [machine-then-human] On the Galaxy A13, in light and in dark, the Curves sheet has Meitu's arrangement in Kanso's colours: a wide graph, the four channel entries with dots, the coloured line, a selected point that stands out, and the two icon buttons beside the graph — nothing cut off, nothing hard to see.
    Proof: Verifier drives a Journey on `R58W1120QVA` from a fresh debug install: open a photo → Edit → Colour → Curves, in light then dark (Settings → theme), tap the line, switch to R; screenshots in the Journey report. A person opens the same screen and signs.
22. [machine-then-human] On the Galaxy A13 the curve handles well: tapping on the line adds a point on the first try, a tap beside it does not feel too strict or too loose, dragging is smooth with the photo following, end points slide sideways, a point can be deleted, a pulled point clips to white, and Undo and Redo walk back through it all.
    Proof: Journey on `R58W1120QVA` from a fresh debug install, a full-size camera photo: tap the line; tap 1 cm beside it; drag from the line; select by tap and by drag; drag the first point right; delete an end point; pull one point high; switch to R and back; Undo ×4, Redo ×4; Save. Did-not-fail evidence; the owner signs the feel, a stutter is a failure.

### Build, start and quality

23. [machine] The app builds, every test passes, lint shows no new problem, and the screenshot check is green.
    Proof: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug` exit 0; lint shows no issue in a changed file.
24. [machine] The house rules hold: every new or changed composable has a light and a dark preview, one type per file, every function explained in a comment, every screen still has its ViewModel and state.
    Proof: `PreviewCoverageTest`, `PreviewHygieneTest`, `PreviewsRenderTest`, `OneTypePerFileTest`, `KdocCoverageTest`, `ScreenStateTest` green.
25. [machine] The Curves sheet paints no colour of its own; every colour comes from the theme or the RGB sheet's existing tints.
    Proof: `grep -nE 'Color\(0x|Color\.hsv|Color\.(Red|Green|Blue|White|Black)' cp/` finds nothing outside `sample…` declarations and `@Preview` functions.
26. [machine-then-human] The README, the Edit feature docs and a new design record describe Curves as built — touch model, natural spline, free end points, two-point minimum, no Lightness — and the old record says which of its parts are replaced.
    Proof: README "Editing features" Curves row and "Curves, end to end"; `docs/features/edit.md`; `docs/features/tool-screen.md` Curves lines; new `docs/adr/ADR-011-…md`; `docs/adr/ADR-009-…md` marks its Fritsch-Carlson, x = 0/1 and Lightness lines superseded by ADR-011; README ADR table lists 011; grep finds no "Fritsch", "monotone", "long press" or "Lightness curve" about Curves in README or `docs/features`; every `app/src/…` path the README names exists. A person reads "Curves, end to end" against the phone.

## Removals

Repository class: product. Only what the PRD replaces or leaves unreachable.

R1. [machine] There is no Lightness channel in Curves.
    Proof: `CurveChannel.entries` is exactly RGB, RED, GREEN, BLUE; grep `LIGHTNESS` in `data/CurveChannel.kt`, `cp/` finds nothing.
R2. [machine] Curves hold no Lightness curve.
    Proof: grep `lightness` (any case) in `data/CurveAdjustments.kt` finds nothing.
R3. [machine] The photo gets no Lightness pass from Curves.
    Proof: `data/EditCurves.kt` has no `applyLightness`, `hslToRgb` or `pack`; `CurveParityTest` has no Lightness test.
R4. [machine] The Lightness label is gone.
    Proof: grep `edit_curves_channel_lightness` in `app/src` finds nothing.
R5. [machine] The two Lightness pictures and their tests are gone.
    Proof: no `99_edit_curves_lightness*` or `100_edit_curves_lightness*` in `app/src/test/screenshots/`; no `edit_curves_lightness_s_curve_` in `ToolScreenshotTest.kt`.
R6. [machine] A long press no longer removes a point.
    Proof: grep `longPress` in `cp/CurveGraph.kt` finds nothing; a `CurvesSheetTest` long press on a middle point keeps it.
R7. [machine] The diagonal guide and the horizontal grid lines are gone.
    Proof: `CurveGraph.kt` draws no diagonal and no horizontal grid line (grep `GRID_CELLS` and the diagonal's `drawLine` find nothing); picture 95 shows none.
R8. [machine] The text "Reset" button is gone; Reset is an icon.
    Proof: `cp/CurveResetButton.kt` has no `TextButton` and no `Text(`; it draws `ic_refresh` with the `edit_curves_reset` spoken name.
R9. [machine] The old never-overshooting (monotone) curve code is gone.
    Proof: grep `Fritsch|tangents\(` in `data/ToneCurve.kt` finds nothing.
R10. [machine] The rule pinning the first point at the left edge and the last at the right edge is gone.
    Proof: `data/ToneCurve.kt`'s `init` has no `x == 0f` / `x == 1f` check, and `removed()` accepts the first and last index while more than two points remain.

Questions (product repository — none of these is a removal):
- `MAX_POINTS = 10` and the minimum gap of 0.02 between points are not in the PRD; the plan keeps both.
- New Curves files stay in the existing `curvespanel/` package beside today's four, rather than moving the panel to the `components/` tree CLAUDE.md describes; a move is not in this PRD.

## Constraints

- Every colour, font, shape and spacing from Kanso's theme in light and dark; Meitu's dark panel is not copied (PRD 4).
- Not in this run: Meitu's preset row and its Hide/Show button (PRD 6).
- CLAUDE.md holds: previews light and dark, one type per file, KDoc, README in the same change.
- RGB and HSL do not change (PRD).

## Verification Evidence Required

| Criterion | Class | Evidence / what to look at | Signed off |
|---|---|---|---|
| 1–14 | machine | named tests in `:app:testDebugUnitTest` | n/a |
| 15–20 | machine | named tests; pictures 95–98, 105–108 judged once, then `verifyRoborazziDebug` | n/a |
| 21 | machine-then-human | Journey on A13, light and dark; then open Edit → Colour → Curves and look | ☐ |
| 22 | machine-then-human | Journey on A13; then try every gesture listed and judge the feel | ☐ |
| 23–25 | machine | gradle commands, guard tests, grep | n/a |
| 26 | machine-then-human | greps; then read "Curves, end to end" beside the phone | ☐ |
| R1–R10 | machine | greps and tests named above | n/a |
