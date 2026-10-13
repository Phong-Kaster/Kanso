package com.phongkaster.kanso.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
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
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.common.ProvideSkeletonPulse
import com.phongkaster.kanso.ui.common.RoundIconButton
import com.phongkaster.kanso.ui.common.SkeletonBlock
import com.phongkaster.kanso.ui.gallery.PhotoThumbnail
import com.phongkaster.kanso.ui.myedits.savedAtText
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

/** The test tag on the "Recent edits" strip: the row of photo cards, or of placeholders while loading. */
const val HOME_RECENT_TAG = "home-recent"

/** The test tag on the round arrow beside "Recent edits", which opens the My edits tab. */
const val HOME_RECENT_MORE_TAG = "home-recent-more"

/** The test tag on the short message shown instead of the strip when Kanso has saved nothing yet. */
const val HOME_RECENT_EMPTY_TAG = "home-recent-empty"

/** The start of every recent photo card's test tag; the card's place (0 is the newest) follows it. */
const val HOME_RECENT_CARD_TAG_PREFIX = "home-recent-"

/** The start of every placeholder card's test tag; the placeholder's place follows it. */
const val HOME_RECENT_SKELETON_TAG_PREFIX = "home-recent-skeleton-"

/**
 * The Uri (as text) of the photo a recent card shows, attached to the card as a semantics fact.
 *
 * Semantics are what Compose tells screen readers and tests about a node, such as its text or its
 * role. This extra, Kanso-only fact lets a test check which photo a card stands for (for example
 * that the photo just saved is first) without comparing pixels; it is invisible to people and to
 * screen readers, like the tool screen's `TOOL_PHOTO_URI_KEY`.
 */
val HOME_RECENT_URI_KEY = SemanticsPropertyKey<String>("HomeRecentUriKey")

/** Returns the test tag of the recent photo card at [index], such as `home-recent-0` (the newest). */
fun homeRecentTag(index: Int): String = "$HOME_RECENT_CARD_TAG_PREFIX$index"

/** Returns the test tag of the placeholder card at [index], such as `home-recent-skeleton-0`. */
fun homeRecentSkeletonTag(index: Int): String = "$HOME_RECENT_SKELETON_TAG_PREFIX$index"

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
 * The height of the "Recent edits" heading row: the 48 dp of the round arrow button in it, so the
 * heading and the arrow share one centre line.
 */
private val RECENT_HEADER_HEIGHT = 48.dp

/** The gap between the "Recent edits" heading row and the cards under it. */
private val RECENT_HEADER_GAP = 4.dp

/** The width of a recent photo card (and of a placeholder card). */
private val RECENT_CARD_WIDTH = 120.dp

/** The height of a recent photo card: taller than wide, like a photo held upright. */
private val RECENT_CARD_HEIGHT = 160.dp

/** The gap between two cards of the strip. */
private val RECENT_CARD_SPACING = 8.dp

/**
 * How many placeholder cards stand in for the photos while they load: more than fit across a
 * phone, so the last one runs off the edge as real cards would.
 */
private const val RECENT_SKELETON_COUNT = 4

/** The content type shared by every recent photo card (see [RecentEditsStrip]). */
private const val RECENT_CARD_CONTENT_TYPE = "recent-photo"

/**
 * The Home screen: in one scrolling column, the title "Kanso", the greeting "What will you edit
 * today?", the hero carousel of the four AI tools with its page dots, a row of five round
 * shortcuts, one per tool, and the "Recent edits" strip of Kanso's latest saved photos. The camera,
 * My edits and Settings are not on Home itself: they are
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
 * gallery, and the photo picked there opens in that tool. [onRecentClick] is called with the Uri
 * (as text) of the recent photo card that was tapped; the caller opens it in the Edit tool.
 * [onSeeAllEdits] is called by the round arrow beside "Recent edits"; the caller selects the My
 * edits tab.
 *
 * Besides reading the state, this stateful part does two things a preview must never do:
 *
 * - It asks the ViewModel to read the saved photos again each time Home enters the composition.
 *   `LaunchedEffect` runs its block once when the composable first appears (and again only if its
 *   key, here [viewModel], changes). Home leaves the composition while another screen covers it,
 *   so coming back from the Saved screen runs it again and the photo just saved is first in the
 *   strip. The photos already shown stay while the new read runs (see [HomeViewModel.refresh]).
 * - It wraps the content in `ProvideSkeletonPulse`, which makes the placeholder cards' soft band
 *   move while the photos load. [HomeContent] on its own keeps them still, which is what previews
 *   and tests draw.
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
    onRecentClick: (String) -> Unit = {},
    onSeeAllEdits: () -> Unit = {},
) {
    // collectAsState turns the ViewModel's StateFlow into Compose state: whenever a new
    // snapshot arrives, Home is drawn again with it. `by` lets `state` be used as the current
    // snapshot itself, rather than as a box holding it.
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    ProvideSkeletonPulse {
        HomeContent(
            state = state,
            onToolClick = onToolClick,
            modifier = modifier,
            contentPadding = contentPadding,
            onRecentClick = onRecentClick,
            onSeeAllEdits = onSeeAllEdits,
        )
    }
}

/**
 * The stateless part of Home (see [HomeScreen] for why there are two): draws one scrolling column
 * holding, from top to bottom, the title "Kanso" and the greeting, the hero carousel of
 * `state.carouselTools` with its page dots, the row of round shortcuts of `state.shortcutTools`,
 * and the "Recent edits" heading with its arrow above the strip of `state.recentPhotos`.
 *
 * Why every row fits on a small phone. The owner's rule is that every tool, the Camera button, the
 * arrow and the newest recent photo are one tap from Home, with nothing scrolled. On a 360 x 780 dp
 * phone with a 24 dp status bar and a 48 dp navigation bar, the rows add up, from the top of the
 * window: status bar 24, title 48, greeting 28, gap 12, carousel 176, gap 12, dots 8, gap 12,
 * shortcuts 84 (button 48, gap 4, two lines of name 32), gap 12, heading row 48, gap 4, cards 160:
 * the cards end at 628 dp, 24 dp above the floating bar's top (780 - 48 - 16 - 64 = 652). The
 * column still scrolls, for bigger fonts and smaller screens, but on that phone nothing needs to.
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
 * button or the shortcut; [onRecentClick] with the Uri (as text) of the tapped recent photo;
 * [onSeeAllEdits] when the arrow beside "Recent edits" is tapped. The last two have defaults that
 * do nothing, for callers that only draw Home. [contentPadding]'s bottom is added after the last
 * row, inside the scrolling part, so it is space the person scrolls through, not a margin that
 * shrinks the screen.
 *
 * [thumbnail] draws one recent card's picture, given the photo and the card's modifier (size,
 * rounded shape, background, tap, spoken name and test tag, which it must use). The app keeps the
 * default, the gallery's `PhotoThumbnail`, which loads the picture with Coil; a preview cannot read
 * the phone's photos, so it passes a coloured box instead.
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    onToolClick: (Tool) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onRecentClick: (String) -> Unit = {},
    onSeeAllEdits: () -> Unit = {},
    thumbnail: @Composable (photo: Photo, modifier: Modifier) -> Unit = { photo, cardModifier ->
        PhotoThumbnail(
            photo = photo,
            // Decoded a little larger than the card's longer side, so the picture stays sharp.
            sizePx = with(LocalDensity.current) { RECENT_CARD_HEIGHT.roundToPx() },
            modifier = cardModifier,
            // The card carries its own spoken name ("Edited photo" and the date).
            contentDescription = null,
        )
    },
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
            Spacer(Modifier.height(ROW_GAP))
            RecentEditsHeader(onSeeAllEdits = onSeeAllEdits)
            Spacer(Modifier.height(RECENT_HEADER_GAP))
            RecentEdits(
                photos = state.recentPhotos,
                loading = state.recentLoading,
                onRecentClick = onRecentClick,
                thumbnail = thumbnail,
            )
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
 * The heading row of the "Recent edits" section: the words "Recent edits" on the left, marked as a
 * heading for screen readers, and on the right a round arrow button ([RoundIconButton], the app's
 * shared 48 dp tonal circle) that calls [onSeeAllEdits]; the caller opens the My edits tab, where
 * every saved photo is listed. The row is exactly as tall as the button and centres both, so the
 * heading and the arrow share one centre line.
 */
@Composable
private fun RecentEditsHeader(onSeeAllEdits: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RECENT_HEADER_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_hero_recent_title),
            style = MaterialTheme.typography.titleMediumEmphasized,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        RoundIconButton(
            onClick = onSeeAllEdits,
            iconRes = R.drawable.ic_arrow_forward,
            contentDescription = stringResource(R.string.home_hero_recent_more),
            modifier = Modifier.testTag(HOME_RECENT_MORE_TAG),
        )
    }
}

/**
 * The body of the "Recent edits" section, one of three faces picked from [photos] and [loading]
 * (see [HomeUiState]):
 *
 * - some [photos]: the strip of cards ([RecentEditsStrip]), even while a new read runs, so the
 *   cards never blink away when the person comes back to Home;
 * - no photos and [loading]: placeholder cards ([RecentEditsLoading]) while the first read runs;
 * - no photos and not [loading]: a short message ([RecentEditsEmpty]), as on a fresh install.
 *
 * [onRecentClick] gets the tapped photo's Uri as text; [thumbnail] draws each card's picture (see
 * [HomeContent]).
 */
@Composable
private fun RecentEdits(
    photos: List<Photo>,
    loading: Boolean,
    onRecentClick: (String) -> Unit,
    thumbnail: @Composable (photo: Photo, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        photos.isNotEmpty() -> RecentEditsStrip(
            photos = photos,
            onRecentClick = onRecentClick,
            thumbnail = thumbnail,
            modifier = modifier,
        )
        loading -> RecentEditsLoading(modifier = modifier)
        else -> RecentEditsEmpty(modifier = modifier)
    }
}

/**
 * The strip of recent photos: one tall rounded card per photo of [photos], newest first, left to
 * right, in a row the person swipes sideways. A tap on a card calls [onRecentClick] with that
 * photo's Uri as text.
 *
 * What `LazyRow` is. The sideways twin of a lazy list: a row that scrolls left and right and
 * builds only the cards in view (plus a few just outside), so ten cards cost no more than three.
 * Each card is keyed by its photo's id, so when a new edit arrives at the front the strip keeps
 * the other cards' pictures instead of rebuilding them all. It scrolls on its own axis only: a
 * sideways swipe on it moves the cards, never Home's column.
 *
 * One catch of keyed lazy lists: when a photo is added at the front, the list keeps the card the
 * person was looking at in place, which pushes the new first card just off the left edge. The
 * strip's `LazyListState` (the object that remembers how far the row is scrolled) is therefore
 * sent back to the start whenever the newest photo changes, so a photo just saved is the first
 * thing seen when the person comes back to Home.
 */
@Composable
private fun RecentEditsStrip(
    photos: List<Photo>,
    onRecentClick: (String) -> Unit,
    thumbnail: @Composable (photo: Photo, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val newestId = photos.firstOrNull()?.id
    LaunchedEffect(newestId) { listState.scrollToItem(0) }
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag(HOME_RECENT_TAG),
        horizontalArrangement = Arrangement.spacedBy(RECENT_CARD_SPACING),
    ) {
        itemsIndexed(
            items = photos,
            key = { _, photo -> photo.id },
            contentType = { _, _ -> RECENT_CARD_CONTENT_TYPE },
        ) { index, photo ->
            RecentEditCard(
                index = index,
                photo = photo,
                onClick = { onRecentClick(photo.uri) },
                thumbnail = thumbnail,
            )
        }
    }
}

/**
 * One card of the strip: [photo]'s picture, [RECENT_CARD_WIDTH] wide and [RECENT_CARD_HEIGHT]
 * tall, cut to the theme's large rounded shape, at place [index] (0 is the newest). A tap calls
 * [onClick].
 *
 * The card shows no text. Its spoken name is the one the My edits tiles use, "Edited photo" and
 * the date and time it was saved, so a screen reader names an edit the same way in both places.
 * The order of the modifiers matters, because each wraps the ones after it: `clip` comes before
 * the background, the tap and the picture, so all three are cut to the rounded shape, the tap's
 * ripple included. The background, `surfaceContainerHigh`, shows until the picture has loaded.
 */
@Composable
private fun RecentEditCard(
    index: Int,
    photo: Photo,
    onClick: () -> Unit,
    thumbnail: @Composable (photo: Photo, modifier: Modifier) -> Unit,
) {
    // Formatting a date takes a moment; remember keeps the text until the photo's time changes.
    val savedAt = remember(photo.modifiedSeconds) { savedAtText(photo.modifiedSeconds) }
    val description = stringResource(R.string.my_edits_photo_description, savedAt)
    thumbnail(
        photo,
        Modifier
            .size(width = RECENT_CARD_WIDTH, height = RECENT_CARD_HEIGHT)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                this[HOME_RECENT_URI_KEY] = photo.uri
            }
            .testTag(homeRecentTag(index)),
    )
}

/**
 * What the strip shows while the saved photos are read for the first time: [RECENT_SKELETON_COUNT]
 * placeholder cards ([SkeletonBlock]), each the size and rounded shape of a real card and spaced
 * like them, so nothing jumps when the photos arrive.
 *
 * Why placeholders instead of a spinner. A spinner says only "wait"; it sits where nothing will
 * be and then vanishes, and the cards pop in around it. Placeholders (often called skeletons)
 * already draw the strip's shape, so the person sees at once what is coming and where, and the
 * photos simply fill the shapes in. They are also calmer: their soft band moves only while
 * `ProvideSkeletonPulse` is active, and stands still in previews and tests.
 *
 * The placeholders have no text and no meaning of their own, so the strip carries one spoken
 * name saying the edits are loading, and the placeholders add nothing for screen readers. It is a
 * `LazyRow` like the real strip (so each placeholder gets its full width even past the edge), with
 * scrolling turned off: there is nothing to scroll to yet.
 */
@Composable
private fun RecentEditsLoading(modifier: Modifier = Modifier) {
    // Read in composition, so the spoken name follows the phone's language.
    val description = stringResource(R.string.home_hero_recent_loading)
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HOME_RECENT_TAG)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(RECENT_CARD_SPACING),
        userScrollEnabled = false,
    ) {
        items(count = RECENT_SKELETON_COUNT) { index ->
            SkeletonBlock(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .size(width = RECENT_CARD_WIDTH, height = RECENT_CARD_HEIGHT)
                    .testTag(homeRecentSkeletonTag(index)),
            )
        }
    }
}

/**
 * What the section shows when Kanso has saved no photo yet (or none can be read): one short line
 * in the theme's softer text colour, centred on a calm rounded panel as tall as a card, so the
 * rows above keep their places whether or not there are photos. The panel merges its text into
 * one node for screen readers and tests.
 */
@Composable
private fun RecentEditsEmpty(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(RECENT_CARD_HEIGHT)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .semantics(mergeDescendants = true) {}
            .testTag(HOME_RECENT_EMPTY_TAG)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_hero_recent_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Three saved photos for the previews, newest first, one hour apart, named like Kanso's own
 * files. Their Uris look like the phone's real ones, but nothing reads them: the previews draw
 * [SampleRecentCard] instead of loading the pictures.
 */
private val sampleRecentPhotos by lazy {
    (1..3).map { i ->
        Photo(
            id = i.toLong(),
            uri = "content://media/external/images/media/${3_000 + i}",
            name = "Kanso_20261012_%02d0000.jpg".format(24 - i),
            albumId = 1L,
            modifiedSeconds = 1_791_800_000L - i * 3_600L,
        )
    }
}

/**
 * The stand-in for a recent photo's picture in the previews: a box of a soft colour whose hue
 * moves round the colour wheel with the photo's id, drawn with the card's [modifier], which
 * already cuts it to the rounded card.
 */
@Composable
private fun SampleRecentCard(photo: Photo, modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.background(
            Color.hsv(hue = (photo.id * 53 % 360).toFloat(), saturation = 0.45f, value = 0.85f),
        ),
    )
}

/**
 * The sample state the preview draws: the same two lists of tools the real Home shows, written
 * out by hand, and three recent photos. It is a plain value, so building it needs nothing of the
 * running app.
 */
private val sampleHomeState by lazy {
    HomeUiState(
        carouselTools = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
        shortcutTools = listOf(Tool.EDIT, Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
        recentPhotos = sampleRecentPhotos,
        recentLoading = false,
    )
}

/**
 * Home for the previews below: [HomeContent] drawing [state], with [SampleRecentCard] in every
 * recent card and taps that do nothing. `ProvideSkeletonPulse(pulsing = false)` keeps the
 * placeholder cards on their still frame: a preview has no test clock, and an animation that never
 * ends would keep it from ever finishing.
 */
@Composable
private fun SampleHome(state: HomeUiState) {
    ProvideSkeletonPulse(pulsing = false) {
        HomeContent(
            state = state,
            onToolClick = {},
            thumbnail = { photo, cardModifier -> SampleRecentCard(photo, cardModifier) },
        )
    }
}

/**
 * Home as the person sees it, drawn once in light and once in dark.
 *
 * `@PreviewLightDark` asks Android Studio for two pictures of this function, one with the phone
 * in day mode and one in night mode; `ThemeMode.System` makes KansoTheme follow that setting.
 * The sample data is [sampleHomeState], built by hand; no state holder from the app is needed to
 * make one. [HomeContent] draws every part of Home ([HomeTitle], [HomeGreeting], [HeroCarousel]
 * with its [HeroBanner]s, [CarouselDots], [ToolShortcuts] with its [ToolShortcut]s, and
 * [RecentEditsHeader] above [RecentEditsStrip] with its [RecentEditCard]s), so this one picture
 * covers them all; [HomeEmptyPreview] and [HomeLoadingPreview] show the strip's two other faces.
 * The taps do nothing, because a preview cannot be tapped into another screen.
 */
@PreviewLightDark
@Composable
private fun HomePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SampleHome(sampleHomeState)
    }
}

/** Home on a fresh install, in light and dark: the strip's short message instead of photos. */
@PreviewLightDark
@Composable
private fun HomeEmptyPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SampleHome(sampleHomeState.copy(recentPhotos = emptyList(), recentLoading = false))
    }
}

/** Home while the recent photos are read, in light and dark: the placeholder cards, standing still. */
@PreviewLightDark
@Composable
private fun HomeLoadingPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        SampleHome(sampleHomeState.copy(recentPhotos = emptyList(), recentLoading = true))
    }
}
