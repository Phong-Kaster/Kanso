package com.phongkaster.kanso.settings

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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.library.colorpicker.HSVColorPicker
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

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

/** Matches a node whose own text is exactly [text]. */
private fun hasExactText(text: String): SemanticsMatcher =
    SemanticsMatcher("has exact text '$text'") { node ->
        node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text == text }
    }
