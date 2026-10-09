# HISTORY

> Append-only audit log. Written every iteration, **never read during Orient** — it exists for a
> human auditing the run, not for the engine deciding what to do next. Keeping it out of the read
> path is what stops orientation cost from growing with the run.

### Iteration 1 - 2026-10-09 (bootstrap)
- **Phase:** none (bootstrap)
- Attempted: read the new PRD (Run 6), the repository, `PROJECT.md`, the guard tests; fanned out five read-only analysis roles at the Capable tier (survey, DoD proposal, decomposition, then an independent critique and a conflict analysis of my draft); authored the DoD (30 criteria, 6 removals), the plan (11 tasks, 4 phases), the task files, the assumptions A-001 to A-012 and D-001; captured the base evidence (238 test names, the badging of the base APK, the base runtime dependency list).
- Learned: the critique changed the draft materially: the Saved cut became a Fast task, Camera moved after Home and Gallery (its tests tap Home's "Camera" text and host the gallery), `AccessRefusedPanel` left the Gallery task, the editor's `onSaved` signature stays (the nav host reads the selected tool), new baseline names are listed in D-001 so a later phase may re-record them, Home's text invariants became binding Constraints, `LoadingIndicator` gets a paused-clock discipline, two proof tests got their own task (T-011).
- Reconciled: new Constraints and Reference facts in `.harness/knowledge/PROJECT.md`; the Run 6 section in `.harness/ISSUES.md`; D-001 queued (blocks every task); nothing implemented.

## Archived Decisions

<!-- Full request + decision + rationale of every consumed Decision Queue entry. -->
