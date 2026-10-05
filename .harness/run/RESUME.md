# RESUME BLOCK

> A derived cache, never a source of truth. On any disagreement with task files or git, this file is wrong.

- **Stage:** escalated until D-001 (DoD approval) is answered in `DECISIONS.md`; then executing.
- **Next Phase:** T-001 - scope: settings/root/app build files, `gradle/libs.versions.toml`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/**`, `KansoApplication.kt`, `MainActivity.kt`, `core/*/build.gradle.kts`, `opencv/build.gradle`, `core/common/.../AnalyticsUtil.kt`, `LaunchTest.kt`, `KdocCoverageTest.kt`. Then one task per Phase: T-002, T-003, T-004, T-005 in order.
- **Queued decisions:** 1 (D-001, DoD approval; blocks T-001 to T-005)
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` (plus `:app:verifyRoborazziDebug` from T-005) | lint: `./gradlew :app:lintDebug`. None verified yet (no app module exists); T-001 verifies them. Gradle wrapper 8.13 is cached locally; JDK 24; Android SDK at `C:\Users\MAC_PC\AppData\Local\Android\Sdk` (platforms 24-37, system images 27/28/34/36).
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
