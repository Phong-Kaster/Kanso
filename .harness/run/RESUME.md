# RESUME BLOCK

> Derived cache, never a source of truth. On disagreement with task files or git, this is wrong.

- **Stage:** build — Phase 2 done (T-002 complete); next Phase 3.
- **Next Phase:** Phase 3
  - T-003 (Fast, 5 slices, sequential; see `TASKS/T-003.md`, scope uses M/T/P abbreviations defined there). Includes carry-over from the Phase 2 review (HSL in `isNeutral`/Back-discard, stale preview comments, README layer notes). Iteration fetches `ic_invert_colors.xml` first; re-records row baselines afterwards (D-001 `## Baselines`).
  - T-002 left: `EditProcessor`/`FilterPreviewRenderer` apply HSL; `ToolViewModel` preview considers `hsl` (`previewSettings` drives it); README has an HSL row "Not wired". Tests appended to `evidence/test-edits.md`.
  - Fixed design: `PLAN.md` "Fixed design", "Sheet layout", "Slider track" - quote into Briefs.
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** (run from the repo root, under `timeout`) build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | pictures: `./gradlew :app:verifyRoborazziDebug` | record new/approved pictures: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app` | Worker compile: `powershell -NoProfile -File D:/Valve/Personal/Kanso/.harness/loop/bin/foreman-compile.ps1` (module `app`)
- **Criteria the next Phase cites (quoted from DoD.md):**
  - **DoD 1:** [machine] The Edit screen has nine tools, and the new one, named "HSL", sits right after RGB and before Filters, with an icon unlike Adjust's and RGB's.
  - **DoD 2:** [machine] HSL offers nine colour ranges in this order, 40° apart: Red 0°, Orange 40°, Light green 80°, Green 120°, Light blue 160°, Blue 200°, Navy blue 240°, Purple 280°, Pink 320°, each chosen by tapping a circle filled with its colour, with the chosen circle marked.
  - **DoD 10:** [machine] The first time HSL opens, Red and Hue are chosen; reopened, it shows the range and setting last chosen.
  - **DoD 11:** [machine] Choosing a range or a setting never changes the photo and adds nothing to Undo.
  - **DoD 12:** [machine] One drag of the HSL slider is one Undo step, and Redo brings it back.
  - **DoD 13:** [machine] The HSL tool shows its "changed" dot while any of its 27 values is away from 0, and only then; no other tool's dot lights up because of HSL.
  - **DoD 14:** [machine] Compare shows the photo without the HSL change, Back after an HSL change asks "Discard your changes?", and the HSL sheet has no reset button.
  - **DoD 17:** [machine] Dragging an HSL slider updates the photo on screen while the finger moves, even when no filter is chosen.
  - **DoD 21:** [machine] The sheet stays 136 dp tall with all three rows fully visible, the photo does not change size when HSL opens, and every circle, chip and the slider is at least 48 dp to touch.
  - **DoD 22:** [machine] The mark on the chosen circle stands out against all nine colours, in light and in dark.
  - **DoD 30:** [machine] The app builds, opens, and the Edit screen with the HSL sheet open does not crash; tests, lint and pictures pass.
  - **DoD 31:** [machine] README describes HSL as the code is: its row in "Editing features" with its real state, and its pipeline traced end to end in "How each editing feature works".
  - **DoD 35:** [machine] The code follows the owner's standing rules: every new composable has a light and a dark preview, one class per file, the screen-folder layout, and every new class and function explained in plain English.
- **Open issues touching the next Phase's files:** Phase 2 review carry-overs, in T-003.md.
- **Reference topics the next Phase touches:** `.harness/knowledge/reference/run11-facts.md`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` (effort medium) | capable: `opus` (effort medium)
