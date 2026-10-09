# Expressive scan: `app/src/main` (DoD criterion 11)

Scanned on 2026-10-10: `app/src/main` only (never `app/src/test`), on branch `loop/kanso-m3-expressive-blue`, HEAD `aaeba85` plus the Phase 5 working tree (the Phase 5 checkpoint commit holds exactly this tree). All 61 Kotlin files under `app/src/main/java` were searched with ripgrep, with line numbers, for:
- `ButtonGroup(`, `HorizontalFloatingToolbar(`, `MaterialShapes.`, `Morph(`, `LoadingIndicator(`, `ContainedLoadingIndicator`, `WavyProgressIndicator`;
- every `…Button(` call, plus `FloatingActionButton`, `SplitButton` and `Fab(`.

Every call site was then re-read to see whether it passes `shapes =` (a `shape =` is reported as such and does not count). For each type, the overloads were read from `material3-android-1.5.0-beta01-sources.jar`, unzipped, at `commonMain/androidx/compose/material3/`. Paths below are relative to `app/src/main/java/com/phongkaster/kanso/`. Comment and KDoc mentions are named as "mention, no call". The first pass of this scan found four calls without `shapes =` that have an overload (rows 20 and P1 to P3); task T-018 fixed them and this file shows the state after the fix.

## 1. Expressive components

| Component | File:line | Note |
|---|---|---|
| `ButtonGroup(` | `ui/saved/SavedScreen.kt:638` | In `SavedActions` (:600). It has three `customItem`s (:653, :676, :696), and each draws an `ActionButton` (`Button`/`FilledTonalButton` with `shapes =`, section 2). Tag `saved-actions`. `overflowIndicator` at :642. KDoc mentions at :561, :567, :726 (no call). |
| `HorizontalFloatingToolbar(` | `ui/tool/ToolScreen.kt:583` | In `EditorToolbar` (:576), called at :327, with `expanded = true`. Its content is `Tool.editingTools.forEach { ToolToggle(...) }` (:587-594). `editingTools = entries` (`navigation/Tool.kt:47`) has five entries: EDIT, REMOVE_BACKGROUND, ENHANCE, RETOUCH, ARTISTIC (`Tool.kt:26-38`). Each `ToolToggle` is `IconToggleButton(` at `ToolScreen.kt:649` with `shapes = IconButtonDefaults.toggleableShapes()` at :653. |
| `MaterialShapes.` | `ui/saved/SavedScreen.kt:463` | `MaterialShapes.Circle` and `MaterialShapes.Cookie9Sided`, in `SavedMarkBadge` (:461). Import at :39. KDoc mention at :431 (no call). |
| `Morph(` | `ui/saved/SavedScreen.kt:463` | `remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }` (`androidx.graphics.shapes.Morph`, import :82), drawn with `morph.toPath(...)` at :482. KDoc mention at :435 (no call). |
| `LoadingIndicator(` | `ui/gallery/GalleryScreen.kt:497` | In `GalleryLoading` (:493), shown while photos load (called at :458). Tag `gallery-loading`. Not inside a `@Preview`. |
| `LoadingIndicator(` | `ui/tool/ToolScreen.kt:443` | In `SavingIndicator` (:442), inside the Save button while Save runs (called at :399). Not inside a `@Preview`. |
| `LoadingIndicator(` | `ui/tool/ToolScreen.kt:507` | In `PhotoLoadingIndicator` (:505), while the photo decodes (called at :465). Not inside a `@Preview`. |
| `LoadingIndicator(` | `ui/saved/SavedScreen.kt:530` | In `SavedLoadingIndicator` (:528), while the saved photo decodes (called at :350). Not inside a `@Preview`. |
| `LoadingIndicator(` | `ui/camera/CameraScreen.kt:454` | In `CameraLoading` (:451), shown when `camera.status == CameraStatus.Starting` (called at :411). Not inside a `@Preview`. |
| `FilterChip(` with `shapes =` (not on the Proof's list) | `ui/gallery/GalleryScreen.kt:556` | `shapes = FilterChipDefaults.shapes()` at :560, in `AlbumChip` (:555). |

None of these are in `app/src/main`: `ContainedLoadingIndicator`, `WavyProgressIndicator`, `CircularProgressIndicator(`, `LinearProgressIndicator(`, `SplitButton`, `FloatingActionButton`, `ExtendedFloatingActionButton`.

## 2. Buttons and icon buttons

Source references are to `material3-android-1.5.0-beta01-sources.jar!/commonMain/androidx/compose/material3/`.

| # | Call | File:line (enclosing composable) | Passes `shapes =`? | Reason or evidence |
|---|---|---|---|---|
| 1 | `IconButton` | `ui/gallery/GalleryScreen.kt:386` (`GalleryTopBar`, back) | yes, `IconButtonDefaults.shapes()` (:388) | overload `IconButton.kt:210` |
| 2 | `FilledTonalIconButton` | `ui/gallery/GalleryScreen.kt:399` (`GalleryTopBar`, open camera) | yes, `IconButtonDefaults.shapes()` (:401) | overload `IconButton.kt:784` |
| 3 | `TextButton` | `ui/gallery/GalleryScreen.kt:774` (`PartialAccessNotice`, share more) | yes, `ButtonDefaults.shapes()` (:776) | overload `Button.kt:864` |
| 4 | `IconButton` | `ui/tool/ToolScreen.kt:372` (`EditorTopBar`, back) | yes, `IconButtonDefaults.shapes()` (:374) | overload `IconButton.kt:210` |
| 5 | `Button` | `ui/tool/ToolScreen.kt:388` (`EditorTopBar`, Save) | yes, `ButtonDefaults.shapes()` (:390) | overload `Button.kt:236` |
| 6 | `IconToggleButton` (x5, one per tool) | `ui/tool/ToolScreen.kt:649` (`ToolToggle`) | yes, `IconButtonDefaults.toggleableShapes()` (:653) | overload `IconButton.kt:413` |
| 7 | `IconButton` | `ui/settings/SettingsScreen.kt:257` (`SettingsContent`, back) | yes, `IconButtonDefaults.shapes()` (:259) | overload `IconButton.kt:210` |
| 8 | `IconButton` | `ui/camera/CameraScreen.kt:258` (`CameraFrame`, back) | yes, `IconButtonDefaults.shapes()` (:260) | overload `IconButton.kt:210` |
| 9 | `OutlinedButton` | `ui/camera/CameraScreen.kt:672` (`RetakeButton`) | yes, `ButtonDefaults.shapesFor(SHOT_ACTION_HEIGHT)` (:674) | overload `Button.kt:713` |
| 10 | `Button` | `ui/camera/CameraScreen.kt:692` (`KeepButton`) | yes, `ButtonDefaults.shapesFor(SHOT_ACTION_HEIGHT)` (:694) | overload `Button.kt:236` |
| 11 | `FilledTonalIconButton` | `ui/camera/CameraScreen.kt:741` (`FlashButton`) | yes, `IconButtonDefaults.shapes(mediumRoundShape, mediumPressedShape)` (:743-746) | overload `IconButton.kt:784` |
| 12 | `FilledTonalIconButton` | `ui/camera/CameraScreen.kt:762` (`SwitchLensButton`) | yes, `IconButtonDefaults.shapes(mediumRoundShape, mediumPressedShape)` (:764-767) | overload `IconButton.kt:784` |
| 13 | `FilledIconButton` | `ui/camera/CameraScreen.kt:823` (`ShutterButton`) | yes, `IconButtonDefaults.shapes(CircleShape, largePressedShape)` (:825-828) | overload `IconButton.kt:557` |
| 14 | `Button` | `ui/home/HomeScreen.kt:279` (`OpenPhotoButton`) | yes, `ButtonDefaults.shapesFor(OPEN_PHOTO_HEIGHT)` (:281) | overload `Button.kt:236` |
| 15 | `FilledTonalButton` | `ui/home/HomeScreen.kt:312` (`CameraButton`) | yes, `ButtonDefaults.shapesFor(CAMERA_HEIGHT)` (:314) | overload `Button.kt:564` |
| 16 | `Button` | `ui/saved/SavedScreen.kt:763` (`ActionButton`, Share) | yes, `shapes = shapes` (:765), where `val shapes = ButtonDefaults.shapes()` (:760) | overload `Button.kt:236` |
| 17 | `FilledTonalButton` | `ui/saved/SavedScreen.kt:774` (`ActionButton`, Edit another and Home) | yes, `shapes = shapes` (:776), same value (:760) | overload `Button.kt:564` |
| 18 | `ButtonGroupDefaults.OverflowIndicator` (draws a `FilledIconButton` inside the library) | `ui/saved/SavedScreen.kt:642` (`SavedActions`) | no; passes neither `shape =` nor `shapes =` | No `shapes` overload for that type in material3 1.5.0-beta01. Its only signature is `ButtonGroup.kt:296` `OverflowIndicator(menuState, modifier, enabled, shape: Shape = IconButtonDefaults.filledShape, colors, interactionSource)` (`shape` at :300). |
| 19 | `RadioButton` (not on the Proof's list) | `ui/settings/SettingsScreen.kt:592` (`SettingsChoiceRow`), `onClick = null` | no; passes neither | No `shapes` overload for that type in material3 1.5.0-beta01. The only `RadioButton` is `RadioButton.kt:77` `(selected, onClick, modifier, enabled, colors, interactionSource)`, with no shape parameter at all. |
| 20 | `Button` | `ui/permission/AccessRefusedPanel.kt:95` (`AccessRefusedPanel`, "open settings") | yes, `ButtonDefaults.shapes()` (:97), with `contentPadding = ButtonDefaults.ContentPadding` (:99) so the button keeps its old size (pictures 12 to 15) | overload `Button.kt:236`. Fixed by T-018: the first pass found it without `shapes =`. The panel is drawn by `ui/gallery/GalleryScreen.kt:329` and by `ui/camera/CameraScreen.kt:292` (`CameraRefusedPanel`). |

These types on the Proof's list have no call in `app/src/main`: `ToggleButton` (it matches only as part of `IconToggleButton`; its overload, `ToggleButton.kt:157`, has `shapes` at :164). There are also no calls to `OutlinedIconButton`, `FilledIconToggleButton`, `FilledTonalIconToggleButton`, `OutlinedIconToggleButton`, `ElevatedButton`, `ElevatedToggleButton`, `OutlinedToggleButton`, `FilledTonalToggleButton`, `FloatingActionButton` or `ExtendedFloatingActionButton`.

Clickable controls that are not on the Proof's list: Home's tool cards are `Surface(onClick = …, shape = MaterialTheme.shapes.largeIncreased)` (`ui/home/HomeScreen.kt:408`, `:422`). The Settings entry is a `Row` with `.clickable(role = Role.Button)` (`ui/home/HomeScreen.kt:473`). Settings rows use `.selectable(...)` (`ui/settings/SettingsScreen.kt:564`).

## 3. Previews and samples

These calls are in `app/src/main` but are reached only from `@PreviewLightDark` functions.

| # | Call | File:line (enclosing composable) | Passes `shapes =`? | Reason or evidence |
|---|---|---|---|---|
| P1 | `Button` | `ui/theme/KansoTheme.kt:263` (`KansoThemePreview`, `@PreviewLightDark` at :246) | yes, `ButtonDefaults.shapes()` | overload `Button.kt:236`; fixed by T-018 |
| P2 | `OutlinedButton` | `ui/theme/KansoTheme.kt:264` (`KansoThemePreview`) | yes, `ButtonDefaults.shapes()` | overload `Button.kt:713`; fixed by T-018 |
| P3 | `TextButton` | `ui/permission/PermissionGate.kt:216` (`SampleGateSlot`) | yes, `ButtonDefaults.shapes()` | overload `Button.kt:864`; fixed by T-018. Reached only through `SampleGate`, whose callers are all previews; drawn only in the partial branch (`PermissionGatePartialPreview`). |

The real `AccessRefusedPanel` (row 20) is also drawn by previews: it is one call site and is counted once, in section 2.

## 4. Gaps

None. Every call in `app/src/main` whose type has a `shapes` overload passes `shapes =`. Two calls do not, because their type has no such overload: `ButtonGroupDefaults.OverflowIndicator` (`SavedScreen.kt:642`) and `RadioButton` (`SettingsScreen.kt:592`); their reasons are in section 2 (rows 18 and 19).
