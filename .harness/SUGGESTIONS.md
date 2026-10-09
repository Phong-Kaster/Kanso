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

