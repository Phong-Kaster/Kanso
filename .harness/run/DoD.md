# Definition of Done — Run 9: the Edit screen becomes a real editor

> Derived from `PRD.md` at bootstrap. Human-owned after approval: the engine may propose changes (Tier 3) but never apply them.
> Test class names below are the names the run will create; the Verifier runs them with `./gradlew :app:testDebugUnitTest --tests "*<Name>"`.
> Run on the Loop Branch `loop/kanso-real-editor` (see assumption A-001: the PRD said `main`, the engine may not touch it).

## Status

- [x] APPROVED — approve via the pending `.harness/run/ESCALATION.md` (D-001); edit criteria freely before approving.
      Approval covers the Verification Class of each criterion, not only its wording, **and the full list of pictures criterion 22 re-records**.

## Acceptance Criteria

### Behaviour

1. [machine] The photo on the Edit screen takes 60% of the space below the top bar and never changes size, whichever tool is open or closed.
   Proof: `EditLayoutTest` (Robolectric, 4000x3000 sample photo, fresh session, no sheet open). Measure the bounds of `TOOL_CARD_TAG`, then open each of the four sheets in turn, then close it, and measure again: every measurement is equal and the height is 0.60 x (window height minus the top bar's bottom edge), within 1 dp.
2. [machine] Under the photo is a row of four tools — Crop, Rotate, Adjust, Filters — each an icon over its name; the open one is highlighted, and one whose settings are not neutral shows a small dot.
   Proof: `EditToolRowTest`, from a fresh session. Exactly four entries in `EditTool` order (`CROP, ROTATE, ADJUST, FILTERS`), each with an icon and a label, none selected, no dot. Open Adjust: Adjust selected. Set brightness to 30: Adjust has the dot, the other three do not. Undo: no dot. Repeat for Crop (a ratio chip), Rotate (one turn) and Filters (1977).
3. [machine] Tapping a tool opens a sheet that fills the rest of the screen below the photo, with nothing dimming the photo.
   Proof: `EditLayoutTest`, from a fresh session, tap each entry. The sheet's top edge is at or below the row's bottom edge, its bottom reaches the window's bottom, no scrim node exists (no `ModalBottomSheet`), and the pixel at the photo's centre is identical with the sheet closed and open (draw the content view into a Bitmap, see `ScreenPixels.kt`).
4. [machine] The photo changes while a slider is still being dragged, before the finger lifts.
   Proof: `LivePreviewTest`. For each of brightness, contrast, saturation and warmth: start at 0, send `down` and `moveBy` without `up`, and read the preview's applied colour matrix: it differs from the identity before `up`. For filter intensity: before `up` the preview-render request for the new intensity has been issued and its bitmap differs from the one before the drag. A crop-rectangle move, a quarter turn and a flip change the preview's geometry (`graphicsLayer` or the cropper region) the moment they are made.
5. [machine] What I set is remembered for the whole session: brightness 70 is still 70 when I open Adjust again, and no slider springs back.
   Proof: `EditSessionTest`, from a fresh session. Open Adjust, set brightness to 70 and release, open Filters, open Adjust again: the slider reads 70, its label reads "70", and the session's brightness is 0.70.
6. [machine] The preview is always the original photo with every current setting applied; nothing is stacked or baked in as I go.
   Proof: `EditSessionTest` with a counting fake `ToolProcessor`/renderer. Set brightness to 50 and release, then to 50 again: the renderer's input is the original bitmap each time (same instance), the settings equal `EditSettings(brightness = 0.5f)`, and no full-size render happened before Done.
7. [machine] Undo takes back one action at a time, in reverse order, across sheets; a slider release, a filter chip, a crop and a quarter turn are one action each.
   Proof: `EditUndoTest`, from a fresh session: brightness 30 (release), Filters to Kelvin, Rotate right, Crop 1:1 (one gesture release). Undo four times: after each, the settings equal the state before that action; Undo is then disabled. A drag with many moves and one release adds exactly one action.
8. [machine] Switching Compare on shows the untouched original and disables every editing control at once; switching it off shows my edit again and re-enables them.
   Proof: `CompareTest`, with non-neutral settings and the Adjust sheet open. Compare on: the preview shows the original (no colour matrix, no geometry, no filter bitmap) and every row entry, slider, chip, turn button, cropper and Undo is `assertIsNotEnabled` (the compare switch itself, Done and Back stay enabled). Compare off: the edit is shown and all are enabled again.
9. [machine] Done renders the settings at full size and writes nothing, then opens the Result screen.
   Proof: `DoneResultTest` with `FakePhotoSaver` and `FakeMediaProvider`, Edit with brightness set. Tap Done: saver calls 0, MediaStore inserts 0, the back stack's top is `Result`, and the rendered bitmap has the size `EditProcessor` gives for the photo after crop and turns (`WorkingSize.EDIT` cap unchanged, A-005).
10. [machine] The Result screen has Share, Save and Go home; Save is the only one that writes a picture to the device.
   Proof: `DoneResultTest` from Result: Share writes no gallery picture (it hands a cache file to the share sheet, A-004), Go home leaves the back stack `[Home]` with 0 inserts, and a fresh Result plus Save gives exactly 1 insert.
11. [machine] Back from Result returns to my editing session with all my settings intact.
   Proof: `DoneResultTest`: settings brightness 70, Kelvin at 40, one turn; Done; system Back. The Edit screen is on top, the settings are equal, and the undo stack is as it was before Done (A-003). Also: a Result opened with no rendered picture in the hand-off store (the process was restarted) shows a plain "Nothing to show" message and Back leaves it without a crash (A-004).
12. [machine] Leaving Edit with changes asks "Discard your changes?" with Discard or Keep editing; leaving without changes asks nothing.
   Proof: `DiscardDialogTest`, system Back and the top-bar Back each driven from (a) a neutral session: leaves, no dialog; (b) brightness 30: dialog shows; "Keep editing" leaves the session unchanged; "Discard" pops to the previous key.
13. [machine] Crop is a real cropper with handles, drag to move and pinch to zoom, driven by the `core/imagecrop` module, and the ratio chips lock the rectangle's shape.
   Proof: `EditCropTest`, Crop sheet open on a fresh session: the app source imports `com.mr0xf00.easycrop` (grep) and the tree holds `CropperPreview`; a corner drag changes the region, an inside drag moves it, a two-pointer pinch changes the view's zoom; tapping 1:1 makes the region's aspect 1.0 (+/-0.01) and it stays 1.0 after a handle drag.
14. [machine] Rotate offers turn left, turn right, flip across and flip upside down.
   Proof: `EditSessionTest`: each of the four controls changes `quarterTurns`, `flipHorizontal` or `flipVertical` by exactly one step and is one undo action; four right turns give the starting settings back.
15. [machine] Adjust has Brightness, Contrast, Saturation and Warmth, each running from -100 through 0 to +100, with the number shown beside its name and 0 in the centre.
   Proof: `AdjustSheetTest`: four sliders, in that order; for each, setting -100, 0, 37 and 100 shows exactly "-100", "0", "37", "100" beside the name; the default shows "0" and the thumb is at the track's centre.
16. [machine] Filters offers None and the seven presets, one at a time, with an intensity from 0 to 100 that changes the photo while dragged.
   Proof: `FiltersSheetTest`: eight chips (None + the seven `FilterPreset` values); tapping Kelvin then 1977 leaves only 1977 selected and `settings.filter == 1977`; None clears it; the intensity slider spans 0..100, defaults to 100 when a filter is first chosen, and a drag without `up` changes the preview bitmap (criterion 4).
17. [machine] The photo I see while editing agrees with the photo Done renders.
   Proof: `ColourMatrixParityTest`: on a fixed 64x64 gradient and a grid of brightness, contrast, saturation and warmth values (-100, -50, 0, 50, 100, singly and in combinations), applying the preview's `ColorMatrix` to the pixels and running `EditProcessor` with the same settings differ by at most 2 (of 255) per channel (A-006); the preview side is run through Android's own `ColorMatrixColorFilter` on a real `Canvas` (not through the shared matrix builder compared with itself, which would prove nothing), with Compose's offsets in 0-255 units; the same test with a filter set and intensity 100 and 50 proves the order filter-then-colour matches.
18. [machine] A slider released while another change is still being worked on is no longer lost.
   Proof: `EditSessionTest` with a deliberately slow fake filter render: release brightness, then contrast before the first render ends. Both settings are in the session, the undo stack holds two actions, and the final render uses both.
19. [machine] The other four tools (Remove background, Enhance, Retouch, Artistic) keep their screens with no row and no sheets; their bottom Save becomes Done at the top right and leads to Result.
   Proof: `DoneResultTest` parameterised over the four tools, from a loaded photo with one result applied: a Done control exists at the top right, no `EditTool` row and no sheet exist, no Save node exists, Done writes 0 files and puts `Result` on top; Back returns to that tool with its result intact.
20. [machine-then-human] On the Galaxy A13 (32-bit Android), dragging a colour slider or the filter intensity on a 12-megapixel photo stays smooth.
   Proof: install the `armeabi-v7a` debug build through `foreman-device.ps1` (only when nobody is using the phone), open a 12 MP gallery photo in Edit, drag each slider and the intensity for 5 s with `gfxinfo` reset before each, record jank %. Then a person drags and signs. If the filter preview cannot keep up the Verifier says so plainly and the DoD is to be changed by the owner; it is never passed quietly. Not driven when the phone is in use.

### Appearance and reachability

21. [machine-then-human] Undo and Compare float together in one small pill at the bottom right of the photo and stay reachable while a sheet is open.
   Proof: `EditLayoutTest` with each sheet open: the pill lies inside the card, its right and bottom edges within 16 dp of the card's, both controls are at least 48 dp square and clickable (`TouchTargetCheck`), and `BottomClearanceTest` shows nothing under the system gesture area. Then a person looks at pictures 8 and 10 of criterion 22.
22. [machine-then-human] The new and changed screens look right in light and dark, and every picture that changes is one the owner has approved here.
   Proof: `verifyRoborazziDebug` passes after recording with `foreman-record-baselines.ps1`. Pictures (light and dark each): (1) tool row, no sheet; (2) Crop sheet; (3) Rotate sheet; (4) Adjust sheet with brightness 70; (5) Filters sheet with 1977; (6) compare switched on; (7) Result with Save; (8) the pill over the photo. **Re-recorded, approved here:** `tool_edit_free_crop_light/dark`, and — because Done replaces the bottom Save bar and `Saved` becomes `Result` — every existing tool and Saved/viewer picture that shows them (`tool_photo`, `tool_narrow_photo`, `editor_enhance`, the cutout, retouch and artistic pictures, `tool_enhance_done`, `saved`, `photo_viewer`; the Verifier lists the exact set from `git diff --name-status` of `app/src/test/screenshots`). Each old picture is kept beside its new one in `.harness/run/evidence/baseline-diffs/` for the run. Then a person opens that folder and signs.
23. [human-only] The photo stays bright above an open sheet, the highlighted tool and the dot are easy to notice, and the sheets read clearly.
   Proof: a person opens the Edit screen on a phone or emulator, opens each sheet, and expects a bright unshaded photo, an obvious highlight on the open tool, a visible dot after a change, and no control hidden behind the system bars.

### Data and storage

24. [machine] Save on Result writes exactly one picture with the same format, quality and folder as before this run.
   Proof: the rewritten `SaveEditedTest` and `MediaStoreSaverTest` from Result with `FakeMediaProvider`: one insert, with the `RELATIVE_PATH`, MIME type and quality equal to the values the same tests recorded at the start of the run (commit `eccbf95`).
25. [machine] The Edit tool's settings are one object for the whole session, and the processor renders them against the original photo, never against a photo it already changed.
   Proof: the rewritten `EditProcessorTest` renders a full `EditSettings` from the original and its expected pixels come from the original; `grep -n "one change" app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditSettings.kt app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditProcessor.kt` prints nothing.

### Build, start and quality

26. [machine] The app builds and opens without crashing, including the Edit screen, its four sheets and Result.
   Proof: `./gradlew :app:assembleDebug :app:testDebugUnitTest` exit 0 from the repository root with `timeout`; `LaunchTest` and `ScreenStateTest` cover Result and the sheets.
27. [machine] Every new or changed composable has a light and a dark preview built from sample data, and no preview touches Koin, a ViewModel, MediaStore, the camera or the network.
   Proof: `PreviewCoverageTest`, `PreviewHygieneTest` and `PreviewsRenderTest` pass.
28. [machine] One class per file; every screen has a ViewModel and one UiState; every class and function has a plain-English KDoc.
   Proof: `OneTypePerFileTest`, `ScreenStateTest` and `KdocCoverageTest` pass; `ui/result/` holds `ResultScreen.kt`, `ResultViewModel.kt`, `ResultUiState.kt`; `data/edit/edit/EditTool.kt` declares `EditTool` with `CROP, ROTATE, ADJUST, FILTERS`.
29. [machine] The app still has no network permission, no new animation library, and `reference/` is untouched.
   Proof: the merged debug manifest declares neither `INTERNET` nor `ACCESS_NETWORK_STATE`; `git diff eccbf95 -- reference/` is empty; `git diff eccbf95 -- gradle/libs.versions.toml app/build.gradle.kts` adds no animation artifact; every animation in `app/src/main` uses Compose's own APIs.
30. [machine] Lint reports no errors and the debug build keeps all three ABIs.
   Proof: `./gradlew :app:lintDebug` exit 0; the debug APK holds `x86_64`, `arm64-v8a` and `armeabi-v7a`.
31. [machine] Every existing test this run had to change says why, in the test itself, and is listed with its reason.
   Proof: `.harness/run/evidence/test-edits.md` lists every base test name (at `eccbf95`) that is missing, renamed or changed at HEAD with a reason, and each changed test file carries a comment line containing "Run 9" and the reason; no test uses `assumeTrue`, `@Ignore` or `Assume.`.
32. [machine] `README.md` and the three feature docs describe the app as the code now is.
   Proof: `git diff eccbf95 --stat` shows `README.md`, `docs/features/edit.md`, `docs/features/tool-screen.md` and `docs/features/saving-and-share.md` changed; README holds an "Editing features" table and a "How each editing feature works" section tracing Edit end to end (screen and ViewModel, steps in order, `core/imagecrop`, threading, size/format/quality/destination, failure behaviour) with file paths, all of which exist (a script checks each backticked path); `grep -n -i "springs back\|Saved screen\|range slider"` of those four files finds nothing (outside a sentence recording the removal).
33. [machine] A new decision record and a glossary exist.
   Proof: `docs/adr/ADR-003-*.md` exists and names ADR-002, Done, Result and the all-at-once options strip; `CONTEXT.md` at the root defines `Tool`, `EditTool`, `EditSettings`, session and preview.
34. [machine-then-human] The written record is true.
   Proof: the Verifier reads `README.md` "How each editing feature works" and `docs/features/edit.md` against the code and notes every sentence it could not back; then a person reads them and signs. A claim the code does not back fails the run.

## Removals

Repository class: **product** (PROJECT.md). Only what the PRD replaces is removed; anything else that looks unused is under Questions.

R1. [machine] Filters no longer stack: choosing a filter replaces the previous one.
    Proof: criterion 16's `FiltersSheetTest`; `EditSettings.filter` is one value and no code applies a filter to an already filtered bitmap.
R2. [machine] Sliders no longer spring back to the middle after release.
    Proof: criterion 5; `grep -n "MIDDLE" app/src/main/java/com/phongkaster/kanso/ui/tool` shows no reset in any release handler.
R3. [machine] The two range sliders are no longer the way to crop.
    Proof: `grep -rn "RangeSlider\|EDIT_FREE_WIDTH\|EDIT_FREE_HEIGHT\|EDIT_FREE_APPLY" app/src` prints nothing.
R4. [machine] The Edit screen has no Replace chip.
    Proof: `EditLayoutTest`: `onAllNodesWithTag(TOOL_REPLACE_TAG)` is empty on Edit and non-empty on the other four tools (A-007).
R5. [machine] None of the five tool screens has a full-width Save bar at the bottom.
    Proof: `grep -rn "SaveBar\|TOOL_SAVE_TAG" app/src` prints nothing; `DoneResultTest` finds no node with the text "Save" on any of the five tool screens.
R6. [machine] "Saved" is gone as a name; Result replaces it.
    Proof: `grep -rnw "Saved\|SavedScreen\|SavedViewModel\|SavedUiState\|SavedPhoto" app/src` prints nothing; `navigation/Saved.kt` and `ui/saved/` no longer exist; `grep -rn "saved_" app/src/main/res/values` prints nothing.

Questions (not removals): none asked; the four other tools' Replace chip and top-bar Undo stay (A-007).

## Constraints

- `main` is not touched (Invariant 7); every Run 9 commit goes to `loop/kanso-real-editor`.
- Every standing rule in `CLAUDE.md` holds; no new animation library; the Galaxy A13 on 32-bit Android decides performance.

## Verification Evidence Required

| Criterion | Class | Evidence / what to look at | Signed off |
|---|---|---|---|
| 1-19, 24-33 | machine | the named test class or command, Verifier re-runs | n/a |
| 20 | machine-then-human | gfxinfo on the A13 + person drags the sliders | ☐ |
| 21, 22 | machine-then-human | tests + a person opens the pictures | ☐ |
| 23 | human-only | open Edit, open each sheet | ☐ |
| 34 | machine-then-human | Verifier read + person reads the docs | ☐ |
| R1-R6 | machine | grep or test named above | n/a |
