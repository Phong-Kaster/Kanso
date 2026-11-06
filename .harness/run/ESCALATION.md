# DECISION QUEUE

> Questions the engine could not answer within its authority. Queueing one does **not** stop the run:
> the engine marks the tasks that entry blocks and keeps working on everything else. The Runtime
> stops only when no executable task remains.
>
> This file is the engine's own log. **Answer in `.harness/run/DECISIONS.md`, never here** - copy an
> entry's id as a heading there and write your decision underneath it, then re-run. The engine is
> denied write access to `DECISIONS.md`, mechanically, so nothing it does here can ever race what you
> write there. Answer any number of entries; unanswered ones stay queued and the tasks they name stay
> unselectable.

---

## D-001 - Approve the Definition of Done for the Colour group, the pictures that may change, and the build commands

- **Status:** answered - approved by the owner 2026-11-06 (DECISIONS.md D-001); applied in Iteration 2
- **Type:** DoD approval + Capability grant
- **Iteration:** 1 (bootstrap)
- **Timestamp:** 2026-11-06
- **Blocks tasks:** T-001

### Question

1. Approve `.harness/run/DoD.md` (edit it freely first). In particular, check the box "How the row will
   look" at its top — it is the engine's reading of the PRD, and four points in it are choices:
   - **a. Compare on the Colour row.** The PRD lists layer 2 as "RGB, HSL, Curves, with a pinned Back".
     The engine keeps Compare pinned at the right on layer 2 too, so "Compare still shows the original"
     works there. Say if Compare should be hidden on layer 2.
   - **b. Tapping Colour opens no tool.** Beautify opens a group's first tool at once (Face opens its
     first slider). The PRD says tapping Colour "switches the row itself", so the engine proposes it
     only switches the row (and closes any other tool's open sheet). Say if Colour should open RGB at once.
   - **c. The phone's Back gesture on layer 2** keeps today's behaviour (close an open sheet, then ask
     to discard or leave) and does not step back to layer 1 — Beautify does the same. Say if it should
     go back to layer 1 first.
   - **d. Sharing with Beautify.** To reuse Beautify's pattern rather than copy it, the engine will move
     Beautify's Back entry and its sideways slide into two shared pieces that both rows use. Beautify's
     own row will look and behave exactly as now (its Back stays at the right; criterion 10 guards it).
     Say if Beautify's files must not be touched at all (the Edit row then copies the pattern instead).
2. **The icon (PRD open item).** Recommendation: **Colour takes the palette icon** RGB has today — Settings
   already uses it for "Theme colour", so in this app it already means colour — and **RGB gets a new
   Material Symbols Rounded icon, "instant_mix"** (three vertical sliders, one per channel). It resembles
   Adjust's horizontal sliders a little; an alternative is "tonality". Or: RGB keeps the palette and
   Colour gets a new icon such as "colors". Either way exactly one icon file is added.
3. **The pictures that may change (PRD open item).** Every Edit picture whose tool row shows changes,
   because the row's fourth and fifth entries become Colour and Filters, and the HSL and Curves pictures
   now show the Colour group with Back. If you approve, copy this block **as is** into `DECISIONS.md`:

```
## Baselines

app/src/test/screenshots/16_tool_photo_light.png
app/src/test/screenshots/17_tool_photo_dark.png
app/src/test/screenshots/40_tool_narrow_photo_light.png
app/src/test/screenshots/41_tool_narrow_photo_dark.png
app/src/test/screenshots/42_tool_narrow_photo_sdk30_light.png
app/src/test/screenshots/51_tool_edit_free_crop_light.png
app/src/test/screenshots/52_tool_edit_free_crop_dark.png
app/src/test/screenshots/55_edit_tool_row_light.png
app/src/test/screenshots/56_edit_tool_row_dark.png
app/src/test/screenshots/57_edit_crop_sheet_light.png
app/src/test/screenshots/58_edit_crop_sheet_dark.png
app/src/test/screenshots/59_edit_rotate_sheet_light.png
app/src/test/screenshots/60_edit_rotate_sheet_dark.png
app/src/test/screenshots/61_edit_adjust_brightness_light.png
app/src/test/screenshots/62_edit_adjust_brightness_dark.png
app/src/test/screenshots/63_edit_filters_1977_light.png
app/src/test/screenshots/64_edit_filters_1977_dark.png
app/src/test/screenshots/65_edit_compare_on_light.png
app/src/test/screenshots/66_edit_compare_on_dark.png
app/src/test/screenshots/67_edit_pill_light.png
app/src/test/screenshots/68_edit_pill_dark.png
app/src/test/screenshots/69_edit_stickers_row_light.png
app/src/test/screenshots/70_edit_stickers_row_dark.png
app/src/test/screenshots/81_edit_stickers_crop_filter_light.png
app/src/test/screenshots/82_edit_stickers_crop_filter_dark.png
app/src/test/screenshots/83_edit_tool_row_hsl_light.png
app/src/test/screenshots/84_edit_tool_row_hsl_dark.png
app/src/test/screenshots/85_edit_hsl_red_hue_light.png
app/src/test/screenshots/86_edit_hsl_red_hue_dark.png
app/src/test/screenshots/87_edit_hsl_blue_saturation_light.png
app/src/test/screenshots/88_edit_hsl_blue_saturation_dark.png
app/src/test/screenshots/89_edit_hsl_orange_lightness_light.png
app/src/test/screenshots/90_edit_hsl_orange_lightness_dark.png
app/src/test/screenshots/91_edit_hsl_blue_after_light.png
app/src/test/screenshots/92_edit_hsl_blue_after_dark.png
app/src/test/screenshots/93_edit_hsl_orange_after_light.png
app/src/test/screenshots/94_edit_hsl_orange_after_dark.png
app/src/test/screenshots/95_edit_curves_master_straight_light.png
app/src/test/screenshots/96_edit_curves_master_straight_dark.png
app/src/test/screenshots/97_edit_curves_red_s_curve_light.png
app/src/test/screenshots/98_edit_curves_red_s_curve_dark.png
app/src/test/screenshots/99_edit_curves_lightness_s_curve_light.png
app/src/test/screenshots/100_edit_curves_lightness_s_curve_dark.png
```

   Expected **not** to change: everything else, including Beautify (47, 48) and the sticker sheets
   (71-80), whose sheet hides the row. If one of the listed pictures turns out not to change, it simply
   stays as is; if an unlisted one changes, that is a defect the run fixes, not a re-record.
   New pictures 101-104 (criterion 11) need no approval.
4. **Standing capabilities.** The approved toolchain has never been granted: every recent run asked for
   Gradle build, test and lint per run. The engine proposes granting them permanently for this
   repository, plus one goal-scoped download of the one icon file from Google's Material icons repository.

### Context

PRD Run 13: gather RGB, HSL and Curves under one Colour entry with Beautify's two-level pattern. The
survey found the PRD's "Back at the left" differs from Beautify (Back at the right); the engine follows
the PRD for Edit and leaves Beautify's look as it is. Everything else above follows the PRD's decisions 1-5.

### Options Considered

1. Approve as written - consequences: one task (T-001), one Phase; the Phase ends with all checks green
   because the approved pictures are re-recorded in the same Phase.
2. Edit choices a-d or the icon, then approve - consequences: the engine re-plans T-001 (Tier 1) and continues.
3. Leave pictures out of `## Baselines` - consequences: the screenshot check cannot go green; T-001 would stop
   at the re-record step and the run would ask again.

### Engine Recommendation

Option 1.

### Proposed Capabilities (if any)

```json
{
  "intent": "Build, test and lint the app — the verified toolchain every Iteration runs (DoD 14).",
  "command": "./gradlew :app:assembleDebug | :app:testDebugUnitTest | :app:lintDebug, run from the repository root under timeout",
  "scope": "this repository's build outputs (app/build, .gradle); no network beyond Gradle's dependency cache",
  "lifetime": "permanent",
  "allow": [
    "Bash(*gradlew*:app:assembleDebug*)",
    "Bash(*gradlew*:app:testDebugUnitTest*)",
    "Bash(*gradlew*:app:lintDebug*)"
  ],
  "target_ledger": ".harness/knowledge/capabilities.json"
}
```

```json
{
  "intent": "Download the one Material Symbols Rounded icon the DoD names (instant_mix, or the one you choose) into the app's drawables.",
  "command": "curl -sL https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android/<name>/materialsymbolsrounded/<name>_24px.xml -o app/src/main/res/drawable/ic_<name>.xml",
  "scope": "one new file under app/src/main/res/drawable/",
  "lifetime": "goal",
  "allow": [
    "Bash(curl -sL https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android/*)"
  ],
  "target_ledger": ".harness/run/capabilities.json"
}
```

### Decision

<!-- Answered in `.harness/run/DECISIONS.md`, under a heading `## D-001` - never here. Put the
     `## Baselines` block there too, as its own heading. -->
