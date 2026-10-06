# ISSUES

- No abandoned or unreachable tasks. No queued decisions.
- Awaiting a person (human criteria 5, 7, 11, 14, 16, 21, 27, 28, 32): none driven yet; they are verified at the end of the run.
- Known flake: `ThemeColourSettingsTest` wallpaper test once failed with a Windows DataStore file-rename AccessDenied; passes on rerun.
- Recorded, not fixed: none.
- Review findings recorded, not fixed (Phase 2): (1) `ToolPhotoKeyTest` recreate/process-death runs a test host built from the same Navigation 3 parts, not `MainActivity`; the nav-host test covers tap -> key only. (2) `ToolScreen` sizes the decode from `LocalConfiguration`; on Android < 15 rotation can change that by the bar size and trigger a second decode. (3) `gallery_body_placeholder` and `gallery_album_label` strings are unused.
- Review findings recorded, not fixed (Phase 3): turning logic exists twice (`uprightMatrix` in UprightJpeg.kt and `PhotoLoader.turnUpright`); the MediaStore name lookup on API 29+ does not filter by folder; kept camera shots older than a day are cleared only when a new shot is taken. Assumption A-007 (pictures 16/17 re-recorded).
