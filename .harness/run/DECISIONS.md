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

**Approved as written** (the human, 2026-10-15), Autonomous, effort medium.

- A-001 accepted: Run 9 is built on `loop/kanso-real-editor`, one commit per Phase; the owner merges to `main`.
- A-003 accepted: Done keeps the settings and the undo stack; the session ends only when Edit is left for good.
- A-002, A-004 to A-008 accepted as written.
- Criterion 22 approved in full: besides `tool_edit_free_crop_light/dark`, every existing tool, Saved and viewer picture that changes because the bottom Save bar goes and Saved becomes Result may be re-recorded. They are listed under `## Baselines` below so the recording wrapper keeps them. Keep each old picture beside its new one in `.harness/run/evidence/baseline-diffs/`.

## Baselines

- app/src/test/screenshots/16_tool_photo_light.png
- app/src/test/screenshots/17_tool_photo_dark.png
- app/src/test/screenshots/18_saved_light.png
- app/src/test/screenshots/19_saved_dark.png
- app/src/test/screenshots/22_editor_enhance_light.png
- app/src/test/screenshots/23_editor_enhance_dark.png
- app/src/test/screenshots/40_tool_narrow_photo_light.png
- app/src/test/screenshots/41_tool_narrow_photo_dark.png
- app/src/test/screenshots/42_tool_narrow_photo_sdk30_light.png
- app/src/test/screenshots/43_photo_viewer_light.png
- app/src/test/screenshots/44_photo_viewer_dark.png
- app/src/test/screenshots/45_tool_cutout_light.png
- app/src/test/screenshots/46_tool_cutout_dark.png
- app/src/test/screenshots/47_tool_retouch_light.png
- app/src/test/screenshots/48_tool_retouch_dark.png
- app/src/test/screenshots/49_tool_artistic_light.png
- app/src/test/screenshots/50_tool_artistic_dark.png
- app/src/test/screenshots/51_tool_edit_free_crop_light.png
- app/src/test/screenshots/52_tool_edit_free_crop_dark.png
- app/src/test/screenshots/53_tool_enhance_done_light.png
- app/src/test/screenshots/54_tool_enhance_done_dark.png

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

## D-003

Signed by the owner, 2026-10-19, after installing the debug build of `2a272fe` on the Galaxy A13:
"everything works as I expect".

- Criteria 20, 21, 22, 23 and 34: verified by the owner.
- A-001 (Loop Branch, not `main`), A-003 (Back from Result keeps the undo stack) and A-009 (R6
  removes "Saved" as the screen's name only): accepted as taken.
