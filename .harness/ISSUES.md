# ISSUES

- No abandoned or unreachable tasks. No queued decisions.
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
