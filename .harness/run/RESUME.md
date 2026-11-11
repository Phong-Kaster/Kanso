# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this file is wrong.

- **Stage:** escalated — bootstrap done, waiting for D-001 (DoD approval, spline, Baselines, Gradle grants, trash icon)
- **Next Phase:** Phase 1
  - T-001 - Slice A: `app/src/main/java/com/phongkaster/kanso/data/edit/edit/{CurveChannel,CurveAdjustments,EditCurves}.kt`, `app/src/main/java/com/phongkaster/kanso/ui/tool/component/toolpanel/editpanel/curvespanel/{CurveChips,CurvesPanel}.kt`; Slice B: `app/src/test/java/com/phongkaster/kanso/tool/edit/{CurveFormulaTest,CurveParityTest,CurvesSheetTest,CurveChipRowTest}.kt`, `app/src/test/java/com/phongkaster/kanso/screenshots/ToolScreenshotTest.kt`
  - Wiring after: remove `edit_curves_channel_lightness` from `app/src/main/res/values/strings_edit_curves.xml`; README.md / `docs/features/edit.md` / `docs/features/tool-screen.md` Lightness lines; delete PNGs 99/100 if listed under `## Baselines`
- **Queued decisions:** 1 — D-001 blocks T-001..T-004
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** (from `reference/build-and-environment.md`, not re-run at bootstrap: no grant) build: `cd /d/Valve/Personal/Kanso && timeout 1200 ./gradlew :app:assembleDebug` | test: `… ./gradlew :app:testDebugUnitTest` | lint: `… ./gradlew :app:lintDebug` | pictures: `… ./gradlew :app:verifyRoborazziDebug`
- **Criteria the next Phase cites (quoted from DoD.md):**
  - **DoD 13:** Curves still run on the phone's processor after HSL, and the saved photo matches the preview — including a curve that overshoots and a curve whose end points were moved.
  - **DoD 14:** RGB, HSL and the Colour group with its "changed" dots work exactly as after Run 13. Proof: `RgbMatrixTest`, `RgbSheetTest`, `HslSheetTest`, `HslParityTest`, `ColourGroupTest` unedited and green; every screenshot picture other than 95–100 and 105–108 passes `verifyRoborazziDebug` unchanged.
  - **R1:** There is no Lightness channel in Curves. Proof: `CurveChannel.entries` is exactly RGB, RED, GREEN, BLUE; grep `LIGHTNESS` in `data/CurveChannel.kt`, `cp/` finds nothing.
  - **R2:** Curves hold no Lightness curve. Proof: grep `lightness` (any case) in `data/CurveAdjustments.kt` finds nothing.
  - **R3:** The photo gets no Lightness pass from Curves. Proof: `data/EditCurves.kt` has no `applyLightness`, `hslToRgb` or `pack`; `CurveParityTest` has no Lightness test.
  - **R5:** The two Lightness pictures and their tests are gone. Proof: no `99_edit_curves_lightness*` or `100_edit_curves_lightness*` in `app/src/test/screenshots/`; no `edit_curves_lightness_s_curve_` in `ToolScreenshotTest.kt`.
- **Open issues touching the next Phase's files (quoted from ISSUES.md):**
  - Run 12 97/98 Red "S-curve" pictures show only one middle point (the first tap did not add one); look at them and fix `drawRedSCurve` in `ToolScreenshotTest.kt` if wanted. (T-004 rewrites the helper.)
- **Reference topics the next Phase touches:** `reference/run14-facts.md`, `reference/run12-facts.md`, `reference/run13-facts.md`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
