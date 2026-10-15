# Verifier, Iteration 7 (2026-10-14)

Fresh run: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:verifyRoborazziDebug :app:lintDebug --continue` → `v7-full.log`.

| Criterion | Result |
|---|---|
| 1-6, 8, 10, 12, 14-22, 25, 28, 33 | 690 tests, 673 pass; the only 17 failures are `ToolScreenshotTest` pictures (below) |
| 14 | `git grep -i 'later run' -- app/src/main` empty |
| 23 | merged manifest: CAMERA, READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED, FOREGROUND_SERVICE, WAKE_LOCK, receiver permission (+ maxSdk storage); no INTERNET / ACCESS_NETWORK_STATE; `app/src/main/AndroidManifest.xml` unchanged since base and no dependency changed |
| 27 | **fails**: 45-54 have no baseline, 16,17,22,23,40-42 differ; `recordRoborazziDebug` denied |
| 29 | assembleDebug and lintDebug succeed; APK has lib/armeabi-v7a, arm64-v8a, x86_64; test task fails only on the 17 pictures |
| 30, 34 | `git diff ec2227f` on libs.versions.toml, core, opencv, libs, reference, app/build.gradle.kts: empty |
| R1 | empty |
| **R2** | **fails**: README.md:161 "built, but not called" |
| **31** | **fails**: README.md:18 "coming in a later run"; no "How each editing feature works" subsection for Edit, Remove background, Enhance; intro, saving and upright sections describe the pre-Run-8 app. `readme-paths.sh` passes but scans only lines 24-160 |
| 7, 9, 11, 13 | **not driven**: the A13 (R58W1120QVA) was in use (focus `com.languagelearning.app`), no emulator running, and the device wrapper has no operation to place a test photo |

Gap filed: T-008 (README). PARTIAL-candidate cleared.
