# ASSUMPTIONS

> Autonomous mode only. Every decision the engine made alone, where a Collaborative run would have stopped to ask.

---

## A-001 - The navigation host gets a preview through a stateless display

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 1 (bootstrap); Iteration 5 (T-004)
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

## A-003 (Tier 2) - Camera leaves that cannot be drawn on the host

### Question

`LivePreview` (needs the camera surface) and `ShotView` (loads a photo file through Coil) have no preview of their own, and `CameraScreen`/`CameraBody` are stateful wrappers that call `CameraFrame`/`CameraContent`. DoD 1 allows "a thin stateful wrapper whose `...Content` part is" previewed. Does the coverage test accept them?

### Options Considered

1. Fail the coverage test on them - consequences: forces a fake camera into a preview, which DoD 4 forbids.
2. Accept, in `PreviewCoverageTest`, the composables that only fill the slots of a previewed `...Content` (`LivePreview`, `ShotView`) and the stateful wrappers `CameraScreen`, `CameraBody` - consequences: DoD 1's proof is narrowed by four named functions.

### Taken, and why

Option 2: DoD 4 (previews never touch the camera) outranks full coverage of the two slot fillers; the stand-ins in the previews draw their place. T-009 names the exemptions in its test.

- **First dependent checkpoint:** Iteration 3
- **Revert:** `git revert <sha of the loop(phase-2) checkpoint>`

## A-004 (Tier 2) - Gallery's PhotoThumbnail has no preview

### Question

`PhotoThumbnail` (the Coil-loading cell picture in `GalleryScreen.kt`) is never drawn by a preview. Does the coverage test accept it?

### Options Considered

1. Fail the coverage test - consequences: forces a Coil load of a real MediaStore photo into a preview, which DoD 4 forbids.
2. Exempt `PhotoThumbnail` in `PreviewCoverageTest`, like A-003 - consequences: DoD 1's proof is narrowed by one named function; the gallery previews pass a `SampleThumbnail` into the same slot.

### Taken, and why

Option 2: same reasoning as A-003 (DoD 4 outranks full coverage of a slot filler). T-009 names it in its exemptions.

- **First dependent checkpoint:** Iteration 3
- **Revert:** `git revert <sha of the loop(phase-3) checkpoint>`
