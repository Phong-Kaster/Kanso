# T-000 - <title>

- **Status:** pending | in-progress | complete | blocked | deferred | abandoned | unreachable | removed
- **Depends on:** -
- **Blocked by decision:** -   <!-- Decision Queue entry id, when deferred -->
- **Attempts:** 0 / 3
- **Declared File Scope:** <!-- the files a Worker on this task may write. Shared/integration files
     belong to no task: a Wiring Worker writes those. -->
- **Model Tier:** Fast | Capable <!-- assigned at planning time by the arm's-length conflict-analysis
     role, sanity-checked by the critique role (ADR-013). Never self-assigned. Escalates to Capable
     automatically on a failed Fast-tier attempt. -->
- **Follow:** <!-- existing files a Worker copies the shape of, each in backticks with what to take from it:
     one for the production code, one for every kind of test the task writes - its imports, its
     helpers, how it measures a size, a colour or a position. Written at bootstrap from the survey. -->
  - `path/to/ExistingTest.kt` - imports, the test rule, how a node's size is read in dp

## Description

<!-- What to build, scoped small enough to implement and verify within one iteration. -->

## Acceptance

<!-- Task-level acceptance: what evidence marks this task complete. Maps to DoD criteria where
     applicable ("the user/system can now observe/do X"). -->

## Evidence

<!-- Filled on completion: the checkpoint commit carries build/test/lint output, so a sha is enough. -->

## Failed attempts

<!-- Written AT THE MOMENT OF FAILURE, not later. Failed work is reverted and enters no commit, so
     this is the only place its detail survives. Cap each error tail at ~40 lines.
     Three entries here means the task is abandoned. -->

### Attempt 1 - <timestamp>
- Approach: ...
- Command: `...`
- Error (tail): ...

## Notes

<!-- Discoveries, review findings, reconciliation outcomes affecting this task. -->
