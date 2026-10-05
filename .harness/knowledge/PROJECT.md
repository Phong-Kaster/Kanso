# PROJECT KNOWLEDGE

## Constraints

- **Never** edit anything under `reference/` or import from it, because the PRD makes it read-only and DoD 27 diffs it against `a0d9a79`.
- **Never** add INTERNET or ACCESS_NETWORK_STATE to the app or leave one merged in, because the PRD says the app has no network; use `tools:node="remove"` — evidence: DoD 23.
- **Never** type into or rely on `core/colorpicker`'s `HSVColorPicker` hex field: it parses the typed hex with `toLong()` as base 10 (`core/colorpicker/src/main/java/com/library/colorpicker/HSVColorPicker.kt:67`). Its text is hard-coded black (around line 106), so on a dark surface wrap it in a light container.
- **Never** edit an engine module's source apart from `core/common/.../AnalyticsUtil.kt` (A-001); build scripts only as far as the build requires.
- **Never** use `androidx.compose.material.` (Material 2) in `app`.

## Reference

- Repository class: **product** (the engine import is real code, no demo features). Evidence: one commit `a0d9a79` importing Retica's engine; `reference/retica-app` is Retica's original app, read-only.
- Layout: no `settings.gradle.kts` and no `app` yet (T-001 writes them). Modules: `core/{colorpicker,common,editimage,gallery,imagecrop,imageprocessor,localml,overlaybrush}`, `opencv`, prebuilt AARs in `libs/`.
- Engine build style: AGP 8.9.2 DSL, `kotlinOptions`, JVM target 22, compileSdk 35, minSdk 27 (colorpicker 24), Compose BOM 2025.06.00. Gradle wrapper 8.13 (cached). Latest stable found 2026-10-05: Navigation 3 1.2.0, lifecycle-viewmodel-navigation3 2.11.0, Koin 4.2.2, Compose BOM 2026.09.00, Kotlin 2.4.20, Roborazzi 1.76.0.
- Environment: JDK 24, Android SDK `C:\Users\MAC_PC\AppData\Local\Android\Sdk` (platforms 24-37; system images 27, 28, 34, 36).
- Roborazzi is the UI check; see `.harness/loop/packs/android/roborazzi.md`. Baselines go in `app/src/test/screenshots/`.
- Build / test / lint commands: unverified until T-001 (see RESUME.md).
