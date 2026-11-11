# Test edits — Run 14

> Every test added, edited, renamed or removed, with its reason (PROJECT.md Constraint; DoD 14, 24).

## Phase 1 (T-001) - removed because the Lightness channel no longer exists (DoD R1, R2, R3, R5, owner-approved D-001)

- `CurveFormulaTest`: removed `a_lightness_curve_keeps_hue_and_saturation`, `greys_follow_the_lightness_curve`, `a_lightness_curve_can_reach_pure_black_and_pure_white`, and the helpers `lightness(...)`, `hueAndSaturation` - they tested the deleted Lightness pass.
- `CurveParityTest`: removed `a_lightness_curve_matches_done` - the Lightness pass is gone.
- `CurvesSheetTest`: removed `the_lightness_chip_can_be_chosen` and `the_chosen_lightness_chip_is_remembered_when_the_sheet_is_drawn_again` (no such chip); renamed `the_five_chips_have_names_and_are_at_least_48_dp` to `the_four_chips_have_names_and_are_at_least_48_dp` (count now four).
- `ToolScreenshotTest`: removed `edit_curves_lightness_s_curve_light` and `_dark` (pictures 99/100, deleted with owner approval under `## Baselines`).
- `CurveChipRowTest`: KDoc only.
