# Verifier, Iteration 9 (2026-10-26), HEAD 5d37d8d

- `assembleDebug testDebugUnitTest lintDebug --continue`: exit 0 (`verifier-full.log`; Gradle reused up-to-date test results).
- `verifyRoborazziDebug`: exit 1, 941 tests, 15 failed, all older pictures outside D-001's list (`verifier-roborazzi.log`; A-001). Criteria 27 and 34 are not met.
- 26: merged debug manifest permissions are CAMERA, FOREGROUND_SERVICE, READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED, WAKE_LOCK and the dynamic-receiver one; no INTERNET or ACCESS_NETWORK_STATE.
- 29: `ic_duplicate.xml` uses a 960 viewport and no tint.
- 30: the modified baselines since fadd7c4 are exactly 51, 52 and 55-68, and each has `baseline-diffs/<name>.old.png`.
- 32: 60 `img_sticker_*.webp`, each with the same sha256 as its reference file; no `decal_`.
- 33: `git diff fadd7c4 -- reference/` is empty; no imports of `reference/` (the one hit in EngineSegmenter is a KDoc path to `.harness/knowledge/reference/`).
- 35: the skip grep is empty.
- 37: no lines added to `libs.versions.toml` or `app/build.gradle.kts`.
- 39: ADR-004 exists, and CONTEXT.md defines Sticker and the sticker catalogue and lists five EditTools.
- 38/40: gap found. `docs/features/edit.md:108` names `ui/tool/EditPill.kt`, which does not exist. A fresh reader found 8 false or imprecise sentences plus 2 stale KDocs. Filed as T-007.
- Not driven: 23, 25, 28, 31 and 40's human half. They are driven at the next Verifier gate, once the gaps are closed.
