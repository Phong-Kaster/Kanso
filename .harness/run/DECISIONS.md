# DECISIONS

## Note from the owner, 2026-11-04 17:40 (facts for Phase 3, not an answer to a D- entry)

The owner stopped Iteration 4 at 17:40 on purpose; it did not crash. Its partial Phase 3 work is
in the working tree, so recover it as usual.

Why it was stopped: the full unit-test runs `p3-full-2`, `-3` and `-4` did not fail on slow tests
or a full disk. **They hang inside one test**, every time:

- `HslSheetTest.tapping_a_circle_chooses_it_and_changes_nothing`, at `HslSheetTest.kt:180`,
  `compose.onNodeWithTag(hslRangeTag(target)).performScrollTo()`.
- Thread dump of the test JVM at 17:28 (`p3-full-3`, log silent for 7 min): the test thread loops in
  `AndroidComposeUiTestEnvironment.waitForIdle` → `RobolectricIdlingStrategy.runUntilIdle` →
  `ComposeIdlingResource.isIdleNow` → `MainTestClock.advanceTimeByFrame`, for ever: Compose never
  becomes idle after scrolling to a colour-range circle.
- `p3-full-3` exit 1 ("Gradle Test Executor 145 finished with non-zero exit value 1") was the
  owner killing that hung JVM, not a disk or daemon problem.

Fix that test (or what it drives in `ColourRangeRow` / `ColourRangeCircle`) before running the
whole suite again, and run `HslSheetTest` on its own first, under a short `timeout`, to prove it
ends.


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

- app/src/test/screenshots/16_tool_photo_light.png
- app/src/test/screenshots/17_tool_photo_dark.png
- app/src/test/screenshots/40_tool_narrow_photo_light.png
- app/src/test/screenshots/41_tool_narrow_photo_dark.png
- app/src/test/screenshots/42_tool_narrow_photo_sdk30_light.png
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
- app/src/test/screenshots/69_edit_stickers_row_light.png
- app/src/test/screenshots/70_edit_stickers_row_dark.png
- app/src/test/screenshots/71_edit_stickers_food_light.png
- app/src/test/screenshots/72_edit_stickers_food_dark.png
- app/src/test/screenshots/73_edit_stickers_mood_light.png
- app/src/test/screenshots/74_edit_stickers_mood_dark.png
- app/src/test/screenshots/75_edit_stickers_quote_light.png
- app/src/test/screenshots/76_edit_stickers_quote_dark.png
- app/src/test/screenshots/77_edit_stickers_emoji_light.png
- app/src/test/screenshots/78_edit_stickers_emoji_dark.png
- app/src/test/screenshots/79_edit_sticker_selected_light.png
- app/src/test/screenshots/80_edit_sticker_selected_dark.png
- app/src/test/screenshots/81_edit_stickers_crop_filter_light.png
- app/src/test/screenshots/82_edit_stickers_crop_filter_dark.png

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
