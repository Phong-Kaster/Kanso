# Definition of Done

> Derived from `PRD.md` at bootstrap (Run 8: the five editing tools edit the photo). Human-owned after approval: the engine may propose changes (Tier 3) but never apply them.
> Loop Branch `loop/kanso-editing-tools`, started from commit `ec2227f` (written `<base>` below). Paths are relative to the repository root. Gradle commands start with `cd /d/Valve/Personal/Kanso &&`, run under `timeout`, and write their output to `.harness/run/evidence/`. "A test proof passes" means `set -o pipefail; timeout 2400 ./gradlew :app:testDebugUnitTest --tests '<each class named>'` exits 0 and ends `BUILD SUCCESSFUL`.
> Words the proofs use:
> - **the five tools**: Edit, Remove background, Enhance, Retouch, Artistic. **the AI tools**: the last four.
> - **the seam**: each engine step sits behind a small Kotlin interface in `app/` (a "processor"). Host tests (Robolectric, `@GraphicsMode(NATIVE)`, real `Bitmap` pixels) prove the Kotlin steps with a fake behind the seam, because the engine's GL, LiteRT, MediaPipe and OpenCV code is native and cannot load on the host JVM. The engine itself is proved by driving it on a device (class `machine-then-human`).
> - **the device**: the Galaxy A13 or the emulator, whichever is free; before driving either, check nobody else's run or person is using it (PRD). Driven only through `.harness/loop/bin/foreman-device.ps1`.
> - **working copy**: the photo decoded by `PhotoLoader` at a long edge of at most `displayMaxEdgePx` (twice the screen's longest edge). The heavy tools cap further (Q3).
> - **differs** (two colours): the largest channel difference is more than 8.
> Out of scope, as in the PRD: anything beyond the five tools' editing (stickers, brushes, text), credits, a paywall, a network model download.
> Where a proof rests on a reading the PRD leaves open it names the reading (Q1 ... Q6, after the Removals). Approving without editing accepts each as an assumption. **Q1, Q2 and Q4 are the ones worth your eye.**

## Status

- [ ] APPROVED — approve via the pending `.harness/run/ESCALATION.md` (D-001); edit criteria freely before approving.
      Approval covers the Verification Class of each criterion, not only its wording. It also covers:
      - the re-recording of the 7 existing tool-screen pictures and the recording of new ones listed under `## Baselines` in D-001 (criterion 27);
      - the readings Q1 to Q6 unless edited.
      No new dependency is requested; the dependency files must not change (criterion 30).

## Acceptance Criteria

### Behaviour

1. [machine] In Edit, the person can crop the photo freely or to 1:1, 4:3, 3:4, 16:9 or 9:16, and the photo shown becomes the cropped one.
   Proof: `EditProcessorTest` (new) at SDK 35, `@GraphicsMode(NATIVE)`. A 400x300 working copy with a distinct colour in each quadrant: ratio crops give a bitmap whose width/height equals the ratio within 1 px, centred, and the pixel at its middle equals the source pixel at the source's middle; a free crop to the rectangle (0.25, 0.25, 0.75, 0.75) gives 200x150 with pixel (0,0) equal to source (100,75). Applying "no crop" returns the same pixels as the input.

2. [machine] In Edit, the person can turn the photo a quarter-turn left or right, and flip it.
   Proof: `EditProcessorTest`. Rotating the 400x300 copy 90 degrees clockwise gives 300x400 and the source top-left pixel lands at the new top-right; 90 anticlockwise puts it at bottom-left; four turns equal the original pixel for pixel; a horizontal flip puts the source top-left pixel at the top-right and keeps 400x300.

3. [machine] In Edit, brightness, contrast, saturation and warmth each change the photo in the way their name says, and leaving them in the middle changes nothing.
   Proof: `EditProcessorTest` on a 100x100 mid-grey-and-colour test patch. At all four sliders at their middle the output equals the input pixel for pixel. Brightness up: mean luminance rises (down: falls). Contrast up: luminance standard deviation rises (down: falls). Saturation at minimum: every pixel has R, G and B within 2 of each other; saturation up: mean (max-min channel) rises. Warmth up: mean R minus mean B rises (down: falls). Each is tested alone, so a slider moving the wrong channel fails.

4. [machine] In Edit, choosing a filter changes the colours of the photo, "None" gives the photo back, and a lower intensity gives a result between the two.
   Proof: `EditFilterTest` (new), same patch. `None` equals the input; each listed filter (at least 6, from the engine's bundled look-up images, Q5) gives output that `differs` from the input in the mean; at intensity 0.5 each channel mean lies between the input's and the full-strength output's.

5. [machine] The person sees each Edit change on the photo as they make it.
   Proof: `EditToolTest` (new) at SDK 35, hosting `ToolContent` on a fake photo with the Edit panel in its panel slot and the real `EditProcessor`. After setting `edit-brightness` to its maximum, the image node `tool-photo` shows a bitmap whose mean luminance is higher than before, within 5 s of real time; the same for choosing a crop ratio (its aspect changes) and a filter.

6. [machine] In Remove background, the part of the photo the segmenter calls background becomes fully transparent and the subject is kept exactly as it was; the result is shown over a checkerboard.
   Proof: `CutoutProcessorTest` (new) with a fake segmenter returning a mask that is 1 on a centre disc and 0 elsewhere on a 200x200 coloured photo: output pixels outside the disc have alpha 0, pixels well inside have alpha 255 and the source's colour, and the bitmap has `hasAlpha()`. `CutoutPreviewTest` (new): `ToolContent` with the Remove background panel in its slot draws a node `cutout-checkerboard` behind `tool-photo`, whose two tile colours differ.

7. [machine-then-human] On the device, Remove background on a real photo with a person cuts the person out with the engine, with no network and no download.
   Proof: install the debug build with the device wrapper, airplane mode not required (the app has no network permission, criterion 25), open Remove background on a photo of a person pushed to `Pictures`, wait for the result. Driven from: fresh install, photo access granted, Google Play services model cache not relied on (Q2). Pass if the result's four corner pixels have alpha 0 and the centre pixel of the subject has alpha 255 (read from the saved PNG), within 60 s. The person then looks at the cut edge.

8. [machine] Enhance gives a larger and better-lit copy of the photo, and the person can slide or press to compare it with the original.
   Proof: `EnhanceProcessorTest` (new) with a fake upscaler that doubles the size: output is exactly twice the capped input on each side; on a dark patch (mean luminance 40) the lighting step raises mean luminance toward 100..160, and on an already well-lit patch (mean 128) mean luminance stays within 10 of it; on a flat input the output is not darker than the input by more than 2. `EnhanceCompareTest` (new): `ToolContent` with the Enhance panel in its slot shows `enhance-compare` with the original and the enhanced bitmap, and pressing/holding `enhance-compare-hold` shows the original (the shown bitmap equals the input) and releasing shows the enhanced one.

9. [machine-then-human] On the device, Enhance runs the engine's upscaler (Real-ESRGAN) on a real photo and finishes without freezing or crashing.
   Proof: on the device, a 4000x3000 photo opened in Enhance finishes within 120 s, the tool showed a progress indication while it ran (`uiautomator dump` finds a node with resource id `tool-progress` at least once; the tool screen sets `testTagsAsResourceId`), the app process did not die (`pidof` unchanged), and the saved file is larger on the long edge than the capped input. Driven from: fresh install, photo access granted, the test photo placed by the device wrapper if it has an operation for that; if it has none, this criterion is reported "not driven" and stays for the person. The person then looks at the detail.

10. [machine] Retouch finds the face and smooths the skin by the amount of the strength control; a photo with no face says so and nothing changes.
    Proof: `RetouchProcessorTest` (new) with a fake face finder returning a face region (a centre rectangle) and a fake smoother: at strength 0 the output equals the input pixel for pixel; at strength 1 the pixel variance inside the face region of a noisy 200x200 patch is below 60% of the input's while pixels outside the region are unchanged; at strength 0.5 the variance lies between the two. With a fake finder returning no faces, `RetouchToolTest` (`ToolContent` with the Retouch panel in its slot) shows the message in `retouch-no-face` ("No face found in this photo. Nothing was changed."), the photo shown equals the original and Save is not offered as an edit (Q4).

11. [machine-then-human] On the device, Retouch on a real portrait detects the face with the bundled landmark model and smooths the skin.
    Proof: on the device, a portrait photo with one face opened in Retouch, strength at maximum: within 60 s the result differs from the original (mean absolute pixel difference over the face area above 2, read from the saved file) and a photo of a landscape shows `retouch-no-face`. Driven from: fresh install, photo access granted. The person then looks at the skin and the eyes.

12. [machine] Artistic offers at least four styles and a strength control: strength 0 gives the original, full strength the painted version, and in between a mix; choosing another style gives another picture.
    Proof: `ArtisticProcessorTest` (new) with a fake stylizer that returns a fixed per-style recolouring: strength 0 equals the input pixel for pixel; strength 1 equals the stylizer's output; strength 0.5 is within 1 per channel of the average of the two; two styles give outputs that `differ`. The style list has at least 4 entries, each with a name and a style image that exists in `app/src/main/assets` (Q1).

13. [machine-then-human] On the device, Artistic restyles a real photo with the bundled style models.
    Proof: on the device, a photo opened in Artistic, the first style, strength at maximum: within 120 s the saved result differs from the original (mean absolute difference above 10), the process did not die, and a second style gives a result that differs from the first. Driven from: fresh install, photo access granted. The person then looks at whether it looks painted.

### Every tool

14. [machine] The words "coming in a later run" appear nowhere in the app, and each of the five tools shows its own controls instead.
    Proof: `git grep -n -i 'later run' -- app/src/main` prints nothing; `ToolScreenTest` (rewritten) finds `edit-panel`, `cutout-panel`, `enhance-panel`, `retouch-panel`, `artistic-panel` on the matching tool and none on the others.

15. [machine] The person can undo edits back to the original photo before saving.
    Proof: `ToolViewModelTest` (existing, extended) with fake processors: after two applied edits, `undo()` shows the first result and a second `undo()` shows a bitmap equal pixel for pixel to the original working copy; `undo` with nothing to undo is a no-op; the Undo control `tool-undo` is disabled when nothing is to undo.

16. [machine] While a long operation runs, the screen shows its progress, stays responsive and cannot be saved by mistake; the work never runs on the main thread.
    Proof: `ToolViewModelTest`: a gated fake processor records `Thread.currentThread()`: it is not the main (Looper) thread. While gated, the state is `Processing` with a progress value in 0..1 or indeterminate, `tool-progress` exists in `ToolScreenTest`, Save is disabled and a second apply is ignored (the fake is called once); after the gate opens, the result is shown and Save is enabled.

17. [machine] When an edit fails — out of memory, no subject found, a model that will not load — the screen says what happened in plain words and the photo shown stays as it was.
    Proof: `ToolViewModelTest` with fakes that throw `OutOfMemoryError`, return "no subject", and throw `IllegalStateException("model")`: each gives its own failure state (three different messages, none empty), the shown bitmap is the one before the attempt, the original Uri is untouched, the app does not crash, and a retry works afterwards. A thrown `CancellationException` is not turned into a failure.

18. [machine] Save writes what the person edited, never the unedited original: a JPEG for opaque results and a PNG with transparency for a cut-out.
    Proof: `SaveEditedTest` (new) at SDK 35 and at SDK 28, with the real `MediaStorePhotoSaver` over Robolectric's content resolver: after an Edit brightness change, the saved file decodes to a bitmap equal (mean absolute difference below 3 for JPEG) to the shown bitmap and `differs` from the decoded original; for a cut-out the file's MIME is `image/png`, its name ends `.png`, it decodes with alpha 0 at the transparent pixels and its first 8 bytes are the PNG signature; an unedited photo still saves as before (the existing `MediaStoreSaverTest` and `LegacySaverTest` stay green).

19. [machine] The photo saved is exactly the size of the one the person saw.
    Proof: `SaveEditedTest`: saved width and height equal the shown bitmap's. The README states that size rule (criterion 31).

20. [machine] The original photo file is never changed by editing or saving.
    Proof: `SaveEditedTest`: the SHA-256 of the source file's bytes before opening the tool and after Save is identical, for each of the five tools.

21. [machine] A saved cut-out PNG appears in My edits and the Saved screen like any other saved photo, and Share offers it with the right file type.
    Proof: `EditsSourceTest` and `SavedNavigationTest` (existing, extended) with a PNG row in the fake source: it is listed; the share intent's type for a `.png` Uri is `image/png` and for `.jpg` stays `image/jpeg` (`SharePhotoTest`, new or extended).

22. [machine] Every tool still works on a very large photo: the heavy ones work on a smaller copy of it rather than crashing.
    Proof: `ToolViewModelTest` and each processor test: given a 6000x4000 input, Remove background, Enhance, Retouch and Artistic hand the engine a bitmap whose long edge is at most their cap (Q3: 2000, 512, 2000, 1024) and a thrown out-of-memory error from the engine step produces the failure of criterion 17.

35. [human] On the owner's Galaxy A13 (32-bit), each of the five tools works on an ordinary photo from the gallery without crashing or freezing, and Save writes what was edited.
    Proof: on the A13 install the debug build, open each tool on a photo, edit, Save, and open the saved photo in My edits; expect no crash, a visible progress indication on the slow tools, and the saved photo showing the edit. The emulator (x86_64) cannot prove the 32-bit path, which is why this is a person's check.

### Permissions the user is asked for

23. [machine] The app asks for no new permission and still has no network permission.
    Proof: the merged debug manifest (`app/build/intermediates/merged_manifests/debug/.../AndroidManifest.xml`, read with `aapt2 dump permissions`) lists no permission that `<base>`'s did not, and neither `INTERNET` nor `ACCESS_NETWORK_STATE`.

### Appearance and reachability

24. [human] Each tool's controls sit in the panel under the rounded photo card, the photo stays visible while editing, and there is one full-width Save button at the bottom, in light and dark.
    Proof: open each of the five tools on a photo in light and dark mode; expect the photo in its rounded card, its controls in the panel under it, one Save button across the bottom, nothing cut off or hidden behind the floating bar or the system buttons, and every control easy to see and press.

25. [machine] Every control of the five tools is at least 48 dp to press and readable against its background, and nothing is hidden by the bottom bar.
    Proof: `AccessibilityWalkTest`, `OverPhotoContrastTest` and `BottomClearanceTest` (existing, extended to the five tools' panels) pass.

26. [human] A person can tell what is happening while a tool works: a progress indication shows, and the before/after and the checkerboard read clearly.
    Proof: on the device, run Remove background and Enhance on a large photo; expect a visible progress indication during the work, the cut-out clearly over a checkerboard, and Enhance's compare control easy to find and understand.

27. [machine] The tool-screen pictures show the controls of each tool: the 7 existing pictures re-recorded and one new picture each for Remove background, Retouch and Artistic (and Enhance and Edit in their working states), light and dark, as listed in D-001.
    Proof: `./gradlew :app:verifyRoborazziDebug` exits 0 with the new baselines committed; the Verifier opens each new picture and judges that the controls are on screen and legible. Only the pictures listed in D-001 may be re-recorded (never any other).

### Data and storage

28. [machine] Edited photos are saved to `Pictures/Kanso`, with the same naming as before and the extension that matches the format.
    Proof: `SaveEditedTest`: relative path `Pictures/Kanso/`, name `Kanso_<yyyyMMdd_HHmmss>[_n].jpg` or `.png`, unique across two saves in the same second.

### Build, start and quality

29. [machine] The app builds, the whole test suite and lint pass, and the app opens without crashing.
    Proof: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` exits 0; the debug APK still contains `lib/armeabi-v7a/`, `lib/arm64-v8a/` and `lib/x86_64/`; `LaunchTest` passes.

30. [machine] No new library is added and no engine module's source is edited.
    Proof: `git diff <base> -- gradle/libs.versions.toml` and `git diff <base> --stat -- core opencv libs` print no change (build scripts of `app/` may change only in lines that name an existing module).

31. [machine] The README says, for each of the five tools, exactly what the code does.
    Proof: the "Editing features" table has no row marked "UI only" or "Not wired" for the five tools (`git grep -n -E 'UI only|Not wired|coming in a later run' -- README.md` prints nothing); the section "How each editing feature works" has one subsection per tool naming its screen, ViewModel, every processing step in order, the engine module, library or model, the thread, input and output size and format, where it is saved and what happens on failure; and every path in backticks in that section exists (`git ls-files` check by script `.harness/run/evidence/readme-paths.sh`, which the Verifier writes and runs).

32. [human] The README's description of each of the five tools matches what the person sees the tool do.
    Proof: read "Editing features" and "How each editing feature works" in `README.md`, then use each tool on the device; expect every sentence you check to be true, and say which one is not.

33. [machine] The standing rules hold: a preview in light and dark for every new composable, one class per file, a KDoc on everything, a ViewModel and UiState per screen.
    Proof: `PreviewsRenderTest`, `KdocCoverageTest`, `OneTypePerFileTest` and `ScreenStateTest` pass, and `.harness/run/evidence/test-edits.md` lists every test added, edited, renamed or removed with its reason.

34. [machine] `reference/` is unchanged.
    Proof: `git diff --stat <base> -- reference` prints nothing.

## Removals

Repository class: **product**. Only what this PRD replaces is removed.

R1. [machine] The five "coming in a later run" messages and the placeholder panel that showed them are gone.
    Proof: `git grep -n -E 'editor_panel_|EditorPanel' -- app/src` prints nothing.
R2. [machine] The README no longer says the image engine is "built, but not called".
    Proof: `git grep -n -i 'built, but not called' -- README.md` prints nothing; the section describes what each engine module is called for.

Questions (product repositories): none beyond Q1 to Q6.

## Readings (Q)

- **Q1 Artistic styles.** The bundled style models make a style from a *style image*, and no style images exist anywhere in the repository (Retica's 12 `style_transfer_style/*.webp` are missing from `reference/`). Reading taken: Kanso ships its own style images in `app/src/main/assets/styles/`, drawn by the run itself (colour-and-brushstroke patterns written in code and committed as WebP), at least four, named Warm wash, Cool wash, Mosaic and Ink. They are generated, not famous paintings, so there is no copyright question. Alternative: you supply images. **Worth your eye.**
- **Q2 Remove background engine.** `libs/localml.aar` references ML Kit *subject* segmentation (a Google Play services model that downloads on first use — forbidden offline) and *selfie* segmentation (model inside the library — allowed, people only). Reading taken: Remove background uses the bundled selfie-segmentation path, so it cuts out **people**; a photo with no person says "No subject found". If the engine cannot be made to avoid the download, the tool is limited as said and the README says so. **Worth your eye.**
- **Q3 Working sizes.** Remove background 2000, Enhance 512, Retouch 2000, Artistic 1024 px on the long edge; Edit works at the working copy. A device check may lower a cap; the README states the numbers in force.
- **Q4 No face.** Retouch with no face changes nothing and Save then writes the unchanged photo exactly as Save of an unedited photo does today. **Worth your eye.**
- **Q5 Filters.** Edit's filters apply the engine's bundled look-up images (`assets/lookup/`, `assets/filter_materials/` in the AARs) on the CPU, so the result can be tested on the host; the live GL preview path is not used.
- **Q6 Preview and save.** What is shown while editing is what is saved: both are the working copy at the same size (criterion 19). A full-resolution save is not attempted, because a 12 MP photo is already beyond the A13's memory for Enhance and Artistic.

## Constraints

- No ads, no analytics, no accounts, no network permission, no model download; `reference/` read-only; the debug build keeps `x86_64`, `arm64-v8a`, `armeabi-v7a`.
- Every standing rule in `CLAUDE.md` (previews, one class per file, ViewModel and UiState per screen, Compose animation APIs, KDoc, README follows the code).
- Run 7's layout (InFlux arrangement, Kanso's colours, floating bar) is kept.

## Verification Evidence Required

| Criterion | Class | Evidence / what to look at | Signed off |
|---|---|---|---|
| 1-6, 8, 10, 12, 14-23, 25, 27-31, 33, 34 | machine | the named test classes and commands | n/a |
| 7, 9, 11, 13 | machine-then-human | driven on the device by the Verifier, then the person looks | ☐ |
| 24, 26, 32, 35 | human | open each tool, look, read | ☐ |
| R1, R2 | machine | the named `git grep` prints nothing | n/a |
