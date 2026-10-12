# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.
> Execution history lives in HISTORY.md, not here — this file must not grow with the run.

## Current

- **Stage:** approved, re-cut and prepared (Iteration 2): D-001 answered with one edit (every tool one tap, five round shortcuts on Home); the DoD, plan and task files re-cut; Phase 1 preparation done. Phase 1 itself (the three Workers) is the next Iteration.
- **Loop Branch:** loop/kanso-influx-layout (started from `a720960`)
- **Next Phase:** Phase 1 = T-001, T-002, T-003 (preparation already done; start with the Workers)
- **DONE-candidate:** no
- **Run Mode:** Autonomous

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | pending (Phase 1) | tab shell (`navigation/Kanso*`, `BackStackActions.kt`, `MyEdits.kt`), Home's Camera/Settings entries, Settings without Back, the tests that reach Settings and the camera, `NavTransitionTest`, `KansoBlueContrastTest` | - |
| T-002 | pending (Phase 1) | `data/media/*Saved*`, `ui/myedits/*`, `ui/common/SkeletonBlock.kt`, `PhotoThumbnail` visibility, `myedits/*` tests | - |
| T-003 | pending (Phase 1) | `ui/tool/*`, `ui/common/{BlurredBackdrop,RoundIconButton}.kt`, tool tests, `HomeTaps.kt`, `ReplaceNavigationTest` case (a) | - |
| T-004 | pending (Phase 2) | `ui/saved/*`, `navigation/EditedPhoto.kt`, saved tests, `MyEditsViewerNavigationTest` | - |
| T-005 | pending (Phase 2) | `ui/home/*` (title, carousel, the five tool shortcuts), Home-facing tests, `HomeTaps.kt` body | - |
| T-006 | pending (Phase 2) | `ui/gallery/GalleryScreen.kt`, `ui/camera/CameraScreen.kt`, their tests | - |
| T-007 | pending (Phase 3) | `ui/settings/SettingsScreen.kt`, settings tests | - |
| T-008 | pending (Phase 3) | `ui/home/*` (Recent edits), Home reach/recent/loading tests | - |
| T-009 | pending (Phase 3) | `core/colorpicker/.../HSVColorPicker.kt` (hex handling), picker tests | - |
| T-010 | pending (Phase 4) | `a11y/AccessibilityWalkTest.kt`, `theme/ThemeFollowTest.kt`, `a11y/OverPhotoContrastTest.kt` | - |
| T-011 | pending (Phase 4) | `navigation/ReplaceNavigationTest.kt`, `NavStateTest.kt`, `a11y/BottomClearanceTest.kt` | - |

## Signed off

None yet (no criterion awaits a signature before the build exists).

## Assumed criteria (Tier 3)

None. A-002 to A-024 are "Missing information" readings of the PRD, each shown in the DoD ("Readings the PRD leaves open"); the owner approved them as written at the gate. A-001 was reversed by the owner (D-001).

## Assumptions

- A-002 to A-024 in `ASSUMPTIONS.md`: Back from the tabs, the Camera button, close on a My edits photo, Saved's full-bleed reading, the viewer's button, mark and title, the gallery's camera button, card names, strip contents, permissions, the carousel kind, the bar with the keyboard, the blur fallback, one screen per tool, Replace, tab motion, thumbnails, the hex field and the engine-edit exception, new wording, the ADR, the selected pill's colour, Settings' title, a tab's state. A-001 (every tool one tap) is the owner's decision, not an assumption.
