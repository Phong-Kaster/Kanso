# DECISIONS

> Your answers go here, never in `.harness/run/ESCALATION.md`. That file is the engine's own log —
> it may still be appending to it or rewriting it long after this file first appeared, because
> queuing a decision does not stop the run (the engine keeps working on everything else it can).
> Waiting for `ESCALATION.md` to exist is not the same as the engine having actually stopped, and a
> decision written into that file while the engine is still using it can be lost or half-read.
>
> This file is yours alone. The Runtime denies the engine both Edit and Write on it, mechanically —
> not by asking it nicely — so nothing you write here can ever race an engine write.
>
> For each entry you want to answer, copy its id from `ESCALATION.md` (e.g. `D-001`) as a heading
> below, and write your decision and rationale under it. The rationale joins the audit trail.
> The engine picks up every id present here at the **start** of its next Iteration, never mid-flight
> — so answer, save, and only then re-run. Wait for `Status: ESCALATE` in the run's output as your
> signal that it is safe to answer, not for this file or `ESCALATION.md` merely existing.

---

## D-001

**Approved by the owner, 2026-11-06.**

1. **DoD approved** as written in `.harness/run/DoD.md`, with the engine's reading of the row:
   - **a. Compare stays pinned** at the right on layer 2 too.
   - **b. Tapping Colour only switches the row** (and closes any other tool's open sheet); no colour
     tool opens until one is picked.
   - **c. The phone's Back gesture keeps today's behaviour** on layer 2 (close an open sheet, then ask
     to discard or leave); it does not step back to layer 1, as in Beautify.
   - **d. Share Beautify's pieces:** move Beautify's Back entry and its sideways slide into shared
     pieces both rows use; Beautify must look and behave exactly as now (criterion 10).
2. **Icon:** Colour takes the palette icon RGB has today; **RGB gets Material Symbols Rounded
   "instant_mix"**.
3. **Pictures:** all 43 listed in D-001 may be re-recorded (list under `## Baselines` below, written
   as bullets because `foreman-record-baselines.ps1` reads only `- ` lines; the block in D-001 had
   none). An unlisted picture that changes is a defect to fix, not a re-record.
4. **Capabilities granted:** the standing Gradle assemble / testDebugUnitTest / lintDebug grant,
   **permanent** (the owner already approved it permanently in Run 12's D-001, but it was never
   written to `.harness/knowledge/capabilities.json`); and the one-file `instant_mix` download,
   **goal-scoped**.
5. **Mode:** the owner switches this run to Autonomous after approval.

## Baselines

- `app/src/test/screenshots/16_tool_photo_light.png`
- `app/src/test/screenshots/17_tool_photo_dark.png`
- `app/src/test/screenshots/40_tool_narrow_photo_light.png`
- `app/src/test/screenshots/41_tool_narrow_photo_dark.png`
- `app/src/test/screenshots/42_tool_narrow_photo_sdk30_light.png`
- `app/src/test/screenshots/51_tool_edit_free_crop_light.png`
- `app/src/test/screenshots/52_tool_edit_free_crop_dark.png`
- `app/src/test/screenshots/55_edit_tool_row_light.png`
- `app/src/test/screenshots/56_edit_tool_row_dark.png`
- `app/src/test/screenshots/57_edit_crop_sheet_light.png`
- `app/src/test/screenshots/58_edit_crop_sheet_dark.png`
- `app/src/test/screenshots/59_edit_rotate_sheet_light.png`
- `app/src/test/screenshots/60_edit_rotate_sheet_dark.png`
- `app/src/test/screenshots/61_edit_adjust_brightness_light.png`
- `app/src/test/screenshots/62_edit_adjust_brightness_dark.png`
- `app/src/test/screenshots/63_edit_filters_1977_light.png`
- `app/src/test/screenshots/64_edit_filters_1977_dark.png`
- `app/src/test/screenshots/65_edit_compare_on_light.png`
- `app/src/test/screenshots/66_edit_compare_on_dark.png`
- `app/src/test/screenshots/67_edit_pill_light.png`
- `app/src/test/screenshots/68_edit_pill_dark.png`
- `app/src/test/screenshots/69_edit_stickers_row_light.png`
- `app/src/test/screenshots/70_edit_stickers_row_dark.png`
- `app/src/test/screenshots/81_edit_stickers_crop_filter_light.png`
- `app/src/test/screenshots/82_edit_stickers_crop_filter_dark.png`
- `app/src/test/screenshots/83_edit_tool_row_hsl_light.png`
- `app/src/test/screenshots/84_edit_tool_row_hsl_dark.png`
- `app/src/test/screenshots/85_edit_hsl_red_hue_light.png`
- `app/src/test/screenshots/86_edit_hsl_red_hue_dark.png`
- `app/src/test/screenshots/87_edit_hsl_blue_saturation_light.png`
- `app/src/test/screenshots/88_edit_hsl_blue_saturation_dark.png`
- `app/src/test/screenshots/89_edit_hsl_orange_lightness_light.png`
- `app/src/test/screenshots/90_edit_hsl_orange_lightness_dark.png`
- `app/src/test/screenshots/91_edit_hsl_blue_after_light.png`
- `app/src/test/screenshots/92_edit_hsl_blue_after_dark.png`
- `app/src/test/screenshots/93_edit_hsl_orange_after_light.png`
- `app/src/test/screenshots/94_edit_hsl_orange_after_dark.png`
- `app/src/test/screenshots/95_edit_curves_master_straight_light.png`
- `app/src/test/screenshots/96_edit_curves_master_straight_dark.png`
- `app/src/test/screenshots/97_edit_curves_red_s_curve_light.png`
- `app/src/test/screenshots/98_edit_curves_red_s_curve_dark.png`
- `app/src/test/screenshots/99_edit_curves_lightness_s_curve_light.png`
- `app/src/test/screenshots/100_edit_curves_lightness_s_curve_dark.png`

<!--
A Human Verification Request is answered by ticking, not by prose. The engine writes the empty
checklist under the entry's id; you change `[ ]` to `[x]` for each item you have confirmed, and add a
line under any you are failing saying what you saw instead. Shape:

## D-002

- [ ] 15 - fresh install, grant when asked, open -> the greeting appears
- [x] 16 - second open the same day -> no second greeting
- [ ] 18 - next calendar day -> the greeting comes back

The ticks live HERE and only here. `FOREMAN.html` shows the same list so you can follow along
with a phone in your hand, but that page is rebuilt from the engine's files after every iteration
and nothing on it is ever read back. This file is the one
the Runtime denies the engine write access to, mechanically, which is the whole reason a tick in it
cannot be lost.

An item left unticked stays queued. Partial answers are fine and expected: answer what you have
actually checked, and the rest waits.
-->
