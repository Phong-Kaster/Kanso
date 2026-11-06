# RESUME BLOCK

> A derived cache; on any disagreement with task files or git, this file is wrong.

- **Stage:** build (Run Mode Autonomous)
- **Next Phase:** none. T-001..T-004 and T-006 done (T-005 removed). PARTIAL-candidate set: pictures 99/100 red (old baselines show the D-004 bug; not on `## Baselines`). Next invocation is the Verifier (§11, §14.4).
- **Queued decisions:** none (D-001..D-004 answered)
- **Abandoned / unreachable:** none
- **Verified commands:** `cd /d/Valve/Personal/Kanso && timeout 3000 ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --continue` (about 7-10 min; run in background and wait) | pictures: `./gradlew :app:verifyRoborazziDebug` (red only on 99/100) | new baselines: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app` (re-records only `## Baselines` paths)
- **DoD 23 (owner, D-004):** "On every curve (RGB, Red, Green, Blue, Lightness), each of the five chips shows its name on a single line, and Reset stays fully visible and tappable in the sheet, on a 411 dp wide phone and on a 360 dp wide one, in light and in dark, at the default text size." Proof: `CurveChipRowTest`. Note: the owner's DoD edit put 23 between 22's text and 22's Proof line; 22's Proof is 22's.
- **Worker Brief notes:** Constraints copy in `C:\Users\MAC_PC\AppData\Local\Temp\kc\part-00..03.md` (recreate: `(sed -n 3,71p; sed -n 103,105p) PROJECT.md` then `split -l 18`; a Worker's Read refuses files over ~10 KB). Test edits log: `.harness/run/evidence/test-edits.md`. Tags `curve-graph`, `curve-reset`.
- **Model tiers:** fast `sonnet` (medium) | capable `opus` (medium)
- **Device:** Galaxy A13 serial `R58W1120QVA` (seen 2026-11-06)
