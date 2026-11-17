# Plan: four Face reshapes and sixteen filters from Photo-Edit-Toolbox

**Status:** planned 2026-11-17, not started. Runs as one Foreman run from `main`, **after** the Beautify
run (`loop/kanso-beautify-reshapes`, Run 15) is signed off and merged (decision D1). The PRD for the
run is [section 9](#9-prdmd-for-the-foreman-run); copy it to `PRD.md` at launch.

Photo-Edit-Toolbox (`Apehero-Saigon/Photo-Edit-Toolbox`, branch `feature/v0.3.0`, pinned at
`ee4c4b1`) has two groups of built features that Kanso lacks (compared 2026-11-17 against Kanso's
`main` at `516f6ab` and the Beautify branch at `9886fc2`):

| Group | In the toolbox | In Kanso today |
|---|---|---|
| **Face reshapes** | Chin tip, Jaw angle, Double chin, Smooth (outline), built 2026-11-17 in `3685494`, matched to Meitu on the Galaxy A13 | none of the four |
| **Filters** | 17 colour cubes: Warm, Cool, Fade, Mono, Vivid, Sepia, Noir, Cinema, Vintage, Pastel, Rose, Forest, Cross, Bleach, Cyano, Dusk, Autumn | 8: Light, 1977, Kelvin, Nashville, Walden, Hudson, Amaro, Autumn (Autumn byte-identical to the toolbox's) |

Everything else the toolbox does, Kanso already does (Crop with the same eight shapes, Rotate, Adjust,
RGB, HSL, Curves, Text, Date, Compare, Undo, Redo, the four Body reshapes and, on the Beautify branch,
the other twelve Face reshapes).

Contents:

1. [Decisions (owner, 2026-11-17)](#1-decisions-owner-2026-11-17)
2. [Before the run](#2-before-the-run)
3. [The reference copy](#3-the-reference-copy)
4. [Face: Chin tip, Jaw angle, Double chin, Contour](#4-face-chin-tip-jaw-angle-double-chin-contour)
5. [Filters: sixteen more](#5-filters-sixteen-more)
6. [Docs that change in the same commits](#6-docs-that-change-in-the-same-commits)
7. [Checking on the phone](#7-checking-on-the-phone)
8. [Risks, limits and what is not in this run](#8-risks-limits-and-what-is-not-in-this-run)
9. [PRD.md for the Foreman run](#9-prdmd-for-the-foreman-run)

---

## 1. Decisions (owner, 2026-11-17)

| # | Decision |
|---|---|
| D1 | **Merge the Beautify branch first, then one run from `main`.** The three new reshapes build on the Beautify branch's three-layer Face row (ADR-012) and its pushes. Kanso's rule: every change starts from `main`. |
| D2 | **The toolbox's outline "Smooth" is called *Contour* in Kanso.** Kanso's top-row *Smooth* already means skin smoothing; the two must never share a name. |
| D3 | **Add all sixteen filters; keep Kanso's eight.** 24 filters plus None. |
| D4 | **Copy the toolbox's code and images**; the owner has the rights to publish them in public Kanso (asked because Kanso is public and the toolbox is a private company repository). |
| D5 | **The open edits on the Beautify branch are not deleted.** Checked 2026-11-17: `FoldGuard.kt`, `BeautifyStep.kt`, their tests, `PortraitFace.kt` and the changes to `FaceAmounts.kt`, `FaceShape.kt`, `BeautifySlider.kt`, `FaceShapeTest.kt` are in no commit and not in `main`; the branch itself is 13 commits ahead of `main` and **not pushed to GitHub**. |

Proposed, for the owner to confirm at the DoD approval (the run asks before it builds):

| # | Proposal |
|---|---|
| P1 | **Order in Face's own row (layer 3)** follows the owner's sheet, as the toolbox does: Width, Lift, Contour, Temple, Cheekbones, Chin length, Chin tip, Chin shape, Jaw angle, Double chin, Hairline, Jawline (twelve, from eight). |
| P2 | **Directions as the toolbox**: every slider two-way, -100 through 0 to +100, +100 = Meitu's right end (or its 100 for a one-way slider). Chin tip +100 a more pointed chin; Jaw angle +100 the jaw's corners raised and drawn in, -100 squarer at 0.7 of that strength; Double chin +100 the jaw's lower edge lifted toward the face; Contour +100 the outline evened out (the jaw's corner drawn in, the dip above it filled). |
| P3 | **Filter order:** None, Kanso's eight as today, then the sixteen in the toolbox's order. |
| P4 | **Four new icons** in the style of `ic_face_*` (the toolbox ships no icons), as the Beautify run drew nine. |

## 2. Before the run

1. The owner checks the Beautify run's DoD items 16-23 on the Galaxy A13 and signs them.
2. The owner decides what happens to the open edits of D5 (commit them to the Beautify branch, or
   drop them). Nothing is deleted without that answer.
3. The Beautify branch is pushed and merged with Kanso's chain (README and docs up to date, pull
   request, merge commit, `git checkout main` + `git pull --ff-only`, branch deleted locally and on
   `origin`).
4. This plan's branch (`docs/port-toolbox-face-and-filters`: this file and `reference/photo-edit-toolbox/`)
   is merged the same way.
5. `git checkout main` + `git pull --ff-only`, section 9 copied to `PRD.md`, then `/foreman`.

## 3. The reference copy

`reference/photo-edit-toolbox/` holds, unchanged, the toolbox files this run ports, taken from
`ee4c4b1` (the four reshapes were added in `3685494`). Like `reference/retica-app/`, it is read-only:
never edited, built or imported. It exists because the run's engine may read only inside Kanso.

| Path under `reference/photo-edit-toolbox/` | What it is |
|---|---|
| `core/src/main/java/.../reshape/FaceShift.kt` | Moves landmarks by shares of the eyes-to-chin length, *down* the face and *in* toward its centre line: the frame Meitu was measured in, so the measured numbers go in directly |
| `.../reshape/ChinTipWarp.kt`, `JawAngleWarp.kt`, `DoubleChinWarp.kt`, `FaceSmoothWarp.kt` | The four reshapes (FaceSmoothWarp is Contour) |
| `.../reshape/FaceWarps.kt`, `FaceWarpAmounts.kt`, `ReshapeShapes.kt`, `FaceReshape.kt`, `PhotoEdit.kt` | Where the four join the other reshapes (for reading, not porting) |
| `core/src/test/.../reshape/*Test.kt`, `MadeUpFace.kt`, `ReshapeEditorTest.kt` | The toolbox's tests for the four, to port to Kanso's test style |
| `docs/plans/face-shape-batch-2.md` | Meitu's measurements on the A13 (section 1), how they were built and checked |
| `core/src/main/assets/filters/*.png` | The sixteen colour cubes, 512 x 512 RGB, the same format as Kanso's `filters/autumn.png` |
| `tools/generate_filters.py` | How the cubes were made (no third-party material) |

## 4. Face: Chin tip, Jaw angle, Double chin, Contour

**What Meitu does** (owner's measurements on the Galaxy A13, 2026-11-17, from the reference's
`face-shape-batch-2.md` §1; moves in % of the eyes-to-chin length, *down* along the face, *in* toward
its centre line; landmark numbers are MediaPipe's face mesh):

| Kanso tool | Meitu slider | At +100 | At -100 |
|---|---|---|---|
| Chin tip | two-way | 152 down -1.0; 148/377 -2.1 down, +0.9..1.0 in; 176/400 -1.7..-2.1 down, +1.1..1.3 in; 150/379 -0.2/+0.2; jaw and mouth 0 | the same, reversed |
| Jaw angle | two-way | 172/397 -2.2..-2.4 down, +1.5..1.8 in; 58/288 -1.6..-1.7, +1.2..1.3; 136/365 -1.9..-2.1, +1.1..1.5; 150/379 -1.2..-1.4, +0.6..0.8; chin 0 | about 0.6-0.7 of +100, reversed |
| Contour (Meitu "Smooth") | one-way 0..100 | sideways only: 172/397 +0.4/+0.6, 136/365 +0.2/+0.6, 58/288 +0.3/+0.2, 127/356 +0.4/+0.1, 234/454 +0.3/+0.1, 132/361 -0.1/-0.5; chin 0 | (Kanso: the same, reversed) |
| Double chin | one-way 0..100 | one side only in Meitu: 172 -0.2, 136 -0.4, 150 -0.5, 149 -0.5, 176 -0.3, 148 -0.1 down (in +0.1..0.2); the neck just below a little less | (Kanso: the same, reversed) |

The toolbox applies Contour's and Double chin's measured average to both sides (Meitu adapts them to
each face, which the mesh cannot see); Kanso does the same.

**Port, file by file** (paths under `app/src/main/java/com/phongkaster/kanso/`):

| Kanso | From the reference | Note |
|---|---|---|
| `data/edit/face/FaceShift.kt` | `FaceShift.kt` | Kanso's own landmark types (`FaceLandmarks`, `FacePoint`, `FaceMeshIndex`) instead of the toolbox's |
| `data/edit/face/FaceChinTip.kt`, `FaceJawAngle.kt`, `FaceDoubleChin.kt`, `FaceContour.kt` | `ChinTipWarp.kt`, `JawAngleWarp.kt`, `DoubleChinWarp.kt`, `FaceSmoothWarp.kt` | one file per tool, as `FaceTemple.kt` and the others; pushes through Kanso's `WarpPush` / `LocalWarp` |
| `data/edit/face/FaceAmounts.kt`, `FaceShape.kt` | `FaceWarpAmounts.kt`, `FaceWarps.kt` | four new amounts; all four at 0 add no push |
| `ui/tool/model/BeautifyTool.kt`, `BeautifyAmounts.kt`, the Face row of layer 3 | (P1) | P1's order; `BeautifyFaceTab.FACE` |
| `res/values/strings_beautify.xml`, `res/drawable/ic_face_*.xml` | (D2, P4) | four names (Contour, Chin tip, Jaw angle, Double chin), four icons |

**Tests** (JVM), ported from the reference's tests: one file per tool and `FaceShiftTest`; each checks
the direction and the measured strength at both ends within the toolbox's tolerance, that the chin, jaw
or mouth points the table says do not move stay put, and that 0 adds no push. `BeautifyProcessorTest` /
`BeautifyToolTest` cover set, Undo, Redo and Done for the four. The twelve existing face tools' tests
pass unchanged.

## 5. Filters: sixteen more

1. Copy the sixteen PNGs from `reference/photo-edit-toolbox/core/src/main/assets/filters/` to
   `app/src/main/assets/filters/`, byte for byte (a test compares their hashes with the reference).
2. `data/edit/edit/FilterPreset.kt`: sixteen entries (`WARM("filters/warm.png")` …), each with a KDoc
   describing its look (the toolbox's `generate_filters.py` says what each does).
3. `FilterChips.kt` and `strings_edit.xml`: sixteen chips, P3's order. The chips row scrolls.
4. Tests: every preset loads and changes a grey ramp; None leaves it unchanged; the chips list 25
   entries in order. Screenshot tests of the Filters sheet re-recorded with the owner's approval.

## 6. Docs that change in the same commits

- `README.md`: the Beautify and Filters rows of "Editing features at a glance", "How each editing
  feature works", the third-party / origin section (the toolbox, pinned SHA, and that the owner
  confirmed the right to publish).
- `docs/features/face-shape.md` (four tools, the measurement table), `docs/features/beautify.md` (the
  twelve-tool row), `docs/features/edit.md` (the filter list).
- `docs/adr/ADR-012-beautify-face-has-three-layers.md`: layer 3 now holds twelve tools.
- `CONTEXT.md`: *Contour* (outline) vs *Smooth* (skin).

## 7. Checking on the phone

On the Galaxy A13 (the owner's phone, 32-bit), with the portrait the Beautify run used
(`/sdcard/Pictures/kanso_reshape_portrait.jpg`), opened from Kanso's own gallery:

| Check | Who |
|---|---|
| Each of the four at ±100: the right direction, about Meitu's strength, nothing else moves | the run first (screenshots), then the owner |
| Drag, lift, Undo, Done, Save for each of the four | the run, then the owner |
| All twelve face tools at once render on Done without a crash | the run |
| Each of the sixteen filters: chosen, intensity moved, Done, Save | the run, then the owner by eye |
| The four new icons | the owner |

## 8. Risks, limits and what is not in this run

- **Contour and Double chin are gentle** (at most about 0.5% of the eyes-to-chin length), as Meitu on
  the measured portrait. If they look too weak on real photos, each is one constant.
- **Twelve tools in one row** need the row to scroll on a 411 dp screen; the open tool must scroll into
  view.
- **Not in this run:** skin smoothing (its own work, branch `feat/skin-smoothing`, uncommitted); the
  toolbox's planned scope (Lighting presets, Effects, Remover, Frames, Brushes, Text templates; ADR-002
  of the toolbox), none of which is built there yet; Meitu's Double chin Pro, Jawline Pro and
  Symmetry (members' features, never measured).

## 9. PRD.md for the Foreman run

```markdown
# Kanso — Four Face reshapes and sixteen filters from Photo-Edit-Toolbox

Kanso's Beautify › Face has twelve reshapes and Edit has eight filters. **This run adds four more
reshapes — Chin tip, Jaw angle, Double chin and Contour — and sixteen filters, ported from
Photo-Edit-Toolbox.** The whole design is in
[`docs/plans/port-toolbox-face-and-filters.md`](docs/plans/port-toolbox-face-and-filters.md): follow it.
The toolbox files to port are copied, unchanged, in `reference/photo-edit-toolbox/` (read-only, like
`reference/retica-app/`).

## The decisions (mine)

1. Contour is the toolbox's outline "Smooth"; it is never called Smooth in Kanso, whose Smooth is skin.
2. Face's own row (layer 3) holds twelve tools in my sheet's order: Width, Lift, Contour, Temple,
   Cheekbones, Chin length, Chin tip, Chin shape, Jaw angle, Double chin, Hairline, Jawline.
3. Every new slider is two-way, -100 through 0 to +100, +100 = Meitu's right end (plan §4).
4. Keep all eight filters and add the sixteen after them, byte for byte from the reference.
5. Four new icons in the style of `ic_face_*`.

## What must keep working

- The twelve existing face tools, Body and Smooth behave exactly as today.
- Live drag with no spinner, the full render on lift with no jump, Undo, Done and Save for every new
  tool and filter.
- No face: "No face found in this photo. Nothing was changed."; two faces both reshaped.
- All values at 0 leave the photo byte-identical; None leaves the photo unchanged.

## Definition of Done (to be detailed and approved at bootstrap)

- The four reshapes match plan §4 at both ends (JVM tests), and on the Galaxy A13 portrait.
- The sixteen PNGs are byte-identical to the reference; every filter applies in preview and Done.
- README and the docs of plan §6 updated in the same commits; screenshot changes approved by me.
- Negative checks: no new permission, no new dependency, nothing under `reference/` built or imported,
  no tool or string named "Smooth" other than skin Smooth.
```
