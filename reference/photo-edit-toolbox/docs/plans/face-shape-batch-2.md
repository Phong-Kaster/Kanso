# Plan: Smooth, Chin tip, Jaw angle, Double chin

**Status:** built 2026-11-17 (owner's order), in the unpublished 0.3.0 = folder 0.5.0; design-notes
item 25 records how. The first batch chosen from the owner's feature sheet under ADR-002: tab
`Photo edit toolbox` of "Vẽ voi_2026", Beautify > Head > Face shape: **Smooth, Chin tip, Jaw angle,
Double chin**. Branch `feature/v0.3.0`.

## 1. Measured in Meitu (Galaxy A13, 2026-11-17)

Free sliders only, dragged in the preview and closed with X; nothing applied or saved (owner's
exception to "look only"). One front-facing portrait already on the phone. Landmarks from the
library's own face model on the neutral screenshot; each landmark's move read by dense optical flow
(DIS) to each end; moves split into "down" (along between the eyes to the chin) and "in" (toward the
face's centre line), as % of the eyes-to-chin length (530 screen px). Negative "down" is up.

| Reshape | Slider | +100 (or 100) | -100 |
|---|---|---|---|
| Chin tip | two-way | 152 down -1.0; 148/377 -2.1 down, +0.9..1.0 in; 176/400 -1.7..-2.1 down, +1.1..1.3 in; 150/379 -0.2/+0.2; jaw, mouth 0 | the same reversed |
| Jaw angle | two-way | 172/397 -2.2..-2.4 down, +1.5..1.8 in; 58/288 -1.6..-1.7, +1.2..1.3; 136/365 -1.9..-2.1, +1.1..1.5; 150/379 -1.2..-1.4, +0.6..0.8; chin 0 | about 0.6-0.7 of +100, reversed |
| Smooth | one-way 0..100 | sideways only: 172/397 +0.4/+0.6, 136/365 +0.2/+0.6, 58/288 +0.3/+0.2, 127/356 +0.4/+0.1, 234/454 +0.3/+0.1, 132/361 -0.1/-0.5; chin 0; 50 is half | none |
| Double chin | one-way 0..100 | one side only: 172 -0.2, 136 -0.4, 150 -0.5, 149 -0.5, 176 -0.3, 148 -0.1 down (in +0.1..0.2); the neck just below a little less; other side and chin 0 | none |

Meitu also lists Double chin Pro, Jawline and Symmetry as members' features; not touched.

## 2. Built

- `FaceShift` (core/reshape): moves landmarks by (down, in) shares of the eyes-to-chin length; the
  frame of the measurements, so the numbers go in directly.
- `ChinTipWarp`, `JawAngleWarp` (low end × 0.7), `FaceSmoothWarp` (sideways only, the two sides'
  average), `DoubleChinWarp` (both sides). Moves tuned offline: the neutral screenshot warped with the
  library's maths and read back the same way, until each matched Meitu to within 0.1%.
- `FaceReshape.SMOOTH`, `CHIN_TIP`, `JAW_ANGLE`, `DOUBLE_CHIN`; `PhotoEdit.faceSmooth`,
  `faceChinTip`, `faceJawAngle`, `faceDoubleChin` (appended). Every slider two-way, +1 = Meitu's 100.
- Sample: the Face tab lists its twelve tools in the sheet's order (Width, Lift, Smooth, Temple,
  Cheekbones, Chin length, Chin tip, Chin shape, Jaw angle, Double chin, Hairline, Jawline).
- Tests: one file per warp and `FaceShiftTest`; the editor test covers the four (set, undo, redo,
  Done). `ReshapeEditorTest`'s photo is now steep diagonal ramps, so moves under a pixel in any
  direction show.

## 3. Checked on the A13

Same portrait in the demo app, read the same way, at ±100: Chin tip and Jaw angle within 0.1-0.3% of
Meitu everywhere; Double chin as Meitu on the side Meitu changed (and the other side too); Smooth's
two sides average as Meitu. All four at +100 render on Done.

## 4. Still open

- Smooth and Double chin are gentle (at most about 0.5% of the eyes-to-chin length), as Meitu on this
  portrait; Meitu adapts both to each face, which the mesh cannot see. If they look too weak on real
  photos, the owner may ask for more strength (one constant each).
- The sheet rows turn green once the owner confirms on the device.
