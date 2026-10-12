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

**Approved with one edit** (the owner, 2026-10-12). Every other criterion, removal, Verification Class
and assumption stands as written, A-005 (whole photo over a blurred copy on Saved) and A-004 included.

**Edit — A-001 is reversed: every tool is one tap from Home, with no swipe.** The goal of this run is
speed to the popular features, and three AI tools behind carousel swipes misses it.

- Replace the row of two pill buttons ("Edit", "Remove background") with **a row of five round tool
  shortcuts** directly under the carousel's dots: Edit, Remove background, Enhance, Retouch, Artistic,
  each a round icon button with its name under it, in that order, all five visible at once on a
  360 x 780 dp phone without scrolling the row or the screen.
- Each shortcut opens that tool's gallery at once, exactly as the pills would have (criterion 5).
- The hero carousel of the four AI tools stays as it is, with "Try now" on each banner.
- Criterion 6 becomes: on a small phone, right after launch and with nothing scrolled, all five tool
  shortcuts, the first banner's "Try now", the Camera button, the recent-edits arrow and the newest
  recent photo are fully on screen and each is one tap from where it leads.
- Criterion 4 and R5 read "a row of five tool shortcuts" wherever they say "two pill buttons".

Re-cut the plan for this before any task starts.

## Baselines
- app/src/test/screenshots/1_home_light.png
- app/src/test/screenshots/2_home_dark.png
- app/src/test/screenshots/3_home_custom_colour.png
- app/src/test/screenshots/4_settings_light.png
- app/src/test/screenshots/5_settings_dark.png
- app/src/test/screenshots/6_settings_custom_colour.png
- app/src/test/screenshots/10_gallery_light.png
- app/src/test/screenshots/11_gallery_dark.png
- app/src/test/screenshots/12_gallery_refused_light.png
- app/src/test/screenshots/13_gallery_refused_dark.png
- app/src/test/screenshots/14_camera_refused_light.png
- app/src/test/screenshots/15_camera_refused_dark.png
- app/src/test/screenshots/16_tool_photo_light.png
- app/src/test/screenshots/17_tool_photo_dark.png
- app/src/test/screenshots/18_saved_light.png
- app/src/test/screenshots/19_saved_dark.png
- app/src/test/screenshots/20_camera_live_light.png
- app/src/test/screenshots/21_camera_live_dark.png
- app/src/test/screenshots/22_editor_enhance_light.png
- app/src/test/screenshots/23_editor_enhance_dark.png
- app/src/test/screenshots/24_settings_swatches_light.png
- app/src/test/screenshots/25_settings_swatches_dark.png
- app/src/test/screenshots/26_gallery_loading_light.png
- app/src/test/screenshots/27_gallery_loading_dark.png
- app/src/test/screenshots/28_my_edits_light.png
- app/src/test/screenshots/29_my_edits_dark.png
- app/src/test/screenshots/30_my_edits_empty_light.png
- app/src/test/screenshots/31_my_edits_empty_dark.png
- app/src/test/screenshots/32_my_edits_loading_light.png
- app/src/test/screenshots/33_my_edits_loading_dark.png
- app/src/test/screenshots/34_home_empty_light.png
- app/src/test/screenshots/35_home_empty_dark.png
- app/src/test/screenshots/36_home_loading_light.png
- app/src/test/screenshots/37_home_loading_dark.png
- app/src/test/screenshots/38_home_small_phone_light.png
- app/src/test/screenshots/39_home_small_phone_dark.png
- app/src/test/screenshots/40_tool_narrow_photo_light.png
- app/src/test/screenshots/41_tool_narrow_photo_dark.png
- app/src/test/screenshots/42_tool_narrow_photo_sdk30_light.png
- app/src/test/screenshots/43_photo_viewer_light.png
- app/src/test/screenshots/44_photo_viewer_dark.png

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
