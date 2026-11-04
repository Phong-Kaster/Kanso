# Definition of Done — Run 11: HSL on the Edit screen

> Derived from `PRD.md` at bootstrap (2026-11-04). Human-owned after approval: the engine may propose changes (Tier 3) but never apply them.
> Every criterion must be verifiable by evidence, and must declare WHO can verify it (ADR-015).
> Commands below run from the repository root, each under `timeout`: `./gradlew :app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:verifyRoborazziDebug`.

## Status

- [ ] APPROVED — approve via the pending `.harness/run/ESCALATION.md` (D-001); edit criteria freely before approving.
      Approval covers the Verification Class of each criterion, not only its wording.

## Acceptance Criteria

### Behaviour

1. [machine] The Edit screen has nine tools, and the new one, named "HSL", sits right after RGB and before Filters, with an icon unlike Adjust's and RGB's.
   Proof: `EditToolRowTest` asserts the row order `… ADJUST, RGB, HSL, FILTERS …`, the label "HSL", and that HSL's drawable differs from Adjust's (`ic_tune`) and RGB's (`ic_palette`); driven from a freshly opened Edit screen.
2. [machine] HSL offers nine colour ranges in this order, 40° apart: Red 0°, Orange 40°, Light green 80°, Green 120°, Light blue 160°, Blue 200°, Navy blue 240°, Purple 280°, Pink 320°, each chosen by tapping a circle filled with its colour, with the chosen circle marked.
   Proof: `HslRangeWeightTest` asserts `ColourRange.entries` order and centres; `HslSheetTest` asserts nine circles in that order, each with its range name as its description, exactly one selected (`Role.RadioButton`, selected state) after each tap.
3. [machine] Each range has its own Hue, Saturation and Lightness, 27 values in all, each from -100 to +100, with 0 meaning no change.
   Proof: `HslFormulaTest` asserts a new `EditSettings` holds 27 HSL values all at 0, that each can be set independently, and that values beyond the ends are clamped to -100 / +100 as the slider shows them.
4. [machine] Hue at +100 or -100 turns a range's colours at most 20° towards a neighbouring range, in opposite directions for the two ends.
   Proof: `HslFormulaTest`: a fully saturated pixel at each range's centre hue, Hue +100 → hue shifts +20° (±0.5°), -100 → -20° (±0.5°), +50 → about +10°.
5. [machine] Saturation at -100 turns a range grey, and at +100 makes it twice as strong, never beyond full colour.
   Proof: `HslFormulaTest`: a half-saturated centre pixel at -100 gives R=G=B (±1); at +100 its saturation is min(2·s, 1) (±1/255) and every channel stays within 0..255.
6. [machine] Lightness at -100 darkens a range strongly and at +100 lightens it strongly, but a range colour never becomes pure black or pure white.
   Proof: `HslFormulaTest`: saturated centre pixels at -100 lose at least half their lightness and are not (0,0,0); at +100 gain at least half the distance to white and are not (255,255,255).
7. [machine] Range edges are soft: a colour exactly at a range's centre belongs to it fully, a colour halfway between two centres is shared equally by both, the share falls smoothly to nothing at the next centre, and greys, near-blacks and near-whites are hardly touched.
   Proof: `HslRangeWeightTest`: weight 1 at each centre, 0.5 + 0.5 at each midpoint (the two neighbours sum to 1 for every hue), 0 at and beyond the next centre, never increasing from centre to next centre; the colourfulness gate gives ≤ 0.05 for greys, (10,10,10), (245,245,245) and any colour whose max−min ≤ 8/255.
8. [machine] A smooth gradient of colours, like a sky, never shows a band after an HSL change.
   Proof: `HslFormulaTest`: a 361-pixel ramp through every hue at full saturation, put through Blue Hue +100, Saturation +100 and Lightness -100 one at a time; neighbouring output pixels never differ by more than 12/255 in any channel.
9. [machine] Moving any Blue slider leaves skin tones, reds and greys unchanged.
   Proof: `HslFormulaTest`: Blue Hue, Saturation and Lightness each at +100 and -100; a skin tone (about 25° hue), a pure red and five greys from black to white change by at most 1/255 per channel.
10. [machine] The first time HSL opens, Red and Hue are chosen; reopened, it shows the range and setting last chosen.
    Proof: `HslToolTest` (modelled on `RgbToolTest`), driven from a fresh Edit session: open HSL → Red + Hue; choose Purple and Lightness, open another tool, reopen HSL → Purple + Lightness.
11. [machine] Choosing a range or a setting never changes the photo and adds nothing to Undo.
    Proof: `HslToolTest`: after each range tap and each chip tap, `EditSettings` and the Undo availability are unchanged.
12. [machine] One drag of the HSL slider is one Undo step, and Redo brings it back.
    Proof: `HslToolTest`: a drag with many intermediate values then a release gives exactly one Undo entry; Undo restores 0, Redo restores the dragged value.
13. [machine] The HSL tool shows its "changed" dot while any of its 27 values is away from 0, and only then; no other tool's dot lights up because of HSL.
    Proof: `EditToolRowTest` (or `HslToolTest`) sets each of the 27 values in turn and back to 0, checking HSL's dot and Adjust's and RGB's.
14. [machine] Compare shows the photo without the HSL change, Back after an HSL change asks "Discard your changes?", and the HSL sheet has no reset button.
    Proof: `HslToolTest` and `HslSheetTest`, driven from a session with Blue Saturation at +60: comparing shows the unedited photo; Back opens the discard dialog; no node in the HSL sheet is labelled reset.
15. [machine] The HSL change is applied after the filter and before Adjust and RGB.
    Proof: `EditProcessorTest`: with a filter, a Blue change and a Brightness change, the output equals filter → HSL → colour matrix computed by hand, and differs from HSL before the filter.
16. [machine] The live preview and the saved photo use one HSL formula, in one place, and agree.
    Proof: `HslParityTest` (modelled on `ColourMatrixParityTest`) renders a ≤ 640 px source through the live-preview path and through `EditProcessor` with the same HSL settings: every channel within 1/255. A source search finds the per-pixel HSL formula defined only in `EditHsl.kt`.
17. [machine] Dragging an HSL slider updates the photo on screen while the finger moves, even when no filter is chosen.
    Proof: `HslToolTest` (or `FilterPreviewRendererTest`): with no filter and no layers, a preview-only HSL value produces a new preview bitmap that differs from the unfiltered photo, before the drag is released.

### Appearance and reachability

18. [machine] The tool row's pictures show nine tools, and a new pair shows the row scrolled so HSL is in view.
    Proof: `verifyRoborazziDebug` against the re-recorded `55_edit_tool_row_light.png`, `56_edit_tool_row_dark.png` and the new `83_edit_tool_row_hsl_light.png`, `84_edit_tool_row_hsl_dark.png`, each looked at by the engine once when recorded.
19. [machine] The HSL sheet looks right with each of its three settings: nine coloured circles with the chosen one marked, three chips, and a slider whose track shows what it does — a hue gradient for Hue, grey to full colour for Saturation, dark to light for Lightness — in light and dark.
    Proof: `verifyRoborazziDebug` against new `85_edit_hsl_red_hue_light.png`, `86_edit_hsl_red_hue_dark.png`, `87_edit_hsl_blue_saturation_light.png`, `88_edit_hsl_blue_saturation_dark.png`, `89_edit_hsl_orange_lightness_light.png`, `90_edit_hsl_orange_lightness_dark.png`, each looked at by the engine once when recorded.
20. [machine] A Blue change and an Orange change show visibly on a photo, in light and dark.
    Proof: `verifyRoborazziDebug` against new `91_edit_hsl_blue_after_light.png`, `92_edit_hsl_blue_after_dark.png`, `93_edit_hsl_orange_after_light.png`, `94_edit_hsl_orange_after_dark.png`, drawn from a sample photo holding a sky blue, an orange, a skin tone and a grey; the before state is picture 61/62's photo with no change. Each looked at by the engine once when recorded.
21. [machine] The sheet stays 136 dp tall with all three rows fully visible, the photo does not change size when HSL opens, and every circle, chip and the slider is at least 48 dp to touch.
    Proof: `HslSheetTest`: sheet height 136 dp; circles, chips and slider all displayed without scrolling the sheet vertically on a 411 dp wide screen; the photo's bounds equal those with Adjust open; each control's touch bounds ≥ 48 dp (as `StickerFrameTest` measures them).
22. [machine] The mark on the chosen circle stands out against all nine colours, in light and in dark.
    Proof: `a11y/ColourRangeMarkContrastTest` (modelled on `RgbTintContrastTest`): contrast ≥ 3:1 between the mark and each circle fill, and between each fill's edge and the sheet surface or a ring, for Kanso blue and four custom seeds, light and dark.
23. [machine-then-human] Pictures that changed only because the row gained a tool really differ only in the row.
    Proof: each re-recorded picture's old version is kept as `<name>.old.png` in `.harness/run/evidence/baseline-diffs/`; the engine compares each pair first; then the owner opens each pair side by side and expects the only difference to be the tool row.
24. [machine-then-human] On a phone, HSL is easy to find and use: the circles read as their colours, the chosen one is obvious, and a bluer sky does not change skin.
    Proof: driven first by the Verifier through `foreman-device.ps1` on the emulator or the Galaxy A13 (only if nobody is using it), from a fresh install with photo access granted: open a photo in Edit, scroll the row to HSL, open it, choose Blue, Saturation, drag to +60. Then the owner: open a photo with sky and a face, Edit → HSL → Blue → Saturation, drag right; expect the sky bluer, the face unchanged, each circle clearly its colour, the chosen one obvious, in light and dark.
25. [human-only] On the owner's Galaxy A13, the photo follows the finger while any HSL slider is dragged, with no stutter.
    Proof: on the Galaxy A13 (32-bit Android), open a full-size camera photo in Edit → HSL; for Blue and Orange, drag each of Hue, Saturation and Lightness end to end and back. Expect the photo to follow smoothly. A stutter is a failure of the run, reported, not shipped.
26. [human-only] The README, the two feature pages and the ADR tell the truth about HSL and are readable.
    Proof: the owner reads README's HSL row and section, `docs/features/edit.md`, `docs/features/tool-screen.md` and ADR-008, then uses HSL; expect no untrue sentence.

### Build, start and quality

27. [machine] With all 27 values at 0, the saved picture is pixel-identical to one saved without the tool.
    Proof: `HslParityTest` (or `EditProcessorTest`): `EditProcessor` with neutral HSL and with a filter, crop and Adjust change gives every pixel equal to the same render with the HSL step removed (the step is skipped when `hasHslChange` is false).
28. [machine] The other eight tools render exactly as before and every test that existed before this run still passes.
    Proof: `./gradlew :app:testDebugUnitTest` exits 0 with 0 skipped and at least the base's test count; `verifyRoborazziDebug` exits 0; every baseline outside 18–20's new files and 23's owner-approved list is byte-identical to the base (`git diff --stat 8b2b0f2 -- app/src/test/screenshots/`); every changed or removed test is listed with its reason in `.harness/run/evidence/test-edits.md`.
29. [machine] No new permission, library or native library is added unless ADR-008 justifies it.
    Proof: `git diff 8b2b0f2 -- gradle/libs.versions.toml '*.gradle.kts' '*.gradle' app/src/main/AndroidManifest.xml` adds no dependency or permission line, and the debug APK's `lib/*/` listing equals the base's.
30. [machine] The app builds, opens, and the Edit screen with the HSL sheet open does not crash; tests, lint and pictures pass.
    Proof: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyRoborazziDebug` exits 0; `HslToolTest` opens the Edit screen and the HSL sheet under Robolectric without throwing.
31. [machine] README describes HSL as the code is: its row in "Editing features" with its real state, and its pipeline traced end to end in "How each editing feature works".
    Proof: `README.md` names HSL in the "Editing features" table and has an HSL section naming the screen and ViewModel, `EditHsl.kt`, its place after the filter and before the colour matrix, the preview copy (≤ 640 px) and Done at full size, threading, input and output, and failure behaviour, with file paths that exist; each behaviour-changing commit of this run touches `README.md`.
32. [machine] The Edit and tool-screen feature pages describe HSL, with flow diagrams.
    Proof: `docs/features/edit.md` and `docs/features/tool-screen.md` each mention HSL inside a ```` ```mermaid ```` block.
33. [machine] A new decision record says how HSL is computed and why that way, how the preview and the saved photo stay identical, and what it costs on the Galaxy A13.
    Proof: `docs/adr/ADR-008-*.md` exists with sections for the engine choice, preview-equals-Done, and the cost on the Galaxy A13 (a measured figure, or an explicit "not measured, because …").
34. [machine] The glossary defines "colour range" and says the Edit screen now has nine tools.
    Proof: `CONTEXT.md` has a "colour range" entry and its `EditTool` entry says nine tools.
35. [machine] The code follows the owner's standing rules: every new composable has a light and a dark preview, one class per file, the screen-folder layout, and every new class and function explained in plain English.
    Proof: `PreviewCoverageTest`, `PreviewsRenderTest`, `PreviewHygieneTest`, `OneTypePerFileTest`, `KdocCoverageTest` pass; the new HSL composables live under `ui/tool/component/toolpanel/editpanel/hslpanel/`, a child with sub-components in its own folder.
36. [machine] Retica's original app is untouched and nothing imports from it.
    Proof: `git diff --stat 8b2b0f2 -- reference/` is empty; no file under `app/` names `reference/` or a `com.photo.editor` import that is not an engine module.
37. [machine] Pictures were recorded only through the approved route, and every existing picture that changed is on the owner's list.
    Proof: `.harness/run/evidence/record-*.log` comes from `bin/foreman-record-baselines.ps1`; each changed existing baseline appears under `## Baselines` in `DECISIONS.md` and has its `<name>.old.png` in `.harness/run/evidence/baseline-diffs/`.

## Removals

Repository class (from PROJECT.md): product.

Nothing is removed: HSL is added beside Adjust, RGB and Filters and replaces none of them, and the PRD leaves nothing unreachable.

Questions (product repositories only): none.

## Constraints

- No ads, analytics, accounts or network permission; no new dependency, permission or native library unless ADR-008 justifies it.
- `reference/` is read-only: never edit, build or import it. Retica supplies the look of the HSL screen only.
- One HSL formula, in `EditHsl.kt`, serves both the live preview and `EditProcessor`; the step comes after the filter and before the colour matrix and is skipped entirely when all 27 values are 0.
- The sheet stays 136 dp tall and the photo never changes size.
- The Galaxy A13 (32-bit, `armeabi-v7a`) is the performance floor; the debug build keeps `x86_64`, `arm64-v8a` and `armeabi-v7a`.
- Motion uses only Compose's own animation APIs.
- Existing baselines change only through `foreman-record-baselines.ps1`, only those the owner lists, with the old copies kept in `.harness/run/evidence/baseline-diffs/`.
- `README.md` changes in the same commit as the behaviour it describes.
- Before driving a phone or emulator, check nobody else's run or person is using it.

## Verification Evidence Required

| Criterion | Class | Evidence / what to look at | Signed off |
|---|---|---|---|
| 1–17 | machine | `./gradlew :app:testDebugUnitTest` — the named tests | n/a |
| 18–20 | machine | `./gradlew :app:verifyRoborazziDebug` — the named PNGs | n/a |
| 21–22 | machine | `HslSheetTest`, `ColourRangeMarkContrastTest` | n/a |
| 23 | machine-then-human | `.harness/run/evidence/baseline-diffs/` old beside new: only the tool row differs | ☐ |
| 24 | machine-then-human | phone: Edit → HSL → Blue → Saturation +60: sky bluer, face unchanged, circles read as their colours | ☐ |
| 25 | human-only | Galaxy A13: drag every HSL slider for Blue and Orange on a full-size photo: no stutter | ☐ |
| 26 | human-only | read README HSL parts, `edit.md`, `tool-screen.md`, ADR-008, then use HSL: no untrue sentence | ☐ |
| 27–37 | machine | the commands and files named in each Proof | n/a |
