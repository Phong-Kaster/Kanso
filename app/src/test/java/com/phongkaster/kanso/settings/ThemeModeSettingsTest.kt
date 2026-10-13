package com.phongkaster.kanso.settings

import android.content.Context
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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.openTab
import com.phongkaster.kanso.navigation.tapAboveBar
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_GROUP_APPEARANCE_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_DARK_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_LIGHT_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_SYSTEM_TAG
import com.phongkaster.kanso.ui.settings.settingsRowIconTag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Drives the real app through Settings -> Appearance -> Theme mode and reads the result from the
 * drawn pixels: picking a mode re-themes Settings and Home at once, and System follows the
 * phone's night setting. Surviving a restart is checked in ThemeModeRestartTest.
 *
 * Every check looks at two things in one drawing: the background, and the text of a known label
 * (the "Dark" row on Settings, the title "Kanso" on Home), which must have the opposite
 * brightness. So a screen that only painted its own background would not pass.
 *
 * Settings is opened with the floating tab bar's Settings tab (by its tag, `TabTaps.openTab`), and
 * rows are tapped with `TabTaps.tapAboveBar`, because a row scrolled into view on this 320 x 470 dp
 * screen can rest under the bar.
 *
 * One more test reads the semantics tree instead of pixels: the theme mode rows sit together in
 * their grouped container, each with an icon and its label.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ThemeModeSettingsTest {

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
     * Picking Dark turns Settings dark at once, Home is dark after Back, and picking Light turns
     * both light again, all without a restart.
     */
    @Test
    @Config(qualifiers = "notnight")
    fun picking_dark_then_light_rethemes_settings_and_home_at_once() {
        waitForHome()
        assertDrawnLight(sampleHome())

        openSettings()
        assertDrawnLight(sampleSettings())
        compose.onNodeWithTag(THEME_MODE_SYSTEM_TAG).assertIsSelected()
        pick(THEME_MODE_DARK_TAG)
        waitUntilDark()
        assertDrawnDark(sampleSettings())
        compose.onNodeWithTag(THEME_MODE_DARK_TAG).assertIsSelected()
        compose.onNodeWithTag(THEME_MODE_SYSTEM_TAG).assertIsNotSelected()

        Espresso.pressBack()
        waitForHomeOnly()
        assertDrawnDark(sampleHome())

        openSettings()
        pick(THEME_MODE_LIGHT_TAG)
        waitUntilLight()
        assertDrawnLight(sampleSettings())
        compose.onNodeWithTag(THEME_MODE_LIGHT_TAG).assertIsSelected()

        Espresso.pressBack()
        waitForHomeOnly()
        assertDrawnLight(sampleHome())
    }

    /** On a phone in night mode, System (the default) is dark, Light overrides it, System returns. */
    @Test
    @Config(qualifiers = "night")
    fun system_mode_follows_a_night_phone() {
        waitForHome()
        assertDrawnDark(sampleHome())

        openSettings()
        assertDrawnDark(sampleSettings())
        compose.onNodeWithTag(THEME_MODE_SYSTEM_TAG).assertIsSelected()
        pick(THEME_MODE_LIGHT_TAG)
        waitUntilLight()
        assertDrawnLight(sampleSettings())
        pick(THEME_MODE_SYSTEM_TAG)
        waitUntilDark()
        assertDrawnDark(sampleSettings())

        Espresso.pressBack()
        waitForHomeOnly()
        assertDrawnDark(sampleHome())
    }

    /** On a phone in day mode, System (the default) is light, Dark overrides it, System returns. */
    @Test
    @Config(qualifiers = "notnight")
    fun system_mode_follows_a_day_phone() {
        waitForHome()
        assertDrawnLight(sampleHome())

        openSettings()
        assertDrawnLight(sampleSettings())
        pick(THEME_MODE_DARK_TAG)
        waitUntilDark()
        assertDrawnDark(sampleSettings())
        pick(THEME_MODE_SYSTEM_TAG)
        waitUntilLight()
        assertDrawnLight(sampleSettings())

        Espresso.pressBack()
        waitForHomeOnly()
        assertDrawnLight(sampleHome())
    }

    /**
     * DoD 19: the three theme mode rows sit inside the grouped container tagged
     * `settings-group-appearance`, which holds no other choice; each is still a radio-button row
     * and holds its own icon (decorative, or an image with a description) and its label.
     */
    @Test
    @Config(qualifiers = "notnight")
    fun theme_mode_rows_sit_in_the_appearance_group_with_an_icon_and_a_label() {
        waitForHome()
        openSettings()
        assertGroupHolds(
            SETTINGS_GROUP_APPEARANCE_TAG,
            THEME_MODE_SYSTEM_TAG to R.string.theme_mode_system,
            THEME_MODE_LIGHT_TAG to R.string.theme_mode_light,
            THEME_MODE_DARK_TAG to R.string.theme_mode_dark,
        )
        compose.onNodeWithTag(THEME_MODE_SYSTEM_TAG).assertIsSelected()
        compose.onNodeWithTag(THEME_MODE_LIGHT_TAG).assertIsNotSelected()
        compose.onNodeWithTag(THEME_MODE_DARK_TAG).assertIsNotSelected()
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

    /**
     * Draws Home and samples its background and the text of its title, "Kanso": plain text on
     * the plain background. (The "Settings" tab's label is no sample: it sits on the bar's inverse
     * colour, so its brightness is the opposite of the screen's text.) The title is first scrolled
     * into view, since only what is on screen is drawn.
     */
    private fun sampleHome(): ScreenSample {
        val title = hasText(context.getString(R.string.app_name)) and hasAnyAncestor(hasTestTag(HOME_SCREEN_TAG))
        compose.onNode(title).performScrollTo()
        return sampleAround(title)
    }

    /** Draws Settings and samples its background and the text of its "Dark" row. */
    private fun sampleSettings(): ScreenSample = sampleAround(
        hasText(context.getString(R.string.theme_mode_dark)) and
            hasAnyAncestor(hasTestTag(SETTINGS_SCREEN_TAG)),
    )

    /**
     * Lets Compose settle, finds the one text node matching [text] and samples the drawn screen
     * around it. The unmerged tree is searched so the bounds are the text's own, not its row's.
     */
    private fun sampleAround(text: SemanticsMatcher): ScreenSample {
        compose.waitForIdle()
        val bounds = compose.onNode(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        return compose.activity.sampleScreen(bounds)
    }

    /** Taps the theme mode row tagged [tag], above the floating bar. */
    private fun pick(tag: String) {
        compose.onNodeWithTag(tag).tapAboveBar()
    }

    /** Waits until the screen's background is drawn dark, which needs the DataStore write. */
    private fun waitUntilDark() {
        compose.waitUntil(timeoutMillis = 10_000) {
            isDark(compose.activity.drawnColorNearBottomLeft())
        }
    }

    /** Waits until the screen's background is drawn light, which needs the DataStore write. */
    private fun waitUntilLight() {
        compose.waitUntil(timeoutMillis = 10_000) {
            isLight(compose.activity.drawnColorNearBottomLeft())
        }
    }

    /** Taps the Settings tab on the floating bar and waits until the Settings screen is shown. */
    private fun openSettings() {
        openTab(compose, KansoTab.SETTINGS)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(context.getString(R.string.theme_mode)).assertIsDisplayed()
    }

    /** Waits until Home is on screen, found by its own tag. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home is shown and the Settings screen has left the screen. */
    private fun waitForHomeOnly() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isEmpty()
        }
    }
}
