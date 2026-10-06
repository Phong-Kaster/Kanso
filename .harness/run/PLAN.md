# PLAN

> Machine-owned execution strategy. The human never reviews this file - only the Definition of Done.

## Strategy

Build the shared path in the order a person walks it: fix the Run 1 carry-overs first (small, disjoint), then Gallery + permission, then the photo grid and the tool screen, then camera and Save, then the camera extras and the Saved screen. Every task ends with something observable (a Robolectric test or a pinned Roborazzi picture). The orchestrator (the Iteration) owns every shared file; Workers never touch them.

Paths below are relative to `app/src/main/java/com/phongkaster/kanso/` (`K/`) and `app/src/test/java/com/phongkaster/kanso/` (`T/`).

**Shared files, never in a Worker's scope; the Iteration edits them:** `K/navigation/Destinations.kt`, `K/navigation/KansoNavHost.kt`, `K/navigation/Tool.kt`, `K/di/AppModule.kt`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `T/navigation/NavigationTest.kt`, `T/screenshots/ScreenshotTest.kt`, everything under `app/src/test/screenshots/`.

**Seams that keep the host testable:** an app-side `PhotoSource` interface (wraps the engine's `MediaRepository`, IO dispatcher, Koin-bound, faked in tests); a `CameraController` boundary (CameraX behind an interface, faked in tests); a `PhotoSaver` interface. Nav keys carry the photo as a `String` (a `Uri` is not `@Serializable`).

**Baselines:** only the human can approve changes to `6_` and `7_`-`9_` pictures (DoD § Baselines, A-006). New pictures `10_`-`19_` are free.

## Task Graph

- T-001 - Theme colour labels from strings.xml (depends on: -) - scope: `K/ui/settings/SettingsScreen.kt`, `T/settings/ThemeColourLabelsTest.kt` - tier: Fast
- T-002 - Picker writes the theme once the drag settles (depends on: -) - scope: `K/ui/settings/SettingsViewModel.kt`, `T/settings/CustomColourDebounceTest.kt` - tier: Capable
- T-003 - Tools open a gallery that asks for photo access the first time (depends on: -) - scope: `K/ui/gallery/GalleryScreen.kt`, `K/ui/gallery/GalleryViewModel.kt`, `K/ui/permission/`, `T/gallery/GalleryPermissionTest.kt`, `T/screenshots/GalleryRefusedScreenshotTest.kt` - tier: Capable
- T-004 - Gallery grid of photos by album; tap opens the tool (depends on: T-003) - scope: `K/ui/gallery/` (all), `K/data/media/`, `T/gallery/GalleryGridTest.kt`, `T/screenshots/GalleryGridScreenshotTest.kt` - tier: Capable
- T-005 - Tool screen shows the photo upright, downsampled, surviving process death (depends on: -; observable once T-004 wired) - scope: `K/ui/tool/`, `K/data/photo/PhotoLoader.kt`, `T/tool/`, `T/screenshots/ToolScreenshotTest.kt` - tier: Capable
- T-006 - Camera takes a shot; Keep opens the tool (depends on: T-003, T-005) - scope: `K/ui/camera/`, `T/camera/`, `T/screenshots/CameraRefusedScreenshotTest.kt` - tier: Capable
- T-007 - Save writes a full-resolution upright JPEG to Pictures/Kanso (depends on: T-003, T-005) - scope: `K/data/photo/PhotoSaver.kt`, `K/data/photo/UprightJpeg.kt`, `K/ui/tool/` (Save action only, after T-005), `T/save/` - tier: Capable
- T-008 - Camera lens switch, flash, pinch zoom, gallery camera button (depends on: T-004, T-006) - scope: `K/ui/camera/`, `K/ui/gallery/GalleryScreen.kt` (top-bar button only), `T/camera/` - tier: Capable
- T-010 - Colour picker without the transparency bar (depends on: T-001) - scope: `core/colorpicker/.../HSVColorPicker.kt`, `K/ui/settings/SettingsScreen.kt` (call site), `T/settings/PickerNoAlphaTest.kt` - tier: Capable (adds a parameter to a module another module also calls)
- T-009 - Saved screen with Share, Edit another, Home (depends on: T-007) - scope: `K/ui/saved/`, `T/saved/`, `T/screenshots/SavedScreenshotTest.kt` - tier: Capable

## Phase Grouping

| Phase | Tasks | Shared files the Iteration wires itself |
|---|---|---|
| 1 | T-001, T-002, T-003 | `T/fakes/` (fake `PhotoSource`, shared screenshot helper) and moving `BackArrowIcon` to `K/ui/common/` (Iteration, before dispatch), `strings.xml`, `Destinations.kt` (Gallery key), `KansoNavHost.kt`, `AppModule.kt`, `AndroidManifest.xml` (read permissions), `NavigationTest.kt`, `ScreenshotTest.kt` (placeholder tests off), screenshot 6 |
| 2 | T-004, T-005, T-010 | `libs.versions.toml` + `app/build.gradle.kts` (Coil, ExifInterface, test heap if needed), `Destinations.kt` (ToolPhoto key), `KansoNavHost.kt`, `AppModule.kt`, `strings.xml`; Camera tile keeps its Placeholder route until Phase 3 |
| 3 | T-006, T-007 | delete Placeholder files (R1) once the Camera key exists, CameraX in catalog + build file, `Destinations.kt` (Camera key), Home camera tile route, manifest (CAMERA, WRITE<=28, uses-feature), `AppModule.kt`, `strings.xml` |
| 4 | T-008, T-009 | `Destinations.kt` (Saved key), `KansoNavHost.kt`, `strings.xml` |

T-001 and T-010 never share a Phase (both write `SettingsScreen.kt`). T-004 and T-008 never share a Phase (both write `GalleryScreen.kt`). T-005 and T-007 never share a Phase (both write `K/ui/tool/`). If T-004 overflows an Iteration, split the Android 14 partial-access notice out as T-004b in Phase 3.

## Known Risks

- Moving the five tiles to Gallery breaks `NavigationTest` and screenshots 7-9 in Phase 1; the Iteration re-points the tests and needs the `## Baselines` approval to remove 7-9 (A-006).
- CameraX version: newest stable whose AAR metadata accepts compileSdk 36 and AGP 8.13; check at T-006 (A-005). Navigation 3 1.2.0 already forced a pin in Run 1.
- Robolectric cannot start CameraX, so the camera is faked behind a Koin boundary; the real preview is proven only on the emulator/phone (criterion 7).
- Emulator `Application...` system window may block `dump`/`tap` again (Run 1 A-005): criteria needing it are reported "not driven", never skipped silently.
- Decoding a 50 MP photo to rotate it can run out of memory; A-001 gives a verbatim-copy fallback.
- `MediaRepository` reads deprecated `DATA` into a non-null field: a photo with null `DATA` silently drops out of the engine's list. The app-side source must not depend on `path`; when it matters, query ids through the engine and build `contentUri` itself, or record the gap in ISSUES.
