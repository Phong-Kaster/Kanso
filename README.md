# Kanso

*Kanso (簡素) — Japanese for simplicity: leaving out what is not needed.*

Kanso is a photo editor that keeps only the tools. No ads, no analytics, no sign-in, and nothing
leaves your phone: every edit and every AI effect runs on the device.

- **AI tools** — remove a background, enhance a photo, erase an object, turn a photo into art,
  and retouch a face with beauty and makeup.
- **Editing** — crop and rotate, adjust colour and light, and apply filters.
- **Camera and gallery** — shoot or pick a photo, then save the result back to your gallery.
- **Your look** — Material 3 in light, dark or follow-the-system mode, with a theme colour you pick
  yourself or take from your wallpaper.

Built with Jetpack Compose, Navigation 3 and Koin, on an image engine of OpenGL, OpenCV,
TensorFlow Lite, MediaPipe and ML Kit.

> Removing the background of an object uses ML Kit's subject segmentation, whose model Google Play
> services downloads the first time it is used. Everything else works offline from the first launch.

## Where the code comes from

| Path | What it is |
|---|---|
| `core/` | The image engine's modules, imported from Retica at commit `9d227a8`. Four of them (`editimage`, `imageprocessor`, `localml`, `overlaybrush`) wrap prebuilt libraries and carry no source of their own. |
| `libs/` | Those prebuilt libraries (AARs), plus TensorFlow Lite. |
| `opencv/` | The OpenCV Android SDK module the engine depends on. |
| `reference/retica-app/` | Retica's original app code, **read-only**: how the engine's use cases are called. It is not built, and nothing in Kanso imports it. Its build file, signing configuration and advertising code were left behind. |

The app itself is written by [Foreman](https://github.com/Phong-Kaster/Foreman), one goal at a time.

## Status

Setting up: the engine is imported, the app is not written yet.
