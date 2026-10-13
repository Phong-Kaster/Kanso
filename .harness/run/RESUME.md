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

- **Stage:** the Verifier (Iteration 12) found gaps and cleared the DONE-candidate. **Next: Phase 6 = T-013 (Fast) + T-015 (Capable)**, dispatched at once (disjoint scopes); then Phase 7 = T-014; then the Verifier again.
  - T-013 - `M data/media/MediaStoreSavedPhotoSource.kt` (drop the added `@Suppress("DEPRECATION")` by naming the `"_data"` column as a const; anchor the Android 8.1/9 folder match to the Pictures root, no subfolders), `M ui/common/SkeletonBlock.kt` (read "Remove animations" from `Settings.Global.ANIMATOR_DURATION_SCALE`, not `ValueAnimator`) - Fast. The Iteration: `git rm app/src/main/res/drawable/ic_add_photo_alternate.xml`.
  - T-015 - `M navigation/KansoTabBar.kt` (KDoc true to the `Surface`+`Row` build, why by hand), `M ui/settings/SettingsScreen.kt` (title = Home's `headlineLargeEmphasized`, 8 dp top), `M ui/home/HomeScreen.kt` (strip scrolls to 0 only when the newest id changed, `rememberSaveable`), `M ui/common/BlurredBackdrop.kt` (tiny copy off the main thread, `produceState` + `Dispatchers.Default`), `M ui/saved/SavedScreen.kt` (non-teal `sampleWideBitmap`) - Capable. The Iteration: stale comments `navigation/Tool.kt:12-13`, `KansoNavHost.kt:104,123`; `compareRoborazziDebug`, compare files into `evidence/baseline-diffs/phase6/`, re-record 4, 5, 6, 24, 25 (D-001 approval); append every test edit to `evidence/test-edits.md`.
  - (`M` = `app/src/main/java/com/phongkaster/kanso/`.) Task files hold the full findings; review text in HISTORY.md, Iteration 12.
- **DoD text the Phase cites (quoted):**
  - 7: "The "Recent edits" strip shows Kanso's saved photos, newest first, as tall rounded cards that scroll sideways. Tapping a card opens that photo in the Edit tool, and the arrow opens My edits. A fresh install shows a short message instead. A photo just saved is first in the strip when the person comes back to Home." (e) `home-shortcut-EDIT` -> photo 1 -> Save (Uri X added as newest) -> Saved -> `saved-close`: within 10 s `home-recent-0` carries X.
  - 20(a): "`git grep -n -E 'ObjectAnimator|ValueAnimator|AnimatorSet|android\.view\.animation|ViewPropertyAnimator|\.animate\(\)' -- app/src/main` prints nothing ... the word "shimmer" is not used in `app/src/main`."
  - 32: "Recent edits and My edits show only the photos in Kanso's own album in Pictures, newest first, on Android 8.1, 11 and 15. Looking at them never changes or deletes anything." `EditsSourceTest` at SDK 27, 30, 35: exactly the 5 of `Pictures/Kanso`, not `DCIM/Camera` or `Pictures/Other`; at SDK 27 matched by file path.
  - 37: "The new ideas are explained in plain English where they first appear: the carousel, tabs in Navigation 3, the floating bar, the blur, the placeholders and the shade on Saved." The bar's file mentions "tab" and `inverseSurface`; the blurred-backdrop file mentions `blur` and "Android 12".
  - 38: "No `@Suppress("DEPRECATION")` or `@Suppress("DEPRECATION_ERROR")` is added under `app/src/main`."
  - R4/R9: "`app/src/main/res/drawable/ic_add_photo_alternate.xml` is either absent or referenced as `R.drawable.ic_add_photo_alternate` in `app/src/main/java`."
  - 23: "Nothing paints a colour of its own, InFlux's teal appears nowhere."
- **Open ISSUES items naming these files:** `ThemeFollowTest.checkSaved` (test only, not in scope).
- **Queued decisions:** 0 (D-001 answered).
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | screenshots: `./gradlew :app:verifyRoborazziDebug` (compare: `:app:compareRoborazziDebug`; record new/approved: `powershell -NoProfile -File .harness/loop/bin/foreman-record-baselines.ps1 -Module app`) | the whole set: `./gradlew cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (about 3 to 8 min). Always under `timeout`, output to `.harness/run/evidence/`, begin with `cd /d/Valve/Personal/Kanso &&`. A shell here rejects some multi-line `cat <<EOF` heredocs: write files with the Write tool; a `( ... ) &` subshell with `run_in_background` returns at once, so wait for its exit file in a bounded foreground loop.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` (dispatch every Worker with `model` set explicitly; the Reviewer and the Verifier are always `opus`)
- **Time note:** the Runtime kills an Iteration at 90 minutes; the whole set takes about 3.5 min warm. Iteration 12 found that the shell's `cat <<'EOF'` heredocs work here.
