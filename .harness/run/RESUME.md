# RESUME BLOCK

> A derived cache; on any disagreement with task files or git, this file is wrong.

- **Stage:** verify, ended `DONE_PARTIAL` at Iteration 7 (Run Mode Autonomous)
- **Next Phase:** none. T-001..T-004 and T-006 are done (T-005 was removed). Every machine criterion was re-proved at Iteration 7; criteria 15, 16 (not driven on the device), 17 and 22 await a person (`.harness/ISSUES.md`, "Awaiting a person, Run 12"). If the owner fails an item in `DECISIONS.md`, reconcile it into a task.
- **Queued decisions:** none (D-001..D-004 answered)
- **Abandoned / unreachable:** none
- **Verified commands:** `cd /d/Valve/Personal/Kanso && timeout 3000 ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --continue` (about 10-12 min; run it in the background and wait) | pictures: `./gradlew :app:verifyRoborazziDebug` (green) | new baselines: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app` (re-records only paths listed under `## Baselines`)
- **Known flaky:** `SettingsUiStateTest.state_follows_the_theme_mode_set_through_the_view_model` (failed once, then passed twice)
- **Worker Brief notes:** a copy of the Constraints is in `C:\Users\MAC_PC\AppData\Local\Temp\kc\part-00..03.md` (to recreate: `(sed -n 3,71p; sed -n 103,105p) PROJECT.md`, then `split -l 18`; a Worker's Read refuses files over about 10 KB). Test edits log: `.harness/run/evidence/test-edits.md`.
- **Model tiers:** fast `sonnet` (medium) | capable `opus` (medium)
- **Device:** Galaxy A13, serial `R58W1120QVA`; the Iteration 7 debug build is installed. Another app (`com.selyn.aiphoto.editor`) held the screen.
