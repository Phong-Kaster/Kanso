# Definition of Done

> Derived from `PRD.md` (Run 3: a preview for every composable) at bootstrap. Human-owned after approval: the engine may propose changes (Tier 3) but never apply them.
> Base for every "unchanged" check below: commit `9a61275` (the tip of the previous run, before this run's first change).

## Status

- [ ] APPROVED — approve via the pending `.harness/run/ESCALATION.md` (D-001); edit criteria freely before approving.
      Approval covers the Verification Class of each criterion, not only its wording, and the one new test-only
      library named under Constraints.

## Acceptance Criteria

### Behaviour

1. [machine] Every screen and every piece of a screen in the app has a preview, in light and in dark, in the same file.
   Proof: a new unit test `PreviewCoverageTest` reads every `@Composable` in `app/src/main`; each is either a preview itself, drawn by a preview in its file (directly, or through another composable of that file that a preview draws), or a thin stateful wrapper whose `…Content` part is. Every preview is `@PreviewLightDark` or a pair of `@Preview` (one with `UI_MODE_NIGHT_YES`). `./gradlew :app:testDebugUnitTest --tests '*PreviewCoverageTest'` exits 0, and the test's self-check proves it reports a composable that has no preview.
2. [machine] Every preview draws without an error, in light and in dark.
   Proof: a new unit test `PreviewsRenderTest` finds every preview in the app's compiled code (ComposablePreviewScanner, private ones included) and draws each one in a Robolectric host with Koin **not started**; it fails on any exception, and its self-check proves a preview that throws is reported. `./gradlew :app:testDebugUnitTest --tests '*PreviewsRenderTest'` exits 0 and the test asserts at least one preview exists per file that holds a composable.
3. [machine] The states worth looking at have their own preview: the gallery with photo access refused and with only some photos shared, the camera before and after a shot (Keep and Retake), the camera and gallery refused panels, the tool screen with a photo, the Saved screen, Settings with each theme mode and with a wallpaper and a custom colour picked, Home.
   Proof: `PreviewsRenderTest` asserts these preview names exist: `GalleryPhotosPreview`, `GalleryPartialAccessPreview`, `GalleryRefusedPreview`, `GalleryEmptyPreview`, `CameraLivePreview`, `CameraAfterShotPreview`, `CameraRefusedPreview`, `ToolPhotoPreview`, `ToolUnreadablePreview`, `SavedPreview`, `SettingsSystemModePreview`, `SettingsLightModePreview`, `SettingsDarkModePreview`, `SettingsWallpaperColourPreview`, `SettingsCustomColourPreview`, `HomePreview`, `AccessRefusedPanelPreview`, `KansoNavHostPreview`.
4. [machine] A screen that needs the app's wiring, a ViewModel, the phone's photos, the camera or a permission dialog is split in two, and its preview shows only the part that needs none of them.
   Proof: `PreviewHygieneTest` (new) scans every preview function and every `sample…` helper in `app/src/main` and fails if one mentions `koin`, `ViewModel`, `MediaStore`, `PhotoLoader`, `CameraController`, `CameraX`, `PreviewView`, `rememberLauncherForActivityResult` or `LocalContext.current.contentResolver`; and `PreviewsRenderTest` asserts Koin is not running while it draws.
5. [machine] Sample data made for previews is never reachable from code the app runs.
   Proof: `PreviewHygieneTest` asserts every top-level `sample…` / `Sample…` declaration in `app/src/main` is `private` and is referenced only from preview functions or from other `sample…` declarations in the same file.
6. [machine] The preview tools reach the debug build and the code can use the preview annotations, but the app's release build carries no preview tooling.
   Proof: `app/build.gradle.kts` has `implementation(libs.compose.ui.tooling.preview)` and `debugImplementation(libs.compose.ui.tooling)` and nothing else of `ui-tooling`; `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` lists `androidx.compose.ui:ui-tooling-preview` and no `androidx.compose.ui:ui-tooling:` entry; `./gradlew :app:dependencies --configuration debugRuntimeClasspath` lists `androidx.compose.ui:ui-tooling:`.
7. [machine-then-human] In Android Studio, opening any screen file and its Preview pane shows each preview, once in light and once in dark, and each looks like the screen it stands for.
   Proof: driven first by criterion 2 (every preview draws on the host without error); a person then opens `GalleryScreen.kt`, `CameraScreen.kt`, `SavedScreen.kt`, `ToolScreen.kt`, `SettingsScreen.kt`, `HomeScreen.kt`, `PermissionGate.kt`, `AccessRefusedPanel.kt`, `KansoNavHost.kt` and `KansoTheme.kt` in Android Studio, opens the Preview pane (Split view), and checks the pictures are readable, show sample content that looks real, and that dark is really dark. A host render passing says nothing is thrown; it does not say the picture is right.

### Behaviour and appearance stay the same

8. [machine] The app's existing tests are not changed, and they all pass.
   Proof: `git diff --name-status 9a61275..HEAD -- app/src/test` lists only `A` (added) lines, no `M`, `D` or `R`; `./gradlew :app:testDebugUnitTest` exits 0 (rerun once if the known `ThemeColourSettingsTest` Windows rename flake appears).
9. [machine] Every existing screen picture is byte-for-byte what it was.
   Proof: `./gradlew :app:verifyRoborazziDebug` exits 0 and `git diff --exit-code 9a61275 -- app/src/test/screenshots` prints nothing (the 16 baselines `1_home_light.png` … `19_saved_dark.png` are the reference images; none is re-recorded).
10. [machine-then-human] On the phone the app looks and behaves as it did after Run 2: Home, the gallery, a tool with a photo, Save and the Saved screen, Settings with the colour picker, and the camera.
    Proof: driven first, from a fresh install of the debug build with permissions not yet granted, through `foreman-device.ps1` on the attached Galaxy A13 or emulator, where the device allows (written as "did not fail when driven from a fresh install", never as "works"); the camera is not pinned by any picture, so a person must open Camera from Home, see the live picture, take a shot and see Keep and Retake, then compare Home, gallery, tool, Saved and Settings with how they were.

### Code that teaches

11. [machine] Every class, function and composable, previews and the new `…Content` composables included, has a plain-English KDoc comment.
    Proof: the existing `KdocCoverageTest` passes unchanged in `./gradlew :app:testDebugUnitTest`.
12. [human-only] State hoisting (the stateful part that gathers state, the stateless `…Content` part that takes it) is explained in plain English where it is first used, for a reader who has never met it, and a preview's sample data is explained where it is made.
    Proof: read the first screen split in this run (the one the run's commit log names first, `HomeScreen.kt`) and then one more (`GalleryScreen.kt`); expect a paragraph a beginner can follow that says why the screen is two composables, what each does, and why that makes a preview possible.

### Build, start and quality

13. [machine] The app builds, passes lint, and the whole test suite passes.
    Proof: fresh, separate runs of `./gradlew :app:assembleDebug`, `./gradlew :app:testDebugUnitTest`, `./gradlew :app:verifyRoborazziDebug` and `./gradlew :app:lintDebug` all exit 0.
14. [machine] The constraints of the project hold: Navigation 3 stays at 1.1.7, the Android Gradle Plugin stays 8.x, the debug build keeps its three ABIs, `reference/` is untouched, and the app still has no network permission, ads, analytics or accounts.
    Proof: `gradle/libs.versions.toml` has `nav3 = "1.1.7"` and `agp = "8.13.0"`; `app/build.gradle.kts` still lists `x86_64`, `arm64-v8a` and `armeabi-v7a`; `git diff --exit-code a0d9a79 -- reference` prints nothing; the merged debug manifest declares neither `INTERNET` nor `ACCESS_NETWORK_STATE`; `grep -rniE "firebase|admob|analytics|play-services-ads" app/build.gradle.kts gradle/libs.versions.toml` (excluding the existing engine entries recorded in `.harness/knowledge/PROJECT.md`) finds no new entry.

## Removals

Repository class: **product**. This run adds previews and restructures screens; the PRD makes nothing the app ships unnecessary, so there is nothing to remove. (An existing code path that this run replaces — a screen's body moving into its `…Content` part — is moved, not left behind: the old body is not kept beside the new one.)

Questions (product repositories only): none.

## Constraints

- `CLAUDE.md` applies to every change: previews in light and dark in the same file, built from sample data; never touching Koin, a ViewModel, `MediaStore`, the camera or the network; KDoc everywhere; no ads, analytics, accounts or network permission; `reference/` read-only; three debug ABIs.
- Behaviour and pixels stay the same: existing tests are untouched, existing baselines byte-for-byte. A baseline that moves is a defect, not an approval request.
- Navigation 3 1.1.7, AGP 8.x.
- **One new test-only library, for approval at this gate:** `io.github.sergio-sastre.ComposablePreviewScanner:android` (latest stable at bootstrap: 0.9.3), `testImplementation` only, so nothing of it ships in the APK. It finds every `@Preview` in the compiled code so criterion 2 can prove all of them draw; without it a preview someone forgets to list is a preview nobody tests. If you refuse it, the engine falls back to a hand-kept list of previews in the test plus the source scan of criterion 1, which proves less (criterion 2 then covers only the listed previews).
- `androidx.compose.ui:ui-tooling-preview` becomes an `implementation` dependency (a small annotation library the Compose BOM already versions); the PRD asks for the annotations to be available to the main code.

## Verification Evidence Required

| Criterion | Class | Evidence / what to look at | Signed off |
|---|---|---|---|
| 1 | machine | `testDebugUnitTest --tests '*PreviewCoverageTest'` green | n/a |
| 2 | machine | `testDebugUnitTest --tests '*PreviewsRenderTest'` green, Koin stopped | n/a |
| 3 | machine | named previews asserted by `PreviewsRenderTest` | n/a |
| 4 | machine | `PreviewHygieneTest` green | n/a |
| 5 | machine | `PreviewHygieneTest` green | n/a |
| 6 | machine | `:app:dependencies` for release and debug classpaths; `app/build.gradle.kts` lines | n/a |
| 7 | machine-then-human | open each listed file in Android Studio Split view; pictures are readable, look real, dark is dark | ☐ |
| 8 | machine | `git diff --name-status 9a61275..HEAD -- app/src/test` only `A`; full unit-test run | n/a |
| 9 | machine | `verifyRoborazziDebug` + `git diff --exit-code 9a61275 -- app/src/test/screenshots` | n/a |
| 10 | machine-then-human | device pre-check from a fresh install, then a person walks the screens and the camera | ☐ |
| 11 | machine | `KdocCoverageTest` green | n/a |
| 12 | human-only | read `HomeScreen.kt` and `GalleryScreen.kt`: the split is explained for a beginner | ☐ |
| 13 | machine | assembleDebug, testDebugUnitTest, verifyRoborazziDebug, lintDebug exit 0 | n/a |
| 14 | machine | the greps and diffs listed in the Proof | n/a |
