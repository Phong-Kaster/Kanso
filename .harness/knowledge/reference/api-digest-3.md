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

