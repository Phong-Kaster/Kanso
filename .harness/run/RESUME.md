# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this is wrong.

- **Stage:** build — Phase 1 done (T-001 complete, checkpointed); next Phase 2.
- **Next Phase:** Phase 2
  - T-002 (Fast, 2 slices; see `TASKS/T-002.md`, full paths) - slice 1: `app/src/main/java/com/phongkaster/kanso/data/edit/edit/EditProcessor.kt`, `FilterPreviewRenderer.kt` (same dir), `app/src/main/java/com/phongkaster/kanso/ui/tool/ToolViewModel.kt` (preview code only), `app/src/main/java/com/phongkaster/kanso/ui/tool/component/photocard/LivePreviewImage.kt` (KDoc only), `README.md`; slice 2: `app/src/test/java/com/phongkaster/kanso/tool/edit/EditProcessorTest.kt`, `FilterPreviewRendererTest.kt`, `HslParityTest.kt` (new)
  - T-001 left: `EditHsl.apply(pixels: IntArray, adjustments: HslAdjustments)`, `EditSettings.hsl`, `hasHslChange` (not yet in `isNeutral(tool)`; T-003). Evidence file `.harness/run/evidence/test-edits.md` exists: append every test T-002 adds/edits.
  - Fixed design: `PLAN.md` "Fixed design" - quote it into the Brief.
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** (run from the repo root, under `timeout`) build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | pictures: `./gradlew :app:verifyRoborazziDebug` | record new/approved pictures: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app` | Worker compile: `powershell -NoProfile -File D:/Valve/Personal/Kanso/.harness/loop/bin/foreman-compile.ps1` (module `app`)
- **Criteria the next Phase cites (quoted from DoD.md):**
  - **DoD 15:** The HSL change is applied after the filter and before Adjust and RGB. Proof: `EditProcessorTest`: with a filter, a Blue change and a Brightness change, the output equals filter → HSL → colour matrix computed by hand, and differs from HSL before the filter.
  - **DoD 16:** The live preview and the saved photo use one HSL formula, in one place, and agree. Proof: `HslParityTest` (modelled on `ColourMatrixParityTest`) renders a ≤ 640 px source through the live-preview path and through `EditProcessor` with the same HSL settings: every channel within 1/255. A source search finds the per-pixel HSL formula defined only in `EditHsl.kt`.
  - **DoD 17:** Dragging an HSL slider updates the photo on screen while the finger moves, even when no filter is chosen. Proof: `HslToolTest` (or `FilterPreviewRendererTest`): with no filter and no layers, a preview-only HSL value produces a new preview bitmap that differs from the unfiltered photo, before the drag is released.
  - **DoD 27:** With all 27 values at 0, the saved picture is pixel-identical to one saved without the tool. Proof: `HslParityTest` (or `EditProcessorTest`): `EditProcessor` with neutral HSL and with a filter, crop and Adjust change gives every pixel equal to the same render with the HSL step removed (the step is skipped when `hasHslChange` is false).
- **Open issues touching the next Phase's files (quoted from ISSUES.md):** none
- **Reference topics the next Phase touches:** `.harness/knowledge/reference/run11-facts.md`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
