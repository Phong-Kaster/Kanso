# DECISION QUEUE

> Answer in `.harness/run/DECISIONS.md`, never here: copy an entry's id as a heading there and write your decision underneath it, then re-run.

---

## D-001 - Approve the Definition of Done for Run 3 (previews for every composable)

- **Status:** answered (Iteration 2, 2026-10-07): approved as written
- **Type:** DoD approval
- **Iteration:** 1
- **Timestamp:** 2026-10-07
- **Blocks tasks:** T-001, T-002, T-003, T-004, T-005, T-006, T-007, T-008, T-009

### Question

Approve `.harness/run/DoD.md` (14 criteria, no removals), editing it first if you wish. Approval covers each criterion's Verification Class and the one new test-only library named in it: ComposablePreviewScanner 0.9.3 (`testImplementation`), which lets a test find and draw every `@Preview`. Reply `## D-001` with "approved", or with your edits.

### Context

The PRD asks for a preview for each of the 36 composables in 10 files, light and dark, with the screens that use Koin, a ViewModel, MediaStore, the camera or a permission launcher split into a stateful part and a stateless `...Content` part, without moving a pixel or a test. Three criteria need a person: 7 (look at the previews in Android Studio), 10 (walk the app on the phone, camera included) and 12 (read the state-hoisting explanation). Those stay unsigned, so the run can end at best as `DONE_PARTIAL` until you sign them.

### Options Considered

1. Approve as written - consequences: the scanner dependency is added; criterion 2 proves every preview draws.
2. Approve but refuse the scanner - consequences: the engine falls back to a hand-kept list of previews in the test; criterion 2 then proves only the listed previews.

### Engine Recommendation

Option 1.

### Proposed Capabilities (if any)

None (Autonomous mode runs under the Deny List).

### Decision

Approved as written in `.harness/run/DECISIONS.md` (2026-10-07).
