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

