# ISSUES

## Run 4 (one class per file, one UiState per screen): Phase 3 done, Iteration 4

Phase 1 (the six screens), Phase 2 (the two guard tests) and Phase 3 (the last file splits) are built and verified. Every task is complete; nothing is abandoned or unreachable. Problems so far:

- **Review findings recorded and fixed (Phase 3, non-blocking):** a KDoc that gave a wrong reason for `internal` (`PickerMatchers.kt`) and one that still claimed the empty allow-list proves the checker on real code (`OneTypePerFileTest.kt`); both reworded in the Phase 3 checkpoint.
- **Recorded, not a defect (Phase 3):** `every_allowed_file_still_breaks_the_rule` passes at once now that `ALLOWED` is empty; the proof that the checker finds a second type and a misnamed type rests on the sample-source tests in the same class.

- **Review findings recorded, not fixed (Phase 2, Minor):** `ScreenStateTest` reads declared types only, so a ViewModel whose `val state: StateFlow<…UiState> = mutableState` (no `asStateFlow()`) passes, a `Flow<…>` property that holds a `MutableStateFlow` at run time passes, and a top-level extension property such as `val HomeViewModel.busy: StateFlow<Boolean>` in a ViewModel file is not a member of the class and is not seen. Its `var`/`Mutable…` check misses `ArrayList`, `HashMap` and arrays, and would report a local `var` inside a helper function in a `…UiState.kt` (loud, harmless). Neither test is a substitute for the Phase 1 review of how each state is built.
- **Not a defect, recorded:** the first Phase 2 combined run failed on `cleanTestDebugUnitTest` only because the engine's shell stood inside `app/build/test-results`; rerun green (Constraint in `knowledge/PROJECT.md`, suggestion S-001).

- **Review finding recorded, not fixed (Phase 1, Minor):** `CameraContent` (`CameraScreen.kt`) takes a `CameraUiState` but decides "a photo waits" from its `shotPicture` slot, not from `state.shot`, so a caller that passes a state holding a shot without the slot would get the live picture. The real screen passes the slot exactly when the state holds a shot, the output is identical, and the KDoc says so; `CameraAfterShotPreview` passes `shot = null` with a shot picture. Left as is.
- **Assumptions recorded:** A-001 to A-012 in `.harness/run/ASSUMPTIONS.md` (the readings of the PRD this plan takes). None is Tier 3.
- **Will await a person at the end of the run:** DoD 6 (walk the six screens on the phone; the same walk as Run 3's item 10 below, so one signed walk may close both) and DoD 15 (read the UiState explanation in the three Home files). They are listed here, as unsigned items, by the Verifier.
- **Left out by the PRD, recorded as questions in the DoD:** the unused strings `gallery_body_placeholder` and `gallery_album_label`; the no-op `onShare = {}` hook in the navigation host; the no-op default of `GalleryScreen.onOpenCamera`; `ViewModelClearedProbe` being test-only code in the app.

## Run 3 (previews): ended DONE_PARTIAL, Verifier Iteration 12

Every machine criterion re-proved on 177b1f8 by fresh runs: `assembleDebug`, `testDebugUnitTest` (198 tests, 0 failed), `verifyRoborazziDebug`, `lintDebug` all exit 0. No abandoned or unreachable tasks. The run is not `DONE` only because three criteria need a person.

### Awaiting a person, Run 3

Not driven on a device: the only device attached is the owner's Galaxy A13, and another app had focus (in use); no emulator was attached. Each item is unsigned. Tick it in `DECISIONS.md` or here when done.

- [ ] Run 3 7: **not looked at by anyone.** Driven only as far as criterion 2: every preview draws on the host, in light and dark, without an error. Open each of `GalleryScreen.kt`, `CameraScreen.kt`, `SavedScreen.kt`, `ToolScreen.kt`, `SettingsScreen.kt`, `HomeScreen.kt`, `PermissionGate.kt`, `AccessRefusedPanel.kt`, `KansoNavHost.kt`, `KansoTheme.kt` in Android Studio and switch to Split view. Expect each preview once in light and once in dark, readable, with sample content that looks real, and the dark one actually dark.
- [ ] Run 3 10: **not driven.** Install the debug build fresh on the phone with no permissions granted. Open Home, the gallery (allow photos), a tool with a photo, Save, the Saved screen, and Settings with the colour picker. Expect each to look and behave as after Run 2. Then open Camera from Home. Expect a live picture. Take a shot and expect Keep and Retake.
- [ ] Run 3 12: **reading only.** Read `HomeScreen.kt`, then `GalleryScreen.kt`. Expect a paragraph a beginner can follow that says why the screen is two composables (stateful and `…Content`), what each one does, and why the split makes a preview possible. Also expect the sample data to be explained where it is made.

### Run 3, recorded but not fixed

- Reviewer notes on T-009: a composable that appears only as a slot default argument counts as drawn. Two same-named functions in one file collapse in the coverage scan. A preview pair made of two night-only `@Preview`s would pass.
- `ToolSavingPreview` was dropped because its indeterminate spinner hung the render test, so the Saving state has no preview. A watchdog halts the test JVM on a hang and prints to stderr only, with no report.
- Assumptions: A-001 (stateless `KansoNavDisplay`), A-002 (`sampleSizeFor` exempt from DoD 5), A-003/A-004 (camera `LivePreview`, `ShotView` and gallery `PhotoThumbnail` exempt from DoD 1).
- Fixed during the run: the Windows DataStore rename flake (T-010) and the camera retake flake (T-011).

- Run 2 left no abandoned or unreachable tasks.
- Fixed (T-010): the Windows DataStore file-rename flake; 5/5 unit and 3/3 Roborazzi fresh runs green.
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
