package com.phongkaster.kanso.settings

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.ui.settings.CUSTOM_COLOUR_PICKER_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_GROUP_COLOUR_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.SettingsViewModel
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_KANSO_BLUE_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_WALLPAPER_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_DARK_TAG
import com.phongkaster.kanso.ui.settings.settingsRowIconTag
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
import com.phongkaster.kanso.ui.theme.seedColorScheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.robolectric.annotation.Config

/**
 * Drives the real app through Settings -> Appearance -> Theme colour: a fresh install is in
 * Kanso blue with the Kanso blue row first and selected, the wallpaper option is offered only on
 * Android 12 and later (between Kanso blue and Custom colour), choosing it sets the colour source
 * to Wallpaper, choosing Kanso blue afterwards brings Kanso blue back, the Custom option shows
 * the colour picker, and a custom colour set through the picker's callback re-themes Settings
 * and Home at once, in light and in dark. Surviving a restart is checked in
 * ThemeColourRestartTest, and the colour swatch on each row in ThemeSwatchTest. One test reads how
 * the rows are grouped: all of them inside the colour group, each with an icon and its label.
 *
 * The colours are read from the scheme KansoTheme publishes on its semantics node, which is
 * exactly what `MaterialTheme.colorScheme` returns on every screen below it.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "notnight")
class ThemeColourSettingsTest {

    /** Launches MainActivity before each test and lets the test find and tap its Compose nodes. */
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
     * On Android 11 the colour section has no wallpaper option: two rows, Kanso blue then Custom
     * colour, and Kanso blue is in use and selected.
     */
    @Test
    @Config(sdk = [30])
    fun android_11_offers_no_wallpaper_option() {
        waitForHome()
        assertEquals(seedColorScheme(KANSO_BLUE_SEED, dark = false).primary, appliedScheme().primary)

        openSettings()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).performScrollTo().assertIsDisplayed()
        assertTrue(
            "The wallpaper option must not be offered below Android 12",
            compose.onAllNodesWithTag(THEME_COLOUR_WALLPAPER_TAG).fetchSemanticsNodes().isEmpty(),
        )
        assertRowsInOrder(THEME_COLOUR_KANSO_BLUE_TAG, THEME_COLOUR_CUSTOM_TAG)
        compose.onNodeWithTag(THEME_COLOUR_KANSO_BLUE_TAG).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).assertIsNotSelected()
    }

    /**
     * On Android 15 a fresh install is in Kanso blue: the colour rows read Kanso blue, From
     * wallpaper, Custom colour, top to bottom, with Kanso blue selected. Choosing Custom shows
     * the picker, choosing the wallpaper then sets the source to Wallpaper and applies the
     * system's dynamic scheme, and choosing Kanso blue last saves Kanso blue and re-themes the
     * app in it again, so each of the three rows is tapped once.
     */
    @Test
    fun android_15_offers_the_wallpaper_option_and_choosing_it_sets_wallpaper() {
        waitForHome()
        assertEquals(seedColorScheme(KANSO_BLUE_SEED, dark = false).primary, appliedScheme().primary)
        openSettings()
        assertRowsInOrder(
            THEME_COLOUR_KANSO_BLUE_TAG,
            THEME_COLOUR_WALLPAPER_TAG,
            THEME_COLOUR_CUSTOM_TAG,
        )
        compose.onNodeWithTag(THEME_COLOUR_KANSO_BLUE_TAG).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).assertIsNotSelected()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).assertIsNotSelected()

        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).performScrollTo().performClick()
        waitForPicker()
        compose.onNodeWithTag(CUSTOM_COLOUR_PICKER_TAG).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).assertIsSelected()

        compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).performScrollTo().performClick()
        awaitSavedThemeColour(ThemeColour.Wallpaper)
        compose.waitUntil(timeoutMillis = 10_000) {
            appliedScheme().primary == dynamicLightColorScheme(context).primary
        }
        compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).assertIsSelected()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).assertIsNotSelected()
        compose.onNodeWithTag(THEME_COLOUR_KANSO_BLUE_TAG).assertIsNotSelected()
        assertTrue(
            "The picker is shown only while Custom is selected",
            compose.onAllNodesWithTag(CUSTOM_COLOUR_PICKER_TAG).fetchSemanticsNodes().isEmpty(),
        )

        // Back to Kanso blue: the choice is saved, and the app is drawn in Kanso blue again.
        compose.onNodeWithTag(THEME_COLOUR_KANSO_BLUE_TAG).performScrollTo().performClick()
        awaitSavedThemeColour(ThemeColour.KansoBlue)
        compose.waitUntil(timeoutMillis = 10_000) {
            appliedScheme().primary == seedColorScheme(KANSO_BLUE_SEED, dark = false).primary
        }
        compose.onNodeWithTag(THEME_COLOUR_KANSO_BLUE_TAG).assertIsSelected()
        compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).assertIsNotSelected()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).assertIsNotSelected()
    }

    /**
     * DoD 19: on Android 15 the three colour rows sit inside the grouped container tagged
     * `settings-group-colour`, which holds no other choice; each is still a radio-button row and
     * holds its own icon (decorative, or an image with a description) and its label. The
     * selection of a fresh install is unchanged: Kanso blue on, the other two off.
     */
    @Test
    fun colour_rows_sit_in_the_colour_group_with_an_icon_and_a_label() {
        waitForHome()
        openSettings()
        assertGroupHolds(
            SETTINGS_GROUP_COLOUR_TAG,
            THEME_COLOUR_KANSO_BLUE_TAG to R.string.theme_colour_kanso_blue,
            THEME_COLOUR_WALLPAPER_TAG to R.string.theme_colour_wallpaper,
            THEME_COLOUR_CUSTOM_TAG to R.string.theme_colour_custom,
        )
        compose.onNodeWithTag(THEME_COLOUR_KANSO_BLUE_TAG).assertIsSelected()
        compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).assertIsNotSelected()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).assertIsNotSelected()
    }

    /**
     * In light mode, the custom colour set through the picker's callback gives Settings the
     * generated light scheme at once, and Home has it after Back, with no restart.
     */
    @Test
    fun custom_colour_applies_at_once_in_light_mode() {
        val expected = seedColorScheme(GREEN, dark = false).primary
        waitForHome()
        openSettings()

        pickCustomColour()
        compose.waitUntil(timeoutMillis = 10_000) { appliedScheme().primary == expected }
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(CUSTOM_COLOUR_PICKER_TAG).assertExists()

        Espresso.pressBack()
        waitForHomeOnly()
        assertEquals(expected, appliedScheme().primary)
    }

    /**
     * In dark mode, the custom colour set through the picker's callback gives Settings the
     * generated dark scheme at once, and Home has it after Back, with no restart.
     */
    @Test
    fun custom_colour_applies_at_once_in_dark_mode() {
        val expected = seedColorScheme(GREEN, dark = true).primary
        waitForHome()
        openSettings()
        compose.onNodeWithTag(THEME_MODE_DARK_TAG).performScrollTo().performClick()

        pickCustomColour()
        compose.waitUntil(timeoutMillis = 10_000) { appliedScheme().primary == expected }

        Espresso.pressBack()
        waitForHomeOnly()
        assertEquals(expected, appliedScheme().primary)
        assertEquals(seedColorScheme(GREEN, dark = true).background, appliedScheme().background)
    }

    /**
     * Sets the custom colour #2E7D32 through SettingsViewModel's onCustomColourChosen, the same
     * function the picker's onColorChanged calls (the picker itself is not dragged).
     */
    private fun pickCustomColour() {
        SettingsViewModel(repository()).onCustomColourChosen(GREEN)
    }

    /**
     * Fails unless each of [tags] names exactly one selectable colour row, and the rows lie
     * strictly one below the other in the order given (their tops in the root's coordinates).
     *
     * The tops are read with `positionInRoot`, where the row is laid out, and not with
     * `boundsInRoot`: Compose clips `boundsInRoot` to the part of the screen that is visible, so
     * a row scrolled out of view reports a top of 0 and the order would look wrong.
     */
    private fun assertRowsInOrder(vararg tags: String) {
        val tops = tags.map { tag ->
            val row = compose.onNodeWithTag(tag).fetchSemanticsNode()
            // A selectable row (a radio-button row) carries the Selected property, on or off.
            assertTrue("Row $tag must be selectable", SemanticsProperties.Selected in row.config)
            // Unclipped: the row's real place in the scrolling list, even when off screen.
            tag to row.positionInRoot.y
        }
        tops.zipWithNext().forEach { (upper, lower) ->
            assertTrue(
                "Row ${upper.first} (top ${upper.second}) must lie above ${lower.first} " +
                    "(top ${lower.second}); order found: $tops",
                upper.second < lower.second,
            )
        }
        val colourTags = listOf(
            THEME_COLOUR_KANSO_BLUE_TAG,
            THEME_COLOUR_WALLPAPER_TAG,
            THEME_COLOUR_CUSTOM_TAG,
        )
        val shown = colourTags.sumOf { compose.onAllNodesWithTag(it).fetchSemanticsNodes().size }
        assertEquals("Expected exactly the colour rows ${tags.toList()}", tags.size, shown)
    }

    /**
     * Fails unless the node tagged [groupTag] holds exactly the selectable rows of [rows] (each a
     * row tag and the string resource of its label), each one a radio button holding its icon
     * node (tagged with `settingsRowIconTag`) and its label text.
     *
     * The icon and the label are looked up in the unmerged tree: a row merges its children into
     * itself for screen readers, so only the unmerged tree still shows them as nodes of their own.
     */
    private fun assertGroupHolds(groupTag: String, vararg rows: Pair<String, Int>) {
        compose.onNodeWithTag(groupTag).assertExists()
        rows.forEach { (rowTag, labelRes) ->
            compose.onNode(hasTestTag(rowTag) and hasAnyAncestor(hasTestTag(groupTag)))
                .assertExists()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            compose.onNode(
                hasTestTag(settingsRowIconTag(rowTag)) and hasAnyAncestor(hasTestTag(rowTag)),
                useUnmergedTree = true,
            ).assertExists().assert(isImageOrDecorative())
            compose.onNode(
                hasText(context.getString(labelRes)) and hasAnyAncestor(hasTestTag(rowTag)),
                useUnmergedTree = true,
            ).assertExists()
        }
        val held = compose.onAllNodes(isSelectable() and hasAnyAncestor(hasTestTag(groupTag)))
            .fetchSemanticsNodes()
            .size
        assertEquals("The group $groupTag must hold exactly its ${rows.size} rows", rows.size, held)
    }

    /**
     * Matches an icon that is either an image with a spoken description, or decorative: it has no
     * content description at all, so screen readers skip it and read the row's label instead.
     */
    private fun isImageOrDecorative(): SemanticsMatcher =
        SemanticsMatcher("an image with a content description, or decorative") { node ->
            val description = node.config.getOrNull(SemanticsProperties.ContentDescription)
            val isImage = node.config.getOrNull(SemanticsProperties.Role) == Role.Image
            description == null || (isImage && description.any { it.isNotBlank() })
        }

    /** Returns the colour scheme KansoTheme currently hands to every screen. */
    private fun appliedScheme(): ColorScheme =
        compose.onNodeWithTag(KANSO_THEME_TAG).fetchSemanticsNode().config[KansoColorSchemeKey]

    /** Reads the theme colour saved in the settings file right now. */
    private fun savedThemeColour(): ThemeColour = runBlocking { repository().themeColour.first() }

    /**
     * Waits, in real time, until the saved theme colour is [expected]. The write runs on a
     * background thread, which Compose's test clock does not wait for.
     */
    private fun awaitSavedThemeColour(expected: ThemeColour) {
        val deadline = System.currentTimeMillis() + 10_000
        while (savedThemeColour() != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            compose.waitForIdle()
        }
        assertEquals(expected, savedThemeColour())
    }

    /** Returns the app's one settings repository from Koin. */
    private fun repository(): SettingsRepository = KoinPlatform.getKoin().get()

    /** Waits until the colour picker is on screen. */
    private fun waitForPicker() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(CUSTOM_COLOUR_PICKER_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Taps the Settings row on Home and waits until the Settings screen is shown. */
    private fun openSettings() {
        compose.onNodeWithText(context.getString(R.string.settings))
            .performScrollTo()
            .performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home's title is on screen. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home is shown and the Settings screen has left the screen. */
    private fun waitForHomeOnly() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isEmpty()
        }
    }

    /** Colours the tests use. */
    private companion object {
        /** The custom colour of the DoD: green #2E7D32. */
        val GREEN = Color(0xFF2E7D32)
    }
}
