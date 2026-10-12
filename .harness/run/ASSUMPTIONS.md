# ASSUMPTIONS

> Autonomous mode only (ENGINE.md §14.1, ADR-027). Every decision the engine made alone, where a
> Collaborative run would have stopped to ask. The Runtime renders this file into the This run tab of
> `FOREMAN.html` after every iteration, so each entry must make sense to someone who has read nothing else.
>
> To overturn one: answer its id in `.harness/run/DECISIONS.md` and re-run. A human answer always
> outranks an assumption — the engine reverts what depended on it. Or run the revert command below
> yourself, on the Loop Branch, after the run has stopped.
>
> Run 7 (the InFlux layout, Kanso's own colours). A-001 to A-024 are the readings Q1 to Q24 listed in
> `DoD.md` ("Readings the PRD leaves open"); each carries the same number as its Q. They were taken at
> the bootstrap (Iteration 1) so the DoD could be written; approving the DoD without editing a reading
> accepts it. **A-001 and A-005 are the two worth your eye.** The first checkpoint that depends on each
> is the Phase named in its entry; the Iteration after it replaces "Iteration N" with the commit SHA.


---

## A-001 - Every tool in one tap, against a carousel of four

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

The PRD's goal says every tool, the recent edits and the Camera are one tap from Home without scrolling past anything; item 2 puts the four AI tools in a carousel and says the two pill buttons are there "so both are on screen on a fresh launch without scrolling the carousel". Does "one tap" cover Enhance, Retouch and Artistic?

### Options Considered

1. (A) One tap covers Edit, Remove background, the Camera, the recent-edits arrow and the newest recent photo; Enhance, Retouch and Artistic take 1, 2 and 3 swipes of the carousel plus a tap - consequences: matches item 2's own wording; the carousel stays a carousel.
2. (B) Literally every tool one tap at launch, which needs a carousel showing all four banners at once or another entry for each - consequences: contradicts item 2's pills; rewrites DoD 6.

### Taken, and why

(A). Item 2's sentence about the pills only makes sense if the other banners need a swipe. DoD 6 is written to (A), prints the gesture count per tool, and says how it changes under (B). DoD 6 is the criterion it shapes; first checkpoint: Phase 3 (T-008, `HomeReachTest`).


---

## A-002 - Back from the tabs

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

What does Back do on My edits and on Settings?

### Options Considered

1. (A) Return to Home; tabs never stack (today's "Back from Settings goes Home").
2. (B) Each tab keeps its own history.
3. (C) Close the app.

### Taken, and why

(A). It keeps the behaviour the settings and navigation tests already expect; `selectTab` clears the stack down to Home before adding the tab. First checkpoint: Phase 1 (T-001).


---

## A-003 - The round Camera button

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

Where does the button take the person, and where does Back lead?

### Options Considered

1. (A) Camera for the Edit tool; a kept shot opens the Edit tool; Back returns to the tab it was opened from.
2. (B) Ask which tool first.

### Taken, and why

(A), as the Home shortcut worked. First checkpoint: Phase 1 (T-001, the Iteration's wiring).


---

## A-004 - Close on a photo opened from My edits

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

PRD item 6 says the round close button goes "back to Home" and lists "a photo opened from My edits" under the same heading. Close to Home, or back to My edits?

### Options Considered

1. (A) Close goes Home for both; system Back returns to My edits (the PRD's literal text).
2. (B) Close returns to My edits.

### Taken, and why

(A), the reading closest to the PRD's words. The other is a one-line change in the nav host. First checkpoint: Phase 2 (T-004, wiring).


---

## A-005 - "The photo fills the screen" on Saved (worth your eye)

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

Item 6 says the photo fills the screen behind a gradient. Does it fill edge to edge (cropping), or fill as much as fits?

### Options Considered

1. (A) Edge to edge, cropping the photo - consequences: nothing empty, but the sides (or top and bottom) of the saved photo are cut off.
2. (B) The whole photo as large as fits, centred, over a blurred copy of itself that fills the rest of the screen (the same look as the tool card) - consequences: nothing of the photo is cut off; some blurred band shows beside a narrow photo.

### Taken, and why

(B), so nothing of the person's work is hidden and Saved matches the tool card. (A) is a one-line change of the content scale. First checkpoint: Phase 2 (T-004).


---

## A-006 - The second button on a photo opened from My edits

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

Item 3 says Share and Edit; Saved says "Edit another". What does the wide button read on a My edits photo?

### Options Considered

1. (A) "Edit", opening that photo in the Edit tool; after a save it stays "Edit another".
2. (B) "Edit another" in both.

### Taken, and why

(A). First checkpoint: Phase 2 (T-004).


---

## A-007 - The success mark, the title and the saved line on a My edits photo

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

Which of Saved's extras appear on a photo opened from My edits?

### Options Considered

1. (A) No mark, title "My edits", no "saved in Pictures/Kanso" line.
2. (B) The same as Saved.

### Taken, and why

(A): the mark celebrates a save that just happened. First checkpoint: Phase 2 (T-004).


---

## A-008 - The gallery's own camera button

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

Item 5 lists the gallery's parts without it, and the "gone" list does not name it. Keep it?

### Options Considered

1. (A) Keep it: it is the only way to take a photo straight into a chosen tool.
2. (B) Remove it.

### Taken, and why

(A). First checkpoint: Phase 2 (T-006).


---

## A-009 - Names on the recent cards and My edits tiles

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

The gallery reads a photo's file name to screen readers. Do the new cards show names?

### Options Considered

1. (A) No visible text; each card has the description "Edited photo, <saved date and time>".
2. (B) Show the file name (`Kanso_20261010_101500.jpg`).

### Taken, and why

(A): file names are noise, and the date reads well aloud. First checkpoint: Phase 1 (T-002).


---

## A-010 - What the strip and My edits hold

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

Which photos, in what order, how many?

### Options Considered

1. (A) Everything readable in `Pictures/Kanso`, newest first by the file's last-changed time; the strip shows the newest 10, My edits all; no delete action.
2. (B) Only photos saved by this install.

### Taken, and why

(A). The PRD asks for no delete, and a delete would need a confirmation step. First checkpoint: Phase 1 (T-002).


---

## A-011 - Reading `Pictures/Kanso` without asking for a permission

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

On Android 8.1/9 reading needs the photo-read permission; on Android 10+ a reinstalled app no longer owns its old photos. May Home or My edits ask?

### Options Considered

1. (A) Never ask from Home or My edits: show what can be read, and the empty message otherwise; the gallery and the first save still ask as today.
2. (B) Ask when Home first opens.

### Taken, and why

(A): a permission prompt on launch would undo the speed goal and PRD says no new permission. Photos from an earlier install reappear once photo access is granted in the gallery. First checkpoint: Phase 1 (T-002).


---

## A-012 - Carousel kind and what is tappable

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

Which Material 3 carousel, and what do the banner, the button and the dots do?

### Options Considered

1. (A) `HorizontalUncontainedCarousel` with one-banner-per-swipe snapping (the next banner peeks); the whole banner and its "Try now" open the gallery; dots show position and are not tappable; no auto-advance.
2. (B) `HorizontalMultiBrowseCarousel` or the hero carousel, which shrink the neighbours to 40-56 dp slices of the banner's middle.

### Taken, and why

(A): full-size banners keep their text readable. First checkpoint: Phase 2 (T-005).


---

## A-013 - The bar with the keyboard open, and in landscape

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

The Settings colour box needs the keyboard; a floating bar could cover it.

### Options Considered

1. (A) The bar is hidden while the keyboard is open; in landscape it stays and content scrolls; the "nothing scrolled" promise is for portrait phones.
2. (B) The bar stays above the keyboard.

### Taken, and why

(A). First checkpoint: Phase 1 (T-001, `KansoTabShell`).


---

## A-014 - The blurred copy, and older phones

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

Compose's blur draws nothing below Android 12 (and likely nothing in the JVM tests).

### Options Considered

1. (A) Fill what the photo leaves empty with a tiny scaled-down copy stretched back up on every phone, plus Compose's blur on top on Android 12+; never a sharp duplicate.
2. (B) Compose's blur only: older phones show a sharp copy.

### Taken, and why

(A). First checkpoint: Phase 1 (T-003).


---

## A-015 - "One screen per tool"

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

Five separate screens with five ViewModels, or one screen shaped by the tool?

### Options Considered

1. (A) Each tool opens its own screen from its navigation key; one shared layout, ViewModel and UiState parameterised by the tool; no switching.
2. (B) Five packages.

### Taken, and why

(A): the options strips are placeholders until each tool's own run; that run can split a tool out. First checkpoint: Phase 1 (T-003).


---

## A-016 - Replace when the tool screen was not opened from a gallery

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

Replace goes back to the gallery for the same tool; what if the photo came from the camera, Recent edits or My edits?

### Options Considered

1. (A) Always leave that tool's gallery on top: pop to it if it is directly below, otherwise replace the tool screen with it.
2. (B) Disable Replace unless a gallery is below.

### Taken, and why

(A). First checkpoint: Phase 1 (T-001's `replaceWithGallery`, T-003).


---

## A-017 - Motion between tabs

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

The PRD is silent on how a tab change moves.

### Options Considered

1. (A) A fade-through between tabs; pushes keep today's slide; the bar does not move with the screens.
2. (B) The same slide as every other move.

### Taken, and why

(A): lateral moves between peers are a fade in Material guidance. First checkpoint: Phase 1 (T-001).


---

## A-018 - Thumbnails and the preview guard

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

New photo cards would add a composable that loads a real image; `PreviewCoverageTest.EXEMPT` may not grow without your approval.

### Options Considered

1. (A) Reuse the gallery's `PhotoThumbnail` (made `internal`, same name and file) through a slot with a coloured stand-in in previews; EXEMPT stays at three.
2. (B) Add a fourth exemption.

### Taken, and why

(A). First checkpoint: Phase 1 (T-002).


---

## A-019 - The colour picker's hex field, and the engine-edit constraint

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

PROJECT.md says never to edit engine source (Run 1) and never to rely on the hex field; PRD item 9 asks to fix exactly that field in `core/colorpicker`. Also: is lowercase `2e7d32` a complete code?

### Options Considered

1. (A) The PRD outranks the cache: one exception to the engine rule, limited to the hex field's `value`/`onValueChange` (DoD 39); lowercase counts, shown in capitals; exactly six digits apply, fewer change nothing.
2. (B) Work around it in the app.

### Taken, and why

(A). PROJECT.md's two Constraints were rewritten at the bootstrap. First checkpoint: Phase 3 (T-009).


---

## A-020 - New wording

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

The PRD fixes only "What will you edit today?", "Try now", "Recent edits", "Replace", "Save" and "Edit another".

### Options Considered

1. (A) The run writes the banner descriptions, the Settings row descriptions and the empty and loading messages; a person judges them in DoD 29 and 30.
2. (B) Stop and ask for each text.

### Taken, and why

(A). First checkpoint: Phase 2 (T-005).


---

## A-021 - ADR-001

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

The PRD reverses ADR-001's one-editor decision. How is that recorded?

### Options Considered

1. (A) A new `docs/adr/ADR-002` and a one-line "superseded in part" note at the top of ADR-001.
2. (B) Edit ADR-001.

### Taken, and why

(A): an ADR is a record, not a living document. First checkpoint: Phase 1 (the Iteration).


---

## A-022 - The selected tab's pill colour

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

The PRD says "a raised pill inside the bar" without a colour.

### Options Considered

1. (A) `inversePrimary` for the pill, `inverseSurface` for the icon on it, `inversePrimary` for the selected label, `inverseOnSurface` for unselected icons and labels: roles built to sit on the inverse surface, in all three schemes.
2. (B) `primaryContainer` with `onPrimaryContainer`.

### Taken, and why

(A): measured contrast of at least 3.0 (pill) and 4.5 (icon, labels) in DoD 22. First checkpoint: Phase 1 (T-001).


---

## A-023 - Settings' title

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

With no Back button Settings has no top bar.

### Options Considered

1. (A) A large "Settings" title is the first item of Settings' scrolling column; the word is on screen twice (title and tab) and tests find the tab by its tag.
2. (B) No title.

### Taken, and why

(A). First checkpoint: Phase 1 (T-001).


---

## A-024 - A tab's state when you leave it

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration N (filled in with the commit SHA by the Iteration after it)
- **Revert:** `git revert <sha>` (after the SHA is filled in)

### Question

A tab's screen is dropped when you switch tabs (a replaced key is popped).

### Options Considered

1. (A) Settings returns to the top, My edits reads the photos again (so a photo just saved is always there), Home keeps its place at the bottom of the stack.
2. (B) Keep every visited tab alive on the stack.

### Taken, and why

(A): simplest with Navigation 3's decorators. First checkpoint: Phase 1 (T-001, T-002).
