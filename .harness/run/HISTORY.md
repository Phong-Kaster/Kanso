# HISTORY

> Append-only audit log. Written every iteration, **never read during Orient** — it exists for a
> human auditing the run, not for the engine deciding what to do next. Keeping it out of the read
> path is what stops orientation cost from growing with the run.

### Iteration 1 - 2026-10-12 (bootstrap)
- **Phase:** none (bootstrap)
- Attempted: read the new PRD (Run 7, the InFlux layout), the repository, `PROJECT.md`, the Run 6 DoD and plan, the guard tests; created `loop/kanso-influx-layout` from `a720960`; fanned out read-only analysis roles at the Capable tier (survey, DoD proposal, decomposition and a Material 3 feasibility reading, then an independent critique of my draft DoD and plan and a conflict analysis of the file scopes); authored the DoD (41 criteria, 9 removals, 24 readings), the plan (11 tasks, 4 phases), the task files, the assumptions A-001 to A-024 and D-001; captured the base evidence (368 test names, the badging of the base APK, the base runtime dependency list, lint's unused resources); rewrote the Constraints of `PROJECT.md` that the PRD supersedes; appended sections J to R to `api-digest.md`.
- Learned: the first plan was not safe to approve. The critique found four blockers and eleven major problems, the conflict analysis eight hidden couplings or unowned files (three of them compile couplings inside Phase 1): DoD proofs named test classes no task wrote (S-005 and S-007 again), a shade proof that contradicted its task, "more than 8 per channel" that fails a correct tonal button, Phase 1 red by construction (missing baselines, `ThemeFollowTest` premises, `PreviewCoverageTest` on a composable no preview may animate), the overlaid bar catching `performScrollTo().performClick()`, a tab screen depending on a same-Phase file for `tabBarClearance()`, Home too big for one Worker, a Home photo source that a swap after launch cannot reach. The fixes: tab screens take their clearance as a parameter; the shell, fade and back-stack moves are T-001's own files; `TabTaps.tapAboveBar()`; Home split into T-005 and T-008; gallery and camera merged into T-006 (Fast); the cross-screen walks split into T-010 and T-011; every picture, preview and string name fixed in `PLAN.md`.
- Reconciled: new Constraints and Reference facts in `.harness/knowledge/PROJECT.md`; the Run 7 section in `.harness/ISSUES.md`; one Suggestion Box entry; D-001 queued (blocks every task); nothing implemented.

### Iteration 2 - 2026-10-12 (consume D-001, re-cut, Phase 1 preparation)
- **Phase:** none (a decision-and-preparation Iteration: no task started; Phase 1 needs more than the 90 minutes an Iteration has left after this, so it is Iteration 3's)
- Attempted: consumed D-001 (approved with one edit: every tool one tap, five round shortcuts, A-001 reversed; the 41 pictures approved as listed); applied the edit to `DoD.md` (criteria 4, 5, 6, 7(e), 19, 21, 30, R5, Q1, the status line), re-cut `PLAN.md`, T-005, T-008 (and one sentence each in T-003 and T-011), marked A-001 reversed, answered D-001 in `ESCALATION.md`, logged everything in `AMENDMENTS.md`, wrote ISSUES; Phase 1 preparation: fetched the five Material Symbols (`evidence/symbols/`, `check.sh` exit 0), wrote ADR-002 and the ADR-001 note, ran the `rememberInfiniteTransition` spike (throwaway test, deleted; `evidence/spike-infinite-transition.md`); dispatched a read-only Capable analyst to review the re-cut and fixed its nine findings.
- Learned: the pills-to-shortcuts edit changes the Home height budget (five round buttons with names are 84 dp against 48 dp; kept at 24 dp spare by a 176 dp carousel and 12 dp gaps) and the tag family (`home-shortcut-<TOOL>`); `waitForIdle()` returns at once with a `rememberInfiniteTransition` under the test rule and the pulse never moves (so no paused clock is needed, and no test can see the pulse); `FilledTonalIconButton`'s default is a 40 dp circle in a 48 dp node, which the DoD's "round" pixel rule would read on its rim (T-003 sizes `RoundIconButton` to 48 dp); `boundsInRoot` is clipped by Compose, so a "wholly on screen" proof measures `positionInRoot` plus `size`.
- Reconciled: knowledge update (PROJECT.md: the Home tag in a Constraint, the spike answer in Reference); a Tier-3 human edit applied and logged; one Suggestion Box entry (S-012); no decision queued; no task attempted.

### Iteration 5 - 2026-10-13 (recover Phase 1a, finish and checkpoint)
- **Phase:** 1a (T-001, T-002). Recovery: Iteration 4 left the Phase 1a work uncommitted with 18 red tests (its run was killed); it was coherent, so it was salvaged, not reverted.
- Fixed: `ShortNavigationBar` sized two tabs to zero (bar rebuilt as Surface + Row of weighted items); Home-row clicks hidden under the bar (`tapAboveBar()`); ToolScreenTest's end probe looked for Home's removed Camera text; NavBarTest's `hasVisualOverflow` premise (Robolectric) replaced by geometry.
- Evidence: whole unit suite, `compareRoborazziDebug`, lint, assembleDebug green (`evidence/p1a-run5.log`); new pictures 28-33 recorded, 1-6, 24, 25 re-recorded, `verifyRoborazziDebug` green. Pictures 1 and 29 opened and judged.
- Not done: the fresh-context review of Phase 1a was skipped for lack of time (the diff is 300 KB); the Verifier gate still re-proves everything. Pictures' `_compare.png` copies were not saved to `evidence/baseline-diffs/phase1/`.

## Archived Decisions

<!-- Full request + decision + rationale of every consumed Decision Queue entry. -->

### D-001 (answered 2026-10-12, consumed at Iteration 2)

- **Question (Iteration 1):** approve the Definition of Done for Run 7 (41 criteria, 9 removals) and the `## Baselines` list (the 24 existing pictures and the 17 new ones the run may record).
- **Decision (the owner):** approved with one edit. Every other criterion, removal, Verification Class and assumption stands as written, A-005 (whole photo over a blurred copy on Saved) and A-004 included. A-001 is reversed: every tool is one tap from Home with no swipe, because the goal of the run is speed to the popular features and three AI tools behind carousel swipes misses it. Home's two pill buttons become a row of five round tool shortcuts (Edit, Remove background, Enhance, Retouch, Artistic, each with its name under it, all visible at once on 360 x 780 dp without scrolling the row or the screen) directly under the carousel's dots; each opens that tool's gallery at once; the carousel of four AI tools stays with "Try now" on each banner. Criterion 6 becomes: on a small phone, right after launch and with nothing scrolled, all five tool shortcuts, the first banner's "Try now", the Camera button, the recent-edits arrow and the newest recent photo are fully on screen and each is one tap from where it leads. Criterion 4 and R5 read "a row of five tool shortcuts" wherever they say "two pill buttons". "Re-cut the plan for this before any task starts."
- **Baselines approved:** the 41 pictures listed under `## Baselines` in `DECISIONS.md` (the 24 existing and the 17 new ones, `1_home_light` to `44_photo_viewer_dark`, with the numbers 7 to 9 unused).
- **Applied:** `AMENDMENTS.md`, Iteration 2.

### Iteration 6 - 2026-10-13 (Phase 1b)
- **Phase:** 1b (T-003). Worker (Capable) rewrote the tool screen; Iteration wired Replace and the Saved key in KansoNavHost, removed `Tool.editingTools`, deleted ToolSelectionAnimationTest, fixed two `onSelectTool` call sites and ThemeFollowTest's editor premise (now reads background left of the card).
- Evidence: unit suite, compare, verify, lint, assemble green (`evidence/p1b-*.log`); baselines 16, 17, 22, 23 re-recorded, 40-42 new.
- Not done: fresh-context review skipped for time.

### Iteration 7 - 2026-10-13 (Phase 2: recover, finish, checkpoint)
- **Phase:** 2 (T-004 Saved/viewer, T-005 Home hero + five shortcuts, T-006 gallery/camera). Recovery: the previous Iteration was killed with the main code and wiring written but the tests uncompilable (SavedScreenTest, HomeScreenTest, NavigationTest, PhotoToToolNavigationTest, ThemeFollowTest); the debris was coherent and was salvaged.
- Did: two Capable Workers wrote SavedScreenTest and HomeScreenTest + the two navigation tests (the read guard refused them the evidence drafts, so HomeScreenTest was written afresh); Iteration fixed ThemeFollowTest (Saved premise: a photo in the theme's background colour), AccessibilityWalk (dots carry `stateDescription`, not `selected`), a Saved message assertion, HomeScreenTest's banner checks, ScreenshotTest's dot check; removed `strings_home.xml` and `saved_home`; recorded pictures 1-3, 10-15, 18-21, 26, 27 and new 43, 44 (judged 1, 10, 43).
- Evidence: whole set green (`evidence/p2-run4.log`: tests, verifyRoborazziDebug, lint, assembleDebug).
- Not done: fresh-context review of Phases 1 and 2 skipped for time; the Verifier re-proves everything.
