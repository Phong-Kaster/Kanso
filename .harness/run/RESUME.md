# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this file is wrong.

- **Stage:** build - Phase 1 done (T-001 complete); Phase 2 next
- **Next Phase:** Phase 2 (T-002 and T-003, scopes disjoint)
  - T-002 (Fast): `app/src/main/java/com/phongkaster/kanso/data/edit/edit/ToneCurve.kt`, `app/src/test/java/com/phongkaster/kanso/tool/edit/{CurveFormulaTest,CurveParityTest,CurvesToolTest}.kt`
  - T-003 (Fast, 2 slices): A: `app/src/main/java/com/phongkaster/kanso/ui/tool/component/toolpanel/editpanel/curvespanel/{CurveGraph,CurveChips,CurveResetButton,CurvesPanel,CurveChannelDot(new)}.kt` + `app/src/main/res/values/strings_edit_curves.xml` (new keys only); B: `app/src/test/java/com/phongkaster/kanso/tool/edit/CurveChipRowTest.kt`, `app/src/test/java/com/phongkaster/kanso/a11y/CurveLineContrastTest.kt` (new)
  - Wiring after: README.md, `docs/features/edit.md`, `docs/features/tool-screen.md`, `docs/adr/ADR-011-...md` (new), `docs/adr/ADR-009-...md`; record every test edit in `evidence/test-edits.md`
  - T-002 must not touch CurvesPanel/CurveGraph (T-003 owns them); contract in PLAN.md.
- **Queued decisions:** 0 (D-001 answered). Mode: Autonomous.
- **Abandoned / Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && timeout 2400 ./gradlew :app:assembleDebug` | test: `... :app:testDebugUnitTest` (a full run OOMs on StickerScreenshotTest; rerun those alone with `--tests '*StickerScreenshotTest*'`; delete `app/hs_err_pid*.log`) | lint: `... :app:lintDebug` | pictures: `... :app:verifyRoborazziDebug`. Run long Gradle in the background writing to evidence/ and wait with a Bash until-loop (no Monitor tool).
- **Criteria the next Phase cites (quoted from DoD.md):**
  - **8:** The first and last points move sideways as well as up and down; left of the first point and right of the last the line is flat at that point's height; a point still cannot pass its neighbour, and dragging past the frame holds the point at the edge.
  - **9:** The line is a smooth natural spline through every point that may overshoot between points, cut off at pure black and pure white - so one raised point bends the whole line more than today and can make a flat white top.
  - **11:** Every drag is one Undo step, and adding and deleting a point are one Undo step each; Undo and Redo work across channels as before.
  - **13:** Curves still run on the phone's processor after HSL, and the saved photo matches the preview - including a curve that overshoots and a curve whose end points were moved.
  - **15:** The graph is wide - about two to one - and shows only a frame and three vertical lines, with no diagonal guide and no horizontal lines.
  - **16:** The channels read `RGB · R · G · B`, each with a colour dot (RGB's dot shows all three colours); a screen reader still says "RGB", "Red", "Green" and "Blue"; each entry is easy to tap.
  - **17:** The line takes the channel's colour - the normal text colour for RGB, and for Red, Green and Blue the same tints the RGB sheet already uses - and stands out at least 3 : 1 from the graph's background in light and dark.
  - **19 (Reset part):** The delete (trash) and Reset buttons are icons in a column beside the graph, delete above Reset, each at least 48 dp to touch, with spoken names, and both on screen at 360 dp and 411 dp.
  - **25:** The Curves sheet paints no colour of its own; every colour comes from the theme or the RGB sheet's existing tints.
  - **R7:** The diagonal guide and the horizontal grid lines are gone. **R8:** The text "Reset" button is gone; Reset is an icon. **R9:** The old never-overshooting (monotone) curve code is gone. **R10:** The rule pinning the first point at the left edge and the last at the right edge is gone.
  - (Exact numbers for DoD 9 and each task's full text: its task file.)
- **Open issues touching the next Phase's files:** none new (the Run 12 97/98 S-curve issue belongs to Phase 3, T-004).
- **Reference topics:** `reference/run14-facts.md`, `reference/run12-facts.md`; Constraints: `reference/constraints-verbatim.md` (pass its path in Briefs; Workers read it)
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
