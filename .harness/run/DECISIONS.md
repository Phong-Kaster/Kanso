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

**Approved as written** (2026-10-07). All 14 criteria, with their Verification Classes, are approved
unchanged; there are no Removals. The new test-only library ComposablePreviewScanner 0.9.3
(`testImplementation`) is approved, and so is `ui-tooling-preview` as an `implementation`
dependency. Assumption A-001 (the navigation host gets a stateless display for its preview) is not
overturned.

Rationale: the human read the DoD and approved it verbatim. The run continues in Autonomous mode.

## Note from the operator, 2026-10-07: a preview never finishes drawing

Not an answer to a queued decision; information the run did not have. The operator stopped the run
at 14:22 and restarted it after writing this.

- With the uncommitted Phase 3 work in the tree (Tool, Saved and Gallery previews), the test JVM of
  `testDebugUnitTest` ran for 35 minutes at 100% of one core and 3.5 GB, and wrote no test result.
  The build the iteration was waiting on never finished; the operator killed that JVM at 14:20, and
  the next iteration's full rebuild (`build2.log`) would have hung the same way.
- A thread dump (`%TEMP%\kanso-backups\run3-hung-test.jstack.txt`) shows where:
  `PreviewsRenderTest.every_preview_draws_without_an_error` → `drawPreview` (line 148) →
  `ShadowPausedLooper.idleFor(500 ms)` → `Choreographer.doFrame` → `Recomposer` →
  `deriveStateLocked` / `advanceGlobalSnapshot`, over and over inside one frame. One preview keeps
  invalidating its own composition, which is what a state write during composition looks like, so
  half a second of the test clock never ends.
- Which preview it is was not established. The tests passed at 5253164, so it is one of the Phase 3
  files.
- Nothing bounds a test that hangs: the iteration polled the log for 35 minutes. A hung test should
  fail with the preview's name instead of waiting.

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
