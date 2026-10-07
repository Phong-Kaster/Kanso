# RESUME BLOCK

> Derived cache; on any disagreement with the task files or git, this file is wrong.

- **Stage:** bootstrap done; DoD approval D-001 pending (Autonomous mode: the one blocking gate)
- **Next Phase (1):** T-001 - scope: `ui/home/HomeScreen.kt`, `ui/theme/KansoTheme.kt`, new `app/src/test/.../previews/PreviewsRenderTest.kt`, new `.../previews/PreviewHygieneTest.kt`. The Iteration wires `gradle/libs.versions.toml` + `app/build.gradle.kts` (ui-tooling-preview `implementation`; ComposablePreviewScanner 0.9.3 `testImplementation`).
- **Later phases:** 2 = T-002 (settings), T-003 (permission), T-008 (camera); 3 = T-005 (tool, Fast), T-006 (saved, Fast), T-007 (gallery); 4 = T-004 (nav host); 5 = T-009 (coverage test). Scopes in PLAN.md / task files.
- **Queued decisions:** 1 - D-001 (DoD approval) blocks every task until the human approves it.
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` and `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` (from `.harness/knowledge/PROJECT.md`; heap 3g, run long ones in the background)
- **Base commit for "unchanged" checks:** `9a61275`
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus`
