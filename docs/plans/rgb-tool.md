# Plan: the RGB tool on the Edit screen

**Status:** built (2026-11-03, `72e7174`): model, matrix step, sheet, tests and docs, all JVM
tests green. Checked by the owner on the vivo V2408A the same day: works as expected, and the x2
maximum stays. Checked by the owner on the Galaxy A13 on 2026-11-06 as well (see
[Still open](#4-still-open-to-settle-while-building-not-design-decisions)). Every decision below is
the owner's (2026-11-03), reached in a question-by-question design session.

---

## 1. What it is

A new Edit tool, **RGB**, that turns each of the photo's three colour channels (red, green, blue)
up or down across the whole picture. Lowering Red pushes the photo towards cyan, lowering Green
towards magenta, lowering Blue towards yellow; raising one does the opposite. It fixes a colour cast
(a photo taken under yellow light) or tints the whole photo.

It is the RGB part of Retica's COLOR tool (Curve | HSL | RGB). Retica shows four sliders at once
(Red, Green, Blue 0-255 and Opacity 0-100%). Kanso keeps its own one-slider pattern instead (see the
decisions). Note: in `reference/retica-app`, `ColorRGBEditor.kt` is in fact a Shadows/Highlights
tint and has no Red/Green/Blue sliders, so the screenshot came from another Retica version. Nothing
is ported from it.

### The three Retica colour tools, for reference

| Tool | Chooses pixels by | Control | Typical use | Cost in Kanso |
|---|---|---|---|---|
| **RGB** | nothing: every pixel, same rule | 3 sliders | fix a colour cast | low: fits the existing colour matrix (GPU) |
| **Curve** | how light the pixel is | drag points on a graph | contrast, film looks | high: a new graph UI |
| **HSL** | the pixel's hue (colour range) | pick a range, then 3 sliders | bluer sky, skin untouched | medium: per-pixel CPU work, like a filter |

Only RGB is in this plan.

---

## 2. The decisions (owner, 2026-11-03)

| # | Decision | Why |
|---|---|---|
| 1 | **A tool of its own, the eighth**, right after Adjust: `Crop \| Rotate \| Adjust \| RGB \| Filters \| Stickers \| Text \| Date`. Not chips inside Adjust, not a "Colour" tool with sub-tabs. | Adjust stays about light; seven chips would crowd it; the row already scrolls with seven tools. |
| 2 | **Only RGB this time.** HSL and Curve are out of scope; if they come later, each is its own tool. | Each needs a different engine (per-pixel CPU, or a graph UI). |
| 3 | **Each slider runs -100 .. 0 .. +100, 0 = no change, and multiplies its channel**: -100 is x0 (channel gone), +100 is x2. Factor = `1 + value` with value in -1..1. Results are clamped to 0..255. | Reads like every other Kanso slider (middle = unchanged). Multiplying keeps black black, so shadows are not tinted (adding, as Warmth does, would tint them). Can raise as well as lower, unlike Retica's 0-255. |
| 4 | **No Opacity slider.** The sheet has three chips, Red / Green / Blue, and one slider. | Pulling the sliders towards 0 already weakens the effect; "opacity" already means a layer's see-through in Kanso. |
| 5 | **Inside the render, RGB joins the one colour-matrix step**, in this order: filter, then Contrast, Saturation, Warmth, **RGB**, Brightness. | Runs on the GPU in the live preview (smooth on the Galaxy A13), and preview and Done stay identical through `EditColourMatrix`. After the filter, so RGB fine-tunes the filtered look. Before Brightness, so lifted blacks stay neutral grey instead of tinted grey. Invisible to the person: in the UI, RGB is still its own tool. |
| 6 | **Icon `palette`** (Material Symbols, `R.drawable.ic_palette`, already in the app for Settings' theme row), **name "RGB"**. | The most recognisable "colour" icon; `instant_mix` would look like Adjust's `tune` next to it. |
| 7 | **The slider is tinted by the chosen channel**: red for Red, green for Green, blue for Blue (track and thumb). Shape, label and number stay as every other `LabelledSlider`. The three tints must read clearly in light and in dark theme. | The colour tells the person which channel they are moving without reading. |
| 8 | **Behaves like Adjust in everything else**, see the list below. | One way of working across the sheets. |
| 9 | **Work on the branch `feat/autumn-filter`** (not a new branch). | Owner's choice. The branch already has Redo and the arrow Undo/Redo that RGB relies on. |

### Decision 8 in full: same rules as Adjust

1. The first time the sheet opens, the **Red** chip is chosen.
2. Reopened, it shows the chip chosen last. Choosing a chip changes only which slider shows, never
   the photo.
3. The photo follows the finger while dragging; each drag-and-let-go is **one Undo step**, and Redo
   brings it back.
4. The tool's entry in the row carries the small **"changed" dot** while any channel is away from 0.
5. **Compare** shows the original without RGB; **Done** draws RGB into the saved picture.
6. **Back** with an RGB change asks "Discard your changes?", as for any other change.
7. **No reset button.** Drag back to 0 or use Undo, as in Adjust.

---

## 3. What the work delivers (expected touch points)

Paths are under `app/src/main/java/com/phongkaster/kanso/`. They are where the matching Adjust
code lives today; check them again before starting.

1. **Model**
   - `data/edit/edit/EditTool.kt`: add `RGB` after `ADJUST`, with KDoc.
   - `data/edit/edit/EditSettings.kt`: add `red`, `green`, `blue` (each -1..1, 0 = unchanged);
     include them in `hasColourChange`, `isNeutral` and the per-tool "changed" check that drives the
     dot (`EditTool.RGB -> …`).
2. **Engine**
   - `data/edit/edit/EditColourMatrix.kt`: one channel-scale step (`1 + red`, `1 + green`,
     `1 + blue` on the diagonal) between warmth and brightness. `EditProcessor` and
     `LivePreviewImage` pick it up with no change of their own, since both already use this matrix.
3. **Screen state**
   - New `ui/tool/model/RgbChannel.kt`: enum `RED`, `GREEN`, `BLUE` with label, slider test tag and
     tint (the way `AdjustSlider.kt` does it).
   - `ui/tool/ToolUiState.kt`: `rgbChannel: RgbChannel = RgbChannel.RED`.
   - `ui/tool/ToolViewModel.kt`: `chooseRgbChannel(channel)`, next to `chooseAdjustSlider`.
4. **UI**
   - New `ui/tool/component/toolpanel/editpanel/RgbChips.kt` (like `AdjustChips.kt`), with light and
     dark previews.
   - `ui/tool/component/toolpanel/editpanel/LabelledSlider.kt`: accept a tint for track and thumb
     (a real feature parameter, used by RGB).
   - `ui/tool/component/toolpanel/editpanel/EditPanel.kt`: the `EditTool.RGB` branch (chips plus one
     tinted slider, `onPreview` while moving, `onCommit` on let go).
   - `ui/tool/component/editbottombar/edittoolrow/EditToolRow.kt`: `toolIcon` →
     `R.drawable.ic_palette`, `toolName` → a new string `edit_layout_rgb` = "RGB".
5. **Tests** (`app/src/test/java/com/phongkaster/kanso/tool/edit/`)
   - Matrix: x0 / x1 / x2 per channel, black stays black, order (RGB before Brightness);
     `ColourMatrixParityTest` keeps preview = Done.
   - `EditProcessorTest`: a channel at -100 and +100 on real pixels, clamped to 0..255.
   - A sheet test like `AdjustSheetTest`: Red chosen first, chip remembered, one Undo step per drag,
     the dot, Compare.
   - `EditToolRowTest`: eight tools, RGB after Adjust.
   - Slider tint contrast in both themes (as `a11y/OverPhotoContrastTest` checks other colours).
   - Re-record the screenshot goldens the eighth tool changes.
6. **Docs, in the same commit as the code** (Kanso rule: a behaviour change without its docs is
   unfinished)
   - `README.md`: a row in "Editing features at a glance", and the RGB pipeline in "How each editing
     feature works" (screen and ViewModel, matrix step and its order, GPU preview, Done on
     `Dispatchers.Default`, output, failure).
   - `docs/features/edit.md`: the tool list, the matrix order in "Done, the full render", Limits.
   - `CONTEXT.md`: `EditTool` now has eight groups, and a glossary entry for the RGB tool (no
     implementation details).
7. **Kanso conventions to keep**: one class per file; every composable has light and dark previews
   built from sample data; KDoc in plain English on every type and function; no trailing lambdas.

---

## 4. Still open (to settle while building, not design decisions)

- **The exact x2 maximum.** Settled (owner, 2026-11-03): tried on the vivo V2408A, works as
  expected, x2 stays. Were +100 ever found too strong (bright areas burning out), lowering it (for
  example to x1.5) is one number, not a design change.
- **The three tint values.** Settled while building: deep red, green and blue on light themes, pale
  ones on dark themes (`RgbChannel`), each at least 3:1 on the sheet (`RgbTintContrastTest`).
- **Checked on the owner's Galaxy A13** (32-bit, `armeabi-v7a`): yes, 2026-11-06, works as
  expected. The vivo V2408A (64-bit) was checked on 2026-11-03.
- **No ADR planned.** A separate tool is easy to change later and not surprising.

---

## How to resume

1. `git checkout feat/autumn-filter` and `git pull` (it holds 5 commits not yet in `main`, no PR
   yet).
2. Read this file, then `docs/features/edit.md` ("The session and the live preview" and "Done, the
   full render").
3. Start with section 3, step 1 (model) and step 2 (matrix) with their tests, since everything else
   reads from them.

Estimated size: about 1.5 to 2 hours of agent work for code, previews, tests, goldens and docs.
