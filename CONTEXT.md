# Kanso glossary

The words this code uses, what each one means here, and where it lives. Paths are under
`app/src/main/java/com/phongkaster/kanso/`.

**Tool** - One of the five things Kanso does to a photo: `EDIT`, `REMOVE_BACKGROUND`, `ENHANCE`,
`BEAUTIFY`, `ARTISTIC`. An enum with a label and an icon; it travels in navigation keys, and each
tool opens its own tool screen (`ui/tool/ToolScreen.kt`, `ui/tool/ToolViewModel.kt`).
`navigation/Tool.kt`. *Not* an `EditTool`, and not the camera (the camera makes a photo, so it has
its own `Camera` key).

**EditTool** - One of the seven groups of controls inside the Edit tool's screen: `CROP`, `ROTATE`,
`ADJUST`, `FILTERS`, `STICKERS`, `TEXT`, `DATE`. Text and Date are tools of their own with their own
sheets, not sections of the Stickers sheet. Each is an entry in the row under the photo (`ui/tool/component/editbottombar/edittoolrow/EditToolRow.kt`) and
opens one sheet; only one sheet is open at a time (a layer sheet, Stickers, Text or Date, takes the
row's place while open). Holds no strings or icons.
`data/edit/edit/EditTool.kt`. *Not* a `Tool`: all five live inside `Tool.EDIT`.

**Layer** - One thing placed on the photo in Edit, which the person can select, move,
pinch and turn. A layer has one of three kinds: a *picture* (a **Sticker**, added by the Stickers
tool), *text* (added by the Text tool) or a *date* (added by the Date tool). All layers sit in one
list, oldest first, so the stacking order runs across every kind; selecting a layer brings it to the front. A photo holds at most 20 layers in total. Every layer, of either kind, has a size (1-100, relative to the
photo) and an opacity (10-100%), each set by a slider in its tool's sheet; pinching a layer
changes the same size. A layer is part of the photo (ADR-004, ADR-005, ADR-006): crop, turns, flips,
filter and sliders act on it. `data/edit/edit/Layer.kt`, `LayerContent.kt`. *Not* a layer drawn over the finished picture (the
option ADR-004 rejected), and not Compose's `graphicsLayer`.

**Sticker** - One ready-made picture placed on the photo in Edit: a **Layer** of the *picture* kind
(`LayerContent.Picture`, a catalogue picture id); its place, width, turn, mirror flag and opacity
are the layer's. It counts towards the 20 layers (`EditSettings.MAX_LAYERS`) in
`EditSettings.layers`. Which layer is selected is screen state (`ToolUiState.selectedLayerKey`),
not part of the layer. *Not* an entry of the catalogue (a `CatalogSticker`), which is only a
picture to choose.

**Text (text layer)** - Words typed onto the photo in Edit: a **Layer** of the *text* kind
(`LayerContent.Text`): the words (at most 200 characters), an opaque colour, a font (`TextFont`:
the phone's own or one of five bundled), an alignment (`TextAlignment`) and a wrap width in
letter heights, and an optional **background box** behind the words (any colour, see-through
too). An empty text is never kept: it is dropped when it loses the selection. A run of
typing is one Undo step. *Not* a sticker: it has its own tool and sheet (ADR-005).

**Date (date layer)** - A day and time written onto the photo in Edit: a **Layer** of the *date*
kind, which remembers the moment (date and time) and the **date format** it is written in, never
the words themselves. It starts at now; the person changes the moment with a date picker and a time
picker. It has a colour, a font and a background box like a text, but one line, so no alignment,
and no flip. It has
its own tool, Date, after Text in the tool row, and counts towards the 20 layers. *Not* a text: its
words cannot be typed, only its moment and format chosen.

**Date format** - One of the 20 fixed ways a date layer writes its moment, always in English: 14
date-only (from `Wed, 28 October 2026` to the film-camera stamp `'26 10 28`) then 6 with a time
(`14:30`, `2:30 PM`, ...). Tapping a selected date layer moves it to the next format, after the last
comes the first. *Not* a font or a style: the same format can be drawn in any colour and font.

**Sticker catalogue (`StickerCatalog`)** - The 60 pictures the Stickers sheet offers, in four
categories (Food 17, Mood 17, Quote 6, Emoji 20; `StickerCategory`), each a `CatalogSticker` with
an explicit `R.drawable.img_sticker_*` reference. Choosing one adds a `Sticker` with that picture's
id. `data/edit/edit/StickerCatalog.kt`. *Not* the stickers on the photo.

**EditSettings** - Everything the Edit screen has done to the photo, as one immutable value: crop
rectangle (fractions of the photo), crop ratio lock, quarter turns, two flips, brightness,
contrast, saturation, warmth (each -1..1, 0 = unchanged), one filter and its intensity (0..1), and
the list of stickers. It
is the whole state, never a step: it is always applied to the original photo, so a second filter
replaces the first. The default value changes nothing (`isNeutral`).
`data/edit/edit/EditSettings.kt`. *Not* app settings (`SettingsRepository`, theme).

**Session (`EditSession`)** - One Edit screen session: the current `EditSettings`, an undo stack of
earlier `EditSettings`, and a `pending` value during a slider drag. Immutable; each call returns
a new session.
- `change(new)` - one action (chip, turn, flip, ratio, finished crop): the old settings become an
  undo entry; a no-op change adds none.
- `preview(new)` - a slider is moving: settings follow the finger, no undo entry yet.
- `commit()` - the finger let go: the whole drag becomes one undo entry.
- `undo()` - back to the newest undo entry (committing any drag first).

`data/edit/edit/EditSession.kt`; the ViewModel holds it. Undo stores settings, never pixels. *Not*
ended by Done: Back from Result returns to the same session. Only `Tool.EDIT` uses it; the other
tools' Undo steps back through a list of earlier pictures in `ToolViewModel`.

**Preview (live preview)** - What the Edit screen draws while the person edits, with no full-size
render:
- `LivePreviewImage` (`ui/tool/component/photocard/LivePreviewImage.kt`) draws the photo with Compose every frame,
  applying crop, then turns, then flips, and the four sliders as a GPU
  `ColorFilter.colorMatrix`.
- `EditColourMatrix` (`data/edit/edit/EditColourMatrix.kt`) is the one place the four sliders
  become a 20-number colour matrix; the live preview and `EditProcessor` both use it.
- `FilterPreviewRenderer` (`data/edit/edit/FilterPreviewRenderer.kt`) handles the one part Compose
  cannot draw: the filter. It runs the real `EditProcessor` with only the filter and the stickers
  set (all but the one being touched), off the main thread, on a copy of the original shrunk to at
  most `PREVIEW_EDGE` = 640 px on its longest edge. `LivePreviewImage` then draws everything else
  on top of that small picture, including the sticker under the finger (sliders, no filter).

*Not* a `@Preview` composable (the design-time previews CLAUDE.md requires), and not the camera's
`LivePreview` (`ui/camera/CameraScreen.kt`).

**EditProcessor** - The Edit tool's renderer used at Done: applies a whole `EditSettings` to the
original photo on the CPU (`Dispatchers.Default`) in this order: stickers, crop, turns, flips,
filter, then sliders. Inputs above `WorkingSize.EDIT` (3000 px longest edge) are scaled down first. The output
is opaque (`setHasAlpha(false)`). Out of memory gives `EditFailure.OutOfMemory`; any other error
gives `EditFailure.Other`. `data/edit/edit/EditProcessor.kt`, one implementation of `ToolProcessor`
(`data/edit/ToolProcessor.kt`, the seam every tool runs through).

**Done** - The tool screen's finish button (`ToolViewModel.done()`). On Edit it renders the current
settings once at full working size from the original (`renderFinal`/`renderFull`), showing a
spinner (`ToolUiState.finishing`). With nothing changed it hands over the original. The other four
tools hand over the picture shown now. The picture goes into `RenderedPhotoStore`, then the screen
opens Result. Done writes no file. If the render fails, the person stays on the screen and a
message on the photo card says why. With the Crop sheet open the cropper replaces the photo card,
so no message is drawn and Done can be tapped again.

**Result** - The screen after Done: the rendered picture with Share, Save and Go home
(`navigation/Result.kt`, `ui/result/ResultScreen.kt`, `ui/result/ResultViewModel.kt`). Save is
the only place a picture is written to the gallery. The key carries no data. The same screen also
works as the viewer for a photo opened from My edits, and then it never saves.

**RenderedPhotoStore** - The hand-off between Done and Result. It holds one bitmap, plus the
original's Uri when the picture is the untouched photo, so Save can copy that file. A navigation
key is saved as text and cannot carry a bitmap. It is a Koin single, so it lives as long as the
process: after the process is killed it is empty and Result says "Nothing to show".
`data/edit/RenderedPhotoStore.kt`. *Not* storage on disk.

**Compare / Undo pill** - `EditPill` (`ui/tool/EditPill.kt`), the small pill floating over the
photo's bottom-right corner on the Edit screen. Undo calls `ToolViewModel.undo()`. It is disabled
when there is nothing to undo, while an edit runs, or while Compare is on. Compare is a switch
(`setComparing`): while it is on, the screen shows the untouched original and editing does nothing.
The edit itself is kept. Compare is never disabled. *Not* in a `ModalBottomSheet`: the sheets
leave the photo reachable so the pill stays tappable.
