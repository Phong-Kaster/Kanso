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

Every tool opens on its own screen with the same frame: Back, the tool's name, Undo and Done at
the top right, the photo in a card, and the tool's controls. Done renders the result and opens the
Result screen. [The tool screen](docs/features/tool-screen.md) explains the shared parts: decoding,
the working size, progress, Undo, failures and Done.

| Tool | What it does | Saved as |
|---|---|---|
| [Edit](docs/features/edit.md) | Seven tools in a row under the photo (Crop, Rotate, Adjust, Filters, Stickers, Text, Date) with Compare pinned at the right. Crop with handles, pinch and seven shapes; quarter turns and flips; brightness, contrast, saturation and warmth; one of seven filters with an intensity; up to 20 **layers** (catalogue stickers, typed texts and dates together), each moved, pinched, turned, resized, faded, duplicated and deleted on the photo. The photo is a live preview; Done renders it once. | JPEG quality 100, at most 3000 px (an untouched photo: its own file) |
| [Remove background](docs/features/remove-background.md) | Cuts the person out with ML Kit's bundled selfie-segmentation model and makes the rest transparent, shown over a checkerboard. People only, in practice. | PNG with transparency, at most 2000 px |
| [Enhance](docs/features/enhance.md) | Shrinks the photo to 512 px, upscales it four times with the bundled Real-ESRGAN model and brightens it if it is dark; hold to compare with the original. | JPEG, at most 2048 px |
| [Beautify](docs/features/beautify.md) | Zooms onto the largest face, then a two-level row of tools like Meitu's (Face, with Width, Lift and a pinned Back; Smooth): Face width (-100..+100) and Face lift (-100..+100, the jowls and jaw dropped or raised and tightened), where MediaPipe's 478 face landmarks drive one Kotlin warp of the cheek and jaw outline, and Smooth (OpenCV's bilateral filter, 0..100). The photo follows the slider live while dragging (a mesh warp on the GPU); letting go renders both at full quality quietly in the background. | JPEG, at most 2000 px |
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
and an edited 2K photo was saved without losing quality; the other features have not been checked
on a phone yet.

| Feature | State | Notes |
|---|---|---|
| Edit: Crop | Works | `core/imagecrop`'s cropper on the original photo: handles, drag, pinch; Free or a locked shape. One finished gesture is one Undo step. |
| Edit: Rotate and flip | Works | Quarter turns left and right, flip across, flip upside down. |
| Edit: Adjust | Works | Brightness, contrast, saturation and warmth: four tiles over one slider for the chosen tile. Drawn live on the GPU as a colour matrix. |
| Edit: Filters | Works | None or one of seven filters with an intensity; a new filter replaces the old one. Previewed on a 640 px copy. |
| Edit: Stickers | Works | 60 bundled pictures in four categories. Each sticker is a **layer** (see below), with a mirror button. |
| Edit: Text | Works | Typed words in one of six fonts (the phone's own and five bundled, all drawing Vietnamese), twelve ready colours or any colour from a picker, an optional background box (twelve colours, or any colour with transparency), three alignments, wrapping lines. Each text is a **layer**. |
| Edit: Date | Works | A date and time written in one of 20 English formats (`Wed, 28 October 2026`, `2026`, `'26 10 28`, `14:30` …), starting at now, changed with a day picker and a time picker; the Text sheet's fonts, colours and background box. A tap on the selected date moves it to the next format. Each date is a **layer** ([ADR-006](docs/adr/ADR-006-a-date-is-its-own-layer-kind.md)). |
| Edit: Layers (stickers, text and dates) | Works | Up to 20 in total. Select by touch (from any sheet but Crop), move with one finger, pinch and twist with two, resize and turn with the corner handle; Size (1–100) and Opacity (10–100%) tabs in all three layer sheets; a selected layer comes to the front. Drawn into the photo before the crop, so crop, turns, flips, filter and sliders all act on it ([ADR-004](docs/adr/ADR-004-sticker-is-part-of-the-photo.md), [ADR-005](docs/adr/ADR-005-stickers-and-text-are-layers.md)). |
| Edit: Undo and Compare | Works | Undo (top bar) goes back one action at a time, no redo. Compare (pinned in the row) shows the original while it is on and disables every editing control except Done. |
| Remove background | Works | ML Kit selfie segmentation: people only, in practice. |
| Enhance | Works | Real-ESRGAN four-times upscale of a 512 px copy, plus brightening when dark. |
| Beautify | Works | Face width and Face lift (MediaPipe landmarks plus a Kotlin warp) and OpenCV bilateral smoothing, each a slider; zooms onto the face. Width and smoothing seen on the Galaxy A13, 2026-10-28; Lift not yet tried on a phone. |
| Artistic | Works | Four styles from the Inception v3 style-transfer models, with a strength slider. |
| Done and Result | Works | Done hands the picture to Result; Save is the only gallery write. |

**Known limits.**

- The row of tools scrolls sideways. On the owner's Galaxy A13 (checked on the phone, 2026-10-28)
  Crop to Stickers and Compare are in view and Text is hidden past the right edge until the row is
  swiped left; Date, added after Text, sits further right still and is reached the same way (the
  owner has used it on the phone).
  This replaces the old rule
  that every tool fits without scrolling (DoD 2), which held for five tools.
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
   so more tools can be added later, with Compare pinned outside it at the right) with, just above it, a 136 dp sheet area
   (`editbottombar/EditSheet.kt`) for Crop, Rotate, Adjust and Filters, which never changes height.
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
   undo entry; `commit` makes the whole run one entry. Undo stores settings, never pixels. The
   cropper, `ui/tool/component/EditCropper.kt`, wraps `core/imagecrop`'s `CropperPreview` and shows
   the original with the layers baked into a copy of it, un-turned and unfiltered, because the crop
   is kept as fractions of the original.
4. **Live preview.** `ui/tool/component/photocard/LivePreviewImage.kt` draws the original on a
   Compose `Canvas` every frame: crop, quarter turns, flips, and the four sliders as one
   `ColorFilter.colorMatrix` (`data/edit/edit/EditColourMatrix.kt`). The CPU part is
   `data/edit/edit/FilterPreviewRenderer.kt`: the filter, its intensity and every layer except the
   one under a finger, rendered with the real `EditProcessor` on a copy of the original at most
   640 px (`PREVIEW_EDGE`), on `Dispatchers.Default`. The ViewModel sends requests through a
   conflated `Channel`, so only the newest one is rendered, and skips a request equal to the last.
5. **Done.** `ToolViewModel.done` shows a spinner in Done and renders the current settings once
   from the decoded original, shrunk to at most 3000 px (`WorkingSize.EDIT`, so a 2K photo keeps its size), with
   `data/edit/edit/EditProcessor.kt` on `Dispatchers.Default`, in this order: **layers**, crop,
   quarter turns, flips, filter (look-up images from `libs/imageprocessor.aar` and
   `libs/editimage.aar`, read from the assets), colour matrix, then every pixel made opaque. If no
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
   a time. So the crop cuts a layer, the turns and flips carry it, and the filter and sliders colour
   it. A layer whose picture cannot be made, or whose numbers are not finite, is skipped without an
   error.
9. **The cost, accepted in ADR-004.** While a finger moves a layer, that layer is drawn by
   `LivePreviewImage` itself (with the sliders and its opacity, but no filter) so it can follow the
   finger; it joins the filtered copy when the finger lifts. The preview and Done can differ
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

1. **Screen.** Text is the sixth entry of the row (`EditTool.TEXT`, icon `ic_text_fields`). Tapping
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

1. **Screen.** Date is the seventh entry of the row (`EditTool.DATE`, icon `ic_calendar_today`).
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
| Beautify | `data/edit/beautify/BeautifyProcessor.kt` | `LandmarkFaceFinder.kt`, `EngineSkinSmoother.kt`, `data/edit/face/EngineLandmarkFinder.kt` | [page](docs/features/beautify.md) |
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
| Edit (filters) | `data/edit/edit/FilterLut.kt` (reads images only) | none | `lookup/lookup_light.png` (`libs/imageprocessor.aar`) and six curve strips in `filter_materials/map/` (`libs/editimage.aar`) |
| Edit (crop) | `ui/tool/component/EditCropper.kt` | `CropperPreview`, `CropState` (`core/imagecrop`) | none |
| Edit (text) | `data/edit/edit/TextLayerLayout.kt` | Android `StaticLayout` | five OFL fonts in `app/src/main/res/font/` |
| Remove background | `data/edit/cutout/EngineSegmenter.kt` | `SelfieSegmentationUC`, `OpenCVLoader` | ML Kit `segmentation-selfie` |
| Enhance | `data/edit/enhance/EngineUpscaler.kt` | `EnhanceImageUC` | `realesrgan_64x64_float16.tflite` |
| Beautify | `data/edit/face/EngineLandmarkFinder.kt`, `data/edit/beautify/EngineSkinSmoother.kt` | MediaPipe `FaceLandmarker` (one, for width, lift, zoom and the smoothing boxes), OpenCV `Imgproc.bilateralFilter` | `face_landmarker.task` |
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

The engine modules, OpenCV and the sticker pictures keep the licences of their own sources.
