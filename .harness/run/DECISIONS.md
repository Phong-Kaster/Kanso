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

Approved as written (Option 1), 2026-10-09: the DoD's 30 criteria and 6 removals, each with its
Verification Class, and assumptions A-001 to A-012 as drafted (blue seed `#1A73E8` through the existing
generator; the four AI tools; Camera as a Home action; the colour picker's light card as the one
exception). The owner asked for the run to go on until the new look is built. The DoD's Questions stay
open for a later run. The pictures the engine may re-record, under the conditions the DoD sets, are the
list under `## Baselines` below.

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
