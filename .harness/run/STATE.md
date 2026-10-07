# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.

## Current

- **Stage:** execute (D-001 approved; Phases 1-4 done)
- **Loop Branch:** loop/kanso-composable-previews
- **Next Phase:** 5 - T-009
- **DONE-candidate:** no

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
| T-009 | pending | new previews/PreviewCoverageTest.kt, previews/PreviewsRenderTest.kt | - |

## Assumptions

- A-001 (Tier 2): the navigation host is split into a stateful host and a stateless `KansoNavDisplay` so it can have a preview.

- A-003 (Tier 2): camera slot fillers `LivePreview`, `ShotView` and wrappers exempt from the coverage rule.

- A-002 (Tier 2): `sampleSizeFor` exempt from the private-`sample` rule (DoD 5 vs DoD 8).

- A-004 (Tier 2): gallery `PhotoThumbnail` exempt from the coverage rule.

## Signed off

- none yet
