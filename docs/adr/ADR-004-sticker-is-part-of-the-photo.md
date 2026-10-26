# A sticker is part of the photo: it is drawn first, so everything else acts on it

Run 10 added a fifth Edit tool, Stickers, ported from the sticker code in `reference/pinity-stickers/`.
This record keeps the decision that shapes it, why it was taken, and what it costs. The code is the
authority; a source path below without a prefix is under `app/src/main/java/com/phongkaster/kanso/`.

**This builds on ADR-003 and amends one point of it.** ADR-003 said only the filter needs a small
CPU preview; the small preview copy now also carries the stickers (all but the one being touched).
ADR-003 made Edit one immutable `EditSettings`,
always rendered from the original photo in a fixed order by `EditProcessor`. A sticker is one more
part of those settings and one more step at the start of that order.

## Context

- A sticker could be treated as a layer on top of the finished picture (like a text label on a
  poster) or as part of the photo itself.
- ADR-003's rule is that nothing is stacked or baked in: every render starts from the original and
  the whole settings, so Undo needs no pixels.
- The owner wants a sticker to move with the picture: a crop that cuts the photo cuts the sticker
  off, a turn or a flip carries it round, and the filter and the colour sliders colour it like the
  rest.

## What was decided

- **A sticker is part of the photo.** It is a `Sticker` value (`data/edit/edit/Sticker.kt`: picture
  id, centre, width, turn in degrees, mirrored) inside `EditSettings.stickers`, at most
  `EditSettings.MAX_STICKERS` = 20. Its place is a fraction of the ORIGINAL photo, before any crop,
  turn or flip, so it belongs to the photo and not to the screen. Which sticker is selected is not
  in the settings (it is `ToolUiState.selectedStickerKey`), so selecting never makes an Undo step.
- **`EditProcessor` draws the stickers first.** The order is now stickers, crop, quarter turns,
  flips, filter, then sliders (`data/edit/edit/EditProcessor.kt`, `drawStickers`). Each sticker is
  alpha-blended into the cropped pixels (shifted by the crop's corner) before the turns and flips
  move them, so the crop keeps only the part of a sticker inside the rectangle, the turns and flips
  move it with the photo, and the filter and the sliders colour it with everything else.
- **The panel and the screen only edit the settings.** `ToolViewModel` adds, moves,
  deletes, duplicates and mirrors stickers through `EditSession`; selecting only updates
  `selectedStickerKey` in the UI state, with no settings change; a whole gesture is one Undo step
  (`previewStickers` while the finger moves, `commitSettings` on finger up), and the sheet's cross
  or system Back puts back the snapshot taken when it opened.
- **The live preview draws the stickers too, with one exception.** `FilterPreviewRenderer` draws
  every sticker that is not being touched into its small preview copy before the filter, so the
  filter and Intensity act on them. The sticker under the finger is drawn by `LivePreviewImage` on
  top, with the colour sliders but no filter, so it follows the finger without a re-render per
  frame.

## Considered Options

- **A layer drawn over the finished picture.** Rejected: the filter and the sliders would not touch
  the sticker, a crop would not cut it, and a turn would need separate handling.
- **Place a sticker in pixels or screen units.** Rejected: pixels depend on the working size and
  screen units on the phone; fractions of the original stay right at any size and survive crop,
  turns and flips (`StickerMapping` converts between what the person sees and these fractions).
- **Bake each sticker into a bitmap when it is added.** Rejected for the same reason as in ADR-003:
  changes would stack and Undo would need a picture per step.
- **Re-render the filter for every frame of a drag.** Rejected: the filter is a CPU job on a 640 px
  copy and cannot keep up with a finger; hence the exception above.

## Consequences

- **The cost: the sticker under the finger shows no filter in the preview.** While one sticker is
  being dragged, pinched or twisted it is drawn with the colour sliders but without the filter, and
  it takes the filter when the finger lifts (one small re-render at the start of a gesture and one
  at its end). Done always applies the filter to every sticker, so the saved picture is right.
- The preview and the final picture differ slightly in detail for the same reason as in ADR-003
  (a filtered copy of at most 640 px against up to 2000 px).
- Because stickers are drawn before the crop, one that lies wholly outside the kept rectangle
  draws nothing in the saved picture.
- The cropper (`ui/tool/components/EditCropper.kt`) shows stickers by baking them into a copy of
  the original with the same mirror, turn and scale as `EditProcessor`; Compare shows the photo
  without them.
- A sticker whose picture cannot be decoded, or whose numbers are not finite, is skipped in Done
  without an error.

## Open

- **Open:** whether the preview and the saved picture look the same to a person on the owner's
  Galaxy A13 is judged on the phone, not by a machine.
