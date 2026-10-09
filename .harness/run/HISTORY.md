# HISTORY

> Append-only audit log. Written every iteration, **never read during Orient** — it exists for a
> human auditing the run, not for the engine deciding what to do next. Keeping it out of the read
> path is what stops orientation cost from growing with the run.

### Iteration 1 - 2026-10-09 (bootstrap)
- **Phase:** none (bootstrap)
- Attempted: read the new PRD (Run 6), the repository, `PROJECT.md`, the guard tests; fanned out five read-only analysis roles at the Capable tier (survey, DoD proposal, decomposition, then an independent critique and a conflict analysis of my draft); authored the DoD (30 criteria, 6 removals), the plan (11 tasks, 4 phases), the task files, the assumptions A-001 to A-012 and D-001; captured the base evidence (238 test names, the badging of the base APK, the base runtime dependency list).
- Learned: the critique changed the draft materially: the Saved cut became a Fast task, Camera moved after Home and Gallery (its tests tap Home's "Camera" text and host the gallery), `AccessRefusedPanel` left the Gallery task, the editor's `onSaved` signature stays (the nav host reads the selected tool), new baseline names are listed in D-001 so a later phase may re-record them, Home's text invariants became binding Constraints, `LoadingIndicator` gets a paused-clock discipline, two proof tests got their own task (T-011).
- Reconciled: new Constraints and Reference facts in `.harness/knowledge/PROJECT.md`; the Run 6 section in `.harness/ISSUES.md`; D-001 queued (blocks every task); nothing implemented.

### Iteration 2 - 2026-10-09 (Phase 1: T-001, T-002, T-003; finished by a second process)
- **Phase:** 1 - Kanso blue through `MaterialExpressiveTheme`; the editor with its toolbar; Saved's button group.
- **Recover:** the first process of this Iteration consumed D-001, did the Phase 1 preparation (drawables, `Tool.iconRes` and `editingTools`, the API digest, the back-callback spike), dispatched the three Workers, wired the shared files and got a combined run with only the 17 expected picture failures, and was killed during `compareRoborazziDebug` (by the Runtime's timeout; no checkpoint, no HISTORY entry). The second process found the tree coherent (every non-picture test green in `p1-full-2.log`, the sources unchanged since the compare), so it salvaged rather than reverted: it re-ran the compare, judged the pictures, ran the review, closed the review's findings and committed once.
- Attempted: re-ran `compareRoborazziDebug` (15 changed, 2 added, 1 unchanged), copied the comparison images to `evidence/baseline-diffs/phase1/`, opened the new editor, Saved, Settings, Home, gallery and camera pictures, re-recorded the approved baselines with `foreman-record-baselines.ps1` (15 re-recorded, 2 new); dispatched the Fresh-Context Reviewer (Capable): two blocking findings (the DoD 3 walk lived in `SavedNavigationTest` while DoD 3's command runs `ToolScreenTest` and `ToolViewModelTest`; no test restarted the app with the wallpaper choice kept at SDK 35, DoD 22) and six advisory (Saved's icons unchecked, `ActionMenuItem` without a preview, `DoD 16` grep catching the seed constant, the editor's gesture zone untested, `test-edits.md` wrong in three rows, eleven drawables unused until later phases); dispatched three Capable Workers on disjoint scopes to close the first five; fixed `test-edits.md`; ran the whole set once more.
- Evidence: `cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` exit 0 in 2 min 8 s (`evidence/p1-full-3.log`): 286 tests (0 failed, 0 errors, 0 skipped; 238 at the base, 7 names replaced, 55 added), `verifyRoborazziDebug` 18 total / 0 changed, lint 0 errors, debug APK built. Greps: no `MaterialTheme(`, no `Baseline`/`Default purple` left in `app/src/main`, no `@Ignore`/`Assume` in the tests.
- Learned: a DoD proof that names test classes is only as strong as the assertions in those classes (new Reference in PROJECT.md); an `Icon` with no description leaves no semantics node (tag it, read the unmerged tree); the editor's whole-app walk taps Home by `home-tool-EDIT` when it exists and by the "Edit" row until Phase 2 gives Home its cards.
- Reconciled: A-013 (the seed through a Long constant, Tier 2); Tier-1 amendments in `AMENDMENTS.md`; PROJECT.md Constraint and References; ISSUES.md regenerated; T-001 to T-003 complete; the eleven unused drawables are for Phases 2 and 3.

## Archived Decisions

<!-- Full request + decision + rationale of every consumed Decision Queue entry. -->

### D-001 - Approve the Definition of Done for Run 6, and the screenshot pictures it may re-record (answered, consumed in Iteration 2)

- **Question (Iteration 1):** approve `.harness/run/DoD.md` (30 criteria, 6 removals) and list under `## Baselines` the pictures the engine may re-record.
- **Decision (2026-10-09):** "Approved as written (Option 1)": the DoD with its Verification Classes, A-001 to A-012 as drafted (blue seed `#1A73E8` through the existing generator; the four AI tools; Camera as a Home action; the colour picker's light card as the one exception); the owner asked for the run to go on until the new look is built; the DoD's Questions stay open for a later run. The 24 pictures under `## Baselines` (the 16 existing and the 8 new names `20` to `27`) may be re-recorded under the DoD's conditions.
- **Effect:** T-001 to T-011 unblocked, plan unchanged.

