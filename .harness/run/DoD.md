# Definition of Done — Run 13: a Colour group on the Edit screen

> Derived from `PRD.md` at bootstrap. Human-owned after approval: the engine may propose changes (Tier 3) but never apply them.
> Every criterion must be verifiable by evidence, and must declare WHO can verify it (ADR-015).
> Base commit: `772e6dd` (main). Loop Branch: `loop/kanso-colour-group`.

## Status

- [ ] APPROVED — approve via the pending `.harness/run/ESCALATION.md` entry D-001; edit criteria freely before approving.
      Approval covers the Verification Class of each criterion, not only its wording.

## How the row will look (the engine's reading of the PRD; edit if wrong)

- **Layer 1:** Crop, Rotate, Adjust, **Colour**, Filters, Stickers, Text, Date | Compare (pinned right, as today).
- **Layer 2** (after tapping Colour): **Back** (pinned left) | RGB, HSL, Curves | Compare (still pinned right).
- Tapping Colour only switches the row; it opens no tool and closes any other tool's open sheet.
- Back (on the row) closes an open RGB, HSL or Curves sheet, keeps its values and shows layer 1 — what
  Beautify's Back does today.
- The phone's own Back gesture works as today: it first closes an open sheet, then asks to discard
  changes (or leaves when there are none). It does not step from layer 2 to layer 1, as in Beautify.
- Beautify keeps its own look (its Back stays on the right). Its Back entry and its slide between rows
  are moved into shared pieces that both Beautify and Edit use, so there is one two-level pattern.
- Icon: Colour takes the palette icon RGB has today (the same one Settings uses for "Theme colour");
  RGB gets a new Material Symbols Rounded icon, "instant_mix" (three sliders).

## Acceptance Criteria

### Behaviour

1. [machine] The first row of Edit tools reads Crop, Rotate, Adjust, Colour, Filters, Stickers, Text, Date, with Compare pinned at the end; RGB, HSL and Curves are not in it.
   Proof: a Robolectric test (900 dp wide window) opens Edit on a sample photo with no tool open; the entries' `positionInRoot.x` strictly increase in that order, there are exactly 8 entries plus `edit-compare`, and no `edit-tool-RGB`, `edit-tool-HSL` or `edit-tool-CURVES` node exists. The Colour entry's text is exactly "Colour".
2. [machine] Tapping Colour changes the row to Back, RGB, HSL, Curves, with Compare still at the end, and tapping Back brings the first row back.
   Proof: from criterion 1's state, tap `edit-group-COLOUR`; `edit-group-back` is left of `edit-tool-RGB` < `edit-tool-HSL` < `edit-tool-CURVES` < `edit-compare` by `positionInRoot.x`; no layer-1 tool tag exists; no sheet is open (`openTool == null`). Back's x is unchanged after the list is scrolled. Tap `edit-group-back`: the 8 layer-1 entries are back.
3. [machine] Tapping Colour while another tool's sheet is open closes that sheet.
   Proof: fresh Edit, open Adjust from the row, tap `edit-group-COLOUR`; `openTool == null` and layer 2 is shown; the Adjust settings are unchanged.
4. [machine] Colour shows the "changed" dot while RGB, HSL or Curves holds a change, and inside the group each of the three shows its own dot.
   Proof: for each of RGB, HSL and Curves in turn, starting from a fresh session with no change, make one change to that tool only through its sheet, then go Back to layer 1: `edit-group-dot-COLOUR` exists; tap Colour: only that tool's `edit-tool-dot-<NAME>` exists. With all three neutral, `edit-group-dot-COLOUR` does not exist.
5. [machine] Going Back to the first row from an open colour tool closes its sheet and keeps the change, as in Beautify.
   Proof: fresh session; tap Colour, open HSL, move one slider, tap `edit-group-back`: `openTool == null`, the HSL settings still hold the change, layer 1 is shown and `edit-group-dot-COLOUR` exists. Repeat for RGB and Curves.
6. [machine] With only a colour change made, leaving Edit still asks whether to discard it.
   Proof: fresh session, HSL saturation moved, sheet closed, row on layer 2: press the top-bar Back → the discard dialog shows (`discardPrompt == true`). Same from layer 1. Same with the HSL sheet still open: the first system Back closes the sheet, the second shows the dialog. Repeat the layer-2 case for RGB and Curves.
7. [machine] Compare still shows the original photo, and while it is held every entry on either row is disabled except Compare.
   Proof: fresh session, HSL changed, row on layer 2, Compare switched on: the photo's sampled pixels equal the original's (as `CompareTest` samples them); `edit-group-back`, RGB, HSL and Curves are not enabled; `edit-compare` is enabled. Same on layer 1 with `edit-group-COLOUR` not enabled.
8. [machine] The row stays on the Colour group across a screen rotation.
   Proof: `StateRestorationTester` from "layer 2 shown, Curves sheet open"; after `emulateSavedInstanceStateRestore()` layer 2 is shown.
9. [machine] Nothing about RGB, HSL or Curves changes: their sheets, sliders, curves, formulas, order in the pipeline, Undo/Redo and the saved photo.
   Proof: `git diff 772e6dd --stat` shows no change under `app/src/main/java/com/phongkaster/kanso/data/edit/` except KDoc lines of `EditTool.kt` (the enum keeps its 10 cases in order; `RgbMatrixTest`'s size check passes unedited), and none under `ui/tool/component/toolpanel/editpanel/` (the three sheets). These tests are unedited (`git diff 772e6dd` empty on them) and pass: `RgbMatrixTest`, `RgbToolTest`, `RgbSheetTest`, `HslToolTest`, `CurvesToolTest`, `CurvesPreviewTest`, `CurvesSheetTest`, `CurveChipRowTest`, and the Edit save/undo tests that cover these tools (the Iteration names them in `evidence/test-edits.md` before Phase 1).
10. [machine] Beautify's Face and Body rows look and work exactly as before.
    Proof: `BeautifyToolTest` is unedited and passes; pictures `47_*` and `48_*` are unchanged under `verifyRoborazziDebug`.

### Appearance and reachability

11. [machine-then-human] Both rows are clear and readable in light and dark: "Colour" on one line under its icon, Back easy to find at the left of the group, the dots visible.
    Proof: new pinned pictures `101_edit_colour_row_light.png`, `102_edit_colour_row_dark.png` (layer 1, HSL changed, Colour dot showing) and `103_edit_colour_group_light.png`, `104_edit_colour_group_dark.png` (layer 2, HSL sheet open, HSL dot showing), recorded and judged by the engine; a label test with `@GraphicsMode(NATIVE)` checks "Colour" has `lineCount == 1`. Then a person opens the four pictures and looks.
12. [machine-then-human] On the owner's Galaxy A13, the Colour round trip works and looks right.
    Proof: driven only on the device with serial `R58W1120QVA` (never the attached emulator), fresh install of the debug build: open a photo in Edit, then (a) Colour is visible without swiping, (b) tap Colour: Back, RGB, HSL, Curves and Compare are visible, (c) open HSL, change it, tap Back: the Colour dot shows, (d) hold Compare: the original shows, (e) system Back asks to discard, (f) Done, Save: the saved photo has the HSL change. The owner then repeats it and signs.
13. [human-only] Moving into and out of the Colour group feels the same as Beautify's Face and Body groups.
    Proof: on the A13, tap Colour and Back a few times, then open Beautify and tap Face and Back. Expect the same sideways slide, the same speed, nothing jumping.

### Build, start and quality

14. [machine] The app builds, opens, and every test, lint and screenshot check passes.
    Proof: `./gradlew :app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug` and `:app:verifyRoborazziDebug` all succeed at the final commit (lint reports no new issue against the base).
15. [machine] Only the pictures the owner approved changed, and the four new ones exist.
    Proof: `git diff --name-status 772e6dd -- app/src/test/screenshots` shows M only for paths listed under `## Baselines` in `DECISIONS.md`, and A only for `101_…` to `104_…`. Any other picture that changes is a defect.
16. [machine] The house rules in CLAUDE.md hold for every new and changed file.
    Proof: `PreviewsRenderTest`, `PreviewCoverageTest`, `OneTypePerFileTest`, `KdocCoverageTest` and `ScreenStateTest` pass; each new composable is in its own file named after it with a light and dark preview; the new string lives only in `res/values/strings_edit_colour.xml` with the prefix `edit_colour_`; no new ViewModel. (Whether the comments actually teach is judged by the Reviewer, not by this criterion.)
17. [machine] The README and the feature docs describe the new row.
    Proof: `README.md`, `docs/features/edit.md` and `docs/features/tool-screen.md` describe layer 1 and the Colour group (with Back) in the new order; the RGB, HSL and Curves rows of "Editing features" and their "end to end" sections say they are reached through Colour; grep finds no "ten tools", "fourth entry", "fifth entry" or "sixth entry" about RGB, HSL or Curves; every `app/src/…` path the README names exists.
18. [human-only] The README tells the truth about RGB, HSL and Curves.
    Proof: read the README's "Editing features" rows and "end to end" sections for RGB, HSL and Curves, and the Edit row description, then use the three tools on the phone. Expect no untrue sentence; note any that is.
19. [machine] Every test the run adds, changes or removes is listed with its reason.
    Proof: `.harness/run/evidence/test-edits.md` names each test file in `git diff 772e6dd --name-status -- app/src/test/java` with its reason.

## Removals

Repository class: product. Only what the PRD replaces is removed.

R1. [machine] RGB, HSL and Curves no longer have entries of their own in the first row.
    Proof: criterion 1's test; the first row is built from the new top-row list, not from `EditTool.entries` directly.
R2. [machine] The old pictures of the row scrolled to HSL show the Colour group instead.
    Proof: `ToolScreenshotTest`'s two `edit_tool_row_hsl_*` tests open Colour instead of scrolling the first row to HSL, and pictures `83_*`/`84_*` are re-recorded (listed for approval in D-001).

Not removed: the `EditTool` cases RGB, HSL and CURVES, which name the tools in the pipeline.

Questions: none — nothing else in the Edit row becomes unused.

## Constraints

- PRD decision 5: the three tools' sheets, sliders, curves, formulas, pipeline order, Undo/Redo and saved photo do not change.
- CLAUDE.md applies in full (previews, one type per file, screen package hierarchy, motion with Compose's animation APIs, KDoc, README in the same change).
- No network permission, no analytics, `reference/` untouched.
- Device testing is on the Galaxy A13 `R58W1120QVA` only.

## Verification Evidence Required

| Criterion | Class | Evidence / what to look at | Signed off |
|---|---|---|---|
| 1-10 | machine | Robolectric tests named in `evidence/test-edits.md`; `git diff 772e6dd` checks | n/a |
| 11 | machine-then-human | pictures 101-104; then a person opens them | ☐ |
| 12 | machine-then-human | device run on `R58W1120QVA`; then the owner repeats it | ☐ |
| 13 | human-only | A13: Colour in/out vs Beautify Face in/out | ☐ |
| 14-17, 19 | machine | Gradle output in `evidence/`; greps | n/a |
| 18 | human-only | read README's RGB/HSL/Curves parts, use the tools | ☐ |
| R1, R2 | machine | criterion 1's test; `ToolScreenshotTest` 83/84 | n/a |
