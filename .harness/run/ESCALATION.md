# DECISION QUEUE

> Questions the engine could not answer within its authority. Queueing one does **not** stop the run:
> the engine marks the tasks that entry blocks and keeps working on everything else. The Runtime
> stops only when no executable task remains.
>
> This file is the engine's own log. **Answer in `.harness/run/DECISIONS.md`, never here** - copy an
> entry's id as a heading there and write your decision underneath it, then re-run. The engine is
> denied write access to `DECISIONS.md`, mechanically, so nothing it does here can ever race what you
> write there. Answer any number of entries; unanswered ones stay queued and the tasks they name stay
> unselectable.

---

## D-001 - Approve the Definition of Done for Run 11 (HSL), and the list of pictures to re-record

- **Status:** pending
- **Type:** DoD approval
- **Iteration:** 1 (bootstrap)
- **Timestamp:** 2026-11-04
- **Blocks tasks:** T-001, T-002, T-003, T-004, T-005

### Question

1. Approve `.harness/run/DoD.md` (37 criteria, no removals). Edit any criterion before approving;
   approval covers each criterion's class (machine / machine-then-human / human-only) too.
2. **Pictures.** Your PRD approves re-recording pictures that change because the row gains a tool,
   but the recording script keeps every existing picture unless it is listed under a
   `## Baselines` heading in `DECISIONS.md`. Adding HSL can move the row in these 35 pictures. If you
   agree, paste this list under `## Baselines` (remove any you do not want re-recorded):

```
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
```

   (A listed picture whose row is hidden by its sheet simply comes out unchanged.) Each old version is kept as `<name>.old.png` in
   `.harness/run/evidence/baseline-diffs/` for criterion 23.

### Context

Run 11 adds HSL as the ninth Edit tool (PRD). Bootstrap fan-out: conventions survey, DoD proposal,
task decomposition, conflict and tier analysis, and an independent critique. The plan has four
Phases, one commit each, in the PRD's order. This is Autonomous mode: after this approval the run
does not stop to ask again; every later choice is recorded in `ASSUMPTIONS.md`. Four were already
taken while planning (A-001 to A-004) — read them before approving; any of them can be overturned
here.

Worth your eye before approving:
- **Criterion 21 (A-001):** your 136 dp sheet holds three rows only if each row is 40 dp tall, so
  the circles, chips and slider are 40 dp on screen with a 48 dp touch area. The alternative was a
  sheet that scrolls, or a taller sheet.
- **Criteria 7–9:** the numbers ("hardly touched" ≤ 0.05, "unchanged" ≤ 1/255, no band = neighbours
  within 12/255) are mine; change them if you want them stricter.
- **Criterion 25** (no stutter on your A13) can only be signed by you on the phone.

### Options Considered

1. Approve as written, with the `## Baselines` list - consequences: the run builds all four Phases
   without stopping.
2. Approve without the list - consequences: Phase 3 cannot pass the screenshot check (the old row
   pictures stay), so the run ends `DONE_PARTIAL` with the pictures for you to re-record.
3. Edit criteria, then approve.

### Engine Recommendation

Option 1.

### Proposed Capabilities (if any)

None: Autonomous mode runs under the Deny List.

### Decision

<!-- Answered in `.harness/run/DECISIONS.md`, under a heading `## D-001` - never here. -->
