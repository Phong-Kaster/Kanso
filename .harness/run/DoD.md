# Definition of Done — Run 12: Curves on the Edit screen

> Derived from `PRD.md` at bootstrap (2026-11-06). Human-owned after approval: the engine may propose
> changes (Tier 3) but never apply them. Every criterion declares WHO can verify it (ADR-015, ADR-030).
>
> PRD, verbatim: "plan, write code and test on galaxy a13 to build curve feature which relate with RGB , HSL".
> Reading taken (approve or change it in D-001): a new **Curves** tool on the Edit screen, right after
> HSL, with one tone curve each for **RGB** (all colours), **Red**, **Green**, **Blue** and **Lightness**
> (the L of HSL). Hue curves are an optional extra at the end of this file: delete that section to leave
> them out.

## Status

- [ ] APPROVED — approve via D-001 in `.harness/run/ESCALATION.md` (answer in `DECISIONS.md`); edit
      criteria freely before approving. Approval covers the Verification Class of each criterion.

## Acceptance Criteria

### Behaviour

1. [machine] The Edit screen has a new tool, Curves, right after HSL, and it opens a sheet with a graph showing a straight diagonal line between two end points.
   Proof: `EditTool` order is `…, RGB, HSL, CURVES, FILTERS, …` with 10 entries (`RgbMatrixTest`/`StickerAddTest` counts updated, `HslSheetTest` HSL-before-FILTERS assert rewritten); `CurvesSheetTest`, driven from a photo opened on Edit with default settings: `scrollToolRowTo(CURVES)`, tap it, the curve graph node and the RGB chip are displayed and the master curve holds exactly the points (0,0) and (1,1).

2. [machine] A curve left as a straight diagonal changes nothing in the photo.
   Proof: `CurveFormulaTest`: the neutral curve's table is the identity for every channel; Done (`EditProcessor.render`) with only neutral curves is byte-equal to Done with no Curves at all on a bitmap holding all 256 levels of each channel; `EditSettings.isNeutral(CURVES)` is true for the defaults.

3. [machine] The curve passes exactly through every point the user placed, never overshoots between two points, and never goes below black or above white.
   Proof: `CurveFormulaTest`, one named test each, driven from point sets written in the test: two points give a straight line; a flat segment stays flat; a falling (inverted) curve works; between two neighbouring points every table entry lies within their two heights (monotone cubic, Fritsch–Carlson); the table has exactly 256 entries, entry 0 and entry 255 equal the end points' heights; all entries are within 0..255; two points closer than the minimum gap cannot exist.

4. [machine] Tapping an empty spot on the graph adds a point there, dragging a point moves it (it cannot pass its neighbours, and the two end points only move up and down), and a long press removes a middle point; a curve holds at most 10 points.
   Proof: `CurvesSheetTest`, `performTouchInput` (`down`/`moveTo`/`up`, `click`, `longClick`), driven from a neutral master curve: the settings' points change as stated; an 11th tap adds nothing; a long press on an end point removes nothing; a vertical drag moves the point and does not scroll the sheet.

5. [machine] Each tap, drag, removal or Reset is exactly one Undo step, and Redo brings it back.
   Proof: `CurvesToolTest` (hand-built ViewModel as in `HslToolTest`), driven from a fresh session: a drag of N `previewSettings` calls and one `commitSettings` raises the undo depth by 1; Undo restores the earlier curve and Redo the later one; a tap, a long press and Reset each raise it by 1.

6. [machine] Reset puts the curve on screen back to a straight diagonal, and is disabled while that curve is already straight.
   Proof: `CurvesSheetTest`/`CurvesToolTest`, driven from a master curve with two extra points: tapping Reset leaves only (0,0) and (1,1) on the chosen curve and leaves the other curves untouched; on a neutral curve the Reset node is not enabled.

7. [machine] The photo follows the finger while a point is dragged, even with no filter chosen, and what is shown matches the saved photo.
   Proof: `CurvesPreviewTest`, driven from a fresh session with no filter, no HSL change and no layer: a `previewSettings` with only a curve change produces a new, non-null filter preview; `CurveParityTest` (`@GraphicsMode(NATIVE)`, as `HslParityTest`): the preview render and the Done render scaled to the preview's size agree within `HslParityTest`'s tolerance for filter + HSL + curves + brightness set together.

8. [machine] Curves are applied after the Filter and HSL, and before Adjust and RGB, both while editing and in the saved photo.
   Proof: an order test in `CurveParityTest` (or `CurveFormulaTest`) with a combination that does not commute (a filter, an HSL change, an S-curve and a brightness change): Done and the preview each equal filter → HSL → curves → colour matrix, and differ from curves placed after the colour matrix.

9. [machine] The sheet has chips for RGB, Red, Green, Blue and Lightness; RGB is chosen the first time; choosing a chip changes nothing in the photo and adds no Undo step; the chosen chip is remembered when the sheet is opened again.
   Proof: `CurvesToolTest` (as `chooseHslChannel` in `HslToolTest`) and `CurvesSheetTest`, driven from Curves opened on a fresh session: the chosen channel starts as RGB; choosing Green leaves the settings and the undo depth unchanged; after opening another tool and Curves again, Green is still chosen.

10. [machine] A Red, Green or Blue curve changes only that colour, and the RGB curve changes all three alike, after the single-colour curves.
    Proof: `CurveFormulaTest`, driven from synthetic pixels: a Red-only S-curve leaves green and blue byte-equal (and likewise for Green and Blue); with both a Red curve and an RGB curve set, each red value equals `rgb(red(x))`.

11. [machine] The Lightness curve changes only how light the photo is: colours keep their hue and saturation, greys follow it too, and it can reach pure black and pure white.
    Proof: `CurveFormulaTest`, driven from synthetic pixels: for coloured pixels (HSL saturation ≥ 0.1, lightness 0.1..0.9 before and after) hue stays within 2° and saturation within 0.02; a grey pixel stays grey and its level equals the curve's value at its lightness; a curve through (0.5, 0) turns mid-grey black and one through (0.5, 1) turns it white.

12. [machine] The Curves tool's "changed" dot shows only while some curve is bent; pressing Back with only a curve change asks to discard it; Compare shows the photo without the curves.
    Proof: `EditSettings.isNeutral(CURVES)` test (neutral vs one bent channel); an `onBackPressed` test in `CurvesToolTest` driven from settings with only a curve change; the `CompareTest` loops over `EditTool.entries` include CURVES and pass.

13. [machine] The saved photo contains the curve.
    Proof: a Done render test, driven from a 3000 px source with only an S-curve on RGB: every output pixel equals the curve's table applied to its input pixel; the saved file's size equals the source's (no downscale).

14. [machine] The chips and Reset have spoken names and touch areas of at least 48 dp, and the graph has a spoken description naming the chosen curve.
    Proof: `CurvesSheetTest`, driven from Curves opened on a fresh session: each chip's and Reset's touch bounds are ≥ 48 dp in both directions (density converted as in `HslSheetTest`); each has a non-empty content description or text; the graph's content description contains the chosen chip's name.

### Appearance and reachability

15. [machine-then-human] The Curves sheet looks right in light and in dark: the graph, its line and points, the chips and Reset are clearly visible, the chosen chip is obvious, and nothing is cut off.
    Proof: new `ToolScreenshotTest` pictures numbered from 95: master curve straight (light, dark), an S-curve on Red (light, dark), the Lightness chip with a bent curve (light, dark); recorded through `foreman-record-baselines.ps1`, opened and judged by the Iteration, then pinned by `verifyRoborazziDebug`. Then a person opens the six pictures and signs.

16. [machine-then-human] On the Galaxy A13, the owner can open Curves on a photo, add and drag a point and see the photo follow; an S-curve on RGB adds contrast, raising the Red curve makes the photo redder, and Done saves the result to My edits.
    Proof: driven by the Verifier through `foreman-device.ps1` from this state: the A13 attached and unlocked, the debug build installed over the existing app with `-Op install` (no `-Op clear`, so the owner's data stays), at least one photo in the gallery; start, open a photo in Edit, scroll the tool row to Curves, tap it, swipe on the graph, take a screenshot kept outside the repository. A passing pre-check is written "did not fail when driven from that state". Then the owner tries it and signs.

17. [human-only] On the Galaxy A13, dragging a point on a full-size camera photo is smooth: the photo follows the finger with no stutter, on RGB, Red and Lightness.
    Proof: on the A13, open a full-size camera photo in Edit → Curves. On RGB, Red and Lightness, drag a middle point from bottom to top and back. Expect the photo to follow the finger smoothly. A stutter is a failure to report, not to ship.

### Build, start and quality

18. [machine] The app builds, every existing and new test passes, and the lint and screenshot checks pass.
    Proof: `:app:assembleDebug`, `:app:testDebugUnitTest` (at least 1212 + the new tests, 0 failed, 0 skipped, run count equal to the `@Test` count), `:app:lintDebug` and `:app:verifyRoborazziDebug` all exit 0 at the final commit. An existing picture that changes only because the row has a tenth entry is re-recorded only if it is listed under `## Baselines` in `DECISIONS.md`. Every test added, changed or removed is listed with its reason in `.harness/run/evidence/test-edits.md`.

19. [machine] No new library, no new permission, and still no network.
    Proof: `git diff main -- gradle/libs.versions.toml '**/build.gradle.kts'` adds no dependency; the merged debug manifest's permissions equal `main`'s and include no `INTERNET`.

20. [machine] The new code follows the project's conventions: every composable has light and dark previews, one type per file, plain-English comments on everything, new texts in their own file, no hard-coded colours.
    Proof: `PreviewsRenderTest`, `PreviewCoverageTest`, `OneTypePerFileTest`, `KdocCoverageTest` pass; new UI files live under `ui/tool/component/toolpanel/editpanel/curvespanel/` (one composable per file, CLAUDE.md hierarchy); new strings only in `res/values/strings_edit_curves.xml`, every name starting `edit_curves_`; `git diff main -- app/src/main` adds no `Color(0x` outside a `sample…` declaration or `@Preview`.

21. [machine] Nothing outside the feature changes: Retica's reference code, the engine modules and the HSL formula are untouched.
    Proof: `git diff main --stat -- reference/ core/ opencv/ libs/ app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditHsl.kt` is empty; the HSL tests and pictures 83-94 pass unchanged unless listed under `## Baselines`.

22. [machine-then-human] The docs tell the truth about Curves.
    Proof: the README's "Editing features" table has a Curves row with its real state, its limits, and a "Curves, end to end" section (screen and ViewModel, each step in order, CPU path and threading, live preview size, Done output, what happens on failure, file paths); `docs/features/edit.md` and `docs/features/tool-screen.md` describe Curves; `docs/adr/ADR-009-…` records why curves run on the CPU after HSL. Pre-check: every `app/src/…` path the README names exists. Then the owner reads them, uses Curves, and notes any untrue sentence.

### Optional: hue curves (delete this whole section to leave them out)

23. [machine] Two more chips, Hue → Saturation and Hue → Lightness, show a flat line across a rainbow strip; raising the line over a hue makes that hue more saturated (or lighter), the two ends of the strip are the same hue, and greys are left alone.
    Proof: `CurveFormulaTest`, driven from synthetic pixels: a point raised over blue (240°) raises the saturation (lightness) of blue pixels and leaves red pixels within 0.02; a point at 0° and 360° hold the same value; a grey pixel is byte-equal; a flat line changes nothing; `CurvesSheetTest` shows the two chips.

24. [machine-then-human] The hue strip under the hue curves is easy to read in light and in dark.
    Proof: two more `ToolScreenshotTest` pictures (light, dark) of Hue → Saturation with a raised point, recorded through the wrapper and judged by the Iteration; then a person signs.

## Removals

None. Kanso is a product, and Curves replaces nothing: RGB and HSL stay as they are.

Questions (product repositories only): none.

## Constraints

- Curves run on the CPU, after HSL and before the colour matrix, like HSL (ADR-008); no GPU shader (`minSdk` 27 has no AGSL). Recorded as ADR-009.
- No new library, permission or network access; offline only.
- Like every other Edit tool today, curves are lost if Android kills the app in the background; nothing new is saved.
- Points are edited by touch; editing points with TalkBack is out of scope (the chips, Reset and the graph's description are spoken).
- The debug build keeps `x86_64`, `arm64-v8a` and `armeabi-v7a`; the owner's phone is a 32-bit Galaxy A13.
- `reference/`, `core/`, `opencv/`, `libs/` and `EditHsl.kt` are not edited.

## Verification Evidence Required

| Criterion | Class | Evidence / what to look at | Signed off |
|---|---|---|---|
| 1-14 | machine | the named tests in `:app:testDebugUnitTest`, exit 0 | n/a |
| 15 | machine-then-human | pictures 95-100 in `app/src/test/screenshots/`: graph, line, points, chips and Reset visible in light and dark, chosen chip obvious, nothing cut off | ☐ |
| 16 | machine-then-human | on the A13: Edit → Curves, tap the graph, drag the point; RGB S-curve adds contrast, raised Red makes it redder, Done → My edits shows it | ☐ |
| 17 | human-only | on the A13, full-size photo, drag a point end to end on RGB, Red, Lightness: smooth | ☐ |
| 18-21 | machine | assemble, test, lint, verifyRoborazzi exit 0; the `git diff` checks above print nothing | n/a |
| 22 | machine-then-human | README Curves row and "Curves, end to end", `docs/features/edit.md`, `tool-screen.md`, ADR-009: no untrue sentence | ☐ |
| 23 | machine | (optional) `CurveFormulaTest` hue cases | n/a |
| 24 | machine-then-human | (optional) the two hue-strip pictures | ☐ |
