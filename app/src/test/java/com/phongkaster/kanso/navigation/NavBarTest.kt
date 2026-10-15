package com.phongkaster.kanso.navigation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 1 on the floating tab bar and its shell, hosted on their own (no MainActivity, no Koin):
 * `KansoTabShell` is wired to a plain back stack exactly as the navigation host wires it (the
 * bar shows while the top key is a tab, a tab tap calls `selectTab`, the Camera button pushes a
 * camera key while a tab is on top), with a plain stand-in screen per key.
 *
 * It checks the structure (three tabs with the Tab role, left to right, each an icon over its
 * label), the selection, the round Camera button outside the bar and to its right, the pill
 * shape, where the bar floats with no inset and with a gesture zone, that an open keyboard hides
 * the bar, that a hidden bar is not in the tree, and that everything fits on a 320 dp wide
 * phone. The shapes are read from the drawn pixels, so Robolectric's real drawing code is on
 * (`GraphicsMode.Mode.NATIVE`).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = PHONE)
class NavBarTest {

    /** Starts an empty activity to host the shell and hands the test that activity. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The back stack the hosted shell is wired to, Home at the bottom. */
    private val backStack = mutableStateListOf<NavKey>(Home)

    /** How many times the Camera button was tapped. */
    private var cameraTaps = 0

    /** The Android view the shell is drawn in, the one that receives window insets. */
    private var composeView: View? = null

    /** Whether the shell, at its last drawing, saw the keyboard open. */
    private var keyboardSeen = false

    /** The clearance `tabBarClearance()` returned at the last drawing. */
    private var clearanceSeen = Dp.Unspecified

    /**
     * Stops Koin: the test does not use it, but KansoApplication starts it for every Robolectric
     * test, and its state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * The bar holds exactly three nodes with the Tab role, left to right `nav-tab-HOME`,
     * `nav-tab-MY_EDITS` and `nav-tab-SETTINGS`, reading "Home", "My edits" and "Settings"; each
     * holds its icon `<tab>-icon`, whose horizontal centre is within 1 dp of its label's and whose
     * bottom is at or above the label's top.
     */
    @Test
    fun the_bar_holds_three_tabs_left_to_right_each_an_icon_over_its_label() {
        showShell()
        compose.onAllNodesWithTag(KANSO_TAB_BAR_TAG).assertCountEquals(1)

        val tabs = compose.onAllNodes(isTab and hasAnyAncestor(hasTestTag(KANSO_TAB_BAR_TAG)))
            .fetchSemanticsNodes()
            .sortedBy { it.positionInRoot.x }
        assertEquals(
            "The bar must hold exactly the three tabs, left to right",
            KansoTab.entries.map { kansoTabTag(it) },
            tabs.map { it.config.getOrNull(SemanticsProperties.TestTag) },
        )

        val oneDp = px(1.dp)
        KansoTab.entries.forEach { tab ->
            compose.onNodeWithTag(kansoTabTag(tab)).assert(hasText(label(tab)))
            val icon = compose.onNode(
                hasTestTag(kansoTabIconTag(tab)) and hasAnyAncestor(hasTestTag(kansoTabTag(tab))),
                useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot
            val text = labelNode(tab).boundsInRoot
            assertTrue(
                "The ${tab.name} icon (centre ${icon.center.x}) must sit over its label (centre ${text.center.x})",
                abs(icon.center.x - text.center.x) <= oneDp,
            )
            assertTrue(
                "The ${tab.name} icon (bottom ${icon.bottom}) must end at or above its label (top ${text.top})",
                icon.bottom <= text.top + 0.5f,
            )
        }
    }

    /**
     * Home is selected at the start and the others are not; tapping each tab shows its screen,
     * selects it and deselects the others, with one tab above Home on the back stack; tapping the
     * selected tab again changes nothing.
     */
    @Test
    fun the_selected_tab_follows_taps_and_tapping_it_again_changes_nothing() {
        showShell()
        assertSelected(KansoTab.HOME)

        compose.onNodeWithTag(kansoTabTag(KansoTab.MY_EDITS)).performClick()
        compose.onNodeWithTag(standInTag(MyEdits)).assertExists()
        assertSelected(KansoTab.MY_EDITS)
        assertEquals(listOf<NavKey>(Home, MyEdits), backStack.toList())

        compose.onNodeWithTag(kansoTabTag(KansoTab.SETTINGS)).performClick()
        compose.onNodeWithTag(standInTag(Settings)).assertExists()
        assertSelected(KansoTab.SETTINGS)
        assertEquals(listOf<NavKey>(Home, Settings), backStack.toList())

        compose.onNodeWithTag(kansoTabTag(KansoTab.SETTINGS)).performClick()
        compose.onNodeWithTag(standInTag(Settings)).assertExists()
        assertSelected(KansoTab.SETTINGS)
        assertEquals(listOf<NavKey>(Home, Settings), backStack.toList())

        compose.onNodeWithTag(kansoTabTag(KansoTab.HOME)).performClick()
        compose.onNodeWithTag(standInTag(Home)).assertExists()
        assertSelected(KansoTab.HOME)
        assertEquals(listOf<NavKey>(Home), backStack.toList())
    }

    /**
     * `nav-camera` is a button named "Camera" with no visible text, outside `nav-bar` and to its
     * right, its vertical centre within 4 dp of the bar's, and round: as wide as it is tall, its
     * corner is not the button's colour while the inside of its left edge is.
     */
    @Test
    fun the_camera_button_is_round_named_camera_outside_the_bar_and_to_its_right() {
        showShell()
        val camera = compose.onNodeWithTag(KANSO_CAMERA_BUTTON_TAG)
            .assert(hasContentDescription(context().getString(R.string.tool_camera)))
            .assert(!hasAnyAncestor(hasTestTag(KANSO_TAB_BAR_TAG)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .fetchSemanticsNode()
        assertTrue(
            "The Camera button must show no text",
            camera.config.getOrNull(SemanticsProperties.Text).isNullOrEmpty(),
        )

        val bar = bounds(KANSO_TAB_BAR_TAG)
        val button = camera.boundsInRoot
        assertTrue("The Camera button (left ${button.left}) must lie right of the bar (right ${bar.right})", button.left >= bar.right)
        assertTrue(
            "The Camera button's centre (${button.center.y}) must be within 4 dp of the bar's (${bar.center.y})",
            abs(button.center.y - bar.center.y) <= px(4.dp),
        )
        assertEquals("The Camera button must be as wide as it is tall", button.width, button.height, px(1.dp))

        val container = scheme().primary
        assertNear("Inside the Camera button's left edge", container, pixel(button.left + px(4.dp), button.center.y), 2)
        assertFar("The Camera button's corner", container, pixel(button.left + 2f, button.top + 2f))
    }

    /**
     * `nav-bar` is a pill on the inverse surface: inside its left end, at mid height, it is
     * `inverseSurface`, while its top-left corner is not, because the ends are half circles.
     */
    @Test
    fun the_bar_is_a_pill_on_the_inverse_surface() {
        showShell()
        val bar = bounds(KANSO_TAB_BAR_TAG)
        val ground = scheme().inverseSurface
        assertNear("Inside the bar's left end", ground, pixel(bar.left + px(4.dp), bar.center.y), 2)
        assertFar("The bar's top-left corner", ground, pixel(bar.left + 2f, bar.top + 2f))
        assertFar("The bar's bottom-right corner", ground, pixel(bar.right - 3f, bar.bottom - 3f))
    }

    /**
     * With a 0 dp inset sent (no navigation bar, no gesture zone), the bottoms of `nav-bar` and
     * `nav-camera` are at least 8 dp above the window's bottom (16 dp by design), and the space a
     * tab screen leaves for the bar is the bar's 64 dp plus 16 dp below and above it.
     */
    @Test
    fun with_no_insets_the_bar_and_the_button_float_above_the_bottom() {
        showShell()
        sendInsets(WindowInsetsCompat.Builder().build())

        val bottom = compose.onRoot().fetchSemanticsNode().size.height.toFloat()
        assertAtLeastAbove("nav-bar", bounds(KANSO_TAB_BAR_TAG).bottom, bottom, px(8.dp))
        assertAtLeastAbove("nav-camera", bounds(KANSO_CAMERA_BUTTON_TAG).bottom, bottom, px(8.dp))
        assertEquals("tabBarClearance() with no insets", 96f, clearanceSeen.value, 0.5f)
    }

    /**
     * With a 0 dp navigation bar and a 32 dp bottom gesture zone (swipe navigation with the hint
     * hidden), the bar and the button float above the gesture zone and 16 dp more, so no tab sits
     * where a swipe up goes home; the clearance grows by the zone.
     */
    @Test
    fun a_gesture_zone_with_a_zero_tall_navigation_bar_lifts_the_bar_above_the_zone() {
        showShell()
        val zone = px(GESTURE_ZONE).roundToInt()
        sendInsets(
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemGestures(), Insets.of(0, 0, 0, zone))
                .build(),
        )

        val bottom = compose.onRoot().fetchSemanticsNode().size.height.toFloat()
        val lift = zone + px(16.dp) - px(1.dp)
        assertAtLeastAbove("nav-bar", bounds(KANSO_TAB_BAR_TAG).bottom, bottom, lift)
        assertAtLeastAbove("nav-camera", bounds(KANSO_CAMERA_BUTTON_TAG).bottom, bottom, lift)
        assertEquals("tabBarClearance() with a 32 dp gesture zone", 128f, clearanceSeen.value, 1f)
    }

    /**
     * While the keyboard is open (an IME inset sent), neither the bar nor the Camera button is
     * shown; once it closes, both come back.
     */
    @Test
    fun while_the_keyboard_is_open_the_bar_and_the_camera_button_are_not_shown() {
        showShell()
        compose.onNodeWithTag(KANSO_TAB_BAR_TAG).assertIsDisplayed()

        sendInsets(
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, px(KEYBOARD).roundToInt()))
                .setVisible(WindowInsetsCompat.Type.ime(), true)
                .build(),
        )
        assertTrue("The shell never saw the keyboard the test opened", keyboardSeen)
        compose.onNodeWithTag(KANSO_TAB_BAR_TAG).assertDoesNotExist()
        compose.onNodeWithTag(KANSO_CAMERA_BUTTON_TAG).assertDoesNotExist()

        sendInsets(
            WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 0))
                .setVisible(WindowInsetsCompat.Type.ime(), false)
                .build(),
        )
        assertFalse("The shell still sees the keyboard after it closed", keyboardSeen)
        compose.onNodeWithTag(KANSO_TAB_BAR_TAG).assertIsDisplayed()
        compose.onNodeWithTag(KANSO_CAMERA_BUTTON_TAG).assertIsDisplayed()
    }

    /**
     * A screen that is not a tab hides the bar entirely: after the Camera button pushes the
     * camera key, neither the bar nor the button is in the tree; back on Home, both return with
     * Home selected.
     */
    @Test
    fun a_hidden_bar_is_not_in_the_tree_and_comes_back_on_a_tab() {
        showShell()
        compose.onNodeWithTag(KANSO_CAMERA_BUTTON_TAG).performClick()
        compose.waitForIdle()
        assertEquals(1, cameraTaps)
        assertEquals(listOf(Home, Camera(Tool.EDIT)), backStack.toList())
        compose.onNodeWithTag(standInTag(Camera(Tool.EDIT))).assertExists()
        compose.onNodeWithTag(KANSO_TAB_BAR_TAG).assertDoesNotExist()
        compose.onNodeWithTag(KANSO_CAMERA_BUTTON_TAG).assertDoesNotExist()

        compose.runOnIdle { backStack.removeLastOrNull() }
        compose.waitForIdle()
        compose.onNodeWithTag(KANSO_TAB_BAR_TAG).assertIsDisplayed()
        compose.onNodeWithTag(KANSO_CAMERA_BUTTON_TAG).assertIsDisplayed()
        assertSelected(KansoTab.HOME)
    }

    /**
     * On Robolectric's default 320 x 470 dp screen the three tabs and the Camera button all fit:
     * every one is shown inside the window, the tabs lie inside the bar and left of the button,
     * each tab is at least 48 dp wide, and each label is drawn whole on one line.
     */
    @Test
    @Config(qualifiers = "w320dp-h470dp")
    fun on_a_320_dp_screen_the_three_tabs_and_the_camera_button_fit() {
        showShell()
        val width = compose.onRoot().fetchSemanticsNode().size.width.toFloat()
        val bar = bounds(KANSO_TAB_BAR_TAG)
        val button = bounds(KANSO_CAMERA_BUTTON_TAG)
        compose.onNodeWithTag(KANSO_CAMERA_BUTTON_TAG).assertIsDisplayed()
        assertTrue("The Camera button (right ${button.right}) must end inside the window ($width)", button.right <= width)
        assertTrue("The bar (right ${bar.right}) must end left of the button (${button.left})", bar.right <= button.left)
        KansoTab.entries.forEach { tab ->
            compose.onNodeWithTag(kansoTabTag(tab)).assertIsDisplayed()
            val item = bounds(kansoTabTag(tab))
            assertTrue("${tab.name} must lie inside the bar", item.left >= bar.left - 0.5f && item.right <= bar.right + 0.5f)
            assertTrue("${tab.name} must be at least 48 dp wide, was ${item.width} px", item.width >= px(48.dp))
            val layout = textLayout(labelNode(tab))
            assertEquals("The ${tab.name} label must take one line", 1, layout.lineCount)
            // `hasVisualOverflow` reads true here for a label that is whole (Robolectric's font
            // metrics), so "not cut off" is checked from the geometry instead: the longest line ends
            // within the width the text was given, and the label lies inside its item.
            val label = labelNode(tab).boundsInRoot
            assertTrue("The ${tab.name} label must not be cut off", layout.getLineRight(0) <= layout.size.width + 0.5f)
            assertTrue(
                "The ${tab.name} label must lie inside its item",
                label.left >= item.left - 0.5f && label.right <= item.right + 0.5f,
            )
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Shows the shell in Kanso's light theme, wired to [backStack] the way the navigation host
     * wires it, with a stand-in screen for the key on top, and a probe that reports the view,
     * the keyboard and the clearance back to the test.
     */
    private fun showShell() {
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                Probe()
                val top = backStack.last()
                KansoTabShell(
                    showBar = isTabKey(top),
                    selected = KansoTab.entries.firstOrNull { it.key == top } ?: KansoTab.HOME,
                    onTabSelected = { tab -> backStack.selectTab(tab) },
                    onCamera = {
                        cameraTaps++
                        if (isTabKey(backStack.last())) backStack.add(Camera(Tool.EDIT))
                    },
                ) {
                    StandIn(top)
                }
            }
        }
        compose.waitForIdle()
    }

    /**
     * Reads, while the shell is drawn, the Android view it is drawn in, whether the keyboard is
     * open and the clearance the bar asks for, and hands them to the test after each drawing.
     */
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun Probe() {
        val view = LocalView.current
        val keyboard = WindowInsets.isImeVisible
        val clearance = tabBarClearance()
        SideEffect {
            composeView = view
            keyboardSeen = keyboard
            clearanceSeen = clearance
        }
    }

    /** A plain screen for [key]: the theme's background, tagged with [standInTag]. */
    @Composable
    private fun StandIn(key: NavKey) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag(standInTag(key)),
        )
    }

    /** Returns the stand-in screen's tag for [key]. */
    private fun standInTag(key: NavKey): String = "stand-in-$key"

    /** Sends [insets] to the view the shell is drawn in, as the system would, and lets it settle. */
    private fun sendInsets(insets: WindowInsetsCompat) {
        compose.runOnIdle { ViewCompat.dispatchApplyWindowInsets(checkNotNull(composeView), insets) }
        compose.waitForIdle()
    }

    /** Fails unless [tab] is selected on the bar and the other two tabs are not. */
    private fun assertSelected(tab: KansoTab) {
        KansoTab.entries.forEach { other ->
            val node = compose.onNodeWithTag(kansoTabTag(other))
            if (other == tab) node.assertIsSelected() else node.assertIsNotSelected()
        }
    }

    /** Fails unless [nodeBottom] is at least [gap] pixels above [windowBottom]; [what] names the node. */
    private fun assertAtLeastAbove(what: String, nodeBottom: Float, windowBottom: Float, gap: Float) {
        assertTrue(
            "$what ends at $nodeBottom px, less than $gap px above the window's bottom ($windowBottom px)",
            windowBottom - nodeBottom >= gap,
        )
    }

    /** Returns the label text node inside [tab]'s item, from the unmerged tree. */
    private fun labelNode(tab: KansoTab): SemanticsNode = compose.onNode(
        hasText(label(tab)) and hasAnyAncestor(hasTestTag(kansoTabTag(tab))),
        useUnmergedTree = true,
    ).fetchSemanticsNode()

    /** Returns how the text of [node] was laid out, through its semantics action. */
    private fun textLayout(node: SemanticsNode): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    /** Returns the bounds of the one node tagged [tag], in the root's pixels (nothing here scrolls). */
    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    /** Returns the colour scheme KansoTheme hands to the shell. */
    private fun scheme(): ColorScheme =
        compose.onNodeWithTag(KANSO_THEME_TAG).fetchSemanticsNode().config[KansoColorSchemeKey]

    /** Returns [tab]'s label as the bar shows it. */
    private fun label(tab: KansoTab): String = context().getString(tab.labelRes)

    /** Returns the app context, to read the same strings the bar shows. */
    private fun context(): android.content.Context = ApplicationProvider.getApplicationContext()

    /** Returns [dp] in pixels at the test screen's density. */
    private fun px(dp: Dp): Float = with(compose.density) { dp.toPx() }

    /**
     * Draws the activity's content view into a fresh bitmap and returns the colour of the pixel
     * at ([x], [y]), in the Compose root's pixels (the root fills the view from its top-left).
     */
    private fun pixel(x: Float, y: Float): Color {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return try {
            view.draw(Canvas(bitmap))
            val px = x.roundToInt().coerceIn(0, bitmap.width - 1)
            val py = y.roundToInt().coerceIn(0, bitmap.height - 1)
            Color(bitmap.getPixel(px, py))
        } finally {
            bitmap.recycle()
        }
    }

    /** Fails unless [actual] equals [expected] within [tolerance] in each 0-255 channel. */
    private fun assertNear(what: String, expected: Color, actual: Color, tolerance: Int) {
        assertTrue("$what: expected ${hex(expected)}, drawn ${hex(actual)}", channelsWithin(expected, actual, tolerance))
    }

    /** Fails unless [actual] differs from [colour] by more than 8 in at least one 0-255 channel. */
    private fun assertFar(what: String, colour: Color, actual: Color) {
        assertFalse("$what should not be ${hex(colour)}, drawn ${hex(actual)}", channelsWithin(colour, actual, 8))
    }

    /** Tells whether [a] and [b] are within [tolerance] of each other in red, green and blue. */
    private fun channelsWithin(a: Color, b: Color, tolerance: Int): Boolean {
        val first = a.toArgb()
        val second = b.toArgb()
        return listOf(16, 8, 0).all { shift ->
            abs(((first shr shift) and 0xFF) - ((second shr shift) and 0xFF)) <= tolerance
        }
    }

    /** Writes [colour] as `#AARRGGBB`, for failure messages. */
    private fun hex(colour: Color): String = "#%08X".format(colour.toArgb())

    /** Values the tests share. */
    private companion object {
        /** The bottom gesture zone of the gesture test: 32 dp, a typical swipe-navigation zone. */
        val GESTURE_ZONE = 32.dp

        /** The keyboard's height in the keyboard test. */
        val KEYBOARD = 300.dp

        /** Matches a node with the Tab role, as each item of the bar is. */
        val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    }
}

/** The phone screen the hosted bar is drawn on: 411 x 891 dp at xxhdpi, as in the pictures. */
private const val PHONE = "w411dp-h891dp-xxhdpi"
