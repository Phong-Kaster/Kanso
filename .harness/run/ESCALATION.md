# DECISION QUEUE

> Questions the engine could not answer within its authority. Queueing one does **not** stop the run:
> the engine marks the tasks that entry blocks and keeps working on everything else. The Runtime
> stops only when no executable task remains.
>
> This file is the engine's own log. **Answer in `.harness/run/DECISIONS.md`, never here** - copy an
> entry's id as a heading there and write your decision underneath it, then re-run.

---

## D-001 - Approve the Curves Definition of Done, its scope, and the standing build commands

- **Status:** answered (D-001, 2026-11-06: approved, option 1, capability approved)
- **Type:** DoD approval
- **Iteration:** 1 (bootstrap)
- **Timestamp:** 2026-11-06
- **Blocks tasks:** T-001, T-002, T-003, T-004, T-005

### Question

1. Approve `.harness/run/DoD.md` (edit it freely first).
2. Which scope? Keep or delete the "Optional: hue curves" section (criteria 23-24).
3. Approve the proposed standing build/test/lint capability below.
4. Optionally, start a `## Baselines` list (see Context, point C).

### Context

The PRD is one line: "plan, write code and test on galaxy a13 to build curve feature which relate with
RGB, HSL". The engine read it as **a new Curves tool on the Edit screen, right after HSL**, with one tone
curve each for RGB (all colours), Red, Green, Blue and Lightness (the L of HSL).

- **A. Where curves run.** A curve cannot be expressed as the colour matrix the GPU draws over the
  photo, so it runs on the CPU like HSL: after the Filter and HSL, before Adjust and RGB. The live
  preview and the saved photo then match. This will be written down as ADR-009.
- **B. How you edit a curve.** Tap an empty spot to add a point. Drag a point to move it. Long-press a
  middle point to remove it. You get at most 10 points per curve. Reset straightens the curve that is
  on screen. Points can't be edited with TalkBack; the chips, Reset and the graph's description are
  spoken.
- **C. Pictures.** Adding a tenth tool can change existing screenshots that show the tool row beside
  HSL (probably 83 and 84). The engine cannot overwrite an existing picture. When `verifyRoborazziDebug`
  goes red, it will list the exact paths in a new decision, and you add them under `## Baselines`.
- **D. The A13.** The Galaxy A13 is attached (SM-A135F). At the end, the Verifier drives Curves on it
  through the device wrapper. It installs over the existing app and never clears the app's data
  (criterion 16). Smoothness is yours to judge (criterion 17).
- **E. Icon.** No curve icon exists. A Wiring Worker will write `ic_show_chart.xml` with the Material
  "show chart" path, white fill and no tint, like the other icons. If you would rather supply the
  Material Symbols Rounded file yourself, say so and drop it in `app/src/main/res/drawable/`.
- **F. Build commands.** No ledger currently grants assemble, test or lint, and every run has had to ask
  for them. The proposal makes them permanent.

### Options Considered

1. **RGB, Red, Green, Blue and Lightness curves** (delete criteria 23-24). This uses one editor and one
   formula, covers "RGB" and the L of "HSL", and needs 4 tasks.
2. **Option 1 plus Hue → Saturation and Hue → Lightness curves** (keep 23-24). This is closer to
   Lightroom's HSL curves. It needs a second kind of graph (a hue axis that wraps around, with a flat
   line that means "no change"), so it adds T-005 and two pictures.
3. **Replace the RGB and HSL sliders with curves.** The engine does not recommend this: it would remove
   working tools that you signed off on 2026-11-06.

### Engine Recommendation

Option 1. Delete the optional section and approve the rest. Hue curves can be a later run, once the
Curves editor exists.

### Proposed Capabilities (if any)

```json
{
  "intent": "Build, unit-test (with Robolectric and Roborazzi verify via the baseline ledger) and lint the app every Iteration, the verified toolchain recorded in knowledge/reference/build-and-environment.md. Asked for again at every run so far; permanent so it stops being re-asked.",
  "command": "./gradlew :app:assembleDebug | ./gradlew :app:testDebugUnitTest [--tests <filter>] | ./gradlew :app:lintDebug, optionally prefixed by 'cd /d/Valve/Personal/Kanso &&' and 'timeout <n>'",
  "scope": "this repository; reads sources, writes only Gradle build outputs under build/ and app/build/",
  "lifetime": "permanent",
  "allow": [
    "Bash(*gradlew :app:assembleDebug*)",
    "Bash(*gradlew :app:testDebugUnitTest*)",
    "Bash(*gradlew :app:lintDebug*)"
  ],
  "target_ledger": ".harness/knowledge/capabilities.json"
}
```

### Decision

<!-- Answer in `.harness/run/DECISIONS.md` under `## D-001`. For example: "Approved, option 1,
     capability approved." -->

---

## D-002 - Re-record pictures 63 and 64 (Autonomous: informational, nothing blocked)

- **Status:** answered (approved 2026-11-06; re-recorded in Iteration 3)
- **Type:** baseline approval
- **Iteration:** 2
- **Blocks tasks:** none (work continues)

### Question
Adding the Curves entry moves the tool row in `app/src/test/screenshots/63_edit_filters_1977_light.png` and `64_edit_filters_1977_dark.png` (the row scrolls so the selected Filters stays in view). The engine may not overwrite an existing baseline. To approve re-recording, list both under a `## Baselines` heading in `.harness/run/DECISIONS.md`.

### Engine Recommendation
Approve both; the change is the tool row only.

---

## D-003 - Re-record pictures 95 and 96 (Autonomous: informational, nothing blocked)

- **Status:** answered (approved by the owner 2026-11-06; re-recorded in Iteration 4)
- **Type:** baseline approval
- **Iteration:** 3
- **Blocks tasks:** none (work continues)

### Question
The Curves chips (RGB, Red, Green, Blue) now sit in the sheet's top row, so `app/src/test/screenshots/95_edit_curves_master_straight_light.png` and `96_edit_curves_master_straight_dark.png` no longer match (`verifyRoborazziDebug` fails on them, evidence `p3-verify.txt`). The engine may not overwrite a baseline. To approve, list both under `## Baselines` in `.harness/run/DECISIONS.md`.

### Engine Recommendation
Approve both; the change is the chips row only.

### Decision
