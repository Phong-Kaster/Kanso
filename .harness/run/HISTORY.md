# HISTORY

> Append-only audit log. Written every iteration, **never read during Orient** — it exists for a
> human auditing the run, not for the engine deciding what to do next. Keeping it out of the read
> path is what stops orientation cost from growing with the run.

<!-- Newest first. One entry per iteration. -->

### Iteration 1 - 2026-10-09 (Bootstrap)
- **Phase:** none (no implementation)
- Attempted: surveyed the repository; four read-only analysts at the Capable tier (survey and inventory, DoD proposal, decomposition with file scopes and tiers, then a critique of the draft plan and DoD); authored `DoD.md`, `PLAN.md`, seven task files, `ASSUMPTIONS.md`, `ESCALATION.md`.
- Learned: the PRD's count (8 main, 6 test files holding more than one type) is right, and five more files break "named after it" (`PermissionGate.kt`, `UprightJpeg.kt`, `SettingsStore.kt`, `SavedScreen.kt`, test `ScreenPixels.kt`); Camera and Tool also expose two StateFlows, which the PRD does not mention. The critique forced three changes: the guard test ships first with an allow-list so it can be shown red on the real tree (it cannot run before the splits otherwise); the mechanical splits go to the Fast tier in their own Phase; and the test-name proof needs `tr -d '\r'` and a next-`fun` match (29 tests have `@Config` between `@Test` and `fun`, six test files use CRLF).
- Reconciled: DoD written (17 criteria, 6 removals, 2 needing a person); 12 assumptions recorded; D-001 queued and blocks every task.

## Archived Decisions

<!-- Full request + decision + rationale of every consumed Decision Queue entry. -->
