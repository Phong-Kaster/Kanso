# Kanso — Run 10: stickers on the Edit screen

The Edit screen can crop, turn, flip, adjust colour and apply a filter, but it cannot put anything on
the photo. Pinity, another app of mine, has a sticker feature: a catalogue of 60 picture stickers
that can be dragged, pinched, turned and resized over the photo. **This run brings those stickers to
Kanso's Edit screen** (decided by me, 2026-10-26).

Pinity's code and pictures are copied, read-only, into `reference/pinity-stickers/` (from Pinity at
commit `58d4e27`). Treat that folder as `reference/retica-app/` is treated: never edit it, build it
or import from it. Port from it into Kanso's own packages.

## The decisions (mine, 2026-10-26)

1. **A sticker is part of the photo, not a layer above it.** It behaves like my phone's gallery
   editor: put a sticker on, then crop, and the crop can cut the sticker off; turn the photo and the
   sticker turns with it.
2. **Filters and the colour sliders colour the stickers too**, as they colour every other pixel.
3. **The 60 Pinity sticker pictures may be used in Kanso.** Only the four picture categories come
   across: Food (17), Mood (17), Quote (6), Emoji (20). Pinity's text, clock, date, time, location
   and weather decals do **not**.
4. **Stickers can be touched only while the Stickers panel is open.** With any other tool open, or
   none, the stickers are just part of the photo and a touch on one does nothing.
5. **At most 20 stickers on one photo.** Adding a 21st adds nothing and shows a short message.

## What this run delivers

1. **A fifth tool, Stickers**, after Filters in the row of tools: `EditTool.STICKERS`, with its own
   icon and name, highlighted while open and carrying the "changed" dot when the photo has a sticker.
2. **The Stickers panel**: tabs **Food / Mood / Quote / Emoji** over a grid of that category's
   stickers. Tapping one adds it to the photo, at the centre of the picture as shown, about a third
   of the shown picture's width, and selects it.
3. **The selected sticker carries a frame** with one round button on each corner:
   - **top left — delete:** removes the sticker;
   - **top right — duplicate:** adds a copy slightly down and to the right of it, and selects the copy;
   - **bottom left — flip:** mirrors the sticker left to right (tap again to mirror it back);
   - **bottom right — resize and turn, one handle:** dragging it scales the sticker by the finger's
     distance from the sticker's centre and turns it by the finger's angle around that centre, the
     sticker keeping its shape.
4. **Moving a sticker:** drag it with one finger; two fingers pinch to resize and twist to turn.
   Tapping another sticker selects that one; tapping the photo outside every sticker selects none.
   A newer sticker lies on top of an older one.
5. **Undo takes back sticker actions like any other action**, in the same stack: an add, a delete,
   a duplicate, a flip, and one whole gesture (a drag, a pinch, a pull on the corner handle) from
   finger down to finger up are each one action. Selecting a sticker is not an action.
6. **Compare** shows the untouched original, with no stickers on it.
7. **Done renders the stickers at full size** into the result, sharp, at the photo's own resolution.

## What to port, and what to rewrite

From `reference/pinity-stickers/`:

- **Port:** the sticker pictures (`res/drawable/img_sticker_*.webp`, 60 files), the handle icons
  `ic_close_circle`, `ic_flip` and `ic_resize_circle`, the category list (`Decal.kt`,
  `DecalCategory.kt`, picture entries only), the drawing of a sticker and its frame
  (`DecalOverlayPreview.kt`), and the one-finger drag and two-finger pinch/twist of
  `DecalOverlayPointer.kt` with its geometry helpers (`Offset.kt`, `KotlinUtil.kt`).
- **Draw new:** the **duplicate** icon, matching Kanso's own icons (Material Symbols, 960 viewport,
  as `res/drawable/ic_close.xml`), and the four corner buttons restyled to Kanso's theme colours, so
  they read as Kanso, not as Pinity.
- **Rewrite, do not port:**
  - Pinity's corners (top right deletes, bottom left turns, bottom right resizes) become the layout
    above; the bottom-right handle now does resize and turn together.
  - Pinity keeps each sticker as a changeable object in the ViewModel, placed in screen pixels.
    Kanso's edit state is one immutable `EditSettings` in an `EditSession`, and that is how Undo
    works. Stickers become a list in `EditSettings`, each one a value: which sticker, its centre
    as fractions of the **original** photo, its size as a fraction of the original photo's width, its
    turn in degrees and whether it is mirrored.
  - Pinity saves by screenshotting the view and stretching it to the photo's size
    (`EditPhotoFragment.kt` lines 660–755 in Pinity), which makes stickers blurry. Kanso renders
    them in `EditProcessor`, below.
  - Pinity's tap on an active sticker that swaps it for the next sticker in the catalogue is dropped.

## How to build it

- **Stickers first, then everything else.** `EditProcessor` draws the stickers onto the original
  photo before anything else, so the order becomes: **stickers, crop, quarter turns, flips, filter,
  colour sliders.** That one ordering is what makes decisions 1 and 2 true. Drawing them after the
  crop rectangle is cut out, shifted by its offset, gives the same pixels without a second
  full-size copy of the photo, which matters on the Galaxy A13.
- **One mapping between the screen and the photo.** A touch lands on the picture as shown, which is
  cropped, turned and flipped; the sticker is stored on the original. Put the mapping both ways
  (shown picture ↔ original) in one place, pure Kotlin, and test it on the JVM for every quarter
  turn, both flips and a crop: a point mapped there and back comes back to itself.
- **The live preview.** Kanso's preview draws crop, turns, flips and the sliders live in
  `LivePreviewImage`; only the filter is computed, by `FilterPreviewRenderer` on a small copy of
  the original. So: the stickers that are not being touched go into that small copy before the
  filter, as Done does. The sticker under the finger is drawn live by `LivePreviewImage`, inside
  the same crop, turn and flip and with the same colour matrix, and joins the small copy when the
  finger lifts. While a finger is on a sticker, that sticker shows the sliders but not the filter;
  that is accepted.
- **Selection is screen state, not edit state.** Which sticker is selected lives in `ToolUiState`,
  never in `EditSettings`, so selecting creates no undo entry and Done ignores it. Closing the
  Stickers panel selects none.
- **The Galaxy A13 on 32-bit Android decides.** A drag that stutters there with 20 stickers on the
  photo is a failure: say so rather than ship it.
- Every standing rule in `CLAUDE.md` holds: a light and dark preview for every new composable, one
  class per file, the screen-package folder layout, the code teaches.

## Tests and pictures

- JVM tests: each sticker action through `EditSession` (add, delete, duplicate, flip, one gesture as
  one undo entry, the 20-sticker limit); the screen ↔ photo mapping round trip; `EditProcessor`'s
  order: a sticker cut off by a crop is cut off in the output, and a sticker under a filter comes out
  filtered.
- **I approve recording the new pictures**: the row with five tools, the Stickers panel on each of
  the four tabs, a selected sticker with its four corner buttons, and a photo with stickers after a
  crop and a filter, in light and in dark. Existing pictures that change because the row now has five
  entries may be re-recorded; keep each beside its old version in
  `.harness/run/evidence/baseline-diffs/` while the run lasts.
- Device checks may use the Galaxy A13 or the emulator, whichever is free. Before driving either,
  check nobody else's run or person is using it.

## README.md and the written record are part of the work

- `README.md`, `docs/features/edit.md` and `docs/features/tool-screen.md` describe the Stickers
  tool, in the same change, mermaid flows included. `README.md`'s table of folders gains
  `reference/pinity-stickers/`, read-only.
- **A new ADR, `ADR-004`**, records that a sticker is part of the photo: drawn first, so crop, turns,
  flips, filter and sliders all apply to it, and what that cost (the filter missing from the sticker
  under the finger in the preview).
- `CONTEXT.md` gains **Sticker** (one placed on the photo) and **sticker catalogue** (the 60 to
  choose from), and `EditTool` now means five tools.

## How it is built

One commit per phase:

1. the sticker model in `EditSettings`, the undo actions, and `EditProcessor`'s new first step;
2. the pictures, the catalogue and the Stickers panel with its tabs;
3. the sticker on the photo: drawing, the frame and its four buttons, the gestures, the mapping;
4. the live preview with stickers;
5. the docs, the ADR, `CONTEXT.md` and the pictures.
