package com.phongkaster.kanso.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The test tag on the root of the Home screen. */
const val HOME_SCREEN_TAG = "home-screen"

/**
 * The test tag on Home's large "Open a photo" button. The button's text is its spoken name; the
 * tag only lets tests find the button without depending on the words.
 */
const val HOME_OPEN_PHOTO_TAG = "home-open-photo"

/**
 * Returns the test tag of [tool]'s card on Home, such as `home-tool-RETOUCH`.
 *
 * Tests find a card by this tag, never by its text alone: the Edit card and the "Edit" header
 * above it read the same word, so a search for the text "Edit" finds two nodes.
 */
fun homeToolTag(tool: Tool): String = "home-tool-${tool.name}"

/**
 * The height of the "Open a photo" button: Material 3 Expressive's large button size, 96 dp. It
 * is the one thing most people come to Home for, so it is the biggest and boldest control there.
 */
private val OPEN_PHOTO_HEIGHT = ButtonDefaults.LargeContainerHeight

/**
 * The smallest height of a tool card: 72 dp, well above Android's smallest touch target (48 dp),
 * so a card is easy to hit and its icon and label have room around them.
 */
private val CARD_MIN_HEIGHT = 72.dp

/** The size of the tinted square behind each card's icon. */
private val ICON_BADGE_SIZE = 40.dp

/** The space between the screen's left and right edges and Home's buttons and cards. */
private val SCREEN_PADDING = 16.dp

/** The space above and below Home's title, so it stands as tall as a top bar would. */
private val TITLE_PADDING = 16.dp

/**
 * How much a tool card shrinks while a finger is on it: to 97 % of its size, enough to feel the
 * press without moving the text far.
 */
private const val PRESSED_CARD_SCALE = 0.97f

/**
 * The Home screen: in one scrolling column, the title "Kanso", the large "Open a photo" button
 * and the tool cards in two groups ("AI tools" and "Edit"). The camera and Settings are not on
 * Home itself: they are reached from the floating tab bar the navigation host draws over every
 * tab screen (see `KansoTabShell`).
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
 *   through the callbacks it is given.
 *
 * Because [HomeContent] needs only plain values (a [HomeUiState] and two lambdas), a preview can
 * call it with a state built from made-up sample data and see exactly what the person sees,
 * with none of the app's machinery running. That is what `HomePreview` at the bottom of this
 * file does. The other screens of the app follow the same pattern. (What a UiState is, and why
 * every screen has one, is explained in `HomeUiState.kt`.)
 *
 * The screen itself never navigates: it only reports what was tapped, and the caller (the
 * navigation host) decides where that leads. [onOpenPhoto] is called for "Open a photo" (the
 * caller opens the gallery, and the photo picked there opens in the editor on the Edit tool),
 * and [onToolClick] with the tool of the card that was tapped.
 *
 * [contentPadding] is extra empty space the caller asks for at the end of the column; the
 * navigation host passes the height the floating tab bar covers, so the last card can always be
 * scrolled up above the bar instead of resting under it.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenPhoto: () -> Unit,
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
        onOpenPhoto = onOpenPhoto,
        onToolClick = onToolClick,
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

/**
 * The stateless part of Home (see [HomeScreen] for why there are two): draws one scrolling column
 * holding, from top to bottom, the title "Kanso", the "Open a photo" button, the "AI tools" header
 * with one card per tool of `state.aiTools`, and the "Edit" header with one card per tool of
 * `state.editTools`.
 *
 * The title is the first item of the column, not a separate top bar, so it scrolls away with the
 * rest and everything Home shows lives in the one scrolling column. With no top bar, the
 * [Scaffold] adds the status bar's height to `innerPadding` instead, so the title never sits
 * under the clock and the battery icon.
 *
 * A plain scrolling Column is used, not a lazy list: a lazy list only builds the rows that are on
 * screen, which pays off for hundreds of rows, while Home has fewer than a dozen. Everything in
 * the column is built at once, so every control exists for tests and screen readers even when it
 * is scrolled out of view, and a test can scroll to it.
 *
 * Taps are reported, never acted on here: [onOpenPhoto] for "Open a photo", [onToolClick] with
 * the card's tool. [contentPadding]'s bottom is added after the last card, inside the scrolling
 * part, so it is space the person scrolls through, not a margin that shrinks the screen.
 */
@Composable
fun HomeContent(
    state: HomeUiState,
    onOpenPhoto: () -> Unit,
    onToolClick: (Tool) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Scaffold(
        modifier = modifier.testTag(HOME_SCREEN_TAG),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // innerPadding is the space the system bars take: the status bar at the top (this
                // screen has no top bar) and the navigation bar at the bottom. consumeWindowInsets
                // tells everything inside that
                // this space is already dealt with, so the inset padding below only adds what
                // is still missing.
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
                // to its end the last card comes to rest above both strips.
                .windowInsetsPadding(
                    WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
                )
                // The caller's extra space at the end (the floating tab bar's height), also inside
                // the scrolling content, so the last card can be scrolled up above the bar.
                .padding(bottom = contentPadding.calculateBottomPadding())
                .padding(horizontal = SCREEN_PADDING, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HomeTitle()
            OpenPhotoButton(onClick = onOpenPhoto)
            ToolGroup(
                title = stringResource(R.string.home_header_ai_tools),
                tools = state.aiTools,
                onToolClick = onToolClick,
            )
            ToolGroup(
                title = stringResource(R.string.home_header_edit),
                tools = state.editTools,
                onToolClick = onToolClick,
            )
        }
    }
}

/**
 * Home's title: the app's name, "Kanso", at the top of the column.
 *
 * `headlineSmallEmphasized` is one of the "Emphasized" text styles Material 3 Expressive adds to
 * the theme's typography: each one is the same size as its plain twin (here `headlineSmall`) but
 * heavier, for the few words on a screen that should stand out, such as a title. The
 * `heading()` semantics mark the text as a heading, so a screen reader announces it as one and
 * lets the person jump between headings. The vertical padding gives the title the height of the
 * top bar it replaces, so the first button does not crowd it.
 */
@Composable
private fun HomeTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        modifier = modifier
            .padding(vertical = TITLE_PADDING)
            .semantics { heading() },
    )
}

/**
 * The large "Open a photo" button at the top of Home: an icon of a picture with a plus and the
 * words "Open a photo", on the theme's primary colour, across the whole width. Tapping it calls
 * [onClick].
 *
 * `shapes =` is how Material 3 Expressive buttons get their springy feel. A plain button takes one
 * `shape`; this version takes a pair of shapes, one at rest and one while pressed, and the button
 * morphs from one to the other while a finger is down, animated by the theme's motion scheme.
 * `ButtonDefaults.shapesFor(height)` picks the pair Material designed for a button of that height
 * (a full pill at rest, rounded corners while pressed). The icon size, the gap after it, the
 * padding and the text style come from the same height, so the whole button is in proportion.
 *
 * The icon has no content description: the words next to it already name the button, and the
 * button merges both into one node that screen readers read as "Open a photo".
 */
@Composable
private fun OpenPhotoButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapesFor(OPEN_PHOTO_HEIGHT),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = OPEN_PHOTO_HEIGHT)
            .testTag(HOME_OPEN_PHOTO_TAG),
        contentPadding = ButtonDefaults.contentPaddingFor(OPEN_PHOTO_HEIGHT, hasStartIcon = true),
    ) {
        // The drawable paints white; Icon tints it with the button's content colour.
        Icon(
            painter = painterResource(R.drawable.ic_add_photo_alternate),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(OPEN_PHOTO_HEIGHT)),
        )
        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(OPEN_PHOTO_HEIGHT)))
        Text(
            text = stringResource(R.string.home_open_photo),
            style = ButtonDefaults.textStyleFor(OPEN_PHOTO_HEIGHT),
        )
    }
}

/**
 * One group of tool cards: the header [title], then one card per tool of [tools], in that order,
 * one under the other. Tapping a card calls [onToolClick] with its tool.
 */
@Composable
private fun ToolGroup(
    title: String,
    tools: List<Tool>,
    onToolClick: (Tool) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GroupHeader(text = title)
        tools.forEach { tool ->
            ToolCard(tool = tool, onClick = { onToolClick(tool) })
        }
    }
}

/**
 * The header above a group of cards, showing [text] in the theme's primary colour and the
 * emphasized title style (see [HomeContent] for what "Emphasized" means).
 *
 * `heading()` marks it as a heading in the semantics tree, the description of the screen that
 * screen readers and tests read instead of pixels. TalkBack can then jump from heading to
 * heading, so a person who cannot see the screen skips straight to a group.
 */
@Composable
private fun GroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = 4.dp, top = 12.dp)
            .semantics { heading() },
    )
}

/**
 * The card of [tool]: its icon on a small tinted square, then its name, on a rounded card across
 * the whole width. Tapping anywhere on the card calls [onClick].
 *
 * The card is a `Surface` given an `onClick`: a Material container that draws its colour in its
 * shape, makes its whole area tappable with a ripple, and keeps it at least 48 dp in each
 * direction. A tappable Surface also merges everything inside it into one node of the semantics
 * tree, so a screen reader reads the card as one button named after the tool (the icon has no
 * description, the name says it all), and a test that finds the card by its tag
 * ([homeToolTag]) sees one clickable node whose text is the tool's name.
 *
 * Its shape is the theme's `largeIncreased` (20 dp corners), one of the larger rounded shapes
 * Material 3 Expressive adds. Its colours are the theme's raised surface colour with the matching
 * text colour, and the icon's square is the secondary container colour with its matching icon
 * colour, so text and icon stay readable in light and dark and with any theme colour.
 *
 * Press feedback: while a finger is down the card shrinks a little and springs back on release.
 * `collectIsPressedAsState` turns the card's presses (reported by its interaction source) into a
 * true or false state; `animateFloatAsState` then moves the scale towards 0.97 or 1 over a few
 * frames, with the theme motion scheme's fast spatial spring, the bouncy spring Material 3
 * Expressive uses for small movements. The scale is applied in `graphicsLayer`, which only
 * changes how the card is drawn: its place and size, and so its touch area, stay the same.
 */
@Composable
private fun ToolCard(tool: Tool, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_CARD_SCALE else 1f,
        // Read in composition: MaterialTheme.motionScheme is only available while composing.
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>(),
        label = "toolCardPressScale",
    )
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CARD_MIN_HEIGHT)
            // The scale is read inside the layer's block, so a new value only redraws the card;
            // nothing is composed or laid out again.
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            // A card acts like a button, so screen readers announce it as one.
            .semantics { role = Role.Button }
            .testTag(homeToolTag(tool)),
        shape = MaterialTheme.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(ICON_BADGE_SIZE)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // The drawable paints white; Icon tints it with the colour made for this square.
                Icon(
                    painter = painterResource(tool.iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(tool.labelRes),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/**
 * The sample state the preview draws: the same two groups the real Home shows, written out by
 * hand. It is a plain value, so building it needs nothing of the running app.
 */
private val sampleHomeState = HomeUiState(
    aiTools = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
    editTools = listOf(Tool.EDIT),
)

/**
 * Home as the person sees it, drawn once in light and once in dark.
 *
 * `@PreviewLightDark` asks Android Studio for two pictures of this function, one with the phone
 * in day mode and one in night mode; `ThemeMode.System` makes KansoTheme follow that setting.
 * The sample data is [sampleHomeState], built by hand; no state holder from the app is needed to
 * make one. [HomeContent] draws every part of Home ([OpenPhotoButton], and [ToolGroup] with its
 * [GroupHeader] and [ToolCard]s), so this one picture covers them all. The taps do nothing,
 * because a preview cannot be tapped into another screen.
 */
@PreviewLightDark
@Composable
private fun HomePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        HomeContent(
            state = sampleHomeState,
            onOpenPhoto = {},
            onToolClick = {},
        )
    }
}
