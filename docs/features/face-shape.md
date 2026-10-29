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

All four move pixels only, with the same warp as Width and Lift; nothing is drawn or added.
They need a face: with none found, "No face found in this photo. Nothing was changed."

## How to use them

- Beautify → **Face** → the tool in the row (Width, Lift, **Temple**, …) → drag its slider. The
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

- [`tool/face/FaceTempleTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/face/FaceTempleTest.kt):
  0 and NaN give no pushes; a positive temple pushes both temples outward and a negative one inward,
  above the eyes; both sides mirror; values beyond ±1 clamp; the moves scale with the face.
- [`tool/face/FaceShapeTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/face/FaceShapeTest.kt):
  each tool alone gives exactly its own pushes.
- [`tool/face/LocalWarpTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/face/LocalWarpTest.kt):
  a small move in a large circle never folds the picture.
- [`tool/LiveLookRaceTest.kt`](../../app/src/test/java/com/phongkaster/kanso/tool/LiveLookRaceTest.kt):
  a live frame older than the finished render is dropped; a newer drag still shows.
- `BeautifyProcessorTest`: Temple reshapes the face and needs one. `BeautifyToolTest`: Temple sits in
  Face's row and hands over its value alone.

## Known limits

- The numbers are tuned against Meitu on one portrait; other faces may want other values.
- Hair or background right beside the face moves with it (no BG Lock).

[Back to README](../../README.md)
