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


## A-002 - `sampleSizeFor` is exempt from "every top-level `sample...` is private"

- **Tier:** 2 (plan/architecture)
- **Iteration:** 2
- **First dependent checkpoint:** Iteration 2
- **Revert:** `git revert <sha of the loop(phase-1) checkpoint>`

### Question

DoD 5 says every top-level `sample...` declaration is private. `data/photo/PhotoLoader.kt` already has a public top-level `sampleSizeFor` (an image-decoding helper, not preview data) that an existing test calls. DoD 8 forbids changing existing tests. Which wins?

### Options Considered

1. Make or rename `sampleSizeFor` - consequences: breaks an existing test, which DoD 8 forbids.
2. Exempt that one name in `PreviewHygieneTest` (`NOT_PREVIEW_SAMPLES`) - consequences: DoD 5's proof is narrowed by one named, non-preview function.

### Taken, and why

Option 2: DoD 8 is the hard rule of the PRD ("do not change a test"); the function is not sample data for previews, so the intent of DoD 5 is kept.
