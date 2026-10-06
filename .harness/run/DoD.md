# Definition of Done

> Derived from `PRD.md` (Run 2: pick, shoot, save) at bootstrap. Human-owned after approval: the engine may propose changes (Tier 3) but never apply them.
> Every criterion is written twice (ADR-035): one plain sentence for you to approve, and a `Proof:` line holding what a command needs.

## Status

- [ ] APPROVED — approve via the pending `.harness/run/ESCALATION.md` (D-001); edit criteria freely before approving.
      Approval covers the Verification Class of each criterion, not only its wording.

## Acceptance Criteria

Classes: `machine` = a command proves it. `machine-then-human` = a command drives it first, a person still looks and signs. `human-only` = nothing can drive it.
"Fake photos" means a fake photo source bound in Koin, because Robolectric has no real MediaStore. Host tests run under Robolectric with `@GraphicsMode(NATIVE)`; the SDK level is named per criterion with `@Config(sdk = [...])`. The camera hardware sits behind a Koin-bound boundary so the host can fake it.

### Behaviour

1. [machine] Edit, Remove background, Enhance, Retouch and Artistic on Home each open a photo gallery for that tool, not a placeholder.
   Proof: Compose test, `@Config(sdk = [35])`, photo access granted with `shadowOf(app).grantPermissions`, fake photos bound. Tap each of the five tiles in turn from a fresh Home: the top of the back stack is the Gallery key carrying that tool, the gallery screen's root tag is shown, and Back returns to Home.
2. [machine] The gallery lists photos newest first, and choosing an album shows only that album's photos; "All photos" shows every photo.
   Proof: same state, a fake source with three albums and photos with distinct dates. Grid item order equals dates descending. Choosing album B shows exactly B's photos in that order. Choosing "All photos" shows the union. The real app-side source keeps the engine's own order (`MediaRepository`, date modified, newest first; assumption A-004).
3. [machine] Tapping a photo opens the tool's screen with that photo on it.
   Proof: same state. Tapping item k makes the top key the tool-screen key holding that tool and item k's content URI (as text), and the tool screen's image node is tagged with that URI.
4. [machine] The gallery stays light with thousands of photos: it builds only what is on screen, keys its items by photo id, and reads the photo library off the main thread.
   Proof: fake source of 5,000 photos. After load fewer than 100 grid item nodes exist; `performScrollToIndex(4999)` shows the last item; the fake records the calling thread and the test asserts it is not the main looper thread; after the list reloads with one new photo at the top, the scroll position still shows the same photo it showed before (stable keys). Source check: the grid is a `LazyVerticalGrid` with `key =`.
5. [machine-then-human] On a phone with thousands of photos the gallery scrolls smoothly and thumbnails fill in quickly.
   Proof: emulator with at least 2,000 images pushed and media-scanned, fresh install, permission granted; fling top to bottom twice through `foreman-device.ps1` and read frame stats if the wrapper allows, else report "not driven". Written as "did not fail when driven from 2,000 images on the emulator". A person then flings on a phone and signs.
6. [machine] The Camera tile on Home and the camera button in the gallery's top bar open the camera; keeping a shot opens the Edit tool from Home, or the gallery's own tool from the gallery; retaking returns to the live preview.
   Proof: `@Config(sdk = [35])`, camera permission granted, fake camera returning a fixture JPEG. Home → Camera → Keep: top key is the tool screen for Edit with the shot's URI, and the camera key is no longer in the back stack. Gallery(Retouch) → camera button → Keep: tool screen for Retouch. Retake: camera key still on top, the shutter tag shown again, and the shot's file no longer exists.
7. [machine-then-human] On a real camera the live preview shows, the shutter takes a shot, the back/front switch works, flash cycles auto, on, off, pinch zooms, and keep/retake behave.
   Proof: emulator (API 34 or 36, emulated back and front cameras) through `foreman-device.ps1`, fresh install, camera permission granted when asked. Driven: the preview is not a black frame, the shutter shows keep/retake, switch changes the lens, the flash control shows three different states, keep opens the tool screen with the shot. Pinch cannot be injected, so a person does it on a phone and signs. If the emulator's covering system window blocks `dump`/`tap` (Run 1 A-005), it is reported "not driven".
8. [machine] The tool screen shows a photo upright, even when its EXIF says it is rotated.
   Proof: a fixture JPEG 400x200 pixels with EXIF orientation 6 goes through the app's display decoder and comes out 200x400; on the tool screen at `sdk = [35]` the drawn image is taller than wide and its bounds lie inside the content area (fitted, not cropped or overflowing). A second fixture with orientation 1 is unchanged.
9. [machine] The tool screen never loads a huge photo at full size just to show it.
   Proof: the decoder's sample-size calculation for an 8000x6000 image (read from bounds only) on a 411x891 dp, xxhdpi screen gives a decoded longest edge of at most twice the screen's longest edge in pixels and under 8000, and a real decode of a small generated JPEG honours that sample size; no code on the display route calls the engine's `uriToBitmap`.
10. [machine] The chosen photo and tool survive rotation and the app being killed in the background, because they travel in the navigation key.
    Proof: from the tool screen for (Edit, URI): `ActivityScenario.recreate()` shows the same tool and URI; for process death the saved-state Bundle is captured, Koin is stopped and restarted, a new activity restores from the Bundle and shows the same tool and URI; the key round-trips through kotlinx-serialization JSON unchanged.
11. [machine-then-human] On a phone, a photo chosen and then left in the background until Android kills the app is still shown, upright and fitted, when the app is reopened.
    Proof: emulator, fresh install, push an EXIF-6 fixture, open Edit with it, press Home, kill the process (device wrapper, else "not driven"), reopen from Recents: same tool screen and photo. A person checks it is upright and fitted and signs.
12. [machine] Save writes a new full-resolution, upright JPEG named `Kanso_<time>.jpg` into the phone's `Pictures/Kanso` album and never changes the original.
    Proof: `sdk = [35]`, a fake `media` ContentProvider backed by a temp directory. The insert has `RELATIVE_PATH` `Pictures/Kanso/`, `DISPLAY_NAME` matching `Kanso_\d{8}_\d{6}(_\d+)?\.jpg`, `MIME_TYPE` `image/jpeg`, `IS_PENDING` 1 then cleared. The written bytes are a JPEG whose pixel size is the source's upright size (a 4000x3000 EXIF-6 fixture gives 3000x4000) with EXIF orientation normal or absent. The source file's SHA-256 before and after is equal. A photo with no rotation is copied byte for byte. A rotated photo whose decode fails (the test injects a decoder that throws `OutOfMemoryError`) falls back to a verbatim copy that keeps its EXIF tag (assumption A-001) and says so on screen. Two saves in the same second give two different files, never an overwrite (also at `sdk = [28]`).
13. [machine] On Android 8.1 and 9 Save asks for storage-write permission and writes into `Pictures/Kanso`; on Android 10 and later it asks for nothing.
    Proof: `sdk = [27]` and `[28]`, permission not granted: Save requests `WRITE_EXTERNAL_STORAGE`; granting writes `<Pictures>/Kanso/Kanso_*.jpg` and runs a media scan; denying shows a message, writes no file and does not crash. `sdk = [29]` and `[35]`: Save makes no permission request.
14. [machine-then-human] A saved photo shows up in the phone's own gallery app in a Kanso album, upright and full size.
    Proof: emulator API 36, fresh install: save through the tool screen, then read `relative_path`, `width`, `height` of the new row through an allowed op, else "not driven". A person opens the system Photos or Files app and signs.
15. [machine] After Save a Saved screen shows the saved photo; Share opens the system share sheet, Edit another returns to that tool's gallery, Home returns to Home.
    Proof: `sdk = [35]`, fake saver returning a URI. Top key is the Saved key holding the tool and the URI; the photo node is shown. Share fires an `ACTION_CHOOSER` wrapping an `ACTION_SEND` of type `image/jpeg` with `EXTRA_STREAM` and the read-grant flag, no package or component set (`nextStartedActivity`). Edit another leaves the back stack as `[Home, Gallery(tool)]`; Home leaves `[Home]`.
16. [machine-then-human] Sharing from the Saved screen really hands the photo to another app.
    Proof: emulator after a Save, tap Share: the focused window belongs to the system chooser (device wrapper, else "not driven"). A person shares to Files or Messages and signs.

### Permissions the user is asked for

17. [machine] A fresh install asks for nothing at launch, and the first time the gallery opens it asks for the photo permission that fits the Android version.
    Proof: nothing granted. After launch no permission has been requested. After tapping Edit the request is: `sdk = [34]` and `[35]` `READ_MEDIA_IMAGES` plus `READ_MEDIA_VISUAL_USER_SELECTED`; `[33]` `READ_MEDIA_IMAGES` only; `[32]` and `[27]` `READ_EXTERNAL_STORAGE` only.
18. [machine] If photo access is refused, the gallery says plainly why it needs it and offers a button to the app's system settings; nothing crashes and it does not ask again by itself.
    Proof: `sdk = [35]` and `[30]`, request answered "denied": the refused-state tag, an explanation text and the settings button are shown; the button starts `ACTION_APPLICATION_DETAILS_SETTINGS` for `package:com.phongkaster.kanso`; the request count stays 1 after `recreate()` and after pause/resume.
19. [machine] On Android 14 and later, when only some photos are shared, the gallery shows those photos, says that only some are shared, and offers a way to share more.
    Proof: `sdk = [34]`, only `READ_MEDIA_VISUAL_USER_SELECTED` granted: the partial-access notice tag and the fake photos are shown; tapping "share more" issues a new request for both permissions.
20. [machine] The camera asks for camera permission the first time it opens, and a refusal is handled like the gallery's.
    Proof: `sdk = [35]`, camera not granted. Launch requests nothing; tapping Camera requests `CAMERA`; denial shows the camera refused-state tag, an explanation and a settings button that fires the app-details intent; no new request after `recreate()`.
21. [machine-then-human] The real system permission dialogs behave as described: allow, "selected photos only", deny, and the trip to settings and back.
    Proof: emulator API 34 or 36, uninstalled first; through the device wrapper where it allows (it has install/clear/grant/revoke/start), else "not driven". After revoking both permissions, reopening Edit shows the refused state and its button reaches App info; same for Camera. A person signs the dialogs themselves.

### Appearance and reachability

22. [machine] The gallery with photos looks as approved in light and dark.
    Proof: `./gradlew :app:verifyRoborazziDebug` exits 0 with `app/src/test/screenshots/10_gallery_light.png` and `11_gallery_dark.png` present; fake photos, permission granted, light/dark forced through Settings, `sdk = [30]`, the Run 1 phone qualifiers. The engine opens each image before committing it.
23. [machine] The gallery's "access refused" state looks as approved in light and dark.
    Proof: as 22 with `12_gallery_refused_light.png` and `13_gallery_refused_dark.png`.
24. [machine] The camera's "access refused" state looks as approved in light and dark.
    Proof: as 22 with `14_camera_refused_light.png` and `15_camera_refused_dark.png` (the live preview is not rendered on the host; criterion 7 covers it).
25. [machine] A tool screen showing a photo, with a visible Save action, looks as approved in light and dark.
    Proof: as 22 with `16_tool_photo_light.png` and `17_tool_photo_dark.png`, Edit with a fixture photo.
26. [machine] The Saved screen looks as approved in light and dark.
    Proof: as 22 with `18_saved_light.png` and `19_saved_dark.png`.
27. [machine-then-human] On a phone in light and dark, every new screen has readable text and visible controls, and the shutter, keep/retake, Save and bottom actions sit clear of the gesture bar.
    Proof: emulator or phone with gesture navigation: walk the gallery (with album switcher and partial-access notice), both refused states, camera, tool screen and Saved screen, in both modes; the engine records screen dumps where it can drive. A person signs against POLICIES § User-Interface Defects.
28. [human-only] The code teaches: runtime permissions in Compose, the lazy grid and its keys, CameraX's preview and capture use cases, `MediaStore` and scoped storage, and passing data in a navigation key are each explained in plain English where first used.
    Proof: read the gallery, permission, camera, save and navigation files; each concept is explained at its first use, enough to follow without outside reading. A person signs.

### Data and storage

29. [machine] Dragging the custom colour picker saves the theme once, after the drag settles, not on every movement.
    Proof: ViewModel test with a counting fake repository and a test dispatcher: 50 colour changes 16 ms apart give 0 writes; after the settle interval, exactly 1 write holding the last colour. The colour still survives a restart (existing `ThemeColourRestartTest` passes).
30. [machine] The colour picker no longer shows a transparency bar or percentage that do nothing.
    Proof: Settings with Custom chosen: no alpha bar node, no alpha percentage text and no transparent sizing `"100%"` text in the semantics tree. `git diff 288b590 -- core/colorpicker` shows only one Boolean parameter defaulting to `true` plus conditions around the alpha bar and the percentage box in `HSVColorPicker.kt`; the other caller of `HSVColorPicker` (`core/common` colour-picker bottom sheet) is unchanged and still compiles.
31. [machine] The four theme-colour labels come from the app's string resources, not from constants.
    Proof: `app/src/main/res/values/strings.xml` holds the four labels; none of those four literals appears in `app/src/main/java`; the Settings tests pass.
32. [machine-then-human] Dragging the colour picker on a phone feels smooth and the picker looks complete without the transparency bar.
    Proof: emulator, Settings → Custom, drag through the device wrapper if it can; logcat shows no burst of DataStore writes. A person signs.

### Build, start and quality

33. [machine] The app builds, the unit and screenshot tests pass, lint passes, and the app starts on the emulator without crashing.
    Proof: `./gradlew :app:assembleDebug`, `:app:testDebugUnitTest`, `:app:verifyRoborazziDebug` and `:app:lintDebug` each exit 0 in separate unpiped runs; on a fresh install `-Op start` shows no `FATAL EXCEPTION` for the package in logcat (or the criterion records "not driven").
34. [machine] The app asks for no network access, and its only new permissions are photo reading, camera, and storage writing on old Android.
    Proof: merged debug and release manifests contain no `INTERNET` or `ACCESS_NETWORK_STATE`; compared with `288b590`'s merged manifest the only additions are `READ_MEDIA_IMAGES`, `READ_MEDIA_VISUAL_USER_SELECTED`, `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"`, `CAMERA`, `WRITE_EXTERNAL_STORAGE` with `maxSdkVersion="28"`, plus a camera `uses-feature` with `required="false"`.
35. [machine] Every class, function and composable in the app code has a plain-English KDoc comment.
    Proof: `KdocCoverageTest` passes over all files this run adds.
36. [machine] The read-only reference folder is unchanged.
    Proof: `git diff --quiet a0d9a79 -- reference/` exits 0.
37. [machine] The build adds nothing from Apero or Firebase beyond Run 1, no private Maven repository, and keeps Navigation 3 at 1.1.7, AGP 8.x and the two debug ABIs.
    Proof: `:app:dependencies` for the debug and release runtime classpaths contain no coordinate with `apero`, `firebase-analytics`, `firebase-messaging`, `firebase-config`, `firebase-crashlytics`, `com.google.android.play:`, `billing`; repository blocks in the Gradle files are unchanged from `288b590` (no credentials); `libs.versions.toml` has `nav3 = "1.1.7"` and `agp = "8.13.0"`; `app/build.gradle.kts` keeps `abiFilters += listOf("x86_64", "arm64-v8a")`. New libraries this run are CameraX, Coil (and its test-only image-loader fake if the screenshots need one) and ExifInterface only.

## Baselines

Approving this DoD asks you to list these four existing reference images under a `## Baselines` heading in `.harness/run/DECISIONS.md` when you answer D-001 (the engine can never re-record or delete an approved baseline itself, ADR-036):

- `app/src/test/screenshots/6_settings_custom_colour.png` — re-recorded, because hiding the transparency bar changes the picker if the picture shows it (criterion 30).
- `app/src/test/screenshots/7_placeholder_light.png`, `8_placeholder_dark.png`, `9_placeholder_custom_colour.png` — removed with the placeholder screen (R1, R3).

Without that listing, the tasks that need them stay deferred and the run ends `DONE_PARTIAL` (assumption A-006).

## Removals

Repository class (from PROJECT.md): **product** — only what Run 2 replaces or leaves unreachable is removed.

R1. [machine] The "coming soon" placeholder tool screen is gone; no tool opens it any more.
    Proof: `Placeholder`, `PlaceholderScreen`, `PlaceholderViewModel` and `PLACEHOLDER_SCREEN_TAG` appear nowhere in `app/src` (`grep -rn --exclude-dir=build`); `BackArrowIcon` moves out of the `placeholder` package because Settings still uses it.
R2. [machine] The manifest no longer strips the storage permissions outright.
    Proof: `app/src/main/AndroidManifest.xml` has no `tools:node="remove"` for `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`; both appear with the `maxSdkVersion` caps from criterion 34, and the merged manifest agrees.
R3. [machine] The placeholder screenshot tests and their three pictures are gone.
    Proof: `7_placeholder_light.png`, `8_placeholder_dark.png`, `9_placeholder_custom_colour.png` do not exist and `ScreenshotTest` holds no placeholder test (needs the `## Baselines` listing above).

Questions (not removals, for you): `ViewModelClearedProbe` still lives in main code (Run 1 note) — left in place this run; `Tool.CAMERA` stays a Home tile with its own route rather than a tool screen.

## Constraints

- No ads, analytics, accounts or network; no network permission. Nothing from Apero; no Firebase beyond Run 1's ML Kit infrastructure.
- Every class, function and composable carries a KDoc; concepts are explained where first used.
- `reference/` byte-for-byte unchanged; Navigation 3 stays 1.1.7 and AGP 8.x; no permission this run does not use; the debug build keeps its two ABIs.
- `core/gallery` and the other engine modules are reused as they are, except the one `core/colorpicker` change in criterion 30.

## Verification Evidence Required

| Criterion | Class | Evidence / what to look at | Signed off |
|---|---|---|---|
| 1-4, 6, 8-10, 12, 13, 15, 17-20, 29-31 | machine | the named Robolectric / unit tests in `./gradlew :app:testDebugUnitTest` | n/a |
| 22-26 | machine | `./gradlew :app:verifyRoborazziDebug` against the named pictures | n/a |
| 33-37, R1-R3 | machine | the commands in each Proof | n/a |
| 5 | machine-then-human | fling a 2,000-photo gallery, read jank | ☐ |
| 7 | machine-then-human | camera on emulator, then pinch on a phone | ☐ |
| 11 | machine-then-human | kill the app in the background and reopen | ☐ |
| 14 | machine-then-human | open the system gallery, find the Kanso album | ☐ |
| 16 | machine-then-human | share to another app | ☐ |
| 21 | machine-then-human | real permission dialogs | ☐ |
| 27 | machine-then-human | look at every new screen, light and dark | ☐ |
| 32 | machine-then-human | drag the colour picker | ☐ |
| 28 | human-only | read the explanatory comments | ☐ |
