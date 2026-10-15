# Kanso — Run 9: the Edit screen becomes a real editor

Today the Edit screen shows every control at once. Crop chips, the free crop's two range sliders,
four turn and flip buttons, four colour sliders and seven filter chips all live in one 260 dp strip
under the photo (`app/src/main/java/com/phongkaster/kanso/ui/tool/EditPanel.kt`). It is crowded, and
worse, nothing can be seen while it is being done: there is no live preview, a slider springs back to
the middle the moment I let go, changes stack on top of each other, and the photo itself resizes
whenever the panel grows.

**This run turns Edit into an editor I can actually see** (decided by me, 2026-10-15): a row of four
tools under a photo that never moves, each opening a bottom sheet, with the photo changing under my
thumb as I drag.

## What this run delivers

1. **A fixed preview and a row of four tools.** The photo takes 60% of the screen below the top bar
   and **never changes size**, whatever is open. Under it, a horizontal lazy row of four entries,
   each an icon over its name: **Crop**, **Rotate**, **Adjust**, **Filters**. Tapping one opens a
   bottom sheet that fills the remaining 40%, with no scrim, so the photo above stays bright.
2. **Live preview.** Drag Brightness and the photo brightens as I drag. Every tool previews: the
   four colour sliders, the filter and its intensity, the crop rectangle, the turns and flips.
3. **The screen remembers what I set.** One set of settings for the whole session: brightness 70
   stays 70, and it is still 70 when I open the Adjust sheet again. No slider springs back to the
   middle. The preview is always the original photo with all the current settings applied, so
   nothing stacks and nothing is baked in as I go.
4. **Undo is a stack of my actions.** A slider release, a filter chip, a crop, a quarter turn — each
   is one action. Undo pops the top one and the photo goes back to how it was before it. It works
   across sheets. Done empties the stack and ends the session.
5. **A compare switch.** Next to Undo. Switched on, I see the untouched original and every editing
   control is disabled at once. Switched off, I see my edit again and the controls come back.
6. **Undo and compare float on the photo**, in one small pill at the **bottom right** of the preview,
   so they are reachable while a sheet fills the band below.
7. **Done renders, Result saves.** The top right of the Edit screen is **Done**, not Save: it renders
   the settings at full size and writes nothing. It opens the **Result** screen, which carries
   **Share**, **Save** and **Go home**; Save is the only thing that writes to the device. Back from
   Result returns to my session with everything intact.
8. **Back asks.** Leaving Edit with changes shows "Discard your changes?" — Discard or Keep editing.

## The four sheets

- **Crop** — the photo card becomes a real cropper, with handles on the rectangle, drag to move and
  pinch to zoom, driven by `core/imagecrop` (the easycrop source already in this repository, used by
  nothing yet). The ratio chips sit in the sheet and lock the rectangle's shape.
- **Rotate** — turn left, turn right, flip across, flip upside down.
- **Adjust** — Brightness, Contrast, Saturation, Warmth. Each runs **−100 … 0 … +100** with the
  number shown beside its name and 0 at the centre.
- **Filters** — **None** and the seven presets, **one at a time**: tapping 1977 replaces Kelvin.
  Intensity runs 0 … 100 and is live, so dragging it changes the photo under my thumb.

## How to build it

- **The colour sliders must be instant on my phone.** Brightness, Contrast, Saturation and Warmth
  are drawn with Compose's `ColorFilter.colorMatrix`, so the GPU does them every frame with no bitmap
  work. Crop, the turns and the flips are geometry, also free. Only the filter and its intensity need
  a real pass: re-render a small preview bitmap (480–720 px) off the main thread, dropping frames
  that a faster drag has already overtaken.
- **What I see must be what I get.** A test runs the preview's colour matrix and `EditProcessor`'s
  matrix over the same picture and proves they agree, so the preview never lies about what Done will
  render.
- **One settings object.** `EditSettings` stops meaning "one change" and means "the session's state";
  `EditProcessor` already takes a whole `EditSettings`, so it runs against the **original**, not
  against the photo as shown. The old "apply one change at a time onto the shown bitmap" workaround
  goes, and with it the known defect where a slider released during a run is silently dropped.
- **The row entries are `EditTool`** (`CROP`, `ROTATE`, `ADJUST`, `FILTERS`), a new enum in
  `data/edit/edit/`. `Tool` keeps meaning the five tools, one screen each. A row entry is highlighted
  while its sheet is open, and carries a small dot whenever its settings are not neutral.
- **The other four tools keep their screens** (Remove background, Enhance, Retouch, Artistic): no row,
  no sheets. They change only at the two ends — their bottom Save becomes **Done**, and they go to the
  **Result** screen, which is `Saved` renamed and no longer means "already written".
- **The Galaxy A13 on 32-bit Android decides.** A preview that stutters there is a failure, not a
  compromise to be made quietly: if the conflated filter preview cannot keep up, say so and we change
  the DoD.
- Every standing rule in `CLAUDE.md` holds — a preview for every composable, one class per file, a
  ViewModel and a UiState per screen, the code teaches, no new animation library without my approval.

## What this run removes, on purpose

Filter stacking (one filter at a time now); sliders that spring back to the middle; the two range
sliders as the only way to crop; the **Replace** chip on the Edit screen; the full-width Save bar at
the bottom of all five tool screens; `Saved` as a name. Each is recorded as a deliberate removal, not
left to look like a regression.

## README.md and the written record are part of the work (decided by me, 2026-10-15)

- `README.md`, `docs/features/edit.md`, `docs/features/tool-screen.md` and
  `docs/features/saving-and-share.md` are rewritten in the same change, down to the mermaid flows and
  the limits. A claim the code does not back fails the run.
- **A new ADR** records what this reverses in `ADR-002`: Save leaves the bottom bar for a top-right
  Done on all five screens, `Saved` becomes `Result` and stops meaning "already written", and Edit
  gives up the all-at-once options strip.
- **A new `CONTEXT.md`** at the root writes down the words: `Tool` (the five), `EditTool` (the four),
  `EditSettings` (its new meaning), session, preview.

## Pictures and tests

- **I approve the re-recording** of `tool_edit_free_crop_light` and `tool_edit_free_crop_dark`, and
  the recording of the new pictures: the tool row, each of the four sheets, the compare switch on, and
  the Result screen's Save — in light and in dark, about 14 images. Keep each beside its old version
  in `.harness/run/evidence/baseline-diffs/` while the run lasts.
- The tests that assumed stacking, springing-back sliders, an intensity that only affects the next tap,
  or a Save that writes a file are rewritten with their reason given.
- The device checks may use the Galaxy A13 or the emulator, whichever is free. Before driving either,
  check nobody else's run or person is using it.

## How it is built

`main`, no branch and no worktree (decided by me, 2026-10-15), one commit per phase:

1. the layout — fixed photo, the row, the sheets, the floating pill;
2. the settings model and the undo stack;
3. live preview;
4. the cropper;
5. Done and Result, across all five tools;
6. the docs, the ADR, `CONTEXT.md` and the pictures.
