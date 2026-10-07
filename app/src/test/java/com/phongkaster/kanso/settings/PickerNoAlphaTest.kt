package com.phongkaster.kanso.settings

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.library.colorpicker.HSVColorPicker
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

/**
 * Hosts the engine's HSVColorPicker directly, twice: once as every other caller uses it (the
 * default, `showAlpha = true`) and once with `showAlpha = false`. The default still shows its
 * percentage, so the photo editor's colour sheet in core/common behaves as before; the other
 * shows none and is shorter by at least the transparency bar and its gap.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class PickerDefaultAlphaTest {

    /** Hosts the composables under test in an empty activity. */
    @get:Rule
    val compose = createComposeRule()

    /** Stops Koin, which the app's Application class starts, so it does not leak into other tests. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** The default picker shows a percentage (and "100%" for an opaque colour); the other none. */
    @Test
    fun default_picker_keeps_its_percentage_and_the_other_has_none() {
        showBothPickers()

        assertTrue(
            "The default picker should still show its percentage",
            compose.onAllNodes(
                hasAnyAncestor(hasTestTag(WITH_ALPHA_TAG)) and hasPercentageText(),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty(),
        )
        assertTrue(
            "The default picker should show 100% for an opaque white",
            compose.onAllNodes(
                hasAnyAncestor(hasTestTag(WITH_ALPHA_TAG)) and hasExactText("100%"),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty(),
        )
        assertTrue(
            "The picker without alpha should show no percentage",
            compose.onAllNodes(
                hasAnyAncestor(hasTestTag(WITHOUT_ALPHA_TAG)) and hasPercentageText(),
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty(),
        )
    }

    /**
     * The transparency bar draws on a Canvas with no semantics, so its absence is measured: the
     * picker without alpha is shorter by at least the bar's 8dp plus the column's 16dp gap.
     */
    @Test
    fun picker_without_alpha_has_no_transparency_bar() {
        showBothPickers()

        val withAlpha = compose.onNodeWithTag(WITH_ALPHA_TAG).fetchSemanticsNode().size.height
        val withoutAlpha = compose.onNodeWithTag(WITHOUT_ALPHA_TAG).fetchSemanticsNode().size.height
        // Half a pixel of slack absorbs rounding from dp to whole pixels.
        val barAndGapPx = with(compose.density) { (8.dp + 16.dp).toPx() } - 0.5f
        assertTrue(
            "Expected the picker without alpha to be at least ${barAndGapPx}px shorter " +
                "(with alpha: $withAlpha px, without: $withoutAlpha px)",
            withAlpha - withoutAlpha >= barAndGapPx,
        )
    }

    /** Shows one default picker and one with `showAlpha = false`, each at the same width. */
    private fun showBothPickers() {
        compose.setContent {
            Column {
                Box(modifier = Modifier.width(PICKER_WIDTH).testTag(WITH_ALPHA_TAG)) {
                    HSVColorPicker(onColorChanged = {})
                }
                Box(modifier = Modifier.width(PICKER_WIDTH).testTag(WITHOUT_ALPHA_TAG)) {
                    HSVColorPicker(onColorChanged = {}, showAlpha = false)
                }
            }
        }
        compose.waitForIdle()
    }

    /** Tags and size the two hosted pickers use. */
    private companion object {
        /** Tag on the box around the default picker. */
        const val WITH_ALPHA_TAG = "picker_with_alpha"

        /** Tag on the box around the picker with `showAlpha = false`. */
        const val WITHOUT_ALPHA_TAG = "picker_without_alpha"

        /** Both pickers get this width, so only the alpha parts can make their heights differ. */
        val PICKER_WIDTH = 360.dp
    }
}

/** Any number followed by a percent sign, such as "100%" or "42%". */
private val PERCENTAGE = Regex("""\d+%""")

/** Matches a node whose own text reads like a percentage or contains "100%". */
private fun hasPercentageText(): SemanticsMatcher =
    SemanticsMatcher("has text with a percentage") { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty()
            .any { PERCENTAGE.containsMatchIn(it.text) || it.text.contains("100%") }
    }

/** Matches a text field, that is a node carrying editable text. */
private fun hasEditableText(): SemanticsMatcher =
    SemanticsMatcher("is a text field") { node ->
        node.config.getOrNull(SemanticsProperties.EditableText) != null
    }

/** Matches a node whose own text is exactly [text]. */
private fun hasExactText(text: String): SemanticsMatcher =
    SemanticsMatcher("has exact text '$text'") { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text == text }
    }
