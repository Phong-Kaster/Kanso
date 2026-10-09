# WORKER BRIEF - T-003 (round 1)

You are the Capable Worker for **one new file**: `app/src/test/java/com/phongkaster/kanso/ExpressiveApiTest.kt`.
You hold no git, build or test capability: you create files and report. The Iteration compiles and runs the tests
and will hand you the compiler or test error text for anything you missed (a resumed round counts as an attempt).

## Declared File Scope (the only file you may write)

- `app/src/test/java/com/phongkaster/kanso/ExpressiveApiTest.kt` (new). Writing anywhere else is a violation.
  You may *read* anything in the repository. Other tasks: T-001 and T-002 are complete; none is in progress.

## Why this file exists

The app now resolves **Material3 1.5.0-beta01** (the Iteration already wired the catalog and `app/build.gradle.kts`;
do not touch them). The test proves that the Expressive components compile against that version and actually draw,
behind the opt-in the library requires. Nothing in `app/src/main` uses an Expressive component (the new look is the next run).

## What the test must do

A Robolectric Compose test, one class `ExpressiveApiTest`, in package `com.phongkaster.kanso`, with
`@OptIn(ExperimentalMaterial3ExpressiveApi::class)` on the class (`androidx.compose.material3.ExperimentalMaterial3ExpressiveApi`).
In 1.5.0-beta01 `LoadingIndicator`, `MaterialShapes` and `RoundedPolygon.toShape()` are still marked experimental; `ButtonGroup`,
`HorizontalFloatingToolbar` and the wavy progress indicators no longer are, but the whole class sits behind the opt-in anyway.

Follow the style of the existing tests, for example `app/src/test/java/com/phongkaster/kanso/theme/KansoThemeTest.kt`:

```
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ExpressiveApiTest {
    /** Hosts the composable under test in an empty activity. */
    @get:Rule
    val compose = createComposeRule()
    ...
}
```

(`androidx.compose.ui.test.junit4.createComposeRule`, `androidx.test.ext.junit.runners.AndroidJUnit4`, `org.robolectric.annotation.Config`,
`org.robolectric.annotation.GraphicsMode`, `org.junit.Rule`, `org.junit.Test`, `org.junit.runner.RunWith`.)
Compose 1.12 deprecates `createComposeRule` with a compiler warning; that is fine and matches every other test in the repository.

Write at least these six tests, each of which composes **inside `compose.setContent { ... }` only** and asserts the thing is displayed
(`onNodeWithTag("...").assertIsDisplayed()` or `onNodeWithText("...").assertIsDisplayed()`; give nodes `Modifier.testTag(...)` where there is no text):

1. `ButtonGroup`: `ButtonGroup(overflowIndicator = { }) { clickableItem(onClick = { }, label = "Edit"); clickableItem(onClick = { }, label = "Share") }`
   (the members of `ButtonGroupScope` are `clickableItem(onClick, label, icon, weight, enabled)`, `toggleableItem(checked, label, onCheckedChange, ...)`
   and `customItem(buttonGroupContent, menuContent)`); assert both labels are displayed.
2. `HorizontalFloatingToolbar(expanded = true) { Text("Toolbar") }`
   (signature: `HorizontalFloatingToolbar(expanded: Boolean, modifier, colors, contentPadding, scrollBehavior, shape, leadingContent, trailingContent, ..., content: RowScope.() -> Unit)`);
   assert its text is displayed.
3. `LoadingIndicator(modifier = Modifier.testTag("loading"))` (indeterminate, it morphs between shapes forever); assert displayed.
4. `MaterialShapes`: for example `Box(Modifier.size(48.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(Color.Red).testTag("shape"))`
   (`MaterialShapes.Cookie9Sided`, `Sunny`, `Circle`, `Pill` are `RoundedPolygon`s in a companion object; `toShape()` is `androidx.compose.material3.toShape`
   and is a composable extension, legal inside the `setContent` lambda); assert the node is displayed.
5. A wavy progress indicator: `LinearWavyProgressIndicator(progress = { 0.5f }, modifier = Modifier.testTag("wavy"))`, and also
   `CircularWavyProgressIndicator(progress = { 0.5f }, modifier = ...)`; assert displayed. (In 1.5.0-beta01 these are the names;
   each has a determinate form taking `progress: () -> Float` and an indeterminate form with no `progress`.)
6. One test that puts all five in a single `setContent` lambda (a `Column` holding a button group, a floating toolbar, a loading indicator, the shape and the wavy indicator), so the five names appear together, and asserts each is displayed.

**Infinite animations must not hang the test** (Run 3: a spinner hung a render). `LoadingIndicator` and the wavy indicators animate forever.
In every test set `compose.mainClock.autoAdvance = false` **before** `compose.setContent { ... }`, then move time by hand with
`compose.mainClock.advanceTimeBy(500)` and assert. Do **not** write a helper that takes a content lambda (it would need the composable annotation text, which
must not appear in the file, see below): repeat the three lines (`autoAdvance = false`, `setContent { ... }`, `advanceTimeBy(500)`) in each test, with a short comment saying why the clock is paused.

## Hard rules for this file (the DoD and the guard tests grep it)

- **One top-level type**, `class ExpressiveApiTest`, in a file with that name. No second top-level class, object, enum or interface; no top-level `val` or `fun`.
- **A KDoc comment (`/** ... */`, plain English, saying what and why) on the class and on EVERY `fun` in it**, immediately above the function; `@Test` goes between the KDoc and the `fun`, which is fine. This awk must print nothing for the file:
  `awk '/^\s*@/ {next} /^\s*$/ {next} /^\s*((private|internal) )?(class|fun) / { if (prev !~ /\*\/\s*$/) print FILENAME ":" NR ": no KDoc" } {prev=$0}'`
  (it skips annotation lines and blank lines, so the previous non-annotation, non-blank line before `class` or `fun` must end with the closing of the KDoc comment). Put the KDoc above the `@OptIn` and `@RunWith` annotations of the class.
- **No composable function of your own, and the raw text of the composable annotation must not appear anywhere in the file**, not in a comment or KDoc either (the guard
  `PreviewsRenderTest.every_file_with_a_composable_has_a_preview` matches that raw text and then demands a preview). Compose inside `setContent { }` lambdas only; say "composable" in lower case in prose.
  Do not name any declaration starting with `sample` or `Sample`.
- **Never call `captureRoboImage`** or import anything from `io.github.takahirom.roborazzi`: the baseline recorder would add a 17th picture.
- It must not touch Koin, a view model, the media store, the camera or the network. Do not write the words Koin, ViewModel or MediaStore in the file at all.
- Keep every string and comment in the file free of double-quote characters inside text, dollar signs, and unbalanced braces (`OneTypePerFileTest`, `KdocCoverageTest`, `PreviewHygieneTest` scan test files with simple lexers). Use plain words in strings: Edit, Share, Toolbar.
- Never write a block-comment opener inside a KDoc or comment without its closer (Kotlin block comments nest); do not put a slash followed by a star inside any comment.
- Comment density like the surrounding tests: a KDoc on the class and each function; a short comment on a non-obvious line.
- Plain English, as the code teaches: the owner is learning Compose from this code. 4-space indent.
- Use only imports that exist on the test classpath: `androidx.compose.material3.*`, `androidx.compose.foundation.*`, `androidx.compose.foundation.layout.*`, `androidx.compose.ui.*`, `androidx.compose.ui.test.*`, `androidx.compose.ui.graphics.Color`, `androidx.compose.ui.draw.clip`, `androidx.compose.ui.unit.dp`, `androidx.compose.runtime.*`, JUnit 4, Robolectric. Prefer explicit imports over star imports (the neighbouring tests use explicit imports).

## API of Material3 1.5.0-beta01 (read from its sources; you cannot read the jar)

- `ButtonGroup(overflowIndicator: @Composable (ButtonGroupMenuState) -> Unit, modifier, expandedRatio, horizontalArrangement, verticalAlignment, content: ButtonGroupScope.() -> Unit)`
- `HorizontalFloatingToolbar(expanded: Boolean, modifier, colors, contentPadding, scrollBehavior, shape, leadingContent, trailingContent, expandedShadowElevation, collapsedShadowElevation, content: RowScope.() -> Unit)` (also an overload with `floatingActionButton`)
- `LoadingIndicator(modifier, color, polygons)` and `LoadingIndicator(progress: () -> Float, modifier, color, polygons)`, both experimental
- `LinearWavyProgressIndicator(progress: () -> Float, modifier, color, trackColor, stroke, trackStroke, gapSize, stopSize, amplitude, wavelength, waveSpeed)` and the indeterminate form without `progress`
- `CircularWavyProgressIndicator(progress: () -> Float, modifier, color, trackColor, stroke, trackStroke, gapSize, amplitude, wavelength, waveSpeed)` and the indeterminate form
- `MaterialShapes` (sealed class, experimental) with a companion holding `Circle`, `Pill`, `Sunny`, `Cookie9Sided`, ...: each a `RoundedPolygon`; `RoundedPolygon.toShape(startAngle: Int = 0): Shape` is a composable extension (experimental)

## Constraints from `.harness/knowledge/PROJECT.md`, verbatim and unfiltered

- **Never** edit anything under `reference/` or import from it, because the PRD makes it read-only and DoD 27 diffs it against `a0d9a79`.
- **Never** add INTERNET or ACCESS_NETWORK_STATE to the app or leave one merged in, because the PRD says the app has no network; use `tools:node="remove"` — evidence: DoD 23.
- **Never** type into or rely on `core/colorpicker`'s `HSVColorPicker` hex field: it parses the typed hex with `toLong()` as base 10 (`core/colorpicker/src/main/java/com/library/colorpicker/HSVColorPicker.kt:67`). Its text is hard-coded black (around line 106), so on a dark surface wrap it in a light container.
- **Never** edit an engine module's source apart from `core/common/.../AnalyticsUtil.kt` (A-001); build scripts only as far as the build requires.
- **Never** use `androidx.compose.material.` (Material 2) in `app`.
- **Never** write the text `@Composable` in a `app/src/main` file that holds no composable and no preview, KDoc included: `PreviewsRenderTest.every_file_with_a_composable_has_a_preview` matches the raw text and then demands a preview in that file — evidence: `app/src/test/java/com/phongkaster/kanso/previews/PreviewsRenderTest.kt:97-98`. A new UiState or ViewModel file must say "the screen function", not the annotation.
- **Never** leave a second top-level type in a file, or a type in a file not named after it (CLAUDE.md, Run 4): a helper type in a test nests inside the test class or gets its own file. `OneTypePerFileTest` enforces it from Run 4 on.
- **Never** start Gradle from a shell whose working directory is inside `app/build` (for example after a `cd app/build/test-results/...` to read test XML): on Windows the directory is locked and `:app:cleanTestDebugUnitTest` fails with `Unable to delete directory`. Read result files by relative path from the repository root, or begin every Gradle command with `cd /d/Valve/Personal/Kanso &&` — evidence: Run 4 Phase 2 attempt 1, `.harness/run/evidence/p2-verify-attempt1-cwd-lock.log`.
- **Never** write a block-comment opener inside a KDoc or comment without its closer: Kotlin block comments nest, so the file stops compiling (`KdocCoverageTest.kt:14` shows a nested pair). In a Kotlin raw-string test sample write a literal three-quote run as the `TRIPLE_QUOTE` constant and a literal dollar-brace as `${'$'}{`.
- **Never** commit a raw device dump, screenshot or logcat from the owner's phone: a gallery or tool screen lists the owner's photo and album file names, and `foreman-device.ps1 -Op dump` can capture the notification shade (see below). Redirect raw output outside the repository, read it there, and keep only a written summary in `evidence/`. Before redirecting a `dump` into a file, check that it is the app: `grep -o 'package="[^"]*"' <file> | sort -u` must name only `com.phongkaster.kanso` (or the permission dialog); delete it at once if it names `com.android.systemui` alone. Delete files outside the repository with `foreman-trash.ps1` (a plain `rm -rf /tmp/...` is denied) — evidence: Run 4 Verifier, Iteration 5.
- **Never** raise the catalog entry `compose-material3` (1.3.2) to reach Material3 1.5.0-beta01: `core/*` consume that entry at compileSdk 35/36, and Material3 1.5.0-beta01 declares `minCompileSdk=37` (PRD Run 5), which `checkDebugAarMetadata` rejects in such a module, as it did for Navigation 3 1.2.0 in Run 1 (pin line below). The app takes Material3 1.5.0-beta01 through its own catalog alias for the same coordinate (Run 5, A-001).
- **Never** apply `org.jetbrains.kotlin.android` (alias `kotlinAndroid`) or set an AGP opt-out flag (`android.builtInKotlin=false`, `android.newDsl=false`, `android.newDsl.optOut=...`, `android.uniquePackageNames=false`, or any other `android.*` line added to hold an AGP 8 behaviour) in any build file: AGP 9 compiles Kotlin itself, and the approved DoD 11 and R1 forbid a flag and the plugin — evidence: Run 5 Phase 1, `.harness/run/evidence/phase1-modules.log`.
- **Never** keep the Compose compiler plugin or the serialization plugin off the catalog's `kotlin` version: with `kotlin-android` gone they are what lifts the Kotlin Gradle plugin from AGP 9.4.1's built-in default 2.2.10 to the catalog's (`buildEnvironment`: `kotlin-gradle-plugin:2.2.10 -> 2.4.21` since Phase 2, `-> 2.3.20` in Phase 1); DoD 9 wants the catalog's Kotlin to be the one that compiles — evidence: `.harness/run/evidence/buildenv.txt` (the Phase 1 copy is in commit `d816bde`; the file now holds Phase 2's, as does `phase2-buildenv.log`).
- **Never** give `core/editimage`, `core/overlaybrush`, `core/imageprocessor` or `core/localml` the namespace of the prebuilt AAR each one wraps (`com.photo.editor.editimage`, `com.photo.editor.overlaybrush`, `com.photoeditor.opengl`, `com.photo.editor.localml`), and **never** declare the empty artifact `com.google.ai.edge.litert:litert-support` (use `litert-support-api` directly, as `core/localml` does): AGP 9 refuses one namespace in two modules or libraries and the manifest merge of the app fails. The four modules have no `src/` (empty wrappers of `libs/*.aar`), so their namespace is a build-script line (`com.phongkaster.kanso.engine.<module>`), not engine source; the litert shim has no classes and shares `org.tensorflow.lite.support` with `litert-support-api` (1.4.2 too) — evidence: Run 5 Phase 1, attempt 1, `.harness/run/TASKS/T-001.md`.
- **Never** drop `ndkVersion = "27.0.12077973"` from `app/build.gradle.kts` and `opencv/build.gradle`: AGP 9.4.1's default NDK is 28.2.13676358 (Gradle installs it into the SDK on first configure), which changes `libc++_shared.so` and the stripped libraries for all three ABIs, and so what runs on the 32-bit phone; with the pin the 34 native libraries are byte for byte the base's (`.harness/run/evidence/base-native-libs.txt` against `head-native-libs.txt`, DoD 15).
- **Never** write the words lint, baseline or disable (any case), `kotlin-android` or `AGP 8` in a line or comment you add to a `*.kts`, `*.gradle` or `gradle.properties` file, and never write a `group:artifact` colon string for a library outside the catalog in a build script: DoD 16, R1, R3 and DoD 14 grep added lines for them, comments included — evidence: DoD 14, 16, R1, R3.
- Constraint: Robolectric tests that launch `MainActivity` share Koin's JVM-global state; `KansoApplication` calls `stopKoin()` before `startKoin`. Never remove it. The app has no Material icons dependency: draw icons with `ImageVector.Builder`. Banned-word grep matches substrings (e.g. "introduce") in `app/src/main`.
- **Never** build a DataStore with the default file storage (`PreferenceDataStoreFactory`) if JVM tests on this Windows host write it twice: it uses `File.renameTo`, which fails once the file exists (`Unable to rename ...preferences_pb.tmp`). Use `DataStoreFactory.create(OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {...})` as `createSettingsDataStore` does (T-003, attempt 1 failure).
- **Never** read pixels in Robolectric tests with `captureToImage()` on this setup: it times out in `forceRedraw`. Draw the content view into a Bitmap instead (`app/src/test/.../settings/ScreenPixels.kt`).
- Verified 2026-10-06 (Phase 1): unit tests, verifyRoborazziDebug, lintDebug, assembleDebug green. Constraint: never construct a ViewModel inside a composable (lint error ViewModelConstructorInComposable, also in tests): build it before setContent. Constraint: in kotlinx-coroutines-test, never pass `backgroundScope` as a scope whose work the test must see finish: `advanceUntilIdle` skips it; use `CoroutineScope(StandardTestDispatcher(testScheduler))`.
- Verified 2026-10-06 (Phase 2): unit tests, verifyRoborazziDebug, lintDebug, assembleDebug green. Constraint: in Robolectric Compose tests that wait on a ViewModel loaded off the main thread, call `shadowOf(Looper.getMainLooper()).idle()` inside `waitUntil`, or it times out on the second load. Coil 2.7.0 is the image loader (ImageRequest cache key includes the photo's modified time). Tests swap Koin recipes after launch with `loadKoinModules(module { single<PhotoSource> { fake } })`; fakes: `FakePhotoSource` (T/gallery/FakePhotoSource.kt), `FakePhotoLoader` (T/tool/FakePhotoLoader.kt). `core/colorpicker` sets no semantics: its bars can only be checked by text or size.
- Verified 2026-10-06 (Phase 3): unit tests, verifyRoborazziDebug, lintDebug, assembleDebug green. CameraX 1.6.2 (compileSdk 36 accepted). Constraint: the unit-test JVM needs `maxHeapSize = "3g"` (set in app/build.gradle.kts) or the whole run dies with exit 1 / looks hung once many Robolectric screens run together; run groups with `--tests` when diagnosing. Camera: Robolectric cannot run CameraX, tests swap `factory<CameraController>` with `FakeCameraController` after launch. Save: tests use `FakeMediaProvider.install(dir)` (T/save); single-permission requests need `SinglePermissionRegistry` (T/save/SinglePermissionRegistry.kt), not `RecordingRegistry`. Constraint: on Android 8.1-9 `SaveResult.Saved.uri` is a file:// Uri; never share it as is.
- Run 5 Phase 1 (2026-10-09, Iteration 2): the project builds on Android Gradle Plugin 9.4.1 and Gradle 9.6.0 with every library as at base. Verified commands (unchanged): `./gradlew :app:assembleDebug`, `:app:testDebugUnitTest`, `:app:verifyRoborazziDebug`, `:app:lintDebug`; modules `:opencv:assemble{Debug,Release} :core:<m>:assemble{Debug,Release}` (both variants of nine modules, 2 min 35 s warm-ish); the combined `cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` took 5 min 19 s: 231 tests (0 failed, 0 errors, 0 skipped), `verifyRoborazziDebug` 16 of 16 unchanged, lint exit 0 (its only new line is the warning OldTargetApi: `targetSdk = 36` while 37 is the latest, which the PRD keeps), debug APK 219 MB with the same permissions, the three ABIs and the same 34 native libraries as base (CRC for CRC); the only non-code differences from the base APK are the compile-SDK stamps in the binary manifest and AGP's own metadata file. What the migration took: remove `kotlinAndroid` from the catalog, the root script and every module; OpenCV's `build.gradle` drops `try { apply plugin: 'kotlin-android' }`, `targetSdkVersion`, `versionCode`/`versionName`, the nested `android { buildFeatures }`, the per-build-type `packagingOptions { doNotStrip }` (now `packaging { jniLibs { keepDebugSymbols } }`), `proguard-android.txt` (AGP 9: `proguard-android-optimize.txt`) and the missing `proguard-rules.txt`, and uses `setSrcDirs`, `compileSdk`/`minSdk`, `namespace =`, `path = file(...)`; `core/*` needed only the plugin line removed; the four empty wrapper modules and `core/localml`'s LiteRT shim needed the changes of the Constraint above; the old Groovy forms mostly ran without error on 9.4.1, only a hard error (the proguard file) stops the configuration. Warnings that are not errors and were left (a fix would be a source change or beyond "as far as the build requires"): `core/localml` `buildFeatures.androidResources` deprecated (removed in AGP 10), the Compose feature flag `OptimizeNonSkippingGroups` in `core/imagecrop` is on by default (may warn again after Kotlin moves), `opencv/java/AndroidManifest.xml` still says `package="org.opencv"` (ignored, the namespace is the build script's), two redundant-conversion warnings in `core/common` `DimensionUtil.kt`. The wrapper cannot be refreshed under Gradle 9.6.0 until every module configures under AGP 9 (a `wrapper` run configures all projects), so the order is: set the new distribution with the old Gradle, bump AGP, drop `kotlin-android` and fix the module scripts, then run `wrapper` again. Tool use: the Monitor tool asks for approval on a compound command; run Gradle with `Bash` `run_in_background`, write its exit code to a file under `/tmp`, and wait inside the turn with a bounded foreground loop that checks for that file.

## Report back (a manifest, not a payload)

The files you wrote (expect exactly one), the behaviour now covered (one line per test), anything you could not do, anything you learned. Do not paste the file.

- **Attempt:** 1 of 3
