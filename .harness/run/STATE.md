# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.

## Current

- **Stage:** execute (D-001 approved; Phase 1 done)
- **Loop Branch:** loop/kanso-composable-previews
- **Next Phase:** 2 - T-002, T-003, T-008
- **DONE-candidate:** no

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete | ui/home/HomeScreen.kt, ui/theme/KansoTheme.kt, new previews/PreviewsRenderTest.kt, new previews/PreviewHygieneTest.kt | build, tests, verifyRoborazzi, lint green (Iteration 2) |
| T-002 | pending | ui/settings/SettingsScreen.kt | - |
| T-003 | pending | ui/permission/AccessRefusedPanel.kt, ui/permission/PermissionGate.kt | - |
| T-008 | pending | ui/camera/CameraScreen.kt | - |
| T-005 | pending | ui/tool/ToolScreen.kt | - |
| T-006 | pending | ui/saved/SavedScreen.kt | - |
| T-007 | pending | ui/gallery/GalleryScreen.kt | - |
| T-004 | pending | navigation/KansoNavHost.kt | - |
| T-009 | pending | new previews/PreviewCoverageTest.kt, previews/PreviewsRenderTest.kt | - |

## Assumptions

- A-001 (Tier 2): the navigation host is split into a stateful host and a stateless `KansoNavDisplay` so it can have a preview.

- A-002 (Tier 2): `sampleSizeFor` exempt from the private-`sample` rule (DoD 5 vs DoD 8).

## Signed off

- none yet
