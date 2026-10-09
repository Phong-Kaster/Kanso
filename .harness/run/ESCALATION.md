# DECISION QUEUE

> Questions the engine could not answer within its authority. Queueing one does **not** stop the run:
> the engine marks the tasks that entry blocks and keeps working on everything else. The Runtime
> stops only when no executable task remains.
>
> This file is the engine's own log. **Answer in `.harness/run/DECISIONS.md`, never here** - copy an
> entry's id as a heading there and write your decision underneath it, then re-run. The engine is
> denied write access to `DECISIONS.md`, mechanically, so nothing it does here can ever race what you
> write there. Answer any number of entries; unanswered ones stay queued and the tasks they name stay
> unselectable.

---

## D-001 - Approve the Definition of Done for Run 4

- **Status:** pending
- **Type:** DoD approval
- **Iteration:** 1
- **Timestamp:** 2026-10-09
- **Blocks tasks:** T-001, T-002, T-003, T-004, T-005, T-006, T-007

### Question

Approve `.harness/run/DoD.md` (17 criteria and 6 removals), editing it first if you wish. Approval covers each criterion's Verification Class
as well as its wording. Reply `## D-001` in `.harness/run/DECISIONS.md` with "approved", or with your edits. No new library and no capability is requested.

### Context

The PRD brings the code of Runs 1-3 up to CLAUDE.md's new rules: one top-level type per file and every screen having a `…Screen.kt`, `…ViewModel.kt` and
`…UiState.kt`, changing nothing the person using the app sees. Two criteria need a person: **6** (walk the six screens on the phone, the same walk as Run 3's
unsigned item 10, so one signed walk may close both) and **15** (read the UiState explanation in the three Home files). Both stay unsigned in Autonomous
mode, so the run can end at best as `DONE_PARTIAL` until you sign them.

Where the PRD and `CLAUDE.md` leave a reading open, this DoD takes the one below. Each is recorded as an assumption (`ASSUMPTIONS.md`, A-001 to A-012) and
can be reversed by answering here:

- **"Named after it" is enforced** (CLAUDE.md), so five files the PRD's count of 8 + 6 leaves out are split or renamed too: `PermissionGate.kt`, `UprightJpeg.kt`,
  `SettingsStore.kt` (a `private` type, made `internal`), `SavedScreen.kt` and the test file `ScreenPixels.kt`.
- **The six navigation keys become six small files**, not one sealed type: their class names stay the same, so saved back stacks and the navigation host are untouched.
- **"Exactly one StateFlow"** also merges Camera's two flows and Tool's two flows into one UiState each (the PRD names only Settings, Home and Saved). Tool's one-shot
  `savedEvents` (not a StateFlow) and plain read-only values may sit beside it. A `…Content` may also take callbacks, a modifier, picture slots and facts only the drawing part
  can know (the permission answer, whether Android 12 wallpaper colours are offered, whether a share is in progress).
- **Saved's ViewModel only loads the photo.** Its share block stays in the screen because it needs the Activity.
- **The guard test ships first with an allow-list** of the 14 files still breaking the rule; the next Phase splits them and empties the list. That is how the test is shown red on the real tree.
- **KDoc coverage keeps scanning `app/src/main` only**; the new and moved test files are read by the Reviewer.

### Options Considered

1. Approve as written - consequences: three Phases (screens; the two guard tests; the file splits) and a Verifier pass; the run can end `DONE_PARTIAL` while criteria 6 and 15 await your signature.
2. Approve with edits (for example, one sealed type for the navigation keys, or leave the five extra files alone) - consequences: the plan is re-cut by a Tier-3 amendment before any task starts.

### Engine Recommendation

Option 1.

### Proposed Capabilities (if any)

None (Autonomous mode runs under the Deny List).

### Decision

<!-- Answered in `.harness/run/DECISIONS.md`, under a heading `## D-001` - never here. -->

---
