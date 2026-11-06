# RESUME BLOCK

> A derived cache; on any disagreement with task files or git, this file is wrong.

- **Stage:** build (Run Mode Autonomous)
- **Next Phase:** none. All tasks done; PARTIAL-candidate set (pictures 97/98 not on the Baselines list, red). Next invocation is the Verifier (§11, §14.4).
- **Queued decisions:** none (D-003 answered)
- **Abandoned / unreachable:** none
- **Verified commands:** `cd /d/Valve/Personal/Kanso && timeout 3000 ./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --continue` | pictures: `./gradlew :app:verifyRoborazziDebug` (green) | new baselines: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`
- **Criteria T-003 cites:** find by Grep on the number in `.harness/run/DoD.md` (9, 10, 14, 15 likely).
- **Worker Brief notes:** Constraints copy lives in 4 files `C:\Users\MAC_PC\AppData\Local\Temp\kc\part-00..03.md` (recreate with `sed -n 3,72p .harness/knowledge/PROJECT.md | split -l 18` if /tmp is gone; a Worker's Read refuses files over ~10 KB). The scope checker cannot parse `...` shorthand: check scopes by hand. Test edits log: `.harness/run/evidence/test-edits.md` (append). Curves API: `ToneCurve` (NEUTRAL, table(), added/moved/removed), `CurveAdjustments`, `EditCurves.apply`, `EditSettings.curves/hasCurveChange`; graph tag `curve-graph`, reset tag `curve-reset`.
- **Model tiers:** fast `sonnet` (medium) | capable `opus` (medium)
- **Device:** Galaxy A13 serial `R58W1120QVA` (seen 2026-11-06)
