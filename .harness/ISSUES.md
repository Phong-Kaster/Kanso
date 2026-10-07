# ISSUES

- Run 3 (previews): D-001 approved; Phases 1-2 done. No abandoned or unreachable tasks. Assumptions A-002 (`sampleSizeFor` exempt from DoD 5's private-sample rule), A-003 (camera slot fillers `LivePreview`, `ShotView` and wrappers exempt from DoD 1 coverage; T-009 names them). Review minors, not fixed (assigned to T-009): comments inside a preview body can trip the forbidden-word scan; Koin is checked before and after drawing, not during. Flaky Windows Robolectric tests: see PROJECT.md.

- Run 2 left no abandoned or unreachable tasks.
- Known flake: `ThemeColourSettingsTest` wallpaper test once failed with a Windows DataStore file-rename AccessDenied; passes on rerun.
- Recorded, not fixed: none.
- Review findings recorded, not fixed (Phase 2): (1) `ToolPhotoKeyTest` recreate/process-death runs a test host built from the same Navigation 3 parts, not `MainActivity`; the nav-host test covers tap -> key only. (2) `ToolScreen` sizes the decode from `LocalConfiguration`; on Android < 15 rotation can change that by the bar size and trigger a second decode. (3) `gallery_body_placeholder` and `gallery_album_label` strings are unused.
- Review findings recorded, not fixed (Phase 4): camera picture area is hard-coded `Color.Black` (viewfinder look; the reviewer says POLICIES wants theme colours: Verifier to judge on screen); `onShare` is a no-op hook in the nav host; `GalleryScreen.onOpenCamera` has a default `{}`; Share does nothing if MediaStore never learns a file:// photo on Android 8.1-9 (no message string). Pictures 10-13, 16, 17 re-recorded under the D-001 Baselines approval; 18/19 new.
- Review findings recorded, not fixed (Phase 3): turning logic exists twice (`uprightMatrix` in UprightJpeg.kt and `PhotoLoader.turnUpright`); the MediaStore name lookup on API 29+ does not filter by folder; kept camera shots older than a day are cleared only when a new shot is taken. Assumption A-007 (pictures 16/17 re-recorded).

## Verifier result (Iteration 6)

Fresh, separate runs all exit 0: `assembleDebug`, `testDebugUnitTest`, `verifyRoborazziDebug`, `lintDebug`. `reference/` unchanged vs a0d9a79; no apero/firebase/play/billing on the debug runtime classpath; nav3 1.1.7, agp 8.13.0, abiFilters kept; merged manifest permissions match criterion 34; placeholder gone, pictures 7-9 absent.

## Awaiting a person

Not driven: the only device attached is a real phone (SM_A135F), not an emulator the build controls, and it may be in use. Each item below is "not driven", not "passed". Please tick when done.

- [ ] 5 Gallery scroll with 2,000+ photos: fling top to bottom on a phone; expect smooth, thumbnails fill in.
- [ ] 7 Camera: open Camera from Home; preview is live, shutter gives Keep/Retake, back/front switch, flash cycles auto/on/off, pinch zooms, Keep opens the tool with the shot.
- [ ] 11 Open Edit with a photo, press Home, kill the app (Recents or Developer options "don't keep activities"), reopen: same photo, upright and fitted.
- [ ] 14 Save a photo, open the system Photos/Files app: a Kanso album holds it, upright, full size.
- [ ] 16 On the Saved screen tap Share: system share sheet opens; share to Files or Messages. (Note: `onShare` is a no-op hook in the nav host per Phase 4 review; criterion 15's unit test covers the intent, check it really opens.)
- [ ] 21 Real permission dialogs: allow, "selected photos only", deny, then Settings and back, for gallery and camera.
- [ ] 27 Walk every new screen in light and dark: readable text, visible controls, shutter/keep/retake/Save clear of the gesture bar. Also judge the camera's hard-coded black picture area.
- [ ] 28 Read the gallery, permission, camera, save and navigation files: each concept is explained where first used.
- [ ] 32 Settings, Custom colour: drag the picker; smooth, and no transparency bar.

Run 1 (app shell; its DoD is at 57c9de7). These five were dropped from this file by 0670172 while
still unsigned, and restored from 288b590. Run 1's own numbers, so "Run 1 21" is not Run 2's 21.
19 and 22 were never driven: the emulator's focused window stayed `Application` (Run 1 A-005).

- [ ] Run 1 12 - Kill the app, open it from the launcher with theme mode on System → system splash (app icon on a plain background), then Home, with no extra screen, logo animation or flash of another colour.
- [ ] Run 1 19 - Android 12+ phone: Settings → theme colour → From wallpaper; change the phone's wallpaper → the app's accent colour follows.
- [ ] Run 1 21 - Open Home, each of the six tools, and Settings in light, dark and with a picked colour; in Settings open the custom colour picker, drag, read its hex field in dark mode → plain surfaces, readable text, every control visible, picker text readable, no ornament.
- [ ] Run 1 22 - Open a tool, back gesture → Home; back gesture again → app closes to the launcher.
- [ ] Run 1 26 - Read `app/src/main/java/com/phongkaster/kanso/navigation/` and the Home and Settings screen files → back stack, `NavDisplay`, `entryProvider`, per-entry ViewModels, `remember`/state and the theme are each explained in plain English where first used.

## Driven on a Galaxy A13, 2026-10-07

The checklist above was driven over adb on the human's Galaxy A13 (SM-A135F: Android 14, 32-bit
`armeabi-v7a`, 3-button navigation, 279 photos), debug build of addb7f5. Driven is not signed: the
boxes stay for the human. What was seen:

- Did not fail: Run 1 12, Run 1 21, Run 1 22; Run 2 11, 14, 21, 27. Run 2 7 except pinch to zoom
  (adb cannot pinch); Run 2 16 up to the share sheet showing "1 image" (no target was chosen).
- Not driven: Run 1 19 (changing the wallpaper is a phone setting), Run 1 26 and Run 2 28 (reading).
- Install: the debug APK had no `armeabi-v7a` and the A13 refused it (INSTALL_FAILED_NO_MATCHING_ABIS);
  fixed in addb7f5.

Defects found, not fixed:

- Cold start takes 5.5-6.2 s (`am start -W` TotalTime, three runs); the splash stays up the whole
  time and `Choreographer` reports 133 skipped frames on the main thread.
- The gallery stutters while flinging 279 photos: 70 of 95 frames janky (74%), 50th percentile 36 ms,
  90th 200 ms (`dumpsys gfxinfo`). Criterion 5 asks for smooth with 2,000+.
- Dragging the custom colour picker stutters: 28 of 30 frames janky (93%), 50th percentile 150 ms.
- In dark mode the colour picker sits on a white card from `core/colorpicker` that clashes with the
  dark screen.
- All four figures are from a debug build; measure a release build before deciding what to fix.
