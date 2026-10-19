# RESUME BLOCK

> Derived cache; loses to task files and git.

- **Stage:** Phase 7, the Verifier's gaps. After it: PARTIAL-candidate (Tier-3 A-001, A-003, A-009), so the next Verifier re-proves and drives 20/21/22/34, then `DONE_PARTIAL` (SS14.4).
- **Next Phase 7:** (`M/`=`app/src/main/java/com/phongkaster/kanso/`, `T/`=`app/src/test/java/com/phongkaster/kanso/`, `V/`=`app/src/main/res/values/`)
  - Iteration first: `git mv V/strings_saved.xml V/strings_result_mark.xml` and `git mv V/strings_saved_layout.xml V/strings_result_layout.xml`.
  - T-010 (Fast), sequential slices: (1) `V/strings.xml`, `V/strings_result_mark.xml`, `V/strings_result_layout.xml`, `M/ui/result/ResultScreen.kt`, `M/ui/common/BlurredBackdrop.kt`, `M/ui/common/RoundIconButton.kt`; (2) `M/ui/home/HomeScreen.kt`, `M/ui/home/HomeViewModel.kt`, `T/navigation/MyEditsViewerNavigationTest.kt`, `T/result/ResultScreenTest.kt`, `T/result/ResultViewModelTest.kt`, `T/result/SharePhotoTest.kt`; (3) `T/result/ShareableUriTest.kt`, `T/result/FakeImagesProvider.kt`, `T/screenshots/ResultScreenshotTest.kt`, `T/ExpressiveApiTest.kt`, `T/navigation/BackStackActionsTest.kt`, `T/a11y/AccessibilityWalkTest.kt`; (4) `T/a11y/OverPhotoContrastTest.kt`, `T/theme/ThemeFollowTest.kt`, `T/a11y/BottomClearanceTest.kt`.
  - T-011 (Fast), one Worker, parallel with T-010: `README.md`, `docs/features/edit.md`, `M/ui/tool/ToolScreen.kt`, `M/ui/tool/EditPanel.kt`, `M/ui/tool/EditCropper.kt` (comments only in .kt). Details in `TASKS/T-011.md`.
- **Criteria cited, quoted:**
  - R6. [machine] "Saved" is gone as a name; Result replaces it. Proof: `grep -rnw "Saved\|SavedScreen\|SavedViewModel\|SavedUiState\|SavedPhoto" app/src` prints nothing; `navigation/Saved.kt` and `ui/saved/` no longer exist; `grep -rn "saved_" app/src/main/res/values` prints nothing. (Read by A-009: the word stays where it means "written": `SaveResult.Saved`, `SaveState.Saved`, the button's "Saved" label.)
  - 34. [machine-then-human] The written record is true. Proof: the Verifier reads `README.md` "How each editing feature works" and `docs/features/edit.md` against the code and notes every sentence it could not back; then a person reads them and signs. A claim the code does not back fails the run.
  - 31. [machine] Every existing test this run had to change says why, in the test itself, and is listed with its reason. Proof: `.harness/run/evidence/test-edits.md` lists every base test name (at `eccbf95`) that is missing, renamed or changed at HEAD with a reason, and each changed test file carries a comment line containing "Run 9" and the reason; no test uses `assumeTrue`, `@Ignore` or `Assume.`.
  - 32 (keep passing): README and the three feature docs hold no "springs back", "Saved screen" or "range slider" outside a sentence recording the removal; every backticked path exists.
- **Open ISSUES items in these scopes:** none beyond the two tasks themselves.
- **Queued decisions:** 0. **Abandoned / unreachable:** none.
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && timeout 1500 ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | pictures: `verifyRoborazziDebug` (whole set 7m 45s, 812 tests, 2026-10-19)
- **Known weak spot:** ToolScreenshotTest.chooseFilter1977 still uses a 1.5 s settle sleep after polling the chip.
- **Model tiers:** fast: `sonnet` | capable: `opus`
