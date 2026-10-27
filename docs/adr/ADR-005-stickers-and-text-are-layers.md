# Stickers and text are layers: one list, one gesture handler, two kinds of picture

Run 11 adds a sixth Edit tool, Text. This record keeps the decision that shapes it, why it was
taken, and what it costs. It builds on ADR-004 (a sticker is part of the photo) and keeps every
point of it: a text is part of the photo in exactly the same way. The code is the authority; a
source path below without a prefix is under `app/src/main/java/com/phongkaster/kanso/`.

## Context

- Before Run 11 the only thing placed on a photo was a `Sticker`: a picture id, a centre, a width,
  a turn and a mirror flag, stored as fractions of the ORIGINAL photo (ADR-004). Around it sit a
  mapping to the shown picture, one gesture handler for move, pinch and turn, a frame with four
  corner buttons, the live preview's drawing of the touched sticker, the cropper's baked copy and
  `EditProcessor`'s drawing step.
- The owner wants Text to be a tool of its own (its own entry in the tool row and its own sheet,
  not a section of the Stickers sheet), yet a text must be moved, pinched, turned, cropped,
  filtered and undone exactly like a sticker.
- The owner also wants a size and an opacity slider for every placed item, the same corner
  layout on all of them, and the one being selected brought to the front.

## What was decided

- **One model, the Layer.** A `Layer` (`data/edit/edit/Layer.kt`) is anything placed on the photo:
  a key, a `LayerContent`, a centre, a width, a turn, a mirror flag and an opacity. Its content is
  one of two kinds (`data/edit/edit/LayerContent.kt`): `Picture(pictureId)`, a catalogue sticker,
  or `Text(text, colour, font, alignment, wrap)`. `EditSettings.layers` holds them all in one list,
  oldest first, at most `EditSettings.MAX_LAYERS` = 20 in total.
- **A text is drawn as a picture.** Everything that draws a layer asks one source,
  `LayerPictures.picture(content, widthPx)`, for a bitmap about `widthPx` wide. For a sticker it
  decodes the drawable; for a text it lays the words out with Android's `StaticLayout` in the
  chosen bundled font and draws them at that width (`data/edit/edit/TextLayerLayout.kt`). From
  there on the mapping, the gestures, the frame, the live preview, the cropper and
  `EditProcessor` treat both kinds the same.
- **A text's layout does not depend on its size.** The lines wrap at a fixed width counted in
  letter heights (`LayerContent.Text.wrapEms`), worked out when the text is added so that, at the
  starting size, a line wraps at 80% of the photo's width. A bigger or smaller text is the same
  block scaled, as in Instagram, so a text's shape (height over width) comes from its words, font
  and alignment alone, and a pinch scales it like a sticker.
- **The two sheets only differ in what they show.** The Stickers sheet adds `Picture` layers; the
  Text sheet adds and edits `Text` layers. Touching a layer from any sheet but Crop opens the
  sheet of its kind with it selected; the sheet it leaves keeps its changes, like its ✓.

## Considered Options

- **A second, separate list for texts.** Rejected: the gesture handler, the geometry, the frame,
  the corner buttons and three drawing paths would have to be copied or made generic over two
  types, and a text could never sit between two stickers.
- **Keep the name `Sticker` for both kinds.** Rejected by the owner: Text is a tool of its own,
  not a kind of sticker, so the shared name had to be neutral.
- **Re-wrap the text at 80% of the photo at every size.** Rejected: the shape would then change
  while a finger pinches it (a line breaking in two mid-gesture), and the size could no longer
  be carried by the layer's width alone.

## Consequences

- The rename touches every file that knew a `Sticker`; catalogue words (`CatalogSticker`,
  `StickerCatalog`, the Stickers sheet) keep their name, because they are about pictures only.
- A text drawn larger than its starting size can be wider than the photo; the person shrinks it
  with the size slider or a pinch.
- The photo's own flips mirror a text with the photo (it is part of the photo, ADR-004); the text
  has no flip button of its own.
- A text whose words are empty is never kept: it is dropped when it loses the selection, and its
  adding leaves no undo step.
- ADR-004's cost carries over: the layer under a finger shows no filter until the finger lifts.

## Open

- **Open:** whether the default size (20) and the 80% wrap read well on the owner's Galaxy A13 is
  judged on the phone, not by a machine.
