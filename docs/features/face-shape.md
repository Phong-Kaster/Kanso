# Face shape: Temple, Cheekbones, Chin length and Hairline

[Back to README](../../README.md)

Code links below point into `app/src/main/java/com/phongkaster/kanso/`; the link text leaves that
prefix out. Test links point into `app/src/test/java/com/phongkaster/kanso/`. These tools live in
Beautify's **Face** group beside Width and Lift; the screen, the live look, the full render, Undo
and Done are described on [Beautify](beautify.md) and are the same for every face tool.

---

## What they are

Four reshapes of the face, each modelled on the Meitu tool of the same name (Beautify > Face), each
a two-sided slider from **-100** through **0** (the face as it is, the default) to **+100**, the
same way round as Meitu's. Meitu's sliders were measured on the owner's Galaxy A13 on 2026-10-29,
on the same standing portrait, by dragging each to both ends and comparing screenshots; the owner's
own slider values were put back afterwards.

| Tool | Meitu, measured | Kanso: right end (+100) | Kanso: left end (-100) | State |
|---|---|---|---|---|
| **Temple** | +100 moved the hair's edge at eyebrow height about 25 screen px outward, -100 inward | temples fuller | temples hollower | Works, checked on the A13 |
| **Cheekbones** | +100 moved the face's edge across the cheekbones about 7 px inward on each side, -100 about 5 px outward | slimmer across the cheekbones | wider | Works, checked on the A13 |
| **Chin length** | +100 raised the chin's lower edge about 24 px, -100 lowered it about 18 px (about 3% of the eyes-to-chin length) | shorter chin (moved up) | longer chin (moved down) | Works, checked on the A13: the chin moves about 8 screen px each way (about 3.5% of the eyes-to-chin length, as Meitu); it can look subtle at Kanso's zoom |
| **Hairline** | +100 lowered the hairline at the parting about 17 px, -100 raised it about 17 px (about 2.5% of the eyes-to-chin length) | lower hairline, smaller forehead | higher hairline | Works, checked on the A13: the hairline moves about 8 screen px each way, the eyebrows not at all |

All four move pixels only, with the same warp as Width and Lift; nothing is drawn or added.
They need a face: with none found, "No face found in this photo. Nothing was changed."

## How to use them

- Beautify → **Face** → the tool in the row (Width, Lift, **Temple**, **Cheekbones**, **Chin
  length**, **Hairline**; swipe the row to reach the last ones) → drag its slider. The
  photo follows the finger live; letting go renders at full quality in the background.
- Each tool keeps its value while you move between tools, and they all add up: a face can be
  narrowed, lifted and given fuller temples at once.

## How they work

Every face tool is the same three steps (see [Beautify](beautify.md), step 6, for the details
they share):

1. **Find the face.** MediaPipe's face landmarker
   ([`data/edit/face/EngineLandmarkFinder.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/EngineLandmarkFinder.kt))
   gives 478 numbered points per face. [`FaceMeshIndex`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceMeshIndex.kt)
   names the ones each tool uses.
2. **Decide where the outline goes.** Each tool is one small object that turns the landmarks and
   its slider value into [`WarpPush`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/WarpPush.kt)es
   (a point, a move, a reach). Every distance is a share of the face itself (its width cheek to
   cheek, or its length from between the eyes to the chin), so a value looks alike on any photo
   size and a tilted head is reshaped along its own tilt.
   [`FaceShape`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceShape.kt) gathers
   every tool's pushes from one [`FaceAmounts`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceAmounts.kt)
   (each tool's value by name) and adds pushes on the same point together.
3. **Move the pixels.** [`LocalWarp`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/LocalWarp.kt)
   moves every pixel within each push's reach by `w × move`, `w = (1 − d²/r²)²`, in one pass for
   all tools and all faces; [`WarpMesh`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/WarpMesh.kt)
   draws the same moves live on the GPU while a slider is dragged.

### Temple

[`FaceTemple.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceTemple.kt).
Three points per side on the face's outline above the eye, from high on the forehead down to the
eye's outer corner (`LEFT_TEMPLE` 54, 21, 162; `RIGHT_TEMPLE` 284, 251, 389), are pushed straight
away from the face's centre line (between the eyes, 168, to the chin, 152) for a fuller temple, or
toward it for a hollower one, by `temple × 0.13 × weight` of their distance to the line (weights
0.6, 1, 0.8: the hollow in the middle moves most). Each push reaches 0.2 of the face's width, so
the eyes and the cheeks keep their shape. On the A13 at ±100 the temple's edge moves about 15
screen px each way, about 4% of the face's width, as Meitu's; a first version (0.08) moved it about
2.5%.

### Cheekbones

[`FaceCheekbones.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceCheekbones.kt).
The top three points of the face's outline on each side (`LEFT_OUTLINE` 234, 93, 132 and their
mirrors 454, 323, 361: the cheek at eye height down to the cheekbone's lower edge) move along the
shortest path to the centre line by `cheekbones × 0.07 × weight` of their distance to it (weights
0.7, 1, 0.6), inward for a positive value and outward for a negative one, each reaching 0.2 of the
face's width. Unlike Width, the jaw and the chin get no push. The points are Width's own, so
[`FaceShape`] adds the two together. On the A13 at ±100 the edge across the cheekbones moves about
7 to 9 screen px on each side.

### Chin length and Hairline

Both move a group of points straight along the face's up-and-down line (from between the eyes to
the chin, so a tilted head moves along its tilt), through the shared
[`FaceVertical.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceVertical.kt):
each point moves `value × share × weight` of the eyes-to-chin length, each push reaching 0.25 of the
face's width.

- **Chin length**, [`FaceChinLength.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceChinLength.kt):
  the chin and its neighbours on the lower outline (`CHIN_AREA` 152, 148, 377, 176, 400; weights 1,
  0.8, 0.8, 0.5, 0.5) move up for a positive value (a shorter chin) and down for a negative one, by
  0.035 of the eyes-to-chin length at full strength. The mouth is above the pushes' reach.
- **Hairline**, [`FaceHairline.kt`](../../app/src/main/java/com/phongkaster/kanso/data/edit/face/FaceHairline.kt):
  the top of the forehead and its neighbours along the hairline (`HAIRLINE` 10, 109, 338, 67, 297;
  weights 1, 0.85, 0.85, 0.55, 0.55) move down for a positive value (a lower hairline, a smaller
  forehead) and up for a negative one, by 0.035 of the eyes-to-chin length. The middle moves most,
  so the hairline keeps its curve. The face mesh stops on the forehead's skin, below the real
  hairline, so each push is centred 0.2 of the eyes-to-chin length **above** its landmark, on the
  hairline, reaching 0.3 of the face's width; the eyebrows stay outside the reach. A first version
  centred on the landmarks moved the hairline only about 1 screen px on the A13.

## Two engine fixes found through Temple (2026-10-29)

- **No more seam at the edge of a push.** The warp used Gustafsson's falloff, which with a move
  much smaller than its reach (every Kanso tool) stays near full strength almost to the edge and
  then drops to nothing within a few pixels: the picture folded over itself there. On smooth skin
  (Temple) it showed as a hard line across the face. The falloff is now `w = (1 − d²/r²)²`, which
  is smooth everywhere; `LocalWarpTest` fails if a push ever folds the picture again.
- **No stale live frame over the result.** A live frame still being made when the full render
  finished could land after it and stay on screen over the result for good. `ToolViewModel` now
  drops any live frame asked for before the finished render began; `LiveLookRaceTest` forces that
  race.

## Tests

- [`tool/face/FaceCheekbonesTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/face/FaceCheekbonesTest.kt):
  only the outline's top three points per side are pushed (the jaw is not), inward for a positive
  value and outward for a negative one; values clamp; the moves scale with the face.
- [`tool/face/FaceChinAndHairlineTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/face/FaceChinAndHairlineTest.kt):
  a positive chin length moves the chin's points straight up, the chin most, a negative one down; a
  positive hairline moves the hairline's points down, the middle most, all above the eyes; values
  clamp; the moves scale with the face.

- [`tool/face/FaceTempleTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/face/FaceTempleTest.kt):
  0 and NaN give no pushes; a positive temple pushes both temples outward and a negative one inward,
  above the eyes; both sides mirror; values beyond ±1 clamp; the moves scale with the face.
- [`tool/face/FaceShapeTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/face/FaceShapeTest.kt):
  each of the six face tools alone gives exactly its own pushes.
- [`tool/face/LocalWarpTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/face/LocalWarpTest.kt):
  a small move in a large circle never folds the picture.
- [`tool/LiveLookRaceTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/LiveLookRaceTest.kt):
  a live frame older than the finished render is dropped; a newer drag still shows.
- `BeautifyProcessorTest`: Temple reshapes the face and needs one. `BeautifyToolTest`: Temple sits in
  Face's row and hands over its value alone; Cheekbones, Chin length and Hairline each hand over
  their own value with every other one at 0 (the test scrolls the row to Hairline).

## Known limits

- The numbers are tuned against Meitu on one portrait; other faces may want other values.
- Face's row now holds six tools, more than fit the A13's screen: Chin length and Hairline are
  reached by swiping the row.
- Hair or background right beside the face moves with it (no BG Lock).

[Back to README](../../README.md)
