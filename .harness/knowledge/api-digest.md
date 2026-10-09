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

## D. Text buttons with `shapes`, toggle buttons

```kotlin
// M3/Button.kt:236  (ElevatedButton, FilledTonalButton, OutlinedButton, TextButton: same parameter list, different default colors/elevation/border)
@Composable fun Button(onClick: () -> Unit, shapes: ButtonShapes, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(), elevation: ButtonElevation? = ButtonDefaults.buttonElevation(), border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit)
class ButtonShapes(val shape: Shape, val pressedShape: Shape)            // both required
// ButtonDefaults: MinWidth 58dp, MinHeight 40dp, ExtraSmallContainerHeight 32, MediumContainerHeight 56, LargeContainerHeight 96, ExtraLargeContainerHeight 136
//   (there is no SmallContainerHeight: small is MinHeight); IconSize 18dp, IconSpacing 8dp
@Composable fun shapes(): ButtonShapes;  @Composable fun shapes(shape: Shape? = null, pressedShape: Shape? = null): ButtonShapes
@Composable fun shapesFor(buttonHeight: Dp): ButtonShapes
fun contentPaddingFor(buttonHeight: Dp, hasStartIcon: Boolean = false, hasEndIcon: Boolean = false): PaddingValues
fun iconSizeFor(buttonHeight: Dp): Dp;  fun iconSpacingFor(buttonHeight: Dp): Dp;  @Composable fun textStyleFor(buttonHeight: Dp): TextStyle
// M3/ToggleButton.kt:157 (ElevatedToggleButton, FilledTonalToggleButton [sic: no "TonalToggleButton"], OutlinedToggleButton)
@Composable fun ToggleButton(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier,
    buttonSize: ToggleButtonSize = ToggleButtonDefaults.size, enabled: Boolean = true, icon: @Composable (() -> Unit)? = null,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(buttonSize), colors: ToggleButtonColors = ToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = ToggleButtonDefaults.elevation(), border: BorderStroke? = null,
    contentPadding: PaddingValues = ..., interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit)
// ToggleButtonSize.{ExtraSmall, Small, Medium, Large, ExtraLarge}; ToggleButtonShapes(shape, pressedShape, checkedShape) all required; no ToggleButtonDefaults.shapes(), use shapesFor(...)
```

```kotlin
val h = ButtonDefaults.MediumContainerHeight
Button(onClick = onSave, shapes = ButtonDefaults.shapesFor(h), modifier = Modifier.heightIn(min = h).testTag("save"),
    contentPadding = ButtonDefaults.contentPaddingFor(h, hasStartIcon = true)) {
    Icon(painterResource(R.drawable.ic_save), null, Modifier.size(ButtonDefaults.iconSizeFor(h)))
    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(h)))
    Text("Save", style = ButtonDefaults.textStyleFor(h)) }
```

Buttons sit on `Surface(onClick)`, so the layout is at least 48dp tall (visual minimum 40dp). The legacy `Button(onClick, modifier, enabled, shape, ...)` still exists; passing `shapes =` selects the morphing overload.

## E. ButtonGroup

```kotlin
// M3/ButtonGroup.kt:129 - the ONLY public overload in 1.5.0-beta01
@Composable
public fun ButtonGroup(
    overflowIndicator: @Composable (ButtonGroupMenuState) -> Unit,
    modifier: Modifier = Modifier,
    expandedRatio: Float = ButtonGroupDefaults.ExpandedRatio,                    // 0.15f
    horizontalArrangement: Arrangement.Horizontal = ButtonGroupDefaults.HorizontalArrangement,   // spacedBy(12.dp)
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: ButtonGroupScope.() -> Unit,                                         // NOT @Composable
)
public sealed interface ButtonGroupScope {
    fun Modifier.weight(weight: Float): Modifier                                  // > 0 or NaN, else it throws
    fun Modifier.animateWidth(interactionSource: InteractionSource): Modifier     // lives on the scope, not on ButtonGroupDefaults
    fun Modifier.animateWidth(interactionSource: InteractionSource, compressionLimit: Dp): Modifier
    fun Modifier.align(alignment: Alignment.Vertical): Modifier
    fun clickableItem(onClick: () -> Unit, label: String, icon: (@Composable () -> Unit)? = null, weight: Float = Float.NaN, enabled: Boolean = true)
    fun toggleableItem(checked: Boolean, label: String, onCheckedChange: (Boolean) -> Unit, icon: (@Composable () -> Unit)? = null, weight: Float = Float.NaN, enabled: Boolean = true)
    fun customItem(buttonGroupContent: @Composable () -> Unit, menuContent: @Composable (ButtonGroupMenuState) -> Unit)
}
// ButtonGroupDefaults: ExpandedRatio, HorizontalArrangement, ConnectedSpaceBetween 2.dp, connected{Leading|Middle|Trailing}ButtonShapes(...): ToggleButtonShapes,
//   @Composable OverflowIndicator(menuState: ButtonGroupMenuState, modifier = Modifier, enabled = true, shape = IconButtonDefaults.filledShape, colors = ..., interactionSource = null)
// ButtonGroupMenuState(initialIsShowing = false) { val isShowing; fun dismiss(); fun show() }
```

- **A test tag or Modifier reaches an item ONLY through `customItem`.** `clickableItem` / `toggleableItem` take no modifier (each builds its own `Button`/`ToggleButton`); a test can find them only by their label text. In `customItem { buttonGroupContent }` put `Modifier.testTag(...)` on your own button; `animateWidth` and `weight` resolve there through the outer scope. A `modifier` given to `ButtonGroup` reaches only the group's layout node.
- The `content` lambda is not composable: resolve every `stringResource(...)` BEFORE calling `ButtonGroup`.
- Items are `Button`/`ToggleButton`, at least 48dp tall in layout. Labels are `maxLines = 1, softWrap = false`.
- **Overflow:** if the items' intrinsic widths do not fit, later items move into a `DropdownMenu` opened by `overflowIndicator` (`menu.show()`). With `overflowIndicator = { }` the items that do not fit become UNREACHABLE. Nothing wraps. To never hide an item: give every item a `weight` (then the weighted widths fill the row and all fit) and/or pass `ButtonGroupDefaults.OverflowIndicator(it)`. Large text with weighted items draws the label past its edge: leave room.
- Pressing an item widens it (up to 15%) and compresses its neighbours.

```kotlin
ButtonGroup(overflowIndicator = { menu -> ButtonGroupDefaults.OverflowIndicator(menuState = menu) }) {
    customItem(
        buttonGroupContent = {
            val source = remember { MutableInteractionSource() }
            Button(onClick = onShare, interactionSource = source, modifier = Modifier.testTag("share").animateWidth(source)) { Text(shareLabel) }
        },
        menuContent = { menu -> DropdownMenuItem(text = { Text(shareLabel) }, onClick = { onShare(); menu.dismiss() }) },
    )
}
```

## F. LoadingIndicator

```kotlin
// M3/LoadingIndicator.kt  (@ExperimentalMaterial3ExpressiveApi)
@Composable fun LoadingIndicator(progress: () -> Float, modifier: Modifier = Modifier, color: Color = LoadingIndicatorDefaults.indicatorColor,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.DeterminateIndicatorPolygons)                                  // :98
@Composable fun LoadingIndicator(modifier: Modifier = Modifier, color: Color = LoadingIndicatorDefaults.indicatorColor,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons)                                // :136  (the one Kanso uses)
@Composable fun ContainedLoadingIndicator(modifier: Modifier = Modifier, containerColor: Color = LoadingIndicatorDefaults.containedContainerColor,
    indicatorColor: Color = LoadingIndicatorDefaults.containedIndicatorColor, containerShape: Shape = LoadingIndicatorDefaults.containerShape,
    polygons: List<RoundedPolygon> = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons)                                // :227
// (a determinate ContainedLoadingIndicator(progress = ...) exists too, :184)
// LoadingIndicatorDefaults: ContainerWidth 48dp, ContainerHeight 48dp, IndicatorSize 38dp, containerShape, indicatorColor (primary),
//   containedIndicatorColor (onPrimaryContainer), containedContainerColor (primaryContainer)  - lower-case properties; "IndicatorColor"/"ContainerColor" do not exist
```

- The box is a fixed 48 x 48 dp AFTER the caller's modifier (a caller `Modifier.size` overrides it). Semantics: `progressSemantics()` (indeterminate range), so a content description set on the modifier is the node's name.
- **The indeterminate form animates forever** (`while (true)` morph every 650 ms plus a 4666 ms rotation) unless the coroutine context carries an `InfiniteAnimationPolicy`. Whether the Compose test's policy cancels it is UNCONFIRMED. Proven pattern (`ExpressiveApiTest`): set `mainClock.autoAdvance = false` before `setContent`, then `advanceTimeBy`. A determinate one goes idle normally.
- Wavy indicators (not used by Kanso): `LinearWavyProgressIndicator`, `CircularWavyProgressIndicator`, no opt-in.

## G. Shape morphing

```kotlin
// M3/MaterialShapes.kt   (@ExperimentalMaterial3ExpressiveApi)
public fun Morph.toPath(progress: Float, path: Path = Path(), startAngle: Int = 0): Path      // not composable; Compose Path; coordinates 0..1
@Composable public fun RoundedPolygon.toPath(startAngle: Int = 0): Path
@Composable public fun RoundedPolygon.toShape(startAngle: Int = 0): Shape                      // composable: never in a top-level val
sealed class MaterialShapes { companion object { val X: RoundedPolygon ... } }
```

`MaterialShapes` members: Circle, Square, Slanted, Arch, Fan, Arrow, SemiCircle, Oval, Pill, Triangle, Diamond, ClamShell, Pentagon, Gem, Sunny, VerySunny, Cookie4Sided, Cookie6Sided, Cookie7Sided, Cookie9Sided, Cookie12Sided, Ghostish, Clover4Leaf, Clover8Leaf, Burst, SoftBurst, Boom, SoftBoom, Flower, Puffy, PuffyDiamond, PixelCircle, PixelTriangle, Bun, Heart.

`Morph(start: RoundedPolygon, end: RoundedPolygon)` is `androidx.graphics.shapes.Morph` (Material3 builds it as `Morph(a.normalized(), b.normalized())`; the polygons of `MaterialShapes` are already normalized). Draw a morph by scaling the 0..1 path to the size, as `toShape` does. The project rule "one top-level type per file" applies to a custom `Shape` class: give it its own file.

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MorphShape(private val morph: Morph, private val progress: Float) : Shape {      // own file: MorphShape.kt
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress = progress)
        path.transform(Matrix().apply { scale(x = size.width, y = size.height) })
        return Outline.Generic(path)
    }
}
// caller: val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
//         val p by animateFloatAsState(if (done) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec())
//         Box(Modifier.size(48.dp).clip(MorphShape(morph, p)).background(MaterialTheme.colorScheme.primary))
```

With an expressive spatial spring `progress` overshoots 1 (damping 0.8): clamp it or use an effects spec if overshoot looks wrong. Import `androidx.compose.material3.toPath` (Compose `Path`), not the android.graphics one.

## H. Navigation 3 1.2.0 `NavDisplay`

```kotlin
// nav3ui/commonMain/androidx/navigation3/ui/NavDisplay.kt:335  (the one with a back stack)
@Composable
public fun <T : Any> NavDisplay(
    backStack: List<T>, modifier: Modifier = Modifier, contentAlignment: Alignment = Alignment.TopStart,
    onBack: () -> Unit = { if (backStack is MutableList<T>) backStack.removeLastOrNull() },
    entryDecorators: List<NavEntryDecorator<T>> = listOf(rememberSaveableStateHolderNavEntryDecorator()),
    sceneStrategies: List<SceneStrategy<T>> = listOf(SinglePaneSceneStrategy()),
    sceneDecoratorStrategies: List<SceneDecoratorStrategy<T>> = emptyList(),
    sharedTransitionScope: SharedTransitionScope? = null,
    sizeTransform: SizeTransform? = null,
    transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = defaultTransitionSpec(),
    popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = defaultPopTransitionSpec(),
    predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = defaultPredictivePopTransitionSpec(),
    entryProvider: (key: T) -> NavEntry<T>,
)
// the singular `sceneStrategy` overloads are HIDDEN (not callable); there is no singular `sceneDecoratorStrategy`.
// Per-entry override (metadata, no opt-in): NavDisplay.transitionSpec { ... }, NavDisplay.popTransitionSpec { ... },
//   NavDisplay.predictivePopTransitionSpec { edge -> ... }  -> Map<String, Any>; pass with entry<K>(metadata = a + b) { ... }
// LocalNavAnimatedContentScope.current throws outside a NavDisplay (so never read it in a preview's `...Content` target).
```

- Defaults (android): push and pop `fadeIn(tween(700))` with `fadeOut(tween(700))`; predictive back `fadeIn(spring(1f, 1600f))` with `scaleOut(0.7f)`. `KansoNavDisplay` currently passes none, so it gets these.
- Push or pop? NavDisplay compares the old and new lists of `NavEntry.contentKey`: it is a **pop** only if the first key is unchanged, the new list is shorter and a prefix of the old one. Replacing the stack, replacing the top, or adding is a push. During a predictive gesture `predictivePopTransitionSpec` runs and the transition is *seeked*. On a push the INCOMING scene's metadata spec wins; on a pop the OUTGOING scene's.
- NavDisplay overwrites `targetContentZIndex` and `sizeTransform` of the `ContentTransform` you return (`.using(SizeTransform)` inside a spec is ignored; use the `sizeTransform` parameter). Spec lambdas run twice per transition setup: keep them pure. Two entries with equal keys share a `contentKey` (no animation, wrong push/pop decision).
- Read the springs in composition and capture them: the spec lambdas are NOT composable.

```kotlin
val slide: FiniteAnimationSpec<IntOffset> = MaterialTheme.motionScheme.defaultSpatialSpec()
val fade: FiniteAnimationSpec<Float> = MaterialTheme.motionScheme.defaultEffectsSpec()
NavDisplay(backStack = backStack, onBack = onBack, entryDecorators = entryDecorators,
    transitionSpec = {
        (slideInHorizontally(animationSpec = slide, initialOffsetX = { it }) + fadeIn(fade)) togetherWith
            (slideOutHorizontally(animationSpec = slide, targetOffsetX = { -it / 4 }) + fadeOut(fade)) },
    popTransitionSpec = {
        (slideInHorizontally(animationSpec = slide, initialOffsetX = { -it / 4 }) + fadeIn(fade)) togetherWith
            (slideOutHorizontally(animationSpec = slide, targetOffsetX = { it }) + fadeOut(fade)) },
    predictivePopTransitionSpec = { _ -> /* same as popTransitionSpec */ },
    entryProvider = entryProvider)
```

Imports: `androidx.compose.animation.{fadeIn, fadeOut, slideInHorizontally, slideOutHorizontally, togetherWith, ContentTransform}`, `androidx.compose.animation.core.FiniteAnimationSpec`, `androidx.compose.ui.unit.IntOffset`, `androidx.navigation3.ui.NavDisplay`, `androidx.navigation3.scene.Scene`.

## I. Compose animation (call shapes attested by Material3; full parameter lists from memory)

```kotlin
val scale by animateFloatAsState(                      // import androidx.compose.runtime.getValue
    targetValue = if (pressed) 0.92f else 1f,
    animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),   // T = Float inferred from the parameter type
    label = "pressScale")
// val s = MaterialTheme.motionScheme.defaultSpatialSpec()            // ERROR: nothing to infer T from
val s = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()        // OK
val o: FiniteAnimationSpec<IntOffset> = MaterialTheme.motionScheme.defaultSpatialSpec()   // OK
val a = remember { Animatable(0f) };  a.animateTo(1f, animationSpec = spec);  a.snapTo(0f)
animateColorAsState(target, animationSpec)       // androidx.compose.animation
animateDpAsState(target, animationSpec = ...)
val t = updateTransition(targetState, label = "x");  t.animateFloat(transitionSpec = { spec }) { state -> ... }
val inf = rememberInfiniteTransition("label");  inf.animateFloat(0f, 1f, animationSpec = infiniteRepeatable(tween(...)), label = "x")
```

The MotionScheme specs are springs and CANNOT go into `rememberInfiniteTransition().animateFloat` (it needs an `InfiniteRepeatableSpec` built from a duration-based spec such as `tween`).

## Not confirmed

Full Compose animation signatures (section I); whether `Typography()` yields distinct Emphasized styles (see A); whether a Compose test's `InfiniteAnimationPolicy` cancels an indeterminate `LoadingIndicator` when the clock auto-advances (use `autoAdvance = false`); whether overflowed `ButtonGroup` items appear in the semantics tree; Robolectric's default width and so whether a group overflows in a test (the tests run at `w411dp`).
