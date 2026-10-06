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

**Approved as written** (2026-10-06). All 37 criteria and Removals R1-R3, with their Verification
Classes, are approved unchanged. Assumptions A-001 to A-006 are not overturned; the live-preview
trade-off of A-003 (the theme changes once the drag settles) is accepted. The two open questions
stay as the DoD leaves them: `ViewModelClearedProbe` stays in main this run, and Camera stays a Home
tile with its own route. The four baselines below are approved for re-recording (6) and removal
(7, 8, 9).

Rationale: the human read the DoD and approved it verbatim. The run continues in Autonomous mode.

## Baselines

- app/src/test/screenshots/6_settings_custom_colour.png
- app/src/test/screenshots/7_placeholder_light.png
- app/src/test/screenshots/8_placeholder_dark.png
- app/src/test/screenshots/9_placeholder_custom_colour.png

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
