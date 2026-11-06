# RESUME BLOCK

> Regenerated every iteration. A derived cache, never a source of truth: on any disagreement with the
> task files or git, this file is wrong.

- **Stage:** bootstrap — waiting for D-001 (DoD approval, scope option, build capability)
- **Next Phase:** Phase 1
  - Wiring Worker first (alone): `app/src/main/res/drawable/ic_show_chart.xml` (Material "show chart" path, `android:fillColor="@android:color/white"`, no `android:tint`), `docs/adr/ADR-009-curves-run-on-the-cpu-after-hsl.md` (copy ADR-008's shape; the design in PLAN.md "Strategy")
  - Before dispatching T-001: run `verifyRoborazziDebug` on the untouched tree to know which pictures are already red
  - T-001 - scope: 24 files in 4 slices, listed in `TASKS/T-001.md`
- **Queued decisions:** 1 — D-001 blocks T-001..T-005
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && timeout 1800 ./gradlew :app:assembleDebug` | test: `... ./gradlew :app:testDebugUnitTest` | lint: `... ./gradlew :app:lintDebug` | screenshots: `... ./gradlew :app:verifyRoborazziDebug`; new baselines: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app` (not yet granted as Gradle rules: D-001)
- **Criteria the next Phase cites (quoted from DoD.md, pending approval):**
  - **DoD 1:** The Edit screen has a new tool, Curves, right after HSL, and it opens a sheet with a graph showing a straight diagonal line between two end points.
  - **DoD 2:** A curve left as a straight diagonal changes nothing in the photo.
  - **DoD 3:** The curve passes exactly through every point the user placed, never overshoots between two points, and never goes below black or above white.
  - **DoD 4:** Tapping an empty spot on the graph adds a point there, dragging a point moves it (it cannot pass its neighbours, and the two end points only move up and down), and a long press removes a middle point; a curve holds at most 10 points.
  - **DoD 5:** Each tap, drag, removal or Reset is exactly one Undo step, and Redo brings it back.
  - **DoD 6:** Reset puts the curve on screen back to a straight diagonal, and is disabled while that curve is already straight.
  - **DoD 12:** The Curves tool's "changed" dot shows only while some curve is bent; pressing Back with only a curve change asks to discard it; Compare shows the photo without the curves.
  - **DoD 13:** The saved photo contains the curve.
  - **DoD 14:** The chips and Reset have spoken names and touch areas of at least 48 dp, and the graph has a spoken description naming the chosen curve.
  - **DoD 20, 21:** conventions (previews, one type per file, KDoc, `strings_edit_curves.xml` with `edit_curves_`, no `Color(0x`); nothing outside the feature changes (`reference/`, `core/`, `opencv/`, `libs/`, `EditHsl.kt`).
  - Re-read the Proof lines from DoD.md by Grep on the number once approved (the human may edit them).
- **Open issues touching the next Phase's files (quoted from ISSUES.md):** Run 11 review note: "stale comments at `ToolViewModel.kt` (`PreviewKey`, `requestPreview`, `renderPreview` KDocs) and `FilterPreviewRenderer.kt:63`" (T-002's files)
- **Reference topics the next Phase touches:** `reference/run11-facts.md` (line numbers stale: sheet is 220 dp, highest picture 94), `reference/run6-compose-testing-a.md`, `reference/build-and-environment.md`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
- **Device:** Galaxy A13 attached, serial `R58W1120QVA` (SM-A135F), seen 2026-11-06
