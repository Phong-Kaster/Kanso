# Kanso — a Colour group on the Edit screen

The Edit screen's row of tools has grown to ten entries (Crop, Rotate, Adjust, RGB, HSL, Curves,
Filters, Stickers, Text, Date, with Compare pinned), and on a 411 dp phone most of them sit off
screen. Three of them do one job, changing the photo's colours: **RGB, HSL and Curves**.
**This run gathers those three under one entry, Colour**, with the same two-level row Beautify
already uses for Face and Body.

- **Layer 1** (the row you see first): Crop, Rotate, Adjust, **Colour**, Filters, Stickers, Text,
  Date, with Compare pinned as today.
- **Layer 2** (after tapping Colour): RGB, HSL, Curves, with a pinned **Back** that returns to
  layer 1.

## The decisions (mine)

1. **Colour sits where RGB is now**, right after Adjust: Crop, Rotate, Adjust, Colour, Filters,
   Stickers, Text, Date | Compare. The top row drops from ten entries to eight.
2. **Layer 2 order: RGB, HSL, Curves**, today's order, simplest tool first.
3. **Behave like Beautify's Face and Body groups**: tapping Colour switches the row itself to its
   three tools, with a pinned Back at the left; each tool keeps its own sheet exactly as it works
   today. Reuse Beautify's two-level pattern and components rather than inventing a second one.
4. **The label is "Colour"**, British spelling like every other string in the app ("Theme colour",
   the Text tool's "Colour" tab).
5. **Nothing about the three tools changes**: their sheets, sliders, curves, formulas, order in the
   pipeline, Undo/Redo and the saved photo stay exactly as they are. Only how you reach them moves.

## What must keep working

- Colour shows the "changed" dot while any of RGB, HSL or Curves holds a change, and each tool in
  layer 2 keeps its own dot.
- Back with only a colour change still asks to discard it; Compare still shows the original.
- Opening a colour tool, then going Back to layer 1, behaves the way Beautify's groups do.

## Open for the DoD (propose, I decide)

- The icon for Colour (RGB uses `ic_palette` today; say whether RGB keeps it or Colour takes it).
- Which existing screenshots of the tool row move; list them so I can approve re-recording.

Test on my Galaxy A13 (serial `R58W1120QVA`) at the end, as before. The emulator that is also attached is not the test device.
