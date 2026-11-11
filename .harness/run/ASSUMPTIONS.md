# ASSUMPTIONS

> Autonomous mode only (ENGINE.md §14.1, ADR-027). Every decision the engine made alone, where a
> Collaborative run would have stopped to ask.

---

## A-001 - Leave the duplicated 72 dp Back width alone

- **Tier:** 2 (plan/architecture)
- **Iteration:** 2
- **First dependent checkpoint:** `390856c` (Iteration 2, Phase 1)
- **Revert:** `git revert 390856c` (undoes the whole Colour group; the assumption itself changed no code)

### Question

The review noted `GroupBackEntry` keeps its own 72 dp minimum width, repeating Beautify's private `ENTRY_MIN_WIDTH`. Should Beautify's row read the shared constant?

### Options Considered

1. Share one constant - consequences: touches `BeautifyToolRow.kt` again and risks Beautify's pixels (pictures 47/48, DoD 10).
2. Leave both - consequences: two numbers that must be kept equal by hand.

### Taken, and why

Option 2. DoD 10 (Beautify unchanged) outranks tidiness; the duplicate is recorded in `ISSUES.md`.
