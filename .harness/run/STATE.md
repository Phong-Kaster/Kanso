# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.

## Current

- **Stage:** bootstrap (done; waiting for the DoD approval, D-001)
- **Loop Branch:** loop/kanso-composable-previews
- **Next Phase:** 1 - T-001
- **DONE-candidate:** no

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | pending | ui/home/HomeScreen.kt, ui/theme/KansoTheme.kt, new previews/PreviewsRenderTest.kt, new previews/PreviewHygieneTest.kt | - |
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

## Signed off

- none yet
