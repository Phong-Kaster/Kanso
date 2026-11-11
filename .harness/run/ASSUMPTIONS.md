# ASSUMPTIONS

> Autonomous mode (ENGINE.md 14.1). Decisions the engine made alone.

---

## A-001 - MAX_POINTS = 10 and MIN_GAP = 0.02 stay; new files stay in curvespanel/

- **Tier:** Missing information
- **Iteration:** 2 (the PRD is silent; the DoD approval did not change them)
- **First dependent checkpoint:** `a9539a2` (Iteration 2)
- **Revert:** none needed as a revert: the assumption kept existing values and changed no code (reverting `a9539a2` would undo the Lightness removal, not this). Undo by changing `MAX_POINTS` / `MIN_GAP` in `data/ToneCurve.kt`.

### Question
Keep today's 10-point limit and 0.02 minimum gap, and keep the Curves files in `curvespanel/` rather than a `components/` restructure?

### Options Considered
1. Keep both - consequences: no behaviour change beyond the PRD.
2. Change limit or restructure - consequences: unrequested churn.

### Taken, and why
Option 1: the PRD does not ask for either.
