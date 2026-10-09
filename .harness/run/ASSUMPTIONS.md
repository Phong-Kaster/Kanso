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
- **First dependent checkpoint:** `a1e76b5` (Iteration 1: the Definition of Done and the plan; `git revert a1e76b5` would undo the whole plan, the Revert line below names the checkpoint that carries the effect)
- **Revert:** `git revert c0871f31036e598238fcfec1aa1f051a33077e4b` (the Phase 3 checkpoint, which carries the effect; it also reverts that Iteration's notes under `.harness/`). For Material3 alone, by hand: point `app/build.gradle.kts` back at `implementation(libs.compose.material3)` (the app then takes the BOM's Material3 1.4.0, as after Phase 2), delete the `compose-material3-app` version and alias from `gradle/libs.versions.toml`, and delete `app/src/test/java/com/phongkaster/kanso/ExpressiveApiTest.kt`, whose components need 1.5.0
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
- **First dependent checkpoint:** `a1e76b5` (Iteration 1: the Definition of Done and the plan; `git revert a1e76b5` would undo the whole plan, the Revert line below names the checkpoint that carries the effect)
- **Revert:** for Kotlin alone, set `kotlin = "2.3.20"` in `gradle/libs.versions.toml` (Phase 1 built and tested on it under AGP 9.4.1; the move to 2.4.21 is the Phase 2 checkpoint `58d6efb`, which changed only that catalog line among its four); the Gradle floor goes only with AGP itself, so `git revert d816bde` is sound for it only together with the later Phases, which need compileSdk 37 and Navigation 3 1.2.0
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
- **First dependent checkpoint:** `a1e76b5` (Iteration 1: the Definition of Done and the plan; `git revert a1e76b5` would undo the whole plan, the Revert line below names the checkpoint that carries the effect)
- **Revert:** to undo only this, raise `compileSdk` to 37 in the `core/*` and `opencv` build scripts (option 2 below); `git revert d816bde` would put the app back on compileSdk 36, which Navigation 3 1.2.0 (Phase 2) refuses, so it is sound only together with the later Phases
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
- **First dependent checkpoint:** `a1e76b5` (Iteration 1: the Definition of Done and the plan; `git revert a1e76b5` would undo the whole plan, the Revert line below names the checkpoint that carries the effect)
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
- **First dependent checkpoint:** `a1e76b5` (Iteration 1: the Definition of Done and the plan; `git revert a1e76b5` would undo the whole plan, the Revert line below names the checkpoint that carries the effect)
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
- **First dependent checkpoint:** `a1e76b5` (Iteration 1: the Definition of Done and the plan; `git revert a1e76b5` would undo the whole plan, the Revert line below names the checkpoint that carries the effect)
- **Revert:** not needed: no baseline was re-recorded in any Phase (`git diff 76ecb97 c0871f3 -- app/src/test/screenshots` is empty; checked again by the Verifier, Iteration 5)
- **Assumed DoD criteria:** none (DoD 5, 6, 7 and "Approval requested")

### Question

The PRD: "Ask me in the DoD to approve re-recording the existing Roborazzi baselines." Foreman enforces the approval as a list of file paths under a `## Baselines` heading in `.harness/run/DECISIONS.md`. What if that list is missing or shorter than the pictures that move?

### Options Considered

1. Re-record only what is listed; anything else that moved stays a failing comparison, kept as evidence, and criterion 6 stays unmet - consequences: the run ends `DONE_PARTIAL` instead of pretending a green; nothing moves without you.
2. Re-record everything that moved and ask afterwards - consequences: impossible by design (the wrapper and the Runtime refuse it) and against the PRD.

### Taken, and why

Option 1. D-001 carries the exact text to paste so one answer covers both the DoD and the baselines.

---

## A-007 - The four empty wrapper modules get their own namespaces, although DoD 11 names a shared namespace as "unmet, reported"

- **Tier:** 3 (it touches how the approved DoD 11 reads; taken as the reading closest to the PRD's literal text, item 4: adapt the build scripts "only as far as the build requires", source code not changed)
- **Iteration:** 2
- **First dependent checkpoint:** `d816bde` (the Phase 1 checkpoint, `d816bde17db519bfc545f268ef75966368ebaed1`)
- **Revert:** `git revert d816bde` undoes the whole toolchain move; to undo only this, put the four `namespace` lines of `core/editimage`, `overlaybrush`, `imageprocessor` and `localml` back to the AAR's namespace, which makes AGP 9 refuse the app's manifest merge again
- **Assumed DoD criteria:** 11 (its sentence "a change AGP 9 forces on an engine source file (for example a namespace shared by a prebuilt library and its module) makes this criterion unmet and is reported"); listed as **assumed** in `STATE.md`

### Question

With AGP 9.4.1 the app's manifest merge fails: `Namespace 'com.photo.editor.editimage' is used in multiple modules and/or libraries: :core:editimage, local:editimage:` (and the same for `overlaybrush`, `imageprocessor` and `localml`). DoD 11 expects this case to make criterion 11 "unmet and reported", because it assumed fixing it would change an engine source file. What was found: these four modules have **no `src/` folder at all** (only a build script and a proguard file); each is an empty wrapper of its prebuilt AAR in `libs/` (`api(group = "local", name = ..., ext = "aar")`), and the namespace is a line in the wrapper's build script. Nothing in `app/src`, `core/*/src` or `opencv/java` refers to the wrappers' `R` or `BuildConfig`. Without a fix the app cannot be built, so no other criterion can be proved.

### Options Considered

1. Give each wrapper its own namespace in its build script (`com.phongkaster.kanso.engine.<module>`) - consequences: no engine source file changes (DoD 11's own proof command, `git diff --exit-code --stat <base> HEAD -- 'core/*/src' opencv/java opencv/native opencv/libcxx_helper libs 'core/*/proguard-rules.pro'`, exits 0); the AARs keep their namespaces, classes and resources; the debug APK differs from the base APK only in the compile-SDK stamps of the binary manifest and AGP's metadata file (`evidence/phase1-apk-vs-base.txt`); the only thing that changes is the empty `R` class of each wrapper.
2. Leave it and report criterion 11 unmet - consequences: the app does not build under AGP 9; T-001 fails its attempts and is abandoned, T-002 and T-003 become unreachable, nothing of the PRD is delivered, and the report would say "AGP 9 cannot build this engine" without the way out.
3. `android.uniquePackageNames=false` in `gradle.properties` - consequences: an opt-out flag that holds an AGP 8 behaviour, forbidden by DoD 11 and by D-001 ("AGP 9 on its defaults").

### Taken, and why

Option 1. It is a build-script adaptation the PRD allows ("only as far as the build requires"), it leaves every proof command of criterion 11 passing, and it is one `git revert` away. The owner may still prefer to read the DoD's sentence strictly; this assumption puts that choice in front of them, and the run cannot end `DONE` while it stands. The Reviewer flagged the same point (Phase 1 review).

---

## A-008 - `core/localml` depends on `litert-support-api` directly instead of the empty `litert-support`

- **Tier:** 2
- **Iteration:** 2
- **First dependent checkpoint:** `d816bde` (the Phase 1 checkpoint, `d816bde17db519bfc545f268ef75966368ebaed1`)
- **Revert:** `git revert d816bde`; to undo only this, restore `implementation(libs.litert.support)` in `core/localml/build.gradle.kts`, which makes AGP 9 refuse the manifest merge again
- **Assumed DoD criteria:** none (DoD 14's coordinate-set check still passes; its sentence "No library was added" is read as "no library that was not already resolved")

### Question

AGP 9.4.1 also refuses the pair `com.google.ai.edge.litert:litert-support:1.3.0` and `litert-support-api:1.3.0`: both declare the namespace `org.tensorflow.lite.support`. `litert-support` is an empty forwarding artifact (empty `classes.jar`, no resources, empty `R.txt`; its POM only depends on `litert-support-api:[1.3.0]` and `litert:1.3.0`); 1.4.2 has the same two namespaces, so a version move does not help. How does `core/localml` keep the LiteRT support classes without the clash?

### Options Considered

1. Declare `litert-support-api` (same version, `libs.versions.litertSupport`) directly with named arguments and drop the empty artifact - consequences: the resolved classpath is the same minus an empty artifact; the catalog keeps its `litert-support` alias, now unused, so DoD 14's coordinate-set check passes textually; the declaration is not a catalog entry (its colon-string grep cannot see it), so `PROJECT.md` and this entry say so.
2. Add a catalog alias for `litert-support-api` and delete or retarget the `litert-support` alias - consequences: cleaner, but the catalog's coordinate set differs from the base, which DoD 14 forbids.
3. The opt-out flag of A-007 option 3 - consequences: forbidden.

### Taken, and why

Option 1: it changes nothing that runs and breaks no check. The cleaner option 2 is for a later run if the owner wants the catalog changed.
