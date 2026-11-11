# Test edits — Run 14

> Every test added, edited, renamed or removed, with its reason (PROJECT.md Constraint; DoD 14, 24).

## Phase 1 (T-001) - removed because the Lightness channel no longer exists (DoD R1, R2, R3, R5, owner-approved D-001)

- `CurveFormulaTest`: removed `a_lightness_curve_keeps_hue_and_saturation`, `greys_follow_the_lightness_curve`, `a_lightness_curve_can_reach_pure_black_and_pure_white`, and the helpers `lightness(...)`, `hueAndSaturation` - they tested the deleted Lightness pass.
- `CurveParityTest`: removed `a_lightness_curve_matches_done` - the Lightness pass is gone.
- `CurvesSheetTest`: removed `the_lightness_chip_can_be_chosen` and `the_chosen_lightness_chip_is_remembered_when_the_sheet_is_drawn_again` (no such chip); renamed `the_five_chips_have_names_and_are_at_least_48_dp` to `the_four_chips_have_names_and_are_at_least_48_dp` (count now four).
- `ToolScreenshotTest`: removed `edit_curves_lightness_s_curve_light` and `_dark` (pictures 99/100, deleted with owner approval under `## Baselines`).
- `CurveChipRowTest`: KDoc only.

## Phase 2 (T-002, T-003)


## Phase 2 (Curves: natural spline, free end points, new look)

| File | Test | What and why |
|---|---|---|
| CurveFormulaTest | `the_table_never_overshoots_between_two_points` | Removed. The monotone no-overshoot rule is gone on purpose (ADR-011); overshoot is now intended. |
| CurveFormulaTest | natural-spline known-value tests (3-point, 4-point) | Added, replacing it: pin the spline to hand-computed values. |
| CurveFormulaTest | `an_overshoot_is_clipped_to_white` | Added: an overshooting curve is clipped to 0..1. |
| CurveFormulaTest | `the_curve_is_flat_beyond_its_end_points` | Added: flat at the end point's height outside the first and last point. |
| CurveFormulaTest | `added_beyond_the_ends_makes_a_new_end_point` | Added: a point outside the ends becomes the new end point. |
| CurveFormulaTest | `a_steep_s_curve_stays_inside_black_and_white_and_rises` | Renamed `..._black_and_white`; the "never falls" check dropped because a spline may overshoot. |
| CurveFormulaTest | `moved_cannot_pass_a_neighbour_and_ends_keep_their_x` | Renamed `..._ends_move_sideways`: end points now move in x. |
| CurveFormulaTest | `removed_ignores_the_end_points` | Renamed `removed_keeps_at_least_two_points`: any point can be removed while more than 2 remain. |
| CurveParityTest | `overshooting_and_moved_end_curves_match_done` | Added: preview and Done agree for overshooting curves and moved end points. |
| CurvesToolTest | `four_curve_edits_undo_in_reverse_and_redo_in_order` | Added: Undo/Redo order over four curve edits. |
| CurveChipRowTest | whole file | Rewritten for short labels, the icon Reset button, graph ratio 1.8 to 2.6 and the spoken names RGB, Red, Green, Blue. |
| CurveLineContrastTest | new file | Added: the curve line has 3:1 contrast against surfaceContainerLowest, five seeds, light and dark. |

## Phase 3 (T-004)
- CurvesSheetTest `a_tap_on_empty_space_adds_a_point` -> rewritten as `a_tap_on_the_line_adds_a_point_on_the_line_and_selects_it` (a tap on empty space no longer adds; a tap on the line adds at the line's height).
- `a_drag_moves_the_point_and_commits_once` now starts from an existing point (bentSettings) instead of a tap-added one; it expects exactly 1 commit.
- `at_ten_points_a_tap_adds_nothing` re-aimed at the line with ten unevenly spaced points (gap ~190 dp).
- `a_long_press_removes_a_middle_point` deleted, replaced by `a_long_press_keeps_the_point` (DoD R6 reverses the old behaviour, approved in the DoD).
Reason for all: DoD criteria 1-4, 10, R6.
- CurvesSheetTest, added (DoD 1-7): `a_tap_away_from_the_line_adds_nothing_and_clears_the_selection`, `a_tap_on_a_point_selects_it`, `a_point_within_reach_wins_over_the_line`, `a_drag_from_the_line_adds_a_point_and_commits_once`, `delete_is_disabled_with_nothing_selected_and_with_two_points`, `delete_removes_the_selected_point_and_selects_the_left_neighbour`, `deleting_the_first_point_selects_the_new_first_point`, `reset_clears_the_selection`, `undo_redo_and_a_channel_change_clear_the_selection`; new helper `curvesSettings(points)`.
- CurveLineContrastTest, added (DoD 18): `light_selected_point_ring_reaches_3_to_1_on_the_background`, `dark_selected_point_ring_reaches_3_to_1_on_the_background`.
- ToolScreenshotTest (DoD 20): `drawSCurve` now drags from the line instead of two clicks (pictures 95-98 re-recorded, approved under Baselines); added `edit_curves_rgb_overshoot_light/_dark` (105/106) and `edit_curves_blue_moved_ends_light/_dark` (107/108); helpers `pullRgbPointHigh`, `moveBlueEnds`, `settleCurves`.
