package com.phongkaster.kanso.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.common.RoundIconButton
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag on the root of the Home screen. */
const val HOME_SCREEN_TAG = "home-screen"

/** The test tag on the hero carousel, the strip of tool banners that swipes sideways. */
const val HOME_CAROUSEL_TAG = "home-carousel"

/** The test tag on the row of page dots under the carousel. */
const val HOME_CAROUSEL_DOTS_TAG = "home-carousel-dots"

/** The test tag on the row of five round tool shortcuts under the page dots. */
const val HOME_SHORTCUTS_TAG = "home-shortcuts"

/**
 * Returns the test tag of [tool]'s banner in the carousel, such as `home-banner-RETOUCH`. Tests
 * find a banner by its tag, never by its text: a tool's name is on Home twice (its banner and its
 * shortcut's name).
 */
fun homeBannerTag(tool: Tool): String = "home-banner-${tool.name}"

/** Returns the test tag of the large icon on [tool]'s banner, such as `home-banner-RETOUCH-icon`. */
fun homeBannerIconTag(tool: Tool): String = "${homeBannerTag(tool)}-icon"

/** Returns the test tag of the "Try now" button on [tool]'s banner, such as `home-try-RETOUCH`. */
fun homeTryTag(tool: Tool): String = "home-try-${tool.name}"

/** Returns the test tag of the page dot at [index] under the carousel (0 is the first banner). */
fun homeCarouselDotTag(index: Int): String = "home-carousel-dot-$index"

/** Returns the test tag of [tool]'s round shortcut button, such as `home-shortcut-EDIT`. */
fun homeShortcutTag(tool: Tool): String = "home-shortcut-${tool.name}"

/** Returns the test tag of the name under [tool]'s shortcut, such as `home-shortcut-EDIT-label`. */
fun homeShortcutLabelTag(tool: Tool): String = "${homeShortcutTag(tool)}-label"

/** The space between the screen's left and right edges and Home's content. */
private val SCREEN_PADDING = 16.dp

/**
 * The height of the carousel: 176 dp. Home's rows are measured so that, on a small phone, all of
 * them up to the newest recent photo fit on screen without scrolling; the carousel is the one row
 * that may shrink (to 160 dp, not lower) if that budget ever runs short.
 */
private val CAROUSEL_HEIGHT = 176.dp

/**
 * How much narrower a banner is than the carousel. The difference, less [CAROUSEL_ITEM_SPACING],
 * is the slice of the next banner that shows at the right edge (40 dp), the hint that the strip
 * can be swiped.
 */
private val CAROUSEL_PEEK = 48.dp

/** The gap between two banners in the carousel. */
private val CAROUSEL_ITEM_SPACING = 8.dp

/** The size of a tool's icon on its banner: drawn large, it is the banner's picture. */
private val BANNER_ICON_SIZE = 88.dp

/** The space between a banner's edge and what it holds. */
private val BANNER_PADDING = 16.dp

/** The gap between the rows of Home: under the greeting, the carousel and the dots. */
private val ROW_GAP = 12.dp

/** The height and the width of a page dot that is not the current one. */
private val DOT_SIZE = 8.dp

/** The width of the current page dot: stretched into a short pill so it stands out. */
private val CURRENT_DOT_WIDTH = 20.dp

/** The space between two page dots. */
private val DOT_SPACING = 6.dp

/** The gap between a shortcut's round button and the name under it. */
private val SHORTCUT_LABEL_GAP = 4.dp

/**
 * The Home screen: in one scrolling column, the title "Kanso", the greeting "What will you edit
 * today?", the hero carousel of the four AI tools with its page dots, and a row of five round
 * shortcuts, one per tool. The camera, My edits and Settings are not on Home itself: they are
 * reached from the floating tab bar the navigation host draws over every tab screen (see
 * `KansoTabShell`).
 *
 * Why Home is two composables. A screen in this app usually needs things that only exist while
 * the real app runs: here, a [HomeViewModel] that the app's wiring (Koin) creates and keeps
 * alive. Android Studio's preview pane, and a quick test, cannot make one of those. So the screen
 * is cut in two, a technique called "state hoisting" (hoisting means lifting: the state is lifted
 * out of the part that draws, up to its caller):
 *
 * - [HomeScreen], this function, is the *stateful* part. It knows where the state lives: it reads
 *   Home's state, one [HomeUiState], from [viewModel] and hands it on. It draws nothing itself.
 * - [HomeContent] is the *stateless* part. It holds no state of its own and knows nothing about
 *   ViewModels or Koin: it simply draws whatever [HomeUiState] it is given, and reports taps
 *   through the callback it is given.
 *
 * Because [HomeContent] needs only plain values (a [HomeUiState] and a lambda), a preview can
 * call it with a state built from made-up sample data and see exactly what the person sees,
 * with none of the app's machinery running. That is what `HomePreview` at the bottom of this
 * file does. The other screens of the app follow the same pattern. (What a UiState is, and why
 * every screen has one, is explained in `HomeUiState.kt`.)
 *
 * The screen itself never navigates: it only reports what was tapped, and the caller (the
 * navigation host) decides where that leads. [onToolClick] is called with the tool of the banner,
 * the "Try now" button or the shortcut that was tapped; the caller opens that tool's photo
 * gallery, and the photo picked there opens in that tool.
 *
 * [contentPadding] is extra empty space the caller asks for at the end of the column; the
 * navigation host passes the height the floating tab bar covers, so the last row can always be
 * scrolled up above the bar instead of resting under it.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onToolClick: (Tool) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    // collectAsState turns the ViewModel's StateFlow into Compose state: whenever a new
    // snapshot arrives, Home is drawn again with it. `by` lets `state` be used as the current
    // snapshot itself, rather than as a box holding it.
    val state by viewModel.state.collectAsState()

    HomeContent(
        state = state,
        onToolClick = onToolClick,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

/**
 * The stateless part of Home (see [HomeScreen] for why there are two): draws one scrolling column
 * holding, from top to bottom, the title "Kanso" and the greeting, the hero carousel of
 * `state.carouselTools` with its page dots, and the row of round shortcuts of `state.shortcutTools`.
 *
 * The title is the first item of the column, not a separate top bar, so it scrolls away with the
 * rest and everything Home shows lives in the one scrolling column. With no top bar, the
 * [Scaffold] adds the status bar's height to `innerPadding` instead, so the title never sits
 * under the clock and the battery icon.
 *
 * The carousel's position (which banner is in front) is remembered here, with
 * `rememberCarouselState`, because two rows read it: the carousel moves it and the page dots under
 * it show it. It is saved like a scroll position (`rememberSaveable` inside), so Home still shows
 * the same banner after the person comes back from a tool or turns the phone.
 *
 * Taps are reported, never acted on here: [onToolClick] with the tool of the banner, the "Try now"
 * button or the shortcut. [contentPadding]'s bottom is added after the last row, inside the
 * scrolling part, so it is space the person scrolls through, not a margin that shrinks the screen.
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    onToolClick: (Tool) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    // The carousel asks for the number of banners through a lambda, so it always reads the
    // current list, even if a later snapshot held a different one.
    val carouselState = rememberCarouselState { state.carouselTools.size }

    Scaffold(
        modifier = modifier.testTag(HOME_SCREEN_TAG),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // innerPadding is the space the system bars take: the status bar at the top (this
                // screen has no top bar) and the navigation bar at the bottom. consumeWindowInsets
                // tells everything inside that this space is already dealt with, so the inset
                // padding below only adds what is still missing.
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                // The last row must not end up where the system listens for gestures. Window
                // insets are the strips along the screen's edges that the system draws on (the
                // navigation bar) or listens on (the gesture zones; at the bottom, a swipe up
                // means "go home"). The gesture zone can be taller than the navigation bar, even
                // when the navigation bar is 0 dp tall (swipe gestures with the hint hidden), so
                // padding by the navigation bar alone is not enough. `union` takes the taller of
                // the two, and `only(Bottom)` leaves out the side gesture zones, where the back
                // swipe starts. Placed after verticalScroll, this padding is part of the scrolling
                // content: it adds empty space after the last row, so when the column is scrolled
                // to its end the last row comes to rest above both strips.
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
                )
                // The caller's extra space at the end (the floating tab bar's height), also inside
                // the scrolling content, so the last row can be scrolled up above the bar.
                .padding(bottom = contentPadding.calculateBottomPadding())
                .padding(horizontal = SCREEN_PADDING),
        ) {
            HomeTitle()
            HomeGreeting()
            Spacer(Modifier.height(ROW_GAP))
            HeroCarousel(
                tools = state.carouselTools,
                carouselState = carouselState,
                onToolClick = onToolClick,
            )
            Spacer(Modifier.height(ROW_GAP))
            CarouselDots(count = state.carouselTools.size, current = carouselState.currentItem)
            Spacer(Modifier.height(ROW_GAP))
            ToolShortcuts(tools = state.shortcutTools, onToolClick = onToolClick)
        }
    }
}

/**
 * Home's title: the app's name, "Kanso", at the top of the column, 48 dp tall with its padding.
 *
 * `headlineLargeEmphasized` is one of the "Emphasized" text styles Material 3 Expressive adds to
 * the theme's typography: each one is the same size as its plain twin (here `headlineLarge`) but
 * heavier, for the few words on a screen that should stand out, such as a title. The
 * `heading()` semantics mark the text as a heading, so a screen reader announces it as one and
 * lets the person jump between headings.
 */
@Composable
private fun HomeTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineLargeEmphasized,
        modifier = modifier
            .padding(top = 8.dp)
            .semantics { heading() },
    )
}

/**
 * The greeting under the title, "What will you edit today?", in the theme's softer text colour
 * (`onSurfaceVariant`), so it reads as a line under the title, not as a second title.
 */
@Composable
private fun HomeGreeting(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.home_hero_greeting),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 4.dp),
    )
}

/**
 * The hero carousel: one large banner per tool of [tools], side by side in a strip the person
 * swipes sideways, one banner at a time. [carouselState] holds which banner is in front (the
 * caller keeps it, because the page dots read it too). A tap on a banner, or on its "Try now"
 * button, calls [onToolClick] with that banner's tool.
 *
 * What a carousel is. A carousel is a row of items too wide for the screen that the person swipes
 * through, with the next item peeking in at the edge as a hint that there is more. Material 3
 * offers a few kinds; this one is `HorizontalUncontainedCarousel`: every banner keeps its full
 * width ([CAROUSEL_PEEK] narrower than the strip, so a 40 dp slice of the next one shows), and only
 * a banner cut off at the edge is clipped. Under the hood it is a pager (it composes only the
 * banners in view) whose items are clipped through `maskClip`, the carousel's way of giving each
 * item its rounded shape while it moves.
 *
 * `flingBehavior` decides where a swipe comes to rest. The uncontained carousel's default lets a
 * swipe stop anywhere, even half-way between two banners; `CarouselDefaults.singleAdvanceFlingBehavior`
 * moves exactly one banner per swipe and settles it in front, so the page dots always point at a
 * whole banner. The carousel never moves on its own: there is no timer, only the person's swipes.
 *
 * `BoxWithConstraints` measures the width the column gives the carousel, so the banners are sized
 * from the real screen: about 280 dp on a 360 dp phone, wider on a bigger one.
 */
@Composable
private fun HeroCarousel(
    tools: List<Tool>,
    carouselState: CarouselState,
    onToolClick: (Tool) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Read in composition: the shape comes from the theme.
        val shape = MaterialTheme.shapes.extraLarge
        HorizontalUncontainedCarousel(
            state = carouselState,
            itemWidth = maxWidth - CAROUSEL_PEEK,
            // A fixed height: inside a scrolling column the carousel would otherwise have no
            // limit on its height and could not size its banners.
            modifier = Modifier
                .fillMaxWidth()
                .height(CAROUSEL_HEIGHT)
                .testTag(HOME_CAROUSEL_TAG),
            itemSpacing = CAROUSEL_ITEM_SPACING,
            flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(carouselState),
        ) { index ->
            val tool = tools[index]
            HeroBanner(
                tool = tool,
                colors = bannerColors(index),
                onClick = { onToolClick(tool) },
                // maskClip belongs to the carousel's item scope: it clips the banner to its
                // rounded shape, and to the visible part of it while it is cut off at the edge.
                modifier = Modifier
                    .fillMaxHeight()
                    .maskClip(shape),
                shape = shape,
            )
        }
    }
}

/**
 * The colours of the banner at [index] in the carousel: the theme's container colours in turn
 * (primary, secondary, tertiary, then the highest surface container), each with the text colour
 * made for it, so neighbouring banners differ and every banner stays readable in light and dark
 * and with any theme colour. No banner has a picture or a fixed colour of its own.
 */
@Composable
private fun bannerColors(index: Int): CardColors {
    val scheme = MaterialTheme.colorScheme
    return when (index % 4) {
        0 -> CardDefaults.cardColors(containerColor = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer)
        1 -> CardDefaults.cardColors(containerColor = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer)
        2 -> CardDefaults.cardColors(containerColor = scheme.tertiaryContainer, contentColor = scheme.onTertiaryContainer)
        else -> CardDefaults.cardColors(containerColor = scheme.surfaceContainerHighest, contentColor = scheme.onSurface)
    }
}

/**
 * Returns the string resource of the one-line description on [tool]'s banner, or null for a tool
 * that has no banner (Edit, which is only a shortcut).
 */
@StringRes
private fun bannerDescriptionRes(tool: Tool): Int? = when (tool) {
    Tool.REMOVE_BACKGROUND -> R.string.home_hero_description_remove_background
    Tool.ENHANCE -> R.string.home_hero_description_enhance
    Tool.RETOUCH -> R.string.home_hero_description_retouch
    Tool.ARTISTIC -> R.string.home_hero_description_artistic
    Tool.EDIT -> null
}

/**
 * One banner of the carousel: [tool]'s name, a one-line description, a small pill "Try now"
 * button and, on the right, the tool's icon drawn large, all on a rounded card in [colors] with
 * [shape]. A tap anywhere on the card, or on the button, calls [onClick].
 *
 * The card is a `Card` given an `onClick`: a Material container that draws its colour in its
 * shape and makes its whole area tappable. A tappable card merges what is inside it into one node
 * for screen readers (the name and the description), except the button, which has its own click
 * and so stays a separate node. The button's spoken name says which tool it tries ("Try Retouch
 * now"), so the four buttons do not all sound the same.
 *
 * The button passes `shapes =`: Material 3 Expressive's buttons take a pair of shapes, one at
 * rest (a pill) and one while pressed, and morph between them with the theme's motion scheme.
 *
 * The icon has no content description: the name on the banner already says what it is.
 */
@Composable
private fun HeroBanner(
    tool: Tool,
    colors: CardColors,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(tool.labelRes)
    val tryDescription = stringResource(R.string.home_hero_try_now_description, label)
    Card(
        onClick = onClick,
        modifier = modifier.testTag(homeBannerTag(tool)),
        shape = shape,
        colors = colors,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(BANNER_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = bannerDescriptionRes(tool)?.let { stringResource(it) }.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    // One line only: a long description ends in "..." instead of pushing the
                    // button off the banner.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Takes the free height, so the button always sits at the bottom of the banner.
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onClick,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .testTag(homeTryTag(tool))
                        .semantics { contentDescription = tryDescription },
                ) {
                    Text(text = stringResource(R.string.home_hero_try_now))
                }
            }
            Spacer(Modifier.width(8.dp))
            // The drawable paints white; Icon tints it with the card's content colour.
            Icon(
                painter = painterResource(tool.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(BANNER_ICON_SIZE)
                    .testTag(homeBannerIconTag(tool)),
            )
        }
    }
}

/**
 * The page dots under the carousel: [count] small dots in a centred row, the one at [current]
 * (the banner in front) stretched into a short pill in the theme's primary colour, the others
 * small circles in the softer outline colour.
 *
 * The dots only show where the carousel is; they cannot be tapped. Each one is marked with a `stateDescription` ("Current banner"; not `selected`, which would make the dot a control to be 48 dp wide)
 * when it is the current one and has a spoken name ("Banner 2 of 4"), so a screen reader and a
 * test can tell which banner is in front. Material 3 has no page indicator of its own, so the dots
 * are drawn here from the carousel's `currentItem`, which changes as the carousel settles on a new
 * banner. The current dot's width moves with `animateDpAsState` on the theme's fast spring, so the
 * pill slides from dot to dot instead of jumping.
 */
@Composable
private fun CarouselDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DOT_SIZE)
            .testTag(HOME_CAROUSEL_DOTS_TAG),
        horizontalArrangement = Arrangement.spacedBy(DOT_SPACING, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val isCurrent = index == current
            val width by animateDpAsState(
                targetValue = if (isCurrent) CURRENT_DOT_WIDTH else DOT_SIZE,
                // Read in composition: MaterialTheme.motionScheme is only available while composing.
                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Dp>(),
                label = "carouselDotWidth",
            )
            val description = stringResource(R.string.home_hero_dot_description, index + 1, count)
            val currentDescription = stringResource(R.string.home_hero_dot_current)
            Spacer(
                modifier = Modifier
                    .size(width = width, height = DOT_SIZE)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    )
                    .testTag(homeCarouselDotTag(index))
                    .semantics {
                        contentDescription = description
                        if (isCurrent) stateDescription = currentDescription
                    },
            )
        }
    }
}

/**
 * The row of round tool shortcuts under the page dots: one per tool of [tools], left to right,
 * each a round icon button with the tool's name under it. A tap on a button calls [onToolClick]
 * with its tool.
 *
 * Why the row does not scroll. The owner's rule is that every tool is one tap from Home, with no
 * swipe: the carousel only shows one banner at a time, so this row is the way to every tool at
 * once. It is a plain `Row` of five equal columns (`weight(1f)` shares the width evenly), never a
 * scrolling row, so all five are in view together even on a 360 dp phone.
 *
 * Each button is the app's shared `RoundIconButton` (a 48 dp tonal circle), named after its tool
 * for screen readers. The name under it is a label, not part of the button: it is not a separate
 * place to tap, and it may wrap to two lines ("Remove background") in its narrow column.
 */
@Composable
private fun ToolShortcuts(tools: List<Tool>, onToolClick: (Tool) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HOME_SHORTCUTS_TAG),
        verticalAlignment = Alignment.Top,
    ) {
        tools.forEach { tool ->
            ToolShortcut(
                tool = tool,
                onClick = { onToolClick(tool) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One tool shortcut: [tool]'s round icon button, which calls [onClick], and 4 dp under it the
 * tool's name in small centred text, at most two lines. The name spans the column's whole width
 * and is centred in it, as the button is, so the two share one centre line.
 */
@Composable
private fun ToolShortcut(tool: Tool, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(tool.labelRes)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RoundIconButton(
            onClick = onClick,
            iconRes = tool.iconRes,
            contentDescription = label,
            modifier = Modifier.testTag(homeShortcutTag(tool)),
        )
        Spacer(Modifier.height(SHORTCUT_LABEL_GAP))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(homeShortcutLabelTag(tool)),
        )
    }
}

/**
 * The sample state the preview draws: the same two lists the real Home shows, written out by
 * hand. It is a plain value, so building it needs nothing of the running app.
 */
private val sampleHomeState = HomeUiState(
    carouselTools = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
    shortcutTools = listOf(Tool.EDIT, Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
)

/**
 * Home as the person sees it, drawn once in light and once in dark.
 *
 * `@PreviewLightDark` asks Android Studio for two pictures of this function, one with the phone
 * in day mode and one in night mode; `ThemeMode.System` makes KansoTheme follow that setting.
 * The sample data is [sampleHomeState], built by hand; no state holder from the app is needed to
 * make one. [HomeContent] draws every part of Home ([HomeTitle], [HomeGreeting], [HeroCarousel]
 * with its [HeroBanner]s, [CarouselDots], and [ToolShortcuts] with its [ToolShortcut]s), so this
 * one picture covers them all. The taps do nothing, because a preview cannot be tapped into
 * another screen.
 */
@PreviewLightDark
@Composable
private fun HomePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        HomeContent(
            state = sampleHomeState,
            onToolClick = {},
        )
    }
}
