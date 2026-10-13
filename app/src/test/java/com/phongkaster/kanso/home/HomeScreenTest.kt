package com.phongkaster.kanso.home

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.home.HOME_CAROUSEL_DOTS_TAG
import com.phongkaster.kanso.ui.home.HOME_CAROUSEL_TAG
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.home.HOME_SHORTCUTS_TAG
import com.phongkaster.kanso.ui.home.HomeContent
import com.phongkaster.kanso.ui.home.HomeUiState
import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.home.homeBannerIconTag
import com.phongkaster.kanso.ui.home.homeBannerTag
import com.phongkaster.kanso.ui.home.homeCarouselDotTag
import com.phongkaster.kanso.ui.home.homeShortcutLabelTag
import com.phongkaster.kanso.ui.home.homeShortcutTag
import com.phongkaster.kanso.ui.home.homeTryTag
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * Checks Home's stateless part, [HomeContent], hosted on its own on a small phone (360 x 800 dp),
 * with the very state the real [HomeViewModel] hands it.
 *
 * What is checked: the rows come in order (title, greeting, carousel, page dots, shortcut row);
 * the carousel shows the four AI tools' banners in order, each with its name, its one-line
 * description, its large icon and a "Try now" button; four page dots mark the banner in front; a
 * left swipe settles on banner 2 and the dots follow; ten seconds without a touch leave banner 1
 * where it is (the carousel never moves on its own); the five shortcuts are round, level, evenly
 * spaced, named after their tools, with their names centred beneath them, and each tap reports its
 * own tool; and the shortcut row never scrolls sideways.
 *
 * The carousel is lazy (it composes only the banners in view), so banners further along are
 * reached by swiping, as a person would. Positions are read with `positionInRoot` and the node's
 * size, never `boundsInRoot`, which is cut down to the visible part: "wholly on screen" must see the
 * part a clip hides. Each Android version a check runs on is its own `@Test`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class HomeScreenTest {

    /** Starts an empty activity to host Home's stateless part. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test, used to read the same strings Home shows. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Every tool Home reported through `onToolClick`, in the order the taps came. */
    private val tapped = mutableListOf<Tool>()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** On Android 15: title, greeting, carousel, page dots and shortcut row, top to bottom. */
    @Test
    fun rows_come_in_order_on_android_15() {
        show()
        assertRowsInOrder()
    }

    /** The same order on Android 11, its own test so a failure names the version. */
    @Test
    @Config(sdk = [30])
    fun rows_come_in_order_on_android_11() {
        show()
        assertRowsInOrder()
    }

    /**
     * The real state lists the four AI tools for the carousel and every tool for the shortcuts,
     * and the carousel shows the four banners in that order: after k left swipes, banner k + 1's
     * "Try now" is wholly inside the carousel and no other banner's is. Each banner carries its
     * tool's name, its description, its icon on the right half and a "Try now" button whose
     * spoken name names the tool.
     */
    @Test
    fun the_four_banners_show_in_order_with_name_description_icon_and_try_now() {
        val state = show()
        assertEquals(AI_TOOLS, state.carouselTools)
        assertEquals(Tool.entries.toList(), state.shortcutTools)

        AI_TOOLS.forEachIndexed { index, tool ->
            if (index > 0) swipeCarouselLeft()
            assertTrue("After $index swipes, ${tool.name}'s Try now must be wholly on screen", tryNowWhollyInCarousel(tool))
            assertBanner(tool)
        }
    }

    /**
     * Under the carousel there are exactly four page dots, inside the dots row, each named
     * "Banner k of 4", and only the first is selected while banner 1 is in front.
     */
    @Test
    fun four_page_dots_mark_the_first_banner() {
        show()
        compose.onNodeWithTag(HOME_CAROUSEL_DOTS_TAG).assertIsDisplayed()
        compose.onAllNodes(isDot).assertCountEquals(AI_TOOLS.size)
        AI_TOOLS.indices.forEach { index ->
            compose.onNodeWithTag(homeCarouselDotTag(index))
                .assert(hasAnyAncestor(hasTestTag(HOME_CAROUSEL_DOTS_TAG)))
                .assertContentDescriptionEquals(
                    app.getString(R.string.home_hero_dot_description, index + 1, AI_TOOLS.size),
                )
        }
        assertDotSelected(0)
    }

    /**
     * One left swipe settles the carousel on banner 2 (Enhance): its "Try now" is wholly on
     * screen, inside the carousel and the window, and the dots mark banner 2 and no other.
     */
    @Test
    fun a_left_swipe_settles_on_banner_two_and_the_dots_mark_it() {
        show()
        swipeCarouselLeft()

        val second = AI_TOOLS[1]
        assertTrue("Banner 2's Try now must be wholly inside the carousel", tryNowWhollyInCarousel(second))
        val tryNow = box(homeTryTag(second))
        val rootWidth = compose.onRoot().fetchSemanticsNode().size.width
        assertTrue(
            "Banner 2's Try now must be wholly inside the window: $tryNow",
            tryNow.left >= -EPSILON && tryNow.right <= rootWidth + EPSILON,
        )
        assertDotSelected(1)
    }

    /**
     * The carousel never moves on its own: with the clock moved on ten seconds and no touch,
     * banner 1's "Try now" is where it was, still wholly on screen, and the dots still mark
     * banner 1.
     */
    @Test
    fun ten_seconds_without_a_touch_leave_banner_one_in_front() {
        show()
        val first = AI_TOOLS.first()
        val before = box(homeTryTag(first))

        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(TEN_SECONDS_MS)

        val after = box(homeTryTag(first))
        assertEquals("Banner 1's Try now must not move: $before, $after", before.left, after.left, EPSILON)
        assertTrue(tryNowWhollyInCarousel(first))
        assertDotSelected(0)
    }

    /**
     * The five shortcuts, left to right in the order of `Tool`, are all on screen at once, round
     * (as wide as tall), with their tops level within 1 dp and their centres evenly spaced within
     * 1 dp; each one's spoken name is its tool's name; each one's name is shown beneath it, its
     * centre on the button's centre within 1 dp.
     */
    @Test
    fun five_round_shortcuts_are_level_evenly_spaced_and_named_with_their_names_beneath() {
        show()
        assertShortcutGeometry()
    }

    /** The same shortcut geometry on Android 11, its own test so a failure names the version. */
    @Test
    @Config(sdk = [30])
    fun five_round_shortcuts_are_level_and_evenly_spaced_on_android_11() {
        show()
        assertShortcutGeometry()
    }

    /** A tap on each shortcut, left to right, reports exactly that shortcut's tool, once each. */
    @Test
    fun tapping_each_shortcut_reports_its_tool() {
        show()
        Tool.entries.forEach { tool ->
            compose.onNodeWithTag(homeShortcutTag(tool)).assertHasClickAction().performClick()
            compose.waitForIdle()
        }
        assertEquals(Tool.entries.toList(), tapped)
    }

    /** A tap on banner 1 and a tap on its "Try now" each report banner 1's tool. */
    @Test
    fun tapping_the_first_banner_or_its_try_now_reports_its_tool() {
        show()
        val first = AI_TOOLS.first()
        compose.onNodeWithTag(homeBannerTag(first)).assertHasClickAction().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(homeTryTag(first)).assertHasClickAction().performClick()
        compose.waitForIdle()
        assertEquals(listOf(first, first), tapped)
    }

    /**
     * The shortcut row does not scroll sideways: neither the row nor anything around a shortcut
     * has a horizontal scroll range (the vertical scroll of Home's column is allowed).
     */
    @Test
    fun the_shortcut_row_never_scrolls_sideways() {
        show()
        compose.onNodeWithTag(HOME_SHORTCUTS_TAG)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.HorizontalScrollAxisRange))
        Tool.entries.forEach { tool ->
            compose.onNodeWithTag(homeShortcutTag(tool))
                .assert(hasAnyAncestor(hasTestTag(HOME_SHORTCUTS_TAG)))
                .assert(!hasAnyAncestor(hasHorizontalScroll))
        }
        compose.onAllNodes(hasHorizontalScroll and hasAnyAncestor(hasTestTag(HOME_SHORTCUTS_TAG)))
            .assertCountEquals(0)
    }

    /**
     * Hosts [HomeContent] in the light theme with the state a real [HomeViewModel] starts with,
     * recording every reported tap in [tapped], waits until it is drawn and returns the state.
     */
    private fun show(): HomeUiState {
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val state = HomeViewModel(probe = ViewModelClearedProbe()).state.value
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                HomeContent(state = state, onToolClick = { tapped += it })
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(HOME_SCREEN_TAG).assertIsDisplayed()
        return state
    }

    /**
     * Fails unless the title "Kanso" (a heading), the greeting, the carousel, the page dots and
     * the shortcut row lie in that order, top to bottom, each starting no higher than the end of
     * the one before.
     */
    private fun assertRowsInOrder() {
        val title = box(hasText(app.getString(R.string.app_name)) and isHeading)
        val greeting = box(hasText(app.getString(R.string.home_hero_greeting)))
        val rows = listOf(
            "title" to title,
            "greeting" to greeting,
            "carousel" to box(HOME_CAROUSEL_TAG),
            "dots" to box(HOME_CAROUSEL_DOTS_TAG),
            "shortcuts" to box(HOME_SHORTCUTS_TAG),
        )
        rows.zipWithNext().forEach { (above, below) ->
            assertTrue(
                "${above.first} (${above.second}) must lie above ${below.first} (${below.second})",
                above.second.bottom <= below.second.top + EPSILON,
            )
        }
    }

    /**
     * Fails unless [tool]'s banner is in the carousel, shows the tool's name and description, has
     * its icon on the right half, and holds a clickable "Try now" whose spoken name names the tool.
     */
    private fun assertBanner(tool: Tool) {
        val name = app.getString(tool.labelRes)
        compose.onNodeWithTag(homeBannerTag(tool))
            .assert(hasAnyAncestor(hasTestTag(HOME_CAROUSEL_TAG)))
            .assert(hasText(name))
            .assert(hasText(description(tool)))
            .assertHasClickAction()

        // The icon is merged into the tappable card, so it is found in the unmerged tree.
        val banner = box(homeBannerTag(tool))
        val icon = box(homeBannerIconTag(tool), unmerged = true)
        assertTrue("${tool.name}'s icon must sit on the banner's right half: $icon in $banner", icon.center.x > banner.center.x)
        assertTrue("${tool.name}'s icon must be drawn", icon.width > 0f && icon.height > 0f)

        compose.onNodeWithTag(homeTryTag(tool))
            .assert(hasAnyAncestor(hasTestTag(homeBannerTag(tool))))
            .assert(hasText(app.getString(R.string.home_hero_try_now)))
            .assertContentDescriptionEquals(app.getString(R.string.home_hero_try_now_description, name))
            .assertHasClickAction()
    }

    /** The checks of the five shortcuts' shape, spacing, spoken names and names beneath them. */
    private fun assertShortcutGeometry() {
        val oneDp = with(compose.density) { 1.dp.toPx() }
        compose.onAllNodes(isShortcut).assertCountEquals(Tool.entries.size)

        val buttons = Tool.entries.map { tool -> tool to box(homeShortcutTag(tool)) }
        assertEquals(
            "The shortcuts must run left to right in the order of Tool",
            Tool.entries.toList(),
            buttons.sortedBy { it.second.center.x }.map { it.first },
        )
        val rootWidth = compose.onRoot().fetchSemanticsNode().size.width
        buttons.forEach { (tool, button) ->
            compose.onNodeWithTag(homeShortcutTag(tool))
                .assertIsDisplayed()
                .assertContentDescriptionEquals(app.getString(tool.labelRes))
            assertTrue("${tool.name}'s shortcut must be on screen: $button", button.left >= -EPSILON && button.right <= rootWidth + EPSILON)
            assertEquals("${tool.name}'s shortcut must be round: $button", button.width, button.height, oneDp)

            val label = box(homeShortcutLabelTag(tool))
            compose.onNodeWithTag(homeShortcutLabelTag(tool)).assert(hasText(app.getString(tool.labelRes)))
            assertTrue("${tool.name}'s name must lie beneath its button: $label, $button", label.top >= button.bottom - EPSILON)
            assertEquals("${tool.name}'s name must be centred under its button", button.center.x, label.center.x, oneDp)
        }

        val tops = buttons.map { it.second.top }
        assertTrue("The shortcuts' tops must be level within 1 dp: $tops", tops.max() - tops.min() <= oneDp)
        val gaps = buttons.map { it.second.center.x }.zipWithNext { left, right -> right - left }
        assertTrue("The shortcuts' centres must be evenly spaced within 1 dp: $gaps", gaps.max() - gaps.min() <= oneDp)
        assertTrue("The shortcuts must not overlap: $gaps", gaps.min() > 0f)
    }

    /** Swipes the carousel once to the left, as a finger would, and waits until it settles. */
    private fun swipeCarouselLeft() {
        // A slow drag of under one banner's width: one banner forward, not a fling past it.
        compose.onNodeWithTag(HOME_CAROUSEL_TAG).performTouchInput {
            swipeLeft(startX = width * 0.85f, endX = width * 0.4f, durationMillis = 1_000)
        }
        compose.waitForIdle()
    }

    /**
     * Tells whether [tool]'s "Try now" is composed and lies wholly inside the carousel, its whole
     * unclipped box from left to right. A banner the carousel has not composed yet counts as no.
     */
    private fun tryNowWhollyInCarousel(tool: Tool): Boolean {
        val nodes = compose.onAllNodesWithTag(homeTryTag(tool)).fetchSemanticsNodes()
        if (nodes.isEmpty()) return false
        val tryNow = Rect(nodes.single().positionInRoot, nodes.single().size.toSize())
        val carousel = box(HOME_CAROUSEL_TAG)
        return tryNow.left >= carousel.left - EPSILON && tryNow.right <= carousel.right + EPSILON
    }

    /** Fails unless the page dot at [index] is selected and every other dot is not. */
    private fun assertDotSelected(index: Int) {
        AI_TOOLS.indices.forEach { other ->
            val dot = compose.onNodeWithTag(homeCarouselDotTag(other))
            if (other == index) dot.assert(isCurrentDot) else dot.assert(!isCurrentDot)
        }
    }

    /** Returns the description line Home shows on [tool]'s banner. */
    private fun description(tool: Tool): String = app.getString(
        when (tool) {
            Tool.REMOVE_BACKGROUND -> R.string.home_hero_description_remove_background
            Tool.ENHANCE -> R.string.home_hero_description_enhance
            Tool.RETOUCH -> R.string.home_hero_description_retouch
            Tool.ARTISTIC -> R.string.home_hero_description_artistic
            Tool.EDIT -> error("Edit has no banner")
        },
    )

    /**
     * Returns the laid-out box of the one node tagged [tag], in pixels, from its position and size
     * (not clipped to what is visible). [unmerged] looks in the unmerged tree, for a node merged
     * into a tappable parent.
     */
    private fun box(tag: String, unmerged: Boolean = false): Rect {
        val node = compose.onNodeWithTag(tag, useUnmergedTree = unmerged).fetchSemanticsNode()
        return Rect(node.positionInRoot, node.size.toSize())
    }

    /** Returns the laid-out box of the one node matching [matcher], in pixels. */
    private fun box(matcher: SemanticsMatcher): Rect {
        val node = compose.onNode(matcher).fetchSemanticsNode()
        return Rect(node.positionInRoot, node.size.toSize())
    }

    /** Values and matchers the tests share. */
    private companion object {
        /** The four AI tools, in the order the carousel shows their banners. */
        val AI_TOOLS = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC)

        /** Ten seconds, in milliseconds: how long the carousel is left alone. */
        const val TEN_SECONDS_MS = 10_000L

        /** How far, in pixels, two positions may differ and still count as the same. */
        const val EPSILON = 1f

        /** The start of every page dot's tag, `home-carousel-dot-`, taken from the helper itself. */
        val DOT_TAG_PREFIX = homeCarouselDotTag(0).removeSuffix("0")

        /** The start of every shortcut's tag, `home-shortcut-`, taken from the helper itself. */
        val SHORTCUT_TAG_PREFIX = homeShortcutTag(Tool.EDIT).removeSuffix(Tool.EDIT.name)

        /** The end of a shortcut name's tag, `-label`, taken from the helper itself. */
        val LABEL_TAG_SUFFIX = homeShortcutLabelTag(Tool.EDIT).removePrefix(homeShortcutTag(Tool.EDIT))

        /** Matches the dot that carries the "current banner" state description. */
        val isCurrentDot = SemanticsMatcher("is the current dot") { node ->
            node.config.getOrNull(SemanticsProperties.StateDescription) != null
        }

        /** Matches every page dot under the carousel, whatever its index. */
        val isDot = SemanticsMatcher("is a carousel page dot") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(DOT_TAG_PREFIX) == true
        }

        /** Matches every round shortcut button (not the name under it), whatever its tool. */
        val isShortcut = SemanticsMatcher("is a Home shortcut button") { node ->
            val tag = node.config.getOrNull(SemanticsProperties.TestTag)
            tag != null && tag.startsWith(SHORTCUT_TAG_PREFIX) && !tag.endsWith(LABEL_TAG_SUFFIX)
        }

        /** Matches a node marked as a heading, as Home's title is. */
        val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

        /** Matches a node that scrolls sideways. */
        val hasHorizontalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
    }
}
