# ISSUES

- No abandoned or unreachable tasks. No queued decisions.
- Awaiting a person (human criteria 5, 7, 11, 14, 16, 21, 27, 28, 32; pinch zoom of criterion 7 and camera on a real phone need a person): none driven yet; they are verified at the end of the run.
- Known flake: `ThemeColourSettingsTest` wallpaper test once failed with a Windows DataStore file-rename AccessDenied; passes on rerun.
- Recorded, not fixed: none.
- Review findings recorded, not fixed (Phase 2): (1) `ToolPhotoKeyTest` recreate/process-death runs a test host built from the same Navigation 3 parts, not `MainActivity`; the nav-host test covers tap -> key only. (2) `ToolScreen` sizes the decode from `LocalConfiguration`; on Android < 15 rotation can change that by the bar size and trigger a second decode. (3) `gallery_body_placeholder` and `gallery_album_label` strings are unused.
- Review findings recorded, not fixed (Phase 4): camera picture area is hard-coded `Color.Black` (viewfinder look; the reviewer says POLICIES wants theme colours: Verifier to judge on screen); `onShare` is a no-op hook in the nav host; `GalleryScreen.onOpenCamera` has a default `{}`; Share does nothing if MediaStore never learns a file:// photo on Android 8.1-9 (no message string). Pictures 10-13, 16, 17 re-recorded under the D-001 Baselines approval; 18/19 new.
- Review findings recorded, not fixed (Phase 3): turning logic exists twice (`uprightMatrix` in UprightJpeg.kt and `PhotoLoader.turnUpright`); the MediaStore name lookup on API 29+ does not filter by folder; kept camera shots older than a day are cleared only when a new shot is taken. Assumption A-007 (pictures 16/17 re-recorded).
