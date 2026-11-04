# PLAN — Run 11, HSL on the Edit screen (`loop/kanso-hsl`)

> Machine-owned execution strategy. The human never reviews this file - only the Definition of Done.
> Evolves through Tier-1 amendments (logged in AMENDMENTS.md) and Tier-2 queued decisions.

Path prefixes: **M** = `app/src/main/java/com/phongkaster/kanso/`, **T** = `app/src/test/java/com/phongkaster/kanso/`,
**R** = `app/src/main/res/`. Base commit: `8b2b0f2`.

## Strategy

Four Phases, one checkpoint commit each, in the PRD's order: formula → render (Done + live preview) →
the tool on screen → the written record and pictures. The RGB tool (commit `72e7174`) is the template
for every UI step; `EditColourMatrix` is the template for "one formula, one place".

### Fixed design (so every Worker task is Fast)

- **Engine: CPU, on the existing preview copy.** `FilterPreviewRenderer` already runs the real
  `EditProcessor` on a ≤ 640 px copy with only the filter and layers (`filterOnly`), and
  `LivePreviewImage` draws `EditColourMatrix` on top as a GPU `ColorFilter`. HSL is a new
  `EditProcessor` step **after the filter, before the colour matrix**, run only when
  `settings.hasHslChange`; `filterOnly` carries the HSL values, so the preview copy holds
  filter + HSL and the GPU matrix goes on top — the PRD's order in both paths through one function.
  No shader (AGSL needs API 33, `minSdk` 27, and Done would need a second copy of the formula); no
  3D LUT (interpolation is not pixel-identical to the formula). Recorded in ADR-008 (T-004).
- **The formula object:** `M/data/edit/edit/EditHsl.kt`, a pure `object` (no Android types in its
  maths) with `fun apply(pixels: IntArray, adjustments: HslAdjustments)` over ARGB ints, alpha kept.
  Per pixel: RGB → HSL (h in degrees, s, l in 0..1). Precompute once per call a 3600-bin per-hue
  table of the blended (dH, dS, dL).
  - **Range weight** for centre c: d = circular |h − c| in degrees; `w = cos²(π·d/80)` for d < 40,
    else 0 → 1 at the centre, 0.5 + 0.5 at a midpoint, 0 at the next centre, smooth.
  - **Colourfulness gate** `g = smoothstep(0.03, 0.25, max − min)` (RGB in 0..1): greys,
    near-blacks and near-whites ≈ 0.
  - Blended deltas `dX = g · Σ wᵢ · Xᵢ`, each Xᵢ in −1..1.
  - Hue: `h' = h + 20° · dH` (wrapped). Saturation: `s' = clamp(s · (1 + dS), 0, 1)`.
    Lightness: `dL > 0 → l' = l + max(0, 0.95 − l) · dL`; `dL < 0 → l' = l − max(0, l − 0.05) · |dL|`.
  - HSL → RGB, round to 8 bits.
- **Storage:** `EditSettings` gains `hsl: HslAdjustments = HslAdjustments.NEUTRAL` (added **last**
  in the constructor so positional calls keep compiling) and `val hasHslChange`.
  `HslAdjustments` (own file, data class) holds `hue`, `saturation`, `lightness`: three
  `List<Float>` of nine, indexed by `ColourRange.ordinal`, each −1..1 (the slider shows ×100, like
  RGB's `red`), with `operator fun get(range, channel)`, `fun with(range, channel, value)` (clamps)
  and `val isNeutral`. `ColourRange` (enum, nine, `centreDegrees`) and `HslChannel` (enum HUE,
  SATURATION, LIGHTNESS) live in `M/data/edit/edit/` — a deliberate departure from `RgbChannel` in
  `ui/tool/model/`, because `HslAdjustments.get` needs the channel. Their labels and colours are UI
  and live in the composables.
- **Screen:** `ToolUiState` gains `hslRange: ColourRange = RED` and `hslChannel: HslChannel = HUE`;
  `ToolViewModel.chooseHslRange` / `chooseHslChannel` change no setting (as `chooseRgbChannel`).
  The slider drag uses the existing `previewSettings` / `commitSettings` (one drag = one Undo step).
- **Sheet layout in 136 dp** (A-001): three rows of 40 dp layout height each plus 8 dp gaps —
  the nine circles (32 dp disc in a 40 dp cell, horizontally scrollable when the screen is narrower
  than the row), three compact single-choice chips, one slider. Each control's **touch** area is
  ≥ 48 dp through Compose's minimum-touch-target expansion (`minimumInteractiveComponentSize`
  semantics or `Modifier.clickable` on a < 48 dp node), measured as `StickerFrameTest` does. Circle
  fills come from `Color.hsl(centre, 0.75f, 0.5f)` — never a `Color(0x` literal. The marked circle
  has a ring and a check in `MaterialTheme.colorScheme` roles.
- **Slider track:** `ClassicSlider`, `SliderWithValue`, `LabelledSlider` gain an optional
  `trackBrush: Brush? = null` that defaults to today's look, so no existing picture moves. HSL
  passes: Hue — gradient from centre −20° to centre +20°; Saturation — grey to the range's full
  colour; Lightness — dark to light of the range colour.
- **Folders (CLAUDE.md):** `M/ui/tool/component/toolpanel/editpanel/hslpanel/HslPanel.kt` (child,
  with sub-components), `hslpanel/HslChips.kt`, `hslpanel/HslSlider.kt`,
  `hslpanel/colourrangerow/ColourRangeRow.kt` and `colourrangerow/ColourRangeCircle.kt`. A separate
  `ColourRangeCircle` rather than moving `textpanel/ColourCircle.kt` (which would touch the Text
  tool).
- **Strings:** a new `R/values/strings_edit_hsl.xml`, every name prefixed `edit_hsl_` (tool label
  "HSL", nine range names, Hue/Saturation/Lightness). Never edit `strings.xml`.
- **Icon:** Material Symbols Rounded `invert_colors` as `R/drawable/ic_invert_colors.xml`, fetched by
  the Iteration (Workers have no network) before Phase 3's Workers, `android:tint` line removed,
  drawn with `Icon(painterResource(...))`.
- **README rule (A-002):** each commit that changes what the app does updates `README.md`: Phase 2
  (the render step exists; HSL row "not wired"), Phase 3 (the tool works: row "works", section 3.x
  trace), Phase 4 (A13 cost, ADR link, known limits).
- **Pictures (B1):** Phase 3 makes the row nine tools, which moves every picture that shows the row.
  The owner approves that in the PRD; the wrapper needs the list under `## Baselines` in
  `DECISIONS.md` (asked in D-001). Phase 3's Iteration copies each old picture to
  `.harness/run/evidence/baseline-diffs/<name>.old.png`, re-records through
  `foreman-record-baselines.ps1`, and looks at each `_compare.png`. Phase 4 adds the new pictures
  83–94 (T-005) and records them through the same wrapper.

## Task Graph

- T-001 - The HSL formula recolours one colour range of a pixel array (depends on: -) - 7 files, 2 slices - tier: Fast
- T-002 - The saved photo and the live preview both apply HSL (depends on: T-001) - 8 files, 2 slices - tier: Fast
- T-003 - HSL is the ninth Edit tool: circles, chips, slider, Undo per drag (depends on: T-002) - 27 files, 5 slices - tier: Fast
- T-004 - The written record describes HSL (depends on: T-003) - 5 files - tier: Fast
- T-005 - The pictures show HSL (depends on: T-003) - 1 file - tier: Fast

Scopes are in each `TASKS/T-00N.md`.

## Phase Grouping

| Phase | Tasks | Shared files the Iteration / Wiring Worker writes |
|---|---|---|
| 1 | T-001 | - |
| 2 | T-002 | - |
| 3 | T-003 | `R/drawable/ic_invert_colors.xml` (Iteration, fetched); re-recorded row baselines (Iteration, via the wrapper) |
| 4 | T-004, T-005 | new baselines 83–94 (Iteration, via the wrapper); `.harness/run/evidence/test-edits.md` |

`EditSettings.kt` (T-001, T-003), `ToolViewModel.kt` (T-002, T-003) and `README.md` (T-002, T-003,
T-004) are shared only across Phases, which run in order, so no Wiring Worker is needed.

## Known Risks

- **Galaxy A13 stutter.** Every drag step re-renders the ≤ 640 px copy on the CPU. The per-hue table
  keeps each pixel to one RGB↔HSL round trip; if Phase 3's device check stutters, reduce the
  preview edge during a drag (still the same formula) before considering another engine.
- **Parity on gradients.** The preview applies HSL to a shrunk copy; Done at full size. The parity
  test uses a ≤ 640 px source so nothing is shrunk, as `ColourMatrixParityTest` does.
- **Row taps.** With nine tools, Stickers/Text/Date move further off screen; tests that tap them
  without `scrollToolRowTo` break. Known: `EditToolRowTest`, `EditLayoutTest`, `StickerAddTest`,
  `ToolScreenshotTest.openSheet`, `StickerScreenshotTest`. Others are attributed at build time.
- **Exhaustive `when`s** over `EditTool`: `EditSettings.isNeutral`, `EditToolRow` (icon, label),
  `EditPanel`, `EditToolRowTest:501,513` — all in T-003, which is why `EditTool.HSL` is not added in
  T-001.
- **Shared slider.** `ClassicSlider` is used by every slider; `trackBrush` must default to today's
  drawing or existing pictures move.
