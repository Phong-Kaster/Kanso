# Definition of Done

> Derived from `PRD.md` at bootstrap. Human-owned after approval: the engine may propose changes (Tier 3) but never apply them.
> Every criterion is written twice (ADR-035): one plain sentence for you to approve, and a `Proof:` line holding what a command needs.

## Status

- [ ] APPROVED — approve via the pending `.harness/run/ESCALATION.md` (D-001); edit criteria freely before approving.
      Approval covers the Verification Class of each criterion, not only its wording.

## Acceptance Criteria

Classes: `machine` = a command proves it. `machine-then-human` = a command drives it first, a person still looks and signs. `human-only` = nothing can drive it.
"Custom colour" below always means the seed colour `#2E7D32` (green), set through the Settings colour picker.

### Build, start and quality

1. [machine] The project builds into an installable debug app called Kanso, with every engine module and OpenCV part of the build.
   Proof: `./gradlew :app:assembleDebug` succeeds from a clean checkout of the Loop Branch. `./gradlew projects` lists `:app`, `:opencv`, `:core:colorpicker`, `:core:common`, `:core:editimage`, `:core:gallery`, `:core:imagecrop`, `:core:imageprocessor`, `:core:localml`, `:core:overlaybrush`. `app/build.gradle.kts` depends on each of the eight `core` modules and on `:opencv`. `aapt dump badging` of the debug APK shows package `com.phongkaster.kanso`, label `Kanso`, `minSdkVersion:'27'`.
2. [machine] Starting the app from a fresh install shows the Home screen without crashing.
   Proof: a Robolectric/Compose test launches `MainActivity` with `ActivityScenario` from a fresh data directory and finds the Home screen's title node. Also driven once on an emulator through `foreman-device.ps1` (install, clear, start): no `FATAL EXCEPTION` for the package in its log, and the screen dump contains the Home tool list.
3. [machine] The unit tests and lint pass.
   Proof: `./gradlew :app:testDebugUnitTest :app:lintDebug` exits 0.

### Behaviour

4. [machine] Home lists the six tools — Edit, Remove background, Enhance, Retouch, Artistic, Camera — and a way into Settings.
   Proof: Compose test on a fresh launch finds exactly those six tool labels plus a Settings entry on Home, each with a click action.
5. [machine] Tapping any of the six tools opens a simple placeholder screen that names that tool, and Back returns to Home.
   Proof: Compose test, for each of the six tools in turn, starting from Home: click the tool → a node with that tool's name is shown inside the placeholder screen; press system Back (`Espresso.pressBack` / `OnBackPressedDispatcher`) → Home is shown again and the activity is not finished.
6. [machine] Settings opens from Home, and Back from Settings returns to Home.
   Proof: Compose test from Home: click the Settings entry → the Settings title is shown; Back → Home shown, activity not finished.
7. [machine] Pressing Back on Home leaves the app instead of doing nothing or looping.
   Proof: Compose test from a fresh launch: `Espresso.pressBackUnconditionally()` on Home → `ActivityScenario` state becomes `DESTROYED` (or the activity `isFinishing`).
8. [machine] Rotating the screen or any configuration change keeps the screen the person was on.
   Proof: Compose test: open the Edit placeholder, `ActivityScenario.recreate()`, the Edit placeholder is still shown and one Back returns to Home.
9. [machine] The app is one Activity, all Compose: no Fragments, no XML layouts.
   Proof: the merged debug manifest declares exactly one `<activity>` from the app's own package; `app/src/main/res/layout*` does not exist; `grep -rE "Fragment|setContentView|findViewById" app/src/main --include=*.kt` finds nothing outside comment lines (lines starting `//`, `*` or `/*`).
10. [machine] Navigation uses Navigation 3 with a back stack of typed keys, screen-scoped ViewModels, and Koin for dependency injection.
    Proof: `app/src/main` contains `rememberNavBackStack`, `NavDisplay`, `entryProvider`, a `@Serializable` `NavKey` type for each destination, and `koinViewModel`; `./gradlew :app:dependencies --configuration debugRuntimeClasspath` lists `androidx.navigation3:navigation3-runtime`, `androidx.navigation3:navigation3-ui`, `androidx.lifecycle:lifecycle-viewmodel-navigation3` and `io.insert-koin:koin-compose-navigation3`, and does **not** list `androidx.navigation:navigation-compose`. The versions are the latest stable ones found at bootstrap (2026-10-05: Navigation 3 1.2.0, lifecycle-viewmodel-navigation3 2.11.0, Koin 4.2.2); any lower pin the build forces is explained in `.harness/knowledge/PROJECT.md`. ViewModels are scoped to their screen: a Compose test opens a placeholder, which creates its ViewModel, then presses Back and asserts that ViewModel's `onCleared()` ran, while Home's ViewModel is still alive.
11. [machine] The app starts straight into Home through the system splash screen only: no onboarding, intro or splash screen of its own.
    Proof: the launcher activity's theme has the `Theme.SplashScreen` parent and `MainActivity` calls `installSplashScreen()`; `grep -riE "onboard|intro|welcome|SplashActivity" app/src/main` finds nothing outside comment lines; the start key of the back stack is Home.
12. [human-only] The splash is the plain system splash and Home appears right after it, with nothing in between (the system splash follows the phone's own light/dark setting, so it can differ from an app theme mode set the other way).
    Proof: install the debug build on a phone with the app's theme mode on System, kill the app, open it from the launcher; expect the system splash (app icon on a plain background), then Home, with no extra screen, logo animation or flash of another colour in between.

### Settings and appearance

13. [machine] Settings has a theme mode choice — follow the system, light or dark — and picking one changes the look of the whole app immediately.
    Proof: Compose test under Robolectric native graphics mode (`@GraphicsMode(NATIVE)`): Settings → pick Dark → `MaterialTheme.colorScheme.background` read inside the Settings content is dark (luminance < 0.5), and a pixel sampled from the captured screen agrees, without any restart; Back to Home → Home's background is dark; pick Light → both are light (luminance > 0.5). Pick System with the test device in night mode (`@Config(qualifiers = "night")`) → dark, and in `notnight` → light.
14. [machine] The chosen theme mode is still in force after the app is closed and opened again.
    Proof: Unit test on the settings store: write Dark, cancel the first store's coroutine scope (which releases the DataStore file), build a fresh store over the same file, read Dark from disk. Compose test: pick Dark, close the scenario, stop Koin and clear the in-memory singletons, launch a new `MainActivity` over the same file → Home is dark.
15. [machine] Settings has a theme colour choice — from the wallpaper (Android 12 and later) or a colour the person picks — and a picked colour produces a complete Material 3 colour scheme for the whole app.
    Proof: Unit test on the scheme generator with seed `#2E7D32`: light and dark `ColorScheme` each have every Material 3 role set (primary, onPrimary, primaryContainer, onPrimaryContainer, secondary… tertiary…, error…, background, surface, surfaceVariant, outline, inverse…, surface containers) with no two of the roles primary/secondary/tertiary/error equal; `primary` of the light scheme differs from the baseline purple `#6750A4`; every `on*` role has contrast ratio ≥ 3:1 against its pair. Compose test: set the custom colour `#2E7D32` through the same callback the picker uses (`SettingsViewModel`'s custom-colour function; the picker itself is not driven because its hex field cannot take typed input reliably, see Known Risks) → `MaterialTheme.colorScheme.primary` on Home equals the generated one, in light and in dark mode.
16. [machine] The "from the wallpaper" option is offered only on Android 12 and later, and a picked colour is applied immediately everywhere.
    Proof: Compose test with `@Config(sdk = [30])`: the Settings colour section has no wallpaper option. With `@Config(sdk = [35])`: it has one, and choosing it sets the colour source to Wallpaper in the settings state, which `KansoTheme` maps to `dynamicLightColorScheme`/`dynamicDarkColorScheme`. With custom colour picked on Settings, navigating Back to Home shows the generated `primary` without a restart.
17. [machine] The chosen theme colour (wallpaper or custom, including which custom colour) is still in force after the app is closed and opened again.
    Proof: Unit test on the settings store as in 14 for both options and for seed `#2E7D32`; Compose test: pick custom `#2E7D32`, close, relaunch → Home's `primary` equals the generated one.
18. [machine] The custom colour is picked with the engine's colour picker, which opens from the Settings colour section.
    Proof: `app/build.gradle.kts` depends on `:core:colorpicker` and `app/src/main` calls `HSVColorPicker` from `com.library.colorpicker`, passing an `onColorChanged` that calls the same `SettingsViewModel` function the tests in 15 use; a Compose test picks the Custom option and finds a node tagged `custom-colour-picker` on screen.
    Default colour on a fresh install (assumption A-002, Tier 2): Wallpaper on Android 12+, the built-in Material 3 baseline purple `#6750A4` below that.
19. [machine-then-human] On Android 12 and later, choosing "from the wallpaper" gives the app the colours of the phone's wallpaper, and a different wallpaper gives different colours.
    Proof: driven on an Android 12+ emulator via `foreman-device.ps1` from a fresh install with the default theme colour: choose the wallpaper option, read the screen; the person then changes the wallpaper and confirms the app's accent colour follows. The driven part can only show the option applies without crashing; it cannot judge the colours.

### Screens, looks and reachability

20. [machine] The Home, Settings and placeholder screens each match a committed reference image in light mode, in dark mode, and with the custom colour applied.
    Proof: `./gradlew :app:verifyRoborazziDebug` exits 0 with these nine reference images present in `app/src/test/screenshots/`, each recorded from the state named: `1_home_light.png`, `2_home_dark.png`, `3_home_custom_colour.png`, `4_settings_light.png`, `5_settings_dark.png`, `6_settings_custom_colour.png`, `7_placeholder_light.png` (Edit), `8_placeholder_dark.png` (Edit), `9_placeholder_custom_colour.png` (Edit). Light/dark are forced through Settings theme mode; images 1, 2, 4, 5, 7, 8 run at `@Config(sdk = [30])` so the default colour is the fixed baseline purple, and images 3, 6, 9 run at `sdk = [35]` with custom colour `#2E7D32` in light mode. Each image was opened and judged by the engine before being committed (POLICIES.md § User-Interface Defects).
21. [human-only] Every screen looks minimal and calm, with no decoration a screen does not need, every control can be seen and found against its background, and the colour picker is usable and readable in both light and dark mode.
    Proof: on a phone, open Home, each of the six tools, Settings; do this in light, dark and with a picked colour. In Settings open the custom colour picker, drag to choose a colour, and read its hex field in dark mode. Expect: plain Material 3 surfaces, readable text, every tool row and every Settings control clearly visible, the picker's text readable (not black on dark), no ornament, banner or empty decorative block.
22. [machine-then-human] Back works the way a person expects with the real system gesture: from a tool or Settings it goes back to Home, from Home it leaves the app.
    Proof: driven on an emulator with `foreman-device.ps1` (open a tool, back key, expect Home in the screen dump; back again, expect the launcher). The person then does the same with the phone's back gesture.

### Constraints

23. [machine] The app asks for no network access: no internet permission in the debug or release manifest.
    Proof: the merged debug and release manifests (`app/build/intermediates/merged_manifests/*/AndroidManifest.xml`) contain neither `android.permission.INTERNET` nor `ACCESS_NETWORK_STATE`. A library such as ML Kit that merges one in has it removed with `tools:node="remove"` unless that breaks the engine; any such exception is written in `.harness/knowledge/PROJECT.md` and reported in the completion summary. Neither this run's screens nor any code of the app uses the network.
24. [machine] Nothing from Apero, Firebase analytics/messaging/config, or Google Play ads, billing, review or updates is in the app, and no private Maven repository or credential is in any file.
    Proof: `./gradlew :app:dependencies` over the debug and release runtime classpaths lists no coordinate containing `apero`, `firebase-analytics`, `firebase-messaging`, `firebase-config`, `firebase-crashlytics`, `firebase-perf`, `com.google.android.play:`, `play-services-ads`, `billing`, `crashlytics`. (ML Kit, which the engine's `core/localml` needs, brings the Firebase infrastructure libraries `firebase-components` / `firebase-encoders` and Play-services model loading with it; these are allowed, listed in `.harness/knowledge/PROJECT.md`, and none of that code runs this run because no tool uses ML yet — assumption A-003.) `grep -rniE "apero|firebase|crashlytics|google-services|credentials|maven\.pkg|artifactory|password" settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml app core/*/build.gradle.kts opencv/build.gradle` finds nothing; every `maven {}` block in the Gradle files, if any, points only at `google()`/`mavenCentral()` style public repositories with no `credentials`.
25. [machine] Every class, function and composable in the app code has a plain-English KDoc comment.
    Proof: a unit test in `app/src/test` (`KdocCoverageTest`, written in the first task and run every iteration) walks `app/src/main/java/**/*.kt` and fails if any `class`, `object`, `interface` or `fun` (private ones included; `override` functions excluded) is not directly preceded by a `/** … */` comment of at least one sentence; it passes, and it also passes a negative self-test (a sample source without KDoc is reported). The Navigation 3 and Compose concept comments are judged in 26. Every task from T-001 on writes its KDoc and concept comments as part of its own acceptance.
26. [human-only] The code explains itself to someone who has never used Navigation 3: each Navigation 3 or Compose concept is explained where it is first used, and non-obvious lines have a short comment.
    Proof: open `app/src/main/java/com/phongkaster/kanso/navigation/` and the Home and Settings screen files. Expect: the first use of the back stack, `NavDisplay`, entries/`entryProvider`, ViewModel scoping per entry, `remember`/state and the theme are each explained in plain English, enough to follow without outside reading.
27. [machine] `reference/` is byte-for-byte unchanged, and nothing from it is built, imported or copied into the app.
    Proof: `git diff --stat a0d9a79 HEAD -- reference` prints nothing; `grep -rn "retica-app\|reference/" settings.gradle.kts build.gradle.kts app/build.gradle.kts` finds nothing; no file in `app/src` imports a package from `reference/retica-app/java` (`com.photo.editor.ui`, `.platform`, `.data`).

## Removals

Repository class (from PROJECT.md): **product** — the engine import is real code the later runs rely on; there are no demo screens. Only what the PRD makes unnecessary is removed; nothing else looks unused.

R1. [machine] The engine's common module no longer pulls in Firebase analytics.
    Proof: `core/common/build.gradle.kts` has no `firebase`; `grep -rn "com.google.firebase" core opencv app --include=*.kt --include=*.kts` finds nothing; `core/common/.../AnalyticsUtil.kt` is either deleted or a plain local logging function with no Firebase import.
R2. [machine] The version catalog no longer lists libraries and plugins the app does not use: Apero, Firebase, Crashlytics, Google services, Play in-app update and review, Ktor, Retrofit, OkHttp logging, Room, ML Kit barcode/document scanner, Fragment-based navigation, and the baseline-profile and distribution plugins.
    Proof: `grep -nE "apero|firebase|crashlytics|google-service|inappupdate|review|ktor|retrofit|converter-gson|logging-interceptor|room|barcode|document|navigation-fragment|navigation-ui|navigation-runtime|navigation-compose|koin-navigation|koin-ktor|koin-startup|baselineprofile|distribution|safeargs" gradle/libs.versions.toml` finds nothing, and every remaining `[libraries]` and `[plugins]` alias is used as `libs.<alias>` (dots for dashes) or `alias(libs.plugins.<alias>)` in some `build.gradle*` / `settings.gradle.kts` file, checked by a shell loop over the aliases, with `./gradlew` configuring cleanly. Entries that the engine modules still build with (CameraX, ML Kit pose/face/segmentation, LiteRT, Coil, Lottie, Compose, colormath and so on) stay.
R3. [machine] The app contributes none of Retica's permissions or services (notifications, boot receiver, full-screen intent, camera, media storage) and no Firebase messaging service.
    Proof: the merged debug manifest declares none of `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `USE_FULL_SCREEN_INTENT`, `CAMERA`, `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` (the Camera tool is only a placeholder this run), and no `<service>` or `<receiver>` of the app's own. Permissions that only a reused engine library merges in are listed in `.harness/knowledge/PROJECT.md`.

Questions: none.

## Constraints

- No ads, no analytics, no accounts, no network. Nothing from Apero, no Firebase, no Play in-app update/review/billing, no private Maven repository, no credentials in any file.
- Package and applicationId `com.phongkaster.kanso`, app name `Kanso`, minSdk 27.
- One Activity, all Compose, Navigation 3 + Koin; latest stable releases. Material 3 only: no `androidx.compose.material.` (Material 2) import in `app/src/main`, checked by grep outside comments.
- `reference/` is read-only; never edit, build or import from it.
- Engine modules are reused as they are: only their build scripts, and the single Firebase use in `core/common` (R1), may change.
- Out of scope: gallery, camera, export, the editing tools and the AI tools themselves.

## Verification Evidence Required

| Criterion | Class | Evidence / what to look at | Signed off |
|---|---|---|---|
| 1 | machine | `./gradlew :app:assembleDebug`, `projects`, `aapt dump badging` | n/a |
| 2 | machine | launch test, plus emulator log and screen dump | n/a |
| 3 | machine | `./gradlew :app:testDebugUnitTest :app:lintDebug` | n/a |
| 4–8 | machine | Compose navigation tests | n/a |
| 9–11 | machine | manifest, grep and dependency report | n/a |
| 12 | human-only | cold start from the launcher: system splash, then Home | ☐ |
| 13–18 | machine | settings, scheme-generator and persistence tests | n/a |
| 19 | machine-then-human | wallpaper option on an Android 12+ emulator, then the person changes wallpaper | ☐ |
| 20 | machine | `./gradlew :app:verifyRoborazziDebug`, nine named reference images | n/a |
| 21 | human-only | look at every screen in light, dark and picked colour | ☐ |
| 22 | machine-then-human | back key on emulator, then the back gesture on a phone | ☐ |
| 23–25, 27 | machine | manifests, dependency lists, greps, `KdocCoverageTest`, `git diff` | n/a |
| 26 | human-only | read the navigation and screen files | ☐ |
| R1–R3 | machine | greps and merged manifest | n/a |
