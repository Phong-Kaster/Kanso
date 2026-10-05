# RESUME BLOCK

> A derived cache, never a source of truth. On any disagreement with task files or git, this file is wrong.

- **Stage:** executing; T-004 done (ThemeColour Wallpaper/Baseline/Custom, seedColorScheme, picker in Settings, MainActivity passes themeColour).
- **Next Phase:** T-005 only (TASKS/T-005.md). Existing: `SettingsRepository` (data/settings), `createSettingsDataStore` (OkioStorage), `KansoTheme(themeMode)` (ui/theme, `colorSchemeFor(dark)`), `SettingsViewModel(repository)`, pixel helper `app/src/test/.../settings/ScreenPixels.kt`.
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` (plus `:app:verifyRoborazziDebug` from T-005) | lint: `./gradlew :app:lintDebug`. Verified 2026-10-05. AGP 8.13 pins nav3 1.1.7 / lifecycle 2.10 (see PROJECT.md).
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
