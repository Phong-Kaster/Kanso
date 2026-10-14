# Kanso — Run 8: the five editing tools edit the photo

Today none of Kanso's editing tools changes a photo. Edit, Remove background, Enhance, Retouch and
Artistic each open the photo, show "coming in a later run", and Save writes an upright copy of the
original (see `README.md`, "How each editing feature works"). The image engine that can do the work is
already compiled into the app — `core/imagecrop`, `core/common`, `core/editimage`,
`core/imageprocessor`, `core/overlaybrush`, `core/localml`, `opencv/`, with their AARs and bundled
models in `libs/` — but nothing in `app/` calls it.

**This run makes all five tools really edit the photo, on the device, with that engine** (decided by
me, 2026-10-14). What a person sees on Save must be what they edited.

## What this run delivers

1. **Edit**: crop (free and the common ratios) and rotate; adjust brightness, contrast, saturation
   and warmth; apply filters. The person sees each change on the photo as they make it.
2. **Remove background**: cut the main subject out of the photo, show it over a checkerboard, and
   save it with a transparent background (PNG).
3. **Enhance**: a one-tap improvement of sharpness and detail (upscaling, where the engine offers it)
   and lighting, with a before/after comparison.
4. **Retouch**: face and skin retouching on a photo with a face — at least smoothing skin — with a
   strength control; a photo with no face says so instead of failing silently.
5. **Artistic**: turn the photo into a painterly style, from a choice of styles, with a strength
   control.
6. **Every tool**: the "coming in a later run" message is gone from the five tools; edits can be
   undone back to the original before saving; a long operation shows progress and never freezes the
   screen; a failure (out of memory, no subject found, a model that will not load) says what happened
   and leaves the original untouched; **Save writes the edited result**, never the unedited original;
   the original file is never changed.

## How to build it

- **Use the engine already in the repository.** `reference/retica-app/` shows how the original app
  called these libraries: read it to learn the calls, never build, edit or import it (CLAUDE.md).
  Where a prebuilt AAR has no source, learn its API from the classes it exposes and from how Retica
  called it.
- **Offline only.** No network permission, no model download: use the models bundled in
  `libs/localml.aar`. If a tool cannot be done with what is bundled, say so in the DoD rather than
  adding a download.
- **The owner's phone is a Galaxy A13 on 32-bit Android (`armeabi-v7a`).** Every tool must work
  there; a model too heavy for it must work on a smaller copy of the photo rather than crash.
- The device checks may use the Galaxy A13 or the emulator, whichever is free (decided by me,
  2026-10-14). Before driving either one, check nobody else's run or person is using it.
- Keep the Run 7 layout (InFlux arrangement, Kanso's colours, the floating bar). Tool controls fit
  that layout: the photo in its rounded card, the controls in the panel under it, one full-width Save.
- Every existing standing rule in `CLAUDE.md` holds — previews, one class per file, a ViewModel and
  UiState per screen, the code teaches.

## README.md is part of the work (decided by me, 2026-10-14)

Every behaviour or business-logic change updates `README.md` in the same commit (`CLAUDE.md`, "README.md
follows the code"). I pay the closest attention to the editing features: when this run ends, the
README's "Editing features" table and "How each editing feature works" must describe, for each of the
five tools, exactly what the code does — every processing step in order, which engine module,
library or model does it, threading, input and output size and format, and failure handling — with
file paths. A README claim the code does not back fails the run.

## Pictures and tests

- Roborazzi pictures of the five tool screens will change. Ask me, in the DoD, which pictures may be
  re-recorded, the same way Run 7 did.
- Each tool's processing gets tests that prove the output pixels changed the way the tool promises
  (not only that a screen appears).
