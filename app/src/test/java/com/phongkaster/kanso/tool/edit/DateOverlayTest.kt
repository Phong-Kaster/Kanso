package com.phongkaster.kanso.tool.edit

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.DateFormat
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.LayerMapping
import com.phongkaster.kanso.data.edit.edit.TextFont
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_DELETE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_DUPLICATE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_EDIT_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_FLIP_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_OVERLAY_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_RESIZE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LayerOverlay
import java.time.LocalDateTime
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
 * Proves the date's touches on the photo (ADR-006, Android 15): a tap on the date that is already
 * selected is reported as a tap (the screen then moves it to its next format) and makes no
 * gesture step; a tap that selects it, or a drag, is no such tap; and the selected date's frame
 * has delete, duplicate and resize but no flip and no edit button. The real [LayerOverlay] is drawn
 * on a 300 dp square with one date in its middle.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class DateOverlayTest {

    /** Starts an empty activity to host the overlay. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** A tap on the selected date is one tap, and no move, commit or selection. */
    @Test
    fun a_tap_on_the_selected_date_is_reported_as_a_tap() {
        val events = showDate(selected = true)
        compose.onNodeWithTag(LAYER_OVERLAY_TAG).performTouchInput { click(center) }
        compose.waitForIdle()

        assertEquals(listOf(DATE_KEY), events.taps)
        assertTrue("a tap selected again", events.selects.isEmpty())
        assertEquals("a tap committed a gesture", 0, events.commits)
    }

    /** A tap on a date that is not selected selects it and is no tap of the selected date. */
    @Test
    fun a_tap_that_selects_the_date_is_not_a_tap_on_it() {
        val events = showDate(selected = false)
        compose.onNodeWithTag(LAYER_OVERLAY_TAG).performTouchInput { click(center) }
        compose.waitForIdle()

        assertEquals(listOf<Long?>(DATE_KEY), events.selects)
        assertTrue("selecting counted as a tap", events.taps.isEmpty())
    }

    /** A drag of the selected date moves it and is no tap. */
    @Test
    fun a_drag_of_the_selected_date_is_not_a_tap() {
        val events = showDate(selected = true)
        compose.onNodeWithTag(LAYER_OVERLAY_TAG).performTouchInput {
            swipe(center, center + Offset(80f, 40f), durationMillis = 300)
        }
        compose.waitForIdle()

        assertTrue("the drag moved nothing", events.previews > 0)
        assertEquals(1, events.commits)
        assertTrue("a drag counted as a tap", events.taps.isEmpty())
    }

    /** The selected date's frame has delete, duplicate and resize, and no flip or edit button. */
    @Test
    fun the_date_frame_has_no_flip_button() {
        showDate(selected = true)
        compose.onAllNodesWithTag(LAYER_DELETE_TAG).assertCountEquals(1)
        compose.onAllNodesWithTag(LAYER_DUPLICATE_TAG).assertCountEquals(1)
        compose.onAllNodesWithTag(LAYER_RESIZE_TAG).assertCountEquals(1)
        compose.onAllNodesWithTag(LAYER_FLIP_TAG).assertCountEquals(0)
        compose.onAllNodesWithTag(LAYER_EDIT_TAG).assertCountEquals(0)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** What the overlay reported, gathered by the test's callbacks. */
    private class Events {
        /** Each key [LayerOverlay]'s `onSelect` was called with. */
        val selects = mutableListOf<Long?>()

        /** Each key its `onTap` was called with. */
        val taps = mutableListOf<Long>()

        /** How many times `onPreview` was called. */
        var previews = 0

        /** How many times `onCommit` was called. */
        var commits = 0
    }

    /**
     * Draws the overlay on a 300 dp square with one date ("2026", a third of the square wide) in
     * its middle, selected or not, and returns the record its callbacks fill.
     */
    private fun showDate(selected: Boolean): Events {
        val events = Events()
        val date = Layer(
            key = DATE_KEY,
            content = LayerContent.Date(
                moment = LocalDateTime.of(2026, 10, 28, 14, 30),
                format = DateFormat.YEAR,
                colorArgb = 0xFFFFFFFF.toInt(),
                font = TextFont.NONE,
            ),
            centreX = 0.5f,
            centreY = 0.5f,
            width = 0.33f,
            degrees = 0f,
            mirrored = false,
        )
        compose.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier.size(300.dp),
                    content = {
                        LayerOverlay(
                            layers = listOf(date),
                            selectedKey = if (selected) DATE_KEY else null,
                            mapping = LayerMapping(1000, 1000, EditSettings()),
                            enabled = true,
                            onSelect = { events.selects += it },
                            onPreview = { events.previews++ },
                            onCommit = { events.commits++ },
                            onDelete = {},
                            onDuplicate = {},
                            onFlip = {},
                            onTap = { events.taps += it },
                        )
                    },
                )
            }
        }
        compose.waitForIdle()
        return events
    }

    /** Values shared by the tests. */
    private companion object {
        /** The key of the one date. */
        const val DATE_KEY = 7L
    }
}
