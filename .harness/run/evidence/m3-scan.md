# Material 3 scan of `app/src/main` (DoD 19)

Written in Iteration 14 by reading `app/src/main` only. Paths are under `app/src/main/java/com/phongkaster/kanso/`.

## Each piece and the call that draws it

| Piece | Drawn by | Material 3? |
|---|---|---|
| The floating bar | `Surface(` at `navigation/KansoTabBar.kt:153`, a pill on `inverseSurface` | Yes: a Material 3 `Surface`. Material 3 1.5.0-beta01 has no floating pill that also leaves room for a separate round button, so the pill is a `Surface` around the items. |
| Its tabs | `ShortNavigationBarItem(` at `navigation/KansoTabBar.kt:172`, colours from `ShortNavigationBarItemDefaults.colors` (line 129) | Yes |
| The Camera button | `FilledIconButton(` at `navigation/KansoTabBar.kt:198`, `shapes = IconButtonDefaults.shapes()` | Yes |
| The carousel | `HorizontalUncontainedCarousel(` at `ui/home/HomeScreen.kt:478`, inside `HeroCarousel` (line 469) | Yes |
| The banners and their "Try now" buttons | `Card(` at `ui/home/HomeScreen.kt:561`; `Button(` at `ui/home/HomeScreen.kt:595`, `shapes = ButtonDefaults.shapes()` | Yes |
| The five round tool shortcuts on Home | `RoundIconButton(` at `ui/home/HomeScreen.kt:711`, which draws a `FilledTonalIconButton(` at `ui/common/RoundIconButton.kt:56`, `shapes = IconButtonDefaults.shapes()` | Yes |
| The arrow button on Home's recent heading | `RoundIconButton(` at `ui/home/HomeScreen.kt:753` | Yes |
| The round Back button | `RoundBackButton` (`ui/common/RoundIconButton.kt:75`) calling `RoundIconButton` | Yes |
| The round close button, Share and the gallery's camera button | `RoundIconButton(` at `ui/saved/SavedScreen.kt:479` (close), `ui/saved/SavedScreen.kt:542` (Share), `ui/gallery/GalleryScreen.kt:406` | Yes |
| The "Replace" chip | `ElevatedAssistChip(` at `ui/tool/ToolScreen.kt:491` | Yes |
| The full-width Save button | `Button(` at `ui/tool/ToolScreen.kt:354`, `shapes = ButtonDefaults.shapesFor(height)` | Yes |
| The full-width "Edit another" / "Edit" button | `Button(` at `ui/saved/SavedScreen.kt:550`, `shapes = ButtonDefaults.shapesFor(height)` (line 554) | Yes |
| The album chips | `FilterChip(` at `ui/gallery/GalleryScreen.kt:559`, `shapes = FilterChipDefaults.shapes()` | Yes |
| The Settings cards | `Surface(` at `ui/settings/SettingsScreen.kt:532` in `SettingsGroup` (line 527), `MaterialTheme.shapes.extraLarge` | Yes |
| The Settings rows | `Row`s inside `SettingsGroup`; `grep -rn "ListItem" app/src/main` prints nothing | Built by hand: a Material 3 `ListItem` cannot be given the row's icon badge and the card's rounded dividers the design asks for, and it would fix the row's height. |
| The recent strip | `LazyRow(` at `ui/home/HomeScreen.kt:837`, its cards `RecentEditCard` (line 871) | Yes (a lazy layout) |
| The My edits grid | `LazyVerticalGrid(` at `ui/myedits/MyEditsScreen.kt:319` (the loading grid at `:250`) | Yes (a lazy layout) |
| The shade on Saved | a `Box` with `Modifier.background(brush)` at `ui/saved/SavedScreen.kt:453-457` | Built by hand: Material 3 has no top-fade scrim for a photo; the brush is made from the theme's `surface`. |
| The placeholders | `SkeletonBlock` at `ui/common/SkeletonBlock.kt:78`, drawn with `drawBehind` | Built by hand: Material 3 1.5.0-beta01 has no skeleton or shimmer placeholder. |
| The page dots | a `Row` of `Box`es clipped to `CircleShape` at `ui/home/HomeScreen.kt:632-660` | Built by hand: no Material 3 page indicator in 1.5.0-beta01. |

## Every call to a button composable in `app/src/main`

Each has a `shapes` overload in material3 1.5.0-beta01 and passes `shapes =`:

| Call | `shapes =` |
|---|---|
| `navigation/KansoTabBar.kt:198` `FilledIconButton` | `IconButtonDefaults.shapes()` |
| `ui/camera/CameraScreen.kt:665` `OutlinedButton` | `ButtonDefaults.shapesFor(SHOT_ACTION_HEIGHT)` |
| `ui/camera/CameraScreen.kt:685` `Button` | `ButtonDefaults.shapesFor(SHOT_ACTION_HEIGHT)` |
| `ui/camera/CameraScreen.kt:734` `FilledTonalIconButton` | `IconButtonDefaults.shapes(...)` |
| `ui/camera/CameraScreen.kt:755` `FilledTonalIconButton` | `IconButtonDefaults.shapes(...)` |
| `ui/camera/CameraScreen.kt:816` `FilledIconButton` | `IconButtonDefaults.shapes(...)` |
| `ui/common/RoundIconButton.kt:56` `FilledTonalIconButton` | `IconButtonDefaults.shapes()` |
| `ui/gallery/GalleryScreen.kt:784` `TextButton` | `ButtonDefaults.shapes()` |
| `ui/home/HomeScreen.kt:595` `Button` | `ButtonDefaults.shapes()` |
| `ui/permission/AccessRefusedPanel.kt:95` `Button` | `ButtonDefaults.shapes()` |
| `ui/permission/PermissionGate.kt:216` `TextButton` | `ButtonDefaults.shapes()` |
| `ui/saved/SavedScreen.kt:550` `Button` | `ButtonDefaults.shapesFor(height)` |
| `ui/theme/KansoTheme.kt:263` `Button` (preview) | `ButtonDefaults.shapes()` |
| `ui/theme/KansoTheme.kt:264` `OutlinedButton` (preview) | `ButtonDefaults.shapes()` |
| `ui/tool/ToolScreen.kt:354` `Button` | `ButtonDefaults.shapesFor(height)` |

No call to `IconToggleButton`, `ToggleButton` or `FilledTonalButton` exists in `app/src/main`. No call lacks `shapes =`, so there is no defect to report.

Not covered by this file: the pixel and contrast checks of DoD 19 (`NavBarColourTest`, `KansoBlueContrastTest`) and the pictures (DoD 29).
