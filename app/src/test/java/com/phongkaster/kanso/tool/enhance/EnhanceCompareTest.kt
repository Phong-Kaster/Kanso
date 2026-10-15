package com.phongkaster.kanso.tool.enhance

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.tool.EDITOR_PANEL_TAG
import com.phongkaster.kanso.ui.tool.ENHANCE_COMPARE_ENHANCED_TAG
import com.phongkaster.kanso.ui.tool.ENHANCE_COMPARE_HOLD_TAG
import com.phongkaster.kanso.ui.tool.ENHANCE_COMPARE_ORIGINAL_TAG
import com.phongkaster.kanso.ui.tool.ENHANCE_COMPARE_TAG
import com.phongkaster.kanso.ui.tool.ENHANCE_PANEL_TAG
import com.phongkaster.kanso.ui.tool.ENHANCE_RUN_TAG
import com.phongkaster.kanso.ui.tool.ENHANCE_SHOWN_BITMAP_KEY
import com.phongkaster.kanso.ui.tool.EnhancePanel
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 8, the comparison half: the tool screen's content with the Enhance panel in its options
 * slot shows the before/after comparison, holding the hold-to-compare control shows the original
 * (the very bitmap the screen started from) and letting go shows the enhanced one again. Which
 * bitmap is shown is read from the picture's `ENHANCE_SHOWN_BITMAP_KEY` semantics property, not
 * from pixels.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class EnhanceCompareTest {

    /** Starts an empty activity to host the screen's content. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The photo before Enhance: a dim grey picture. */
    private val original: Bitmap = solid(300, 200, Color.rgb(40, 40, 40))

    /** The photo after Enhance: twice as large and brighter. */
    private val enhanced: Bitmap = solid(600, 400, Color.rgb(128, 128, 128))

    /** Hosts the Enhance screen's content, its photo enhanced, with the panel in the slot. */
    private fun showEnhanceScreen(onRun: () -> Unit = {}) {
        val state = ToolUiState(
            tool = Tool.ENHANCE,
            photoUri = "content://com.phongkaster.kanso.test/enhance.jpg",
            photo = ToolUiState.PhotoState.Ready(original),
            edited = enhanced,
            canUndo = true,
        )
        compose.setContent {
            MaterialTheme {
                ToolContent(
                    state = state,
                    onBack = {},
                    onSave = {},
                    panel = { EnhancePanel(onRun = onRun, original = original, enhanced = enhanced) },
                )
            }
        }
        compose.waitForIdle()
    }

    /** Returns the bitmap the comparison picture tagged [tag] is showing. */
    private fun shownBitmap(tag: String): Bitmap =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[ENHANCE_SHOWN_BITMAP_KEY]

    /** The comparison sits in the options strip and shows the enhanced photo at first. */
    @Test
    fun the_comparison_shows_the_enhanced_photo_at_first() {
        showEnhanceScreen()

        compose.onNode(
            hasTestTag(ENHANCE_COMPARE_TAG) and hasAnyAncestor(hasTestTag(ENHANCE_PANEL_TAG)) and
                hasAnyAncestor(hasTestTag(EDITOR_PANEL_TAG)),
        ).assertExists()
        assertSame(enhanced, shownBitmap(ENHANCE_COMPARE_ENHANCED_TAG))
        compose.onAllNodes(hasTestTag(ENHANCE_COMPARE_ORIGINAL_TAG)).assertCountEquals(0)
    }

    /** Holding the control shows the original; letting go shows the enhanced photo again. */
    @Test
    fun holding_shows_the_original_and_releasing_shows_the_enhanced() {
        showEnhanceScreen()

        compose.onNodeWithTag(ENHANCE_COMPARE_HOLD_TAG).performTouchInput { down(center) }
        compose.waitForIdle()
        assertSame(original, shownBitmap(ENHANCE_COMPARE_ORIGINAL_TAG))
        compose.onAllNodes(hasTestTag(ENHANCE_COMPARE_ENHANCED_TAG)).assertCountEquals(0)

        compose.onNodeWithTag(ENHANCE_COMPARE_HOLD_TAG).performTouchInput { up() }
        compose.waitForIdle()
        assertSame(enhanced, shownBitmap(ENHANCE_COMPARE_ENHANCED_TAG))
        compose.onAllNodes(hasTestTag(ENHANCE_COMPARE_ORIGINAL_TAG)).assertCountEquals(0)
    }

    /** The Enhance button calls the panel's run callback once per tap. */
    @Test
    fun the_enhance_button_runs_enhance() {
        var runs = 0
        showEnhanceScreen(onRun = { runs++ })

        compose.onNodeWithTag(ENHANCE_RUN_TAG).performClick()
        compose.waitForIdle()

        assertEquals(1, runs)
    }

    /** Makes a [width] x [height] picture of one [color]. */
    private fun solid(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }
}
