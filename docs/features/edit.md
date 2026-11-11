# Edit: crop, rotate, adjust, colour (RGB, HSL, curves), filters, stickers, text and date

[Back to README](../../README.md)

Code links below point into `app/src/main/java/com/phongkaster/kanso/`; the link text leaves that
prefix out. Test links point into `app/src/test/java/com/phongkaster/kanso/`. The parts every tool
shares (opening the photo, the top bar, failures, Done) are on [The tool screen](tool-screen.md);
what happens after Done is on [Saving, the Result screen and Share](saving-and-share.md).

> **Run 11 (ADR-005): layers and Text.** A sticker is now a **layer** (`data/edit/edit/Layer.kt`,
> content `LayerContent.Picture`), and the sixth tool, **Text**, adds layers of the text kind. The
> sections below were written for Run 10 and still say "sticker", `Sticker`, `StickerOverlay` and
> `ui/tool/components/`; the code now names them `Layer`, `LayerOverlay` (in
> `ui/tool/component/photocard/layeroverlay/`), `LayerMapping`, `LayerPictures` and
> `ui/tool/component/`. What changed in behaviour since Run 10:
>
> - Every layer has an opacity (10–100%) and both layer sheets have Size and Opacity sliders.
> - A layer sheet takes exactly the row's and sheet area's height, so the photo never changes size;
>   its top bar is pinned and a row of tabs picks the one control shown under it (2026-10-28).
> - Touching a layer with any sheet but Crop open (or none) selects it, brings it to the front and
>   opens the sheet of its kind.
> - The frame's corners are the same on every layer: delete top-left, duplicate bottom-left, resize
>   bottom-right; top-right mirrors a sticker and edits a text.
> - The 20-item limit counts stickers and texts together.
>
> The Text tool is traced end to end in the README,
> [section 3.4](../../README.md#34-text), and the shared layer machinery in
> [section 3.2](../../README.md#32-layers-what-stickers-text-and-dates-share).
>
> **ADR-006: Date.** The seventh tool, **Date**, adds layers of the date kind
> (`LayerContent.Date`): a moment and one of 20 English formats, styled with the Text sheet's fonts
> and colours, with no mirror button (its top-right corner stays empty). A tap on the selected date
> moves it to the next format. The 20-item limit now counts stickers, texts and dates together.
> It is traced end to end in the README, [section 3.5](../../README.md#35-date).
>
> **RGB (2026-11-03, plan [`docs/plans/rgb-tool.md`](../plans/rgb-tool.md)).** An eighth tool,
> **RGB**, sits between Adjust and Filters, so the row now holds Crop, Rotate, Adjust, RGB,
> Filters, Stickers, Text and Date; on a 411 dp phone Stickers, Text and Date are one swipe away.
> RGB is described below with Adjust, and its step in "Done, the full render" below; it is traced end
> to end in the README, [section 3.1, step 8](../../README.md#31-the-edit-screen).
>
> **HSL (Run 11, [ADR-008](../adr/ADR-008-hsl-is-computed-on-the-cpu-by-one-formula.md)).** A ninth
> tool, **HSL**, sits between RGB and Filters, so the row now holds Crop, Rotate, Adjust, RGB, HSL,
> Filters, Stickers, Text and Date. It changes hue, saturation and lightness for nine colour ranges
> (27 values) with nine circles, three chips and one slider. Unlike Adjust and RGB it is **not** part
> of the colour matrix: it is a per-pixel step on the CPU (`data/edit/edit/EditHsl.kt`) that runs
> after the filter and before the colour matrix, in the live preview and in Done alike. It is
> described below with RGB, and traced end to end in the README,
> [section 3.1, step 9](../../README.md#31-the-edit-screen).
>
> **Curves ([ADR-009](../adr/ADR-009-curves-run-on-the-cpu-after-hsl.md)).** A tenth tool,
> **Curves**, sits between HSL and Filters, so the row now holds Crop, Rotate, Adjust, RGB, HSL,
> Curves, Filters, Stickers, Text and Date. It is **partial**: the chips RGB (all colours), Red, Green and Blue
> choose the curve the graph edits (RGB first, the last choice is remembered, choosing is not an Undo
> step and changes nothing in the photo). On the graph, a tap on empty space adds a point (at most 10), a drag moves one
> (the two end points only up and down), a long press removes a middle one, and Reset (disabled
> while the chosen curve is straight) straightens only that curve; each gesture is one Undo step. Done applies the
> curve on the CPU (`data/edit/edit/EditCurves.kt`) after HSL and before the colour matrix, so the
> **saved picture has it**. The **live preview follows it** while a point is dragged (a CPU copy, filter, HSL,
> curves, then the colour matrix on top, as Done does; a sticker under a finger is drawn without that
> CPU stage). A screen reader hears the
> graph ("Red curve, N points"), the chips and Reset but cannot move points. Traced end to end in the README,
> [section 3.1, step 10](../../README.md#31-the-edit-screen).
>
> **The Colour group (Run 13).** The row above is no longer flat. Layer 1 reads Crop, Rotate, Adjust,
> **Colour**, Filters, Stickers, Text, Date, with Compare pinned at the right. **Colour** (palette icon,
> `ic_palette`) is a group, not a tool: tapping it opens no sheet and swaps the row for layer 2, **Back**
> (pinned at the left), **RGB**, **HSL**, **Curves**, then Compare. Colour carries the "changed" dot while
> RGB, HSL or Curves holds a change, and each of the three keeps its own dot. Back closes an open colour
> sheet (the change is kept) and returns to layer 1; tapping Colour while another tool's sheet is open
> closes that sheet; while Compare is held every entry but Compare is disabled. The phone's Back
> gesture is unchanged (it closes an open sheet, then asks to discard or leave; it does not step back to
> layer 1). RGB's icon is now `ic_instant_mix`; HSL and Curves keep theirs. Whether layer 2 shows is a
> `rememberSaveable` flag in `EditToolRow` (it survives rotation; it is not in the ViewModel and is lost
> when Stickers, Text or Date replace the row). Back and the sideways slide are the shared
> `GroupBackEntry` and `groupRowTransition`, also used by Beautify. Nothing about how RGB, HSL and
> Curves process the photo changed. The "row" sentences in the older notes above describe the order
> before this change; the notes on RGB, HSL and Curves about their place "between" other tools are
> superseded by this one.

---

## What it is

Edit is Kanso's general editor. At the bottom of the screen sits a row of tools, **Crop**,
**Rotate**, **Adjust**, **Colour** (which swaps the row for Back, RGB, HSL and Curves), **Filters**,
**Stickers**, **Text** and **Date**; tapping a tool opens its sheet just above the row, as in Adobe
Lightroom (tapping it again closes it). The Stickers sheet is the exception: it takes the row's place
(see below). Each tool is 60 dp wide with a 28 dp icon, so the eight entries and Compare do not all fit on a
411 dp wide phone: Stickers is partly cut and Text and Date are reached with a swipe. The Colour
layer (Back, three tools, Compare) fits without scrolling. **Compare** stays
pinned at the right, behind a thin divider, and never scrolls away. **Undo** (↶) and **Redo** (↷),
two arrow icons, sit at the top right next to **Done**; Undo is there on the other tools too, Redo
only on Edit.
While you edit, the photo on screen is a live preview drawn by Compose; the full picture is
rendered once, in plain Kotlin on the phone's processor, when you tap **Done** (with no control
used there is nothing to render, and the decoded original is handed over). No model and no
network are used. You can:

- **Crop**: choose Free, 1:1, 4:3, 3:4, 4:5, 16:9 or 9:16. While the Crop sheet is open the photo becomes
  a cropper (from the `core/imagecrop` module): drag a handle to move an edge or a corner, drag inside
  the rectangle to move it, pinch to zoom. The cropper shows the decoded photo with the stickers
  drawn in, without the turns, flips, filter or slider colours. A fixed shape sets the largest centred rectangle of that
  shape and locks the handles to it; Free unlocks them and keeps the rectangle as it is.
- **Rotate**: Turn left or Turn right a quarter turn, Flip across (left to right) or Flip upside
  down (a second tap flips back).
- **Adjust**: four chips, **Brightness**, **Contrast**, **Saturation** and **Warmth**, and one
  slider for the chosen chip, as in Adobe Lightroom. Each runs from -100 through 0 (no change) to
  +100, with the number beside the name. Choosing a chip changes nothing in the photo; the chip
  chosen last is shown again when Adjust is reopened. The photo follows the finger
  while it drags, and the slider stays where it is let go.
- **RGB**: three colour circles with no text, **Red**, **Green** and **Blue**, drawn exactly like
  HSL's colour-range circles and in the same colours as HSL's Red, Green and Blue, and one
  slider for the chosen circle, tinted in that channel's colour. Each runs from -100 (the channel is
  gone: less red turns the photo cyan, less green magenta, less blue yellow) through 0 (no change)
  to +100 (the channel doubled). It works like Adjust in everything else: Red is chosen the first
  time, the chip chosen last comes back, choosing a chip changes nothing, each drag is one Undo
  step, and there is no reset button and no Opacity slider.
- **HSL**: a row of nine colour circles (Red, Orange, Light green, Green, Light blue, Blue, Navy
  blue, Purple, Pink), a row of three chips (**Hue**, **Saturation**, **Lightness**) and one slider,
  from -100 through 0 (no change) to +100, for the chosen range and chip, so there are 27 values in
  all. The slider's track is a gradient of the chosen colour. Red and Hue are chosen the first time;
  the circle and chip chosen last come back; choosing one changes nothing in the photo; each drag
  is one Undo step; there is no reset button. The photo follows the finger. Greys are not changed.
- **Curves** (partial): chips **RGB**, **Red**, **Green** and **Blue** pick the curve (RGB first; the last
  choice is remembered; no Undo step) and a graph shows it, a straight diagonal at first. Tap empty space
  to add a point (up to 10), drag a point to move it, long press a middle point to remove it, and
  press **Reset** (disabled while the chosen curve is straight) to straighten that curve only. The photo on screen
  follows the finger, even with no filter chosen, and matches the picture Done renders; a sticker
  under a finger is drawn without the curve until it is let go.
- **Filters**: None or one of eight looks, Light, 1977, Kelvin, Nashville, Walden, Hudson, Amaro
  and Autumn, each a text-only pill (`editpanel/FilterChips.kt`, Material `FilterChip`s, no icon), with an **Intensity** slider (0 to 100) that is usable while a filter is chosen. Choosing a filter
  **replaces** the one before; filters never pile up.
- **Stickers** (decision record: [ADR-004](../adr/ADR-004-sticker-is-part-of-the-photo.md)): a sheet that replaces the row of tools, with a cross at the left of a
  tick in its top row, four category tabs (Food 17, Mood 17, Quote 6, Emoji 20 bundled pictures)
  and a grid of three columns that scrolls. A tap on a picture adds that sticker at the middle of
  the picture you see (after the crop, turns and flips), a third of its width, selected; each tap
  is one Undo step and a photo holds at most 20 ("A photo can hold 20 stickers at most" appears when you try a 21st).
  The tick keeps the changes; the cross, and the system **Back**, throw away everything done since
  the sheet opened, Undo steps included. The stickers are drawn on the photo on screen. While the
  sheet is open a finger put down on one selects it (a touch on empty photo selects none): a frame with four corner
  buttons, top-left delete, top-right duplicate (a copy 4 percent of the shown width right and
  down), bottom-left mirror, bottom-right a handle that resizes and turns it together. One finger
  drags, two fingers pinch and twist; a newer sticker lies on top. Each add, delete, duplicate,
  mirror and each whole drag, pinch or handle pull is one Undo step; selecting is not. Closing the
  sheet by tick or cross selects none. Done draws them into the saved picture. The live preview
  shows them with the filter and colour sliders (the one you are moving follows your finger with
  the sliders but no filter, and joins the filtered picture when you let go), the cropper shows
  them, and Compare shows the photo without them.
- **Undo** (top right, next to Done, in reach whatever sheet is open) and **Compare** (pinned at the
  right of the tool row; the Stickers sheet replaces the row while it is open, so Compare is out of
  reach until the sheet closes). Undo takes back one action at a time; Compare is a switch that shows the untouched
  photo while it is on and disables the row of tools, the sheet's controls, the cropper and Undo.
  Done stays usable while comparing and renders the edit, not the original.
- **Done** at the top right renders the picture (with no control used, it hands over the decoded
  original) and opens the Result screen (Share, Save, Go home).
  Nothing is written to the gallery until Save is tapped there.

Each tool with a change carries a small dot on its entry in the row.

## How to use it

- Reach it from the **Edit** shortcut on Home (then pick a photo in the gallery), by tapping a
  **Recent edits** card on Home, from the round **Camera** button beside the tab bar (a kept shot opens
  in Edit), or with **Edit** in the My edits photo viewer. Edit has no carousel banner and no
  Replace chip.
- Tap a tool in the row to open its sheet, change what you like, open another tool; the photo never changes
  size: the sheet area above the row keeps one fixed height whether a tool is open or not (with
  none open it says "Pick a tool to start editing").
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
    U --> L["LivePreviewImage: Canvas crops, turns, flips; ColorFilter.colorMatrix for the sliders and RGB channels"]
    U -->|filter, intensity or sticker changed| Q["conflated Channel of preview requests"]
    U -->|HSL value changed| Q
    Q --> R["FilterPreviewRenderer: 640 px copy of the original, stickers (not the touched one), filter, then EditHsl.apply, Dispatchers.Default"]
    R -->|ToolUiState.filterPreview| L
```

1. **The screen and its state.** The Edit screen is
   [`ui/tool/ToolScreen.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/ToolScreen.kt)
   (`ToolScreen`, `ToolContent`) with
   [`ui/tool/components/editbottombar/EditBottomBar.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/components/editbottombar/EditBottomBar.kt)
   for its layout: `EditPhotoArea`
   ([`ui/tool/components/EditPhotoArea.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/components/EditPhotoArea.kt)),
   which draws the photo card
   ([`ui/tool/components/photocard/PhotoCard.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/components/photocard/PhotoCard.kt))
   or the cropper, filling the `Scaffold`'s body; and `EditBottomBar`, the `Scaffold`'s `bottomBar`, holding the
   [`ui/tool/components/editbottombar/edittoolrow/EditToolRow.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/components/editbottombar/edittoolrow/EditToolRow.kt)
   at the very bottom and the open tool's sheet just above it. The sheet area is one fixed
   height (220 dp, `EDIT_SHEET_HEIGHT`; 136 dp until 2026-11-06), open or not, so the photo above never changes size; every tool's controls fit
   it (Adjust shows four sub-tool chips and one slider, see
   [`ui/tool/AdjustSlider.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/AdjustSlider.kt);
   the chosen chip is `ToolUiState.adjustSlider`, set by `ToolViewModel.chooseAdjustSlider`; RGB
   does the same with three channel chips, see
   [`ui/tool/model/RgbChannel.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/model/RgbChannel.kt),
   `ToolUiState.rgbChannel` and `ToolViewModel.chooseRgbChannel`), and
   very large text scrolls inside it. It is an ordinary `Surface` cross-faded with
   `AnimatedContent`, not a
   Material `ModalBottomSheet`, because a modal sheet dims the photo and swallows touches, and the
   row of tools must stay usable. Its ViewModel is
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
   flips, four sliders and three colour channels (red, green, blue) from -1 to 1 (0 is no change),
   the 27 HSL values (`EditSettings.hsl`, a
   [`HslAdjustments`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/HslAdjustments.kt):
   hue, saturation and lightness for each of nine `ColourRange`s, each -1 to 1), the filter and its
   intensity (0 to 1), and the stickers. It is
   held, with its undo stack, by
   [`data/edit/edit/EditSession.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditSession.kt).
   No control ever asks for a change on top of the last result: each re-renders from the original
   photo. What a control hands the ViewModel differs, though. A turn, a flip, a filter chip and a
   slider move pass a complete new `EditSettings`; a crop chip passes only a `CropRatio`, which
   `EditSession.cropRatioChosen` turns into the rectangle; the cropper passes a `CropRect`; and a
   slider release (`commitSettings`) passes nothing, it just closes the drag as one undo step:
   - [`ui/tool/components/toolpanel/editpanel/EditPanel.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/components/toolpanel/editpanel/EditPanel.kt)
     draws the open sheet from the session's settings (it keeps no state of its own, so a slider
     reopens where it was left). A turn, a flip or a filter chip calls `changeSettings`; a crop
     shape calls `chooseCropRatio`; a slider calls `previewSettings` while it moves and
     `commitSettings` when let go.
   - [`ui/tool/components/EditCropper.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/tool/components/EditCropper.kt)
     wraps `core/imagecrop`'s `CropperPreview` (package `com.mr0xf00.easycrop`,
     [`core/imagecrop/src/main/java/com/mr0xf00/easycrop/ui/CropperPreview.kt`](../../core/imagecrop/src/main/java/com/mr0xf00/easycrop/ui/CropperPreview.kt)).
     It shows the decoded photo with the stickers drawn into a copy of it (on the main
     thread, inside composition; if copying the photo runs out of memory it shows the photo without
     stickers), un-turned, un-flipped and unfiltered, because the crop is stored as
     fractions of the original and the render crops before it turns. When the last finger lifts,
     the rectangle becomes fractions and, if it differs from the session's crop by more than a tenth
     of a percent, goes to `commitCrop` once. A session crop or shape that changes (a chip, Undo)
     moves the cropper's rectangle to match, and a fixed shape locks its aspect (`CropState.aspectLock`,
     switched off while the rectangle is moved so the new shape is not bent back to the old one).
     While Compare is on the cropper is not drawn at all (the plain photo shows); while Done is
     rendering, a layer on top of the cropper takes every touch, so it ignores them. The cropper, the sheets and the row all run on the main thread, as Compose draws;
     the only pixels among them are the cropper's sticker bake (above).
   - `EditSession.change` pushes the old settings on the undo stack (an unchanged value adds
     nothing); `preview` follows the finger without an entry and `commit` turns the whole drag into
     one entry; `cropRatioChosen` sets the largest centred rectangle of the shape, fitted on its
     side when the photo has an odd number of quarter turns, so the turned picture comes out in the
     shape chosen; `undo` goes back to the last entry. There is no redo.
4. **The live preview.** The photo card draws
   `LivePreviewImage` (in `ui/tool/components/photocard/LivePreviewImage.kt`) whenever Compare is off and the Crop sheet
   is closed. It is a Compose `Canvas` that, every frame, takes only the crop rectangle from the
   bitmap (rounded exactly as the final render rounds it), turns it, mirrors it, fits it inside the
   card, and draws it through `ColorFilter.colorMatrix` with the matrix of the four sliders and the three RGB channels from
   [`data/edit/edit/EditColourMatrix.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditColourMatrix.kt),
   the same numbers Done uses. With every slider at 0 no colour filter is set. No pixel is
   recomputed on the CPU for crop, turn, flip or the sliders.
5. **The filter preview.** A filter is a look-up per pixel that Compose cannot draw, so the
   ViewModel computes it. When the filter or its intensity changes (`requestPreview`), the settings
   go into a `Channel(Channel.CONFLATED)`: one collector coroutine renders one request at a time,
   and a newer request replaces one still waiting, so a fast intensity drag never queues up work.
   [`data/edit/edit/FilterPreviewRenderer.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/FilterPreviewRenderer.kt)
   runs the real `EditProcessor` with the filter, its intensity and every sticker except the one
   being touched, on `Dispatchers.Default`, on a copy of
   the original shrunk once to at most 640 px (`PREVIEW_EDGE`). It also renders when there is no
   filter but there are stickers or an HSL change. After the filter it runs `EditHsl.apply` on the
   copy (the same function Done calls) when any of the 27 HSL values is not 0, so a drag of the HSL
   slider changes the photo on screen with no filter chosen; `LivePreviewImage` then draws the
   colour matrix on top, so the order is filter, HSL, colour matrix, as in Done. `requestPreview` is called, among other places, from `addSticker`,
   `deleteSticker`, `duplicateSticker`, `flipSticker`, `commitSettings`, `confirmStickers` and
   `cancelStickers` (also `changeSettings`, `previewSettings`, `chooseCropRatio`, `undo`, `load`,
   `openTool` and `setComparing`).
   The result becomes
   `ToolUiState.filterPreview`, and `LivePreviewImage` draws that bitmap instead of the original,
   cropping, turning and colouring it as above. A crop, turn or slider change keeps the same filter
   preview. A preview that fails leaves `filterPreview` empty, so the card draws the plain photo (no resting stickers either), with no message.

### The Stickers sheet

Tapping Stickers calls `ToolViewModel.openTool(EditTool.STICKERS)`. Opening it keeps a snapshot
of the whole `EditSession` (settings and Undo steps; it is immutable, so keeping it costs nothing).
`EditBottomBar` then draws the sheet in place of the row, as tall as the row plus the fixed
sheet area, so the photo's bounds do not change. `ToolScreen` fills it with `EditPanel`, which
shows `StickersPanel`: `StickersSheetTopBar` (cross left, tick right, 48 dp targets),
`StickerCategoryChips` (a `LazyRow` of `FilterChip`s, the chosen one `selected`) and a
`LazyVerticalGrid` of `StickerTile`s, three columns, from `StickerCatalog.inCategory`. All on the
main thread; no pixel is computed.

Adding a sticker:

```mermaid
flowchart TD
    T["Tile tapped (StickerTile)"] --> A["ToolViewModel.addSticker"]
    A --> G{"Compare on, Done running,<br/>photo not decoded yet,<br/>or 20 stickers already?"}
    G -->|"Compare, Done or no photo yet"| X["ignored"]
    G -->|"20 stickers"| L["stickerLimitShown = true: StickerLimitMessage for about 2.5 s"]
    G -->|no| M["StickerMapping: middle of the picture you see -> fractions of the original photo"]
    M --> S["EditSession.addSticker: one Undo entry, newest on top"]
    S --> P["publishSession: only prunes the selection; addSticker then sets selectedStickerKey = the new sticker"]
    P --> R["preview copy re-rendered with the new sticker (FilterPreviewRenderer)"]
```

One gesture, one Undo entry:

```mermaid
flowchart TD
    D["Touch on the photo while the sheet is open: StickerGestures finds the topmost sticker under the finger"] --> SEL["Finger down on a sticker: selectSticker (no Undo entry), so a drag selects too"]
    D --> MV["Finger or fingers move: previewStickers (no Undo entry)"]
    MV -->|"first change"| TK["touchedStickerKey set: this sticker drawn by LivePreviewImage, sliders but no filter"]
    MV --> MV
    MV -->|"last finger up"| C["commitSettings: the whole gesture is ONE Undo entry"]
    C --> CL["touchedStickerKey cleared: sticker joins the filtered preview copy"]
    CL --> U["Undo takes the whole gesture back in one step"]
```

- **Add.** `addSticker` asks `StickerMapping` for the sticker at the shown picture's centre (the
  mapping inverts what Done does: crop, then turn, then flip), adds it with `EditSession.addSticker`
  (one Undo step) and selects it (`selectedStickerKey`). The processor recipe passed in is the one
  from `editProcessorFor(context)` and becomes the recipe Done renders with. At 20 stickers the
  settings are untouched and `stickerLimitShown` shows `StickerLimitMessage`
  (`ui/tool/components/StickerLimitMessage.kt`) on the photo's top edge with the text "A photo can
  hold 20 stickers at most" (`stickers_limit_message`, `res/values/strings_stickers.xml`) for
  about 2.5 s, then `onStickerLimitMessageShown` clears it.
- **Tick** (`confirmStickers`): forgets the snapshot, closes the sheet, clears the selection.
- **Cross and system Back** (`cancelStickers`; `onBackPressed` calls it first when the sheet is open):
  publishes the snapshot again, so settings and Undo steps are exactly as at opening, closes the
  sheet and clears the selection. Back does not ask "Discard your changes?" here.
- **While Compare is on or Done renders** (`comparing`, `finishing`) the tick, the cross and Back
  do nothing: Back is consumed (the screen stays) but cancels nothing, so Done draws the stickers
  as they were when it was tapped.
- **Undo** clears `selectedStickerKey` when the undone step removed the selected sticker.
- **On the photo.** `PhotoCard` draws a `StickerOverlay` over the live picture (not while Compare
  is on). Touches work only while the sheet is open, Compare is off and Done is not running
  (`stickerEditingOpen`). `StickerGestures` picks the topmost sticker under the finger and calls
  `selectSticker`; a drag, pinch or handle pull calls `previewStickers` (no Undo entry) and its last
  finger up calls `commitSettings` (one Undo entry). `StickerFrame` and `StickerCornerButton` call
  `deleteSticker`, `duplicateSticker` (offset 4 percent of the shown width, via `StickerMapping`)
  and `flipSticker`; the handle resizes and turns about the centre. `selectedStickerKey` lives in
  `ToolUiState`, and `publishSession` clears it when its sticker is gone.
- **Drawing.** Done draws the saved picture's stickers (`EditProcessor`, `DrawableStickerPictures`),
  see below. On screen, `StickerOverlay` draws no pictures (frame, buttons, touches only).
  `FilterPreviewRenderer.render(requested)` is called after `ToolViewModel.requestPreview` leaves
  the touched sticker out of the settings; it draws every other sticker into
  the small preview copy before the filter (so filter and Intensity act on them; it renders with
  no filter when stickers exist, null when nothing to draw). The request key is filter, intensity
  and untouched stickers. The first `previewStickers` that changes a sticker sets
  `ToolUiState.touchedStickerKey`; `LivePreviewImage` draws that sticker in the same crop, turn
  and flip, clipped to the photo, with the slider colour matrix and no filter, so it follows the
  finger. `commitSettings`, the tick or cross, `openTool` and `setComparing` clear the key
  (`publishSession` clears it only when that sticker is gone) and it joins the
  filtered copy (one small re-render at gesture start, one at finger up, none during the drag).
  `EditCropper` (`withStickersDrawn`) bakes stickers into a copy of the original, same mirror,
  turn and scale as `EditProcessor`, so they show while cropping. Compare shows none.

### Done, the full render and the Result screen

```mermaid
flowchart TD
    D["Done tapped (ToolViewModel.done)"] --> N{"Any Edit control used yet?"}
    N -->|no| H0["RenderedPhotoStore.put(decoded original, photo Uri)"]
    N -->|yes| F["finishing = true: spinner in Done"]
    F --> W["WorkingSize.fit(original, 3000 px) on Dispatchers.Default"]
    W --> E["EditProcessor: stickers first, then crop, turn, flip, filter, HSL (EditHsl.apply), colour matrix (Adjust and RGB), opaque"]
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
   then renders the neutral settings (a fresh picture of at most 3000 px, which Save writes as a
   JPEG) rather than handing over the file. Compare does not stop Done: it renders the settings,
   not the original.
2. **The full render.** Its input is the decoded original ("The photo", in the section above: at
   most twice the screen's longest edge, not the file at full size). `renderFull` shrinks it once
   to at most 3000 px on its longest edge (`WorkingSize.EDIT` in
   [`data/edit/WorkingSize.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/WorkingSize.kt))
   and runs
   [`data/edit/edit/EditProcessor.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditProcessor.kt),
   all on `Dispatchers.Default`. The processor always works from the original with the whole
   settings, so nothing is stacked. On the pixels as one `IntArray`, in this order:
   0. **Stickers** (new in Run 10; the Stickers sheet adds them, see "The Stickers sheet" above,
      and [ADR-004](../adr/ADR-004-sticker-is-part-of-the-photo.md) for why they go first): the settings hold up to 20
      (`EditSettings.stickers`, `MAX_STICKERS`), placed as fractions of the original photo. They
      are drawn first, right after the kept rectangle is read out, so the crop cuts them and the
      turns, flips, filter and sliders act on them with the photo (order: stickers, crop, turns,
      flips, filter, sliders). Each add, delete, duplicate, flip or whole drag is one Undo step
      (`EditSession`); a 21st add or duplicate changes nothing.
   1. **Crop**: copies only the kept rectangle (fractions turned into pixels, rounded, edges put in
      order, at least 1 x 1). A fixed shape is no special case: choosing it already set the
      rectangle. Progress 0.25.
   2. **Turn** by moving every pixel (no blurring; four turns give the same pixels back), then the
      **flips**. Progress 0.5.
   3. **Filter**, when one is chosen at an intensity above 0: each pixel goes through the filter's
      look-up, then is mixed with its own colour by the intensity (`filterPixel`, shared with the
      preview). Progress 0.75. The filter comes before the sliders so the sliders fine-tune the
      filtered look.
   3b. **HSL**, when any of the 27 HSL values is away from 0 (`EditSettings.hasHslChange`):
      `EditHsl.apply` ([`data/edit/edit/EditHsl.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditHsl.kt))
      on the pixels in place. Each pixel is converted to hue, saturation and lightness; the
      amounts of the colour ranges its hue falls between are blended (a range weighs 1 at its
      centre and falls as a cosine squared to 0 at the next centre, centres 40 degrees apart); that
      blend is multiplied by a gate that is 0 for greys and 1 for clear colours; hue then turns by
      up to 20 degrees, saturation is multiplied by `1 + amount` (kept in 0..1) and lightness moves
      toward 0.95 or 0.05; then the pixel is converted back with its alpha kept. It comes after the
      filter (HSL fine-tunes the filtered look) and before the colour matrix, and it colours the
      layers with the photo. With all 27 values at 0 it is skipped. The full reasoning is in
      [ADR-008](../adr/ADR-008-hsl-is-computed-on-the-cpu-by-one-formula.md).
   3c. **Curves**, when any tone curve is bent (`EditSettings.hasCurveChange`): `EditCurves.apply`
      ([`data/edit/edit/EditCurves.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditCurves.kt))
      turns each curve into a 256-entry table (monotone cubic interpolation, `ToneCurve.table`) and
      sends each pixel's red, green and blue through their own channel's table and then the RGB
      (master) table, alpha kept; that is the whole step, with no colour-space conversion.
      It comes after HSL and before the colour matrix. All curves straight: skipped.
      See [ADR-009](../adr/ADR-009-curves-run-on-the-cpu-after-hsl.md).
   4. **Colour**, when a slider or an RGB channel is away from 0: one 4 x 5 matrix from
      `EditColourMatrix.of`, in this order: contrast scales each channel around mid-grey 128 (up to
      x2, down to x0.2), saturation uses `setSaturation(1 + s)`, warmth adds up to 40 (of 255) to
      red and takes it from blue, the **RGB channels** multiply red, green and blue each by
      `1 + value` (x0 to x2, no offset, so black stays black), and brightness adds up to 80 to
      every channel. RGB comes after the filter (it fine-tunes the filtered look) and before
      brightness (lifted blacks stay a neutral grey). Values are rounded and clamped to 0..255.
   5. Every pixel's alpha is set to 255 and the result is a new `ARGB_8888` bitmap with
      `setHasAlpha(false)`. Progress 1. (Done shows no progress ring; only the spinner.)

   The turn and flip loops check for cancellation on every row, and the filter and colour loops
   every 65,536 pixels, so leaving Edit (which clears its ViewModel and cancels its coroutines)
   stops a render early.
3. **Where the filters come from.**
   [`data/edit/edit/FilterPreset.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/edit/FilterPreset.kt)
   names eight images, all read from the app's assets at run time (no download). Seven ship inside
   the engine's prebuilt libraries and are merged into the app's assets at build time: Light is lookup/lookup_light.png from
   `libs/imageprocessor.aar`, a 512 x 512 colour cube; 1977, Kelvin, Nashville, Walden, Hudson and
   Amaro are curve strips under filter_materials/map/ from `libs/editimage.aar`. Autumn is Kanso's
   own 512 x 512 colour cube, `app/src/main/assets/filters/autumn.png`, taken from the Photo Edit
   Toolbox (generated by its `tools/generate_filters.py`): a warm, deep look where greys and pale
   blues turn brown, warm colours get richer, shadows darken and contrast rises.
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
     quality 100, at its size, into `Pictures/Kanso`. A photo on which no Edit control was used (Done with
     no control touched) is saved from its own file instead (its bytes copied unchanged, or turned upright and
     written as a new JPEG at quality 95 if it carries an EXIF turn; a
     turned photo too large to turn in memory is copied as it is with its tag kept, and Result
     shows the "kept orientation" message). Such a save is always named `.jpg` and typed
     `image/jpeg`, whatever the photo's own format, so an untouched PNG, WebP or HEIC photo
     copied byte for byte is saved as that PNG, WebP or HEIC data under a `.jpg` name and the
     `image/jpeg` type (a known defect, not fixed yet). A
     picture is saved at most once; the button then reads "Saved". On Android 8.1 and 9 the
     storage-write permission is asked first.
   - **Share** writes a copy of the picture on screen in the cache's `shared/` folder (JPEG
     quality 100 for an opaque picture, PNG for one with an alpha channel; for an untouched photo
     this is the decoded, screen-sized copy, not the file) on `Dispatchers.IO` and sends it through FileProvider and Android's share sheet with
     a one-off read grant ([`ui/result/SharePhoto.kt`](../../app/src/main/java/com/phongkaster/kanso/ui/result/SharePhoto.kt)).
   - **Go home** clears the back stack down to Home, which ends the session; leaving Result also
     clears the store.

**What happens on failure:**

- Done running out of memory shows "This photo is too large to edit on this phone. Try a smaller
  one. Your photo is unchanged."; a filter image that cannot be read, or any other exception,
  shows "Something went wrong and the edit was not made. Your photo is unchanged." The message sits
  at the top of the photo card and a tap hides it. The failure stays in the screen's state until
  that tap or the next Done (changing a setting, Undo and closing a sheet leave it in place). With the Crop sheet open
  the cropper replaces the photo card, so at first no message is visible: the spinner stops and
  Done can be tapped again. The message appears as soon as the photo card is shown again: when
  the Crop sheet is closed, or at once when Compare is turned on (Compare shows the photo card in
  place of the cropper). In every case the person stays on Edit with every setting and Undo step
  kept, and nothing is handed over.
- A filter preview that fails leaves `filterPreview` empty, silently: the card draws the original
  photo, with no filter and no resting stickers on screen; the saved result is unaffected and Done
  reports a real failure.
- A photo that cannot be decoded shows "This photo cannot be opened. Go back and pick another one."
- A failed Save shows "The photo could not be saved. Try again."; a refused storage permission
  shows that nothing was saved.
- After the process is killed, Result's store is empty and it shows "Nothing to show"; the Edit
  session lived only in the ViewModel's memory, so Edit reopens on the photo with no changes.

## Limits, from the code

- **Size.** The photo is decoded at most twice the screen's longest edge, and Done renders at most
  **3000 px** on the longest edge, so a 4000 x 3000 photo comes out at most 3000 x 2250 and a 2K
  photo keeps its size. The saved file is a **JPEG at quality 100**. Done with no control used saves the original file instead (its
  bytes copied unchanged, or re-encoded upright as a JPEG when it carries an EXIF turn; if it is
  too large to turn in memory, copied as it is with its tag kept and Result shows the "kept
  orientation" message). That save is always named `.jpg` and typed `image/jpeg`, so an untouched
  PNG, WebP or HEIC photo keeps its own format inside a file named `.jpg` (a known defect). Once a control was used, even if
  every change was undone, Done renders a new JPEG of at most 3000 px.
- **Preview size.** The filter preview is computed on a copy at most **640 px** on its longest
  edge, so on screen a filtered photo holds less detail than the picture Done renders.
- **RGB.** Each channel runs from x0 to x2. At +100 the bright parts of a photo can burn out
  (clamped at 255). The owner tried RGB on the vivo V2408A (2026-11-03), found it works as
  expected and kept the x2 maximum; on 2026-11-06 the owner checked it on the Galaxy A13 too.
- **HSL.** Hue turns at most 20 degrees at +-100; greys and near-greys are never changed; the
  preview is computed on the 640 px copy, Done on up to 3000 px. Checked by the owner on the Galaxy A13 and vivo V2408A
  (2026-11-06): works as expected. Its time per frame on the Galaxy A13 is not measured. A layer being dragged under a
  finger is shown without HSL until it is let go, as it is shown without the filter.
- **Curves.** Partial: the RGB, Red, Green and Blue curves are editable, at most 10 points, points at least 0.02
  apart. The saved picture has the curve and the live preview follows it. Not measured on
  any phone.
- **One filter at a time.** Choosing a filter replaces the earlier one. Moving from one filter to
  another keeps the intensity; coming from None it starts at 100; None resets it.
- **Crop.** The cropper shows the photo, with the stickers drawn in (without them if copying the
  photo runs out of memory), un-turned, un-flipped and unfiltered. A turn after a crop turns
  the cropped picture with it (a 16:9 crop turned once comes out 9:16).
- **Stickers.** At most **20** (`EditSettings.MAX_STICKERS`); a 21st add or duplicate changes
  nothing and shows a message for about 2.5 seconds. During a gesture or handle pull a sticker's
  width is kept between 5 and 100 percent of the shown width; a later tighter crop can push it past 100. While a sticker is being moved it shows no filter in the preview
  (it does in the saved picture). A sticker wholly outside the crop draws nothing in the saved
  picture. A picture that cannot be decoded is skipped in Done. The sheet's cross and system Back
  throw away what was done since it opened, Undo steps included.
- **Undo** takes back one action at a time (a chip, a turn, a flip, a crop ratio, one finished crop
  gesture, one whole slider drag, or one sticker added, deleted, duplicated, mirrored or whole
  gesture), whichever sheet it came from.
- **Redo** (Edit only; `EditSession.redo`, `ToolViewModel.redo`, `topbar/RedoButton.kt`) brings back
  what Undo took back, one action at a time, in the order it was undone; a redone action can be
  undone again. Any new action after an Undo (a change, or a drag or gesture that moved something)
  empties the redo list, as in every editor. Redo stores settings, never pixels, so it costs no
  memory to speak of. It is off while Compare is on or Done renders, like Undo.
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
[`RgbSheetTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/RgbSheetTest.kt),
[`RgbToolTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/RgbToolTest.kt),
[`RgbMatrixTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/RgbMatrixTest.kt)
(x0 / x1 / x2, black stays black, after warmth and before brightness), with the slider tints'
contrast in [`a11y/RgbTintContrastTest.kt`](../../app/src/test/java/com/phongkaster/kanso/a11y/RgbTintContrastTest.kt),
[`HslFormulaTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/HslFormulaTest.kt),
[`HslRangeWeightTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/HslRangeWeightTest.kt),
[`HslParityTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/HslParityTest.kt)
(the preview and Done agree pixel by pixel),
[`HslToolTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/HslToolTest.kt),
[`HslSheetTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/HslSheetTest.kt), with the circles'
mark contrast in [`a11y/ColourRangeMarkContrastTest.kt`](../../app/src/test/java/com/phongkaster/kanso/a11y/ColourRangeMarkContrastTest.kt),
[`FiltersSheetTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/FiltersSheetTest.kt),
[`EditToolRowTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/EditToolRowTest.kt),
[`EditLayoutTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/EditLayoutTest.kt),
[`StickersPanelTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/StickersPanelTest.kt)
(chips, three-column grid, 17/17/6/20 tiles),
[`StickerAddTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/StickerAddTest.kt)
(add at the shown centre, the limit, tick, cross and Back, the shared recipe),
and the other sticker tests in the same folder: `StickerCatalogTest.kt`, `StickerSessionTest.kt`,
`StickerMappingTest.kt`, `StickerGeometryTest.kt`, `StickerSelectionTest.kt`,
`StickerGestureTest.kt`, `StickerFrameTest.kt`, `StickerOverlayTest.kt`,
`StickerLivePreviewTest.kt`, `StickerPreviewRendererTest.kt`, `StickerCropperTest.kt`,
`StickerCompareTest.kt` and `EditProcessorStickerTest.kt` (the order and the blend in Done). The
Stickers screenshots (pictures 69 to 82 in `app/src/test/screenshots/`) are recorded only by
[`screenshots/StickerScreenshotTest.kt`](../../app/src/test/java/com/phongkaster/kanso/screenshots/StickerScreenshotTest.kt).
The Edit tests also include
[`DiscardDialogTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/DiscardDialogTest.kt)
and [`EditToolTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/edit/EditToolTest.kt),
which capture no picture.
Done and the Result screen are covered by
[`tool/DoneResultTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/DoneResultTest.kt),
[`result/ResultViewModelTest.kt`](../../app/src/test/java/com/phongkaster/kanso/result/ResultViewModelTest.kt)
and [`result/ResultScreenTest.kt`](../../app/src/test/java/com/phongkaster/kanso/result/ResultScreenTest.kt).

## Known limits and open items

- The limits above.
- Checked by the owner on the Galaxy A13 and vivo V2408A (2026-11-06): every tool runs as expected.

[Back to README](../../README.md)
