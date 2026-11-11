# DECISION QUEUE

> Questions the engine could not answer within its authority. Queueing one does **not** stop the run:
> the engine marks the tasks that entry blocks and keeps working on everything else.
>
> **Answer in `.harness/run/DECISIONS.md`, never here** - copy an entry's id as a heading there and
> write your decision underneath it, then re-run.

---

## D-001 - Approve the Definition of Done for Run 14 (Curves like Meitu's), the spline, and the build grants

- **Status:** answered (owner approved 2026-11-11; natural cubic; Baselines listed; Gradle grant; ic_delete fetch)
- **Type:** DoD approval + Capability grant
- **Iteration:** 1 (bootstrap)
- **Timestamp:** 2026-11-11
- **Blocks tasks:** T-001, T-002, T-003, T-004

### Question

1. Approve `.harness/run/DoD.md` (26 criteria, removals R1–R10) — edit freely before approving.
2. Choose the spline (the PRD asks to see the comparison — below). The DoD is written for **natural cubic**.
3. Approve the screenshot changes by listing them under a `## Baselines` heading in `DECISIONS.md`.
4. Approve the Gradle grants below.
5. The trash icon.

### Context — the spline comparison (PRD "Open for the DoD")

I could not open the Meitu screenshots (outside the repository), so this works from your table.
Values of the line (0 = black, 1 = white) at points across the graph:

**One point at (0.5, 0.75):**

| x | 0.125 | 0.25 | 0.375 | 0.5 | 0.625 | 0.75 | 0.875 |
|---|---|---|---|---|---|---|---|
| Natural cubic | 0.217 | 0.422 | 0.604 | 0.75 | 0.854 | 0.922 | 0.967 |
| Catmull-Rom | 0.199 | 0.406 | 0.598 | 0.75 | 0.848 | 0.906 | 0.949 |
| Today (monotone) | 0.199 | 0.406 | 0.598 | 0.75 | 0.848 | 0.906 | 0.949 |

**Two points (0.25, 0.15) and (0.75, 0.85), an S-curve:** all three agree within 0.006 — this shape
cannot tell them apart.

**One point pulled hard, (0.25, 0.75):**

| x | 0.125 | 0.25 | 0.375 | 0.5 | 0.625 | 0.75 | 0.875 |
|---|---|---|---|---|---|---|---|
| Natural cubic (before clip) | 0.406 | 0.75 | 0.983 | 1.111 | 1.156 | 1.139 | 1.080 |
| Catmull-Rom | 0.438 | 0.75 | 0.850 | 0.907 | 0.938 | 0.954 | 0.970 |

- **Natural cubic** bends the whole line further with one point, and a strongly pulled point overshoots
  and is clipped into a **flat white top** from about x = 0.39 — the flat top you saw in Meitu.
- **Catmull-Rom** with one point is identical to today's curve, so it would not change how one point
  bends, and it barely overshoots.

### Options Considered

1. Natural cubic - consequences: matches the flat top/bottom you observed and Photoshop's feel; moving one point shifts every segment.
2. Catmull-Rom - consequences: local (a point moves only its neighbours' segments), but one point bends exactly like today.

### Engine Recommendation

1. Approve the DoD as written; it fixes these open PRD items, each editable:
   - spline: **natural cubic** (criterion 9);
   - "on the line": within **24 dp** of the drawn line, the same reach as grabbing a point; a point wins
     when both are in reach; a tap on the flat part beyond an end point adds a new end point (1, 3);
   - selected point: filled with the graph background inside a thicker ring of the **line's colour**;
     others filled with the line's colour inside a background-coloured ring (18);
   - RGB line: the theme's text colour (`onSurface`); R/G/B lines: the tints the RGB sheet already uses (17);
   - entries labelled RGB / R / G / B, still read aloud as RGB / Red / Green / Blue (16);
   - selection clears on channel switch, Undo, Redo and Reset (5);
   - pictures: 95–98 re-recorded, 99/100 deleted, 105–108 added (20).
2. Write in `DECISIONS.md`, so the record script lets the engine replace and delete exactly these:

   ```
   ## Baselines
   - app/src/test/screenshots/95_edit_curves_master_straight_light.png
   - app/src/test/screenshots/96_edit_curves_master_straight_dark.png
   - app/src/test/screenshots/97_edit_curves_red_s_curve_light.png
   - app/src/test/screenshots/98_edit_curves_red_s_curve_dark.png
   - app/src/test/screenshots/99_edit_curves_lightness_s_curve_light.png
   - app/src/test/screenshots/100_edit_curves_lightness_s_curve_dark.png
   ```
   (105–108 are new and need no listing.)
3. Trash icon: no delete icon exists in the app. I will add `res/drawable/ic_delete.xml`, Material
   Symbols Rounded "delete" (24 px, weight 400), fetched from github.com/google/material-design-icons as
   the earlier icons were. If that fetch is not allowed, drop the file in yourself before Phase 3, or say
   which existing icon to use.
4. Approve the grants below (goal lifetime, this run only). Without them no Phase can build or test.

### Proposed Capabilities (if any)

```json
{
  "intent": "Build, unit-test and lint the app after every Phase and at the Verifier gate (DoD 23)",
  "command": "./gradlew :app:assembleDebug / :app:testDebugUnitTest / :app:lintDebug, always from the repository root under timeout",
  "scope": "this repository's Gradle build outputs only",
  "lifetime": "goal",
  "allow": [
    "Bash(*gradlew* :app:assembleDebug*)",
    "Bash(*gradlew* :app:testDebugUnitTest*)",
    "Bash(*gradlew* :app:lintDebug*)"
  ],
  "target_ledger": ".harness/run/capabilities.json"
}
```

### Decision

<!-- Answered in `.harness/run/DECISIONS.md`, under a heading `## D-001` - never here. -->
