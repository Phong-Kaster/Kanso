# Kanso

*Kanso (簡素) — Japanese for simplicity: leaving out what is not needed.*

Kanso is an Android photo editor with a simple, offline design: no ads, no analytics, no sign-in.
The manifest removes the `INTERNET` and `ACCESS_NETWORK_STATE` permissions that the engine
libraries would otherwise merge in (`app/src/main/AndroidManifest.xml`). Every model it uses ships
inside the app; nothing is downloaded. A photo only leaves the phone when you send it to another
app with Share.

This README describes **what the code on branch `loop/kanso-editing-tools` actually does**. It is
not a roadmap. It is the index: each feature below links to its own page in
[`docs/features/`](docs/features/), which explains what the feature is, how to use it, how it works
step by step with file paths, which tests cover it, and what is still open.

> **Current state.** The app works from start to end: Home, a photo gallery for each tool, the
> camera, a screen for each of the five editing tools, saving into `Pictures/Kanso`, a Saved screen
> with Share, the My edits tab and Settings. All five tools edit the photo. Edit runs in plain
> Kotlin; Remove background, Enhance, Retouch and Artistic call the image engine's bundled on-device
> models (`libs/localml.aar`, through `core/localml`) and OpenCV. On the Android emulator (API 34,
> x86_64), on 2026-10-14, all five ran with no crash and each saved its result into
> `Pictures/Kanso` (Remove background as PNG, the others as JPEG); the camera, My edits, the photo
> viewer and Settings were also driven with no crash. Nothing has yet been checked on the owner's
> phone, a Galaxy A13.

---

## Features

### Editing tools

Every tool opens on its own screen with the same frame: Back, the tool's name, Undo, the photo in
a card with a Replace chip, the tool's controls, and Save.
[The tool screen](docs/features/tool-screen.md) explains that shared part: decoding, the working
size, progress, Undo, failures and Save.

| Feature | What it does | Saved as |
|---|---|---|
| [Edit](docs/features/edit.md) | Crop (free, 1:1, 4:3, 3:4, 16:9, 9:16), quarter turns, flips, brightness, contrast, saturation, warmth, and seven filters with an intensity, in plain Kotlin. | JPEG, at most 2000 px |
| [Remove background](docs/features/remove-background.md) | Cuts the person out with ML Kit's bundled selfie-segmentation model and makes the rest transparent, shown over a checkerboard. People only, in practice. | PNG with transparency, at most 2000 px |
| [Enhance](docs/features/enhance.md) | Shrinks the photo to 512 px, upscales it four times with the bundled Real-ESRGAN model, and brightens it if it is dark; hold a control to compare with the original. | JPEG, at most 2048 px |
| [Retouch](docs/features/retouch.md) | Finds faces with the bundled face-landmark model and smooths each face with OpenCV's edge-preserving bilateral filter, mixed by a strength slider. | JPEG, at most 2000 px |
| [Artistic](docs/features/artistic.md) | Repaints the photo in one of four styles (Warm wash, Cool wash, Mosaic, Ink) with the bundled Inception v3 style-transfer models, mixed by a strength slider. | JPEG, at most 1024 px |

### Getting a photo

| Feature | What it does |
|---|---|
| [Gallery and albums](docs/features/gallery-and-albums.md) | Each tool's photo picker: the phone's photos newest first, filtered by album, with full, partial (Android 14+) and refused photo access handled. |
| [Camera](docs/features/camera.md) | Takes a photo into Kanso's private storage, with lens switch, flash and pinch zoom; Keep opens it in the tool. |

### After editing

| Feature | What it does |
|---|---|
| [Saving, the Saved screen and Share](docs/features/saving-and-share.md) | Save writes a new file into `Pictures/Kanso` (the original is never changed); the Saved screen shows it; Share sends it through Android's share sheet with a one-off read grant. |
| [My edits and the photo viewer](docs/features/my-edits.md) | A tab listing everything Kanso saved, newest first; a tapped photo opens in a viewer with Share and Edit. |

### Around the app

| Feature | What it does |
|---|---|
| [Home](docs/features/home.md) | The carousel of the four AI tools, five round tool shortcuts, the Recent edits strip, and the floating tab bar with its Camera button. |
| [Settings: theme mode and theme colour](docs/features/settings-and-theme.md) | Light, dark or system; Kanso blue, wallpaper colours (Android 12+) or a custom colour. Applied at once and kept across restarts. |
| [Launcher icon](docs/features/launcher-icon.md) | The home-screen icon switches to the closest of nine background colours to match the theme colour. |

---

## Architecture

Paths are under `app/src/main/java/com/phongkaster/kanso/`.

- **One activity** (`MainActivity`) in a task of its own, drawn entirely with Jetpack Compose. The
  launcher's entries open `LauncherActivity`, which starts `MainActivity` and closes, so switching
  the icon colour never closes the app (see [Launcher icon](docs/features/launcher-icon.md)).
  `KansoApplication` starts Koin with `di/AppModule.kt`.
- **Navigation 3** (`navigation/KansoNavHost.kt`): one saved back stack of `@Serializable` keys:
  `Home`, `MyEdits`, `Settings` (the tabs), `Gallery(tool)`, `Camera(tool)`, `ToolPhoto(tool, uri)`,
  `Saved(tool, uri)`, `EditedPhoto(uri)`. Each entry gets its own `ViewModelStore`, so a ViewModel
  survives rotation and is cleared when its screen is popped. The tabs live in `KansoTabShell`,
  under a floating tab bar.
- **State:** every screen has one ViewModel exposing one immutable `UiState` as a `StateFlow` (the
  pattern is explained on `ui/home/HomeUiState.kt`); one-off events use a `Channel`. Each screen is
  split into a stateful composable and a stateless `…Content` composable with light and dark
  previews.
- **Editing:** every tool runs through one interface, `data/edit/ToolProcessor.kt`. Each engine
  call sits in one wrapper file (`Engine*.kt`) behind a small interface, so everything else is
  tested on the computer with fakes; the engine is native code and cannot load there.
- **Data:** `PhotoSource` and `SavedPhotoSource` (MediaStore reads), `PhotoLoader` (decode),
  `PhotoSaver` (write), `SettingsRepository` (Preferences DataStore at
  `files/datastore/settings.preferences_pb`).
- **UI:** Jetpack Compose with Material 3 1.5 (Expressive components and motion), Coil for
  thumbnails, MaterialKolor for colour schemes built from a seed colour.
- **SDK levels:** `minSdk 27`, `targetSdk 36`, `compileSdk 37`, NDK `27.0.12077973`. Debug builds
  keep the `x86_64`, `arm64-v8a` and `armeabi-v7a` ABIs.

### The image engine: what each tool calls

`app/build.gradle.kts` depends on every engine module and on `:opencv`, so all of them, with their
native libraries and bundled assets, are packed into the APK. Only these files of `app/` call them:

| Tool | Wrapper file in `app/` | Engine class | Model or library (all bundled) |
|---|---|---|---|
| Edit | `data/edit/edit/FilterLut.kt` (reads images only) | none | `lookup/lookup_light.png` (`libs/imageprocessor.aar`), `filter_materials/map/*Map.png` (`libs/editimage.aar`) |
| Remove background | `data/edit/cutout/EngineSegmenter.kt` | `SelfieSegmentationUC`, `OpenCVLoader` | ML Kit `segmentation-selfie` (its library ships the model) |
| Enhance | `data/edit/enhance/EngineUpscaler.kt` | `EnhanceImageUC` | `realesrgan_64x64_float16.tflite` |
| Retouch | `data/edit/retouch/EngineFaceFinder.kt` | `DetectFaceLandmarkUC` (with `UnifyPoint` from `core/common`) | `face_landmarker.task` |
| Retouch | `data/edit/retouch/EngineSkinSmoother.kt` | OpenCV `Imgproc.bilateralFilter` | OpenCV, no model |
| Artistic | `data/edit/artistic/EngineStylizer.kt` | `StylePredictInceptionV3UC`, `StyleTransferInceptionV3BlendUC` | `image_stylization_inceptionv3_dr_predict.tflite`, `image_stylization_inceptionv3_dr_transfer.tflite` |

Outside editing, `data/media/MediaStorePhotoSource.kt` uses `core/gallery`'s `MediaRepository`,
and `ui/settings/SettingsScreen.kt` uses `core/colorpicker`'s `HSVColorPicker`. The `.tflite` and
`.task` models sit in the assets of `libs/localml.aar`, which also holds
`hifill_inpaint_512_int8.tflite` and `magic_touch.tflite`; those two are packed in the APK but never
called. The AARs ship no source, so each feature page describes what Kanso passes in and what it
does with the answer.

---

## Building and testing

Build with Gradle from the repository root:

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

The tests live in `app/src/test/` (about 690 tests in about 90 test classes, plus 51 recorded
pictures in `app/src/test/screenshots/`). They cover the screens, navigation, every tool's
processor with fakes, saving, Share, the camera (through a fake `CameraController`), settings,
previews and screenshots. They need no device. The `Engine*.kt` wrappers are native code and are
only exercised on a device or emulator. Each feature page lists its own tests.

---

## Where the code comes from

| Path | What it is |
|---|---|
| `app/` | The Kanso app, written by Foreman. |
| `core/` | The image engine's modules, imported from Retica at commit `9d227a8`. Four of them (`editimage`, `imageprocessor`, `localml`, `overlaybrush`) wrap prebuilt libraries and have no source of their own. The app calls `core/gallery`, `core/colorpicker` and `core/localml`, uses one type of `core/common`, and reads filter images from `core/editimage` and `core/imageprocessor`. `core/imagecrop` and `core/overlaybrush` are built in but not called. |
| `libs/` | Those prebuilt libraries (AARs), plus two TensorFlow Lite AARs. `tensorflow-lite-select-tf-ops.aar` is used by `core/localml`. No build file references `tensorflow-lite.aar`. |
| `opencv/` | The OpenCV Android SDK module the engine depends on. |
| `reference/retica-app/` | Retica's original app code, **read-only**: how the engine's use cases are called. It is not built, and nothing in Kanso imports it. |
| `docs/features/` | One page per feature (linked above). |
| `docs/adr/` | The architecture decisions behind Kanso. |

The app itself is written by [Foreman](https://github.com/Phong-Kaster/Foreman), one goal at a time.
