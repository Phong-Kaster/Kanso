# Engine facts for Retouch and Artistic (javap of localml.aar, Run 8 Iteration 5)

All bundled in the AAR assets (no download): `face_landmarker.task`, `image_stylization_inceptionv3_dr_predict.tflite`, `image_stylization_inceptionv3_dr_transfer.tflite`. The engine cannot load on the host JVM: only `Engine*` wrapper files may name engine classes; everything else is tested with fakes behind an interface.

## Face (package com.photo.editor.localml.facelandmark)
- `DetectFaceLandmarkUC(context: Context)`; `suspend fun invoke(Bitmap): List<List<UnifyPoint>>` one list of landmark points per face (empty list = no face). `fun initModel(scope: CoroutineScope): Job` (default arg exists: `initModel$default`), Retica calls it once before invoke; wait with `.join()`.
- `com.photo.editor.common.geometry.UnifyPoint(val x: Float, val y: Float)` (core/common). The ML Kit path divides by the bitmap size, so values are normalised 0..1 of width/height; be defensive: if any coordinate is above 1.5 treat the points as pixels. Face region = bounding box of a face's points, padded ~10%, clamped to the bitmap.
- Smoother: use OpenCV (already a module dependency, `opencv/`): `OpenCVLoader.initLocal()` first, then `Utils.bitmapToMat`, `Imgproc.bilateralFilter` (or `Photo.edgePreservingFilter`) on the face rectangle only, `Utils.matToBitmap`. Do not use CompositeBeautyProgram (needs a GL thread). Release Mats.

## Style (package com.photo.editor.localml.styletransfer)
- `StylePredictInceptionV3UC(context)`; `StyleTransferInceptionV3BlendUC(context, stylePredictUC)`.
- `suspend fun invoke(photo: Bitmap, style: Bitmap, ratio: Float): Bitmap` (Blend). Which end of `ratio` is "full style" is NOT visible: the app takes the stylizer at the ratio constant `FULL_STYLE_RATIO = 1f` named in one place with a KDoc saying it is unverified until the device run (DoD 13), and does the strength mixing itself in Kotlin. `fun initModel(scope: CoroutineScope): Job` exists, join it before invoke.
- Retica loaded a style image from assets with `assetToBitmap` (BitmapFactory.decodeStream(context.assets.open(path))). Retica's own styles: illustration, picasso, van_gogh webps; they are NOT in this repo: this run draws its own.
