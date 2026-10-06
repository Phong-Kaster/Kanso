# ASSUMPTIONS

> Autonomous mode only (ENGINE.md §14.1, ADR-027). Every decision the engine made alone, where a
> Collaborative run would have stopped to ask. Run 2's numbering starts again at A-001 (Run 1's are in git history).
>
> To overturn one: answer its id in `.harness/run/DECISIONS.md` and re-run. A human answer always
> outranks an assumption — the engine reverts what depended on it.

---

## A-001 - What "upright" means when saving

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1 (bootstrap)
- **First dependent checkpoint:** T-007's checkpoint (filled in by the Iteration after it)
- **Revert:** `git revert <sha>`

### Question

Does "full resolution and upright" mean rotated pixels, or the original bytes with the orientation tag kept?

### Options Considered

1. Always decode, rotate pixels, re-encode - consequences: truly upright everywhere; a 50-megapixel photo is about 200 MB as ARGB_8888 and can run out of memory.
2. Always copy bytes and keep the EXIF tag - consequences: lossless and cheap, but some viewers ignore the tag, so the file is not upright for them.
3. Copy bytes when there is no rotation; decode and rotate only when EXIF says rotated; if that decode fails for memory, copy bytes keeping the tag and tell the user - consequences: upright pixels in nearly every case, no memory crash, one honest fallback.

### Taken, and why

Option 3. It meets the PRD's words in every case that can be done safely and never crashes.

---

## A-002 - Where a kept camera shot lives

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1 (bootstrap)
- **First dependent checkpoint:** T-006's checkpoint
- **Revert:** `git revert <sha>`

### Question

After Keep, does the shot go into the phone's gallery straight away, or stay private until Save?

### Options Considered

1. Straight into MediaStore - consequences: needs a write path on Android 8.1-9 and puts unedited shots in the phone's gallery.
2. App-private file in `filesDir`, shown by its file URI, copied to `Pictures/Kanso` only when Save is pressed - consequences: no permission, no FileProvider, survives process death; the file stays in app storage until the app is cleared.

### Taken, and why

Option 2. The PRD says Save is what writes to the gallery. Keep also replaces the camera in the back stack, so Back from the tool does not return to a finished camera.

---

## A-003 - No live theme preview while dragging the colour picker

- **Tier:** 2 (plan/architecture)
- **Iteration:** 1 (bootstrap)
- **First dependent checkpoint:** T-002's checkpoint
- **Revert:** `git revert <sha>`

### Question

Should the app re-colour live during the drag while writing once at the end?

### Options Considered

1. Live in-memory preview plus a debounced write - consequences: nicer feel, needs a second state source for the theme.
2. Debounced write only; the theme changes once the drag settles (about 300 ms) - consequences: smaller change, matches the PRD's "write once the drag settles".

### Taken, and why

Option 2, the smallest change that meets the PRD.

---

## A-004 - "Newest first" means the engine's own order

- **Tier:** Missing information
- **Iteration:** 1 (bootstrap)
- **First dependent checkpoint:** T-004's checkpoint
- **Revert:** `git revert <sha>`

### Question

Newest by date taken, added or modified?

### Options Considered

1. Re-sort by date taken - consequences: needs a new query, and the engine says to reuse `MediaRepository`.
2. Keep `MediaRepository`'s order (date modified, newest first).

### Taken, and why

Option 2: the PRD says to reuse `core/gallery`, and the engine already orders newest first.

---

## A-005 - CameraX version

- **Tier:** Missing information
- **Iteration:** 1 (bootstrap)
- **First dependent checkpoint:** T-006's checkpoint
- **Revert:** `git revert <sha>`

### Question

Which CameraX release? Newer androidx libraries needed AGP 9 and compileSdk 37 in Run 1.

### Options Considered

1. Newest stable regardless - consequences: may fail `checkDebugAarMetadata`.
2. Newest stable that `checkDebugAarMetadata` accepts on AGP 8.13 and compileSdk 36; record the pin in `PROJECT.md`.

### Taken, and why

Option 2, the same rule Run 1 applied; the PRD keeps AGP at 8.

---

## A-006 - Baselines the engine cannot move (RESOLVED by D-001)

- **Tier:** 3 (intent) - touches DoD criteria 30, R1, R3
- **Iteration:** 1 (bootstrap)
- **First dependent checkpoint:** T-001 / T-003 checkpoints
- **Revert:** `git revert <sha>`
- **Assumed DoD criteria:** none. Resolved Iteration 2: the human listed the four baselines (D-001); 7-9 removed with the placeholder tests, 6 is re-recorded in T-010 if the picture changes.

### Question

Re-recording `6_settings_custom_colour.png` and deleting `7_`-`9_placeholder_*` need the human's `## Baselines` listing, which only they can write.

### Options Considered

1. Wait for the listing - consequences: Autonomous mode never waits for an answer after the DoD gate.
2. If the listing is absent when the task needs it: keep the old tests green by leaving the old pictures in place and their tests removed only if listed; otherwise point the placeholder tests at the new Gallery screen under new picture names, and report the stale pictures in `ISSUES.md` for the human to delete.

### Taken, and why

Option 2. Nothing the engine does can move an approved baseline; the run reports the leftover instead of working around the lock.

---

## A-007 - Pictures 16/17 re-recorded because Save became enabled

- **Tier:** missing information
- **Iteration:** 4
- **First dependent checkpoint:** Phase 3 checkpoint
- **Revert:** `git revert <sha>`

### Question

Making Save work changes `16_tool_photo_light.png` / `17_tool_photo_dark.png` (the Save label is no longer greyed). Are they baselines the engine may move?

### Options Considered

1. Leave the old pictures and fail verifyRoborazzi - consequences: the build stays red.
2. Delete and record again, since DoD 10-19 are this run's own new pictures (only 6-9 are the human's) and no person has signed them; the engine opened both new pictures before committing.

### Taken, and why

Option 2. The DoD names 16/17 as the Save-visible tool screen, and the old pictures showed Save disabled, which DoD 25 no longer describes.
