# Engine notes: Retouch (Run 8, T-005)

Retica's own Retouch screen was object removal; its skin smoothing lived in its Edit screen. Kanso's
Retouch is skin smoothing only. Object removal was not copied.

## Pipeline (app/src/main/java/com/phongkaster/kanso/data/edit/retouch/)
1. `RetouchProcessor` (a `ToolProcessor`) fits the input to `WorkingSize.RETOUCH` (2000 px long edge).
2. `FaceFinder.findFaces(bitmap)` -> `List<android.graphics.Rect>` in pixels; empty -> `EditFailure.NoFace`.
   Rects are clamped to the bitmap by the processor; fully-outside rects are dropped.
3. `SkinSmoother.smooth(bitmap, face)` per face, chained (face 2 is smoothed on face 1's output);
   it returns a same-size copy, smoothed only inside the rect. A wrong-size answer -> ModelUnavailable.
4. Kotlin blend over the whole bitmap: `out = o + strength*(s-o)` per channel, rounded, alpha forced
   0xFF, `setHasAlpha(false)`. Strength (`@Volatile var strength`, default 1) is read once per run,
   clamped 0..1, NaN -> 1. Strength 0 -> a new bitmap equal to the input pixel for pixel.
5. Progress: 0, 0.05 (fit), 0.4 (faces found), up to 0.9 across faces, 1.
   OOM -> OutOfMemory; LinkageError/Exception -> ModelUnavailable; CancellationException rethrown.

## EngineFaceFinder
- `DetectFaceLandmarkUC(context.applicationContext)`, built lazily ONCE on the first photo and kept
  (model load is slow and the slider re-runs Retouch on one photo several times); a coroutine `Mutex`
  guards the build. `initModel(scope).join()` runs inside `coroutineScope { }` before the first invoke.
- `invoke(bitmap): List<List<UnifyPoint>>`, one list per face. Points are read as fractions 0..1 of
  width/height unless any coordinate > 1.5 (then pixels). Rect = landmark bounding box padded 10% of
  its size on each side, clamped. Unverified on a device until the Verifier's run (DoD 11).

## EngineSkinSmoother: why OpenCV bilateral, not the GL beauty shader
- Uses `OpenCVLoader.initLocal()` (false -> IllegalStateException -> ModelUnavailable),
  `Utils.bitmapToMat` (RGBA) on a mutable ARGB copy, `submat` of the face rect, RGBA->RGB,
  `Imgproc.bilateralFilter(d = min(faceW, faceH)/50 clamped 5..15, sigmaColor 40, sigmaSpace 8)`,
  RGB->RGBA, `copyTo(submat)`, `Utils.matToBitmap`. Every Mat (and the submat header) is released in
  `finally`. Bilateral needs 1 or 3 channels and src != dst, hence the conversions.
- Not `CompositeBeautyProgram` (core/imageprocessor): it renders through OpenGL and needs a thread that
  owns a GL context; the processor runs on `Dispatchers.Default`. OpenCV runs on any thread, is
  already bundled (`opencv/`), and is edge-preserving, which is what skin smoothing needs.

## Tests (app/src/test/java/com/phongkaster/kanso/tool/retouch/)
- `FakeFaceFinder` (records sizes, optional throw, `centre(photo)`, `none()`), `FakeSkinSmoother`
  (5x5 box blur inside the rect only, records sizes, optional throw).
- `RetouchToolTest.no_face_shows_the_message` needs ToolScreen to wrap the NoFace failure card in a
  node tagged `retouch-no-face` holding `tool_failed_no_face` (wired by the Iteration).
