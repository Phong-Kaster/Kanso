# Edit: crop, rotate, adjust and filters

[Back to README](../../README.md)

Code links below point into `app/src/main/java/com/phongkaster/kanso/`; the link text leaves that
prefix out. Test links point into `app/src/test/java/com/phongkaster/kanso/`. The parts every tool
shares (opening the photo, the top bar, failures, Done) are on [The tool screen](tool-screen.md);
what happens after Done is on [Saving, the Result screen and Share](saving-and-share.md).

---

## What it is

Edit is Kanso's general editor. Under the photo sits a row of four tools, **Crop**, **Rotate**,
**Adjust** and **Filters**; tapping one opens its sheet under the row (tapping it again closes it).
While you edit, the photo on screen is a live preview drawn by Compose; the full picture is
rendered once, in plain Kotlin on the phone's processor, when you tap **Done**. No model and no
network are used. You can:

- **Crop**: choose Free, 1:1, 4:3, 3:4, 16:9 or 9:16. While the Crop sheet is open the photo becomes
  a cropper (from the `core/imagecrop` module): drag a handle to move an edge or a corner, drag inside
  the rectangle to move it, pinch to zoom. The cropper shows the plain decoded photo, without the
  turns, flips, filter or slider colours. A fixed shape sets the largest centred rectangle of that
  shape and locks the handles to it; Free unlocks them and keeps the rectangle as it is.
- **Rotate**: Turn left or Turn right a quarter turn, Flip across (left to right) or Flip upside
  down (a second tap flips back).
- **Adjust**: four sliders, **Brightness**, **Contrast**, **Saturation** and **Warmth**, each from
  -100 through 0 (no change) to +100, with the number beside the name. The photo follows the finger
  while it drags, and the slider stays where it is let go.
- **Filters**: None or one of seven looks, Light, 1977, Kelvin, Nashville, Walden, Hudson and Amaro,
  with an **Intensity** slider (0 to 100) that is usable while a filter is chosen. Choosing a filter
  **replaces** the one before; filters never pile up.
- **Undo** and **Compare**, in a small pill at the photo's bottom right that stays in reach whatever
  sheet is open. Undo takes back one action at a time; Compare is a switch that shows the untouched
  photo while it is on and disables the row of tools, the sheet's controls, the cropper and Undo.
  Done stays usable while comparing and renders the edit, not the original.
- **Done** at the top right renders the picture and opens the Result screen (Share, Save, Go home).
  Nothing is written to the gallery until Save is tapped there.

Each tool with a change carries a small dot on its entry in the row.

## How to use it

- Reach it from the **Edit** shortcut on Home (then pick a photo in the gallery), by tapping a
  **Recent edits** card on Home, from the round **Camera** button on the tab bar (a kept shot opens
  in Edit), or with **Edit** in the My edits photo viewer. Edit has no carousel banner and no
  Replace chip.
- Tap a tool in the row to open its sheet, change what you like, open another tool; the photo keeps
  the same size because it always takes 60% of the height below the top bar and the controls the
  other 40%.
- Switch **Compare** on to see the original, and off again to go on editing.
- Tap **Done**. While it renders, a small spinner shows in the Done button, which ignores taps, and
  the row of tools, the sheet and the cropper are disabled (with no control used yet there is
  nothing to render and Result opens at once). Then the Result screen opens; **Back** from Result returns to Edit with every setting and Undo step
  as it was left.
- **Back** on Edit first closes an open sheet. With changes made, it then asks "Discard your
  changes?" with **Discard** and **Keep editing**; with no changes it leaves at once.

## How it works

### The session and the live preview

```mermaid
flowchart TD
    A["A chip, turn, flip or finished crop gesture"] -->|changeSettings / chooseCropRatio / commitCrop| S["EditSession.change: new EditSettings, old one on the undo stack"]
    B["A slider being dragged"] -->|previewSettings| P["EditSession.preview: settings follow the finger, no undo entry"]
    B2["Slider let go"] -->|commitSettings| C["EditSession.commit: the whole drag is one undo entry"]
    S --> U["ToolUiState.settings"]
    P --> U
    C --> U
    U --> L["LivePreviewImage: Canvas crops, turns, flips; ColorFilter.colorMatrix for the sliders"]
    U -->|filter or intensity changed| Q["conflated Channel of filter requests"]
    Q --> R["FilterPreviewRenderer: 640 px copy of the original, filter only, Dispatchers.Default"]
    R -->|ToolUiState.filterPreview| L
```

1. **The screen and its state.** The Edit screen is
   [`ui/tool/ToolScreen.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/ToolScreen.kt)
   (`ToolScreen`, `ToolContent`) with
   [`ui/tool/EditToolContent.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/EditToolContent.kt)
   for its body: the photo area, the
   [`ui/tool/EditPill.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/EditPill.kt) on it,
   the [`ui/tool/EditToolRow.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/EditToolRow.kt)
   and the sheet area. The sheet is an ordinary `Surface` animated in with `AnimatedContent`, not a
   Material `ModalBottomSheet`, because a modal sheet dims the photo and swallows touches, and the
   pill must stay usable. Its ViewModel is
   [`ui/tool/ToolViewModel.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/ToolViewModel.kt),
   which exposes one `StateFlow<ToolUiState>`
   ([`ui/tool/ToolUiState.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/ToolUiState.kt)).
2. **The photo.** `ToolViewModel.load` decodes the photo with
   [`data/photo/PhotoLoader.kt`](../../app/src/main/java/com/phongkaster/kanso/data/photo/PhotoLoader.kt),
   upright, on `Dispatchers.IO`, with its longest edge at most twice the screen's longest edge
   (`displayMaxEdgePx`). This decoded bitmap is "the original" for the rest of the visit; the file
   itself is never touched.
3. **One settings value for the whole visit.** Everything the person has done is one immutable
   [`data/edit/edit/EditSettings.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditSettings.kt):
   the crop rectangle (fractions of the original), the locked crop shape, quarter turns, the two
   flips, four sliders from -1 to 1 (0 is no change), the filter and its intensity (0 to 1). It is
   held, with its undo stack, by
   [`data/edit/edit/EditSession.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditSession.kt).
   Every control hands the ViewModel a complete new value, never a change on top of the last
   result:
   - [`ui/tool/EditPanel.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/EditPanel.kt)
     draws the open sheet from the session's settings (it keeps no state of its own, so a slider
     reopens where it was left). A turn, a flip or a filter chip calls `changeSettings`; a crop
     shape calls `chooseCropRatio`; a slider calls `previewSettings` while it moves and
     `commitSettings` when let go.
   - [`ui/tool/EditCropper.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/EditCropper.kt)
     wraps `core/imagecrop`'s `CropperPreview` (package `com.mr0xf00.easycrop`,
     [`core/imagecrop/src/main/java/com/mr0xf00/easycrop/ui/CropperPreview.kt`](../../core/imagecrop/src/main/java/com/mr0xf00/easycrop/ui/CropperPreview.kt)).
     It always shows the original photo un-turned and un-flipped, because the crop is stored as
     fractions of the original and the render crops before it turns. When the last finger lifts,
     the rectangle becomes fractions and, if it differs from the session's crop by more than a tenth
     of a percent, goes to `commitCrop` once. A session crop or shape that changes (a chip, Undo)
     moves the cropper's rectangle to match, and a fixed shape locks its aspect (`CropState.aspectLock`,
     switched off while the rectangle is moved so the new shape is not bent back to the old one).
     While Compare is on the cropper is not drawn at all (the plain photo shows); while Done is
     rendering, a layer on top of the cropper takes every touch, so it ignores them. The cropper, the sheets and the row all run on the main thread, as Compose draws;
     none of them touches pixels.
   - `EditSession.change` pushes the old settings on the undo stack (an unchanged value adds
     nothing); `preview` follows the finger without an entry and `commit` turns the whole drag into
     one entry; `cropRatioChosen` sets the largest centred rectangle of the shape, fitted on its
     side when the photo has an odd number of quarter turns, so the turned picture comes out in the
     shape chosen; `undo` goes back to the last entry. There is no redo.
4. **The live preview.** The photo card draws
   `LivePreviewImage` (in `ui/tool/EditToolContent.kt`) whenever Compare is off and the Crop sheet
   is closed. It is a Compose `Canvas` that, every frame, takes only the crop rectangle from the
   bitmap (rounded exactly as the final render rounds it), turns it, mirrors it, fits it inside the
   card, and draws it through `ColorFilter.colorMatrix` with the four sliders' matrix from
   [`data/edit/edit/EditColourMatrix.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditColourMatrix.kt),
   the same numbers Done uses. With every slider at 0 no colour filter is set. No pixel is
   recomputed on the CPU for crop, turn, flip or the sliders.
5. **The filter preview.** A filter is a look-up per pixel that Compose cannot draw, so the
   ViewModel computes it. When the filter or its intensity changes (`requestPreview`), the settings
   go into a `Channel(Channel.CONFLATED)`: one collector coroutine renders one request at a time,
   and a newer request replaces one still waiting, so a fast intensity drag never queues up work.
   [`data/edit/edit/FilterPreviewRenderer.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/FilterPreviewRenderer.kt)
   runs the real `EditProcessor` with only the filter set, on `Dispatchers.Default`, on a copy of
   the original shrunk once to at most 640 px (`PREVIEW_EDGE`). The result becomes
   `ToolUiState.filterPreview`, and `LivePreviewImage` draws that bitmap instead of the original,
   cropping, turning and colouring it as above. A crop, turn or slider change keeps the same filter
   preview. A preview that fails shows the plain photo, with no message.

### Done, the full render and the Result screen

```mermaid
flowchart TD
    D["Done tapped (ToolViewModel.done)"] --> N{"Any Edit control used yet?"}
    N -->|no| H0["RenderedPhotoStore.put(decoded original, photo Uri)"]
    N -->|yes| F["finishing = true: spinner in Done"]
    F --> W["WorkingSize.fit(original, 2000 px) on Dispatchers.Default"]
    W --> E["EditProcessor: crop, turn, flip, filter, colour matrix, opaque"]
    E -->|Edited| H1["RenderedPhotoStore.put(rendered bitmap)"]
    E -->|Failed| X["failure message on the photo card; stay on Edit"]
    H0 --> R["doneEvents: navigate to Result"]
    H1 --> R
    R --> V["ResultViewModel reads the store once"]
    V --> SH["Share: copy in cache/shared/, FileProvider, share sheet"]
    V --> SV["Save: PhotoSaver, new file in Pictures/Kanso"]
    V --> GH["Go home: back stack cleared to Home"]
```

1. **Done.** `ToolViewModel.done` does nothing while a Done is already rendering, before the photo
   is shown, or while an edit runs. If no Edit control has been used yet on this visit (the
   ViewModel has no processor recipe yet), the decoded original goes to the hand-off store with the
   photo's Uri, at once. Otherwise it reads the settings at the tap, sets `finishing` (the Done
   button shows a small spinner and ignores taps; the row of tools, the sheet's controls and the
   cropper are disabled) and renders. Changes that were all undone still count as used, so Done
   then renders the neutral settings (a fresh picture of at most 2000 px, which Save writes as a
   JPEG) rather than handing over the file. Compare does not stop Done: it renders the settings,
   not the original.
2. **The full render.** Its input is the decoded original ("The photo", in the section above: at
   most twice the screen's longest edge, not the file at full size). `renderFull` shrinks it once
   to at most 2000 px on its longest edge (`WorkingSize.EDIT` in
   [`data/edit/WorkingSize.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/WorkingSize.kt))
   and runs
   [`data/edit/edit/EditProcessor.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditProcessor.kt),
   all on `Dispatchers.Default`. The processor always works from the original with the whole
   settings, so nothing is stacked. On the pixels as one `IntArray`, in this order:
   1. **Crop**: copies only the kept rectangle (fractions turned into pixels, rounded, edges put in
      order, at least 1 x 1). A fixed shape is no special case: choosing it already set the
      rectangle. Progress 0.25.
   2. **Turn** by moving every pixel (no blurring; four turns give the same pixels back), then the
      **flips**. Progress 0.5.
   3. **Filter**, when one is chosen at an intensity above 0: each pixel goes through the filter's
      look-up, then is mixed with its own colour by the intensity (`filterPixel`, shared with the
      preview). Progress 0.75. The filter comes before the sliders so the sliders fine-tune the
      filtered look.
   4. **Colour**, when a slider is away from 0: one 4 x 5 matrix from `EditColourMatrix.of`, in this
      order: contrast scales each channel around mid-grey 128 (up to x2, down to x0.2), saturation
      uses `setSaturation(1 + s)`, warmth adds up to 40 (of 255) to red and takes it from blue,
      brightness adds up to 80 to every channel. Values are rounded and clamped to 0..255.
   5. Every pixel's alpha is set to 255 and the result is a new `ARGB_8888` bitmap with
      `setHasAlpha(false)`. Progress 1. (Done shows no progress ring; only the spinner.)

   The turn and flip loops check for cancellation on every row, and the filter and colour loops
   every 65,536 pixels, so leaving Edit (which clears its ViewModel and cancels its coroutines)
   stops a render early.
3. **Where the filters come from.**
   [`data/edit/edit/FilterPreset.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/FilterPreset.kt)
   names seven images that ship inside the engine's prebuilt libraries and are merged into the
   app's assets at build time (no download): Light is lookup/lookup_light.png from
   `libs/imageprocessor.aar`, a 512 x 512 colour cube; 1977, Kelvin, Nashville, Walden, Hudson and
   Amaro are curve strips under filter_materials/map/ from `libs/editimage.aar`.
   [`data/edit/edit/FilterLut.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/FilterLut.kt)
   tells the two kinds apart by shape (a cube is read as a 3D table, a strip as three tone curves).
   `FilterLut.fromAssets` decodes each image the first time it is asked for and keeps it in that
   source's cache. No engine code runs.
4. **The hand-off.** The picture goes into
   [`data/edit/RenderedPhotoStore.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/RenderedPhotoStore.kt),
   a Koin single that holds one bitmap (a navigation key is saved as text and cannot carry one), and
   `doneEvents` (a `Channel`, so it fires once) tells the screen to push
   [`navigation/Result.kt`](../../app/src/main/java/com/phongkaster/kanso/navigation/Result.kt) on
   the back stack above Edit
   ([`navigation/KansoNavHost.kt`](../../app/src/main/java/com/phongkaster/kanso/navigation/KansoNavHost.kt)).
   Done does not end the session.
5. **Result.** [`ui/result/ResultViewModel.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/result/ResultViewModel.kt)
   reads the store once when it is built and
   [`ui/result/ResultScreen.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/result/ResultScreen.kt)
   shows it under the title "Your edit" with Share, Save and Go home:
   - **Save** is the only gallery write. `ResultViewModel.save` calls
     [`data/photo/MediaStorePhotoSaver.kt`](../../app/src/main/java/com/phongkaster/kanso/data/photo/MediaStorePhotoSaver.kt)
     on `Dispatchers.IO`: a rendered Edit picture is opaque, so it is written as a new JPEG at
     quality 95, at its size, into `Pictures/Kanso`. An untouched photo (Done with no change) is
     saved from its own file instead (its bytes, or turned upright if it carries an EXIF turn). A
     picture is saved at most once; the button then reads "Saved". On Android 8.1 and 9 the
     storage-write permission is asked first.
   - **Share** writes a copy of the picture on screen in the cache's `shared/` folder (JPEG
     quality 95 for an opaque picture, PNG for one with an alpha channel; for an untouched photo
     this is the decoded, screen-sized copy, not the file) on `Dispatchers.IO` and sends it through FileProvider and Android's share sheet with
     a one-off read grant ([`ui/result/SharePhoto.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/result/SharePhoto.kt)).
   - **Go home** clears the back stack down to Home, which ends the session; leaving Result also
     clears the store.

**What happens on failure:**

- Done running out of memory shows "This photo is too large to edit on this phone. Try a smaller
  one. Your photo is unchanged."; a filter image that cannot be read, or any other exception,
  shows "Something went wrong and the edit was not made. Your photo is unchanged." The message sits
  at the top of the photo card and a tap hides it. With the Crop sheet open the cropper replaces
  the photo card, so no message is drawn: the spinner just stops and Done can be tapped again. In both cases the person stays on Edit with
  every setting and Undo step kept, and nothing is handed over.
- A filter preview that fails shows the photo without the filter, silently; Done reports a real
  failure.
- A photo that cannot be decoded shows "This photo cannot be opened. Go back and pick another one."
- A failed Save shows "The photo could not be saved. Try again."; a refused storage permission
  shows that nothing was saved.
- After the process is killed, Result's store is empty and it shows "Nothing to show"; the Edit
  session lived only in the ViewModel's memory, so Edit reopens on the photo with no changes.

## Limits, from the code

- **Size.** The photo is decoded at most twice the screen's longest edge, and Done renders at most
  **2000 px** on the longest edge, so a 4000 x 3000 photo comes out at most 2000 x 1500. The saved
  file is a **JPEG at quality 95**. Done with no control used saves the original file instead (its
  bytes, or re-encoded upright when it carries an EXIF turn); once a control was used, even if
  every change was undone, Done renders a new JPEG of at most 2000 px.
- **Preview size.** The filter preview is computed on a copy at most **640 px** on its longest
  edge, so on screen a filtered photo holds less detail than the picture Done renders.
- **One filter at a time.** Choosing a filter replaces the earlier one. Moving from one filter to
  another keeps the intensity; coming from None it starts at 100; None resets it.
- **Crop.** The cropper always shows the photo un-turned and un-flipped. A turn after a crop turns
  the cropped picture with it (a 16:9 crop turned once comes out 9:16).
- **Undo** takes back one action at a time (a chip, a turn, a flip, a crop ratio, one finished crop
  gesture, or one whole slider drag), whichever sheet it came from. There is no redo.
- **Back with changes** asks before discarding; the check is on the current settings, so undoing
  every change leaves without asking.
- **No session across restarts.** Settings and Undo steps are not saved; they live as long as the
  Edit screen's ViewModel.
- **Removed in Run 9.** The old one-step-at-a-time Edit panel is gone: sliders no longer return to
  the middle after each change, the free crop no longer uses two Width and Height sliders, and
  filters no longer stack on top of each other.

## Tests

The Edit tests live in `app/src/test/java/com/phongkaster/kanso/tool/edit/`:
[`EditProcessorTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/EditProcessorTest.kt),
[`EditFilterTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/EditFilterTest.kt),
[`EditSessionTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/EditSessionTest.kt),
[`ColourMatrixParityTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/ColourMatrixParityTest.kt),
[`FilterPreviewRendererTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/FilterPreviewRendererTest.kt),
[`LivePreviewTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/LivePreviewTest.kt),
[`EditCropTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/EditCropTest.kt),
[`EditUndoTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/EditUndoTest.kt),
[`CompareTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/CompareTest.kt),
[`AdjustSheetTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/AdjustSheetTest.kt),
[`FiltersSheetTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/FiltersSheetTest.kt),
[`EditToolRowTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/EditToolRowTest.kt),
[`EditLayoutTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/EditLayoutTest.kt),
[`DiscardDialogTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/DiscardDialogTest.kt)
and [`EditToolTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/EditToolTest.kt).
Done and the Result screen are covered by
[`tool/DoneResultTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/DoneResultTest.kt),
[`result/ResultViewModelTest.kt`](../../app/src/test/java/com/phongkaster/kanso/result/ResultViewModelTest.kt)
and [`result/ResultScreenTest.kt`](../../app/src/test/java/com/phongkaster/kanso/result/ResultScreenTest.kt).

## Known limits and open items

- The limits above.
- Not yet checked on the owner's phone, a Galaxy A13 (32-bit, `armeabi-v7a`).

[Back to README](../../README.md)
