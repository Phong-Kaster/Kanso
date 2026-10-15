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

