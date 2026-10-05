# PROJECT KNOWLEDGE

## Constraints

- **Never** edit anything under `reference/` or import from it, because the PRD makes it read-only and DoD 27 diffs it against `a0d9a79`.
- **Never** add INTERNET or ACCESS_NETWORK_STATE to the app or leave one merged in, because the PRD says the app has no network; use `tools:node="remove"` — evidence: DoD 23.
- **Never** type into or rely on `core/colorpicker`'s `HSVColorPicker` hex field: it parses the typed hex with `toLong()` as base 10 (`core/colorpicker/src/main/java/com/library/colorpicker/HSVColorPicker.kt:67`). Its text is hard-coded black (around line 106), so on a dark surface wrap it in a light container.
- **Never** edit an engine module's source apart from `core/common/.../AnalyticsUtil.kt` (A-001); build scripts only as far as the build requires.
- **Never** use `androidx.compose.material.` (Material 2) in `app`.

## Reference

- Repository class: **product** (the engine import is real code, no demo features). Evidence: one commit `a0d9a79` importing Retica's engine; `reference/retica-app` is Retica's original app, read-only.
- Layout: `settings.gradle.kts` and `app` exist (T-001). Modules: `core/{colorpicker,common,editimage,gallery,imagecrop,imageprocessor,localml,overlaybrush}`, `opencv`, prebuilt AARs in `libs/`.
- Engine build style: AGP 8.9.2 DSL, `kotlinOptions`, JVM target 22, compileSdk 35, minSdk 27 (colorpicker 24), Compose BOM 2025.06.00. Gradle wrapper 8.13 (cached). Latest stable found 2026-10-05: Navigation 3 1.2.0, lifecycle-viewmodel-navigation3 2.11.0, Koin 4.2.2, Compose BOM 2026.09.00, Kotlin 2.4.20, Roborazzi 1.76.0.
- Environment: JDK 24, Android SDK `C:\Users\MAC_PC\AppData\Local\Android\Sdk` (platforms 24-37; system images 27, 28, 34, 36).
- Roborazzi is the UI check; see `.harness/loop/packs/android/roborazzi.md`. Baselines go in `app/src/test/screenshots/`.
- **Version pins below latest stable (forced, 2026-10-05):** Navigation 3 1.2.0, lifecycle 2.11.0 and Compose BOM 2026.09.00 each require Android Gradle Plugin 9.1+ and compileSdk 37 (verified: `checkDebugAarMetadata` fails). The engine modules use the AGP 8 DSL (`kotlinOptions`), so the build stays on AGP 8.13.0, giving nav3 1.1.7, lifecycle-viewmodel-navigation3 2.10.0, Compose BOM 2026.05.00, Kotlin 2.3.20 (DoD 10, Constraint "latest stable"). Moving to AGP 9 means migrating every engine build script: a Tier-2 decision for a later run.
- **Libraries merged into the app's manifest (DoD 24, R3):** `WAKE_LOCK`, `FOREGROUND_SERVICE`, `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (WorkManager, AndroidX) plus WorkManager / ML Kit / datatransport / profileinstaller services and receivers; none is the app's own. ML Kit also brings `firebase-components` / `firebase-encoders` as infrastructure (A-003), no analytics or messaging. Removed with `tools:node="remove"`: INTERNET, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED, READ/WRITE_EXTERNAL_STORAGE, READ_PHONE_STATE (old TensorFlow Lite GPU).
- Run DoD greps with `--exclude-dir=build`: `app/build` holds merged manifests that mention ML Kit's firebase components.
- Build / test / lint commands (verified 2026-10-05): `./gradlew :app:assembleDebug`, `./gradlew :app:testDebugUnitTest`, `./gradlew :app:lintDebug`.
