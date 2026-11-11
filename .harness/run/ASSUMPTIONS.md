# ASSUMPTIONS

> Autonomous mode (ENGINE.md 14.1). Decisions the engine made alone.

---

## A-001 - MAX_POINTS = 10 and MIN_GAP = 0.02 stay; new files stay in curvespanel/

- **Tier:** Missing information
- **Iteration:** 2 (the PRD is silent; the DoD approval did not change them)
- **First dependent checkpoint:** Iteration 2 (SHA filled in by the next Iteration)
- **Revert:** `git revert <sha>`

### Question
Keep today's 10-point limit and 0.02 minimum gap, and keep the Curves files in `curvespanel/` rather than a `components/` restructure?

### Options Considered
1. Keep both - consequences: no behaviour change beyond the PRD.
2. Change limit or restructure - consequences: unrequested churn.

### Taken, and why
Option 1: the PRD does not ask for either.
