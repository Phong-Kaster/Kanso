# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this is wrong.

- **Stage:** escalated — waiting for D-001 (DoD approval + `## Baselines` list). On approval: consume D-001 (§6.2), tick DoD Status, then Phase 1.
- **Next Phase:** Phase 1
  - T-001 (Fast, 2 slices) - scope: slice 1 `app/src/main/java/com/phongkaster/kanso/data/edit/edit/ColourRange.kt` (new), `HslChannel.kt` (new), `HslAdjustments.kt` (new), `EditHsl.kt` (new), `EditSettings.kt`; slice 2 `app/src/test/java/com/phongkaster/kanso/tool/edit/HslRangeWeightTest.kt` (new), `HslFormulaTest.kt` (new)
  - The formula and storage are fixed in `PLAN.md` "Fixed design" — quote that section into the Brief.
- **Queued decisions:** 1 — D-001 blocks T-001..T-005
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** (run from the repo root, under `timeout`) build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | pictures: `./gradlew :app:verifyRoborazziDebug` | record new/approved pictures: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app` | Worker compile: `powershell -NoProfile -File D:/Valve/Personal/Kanso/.harness/loop/bin/foreman-compile.ps1` (module `app`)
- **Criteria the next Phase cites (quoted from DoD.md):**
  - **DoD 2:** HSL offers nine colour ranges in this order, 40° apart: Red 0°, Orange 40°, Light green 80°, Green 120°, Light blue 160°, Blue 200°, Navy blue 240°, Purple 280°, Pink 320°, each chosen by tapping a circle filled with its colour, with the chosen circle marked. Proof (T-001 part): `HslRangeWeightTest` asserts `ColourRange.entries` order and centres.
  - **DoD 3:** Each range has its own Hue, Saturation and Lightness, 27 values in all, each from -100 to +100, with 0 meaning no change. Proof: `HslFormulaTest` asserts a new `EditSettings` holds 27 HSL values all at 0, that each can be set independently, and that values beyond the ends are clamped to -100 / +100 as the slider shows them.
  - **DoD 4:** Hue at ±100 turns a range's colours at most 20° towards a neighbouring range. Proof: `HslFormulaTest`: a fully saturated pixel at each centre, Hue +100 → +20° (±0.5°), -100 → -20° (±0.5°), +50 → about +10°.
  - **DoD 5:** Saturation -100 → grey, +100 → twice as strong, never beyond full colour. Proof: `HslFormulaTest`: a half-saturated centre pixel at -100 gives R=G=B (±1); at +100 saturation is min(2·s, 1) (±1/255), channels within 0..255.
  - **DoD 6:** Lightness -100 darkens strongly, +100 lightens strongly, never pure black or white. Proof: `HslFormulaTest`: saturated centre pixels at -100 lose at least half their lightness and are not (0,0,0); at +100 gain at least half the distance to white and are not (255,255,255).
  - **DoD 7:** Soft edges. Proof: `HslRangeWeightTest`: weight 1 at each centre, 0.5 + 0.5 at each midpoint (neighbours sum to 1 for every hue), 0 at and beyond the next centre, never increasing from centre to next centre; the gate gives ≤ 0.05 for greys, (10,10,10), (245,245,245) and any colour whose max−min ≤ 8/255.
  - **DoD 8:** No banding. Proof: `HslFormulaTest`: a 361-pixel full-saturation hue ramp through Blue Hue +100, Saturation +100, Lightness -100 one at a time; neighbouring output pixels never differ by more than 12/255 in any channel.
  - **DoD 9:** Blue leaves skin, reds and greys. Proof: `HslFormulaTest`: Blue H/S/L each at ±100; a skin tone (~25° hue), pure red and five greys change by ≤ 1/255 per channel.
- **Open issues touching the next Phase's files (quoted from ISSUES.md):** none
- **Reference topics the next Phase touches:** `.harness/knowledge/reference/run11-facts.md`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
