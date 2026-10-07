# ASSUMPTIONS

> Autonomous mode only. Every decision the engine made alone, where a Collaborative run would have stopped to ask.

---

## A-001 - The navigation host gets a preview through a stateless display

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 1 (bootstrap); the commit that first depends on it is T-004's
- **Revert:** `git revert <sha of T-004's checkpoint>`

### Question

CLAUDE.md says every composable has a preview, and a preview never touches Koin or a ViewModel. `KansoNavHost` builds every screen's ViewModel with Koin inside its entries. Is it previewed, exempt, or restructured?

### Options Considered

1. Exempt `KansoNavHost` from the rule - consequences: breaks the owner's "every composable" rule and the DoD's coverage check needs a special case.
2. Split it into the stateful `KansoNavHost` and a stateless `KansoNavDisplay` (back stack, onBack, decorators and entry provider as parameters); the preview supplies an entry provider drawing `HomeContent` with sample data - consequences: one more small composable, no behaviour change, and a reader can see Navigation 3 draw a screen, which suits the owner's goal of learning it.
3. Preview the whole host with a fake Koin - consequences: previews would touch Koin, which the rule forbids.

### Taken, and why

Option 2: it follows the PRD's own rule for screens that need wiring (split into stateful and stateless) and keeps the rule free of exceptions.
