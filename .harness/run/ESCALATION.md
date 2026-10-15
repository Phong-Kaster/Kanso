# DECISION QUEUE

> Autonomous mode: only the DoD approval is queued. Answer in `.harness/run/DECISIONS.md` under `## D-001`.

---

## D-001 - Approve the Definition of Done for Run 9

- **Status:** answered
- **Type:** DoD approval
- **Iteration:** 1
- **Timestamp:** 2026-10-15
- **Blocks tasks:** T-001, T-002, T-003, T-004, T-005, T-006, T-007, T-008, T-009

### Question

Approve `.harness/run/DoD.md` (edit freely before approving). In particular:

1. **Pictures (criterion 22):** the PRD approves re-recording only `tool_edit_free_crop_light/dark`. Removing the bottom Save bar and renaming Saved also changes about 17 other existing pictures (tool screens, `saved`, `photo_viewer`). Approving the DoD approves their re-recording too; for the recording wrapper to keep them, list those baselines under a `## Baselines` heading in `DECISIONS.md`.
2. **Assumptions A-001 to A-008** (`ASSUMPTIONS.md`), notably A-001 (Loop Branch instead of `main`), A-002 (filter applied before colour so the GPU preview matches), A-003 (Back from Result keeps the undo stack), A-004 (Share through a cache file and a FileProvider).
3. Criterion 20 (A13 smoothness) is machine-then-human and is only driven when the phone is free.

### Context

Bootstrap analysis by four read-only analysts (survey, decomposition, DoD, critique); the plan is in `PLAN.md`, 9 tasks in 6 Phases.

### Options Considered

1. Approve as written. 2. Edit the criteria, then approve.

### Engine Recommendation

Approve after reading criteria 9-11 and 22, and A-001 to A-004.

### Decision

Answered in DECISIONS.md: approved as written; criterion 22 re-records the listed baselines (2026-10-15).
