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

## D-001 - Approve the Definition of Done for Run 5, and the screenshot re-recording it asks for

- **Status:** answered (Iteration 2, 2026-10-09)
- **Type:** DoD approval
- **Iteration:** 1
- **Timestamp:** 2026-10-09
- **Blocks tasks:** T-001, T-002, T-003

### Question

Approve `.harness/run/DoD.md` (17 criteria and 3 removals), editing it first if you wish. Approval covers each criterion's Verification Class as well as its wording. The PRD also asks to be asked, in the DoD, about re-recording the Roborazzi baselines: that approval is the list below. Reply `## D-001` in `.harness/run/DECISIONS.md` with "approved" (or your edits), and, **in the same file, a separate `## Baselines` heading** listing the pictures the engine may re-record, exactly like this (this is Foreman's own rule: it only keeps a re-recorded picture whose path is listed under that heading, and it stops the run if any other route changes one):

```
## Baselines
- app/src/test/screenshots/1_home_light.png
- app/src/test/screenshots/2_home_dark.png
- app/src/test/screenshots/3_home_custom_colour.png
- app/src/test/screenshots/4_settings_light.png
- app/src/test/screenshots/5_settings_dark.png
- app/src/test/screenshots/6_settings_custom_colour.png
- app/src/test/screenshots/10_gallery_light.png
- app/src/test/screenshots/11_gallery_dark.png
- app/src/test/screenshots/12_gallery_refused_light.png
- app/src/test/screenshots/13_gallery_refused_dark.png
- app/src/test/screenshots/14_camera_refused_light.png
- app/src/test/screenshots/15_camera_refused_dark.png
- app/src/test/screenshots/16_tool_photo_light.png
- app/src/test/screenshots/17_tool_photo_dark.png
- app/src/test/screenshots/18_saved_light.png
- app/src/test/screenshots/19_saved_dark.png
```

Listing a picture does not mean it will change; it means the engine **may** re-record it, and only after a library move (never in the toolchain-only first step), only with its `_compare.png` kept in `.harness/run/evidence/baseline-diffs/` and a README line saying which library change moved it. Leave a picture out to forbid it. Without the list, a picture that a library moves stays a failing check, DoD criterion 6 stays unmet, and the run ends `DONE_PARTIAL`.

### Context

The PRD moves the toolchain to Android Gradle Plugin 9 and the current Compose libraries (Gradle 9.6+, compileSdk 37, Compose BOM 2026.09.00, Navigation 3 1.2.0, lifecycle-viewmodel-navigation3 2.11.0, CameraX at its newest stable, Material3 1.5.0-beta01), changing nothing a person using the app means to see. Three phases: (1) Android Gradle Plugin 9.4.1 alone, with no picture allowed to move; (2) Compose 1.12.1, Navigation 3 and lifecycle; (3) Material3 1.5.0-beta01 and a test showing the Expressive components can be used.

Two criteria need a person, so in Autonomous mode the run can end at best as `DONE_PARTIAL` until you sign them: **3** (the app installs and works on your Galaxy A13, which also checks the 32-bit build) and **7** (look at each screen picture that changed: reference, marked difference, new; if nothing moved, a one-line "nothing moved"). The comparison images live under `.harness/run/evidence/baseline-diffs/` and the unsigned item in `ISSUES.md` names the commit that holds them, so you can open them before you merge.

Where this DoD departs from, or reads, the PRD (each is an assumption in `ASSUMPTIONS.md`, A-001 to A-006; answer here to reverse one):

- **The Compose BOM and Material3 1.5.0: a real choice, please read criterion 12** (A-001). The PRD asks for "a Compose BOM that resolves Material3 1.5.0 (beta01 or newer)". The stable BOMs (2026.05.00 to 2026.09.00, the newest) all map Material3 to 1.4.0. Only the **beta** BOM `androidx.compose:compose-bom-beta:2026.10.00` maps it to 1.5.0-beta01, and that BOM also makes *every* Compose library 1.13.0-beta01 (UI, foundation, animation, runtime, UI test). The DoD as written takes the stable BOM (Compose 1.12.1) **plus** Material3 1.5.0-beta01 pinned in the app above it, so only Material3 is a pre-release (Material3 1.5.0-beta01 itself needs only Compose 1.12.0). That meets the PRD's outcome but not its literal "a BOM that resolves it". To use the beta BOM instead, answer "use the beta BOM" here; criterion 12 and the plan are then re-cut (a Tier-3 amendment) before any task starts, and more pictures are likely to move.
- **Kotlin leaves its recorded pin** (A-002). PRD item 1 moves Kotlin "as far as AGP 9 requires"; PRD item 3 removes every pin `PROJECT.md` recorded, and Kotlin 2.3.20 is one. The DoD reads both as: **Kotlin goes to the newest stable (2.4.21) in Phase 2** unless the build refuses it (then the older version stays, with the error written in `PROJECT.md`). **Gradle** goes to the floor AGP 9.4 names (9.6.0, or the number AGP's error gives). KSP is not used, so it does not move.
- **Only the app moves to compileSdk 37** (A-003); the engine modules keep their own compile-time Compose versions and compileSdk (35, 36) unless the build forces a change (DoD questions Q1, Q3).
- **Libraries outside the PRD's list stay** (A-004; DoD Q2): `activity-compose`, `lifecycle-runtime-ktx`, `core-ktx`, `datastore`.
- **"Newest stable" excludes alphas and betas** (A-005): Navigation 3 1.2.0, lifecycle-viewmodel-navigation3 2.11.0, CameraX 1.6.2 (already used), the exception being Material3, where the PRD itself names 1.5.0-beta01.
- **Added to the PRD's list, for you to cut if unwanted:** a release build of the app and of every module (criteria 11 and 16: a release build can break while the debug one is green, and OpenCV's release variant carries the prefab and publishing settings); a debug install on the Galaxy A13 (criterion 3: the PRD names the three ABIs and the A13 as constraints); and AGP 9 on its defaults with no opt-out flag (criterion 11: the PRD's own example is AGP 9's built-in Kotlin). If a module cannot be moved without an opt-out flag, criterion 11 stays unmet and is reported, rather than quietly kept on a flag.

### Options Considered

1. Approve as written, with the `## Baselines` list - consequences: three phases and a Verifier pass; Material3 1.5.0-beta01 on the stable Compose BOM; the run ends `DONE_PARTIAL` until you sign criteria 3 and 7; pictures a library moves can be re-recorded and kept with their comparison images.
2. Approve with the beta BOM instead ("use the beta BOM") - consequences: the PRD's wording is met literally; all of Compose is 1.13.0-beta01; the plan is re-cut by a Tier-3 amendment before any task starts.
3. Approve with other edits (for example, drop the release builds, or the device criterion) - consequences: the plan is re-cut by a Tier-3 amendment before any task starts.
4. Approve without the `## Baselines` list - consequences: the first library move that changes a picture leaves a failing check, criterion 6 stays unmet, the run ends `DONE_PARTIAL` with the comparison images as evidence.

### Engine Recommendation

Option 1.

### Proposed Capabilities (if any)

None (Autonomous mode runs under the Deny List; the baseline recorder is the existing `foreman-record-baselines.ps1` wrapper, which already honours the `## Baselines` list).

### Decision

Answered in `.harness/run/DECISIONS.md` (`## D-001`, with a `## Baselines` list of 16 pictures): approved as written, Option 1. Applied in Iteration 2; see `AMENDMENTS.md`.

---
