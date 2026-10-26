# ASSUMPTIONS

> Autonomous mode only (ENGINE.md §14.1, ADR-027). Every decision the engine made alone, where a
> Collaborative run would have stopped to ask. The Runtime renders this file into the This run tab of
> `FOREMAN.html` after every iteration, so each entry must make sense to someone who has read nothing else.
>
> To overturn one: answer its id in `.harness/run/DECISIONS.md` and re-run. A human answer always
> outranks an assumption — the engine reverts what depended on it. Or run the revert command below
> yourself, on the Loop Branch, after the run has stopped.

---

## A-001 - 15 out-of-date screenshot references are left as they are, so the screenshot check still fails

- **Tier:** 3 (intent)
- **Iteration:** 9
- **First dependent checkpoint:** none (nothing was changed; this records a reading, not an edit)
- **Revert:** nothing to revert; answering this id with "re-record them" lets a later Iteration record them
- **Assumed DoD criteria:** 27 and 34 (both say `./gradlew :app:verifyRoborazziDebug` exits 0)

### Question

The Verifier's fresh `verifyRoborazziDebug` (2026-10-26, `.harness/run/evidence/verifier-roborazzi.log`)
fails 15 pictures: `ScreenshotTest` settings_light/dark, settings_swatches_light/dark, settings_custom_colour,
home_custom_colour, and `ToolScreenshotTest` tool_photo_light/dark, tool_artistic_light/dark,
tool_retouch_light/dark, tool_narrow_photo_light/dark, tool_narrow_photo_sdk30_light. They were already failing
before this run's first change (Iteration 4 found them stale; the reference for picture 16 still shows the Undo /
Compare pill that Run 9 removed). D-001 allowed re-recording only pictures 51, 52 and 55 to 68. Criteria 27
and 34 require the whole check to exit 0. Re-record the 15, or leave them?

### Options Considered

1. Re-record the 15 - consequences: the check goes green, but it replaces the standard these pictures are judged
   against without anyone looking, which ENGINE.md §5 reserves for a person.
2. Leave them, and report criteria 27 and 34 as assumed, not met - consequences: the run cannot end `DONE`;
   it ends `DONE_PARTIAL` with the 15 listed for the owner.

### Taken, and why

Option 2. Moving a reference picture is a human decision, and D-001 named exactly which ones could move. Every
one of the 15 needs a person to look at the new image anyway; the list is in `.harness/ISSUES.md`.
