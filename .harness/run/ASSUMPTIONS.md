# ASSUMPTIONS

> Autonomous mode only (ENGINE.md §14.1, ADR-027). Every decision the engine made alone, where a
> Collaborative run would have stopped to ask. The Runtime renders this file into the This run tab of
> `FOREMAN.html` after every iteration, so each entry must make sense to someone who has read nothing else.
>
> To overturn one: answer its id in `.harness/run/DECISIONS.md` and re-run. A human answer always
> outranks an assumption — the engine reverts what depended on it. Or run the revert command below
> yourself, on the Loop Branch, after the run has stopped.

---

## A-001 - Material3 1.5.0-beta01 is pinned in the app, above the stable Compose BOM

- **Tier:** Missing information (the PRD's wording "a Compose BOM that resolves Material3 1.5.0" can be met only by a beta BOM, which has a price; the choice is put to the human in D-001)
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 1 (the Definition of Done); filled in with a commit SHA by Iteration 2
- **Revert:** `git revert <sha>` (of the Phase 3 checkpoint, once it exists)
- **Assumed DoD criteria:** none (DoD 12 states the delivered outcome in these words, and you approve it at the gate)

### Question

The PRD asks for "a Compose BOM that resolves Material3 **1.5.0** (beta01 or newer)". The BOM POMs read on 2026-10-09 say the stable BOMs 2026.05.00, 2026.08.00 and 2026.09.00 (the newest) all map `androidx.compose.material3` to **1.4.0**. Only the **beta** BOM `androidx.compose:compose-bom-beta:2026.10.00` (and `compose-bom-alpha:2026.10.00`) maps it, to 1.5.0-beta01, and it also maps Compose UI, foundation, animation and runtime to **1.13.0-beta01**. Material3 1.5.0-beta01's own POM needs Compose UI and foundation only 1.12.0. How should the run get Material3 1.5.0-beta01 into the app?

### Options Considered

1. Newest stable BOM (2026.09.00, Compose 1.12.1) plus an explicit Material3 1.5.0-beta01 in the app - consequences: only Material3 is a pre-release; Gradle picks the highest requested Material3, so the app resolves 1.5.0-beta01 on Compose 1.12.1; one catalog alias for the same coordinate, no new library; the app is the only module on 1.5.0, so the engine modules' compileSdk 35/36 are not asked to consume an artifact that needs compileSdk 37; fewer moved pictures to explain. The PRD's "a BOM that resolves it" is met in outcome, not in wording.
2. `compose-bom-beta` 2026.10.00 - consequences: meets the PRD's wording literally and needs no override line; but every Compose library (UI, foundation, animation, runtime, UI test) becomes 1.13.0-beta01, more pictures and behaviour can move, the BOM coordinate changes (`compose-bom` to `compose-bom-beta`, which the "no new library" rule would have to be read as allowing), and the test toolchain runs on pre-release Compose.
3. Wait for a stable BOM that maps Material3 1.5.x - consequences: the run cannot finish; the next run (the Expressive look) is blocked.

### Taken, and why

Option 1. It keeps the pre-release surface to the one library the PRD needs, adds no coordinate, and is a one-line change to undo or to replace with option 2. D-001 puts option 2 to the human as a real alternative; the answer there outranks this assumption.

---

## A-002 - Gradle moves to AGP 9.4.1's floor; Kotlin leaves its recorded pin for the newest stable, unless the build refuses

- **Tier:** Missing information (two sentences of the PRD pull in different directions)
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 1 (plan); filled in by Iteration 2
- **Revert:** `git revert <sha>` (of the Phase 1 checkpoint for Gradle, of the Phase 2 checkpoint for Kotlin)
- **Assumed DoD criteria:** none (DoD 9, R3)

### Question

PRD item 1: "The Gradle wrapper and the Kotlin and KSP versions move as far as AGP 9 requires." PRD item 3: "each lower pin Runs 1-3 recorded in `.harness/knowledge/PROJECT.md` is removed, with what replaced it". PROJECT.md records Kotlin 2.3.20 among the pins forced by AGP 8.13. Newest stable Gradle is 9.8.1 and newest stable Kotlin 2.4.21; AGP 9.4's notes give a floor of Gradle 9.6.0 and nothing names a Kotlin floor. KSP is not used anywhere in the build, so it does not move.

### Options Considered

1. Gradle at the floor AGP names (9.6.0, or the number its error gives); Kotlin to the newest stable (2.4.21) in Phase 2, with the library moves, and held at an older release only when the build refuses it, with the error recorded - consequences: item 1's "as far as AGP 9 requires" is honoured for Gradle (the tool that must match AGP); item 3's pin removal is honoured for Kotlin; a Kotlin failure is attributed to its own step.
2. Both at the minimum (Gradle 9.6.0, Kotlin stays 2.3.20) - consequences: smallest change, but leaves a recorded pin in place, against item 3.
3. Newest of everything (Gradle 9.8.1, Kotlin 2.4.21) - consequences: a newer Gradle than AGP 9.4.1 was tested against may break a plugin (Roborazzi, Compose compiler); a failure is harder to attribute.

### Taken, and why

Option 1. DoD 9 asks PROJECT.md to record each version and why it is not older or newer. Because removing `kotlin-android` also removes the only line that put the Kotlin plugin on the build's classpath, DoD 9 also checks that the Kotlin that compiles is the catalog's.

---

## A-003 - Only the app moves to compileSdk 37; the engine modules keep their own compile-time versions

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 1 (plan); filled in by Iteration 2
- **Revert:** `git revert <sha>` (of the Phase 1 checkpoint)
- **Assumed DoD criteria:** none (DoD 10 and Questions Q1, Q3)

### Question

PRD item 2: "compileSdk 37." and item 4: the engine's modules build under AGP 9, "their source code is not changed", build scripts only "as far as the build requires". Does compileSdk 37 apply to the app only, or to every module, and do the engine modules' own older Compose pins (`composeUi` 1.8.2, `compose-material3` 1.3.2, `composeMaterialIcons` 1.7.8) move?

### Options Considered

1. App only; engine modules unchanged beyond what AGP 9 forces - consequences: engine modules stay at compileSdk 35 (36 for `localml`) and their older compile-time Compose versions; the app resolves the newer versions at run time. A binary break would show as a test or baseline failure (PLAN, risks) and is then fixed with a build-script line in that module.
2. Every module to compileSdk 37 and to the BOM - consequences: touches nine build scripts beyond what the build requires, against the PRD's "only as far as the build requires", and moves the engine's compile-time versions silently.

### Taken, and why

Option 1: smallest change that meets the PRD. Left for the human as Questions Q1 and Q3 in the DoD.

---

## A-004 - Libraries outside the PRD's list are left where they are

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 1 (plan); filled in by Iteration 2
- **Revert:** not needed (nothing changes)
- **Assumed DoD criteria:** none (DoD Q2)

### Question

The PRD names the Compose BOM, Navigation 3 with `lifecycle-viewmodel-navigation3`, CameraX and the test toolchain. Older AndroidX libraries exist too (`activity-compose` 1.10.1, stable 1.13.0; `lifecycle-runtime-ktx` 2.9.1; `core-ktx` 1.16.0; `datastore` 1.1.7). Move them?

### Options Considered

1. Leave them, move one only if the build forces it - consequences: honours "No new library beyond newer versions of the ones already there" and keeps pixel and behaviour changes attributable.
2. Move every AndroidX library to its newest stable - consequences: unrequested scope, more behaviour risk.

### Taken, and why

Option 1. Listed in the DoD (Q2) so you can ask for them in a later run.

---

## A-005 - "Newest stable" means no alpha, beta or release candidate

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 1 (the Definition of Done); filled in by Iteration 2
- **Revert:** not needed
- **Assumed DoD criteria:** none (DoD 8, 12, 13)

### Question

Maven metadata also lists newer pre-releases: Navigation 3 1.3.0-alpha02, lifecycle-viewmodel-navigation3 2.12.0-alpha04, CameraX 1.7.0-alpha03, AGP 9.5.0-alpha09. Which does "newest stable" mean?

### Taken, and why

A version whose name carries none of `alpha`, `beta`, `rc`, `dev`: Navigation 3 1.2.0, lifecycle-viewmodel-navigation3 2.11.0, CameraX 1.6.2 (already in use), AGP 9.4.1, Compose BOM 2026.09.00. The one deliberate exception is Material3, where the PRD itself names 1.5.0-beta01 because no stable 1.5 exists. Options considered: take the alphas (rejected: the PRD says stable).

---

## A-006 - Baselines are re-recorded only with the human's listed approval, and never in Phase 1

- **Tier:** Missing information
- **Iteration:** 1
- **First dependent checkpoint:** Iteration 1 (the Definition of Done); filled in by Iteration 2
- **Revert:** `git revert <sha>` (of the checkpoint that re-recorded)
- **Assumed DoD criteria:** none (DoD 5, 6, 7 and "Approval requested")

### Question

The PRD: "Ask me in the DoD to approve re-recording the existing Roborazzi baselines." Foreman enforces the approval as a list of file paths under a `## Baselines` heading in `.harness/run/DECISIONS.md`. What if that list is missing or shorter than the pictures that move?

### Options Considered

1. Re-record only what is listed; anything else that moved stays a failing comparison, kept as evidence, and criterion 6 stays unmet - consequences: the run ends `DONE_PARTIAL` instead of pretending a green; nothing moves without you.
2. Re-record everything that moved and ask afterwards - consequences: impossible by design (the wrapper and the Runtime refuse it) and against the PRD.

### Taken, and why

Option 1. D-001 carries the exact text to paste so one answer covers both the DoD and the baselines.
