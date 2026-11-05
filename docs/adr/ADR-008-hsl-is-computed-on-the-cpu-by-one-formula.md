# HSL is computed on the CPU by one formula, for the preview and for Done

The owner wanted an HSL tool in Edit, as in Lightroom: hue, saturation and lightness changed
separately for each of nine colour ranges. This record keeps how it is computed, why that way, how
the live preview and the saved photo stay identical, and what it costs. A source path below without
a prefix is under `app/src/main/java/com/phongkaster/kanso/`.

## Context

- Adjust and RGB are one 4 x 5 colour matrix (`data/edit/edit/EditColourMatrix.kt`). A matrix cannot
  do HSL: the change depends on each pixel's own hue, so it is not one linear map of red, green and
  blue.
- The live preview of a filter is already a CPU render: `FilterPreviewRenderer` runs the real
  `EditProcessor` on a copy of the original of at most 640 px, on `Dispatchers.Default`, and
  `LivePreviewImage` puts the colour matrix on top on the GPU.
- `minSdk` is 27. `RuntimeShader` (AGSL), the way to run a per-pixel shader in Compose, needs API 33.
- Kanso has a rule that the preview and the saved photo must show the same picture.

## What was decided

- **One plain Kotlin function does the maths.** `EditHsl.apply(pixels, adjustments)`
  (`data/edit/edit/EditHsl.kt`) works on an `IntArray` of ARGB pixels, in place, with no Android type,
  so a JVM unit test runs the same code as the phone. The 27 values (hue, saturation and lightness
  for each of nine ranges, each -1..1) are `HslAdjustments`.
- **The formula, per pixel.**
  1. Convert RGB to hue (degrees), saturation and lightness.
  2. The nine colour ranges (`ColourRange.kt`) have centres 40 degrees apart: Red 0, Orange 40, Light
     green 80, Green 120, Light blue 160, Blue 200, Navy blue 240, Purple 280, Pink 320. A range's
     weight at the pixel's hue is `cos(pi * d / 80)` squared, where `d` is the distance round the
     wheel to the centre (1 at the centre, 0 from 40 degrees away). Two neighbours therefore always
     weigh 1 together, so there is no jump and no banding between ranges.
  3. The pixel's hue, saturation and lightness amounts are the sum of weight times amount over the
     ranges. These sums are worked out once per call for 3600 hue steps (0.1 degree) and looked up,
     interpolated between the two nearest steps, so the per-pixel path has no trigonometry.
  4. **A gate for greys.** Hue means nothing for a grey, so the blended amounts are multiplied by a
     smoothstep of the pixel's spread (largest minus smallest channel, 0..1) between 0.03 and 0.25.
     A pure grey is skipped; a near-grey changes a little; a clear colour changes in full.
  5. Hue turns by up to 20 degrees (`amount x 20`). Saturation is multiplied by `1 + amount` and kept
     within 0..1. Lightness moves toward 0.95 for a positive amount and toward 0.05 for a negative
     one, by that share of the way, so +100 never makes pure white and -100 never pure black.
  6. Convert back to RGB, rounded and clamped to 0..255; alpha is kept.
- **Limits that follow.** Hue is capped at 20 degrees a side, so it cannot swap one colour for a far
  one. A range's reach ends at its neighbours' centres. A grey, a white, a black or a near-grey is
  not changed by any value (no colour tint can be added to a grey with this tool). The saturation
  of a colour already at 1 cannot rise.
- **Same step order in the preview and in Done.** In both: filter, then HSL, then the colour matrix
  (Adjust and RGB). Done is `EditProcessor.render` (layers, crop, turns, flips, filter, HSL, colour
  matrix, opaque). The preview is `FilterPreviewRenderer`, which calls the same `EditProcessor`
  and so the same `EditHsl.apply`, and then `LivePreviewImage` draws the colour matrix on top. The
  step is skipped when all 27 values are 0 (`HslAdjustments.isNeutral`), so a photo with no HSL
  change costs nothing and its pixels are not touched. A test compares the two paths
  (`app/src/test/java/com/phongkaster/kanso/tool/edit/HslParityTest.kt`).
- **The preview renders even with no filter chosen**, so a drag of the HSL slider changes the photo
  on screen while the finger moves; the render is a conflated request (a newer one replaces one
  still waiting), as for a filter.

## Why not the other ways

- **Not an AGSL shader.** `RuntimeShader` needs API 33 and the app supports 27, so older phones
  would need a second path anyway. And a shader is a second copy of the formula, in another
  language, that would have to be kept equal to the Kotlin one Done needs; two copies drift.
- **Not a 3D look-up table.** A table is an approximation (it blends between its sample points), so
  the preview and the saved photo would not be pixel for pixel the same as the exact formula.
- **Not the colour matrix.** It cannot make a change that depends on each pixel's hue.

## What it costs

- **Preview detail.** The preview is the 640 px copy, so on screen the HSL change is computed on
  fewer pixels than Done's (at most 3000 px); the look is the same, the detail is not.
- **A CPU pass per change.** Each slider move re-renders the 640 px copy on `Dispatchers.Default`
  (filter, if any, then HSL). Done runs the step once on up to 3000 px, on `Dispatchers.Default`.
  The per-pixel loop in `EditHsl.apply` has no cancellation check of its own (the processor checks
  right after the step), so leaving Edit mid-step lets that one pass finish.
- **Time on the Galaxy A13: not measured, because the A13 was not available to the run; to be signed
  by the owner (DoD human-only items).** No number is claimed here. Until it is measured, how smooth
  the slider drag is on that phone is unknown.
- **Greys cannot be tinted** and hue turns at most 20 degrees (see the limits above): chosen for
  smooth, non-banded results over reach.
