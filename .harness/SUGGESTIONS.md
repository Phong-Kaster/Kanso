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
