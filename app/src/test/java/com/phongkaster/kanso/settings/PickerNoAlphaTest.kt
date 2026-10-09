package com.phongkaster.kanso.settings

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.ui.settings.CUSTOM_COLOUR_PICKER_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_TAG
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * Drives the real app to Settings, chooses Custom colour and checks that the colour picker shows
 * no transparency control: no text that reads like a percentage (such as "100%") anywhere on
 * screen. A theme colour is always fully opaque, so Settings asks the picker for
 * `showAlpha = false`.
 *
 * The engine's transparency bar is a plain Canvas with no semantics (no tag, no text), so it
 * cannot be found by name in the semantics tree. Its absence is proved by size instead, in
 * [PickerDefaultAlphaTest]: the same picker with `showAlpha = false` is at least one bar
 * (8dp) plus one gap (16dp) shorter than the default one.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "notnight")
class PickerNoAlphaTest {

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

    /** With Custom chosen, the picker is shown but no percentage text is anywhere on screen. */
    @Test
    fun custom_colour_picker_shows_no_percentage() {
        waitForHome()
        openSettings()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(CUSTOM_COLOUR_PICKER_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(CUSTOM_COLOUR_PICKER_TAG).performScrollTo()
        compose.waitForIdle()

        // The picker really is on screen: its hex text field is there. Without this check, "no
        // percentage found" could simply mean "no picker at all". (The field is only looked at,
        // never typed into.)
        assertTrue(
            "The colour picker's hex field should be shown",
            compose.onAllNodes(
                hasAnyAncestor(hasTestTag(CUSTOM_COLOUR_PICKER_TAG)) and hasEditableText(),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty(),
        )

        // The unmerged tree lists every single Text, including the invisible "100%" the engine
        // uses only to size its percentage box.
        val percentages = compose.onAllNodes(hasPercentageText(), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString() }
        assertTrue("No percentage text expected, found: $percentages", percentages.isEmpty())
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
}

/** Matches a text field, that is a node carrying editable text. */
private fun hasEditableText(): SemanticsMatcher =
    SemanticsMatcher("is a text field") { node ->
        node.config.getOrNull(SemanticsProperties.EditableText) != null
    }
