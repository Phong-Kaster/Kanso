# RESUME BLOCK

> A derived cache, never a source of truth. On any disagreement with task files or git, this file is wrong.

- **Stage:** executing; T-001 done (app builds, engine wired).
- **Next Phase:** T-002 only - scope: under `app/src/main/java/com/phongkaster/kanso/`: `navigation/**`, `ui/home/**`, `ui/placeholder/**`, `ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`, `di/**`; `res/values/strings.xml`, `values/themes.xml`, `values-night/themes.xml`; `app/src/test/.../navigation/**`. Then T-003, T-004, T-005 one per Phase.
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` (plus `:app:verifyRoborazziDebug` from T-005) | lint: `./gradlew :app:lintDebug`. Verified 2026-10-05. AGP 8.13 pins nav3 1.1.7 / lifecycle 2.10 (see PROJECT.md).
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
