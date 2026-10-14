# Kanso

*Kanso (簡素) — Japanese for simplicity: leaving out what is not needed.*

Kanso is an Android photo editor with a simple, offline design: no ads, no analytics, no sign-in.
The manifest removes the `INTERNET` and `ACCESS_NETWORK_STATE` permissions that the engine
libraries would otherwise merge in (`app/src/main/AndroidManifest.xml`). A photo only leaves the
phone when you send it to another app with Share.

This README describes **what the code on branch `loop/kanso-influx-layout` actually does**. It is
not a roadmap. Where the UI promises something the code does not do yet, this file says so.

> **Current state, in one paragraph.** The app shell works from start to end: Home, a photo
> gallery for each tool, the camera, a screen for each tool, saving into `Pictures/Kanso`, a Saved
> screen with Share, the My edits tab and Settings. **No tool edits pixels yet.** Each of the five
> tool screens decodes the photo, shows it, and when you tap Save writes a copy of the *original*
> photo (only turned upright, see below). The panel under the photo says the tool's controls are
> "coming in a later run". The image engine in `core/` and `libs/` (OpenGL, OpenCV, TensorFlow Lite
> / LiteRT, MediaPipe, ML Kit) is compiled into the APK, but nothing in `app/` calls its editing
> code.

---

## Features

### Editing features

| Feature | Where you reach it | State | What really happens |
|---|---|---|---|
| **Edit** (crop, colour, filters) | Home shortcut, a "Recent edits" photo, the Camera button, "Edit" in the photo viewer | **UI only** | Shows the photo; Save writes an upright copy of the original. Its controls panel is an empty slot until this tool is built (Run 8). |
| **Remove background** | Home carousel banner and shortcut | **UI only** | Same as Edit. Its controls panel is an empty slot until this tool is built (Run 8). |
| **Enhance** | Home carousel banner and shortcut | **UI only** | Same as Edit. Its controls panel is an empty slot until this tool is built (Run 8). |
| **Retouch** | Home carousel banner and shortcut | **UI only** | Same as Edit. Its controls panel is an empty slot until this tool is built (Run 8). |
| **Artistic** | Home carousel banner and shortcut | **UI only** | Same as Edit. Its controls panel is an empty slot until this tool is built (Run 8). |
| **Turn upright (EXIF orientation)** | Automatic, on display and on Save | **Works** | The only change Kanso makes to pixels today. See [below](#turn-upright-the-one-pixel-change-kanso-makes). |
| Engine features in `core/` (crop UI, filters, brushes, AI models) | Nowhere | **Not wired** | Compiled into the APK; no code in `app/` calls them. See [The image engine](#the-image-engine-in-core-and-libs-built-but-not-called). |

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

All five tools share one screen, one ViewModel and one pipeline. They differ only in their title
and in the message in the panel under the photo.

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

Koin builds one `ToolViewModel(tool, photoUri, loader = PhotoLoader, probe, saver = PhotoSaver)` per
`ToolPhoto` entry. The ViewModel is scoped to that navigation entry and cleared when the entry is
popped. It depends on nothing from `core/`.

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

A Back button and the tool's name; the photo in a rounded card over a blurred copy of itself; a
"Replace" chip, which goes back to this tool's gallery (`replaceWithGallery`); an Undo button
(`tool-undo`, top right); a progress ring (`tool-progress`) and a plain-words failure message
(`tool-failure`) when an edit runs or fails; the tool's panel slot (`ToolPanel.kt`, empty for now);
and a full-width Save button, disabled while an edit runs. **No tool has controls yet.** The
pipeline they will use is in place: `ToolViewModel.apply(processor)` shrinks the shown bitmap to
the tool's working size (`data/edit/WorkingSize.kt`), runs a `ToolProcessor` off the main thread
with progress, and on success keeps the result as the shown photo and the previous one for Undo; a
failure (`EditFailure`) leaves the photo as it was.

**5. Saving** (`ToolViewModel.save()` → `data/photo/MediaStorePhotoSaver.kt`)

- `ToolViewModel.save()` calls `saver.save(photoUri)` with the **original Uri** for an unedited
  photo, and `saver.save(bitmap)` with the shown edited bitmap otherwise (JPEG q95, or PNG when the
  bitmap has an alpha channel; same name scheme, same size as shown). A second tap while a save is running is ignored.
- On Android 8.1 and 9 (API 27–28), `needsWritePermission()` is true until
  `WRITE_EXTERNAL_STORAGE` is granted, so the screen asks first. A refusal shows "Kanso needs
  permission to write to your storage to save the photo. Nothing was saved." Android 10+ asks for
  nothing.
- The full saving path is in [Saving and output](#saving-and-output).
- On success the ViewModel sends the new Uri through a `Channel` (delivered once), and the nav host
  replaces everything above Home with `Saved(tool, uri)`.

**Result:** whichever tool you open, the saved file is the original photo, turned upright if it had
an EXIF rotation, and otherwise identical byte for byte.

### Turn upright: the one pixel change Kanso makes

This is the only image transformation in the app's code today. It happens in two places, on
purpose separately:

| | On display | On save |
|---|---|---|
| Code | `data/photo/PhotoLoader.kt` (`turnUpright`) | `data/photo/UprightJpeg.kt` + `MediaStorePhotoSaver.prepare()` |
| Input | Down-sampled bitmap (≤ 2× screen edge) | Full-size decode (`BitmapFactory.decodeStream`, no sampling) |
| Transform | `Matrix` rotate/mirror for EXIF 2–8, `Bitmap.createBitmap(..., filter = true)` | Same matrix (`uprightMatrix`), applied by `applyUpright` |
| Output | Bitmap on screen | New JPEG, `Bitmap.CompressFormat.JPEG`, quality **95** (`UPRIGHT_JPEG_QUALITY`), **no EXIF block** |
| No orientation tag | Shown as decoded | **Original bytes copied untouched**, EXIF included |
| Out of memory | "Cannot be opened" message | Original bytes copied with their tag; the screen says "Saved. This photo is very large, so it was copied as it is and some apps may show it sideways." |

Camera shots go through this path too. `CameraXController` writes the rotation into the JPEG's EXIF
instead of turning the pixels, so a kept shot saved from a tool screen is normally decoded at full
size, turned, and re-encoded at quality 95.

### The image engine in `core/` and `libs/`: built, but not called

`app/build.gradle.kts` depends on every engine module (`:core:colorpicker`, `:core:common`,
`:core:editimage`, `:core:gallery`, `:core:imagecrop`, `:core:imageprocessor`, `:core:localml`,
`:core:overlaybrush`) and on `:opencv`, so all of them, with their native libraries and bundled
assets, are packed into the APK. Searching `app/src/main` for imports from these modules finds
exactly three:

- `com.photo.gallery.data.repository.MediaRepository` and `com.photo.gallery.model.GalleryBucket`
  (`core/gallery`), used by `data/media/MediaStorePhotoSource.kt` for the gallery;
- `com.library.colorpicker.HSVColorPicker` (`core/colorpicker`), used by
  `ui/settings/SettingsScreen.kt` for the custom theme colour.

Nothing calls the editing parts. This is what they hold:

| Module | Source in repo | What it contains (by its files and build script) | Called by Kanso? |
|---|---|---|---|
| `core/imagecrop` | Yes (Kotlin) | A Compose image cropper (`ImageCropper`, `CropState`, `CropShapes`, …) | No |
| `core/common` | Yes (Kotlin) | Shared Compose widgets and utilities (`BitmapUtil`, `TransformableImage`, sliders, sheets) | No |
| `core/editimage` | No: wraps `libs/editimage.aar` (9.3 MB) | Prebuilt; its assets include filter look-up maps under `assets/filter_materials/` | No |
| `core/imageprocessor` | No: wraps `libs/imageprocessor.aar` | Prebuilt; its assets include LUTs (`assets/lookup/`) and GLSL shaders (`assets/shaders/`) | No |
| `core/overlaybrush` | No: wraps `libs/overlaybrush.aar` | Prebuilt; its assets include brush stamps (`assets/brush_materials/`) | No |
| `core/localml` | No: wraps `libs/localml.aar` (23 MB) | Prebuilt; declares ML Kit (face mesh, pose, selfie and subject segmentation), MediaPipe Tasks Vision, LiteRT (+GPU) and `libs/tensorflow-lite-select-tf-ops.aar` | No |

The AI models are **bundled inside `libs/localml.aar`**, not downloaded. Its `assets/` holds
`face_landmarker.task`, `hifill_inpaint_512_int8.tflite`,
`image_stylization_inceptionv3_dr_predict.tflite`, `image_stylization_inceptionv3_dr_transfer.tflite`,
`magic_touch.tflite` and `realesrgan_64x64_float16.tflite`. What these prebuilt libraries do with
them happens inside the AARs. This repository has no source for them, and Kanso does not call
them, so this README makes no claim about how they process an image.

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
| **File name** | `Kanso_<yyyyMMdd_HHmmss>.jpg`; `_1`, `_2`, … (up to 9 999) when that name is taken. An existing file is never overwritten. |
| **MIME type written** | `image/jpeg` |
| **Content** | Upright re-encoded JPEG at quality 95, or the original bytes (see [Turn upright](#turn-upright-the-one-pixel-change-kanso-makes)) |
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
  by `DATA`, and if that fails it asks the media scanner (waiting up to 5 s). It then opens
  `Intent.createChooser(ACTION_SEND, image/jpeg)` with `FLAG_GRANT_READ_URI_PERMISSION` and the Uri
  in `ClipData`. If no content Uri is found, the share sheet does not open. "Edit another" opens
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
| `core/` | The image engine's modules, imported from Retica at commit `9d227a8`. Four of them (`editimage`, `imageprocessor`, `localml`, `overlaybrush`) wrap prebuilt libraries and have no source of their own. Only `core/gallery` and `core/colorpicker` are called by the app today. |
| `libs/` | Those prebuilt libraries (AARs), plus two TensorFlow Lite AARs. `tensorflow-lite-select-tf-ops.aar` is used by `core/localml`. No build file references `tensorflow-lite.aar`. |
| `opencv/` | The OpenCV Android SDK module the engine depends on. |
| `reference/retica-app/` | Retica's original app code, **read-only**: how the engine's use cases are called. It is not built, and nothing in Kanso imports it. Its build file, signing configuration and advertising code were left behind. |

The app itself is written by [Foreman](https://github.com/Phong-Kaster/Foreman), one goal at a time.
