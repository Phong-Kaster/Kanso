# A date is its own layer kind, not a text

The owner wants a seventh Edit tool, Date, to stamp a date and time on a photo the way Instagram's
date sticker does. This record keeps the decision that shapes it, why it was taken, and what it
costs. It builds on ADR-004 (a layer is part of the photo) and ADR-005 (stickers and text are
layers in one list) and keeps every point of both. A source path below without a prefix is under
`app/src/main/java/com/phongkaster/kanso/`.

## Context

- After Run 11 a `Layer` has two kinds of `LayerContent`: `Picture(pictureId)` and
  `Text(text, colour, font, alignment, wrap)` (`data/edit/edit/LayerContent.kt`). Everything that
  draws a layer asks `LayerPictures.picture(content, widthPx)` for a bitmap.
- A date could be added as an ordinary `Text` whose words are the formatted date. That is the
  cheapest way: nothing past the sheet would change.
- The owner wants more than words: the day and time chosen with pickers, 20 English formats to
  pick from, and a tap on a selected date moving it to the next format.

## What was decided

- **A third kind, `LayerContent.Date`.** It holds the moment (a date and a time), the date format
  (one of 20), a colour and a font. It does not hold the words: they are written from the moment
  and the format whenever the layer is drawn, so changing either rewrites them.
- **Drawn like a text.** The words go through the same text drawing as a `Text` layer
  (`TextLayerLayout`), on one line, so a date gets the same fonts and colours and the rest of the
  pipeline (mapping, gestures, frame, live preview, cropper, `EditProcessor`) treats it as any
  other layer.
- **Its own tool and sheet, Date, after Text.** With no date selected, a format chip adds a new
  date at the middle of the shown picture; with one selected, it changes that date's format. The
  pickers change the selected date, or with none selected, the moment the next date gets (now each
  time the sheet opens). Tapping a selected date moves it to the next format. A new date looks like
  a new text: white, the phone's font, size 20, fully solid.
- **A background box, shared with Text** (added on the owner's request the same day). A first
  version cycled none, solid and see-through with one button and turned the letters black or white;
  the owner replaced it with a Colour tab for the letters and a Background tab for the box (none,
  twelve colours, or any colour with transparency), in both the Text and Date sheets. It is
  `boxArgb` on both kinds, drawn by the text layout as a rounded box filling the block.
- **Corners: delete, duplicate, resize and turn.** No flip (mirrored digits cannot be read) and no
  edit button (the sheet and the tap do the editing).

## Considered Options

- **A `Text` filled with the formatted date.** Rejected by the owner: once added it forgets that it
  is a date, so neither the pickers nor tap-to-cycle could work on it later.
- **A category of the Stickers sheet, or a button in the Text sheet.** Rejected by the owner: Date
  is a tool of its own in the row, like Text (ADR-005).

## Consequences

- Every `when` over `LayerContent` gets a third case; the compiler lists each one.
- The words of a date cannot be typed. Someone who wants "Our day, 28 Oct" uses a Text layer.
- All 20 formats are English (`Locale.ENGLISH`) whatever the phone's language, like the rest of
  Kanso.
- The moment is stored without a time zone: it is what the person picked, and a photo edited on a
  trip still says the day they chose.
