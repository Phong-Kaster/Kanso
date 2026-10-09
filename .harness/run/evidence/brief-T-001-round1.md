# WORKER BRIEF - T-001 (round 1)

You are the Capable Worker for the nine module build scripts of T-001. You hold no git, build or test
capability: you edit files and report. The Iteration runs every build and hands you the AGP 9 error text.

## Your task

Make these nine build scripts configure and build under **Android Gradle Plugin 9.4.1 on Gradle 9.6.0**, changing as
little as the build requires and changing **no engine source**:

- `opencv/build.gradle` (Groovy; OpenCV 4.12.0-dev, prefab publishing, CMake `libcxx_helper`)
- `core/colorpicker/build.gradle.kts`, `core/common/build.gradle.kts`, `core/editimage/build.gradle.kts`,
  `core/gallery/build.gradle.kts`, `core/imagecrop/build.gradle.kts`, `core/imageprocessor/build.gradle.kts`,
  `core/localml/build.gradle.kts`, `core/overlaybrush/build.gradle.kts`

Facts already settled (do not redo them): the Iteration has already bumped `agp` to 9.4.1 in `gradle/libs.versions.toml`,
**removed the alias `libs.plugins.kotlinAndroid`** (id `org.jetbrains.kotlin.android`) from the catalog, the root
`build.gradle.kts` and `app/build.gradle.kts`, and set the wrapper to Gradle 9.6.0. AGP 9 compiles Kotlin itself
(built-in Kotlin), so **every module's `alias(libs.plugins.kotlinAndroid)` line (and OpenCV's `try { apply plugin: 'kotlin-android' }` block and its `plugins.withId('org.jetbrains.kotlin.android')` block) must go**, or the build fails. The Compose compiler plugin
(`libs.plugins.compose.compiler`) and the serialization plugin stay. `kotlin { compilerOptions { jvmTarget ... } }` stays and works with built-in Kotlin.

Rules of the DoD you must obey:
- **Never** set `android.builtInKotlin=false`, `android.newDsl=false`, `android.newDsl.optOut=...` or any other
  `android.*` opt-out flag, in `gradle.properties` or anywhere, to hold an AGP 8 behaviour. If a module cannot be
  moved without one, STOP and say so in your report: that is reported, not worked around.
- Library versions do not move (BOM 2026.05.00, Kotlin 2.3.20, nav3 1.1.7, ...). Do not touch `gradle/libs.versions.toml`,
  `gradle.properties`, `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts` (the Iteration owns them; ask in your report).
- No `core/*/src`, no `opencv/java`, `opencv/native`, `opencv/libcxx_helper`, no `libs/`.
- Keep the existing comments' meaning; match the file's style (the Kotlin scripts are Kotlin DSL; `opencv/build.gradle` is Groovy).
- Do not suppress, baseline or disable a lint check.

Known traps (look at each in your files; the build will tell the rest):
- (a) **opencv, Groovy.** The new DSL has no `compileSdkVersion`, `minSdkVersion`, `targetSdkVersion` (a library takes none),
  `versionCode`/`versionName` (not library settings), `packagingOptions` (use `packaging { jniLibs { keepDebugSymbols += '**/*.so' } }` for `doNotStrip`),
  `jniLibs.srcDirs = [...]` (use `setSrcDirs`/`srcDir`), a nested `android { buildFeatures }`; `prefab` and `externalNativeBuild`
  need their new-DSL forms. Reason from the error text I give you each round.
- (b) the `try { apply plugin: 'kotlin-android' }` block and the `plugins.withId('org.jetbrains.kotlin.android') { kotlin { compilerOptions { jvmTarget ... } } }`
  block at the end of `opencv/build.gradle` go: built-in Kotlin compiles `opencv/java/src/**/*.kt` and its `jvmTarget` follows
  `compileOptions.targetCompatibility` (22); if the build later says "Inconsistent JVM-target", set `kotlin { compilerOptions { jvmTarget ... } }` unconditionally. The println "Configure OpenCV with/without Kotlin" lines are OpenCV's own: keep the build output meaningful.
- (c) `getDefaultProguardFile('proguard-android.txt')` is unsupported from AGP 9 ("includes `-dontoptimize`"): use `proguard-android-optimize.txt` and drop `proguard-rules.txt` if the file does not exist in `opencv/`. **This is the first error the build hits** (`opencv/build.gradle` line 144, see below).
- (d) AGP 9.4 offers `android.newDsl.optOut=:module` per module: a last resort that leaves DoD 11 unmet. Do not use it; report instead.
- The NDK: do not set `ndkVersion` now. The Iteration compares native libraries with the base build afterwards and will tell you.
- `core/editimage`, `gallery` and `localml` set `isMinifyEnabled = true` on release, so AGP 9's R8 defaults may apply.
- `core/localml` sets `buildFeatures { mlModelBinding = true }` with no `src/main/ml` folder: delete the line only if AGP 9 rejects it ("as far as the build requires").
- The prebuilt AARs in `libs/` (`editimage`, `imageprocessor`, `localml`, `overlaybrush`) may share a namespace with their modules; if AGP 9 turns the duplicate-namespace warning into an error, DO NOT change a module namespace (it would change an engine source package): report it.
- `core/imagecrop` sets the Compose compiler feature flag `OptimizeNonSkippingGroups`: leave it unless the build complains.
- `compileSdk` stays what each module says now (35, `localml` 36): only the app moves to 37. Keep `compileOptions` (JVM 22) as is.

First error the build reports (round 1, from `./gradlew help` after the shared edits):

```
Build file 'opencv/build.gradle' line: 144
A problem occurred evaluating project ':opencv'.
> `getDefaultProguardFile('proguard-android.txt')` is no longer supported since it includes `-dontoptimize`, which prevents R8 from performing many optimizations. Instead use `getDefaultProguardFile('proguard-android-optimize.txt)`, and if needed, temporarily use `-dontoptimize` in a custom keep rule file while fixing breakages.
```

You cannot run Gradle, so work through the whole of each script against the traps above and fix everything you can
foresee for AGP 9.4.1's new DSL in one pass, with minimal edits. The Iteration will run the build and send you the next error text for anything you missed.

- **Attempt:** 1 of 3
- **Model Tier:** Capable
- **Previous attempt failed with:** not applicable (first dispatch)

## Declared File Scope

You may write **only** these files. Writing outside this list is a violation and your work will be reverted:

- `opencv/build.gradle`
- `core/colorpicker/build.gradle.kts`
- `core/common/build.gradle.kts`
- `core/editimage/build.gradle.kts`
- `core/gallery/build.gradle.kts`
- `core/imagecrop/build.gradle.kts`
- `core/imageprocessor/build.gradle.kts`
- `core/localml/build.gradle.kts`
- `core/overlaybrush/build.gradle.kts`

## Status of other work

| Task | Status |
|---|---|
| T-002 | pending (after T-001; not started) |
| T-003 | pending (after T-002; not started) |

## Pointers

- Shared files already edited by the Iteration: `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts` (read them for the plugin aliases that remain: `android.application`, `androidLibrary`, `compose.compiler`, `serialization`, `roborazzi`).

## Constraints (binding, copied verbatim from `.harness/knowledge/PROJECT.md`)

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
- Constraint: Robolectric tests that launch `MainActivity` share Koin's JVM-global state; `KansoApplication` calls `stopKoin()` before `startKoin`. Never remove it. The app has no Material icons dependency: draw icons with `ImageVector.Builder`. Banned-word grep matches substrings (e.g. "introduce") in `app/src/main`.
- **Never** build a DataStore with the default file storage (`PreferenceDataStoreFactory`) if JVM tests on this Windows host write it twice: it uses `File.renameTo`, which fails once the file exists (`Unable to rename ...preferences_pb.tmp`). Use `DataStoreFactory.create(OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {...})` as `createSettingsDataStore` does (T-003, attempt 1 failure).
- **Never** read pixels in Robolectric tests with `captureToImage()` on this setup: it times out in `forceRedraw`. Draw the content view into a Bitmap instead (`app/src/test/.../settings/ScreenPixels.kt`).
- Verified 2026-10-06 (Phase 1): unit tests, verifyRoborazziDebug, lintDebug, assembleDebug green. Constraint: never construct a ViewModel inside a composable (lint error ViewModelConstructorInComposable, also in tests): build it before setContent. Constraint: in kotlinx-coroutines-test, never pass `backgroundScope` as a scope whose work the test must see finish: `advanceUntilIdle` skips it; use `CoroutineScope(StandardTestDispatcher(testScheduler))`.
- Verified 2026-10-06 (Phase 2): unit tests, verifyRoborazziDebug, lintDebug, assembleDebug green. Constraint: in Robolectric Compose tests that wait on a ViewModel loaded off the main thread, call `shadowOf(Looper.getMainLooper()).idle()` inside `waitUntil`, or it times out on the second load. Coil 2.7.0 is the image loader (ImageRequest cache key includes the photo's modified time). Tests swap Koin recipes after launch with `loadKoinModules(module { single<PhotoSource> { fake } })`; fakes: `FakePhotoSource` (T/gallery/FakePhotoSource.kt), `FakePhotoLoader` (T/tool/FakePhotoLoader.kt). `core/colorpicker` sets no semantics: its bars can only be checked by text or size.
- Verified 2026-10-06 (Phase 3): unit tests, verifyRoborazziDebug, lintDebug, assembleDebug green. CameraX 1.6.2 (compileSdk 36 accepted). Constraint: the unit-test JVM needs `maxHeapSize = "3g"` (set in app/build.gradle.kts) or the whole run dies with exit 1 / looks hung once many Robolectric screens run together; run groups with `--tests` when diagnosing. Camera: Robolectric cannot run CameraX, tests swap `factory<CameraController>` with `FakeCameraController` after launch. Save: tests use `FakeMediaProvider.install(dir)` (T/save); single-permission requests need `SinglePermissionRegistry` (T/save/SinglePermissionRegistry.kt), not `RecordingRegistry`. Constraint: on Android 8.1-9 `SaveResult.Saved.uri` is a file:// Uri; never share it as is.

## Conventions

- Windows host; some files are CRLF, some LF. Keep each file's existing line endings.
- Kotlin DSL scripts use `android { ... }` with the new AGP 9 DSL by default.

## Report back

A manifest, not a payload: files you wrote; what now works (by your reading, since you cannot build); anything you could not do and why; anything you learned that belongs in project knowledge.
