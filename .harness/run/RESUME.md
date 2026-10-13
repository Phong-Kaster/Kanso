# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.
>
> Keep it small. Every iteration pays to read it, and unlike ENGINE.md it is not served from a
> prompt cache. Never let history accumulate here.

- **Stage:** The Verifier (Iteration 17) re-proved every machine criterion green and looked at all 41 pictures; one gap: pictures 28, 29, 34, 35 lack the floating bar DoD 22 promises. PARTIAL-candidate cleared. **Next: Phase 9 = T-017**, then the Verifier again (re-prove; look again at the four re-recorded pictures; drive the Galaxy A13 if `-Op focus -Serial R58W1120QVA` names Kanso).
- **Phase 9 - T-017 (Fast, `sonnet`):**
  - Worker scope: `app/src/test/java/com/phongkaster/kanso/screenshots/MyEditsScreenshotTest.kt` (`showPhotos` + KDoc), `app/src/test/java/com/phongkaster/kanso/screenshots/HomeScreenshotTest.kt` (`showEmpty` + KDoc). Pattern to copy: `HomeScreenshotTest.showSmallPhone` lines 190-198 (`KansoTabShell(showBar = true, selected = ..., onTabSelected = {}, onCamera = {})`, content gets `contentPadding = PaddingValues(bottom = tabBarClearance())`; `MyEditsContent` has a `contentPadding` parameter, `MyEditsScreen.kt:143`). Then assert `KANSO_TAB_BAR_TAG` is displayed.
  - The Iteration: `:app:compareRoborazziDebug`, copy the four `*_compare.png` to `.harness/run/evidence/baseline-diffs/phase9/`; `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app` (D-001 lists all four); open each picture; refresh `after/` and `index.md` rows for 28, 29, 34, 35; one line per test file in `evidence/test-edits.md`; whole set; write A-029's SHA and revert in the next Iteration.
- **DoD text the Phase cites:**
  - DoD 22 (last clause): "Pictures 1, 2, 28, 29, 34 and 35 show the bar in the light and dark pictures (criterion 29)." The bar is dark in light mode and light in dark mode.
  - DoD 29 (d): "`./gradlew :app:verifyRoborazziDebug` exits 0, and `app/build/test-results/roborazzi/debug/results-summary.json` reports total 41 and changed 0. `git diff --name-status <base> HEAD -- app/src/test/screenshots` shows `M` for exactly the 24, `A` for exactly the new ones recorded, and no `D`."
  - DoD 35: every test passes; 571 `@Test` lines (unchanged by T-017); every changed test file listed with its reason in `test-edits.md`.
- **Open ISSUES items naming these files:** none.
- **Queued decisions:** 0 (D-001 answered).
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` (compare: `:app:compareRoborazziDebug`; record new/approved: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`) | the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (about 3 to 8 min). Always under `timeout`, output to `.harness/run/evidence/`, begin with `cd /d/Valve/Personal/Kanso &&`. A long chain runs well as a script with `run_in_background` plus a bounded foreground wait on its times file (`evidence/v17-chain.sh`, `v17-wait.sh`).
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` (dispatch every Worker with `model` set explicitly; the Reviewer and the Verifier are always `opus`)
- **Time note:** the Runtime kills an Iteration at 90 minutes; the Verifier's chain took 8.5 min in Iteration 17.
