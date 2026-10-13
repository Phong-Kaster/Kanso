# STATE

> Machine-owned execution memory. Updated every iteration; committed atomically with the code it describes.
> Execution history lives in HISTORY.md, not here — this file must not grow with the run.

## Current

- **Stage:** Iteration 18 did T-017: pictures 28, 29, 34, 35 re-recorded with the bar; whole set green. PARTIAL-candidate (Tier-3 assumptions A-025..A-027 stop DONE). Next: the Verifier.
- **Loop Branch:** loop/kanso-influx-layout (started from `a720960`)
- **Next Phase:** none; the Verifier re-proves
- **DONE-candidate:** no. **PARTIAL-candidate:** yes (Iteration 18)
- **Run Mode:** Autonomous

## Progress

| Task | Status | Declared File Scope | Evidence |
|---|---|---|---|
| T-001 | complete (1a) | tab shell, tests | commit of Iteration 5; evidence/p1a-run5.log |
| T-002 | complete (1a) | `data/media/*Saved*`, `ui/myedits/*`, `ui/common/SkeletonBlock.kt`, `PhotoThumbnail` visibility, `myedits/*` tests | - |
| T-003 | complete | `ui/tool/*`, `ui/common/{BlurredBackdrop,RoundIconButton}.kt`, tool tests, `HomeTaps.kt`, `ReplaceNavigationTest` case (a) | - |
| T-004 | complete | `ui/saved/*`, `navigation/EditedPhoto.kt`, saved tests, `MyEditsViewerNavigationTest` | - |
| T-005 | complete | `ui/home/*` (title, carousel, the five tool shortcuts), Home-facing tests, `HomeTaps.kt` body | - |
| T-006 | complete | `ui/gallery/GalleryScreen.kt`, `ui/camera/CameraScreen.kt`, their tests | - |
| T-007 | complete | `ui/settings/SettingsScreen.kt`, settings tests | - |
| T-008 | complete | `ui/home/*` (Recent edits), Home reach/recent/loading tests | - |
| T-009 | complete | `core/colorpicker/.../HSVColorPicker.kt` (hex handling), picker tests | - |
| T-010 | complete (review F1/F3/F4 -> T-012) | `a11y/AccessibilityWalkTest.kt`, `theme/ThemeFollowTest.kt`, `a11y/OverPhotoContrastTest.kt` | - |
| T-011 | complete (review F2/F5 -> T-012) | `navigation/ReplaceNavigationTest.kt`, `NavStateTest.kt`, `a11y/BottomClearanceTest.kt` | - |
| T-012 | complete | review fixes in the six Phase 4 test files | - |
| T-013 | complete | `data/media/MediaStoreSavedPhotoSource.kt`, `ui/common/SkeletonBlock.kt`; Iteration: `git rm` `ic_add_photo_alternate.xml` | - |
| T-014 | complete (Iteration 14) | `evidence/{test-edits.md,orphans.sh,m3-scan.md,fixed-colours.md}` | - |
| T-016 | complete (Iteration 16) | `previews/PreviewsRenderTest.kt` (`requiredPreviewNames`), `evidence/test-edits.md` | - |
| T-017 | complete (Iteration 18) | `screenshots/{MyEditsScreenshotTest,HomeScreenshotTest}.kt`; Iteration: pictures 28, 29, 34, 35 | evidence/p9-whole.log |
| T-015 | complete except F4 (A-028) | `navigation/KansoTabBar.kt`, `ui/settings/SettingsScreen.kt`, `ui/home/HomeScreen.kt`, `ui/common/BlurredBackdrop.kt`, `ui/saved/SavedScreen.kt`; Iteration: `Tool.kt`, `KansoNavHost.kt` comments, pictures 4-6, 24, 25 | - |

## Signed off

None yet (no criterion awaits a signature before the build exists).

## Assumed criteria (Tier 3)

- 20 (A-025: catalog lines at the base vs DoD 40), 29 (A-027: Phases 2-3 compare images never kept), 33 (A-026: a line number moved). They can carry evidence but never make the run `DONE`.

Before Iteration 12: none. A-002 to A-024 are "Missing information" readings of the PRD, each shown in the DoD ("Readings the PRD leaves open"); the owner approved them as written at the gate. A-001 was reversed by the owner (D-001).

## Assumptions

- A-029 (Iteration 18, SHA in the next Iteration): pictures 28, 29, 34, 35 are re-recorded to show the bar DoD 22 promises.
- A-002 to A-024 in `ASSUMPTIONS.md`: Back from the tabs, the Camera button, close on a My edits photo, Saved's full-bleed reading, the viewer's button, mark and title, the gallery's camera button, card names, strip contents, permissions, the carousel kind, the bar with the keyboard, the blur fallback, one screen per tool, Replace, tab motion, thumbnails, the hex field and the engine-edit exception, new wording, the ADR, the selected pill's colour, Settings' title, a tab's state. A-001 (every tool one tap) is the owner's decision, not an assumption.
