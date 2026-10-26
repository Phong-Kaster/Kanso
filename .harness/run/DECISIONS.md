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

**Approved, with my own edits to `.harness/run/DoD.md` (2026-10-26).** I edited `DoD.md` directly before
approving, as the entry invites: those edits are mine, not crash debris. Keep them and tick APPROVED.

1. **The Stickers panel is a sheet that I confirm or cancel** (my design, replacing the PRD's "tabs over a grid").
   Tapping Stickers opens a bottom sheet in place of the row of tools. Row 1 is a horizontally scrolling row of
   category chips (Food, Mood, Quote, Emoji, in Pinity's chip style); row 2 is a vertical grid three stickers
   wide, scrolling, for the chosen category. A ✕ at the sheet's top left cancels: the photo and the Undo history
   go back exactly to how they were when the sheet opened (stickers confirmed in an earlier sheet stay) and the
   sheet closes. A ✓ at its top right keeps the changes and closes the sheet. Back while the sheet is open does
   what ✕ does. Criteria 3, 11 and 12 rewritten; 41, 42 and 43 added; 27 and 28 say "category" for "tab".
2. **Undo stays one action per step**, inside the open sheet and after ✓: add → turn → flip → resize is undone
   as resize, flip, turn, then the sticker disappears (criterion 12).
3. **Criterion 2 (all five tools fit at 411 dp) and criterion 20 (stickers visible while cropping) are kept as
   written**, on the engine's recommendation. Q1: `ic_rotate_circle` stays unported.
4. **Criterion 18 is kept:** stickers are drawn at the resolution Done already saves at (at most 2,000 px on
   the long side), not the file's full size.
5. **The three proposed capabilities are approved** as written: the Gradle build/test/lint/Roborazzi set
   (permanent), the baseline recorder (goal), and the sticker copy + sha256 + baseline-diff copies (goal).
6. **The 16 tool-row pictures may be re-recorded**, listed under Baselines below.

The rest of this run is Autonomous: decide later questions on your own recommendation and record them as
assumptions.

## Baselines
- app/src/test/screenshots/51_tool_edit_free_crop_light.png
- app/src/test/screenshots/52_tool_edit_free_crop_dark.png
- app/src/test/screenshots/55_edit_tool_row_light.png
- app/src/test/screenshots/56_edit_tool_row_dark.png
- app/src/test/screenshots/57_edit_crop_sheet_light.png
- app/src/test/screenshots/58_edit_crop_sheet_dark.png
- app/src/test/screenshots/59_edit_rotate_sheet_light.png
- app/src/test/screenshots/60_edit_rotate_sheet_dark.png
- app/src/test/screenshots/61_edit_adjust_brightness_light.png
- app/src/test/screenshots/62_edit_adjust_brightness_dark.png
- app/src/test/screenshots/63_edit_filters_1977_light.png
- app/src/test/screenshots/64_edit_filters_1977_dark.png
- app/src/test/screenshots/65_edit_compare_on_light.png
- app/src/test/screenshots/66_edit_compare_on_dark.png
- app/src/test/screenshots/67_edit_pill_light.png
- app/src/test/screenshots/68_edit_pill_dark.png

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
