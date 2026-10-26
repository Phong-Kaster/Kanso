# Definition of Done — Run 10: stickers on the Edit screen

> Derived from `PRD.md` at bootstrap. Human-owned after approval: the engine may propose changes (Tier 3) but never apply them.
> Every criterion must be verifiable by evidence, and must declare WHO can verify it (ADR-015).

## Status

- [x] APPROVED — D-001, 2026-10-26 (with the owner's edits to criteria 3, 11, 12, 27, 28 and 41-43).
      Approval covers the Verification Class of each criterion, not only its wording.

## Acceptance Criteria

The base for every "since the base" check is `fadd7c4` (the commit this run started from; `reference/pinity-stickers/`
arrived in `8114d3d`). Picture numbers 69 to 82 are new; light pictures have odd numbers, dark ones even.
Criteria 2, 18 and 20 answer questions the PRD leaves open (see Questions under Removals): edit them if you meant otherwise.

**Edited by the owner at approval (2026-10-26, D-001).** The PRD's "panel with tabs over a grid" became a
Stickers **sheet** that is confirmed or cancelled: criteria 3, 11 and 12 were rewritten, 27 and 28 now say
"category chip" for "tab", and criteria 41, 42 and 43 were added (after 22, so no other number moved). Where the
PRD and this DoD differ on the sheet, this DoD is the owner's later word.

### Behaviour

1. [machine] The Edit screen has a fifth tool, Stickers, after Filters; it is highlighted while open and shows the small "changed" dot whenever the photo has a sticker on it.
   Proof: `EditTool.entries` is exactly `CROP, ROTATE, ADJUST, FILTERS, STICKERS`. A Robolectric Compose test (model `tool/edit/EditToolRowTest.kt`), driven from a photo loaded in Edit with no change made, finds five `editToolTag(...)` nodes in that order, left to right by `positionInRoot`; tapping the Stickers entry opens the Stickers panel and marks the entry selected; `editToolDotTag(STICKERS)` is absent before the first sticker, present after one is added, and absent again after Undo.

2. [machine] On a phone 411 dp wide, all five tools are fully on screen beside Compare, without scrolling the row.
   Proof: the same test at qualifiers `w411dp-h891dp-xxhdpi`: each of the five entries' bounds (`positionInRoot` and size) lies inside the row's visible bounds, and the row's horizontal scroll position is 0.

3. [machine] The Stickers sheet has two rows: on top, a horizontally scrolling row of category chips, Food, Mood, Quote and Emoji, in Pinity's chip style; under it, a vertically scrolling grid three stickers wide showing the chosen category's stickers: 17, 17, 6 and 20.
   Proof: a JVM test of the sticker catalogue: four categories in that order with 17/17/6/20 entries (60 in all), each entry pointing at an `img_sticker_*` drawable that decodes. A Robolectric Compose test, driven from the Stickers sheet just opened (Food chosen): the chip row is a `LazyRow` and the sticker grid a `LazyVerticalGrid` with exactly 3 columns (three items share one `positionInRoot` y, the fourth starts a new row); tapping each chip in turn marks it chosen and, scrolling through the whole grid, counts that category's stickers by tag.

4. [machine] Tapping a sticker in the panel puts it in the middle of the picture as shown, about a third of the shown picture's width, and selects it.
   Proof: a ViewModel or Compose test driven from a photo cropped off-centre and turned one quarter turn: one tap on a grid item gives one sticker; mapped to the shown picture its centre is (0.5, 0.5) ± 0.01 and its width is between 0.30 and 0.37 of the shown picture's width; the screen state's selected sticker is that sticker.

5. [machine] The selected sticker's top-left button deletes it.
   Proof: a Compose test driven from two stickers with the second selected: tapping the delete corner leaves only the first sticker, and none is selected.

6. [machine] The top-right button adds a copy slightly down and to the right of the sticker and selects the copy.
   Proof: driven from one selected sticker turned 30° and mirrored, on a photo turned one quarter turn: tapping the duplicate corner gives two stickers; the copy has the same picture, size, turn and mirror; on the shown picture its centre is right of and below the original's by more than 0 and at most 10% of the shown width on each axis; the copy is the newest sticker and is selected.

7. [machine] The bottom-left button mirrors the sticker left to right, and a second tap mirrors it back.
   Proof: driven from one selected, unmirrored sticker: one tap on the flip corner makes it mirrored; a second tap gives settings equal to the starting settings.

8. [machine] Dragging the bottom-right handle resizes and turns the sticker together, following the finger around the sticker's centre, and the sticker keeps its shape.
   Proof: a JVM test of the handle geometry: a finger moved from distance d at angle a to distance 2d at angle a + 90° doubles the size and adds 90° to the turn (± 0.5°), with the width-to-height ratio unchanged. A Compose test drags the handle node with touch input on a selected sticker and finds size and turn changed accordingly (± 5%).

9. [machine] A sticker can be moved with one finger, and resized and turned with a two-finger pinch and twist.
   Proof: Compose multi-pointer touch-input tests on a selected sticker, driven from a photo turned one quarter turn and flipped: a one-finger swipe of (dx, dy) moves the sticker's shown centre by (dx, dy) ± 2 px; a two-finger pinch spreading the fingers 1.5x scales it 1.5x ± 5%; a two-finger twist of 45° turns it 45° ± 2°.

10. [machine] Tapping another sticker selects that one, tapping the photo away from every sticker selects none, and a newer sticker lies on top of an older one.
    Proof: driven from two overlapping stickers, the older selected: a tap where only the newer lies selects it; a tap on the overlap selects the newer; the sticker nodes' semantics order (and draw order) is oldest first; a tap on empty photo selects none; a tap on the selected sticker leaves its picture unchanged (Pinity's swap is dropped); no tap changes the edit settings.

11. [machine] Stickers can be touched only while the Stickers sheet is open; with any other tool open, or none, a touch on a sticker does nothing, and closing the Stickers sheet (by ✓ or by ✕) selects none.
    Proof: driven from one sticker placed and the Stickers sheet then closed with ✓: with Filters open, and again with no tool open, a tap and a swipe on the sticker's shown position leave the settings and the undo history unchanged, select nothing, and draw no sticker frame. Driven from the sheet open with a sticker selected, closing it with ✓, and again with ✕, leaves none selected.

12. [machine] Undo takes back sticker actions one at a time, in the same history as the other edits, both while the Stickers sheet is open and after ✓: an add, a delete, a duplicate, a flip, and one whole drag, pinch or handle pull each count as one action; selecting a sticker is not an action. For example, add a sticker, turn it, flip it, resize it: four taps of Undo take back the resize, then the flip, then the turn, then the sticker disappears.
    Proof: `EditSession` JVM tests (model `tool/edit/EditSessionTest.kt`): add, delete, duplicate and flip each push exactly one entry; a gesture of 50 moves between finger-down and finger-up pushes exactly one, and its undo restores the settings from before the gesture; a mixed sequence (crop, add, filter, drag) undoes in reverse order; the owner's sequence add → turn (handle) → flip → resize (handle) undoes in exactly four steps, each giving the settings from before that action, the fourth leaving no sticker. A Compose test drives the same sequence with the sheet open and taps the screen's Undo four times, then repeats it after ✓ with the sheet closed. A ViewModel test: selecting and deselecting change neither the undo history nor `canUndo`.

13. [machine] A photo holds at most 20 stickers: adding a 21st, from the panel or by duplicating, adds nothing and shows a short message.
    Proof: an `EditSession` test driven from 20 stickers: an add and a duplicate each leave settings and undo history unchanged. A Compose test driven from 20 stickers taps a grid item and finds the limit message (from the run's new `strings_stickers.xml`) displayed, with the count still 20.

14. [machine] Compare shows the untouched original, with no stickers on it, and the sticker controls do nothing while it is on.
    Proof: a Compose test driven from a photo with one opaque sticker, selected, with the Stickers panel open: with Compare on, the pixel at the sticker's place (read through `settings/ScreenPixels.kt`, never `captureToImage`) equals the original photo's pixel, no frame or corner button is drawn, and the sticker grid's items are disabled (as `CompareTest.kt` checks every other panel's controls); with Compare off they return.

15. [machine] A sticker is part of the photo: a crop can cut it off, and turning or flipping the photo turns or flips the sticker with it.
    Proof: `EditProcessor` JVM tests with a solid-colour sticker from a fake picture source: a crop through the sticker's middle keeps only the kept part in the output; a crop that excludes it leaves no sticker pixel; with one quarter turn and a horizontal flip the sticker's pixels sit at the turned and flipped place. The order in `EditProcessor` is stickers, crop, turns, flips, filter, sliders.

16. [machine] Filters and the colour sliders colour the stickers too.
    Proof: `EditProcessor` JVM tests: with a filter at full strength each sticker pixel equals the filtered sticker colour; with brightness raised each sticker pixel equals the sticker colour through the colour matrix.

17. [machine] A point on the picture as shown and a place on the original photo are converted both ways by one mapping, and a point converted there and back returns to itself.
    Proof: a JVM test of the one pure-Kotlin mapping file `StickerMapping.kt` (no `android.` import): for all 4 quarter turns × {no flip, horizontal, vertical, both} × {whole photo, an off-centre crop}, at least 20 points map shown → original → shown within 1e-4, and angle and mirror round-trip too. The Reviewer confirms every changed file that places or moves a sticker converts through it.

18. [machine] Done puts the stickers into the saved picture at the same resolution as the rest of the saved photo (the photo as loaded for editing, which Done already caps at 2,000 pixels on its long side), drawn from the sticker pictures themselves rather than copied from the screen, and ignores which sticker is selected.
    Proof: `EditProcessor`/Done JVM tests: the output size with stickers equals the output size of the same settings without them; a sticker picture with a 1-px checkerboard, placed so it maps 1:1 onto output pixels with no turn, appears in the output pixel for pixel (± 1 per channel); Done's output is identical with a sticker selected and with none; a sticker added from the panel survives a crop finished in the cropper (both build their processor from one factory).

19. [machine] While editing, stickers show in the live picture: the ones not being touched are drawn into the filtered preview, the one under the finger follows it live with the same crop, turn, flip and colour sliders (without the filter) and joins the filtered preview when the finger lifts, and a sticker partly outside the crop is cut at the picture's edge.
    Proof: `FilterPreviewRenderer` JVM test: the small copy includes the stickers before the filter (a sticker under a filter comes out filtered), and is rendered when stickers exist even with no filter. A live-preview test (model `tool/edit/LivePreviewTest.kt`) driven from a filter, raised brightness and two stickers: between finger-down and finger-up the touched sticker is left out of the renderer's request and a new semantics property of the live preview (beside `LivePreviewGeometry`) reports it drawn inside the same crop, turn and flip with the slider colour matrix; after finger-up the next request includes it. A sticker placed half outside the crop: no sticker pixel lies outside the photo's shown bounds (`ScreenPixels.kt`).

20. [machine] While Crop is open, the stickers stay visible on the photo being cropped, so the person sees what the crop will cut.
    Proof: a Compose test driven from one opaque sticker and the Crop panel then opened: the pixel at the sticker's place in the cropper (`ScreenPixels.kt`) is the sticker's colour, not the photo's; the cropper's handles still drag the crop.

21. [machine] Which sticker is selected belongs to the screen, not to the edit, and a selection never points at a sticker that is gone.
    Proof: `EditSettings` and the sticker value type hold no selection field; the selected sticker is a `val` in `ToolUiState`; `ScreenStateTest` passes. A ViewModel test: after undoing the add of the selected sticker, none is selected.

22. [machine] Each corner button can be found by a screen reader and is big enough to tap.
    Proof: a Compose test driven from one selected sticker: each of the four corner buttons has a content description from the run's strings and a touch target of at least 48 × 48 dp (model `test/a11y/TouchTargetCheck.kt`).

41. [machine] Tapping Stickers opens the Stickers sheet in place of the row of tools, with a ✕ button at its top left and a ✓ button at its top right; the photo keeps its size while the sheet is open.
    Proof: a Robolectric Compose test driven from a photo loaded in Edit: before, the five `editToolTag(...)` nodes are displayed; after tapping Stickers, none of them is displayed, the sheet's ✕ and ✓ nodes are, ✕ left of ✓ and both in the sheet's top row by `positionInRoot`, each with a content description from the run's strings and a 48 × 48 dp touch target; the photo area's bounds are equal before and after opening the sheet (as `EditLayoutTest.kt` checks for the other tools).

42. [machine] ✓ keeps every sticker change made since the sheet opened, closes the sheet and shows the row of tools again.
    Proof: driven from a photo with one earlier sticker, the sheet then opened: add a sticker and move the earlier one, tap ✓: the settings hold both changes, the undo history keeps each action as its own entry (criterion 12), the sheet's nodes are gone and the five tool entries are displayed again.

43. [machine] ✕, and the system Back while the sheet is open, put the photo and the Undo history back exactly as they were when the sheet opened, close the sheet and show the row of tools again; stickers confirmed in an earlier sheet stay.
    Proof: an `EditSession`/ViewModel JVM test driven from a photo with one sticker confirmed earlier and a crop: open the sheet, add two stickers, flip and drag the earlier one, then cancel: the settings and the undo history equal the ones from the moment the sheet opened (the earlier sticker unflipped and in its old place, the crop kept), and `canUndo` is what it was. A Compose test does the same through the ✕ node, and again through `Espresso.pressBack()`, and finds the sheet closed and the five tool entries displayed.

23. [machine-then-human] On a phone, a person can add, drag, pinch, twist, delete, duplicate, flip and resize stickers, and with another tool open the stickers do not respond.
    Proof: pre-check on the emulator, or the A13 if nobody is using it, through `foreman-device.ps1`, driven from a fresh debug install with photo access granted and a photo already in the device's gallery, opened in Edit: open Stickers, tap a sticker, drag it, and the screen dump shows its frame — written as "did not fail when driven". Then a person: open Edit on a photo, open Stickers, add three stickers from different tabs, try each corner button, a one-finger drag, a two-finger pinch and twist, tap another sticker, tap empty photo; add stickers until a 21st is refused and read the message; open Filters and try to drag a sticker. Expect each to behave as criteria 4 to 13 say, the message to be readable, and nothing to move while Filters is open.

24. [human-only] On the Galaxy A13, dragging a sticker with 20 stickers on a 12-megapixel photo is smooth.
    Proof: nothing here can measure stutter. A person installs the debug build on the A13, opens a 12 MP gallery photo in Edit, adds 20 stickers, drags one slowly across the picture for about 5 s, pinches and twists another, and pulls a corner handle. Expect no stutter and no freeze. If it stutters, the run reports a failure; it does not ship it.

25. [machine-then-human] The saved result shows the stickers sharp and in the right place, cut by the crop and coloured by the filter.
    Proof: pre-check is criteria 15, 16 and 18. Then a person on a phone: add two stickers, crop through one, choose a filter, tap Done, Save, and open the saved photo zoomed in. Expect crisp sticker edges (not blurry or blocky), the cropped sticker cut off, and the stickers tinted like the photo.

### Permissions the user is asked for

26. [machine] The app asks for no new permission and still has no network access.
    Proof: the merged debug manifest's `uses-permission` set equals the base's, with no `INTERNET` or `ACCESS_NETWORK_STATE`.

### Appearance and reachability

27. [machine] The new pictures the owner approved are recorded and checked on every run: the row with Stickers open, the Stickers sheet on each category, a selected sticker with its four corner buttons, and a photo with stickers after a crop and a filter, each in light and in dark.
    Proof: these files exist in `app/src/test/screenshots/`, added through `bin/foreman-record-baselines.ps1`, and the engine opened each once before committing it: `69/70_edit_stickers_row`, `71/72_edit_stickers_food`, `73/74_edit_stickers_mood`, `75/76_edit_stickers_quote`, `77/78_edit_stickers_emoji`, `79/80_edit_sticker_selected`, `81/82_edit_stickers_crop_filter` (each `_light.png` / `_dark.png`). `./gradlew :app:verifyRoborazziDebug` exits 0.

28. [machine-then-human] A person has looked at the new pictures and the Stickers tool reads as part of Kanso in both themes.
    Proof: pre-check is criterion 27. A person opens pictures 69 to 82 and expects: five tools under the photo, each an icon over its name, none cut off; the sheet's ✕ and ✓ easy to see; each category chip readable and the chosen chip obvious; each grid three stickers wide, showing that category's stickers; the selected sticker with a visible frame and four round buttons (delete top left, duplicate top right, flip bottom left, resize-and-turn bottom right) in Kanso's colours, easy to see on the photo in light and in dark; in 81 and 82 a sticker visibly cut by the crop and tinted by the filter.

29. [machine] The corner buttons use the app's theme colours, and the new duplicate icon is drawn in the same style as Kanso's other icons.
    Proof: the corner-button composables take every colour from `MaterialTheme.colorScheme`; `grep -nE 'Color\(0x|Color\.(White|Black)'` over them finds nothing outside previews and samples. `res/drawable/ic_duplicate.xml` has viewport 960 × 960 like `res/drawable/ic_close.xml` and no `android:tint`. All four corner icons are drawn with `Icon(painterResource(...))`.

30. [machine] Every existing picture that changed is one showing the tool row, and its old version is kept beside it while the run lasts.
    Proof: every file in `git diff --name-only --diff-filter=M fadd7c4 -- app/src/test/screenshots/` is one of pictures 51, 52 or 55 to 68, and has its base version in `.harness/run/evidence/baseline-diffs/`.

31. [machine-then-human] A person has compared each re-recorded picture with its old version and found that the only change is the tool row.
    Proof: pre-check is criterion 30. A person opens each old picture in `.harness/run/evidence/baseline-diffs/` beside its new one and expects the only difference to be the row: five entries with Stickers after Filters, each still readable.

### Data and storage

32. [machine] Kanso ships exactly Pinity's 60 picture stickers in four categories, and nothing of Pinity's text, clock, date, time, location or weather decals.
    Proof: `app/src/main/res/drawable/img_sticker_*.webp` is 60 files, each byte-identical (sha256) to its namesake in `reference/pinity-stickers/res/drawable/`; no `decal_*` resource exists in `app/src`; the catalogue has no weather, location, time, date, clock or text entry.

33. [machine] Pinity's copied code and pictures stay untouched and are never built or imported.
    Proof: `git diff fadd7c4 -- reference/` is empty; a grep over `app/src`, `*.gradle*` and `settings.gradle.kts` finds no import, source set or dependency on `reference/` or Pinity's packages.

### Build, start and quality

34. [machine] The app builds, opens without crashing, and its tests, lint and screenshot checks all pass.
    Proof: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug` exits 0 under `timeout`; `LaunchTest` passes; the debug APK holds `x86_64`, `arm64-v8a` and `armeabi-v7a` libraries.

35. [machine] Existing tests still pass; any that had to change are listed with their reason, and none is skipped.
    Proof: every test name present at the base is present at HEAD or listed with its reason in `.harness/run/evidence/test-edits.md`; `grep -rE 'assume(True|False)|Assume\.|@Ignore' app/src/test` is empty; the number of tests run equals the number of `@Test` lines.

36. [machine] The new code follows the project's standing rules: every new screen part has a light and a dark preview, one type per file, every Stickers part in its place in the Edit screen's folders, and plain-English comments.
    Proof: `PreviewCoverageTest`, `PreviewsRenderTest`, `OneTypePerFileTest`, `KdocCoverageTest` and `ScreenStateTest` pass; every new composable lives under `ui/tool/components/...` as CLAUDE.md's "Screen packages show the hierarchy" says, and `ToolScreen.kt` gains no child composable; new strings live only in new `res/values/strings_<area>.xml` files, all used (lint's unused-resource list equals the base's).

37. [machine] No new library was added: gestures and motion use Compose's own APIs.
    Proof: `git diff fadd7c4 -- gradle/libs.versions.toml app/build.gradle.kts` adds no dependency line.

38. [machine] README.md and the Edit and tool-screen docs describe the Stickers tool as built, with its flows drawn.
    Proof: README's "Editing features" table has a Stickers row with its real state; "How each editing feature works" traces Stickers from the panel to Done (screen, ViewModel, `EditSession`, `EditProcessor` order, threading, input and output, failure, file paths); README's folder table lists `reference/pinity-stickers/` as read-only; `docs/features/edit.md` and `docs/features/tool-screen.md` describe Stickers, each with a mermaid block covering it; every file path these files name exists at HEAD.

39. [machine] The decision that a sticker is part of the photo is written down, and the glossary knows the new words.
    Proof: `docs/adr/ADR-004-*.md` exists and states the drawing order (stickers first, so crop, turns, flips, filter and sliders apply) and its cost (the sticker under the finger shows no filter in the preview); `CONTEXT.md` defines **Sticker** and **sticker catalogue**, and its `EditTool` entry names five tools.

40. [machine-then-human] Everything the docs say about Stickers is true of the app.
    Proof: pre-check: a fresh-context reader compares README, `docs/features/edit.md`, `docs/features/tool-screen.md` and ADR-004 with the code and finds no false sentence. Then a person reads README's Stickers section, uses the tool (add, move, crop, filter, Compare, Undo, Done) and notes any sentence that is untrue.

## Removals

None. Kanso is a product, and this PRD adds a tool without replacing any feature, screen, permission or dependency. Pinity's screenshot-to-save and tap-to-swap behaviours never existed in Kanso, so there is nothing to remove.

Questions (the PRD leaves these open; each is answered by a criterion above that you may edit):

- Q1. Pinity's `ic_rotate_circle.xml` is in `reference/` but the PRD ports only `ic_close_circle`, `ic_flip` and `ic_resize_circle`; it stays unported.
- Q2 (criterion 20). Today the Crop panel shows the plain original photo while you crop, so stickers would vanish exactly when you crop through one. The criterion proposes drawing them on the photo being cropped. Delete criterion 20 to accept them hidden while cropping.
- Q3 (criterion 2). At 411 dp, today's row already scrolls (Filters is partly cut off), so a fifth tool would start off screen. The criterion proposes narrowing the entries (to about 62 dp) so all five fit, which changes the row in pictures 51, 52 and 55 to 68. Delete criterion 2 to keep the scrolling row.
- Q4 (criterion 18). "At the photo's own resolution": Done today saves the photo as loaded for editing, at most 2,000 px on its long side, not the file's full size. The criterion draws stickers at that same resolution, sharp from their own pictures. If you meant the file's full size, say so: Done would then need a full-size decode, which is new work on the A13.

## Constraints

- A sticker is part of the photo, not a layer: `EditProcessor` draws stickers first, so the order is stickers, crop, quarter turns, flips, filter, colour sliders; they are drawn after the crop rectangle is cut out, shifted by its offset, with no second full-size copy of the photo.
- A sticker is an immutable value in `EditSettings`: which picture, its centre as fractions of the **original** photo, its size as a fraction of the original's width, its turn in degrees, and whether it is mirrored. Undo works through `EditSession`.
- Selection lives in `ToolUiState`, never in `EditSettings`.
- One shown ↔ original mapping, in pure Kotlin, tested on the JVM.
- At most 20 stickers per photo. Stickers respond only while the Stickers panel is open.
- Only the Food, Mood, Quote and Emoji picture categories come across.
- `reference/` (including `reference/pinity-stickers/`) is read-only: never edited, built or imported; code is ported into Kanso's own packages.
- Done renders stickers in `EditProcessor`, never by capturing the view.
- The Galaxy A13 (32-bit) decides: a drag that stutters there with 20 stickers is reported as a failure, not shipped.
- Every CLAUDE.md rule holds: light and dark previews, one type per file, the screen-package layout, KDoc that teaches, README in the same change, Compose's own animation APIs, no ads, analytics, accounts or network permission.
- New pictures are recorded only through `bin/foreman-record-baselines.ps1`; each re-recorded existing picture keeps its old version in `.harness/run/evidence/baseline-diffs/` while the run lasts.
- Before driving the A13 or the emulator, check that nobody else is using it.
- One commit per phase, in the PRD's five-phase order.

## Verification Evidence Required

| Criterion | Class | Evidence / what to look at | Signed off |
|---|---|---|---|
| 1–22, 41–43 | machine | `./gradlew :app:testDebugUnitTest` (the tests named in each Proof) | n/a |
| 23 | machine-then-human | device pre-check summary; a person tries every gesture, button and the limit message | ☐ |
| 24 | human-only | A13, 12 MP photo, 20 stickers: drag, pinch, handle are smooth | ☐ |
| 25 | machine-then-human | Done → Save → saved photo zoomed: sharp, cut, tinted | ☐ |
| 26 | machine | merged-manifest permission diff against the base | n/a |
| 27 | machine | pictures 69–82 exist; `verifyRoborazziDebug` exits 0 | n/a |
| 28 | machine-then-human | open pictures 69–82; expectations as written | ☐ |
| 29 | machine | grep of the corner-button files; duplicate icon viewport 960 | n/a |
| 30 | machine | changed baselines vs `baseline-diffs/` | n/a |
| 31 | machine-then-human | old vs new pictures side by side: only the row differs | ☐ |
| 32 | machine | 60 files, sha256 equal to reference; no decal entries | n/a |
| 33 | machine | `git diff fadd7c4 -- reference/` empty; import grep empty | n/a |
| 34 | machine | assemble, test, lint, verifyRoborazzi exit 0; APK ABIs | n/a |
| 35 | machine | base test names vs HEAD vs `test-edits.md`; skip grep empty | n/a |
| 36 | machine | guard tests pass; file locations; lint unused resources | n/a |
| 37 | machine | catalogue and build-script diff adds no dependency | n/a |
| 38 | machine | README and docs greps; every named path exists | n/a |
| 39 | machine | ADR-004 and CONTEXT.md contents | n/a |
| 40 | machine-then-human | fresh reader's report; a person reads and uses the tool | ☐ |
