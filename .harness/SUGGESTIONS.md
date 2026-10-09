# SUGGESTION BOX

> Lessons this repository's runs learned that are true beyond it. Proposals only; the engine never acts on one.

---

## S-001 - Run Gradle from the repository root, never from a shell sitting inside a build directory

- **Destination:** loop
- **Earned by:** Kanso Run 4, Phase 2 (2026-10-09): the combined verification `:app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:verifyRoborazziDebug :app:lintDebug :app:assembleDebug --continue` failed with `Unable to delete directory '…\app\build\test-results\testDebugUnitTest'`. The Iteration's persistent shell had been left inside that directory by an earlier `cd` to read the test XML. About 3 minutes and one full rerun were lost; the evidence log of the failed run had to be kept apart from the real one.
- **Status:** open

### What happened

The Execution Engine's shell keeps its working directory between commands. After a command that did `cd app/build/test-results/testDebugUnitTest` to read result files, a later Gradle run started in the background from that same directory. On Windows a process whose working directory is inside a folder holds a lock on it, so Gradle's `clean…` task could not delete the folder and failed the build. The other tasks ran (`--continue`), so the log looked half-green, which is easy to misread. Reading the XML from the repository root with a relative path (`cat app/build/…`), or starting every Gradle command with `cd <repo root> &&` in the same command, avoids it.

### In plain words

The engine opened a folder to look at some test results, then asked the build tool to delete that same folder while it was still standing in it. Windows refuses to delete a folder someone is standing in, so the build stopped. It is a bookkeeping slip, not a code defect, but it burned a few minutes and produced a confusing half-finished log. The fix is a habit: always start from the project's top folder before running the build.

---

## S-002 - `foreman-device.ps1 -Op dump` lets a dump of the notification shade through

- **Destination:** loop
- **Earned by:** Kanso Run 4 Verifier (2026-10-09, Iteration 5): driving criterion 6 on the owner's Galaxy A13, `-Op dump` printed the open notification shade, with the owner's email senders, a bank notice and account names, and the dump was redirected into a file inside the repository that a checkpoint commit would have published. Caught by reading the dump's `package=` attributes before committing; deleted, never committed.
- **Status:** open

### What happened

`Assert-DumpIsOurs` in `.harness/loop/bin/foreman-device.ps1` refuses a dump that holds a package outside the app, its permission dialog and `com.android.systemui`. `com.android.systemui` is allowed on purpose, so the status and navigation bars may appear. But the notification shade and the quick settings panel belong to the same package, so when a person pulled the shade down between the focus check and the dump (or while the dump ran), every node in the file was `systemui` and the check passed. `-Op focus` called straight afterwards answered `NotificationShade`. The lesson: a check that allows a package for its bars also allows that package's windows with content.

A fix that stays inside the wrapper: after the dump, also read the focused window again and refuse (and write nothing) when it is not the app or its dialog; or refuse a dump whose nodes under `com.android.systemui` include anything beyond the bar's own ids (a `NotificationShade` window, notification rows). A second, smaller one for the engine's brief: raw dumps of screens that list the owner's photos should be summarised, never committed.

### In plain words

The tool that lets the engine look at the phone is built so it only ever sees the app being built. One screen slipped through: the pull-down notification area, which is part of the phone's system, not the app. If the owner pulls it down while the engine is looking, the engine can copy their messages into a file that is then saved with the project. Nothing leaked here because the engine noticed first, but it should not depend on that.

---

## S-003 - After an AGP 9 move, a green build and green tests can hide source that is no longer compiled: read the `NO-SOURCE` lines

- **Destination:** stack · android
- **Earned by:** Kanso Run 5, Phase 1 (2026-10-09): the whole project built and 231 tests passed, yet OpenCV's only live Kotlin file (`opencv/java/src/org/opencv/core/MatMatMul.kt`, class `org.opencv.core.MatMatMulKt`) was no longer in the library. Only the fresh-context review noticed, from `> Task :opencv:compileDebugKotlin NO-SOURCE` in the build log.
- **Status:** open

### What happened

The module set its folders in the AGP 8 way, `java.srcDirs = ['java/src']`, and applied the Kotlin plugin only to add `.kt` support. Under AGP 9's built-in Kotlin, Kotlin sources are read only from the source set's `kotlin` directories; the Android documentation says adding Kotlin folders to the `java` set is not supported. Nothing failed: the Kotlin task simply found no source (`NO-SOURCE`), the library built without the class, and no JVM test used it. The fix is `sourceSets.main.kotlin.directories.add('java/src')` (Groovy) or `kotlin.directories += "..."` (Kotlin DSL). A cheap check that would have caught it with no reviewer: after the migration, for every module that has `.kt` files, require its `compile…Kotlin` task not to say `NO-SOURCE`, and compare the library's class list with the one built before the move (`apkanalyzer dex packages --defined-only` on both APKs, then `comm` on the class names).

### In plain words

Moving the build to the new Android tooling can quietly drop a piece of code from a library. Nothing breaks, because nothing the tests touch uses it, so a green build proves nothing about it. The cheap safeguard is to compare what ends up in the app before and after the move, not just to see that it builds.

---

## S-004 - AGP 9 refuses two modules or libraries with one namespace; a premise about it should be checked in the repository at bootstrap

- **Destination:** stack · android
- **Earned by:** Kanso Run 5, Phase 1 (2026-10-09), attempt 1: `:app:processDebugMainManifest` failed with `Namespace '...' is used in multiple modules and/or libraries` for four wrapper modules and for `com.google.ai.edge.litert:litert-support:1.3.0` next to `litert-support-api:1.3.0`; it cost a failed attempt, a Tier-3 assumption and the run's chance of ending `DONE`.
- **Status:** open

### What happened

AGP 8 only warned about two modules or libraries declaring one namespace; AGP 9 turns it into a manifest-merge error for the app, with no per-module switch short of a global opt-out flag. Two shapes appeared. (1) Four modules that only wrap a prebuilt AAR (`api(group = "local", name = "x", ext = "aar")`, no `src/`) declared the AAR's own namespace; the fix without a flag is a unique `namespace` line in the wrapper's build script, which changes nothing that ships but an empty `R` class. (2) `litert-support` (LiteRT 1.3.0 and 1.4.2) is an empty forwarding artifact whose namespace equals that of `litert-support-api`; the fix is to depend on `litert-support-api` directly. The bootstrap plan and the approved Definition of Done both predicted the first shape and wrote "a namespace change is an engine source change; report the criterion unmet", without ever listing the wrapper modules' folders: they have no source, so the premise was false, and the approved sentence had to be departed from.

### In plain words

The new Android build tool no longer tolerates two parts of an app claiming the same name. This project had four empty shell modules that copied the name of the ready-made library they wrap, and one Google library that does the same with its own twin. The cure is giving the shell a different name, which changes nothing the person using the app sees. The lesson for the planner is smaller: before writing "this failure will need a source change, so report it", open the folder and look.

---

## S-005 - A criterion's proof command should name the test classes that hold its assertions, and the bootstrap critique should check that they do

- **Destination:** loop
- **Earned by:** Kanso Run 6, Phase 1 (2026-10-09): the Fresh-Context Reviewer found that DoD 3 ran `--tests '*ToolScreenTest*' --tests '*ToolViewModelTest*'` while the whole-app walk it describes lived in `SavedNavigationTest`, and that DoD 22 asked for a wallpaper restart in two classes that no longer reached the wallpaper once the default colour changed.
- **Status:** open

### What happened

A criterion said which test classes prove it, but the tests that really contain its assertions were written (correctly, as it happens) in other files, because those files already had the harness for driving the whole app. The command in the DoD would pass without ever running the walk. A second criterion named two tests that used to cover a case only because the default happened to be that case; the default moved and the cover went with it. Both were found by the reviewer, not by a test, and fixed by adding tests to the named classes.

### In plain words

The checklist said "to prove this, run these tests", but the real check was written in a different test file, so running the named tests would have passed without proving anything. Reading a criterion's proof against where its assertions actually live, at the bootstrap critique and again in review, would catch it before a Verifier trusts a green line.


---

## S-006 - Write each Worker's manifest to disk the moment it returns, so a process that ends before its checkpoint loses nothing the Workers learned

- **Destination:** loop
- **Earned by:** Kanso Run 6, Phase 2 (2026-10-10, Iteration 3): a process dispatched three Capable Workers (Home, gallery, editor), their files reached the working tree, and the process ended before the wiring and before any commit; no status was written and the cause of the end is not recorded anywhere the next process can read. The Workers' reports (the files each wrote, what it could not do, what it learned) were never saved, and the task files still read `pending, 0 attempts`. The next process could verify the diff but not recover what the Workers had reported. Iteration 2 of the same run also ended without a checkpoint (the Runtime's timeout, during the picture comparison).
- **Status:** open

### What happened

The engine's contract says a Worker's report is a manifest the orchestrator reads, and that everything learned must be persisted in the same checkpoint commit as the code. Both are right, but the report only exists in the orchestrator's conversation until that commit. When a process ends in between, the working tree keeps the Workers' files (which a recovery step can salvage, and here did: it built, passed, and needed only the shared-file wiring and one missing import) while the reports, any "I could not do X", and any trap a Worker noticed are gone with the conversation. A small change closes the gap: as each Worker returns, the orchestrator writes its manifest to `.harness/run/evidence/phase-<n>-reports/<task>.md` (a plain file, no commit needed), and the Recover step reads that folder before judging the debris. A second, larger option, for the doctrine owner to weigh: let an Iteration make a work-in-progress commit on the Loop Branch after the Workers return, squashed or marked at the real checkpoint.

### In plain words

The engine hires several helpers for a stretch of work and each one hands back a short note when they finish. If the engine is switched off before it files those notes, the finished work is still on the desk but the notes are lost, including any warning a helper wanted to pass on. Saving each note to a file the moment it arrives costs almost nothing and means a restart can pick up exactly where things stood.

---

---

## S-007 - Every test class a criterion's proof names needs a task that writes it; the plan's conflict analysis should list them

- **Destination:** loop
- **Earned by:** Kanso Run 6, Phase 4 (2026-10-10): DoD 13's proof says `NavTransitionTest` and `ToolSelectionAnimationTest` both repeat their moves with the system animation scale at 0. The plan scheduled the first (T-008) and never the second; no later phase existed, so the run would have reached its Verifier with half of the proof missing. The fresh-context review of Phase 4 found it, not a test and not the bootstrap critique.
- **Status:** open

### What happened

At bootstrap the critique role checks the decomposition and the DoD's classes, and the conflict analysis maps each task to the files it touches. Nothing maps the other way: from each test class a Proof names back to the task that writes or edits it. S-005 caught the sibling case (a named class that does not hold the assertion); this one is a named class that no task is ever asked to touch, because the Proof spread one criterion across two classes in two different tasks' areas and the planner scheduled the nearer one.

### In plain words

The checklist said "prove this in test A and test B". The plan only ever sent someone to write test A. A final read of "every test a proof names, and which task writes it" at planning time, and again at the end of each phase, would have caught it before the last step instead of during a review.


## S-008 - Checkpoint a Phase as soon as its build is verified; a review's follow-up tasks are the next Phase, not more of this Iteration

- **Destination:** loop
- **Earned by:** Kanso Run 6, Phase 5 (2026-10-10, Iteration 6): the Iteration dispatched three Workers, verified them, and then ran two fresh-context reviews, each of which produced more work (a spike, three new tasks, a one-off mutation proof, three more fresh full runs), all inside the same Iteration and with no commit in between. It ran about an hour and a half and ended by the Runtime at its 5400-second limit (telemetry row: CRASH, 5400 s) with six tasks' worth of finished work uncommitted, the task files still reading `pending, 0 attempts` and no HISTORY entry. The next process found the tree coherent and salvaged it, but had to re-verify everything (a 3 minute run, a review and the writing of every record) before it could checkpoint.
- **Status:** open

### What happened

The engine's contract is one Iteration, one Phase, one atomic checkpoint at the end, with Phase size capped so the end comes soon. A review that adds tasks quietly lifts that cap: the Iteration kept going, each follow-up was small and each looked cheaper than stopping, and nothing in the contract says when the sum is too much for one process. Compare S-006, where the cost of an early stop was lost reports; here it is lost time and a longer recovery.

### In plain words

The engine is meant to do one bounded chunk of work and then save it. After the work passed its checks, the engine kept finding more things to fix and prove, and never saved in between, so when the clock ran out the saved copy was an hour and a half old. A rule such as "once the build is green and the records are written, save; anything the review finds becomes the next chunk" would make a cut-off cost minutes, not an hour.

---

## S-009 - At bootstrap, run every "prints nothing" Proof against the base, and audit each test against its Proof before the Verifier does

- **Destination:** loop (bootstrap §5 and the per-Phase review)
- **Earned by:** Kanso Run 6, the Verifier of Iteration 7 (2026-10-10). Five Phases were green, and the Verifier still had to file a sixth for three Proof clauses:
  - DoD 29's `git grep -n 'reference/' -- settings.gradle.kts app` "prints nothing", but it matched a comment that was in the repository **before the run started**, so the Proof was false on the day it was approved.
  - Two tests checked less than their Proofs promised: one colour role instead of a whole scheme, and a fill check run on a different photo than the one the Proof names. Five fresh-context reviews had not caught either, because each read the diff, not the test against the Proof.
- **Status:** open

### What happened

The bootstrap wrote each search Proof from what the code should look like after the run, and nobody ran it against the code before the run. The per-Phase reviews read the diff and the Constraints. Only the Verifier's audit read each test against the Proof it claims to satisfy, and that came after all the building was done.

### In plain words

Two cheap checks would have saved a whole extra round:
- Before asking the person to approve the checklist, run every "this search should find nothing" line on today's code. If it already finds something, the line is wrong before any work starts.
- When a test is written for a checklist line, have the reviewer hold the two side by side and ask "does this test check everything the line says?"

---

## S-010 - Let `foreman-device.ps1 -Op focus` answer before a debug APK exists

- **Destination:** loop (`.harness/loop/bin/foreman-device.ps1`)
- **Earned by:** Kanso Run 6, the Verifier of Iteration 9 (2026-10-10). The Verifier started with `:app:clean`, as a fresh re-proof should. Its first `-Op focus`, sent to learn whether a person was using the phone, was refused: "this repository has no debug APK built yet". It had to wait about 3 minutes for the build before it could ask again.
- **Status:** open

### What happened

The wrapper checks for a debug APK the repository built before it runs any operation, `focus` included. `focus` reads which window has the screen. It installs, taps and reads nothing of the app, so the APK check guards nothing there. Its answer decides whether the phone may be driven at all, and the Verifier wants that answer first, while the build is still running.

### In plain words

Before touching the phone, the engine asks "is someone using it?". The helper refused to answer until the app had been built, even though answering does not touch the app. Letting that one question through, while every other operation still needs a built app, would let the check happen alongside the build instead of after it.

---
