# Every tool ends with Done and a Result screen, and Edit is one session of settings drawn live

Run 9 turned Edit into a real editor and changed how every tool hands over its picture. This record
keeps the four decisions behind that, why they were taken, and what they cost. The code is the
authority; a source path below without a prefix is under `app/src/main/java/com/phongkaster/kanso/`.

**This changes part of ADR-002 and keeps the rest.** ADR-002 (2026-10-12) gave each of the five
tools its own screen, with "the tool's options strip under the card" and "one full-width Save
button" at the bottom, and a Saved screen after it. Run 9 keeps one screen per tool, the floating tab
bar and every tool one tap from Home, but removes the Save button from the tool screens, turns the
Saved screen into the Result screen, and replaces Edit's options strip with a row of four tools and
their sheets. ADR-002 is not edited by this run; read it together with this record.

## Context

- Before Run 9 a tool screen ended with a bottom **Save** bar that wrote to the gallery at once and
  then showed the Saved screen. A person who only wanted to share, or who wanted to look at the
  result before keeping it, still got a file in `Pictures/Kanso`.
- ADR-002 gave Edit one options strip under the photo card, like every other tool.
- Run 9 asked for a real cropper with handles, drag and pinch on the original photo
  (`core/imagecrop`), for the other Edit tools to stay in reach while one is open, and for an Undo
  and a Compare that work whichever tool is open.

## What was decided

- **Done on every tool, then a Result screen; Save on Result is the only gallery write.** Each tool
  screen has a **Done** button at the top right (`DoneButton` in `ui/tool/ToolScreen.kt`) and no
  bottom bar. `ToolViewModel.done()` puts the finished picture into `RenderedPhotoStore`
  (`data/edit/RenderedPhotoStore.kt`, one app-wide picture, because a navigation key is saved as
  text and cannot carry a bitmap) and tells the screen through `doneEvents` to push the key
  `Result` (`navigation/Result.kt`). The Result screen (`ui/result/ResultScreen.kt`,
  `ResultViewModel`, `ResultUiState`), titled "Your edit", offers **Share**, **Save** and **Go
  home**. `ToolViewModel` holds no `PhotoSaver`; only `ResultViewModel` does, so nothing reaches the
  gallery until Save is tapped there: a JPEG at quality 95 in `Pictures/Kanso` for an opaque
  picture, a PNG for a cut-out with alpha, and a copy of the original file for an untouched photo.
  Share writes only a copy in the app's cache folder named "shared". The old Saved screen is this screen; the
  My edits viewer (`navigation/EditedPhoto.kt`) is the same screen with `viewer = true` (Share and
  Edit, no Save). Done does not end the session: Back from Result returns to the tool screen as it
  was left.
- **The all-at-once options strip: Edit shows all its tools at once, a row of four tools with
  non-modal sheets.** Instead of one options strip, the Edit screen keeps the photo at a fixed 60% of the height and puts the row of
  **Crop**, **Rotate**, **Adjust** and **Filters** under it (`ui/tool/components/editbottombar/edittoolrow/EditToolRow.kt`), all four
  always on screen. A tap opens that tool's sheet (`ui/tool/components/toolpanel/editpanel/EditPanel.kt`) below the row; another
  tool can be opened at any time, and the changes of all four add up. The sheet is an ordinary
  `Surface` animated with `AnimatedContent` (`ui/tool/components/editbottombar/EditSheet.kt`), not Material's
  `ModalBottomSheet`, because a modal sheet dims the photo and takes every touch outside it, and the
  floating **Undo** / **Compare** pill on the photo (`ui/tool/EditPill.kt`) must stay usable while a
  sheet is open. While Crop is open the photo becomes the cropper (`ui/tool/components/EditCropper.kt`, around
  `core/imagecrop`'s `CropperPreview`).
- **One immutable session of settings, always rendered from the original.** Everything done on Edit
  is one `EditSettings` value (`data/edit/edit/EditSettings.kt`: crop, crop shape, quarter turns,
  flips, four sliders, filter and its intensity), held with its undo stack by one immutable
  `EditSession` (`data/edit/edit/EditSession.kt`). `change` makes one undo step, `preview` follows a
  slider without one, `commit` turns a whole drag into one step, `undo` steps back; there is no redo.
  Undo needs no pixels, because `EditProcessor` (`data/edit/edit/EditProcessor.kt`) renders any
  settings from the original photo in a fixed order (crop, turns, flips, filter, sliders), so
  nothing is stacked or baked in: a second filter replaces the first and a slider back at its middle
  gives the photo back exactly.
- **A live preview while editing, one full render at Done.** While the person edits, the photo is
  drawn by Compose (`LivePreviewImage` in `ui/tool/components/photocard/LivePreviewImage.kt`): a `Canvas` that crops,
  turns and flips the original and colours it through `ColorFilter.colorMatrix` with the matrix of
  `EditColourMatrix`, the numbers the final render uses. A filter, which Compose cannot draw, is
  previewed by `FilterPreviewRenderer` on a copy of at most 640 px, one request at a time through a
  conflated channel, on `Dispatchers.Default`. Only Done renders the full picture: with any change
  made, `EditProcessor` runs on the original fitted to 2000 px (`WorkingSize.EDIT`) on
  `Dispatchers.Default`, while Done shows a small spinner; a failed render keeps the person on Edit
  with the failure's message.

## Considered Options

- **Keep the bottom Save bar of ADR-002** (save at once, then the Saved screen). Not kept: it wrote
  to the gallery before the person had seen the result and gave no way to share without saving.
- **Pass the picture to Result in the navigation key.** Not possible: Navigation 3 keys here are
  `@Serializable` and saved as text, so a bitmap goes through `RenderedPhotoStore` instead.
- **One options strip with tabs, or one tool at a time.** Not chosen for Edit: the person must be
  able to reach every tool, and Undo and Compare, without leaving the one open.
- **Material's `ModalBottomSheet` for the sheets.** Rejected: it dims the photo and blocks touches
  outside it, so the pill on the photo would be unreachable.
- **Keep baking each Edit change into a bitmap, with a bitmap undo history** (as the four AI tools
  still do). Rejected for Edit: changes would stack, and every undo step would hold a whole picture.
- **Render every change with `EditProcessor` for the preview.** Not chosen: a full CPU render on
  every slider step would recompute every pixel while the finger moves; Compose can draw the crop,
  the turns, the flips and the colour matrix itself, so only the filter needs a small CPU preview.

## Consequences

- No tool screen writes a file. A picture reaches `Pictures/Kanso` only through Save on Result, and
  so My edits only shows what was saved there.
- `RenderedPhotoStore` lives in memory. If Android kills the app while Result is open, Result comes
  back with "Nothing to show"; the tool screen reopens on the photo without its edits.
- The Edit screen does not take the registry path: `ToolViewModel.apply` does nothing on Edit, and
  Edit has no entry in `ProcessorRegistry`. Remove background, Enhance, Retouch and Artistic keep
  their bitmap results and bitmap Undo, and hand the picture shown to Done at once.
- The preview and the final picture are drawn by different code (Compose on the screen, plain Kotlin
  at Done) from the same settings and the same colour matrix; the filter preview is at most 640 px,
  the final render up to 2000 px, so fine detail of a filter can differ slightly between them.
- The Saved screen's code, tags and previews are renamed Result (`ResultPreview`,
  `ResultMarkPreview`, tags `RESULT_*`); existing string resource names such as `saved_*` stay, and
  so does the test file name `app/src/test/java/com/phongkaster/kanso/save/SaveButtonTest.kt`,
  which now tests Done.
- Back on Edit first closes an open sheet, then, with changes made, asks before discarding them
  (`ui/tool/components/DiscardChangesDialog.kt`).

## Open

- **Open:** whether the live preview and the rendered picture look the same to a person on the
  owner's Galaxy A13 is judged on the phone, not by a machine.
