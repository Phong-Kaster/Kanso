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


---

# Run 7 additions (sections J to Q), 2026-10-12

Read from the same sources jars (`C:/Users/MAC_PC/AppData/Local/Temp/kanso-src/{m3,nav3ui,nav3rt}/`, `M3/` = `commonMain/androidx/compose/material3/`, `NAV3UI/` = `nav3ui/commonMain/androidx/navigation3/ui/`, `NAV3RT/` = `nav3rt/commonMain/androidx/navigation3/runtime/`) by the Run 7 feasibility analyst; **none of the components below needs an opt-in** except where a line says so. Signatures are copied from the sources. Compose test, foundation, animation-core and Coil sources are NOT in the scratch folder: anything about them is marked "memory".

## J. ShortNavigationBar (the tab bar; no opt-in)

```kotlin
// M3/ShortNavigationBar.kt:96
fun ShortNavigationBar(modifier: Modifier = Modifier, containerColor: Color = ShortNavigationBarDefaults.containerColor,
    contentColor: Color = ShortNavigationBarDefaults.contentColor, windowInsets: WindowInsets = ShortNavigationBarDefaults.windowInsets,
    arrangement: ShortNavigationBarArrangement = ShortNavigationBarDefaults.arrangement, content: @Composable () -> Unit)
// :251
fun ShortNavigationBarItem(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit, label: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier, enabled: Boolean = true, iconPosition: NavigationItemIconPosition = NavigationItemIconPosition.Top,
    colors: NavigationItemColors = ShortNavigationBarItemDefaults.colors(), interactionSource: MutableInteractionSource? = null)
// :415 ShortNavigationBarItemDefaults.colors(selectedIconColor, selectedTextColorTopIconPosition, selectedTextColorStartIconPosition,
//        selectedIndicatorColor, unselectedIconColor, unselectedTextColor, disabledIconColor, disabledTextColor)  -- all defaulted
//      the overload with `selectedTextColor` is DeprecationLevel.HIDDEN (:438): writing selectedTextColor = does not compile
// NavigationItem.kt:119 class NavigationItemColors(...same eight...); copy(...) :192
// NavigationItem.kt:85 NavigationItemIconPosition.Top / .Start ; ShortNavigationBarArrangement.EqualWeight / .Centered (:188-205)
```

- Body: `Surface(color, contentColor, modifier) { Layout(windowInsetsPadding(windowInsets).defaultMinSize(64.dp).selectableGroup()) }` (:163-168). It has **no `shape` parameter**: make it a pill with `Modifier.shadow(elevation, CircleShape)` then `Modifier.clip(CircleShape)` in `modifier`.
- Default `windowInsets` = `systemBarsForVisualComponents.only(Horizontal + Bottom)` (:381-386), padded INSIDE the bar: pass `WindowInsets(0, 0, 0, 0)` for a floating bar and pad the outer Row with `WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom))` through `windowInsetsPadding`.
- `EqualWeight`: every item is width/count wide, all as tall as the tallest (:487-533). Needs a bounded width (a Row `weight(1f)`).
- Item: `Box(modifier.selectable(role = Role.Tab, indication = null).defaultMinSize(48.dp))` (`NavigationItem.kt:377-393`): a `testTag` in the item's `modifier` is on the Tab node; `assertIsSelected()` works; the label merges in. The icon's semantics are NOT cleared (:356-358): give the `Icon` `contentDescription = null`.
- Top position: the indicator is a flat 56x32 full pill behind the ICON only (`NavigationBarVerticalItemTokens.kt:24-34`, `NavigationItem.kt:1241-1254`), animated on the DefaultSpatial spring (:1224-1230, finite: tests go idle, `captureRoboImage` is fine).
- Default tokens (`NavigationBarTokens.kt`): container `SurfaceContainer`, height 64, indicator `SecondaryContainer`, active icon `OnSecondaryContainer`, active label `Secondary`, inactive `OnSurfaceVariant`, label `LabelMedium`.
- Roles that exist: `ColorScheme.inversePrimary` :155, `inverseSurface` :171, `inverseOnSurface` :172, `scrim` :179. Kanso's schemes come from MaterialKolor's `dynamicColorScheme`, which fills every role (`ui/theme/SeedColorScheme.kt:41-42`).
- `NavigationBar` (`NavigationBar.kt:109`) is the older 80 dp bar (`TallContainerHeight` :731), `Row.fillMaxWidth`; its `NavigationBarItem` (:171) clears the icon's semantics when a label shows (:194-198).
- Run 7's bar: `ShortNavigationBar(Modifier.weight(1f).shadow(6.dp, CircleShape).clip(CircleShape), containerColor = inverseSurface, contentColor = inverseOnSurface, windowInsets = WindowInsets(0, 0, 0, 0))` plus a separate round `FilledIconButton` beside it, both in one Row padded 16 dp.

## K. Carousel (`androidx.compose.material3.carousel`; no opt-in)

```kotlin
// carousel/Carousel.kt:178
fun HorizontalUncontainedCarousel(state: CarouselState, itemWidth: Dp, modifier: Modifier = Modifier, itemSpacing: Dp = CarouselDefaults.ItemSpacing,
    flingBehavior: TargetedFlingBehavior = CarouselDefaults.noSnapFlingBehavior(), userScrollEnabled: Boolean = true,
    contentPadding: PaddingValues = CarouselDefaults.ContentPadding, content: @Composable CarouselItemScope.(itemIndex: Int) -> Unit)
// :107 HorizontalMultiBrowseCarousel(state, preferredItemWidth: Dp, modifier, itemSpacing, flingBehavior = singleAdvanceFlingBehavior(state),
//        userScrollEnabled, minSmallItemWidth = MinSmallItemSize, maxSmallItemWidth = MaxSmallItemSize, contentPadding, content)
// :260 HorizontalCenteredHeroCarousel(state, modifier, preferredItemWidth: Dp = Dp.Unspecified, itemSpacing, flingBehavior = singleAdvance..., userScrollEnabled,
//        minSmallItemWidth, maxSmallItemWidth, contentPadding, content)
// CarouselState.kt:50 class CarouselState(currentItem: Int = 0, currentItemOffsetFraction: Float = 0f, itemCount: () -> Int) : ScrollableState
//   val currentItem: Int (:67, observable); suspend scrollToItem(item) (:86); suspend animateScrollToItem(item, animationSpec = spring()) (:99)
// CarouselState.kt:150 @Composable fun rememberCarouselState(initialItem: Int = 0, itemCount: () -> Int): CarouselState  (rememberSaveable)
// CarouselDefaults (Carousel.kt:723): @Composable singleAdvanceFlingBehavior(state, snapAnimationSpec = spring(StiffnessMediumLow)) :737,
//   multiBrowseFlingBehavior :769, noSnapFlingBehavior :802, MinSmallItemSize 40.dp :813, MaxSmallItemSize 56.dp :816, ItemSpacing 0.dp :819
// CarouselItemScope.kt:41 val carouselItemDrawInfo; :49 @Composable Modifier.maskClip(shape: Shape); :58 @Composable Modifier.maskBorder(border, shape)
```

- Each carousel is a `HorizontalPager` (`Carousel.kt:354`) with `Role.Carousel` (:368); `beyondViewportPageCount` is 2 for multi-browse (:141) and hero (:296) and **0 for uncontained (:206)**: uncontained composes only the pages in view, so a test must swipe (`performTouchInput { swipeLeft() }` on the carousel node; the snap is a finite spring) to reach banners 3 and 4.
- Items are laid out at full size and clipped (graphics layer, `clip = true`) to a mask CENTRED on the item (:609-625, :643-655): a shrunk or cut-off item shows its middle slice, never its leading edge. Uncontained keeps full-size items and masks only the cut-off one; at 360 dp with `itemWidth = 296.dp` and `itemSpacing = 8.dp` about 40-50 dp of the next banner shows (`Keylines.kt:158-195`; the effect of `contentPadding` was not traced).
- **Uncontained defaults to `noSnapFlingBehavior()` (:184)**: pass `CarouselDefaults.singleAdvanceFlingBehavior(state)` so a banner never rests half-way (then the dots mean something).
- Give the carousel a `Modifier.height(...)` inside a `verticalScroll` column: without it the items' cross axis is unbounded (:569-575).
- `Modifier.carouselParallaxScrollEffect` IS `@ExperimentalMaterial3Api` (`CarouselParallaxScrollEffect.kt:75`): do not use it.
- Page dots: there is no Material 3 page indicator; draw them from `state.currentItem` in composition (it recomposes as the carousel moves); the dots show position only and are not tappable.

## L. Chips (no opt-in)

```kotlin
// Chip.kt:135 AssistChip(onClick, label: @Composable () -> Unit, modifier, enabled = true, leadingIcon = null, trailingIcon = null,
//   shape = AssistChipDefaults.shape, colors = AssistChipDefaults.assistChipColors(), elevation = assistChipElevation(), border = assistChipBorder(enabled),
//   horizontalArrangement = horizontalArrangement(), contentPadding = ContentPadding, interactionSource = null)
// Chip.kt:377 ElevatedAssistChip(same list; colors = elevatedAssistChipColors(), elevation = elevatedAssistChipElevation(), border = null)
// Chip.kt:635 FilterChip(selected, onClick, label, modifier, enabled, leadingIcon, trailingIcon, shape, colors = filterChipColors(), elevation, border, ...)
// Chip.kt:730 FilterChip(selected, onClick, label, shapes: ChipShapes, modifier, enabled, leadingIcon, trailingIcon,
//   colors = FilterChipDefaults.tonalFilterChipColors(), elevation, border, horizontalArrangement, contentPadding, interactionSource)
// FilterChipDefaults.shapes() :2590; shapes(shape = null, selectedShape = null, pressedShape = null) :2601; class ChipShapes(shape, selectedShape, pressedShape = selectedShape) :4217
// AssistChipDefaults: Height 32 dp :1950, IconSize 18 dp :1953, HorizontalSpacing 8 dp :1956, ContentPadding horizontal 8 dp :1962
```

- A flat `AssistChip` container is `Color.Transparent` (:2024): unreadable over a photo. `ElevatedAssistChip` uses `SurfaceContainerLow` (`AssistChipTokens.kt:40-41`). The default shape is `CornerSmall`: pass `shape = CircleShape` for a pill. There is no `shapes` overload for assist chips.
- Chips sit on `Surface(onClick)` (`Role.Button` :3209; a `FilterChip` is `Role.Checkbox` :3258); the touch target is at least 48 dp.

## M. Cards and ListItem (no opt-in)

```kotlin
// Card.kt:136 Card(onClick, modifier, enabled = true, shape = CardDefaults.shape, colors = CardDefaults.cardColors(), elevation = CardDefaults.cardElevation(),
//   border = null, interactionSource = null, content: @Composable ColumnScope.() -> Unit)
// Card.kt:80 Card(modifier, shape, colors, elevation, border, content); ElevatedCard(...) :191; ElevatedCard(onClick, ...) :241
// ListItem.kt:121-149 ListItem(headlineContent, ...) is @Deprecated; use the trailing-`content` overloads:
// ListItem.kt:260 ListItem(modifier, enabled = true, leadingContent, trailingContent, overlineContent, supportingContent, verticalAlignment, shapes, colors, elevation, contentPadding, content)
// ListItem.kt:341 ListItem(onClick, modifier, enabled, leadingContent, ..., onLongClick = null, ..., content);  :421 ListItem(selected: Boolean, onClick, ...)
```

## N. FAB and the floating toolbar with a FAB (not used for the tab bar)

```kotlin
// FloatingActionButton.kt:131 FloatingActionButton(onClick, modifier, shape = FloatingActionButtonDefaults.shape, containerColor, contentColor, elevation, interactionSource, content)
// FloatingToolbar.kt:278 HorizontalFloatingToolbar(expanded, floatingActionButton: @Composable () -> Unit, modifier, colors, contentPadding, scrollBehavior, shape,
//   floatingActionButtonPosition = FloatingToolbarHorizontalFabPosition.End, animationSpec = FloatingToolbarDefaults.animationSpec(),
//   expandedShadowElevation, collapsedShadowElevation, content: RowScope.() -> Unit)
// class FloatingToolbarColors(toolbarContainerColor, toolbarContentColor, fabContainerColor, fabContentColor) :1172
```

- With a FAB the toolbar's Row is `horizontalScroll` (:1591) and as wide as its content (:1658-1661); the FAB is measured 56 dp expanded to 80 dp collapsed (`FabSizeRange` :1153); its box gets "expand/collapse toolbar" accessibility actions (:1598-1616); there is no `selectableGroup` and no Tab role. It is for actions, not for a tab bar.
- `FloatingToolbarDefaults.ContainerSize` = 64 dp, `ScreenOffset` = 16 dp (`FloatingToolbarTokens.kt:27-31`): Run 7's round Camera button is a `FilledIconButton` of `ContainerSize`.

## O. Button size buckets (extends D)

- `Button.kt`: `MinWidth` 58 :1053, `ExtraSmallContainerHeight` 32 :1068, `MinHeight` 40 (small), `MediumContainerHeight` 56 :1071, `LargeContainerHeight` 96 :1074, `ExtraLargeContainerHeight` 136 :1077; icon sizes :1081-1096, `MediumIconSpacing` :1111. There is no `SmallContainerHeight`.
- `contentPaddingFor` (:1578): below 40 extra-small, below 56 small, below 96 medium (24/24 dp), below 136 large. `textStyleFor` (:1640): below 56 `labelLarge`, below 96 `titleMedium`, below 136 `headlineSmall`. `shapesFor` (:1547) by midpoints. Layout is at least 48 dp because buttons sit on `Surface(onClick)`.
- `IconButtonDefaults` container sizes: extraSmall :1078, small :1105, medium :1131 (56 dp), large :1158, extraLarge :1184; a smaller button needs `minimumInteractiveComponentSize()` first (section C).
- Run 7 uses: "Try now" = small `Button(shapes = ...)`, and the five Home tool shortcuts = round `RoundIconButton`s (48 dp circles, D-001); Save and "Edit another" = `MediumContainerHeight` with `fillMaxWidth()` / `weight(1f)`.

## P. Navigation 3: tabs, per-entry transitions, pop clean-up

```kotlin
// NAV3UI/NavDisplay.kt:156 NavDisplay.transitionSpec(spec: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform?): Map<String, Any>
//   :173 popTransitionSpec(... -> ContentTransform?) ; :191 predictivePopTransitionSpec(... (swipeEdge: Int) -> ContentTransform?)  -- null = use NavDisplay's own
// NAV3RT/EntryProvider.kt:204 entry<K>(clazzContentKey = { defaultContentKey(it) }, metadata: Map<String, Any> = emptyMap(), content)
//   :223 entry<K>(clazzContentKey, metadata: (K) -> Map<String, Any>, content)
```

- `isPop` (`NavDisplay.kt:898-909`): same first key, a shorter list that is a prefix of the old one. **Replacing the top key (`[Home, MyEdits]` to `[Home, Settings]`) is a push.** The spec for a push comes from the incoming scene's metadata, for a pop from the outgoing scene's (:792-807). So a tab-to-tab fade is `NavDisplay.transitionSpec { ... } + NavDisplay.popTransitionSpec { ... }` as the entries' `metadata`, returning a fade only when both scenes are tab keys and `null` otherwise.
- A key removed from the back stack: once its entry leaves composition, every decorator's `onPop` runs (`NAV3RT/DecoratedNavEntries.kt:206-221, 258-272`); the saveable-state decorator's `onPop` is `removeState` (`SaveableStateHolderNavEntryDecorator.kt:55`), so that tab's `rememberSaveable` state (scroll position) is gone, and the project's `ViewModelClearedProbe` proves the ViewModel is cleared. Run 7 accepts this for My edits (re-reads the photos) and Settings (back to the top); Home stays at the bottom of the stack and keeps its place.
- The bar lives OUTSIDE `NavDisplay` (in `KansoTabShell`), so the slides never move it; during the predictive back gesture from a non-tab screen to a tab the bar is absent until the gesture is committed (the back stack changes only then).

## Q. Infinite animations and tests

- `LoadingIndicator.kt:440-450` reads `coroutineContext[InfiniteAnimationPolicy]` and wraps its loops in `policy.onInfiniteOperation { }`; `CircularProgressIndicator` uses a plain `rememberInfiniteTransition()` (`ProgressIndicator.kt:630-645`), and `PROJECT.md` records that it does go idle under the Compose test rule. What `ComposeUiTest`'s policy does to `rememberInfiniteTransition` with `autoAdvance` true or false is NOT confirmed (the ui-test sources are not in the scratch folder): the Run 7 spike settles it.
- Repository fact: a spinner hung `PreviewsRenderTest` in Run 3 (previews are drawn in a plain activity with no Compose test rule, `PreviewsRenderTest.kt:141-157`). Never let an infinite animation run in a `@Preview`: read an animated value only from the stateful wrapper, default to a still frame (`LocalSkeletonPhase`, `ProvideSkeletonPulse(pulsing = false)`).
- "Remove animations": a still phase when `ValueAnimator.areAnimatorsEnabled()` is false (API 26+; memory, not confirmed). `PROJECT.md` already says Robolectric cannot drive that setting, so a person checks it on the phone.

## R. Blur and the backdrop (memory, not from source)

- `Modifier.blur(radius)` needs API 31 (RenderEffect) and does nothing below; under Robolectric's software capture it is very likely ignored on every SDK. The Run 7 backdrop therefore draws a tiny scaled-down copy stretched back up (halve with `Bitmap.createScaledBitmap(..., filter = true)` until both sides are at most 64 px; copy a `HARDWARE` bitmap to a software config first) and puts `Modifier.blur` on top. Coil's transformations library is not in the build (`gradle/libs.versions.toml` has only `coil-compose` and `coil-gif`), and the Tool and Saved screens draw a `Bitmap` from the ViewModel, not through Coil.
