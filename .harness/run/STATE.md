# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.

## Current

- **Stage:** verified, ended DONE_PARTIAL (Iteration 12): every machine criterion re-proved; 7, 10, 12 await a person
- **Loop Branch:** loop/kanso-composable-previews
- **Next Phase:** none
- **DONE-candidate:** no (Verifier Iteration 12 held every machine criterion; signatures outstanding, so DONE_PARTIAL, no Cleanup Commit)

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete | ui/home/HomeScreen.kt, ui/theme/KansoTheme.kt, new previews/PreviewsRenderTest.kt, new previews/PreviewHygieneTest.kt | build, tests, verifyRoborazzi, lint green (Iteration 2) |
| T-002 | complete | ui/settings/SettingsScreen.kt | build, tests, verifyRoborazzi, lint green (Iteration 3) |
| T-003 | complete | ui/permission/AccessRefusedPanel.kt, ui/permission/PermissionGate.kt | build, tests, verifyRoborazzi, lint green (Iteration 3) |
| T-008 | complete | ui/camera/CameraScreen.kt | build, tests, verifyRoborazzi, lint green (Iteration 3) |
| T-005 | complete | ui/tool/ToolScreen.kt | build, tests, verifyRoborazzi, lint green (Iteration 4) |
| T-006 | complete | ui/saved/SavedScreen.kt | build, tests, verifyRoborazzi, lint green (Iteration 4) |
| T-007 | complete | ui/gallery/GalleryScreen.kt | build, tests, verifyRoborazzi, lint green (Iteration 4) |
| T-004 | complete | navigation/KansoNavHost.kt | build, tests, verifyRoborazzi, lint green (Iteration 5) |
| T-009 | complete | new previews/PreviewCoverageTest.kt, previews/PreviewsRenderTest.kt, previews/PreviewHygieneTest.kt | build, tests, verifyRoborazzi, lint green (Iteration 6) |
| T-010 | complete | data/settings/SettingsStore.kt | 5/5 unit runs and 3/3 Roborazzi runs green, no rerun (Iteration 9) |
| T-011 | complete | ui/camera/CameraViewModel.kt, ui/camera/CameraScreen.kt | CameraScreenTest 10/10, full unit 3/3, Roborazzi 3/3, lint green (Iteration 11) |

## Assumptions

- A-001 (Tier 2): the navigation host is split into a stateful host and a stateless `KansoNavDisplay` so it can have a preview.

- A-003 (Tier 2): camera slot fillers `LivePreview`, `ShotView` and wrappers exempt from the coverage rule.

- A-002 (Tier 2): `sampleSizeFor` exempt from the private-`sample` rule (DoD 5 vs DoD 8).

- A-004 (Tier 2): gallery `PhotoThumbnail` exempt from the coverage rule.

## Verifier (Iteration 12, on 177b1f8)

Fresh runs: `assembleDebug` 0; `testDebugUnitTest` 0 (198 tests, 0 failed, incl. PreviewCoverageTest, PreviewsRenderTest, PreviewHygieneTest, KdocCoverageTest); `verifyRoborazziDebug` 0; `lintDebug` 0. Machine criteria 1-6, 8, 9, 11, 13, 14 hold.
Driven: 7 by criterion 2 only (every preview draws on the host, light and dark); the Android Studio pane is not drivable. 10 not driven: the only device is the owner's Galaxy A13 (R58W1120QVA), focused on another app (in use); no emulator attached.

## Signed off

- none yet (7, 10, 12 await a person; see `.harness/ISSUES.md` "Awaiting a person, Run 3")
