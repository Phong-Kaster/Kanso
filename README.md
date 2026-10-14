# Kanso

*Kanso (簡素) — Japanese for simplicity: leaving out what is not needed.*

Kanso is an Android photo editor with a simple, offline design: no ads, no analytics, no sign-in.
The manifest removes the `INTERNET` and `ACCESS_NETWORK_STATE` permissions that the engine
libraries would otherwise merge in (`app/src/main/AndroidManifest.xml`). A photo only leaves the
phone when you send it to another app with Share.

This README describes **what the code on branch `loop/kanso-editing-tools` actually does**. It is
not a roadmap. Where the code does something only in part, or a step has not yet been checked on a
phone, this file says so.

> **Current state, in one paragraph.** The app works from start to end: Home, a photo gallery for
> each tool, the camera, a screen for each tool, saving into `Pictures/Kanso`, a Saved screen with
> Share, the My edits tab and Settings. **All five tools edit the photo.** Edit crops, turns, flips,
> adjusts colour and applies filters in plain Kotlin; Remove background, Enhance, Retouch and
> Artistic call the image engine's bundled on-device models (`libs/localml.aar`, through
> `core/localml`) and OpenCV, from one wrapper file each in `app/` (`Engine*.kt`). Nothing is
> downloaded. Each edit is one Undo step, and Save writes the picture shown on screen as a new file
> (a JPEG, or a PNG for a cut-out with a transparent background). An unedited photo is saved as a
> copy of the original, only turned upright. The engine calls have been built and tested with fakes
> on the computer; what the real models return has not yet been checked on a phone.

---

## Features

### Editing features

| Feature | Where you reach it | State | What really happens |
|---|---|---|---|
| **Edit** (crop, colour, filters) | Home shortcut, a "Recent edits" photo, the Camera button, "Edit" in the photo viewer | **Works** (not yet on a device) | `ui/tool/EditPanel.kt` sends each change as an `EditProcessor` (`data/edit/edit/`) to `ToolViewModel.apply` on a background thread: crop (free, 1:1, 4:3, 3:4, 16:9, 9:16), quarter turns, flips, then brightness/contrast/saturation/warmth (colour matrix, applied when the slider is let go, then back to the middle), then one of 7 filters built on the CPU from look-up images bundled in the engine AARs, with an intensity. Working size: 2000 px long edge. Each change is one Undo step; Save writes the shown result. Out of memory shows a message and keeps the photo. |
| **Remove background** | Home carousel banner and shortcut | **Works** (engine output unverified on a device) | `CutoutPanel` runs `CutoutProcessor` with `EngineSegmenter`, which loads OpenCV and calls the engine's `SelfieSegmentationUC` (`libs/localml.aar`), backed by ML Kit's selfie-segmentation model, which ships inside the app (no download); the photo is first shrunk to 2000 px. `MaskCompositor` makes pixels the mask calls background fully transparent; the result is shown over a checkerboard and saved as PNG. No subject shows its own message. |
| **Enhance** | Home carousel banner and shortcut | **Works** (engine output unverified on a device) | `EnhancePanel` runs `EnhanceProcessor`: the photo is shrunk to 512 px, `EngineUpscaler` runs the bundled Real-ESRGAN model (4x, so up to 2048 px) with progress, then `AutoLighting` lifts a dark photo toward mid-tones (a well-lit one is left alone). Press and hold the compare control to see the original. |
| **Retouch** | Home carousel banner and shortcut | **Works** (engine output unverified on a device) | `RetouchPanel` (a strength slider, default 100%, and "Smooth skin") runs `RetouchProcessor`: the photo is shrunk to 2000 px, `EngineFaceFinder` finds faces with the bundled MediaPipe landmark model of `libs/localml.aar` (no download), `EngineSkinSmoother` runs an OpenCV bilateral filter on each face rectangle only, and the smoothed picture is blended over the original by the strength in Kotlin (0 = the photo unchanged). No face shows "No face found in this photo. Nothing was changed." Retica's own Retouch screen was object removal; Kanso's Retouch is skin smoothing only. |
| **Artistic** | Home carousel banner and shortcut | **Works** (engine output unverified on a device) | `ArtisticPanel` (four style chips: Warm wash, Cool wash, Mosaic, Ink; a strength slider; "Apply style") runs `ArtisticProcessor`: the photo is shrunk to 1024 px, `EngineStylizer` runs the bundled Inception V3 style-prediction and style-transfer models once per style, and the styled picture is blended with the photo by the strength in Kotlin, so moving the slider does not run the model again. The four style images are drawn, not Retica's (`app/src/main/assets/styles/*.webp`, drawn by `StyleImages` as a fallback). Whether the engine's ratio 1 means "full style" (`FULL_STYLE_RATIO`) is unverified until a device run. |

### Other features

| Feature | State | Notes |
|---|---|---|
| Photo gallery with albums, per tool | Works | MediaStore, through `core/gallery`'s `MediaRepository`. Handles full, partial (Android 14+) and refused photo access. |
| Camera (take, keep or retake) | Works | CameraX; back/front lens, flash auto/on/off, pinch to zoom. |
| Save to `Pictures/Kanso` | Works | New file each time: a JPEG (quality 95), or a PNG when an edited picture carries transparency. The original is never changed. |
| Saved screen and Share | Works | Android share sheet with a one-off read grant. |
| My edits tab and photo viewer | Works | Lists what Kanso saved in `Pictures/Kanso`, newest first. |
| Home "Recent edits" strip | Works | The newest 10 saved photos. |
| Settings: theme mode and theme colour | Works | Light, dark or system; Kanso blue, wallpaper colours (Android 12+), or a custom colour from `core/colorpicker`'s `HSVColorPicker`. |
| Launcher icon that follows the theme colour | Works | The "enso lens" icon ships on nine backgrounds, one `<activity-alias>` each. `launcher/LauncherIcon.kt` picks the background closest in hue to the theme colour (greys get the near-black one), and `launcher/LauncherIconSwitcher.kt` enables that alias and disables the rest. The launcher takes a few seconds to redraw. |

---

## How each editing feature works

All five tools share one screen (`ui/tool/ToolScreen.kt`), one ViewModel
(`ui/tool/ToolViewModel.kt`) and one way of running an edit. They differ in their title, in the
panel of controls under the photo (`ui/tool/ToolPanel.kt` picks it), and in the processor that
does the edit (`data/edit/`). The shared part comes first, then one section per tool.

### The shared tool pipeline (Edit, Remove background, Enhance, Retouch, Artistic)

**1. Choosing a tool and a photo** (`navigation/KansoNavHost.kt`)

Paths below are under `app/src/main/java/com/phongkaster/kanso/` unless they start with `core/`,
`libs/` or `app/`.

- `navigation/Tool.kt` declares the five tools: `EDIT`, `REMOVE_BACKGROUND`, `ENHANCE`, `RETOUCH`,
  `ARTISTIC`. Each has only a label and an icon; there is no other per-tool data.
- Home (`ui/home/HomeViewModel.kt`) puts `REMOVE_BACKGROUND`, `ENHANCE`, `RETOUCH`, `ARTISTIC` on the
  carousel banners and all five on the round shortcuts. A tap pushes `Gallery(tool)`.
- In the gallery, picking a photo pushes `ToolPhoto(tool, photoUri)`. The photo travels as the text
  of its `content://` Uri inside the navigation key, which is `@Serializable`, so the same tool
  reopens on the same photo after rotation or process death (`navigation/ToolPhoto.kt`).
- Other ways in: a "Recent edits" photo on Home and "Edit" in the My edits viewer push
  `ToolPhoto(Tool.EDIT, uri)`. The floating Camera button pushes `Camera(Tool.EDIT)`; a gallery's
  camera button pushes `Camera(<that gallery's tool>)`. Keeping a shot replaces the camera with
  `ToolPhoto(tool, file://…)`.

**2. Building the screen** (`di/AppModule.kt`, `ui/tool/ToolViewModel.kt`)

Koin builds one `ToolViewModel(tool, photoUri, loader = PhotoLoader, probe, saver = PhotoSaver,
registry = ProcessorRegistry)` per `ToolPhoto` entry. The ViewModel is scoped to that navigation
entry and cleared when the entry is popped. It imports nothing from `core/` itself: the
`ProcessorRegistry` (`data/edit/ProcessorRegistry.kt`), built once in `di/AppModule.kt`, holds the
processor of Remove background, Enhance, Retouch and Artistic, each built on its engine wrapper.
Edit has no registry entry: its panel builds a new `EditProcessor` for each change.

**3. Decoding for display** (`data/photo/PhotoLoader.kt`)

- `ToolScreen` works out a size limit of **twice the screen's longest edge in pixels**
  (`displayMaxEdgePx`) and calls `viewModel.load(maxEdgePx)`. A repeat call with the same size
  does nothing.
- `PhotoLoader.load` runs on `Dispatchers.IO`:
  1. Reads only the bounds (`inJustDecodeBounds`).
  2. Decodes with `inSampleSize` = the smallest power of two that brings the longest edge to the
     limit or below (`sampleSizeFor`). The full-size image is never held in memory.
  3. Reads the EXIF `TAG_ORIENTATION` with AndroidX `ExifInterface` (a damaged block counts as
     upright).
  4. Turns or mirrors the bitmap with a `Matrix` for orientations 2 to 8 and recycles the
     original.
- Any exception, or an `OutOfMemoryError`, returns `null`. The screen then shows "This photo cannot
  be opened. Go back and pick another one." Cancellation is passed on.

**4. What the screen shows** (`ui/tool/ToolScreen.kt`)

A Back button and the tool's name; the photo (the last edit, or the original) in a rounded card
over a blurred copy of itself, or over a checkerboard (`ui/tool/Checkerboard.kt`) when Remove
background's result has transparent parts; a
"Replace" chip, which goes back to this tool's gallery (`replaceWithGallery`); an Undo button
(`tool-undo`, top right); a progress ring (`tool-progress`) and a plain-words failure message
(`tool-failure`) when an edit runs or fails; the tool's panel (`ToolPanel.kt`);
and a full-width Save button, disabled while an edit runs.

**5. Running an edit** (`ToolViewModel.apply`, `data/edit/ToolProcessor.kt`)

- A panel either hands the ViewModel a processor it built (Edit, through `apply`) or asks it to run
  the tool's registered one (`runRegistered`; Retouch and Artistic first set their strength and
  style on it). One edit runs at a time: a tap while one runs is ignored.
- `apply` shrinks the bitmap to the tool's working size (`data/edit/WorkingSize.kt`: Edit 2000 px,
  Remove background 2000 px, Enhance 512 px, Retouch 2000 px, Artistic 1024 px on the longest
  edge), then runs the `ToolProcessor` on `Dispatchers.Default`, reporting progress (0 to 1) to the
  progress ring. Edit, Remove background and Enhance work on the photo as now shown (the last
  edit); Retouch and Artistic always start again from the original.
- On success the result becomes the shown photo and the one before it is kept for Undo, in memory
  only. A slider re-run of Retouch or Artistic replaces its last result instead of adding an Undo
  step.
- A failure comes back as an `EditFailure` (`data/edit/EditFailure.kt`): out of memory, no
  subject, no face, model unavailable, or other. The screen shows its message and the photo and
  Undo stay as they were. Leaving the screen cancels a running edit.

**6. Saving** (`ToolViewModel.save()` → `data/photo/MediaStorePhotoSaver.kt`)

- `ToolViewModel.save()` calls `saver.save(photoUri)` with the **original Uri** for an unedited
  photo, and `saver.save(bitmap)` with the shown edited bitmap otherwise (JPEG quality 95, or a
  lossless PNG when the bitmap has an alpha channel; same name scheme, at exactly the size shown,
  not scaled back up). A second tap while a save is running, or a tap while an edit runs, is
  ignored.
- On Android 8.1 and 9 (API 27–28), `needsWritePermission()` is true until
  `WRITE_EXTERNAL_STORAGE` is granted, so the screen asks first. A refusal shows "Kanso needs
  permission to write to your storage to save the photo. Nothing was saved." Android 10+ asks for
  nothing.
- The full saving path is in [Saving and output](#saving-and-output).
- On success the ViewModel sends the new Uri through a `Channel` (delivered once), and the nav host
  replaces everything above Home with `Saved(tool, uri)`.

**Result:** after an edit, the saved file is the picture shown on screen, at its working size (at
most 2000 px for Edit, Remove background and Retouch, 2048 px for Enhance, 1024 px for Artistic,
and for all but Enhance never larger than the photo as decoded for display in step 3).
With no edit (or after undoing every edit), the saved file is the original photo, turned upright if
it had an EXIF rotation, and otherwise identical byte for byte.

### Edit (`data/edit/edit/`, `ui/tool/EditPanel.kt`)

1. `EditPanel` keeps what its controls show (`EditSettings`) and turns each change into a new
   `EditProcessor` holding that one change only, which `ToolScreen` passes to
   `ToolViewModel.apply`. A crop shape, a quarter turn or a flip is applied when tapped; the four
   colour sliders (brightness, contrast, saturation, warmth) when you let go, after which they spring
   back to the middle; a filter when its chip is tapped, at the intensity slider's value; a free
   crop when you tap "Crop" under its two range sliders. Each is one Undo step.
2. `ToolViewModel` shrinks the shown photo to 2000 px (`WorkingSize.EDIT`) and runs the processor
   on `Dispatchers.Default`; `EditProcessor` runs on `Dispatchers.Default` itself too.
3. `EditProcessor` works on the pixels as one `IntArray`, in this order: crop (free, or the
   largest centred 1:1, 4:3, 3:4, 16:9 or 9:16 rectangle, `CropRatio.kt`); quarter turns; flips;
   one colour matrix built from the four sliders (contrast around mid-grey, saturation, warmth
   shifting red against blue by up to 40, brightness by up to 80, on a 0-255 scale), skipped when
   all four are in the middle; then the filter, mixed with the unfiltered colour by the intensity.
4. The filters (`FilterPreset.kt`, `FilterLut.kt`) are 7 look-up images bundled in the engine's
   AARs and merged into the app's assets at build time: `lookup/lookup_light.png` from
   `libs/imageprocessor.aar` (a 64-level colour cube) and six `filter_materials/map/*Map.png` curve
   strips from `libs/editimage.aar` (1977, Kelvin, Nashville, Walden, Hudson, Amaro), read as one
   tone curve per channel. They are decoded once and kept. No engine code runs: the look-up is done
   in Kotlin on the CPU.
5. The result is a new opaque ARGB_8888 bitmap, so Save writes a JPEG at the size shown.
6. Failure: out of memory gives the out-of-memory message; a filter image that cannot be read
   gives "Something went wrong and the edit was not made. Your photo is unchanged." The photo stays
   as it was.

### Remove background (`data/edit/cutout/`, `ui/tool/CutoutPanel.kt`)

1. `CutoutPanel` shows one button, "Cut out the subject", which calls
   `ToolViewModel.runRegistered()`; the registry holds `CutoutProcessor(EngineSegmenter())`.
2. `ToolViewModel` shrinks the shown photo to 2000 px (`WorkingSize.REMOVE_BACKGROUND`), and
   `CutoutProcessor` shrinks it again if needed, so the engine never gets a larger one. It runs on
   `Dispatchers.Default`.
3. `EngineSegmenter` loads OpenCV's native library (`OpenCVLoader.initLocal()`), builds the
   engine's `SelfieSegmentationUC` (`libs/localml.aar`, through `core/localml`) once and keeps it,
   and calls it with the photo. That use case runs ML Kit's selfie-segmentation model, which
   `core/localml/build.gradle.kts` pulls in as a library that ships the model in the APK (no
   download). The engine's subject-segmentation paths use a Play-services model that downloads on
   first use, so Kanso never calls them. The selfie model is trained on people.
4. The engine's picture is scaled to the photo's size if needed and read as one foreground
   probability per pixel: from its alpha channel when it has see-through pixels, else from its
   brightness. What the engine really returns is still to be checked on a phone.
5. No mask, or no pixel at 0.5 or above: "No person or object stood out in this photo, so nothing
   was cut out. Your photo is unchanged."
6. `MaskCompositor` keeps every pixel's colour and sets its alpha from the mask: 0 up to 0.4, 255
   from 0.6, a straight ramp between, so the outline is soft. The result has an alpha channel; the
   screen shows it over a checkerboard, and Save writes it as a PNG (`image/png`) at the size shown.
7. Failure: out of memory gives the out-of-memory message; OpenCV or the engine failing to load or
   throwing gives "This tool could not start on this phone. Your photo is unchanged."

### Enhance (`data/edit/enhance/`, `ui/tool/EnhancePanel.kt`)

1. `EnhancePanel` shows one button, "Upscale and brighten", which calls
   `ToolViewModel.runRegistered()`; the registry holds `EnhanceProcessor(EngineUpscaler(context))`.
2. `ToolViewModel` and then `EnhanceProcessor` shrink the photo to 512 px (`WorkingSize.ENHANCE`),
   because the model makes it four times wider and taller: an unshrunk 4000 x 3000 photo would ask
   for about 768 MB. It runs on `Dispatchers.Default`.
3. `EngineUpscaler` builds a new `EnhanceImageUC` (`libs/localml.aar`, through `core/localml`) for
   each run, waits for its `initModel()`, and collects its progress stream until it reports a
   result or an error. The model, `realesrgan_64x64_float16.tflite` (Real-ESRGAN, run by
   TensorFlow Lite), is bundled in the AAR's assets: no download. Its progress fills the first 90%
   of the ring. The output is up to 2048 px on its longest edge.
4. `AutoLighting` measures the mean luminance. Below 110 (of 255) it applies a gamma curve to red,
   green and blue chosen to bring the mean toward 128, never stronger than gamma 0.4 and never
   darkening a pixel; a photo at 110 or above is left alone. Plain Kotlin.
5. The result is marked opaque, so Save writes a JPEG at the size shown (up to 2048 px).
6. Once there is a result, the panel shows a comparison picture above its button
   (`ui/tool/EnhanceCompare.kt`) with a "Hold to see the original" control: holding it fades the
   picture to the photo before Enhance, letting go fades back (a screen-reader double tap flips
   between the two).
7. Failure: out of memory gives the out-of-memory message; a model that will not load, or an error
   from the engine, gives "This tool could not start on this phone. Your photo is unchanged."

### Retouch (`data/edit/retouch/`, `ui/tool/RetouchPanel.kt`)

1. `RetouchPanel` (strength slider 0-1, default 1) calls `ToolViewModel.runRegistered(fromOriginal = true)`, which sets `RetouchProcessor.strength` and runs it on the untouched photo, so moving the slider and applying again replaces the last result (one Undo step for the whole slider session).
2. `ToolViewModel` shrinks the photo to 2000 px (`WorkingSize.RETOUCH`) and runs `RetouchProcessor` on `Dispatchers.Default`.
3. `EngineFaceFinder` (built once, kept) calls `DetectFaceLandmarkUC` (bundled `face_landmarker.task`) and turns each face's landmark points into a bounding box padded 10%.
4. No face: `EditFailure.NoFace`; the screen shows the message in a node tagged `retouch-no-face`, and the photo and Undo are untouched.
5. `EngineSkinSmoother` filters each face rectangle with OpenCV `bilateralFilter` (not the engine's GL beauty shader, which needs an OpenGL thread). Mats are released after use.
6. The Kotlin blend `out = original + strength * (smoothed - original)` is applied to every pixel; the result is opaque, so Save writes a JPEG at the size shown.
7. Failure: out of memory shows the out-of-memory message; a model or OpenCV that will not load shows the model message; the photo stays as it was.

### Artistic (`data/edit/artistic/`, `ui/tool/ArtisticPanel.kt`)

1. `ArtisticPanelHost` keeps the chosen style and strength and calls `runRegistered(fromOriginal = true)` which sets them on `ArtisticProcessor`. The original's working-size copy is made once, so the same bitmap reaches the processor every time.
2. The photo is shrunk to 1024 px (`WorkingSize.ARTISTIC`), run on `Dispatchers.Default`.
3. `ArtisticProcessor` remembers the last styled picture for the same input and style: a new strength re-mixes only.
4. Otherwise `EngineStylizer` loads the style image (`assets/styles/<id>.webp`, or draws it with `StyleImages`) and calls `StyleTransferInceptionV3BlendUC` (bundled `image_stylization_inceptionv3_dr_*.tflite`) on a copy of the photo.
5. Mix in Kotlin: strength 0 gives the (1024 px) photo, 1 the styled picture. Opaque result, saved as JPEG at the size shown.
6. Failures as for Retouch.

### Turn upright: saving an unedited photo

This is the only change made to a photo that no tool has edited: Save then copies the original,
turned upright if its EXIF tag says so. (An edited picture is already upright, because every tool
works on the decoded, turned bitmap, and is saved as it is shown; see step 6 above.) Turning
upright happens in two places, on purpose separately:

| | On display | On save |
|---|---|---|
| Code | `data/photo/PhotoLoader.kt` (`turnUpright`) | `data/photo/UprightJpeg.kt` + `MediaStorePhotoSaver.prepare()` |
| Input | Down-sampled bitmap (≤ 2× screen edge) | Full-size decode (`BitmapFactory.decodeStream`, no sampling) |
| Transform | `Matrix` rotate/mirror for EXIF 2–8, `Bitmap.createBitmap(..., filter = true)` | Same matrix (`uprightMatrix`), applied by `applyUpright` |
| Output | Bitmap on screen | New JPEG, `Bitmap.CompressFormat.JPEG`, quality **95** (`UPRIGHT_JPEG_QUALITY`), **no EXIF block** |
| No orientation tag | Shown as decoded | **Original bytes copied untouched**, EXIF included |
| Out of memory | "Cannot be opened" message | Original bytes copied with their tag; the screen says "Saved. This photo is very large, so it was copied as it is and some apps may show it sideways." |

Camera shots go through this path too. `CameraXController` writes the rotation into the JPEG's EXIF
instead of turning the pixels, so a kept shot saved unedited from a tool screen is normally decoded
at full size, turned, and re-encoded at quality 95.

### The image engine in `core/` and `libs/`: what each tool calls

`app/build.gradle.kts` depends on every engine module (`:core:colorpicker`, `:core:common`,
`:core:editimage`, `:core:gallery`, `:core:imagecrop`, `:core:imageprocessor`, `:core:localml`,
`:core:overlaybrush`) and on `:opencv`, so all of them, with their native libraries and bundled
assets, are packed into the APK. Only a few files in `app/src/main` import from them, and every
editing call goes through one wrapper file per engine step, so the rest of the app is tested on
the computer with fakes (the engine is native code and cannot load there):

| Tool | Wrapper file in `app/` | Engine class it calls | Module | Model or library (all bundled, none downloaded) |
|---|---|---|---|---|
| Edit | `data/edit/edit/FilterLut.kt` (reads images only) | none | `core/imageprocessor`, `core/editimage` (assets only) | Filter look-up images `lookup/lookup_light.png` and `filter_materials/map/*Map.png` |
| Remove background | `data/edit/cutout/EngineSegmenter.kt` | `SelfieSegmentationUC`; `OpenCVLoader` | `core/localml`; `opencv/` | ML Kit selfie segmentation (its library ships the model) |
| Enhance | `data/edit/enhance/EngineUpscaler.kt` | `EnhanceImageUC` | `core/localml` | `realesrgan_64x64_float16.tflite` (TensorFlow Lite) |
| Retouch | `data/edit/retouch/EngineFaceFinder.kt` | `DetectFaceLandmarkUC` (with `UnifyPoint` from `core/common`) | `core/localml` | `face_landmarker.task` |
| Retouch | `data/edit/retouch/EngineSkinSmoother.kt` | `Imgproc.bilateralFilter`, `Utils`, `Mat` | `opencv/` | OpenCV, no model |
| Artistic | `data/edit/artistic/EngineStylizer.kt` | `StylePredictInceptionV3UC`, `StyleTransferInceptionV3BlendUC` | `core/localml` | `image_stylization_inceptionv3_dr_predict.tflite`, `image_stylization_inceptionv3_dr_transfer.tflite` |

Outside editing, `data/media/MediaStorePhotoSource.kt` uses `core/gallery`'s `MediaRepository` and
`GalleryBucket` for the gallery, and `ui/settings/SettingsScreen.kt` uses `core/colorpicker`'s
`HSVColorPicker` for the custom theme colour. This is what the engine modules hold:

| Module | Source in repo | What it contains (by its files and build script) | Called by Kanso? |
|---|---|---|---|
| `core/imagecrop` | Yes (Kotlin) | A Compose image cropper (`ImageCropper`, `CropState`, `CropShapes`, …) | No: Edit's crop is Kanso's own |
| `core/common` | Yes (Kotlin) | Shared Compose widgets and utilities (`BitmapUtil`, `TransformableImage`, sliders, sheets) | Only its `UnifyPoint` type, the face points Retouch reads |
| `core/editimage` | No: wraps `libs/editimage.aar` (9.3 MB) | Prebuilt; its assets include filter look-up maps under `assets/filter_materials/` | No code; Edit reads six of its `filter_materials/map/` images |
| `core/imageprocessor` | No: wraps `libs/imageprocessor.aar` | Prebuilt; its assets include LUTs (`assets/lookup/`) and GLSL shaders (`assets/shaders/`) | No code; Edit reads its `lookup/lookup_light.png` image |
| `core/overlaybrush` | No: wraps `libs/overlaybrush.aar` | Prebuilt; its assets include brush stamps (`assets/brush_materials/`) | No |
| `core/localml` | No: wraps `libs/localml.aar` (23 MB) | Prebuilt; declares ML Kit (face mesh, pose, selfie and subject segmentation), MediaPipe Tasks Vision, LiteRT (+GPU) and `libs/tensorflow-lite-select-tf-ops.aar` | Yes: Remove background, Enhance, Retouch and Artistic (table above) |
| `opencv/` | Yes (OpenCV Android SDK) | OpenCV's Java API and native libraries | Yes: Remove background loads it, Retouch filters with it |

The AI models are **bundled inside `libs/localml.aar`**, not downloaded. Its `assets/` holds
`face_landmarker.task`, `hifill_inpaint_512_int8.tflite`,
`image_stylization_inceptionv3_dr_predict.tflite`, `image_stylization_inceptionv3_dr_transfer.tflite`,
`magic_touch.tflite` and `realesrgan_64x64_float16.tflite`. Kanso uses four of them (the face,
style and Real-ESRGAN models); `hifill_inpaint_512_int8.tflite` and `magic_touch.tflite` are packed
in the APK but not called. The selfie-segmentation model comes with ML Kit's library rather than
with these assets. The AARs ship no source, so what happens inside each engine call is known only
from its class files and from Retica's calls; this README describes what Kanso passes in and what
it does with the answer, and where the answer's exact form is unverified it says so.

`reference/retica-app/` shows how the original Retica app called these libraries. It is not built,
and nothing in Kanso imports it, so its behaviour is not Kanso's.

---

## Input: where photos come from

### The gallery (`ui/gallery/`, `data/media/MediaStorePhotoSource.kt`)

- **Permissions** (`ui/permission/PhotoPermissions.kt`, `PermissionGate.kt`):
  `READ_EXTERNAL_STORAGE` up to Android 12, `READ_MEDIA_IMAGES` on Android 13, and
  `READ_MEDIA_IMAGES` + `READ_MEDIA_VISUAL_USER_SELECTED` on Android 14+. With partial access
  the gallery says "You have shared only some of your photos with Kanso." and offers "Share more
  photos". After a refusal it shows "Photo access is off" with "Open settings".
- **Query:** `MediaRepository.getImageBuckets` for albums (sorted by name) and
  `getBucketImages(context, bucket)` for photos, filtered by `BUCKET_ID` and sorted
  `DATE_MODIFIED DESC`. Both run on `Dispatchers.IO`. Only the `content://` Uri is passed on, never
  the file path.
- The gallery reloads every time it comes back to the front. A failed read shows the empty state.
  If the chosen album has disappeared, it falls back to "All photos".

### The camera (`ui/camera/`)

- `CAMERA` permission through the same `PermissionGate`. The camera is not required
  (`uses-feature … required="false"`).
- `CameraXController` binds a `Preview` and an `ImageCapture` (`CAPTURE_MODE_MINIMIZE_LATENCY`) to
  the lifecycle. If the chosen lens is missing it uses the other one. Switching lens rebinds. Flash
  is a setting of `ImageCapture`. Zoom goes through `cameraControl.setZoomRatio`, clamped to the
  lens's range.
- A shot is written by CameraX to `filesDir/shots/shot_*.jpg` (private to the app). Each new shot
  first deletes shots older than 24 hours. Retake deletes the file. Leaving without Keep deletes
  it. Keep hands the tool screen a `file://` Uri, and the kept file is not deleted while the tool
  screen shows it.
- Failures: no camera, or the camera is busy → "The camera could not be started." A failed capture
  → "The photo could not be taken. Try again."

---

## Saving and output

`data/photo/MediaStorePhotoSaver.kt`, called from `ToolViewModel.save()`, all on `Dispatchers.IO`.

| | |
|---|---|
| **Folder** | `Pictures/Kanso/` |
| **File name** | `Kanso_<yyyyMMdd_HHmmss>.jpg`, or `.png` for an edited picture with an alpha channel; `_1`, `_2`, … (up to 9 999) when that name is taken. An existing file is never overwritten. |
| **MIME type written** | `image/jpeg` for an unedited photo and an opaque edited picture; `image/png` for an edited picture with an alpha channel (a Remove background cut-out) |
| **Content** | Unedited photo: upright re-encoded JPEG at quality 95, or the original bytes (see [Turn upright](#turn-upright-saving-an-unedited-photo)). Edited picture (`save(bitmap)`): the bitmap shown on screen, at the size shown, never scaled or turned, compressed as JPEG quality 95 when opaque (`EDITED_JPEG_QUALITY`) or as a lossless PNG keeping its transparency when it has an alpha channel (`saveFormatFor`). |
| **Original photo** | Opened only for reading. Never changed. |

- **Android 10+ (API 29+):** inserts a MediaStore row with `RELATIVE_PATH = Pictures/Kanso/` and
  `IS_PENDING = 1`, writes through `openOutputStream`, then sets `IS_PENDING = 0`. If anything fails
  after the row exists, the row is deleted. Free names are found first by querying `DISPLAY_NAME
  LIKE '<base>%'`.
- **Android 8.1–9 (API 27–28):** writes a file into
  `Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES)/Kanso/` (`createNewFile` claims
  the name atomically), deletes it if the write fails, then calls
  `MediaScannerConnection.scanFile`. The result is a `file://` Uri.
- **Failure:** any exception or `OutOfMemoryError` returns `SaveResult.Failed`, and the screen
  says "The photo could not be saved. Try again."
- **Format caveat, from the code:** a photo with no EXIF rotation is copied byte for byte. A PNG or
  WebP source with no rotation tag is therefore saved with its original bytes under a `.jpg` name
  and the `image/jpeg` MIME type. Only a rotated photo is re-encoded as a real JPEG.

### After saving (`ui/saved/`, `ui/myedits/`)

- **Saved screen:** decodes the new photo with the same `PhotoLoader`. **Share**
  (`ui/saved/SharePhoto.kt`) needs a `content://` Uri. On Android 8.1–9 it looks up the file's row
  by `DATA`, and if that fails it asks the media scanner (waiting up to 5 s). It then reads the
  photo's type (`shareMimeType`): the type MediaStore reports when it is an image type, else
  `image/png` for a name ending `.png` and `image/jpeg` otherwise, so a cut-out is shared as a
  PNG. It opens `Intent.createChooser(ACTION_SEND, <that type>)` with
  `FLAG_GRANT_READ_URI_PERMISSION` and the Uri in `ClipData`. If no content Uri is found, the share sheet does not open. "Edit another" opens
  the same tool's gallery.
- **My edits** and Home's **Recent edits** (`data/media/MediaStoreSavedPhotoSource.kt`) query
  MediaStore for `RELATIVE_PATH = 'Pictures/Kanso/'` (API 29+), or for a `_data` path match under
  `/storage/emulated/<n>/Pictures/Kanso/` with no subfolder (API 27–28), sorted
  `DATE_MODIFIED DESC`. Home keeps the newest 10. On API 27–28 this query needs read permission,
  which this class never asks for; without it the list is empty.

---

## Architecture

- **One activity** (`MainActivity`), in a task of its own. The launcher's `<activity-alias>` entries
  open `LauncherActivity`, which starts `MainActivity` and closes, so that disabling an alias when the
  icon colour changes never closes the app.
  `KansoApplication` starts Koin with `di/AppModule.kt`.
- **Navigation 3** (`navigation/KansoNavHost.kt`): one saved back stack of `@Serializable` keys —
  `Home`, `MyEdits`, `Settings` (tabs), `Gallery(tool)`, `Camera(tool)`, `ToolPhoto(tool, uri)`,
  `Saved(tool, uri)`, `EditedPhoto(uri)`. Each entry gets its own `ViewModelStore`. Tabs live in
  `KansoTabShell`, with a floating tab bar and a Camera button.
- **State:** every screen has one immutable `UiState` exposed as a `StateFlow` (the pattern is
  explained on `ui/home/HomeUiState.kt`); one-off events use a `Channel`.
- **Data:** `PhotoSource` / `SavedPhotoSource` (MediaStore reads), `PhotoLoader` (decode),
  `PhotoSaver` (write), `SettingsRepository` (Preferences DataStore at
  `files/datastore/settings.preferences_pb`).
- **UI:** Jetpack Compose with Material 3 1.5 (Expressive components), Coil for gallery thumbnails,
  MaterialKolor for building a colour scheme from a seed colour.
- **SDK levels:** `minSdk 27`, `targetSdk 36`, `compileSdk 37`, NDK `27.0.12077973`. Debug builds
  keep only the `x86_64`, `arm64-v8a` and `armeabi-v7a` ABIs.

---

## Building and testing

The app must be built with Gradle from the repository root:

```bash
./gradlew :app:assembleDebug          # build the debug APK
./gradlew :app:testDebugUnitTest      # JVM tests (Robolectric, Compose UI test)
./gradlew :app:verifyRoborazziDebug   # screenshot tests against the recorded images
./gradlew :app:lintDebug              # Android lint
```

All of them together:

```bash
./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue
```

The tests live in `app/src/test/` (about 80 test classes). They cover the screens, navigation, save,
camera (through a fake `CameraController`), settings, previews and screenshots. They need no device.

---

## Where the code comes from

| Path | What it is |
|---|---|
| `app/` | The Kanso app, written by Foreman. |
| `core/` | The image engine's modules, imported from Retica at commit `9d227a8`. Four of them (`editimage`, `imageprocessor`, `localml`, `overlaybrush`) wrap prebuilt libraries and have no source of their own. The app calls `core/gallery`, `core/colorpicker` and `core/localml`, uses one type of `core/common`, and reads filter images from `core/editimage` and `core/imageprocessor` (see [the engine section](#the-image-engine-in-core-and-libs-what-each-tool-calls)). |
| `libs/` | Those prebuilt libraries (AARs), plus two TensorFlow Lite AARs. `tensorflow-lite-select-tf-ops.aar` is used by `core/localml`. No build file references `tensorflow-lite.aar`. |
| `opencv/` | The OpenCV Android SDK module the engine depends on. |
| `reference/retica-app/` | Retica's original app code, **read-only**: how the engine's use cases are called. It is not built, and nothing in Kanso imports it. Its build file, signing configuration and advertising code were left behind. |

The app itself is written by [Foreman](https://github.com/Phong-Kaster/Foreman), one goal at a time.
