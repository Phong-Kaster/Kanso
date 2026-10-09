# Kanso — Run 5: move the toolchain to AGP 9 and the current Compose libraries

The next run gives Kanso a Material 3 Expressive look. The Expressive components (`ButtonGroup`,
`FloatingToolbar`, `LoadingIndicator`, `MaterialShapes`, the wavy progress indicators) ship in Material3
**1.5.0**, not in the 1.4.0 Kanso resolves today. Material3 1.5.0-beta01 declares
`minAndroidGradlePluginVersion=9.1.0` and `minCompileSdk=37`.

Kanso is pinned to AGP 8.13. Run 1 had to lower Navigation 3 to 1.1.7 for the same reason. This run lifts
that pin. **It changes nothing the person using the app means to see**: it is the toolchain the next run
builds on.

## What this run delivers

1. **Android Gradle Plugin 9.1 or later** (the newest stable 9.x). The Gradle wrapper and the Kotlin and
   KSP versions move as far as AGP 9 requires.
2. **compileSdk 37.** Leave targetSdk where it is unless the upgrade forces it; if it does, say why.
3. **The current Compose libraries:**
   - a Compose BOM that resolves Material3 **1.5.0** (beta01 or newer), with the Expressive APIs usable
     behind `@ExperimentalMaterial3ExpressiveApi`;
   - Navigation 3 and `lifecycle-viewmodel-navigation3` at their newest stable;
   - CameraX at its newest stable;
   - each lower pin Runs 1-3 recorded in `.harness/knowledge/PROJECT.md` is removed, with what replaced it.
4. **The engine's modules and OpenCV build under AGP 9.** Adapt `core/*/build.gradle.kts` and
   `opencv/build.gradle` (for example `kotlinOptions` → `compilerOptions`, AGP 9's built-in Kotlin) only as
   far as the build requires. Their source code is not changed.
5. **The test toolchain keeps working**: Robolectric, Roborazzi, Compose UI Test and ComposablePreviewScanner
   at versions that run against compileSdk 37.

## Constraints

- `CLAUDE.md` applies in full: one class per file, one UiState per screen, a preview for every composable,
  KDoc in plain English, no ads, analytics, accounts or network permission, `reference/` unchanged, and the
  three debug ABIs.
- **Behaviour stays the same.** Every existing test keeps asserting what it asserts today, and the app asks
  for the same permissions.
- **Pixels may move only because a library draws differently.** Ask me in the DoD to approve re-recording
  the existing Roborazzi baselines. For every baseline that changes, keep its `_compare.png` in the run's
  evidence so I can look at each difference before I merge. A changed baseline that a library change does
  not explain is a defect.
- No new library beyond newer versions of the ones already there.

## Out of scope for this run

The new look (Material 3 Expressive, a blue primary colour, icons, an editor-style layout): that is the
next run. Also out of scope: the speed problems found on the Galaxy A13, and the human checks waiting in
`.harness/ISSUES.md`.
