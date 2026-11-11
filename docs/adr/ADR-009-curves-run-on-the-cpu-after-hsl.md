# Curves run on the CPU after HSL, through 256-entry tables

The owner wanted a Curves tool in Edit, as in Lightroom: a tone curve for all colours, one for each of
red, green and blue. This record keeps where the step runs, how a curve becomes
pixels, how the live preview and the saved photo stay identical, and what it costs. A source path below
without a prefix is under `app/src/main/java/com/phongkaster/kanso/`.

Status: accepted.

## Context

- Curves is the tenth Edit tool, right after HSL.
- Adjust and RGB are one 4 x 5 colour matrix, which the GPU draws. A curve cannot be written as that
  matrix: it bends tones non-linearly, so it is not one linear map of red, green and blue.
- The live preview of a filter is already a CPU render: `FilterPreviewRenderer` runs the real
  `EditProcessor` on a copy of the original of at most 640 px, and `LivePreviewImage` puts the colour
  matrix on top on the GPU. HSL (ADR-008) already runs in that CPU stage.
- Kanso has a rule that the preview and the saved photo must show the same picture.

## What was decided

- **Curves run on the CPU, after Filter and HSL and before the colour matrix.** Done is
  `EditProcessor.render`: filter, HSL, curves, then the colour matrix (Adjust and RGB). The live
  preview is `FilterPreviewRenderer`, whose CPU stage includes curves and calls the same code, and
  `LivePreviewImage` draws the colour matrix on top. So the preview and the saved photo match.
- **A curve is a `ToneCurve`** of 2 to 10 `CurvePoint`s. Each point has x and y in 0..1. The points are
  sorted by x, the first is at x = 0 and the last at x = 1, and neighbours are at least 0.02 apart.
- **A curve becomes a 256-entry table** by monotone cubic interpolation (Fritsch-Carlson). The curve
  passes through every point, never overshoots between points, and stays within 0..255.
- **Per pixel**: `r' = rgbTable[redTable[r]]`, and the same for green and blue. That is the
  whole step: table reads only, no colour-space conversion, alpha kept.
- **A straight diagonal is a no-op.** A curve with points only on the diagonal gives a table equal to
  the identity, so the photo is byte-identical and the step is skipped.

## Why not the other ways

- **Not a GPU 1-D look-up-table shader.** It needs changes in the engine module and a second copy of
  the logic that Done would still need on the CPU.
- **Not replacing the RGB and HSL sliders with curves.** It would remove tools the owner has already
  signed off.
- **Not the colour matrix.** It cannot bend tones non-linearly.

## What it costs

- **One more CPU pass** on the 640 px preview copy for each curve change, on `Dispatchers.Default`,
  and one pass on up to 3000 px in Done.
- **Preview detail.** The preview is computed on fewer pixels than Done's; the look is the same, the
  detail is not.
- **Time on the Galaxy A13: not measured.** No number is claimed here; the owner signs it.
