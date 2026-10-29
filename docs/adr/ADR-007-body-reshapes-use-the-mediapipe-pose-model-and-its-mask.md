# Body reshapes use MediaPipe's pose model and its mask

The owner wants Meitu's Body > Reshape tools in Kanso, one at a time, starting with Waist (then
Arms, Legs, Chest). This record keeps the decisions that shape all of them, why they were taken,
and what they cost. A source path below without a prefix is under
`app/src/main/java/com/phongkaster/kanso/`.

## Context

- Beautify already reshapes faces: MediaPipe's face landmarker gives 478 points, `FaceWidth` and
  `FaceLift` turn the outline into `WarpPush`es, and `LocalWarp` (plain Kotlin) moves the pixels;
  `WarpMesh` draws the same pushes live on the GPU.
- Kanso had no way to find a body. Nothing in `libs/` holds a pose model.
- The A13 once crashed (native SIGBUS) when a second, older MediaPipe graph from the engine ran
  beside Kanso's face landmarker (see `docs/features/beautify.md`).
- Kanso is offline: every model ships in the app.

## What was decided

- **MediaPipe Pose Landmarker (full), bundled.** `pose_landmarker_full.task` (9.4 MB) sits in
  `app/src/main/assets/`, downloaded unchanged from Google's MediaPipe model storage (Apache 2.0).
  "Full" over "lite" for accuracy on still photos, over "heavy" (about 30 MB) for size. It runs on
  the same `mediapipe-tasks-vision` library as the face landmarker, so there is no second copy of
  MediaPipe's native code; on the A13 the two ran together without a crash (2026-10-29).
- **The model's own person mask finds the edges.** Pose points sit at the joints, not on the
  outline, so a tool walks across the mask from the body's centre line to the first background
  pixel (`data/edit/body/BodyEdges.kt`). The mask comes from the same call
  (`setOutputSegmentationMasks(true)`), so no third model (such as ML Kit's selfie segmenter) is
  loaded. Where the walk fails, the edge is guessed from the joints.
- **The same warp as the face.** Each body tool returns `WarpPush`es like `FaceWidth`; the processor
  warps faces and bodies in one `LocalWarp` pass and the live look draws them through one
  `WarpMesh`. A tool is one object in `data/edit/body/` plus one `BeautifyTool` entry naming
  `BeautifyGroup.BODY`.
- **A Body group in Beautify, not a new screen.** The top row is Face | Body | Smooth, as in
  Meitu. While Body is open the photo card shows the whole photo (the face zoom would hide the
  body).
- **Each model only when needed.** The pose model loads only when a body slider is off 0, as the
  face model loads only for a face slider; with smoothing at 0 no face is needed, so a body tool
  works on a photo whose face is too small to find. No person gives a new failure,
  `EditFailure.NoPerson` ("No person found in this photo. Nothing was changed.").
- **One slider value object.** The panel now hands the screen a `BeautifyAmounts` (every slider by
  name) instead of loose floats, since four or more values of one type are easy to swap.

## What it costs

- The app grows by 9.4 MB.
- The background and anything touching the body (an arm on the waist) bend with it until a
  BG Lock exists; the mask could later keep the background still.
- Two MediaPipe landmarkers stay in memory once both have been used on a screen.
