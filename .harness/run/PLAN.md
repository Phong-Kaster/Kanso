# PLAN

> Machine-owned execution strategy. The human never reviews this file - only the Definition of Done.
> Evolves through Tier-1 amendments (logged in AMENDMENTS.md) and Tier-2 queued decisions.

## Strategy

One behaviour change, one task, one Phase. The row change moves 43 existing pictures at once, so the
pictures cannot wait for a later Phase: D-001 approves the re-record list up front and Phase 1 ends green.

Phase 1 order:
1. **Wiring (before slices):** fetch the RGB icon chosen in D-001 into `app/src/main/res/drawable/`
   (strip `android:tint`, Constraint on Material Symbols). Write `.harness/run/evidence/test-edits.md`
   naming the DoD 9 unedited tests (incl. the Edit save/undo tests found by grep).
2. **T-001 slices A → B → C → D**, sequential, one Fast Worker each.
3. Scope check; build, full unit suite, lint.
4. Record: `foreman-record-baselines.ps1 -Module app` (adds 101-104, re-records the approved list); open
   every re-recorded and new picture and judge it; `compareRoborazziDebug` diff set must equal the
   approved list (DoD 15); `verifyRoborazziDebug` green.
5. **Wiring (after):** `docs/adr/ADR-010-edit-row-has-two-levels.md` (state local to the row,
   shared Back/slide with Beautify, Back left in Edit / right in Beautify).
6. Reviewer (Capable), Reconcile, checkpoint. Then DONE-candidate → Verifier (device: `R58W1120QVA` only).

Design fixed in `TASKS/T-001.md` § Description (model, shared pieces, tags, state, closing via
`onOpen` toggle, enabled rules). If D-001 changes choice a-d or the icon, amend T-001 (Tier 1) before dispatch.

## Task Graph

- T-001 - The Edit row shows Colour; tapping it shows Back, RGB, HSL and Curves (depends on: -) - scope: 22 files in slices A-D (see task file) - tier: Fast

## Phase Grouping

| Phase | Tasks | Shared files the Wiring Worker writes |
|---|---|---|
| 1 | T-001 (slices A, B, C, D) | `app/src/main/res/drawable/ic_<rgb icon>.xml` (before); `docs/adr/ADR-010-*.md` (after); PNG baselines by the record wrapper (Iteration) |

## Known Risks

- Beautify refactor (slice A) could move pictures 47/48 or `BeautifyToolTest`: both are DoD 10 guards; a
  move is a defect of slice A.
- `CompareTest.kt:241-258` opens RGB/HSL/Curves through the ViewModel then scrolls to Filters: with
  "layer 2 while a colour tool is open" it must close the tool or go Back first (slice C).
- Pictures 69/70 (sticker row) are listed for re-record although analysts disagreed whether they move;
  a listed picture that does not move simply stays.
- `performScrollTo` on a row that fits without scrolling stalls Roborazzi (PROJECT.md Constraint): layer 2
  has three entries and fits, so tap them where they are.
- A 22-file Fast task has 3 attempts; a failed attempt escalates the rest to Capable.
