# RESUME BLOCK

> A derived cache, never a source of truth. On any disagreement with task files or git, this file is wrong.

- **Stage:** executing; T-002 done (Nav3 shell, Koin, Home, placeholders, Settings stub).
- **Next Phase:** T-003 only (see TASKS/T-003.md for scope). Then T-004, T-005 one per Phase. Existing: `KansoNavHost` (navigation/), `appModule` (di/), Koin started in KansoApplication, SettingsScreen/SettingsViewModel stubs.
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` (plus `:app:verifyRoborazziDebug` from T-005) | lint: `./gradlew :app:lintDebug`. Verified 2026-10-05. AGP 8.13 pins nav3 1.1.7 / lifecycle 2.10 (see PROJECT.md).
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
