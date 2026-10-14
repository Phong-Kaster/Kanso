# RESUME BLOCK

- **Stage:** phase 3 done (T-001..T-006 complete); next Phase 4 = T-007 only
- **Next Phase:** T-007 (pictures, README audit, a11y/preview tests for all five panels) - scope in its task file. Record only the pictures D-001 lists (new from 45: Remove background, Retouch, Artistic light+dark = 6; Edit and Enhance with controls light+dark = 4; plus re-record 16,17,22,23,40,41,42 - DECISIONS.md "Baselines" lists them; if `recordRoborazziDebug` is denied, say so in ISSUES). README rows and traces for all five already written in Iteration 5; T-007 audits them (paths) and writes `evidence/readme-paths.sh`.
- **Seam:** `ToolViewModel.runRegistered(fromOriginal, configure)`; `ToolPanel(tool, modifier, state, onApply, onRun, onRetouch, onStylize)`; panels `EditPanel`, `CutoutPanel`, `EnhancePanel`, `RetouchPanel`, `ArtisticPanel(Host)`.
- **Queued decisions:** 0
- **Abandoned / unreachable:** none
- **Open red:** 7 `ToolScreenshotTest` pictures (16,17,22,23,40,41,42) differ: baselines not re-recorded (command denied); listed in ISSUES.md.
- **Verified commands:** build `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug` | lint `./gradlew :app:lintDebug` (run under `timeout`)
- **Criteria:** read DoD 24-27, 31-33 and 25 with Grep by number (`.harness/run/DoD.md`).
- **Model tiers:** fast: `sonnet` | capable: `opus`
- **Notes:** Workers read `.harness/` only under `knowledge/reference/`; copy PROJECT.md Constraints (lines 3-67) to a temporary `reference/_brief-constraints.md` and delete before commit.
