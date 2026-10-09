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
