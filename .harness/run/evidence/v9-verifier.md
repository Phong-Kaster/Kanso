# Verifier, Iteration 9 (2026-10-14)

Fresh run: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:verifyRoborazziDebug :app:lintDebug --continue` → `v9-full.log`.

| Criterion | Result |
|---|---|
| 1-6, 8, 10, 12, 14-22, 25, 28, 33 | 690 tests, 673 pass; the only 17 failures are `ToolScreenshotTest` pictures (below) |
| 14, R1 | `git grep -i 'later run' -- app/src/main` and `git grep -E 'editor_panel_\|EditorPanel' -- app/src` empty |
| 23 | merged manifest: READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED, CAMERA, WAKE_LOCK, FOREGROUND_SERVICE, receiver permission; no INTERNET / ACCESS_NETWORK_STATE; `AndroidManifest.xml` unchanged since base |
| **27** | **fails**: 45-54 have no baseline, 16, 17, 22, 23, 40, 41, 42 differ. `recordRoborazziDebug --tests "*ToolScreenshotTest*"` tried again: denied by the permission rules |
| 29 | assembleDebug and lintDebug succeed; APK has lib/armeabi-v7a, arm64-v8a, x86_64; the test task fails only on the 17 pictures |
| 30, 34 | `git diff --stat ec2227f` on libs.versions.toml, core, opencv, libs, reference, app/build.gradle.kts, manifest: empty |
| 31, R2 | `git grep -E 'UI only\|Not wired\|coming in a later run'` and `'built, but not called'` on README.md empty; one subsection per tool (lines 153-236) plus shared pipeline, upright, engine; `readme-paths.sh`: 79 paths, 0 missing |
| 7, 9, 11, 13 | **not driven**: `foreman-device.ps1 -Op focus` → `com.languagelearning.app`, the A13 is in use; no emulator |

No gap the engine can close: the pictures need a person to run the record command, the device criteria need a free device. Run ends DONE_PARTIAL.
