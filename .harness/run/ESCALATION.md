# DECISION QUEUE

> Questions the engine could not answer within its authority. Answer in `.harness/run/DECISIONS.md`, never here.

---

## D-001 - Approve the Definition of Done for Run 2

- **Status:** answered
- **Type:** DoD approval
- **Iteration:** 1 (bootstrap)
- **Timestamp:** 2026-10-06
- **Blocks tasks:** T-001, T-002, T-003, T-004, T-005, T-006, T-007, T-008, T-009, T-010

### Question

Approve the Definition of Done (edit freely before approving), and, so the engine can finish the carry-over fix and the placeholder removal, list these four existing pictures under a `## Baselines` heading in `DECISIONS.md`:

- `app/src/test/screenshots/6_settings_custom_colour.png`
- `app/src/test/screenshots/7_placeholder_light.png`
- `app/src/test/screenshots/8_placeholder_dark.png`
- `app/src/test/screenshots/9_placeholder_custom_colour.png`

### Context

37 criteria plus three Removals, ten tasks in four Phases. Six decisions the engine took alone are in `ASSUMPTIONS.md` (A-001 to A-006). Criteria that need the emulator may again be reported "not driven" if its covering system window is still there; they stay with you.

### Options Considered

1. Approve and list the four baselines - consequences: the run completes the picker fix and placeholder removal cleanly.
2. Approve without listing them - consequences: those two jobs end as report items (A-006), run ends `DONE_PARTIAL`.

### Engine Recommendation

Option 1.

### Proposed Capabilities (if any)

None; the run uses the Deny List (Autonomous mode).

### Decision

Approved as written, 4 baselines listed (DECISIONS.md D-001).
