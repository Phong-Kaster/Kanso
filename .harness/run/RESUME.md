# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** escalated (waiting for D-001). Run Mode: Autonomous. Branch `loop/kanso-agp9-toolchain`, base `76ecb97`.
- **Next Phase (1):** after D-001 is answered in `DECISIONS.md` (consume it, §6.2: the DoD approval and any `## Baselines` list, then execute)
  - T-001 - AGP 9.4.1 toolchain move, libraries untouched (Kotlin stays 2.3.20) - scope: wrapper (`gradle/wrapper/**`, `gradlew*`; regenerate on the OLD Gradle first), `gradle/libs.versions.toml`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `app/build.gradle.kts` (the Iteration wires these) and `core/*/build.gradle.kts`, `opencv/build.gradle` (one Capable Worker, with the traps of PLAN "Known Risks"), `knowledge/PROJECT.md` - Capable. No baseline may move; `:app:assembleRelease` in the background; check native libs vs `evidence/base-native-libs.txt` (NDK 27.0.12077973 at base) and that `buildEnvironment` resolves the Kotlin plugin to the catalog's `kotlin`.
  - then Phase 2 = T-002 (BOM 2026.09.00, nav3 1.2.0, lifecycle-nav3 2.11.0, Kotlin 2.4.21; Worker only for what a compile error names; baseline procedure of PLAN, comparison images `<name>_phase2_compare.png`), Phase 3 = T-003 (Material3 1.5.0-beta01 above the stable BOM, `ExpressiveApiTest` by a Worker: no `captureRoboImage`, no composable function; `_phase3_compare.png`), then the Verifier (DONE-candidate/PARTIAL-candidate, §14.4)
  - if D-001 answers "use the beta BOM" (`compose-bom-beta:2026.10.00`), Phase 3 and DoD 12 are re-cut by a Tier-3 amendment first
- **Queued decisions:** 1 - D-001 (DoD approval + `## Baselines` list) blocks T-001, T-002, T-003
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands (base, AGP 8.13):** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | UI: `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` | modules: `:opencv:assembleDebug :opencv:assembleRelease :core:<m>:assembleRelease`. Start each with `cd /d/Valve/Personal/Kanso &&`, redirect to `.harness/run/evidence/<name>.log`, run under `timeout`, never pipe an exit code; long ones in the background.
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` — pass `model` explicitly on every dispatch; the Reviewer is always Capable.
- **Base evidence (in `.harness/run/evidence/`):** `base-badging.txt` (permissions, sdk lines, ABIs of the debug APK at base), `base-testnames.txt` (231 `file:name` pairs), `base-catalog-coordinates.txt` (68), `base-catalog-plugins.txt`, `base-libs.versions.toml`, `base-native-libs.txt` (34 `.so` with CRCs), `base-buildenv.txt`.
- **Facts the plan rests on (2026-10-09):** AGP newest stable 9.4.1, needs Gradle 9.6.0+; the stable Compose BOM (newest 2026.09.00) maps Material3 to 1.4.0, only `compose-bom-beta:2026.10.00` maps 1.5.0-beta01 (with all of Compose at 1.13.0-beta01); Material3 1.5.0-beta01 needs only Compose 1.12.0; nav3 1.2.0, lifecycle-viewmodel-navigation3 2.11.0, CameraX 1.6.2, Kotlin 2.4.21 are the newest stable; the wrapper at base is Gradle 8.14.3; base NDK 27.0.12077973.
- **Plan fixes from the independent critique (kept):** greps carry `':!.harness'`; the screenshot recorder is run with `-Tests "com.phongkaster.kanso.screenshots.*"`, comparison images are per phase and only from this run's `results-summary.json`; the Verifier adds the Phase 3 SHA to `phase-commits.md`.
