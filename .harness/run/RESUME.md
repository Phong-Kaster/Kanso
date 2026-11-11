# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this file is wrong.

- **Stage:** build - Phase 2 done (T-002, T-003 complete); Phase 3 next
- **Next Phase:** Phase 3 (T-004 alone, 2 slices; slices sequential)
  - Slice A: `app/src/main/java/com/phongkaster/kanso/ui/tool/component/toolpanel/editpanel/curvespanel/{CurveGraph,CurvesPanel,CurveDeleteButton(new)}.kt`, `app/src/main/res/values/strings_edit_curves.xml` (new key only)
  - Slice B: `app/src/test/java/com/phongkaster/kanso/tool/edit/CurvesSheetTest.kt`, `app/src/test/java/com/phongkaster/kanso/a11y/CurveLineContrastTest.kt`, `app/src/test/java/com/phongkaster/kanso/screenshots/ToolScreenshotTest.kt`
  - `ic_delete.xml` now exists (Iteration wrote it). CurvesPanel has a 48 dp empty Spacer in the button column's top slot for the delete button; `curveLineColorFor(scheme, channel)` is the shared line-colour function; `ToneCurve.valueAt` exists. Wiring after: README.md, docs/features/edit.md, tool-screen.md, ADR-011 (add selection/delete), record test edits in evidence/test-edits.md (append, never overwrite). Then the Iteration re-records pictures 95-98, adds 105-108 (approved under Baselines in DECISIONS.md).
- **Queued decisions:** 0. Mode: Autonomous.
- **Abandoned / Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && timeout 2400 ./gradlew :app:assembleDebug` | test: `... :app:testDebugUnitTest` (OOM on StickerScreenshotTest possible; rerun alone; delete `app/hs_err_pid*.log`) | lint: `... :app:lintDebug` | pictures: `... :app:verifyRoborazziDebug`. Scope check: `foreman-check-scope.ps1 -Tasks T-004 -Wiring ...` (paths in full, no ellipsis).
- **Criteria T-004 cites (quoted from DoD.md):**
  - 1: Tapping on the curve's line adds a point exactly on the line where you tapped, at the line's own height (not the finger's), and that point becomes the selected one.
  - 2: A tap away from the line and away from every point adds nothing and only clears the selection.
  - 3: A tap counts as "on the line" when it lands within 24 dp of the drawn line - the same reach as grabbing a point today; when a point is also within 24 dp, the point wins and is selected instead.
  - 4: A drag that starts on the line away from any point adds a point there and drags it; the photo follows the finger, and lifting the finger makes one Undo step.
  - 5: Tapping or dragging a point selects it; only one point is selected at a time; switching channel, Undo, Redo and Reset clear the selection.
  - 6: The delete button removes the selected point, then the neighbour to its left becomes selected (or to its right if it has none); end points can be deleted too.
  - 7: The delete button is disabled when no point is selected and when only two points remain.
  - 10: A curve still holds at most ten points: at ten points a tap on the line adds nothing.
  - 12: Reset is an icon that straightens only the open channel and is disabled while that channel is straight.
  - 18: The selected point looks clearly different from the others, using only theme colours: the others are filled with the line's colour inside a ring of the background colour; the selected one is filled with the background colour inside a thicker ring of the line's colour.
  - 19: The delete (trash) and Reset buttons are icons in a column beside the graph, delete above Reset, each at least 48 dp to touch, with spoken names, and both on screen at 360 dp and 411 dp.
  - 20: Curves pictures: RGB straight nothing selected (95, 96), Red S-curve with a selected point (97, 98), RGB one point pulled high overshooting to a flat white top (105, 106), Blue with both end points moved in (107, 108); 99/100 gone.
  - R6: A long press no longer removes a point.
  - (Full task text: TASKS/T-004.md; contract: PLAN.md.)
- **Open issues touching the next Phase's files:** none.
- **Reference topics:** `reference/run14-facts.md`, `reference/run12-facts.md`; Constraints: `reference/constraints-verbatim.md` (pass its path in Briefs)
- **Model tiers:** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
