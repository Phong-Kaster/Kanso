# Definition of Done

> Derived from `PRD.md` at bootstrap (Run 4: one class per file, one UiState per screen). Human-owned after approval: the engine may propose changes (Tier 3) but never apply them.
> Loop Branch `loop/kanso-one-class-per-file`, started from commit `4d5f239` (written `<base>` in the proofs below).

## Status

- [ ] APPROVED — approve via the pending `.harness/run/ESCALATION.md` (D-001); edit criteria freely before approving.
      Approval covers the Verification Class of each criterion, not only its wording.

## Acceptance Criteria

### Behaviour

1. [machine] Every screen still does what it did before this run, and the whole automatic test suite passes.
   Proof: `set -o pipefail; ./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest > .harness/run/evidence/test.log 2>&1` exits 0 and the log ends `BUILD SUCCESSFUL` (read the XML only after seeing that line). Then `cat app/build/test-results/testDebugUnitTest/TEST-*.xml | grep -o '<testsuite [^>]*>' | grep -o -E '(tests|skipped|failures|errors)="[0-9]+"' | awk -F'[="]' '{s[$1]+=$3} END{for (k in s) print k, s[k]}'` prints `failures 0`, `errors 0`, `skipped 0`, and `tests` equal to the number of `@Test` at HEAD (`git grep -h -c '@Test' HEAD -- app/src/test/java | tr -d '\r' | awk '{s+=$1} END{print s}'`), which must be at least 198 plus this run's new tests. `LaunchTest` is among them. A failing full run is a defect, never a flake to rerun past (PROJECT.md, Verifier 2026-10-07).

2. [machine] Every screen looks exactly as it did before, pixel for pixel.
   Proof: `./gradlew :app:verifyRoborazziDebug` exits 0 (16 screenshot tests, 0 failed), and `git diff --exit-code --stat <base> HEAD -- app/src/test/screenshots` exits 0 with no output, so none of the 16 baseline images (`1_home_light.png` … `19_saved_dark.png`) is added, changed or deleted. `.harness/run/DECISIONS.md` holds no `## Baselines` heading.

3. [machine] No existing check was deleted, switched off or weakened; a check may only have moved to another file.
   Proof, from the repository root, under Bash:
   (a) every test name at `<base>` still exists at HEAD: `set -o pipefail; for r in 4d5f239 HEAD; do git grep -h -I -E '@Test|^\s*(private |internal )?fun ' $r -- app/src/test/java | tr -d '\r' | awk '/@Test/{t=1; if (match($0,/fun [^ (]+/)) {print substr($0,RSTART+4,RLENGTH-4); t=0}; next} t && match($0,/fun [^ (]+/){print substr($0,RSTART+4,RLENGTH-4); t=0}' | sort > .harness/run/evidence/names-$r.txt; done; comm -23 .harness/run/evidence/names-4d5f239.txt .harness/run/evidence/names-HEAD.txt` prints nothing (verified 2026-10-09: 198 names at `<base>`, none duplicated);
   (b) `git grep -n '@Ignore' HEAD -- app/src/test` exits 1;
   (c) `git diff --exit-code <base> HEAD -- app/src/test/java/com/phongkaster/kanso/previews app/src/test/java/com/phongkaster/kanso/KdocCoverageTest.kt` exits 0, so no preview or KDoc rule was loosened;
   (d) every assertion line the run removed is back as an added line: `git diff -M -U0 <base> HEAD -- app/src/test/java | grep -E '^-[^-]' | grep -E 'assert|check\(' | sed -E 's/^-\s*//' | sort > .harness/run/evidence/removed.txt`, the same with `'^\+[^+]'` and `s/^\+\s*//` into `added.txt`, then `comm -23 removed.txt added.txt` prints nothing, or each printed line is one the Verifier reads in the diff and explains as a call site that followed a moved type.

4. [machine] The screen shown after saving still shows the saved picture, and for a photo that cannot be read it shows an empty picture area instead of failing.
   Proof: in criterion 1's run `SavedScreenTest`, `SavedNavigationTest` and `SavedScreenshotTest` pass, and the new `SavedViewModelTest` passes: the Saved state starts as loading, becomes the picture when the loader returns one, becomes unreadable when the loader returns null, and a second request for the same size does not load again. Baselines `18_saved_light.png` and `19_saved_dark.png` pin how it looks (criterion 2).

5. [machine] Home still lists the six tools in the same order.
   Proof: the new `HomeViewModelTest` asserts the Home state's tool list equals `Tool.entries` in order; baselines `1_home_light.png`, `2_home_dark.png` and `3_home_custom_colour.png` pin the drawn order (criterion 2); `NavigationTest` passes (criterion 1).

6. [machine-then-human] On a phone, each of the six screens opens and works as it did before this run.
   Proof: driven from a fresh install of this run's debug build (`-Op uninstall` then `-Op install`) with no Kanso data, no permission granted, theme mode System; every device step through `powershell -NoProfile -File .harness/loop/bin/foreman-device.ps1 -Op <operation>`; `-Op devices` decides at Verify time whether a device exists, and nothing is claimed otherwise. Machine pre-check: start the app, `-Op dump` Home; open a tool, answer the photo-access prompt by tapping, `-Op dump` the gallery, pick a photo, `-Op dump` the tool screen, tap Save, `-Op dump` the Saved screen; back to Home, open Settings, choose Dark, `-Op dump`; open Camera, allow it, `-Op dump`; the app's log shows no `FATAL EXCEPTION`. A pass is written "did not fail when driven from a fresh install", never "works". The person then walks the same route: Home; a tool's gallery; the tool with a photo; Save; Saved (Share opens the share sheet); Settings (mode, Custom colour, drag the picker, leave and come back: choices kept); Camera (live picture, shutter gives Keep and Retake, switch lens, flash, Keep opens the tool). Expect each screen to look and behave as the 16 baseline images show. This is the same walk as Run 3's unsigned item 10; one signed walk may close both.

### Permissions the user is asked for

7. [machine] The app asks the person for exactly the same permissions as before, and still has no internet access.
   Proof: `git diff --exit-code <base> HEAD -- app/src/main/AndroidManifest.xml` exits 0; after criterion 9's `assembleDebug`, `grep -c -E 'android.permission.(INTERNET|ACCESS_NETWORK_STATE)' app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml` prints 0.

### Data and storage

8. [machine] The chosen theme mode and theme colour are still saved, shown in Settings, and restored after a restart.
   Proof: in criterion 1's run these pass: `ThemeModeSettingsTest`, `ThemeModeRestartTest`, `ThemeColourSettingsTest`, `ThemeColourRestartTest`, `CustomColourDebounceTest`, `ThemeColourLabelsTest`, `PickerNoAlphaTest`, `PickerDefaultAlphaTest`, and the new `SettingsUiStateTest` (the Settings state starts as System mode and this phone's default colour, then follows what is saved). Baselines `4_settings_light.png`, `5_settings_dark.png`, `6_settings_custom_colour.png` pin how it looks (criterion 2).

### Build, start and quality

9. [machine] The app builds, passes the code-quality check, and uses the same libraries, build tool and phone types as before.
   Proof: `./gradlew :app:assembleDebug` exits 0 with `BUILD SUCCESSFUL`; `./gradlew :app:lintDebug` exits 0; `git diff --exit-code <base> HEAD -- gradle/libs.versions.toml app/build.gradle.kts build.gradle.kts settings.gradle.kts gradle.properties` exits 0 (so Navigation 3 stays 1.1.7, AGP stays 8.13.0, the three debug ABIs stay, and no dependency was added). Starting without a crash is proved by `LaunchTest` in criterion 1.

10. [machine] Every code file in the app and in its tests holds one class, interface, object or enum, named after the file, and a test now fails the build if anyone puts two together or names the file otherwise.
    Proof: the new `OneTypePerFileTest` passes in criterion 1's run with an empty allow-list. It reads every `.kt` under `app/src/main/java` and `app/src/test/java` (never `reference/` or `core/`), ignores comments, strings, `companion object`s and `object :` expressions, counts nested types as part of their parent, and reports `path:line`. Its self-checks on inline samples must show it reports two top-level types, a `private` type beside another, and a misnamed file, and accepts a sealed interface with nested cases. Evidence it can go red on the real tree: the Phase 2 checkpoint commit records the test green with an allow-list of exactly the 14 files that then still broke the rule, each entry proved to still break it; the Phase 3 checkpoint records the allow-list emptied. `git grep -n -E 'ALLOWED' HEAD -- app/src/test/java/com/phongkaster/kanso/OneTypePerFileTest.kt` shows the list is empty.

11. [machine] Each of the six screens (Home, Gallery, Camera, Tool, Saved, Settings) has its three files: what it draws, what keeps its state, and the state itself.
    Proof: `ls` of these 18 paths exits 0, all under `app/src/main/java/com/phongkaster/kanso/ui/`: `home/HomeScreen.kt`, `home/HomeViewModel.kt`, `home/HomeUiState.kt`, `gallery/GalleryScreen.kt`, `gallery/GalleryViewModel.kt`, `gallery/GalleryUiState.kt`, `camera/CameraScreen.kt`, `camera/CameraViewModel.kt`, `camera/CameraUiState.kt`, `tool/ToolScreen.kt`, `tool/ToolViewModel.kt`, `tool/ToolUiState.kt`, `saved/SavedScreen.kt`, `saved/SavedViewModel.kt`, `saved/SavedUiState.kt`, `settings/SettingsScreen.kt`, `settings/SettingsViewModel.kt`, `settings/SettingsUiState.kt`. And `git grep -n -E '^\s*(data |sealed |enum )?(class|interface) \w*UiState\b' HEAD -- 'app/src/main/**/*ViewModel.kt' 'app/src/main/**/*Screen.kt'` exits 1 (no UiState is left inside a ViewModel or Screen file).

12. [machine] Each screen's state keeper gives out exactly one read-only description of the screen, and the drawing part of each screen takes that description.
    Proof: the new `ScreenStateTest` passes in criterion 1's run. It finds every `*ViewModel.kt` under `ui/` and asserts the set is exactly the six; for each it asserts, by Java reflection, exactly one public method returning `kotlinx.coroutines.flow.StateFlow` whose type argument is that screen's `…UiState`; it asserts each `fun <Screen>Content(` declares a `<Screen>UiState` parameter; and it asserts no `…UiState.kt` declares a `var` or a `Mutable…` type. Its self-checks on inline samples report two StateFlows, a `StateFlow<String>`, and a `Content` with no UiState. A one-shot event flow that is not a StateFlow (Tool's `savedEvents`) and plain read-only values (`tool`, `photoUri`) may sit beside the one StateFlow; a `…Content` may also take callbacks, a modifier, picture slots and facts only the drawing part can know (the permission answer, whether Android 12 wallpaper colours are offered, whether a share is in progress).

13. [machine] Every drawing function still has a light and a dark preview in the same file, built from made-up sample data, and every preview still draws without an error.
    Proof: in criterion 1's run `PreviewCoverageTest`, `PreviewHygieneTest` and `PreviewsRenderTest` pass, with their lists unchanged (criterion 3c). Every screen preview now builds its UiState from sample data and never touches Koin, a ViewModel, `MediaStore`, the camera or the network (`PreviewHygieneTest`).

14. [machine] Every class and function in the app's code, new and moved files included, carries a plain-English comment saying what it does.
    Proof: `KdocCoverageTest` passes in criterion 1's run (it scans `app/src/main/java`). The Reviewer reads the new and moved test files for the same.

15. [machine-then-human] A beginner reading the code finds the "one state per screen" idea explained in plain words where it first appears.
    Proof: pre-check: `HomeUiState.kt` contains the words `snapshot`, `ViewModel` and `StateFlow`, none of the other five `…UiState.kt` repeats the full explanation but each points back to it, and no new non-screen file contains the text `@Composable` (`PreviewsRenderTest` would fail). The person then reads `ui/home/HomeUiState.kt`, `HomeViewModel.kt` and `HomeScreen.kt`, in that order: expect a paragraph a beginner can follow saying what a UiState is (one unchangeable snapshot of everything the screen shows), who produces it (the ViewModel, through its one StateFlow), why it is never edited in place (the screen asks through callbacks and a new snapshot replaces the old), how the screen reads it, and why a preview can build one from sample data. Then skim `SettingsViewModel.kt` for `combine` and `SavedViewModel.kt`: each new idea is explained where it is first used.

### Constraints carried over

16. [machine] The original reference app was not touched.
    Proof: `git diff --exit-code --stat a0d9a79 HEAD -- reference/` exits 0 with no output.

17. [machine] The app still has no ads, no analytics and no accounts.
    Proof: `git grep -n -I -i -E 'analytics|admob|firebase|billing|apero|AccountManager|GoogleSignIn' HEAD -- app/src app/build.gradle.kts gradle/libs.versions.toml` exits 1 (no match; verified 2026-10-09 it matches nothing today).

## Removals

Repository class: **product** (`PROJECT.md`). Only what the restructure replaces is removed.

R1. [machine] Settings no longer keeps the theme mode and the theme colour as two separate streams.
    Proof: `git grep -n -E 'val (themeMode|themeColour)\s*:\s*StateFlow' HEAD -- app/src/main/java/com/phongkaster/kanso/ui/settings` exits 1.

R2. [machine] The Saved screen's drawing file no longer loads the photo itself or defines its own state type.
    Proof: `git grep -n -E 'produceState|koinInject|(internal |private )?sealed interface' HEAD -- app/src/main/java/com/phongkaster/kanso/ui/saved/SavedScreen.kt` exits 1 (comments included, so the file's text does not even mention those names).

R3. [machine] Home no longer keeps its tool list apart from its screen state.
    Proof: `git grep -n -E 'val tools\b' HEAD -- app/src/main/java/com/phongkaster/kanso/ui/home/HomeViewModel.kt` exits 1.

R4. [machine] The Tool screen no longer has a separate save-status stream or a separate save-state type.
    Proof: `git grep -n -E 'val saveState\s*:\s*StateFlow|SaveUiState' HEAD -- app/src/main` exits 1 (comments included).

R5. [machine] The Camera screen no longer has a separate camera-status stream.
    Proof: `git grep -n -E 'val cameraState\s*:\s*StateFlow' HEAD -- app/src/main/java/com/phongkaster/kanso/ui/camera` exits 1.

R6. [machine] The single file that held all six navigation keys is gone, replaced by one file per key.
    Proof: `test ! -e app/src/main/java/com/phongkaster/kanso/navigation/Destinations.kt` exits 0, and `Home.kt`, `Camera.kt`, `Gallery.kt`, `ToolPhoto.kt`, `Saved.kt` and `Settings.kt` exist in that folder.

Questions (not removals; the PRD leaves them out of scope):
- Q1. The strings `gallery_body_placeholder` and `gallery_album_label` (`app/src/main/res/values/strings.xml`) are unused.
- Q2. The Saved screen's Share button calls an `onShare = {}` hook in the navigation host that does nothing; the share sheet is opened inside the screen.
- Q3. `GalleryScreen` has an `onOpenCamera` default that does nothing.
- Q4. `ViewModelClearedProbe` is app code that only tests use.

## Constraints

- `CLAUDE.md` applies in full: a preview for every composable, one class per file, one UiState per screen, Compose animation APIs, KDoc in plain English, no ads, analytics, accounts or network, `reference/` unchanged, the three debug ABIs.
- Behaviour and pixels stay the same: every existing baseline byte for byte, no test weakened or deleted (criteria 2, 3).
- Navigation 3 stays 1.1.7 and AGP 8.x; no dependency is added (criterion 9).
- Out of scope for this run: any new look or motion, the A13's speed problems, and the human checks waiting in `.harness/ISSUES.md`.

## Verification Evidence Required

| Criterion | Class | Evidence / what to look at | Signed off |
|---|---|---|---|
| 1 | machine | clean test run log + summed XML counts | n/a |
| 2 | machine | `verifyRoborazziDebug` exit 0; `git diff` of `app/src/test/screenshots` empty | n/a |
| 3 | machine | name list, `@Ignore`, previews/KDoc diff, assertion diff | n/a |
| 4 | machine | Saved tests + `SavedViewModelTest` | n/a |
| 5 | machine | `HomeViewModelTest` + baselines 1-3 | n/a |
| 6 | machine-then-human | device pre-check, then the person's walk of the route above | ☐ |
| 7 | machine | manifest diff empty; no INTERNET in merged manifest | n/a |
| 8 | machine | settings tests + `SettingsUiStateTest` + baselines 4-6 | n/a |
| 9 | machine | `assembleDebug`, `lintDebug`, build-file diff empty | n/a |
| 10 | machine | `OneTypePerFileTest` green, empty allow-list | n/a |
| 11 | machine | `ls` of 18 files; no UiState in ViewModel/Screen files | n/a |
| 12 | machine | `ScreenStateTest` | n/a |
| 13 | machine | the three preview tests | n/a |
| 14 | machine | `KdocCoverageTest` | n/a |
| 15 | machine-then-human | grep pre-check, then a person reads the three Home files | ☐ |
| 16 | machine | `git diff` of `reference/` empty | n/a |
| 17 | machine | grep finds nothing | n/a |
| R1-R6 | machine | the greps and the file test above | n/a |
