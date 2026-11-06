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

1. **DoD approved** as written in `.harness/run/DoD.md`, with the optional hue-curves section
   (criteria 23-24) deleted, as point 2 below says.
2. **Scope: option 1.** Five tone curves (RGB, Red, Green, Blue, Lightness) in a new Curves tool
   right after HSL. The optional "hue curves" section is deleted from the DoD; hue curves can be a
   later run once the Curves editor exists. RGB and HSL stay as they are.
3. **Capability approved, permanent:** the standing assemble / testDebugUnitTest / lintDebug grant
   proposed in D-001, into `.harness/knowledge/capabilities.json`.
4. **No `## Baselines` list yet.** List the exact pictures in a new decision when
   `verifyRoborazziDebug` goes red, as the engine proposed.
5. **Icon:** the engine's proposal stands (a Wiring Worker writes `ic_show_chart.xml` with the
   Material "show chart" path).
6. **Mode:** the owner switches the run to Autonomous after this approval; decide the rest with
   your own recommendations, recorded as assumptions.

Rationale: the engine's recommendation; one editor and one formula covers "RGB" and the L of "HSL"
without removing tools the owner signed off on 2026-11-06.


## D-002

**Approved by the owner, 2026-11-06.** Re-record both pictures. Checked against the Roborazzi
compare image before approving: only the tool row differs (the Curves entry now sits before
Filters, and the row scrolls so Filters stays in view); the photo and the Filters sheet are
identical.

## D-003

**Approved by the owner, 2026-11-06.** Re-record both pictures. Checked against the Roborazzi
compare image: two changes, both accepted. (1) The RGB / Red / Green / Blue chips in the sheet's
top row. (2) The RGB curve's line and points are now white (neutral) instead of the theme's light
blue; the engine's entry mentioned only the chips. Keep the neutral master curve, so the Red,
Green and Blue curves can show their own colours.

## Baselines

- `app/src/test/screenshots/63_edit_filters_1977_light.png`
- `app/src/test/screenshots/64_edit_filters_1977_dark.png`
- `app/src/test/screenshots/95_edit_curves_master_straight_light.png`
- `app/src/test/screenshots/96_edit_curves_master_straight_dark.png`

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
