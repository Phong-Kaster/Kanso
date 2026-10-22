package com.phongkaster.kanso.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.backToSettingsMenu
import com.phongkaster.kanso.navigation.openThemeColourScreen
import com.phongkaster.kanso.navigation.openThemeModeScreen
import com.phongkaster.kanso.navigation.openTab
import com.phongkaster.kanso.navigation.scrollAboveBar
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_GROUP_APPEARANCE_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_GROUP_COLOUR_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_GROUP_MENU_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_MENU_TEXT_SIZE_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_MENU_THEME_COLOUR_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_MENU_THEME_MODE_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_KANSO_BLUE_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_WALLPAPER_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_DARK_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_LIGHT_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_SYSTEM_TAG
import com.phongkaster.kanso.ui.settings.settingsRowDescriptionTag
import com.phongkaster.kanso.ui.settings.settingsRowIconBadgeTag
import com.phongkaster.kanso.ui.settings.settingsRowIconTag
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 15: Settings draws its choices as rounded cards whose rows each show an icon in a tinted
 * rounded square, a title and a one-line description.
 *
 * Settings is reached from a fresh install through the floating bar's Settings tab (its tag, see
 * `TabTaps.openTab`). The cards and squares are checked on the drawn pixels: a card's corner pixel
 * must be the screen's own background (the corner is cut, so the card is rounded) while its edge
 * is a container colour clearly set apart from that background, and each square must be one of
 * the theme's container colours, clearly set apart from its card. The descriptions are read from
 * the semantics tree, where every row's square, icon and description keep their own tags in the
 * unmerged tree (the row merges them for screen readers).
 *
 * Each Android version has its own tests: at SDK 35 the colour card holds the wallpaper row, at
 * SDK 30 (no wallpaper colours before Android 12) it does not.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SettingsRowsTest {

    /** Launches MainActivity before each test and lets the test find and read its nodes. */
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** The app context, used to read the same string resources the screens show. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test. Koin's state outlives the test's Application inside the same
     * JVM, and starting it twice fails.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * In light mode the menu's card and the cards of the Theme mode and Theme colour screens are
     * rounded and stand out from the light background; the menu, a tab, has no Back.
     */
    @Test
    @Config(qualifiers = "notnight")
    fun cards_are_rounded_and_set_apart_from_a_light_background() {
        openSettings()
        assertNoBackButton()
        assertRoundedCard(SETTINGS_GROUP_MENU_TAG)
        onScreenOf(SETTINGS_GROUP_APPEARANCE_TAG) { assertRoundedCard(SETTINGS_GROUP_APPEARANCE_TAG) }
        onScreenOf(SETTINGS_GROUP_COLOUR_TAG) { assertRoundedCard(SETTINGS_GROUP_COLOUR_TAG) }
    }

    /** In dark mode the menu's card and the setting screens' cards are rounded and stand out. */
    @Test
    @Config(qualifiers = "night")
    fun cards_are_rounded_and_set_apart_from_a_dark_background() {
        openSettings()
        assertRoundedCard(SETTINGS_GROUP_MENU_TAG)
        onScreenOf(SETTINGS_GROUP_APPEARANCE_TAG) { assertRoundedCard(SETTINGS_GROUP_APPEARANCE_TAG) }
        onScreenOf(SETTINGS_GROUP_COLOUR_TAG) { assertRoundedCard(SETTINGS_GROUP_COLOUR_TAG) }
    }

    /** At SDK 35 every row, the wallpaper row included, shows its icon in a tinted rounded square. */
    @Test
    @Config(qualifiers = "notnight")
    fun every_row_has_its_icon_in_a_tinted_rounded_square_at_sdk_35() {
        openSettings()
        ROWS_SDK_35.forEach { (rowTag, groupTag) -> onScreenOf(groupTag) { assertBadge(rowTag, groupTag) } }
    }

    /** At SDK 35 the squares are also set apart from the cards in dark mode. */
    @Test
    @Config(qualifiers = "night")
    fun every_row_has_its_icon_in_a_tinted_rounded_square_in_dark_at_sdk_35() {
        openSettings()
        ROWS_SDK_35.forEach { (rowTag, groupTag) -> onScreenOf(groupTag) { assertBadge(rowTag, groupTag) } }
    }

    /** At SDK 30 there is no wallpaper row, and every other row shows its tinted square. */
    @Test
    @Config(sdk = [30], qualifiers = "notnight")
    fun every_row_has_its_icon_in_a_tinted_rounded_square_at_sdk_30() {
        openSettings()
        onScreenOf(SETTINGS_GROUP_COLOUR_TAG) { assertNoWallpaperRow() }
        ROWS_SDK_30.forEach { (rowTag, groupTag) -> onScreenOf(groupTag) { assertBadge(rowTag, groupTag) } }
    }

    /** At SDK 35 every row holds its title and a one-line description of its own. */
    @Test
    @Config(qualifiers = "notnight")
    fun every_row_has_a_title_and_a_one_line_description_at_sdk_35() {
        openSettings()
        assertDescriptions(ROWS_SDK_35)
        assertMenuSummaries()
    }

    /** At SDK 30 every row but the absent wallpaper one holds its title and description. */
    @Test
    @Config(sdk = [30], qualifiers = "notnight")
    fun every_row_has_a_title_and_a_one_line_description_at_sdk_30() {
        openSettings()
        onScreenOf(SETTINGS_GROUP_COLOUR_TAG) { assertNoWallpaperRow() }
        assertDescriptions(ROWS_SDK_30)
    }

    /** Fails if a node described as "Back" exists anywhere on the Settings screen. */
    private fun assertNoBackButton() {
        val backs = compose.onAllNodes(
            hasContentDescription(context.getString(R.string.back)) and
                hasAnyAncestor(hasTestTag(SETTINGS_SCREEN_TAG)),
            useUnmergedTree = true,
        ).fetchSemanticsNodes()
        assertTrue("Settings is a tab and must have no Back button", backs.isEmpty())
    }

    /** Fails if the wallpaper row exists (it must not below Android 12). */
    private fun assertNoWallpaperRow() {
        assertTrue(
            "There must be no wallpaper row below Android 12",
            compose.onAllNodesWithTag(THEME_COLOUR_WALLPAPER_TAG).fetchSemanticsNodes().isEmpty(),
        )
    }

    /**
     * Scrolls the card tagged [groupTag] above the floating bar and reads three pixels: its very
     * top-left corner, a pixel just inside its left edge halfway down, and a pixel of the screen
     * left of the card. Fails unless the corner equals the screen's background (the corner is cut,
     * so the card is rounded), and the edge is `surfaceContainerHigh` or `surfaceContainerHighest`
     * and differs from the background by more than [CLEAR_DIFFERENCE] in a channel.
     */
    private fun assertRoundedCard(groupTag: String) {
        compose.onNodeWithTag(groupTag).scrollAboveBar(compose)
        compose.waitForIdle()
        val card = compose.onNodeWithTag(groupTag).fetchSemanticsNode()
        val scheme = appliedScheme()
        val left = card.positionInRoot.x.roundToInt()
        val top = card.positionInRoot.y.roundToInt()
        val middleY = top + card.size.height / 2
        val inset = px(2)
        val outside = px(8)
        val (corner, edge, ground) = drawScreen { bitmap ->
            Triple(
                pixel(bitmap, left + 1, top + 1),
                pixel(bitmap, left + inset, middleY),
                pixel(bitmap, left - outside, middleY),
            )
        }
        assertTrue(
            "The corner of $groupTag must be the background ${hex(ground)} (rounded), was ${hex(corner)}",
            maxChannelDifference(corner, ground) <= TOLERANCE,
        )
        val roles = listOf(
            "surfaceContainerHigh" to scheme.surfaceContainerHigh,
            "surfaceContainerHighest" to scheme.surfaceContainerHighest,
        )
        assertTrue(
            "The card $groupTag must be surfaceContainerHigh or higher, was ${hex(edge)}",
            roles.any { (_, role) -> maxChannelDifference(edge, role) <= TOLERANCE },
        )
        assertTrue(
            "The card $groupTag (${hex(edge)}) must differ from the background (${hex(ground)}) " +
                "by more than $CLEAR_DIFFERENCE in a channel",
            maxChannelDifference(edge, ground) > CLEAR_DIFFERENCE,
        )
    }

    /**
     * Scrolls the row tagged [rowTag] above the floating bar, then fails unless it lies in the
     * card [groupTag] and holds its square (`<row>-icon-badge`) with its icon (`<row>-icon`)
     * inside; the square's width equals its height within 1 dp, its corner is cut (rounded), its
     * fill equals one of the theme's container roles within [TOLERANCE], and that fill differs
     * from the card's colour beside the row by more than [CLEAR_DIFFERENCE] in a channel.
     */
    private fun assertBadge(rowTag: String, groupTag: String) {
        compose.onNodeWithTag(rowTag).scrollAboveBar(compose)
        compose.waitForIdle()
        compose.onNode(hasTestTag(rowTag) and hasAnyAncestor(hasTestTag(groupTag))).assertExists()
        val badgeTag = settingsRowIconBadgeTag(rowTag)
        val badge = compose.onNode(
            hasTestTag(badgeTag) and hasAnyAncestor(hasTestTag(rowTag)),
            useUnmergedTree = true,
        ).fetchSemanticsNode()
        compose.onNode(
            hasTestTag(settingsRowIconTag(rowTag)) and hasAnyAncestor(hasTestTag(badgeTag)),
            useUnmergedTree = true,
        ).assertExists()

        val width = badge.size.width
        val height = badge.size.height
        assertTrue(
            "The square $badgeTag must be as wide as it is tall within 1 dp, was $width x $height px",
            abs(width - height) <= px(1),
        )

        val scheme = appliedScheme()
        val card = compose.onNodeWithTag(groupTag).fetchSemanticsNode()
        val left = badge.positionInRoot.x.roundToInt()
        val top = badge.positionInRoot.y.roundToInt()
        val middleY = top + height / 2
        val cardX = card.positionInRoot.x.roundToInt() + px(2)
        val (fill, corner, cardColour) = drawScreen { bitmap ->
            Triple(
                pixel(bitmap, left + px(4), middleY),
                pixel(bitmap, left, top),
                pixel(bitmap, cardX, middleY),
            )
        }
        val role = containerRoles(scheme).firstOrNull { (_, colour) ->
            maxChannelDifference(fill, colour) <= TOLERANCE
        }
        assertTrue("The square $badgeTag (${hex(fill)}) must be a container role of the scheme", role != null)
        assertTrue(
            "The square $badgeTag (${hex(fill)}) must differ from its card (${hex(cardColour)}) " +
                "by more than $CLEAR_DIFFERENCE in a channel",
            maxChannelDifference(fill, cardColour) > CLEAR_DIFFERENCE,
        )
        assertTrue(
            "The corner of $badgeTag must be cut (rounded): it drew the fill ${hex(corner)}",
            maxChannelDifference(corner, fill) > CLEAR_DIFFERENCE,
        )
    }

    /**
     * Fails unless each row of [rows] holds its title and its description (`<row>-description`),
     * which is not empty, is laid out on one line and is not the title, and unless no two rows
     * share a description.
     */
    private fun assertDescriptions(rows: List<Pair<String, String>>) {
        val descriptions = rows.map { (rowTag, groupTag) -> onScreenOf(groupTag) { descriptionOf(rowTag) } }
        assertEquals(
            "Every row must have a description of its own: $descriptions",
            descriptions.size,
            descriptions.toSet().size,
        )
    }

    /**
     * Fails unless the Settings menu shows its three rows (Theme mode, Theme colour, Text size),
     * each with a non-empty one-line summary (the setting's current value) of its own.
     */
    private fun assertMenuSummaries() {
        val summaries = listOf(
            SETTINGS_MENU_THEME_MODE_TAG,
            SETTINGS_MENU_THEME_COLOUR_TAG,
            SETTINGS_MENU_TEXT_SIZE_TAG,
        ).map { rowTag ->
            compose.onNodeWithTag(rowTag).scrollAboveBar(compose)
            val node = compose.onNode(
                hasTestTag(settingsRowDescriptionTag(rowTag)) and hasAnyAncestor(hasTestTag(rowTag)),
                useUnmergedTree = true,
            ).fetchSemanticsNode()
            assertTrue("The summary of $rowTag must not be empty", textOf(node).isNotBlank())
            assertEquals("The summary of $rowTag must be drawn on one line", 1, lineCountOf(node))
            textOf(node)
        }
        assertEquals("Every menu row needs its own summary: $summaries", summaries.size, summaries.toSet().size)
    }

    /**
     * Opens the setting screen that holds the card [groupTag] (the Theme mode screen for the mode
     * card, the Theme colour screen for the colour card), runs [block] on it, and goes back to the
     * Settings menu. Returns what [block] returns.
     */
    private fun <T> onScreenOf(groupTag: String, block: () -> T): T {
        if (groupTag == SETTINGS_GROUP_APPEARANCE_TAG) openThemeModeScreen(compose) else openThemeColourScreen(compose)
        val result = block()
        backToSettingsMenu(compose)
        return result
    }

    /**
     * Fails unless the row [rowTag] holds its title and its description (`<row>-description`),
     * which is not empty, is laid out on one line and is not the title; returns the description.
     */
    private fun descriptionOf(rowTag: String): String {
        val descriptions = listOf(rowTag).map { rowTag ->
            compose.onNode(
                hasText(context.getString(titleOf(rowTag))) and hasAnyAncestor(hasTestTag(rowTag)),
                useUnmergedTree = true,
            ).assertExists()
            val node = compose.onNode(
                hasTestTag(settingsRowDescriptionTag(rowTag)) and hasAnyAncestor(hasTestTag(rowTag)),
                useUnmergedTree = true,
            ).fetchSemanticsNode()
            val text = textOf(node)
            assertTrue("The description of $rowTag must not be empty", text.isNotBlank())
            assertTrue(
                "The description of $rowTag must not repeat its title",
                text != context.getString(titleOf(rowTag)),
            )
            assertEquals("The description of $rowTag must be drawn on one line", 1, lineCountOf(node))
            text
        }
        return descriptions.single()
    }

    /** Returns the text of the text node [node]. */
    private fun textOf(node: SemanticsNode): String =
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString("") { it.text }

    /** Returns how many lines the text node [node] was laid out on. */
    private fun lineCountOf(node: SemanticsNode): Int {
        val layouts = mutableListOf<TextLayoutResult>()
        val action = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action
        checkNotNull(action) { "The description node has no text layout" }.invoke(layouts)
        return layouts.single().lineCount
    }

    /** Returns the string resource of the title of the row tagged [rowTag]. */
    private fun titleOf(rowTag: String): Int = when (rowTag) {
        THEME_MODE_SYSTEM_TAG -> R.string.theme_mode_system
        THEME_MODE_LIGHT_TAG -> R.string.theme_mode_light
        THEME_MODE_DARK_TAG -> R.string.theme_mode_dark
        THEME_COLOUR_KANSO_BLUE_TAG -> R.string.theme_colour_kanso_blue
        THEME_COLOUR_WALLPAPER_TAG -> R.string.theme_colour_wallpaper
        THEME_COLOUR_CUSTOM_TAG -> R.string.theme_colour_custom
        else -> error("Unknown row $rowTag")
    }

    /** Returns every container role of [scheme], with its name. */
    private fun containerRoles(scheme: ColorScheme): List<Pair<String, Color>> = listOf(
        "primaryContainer" to scheme.primaryContainer,
        "secondaryContainer" to scheme.secondaryContainer,
        "tertiaryContainer" to scheme.tertiaryContainer,
        "errorContainer" to scheme.errorContainer,
        "surfaceContainerLowest" to scheme.surfaceContainerLowest,
        "surfaceContainerLow" to scheme.surfaceContainerLow,
        "surfaceContainer" to scheme.surfaceContainer,
        "surfaceContainerHigh" to scheme.surfaceContainerHigh,
        "surfaceContainerHighest" to scheme.surfaceContainerHighest,
    )

    /** Returns the colour scheme the app's theme applies, which it writes on its root node. */
    private fun appliedScheme(): ColorScheme =
        compose.onNodeWithTag(KANSO_THEME_TAG).fetchSemanticsNode().config[KansoColorSchemeKey]

    /** Returns [dp] density-independent pixels in whole screen pixels, at least 1. */
    private fun px(dp: Int): Int = with(compose.density) { dp.dp.toPx() }.roundToInt().coerceAtLeast(1)

    /**
     * Draws the activity's content view into a fresh bitmap, hands it to [read], then frees it.
     * The Compose root fills the content view from its top-left corner, so a node's position in
     * the root is its position in the bitmap.
     */
    private fun <T> drawScreen(read: (Bitmap) -> T): T {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return try {
            view.draw(Canvas(bitmap))
            read(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /** Returns the colour of [bitmap] at ([x], [y]), failing if that lies outside it. */
    private fun pixel(bitmap: Bitmap, x: Int, y: Int): Color {
        check(x in 0 until bitmap.width && y in 0 until bitmap.height) {
            "($x, $y) lies outside the drawn screen of ${bitmap.width} x ${bitmap.height}"
        }
        return Color(bitmap.getPixel(x, y))
    }

    /** Returns the largest difference between [a] and [b] in red, green or blue (0 to 255). */
    private fun maxChannelDifference(a: Color, b: Color): Int {
        val first = a.toArgb()
        val second = b.toArgb()
        return listOf(16, 8, 0).maxOf { shift -> abs(((first shr shift) and 0xFF) - ((second shr shift) and 0xFF)) }
    }

    /** Writes a colour as `#AARRGGBB`, for failure messages. */
    private fun hex(colour: Color): String = "#%08X".format(colour.toArgb())

    /** Opens Settings with the floating bar's Settings tab and waits until it is shown. */
    private fun openSettings() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        openTab(compose, KansoTab.SETTINGS)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isEmpty()
        }
        compose.waitForIdle()
    }

    /** The tolerances and the row lists the tests share. */
    companion object {
        /** How far, per channel, a drawn pixel may stray from the colour it should be. */
        const val TOLERANCE = 2

        /** How far apart, in at least one channel, two colours must be to be told apart. */
        const val CLEAR_DIFFERENCE = 8

        /** The rows Settings shows at SDK 30, each with the tag of the card that holds it. */
        val ROWS_SDK_30 = listOf(
            THEME_MODE_SYSTEM_TAG to SETTINGS_GROUP_APPEARANCE_TAG,
            THEME_MODE_LIGHT_TAG to SETTINGS_GROUP_APPEARANCE_TAG,
            THEME_MODE_DARK_TAG to SETTINGS_GROUP_APPEARANCE_TAG,
            THEME_COLOUR_KANSO_BLUE_TAG to SETTINGS_GROUP_COLOUR_TAG,
            THEME_COLOUR_CUSTOM_TAG to SETTINGS_GROUP_COLOUR_TAG,
        )

        /** The rows Settings shows at SDK 35: those of SDK 30 and the wallpaper row. */
        val ROWS_SDK_35 = listOf(
            THEME_MODE_SYSTEM_TAG to SETTINGS_GROUP_APPEARANCE_TAG,
            THEME_MODE_LIGHT_TAG to SETTINGS_GROUP_APPEARANCE_TAG,
            THEME_MODE_DARK_TAG to SETTINGS_GROUP_APPEARANCE_TAG,
            THEME_COLOUR_KANSO_BLUE_TAG to SETTINGS_GROUP_COLOUR_TAG,
            THEME_COLOUR_WALLPAPER_TAG to SETTINGS_GROUP_COLOUR_TAG,
            THEME_COLOUR_CUSTOM_TAG to SETTINGS_GROUP_COLOUR_TAG,
        )
    }
}
