# Engine: Remove background (cutout), Run 8 T-003

Source of truth: javap of `libs/localml.aar` (engine-digest-run8.md); no AAR source exists.

## Classes used / avoided
- USED: `com.photo.editor.localml.removebackground.SelfieSegmentationUC` - public no-arg constructor;
  `suspend fun invoke(bitmap: Bitmap, flag: Boolean): Bitmap` (also an `invoke(Mat, Boolean)`
  overload; `invoke$default` exists, so the Boolean has a default). Holds a private ML Kit
  `com.google.mlkit.vision.segmentation.Segmenter` (selfie model `mlkit.segmentation.selfie`,
  bundled, no download). Private `segmentationToMask(SegmentationMask, Bitmap)` and
  `segmentationToImageMask(SegmentationMask, Bitmap)` suggest the flag picks between them.
- AVOIDED: `SubjectSegmentationUC` (Play-services model, downloads on first use) and
  `RemoveBackgroundUC(SubjectSegmentationUC, SelfieSegmentationUC)` (falls back to it).
  `RefineMaskUC`, `ForegroundToMaskUC` take OpenCV `Mat`: not used.
- Reachability: `core/localml` declares the AAR with `api(...)`, so `app` sees the classes;
  `app` also depends on `:opencv` (`org.opencv.android.OpenCVLoader.initLocal(): boolean`).
- Retica (`reference/`) only registers `factory { SelfieSegmentationUC() }` in Koin; no call site.

## Kanso's choice (UNVERIFIED, needs the device check of DoD 7)
- `EngineSegmenter` calls `OpenCVLoader.initLocal()` (false -> IllegalStateException ->
  ModelUnavailable), then `uc.invoke(photo, true)` (true = assumed "image mask").
- Result -> mask: scaled to the photo's size if needed; if any pixel has alpha < 255 the
  probability is alpha/255, else max(R,G,B)/255 (an opaque black/white mask). So it works whichever
  of the two shapes the engine returns, UNLESS `true` returns the opaque photo itself (then the mask
  would be brightness: wrong). On the phone check: a person photo gives a clean cutout; if not, try
  `false`.
- The UC is built lazily on first segment and kept (building loads the model).

## App seam
- `data/edit/cutout/Segmenter.segment(photo): FloatArray?` - one probability (0..1) per pixel, row
  major; null = no subject.
- `MaskCompositor.composite(photo, mask)`: alpha 0 at p <= 0.4, 255 at p >= 0.6, linear between;
  RGB kept; ARGB_8888, `setHasAlpha(true)`. `hasSubject(mask)` = any p >= 0.5.
- `CutoutProcessor(segmenter, dispatcher = Default)`: fit to 2000, progress 0.1 / 0.8 / 1.0;
  null or all-background mask -> NoSubject; OOM -> OutOfMemory; LinkageError or Exception ->
  ModelUnavailable; CancellationException rethrown.
