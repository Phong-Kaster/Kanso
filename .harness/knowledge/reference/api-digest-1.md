# API digest: Material3 1.5.0-beta01, Navigation 3 1.2.0, Compose animation (Run 6)

Read from the sources jars of Material3 1.5.0-beta01 and Navigation 3 1.2.0 (see `PROJECT.md`, Run 6 sources),
2026-10-09. Signatures are copied from the sources; `M3/` = `commonMain/androidx/compose/material3/`.
A Worker has no network: if you need a signature that is not here, read the unzipped source in
`C:/Users/MAC_PC/AppData/Local/Temp/kanso-src/{m3,nav3ui,nav3rt}/` before you write the call.
Section I (Compose animation) is partly from memory, not from source: compile-check it with the Iteration.

## Opt-ins (read first)

`ExperimentalMaterial3ExpressiveApi` is a `@RequiresOptIn` (level ERROR). Use `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` per use site, never module-wide.

- **Need the opt-in:** `LoadingIndicator`, `ContainedLoadingIndicator` (all overloads), `LoadingIndicatorDefaults`, `MaterialShapes`, `RoundedPolygon.toShape`, `RoundedPolygon.toPath`, `Morph.toPath` (the last three are extension functions: `import androidx.compose.material3.toShape` / `toPath`).
- **Need NO opt-in:** `MaterialExpressiveTheme`, `MotionScheme`, `MaterialTheme.motionScheme`, the `...Emphasized` styles of `Typography`, the extra `Shapes` roles, `HorizontalFloatingToolbar`, `FloatingToolbarDefaults`, every `shapes =` overload of buttons and icon buttons, `IconButtonDefaults`, `ButtonDefaults`, the `ToggleButton` family, `ButtonGroup`, `ButtonGroupScope`, `ButtonGroupDefaults`, the wavy progress indicators, all of Navigation 3. (`ExpressiveApiTest` opts in for the whole class, so it proves nothing about which need it.)
- `androidx.graphics.shapes.{Morph, RoundedPolygon}` are on the app's compile classpath (Material3's POM lists graphics-shapes 1.0.1 as compile scope); `androidx.compose.animation` too (through foundation).

## A. Theme

```kotlin
// M3/MaterialTheme.kt:243
@Composable
public fun MaterialExpressiveTheme(
    colorScheme: ColorScheme? = null, motionScheme: MotionScheme? = null,
    shapes: Shapes? = null, typography: Typography? = null, content: @Composable () -> Unit,
)
// M3/MaterialTheme.kt:149   MaterialTheme.motionScheme  (@Composable getter: read it in composition, never inside a transitionSpec lambda)
// M3/MotionScheme.kt:45-133
public interface MotionScheme {
    public fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T>   // also fastSpatialSpec, slowSpatialSpec,
    public fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T>   //      fastEffectsSpec, slowEffectsSpec
    public companion object { fun standard(): MotionScheme; fun expressive(): MotionScheme }
}
```

Every spec is `spring(dampingRatio, stiffness)` with no visibility threshold. standard / expressive: defaultSpatial 0.9/700 vs **0.8/380**; fastSpatial 0.9/1400 vs **0.6/800**; slowSpatial 0.9/300 vs **0.8/200**; the three Effects specs are identical in both (1.0/1600, 1.0/3800, 1.0/800). Spatial expressive springs overshoot (damping < 1); use the `*EffectsSpec` springs for alpha and colour.

- `Typography` extra styles (property names): `displayLargeEmphasized`, `displayMediumEmphasized`, `displaySmallEmphasized`, `headlineLargeEmphasized`, `headlineMediumEmphasized`, `headlineSmallEmphasized`, `titleLargeEmphasized`, `titleMediumEmphasized`, `titleSmallEmphasized`, `bodyLargeEmphasized`, `bodyMediumEmphasized`, `bodySmallEmphasized`, `labelLargeEmphasized`, `labelMediumEmphasized`, `labelSmallEmphasized`.
- `Shapes` extra roles: `largeIncreased` (20dp), `extraLargeIncreased` (32dp), `extraExtraLarge` (48dp). Existing roles 4, 8, 12, 16, 28dp.
- **Gotcha (unconfirmed by a test):** `Typography()` may bind to the 15-parameter constructor, which copies each regular style into its `...Emphasized` twin (Typography.kt:517-531), so `MaterialTheme.typography.titleLargeEmphasized` can equal `titleLarge`. `Typography(fontFamily = FontFamily.SansSerif)` (Typography.kt:221) falls back to the real emphasized tokens. A unit test settles it: `assertNotEquals(t.titleLarge.fontWeight, t.titleLargeEmphasized.fontWeight)`.
- A null `colorScheme` at top level becomes the expressive LIGHT scheme: always pass the app's scheme. `MaterialExpressiveTheme` only changes defaults (no component reads the expressive flag).

## B. HorizontalFloatingToolbar

```kotlin
// M3/FloatingToolbar.kt:175
@Composable
public fun HorizontalFloatingToolbar(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    colors: FloatingToolbarColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
    contentPadding: PaddingValues = FloatingToolbarDefaults.ContentPadding,       // 8dp all sides
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    shape: Shape = FloatingToolbarDefaults.ContainerShape,                         // full pill
    leadingContent: @Composable (RowScope.() -> Unit)? = null,
    trailingContent: @Composable (RowScope.() -> Unit)? = null,
    expandedShadowElevation: Dp = FloatingToolbarDefaults.ContainerExpandedElevation,
    collapsedShadowElevation: Dp = FloatingToolbarDefaults.ContainerCollapsedElevation,
    content: @Composable RowScope.() -> Unit,
)
// :278 a second overload adds floatingActionButton: @Composable () -> Unit (2nd parameter) and animationSpec.
```

- `FloatingToolbarDefaults`: `ContainerSize` 64dp (a `heightIn(min)`), `ContentPadding` 8dp, `ScreenOffset` 16dp, `standardFloatingToolbarColors()` (container = surfaceContainer), `vibrantFloatingToolbarColors()` (container = primaryContainer), `exitAlwaysScrollBehavior(...)`.
- `content` scope is `RowScope`. `expanded = false` hides only `leadingContent`/`trailingContent`; `content` is always shown. TalkBack forces expanded.
- The toolbar **clips** its children to its pill shape and has no overflow menu: content wider than the pill is cut off (five 48dp buttons need about 5x48 + 16dp padding = 256dp; fine on a phone).
- `LocalContentColor` is set from the colours, so an `Icon` inside takes the tint. At 64dp height with 8dp padding it fits 48dp `IconButton` targets.
- Place it yourself (it does not position itself): `Modifier.align(Alignment.BottomCenter).padding(bottom = FloatingToolbarDefaults.ScreenOffset)` inside a `Box`, plus the navigation-bar inset.

## C. Icon buttons with `shapes`

```kotlin
// M3/IconButton.kt  (each also has a plain `shape: Shape` overload)
@Composable fun IconButton(onClick: () -> Unit, shapes: IconButtonShapes, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(), interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit)   // :210
@Composable fun IconToggleButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit, shapes: IconToggleButtonShapes, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonVibrantColors(), interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit)   // :413
// Same parameter order, differing default colors:
//   FilledIconButton :557, FilledIconToggleButton :667, FilledTonalIconButton :784, FilledTonalIconToggleButton :905,
//   OutlinedIconButton :1038 (+ border), OutlinedIconToggleButton :1154 (+ border)
class IconButtonShapes(val shape: Shape, val pressedShape: Shape = shape)
class IconToggleButtonShapes(val shape: Shape, val pressedShape: Shape = shape, val checkedShape: Shape = shape)
// M3/IconButtonDefaults.kt
@Composable fun shapes(): IconButtonShapes;  @Composable fun shapes(shape: Shape? = null, pressedShape: Shape? = null): IconButtonShapes
@Composable fun toggleableShapes(): IconToggleButtonShapes
@Composable fun toggleableShapes(shape: Shape? = null, pressedShape: Shape? = null, checkedShape: Shape? = null): IconToggleButtonShapes
// shape getters (@Composable get): standardShape, filledShape, outlinedShape, {extraSmall|small|medium|large|extraLarge}{RoundShape|SquareShape|PressedShape|SelectedRoundShape|SelectedSquareShape}
fun smallContainerSize(widthOption: IconButtonWidthOption = Uniform): DpSize   // extraSmall 32, small 40, medium 56, large 96, extraLarge 136 (uniform); icon sizes 20/24/24/32/40
@Composable fun filledIconButtonColors(containerColor, contentColor, disabledContainerColor, disabledContentColor): IconButtonColors   // all params have defaults; no-arg form exists
@Composable fun iconToggleButtonColors(containerColor, contentColor, disabledContainerColor, disabledContentColor, checkedContainerColor, checkedContentColor): IconToggleButtonColors
@Composable fun filledTonalIconToggleButtonColors(... same six ...): IconToggleButtonColors
```

Default `shapes()` is round and morphs to a small corner when pressed; default `toggleableShapes()` morphs to a medium corner when checked. Shapes morph only if every shape in the set is a `CornerBasedShape` and the instances differ.

- Do NOT use (they do not exist): `IconButtonDefaults.defaultShape`, `animatedShape`, `smallSquareShape()` as a function.
- **48 dp:** `IconButton`/`IconToggleButton` apply `minimumInteractiveComponentSize().size(40.dp)` themselves; the Filled/Tonal/Outlined variants sit on `Surface(onClick)` which applies `minimumInteractiveComponentSize()`. A caller `Modifier.size(32.dp)` DEFEATS the 48dp minimum (the layout is pinned at 32); to keep it write `Modifier.minimumInteractiveComponentSize().size(IconButtonDefaults.extraSmallContainerSize())`. A bigger caller size (56dp) is fine.
- A `testTag` in `modifier` lands on the clickable node, with `Role.Button` (`Role.Checkbox` for toggles).
- The two `IconToggleButton` overloads have different default colours (`iconToggleButtonColors()` with `shape`, `iconToggleButtonVibrantColors()` with `shapes`).

```kotlin
FilledIconButton(onClick = onSave, shapes = IconButtonDefaults.shapes(),
    modifier = Modifier.testTag("save").size(IconButtonDefaults.mediumContainerSize())) {
    Icon(painterResource(R.drawable.ic_save), contentDescription = "Save") }
FilledTonalIconToggleButton(checked = on, onCheckedChange = onChange, shapes = IconButtonDefaults.toggleableShapes()) {
    Icon(painterResource(R.drawable.ic_flash_on), contentDescription = "Flash") }
```

