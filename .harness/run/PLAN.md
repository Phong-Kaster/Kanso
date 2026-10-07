# PLAN

> Machine-owned execution strategy. Evolves through Tier-1 amendments (AMENDMENTS.md) and queued/assumed decisions.

## Strategy

The run restructures nine files and adds previews; it must change no pixel and no behaviour. Order of attack:

1. **T-001** makes previews possible at all (build wiring, done by the Iteration) and proves the pattern on the two simplest files (Home, the theme), together with the host test that draws every preview and the hygiene test (no Koin / ViewModel / MediaStore / camera in previews; sample data private and preview-only).
2. **T-002, T-003, T-008** (Settings, permission pieces, Camera): the hardest splits, in parallel (disjoint files).
3. **T-005, T-006, T-007** (Tool, Saved, Gallery): the same split pattern, extended.
4. **T-004** (navigation host): split into the stateful host and a stateless display so a preview can show Navigation 3 drawing a screen.
5. **T-009** adds the coverage test last, when every file is done, so it never goes red between checkpoints.

Every Phase is verified on the combined tree by the Iteration (`testDebugUnitTest`, `verifyRoborazziDebug`, `lintDebug`, `assembleDebug`); existing tests and the 16 baselines must stay untouched. Baselines are never re-recorded; a moved baseline means a Worker changed what the person sees, and the Worker is re-dispatched.

## Preview conventions (every preview task follows these; the Iteration checks them)

- **Split pattern (state hoisting).** A screen that reaches for Koin, a ViewModel, MediaStore, the camera or a permission launcher becomes: the existing public function (same name, same parameters, so existing tests compile unchanged) that gathers the state and calls a new **stateless** `...Content` composable taking the state and callbacks as parameters. Explain the split in a plain-English KDoc paragraph where it is first used (Home first; later files may refer back to it in one sentence and say what is specific).
- **Keep what the person sees identical:** same layout, same `testTag`s, same strings, same semantics, same modifier order. Move code; do not redesign. The Roborazzi baselines are the check.
- **Previews:** `@PreviewLightDark` on a `private fun ...Preview()` in the **same file**, wrapped in `KansoTheme(themeMode = ThemeMode.System)` (System follows the preview's light/dark) and a `Surface`/`Scaffold` background as the real screen has. Use the exact names listed in DoD criterion 3 for the interesting states. Each has KDoc.
- **Sample data:** `private val sample...` / `private fun sample...` in the same file, named with the `sample` prefix, referenced only by previews or other `sample...` declarations. Looks like the real thing (real-looking album names, a few photos). Never shared across files, never public/internal.
- **Previews never touch** Koin, a ViewModel, MediaStore, `PhotoLoader`, `CameraController`/CameraX/`PreviewView`, `rememberLauncherForActivityResult`, the network. Where a Content part needs a picture or a camera surface, take it as a parameter or a `@Composable` slot and pass a drawn stand-in in the preview.
- **Imports** for previews: `androidx.compose.ui.tooling.preview.PreviewLightDark` (`ui-tooling-preview` is added to the build by the Iteration).
- **KDoc** in plain English on every new class/function/composable/preview (`KdocCoverageTest` enforces it).
- Do not edit tests (except the new files named in your scope), baselines, `core/`, `reference/`, or any file outside the Declared File Scope.

## Task Graph

- T-001 - Preview tooling, Home + theme previews, host render test and hygiene test (depends on: -) - scope: `ui/home/HomeScreen.kt`, `ui/theme/KansoTheme.kt`, new `PreviewsRenderTest.kt`, new `PreviewHygieneTest.kt` - tier: Capable
- T-002 - Settings previews (depends on: T-001) - scope: `ui/settings/SettingsScreen.kt` - tier: Capable
- T-003 - Permission gate and refused panel previews (depends on: T-001) - scope: `ui/permission/AccessRefusedPanel.kt`, `ui/permission/PermissionGate.kt` - tier: Capable
- T-008 - Camera previews (depends on: T-001) - scope: `ui/camera/CameraScreen.kt` - tier: Capable
- T-005 - Tool screen previews (depends on: T-001) - scope: `ui/tool/ToolScreen.kt` - tier: Fast (extends the pattern T-001 sets)
- T-006 - Saved screen previews (depends on: T-001) - scope: `ui/saved/SavedScreen.kt` - tier: Fast (extends the pattern T-001 sets)
- T-007 - Gallery previews (depends on: T-001) - scope: `ui/gallery/GalleryScreen.kt` - tier: Capable
- T-004 - Navigation host previews (depends on: T-001) - scope: `navigation/KansoNavHost.kt` - tier: Capable
- T-009 - Preview coverage test and final checks (depends on: T-001..T-008) - scope: new `PreviewCoverageTest.kt`, and `PreviewsRenderTest.kt` (named-preview assertions) - tier: Capable
- T-010 - Full unit-test run passes reliably on Windows (depends on: T-009; Verifier gap, Iteration 7) - scope: `data/settings/SettingsStore.kt` (+ orchestrator: `app/build.gradle.kts` test block) - tier: Capable
- T-011 - Retake reliably deletes the shot on Windows (depends on: T-010; Verifier gap, Iteration 10) - scope: `ui/camera/CameraViewModel.kt`, `ui/camera/CameraScreen.kt` - tier: Capable

(`ui/...` and `navigation/...` are under `app/src/main/java/com/phongkaster/kanso/`; the test files under `app/src/test/java/com/phongkaster/kanso/previews/`.)

## Phase Grouping

| Phase | Tasks | Shared files the Iteration wires itself |
|---|---|---|
| 1 | T-001 | `gradle/libs.versions.toml`, `app/build.gradle.kts` (ui-tooling-preview, ComposablePreviewScanner) |
| 2 | T-002, T-003, T-008 | - |
| 3 | T-005, T-006, T-007 | - |
| 4 | T-004 | - |
| 5 | T-009 | - |
| 6 | T-010 | `app/build.gradle.kts` test block, if the fix is there |
| 7 | T-011 | - |

## Known Risks

- ComposablePreviewScanner 0.9.x vs Kotlin 2.3.20 / Compose BOM 2026.05: the version may need adjusting; T-001 finds out. Fallback is in the DoD Constraints.
- `@PreviewLightDark` expansion and private previews in the scanner need `includePrivatePreviews()` / multipreview support; T-001 verifies.
- Previews drawn on the Robolectric host cannot run AndroidView/PreviewView or load MediaStore images through Coil; stand-ins must be drawn.
- NavDisplay in a preview needs a ViewModelStoreOwner for its entry decorators; the stateless display may keep decorators in the stateful host (A-001).
- Restructuring screens risks moving a baseline pixel (padding order, Scaffold nesting); the 16 baselines catch it.
- Gradle runs are slow on this host (a clean full build took ~28 min); run them in the background and wait inside the turn.
