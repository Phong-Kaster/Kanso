# Curves are a natural cubic spline with free end points

Curves in Kanso are meant to handle like Meitu's: a smooth line through the handles that may swing past
them, and handles at the ends that can be pulled sideways. This record keeps that change to the maths
and the end points. Paths without a prefix are under `app/src/main/java/com/phongkaster/kanso/`.

Status: accepted. It supersedes the interpolation and fixed-end-point decisions of
[ADR-009](ADR-009-curves-run-on-the-cpu-after-hsl.md); the rest of ADR-009 (the step runs on the CPU
after HSL and before the colour matrix) still stands.

## Context

- ADR-009 chose monotone cubic interpolation (Fritsch-Carlson): it never overshoots between points, and
  the first and last points were fixed at x = 0 and x = 1, so a person could not clip shadows or highlights
  by pulling an end point inward.
- The owner wants the feel of Meitu's curve editor, where the line is smooth and may overshoot.

## What was decided

- **A natural cubic spline** (second derivative zero at the first and last point) passes through every
  point. It may overshoot between points. `data/edit/edit/ToneCurve.kt`, `valueAt(x)`.
- **The value is clipped to 0..1**, so an overshoot becomes pure black or white, never an invalid value.
- **Left of the first point and right of the last the line is flat** at that point's height.
- **End points are free.** The first point's x may be anywhere from 0 to the next point's x minus 0.02,
  the last mirrored. Adding a point beyond an end makes it the new end point.
- **Any point may be removed** while more than 2 remain. `MAX_POINTS` 10 and `MIN_GAP` 0.02 are unchanged.
- **`table()` is unchanged in shape:** 256 entries, `round(255 x valueAt(i / 255))`. The step still runs
  on the CPU after HSL, and the saved photo matches the preview (`CurveParityTest`).

## Selecting and deleting a point (Phase 3)

- **Selection state lives in `CurvesPanel`** (`rememberSaveable`), not in `ToolViewModel` or `ToolUiState`: it is
  a view detail that changes no setting and is no Undo step. It clears on channel switch, Undo, Redo and Reset.
- **Delete is one `onChange`** (`CurveDeleteButton`, "Delete point"): the point is removed in a single commit, so one Undo step;
  end points may be deleted while more than 2 remain, then the left neighbour (or the new first point) is selected.
- **A point wins over the line within 24 dp.** A tap or drag near a point selects it; only away from every point does a tap
  (or drag) near the line add a point exactly on the line. Tapping empty space adds nothing; a long press no longer removes a point.

## Why not the other ways

- **Keep monotone interpolation.** It cannot overshoot, so it cannot reproduce the Meitu feel.
- **Catmull-Rom or Bezier handles.** A natural spline needs no extra handles and is a small tridiagonal
  solve per curve.

## What it costs

- A curve can invert tones locally (the line may fall where a person expected it to rise); that is the
  intended Meitu behaviour, and the clip keeps values valid.
- The earlier test that the table never overshoots was removed, and its ground rules changed
  (`.harness/run/evidence/test-edits.md`).
- Time on the Galaxy A13: not measured.
