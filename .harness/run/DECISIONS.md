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

Approved, option 1 (owner, 2026-11-04): the Definition of Done in `.harness/run/DoD.md` is approved
as written, all 37 criteria with their verification classes, and assumptions A-001 to A-004 stand.
The 35 existing pictures listed under `## Baselines` below may be re-recorded, each old version kept
in `.harness/run/evidence/baseline-diffs/`.

## Baselines

- 16_tool_photo_light.png
- 17_tool_photo_dark.png
- 40_tool_narrow_photo_light.png
- 41_tool_narrow_photo_dark.png
- 42_tool_narrow_photo_sdk30_light.png
- 51_tool_edit_free_crop_light.png
- 52_tool_edit_free_crop_dark.png
- 55_edit_tool_row_light.png
- 56_edit_tool_row_dark.png
- 57_edit_crop_sheet_light.png
- 58_edit_crop_sheet_dark.png
- 59_edit_rotate_sheet_light.png
- 60_edit_rotate_sheet_dark.png
- 61_edit_adjust_brightness_light.png
- 62_edit_adjust_brightness_dark.png
- 63_edit_filters_1977_light.png
- 64_edit_filters_1977_dark.png
- 65_edit_compare_on_light.png
- 66_edit_compare_on_dark.png
- 67_edit_pill_light.png
- 68_edit_pill_dark.png
- 69_edit_stickers_row_light.png
- 70_edit_stickers_row_dark.png
- 71_edit_stickers_food_light.png
- 72_edit_stickers_food_dark.png
- 73_edit_stickers_mood_light.png
- 74_edit_stickers_mood_dark.png
- 75_edit_stickers_quote_light.png
- 76_edit_stickers_quote_dark.png
- 77_edit_stickers_emoji_light.png
- 78_edit_stickers_emoji_dark.png
- 79_edit_sticker_selected_light.png
- 80_edit_sticker_selected_dark.png
- 81_edit_stickers_crop_filter_light.png
- 82_edit_stickers_crop_filter_dark.png

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
