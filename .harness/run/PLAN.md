# PLAN

> Machine-owned execution strategy. The human never reviews this file - only the Definition of Done.

## Strategy

Build the shell in a strict chain, because every task needs the one before it to compile and every
task touches the app's wiring (manifest, Koin module, `MainActivity`, theme): **build wiring → Navigation 3 shell → theme mode → theme colour → screenshot checks**. One task per Phase, so
no Worker ever shares a file with another and the Iteration builds after each.

Package root: `com.phongkaster.kanso` under `app/src/main/java/com/phongkaster/kanso/`:
`KansoApplication`, `MainActivity`, `navigation/` (keys + `KansoNavHost`), `ui/home/`, `ui/placeholder/`,
`ui/settings/`, `ui/theme/`, `data/settings/`, `di/`.

Toolchain: the engine's modules are AGP 8-style (`kotlinOptions`, `org.jetbrains.kotlin.android`, JVM 22),
so the root keeps AGP 8.x and raises Kotlin/Compose only as far as the latest-stable Navigation 3,
Koin and Roborazzi demand. The Iteration owns that resolution because only it can build (T-001 notes).
Firebase is cut out of `core/common` by replacing `AnalyticsUtil.kt`'s Firebase calls with a local log-only
version (assumption A-001). Roborazzi/Robolectric are Foreman's standing test dependencies and are
added in T-001.

## Task Graph

- T-001 - Project builds: root + app module, engine wired, Firebase cut, no INTERNET, Roborazzi deps, empty Compose Activity launches (depends on: -) - scope: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/**`, `app/src/main/java/com/phongkaster/kanso/KansoApplication.kt`, `app/src/main/java/com/phongkaster/kanso/MainActivity.kt`, `core/*/build.gradle.kts`, `opencv/build.gradle`, `core/common/src/main/java/com/photo/editor/common/util/AnalyticsUtil.kt`, `app/src/test/**/LaunchTest.kt` - tier: Capable
- T-002 - Navigation 3 shell: splash, Home with six tools, placeholders, Settings entry, back works (depends on: T-001) - scope: `app/src/main/java/com/phongkaster/kanso/navigation/**`, `ui/home/**`, `ui/placeholder/**`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`, `di/**`, `app/src/test/**/navigation/**`, `app/src/main/res/values/{strings,themes}.xml` - tier: Capable
- T-003 - Theme mode: Settings choice, applied app-wide immediately, persisted (depends on: T-002) - scope: `data/settings/**`, `ui/theme/**`, `ui/settings/**`, `app/src/test/**/settings/**`, `app/src/test/**/theme/**` - tier: Capable
- T-004 - Theme colour: wallpaper or picked colour via `core/colorpicker`, generated M3 scheme, persisted (depends on: T-003) - scope: `ui/theme/**`, `data/settings/**`, `ui/settings/**`, `app/src/test/**/theme/**`, `app/src/test/**/settings/**` - tier: Capable
- T-005 - Roborazzi checks: nine baselines of Home, Settings, placeholder in light, dark, custom colour (depends on: T-004) - scope: `app/src/test/java/**/screenshots/**`, `app/src/test/screenshots/**` - tier: Capable

## Phase Grouping

| Phase | Tasks | Shared files the Iteration wires itself |
|---|---|---|
| 1 | T-001 | toolchain resolution, version catalog |
| 2 | T-002 | `MainActivity.kt` wiring, `di/` module list |
| 3 | T-003 | `MainActivity.kt` theme hookup, Koin module |
| 4 | T-004 | Koin module |
| 5 | T-005 | - |

No two tasks share a Phase: each one extends the app wiring the previous task created.

## Known Risks

- Latest-stable Navigation 3 / Koin / Roborazzi may need a newer Kotlin or compileSdk than the engine's modules build with; engine modules use JVM target 22 and AGP-8 DSL. Mitigation: raise Kotlin first, keep AGP 8.x; only touch engine build scripts as far as the build requires.
- Prebuilt engine AARs may call Firebase through `AnalyticsUtil` inlined into them; it would fail only when such a call runs (a later run), not at build. Recorded as A-001.
- Engine libraries may merge `INTERNET` into the manifest; removed with `tools:node="remove"` unless it breaks the engine.
- `core/colorpicker`'s `HSVColorPicker` parses its hex field with `toLong()` on a base-10 string, so typed hex input may misbehave; use its sliders/`onColorChanged`, and note any defect rather than editing the engine.
- Robolectric cannot show a real wallpaper or system bars, so criteria 19, 22 and 12 need a device and a person.
- Latest-stable `material-kolor` may need a newer Kotlin than the engine; if it does not resolve, fall back to a hand-written tonal-palette generator in `ui/theme/` (Tier 1 amendment).
- No task is classed Fast: every task embeds a dependency, architecture or cross-module contract decision (POLICIES: when in doubt, Capable). The teaching-comment requirement is part of every task's acceptance and is mechanised by `KdocCoverageTest`, not left to a final pass.
- ML Kit (needed by `core/localml`) brings Firebase infrastructure libraries, Play-services model loading and possibly INTERNET / ACCESS_NETWORK_STATE into the merged manifest. INTERNET and ACCESS_NETWORK_STATE are removed with `tools:node="remove"`; nothing in this run uses ML (A-003).
- The system splash follows the phone's light/dark setting, not the app's theme mode; a mismatch flashes the other theme for the splash (documented in DoD 12, accepted).
