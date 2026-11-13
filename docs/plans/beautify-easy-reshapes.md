# Plan: six more Beautify reshapes, matched to Meitu

**Status:** plan only (2026-11-13). Decisions D1-D5 settled by the owner the same day (section 2).
**No code until the owner gives the order** (owner, 2026-11-13: "chờ mệnh lệnh"); phase 1 waits for
it too. Nothing below is built yet; every number marked *starting value* is a first guess to be
replaced by the Meitu measurements of phase 1.

The six tools are the "easy" group the owner picked from Meitu's Beautify bar:

| # | Meitu, on the Galaxy A13 | Kanso tool (proposed) | Kind of push |
|---|---|---|---|
| 1 | Beautify › Face › **Eyes** tab › Size | **Eye size** | grow |
| 2 | Beautify › Face › **Nose** tab › Size | **Nose size** | grow |
| 3 | Beautify › Face › **Lips** tab › Size | **Lip size** | grow |
| 4 | Beautify › **Head** › Auto | **Head size** | grow |
| 5 | Beautify › Face › **Face** tab › Jawline | **Jawline** | slide |
| 6 | Beautify › Face › **Face** tab › Chin shape | **Chin shape** | slide |

Each must behave like the same slider in Meitu on the owner's Galaxy A13: the same direction, the
same neutral point, and about the same strength at both ends, as Temple, Cheekbones, Chin length
and Hairline already do ([Face shape](../features/face-shape.md)).

Contents:

1. [Why these six are easy](#1-why-these-six-are-easy)
2. [Decisions (owner, 2026-11-13)](#2-decisions-owner-2026-11-13)
3. [Phase 1: measure Meitu on the Galaxy A13](#3-phase-1-measure-meitu-on-the-galaxy-a13)
4. [Each tool in detail](#4-each-tool-in-detail)
5. [Code changes, file by file](#5-code-changes-file-by-file)
6. [Tests, file by file](#6-tests-file-by-file)
7. [Docs that change in the same commits](#7-docs-that-change-in-the-same-commits)
8. [Order of work, commits and time](#8-order-of-work-commits-and-time)
9. [Checking on the Galaxy A13](#9-checking-on-the-galaxy-a13)
10. [Risks and limits](#10-risks-and-limits)
11. [Measurements (filled in phase 1)](#11-measurements-filled-in-phase-1)

---

## 1. Why these six are easy

They need nothing new under Beautify; every part they use is already in the app and tested.

- **The landmarks are already found.** MediaPipe's face landmarker
  ([`data/edit/face/EngineLandmarkFinder.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/EngineLandmarkFinder.kt))
  already gives 478 points per face: the 468 mesh points (outline, brows, eyes, nose, lips) and the
  10 iris points (468-477). Eyes, irises, nose, lips and jaw are therefore located on every face the
  Face tools find today. No new model, no new asset, no download, no size cost.
- **Both kinds of push already exist.**
  [`WarpPush`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/WarpPush.kt) either
  **slides** pixels (Width, Lift, Waist, Arms) or **grows** them out from a point like a magnifying
  glass, or gathers them in (Chest). Eye, nose, lip and head size are a grow; Jawline and Chin shape
  are a slide toward the face's centre line, the same maths as
  [`FaceWidth`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceWidth.kt) on fewer
  points.
- **The pipeline does not change.**
  [`FaceShape`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceShape.kt) gathers
  every tool's pushes from one
  [`FaceAmounts`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceAmounts.kt);
  [`LocalWarp`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/LocalWarp.kt) moves the
  pixels for the full render and
  [`WarpMesh`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/WarpMesh.kt) draws the
  same moves live on the GPU while a slider is dragged. Finding faces once per photo, the background
  render when the finger lifts, the stale-frame guard (`LiveLookRaceTest`), Undo and Done all keep
  working as they are.
- **Each tool is one small object** of about 30 to 90 lines, like
  [`FaceChinLength.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceChinLength.kt)
  (32 lines), plus its test file, its field in two value classes and its slider in the panel.

What the six do **not** need, and why the other Beautify items were left out of this batch: no skin
mask (Smooth already has its own pipeline), no colour work (Teeth whiten, Makeup), no hair or person
segmentation (Hair), no second pose pass (Reshape › Slender and Neck), no AI or VIP feature.

---

## 2. Decisions (owner, 2026-11-13)

All five are settled. The owner's rule for the layout: **arrange it like Meitu's design.**

| # | Question | Decision |
|---|---|---|
| **D1** | Where do the new tools sit? Face's row holds six tools today and already needs a swipe on the A13; six more makes twelve. | **Three layers, like Meitu's Face panel** (section 2.1). Layer 1: tap **Face**. Layer 2: Face's sub-features **Face / Eyes / Nose / Lips**. Layer 3: tapping a sub-feature opens its **slider, or its menu of tools** when it has more than one. Needs **ADR-012**, since ADR-010 settles two levels. |
| **D2** | Where does **Head size** sit? In Meitu it is its own item on the Beautify bar. | **Top row (layer 1), beside Face, Body and Smooth**, as in Meitu. In code it is still a face tool (placed from the face mesh), so it lives in `data/edit/face/`. Tapping it opens its slider directly, as Smooth does. |
| **D3** | If Meitu's slider is **one-way** (starts at the left, 0 to 100) for one of the six, as Head › Auto's "Intensity" may be. | **Keep Kanso two-way, -100..+100**, like every Face and Body tool. The difference from Meitu is noted in the tool's KDoc and README row. |
| **D4** | Meitu shows **BG Lock** and **Distortion correction** switches above Face, Reshape and Form. | **Not in this batch.** Kanso has no BG Lock for any face tool yet (README "Known limits"); it stays its own future feature. Head size is the tool most likely to need it (section 10). |
| **D5** | Measuring in Meitu (phase 1) means dragging its sliders. | **Allowed as a narrow exception:** free sliders only; drag in the preview; close with X; never ✓, never Save, never a VIP, Pro or credit feature. |

### 2.1 The three layers (D1), step by step

What the person sees, from the top row down. "Layer" counts taps from Beautify's top row.

| Layer | Row shows | Tapping an item… |
|---|---|---|
| **1** (top row) | **Face**, **Body**, **Smooth**, **Head size** | **Face** → layer 2 of Face. **Body** → Body's row (Waist, Arms, Chest, Legs), unchanged. **Smooth** and **Head size** → their slider opens above the row (no layer 2). |
| **2** (Face's sub-features) | **Face**, **Eyes**, **Nose**, **Lips**, and **Back** pinned at the right end | **Face** → layer 3: a menu of its eight tools. **Eyes**, **Nose**, **Lips** → layer 3: their tool. **Back** → layer 1. |
| **3** (one sub-feature) | **Face:** Width, Lift, Temple, Cheekbones, Chin length, Hairline, Jawline, Chin shape, and **Back**. **Eyes:** Eye size. **Nose:** Nose size. **Lips:** Lip size. | A tool opens **its slider** above the row (as today). Where the sub-feature has a single tool (Eyes, Nose, Lips in this batch), its slider opens **at once** when layer 3 opens, and the row shows that one tool selected with Back beside it, so later tools (Eyes › Distance, Lips › Smile…) slot in without changing the layout. **Back** → layer 2. |

Rules carried over from today's two levels, applied to all three:

- The bottom bar **never changes height**; layers swap with the same sideways slide (`groupRowTransition`)
  and the same pinned Back (`GroupBackEntry`) used by Face, Body and Edit's Colour group.
- A tool whose slider is off 0 carries the small dot; so does every row above it that leads to it
  (Face's sub-feature at layer 2, Face at layer 1), so a change is never hidden two taps deep.
- Sliders keep their values while the person moves between layers.
- The zoom onto the largest face stays for every Face sub-feature; **Head size** may zoom out a little
  so the whole head shows (checked in phase 7), as Body zooms out to show the whole person.
- Meitu's fifth tab, **Eyebrows**, is not in this batch; it joins layer 2 when its first tool is built.

---

## 3. Phase 1: measure Meitu on the Galaxy A13

The existing face tools were tuned this way (2026-10-29): the same standing portrait, each Meitu
slider dragged to both ends, screenshots compared, distances read in screen pixels and as a share of
the face. Phase 1 repeats it for the six, **before any code**.

### 3.1 Rules (owner, 2026-11-13)

- Only free sliders. If a panel shows VIP, Pro, Premium, "Unlock" or a credit count, stop there.
- Drag in the preview only. Close every panel with **X**. Never ✓, never Save, never Export, never a
  free trial.
- If Meitu applies a VIP feature by itself when a panel opens (Matte did, 2026-11-13), tap
  "Remove VIP features" and close with X.
- Leave the editor without saving. Delete the test portrait afterwards and check the gallery has no
  new file.

### 3.2 The test portrait

One public, front-facing portrait with the **whole head and hair** visible and the shoulders in
frame (Head size needs the head's outline; Jawline and Chin shape need a clear jaw). Pushed to
`/sdcard/Pictures/` for the session and deleted at the end. The same portrait is used later in
phase 7 for the side-by-side check in Kanso.

### 3.3 Steps, for each of the six

1. Open the portrait in Meitu › Beautify, open the tool's panel and tab.
2. **Neutral point:** note where the thumb starts (middle = two-way, left = one-way) and what number
   Meitu shows. Screenshot.
3. **Right end:** drag to the far right. Screenshot.
4. **Left end:** drag to the far left (for a one-way slider: back to 0). Screenshot.
5. Back to the neutral point, then X.
6. Crop the three screenshots to the feature and measure, in screen pixels:

| Tool | What to measure on the screenshots | Expressed as a share of |
|---|---|---|
| Eye size | each eye's width (outer to inner corner) and height (lid to lid) | the eye's own width at neutral |
| Nose size | the nose's width at the nostril wings, and its length (bridge to tip) | the nose's width at neutral |
| Lip size | the mouth's width (corner to corner) and the lips' total height | the mouth's width at neutral |
| Head size | the head's width at the temples (hair included) and its height (top of hair to chin) | the head's width at neutral |
| Jawline | the jaw's width at its corners (the angle below the ears) | the face's width cheek to cheek |
| Chin shape | the chin's width at mouth-corner height below the lip, and the chin point's height | the face's width cheek to cheek |

7. Also note **what else moves** (for Head size: does the neck or background bend? for Eye size: do
   the brows move?), since the Kanso tool should leave the same things alone.
8. Write the numbers into section 11.

Stop and ask the owner if a slider turns out to be VIP, or behaves very differently from what
section 4 expects (for example, Jawline changing the jaw's *angle* rather than its width).

### 3.4 Time

About one hour for the six, including pushing and deleting the portrait.

---

## 4. Each tool in detail

Conventions, the same as the existing face tools:

- Every value runs from **-1 through 0 to 1** in code and **-100 through 0 to +100** on the slider;
  0 changes nothing. Outside -1..1 is clamped; NaN counts as 0; 0 returns **no pushes at all**.
- Every distance is a **share of the face itself**, never fixed pixels: the face's width (cheek to
  cheek at eye height, `LEFT_OUTLINE[0]` 234 to `RIGHT_OUTLINE[0]` 454), or its length (between the
  eyes, 168, to the chin, 152). A value looks alike on a 500-pixel and a 2000-pixel photo, and a
  tilted head is reshaped along its own tilt.
- "Left" and "right" are the **photo's**, as in
  [`FaceMeshIndex`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceMeshIndex.kt).
- Landmark numbers are MediaPipe's face-mesh numbers. Each is **confirmed in phase 2** by drawing it
  on the test portrait before the tool that uses it is written.
- The constants below are *starting values*; phase 7 tunes them until Kanso at ±100 matches the
  phase-1 measurements, as Temple's 0.08 became 0.13.

### 4.1 Eye size

- **Meitu:** Beautify › Face › Eyes › Size. Expected two-way: right end bigger eyes, left end smaller.
- **File:** `data/edit/face/FaceEyeSize.kt` (`internal object FaceEyeSize`).
- **Landmarks (new in `FaceMeshIndex`):** iris centres `LEFT_IRIS` 468 and `RIGHT_IRIS` 473; eye
  corners `LEFT_EYE_CORNERS` (33, 133) and `RIGHT_EYE_CORNERS` (263, 362). Which of each pair is the
  photo's left is checked in phase 2.
- **Push:** one **grow** per eye, centred on the iris centre (or the corners' midpoint when the iris
  point is missing), `grow = eyeSize × 0.25`, `radius = 1.3 × that eye's width` (corner to corner).
- **Must not move:** the brows (outside the reach at 1.3× the width; checked in a test) and the nose.
- **Edge cases:** an eye whose corners are closer than 1% of the face's width (closed or hidden by a
  turned head) gets no push; the other eye is still enlarged.
- **Strength target:** the eye's width changes by the share measured in phase 1 (section 11).

### 4.2 Nose size

- **Meitu:** Beautify › Face › Nose › Size. Expected two-way: right end bigger, left end smaller.
- **File:** `data/edit/face/FaceNoseSize.kt`.
- **Landmarks (new):** `NOSE_TIP` 1, `NOSE_BRIDGE` 6, `NOSE_WINGS` (129, 358).
- **Push:** one **grow**, centred 60% of the way from the bridge to the tip,
  `grow = noseSize × 0.2`, `radius = 1.1 × the nose's width` (wing to wing).
- **Must not move:** the eyes' inner corners and the upper lip's middle (both at the edge of the
  reach or outside it; checked in a test).
- **Edge cases:** none beyond the shared clamping; every found face has a nose.

### 4.3 Lip size

- **Meitu:** Beautify › Face › Lips › Size. Expected two-way.
- **File:** `data/edit/face/FaceLipSize.kt`.
- **Landmarks (new):** `MOUTH_CORNERS` (61, 291), `INNER_LIPS` (13 upper, 14 lower).
- **Push:** one **grow**, centred on the midpoint of 13 and 14, `grow = lipSize × 0.2`,
  `radius = 0.75 × the mouth's width` (corner to corner).
- **Must not move:** the nose's tip and the chin's point (checked in a test).
- **Edge cases:** an open mouth still grows around its centre (teeth included), as Meitu's does;
  confirm in phase 1 with a closed-mouth portrait only.

### 4.4 Head size

- **Meitu:** Beautify › Head › Auto (an "Intensity" slider; Manual has Large, Small and a selected
  area). Phase 1 settles whether Auto is two-way; see D3.
- **File:** `data/edit/face/FaceHeadSize.kt`.
- **Landmarks:** `FOREHEAD` 10 and `CHIN` 152 (already in `FaceMeshIndex`), the outline's first
  points 234 and 454 for the face's width.
- **Push:** one **grow**, centred on the face's centre line 45% of the way from the forehead point
  to the chin (above the face's middle, because the skull and hair sit above the mesh),
  `grow = headSize × 0.15`, `radius = 1.0 × the face's length` (forehead point to chin), which
  reaches the hair and the top of the neck.
- **Must not move:** the shoulders (outside the reach on a normal portrait; checked in a test with
  the shoulders placed one face length below the chin).
- **Edge cases:** a face near the photo's edge is still grown; `LocalWarp` already clamps reads to
  the photo. Background beside the head bends (no BG Lock, D4); section 10.

### 4.5 Jawline

- **Meitu:** Beautify › Face › Face › Jawline. Expected two-way: right end narrower jaw, left end
  wider, the same way round as Width.
- **File:** `data/edit/face/FaceJawline.kt`.
- **Landmarks:** the lower part of the existing outlines, positions 3, 4 and 5:
  `LEFT_OUTLINE` 58, 172, 136 and `RIGHT_OUTLINE` 288, 397, 365 (a new `JAW_POSITIONS` constant, or
  a slice, in `FaceMeshIndex`).
- **Push:** each point **slides** along the shortest path to the centre line (168 → 152) by
  `jawline × 0.06 × weight` of its distance to the line, weights 0.6, 1, 0.8 (the jaw's corner, 172 /
  397, moves most), each push reaching `0.2 × the face's width`.
- **Shared points:** these are Width's own points, so `FaceShape` adds the two moves together
  (already done for Width, Lift and Cheekbones).
- **Must not move:** the cheekbones (positions 0-2) and the chin's point.

### 4.6 Chin shape

- **Meitu:** Beautify › Face › Face › Chin shape. Expected two-way: right end narrower (pointier)
  chin, left end wider. Phase 1 confirms what it changes; if it turns out to move the chin's point
  rather than its width, stop and ask.
- **File:** `data/edit/face/FaceChinShape.kt`.
- **Landmarks:** the chin's neighbours from the existing `CHIN_AREA`: 148, 176 (photo's left) and
  377, 400 (photo's right). The chin's own point, 152, gets no push.
- **Push:** each point **slides** toward the centre line by `chinShape × 0.08 × weight` of its
  distance to it, weights 1 (148 / 377) and 0.7 (176 / 400), each reaching `0.15 × the face's width`.
- **Shared points:** 148 / 377 and 176 / 400 are also Chin length's points; `FaceShape` adds the
  moves (Chin length slides them up, Chin shape inward).
- **Must not move:** the mouth's corners.

---

## 5. Code changes, file by file

All paths under `app/src/main/java/com/phongkaster/kanso/` unless they start with `app/`. Every new
class and function gets a plain-English KDoc (CLAUDE.md "The code teaches"); one class per file;
every lambda named, no trailing lambdas.

### 5.1 The face engine (`data/edit/face/`)

| File | Change |
|---|---|
| `FaceMeshIndex.kt` | New constants: `LEFT_IRIS` 468, `RIGHT_IRIS` 473, `LEFT_EYE_CORNERS`, `RIGHT_EYE_CORNERS`, `NOSE_TIP` 1, `NOSE_BRIDGE` 6, `NOSE_WINGS`, `MOUTH_CORNERS`, `INNER_LIPS`, and the jaw positions; each with a KDoc saying which tool uses it. |
| `FaceEyeSize.kt` *(new)* | Section 4.1. |
| `FaceNoseSize.kt` *(new)* | Section 4.2. |
| `FaceLipSize.kt` *(new)* | Section 4.3. |
| `FaceHeadSize.kt` *(new)* | Section 4.4. |
| `FaceJawline.kt` *(new)* | Section 4.5. Reuses the centre-line maths of `FaceWidth` (extracted into a shared helper if it is copied twice, like `FaceVertical`). |
| `FaceChinShape.kt` *(new)* | Section 4.6. |
| `FaceAmounts.kt` | Six fields appended (`eyeSize`, `noseSize`, `lipSize`, `headSize`, `jawline`, `chinShape`, all `0f`), their `@param` lines, and `isNone` covering them. |
| `FaceShape.kt` | Adds the six lists. Slides on the same point merge as today; grows are not merged (each grow has its own centre). KDoc lists the twelve tools. |

### 5.2 The processor (`data/edit/beautify/BeautifyProcessor.kt`)

- Six `var`s beside `hairline` (`eyeSize`, `noseSize`, `lipSize`, `headSize`, `jawline`,
  `chinShape`), each with a KDoc naming its Meitu slider and direction.
- `faceAmounts(...)` passes them on, clamped.
- The class KDoc's list of "values that need landmarks" grows by six. Nothing else: the processor
  already asks for landmarks whenever any face value is not 0.

### 5.3 The screen's model (`ui/tool/model/`)

| File | Change |
|---|---|
| `BeautifyFaceTab.kt` *(new, own file)* | The four sub-features of layer 2: `FACE`, `EYES`, `NOSE`, `LIPS`, in Meitu's order, with KDoc. |
| `BeautifyTool.kt` | A second property, `faceTab: BeautifyFaceTab?` (null for Body, Smooth and Head size). The six existing face tools get `faceTab = FACE`. Six entries: `JAWLINE`, `CHIN_SHAPE` (group Face, tab Face), `EYE_SIZE` (Face › Eyes), `NOSE_SIZE` (Face › Nose), `LIP_SIZE` (Face › Lips), `HEAD_SIZE` (`group = null`, top row, like `SMOOTH`). |
| `BeautifyGroup.kt` | Unchanged: Face and Body. Head size needs no group (D2). |
| `BeautifyAmounts.kt` | Six fields appended (`faceEyeSize`, `faceNoseSize`, `faceLipSize`, `faceHeadSize`, `faceJawline`, `faceChinShape`) with `@param` lines. |

### 5.4 The panel and row (`ui/tool/component/toolpanel/beautifypanel/`)

| File | Change |
|---|---|
| `BeautifyPanel.kt` | Six `rememberSaveable` values with `DEFAULT_*` constants at 0, six `BeautifySlider` branches (label, value, clamp to -1..1, test tag), the "off its default" dot for each, and `amounts()` passing them on. Six test-tag constants (`BEAUTIFY_EYE_SIZE_TAG` …). |
| `BeautifyToolRow.kt` | An icon and a title for each new tool and each sub-feature; the row now knows three layers (section 2.1): which group is open (Face) and which Face sub-feature is open. Head size joins the top row. Layers swap with `groupRowTransition`; each layer below the top has `GroupBackEntry`. The dot rule climbs all layers. |
| New composables | The layer-2 row of Face's sub-features, and any entry that does not exist yet (a sub-feature chip). One file per composable, in `components/` following the screen-package rule, each with a light and a dark preview (CLAUDE.md "Every composable has a preview"). |
| `BeautifyPanel.kt` (open state) | The panel's open-tool state also remembers the open layer and sub-feature (`rememberSaveable`), so a rotation or process death returns to the same layer. Opening Eyes, Nose or Lips opens its one slider at once. |

### 5.5 The screen (`ui/tool/ToolScreen.kt`)

Where `amounts.faceHairline` is handed to the processor (around line 989), the six new values are
handed on the same way. The zoom onto the largest face stays; Head size may want the whole head in
view (check in phase 7; if so, zoom out a little while Head size is open, as Body zooms out).

### 5.6 Resources (`app/src/main/res/`)

- `values/strings.xml`: for each tool a row title (`beautify_tool_eye_size` …) and a slider label
  (`beautify_eye_size_label` …); the sub-feature titles (`beautify_face_tab_face`, `_eyes`,
  `_nose`, `_lips`).
- `drawable/`: an icon for each sub-feature (Face can reuse `ic_face_group.xml`).
- `drawable/`: six vector icons `ic_face_eye_size.xml`, `ic_face_nose_size.xml`,
  `ic_face_lip_size.xml`, `ic_face_head_size.xml`, `ic_face_jawline.xml`, `ic_face_chin_shape.xml`,
  drawn in the same outlined style as the existing `ic_face_*` icons (ADR-001).

---

## 6. Tests, file by file

All under `app/src/test/java/com/phongkaster/kanso/`, plain JVM, one class per file.

### 6.1 The test face

`tool/face/SampleFace.kt` places only the points Kanso reads. It gains: both iris centres, the four
eye corners, the nose's tip, bridge and wings, the mouth's corners and inner lips, at the places a
real upright face has them (as shares of `halfWidth` / `halfHeight`). The existing tests keep
passing because no existing point moves.

### 6.2 One test file per tool

`tool/face/FaceEyeSizeTest.kt`, `FaceNoseSizeTest.kt`, `FaceLipSizeTest.kt`, `FaceHeadSizeTest.kt`,
`FaceJawlineTest.kt`, `FaceChinShapeTest.kt`, each checking:

1. 0 and NaN give **no pushes**.
2. **Direction:** +1 makes the feature bigger (grows: `grow > 0`) or narrower (slides: points move
   toward the centre line); -1 the opposite.
3. **Place:** each push sits where section 4 says (iris centre, between bridge and tip, …).
4. **Clamping:** ±3 gives the same pushes as ±1.
5. **Scale:** the same face drawn 4× larger gives moves and radii 4× larger.
6. **Tilt:** the same face turned 20° gives pushes turned with it.
7. **Leaves things alone:** the point named under "Must not move" in section 4 is outside every
   push's reach.
8. Tool-specific: Eye size skips a closed eye; Chin shape gives the chin's own point no push;
   Jawline pushes only positions 3-5.

### 6.3 Existing tests that grow

| File | New checks |
|---|---|
| `tool/face/FaceShapeTest.kt` | Each of the twelve tools alone gives exactly its own pushes; Jawline + Width on the same point add up; Chin shape + Chin length add up. |
| `tool/face/LocalWarpTest.kt` | Two overlapping grows (Head + Eye) never fold the picture. |
| `tool/beautify/BeautifyToolTest.kt` | Layer 1 shows Face, Body, Smooth, Head size. Face → layer 2 shows Face, Eyes, Nose, Lips and Back. Face › Face → layer 3 shows the eight tools and Back. Eyes, Nose and Lips each open their slider at once. Back climbs one layer each time. The dot shows on every layer above a changed tool. Each new tool hands over its value alone with every other one at 0. |
| `BeautifyProcessorTest` | Each new value reshapes a face and needs one; all at 0 leaves the photo byte-identical and asks for no landmarks. |

Each phase ends with `./gradlew :app:testDebugUnitTest` green.

---

## 7. Docs that change in the same commits

CLAUDE.md: "README.md follows the code, in the same change"; a behaviour change without its README
update is unfinished.

| Doc | Change |
|---|---|
| `README.md` | The feature table's Beautify row (line ~59) and the "Editing features" row (line ~111): the six tools, their ranges, their state ("Works, checked on the A13" only after phase 7). Section 3.6 if a file is added to the pipeline list. |
| `docs/features/face-shape.md` | A section per tool like Temple's: Meitu measured, Kanso at both ends, the landmarks, the push, the constants, the A13 check; the Tests list and Known limits updated; the title becomes "Face shape: Temple, Cheekbones, Chin length, Hairline, Jawline, Chin shape, Eye, Nose, Lip and Head size". |
| `docs/features/beautify.md` | "What it is" and "How to use it": the new row (or tabs) and Head size. |
| `CONTEXT.md` | Glossary entries for **grow push** if missing, and for each new tool name. |
| `docs/adr/ADR-012-beautify-face-has-three-layers.md` | **Required (D1).** "Beautify's Face group has three layers, as Meitu's Face panel: Face → Face / Eyes / Nose / Lips → the sub-feature's slider or menu." It extends ADR-010 (two levels) for Beautify's Face group only; Body and Edit's Colour group keep two. README's ADR table gets its row. |
| This plan | Status line and section 11 filled; "built" with the commit hash at the end. |

---

## 8. Order of work, commits and time

Kanso keeps only `main`; the work starts from an up-to-date `main` on a feature branch, and is merged
by the owner's default chain (PR, merge commit, branch deleted) only when the owner asks.

| Phase | Work | Commit (Kanso style) | Time |
|---|---|---|---|
| 0 | Owner settles D1-D5 — **done 2026-11-13** (section 2). Then wait for the owner's order to start. | – | – |
| 1 | Measure the six in Meitu on the A13 (section 3); fill section 11 | `docs(beautify): measure six Meitu sliders` | 1 h |
| 2 | Confirm every new landmark number by drawing it on the test portrait (a throw-away debug overlay, not committed) | – | 1 h |
| 3 | `FaceMeshIndex`, `FaceAmounts`, `FaceShape`, processor fields, `SampleFace` points; neutral tests green | `feat(beautify): plumbing for six more face reshapes` | 1 h |
| 4 | Eye, Nose, Lip and Head size with their tests | `feat(beautify): eye, nose, lip and head size` | 2.5 h |
| 5 | Jawline and Chin shape with their tests | `feat(beautify): jawline and chin shape` | 1.5 h |
| 6 | Three-layer row (section 2.1), panel sliders, Head size in the top row, strings, icons, previews; ADR-012; `BeautifyToolTest` | `feat(beautify): Face gets three layers and six new sliders` | 3 h |
| 7 | On the A13: live drag, finger-up render, Done, Undo; side by side with Meitu at ±100; tune constants | `fix(beautify): tune … to Meitu` (as needed) | 2 h |
| 8 | README, face-shape.md, beautify.md, CONTEXT.md, ADR; owner's own check | `docs(beautify): …` | 1 h |

About **13 hours** in all, the owner's checks not counted. Phases 4 and 5 can swap; 6 needs 3.

---

## 9. Checking on the Galaxy A13

For each of the six, on a debug build, with the phase-1 portrait:

1. Open Beautify, reach the tool, drag slowly from 0 to +100: the photo follows the finger with no
   spinner (the live mesh).
2. Let go: the full render replaces the live frame with no jump and no stale frame.
3. Screenshot at +100 and at -100; measure the same distance as in phase 1; it should be within about
   a quarter of Meitu's. Note the numbers in face-shape.md.
4. Check "Must not move" by eye (brows for Eye size, shoulders for Head size, mouth corners for Chin
   shape).
5. Combine: Width +100 with Jawline +100, Head +100 with Eye +100: no fold, no seam.
6. Done → Result → Save: the saved JPEG shows the change; Undo takes back the session.
7. A photo with two faces: both reshaped alike. A photo with no face: "No face found in this photo.
   Nothing was changed."

---

## 10. Risks and limits

- **Head size bends the background.** A grow wide enough for the whole head bends straight lines
  behind it (door frames, horizons). Meitu offers BG Lock for this. First version: no lock, noted in
  Known limits; if the A13 check shows it plainly, BG Lock from the pose model's person mask becomes
  its own plan (D4).
- **Grows add up.** `LocalWarp` averages overlapping slides but adds grows on top. Head +100 with
  Eye +100 stretches the eyes by both; phase 7 checks it does not fold. If it looks wrong, Head size
  becomes a second pass after the other face pushes.
- **Shared points.** Jawline and Width, Chin shape and Chin length push the same points; their moves
  add. At +100 each, the jaw could move further than Meitu's at the same settings; tune against Meitu
  with both set, not only alone.
- **Landmark numbers.** The numbers in section 4 come from MediaPipe's published mesh map and are
  confirmed on a real face in phase 2. The irises (468-477) exist only because the model refines the
  eyes; `EngineLandmarkFinder` already loads that model.
- **Head › Auto may be one-way in Meitu** (its slider is called "Intensity"). D3 keeps Kanso
  two-way either way.
- **One portrait.** As for the existing face tools, the numbers are tuned against Meitu on one
  portrait; other faces may want other values (face-shape.md "Known limits").

---

## 11. Measurements (filled in phase 1)

| Tool | Meitu slider starts | Meitu right end | Meitu left end | Share of the face | Kanso at ±100 (phase 7) |
|---|---|---|---|---|---|
| Eye size | | | | | |
| Nose size | | | | | |
| Lip size | | | | | |
| Head size | | | | | |
| Jawline | | | | | |
| Chin shape | | | | | |
