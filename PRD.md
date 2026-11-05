# Kanso — HSL on the Edit screen

The Edit screen can change colour across the whole photo (Adjust, RGB, Filters), but it cannot
change **one colour range alone**: a bluer sky with skin untouched, greener grass, calmer reds.
**This run adds an HSL tool to the Edit screen**: pick a colour range, then move its Hue,
Saturation or Lightness, and only the pixels of that range change.

Retica's HSL screen is the UI reference: `reference/retica-app/java/com/photo/editor/ui/screen/edit/`
(`component/bottombar/edit/color/ColorHSLEditor.kt`, `enums/ColorHSLOption.kt`,
`common/EditImageColorState.kt`). Only its UI is there; it has no pixel code. Treat the folder as
always: never edit it, build it or import from it. The pixel engine is Kanso's own.

## The decisions (mine)

1. **HSL is a tool of its own**, not chips inside Adjust or RGB (recorded 2026-11-03 in
   `docs/plans/rgb-tool.md`, decision 2).
2. **Nine evenly spaced colour ranges, as Meitu and Retica have them** (2026-11-04): 40° apart on the
   hue wheel, each centred on its hue, in this order:

   | Range | Centre |
   |---|---|
   | Red | 0° |
   | Orange | 40° |
   | Light green | 80° |
   | Green | 120° |
   | Light blue | 160° |
   | Blue | 200° |
   | Navy blue | 240° |
   | Purple | 280° |
   | Pink | 320° |

   Each range is chosen by tapping a circle filled with its colour; the chosen circle is marked.

## Defaults for everything else

Not yet ruled by me. Follow them unless a real reason (a failed check, the Galaxy A13) says
otherwise, and if so say why in the Run Report.

1. **The ninth tool, right after RGB**: `… | Adjust | RGB | HSL | Filters | …`, name "HSL", a
   Material Symbols icon that does not look like Adjust's or RGB's.
2. **Three settings per range: Hue, Saturation, Lightness**, so 27 values in `EditSettings`, each
   **-100 .. 0 .. +100 with 0 = no change**, like every other Kanso slider.
   - Hue moves the range's colours towards its neighbours, at most ±20° at ±100 (Retica's hue
     gradient spans exactly that).
   - Saturation: -100 makes the range grey, +100 makes it twice as strong (clamped).
   - Lightness: -100 darkens the range strongly, +100 lightens it, never to pure black or white.
3. **The sheet keeps the fixed 136 dp height.** The nine circles in a row, three chips
   (Hue / Saturation / Lightness), and one slider for the chosen chip and range. The photo never
   changes size. The slider track shows what it does, as in Retica: a hue gradient for Hue, grey to
   full colour for Saturation, dark to light for Lightness.
4. **Soft edges.** A pixel belongs to a range by how close its hue is to the centre, falling smoothly
   to nothing at the next centre, so two neighbouring ranges share the pixels between them. A sky
   gradient never shows a band. Greys, near-blacks and near-whites are hardly touched, because their
   hue is meaningless.
5. **Behaves like Adjust and RGB**: Red and Hue chosen the first time; reopened, it shows the last
   range and chip; choosing a range or chip never changes the photo; one drag = one Undo step, and
   Redo brings it back; the "changed" dot while any of the 27 values is away from 0; Compare shows the
   photo without it; Back with a change asks "Discard your changes?"; no reset button.
6. **Order in the render**: after the filter, before the colour matrix (Adjust and RGB), so HSL picks
   ranges from the filtered colours and the sliders fine-tune the result.
7. **Preview equals Done.** One formula, in one place, is used by both the live preview and
   `EditProcessor`, as `EditColourMatrix` is for Adjust and RGB. The photo follows the finger while
   dragging; a drag that stutters on the Galaxy A13 (32-bit Android) is a failure, so say so rather
   than ship it. The engine (CPU on a small copy as `FilterPreviewRenderer` does, a shader, or a
   lookup table) is the run's choice, recorded in an ADR.

## What must stay true (negative checks)

- With all 27 values at 0, the saved picture is **pixel-identical** to one saved without the tool.
- A change in one range leaves pixels far from it unchanged: moving Blue does not move a skin tone,
  a red or a grey.
- The other eight tools render exactly as before; existing tests stay green; no new permission,
  dependency or native library unless the ADR justifies it.

## Tests and pictures

- JVM tests: the range weights (each centre gets its own range fully, the midpoint between two
  centres is shared, a grey gets almost none); each slider's direction and limits; the neutral
  identity; one drag as one Undo entry; preview and Done using the same formula.
- **I approve recording new pictures**: the row with nine tools, the HSL sheet on a few ranges and
  each chip, and a photo before/after a Blue and an Orange change, in light and in dark. Existing
  pictures that change because the row gains a tool may be re-recorded; keep each beside its old
  version in `.harness/run/evidence/baseline-diffs/` while the run lasts.
- Device checks may use the Galaxy A13 or the emulator, whichever is free. Before driving either,
  check nobody else's run or person is using it.

## README.md and the written record are part of the work

- `README.md`, `docs/features/edit.md` and `docs/features/tool-screen.md` describe the HSL tool in
  the same change, mermaid flows included.
- **A new ADR, `ADR-008`**, records how HSL is computed and why that engine, how preview and Done stay
  identical, and what it cost on the Galaxy A13.
- `CONTEXT.md` gains **colour range** (one of the nine) and `EditTool` now means nine tools.
- Every standing rule in `CLAUDE.md` holds: a light and dark preview for every new composable, one
  class per file, the screen-package folder layout, the code teaches.

## How it is built

One commit per phase:

1. the 27 values in `EditSettings`, the range model, the HSL formula and its JVM tests;
2. `EditProcessor`'s new step and the live preview;
3. the HSL tool, its sheet, the circles, chips and slider;
4. the docs, the ADR, `CONTEXT.md` and the pictures.
