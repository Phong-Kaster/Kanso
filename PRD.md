# Kanso — Run 2: pick, shoot, save

Run 1 built the shell: Home, Settings, the theme, and a placeholder screen for each tool. This run
builds the path every tool will share: **get a photo in, show it, and save it back out.** The editing
and AI tools themselves come in later runs; here each tool screen only shows the chosen photo.

## What this run delivers

1. **Gallery.** Edit, Remove background, Enhance, Retouch and Artistic each open an in-app gallery
   first: a grid of the phone's photos, newest first, with a way to switch between albums. Reuse
   `core/gallery` (`MediaRepository`) for reading them. Tapping a photo opens that tool's screen with
   the photo on it. The grid stays smooth with thousands of photos.
2. **Permission to read photos**, asked the first time the gallery opens, never at launch:
   - Android 13 and later: `READ_MEDIA_IMAGES`; Android 14 and later also handles the "selected
     photos only" choice, showing what was shared and a way to share more;
   - Android 12 and earlier: `READ_EXTERNAL_STORAGE`;
   - refused: the gallery says plainly why it needs access and offers a button to the app's system
     settings. Nothing crashes, nothing loops asking again.
3. **Camera.** The Camera tile on Home, and a camera button in the gallery's top bar, open an in-app
   camera (CameraX): live preview, shutter, switch between back and front lens, flash (auto, on, off),
   pinch to zoom. After a shot: keep it or retake. A kept shot opens the Edit tool screen (from Home)
   or the tool the gallery was opened for. Camera permission is asked when the camera first opens;
   refused is handled like the gallery's.
4. **The tool screen shows the photo** fitted to the screen, the right way up — a photo whose EXIF
   says it is rotated is displayed upright — and without loading a full-resolution 50-megapixel image
   just to display it. The chosen photo survives rotation and the process being killed in the
   background; carry it in the screen's Navigation 3 key.
5. **Save.** Each tool screen has a Save action. It writes the photo as a new JPEG in the phone's
   gallery, in a `Kanso` album (`Pictures/Kanso`, named `Kanso_<time>.jpg`), at full resolution and
   upright. The original is never changed or overwritten. Android 10 and later need no storage
   permission for this; the minimum Android version (8.1) may need `WRITE_EXTERNAL_STORAGE` up to
   Android 9 only.
6. **Saved screen**, after Save: the saved photo, and three actions — **Share** (the system share
   sheet, no hard-coded apps), **Edit another** (back to the gallery for the same tool), **Home**.

## Carry-over from Run 1

Run 1 recorded three review notes it did not fix (`.harness/ISSUES.md`). Fix them:

- the custom colour picker writes the theme to storage on every drag movement — write once the drag
  settles;
- the picker's transparency bar does nothing — make it work or hide it (`core/colorpicker` may be
  changed only as far as hiding it needs);
- the four theme-colour labels are constants — move them to `strings.xml`.

Run 1's five unsigned checks in `.harness/ISSUES.md` (12, 19, 21, 22, 26) are mine to sign. Leave
them there; this run does not need to re-verify them.

## Constraints

Everything from Run 1 still holds:

- **No ads, no analytics, no accounts, no network.** No network permission; nothing from Apero, no
  Firebase beyond what Run 1's A-003 allowed for ML Kit, no private Maven repository.
- **The code teaches.** Every class, function and composable gets a KDoc comment in plain English
  saying what it does and why. A concept is explained where it is first used: runtime permissions in
  Compose, `LazyVerticalGrid` and keys, CameraX's preview and capture use cases, `MediaStore` and
  scoped storage, passing data in a Navigation 3 key. Write for a reader who has never used them.
- **Roborazzi** checks the new screens in light and dark: the gallery grid (with fake photos), the
  gallery's "access refused" state, the camera's "access refused" state, a tool screen showing a
  photo, and the saved screen. A camera preview cannot render on the host; prove the camera on a
  device or emulator instead.
- Ask for no permission this run does not use. The debug build may keep only the ABIs Run 1 chose.
- `reference/` stays byte-for-byte unchanged.

## Out of scope for this run

Any editing or AI effect, camera filters, video, choosing several photos at once, and moving to
Android Gradle Plugin 9 (Run 1 pinned Navigation 3 1.1.7 for that reason; keep it).
