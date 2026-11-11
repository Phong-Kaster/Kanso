# Kanso

*Kanso (簡素): Japanese for simplicity, leaving out what is not needed.*

Kanso is an offline Android photo editor. It has no ads, no analytics, no accounts and no network
access: the manifest removes the `INTERNET` and `ACCESS_NETWORK_STATE` permissions that the engine
libraries would otherwise merge in (`app/src/main/AndroidManifest.xml`). Every model, filter, sticker
and font ships inside the app; nothing is downloaded. A photo leaves the phone only when you send it
to another app with Share.

This document describes **what the code on branch `feat/text-layers` does today**. It is not a
roadmap. Each feature links to its own page in [`docs/features/`](docs/features/) for the full
detail, and every design decision of weight has a record in [`docs/adr/`](docs/adr/).

> **Status.** The app works from start to end: Home, a photo gallery for each tool, the camera, a
> screen for each of the five editing tools, a Done button that opens a Result screen (Share, Save
> into `Pictures/Kanso`, Go home), the My edits tab and Settings. The JVM test suite (Robolectric
> and Compose UI tests) passes. On the owner's Galaxy A13 (2026-10-28) the Stickers sheet and the
> Text sheet were driven by hand: adding a text, typing with the keyboard up (only the text area
> shows), and the rows and sliders coming back when the keyboard closes. The other tools were last
> run on an emulator (API 34, x86_64, 2026-10-14), before Run 9 rebuilt the Edit screen.

---

## Table of contents

1. [Features](#1-features)
2. [Editing features at a glance](#2-editing-features-at-a-glance)
3. [How each editing feature works](#3-how-each-editing-feature-works)
   - [3.1 The Edit screen](#31-the-edit-screen)
   - [3.2 Layers: what stickers, text and dates share](#32-layers-what-stickers-text-and-dates-share)
   - [3.3 Stickers](#33-stickers)
   - [3.4 Text](#34-text)
   - [3.5 Date](#35-date)
   - [3.6 Remove background, Enhance, Beautify and Artistic](#36-remove-background-enhance-beautify-and-artistic)
4. [Architecture](#4-architecture)
5. [Privacy and permissions](#5-privacy-and-permissions)
6. [Building and testing](#6-building-and-testing)
7. [Project layout and origins](#7-project-layout-and-origins)
8. [Decision records](#8-decision-records)
9. [Third-party licences](#9-third-party-licences)

---

## 1. Features

### Editing tools

Every tool opens on its own screen with the same frame: Back, the tool's name, Undo (an arrow
icon; Edit adds a Redo arrow beside it) and Done at the top right, the photo in a card, and the tool's controls. Done renders the result and opens the
Result screen. [The tool screen](docs/features/tool-screen.md) explains the shared parts: decoding,
the working size, progress, Undo, failures and Done.

| Tool | What it does | Saved as |
|---|---|---|
| [Edit](docs/features/edit.md) | Eight entries in a row under the photo (Crop, Rotate, Adjust, Colour, Filters, Stickers, Text, Date) with Compare pinned at the right; tapping Colour swaps the row for Back, RGB, HSL and Curves (with Compare still at the right), and Back returns to the first row. Crop with handles, pinch and seven shapes; quarter turns and flips; brightness, contrast, saturation and warmth; the red, green and blue channels each turned down or up (RGB, reached through Colour, as are HSL and Curves); one of eight filters with an intensity; up to 20 **layers** (catalogue stickers, typed texts and dates together), each moved, pinched, turned, resized, faded, duplicated and deleted on the photo. The photo is a live preview; Done renders it once. | JPEG quality 100, at most 3000 px (an untouched photo: its own file) |
| [Remove background](docs/features/remove-background.md) | Cuts the person out with ML Kit's bundled selfie-segmentation model and makes the rest transparent, shown over a checkerboard. People only, in practice. | PNG with transparency, at most 2000 px |
| [Enhance](docs/features/enhance.md) | Shrinks the photo to 512 px, upscales it four times with the bundled Real-ESRGAN model and brightens it if it is dark; hold to compare with the original. | JPEG, at most 2048 px |
| [Beautify](docs/features/beautify.md) | Zooms onto the largest face, then a two-level row of tools like Meitu's (Face, with Width, Lift, Temple, Cheekbones, Chin length, Hairline and a pinned Back; Body, with Waist, Arms, Chest and Legs; Smooth): Face width (-100..+100) and Face lift (-100..+100, the jowls and jaw dropped or raised and tightened), and [Temple, Cheekbones, Chin length, Hairline](docs/features/face-shape.md) (each -100..+100, measured against Meitu), where MediaPipe's 478 face landmarks drive a Kotlin warp of the face's outline; Waist, Arms and Legs (-100..+100, slimmer to the right) and Chest (-100..+100, fuller to the right), where MediaPipe's pose model (33 body points and a person mask) places them for the same warp (Chest grows the pixels instead of sliding them); and Smooth (OpenCV's bilateral filter, 0..100). The photo follows the slider live while dragging (a mesh warp on the GPU); letting go renders both at full quality quietly in the background. | JPEG, at most 2000 px |
| [Artistic](docs/features/artistic.md) | Repaints the photo in one of four styles (Warm wash, Cool wash, Mosaic, Ink) with the bundled Inception v3 style-transfer models, mixed by a strength slider. | JPEG, at most 1024 px |

### Getting a photo

| Feature | What it does |
|---|---|
| [Gallery and albums](docs/features/gallery-and-albums.md) | Each tool's photo picker: the phone's photos newest first, filtered by album, with full, partial (Android 14+) and refused photo access handled. |
| [Camera](docs/features/camera.md) | Takes a photo into Kanso's private storage, with lens switch, flash and pinch zoom; Keep opens it in the tool. |

### After editing

| Feature | What it does |
|---|---|
| [Result, Save and Share](docs/features/saving-and-share.md) | Done hands the rendered picture to the Result screen in memory (`RenderedPhotoStore`). Save is the only gallery write: a new file in `Pictures/Kanso`; the original is never changed. Share writes a copy into the cache's `shared/` folder and sends it through FileProvider with a one-off read grant. |
| [My edits and the photo viewer](docs/features/my-edits.md) | A tab listing everything Kanso saved, newest first; a photo opens in a viewer with Share and Edit. |

### Around the app

| Feature | What it does |
|---|---|
| [Home](docs/features/home.md) | A carousel of the four AI tools, five round tool shortcuts, the Recent edits strip and a floating tab bar with its Camera button. |
| [Settings](docs/features/settings-and-theme.md) | Theme mode (light, dark, system), theme colour (Kanso blue, wallpaper on Android 12+, or any custom colour from an HSV picker) and Kanso's own text size. Applied at once and kept across restarts. |
| [Launcher icon](docs/features/launcher-icon.md) | An enso ring around a lens, in the colour closest to the chosen theme colour. |

---

## 2. Editing features at a glance

"Works" means the path is complete in code from the control to the saved file and covered by the
JVM tests. On the owner's phones (Galaxy A13 and vivo V2408A, 2026-10-28) the Date sheet was tried
and an edited 2K photo was saved without losing quality; on the vivo V2408A the owner tried RGB
(2026-11-03) and found it works as expected; the other features have not been checked on a phone
yet.

| Feature | State | Notes |
|---|---|---|
| Edit: Crop | Works | `core/imagecrop`'s cropper on the original photo: handles, drag, pinch; Free or a locked shape. One finished gesture is one Undo step. |
| Edit: Rotate and flip | Works | Quarter turns left and right, flip across, flip upside down. |
| Edit: Adjust | Works | Brightness, contrast, saturation and warmth: four tiles over one slider for the chosen tile. Drawn live on the GPU as a colour matrix. |
| Edit: RGB | Works | Red, Green and Blue: three colour circles with no text, drawn and coloured like HSL's Red, Green and Blue circles, over one slider for the chosen channel, tinted in that channel's colour, -100 (the channel gone) to +100 (the channel doubled). Part of the same colour matrix as Adjust, so drawn live on the GPU. Checked by the owner on the vivo V2408A (2026-11-03, works as expected, x2 kept) and on the Galaxy A13 (2026-11-06). |
| Edit: Filters | Works | None or one of eight filters with an intensity; a new filter replaces the old one. Previewed on a 640 px copy. |
| Edit: HSL | Works | Hue, saturation and lightness for nine colour ranges (27 values, -100 to +100): nine colour circles, three chips and one slider (`editpanel/hslpanel/`). Applied after the filter and before the colour matrix, in the live preview (even with no filter) and in Done, by one function (`EditHsl.kt`, [ADR-008](docs/adr/ADR-008-hsl-is-computed-on-the-cpu-by-one-formula.md)). Checked by the owner on the Galaxy A13 and vivo V2408A, 2026-11-06 (works as expected); its time per frame on the Galaxy A13 is not measured. |
| Edit: Curves | Partial | Four chips alone in the top row, four equal-width entries that share the width and never scroll (**RGB**, **R**, **G**, **B**, each with a colour dot, RGB's dot sweeping three colours; a screen reader hears RGB, Red, Green, Blue; `CurveChips.kt`, `CurveChannelDot.kt`) choose which curve the graph edits; the graph (`editpanel/curvespanel/`) is wide (about 2:1), drawn with only a frame and three vertical lines (no diagonal, no horizontal lines), and edits that curve (the line is the theme's text colour for RGB and the RGB sheet's tints for R, G and B): a tap within 24 dp of a point selects it, a tap within 24 dp of the drawn line adds a point on the line and selects it (at most 10), a tap elsewhere only clears the selection, a drag from a point moves it and a drag from the line adds a point and drags it (the first and last points move sideways too), and the trash button (`CurveDeleteButton.kt`, spoken "Delete point") deletes the selected point, end points too, while more than 2 remain; the line is a natural cubic spline that may overshoot between points and is clipped to pure black and white, and it is flat left of the first point and right of the last; Reset is a 48 dp icon button in a column right of the graph (the trash button sits above it) and straightens only the chosen curve, and is disabled while that curve is straight. RGB is chosen first and the editor remembers the last chosen chip; choosing a chip is not an Undo step and changes nothing in the photo. Undo and Redo step one gesture at a time, and the tool's changed dot shows while the curve is bent. The saved photo contains the curve: Done runs filter, HSL, curves, colour matrix (`EditCurves.kt` on the CPU, 256-entry tables from a natural cubic spline, [ADR-011](docs/adr/ADR-011-curves-are-a-natural-spline-with-free-end-points.md), [ADR-009](docs/adr/ADR-009-curves-run-on-the-cpu-after-hsl.md)). **The live preview follows the curve**: while you drag a point the photo on screen changes (a 640 px CPU copy, even with no filter chosen), and it matches the saved picture; a sticker under your finger is drawn without the CPU stage (curves included) until you let go. A screen reader hears the graph ("Red curve, N points" and so on), the chips and Reset, but cannot move points. Not measured on any phone. |
| Edit: Stickers | Works | 60 bundled pictures in four categories. Each sticker is a **layer** (see below), with a mirror button. |
| Edit: Text | Works | Typed words in one of six fonts (the phone's own and five bundled, all drawing Vietnamese), twelve ready colours or any colour from a picker, an optional background box (twelve colours, or any colour with transparency), three alignments, wrapping lines. Each text is a **layer**. |
| Edit: Date | Works | A date and time written in one of 20 English formats (`Wed, 28 October 2026`, `2026`, `'26 10 28`, `14:30` …), starting at now, changed with a day picker and a time picker; the Text sheet's fonts, colours and background box. A tap on the selected date moves it to the next format. Each date is a **layer** ([ADR-006](docs/adr/ADR-006-a-date-is-its-own-layer-kind.md)). |
| Edit: Layers (stickers, text and dates) | Works | Up to 20 in total. Select by touch (from any sheet but Crop), move with one finger, pinch and twist with two, resize and turn with the corner handle; Size (1–100) and Opacity (10–100%) tabs in all three layer sheets; a selected layer comes to the front. Drawn into the photo before the crop, so crop, turns, flips, filter and sliders all act on it ([ADR-004](docs/adr/ADR-004-sticker-is-part-of-the-photo.md), [ADR-005](docs/adr/ADR-005-stickers-and-text-are-layers.md)). |
| Edit: Undo, Redo and Compare | Works | Undo (↶, top bar) goes back one action at a time; Redo (↷, beside it) brings back what Undo took back, in order, until a new action empties it. Compare (pinned in the row) shows the original while it is on and disables every editing control except Done. |
| Remove background | Works | ML Kit selfie segmentation: people only, in practice. |
| Enhance | Works | Real-ESRGAN four-times upscale of a 512 px copy, plus brightening when dark. |
| Beautify | Works | Face width, Face lift, Waist, Arms, Chest and Legs (MediaPipe face and pose landmarks plus a Kotlin warp) and OpenCV bilateral smoothing, each a slider; zooms onto the face (the whole photo while Body is open). Width and smoothing seen on the Galaxy A13, 2026-10-28; Waist at ±100, Arms at 99, Chest at ±100 and Legs at 100 rendered on the A13 with no crash, each changing only its own part of the body, and the first body drag follows the finger, 2026-10-29; Lift not yet judged on a phone. |
| Artistic | Works | Four styles from the Inception v3 style-transfer models, with a strength slider. |
| Done and Result | Works | Done hands the picture to Result; Save is the only gallery write. |

**Known limits.**

- The row of tools scrolls sideways. On the owner's Galaxy A13 (checked on the phone, 2026-10-28,
  with seven tools) Crop to Stickers and Compare were in view and Text was hidden past the right
  edge until the row was swiped left; Date sits further right still and is reached the same way.
  Since Run 13 (2026-11-06) the first row holds eight entries (Crop, Rotate, Adjust, Colour,
  Filters, Stickers, Text, Date) with Compare pinned at the right, because RGB, HSL and Curves moved
  into the Colour group. That is fewer swipes than the ten-entry flat row needed, but eight 60 dp
  entries plus Compare's 68 dp are still wider than a 411 dp screen: Stickers is partly cut and
  Text and Date are off screen until the row is swiped left (not yet looked at on a phone). The
  Colour layer is Back (at least 72 dp, `BACK_ENTRY_MIN_WIDTH` in `GroupBackEntry.kt`), three 60 dp
  entries (RGB, HSL, Curves), a divider and Compare's 68 dp: 72 + 3 x 60 + 68 = 320 dp plus
  spacing, which fits a 411 dp screen without scrolling. This replaces the older notes about the flat row growing to nine and ten
  entries, and the old rule that every tool fits without scrolling (DoD 2), which held for five tools.
- **The Colour group's row is a view, not a state.** Whether the second layer shows is a
  `rememberSaveable` flag inside `EditToolRow` (it survives a rotation, is not in `ToolUiState` or the
  ViewModel, and is lost when Stickers, Text or Date replace the row, or when a sticker or text layer is touched
  on the photo: its sheet replaces the row, and after Confirm the row is back on layer 1). The phone's Back gesture is
  unchanged: it closes an open sheet, then asks to discard or leave; it does not step back to layer 1.
- **HSL limits** (decision record: [ADR-008](docs/adr/ADR-008-hsl-is-computed-on-the-cpu-by-one-formula.md)).
  Hue turns by at most 20 degrees at +-100, so a colour cannot be swapped for a far one. Greys,
  white, black and near-greys are never changed (a gate in `EditHsl.kt` skips them), so no tint
  can be added to them, and a colour already at full saturation cannot get more. Lightness stops at
  0.95 and 0.05, never pure white or black. Each range fades into its two neighbours, so changing one
  range also moves colours a little way toward the next centres. The live preview is computed on the
  640 px copy, so on screen it holds less detail than the saved picture; the colours are the same
  formula. **Cost on the Galaxy A13: not measured**, because the A13 was not available to the run.
  The owner checked HSL on the Galaxy A13 and vivo V2408A on 2026-11-06 and signed it off as working.
- An untouched PNG, WebP or HEIC photo saved from Result keeps its own bytes inside a file named
  `.jpg` (a known defect, not fixed yet).

---

## 3. How each editing feature works

Paths are under `app/src/main/java/com/phongkaster/kanso/` unless they start with `core/`, `libs/`
or `app/src/main/res/`.

### 3.1 The Edit screen

1. **Screen and ViewModel.** `navigation/KansoNavHost.kt` shows the `ToolPhoto(tool, photoUri)` key
   with `ui/tool/ToolScreen.kt` and `ui/tool/ToolViewModel.kt`, which exposes one
   `StateFlow<ToolUiState>` (`ui/tool/ToolUiState.kt`). The Edit layout is a `Scaffold` whose
   `bottomBar` is `ui/tool/component/editbottombar/EditBottomBar.kt`: the row of tools
   (`editbottombar/edittoolrow/EditToolRow.kt`, a `LazyRow` of 60 dp entries that scrolls sideways,
   so more tools can be added later, with Compare pinned outside it at the right) with, just above it, a 220 dp sheet area
   (`editbottombar/EditSheet.kt`, `EDIT_SHEET_HEIGHT`) for Crop, Rotate, Adjust, RGB, HSL, Curves and Filters, which never changes height.
   The row has two layers. Layer 1 is Crop, Rotate, Adjust, Colour, Filters, Stickers, Text, Date, then
   Compare; **Colour** (`EditToolGroup.COLOUR` in `ui/tool/model/EditToolGroup.kt`, entries listed by
   `model/EditRowEntry.kt`, palette icon `ic_palette`, name from `strings_edit_colour.xml`) opens no tool: it
   swaps the row for layer 2 (`edittoolrow/EditColourGroupRow.kt`), Back pinned at the left, then RGB, HSL,
   Curves, then Compare. Colour and each of the three keep a "changed" dot while their settings change the
   photo. Back closes an open colour sheet (the change is kept) and returns to layer 1; tapping Colour while
   another tool's sheet is open closes that sheet; while Compare is held every entry but Compare is disabled.
   The Back entry and the sideways slide between layers are shared pieces, `ui/tool/component/GroupBackEntry.kt`
   and `GroupRowTransition.kt` (`groupRowTransition`), also used by Beautify's row
   (`beautifypanel/BeautifyToolRow.kt`, which looks and works as before).
   It was 136 dp until 2026-11-06, when the owner found the photo took too much of the screen and the
   tools too little; on a Galaxy A13 the photo went from about 523 dp to 434 dp tall.
   The photo (`ui/tool/component/EditPhotoArea.kt`) fills the space between the top bar and the
   bottom bar. The sheet is a plain `Surface`, not a `ModalBottomSheet`, so the row stays usable.
   A **layer sheet** (Stickers, Text or Date) takes the row's place instead, in exactly the height of
   the row and the sheet area together, so **the photo never changes size or place** whatever is
   open (the owner's decision, 2026-10-28). Its top bar (✕, title, ✓) is pinned; under it a row of
   **tabs** (`editpanel/LayerTabRow.kt`, Material filter chips) picks the ONE control shown below,
   which scrolls if taller than the room left. While the Text keyboard is up the sheet shrinks to its
   top bar and text area and the bar rides on the keyboard (`WindowInsets.ime` in `EditBottomBar`,
   with `android:windowSoftInputMode="adjustResize"` on `MainActivity`), **over** the photo:
   `ToolContent` sizes the photo from the bar's height measured with the keyboard down
   (`restingBarPx`), not from its height now.
2. **Decode.** `ToolViewModel.load` decodes the photo upright on `Dispatchers.IO`
   (`data/photo/PhotoLoader.kt`), at most twice the screen's longest edge. That bitmap is the
   original for the whole visit; the photo's file is never changed.
3. **The session.** Every change is one immutable `EditSettings` (`data/edit/edit/EditSettings.kt`)
   inside an `EditSession` (`data/edit/edit/EditSession.kt`) with its undo stack. `change` is one
   action (a chip, a turn, a finished crop); `preview` follows a finger or a typing run without an
   undo entry; `commit` makes the whole run one entry. `undo` moves the settings it leaves onto a
   redo stack and `redo` (`ToolViewModel.redo`, the ↷ icon) brings them back; any new action
   empties that stack. Undo and Redo store settings, never pixels. The
   cropper, `ui/tool/component/EditCropper.kt`, wraps `core/imagecrop`'s `CropperPreview` and shows
   the original with the layers baked into a copy of it, un-turned and unfiltered, because the crop
   is kept as fractions of the original.
4. **Live preview.** `ui/tool/component/photocard/LivePreviewImage.kt` draws the original on a
   Compose `Canvas` every frame: crop, quarter turns, flips, and Adjust's four sliders with RGB's
   three channels as one `ColorFilter.colorMatrix` (`data/edit/edit/EditColourMatrix.kt`). The CPU part is
   `data/edit/edit/FilterPreviewRenderer.kt`: the filter, its intensity, the HSL values (when any
   is not 0, run by `EditHsl.apply` after the filter, so the preview works with no filter chosen)
   and every layer except the one under a finger, rendered with the real `EditProcessor` on a copy of the original at most
   640 px (`PREVIEW_EDGE`), on `Dispatchers.Default`. The ViewModel sends requests through a
   conflated `Channel`, so only the newest one is rendered, and skips a request equal to the last.
5. **Done.** `ToolViewModel.done` shows a spinner in Done and renders the current settings once
   from the decoded original, shrunk to at most 3000 px (`WorkingSize.EDIT`, so a 2K photo keeps its size), with
   `data/edit/edit/EditProcessor.kt` on `Dispatchers.Default`, in this order: **layers**, crop,
   quarter turns, flips, filter (look-up images from `libs/imageprocessor.aar`,
   `libs/editimage.aar` and Kanso's own `app/src/main/assets/filters/`, read from the assets), HSL
   (`data/edit/edit/EditHsl.kt`, after the filter and before the colour matrix; skipped when all 27
   values are 0, so the saved picture is then identical to one saved without it), colour
   matrix (Adjust and RGB), then every pixel made opaque. If no
   Edit control was ever used, the decoded original is handed over unchanged with its Uri. The
   picture goes into `data/edit/RenderedPhotoStore.kt` and the `Result` key is pushed above Edit;
   Back from Result returns to the session intact.
6. **Result.** `ui/result/ResultScreen.kt` offers Share, Save and Go home. Save
   (`data/photo/MediaStorePhotoSaver.kt`, `Dispatchers.IO`) writes a new JPEG at quality 100 named
   `Kanso_<yyyyMMdd_HHmmss>.jpg` into `Pictures/Kanso`. Share (`ui/result/SharePhoto.kt`) writes a
   copy under the cache's `shared/` folder and sends it through FileProvider.
7. **On failure.** A Done render that runs out of memory ("This photo is too large to edit on this
   phone…") or fails otherwise shows its message at the top of the photo card, hands nothing over
   and keeps every setting and Undo step. A failed filter preview leaves the preview empty: the card
   draws the original without the filter or the resting layers; Done is unaffected. After a process
   kill, Result shows "Nothing to show" and Edit reopens with no changes.
8. **RGB, end to end** (plan `docs/plans/rgb-tool.md`, owner's decisions of 2026-11-03).
   - *Screen and ViewModel.* RGB is the first tool on the row's second layer, reached by tapping Colour
     (`EditTool.RGB`, icon `ic_instant_mix`, name "RGB"; Colour itself has the palette icon `ic_palette`). Its sheet (`EditPanel.kt`'s
     `EditTool.RGB` branch) shows `editpanel/RgbChips.kt`, three colour circles with no text,
     Red, Green and Blue: HSL's own `ColourRangeCircle` for the colour range of the same name
     (`RgbChannel.range`, `ui/tool/model/RgbChannel.kt`), so the three colours, the ring and the
     check mark match the HSL sheet, and a screen reader hears each circle's name; over ONE
     `LabelledSlider` for the chosen channel. Its track and thumb are tinted in that channel's
     colour, a deep shade on a light theme and a pale one on a dark theme
     (`RgbChannel.tintOn`, at least 3:1 against the sheet). The chosen channel is
     `ToolUiState.rgbChannel`, Red at first, set by `ToolViewModel.chooseRgbChannel`; choosing one
     never changes the photo. There is no Opacity slider and no reset button.
   - *Settings and Undo.* `EditSettings.red`, `green` and `blue`, each -1..1 (shown -100..100, 0 =
     unchanged). A drag calls `previewSettings` as it moves and `commitSettings` when let go, so
     each drag is one Undo step and Redo brings it back. The row's dot shows while any channel is
     away from 0 (`EditSettings.isNeutral(EditTool.RGB)`); Back with only an RGB change asks to
     discard it.
   - *The step.* `EditColourMatrix.of` multiplies each channel by `1 + value` (x0 at -100, x1 at 0,
     x2 at +100) with no offset, so black stays black. It runs inside the one colour matrix, in
     this order: contrast, saturation, warmth, **RGB**, brightness. That puts it after the filter,
     so RGB fine-tunes the filtered look, and before brightness, so blacks that brightness lifts
     stay a neutral grey.
   - *Preview, Done, output.* The live preview hands that matrix to the GPU
     (`LivePreviewImage`), so the photo follows the finger with no CPU work. Done runs it in
     `EditProcessor` on `Dispatchers.Default` with the rest of the edit; each channel is rounded and
     clamped to 0..255. The output is the Edit output: a JPEG at quality 100, at most 3000 px.
     Compare shows the original without RGB. A Done render that fails is the Edit failure above;
     RGB adds no failure of its own.
9. **HSL, end to end** (the second tool on the Colour layer, `EditTool.HSL`, icon `ic_invert_colors`, name "HSL",
   between RGB and Curves).
   - *Screen.* The sheet (`EditPanel.kt`'s `EditTool.HSL` branch) is
     `editpanel/hslpanel/HslPanel.kt`: three rows of 32 dp with 20 dp gaps (136 dp), well inside the
     188 dp the 220 dp sheet leaves, so nothing scrolls. Row 1 is nine circles
     (`hslpanel/colourrangerow/ColourRangeRow.kt` and `ColourRangeCircle.kt`) filled with their
     colours, in order 40 degrees apart: Red 0, Orange 40, Light green 80, Green 120, Light blue
     160, Blue 200, Navy blue 240, Purple 280, Pink 320 (`data/edit/edit/ColourRange.kt`); the chosen
     one carries a ring and a check mark taken from the theme's colours (`markColours`, whichever
     of `onSurface` and `surface` stands out more from the circle). Row 2 is three chips, Hue,
     Saturation and Lightness (`hslpanel/HslChips.kt`). Row 3 is one slider
     (`hslpanel/HslSlider.kt`) from -100 to +100 whose track is a colour gradient of the chosen
     range and channel. Every circle, chip and the slider is 32 dp in the layout and takes touches
     in a 48 dp area. There is no reset button.
   - *ViewModel.* The chosen range and chip are `ToolUiState.hslRange` (Red at first) and
     `hslChannel` (Hue at first), set by `ToolViewModel.chooseHslRange` and `chooseHslChannel`;
     choosing changes no setting and makes no Undo step, and the choice is kept while the sheet is
     closed and opened again. A drag of the slider calls `previewSettings` while it moves and
     `commitSettings` when let go: one Undo step, which Redo brings back.
   - *Settings.* `EditSettings.hsl` (`data/edit/edit/HslAdjustments.kt`): 27 values, hue,
     saturation and lightness for each of the nine ranges, each -1..1 (shown -100..100, 0 =
     unchanged). The row's dot shows while any of the 27 is away from 0
     (`EditSettings.isNeutral(EditTool.HSL)`, `hasHslChange`) and no other tool's dot lights up for
     it. Back with only an HSL change asks to discard it. Compare shows the original without it.
   - *The step* (`data/edit/edit/EditHsl.kt`, plain Kotlin on ARGB ints, on the CPU). For each
     pixel: convert to hue, saturation and lightness; take the amounts of the ranges the hue
     falls between, each range weighing 1 at its centre and falling as a cosine squared to 0 at
     the next centre, so neighbours sum to 1 and nothing bands; a "colourfulness" gate (a smoothstep
     of the spread between the largest and smallest channel, from 0.03 to 0.25) multiplies the
     change, so greys and near-greys are left alone; then hue turns by up to +-20 degrees, saturation
     is multiplied by 1 + amount (kept within 0..1), and lightness moves toward 0.95 (amount above 0)
     or toward 0.05 (amount below 0), by that amount of the way; and the pixel is converted back with
     its alpha kept. The amounts for 3600 hue steps (0.1 degree) are worked out once per call. With
     all 27 values at 0 the step is skipped and no pixel changes.
   - *Live preview.* `FilterPreviewRenderer` renders the filter, then HSL, with the real
     `EditProcessor`, on the copy of the original of at most 640 px, on `Dispatchers.Default`;
     it also does this when no filter is chosen, so a drag of the HSL slider changes the photo on
     screen while the finger moves. `LivePreviewImage` then puts the Adjust and RGB colour matrix
     on top on the GPU. So HSL comes before the colour matrix in the preview as in Done.
   - *Done and output.* `EditProcessor.render`, on `Dispatchers.Default`, in this order: layers,
     crop, quarter turns, flips, filter, **HSL** (`EditHsl.apply`, the same function as the
     preview), colour matrix, every pixel made opaque. HSL therefore colours the layers and the
     photo together, but not a layer being dragged under a finger in the preview (see 3.2, item 9).
     The output is the Edit output: a JPEG at quality 100, at most 3000 px, saved from Result into
     `Pictures/Kanso`. A Done render that fails is the Edit failure above; HSL adds no failure of
     its own.
10. **Curves, end to end** (the third tool on the Colour layer, `EditTool.CURVES`, icon `ic_show_chart`, name
    "Curves", after HSL). State: partial, see the table in section 2.
    - *Screen.* The sheet (`EditPanel.kt`'s `EditTool.CURVES` branch) is
      `editpanel/curvespanel/CurvesPanel.kt`: a 48 dp row holding only the four chips
      (`CurveChips.kt`, each with a colour dot from `CurveChannelDot.kt`), and under it a row with the
      graph (`CurveGraph.kt`) filling the rest of the 188 dp the sheet leaves and a column of buttons
      to its right: the trash (delete point) button at the top and
      under it the Reset icon button (`CurveResetButton.kt`, 48 dp).
      Reset is not in the chips' row because the chips and Reset did not fit side by side (on a phone
      a chip's name wrapped and Reset was pushed off the sheet). The chips are short labels
      (RGB, R, G, B); the four entries share the row's width equally and the row never scrolls
      (`CurveChipRowTest` checks 411 dp and 360 dp, the graph's width to height ratio between 1.8 and 2.6, and the spoken names
      RGB, Red, Green, Blue). RGB's dot sweeps three colours. The graph is wide (about 2:1) and draws only a frame
      and three vertical lines, then the curve and a dot on each point: no diagonal guide, no horizontal lines.
      The chips pick the curve the graph edits (`CurveChannel`); the line is the theme's `onSurface` for RGB and
      the RGB sheet's tints (`RgbChannel.tintOn`) for R, G and B (`CurveLineContrastTest` checks 3:1 against the graph's
      background for five seeds, light and dark). A tap within 24 dp of a point selects it (a point wins over the line); a tap within 24 dp of the drawn line adds a point exactly
      on the line, at the line's own height, and selects it (one Undo step); a tap elsewhere adds nothing and only clears the selection.
      A drag that starts on a point selects and moves it; a drag that starts on the line, away from any point, adds a point there and drags it
      (the first point slides from 0 up to 0.02 short of the next point, the last mirrors it, and no point passes its neighbours, which stay
      at least 0.02 apart); one Undo step on lift. At most 10 points: at ten, a tap on the line adds nothing.
      One point at most is selected: it is drawn filled with the background colour inside a thicker ring of the line colour, the others filled with the line colour inside a
      background-colour ring. The selection clears on a channel switch, Undo, Redo and Reset; it lives in `CurvesPanel` (`rememberSaveable`), not in the ViewModel or `ToolUiState`.
      A trash icon button (`curvespanel/CurveDeleteButton.kt`, spoken name "Delete point") sits above Reset in the column beside the graph, both at least 48 dp; it deletes the selected point
      (end points too) in one Undo step, then selects the left neighbour (or the new first point if the first was deleted); it is disabled with nothing selected or with two points left.
      Left of the first point and right of the last the line is flat at that point's height. A vertical drag that starts on a point is claimed
      by the graph, so the sheet does not take it. Reset straightens only the chosen curve and is disabled while that curve is straight, and
      the graph ignores touches while Compare is on or Done is rendering (while disabled it also stops moving: it shows the saved curve, not a half-made drag). A screen reader hears the
      graph as "<Channel> curve, N points", the chips and Reset; it cannot move points (no TalkBack point editing).
    - *ViewModel.* `ToolUiState.curveChannel` (RGB at first) is the chosen chip; `ToolViewModel.chooseCurveChannel` sets it, it is kept so reopening Curves shows the last curve used, and it is not an Undo step and changes no setting. The graph reports a drag through
      `previewSettings` while the finger moves and `commitSettings` when it lifts, so one drag is one
      Undo step; adding a point by a tap, a delete and Reset are one commit each (selecting alone commits nothing). Redo brings each back.
    - *Settings.* `EditSettings.curves` (`data/edit/edit/CurveAdjustments.kt`): four `ToneCurve`s
      (rgb, red, green, blue), each from 2 to 10 `CurvePoint`s (x and y in 0..1), sorted and at least
      0.02 apart; the end points are free and need not sit at x = 0 and x = 1, checked when one is made (`ToneCurve.kt`). The row's dot shows while
      any curve is bent (`hasCurveChange`, `EditSettings.isNeutral(EditTool.CURVES)`); Back with only
      a curve change asks to discard it; Compare shows the original without it.
    - *The step* (`data/edit/edit/EditCurves.kt`, plain Kotlin on ARGB ints, on the CPU). Each curve
      becomes a 256-entry table, `round(255 x valueAt(i / 255))`, where `valueAt` is a natural cubic
      spline (zero second derivative at both ends): it passes through every point, may overshoot between
      points, is clipped to 0..1 (pure black or white), and is flat beyond the first and last point. A pixel's red, green and blue each go
      through their own channel's table and then through the RGB (master) table; alpha is kept. That is the
      whole step: a loop of table reads over the pixels, with no colour-space conversion and no
      other pass. When every curve is straight the step is skipped and no pixel changes.
    - *Live preview.* `FilterPreviewRenderer` and `ToolViewModel`'s preview request carry the
      filter, the HSL values, the curves and the layers (`PreviewKey.curves`); a curve change alone
      asks for a preview render, so with no filter chosen the photo still follows the finger. The
      render runs the real `EditProcessor` on the 640 px copy in the order filter, HSL, curves, and
      Compose puts the colour matrix on top, the order Done uses, so the two agree (`CurveParityTest`).
      A sticker under a finger is drawn without this CPU stage (curves included) until it is let go.
    - *Done and output.* `EditProcessor.render`, on `Dispatchers.Default`, in this order: layers,
      crop, quarter turns, flips, filter, HSL, **curves** (`EditCurves.apply`, only when a curve is
      bent), colour matrix, every pixel made opaque. The output is the Edit output: a JPEG at quality
      100, at most 3000 px, saved from Result into `Pictures/Kanso`. Cancellation is checked after the
      step. A Done render that fails is the Edit failure above; Curves adds no failure of its own.
    - *Decision records:* [ADR-009](docs/adr/ADR-009-curves-run-on-the-cpu-after-hsl.md) (where the step runs),
      [ADR-011](docs/adr/ADR-011-curves-are-a-natural-spline-with-free-end-points.md) (the spline and free end points).

### 3.2 Layers: what stickers, text and dates share

A **layer** is anything placed on the photo: a catalogue sticker, a typed text or a date. All three
kinds share one model, one list and every piece of machinery below; only how their pixels are made
differs ([ADR-005](docs/adr/ADR-005-stickers-and-text-are-layers.md),
[ADR-006](docs/adr/ADR-006-a-date-is-its-own-layer-kind.md)).

1. **The model.** `data/edit/edit/Layer.kt`: a key, a `LayerContent`, a centre, a width, a turn in
   degrees, a mirror flag and an opacity (default 1, never below `Layer.MIN_OPACITY` = 0.1). The
   centre and width are fractions of the **original** photo, before any crop, turn or flip, so a
   layer belongs to the photo. `LayerContent` (`data/edit/edit/LayerContent.kt`) is a sealed
   interface with three kinds: `Picture(pictureId)`, `Text(text, colorArgb, font, alignment,
   wrapEms)` and `Date(moment, format, colorArgb, font)`. `EditSettings.layers` holds them oldest
   first, at most `EditSettings.MAX_LAYERS` = 20 in total; a 21st add shows "A photo can hold 20
   stickers, texts and dates at most" for 2.5 s
   (`ui/tool/component/StickerLimitMessage.kt`).
2. **One source of pixels.** Everything that draws a layer asks `LayerPictures.picture(content,
   widthPx)` (`data/edit/edit/LayerPictures.kt`) for a bitmap about that wide. The app's
   implementation, `data/edit/edit/AppLayerPictures.kt`, decodes a sticker's drawable with
   `BitmapFactory` and `inSampleSize`, or draws a text with `TextLayerLayout` (see 3.4); a date is
   drawn as the one-line text of its words (see 3.5). The one
   recipe from settings to processor is `editProcessorFor(context)` in
   `ui/tool/model/EditProcessorFactory.kt`, shared by the sheets, the preview renderer, the cropper
   and Done.
3. **On the photo.** `ui/tool/component/photocard/PhotoCard.kt` lays a `LayerOverlay`
   (`photocard/layeroverlay/LayerOverlay.kt`) over the live picture. It draws no pixels of its own
   (they come from the preview), only one invisible node per layer at its place (`LayerPlace.kt`),
   the selected layer's frame, and, for a text with no letters yet, a faint "Your text". Its shape
   per layer (height over width) comes from `rememberLayerAspect` (`LayerImage.kt`). The mapping
   between the shown picture and the original's fractions is `data/edit/edit/LayerMapping.kt`,
   which knows the crop, turns and flips; the geometry is plain functions in `LayerGeometry.kt`.
4. **Touch.** One handler on the whole overlay (`LayerGestures.kt`, raw pointer input on the main
   thread) finds the topmost layer under the finger on its turned rectangle. Layers answer touches
   with any sheet open except Crop (the cropper replaces the photo then), or with none, while
   Compare is off and Done is idle. Touching a layer selects it (`ToolViewModel.selectLayer`),
   **brings it to the front** of the list, and opens the sheet of its kind if that sheet is not
   open; the sheet it leaves keeps its changes, like its ✓. One finger drags; two fingers scale by
   the ratio of their distance and turn by the change of their angle (the width kept between 5 and
   100 percent of the shown width). Each move calls `previewLayers` (shown at once, no undo entry);
   the last finger up calls `commitSettings`, so a whole gesture, the move to the front included,
   is one Undo step. A one-finger touch that stays within the touch slop on the layer that was
   **already** selected is a tap (`onTap`, then `ToolViewModel.tapLayer`), but only on a date
   (`takesTaps`), which moves to its next format; on a sticker or a text a small move stays a small
   move. The few pixels the finger wobbled are put back first (the place only, so a flip made
   meanwhile is kept), so the tap leaves no move behind.
5. **The frame.** `LayerFrame.kt` draws an outline that turns with the layer and four corner
   buttons (`LayerCornerButton.kt`), in the same places on every kind: top-left **✕ delete**,
   bottom-left **⧉ duplicate** (the copy sits 4 percent of the shown width right and down, and is
   selected), bottom-right **⤡ resize and turn** (a whole pull is one Undo step), and top-right the
   kind's own button: **⇋ mirror** for a sticker, **✏️ edit** (open the keyboard) for a text, and
   nothing for a date (mirrored digits cannot be read; a touch there grasps the date itself).
6. **Tabs and sliders.** Every layer sheet has a **Size** tab (`editpanel/LayerSizeSlider.kt`,
   1–100) and an **Opacity** tab (`editpanel/LayerOpacitySlider.kt`, 10–100%). A tab that styles a
   layer is greyed out while nothing is selected, and the sheet then shows its first tab; the chosen
   tab stays while the sheet is open, also when another layer is selected (`ui/tool/model/StickersTab.kt`,
   `TextTab.kt`, `DateTab.kt`). `data/edit/edit/LayerSize.kt` turns the size number into a width: for a sticker it is
   the width in percent of the photo's width; for a text or a date it is the letter height, from 1
   percent (size 1) to 50 percent (size 100) of the photo's height. A pinch changes the same width, so the
   slider follows the fingers. A slider drag is previewed and becomes one Undo step on release.
7. **The sheet's ✕ and ✓.** Opening a layer sheet takes a snapshot of the whole session (settings
   and Undo steps). ✓ (`confirmLayers`), closing the sheet any other way, or switching to another
   layer sheet keeps everything. ✕ and the system Back (`cancelLayers`) put the snapshot back.
8. **Drawing into the photo.** `EditProcessor.drawLayers` runs first in Done's render. For each
   layer it asks for its picture at no more than the photo's longest edge, then draws it with a
   `Matrix` (scale, mirror, turn, move) into a bitmap only as big as the visible part of the layer,
   with the layer's opacity as the paint's alpha, and blends that into the cropped pixels one row at
   a time. So the crop cuts a layer, the turns and flips carry it, and the filter, the HSL tool and
   the sliders colour it (HSL runs after the layers are drawn in, so it colours them with the photo). A layer whose picture cannot be made, or whose numbers are not finite, is skipped without an
   error.
9. **The cost, accepted in ADR-004.** While a finger moves a layer, that layer is drawn by
   `LivePreviewImage` itself (with the sliders and its opacity, but neither the filter nor HSL) so it can follow the
   finger; it joins the filtered and HSL-coloured copy when the finger lifts. The preview and Done can differ
   slightly in detail (640 px against up to 3000 px).

### 3.3 Stickers

1. **Screen.** Tapping Stickers (`ToolViewModel.openTool`) opens its sheet in place of the row.
   `ToolScreen.kt` fills it with `EditPanel`, which draws
   `ui/tool/component/toolpanel/editpanel/stickerspanel/StickersPanel.kt`: the pinned top bar
   (`editpanel/LayerSheetTopBar.kt`), the tabs **Stickers | Size | Opacity**, and for Stickers a row
   of category chips (`StickerCategoryChips.kt`) over a three-column `LazyVerticalGrid` of tiles
   (`StickerTile.kt`) that fills the rest of the sheet and scrolls.
2. **The catalogue.** `data/edit/edit/StickerCatalog.kt` lists 60 bundled pictures, each an explicit
   `R.drawable.img_sticker_*` webp: Food 17, Mood 17, Quote 6, Emoji 20. Nothing is downloaded.
3. **Adding.** A tile tap calls `ToolViewModel.addSticker` on the main thread: `LayerMapping`
   turns the middle of the picture you see into fractions of the original, and
   `EditSession.addLayer` puts a `Picture` layer there (a third of the shown width, upright), on
   top, selected: one Undo step. Taps are ignored while Compare is on or Done renders.
4. **Mirror.** The frame's top-right button calls `flipLayer`: one Undo step.
5. **Input and output.** Input: the original photo shrunk to at most 3000 px and the
   `Picture` layers (picture id, centre, width, turn, mirror, opacity). Each picture is decoded at no
   more than the photo's longest edge (`AppLayerPictures`, nothing cached). Output: the saved JPEG at
   quality 100 in `Pictures/Kanso`.

### 3.4 Text

1. **Screen.** Text is the seventh entry of the first row (`EditTool.TEXT`, icon `ic_text_fields`). Tapping
   it opens the Text sheet in place of the row; `ToolScreen.kt` fills it with
   `ui/tool/component/toolpanel/editpanel/textpanel/TextPanel.kt`: the pinned top bar (✕, "Text",
   ✓), the tabs **Text | Font | Size | Colour | Background | Opacity**, and the chosen tab's control:
   the text area with its alignment button (`TextInputRow.kt`), the font chips (`TextFontRow.kt`),
   the size slider, the letters' colours (`TextColourRow.kt`), the box's colours
   (`TextBackgroundRow.kt`) or the opacity slider. It opens on the Text tab; the keyboard always
   brings the Text tab back.
2. **A new text.** Opening the sheet from the row with no text selected calls
   `ToolViewModel.startText`: a new, empty `Text` layer in the middle of the picture you see, white,
   in the phone's font, centred, at size 20, selected, with the keyboard asked for
   (`ToolUiState.textEditing`). It is only *previewed*: until it has letters it is not an action.
   Typing with no text selected starts one the same way. At 20 layers nothing is added and the limit
   message shows.
3. **Typing.** The text area keeps its own `TextFieldValue` (so the cursor never jumps) and hands
   each new string to `typeText`, which cuts it at 200 characters (`LayerContent.Text.MAX_LENGTH`),
   keeps the letter size (the width scales with the measured block's width) and previews it. While
   the keyboard is up, the sheet shows only its top bar and the text area; the tabs come back when it
   goes down. Enter makes a new line.
4. **One undo step per typing run.** The run is settled (`settleText`) when the keyboard closes,
   another layer is touched, the sheet closes or switches, or Undo is tapped: the adding and all
   the letters become **one** Undo step. A text that is still empty (no letters, or only spaces) is
   taken off the photo instead, leaving no Undo step.
5. **Fonts.** "None" is the phone's default typeface (no file). Five fonts are bundled in
   `app/src/main/res/font/` (about 772 KB in total), each checked against its character map to draw
   every Vietnamese letter (ă, â, đ, ê, ô, ơ, ư with all five tone marks): Be Vietnam Pro Bold,
   Playfair Display, Dancing Script Bold, Patrick Hand and Space Mono (`data/edit/edit/TextFont.kt`).
   Each chip shows "Aa" in its own font. A chip is one Undo step and keeps the letter size.
6. **Colours.** Twelve ready circles (`ui/tool/model/TextSwatch.kt`: white, black, red, orange,
   yellow, green, teal, blue, purple, pink, brown, grey), one Undo step each, and a rainbow circle
   that swaps the tabs and the control for `TextColourPane.kt` (the top bar stays): the same HSV
   picker as Settings → Theme colour (`ui/component/CustomColourPicker.kt`, from `core/colorpicker`,
   transparency bar hidden). The text changes colour live while you drag; the pane's back arrow makes
   the whole drag one Undo step. A colour is stored as an opaque ARGB number.
   **Background.** The Background tab (`TextBackgroundRow.kt`) offers a slashed **None** (no box,
   the default), the same twelve colours as solid boxes, and a rainbow circle opening the picker
   **with** its transparency bar, so a box can be any colour at any see-through strength
   (`LayerContent.Text.boxArgb`, alpha kept; `ToolViewModel.chooseTextBackground`). Each choice is one
   Undo step, a picker drag one step on its back arrow. The letters keep their own colour. A box
   widens the block (margins of 0.4 letter heights across and 0.2 down instead of 0.15), so the
   layer grows while its letters keep their size; `TextLayerLayout` fills the block with a rounded
   box, corners 0.3 letter heights, behind the lines.
7. **Alignment.** The button beside the text area cycles centre → left → right
   (`TextAlignment.next()`), one Undo step each.
8. **Layout and drawing.** `data/edit/edit/TextLayerLayout.kt` lays the words out with Android's
   `StaticLayout` at a fixed reference letter height (100 px) and the text's wrap width, then scales
   the result to the width asked for, with a margin of 0.15 letter heights so swashes and accents are
   never cut. The wrap width (`wrapEms`) is set once, when the text is added, so that at size 20 a
   line wraps at 80 percent of the photo's width (`LayerSize.textWrapEms`); because it is counted in
   letter heights, a bigger or smaller text keeps the same lines and the same shape, and a pinch
   scales it like a picture. The alignment never changes the block's size. A render never exceeds
   about 4 million pixels (a larger one is drawn smaller and stretched by the caller) and returns
   nothing on running out of memory. Measurements are cached (32 entries); typefaces are loaded once
   with `ResourcesCompat.getFont`, falling back to the default typeface if a font cannot be loaded.
9. **Threads.** Measuring runs on the main thread (ViewModel and overlay); the text's pixels are made
   on `Dispatchers.Default` by the preview renderer and Done, and on the main thread for the text
   under a finger (`PhotoCard.kt`, 1024 px wide) and the cropper's bake.
10. **Input and output.** Input: the words, colour, font, alignment, wrap width and box colour of
    each `Text` layer, plus the layer's place, width, turn and opacity. Output: the letters drawn into the saved
    JPEG at quality 100 in `Pictures/Kanso`, filtered and coloured with the photo like any layer. The
    "Your text" placeholder is drawn only by the overlay and never reaches the saved photo.

### 3.5 Date

1. **Screen.** Date is the ninth entry of the row (`EditTool.DATE`, icon `ic_calendar_today`).
   Tapping it (`ToolViewModel.openTool`) sets `ToolUiState.dateMoment` to now, to the minute, and
   opens the Date sheet in place of the row; it adds nothing yet. `ToolScreen.kt` fills the sheet
   with `ui/tool/component/toolpanel/editpanel/datepanel/DatePanel.kt`: the pinned top bar (✕,
   "Date", ✓), the tabs **Format | Day & time | Font | Size | Colour | Background | Opacity**, and the
   chosen tab's control: the 20 format chips (`DateFormatRow.kt`), each written with the moment
   shown; the day and time buttons (`datemomentrow/DateMomentRow.kt`); the Text sheet's own font,
   colour and background rows (with their rainbow pickers); or a slider. It opens on Format; Format
   and Day & time work with no date selected.
2. **The model.** A `LayerContent.Date` keeps the **moment** (`java.time.LocalDateTime`: a date and
   a time to the minute, no time zone) and the **format**, never the words: `Date.words` writes
   them from the two each time the layer is drawn. Its letters' colour is an opaque ARGB number,
   its font one of the six `TextFont`s, and its box (`boxArgb`) a colour with its alpha or none, as
   for a text (see 3.4). There is no alignment (one line) and no mirror.
3. **The formats.** `data/edit/edit/DateFormat.kt`, an enum in the chips' order, always in English
   (`Locale.ENGLISH`): `Wed, 28 October 2026`, `Wednesday, 28th October`, `28 October 2026`,
   `October 28, 2026`, `28 Oct 2026`, `28 Oct`, `October 2026`, `Wednesday`, `2026`, `28/10/2026`,
   `28.10.26`, `2026-10-28`, `WED 28 OCT`, the film-camera stamp `'26 10 28`, then six with a time:
   `14:30`, `2:30 PM`, `Wed, 28 Oct · 14:30`, `Wednesday · 2:30 PM`, `28/10/2026 14:30`,
   `28 October 2026, 2:30 PM`. Each is a `DateTimeFormatter` pattern, except the ordinal one
   (`28th`), written by hand.
4. **Adding.** A chip tap with no date selected calls `ToolViewModel.chooseDateFormat`, which adds
   (`addDate`) a `Date` layer with the sheet's moment and that format in the middle of the picture
   you see: white, the phone's font, size 20, upright, selected, on top. One Undo step. At 20 layers
   nothing is added and the limit message shows. Ignored while Compare is on or Done renders.
5. **Changing a date.** With a date selected: a chip rewrites it in that format; the day picker
   (Material `DatePickerDialog`, `datemomentrow/DateDayDialog.kt`, the time kept) and the time
   picker (Material `TimePicker` in an `AlertDialog`, `datemomentrow/DateTimeDialog.kt`, the day
   kept; 24-hour or 12-hour as the phone's clock is set) change its moment; a font chip or a ready
   colour or a background (`chooseDateBackground`) restyles it. Each is one Undo step, and the width
   follows the new words so the letters keep their size (`editSelectedDate`). A colour dragged in a
   picker is previewed and becomes one Undo step when the pane's back arrow is tapped. With no date
   selected, the pickers change only the moment the next date gets (`chooseDateMoment`), and the
   styling tabs are greyed out.
6. **Tap to cycle.** A tap on the selected date (see 3.2, touch) calls `tapLayer`, which moves it to
   the next format, the first after the last: one Undo step per tap.
7. **Layout and drawing.** `Date.asText()` turns the date into a one-line `Text` (its words,
   colour, font and box, centred, with a wrap width of 1000 letter heights so no format ever wraps),
   and every
   measurement and drawing goes through `TextLayerLayout` exactly as for a text (see 3.4): the
   overlay's shape (`LayerImage.kt`), the size slider (`LayerSize`), and the pixels from
   `AppLayerPictures` for the preview renderer, the cropper and Done.
8. **Threads.** As for a text: measuring on the main thread, the pixels on `Dispatchers.Default` for
   the preview renderer and Done, on the main thread for the date under a finger and the cropper's
   bake. Writing the words is plain Kotlin and cheap.
9. **Input and output.** Input: the moment, format, colour, font and box of each `Date` layer, plus the
   layer's place, width, turn and opacity; the time comes from the phone's clock only when the sheet
   opens. Output: the words drawn into the saved JPEG at quality 100 in `Pictures/Kanso`, filtered and
   coloured with the photo like any layer. Nothing is read from the photo's own date (EXIF).

### 3.6 Remove background, Enhance, Beautify and Artistic

These four share one path in `ui/tool/ToolViewModel.kt`: `apply` shrinks the photo to the tool's
working size (`data/edit/WorkingSize.kt`: Remove background and Beautify 2000 px, Enhance 512 px,
Artistic 1024 px) and runs its `ToolProcessor` on `Dispatchers.Default`; the result becomes the
shown photo. Remove background and Enhance run on the photo as shown; Beautify and Artistic always run
from the original with the chosen strength (and style), so their results never stack, and a repeat
run replaces the last result without an Undo step of its own. A failure shows a message on the photo
card and leaves the photo as it was. Done hands the shown photo to Result at once.

| Tool | Processor | Engine wrapper | Details |
|---|---|---|---|
| Remove background | `data/edit/cutout/CutoutProcessor.kt` | `data/edit/cutout/EngineSegmenter.kt` | [page](docs/features/remove-background.md) |
| Enhance | `data/edit/enhance/EnhanceProcessor.kt` | `EngineUpscaler.kt`, `AutoLighting.kt` | [page](docs/features/enhance.md) |
| Beautify | `data/edit/beautify/BeautifyProcessor.kt` | `LandmarkFaceFinder.kt`, `EngineSkinSmoother.kt`, `data/edit/face/EngineLandmarkFinder.kt`, `data/edit/body/EngineBodyFinder.kt` | [page](docs/features/beautify.md), [face shape](docs/features/face-shape.md) |
| Artistic | `data/edit/artistic/ArtisticProcessor.kt` | `EngineStylizer.kt` | [page](docs/features/artistic.md) |

---

## 4. Architecture

Paths are under `app/src/main/java/com/phongkaster/kanso/`.

- **One activity** (`MainActivity`), drawn entirely with Jetpack Compose, in a task of its own. The
  launcher's entries open `LauncherActivity`, which starts `MainActivity` and closes, so switching
  the icon colour never closes the app. `KansoApplication` starts Koin with `di/AppModule.kt`.
- **Navigation 3** (`navigation/KansoNavHost.kt`): one saved back stack of `@Serializable` keys:
  `Home`, `MyEdits`, `Settings` (the tabs), `ThemeModeSettings`, `ThemeColourSettings`,
  `TextSizeSettings`, `Gallery(tool)`, `Camera(tool)`, `ToolPhoto(tool, uri)`, `Result` and
  `EditedPhoto(uri)`. Each entry gets its own `ViewModelStore`.
- **State.** Every screen has one ViewModel exposing one immutable `UiState` as a `StateFlow`;
  one-off events use a `Channel`. Each screen is a stateful composable plus a stateless `…Content`
  composable with light and dark previews. A screen's package holds the screen file, its ViewModel
  and UiState, a `component/` folder for every composable it draws, and a `model/` folder for its
  own types.
- **Editing.** Every tool runs through one interface, `data/edit/ToolProcessor.kt`. Each engine call
  sits in one wrapper file (`Engine*.kt`) behind a small interface, so everything else is tested on
  the computer with fakes.
- **Data.** `PhotoSource` and `SavedPhotoSource` (MediaStore reads), `PhotoLoader` (decode),
  `PhotoSaver` (write), `RenderedPhotoStore` (the hand-off from Done to Result) and
  `SettingsRepository` (Preferences DataStore).
- **UI.** Jetpack Compose with Material 3 1.5 (Expressive components and motion), Coil for
  thumbnails, MaterialKolor for colour schemes from a seed colour.
- **SDK levels.** `minSdk 27`, `targetSdk 36`, `compileSdk 37`. Debug builds keep the `x86_64`,
  `arm64-v8a` and `armeabi-v7a` ABIs (the owner's phone is 32-bit).

### The image engine: what each tool calls

| Tool | Wrapper in `app/` | Engine class | Model or library (all bundled) |
|---|---|---|---|
| Edit (filters) | `data/edit/edit/FilterLut.kt` (reads images only) | none | `lookup/lookup_light.png` (`libs/imageprocessor.aar`), six curve strips in `filter_materials/map/` (`libs/editimage.aar`) and Kanso's own colour cube `filters/autumn.png` (`app/src/main/assets/`) |
| Edit (crop) | `ui/tool/component/EditCropper.kt` | `CropperPreview`, `CropState` (`core/imagecrop`) | none |
| Edit (text) | `data/edit/edit/TextLayerLayout.kt` | Android `StaticLayout` | five OFL fonts in `app/src/main/res/font/` |
| Remove background | `data/edit/cutout/EngineSegmenter.kt` | `SelfieSegmentationUC`, `OpenCVLoader` | ML Kit `segmentation-selfie` |
| Enhance | `data/edit/enhance/EngineUpscaler.kt` | `EnhanceImageUC` | `realesrgan_64x64_float16.tflite` |
| Beautify | `data/edit/face/EngineLandmarkFinder.kt`, `data/edit/body/EngineBodyFinder.kt`, `data/edit/beautify/EngineSkinSmoother.kt` | MediaPipe `FaceLandmarker` (one, for width, lift, zoom and the smoothing boxes), MediaPipe `PoseLandmarker` (Waist, Arms, Chest and Legs, with its segmentation mask), OpenCV `Imgproc.bilateralFilter` | `face_landmarker.task`, `pose_landmarker_full.task` (in `app/src/main/assets/`) |
| Artistic | `data/edit/artistic/EngineStylizer.kt` | `StylePredictInceptionV3UC`, `StyleTransferInceptionV3BlendUC` | two Inception v3 stylization `.tflite` models |

Outside editing, `data/media/MediaStorePhotoSource.kt` uses `core/gallery`'s `MediaRepository`, and
`ui/component/CustomColourPicker.kt` wraps `core/colorpicker`'s `HSVColorPicker` (used by Settings
and the Text sheet). `libs/localml.aar` also packs `hifill_inpaint_512_int8.tflite` and
`magic_touch.tflite`, which are never called.

---

## 5. Privacy and permissions

- **No network.** `INTERNET` and `ACCESS_NETWORK_STATE` are removed from the merged manifest. No
  model, filter, sticker or font is downloaded.
- **No ads, no analytics, no accounts.**
- **Photos.** Kanso reads the phone's photos through MediaStore with the photo permission (partial
  access on Android 14+ is handled), writes only new files into `Pictures/Kanso` when you tap Save
  (asking for the storage-write permission on Android 8.1 and 9), and never changes an original.
- **Camera.** Photos taken in Kanso go to its private storage until you keep and save them.
- **Sharing.** A shared picture is a copy in the cache's `shared/` folder, sent with a one-off read
  grant through FileProvider.

---

## 6. Building and testing

Build with Gradle from the repository root:

```bash
./gradlew :app:assembleDebug          # the debug APK
./gradlew :app:testDebugUnitTest      # JVM tests (Robolectric, Compose UI tests)
./gradlew :app:verifyRoborazziDebug   # screenshot tests against the recorded images
./gradlew :app:lintDebug              # Android lint
```

All of them together:

```bash
./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue
```

The tests live in `app/src/test/`: more than 100 test classes and 79 recorded screenshots in
`app/src/test/screenshots/`. They cover the screens, navigation, every tool's processor with fakes,
saving, Share, the camera (through a fake `CameraController`), settings, previews and screenshots,
and need no device. The layer and text work is covered by `StickerFrameTest`, `StickerSelectionTest`,
`EditProcessorStickerTest`, `LayerSizeTest`, `TextToolTest` (the ViewModel's text rules),
`TextLayerLayoutTest` (layout, Vietnamese in every font, a text drawn into the photo) and
`TextSheetTest` (the sheet on the real screen). The `Engine*.kt` wrappers are native code and run
only on a device or an emulator.

---

## 7. Project layout and origins

| Path | What it is |
|---|---|
| `app/` | The Kanso app, written by [Foreman](https://github.com/Phong-Kaster/Foreman), one goal at a time. |
| `core/` | The image engine's modules, imported from Retica at commit `9d227a8`. The app calls `core/gallery`, `core/colorpicker` and `core/localml`, uses one type of `core/common`, reads filter images from `core/editimage` and `core/imageprocessor`, and uses `core/imagecrop`'s cropper. `core/overlaybrush` is built in but not called. |
| `libs/` | Prebuilt engine libraries (AARs) and two TensorFlow Lite AARs; `tensorflow-lite-select-tf-ops.aar` is used by `core/localml`. |
| `opencv/` | The OpenCV Android SDK module the engine depends on. |
| `reference/` | Retica's original app and Pinity's sticker code, **read-only**: never built or imported. The 60 sticker pictures in `app/src/main/res/drawable/` are copies from `reference/pinity-stickers/`. |
| `docs/features/` | One page per feature. |
| `docs/adr/` | The decision records (below). |
| `CONTEXT.md` | The glossary: the words this code uses and what each one means here. |

---

## 8. Decision records

| Record | Decision |
|---|---|
| [ADR-001](docs/adr/ADR-001-kanso-is-a-material-3-expressive-app-blue-by-default-with-one-editor.md) | Kanso is a Material 3 Expressive app, blue by default, with one editor. |
| [ADR-002](docs/adr/ADR-002-kanso-has-one-screen-per-tool-and-a-floating-tab-bar.md) | One screen per tool and a floating tab bar. |
| [ADR-003](docs/adr/ADR-003-edit-has-one-session-and-done-opens-a-result-screen.md) | Edit has one session; Done opens a Result screen. |
| [ADR-004](docs/adr/ADR-004-sticker-is-part-of-the-photo.md) | A sticker is part of the photo: it is drawn first, so everything else acts on it. |
| [ADR-005](docs/adr/ADR-005-stickers-and-text-are-layers.md) | Stickers and text are layers: one list, one gesture handler, two kinds of picture. |
| [ADR-006](docs/adr/ADR-006-a-date-is-its-own-layer-kind.md) | A date is its own layer kind, not a text: it keeps its moment and format, so the pickers and tap-to-cycle work on it. |
| [ADR-007](docs/adr/ADR-007-body-reshapes-use-the-mediapipe-pose-model-and-its-mask.md) | Body reshapes (Waist first) use MediaPipe's bundled pose model and its person mask, warped with the faces in one pass, in a Body group of Beautify. |
| [ADR-008](docs/adr/ADR-008-hsl-is-computed-on-the-cpu-by-one-formula.md) | HSL is computed on the CPU by one formula (`EditHsl`) for the preview and for Done; no shader, no look-up table. |
| [ADR-009](docs/adr/ADR-009-curves-run-on-the-cpu-after-hsl.md) | Curves run on the CPU (`EditCurves`) after HSL and before the colour matrix, through 256-entry tables. |
| [ADR-011](docs/adr/ADR-011-curves-are-a-natural-spline-with-free-end-points.md) | A curve is a natural cubic spline that may overshoot (clipped to 0..1), with free end points; supersedes ADR-009's interpolation and end-point decisions. |

---

## 9. Third-party licences

The five bundled fonts are under the SIL Open Font License 1.1; each licence text ships in
`app/src/main/assets/licenses/fonts/`:

| Font | File | Licence |
|---|---|---|
| Be Vietnam Pro Bold | `res/font/be_vietnam_pro_bold.ttf` | `be_vietnam_pro_bold-OFL.txt` |
| Playfair Display Regular | `res/font/playfair_display_regular.ttf` | `playfair_display_regular-OFL.txt` |
| Dancing Script Bold | `res/font/dancing_script.ttf` | `dancing_script-OFL.txt` |
| Patrick Hand Regular | `res/font/patrick_hand_regular.ttf` | `patrick_hand_regular-OFL.txt` |
| Space Mono Regular | `res/font/space_mono_regular.ttf` | `space_mono_regular-OFL.txt` |

The pose model `app/src/main/assets/pose_landmarker_full.task` is Google's MediaPipe Pose
Landmarker (full), downloaded unchanged from MediaPipe's model storage
(`storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/latest/`,
SHA-256 `4eaa5eb7…cdcec4ad`) and published under the Apache License 2.0.

The engine modules, OpenCV and the sticker pictures keep the licences of their own sources.
