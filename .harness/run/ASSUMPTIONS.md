# ASSUMPTIONS

> Autonomous mode (ENGINE.md §14.1). To overturn one: answer its id in `.harness/run/DECISIONS.md` and re-run.

---

## A-001 - Work on a Loop Branch, not on main

- **Tier:** 3 (intent: PRD says "`main`, no branch and no worktree")
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 1 bootstrap commit
- **Revert:** `git revert <sha>` (the branch is simply not merged)
- **Assumed DoD criteria:** 29 (Constraints)

### Question
The PRD says Run 9 is built on `main` with one commit per phase. Invariant 7 forbids the engine touching the default branch.

### Options Considered
1. Commit to `main` as the PRD says - breaks Invariant 7, which outranks the PRD. 
2. Work on `loop/kanso-real-editor`, one commit per Phase - the owner merges (fast-forward gives the same history the PRD wanted).

### Taken, and why
Option 2. The commit-per-phase shape the PRD asks for is kept.

---

## A-002 - The filter is applied before the colour sliders

- **Tier:** 2
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 2 (T-002)
- **Revert:** `git revert <sha>`

### Question
`EditProcessor` applies colour, then filter; the PRD's live preview draws the filter into a bitmap and the colour on the GPU on top (filter first). They only agree if the order changes.

### Options Considered
1. Processor order crop, turn, flip, filter, colour - sliders stay instant on the GPU; Done's output for edits using both changes slightly from earlier runs.
2. Keep the order - a colour drag with a filter on needs a bitmap re-render, so it is no longer instant.

### Taken, and why
Option 1: the PRD's first requirement is instant colour sliders. Recorded in the ADR and README.

---

## A-003 - Back from Result keeps the whole session, undo stack included

- **Tier:** 3 (PRD item 4 "Done empties the stack and ends the session" vs item 7 "Back from Result returns to my session with everything intact")
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 6 (T-006)
- **Revert:** `git revert <sha>`
- **Assumed DoD criteria:** 11

### Question
Does Done empty the undo stack?

### Options Considered
1. Done keeps settings and stack; the session ends (stack empties) when Edit is left for good (Go home, Save then leave, Discard).
2. Done empties the stack but keeps settings.

### Taken, and why
Option 1: "everything intact" is the most literal reading of the later, more specific item 7; "Done ... ends the session" is read as the session ending when the screen is left.

---

## A-004 - Result's picture travels in a process-scoped store; Share uses a cache file

- **Tier:** 2
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 5 (Phase 5)
- **Revert:** `git revert <sha>`

### Question
A navigation key cannot hold a bitmap, and Share needs a content Uri before anything is saved.

### Options Considered
1. A Koin single `RenderedPhotoStore` holds the render; Share writes a temporary PNG into `cacheDir` and shares it through a FileProvider (no network, no gallery write). After a process restart the store is empty and Result says Nothing to show.
2. Share saves to the gallery first - breaks "Save is the only thing that writes".

### Taken, and why
Option 1. The FileProvider adds a manifest `<provider>` (no permission). "Writes to the device" is read as the gallery.

---

## A-005 - "Full size" keeps the existing working-size cap

- **Tier:** 2
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 6
- **Revert:** `git revert <sha>`

### Question
Done "renders at full size"; today Edit works at most `WorkingSize.EDIT` (2000 px) from a photo decoded at twice the screen's long edge.

### Options Considered
1. Keep the cap - the 32-bit A13 does not run out of memory; output matches earlier runs.
2. Re-decode the original uncapped - risks OOM on the A13.

### Taken, and why
Option 1. Documented in README as a limit.

---

## A-006 - Preview/processor colour agreement tolerance is 2 of 255 per channel

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 4 (T-003)
- **Revert:** `git revert <sha>`

### Question
The PRD says the two matrices "agree" without a tolerance.

### Taken, and why
8-bit rounding makes exact equality unlikely; 2/255 is the smallest value reasonable for GPU float vs CPU integer paths.

---

## A-007 - The other four tools keep Replace and the top-bar Undo

- **Tier:** 2
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 6
- **Revert:** `git revert <sha>`

### Question
The PRD removes Replace "on the Edit screen" and says the four others change "only at the two ends".

### Taken, and why
They keep Replace and Undo (Undo left of Done); only Save becomes Done and goes to Result. Back-asks applies to Edit only. On Result, "Edit another" is dropped (the PRD names Share, Save, Go home); the My edits viewer keeps its Share and close.

---

## A-008 - The cropper works on the original in its original orientation

- **Tier:** 2
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 4
- **Revert:** `git revert <sha>`

### Question
`EditProcessor` crops before turning, the preview shows the photo turned.

### Taken, and why
While Crop is open the card shows the un-turned original and the crop is stored as fractions of it, so it matches the processor's order exactly with no coordinate conversion. One crop gesture release is one undo action.
