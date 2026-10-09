# PLAN

> Machine-owned execution strategy. The human never reviews this file - only the Definition of Done.
> Evolves through Tier-1 amendments (logged in AMENDMENTS.md) and Tier-2 queued decisions.
> Run 6: Material 3 Expressive with blue as Kanso's colour. Autonomous mode: D-001 (the DoD approval and the screenshot list) is the only gate.

## Strategy

Eleven tasks in four Phases, built on a clean base (`76522d7`: 238 tests, 16 baselines, Material3 1.5.0-beta01 already in the app).

- **Order of attack.** The theme and colour model come first because they move all 16 baselines in one stroke (so every screen's picture is re-recorded once for colour, and again only when its own screen is redesigned). The editor comes in Phase 1 because it holds the one real design decision (where the selected tool lives) and it makes Home's "opens the editor with that tool selected" testable. Saved's three actions are the one Fast cut (T-003). Home, Gallery and the editor's loading states follow (Phase 2); Camera, Settings and Saved's success mark follow once Home and Gallery are final, because the camera tests tap Home's "Camera" text and host the gallery (Phase 3). Navigation motion and the manifest's predictive-back flag come last (Phase 4), with the two cross-screen proof tests, because they change the timing of every whole-app test and the proofs should run against the final screens.
- **Every task is a checkpoint.** After the Workers report, the Iteration wires the shared files, builds and runs the whole suite, runs `compareRoborazziDebug`, copies each changed comparison picture into `.harness/run/evidence/baseline-diffs/phase<N>/`, re-records the approved baselines with `foreman-record-baselines.ps1`, opens and judges every picture, runs the fresh-context review and commits once.
- **Verification set per Iteration** (named so "everything I ran passed" means something): `cleanTestDebugUnitTest testDebugUnitTest verifyRoborazziDebug lintDebug assembleDebug --continue` (covers main, unit tests, screenshots, lint, the APK), plus the nine modules `:opencv:assembleDebug :core:<m>:assembleDebug` only when a build file moved (none should). Every Gradle command runs under `timeout`, writes to `.harness/run/evidence/`, and starts with `cd /d/Valve/Personal/Kanso &&`.
- **Baselines.** The 16 existing and the 8 new pictures are listed in D-001 for approval. New pictures are recorded in the phase their screen reaches its final look and are not changed by later phases: camera 20/21 and Settings 24/25 in Phase 3, editor 22/23 and gallery-loading 26/27 in Phases 1 and 2.
- **Strings.** Workers add new names only, in a new `res/values/strings_<area>.xml`; no existing string is moved, renamed or removed (tests reference `tool_saving`, `gallery_open_camera`, `tool_camera`, `settings`, `theme_mode_dark` and others by name). The Iteration removes only `theme_colour_default` (R1).
- **Shared files** (no Worker touches them; the Iteration edits them after the Workers report): `navigation/KansoNavHost.kt`, `navigation/Tool.kt`, `di/AppModule.kt`, `res/values/strings.xml`, both `colors.xml`, `AndroidManifest.xml`, `build.gradle.kts` and the catalog, every drawable, `PreviewsRenderTest.kt` (`requiredPreviewNames`), and every baseline PNG.

## Task Graph

- T-001 - Kanso blue through `MaterialExpressiveTheme`; Settings colour rows read blue, wallpaper, custom (depends on: -) - scope: theme, colour model, settings colour rows, theme tests - tier: Capable
- T-002 - One editor for the five tools: toolbar, tool switch, panels, selection kept (depends on: -) - scope: `ui/tool/*`, tool tests - tier: Capable
- T-003 - Saved's Share / Edit another / Home as an Expressive `ButtonGroup` with icons (depends on: -) - scope: `SavedScreen.kt`, `SavedScreenTest.kt` - tier: **Fast**
- T-004 - Home: "Open a photo", camera shortcut, five tool cards in two groups, Settings (depends on: T-001, T-002) - scope: `ui/home/*`, Home-facing tests - tier: Capable
- T-005 - Gallery: rounded thumbnails, Expressive chips, loading indicator, icon buttons (depends on: T-001) - scope: `GalleryScreen.kt`, gallery tests - tier: Capable
- T-009 - Editor loading and Save states use the Expressive loading indicator (depends on: T-002) - scope: `ToolScreen.kt`, `SaveButtonTest.kt` - tier: Capable
- T-006 - Camera: Expressive icon controls, a shutter that answers the press, theme-following picture area (depends on: T-004, T-005) - scope: `CameraScreen.kt`, camera tests - tier: Capable
- T-007 - Settings: grouped rows with icons, three colour swatches (depends on: T-001) - scope: `SettingsScreen.kt`, settings tests - tier: Capable
- T-010 - Saved: the one-shot shape-morph success mark (depends on: T-003) - scope: `SavedScreen.kt`, saved tests - tier: Capable
- T-008 - Screen moves animate through `NavDisplay` with the expressive spring; predictive back on (depends on: T-004) - scope: `navigation/KansoTransitions.kt`, `NavTransitionTest.kt` - tier: Capable
- T-011 - Cross-screen proofs: every screen follows the theme, every control is named and at least 48 dp (depends on: T-004, T-005, T-006, T-007, T-009, T-010) - scope: new `a11y/AccessibilityWalkTest.kt`, `a11y/TouchTargetCheck.kt`, `theme/ThemeFollowTest.kt` - tier: Capable

(A tier is Fast only if all three of POLICIES' conditions hold. T-003 meets them: bounded scope with no shared file; acceptance exactly checkable - the existing `SavedScreenTest` passes unchanged and `ButtonGroup(` is in the source; no architecture decision or new dependency, given the API digest in its Brief. Everything else embeds a design or contract decision or a paused-clock test discipline, so it is Capable. A failed Fast attempt escalates T-003 to Capable for its remaining attempts.)

## Phase Grouping

| Phase | Tasks | Shared files the Iteration wires itself |
|---|---|---|
| 1 | T-001, T-002, T-003 | before dispatch: drawables (fetched), `Tool.kt` (`iconRes`, `editingTools`), API digest, the `enableOnBackInvokedCallback` spike; after: `KansoNavHost.kt` (Saved key from the selected tool), `colors.xml` x2 (hex from the failing `KansoBlueThemeTest`), `strings.xml` (`theme_colour_default` out), `PreviewsRenderTest.kt`, `AppModule.kt` if a Worker needs a new factory parameter, baselines (all 16 recorded for colour; 16, 17, 18, 19, 22, 23 for their screens) |
| 2 | T-004, T-005, T-009 | `KansoNavHost.kt` Home entry and its preview, `Tool.kt` (`CAMERA` out once Home no longer lists it, `editingTools` becomes `entries`), `PreviewsRenderTest.kt`, drawables, baselines (1, 2, 3, 10-13, 26, 27) |
| 3 | T-006, T-007, T-010 | `git rm` of `CameraIcons.kt` (after a grep shows no user) and `BackArrowIcon.kt` (same), `PreviewsRenderTest.kt`, drawables, baselines (4, 5, 6, 14, 15, 18, 19, 20, 21, 24, 25) |
| 4 | T-008, T-011 | `KansoNavHost.kt` (hands `KansoTransitions` to `NavDisplay`), `AndroidManifest.xml` (`enableOnBackInvokedCallback`), `PreviewsRenderTest.kt`; then three fresh full runs for flakes. A defect T-011's tests find in a screen becomes a fix task (Tier 1) in a Phase 5 |

Scopes inside a Phase are pairwise disjoint (checked by the conflict analysis against the files). Across Phases overlap is intended: `ScreenshotTest.kt` (T-001, T-007), `SavedScreen.kt` (T-003, T-010), `ToolScreen.kt` (T-002, T-009), `SaveButtonTest.kt` (T-002 only if a constructor changes, then T-009), `ThemeModeSettingsTest.kt` and `ThemeModeRestartTest.kt` (T-004, T-007).

## Phase 1 preparation (the Iteration, before any Worker)

1. Fetch the Material Symbols Rounded drawables into `app/src/main/res/drawable/` as `ic_<name>.xml` with `curl` from `https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android/<name>/materialsymbolsrounded/<name>_24px.xml`, removing only the `android:tint` attribute; record URL and SHA-256 in `.harness/run/evidence/symbols/SOURCES.md` and keep `check.sh`. The ten: `edit`, `background_replace`, `auto_fix_high`, `face_retouching_natural`, `brush`, `photo_camera`, `settings`, `save`, `share`, `arrow_back` (with `android:autoMirrored="true"`). The extras the screens need (all 200 on 2026-10-09): `home`, `check`, `refresh`, `add_photo_alternate`, `flash_on`, `flash_off`, `flash_auto`, `cameraswitch`, `dark_mode`, `palette`, `contrast`.
2. `Tool.kt`: add `@DrawableRes val iconRes` beside `labelRes`, and `editingTools` (all entries but `CAMERA`) so the editor never names `CAMERA`.
3. Build `.harness/knowledge/api-digest.md` from `material3-android-1.5.0-beta01-sources.jar` and the Navigation 3 1.2.0 sources (download the jars into a scratch directory outside the repository, unzip there): the exact signatures of `MaterialExpressiveTheme`, `MotionScheme.expressive()`, `HorizontalFloatingToolbar`, the toggle and icon-button `shapes` overloads, `ButtonGroup` (how a test tag reaches an item), `LoadingIndicator` / `ContainedLoadingIndicator`, `MaterialShapes`, `Morph`, `NavDisplay`'s transition parameters. Workers have no network and these APIs are newer than their knowledge, so the digest goes into every Brief that needs it.
4. Spike: add `android:enableOnBackInvokedCallback="true"` to a scratch copy of the manifest, run `NavigationTest`, `PhotoToToolNavigationTest`, `ThemeModeSettingsTest` (they use `Espresso.pressBack()`), then revert it; record the result in PROJECT.md so Phase 4 is not a surprise.
5. Update PROJECT.md's stale line ("draw icons with `ImageVector.Builder`").

## Known Risks

- **`LoadingIndicator` can keep a test from ever going idle** (PROJECT.md Constraint; the Compose test's infinite-animation policy covers `rememberInfiniteTransition`, not necessarily `LoadingIndicator`). Tests that show a screen holding one wait for its node to leave (`waitUntil`), or pause the clock with `mainClock.autoAdvance = false` before `setContent`; every Gradle run is under `timeout`. Watch list: `GalleryGridTest`, `GalleryPermissionTest`, `CameraControlsTest` (hosts the gallery), `PhotoToToolNavigationTest`, `SavedNavigationTest`, `NavigationTest`, `SaveButtonTest`, `ToolScreenshotTest`, `SavedScreenshotTest`.
- **T-002's mechanism for keeping the selected tool** (saved state or the navigation key) is unproven under Navigation 3 1.2.0 with Koin; `ToolPhotoKeyTest` is the judge; the signature of `ToolScreen`'s `onSaved` stays `(String) -> Unit`, and the Iteration builds `Saved(selectedTool, uri)` in the nav host from `viewModel.state.value.tool`.
- **Predictive back and `Espresso.pressBack()`** may behave differently once the manifest flag is on (Phase 4; the Phase 1 spike tells early).
- **Home invariants** (PROJECT.md Constraints) are what keeps about a dozen tests outside T-004's scope green: "Kanso", "Settings" and "Camera" once each as text inside the scrolling column; cards addressed by tag; the bottom-left pixel plain background.
- **The window background hex** cannot be computed by a Worker; the Iteration reads it from the failing assertion of `KansoBlueThemeTest` and writes both `colors.xml`.
- **Unprovable by machine**: the back gesture's preview and "Remove animations" are person-only (criteria 13, 14, 21).
- **Flakes.** `ThemeColourSettingsTest` and `CameraScreenTest.retake_...` flaked on Windows before (PROJECT.md); a failing full run is a defect, not a flake to rerun past; Phase 4 ends with three fresh full runs.
