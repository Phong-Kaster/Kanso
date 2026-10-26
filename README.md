# Kanso

*Kanso (簡素) — Japanese for simplicity: leaving out what is not needed.*

Kanso is an Android photo editor with a simple, offline design: no ads, no analytics, no sign-in.
The manifest removes the `INTERNET` and `ACCESS_NETWORK_STATE` permissions that the engine
libraries would otherwise merge in (`app/src/main/AndroidManifest.xml`). Every model it uses ships
inside the app; nothing is downloaded. A photo only leaves the phone when you send it to another
app with Share.

This README describes **what the code on branch `loop/kanso-stickers` actually does**. It is
not a roadmap. It is the index: each feature below links to its own page in
[`docs/features/`](docs/features/), which explains what the feature is, how to use it, how it works
step by step with file paths, which tests cover it, and what is still open.

> **Current state.** The app works from start to end: Home, a photo gallery for each tool, the
> camera, a screen for each of the five editing tools, a Done button that opens a Result screen
> with Share, Save (into `Pictures/Kanso`) and Go home, the My edits tab and Settings. All five tools edit the photo. Edit draws
> its crop, turns, flips and sliders live as Compose draws (a filter on a 640 px copy) and renders in plain Kotlin at Done; Remove background, Enhance, Retouch and Artistic call the image engine's bundled on-device
> models (`libs/localml.aar`, through `core/localml`) and OpenCV. On the Android emulator (API 34,
> x86_64), on 2026-10-14, all five ran with no crash and each saved its result into
> `Pictures/Kanso` (Remove background as PNG, the others as JPEG); the camera, My edits, the photo
> viewer and Settings were also driven with no crash. That emulator run came before Run 9 rebuilt
> the Edit screen (row of tools, live preview, cropper, Result screen). Nothing has yet been checked
> on the owner's phone, a Galaxy A13.

---

## Features

### Editing tools

Every tool opens on its own screen with the same frame: Back, the tool's name, Undo and Done at the top right, the photo in
a card with a Replace chip (not on Edit), and the tool's controls. There is no Save bar: Done renders and opens Result.
[The tool screen](docs/features/tool-screen.md) explains that shared part: decoding, the working
size, progress, Undo, failures and Done.

| Feature | What it does | Saved as |
|---|---|---|
| [Edit](docs/features/edit.md) | A row of Crop, Rotate, Adjust, Filters and Stickers with Compare pinned at its right (Undo is in the top bar), and inside each sheet every feature (crop shape, turn, flip, Adjust slider, filter) is an icon with its title below it, in the bottom bar, each opening a sheet in a fixed-height area just above the row (the photo never changes size): crop with draggable handles, pinch zoom, Original (back to the whole photo), Free, 1:1, 4:3, 3:4, 16:9, 9:16 shapes; quarter turns and flips; brightness, contrast, saturation, warmth (one slider at a time, picked by tiles); one of seven filters (choosing one replaces the other) with an intensity; up to 20 of 60 bundled stickers placed, moved, pinched, turned, mirrored, duplicated and deleted on the photo. The photo is a live preview; Done renders it once in plain Kotlin. Undo is in the top bar beside Done; Compare is pinned at the right of the tool row. | JPEG quality 95, at most 2000 px (Done with no control used: the original file) |
| [Remove background](docs/features/remove-background.md) | Cuts the person out with ML Kit's bundled selfie-segmentation model and makes the rest transparent, shown over a checkerboard. People only, in practice. | PNG with transparency, at most 2000 px |
| [Enhance](docs/features/enhance.md) | Shrinks the photo to 512 px, upscales it four times with the bundled Real-ESRGAN model, and brightens it if it is dark; hold a control to compare with the original. | JPEG, at most 2048 px |
| [Retouch](docs/features/retouch.md) | Finds faces with the bundled face-landmark model and smooths each face with OpenCV's edge-preserving bilateral filter, mixed by a strength slider. | JPEG, at most 2000 px |
| [Artistic](docs/features/artistic.md) | Repaints the photo in one of four styles (Warm wash, Cool wash, Mosaic, Ink) with the bundled Inception v3 style-transfer models, mixed by a strength slider. | JPEG, at most 1024 px |

### Getting a photo

| Feature | What it does |
|---|---|
| [Gallery and albums](docs/features/gallery-and-albums.md) | Each tool's photo picker: the phone's photos newest first, filtered by album, with full, partial (Android 14+) and refused photo access handled. |
| [Camera](docs/features/camera.md) | Takes a photo into Kanso's private storage, with lens switch, flash and pinch zoom; Keep opens it in the tool. |

### After editing

| Feature | What it does |
|---|---|
| [Saving, the Result screen and Share](docs/features/saving-and-share.md) | Done hands the rendered picture to the Result screen through `RenderedPhotoStore` (nothing is written); Result's Save is the only gallery write, a new file in `Pictures/Kanso` (the original is never changed); Share writes a copy under the cache's `shared/` folder (PNG if the bitmap has an alpha channel, else JPEG quality 95) and sends it through FileProvider and Android's share sheet with a one-off read grant. After a process kill Result shows "Nothing to show". |
| [My edits and the photo viewer](docs/features/my-edits.md) | A tab listing everything Kanso saved, newest first; a tapped photo opens in a viewer with Share and Edit. |

### Around the app

| Feature | What it does |
|---|---|
| [Home](docs/features/home.md) | The carousel of the four AI tools, five round tool shortcuts, the Recent edits strip, and the floating tab bar with its Camera button. |
| [Settings: theme mode, theme colour and text size](docs/features/settings-and-theme.md) | A menu of three settings, each on its own screen: light, dark or system; Kanso blue, wallpaper colours (Android 12+) or an exact custom colour; Kanso's own text size (the phone's font size is ignored). Applied at once and kept across restarts. |
| [Launcher icon](docs/features/launcher-icon.md) | An enso ring around a lens: in Google's four colours on white for Kanso blue, in white on the closest of eight other background colours for any other theme colour. |

---

## Editing features

The real state of each editing feature, as the code is. "Works" means the code path is complete
from the control to the saved file; none of them has yet been checked on the owner's Galaxy A13.

| Feature | State | Notes |
|---|---|---|
| Edit: Crop | Works | Cropper from `core/imagecrop` on the original photo: handles, drag, pinch; Free or a fixed shape (locked); one finished gesture is one Undo step. |
| Edit: Rotate and flip | Works | Quarter turns left and right, flip across, flip upside down. |
| Edit: Adjust | Works | Brightness, contrast, saturation, warmth, as four icon-over-title tiles over one slider for the chosen tile (the chosen one is kept in `ToolUiState.adjustSlider`, so reopening Adjust shows it again); the photo follows the finger live, and a slider stays where it is let go. |
| Edit: Filters | Works | None or one of seven filters, with an intensity; choosing a filter replaces the one before. Previewed on a 640 px copy. |
| Edit: Stickers | Works | The fifth tool (decision record: [ADR-004](docs/adr/ADR-004-sticker-is-part-of-the-photo.md)); none of it has been checked on the owner's Galaxy A13. Its sheet (tabs Food 17, Mood 17, Quote 6, Emoji 20; 60 bundled pictures; a cross and a tick) adds a sticker at the middle of the picture you see, selected, one Undo step per tap, at most 20 (a 21st tap shows "A photo can hold 20 stickers at most"). The cross, or system Back, throws the sheet's changes away; the tick keeps them. The stickers are drawn on the photo on screen and, while the sheet is open, can be selected by putting a finger on one (frame with four corner buttons: delete, duplicate, mirror, and a handle that resizes and turns), dragged with one finger, pinched and twisted with two; each add, delete, duplicate, mirror and whole gesture is one Undo step. Done draws the stickers into the saved photo (first step of `EditProcessor`), so the crop, turns, flips, filter and sliders apply to them. The live preview shows them with the filter and the sliders, except the sticker under your finger, which shows the sliders but no filter until you let go; the cropper shows them; Compare shows the photo without them. |
| Edit: Undo and Compare | Works | Undo is an icon over its title in the top bar beside Done; Compare is pinned at the right of the tool row, behind a divider. Undo goes back one action at a time (no redo); Compare shows the original while it is on and disables the row, the sheet, the cropper and Undo (not Done). While the Stickers sheet is open it takes the place of the row, Compare included, so Compare is out of reach until the sheet closes; Undo stays in the top bar. |
| Remove background | Works | ML Kit selfie segmentation: people only, in practice. |
| Enhance | Works | Real-ESRGAN four-times upscale of a 512 px copy, plus brightening when dark. |
| Retouch | Works | Face landmarks plus OpenCV bilateral smoothing, with a strength slider. |
| Artistic | Works | Four styles from the Inception v3 style-transfer models, with a strength slider. |
| Done and Result (Share, Save, Go home) | Works | Done hands the picture to Result; Save is the only gallery write. |

The old Edit panel is gone since Run 9: sliders no longer return to the middle after each change,
the free crop no longer uses two Width and Height sliders, and filters no longer stack.

## How each editing feature works

Paths are under `app/src/main/java/com/phongkaster/kanso/` unless they start with `core/` or
`libs/`. Each feature page in [`docs/features/`](docs/features/) has the full detail; Edit is traced
here end to end, with diagrams on [its page](docs/features/edit.md).

### Edit

1. **Screen and ViewModel.** `navigation/KansoNavHost.kt` shows the `ToolPhoto(tool, photoUri)` key
   (`navigation/ToolPhoto.kt`) with `ui/tool/ToolScreen.kt` and a `ui/tool/ToolViewModel.kt`, which
   exposes one
   `StateFlow<ToolUiState>` (`ui/tool/ToolUiState.kt`). The Edit layout is
   `ui/tool/components/editbottombar/EditBottomBar.kt`: the row of tools (`ui/tool/components/editbottombar/edittoolrow/EditToolRow.kt`) is the `Scaffold`'s
   `bottomBar` (`EditBottomBar`), and a tapped tool's sheet appears just above the row, as in Adobe
   Lightroom; the photo (`EditPhotoArea`) fills the space between the top bar and the bottom bar.
   The sheet area is one fixed height (136 dp) open or not, so the photo never changes size;
   with no tool open it shows "Pick a tool to start editing". Every tool fits it: Adjust shows four
   sub-tool chips and one slider (`ui/tool/AdjustSlider.kt`); very large text scrolls inside it.
   The sheet is a plain `Surface` cross-faded with `AnimatedContent`, not a `ModalBottomSheet`,
   so the row of tools, with Compare pinned at its right
   (`ui/tool/components/editbottombar/edittoolrow/EditToolRow.kt`), stays usable (the Stickers
   sheet replaces the row while it is open; Undo stays in the top bar). The sheets' controls are
   `ui/tool/components/toolpanel/editpanel/EditPanel.kt`.
2. **Decode.** `ToolViewModel.load` decodes the photo upright on `Dispatchers.IO`
   (`data/photo/PhotoLoader.kt`), at most twice the screen's longest edge. That bitmap is the
   original for the whole visit; the photo's file is never changed.
3. **The session.** All changes are one immutable `EditSettings`
   (`data/edit/edit/EditSettings.kt`) inside an `EditSession` (`data/edit/edit/EditSession.kt`)
   with its undo stack. A chip, turn or flip calls `changeSettings`, a crop shape
   `chooseCropRatio`, a finished cropper gesture `commitCrop`, a slider `previewSettings` while it
   moves and `commitSettings` when let go (the whole drag is one Undo step). The cropper,
   `ui/tool/components/EditCropper.kt`, wraps `core/imagecrop`'s `CropperPreview`
   (`core/imagecrop/src/main/java/com/mr0xf00/easycrop/ui/CropperPreview.kt`) and its `CropState`;
   it shows the decoded photo with the stickers drawn into a copy of it (if copying the photo runs
   out of memory, the photo without stickers), un-turned,
   un-flipped and unfiltered, because the crop is kept as
   fractions of the original and the render crops before it turns. When the last finger lifts, a
   rectangle that moved by more than a tenth of a percent becomes one `commitCrop`. All of this,
   and the row and the sheets, runs on the main thread as Compose draws; the only pixels it
   computes are the sticker bake (step 10): a copy of the photo with each sticker picture drawn on it.
4. **Live preview.** `LivePreviewImage` (in `ui/tool/components/photocard/LivePreviewImage.kt`) draws the original on a
   Compose `Canvas` every frame: crop, quarter turns, flips, and the four sliders as one
   `ColorFilter.colorMatrix` from `data/edit/edit/EditColourMatrix.kt`. A filter is the one part
   computed on the CPU: the ViewModel sends filter changes through a conflated `Channel`, and
   `data/edit/edit/FilterPreviewRenderer.kt` renders the filter, its intensity and every sticker
   except the one being touched on a copy of the original at most 640 px, on
   `Dispatchers.Default`, which the canvas then draws in place of the original. It also renders
   when there is no filter but there are stickers. The ViewModel asks for it, among other places, from
   `addSticker`, `deleteSticker`, `duplicateSticker`, `flipSticker`, `commitSettings`,
   `confirmStickers` and `cancelStickers` (also `changeSettings`, `previewSettings`,
   `chooseCropRatio`, `undo`, `load`, `openTool` and `setComparing`).
5. **Done.** `ToolViewModel.done` shows a spinner in Done, disables the row, the sheet and the
   cropper, and renders the whole current settings once from the decoded original (at most twice
   the screen's longest edge), shrunk to at most 2000 px (`WorkingSize.EDIT` in
   `data/edit/WorkingSize.kt`), with `data/edit/edit/EditProcessor.kt` on `Dispatchers.Default`, on
   the pixels as one `IntArray`: stickers (drawn first, cut by the crop, then turned, flipped,
   filtered and coloured with the photo; the settings hold up to 20, each add, delete, duplicate,
   flip or whole drag is one Undo step; the Stickers sheet adds them, see "Stickers" below),
   crop, quarter turns, flips, the filter (look-ups from
   `data/edit/edit/FilterLut.kt` and `data/edit/edit/FilterPreset.kt`, images bundled in
   `libs/imageprocessor.aar` and `libs/editimage.aar`, read from the app's assets with no
   download), the colour matrix, then every pixel made opaque, giving a new `ARGB_8888` bitmap
   with `setHasAlpha(false)`. If no Edit control was used yet, there is nothing to render and the
   decoded original is handed over at once, with the photo's Uri. The picture goes into
   `data/edit/RenderedPhotoStore.kt` (a Koin single holding one bitmap) and the `Result` key
   (`navigation/Result.kt`) is pushed above Edit; Back from Result returns to the session intact.
6. **Result.** `ui/result/ResultViewModel.kt` reads the store once; `ui/result/ResultScreen.kt`
   offers Share, Save and Go home. **Save** is the only gallery write:
   `data/photo/MediaStorePhotoSaver.kt`, on `Dispatchers.IO`, writes a new JPEG at quality 95, at
   the rendered size, named `Kanso_<yyyyMMdd_HHmmss>.jpg` (with `_1`, `_2`, ... added when that
   name is taken), into `Pictures/Kanso` (an untouched photo is saved from its own file: its bytes
   copied unchanged, or re-encoded upright as a JPEG when it carries an EXIF turn; a turned photo
   too large to turn in memory is copied as it is with its tag kept, and Result shows the "kept
   orientation" message). An untouched photo's save is always named `.jpg` and typed
   `image/jpeg`, whatever its own format, so an untouched PNG, WebP or HEIC photo copied byte for
   byte keeps its own format inside a file named `.jpg` (a known defect, not fixed yet). A picture
   is saved once; the button then reads "Saved". On Android 8.1 and 9 the
   storage-write permission is asked first. **Share** (`ui/result/SharePhoto.kt`) writes a copy of
   the picture on screen under the cache's `shared/` folder (JPEG quality 95 for an opaque picture,
   which every Edit render is), on `Dispatchers.IO`, and sends it through FileProvider with a
   one-off read grant. **Go home** clears the back stack to Home and ends the session; leaving
   Result clears the store.
7. **On failure.** A Done render that runs out of memory ("This photo is too large to edit on this
   phone...") or fails in any other way, such as a filter image that cannot be read ("Something
   went wrong and the edit was not made..."), shows its message at the top of the photo card, hands nothing over and keeps every setting and
   Undo step. The failure stays in the state until a tap on the message or the next Done
   (changing a setting, Undo and closing a sheet leave it in place). With the Crop sheet open the
   cropper replaces the photo card, so at first no message is visible (the spinner stops and Done
   can be tapped again); the message appears as soon as the photo card is shown again, when the
   Crop sheet is closed, or at once when Compare is turned on (Compare shows the photo card in
   place of the cropper). A failed filter preview silently leaves
   `filterPreview` empty: the card draws the original photo, with no filter and no resting
   stickers on screen (Done's saved result is unaffected). A photo that cannot be decoded shows "This photo cannot be opened. Go
   back and pick another one." A failed Save says "The photo could not be saved. Try again."; a
   refused storage permission says nothing was saved. After a process kill, Result shows "Nothing
   to show" and Edit reopens on the photo with no changes (the session lived only in memory).

### Edit: Stickers

1. **Screen.** The row has five tools (Crop, Rotate, Adjust, Filters, Stickers). Tapping Stickers
   (`ToolViewModel.openTool`) opens a sheet that takes the row's place in `EditBottomBar`
   (`ui/tool/components/editbottombar/EditBottomBar.kt`), as tall as the row plus the fixed sheet
   area, so the photo keeps its size; none of the five entries is on screen while it is open.
   `ui/tool/ToolScreen.kt` fills the sheet with `EditPanel`
   (`ui/tool/components/toolpanel/editpanel/EditPanel.kt`), which draws `StickersPanel`
   (`ui/tool/components/toolpanel/editpanel/stickerspanel/StickersPanel.kt`): a top bar with a cross
   and a tick (`StickersSheetTopBar.kt`), a `LazyRow` of category chips (`StickerCategoryChips.kt`)
   and a three-column `LazyVerticalGrid` of tiles (`StickerTile.kt`). Opening it takes a snapshot of
   the whole session (settings and Undo steps) in `ToolViewModel`.
2. **The list.** `data/edit/edit/StickerCatalog.kt` lists 60 bundled pictures, each an explicit
   `R.drawable.img_sticker_*` webp in `app/src/main/res/drawable/`: Food 17, Mood 17, Quote 6,
   Emoji 20 (`StickerCategory.kt`, `CatalogSticker.kt`). Nothing is downloaded.
3. **Adding.** A tile tap calls `ToolViewModel.addSticker`, on the main thread. It uses
   `data/edit/edit/StickerMapping.kt` to turn the middle of the picture you SEE (after the crop,
   quarter turns and flips) into fractions of the original photo, and `EditSession.addSticker`
   puts a `Sticker` there (a third of the shown width, upright, key from a counter) on top of the
   others: one Undo step, and the new sticker is `selectedStickerKey`. At 20 stickers nothing
   changes and `stickerLimitShown` makes `StickerLimitMessage`
   (`ui/tool/components/StickerLimitMessage.kt`) show "A photo can hold 20 stickers at most"
   (`stickers_limit_message` in `res/values/strings_stickers.xml`) for about 2.5 seconds. Taps are
   ignored while Compare is on or Done renders.
4. **Keep or throw away.** The tick (`confirmStickers`) keeps everything and closes the sheet. The cross and the system Back (`cancelStickers`,
   reached by `onBackPressed`) put the session back to the snapshot, settings and Undo steps, so
   Undo is as it was, and clear the selection. While Compare is on or Done renders, the tick, the
   cross and Back do nothing (Back is consumed, so the screen stays): Done draws the stickers as
   they were when it was tapped. Undoing the step that added the selected sticker clears the
   selection.
5. **Drawing.** Done draws the saved photo's stickers: `data/edit/edit/EditProcessor.kt` (first step,
   `Dispatchers.Default`) decodes each picture with `data/edit/edit/DrawableStickerPictures.kt`
   (`BitmapFactory`, `inSampleSize`, nothing cached) and blends it into the cropped pixels, so the
   saved JPEG (quality 95, `Pictures/Kanso`) holds them. The one recipe from settings to processor,
   `editProcessorFor(context)` in `ui/tool/EditProcessorFactory.kt`, is used by the sheets, the
   sticker taps and Done, and passed through the cropper to `commitCrop`. The cropper draws its own
   stickers with a `DrawableStickerPictures`, so a sticker never disappears in one of them.
6. **On the photo.** `PhotoCard.kt` puts a `StickerOverlay`
   (`ui/tool/components/photocard/stickeroverlay/StickerOverlay.kt`) over the live picture, inside
   the same box (frame, buttons and touches; the pictures come from the live preview, see 9), so
   stickers show with any tool open, but not while Compare is on. A newer sticker
   lies on top of an older one. Touches work only while the Stickers sheet is open, Compare is off
   and Done is not running (`ToolViewModel.stickerEditingOpen`); otherwise the stickers are only
   drawn. One handler (`StickerGestures.kt`, raw pointer input on the main thread) finds the
   topmost sticker under the finger on its turned rectangle: a finger put down on it calls
   `selectSticker` (so a drag selects too), a touch on empty photo selects none. One finger drags the sticker by the finger's move; two fingers
   scale it by the ratio of their distance and turn it by the change of their angle, with no
   damping (during the gesture, width kept between 5 and 100 percent of the shown width, `StickerGeometry.kt`; a later tighter crop can push it past 100). Each
   move calls `previewStickers`, which shows the new list at once and makes no Undo entry; the
   last finger up calls `commitSettings`, so a whole drag or pinch is one Undo step.
7. **The frame.** The selected sticker gets `StickerFrame.kt`: an outline that turns with it and
   four corner buttons (`StickerCornerButton.kt`): top-left deletes (`deleteSticker`, nothing
   selected afterwards), top-right duplicates (`duplicateSticker`: the copy sits 4 percent of the
   shown width to the right and down as the person sees it, and is selected; at 20 the limit
   message shows), bottom-left mirrors (`flipSticker`), bottom-right is a handle that resizes and
   turns the sticker about its centre together, keeping its shape; a whole handle pull is one
   Undo step. Delete, duplicate and mirror are one Undo step each; selecting is not.
8. **Selection.** It lives in `ToolUiState.selectedStickerKey`, not in the edit settings. It is
   pruned in `publishSession` whenever its sticker is gone (Undo, delete), and closing the sheet
   by the tick or the cross selects none.
9. **Filter and sliders on stickers, live.** The overlay no longer draws sticker pictures (it keeps
   the frame, corner buttons and touch areas). Instead the small preview copy carries them:
   `FilterPreviewRenderer.render(requested)`
   (`data/edit/edit/FilterPreviewRenderer.kt`); `ToolViewModel.requestPreview` first leaves the
   touched sticker out of the settings it passes, so the renderer draws every other sticker into the
   small copy before the filter, so the filter and Intensity act on them; it renders even with no
   filter when stickers exist, and gives nothing when there is no filter and nothing to draw.
   `ToolViewModel` keys the preview request on filter, intensity and the untouched stickers.
   The first `previewStickers` that changes a sticker sets `ToolUiState.touchedStickerKey`; that
   sticker is left out of the small copy and `LivePreviewImage`
   (`ui/tool/components/photocard/LivePreviewImage.kt`) draws it inside the same crop, turn and
   flip, clipped to the photo, with the colour sliders' matrix but no filter, so it follows the
   finger. `commitSettings` (finger up), the tick or cross, `openTool` and `setComparing` clear the key
   (`publishSession` clears it only when that sticker is gone) and
   the sticker joins the filtered copy: one small re-render at the start of a gesture and one at
   its end, none during the drag. The cropper (`ui/tool/components/EditCropper.kt`,
   `withStickersDrawn`) bakes the stickers into a copy of the original photo with the same
   mirror, turn and scale as `EditProcessor`, so they show while you crop. Compare shows the
   untouched photo with no stickers.
10. **Order, cost, input, output, failure.** Why stickers go first is in
    [ADR-004](docs/adr/ADR-004-sticker-is-part-of-the-photo.md). The accepted cost: while a
    finger moves a sticker, that one sticker shows no filter, because a filter is computed on a
    640 px copy and cannot follow a finger; it joins the filtered copy on finger up. Done and
    the preview can also differ slightly in detail (640 px against up to 2000 px). Input: the
    original photo, shrunk to at most 2000 px, and the settings' `Sticker` list (picture id,
    centre, width, turn and mirror, as fractions of the original photo). Output: one opaque
    `ARGB_8888` bitmap, saved by Result as a JPEG at quality 95 in `Pictures/Kanso`. Threading:
    Done's render on `Dispatchers.Default` (each picture asked for at no more than the photo's
    longest edge, though the decoded bitmap can be wider because the decoder picks a
    power-of-two sample size; blended in through one bitmap of the visible part of the sticker,
    so an over-sized sticker cannot run the phone out of memory), the small preview copy on
    `Dispatchers.Default`, everything else (panel, gestures, selection) on the main thread,
    including the decode of the touched sticker's picture (`PhotoCard.kt`) and the cropper's
    sticker bake (`EditCropper.kt`), which run inside composition. A
    picture that cannot be decoded (including running out of memory while decoding that one
    picture), or a sticker with a non-finite number, is skipped in Done without an error; running
    out of memory elsewhere in the render gives the usual "too large to edit" message and
    keeps every setting and Undo step. The 20-sticker limit is `EditSettings.MAX_STICKERS`.
    The pictures are copies of the ones in `reference/pinity-stickers/res/drawable/`; none is
    downloaded.

### Remove background, Enhance, Retouch and Artistic

These four share one path in `ui/tool/ToolViewModel.kt`: `apply` shrinks the photo to the tool's
working size (`data/edit/WorkingSize.kt`: Remove background and Retouch 2000 px, Enhance 512 px,
Artistic 1024 px) and runs its `ToolProcessor` on `Dispatchers.Default`; the result becomes the
shown photo. Each tool's processor is the one
`di/AppModule.kt` puts in `data/edit/ProcessorRegistry.kt`. Remove background and Enhance run on
the photo as shown; Retouch and Artistic always run from the original with the strength (and
style) set, so their results do not stack. Undo: a run on the shown photo (Remove background,
Enhance) keeps the photo from before it for Undo. A Retouch or Artistic run keeps the photo from
before it only when the last result did not also come from such a run from the original; a repeat
run (another strength or style) replaces that result without an Undo step of its own, so Undo goes
back past the whole series to what was shown before it. A failure shows a message on the
photo card and leaves the photo as it was. Done hands the shown photo to Result at once.

- **Remove background**: `data/edit/cutout/CutoutProcessor.kt`, calling
  `data/edit/cutout/EngineSegmenter.kt`; saved as PNG with transparency.
  [Details](docs/features/remove-background.md).
- **Enhance**: `data/edit/enhance/EnhanceProcessor.kt`, calling
  `data/edit/enhance/EngineUpscaler.kt` and `data/edit/enhance/AutoLighting.kt`; saved as JPEG.
  [Details](docs/features/enhance.md).
- **Retouch**: `data/edit/retouch/RetouchProcessor.kt`, calling
  `data/edit/retouch/EngineFaceFinder.kt` and `data/edit/retouch/EngineSkinSmoother.kt`; saved as
  JPEG. [Details](docs/features/retouch.md).
- **Artistic**: `data/edit/artistic/ArtisticProcessor.kt`, calling
  `data/edit/artistic/EngineStylizer.kt`; saved as JPEG. [Details](docs/features/artistic.md).

---

## Architecture

Paths are under `app/src/main/java/com/phongkaster/kanso/`.

- **One activity** (`MainActivity`) in a task of its own, drawn entirely with Jetpack Compose. The
  launcher's entries open `LauncherActivity`, which starts `MainActivity` and closes, so switching
  the icon colour never closes the app (see [Launcher icon](docs/features/launcher-icon.md)).
  `KansoApplication` starts Koin with `di/AppModule.kt`.
- **Navigation 3** (`navigation/KansoNavHost.kt`): one saved back stack of `@Serializable` keys:
  `Home`, `MyEdits`, `Settings` (the tabs), `ThemeModeSettings`, `ThemeColourSettings`, `TextSizeSettings` (the three setting screens), `Gallery(tool)`, `Camera(tool)`, `ToolPhoto(tool, uri)`,
  `Result`, `EditedPhoto(uri)`. Each entry gets its own `ViewModelStore`, so a ViewModel
  survives rotation and is cleared when its screen is popped. The tabs live in `KansoTabShell`,
  under a floating tab bar.
- **State:** every screen has one ViewModel exposing one immutable `UiState` as a `StateFlow` (the
  pattern is explained on `ui/home/HomeUiState.kt`); one-off events use a `Channel`. Each screen is
  split into a stateful composable and a stateless `…Content` composable with light and dark
  previews.
- **Editing:** every tool runs through one interface, `data/edit/ToolProcessor.kt`. Each engine
  call sits in one wrapper file (`Engine*.kt`) behind a small interface, so everything else is
  tested on the computer with fakes; the engine is native code and cannot load there.
- **Data:** `PhotoSource` and `SavedPhotoSource` (MediaStore reads), `PhotoLoader` (decode),
  `PhotoSaver` (write), `RenderedPhotoStore` (the hand-off from Done to Result), `SettingsRepository` (Preferences DataStore at
  `files/datastore/settings.preferences_pb`).
- **UI:** Jetpack Compose with Material 3 1.5 (Expressive components and motion), Coil for
  thumbnails, MaterialKolor for colour schemes built from a seed colour.
- **SDK levels:** `minSdk 27`, `targetSdk 36`, `compileSdk 37`, NDK `27.0.12077973`. Debug builds
  keep the `x86_64`, `arm64-v8a` and `armeabi-v7a` ABIs.

### The image engine: what each tool calls

`app/build.gradle.kts` depends on every engine module and on `:opencv`, so all of them, with their
native libraries and bundled assets, are packed into the APK. Only these files of `app/` call them:

| Tool | Wrapper file in `app/` | Engine class | Model or library (all bundled) |
|---|---|---|---|
| Edit | `data/edit/edit/FilterLut.kt` (reads images only) | none | `lookup/lookup_light.png` (`libs/imageprocessor.aar`), and six curve strips in `filter_materials/map/` (`libs/editimage.aar`): 1977map.png, kelvinMap.png, nashvilleMap.png, waldenMap.png, hudsonMap.png and amaroMap.png |
| Edit (Crop) | `ui/tool/components/EditCropper.kt` | `CropperPreview`, `CropState` (`core/imagecrop`, package `com.mr0xf00.easycrop`) | none: Compose drawing and touch handling |
| Remove background | `data/edit/cutout/EngineSegmenter.kt` | `SelfieSegmentationUC`, `OpenCVLoader` | ML Kit `segmentation-selfie` (its library ships the model) |
| Enhance | `data/edit/enhance/EngineUpscaler.kt` | `EnhanceImageUC` | `realesrgan_64x64_float16.tflite` |
| Retouch | `data/edit/retouch/EngineFaceFinder.kt` | `DetectFaceLandmarkUC` (with `UnifyPoint` from `core/common`) | `face_landmarker.task` |
| Retouch | `data/edit/retouch/EngineSkinSmoother.kt` | OpenCV `Imgproc.bilateralFilter` | OpenCV, no model |
| Artistic | `data/edit/artistic/EngineStylizer.kt` | `StylePredictInceptionV3UC`, `StyleTransferInceptionV3BlendUC` | `image_stylization_inceptionv3_dr_predict.tflite`, `image_stylization_inceptionv3_dr_transfer.tflite` |

Outside editing, `data/media/MediaStorePhotoSource.kt` uses `core/gallery`'s `MediaRepository`,
and `ui/settings/SettingsScreen.kt` uses `core/colorpicker`'s `HSVColorPicker`. The `.tflite` and
`.task` models sit in the assets of `libs/localml.aar`, which also holds
`hifill_inpaint_512_int8.tflite` and `magic_touch.tflite`; those two are packed in the APK but never
called. The AARs ship no source, so each feature page describes what Kanso passes in and what it
does with the answer.

---

## Building and testing

Build with Gradle from the repository root:

```bash
./gradlew :app:assembleDebug          # build the debug APK
./gradlew :app:testDebugUnitTest      # JVM tests (Robolectric, Compose UI test)
./gradlew :app:verifyRoborazziDebug   # screenshot tests against the recorded images
./gradlew :app:lintDebug              # Android lint
```

All of them together:

```bash
./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue
```

The tests live in `app/src/test/` (more than 100 test classes, plus 79 recorded
pictures in `app/src/test/screenshots/`, numbered 1 to 82 with 7, 8 and 9 unused; pictures 69
to 82 are the tool row with Stickers (69, 70), the Stickers sheet's Food, Mood, Quote and Emoji
tabs (71 to 78), a selected sticker (79, 80), and stickers under a crop and a filter (81, 82), in
light and dark, from `StickerScreenshotTest.kt`). They cover the screens, navigation, every tool's
processor with fakes, saving, Share, the camera (through a fake `CameraController`), settings,
previews and screenshots. They need no device. The `Engine*.kt` wrappers are native code and are
only exercised on a device or emulator. Each feature page lists its own tests.

---

## Where the code comes from

| Path | What it is |
|---|---|
| `app/` | The Kanso app, written by Foreman. |
| `core/` | The image engine's modules, imported from Retica at commit `9d227a8`. Four of them (`editimage`, `imageprocessor`, `localml`, `overlaybrush`) wrap prebuilt libraries and have no source of their own. The app calls `core/gallery`, `core/colorpicker` and `core/localml`, uses one type of `core/common`, and reads filter images from `core/editimage` and `core/imageprocessor`. The Edit screen's Crop uses `core/imagecrop`'s cropper (handles, drag, pinch; `ui/tool/components/EditCropper.kt`); `core/overlaybrush` is built in but not called. |
| `libs/` | Those prebuilt libraries (AARs), plus two TensorFlow Lite AARs. `tensorflow-lite-select-tf-ops.aar` is used by `core/localml`. No build file references `tensorflow-lite.aar`. |
| `opencv/` | The OpenCV Android SDK module the engine depends on. |
| `reference/retica-app/` | Retica's original app code, **read-only**: how the engine's use cases are called. It is not built, and nothing in Kanso imports it. |
| `reference/pinity-stickers/` | Pinity's sticker code (`java/`) and its 60 sticker pictures (`res/drawable/`), **read-only**: what the Stickers tool was ported from. It is not built, and nothing in Kanso imports it; the app's pictures are copies in `app/src/main/res/drawable/`. |
| `docs/features/` | One page per feature (linked above). |
| `docs/adr/` | The architecture decisions behind Kanso: [ADR-001](docs/adr/ADR-001-kanso-is-a-material-3-expressive-app-blue-by-default-with-one-editor.md), [ADR-002](docs/adr/ADR-002-kanso-has-one-screen-per-tool-and-a-floating-tab-bar.md), [ADR-003](docs/adr/ADR-003-edit-has-one-session-and-done-opens-a-result-screen.md), [ADR-004](docs/adr/ADR-004-sticker-is-part-of-the-photo.md). |

The app itself is written by [Foreman](https://github.com/Phong-Kaster/Foreman), one goal at a time.
