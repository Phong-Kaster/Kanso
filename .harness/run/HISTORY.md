# HISTORY

> Append-only audit log. Written every iteration, **never read during Orient** — it exists for a
> human auditing the run, not for the engine deciding what to do next. Keeping it out of the read
> path is what stops orientation cost from growing with the run.

<!-- Newest first. One entry per iteration. -->

### Iteration 2 - 2026-10-09 (Phase 1)
- **Phase:** 1 - T-001, T-002, T-003 (three Capable Workers in parallel, scopes disjoint; reported file sets matched `git status`)
- Attempted: D-001 consumed (DoD approved as written); each of the six screens now has one `…UiState`, one `state` StateFlow and a `…Content` that takes it; Saved got a ViewModel; the Iteration wired `KansoNavHost.kt` (Home preview, `entry<Saved>`) and `AppModule.kt` (Saved recipe).
- Verified: one combined run `cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` exit 0 (`evidence/p1-verify.log`); 207 tests, 0 failures; baselines unchanged; the R1-R5, criterion 11 and 15 greps pass.
- Review (Capable, fresh context): no Critical. Minor 1 (Tool's `tool`/`photoUri` still public) and Minor 3 (first `init` block unexplained) fixed by the Iteration; Minor 2 (`CameraContent` decides "a photo waits" from the `shotPicture` slot, not `state.shot`) kept: identical output on the real screen, already explained in its KDoc; recorded in ISSUES.md.
- Learned: `PreviewHygieneTest` scans top-level `sample…` bodies with their comments (PROJECT.md); the Saved fake now goes through the constructor, so Koin swaps are gone there.
- Reconciled: knowledge updated; assumptions A-004..A-010 marked as applied in this checkpoint.


### Iteration 1 - 2026-10-09 (Bootstrap)
- **Phase:** none (no implementation)
- Attempted: surveyed the repository; four read-only analysts at the Capable tier (survey and inventory, DoD proposal, decomposition with file scopes and tiers, then a critique of the draft plan and DoD); authored `DoD.md`, `PLAN.md`, seven task files, `ASSUMPTIONS.md`, `ESCALATION.md`.
- Learned: the PRD's count (8 main, 6 test files holding more than one type) is right, and five more files break "named after it" (`PermissionGate.kt`, `UprightJpeg.kt`, `SettingsStore.kt`, `SavedScreen.kt`, test `ScreenPixels.kt`); Camera and Tool also expose two StateFlows, which the PRD does not mention. The critique forced three changes: the guard test ships first with an allow-list so it can be shown red on the real tree (it cannot run before the splits otherwise); the mechanical splits go to the Fast tier in their own Phase; and the test-name proof needs `tr -d '\r'` and a next-`fun` match (29 tests have `@Config` between `@Test` and `fun`, six test files use CRLF).
- Reconciled: DoD written (17 criteria, 6 removals, 2 needing a person); 12 assumptions recorded; D-001 queued and blocks every task.

## Archived Decisions

<!-- Full request + decision + rationale of every consumed Decision Queue entry. -->

### D-001 - Approve the Definition of Done for Run 4 (answered 2026-10-09)
- **Question:** approve `DoD.md` (17 criteria, 6 removals) with each criterion's Verification Class; recommendation: approve as written.
- **Decision (human):** "Approved as written." All 17 criteria and R1-R6 with their classes unchanged; A-001 to A-012 not overturned; Q1-Q4 out of scope.
- **Rationale (human):** "the human read the DoD and approved it verbatim. The run continues in Autonomous mode."
