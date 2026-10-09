# Verifier, Run 5 (Iteration 5, 2026-10-09)

Re-proved on the committed tree of `c0871f3` (HEAD at the start of this Iteration; this Iteration changed
no code). Every Gradle command ran from the repository root in one sequential script; exit codes and
times are in `verifier-steps.txt`. `assembleDebug` and the module builds were up to date (Gradle found
every input unchanged since Phase 3 built the same code); the unit tests (after `cleanTestDebugUnitTest`),
`verifyRoborazziDebug`, `assembleRelease` and a forced lint analysis (`--rerun`) ran fresh.

| Criterion | Result | Evidence |
|---|---|---|
| 1 | holds | `test.log` BUILD SUCCESSFUL (1 min 49 s); summed XML: tests 238, failures 0, errors 0, skipped 0 (`verifier-test-counts.txt`); `@Test` at HEAD: 238 |
| 2 | holds | (a) `comm` prints nothing; 231 base names all present, 238 at HEAD, none duplicated (`head-testnames.txt`); (b) no `@Ignore`/`assume`, exit 1; (c) no existing test file modified (`git diff --diff-filter=M` empty; the only change under `app/src/test/java` is the added `ExpressiveApiTest.kt`), so the six guard tests have no diff; (d) `removed.txt` empty |
| 3 | **not driven** | the Galaxy A13 (`R58W1120QVA`, the only device attached) was in use: `-Op focus` reported `com.fotome.aiphoto.generator` from 18:34 to 18:39 and `com.android.chrome` once in between; nothing was installed, started or read |
| 4 | holds | `head-badging.txt` permissions/features identical to base; INTERNET/ACCESS_NETWORK_STATE 0; manifest diff empty |
| 5 | holds | `git diff --stat 76ecb97 d816bde -- app/src/test/screenshots` empty; `phase1-roborazzi-summary.json` 16/16 unchanged |
| 6 | holds | `verify-roborazzi.log` exit 0, `verify-roborazzi-summary.json` 16 total, 0 changed; no commit touches `app/src/test/screenshots`; `baseline-diffs/README.md` says "no baseline moved" for each Phase |
| 7 | pre-check holds: nothing moved | no comparison image exists to open (none was made: nothing moved); awaits the person's "nothing moved" |
| 8 | holds | `agp = "9.4.1"`; `buildenv.txt` `com.android.tools.build:gradle:9.4.1`; Google Maven newest 9.x today: 9.4.1 |
| 9 | holds | Gradle 9.6.0 (`verifier-gradle-version.txt`), wrapper SHA-256 matches services.gradle.org; catalog `kotlin = "2.4.21"`, Maven Central newest 2.4.21; `buildenv.txt` `kotlin-gradle-plugin:2.2.10 -> 2.4.21`; no KSP; PROJECT.md states both and why |
| 10 | holds | badging: `compileSdkVersion='37'`, `targetSdkVersion:'36'`, `minSdkVersion:'27'` |
| 11 | every proof command holds; **assumed** (A-007) | `modules.log` BUILD SUCCESSFUL (18 assemble tasks); no `android.builtInKotlin`/`newDsl`, no `kotlin-android`/`kotlinOptions`, no added `android.*` line; engine source diff empty |
| 12 | holds | BOM `2026.09.00`; `material3.txt` 1.5.0-beta01 on `debugRuntimeClasspath`, `verifier-insight-material3-compile.txt` the same on `debugCompileClasspath`; `verifier-insight-ui.txt` 1.12.1; newest 1.5.x on Google Maven today: 1.5.0-beta01; comment with BOM above it in both files; `ExpressiveApiTest` names all five components, 7/7 pass, no `captureRoboImage`, no composable of its own; nothing Expressive in `app/src/main` |
| 13 | holds | catalog nav3 1.2.0, lifecycleNav3 2.11.0, camerax 1.6.2, robolectric 4.17, roborazzi 1.76.0, composablePreviewScanner 0.9.3, each the newest on its Maven metadata today; `verifier-insight-*` resolve navigation3-runtime 1.2.0, lifecycle-viewmodel-navigation3 2.11.0, camera-core 1.6.2 |
| 14 | holds | `head-catalog-coordinates.txt` equals base (67 unique); plugin ids differ only by `org.jetbrains.kotlin.android` removed; no new colon coordinate added to a build script |
| 15 | holds | `lib/arm64-v8a/`, `lib/armeabi-v7a/`, `lib/x86_64/`; `head-native-libs.txt` identical to base (34) |
| 16 | holds | `build-lint.log` exit 0; `release.log` exit 0 (2 min 1 s, `app-release-unsigned.apk` 282 MB); `lint-rerun.log` fresh analysis, 0 errors, 55 warnings; no lint baseline or `lint.xml`; no lint, suppression or check-loosening line added |
| 17 | holds | `reference/` unchanged since `a0d9a79`; banned-word grep exit 1; the six guard tests pass (10, 14, 3, 7, 6, 5 tests); `ExpressiveApiTest.kt` holds one type and the KDoc awk prints nothing |
| R1, R2, R3 | hold | each grep exits 1; PROJECT.md's pin line names what replaced each pin |

Live metadata, read today with `curl`: AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Material3 newest
1.5.x 1.5.0-beta01, navigation3-runtime 1.2.0, lifecycle-viewmodel-navigation3 2.11.0, camera-core 1.6.2,
Robolectric 4.17, Roborazzi 1.76.0, ComposablePreviewScanner 0.9.3; Gradle's newest is 9.8.1 (the run uses
the floor AGP names, 9.6.0, as approved). Nothing newer appeared after approval.

Read by the Verifier: `ExpressiveApiTest.kt` (each test pauses the clock before `setContent`, composes the
component inside the lambda, asserts it is displayed, and for the four untexted components checks for a
painted pixel inside its bounds).
