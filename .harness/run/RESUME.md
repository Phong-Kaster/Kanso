# RESUME BLOCK

> Regenerated every iteration. A derived cache, never a source of truth: on any disagreement with the
> task files or with git, this file is wrong.

- **Stage:** escalated — waiting on D-001 (DoD approval, choices a-d, icon, `## Baselines`, capabilities)
- **Next Phase:** 1
  - Wiring first: RGB icon drawable `app/src/main/res/drawable/ic_<name>.xml` (name from D-001; default `instant_mix`), `evidence/test-edits.md`
  - T-001 (Fast) - slices, sequential:
    - A: `main/ui/tool/model/EditToolGroup.kt`, `main/ui/tool/model/EditRowEntry.kt`, `main/ui/tool/component/GroupBackEntry.kt`, `main/ui/tool/component/GroupRowTransition.kt`, `main/ui/tool/component/toolpanel/beautifypanel/BeautifyToolRow.kt`, `app/src/main/res/values/strings_edit_colour.xml`
    - B: `main/ui/tool/component/editbottombar/edittoolrow/EditToolRow.kt`, `.../edittoolrow/EditColourGroupRow.kt`, `test/tool/edit/ColourGroupTest.kt`, `test/tool/edit/ToolRowScrolling.kt`, `test/tool/edit/EditToolRowTest.kt`, `test/tool/edit/EditLayoutTest.kt`
    - C: `test/tool/edit/CompareTest.kt`, `test/tool/edit/EditToolTest.kt`, `test/tool/edit/StickerAddTest.kt`, `test/tool/edit/HslSheetTest.kt`, `test/screenshots/ToolScreenshotTest.kt`
    - D: `README.md`, `docs/features/edit.md`, `docs/features/tool-screen.md`, `docs/features/beautify.md`, `main/data/edit/edit/EditTool.kt` (KDoc only)
  - Wiring after: `docs/adr/ADR-010-edit-row-has-two-levels.md`; record baselines (Iteration)
  - (`main/` = `app/src/main/java/com/phongkaster/kanso/`, `test/` = `app/src/test/java/com/phongkaster/kanso/`)
- **Queued decisions:** 1 — D-001 blocks T-001
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands** (prefix `cd /d/Valve/Personal/Kanso &&`, run under `timeout`): build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug`, `compareRoborazziDebug`; record: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app` (Gradle build/test/lint need D-001's grant)
- **Criteria the next Phase cites (quoted from DoD.md, short form; the Proof lines are in DoD.md by number):**
  - **DoD 1:** The first row of Edit tools reads Crop, Rotate, Adjust, Colour, Filters, Stickers, Text, Date, with Compare pinned at the end; RGB, HSL and Curves are not in it.
  - **DoD 2:** Tapping Colour changes the row to Back, RGB, HSL, Curves, with Compare still at the end, and tapping Back brings the first row back.
  - **DoD 3:** Tapping Colour while another tool's sheet is open closes that sheet.
  - **DoD 4:** Colour shows the "changed" dot while RGB, HSL or Curves holds a change, and inside the group each of the three shows its own dot.
  - **DoD 5:** Going Back to the first row from an open colour tool closes its sheet and keeps the change, as in Beautify.
  - **DoD 6:** With only a colour change made, leaving Edit still asks whether to discard it.
  - **DoD 7:** Compare still shows the original photo, and while it is held every entry on either row is disabled except Compare.
  - **DoD 8:** The row stays on the Colour group across a screen rotation.
  - **DoD 9:** Nothing about RGB, HSL or Curves changes: their sheets, sliders, curves, formulas, order in the pipeline, Undo/Redo and the saved photo.
  - **DoD 10:** Beautify's Face and Body rows look and work exactly as before.
  - **DoD 11:** Both rows are clear and readable in light and dark: "Colour" on one line under its icon, Back easy to find at the left of the group, the dots visible. (pictures 101-104)
  - **DoD 14-17, 19, R1, R2:** build/test/lint/screenshots green; only approved pictures changed; CLAUDE.md house rules; README/docs describe the new row; test edits logged; RGB/HSL/Curves leave the first row; pictures 83/84 show the Colour group.
- **Open issues touching the next Phase's files (quoted from ISSUES.md):**
  - "Run 12 97/98 Red "S-curve" pictures show only one middle point (the first tap did not add one); look at them and fix `drawRedSCurve` in `ToolScreenshotTest.kt` if wanted. Chips at 320 dp unchecked."
- **Reference topics the next Phase touches:** `reference/run13-facts.md`, `reference/run11-facts.md`, `reference/run12-facts.md`, `reference/run6-compose-testing-a.md`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
