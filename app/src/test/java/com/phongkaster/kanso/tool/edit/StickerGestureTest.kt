package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.Sticker
import com.phongkaster.kanso.data.edit.edit.StickerMapping
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.TOOL_UNDO_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.components.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.ui.tool.components.photocard.stickeroverlay.STICKER_OVERLAY_TAG
import com.phongkaster.kanso.ui.tool.components.photocard.stickeroverlay.angleChange
import com.phongkaster.kanso.ui.tool.editProcessorFor
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Proves the touch gestures on a selected sticker (Run 10, DoD 9, Android 15): a one-finger swipe
 * moves it, a two-finger spread scales it, a two-finger twist turns it, each measured on the SHOWN
 * picture while the photo is turned a quarter turn and flipped, and a whole gesture is one undo
 * entry. The real `ToolScreen` is drawn on a fake 600 x 400 photo with the Stickers sheet open.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StickerGestureTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** THE recipe the screen itself uses. */
    private val processorFor by lazy { editProcessorFor(context) }

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * A one-finger swipe by (dx, dy) on the selected sticker moves its shown centre by (dx, dy)
     * within 2 pixels, on a photo turned a quarter turn and flipped, and leaves its size and turn.
     */
    @Test
    fun a_one_finger_swipe_moves_the_sticker_by_the_swipe() {
        val viewModel = showTurnedAndFlipped()
        val centre = shownCentre(viewModel)
        val width = shownWidth(viewModel)
        val degrees = shownDegrees(viewModel)
        val move = Offset(70f, -45f)

        compose.onNodeWithTag(STICKER_OVERLAY_TAG).performTouchInput {
            swipe(centre, centre + move, durationMillis = 300)
        }
        compose.waitForIdle()

        val after = shownCentre(viewModel)
        assertEquals("dx", move.x, after.x - centre.x, 2f)
        assertEquals("dy", move.y, after.y - centre.y, 2f)
        assertEquals("size", width, shownWidth(viewModel), width * 0.01f)
        assertEquals("turn", 0f, angleChange(degrees, shownDegrees(viewModel)), 0.5f)
    }

    /**
     * Two fingers spreading to 1.5 times their distance scale the selected sticker by 1.5 (within
     * 5 percent) about the point between them, on a turned and flipped photo.
     */
    @Test
    fun a_two_finger_spread_scales_the_sticker() {
        val viewModel = showTurnedAndFlipped()
        val centre = shownCentre(viewModel)
        val width = shownWidth(viewModel)
        val degrees = shownDegrees(viewModel)

        pinch(centre, startRadius = 30f, endRadius = 45f, turnDegrees = 0f)

        assertEquals("scale", 1.5f, shownWidth(viewModel) / width, 1.5f * 0.05f)
        assertEquals("turn", 0f, angleChange(degrees, shownDegrees(viewModel)), 2f)
        val after = shownCentre(viewModel)
        assertEquals("centre x", centre.x, after.x, 2f)
        assertEquals("centre y", centre.y, after.y, 2f)
    }

    /**
     * Two fingers twisting 45 degrees clockwise turn the selected sticker 45 degrees on the
     * shown picture (within 2), without changing its size, on a turned and flipped photo.
     */
    @Test
    fun a_two_finger_twist_turns_the_sticker() {
        val viewModel = showTurnedAndFlipped()
        val centre = shownCentre(viewModel)
        val width = shownWidth(viewModel)
        val degrees = shownDegrees(viewModel)

        pinch(centre, startRadius = 40f, endRadius = 40f, turnDegrees = 45f)

        assertEquals("turn", 45f, angleChange(degrees, shownDegrees(viewModel)), 2f)
        assertEquals("size", width, shownWidth(viewModel), width * 0.03f)
        val after = shownCentre(viewModel)
        assertEquals("centre x", centre.x, after.x, 2f)
        assertEquals("centre y", centre.y, after.y, 2f)
    }

    /**
     * A swipe made of many moves is exactly one undo entry: one tap on the screen's Undo gives the
     * settings from before the swipe (the sticker back where it was), and the history below it is
     * the setup's own single step.
     */
    @Test
    fun a_whole_swipe_is_one_undo_entry() {
        val viewModel = showTurnedAndFlipped()
        val before = viewModel.state.value.settings
        val centre = shownCentre(viewModel)

        compose.onNodeWithTag(STICKER_OVERLAY_TAG).performTouchInput {
            swipe(centre, centre + Offset(60f, 60f), durationMillis = 600)
        }
        compose.waitForIdle()
        assertTrue("the swipe changed nothing", viewModel.state.value.settings != before)

        // The swipe was committed on its own: Undo is on before any tap on it.
        assertTrue("the swipe made an undo entry", viewModel.state.value.canUndo)
        compose.onNodeWithTag(TOOL_UNDO_TAG).performClick()
        compose.waitForIdle()
        assertEquals(before, viewModel.state.value.settings)
    }

    /**
     * A sticker dragged far past the photo's edge ends with its centre still on the photo (the
     * centre is clamped to the overlay), so it can be grasped again.
     */
    @Test
    fun a_sticker_dragged_past_the_edge_keeps_its_centre_on_the_photo() {
        val viewModel = showTurnedAndFlipped()
        val centre = shownCentre(viewModel)
        val size = compose.onNodeWithTag(STICKER_OVERLAY_TAG).fetchSemanticsNode().size

        compose.onNodeWithTag(STICKER_OVERLAY_TAG).performTouchInput {
            swipe(centre, centre + Offset(5000f, 5000f), durationMillis = 300)
        }
        compose.waitForIdle()

        val after = shownCentre(viewModel)
        assertTrue("x ${after.x} inside 0..${size.width}", after.x in -0.5f..(size.width + 0.5f))
        assertTrue("y ${after.y} inside 0..${size.height}", after.y in -0.5f..(size.height + 0.5f))
        assertTrue("it did move", after.x > centre.x || after.y > centre.y)
    }

    /**
     * A flip made through the view model while a finger is dragging the sticker is not reverted by
     * the finger's next move: the sticker ends mirrored.
     */
    @Test
    fun a_flip_during_a_drag_is_kept_by_the_next_move() {
        val viewModel = showTurnedAndFlipped()
        val centre = shownCentre(viewModel)
        val mirroredBefore = viewModel.state.value.settings.stickers.first { it.key == 100L }.mirrored

        compose.onNodeWithTag(STICKER_OVERLAY_TAG).performTouchInput {
            down(centre)
            moveTo(centre + Offset(10f, 0f))
        }
        compose.waitForIdle()
        viewModel.flipSticker(100L, processorFor)
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        compose.onNodeWithTag(STICKER_OVERLAY_TAG).performTouchInput {
            moveTo(centre + Offset(20f, 0f))
            up()
        }
        compose.waitForIdle()

        val mirroredAfter = viewModel.state.value.settings.stickers.first { it.key == 100L }.mirrored
        assertEquals("the flip survived the move", !mirroredBefore, mirroredAfter)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Puts two fingers [startRadius] either side of [centre] (finger 0 on the sticker), then moves
     * them in 9 steps to [endRadius] either side while the line between them turns [turnDegrees]
     * clockwise about [centre], and lifts them.
     */
    private fun pinch(centre: Offset, startRadius: Float, endRadius: Float, turnDegrees: Float) {
        fun at(radius: Float, degrees: Float, sign: Float): Offset {
            val r = Math.toRadians(degrees.toDouble())
            return Offset(centre.x + sign * radius * cos(r).toFloat(), centre.y + sign * radius * sin(r).toFloat())
        }
        compose.onNodeWithTag(STICKER_OVERLAY_TAG).performTouchInput {
            down(0, at(startRadius, 0f, 1f))
            down(1, at(startRadius, 0f, -1f))
            val steps = 9
            for (i in 1..steps) {
                val f = i / steps.toFloat()
                val radius = startRadius + (endRadius - startRadius) * f
                updatePointerTo(0, at(radius, turnDegrees * f, 1f))
                updatePointerTo(1, at(radius, turnDegrees * f, -1f))
                move()
            }
            up(0)
            up(1)
        }
        compose.waitForIdle()
    }

    /** The mapping between the shown picture and the original for the ViewModel's settings now. */
    private fun mappingOf(viewModel: ToolViewModel): StickerMapping {
        val state = viewModel.state.value
        val photo = (state.photo as ToolUiState.PhotoState.Ready).bitmap
        return StickerMapping(photo.width, photo.height, state.settings)
    }

    /** The sticker 100's centre on the shown picture, in the overlay's own pixels. */
    private fun shownCentre(viewModel: ToolViewModel): Offset {
        val sticker = viewModel.state.value.settings.stickers.first { it.key == 100L }
        val p = mappingOf(viewModel).originalToShown(StickerMapping.Point(sticker.centreX, sticker.centreY))
        val size = compose.onNodeWithTag(STICKER_OVERLAY_TAG).fetchSemanticsNode().size
        return Offset(p.x * size.width, p.y * size.height)
    }

    /** The sticker 100's width as a fraction of the shown picture's width. */
    private fun shownWidth(viewModel: ToolViewModel): Float =
        mappingOf(viewModel).originalWidthToShown(viewModel.state.value.settings.stickers.first { it.key == 100L }.width)

    /** The sticker 100's turn as the person sees it. */
    private fun shownDegrees(viewModel: ToolViewModel): Float =
        mappingOf(viewModel).originalAngleToShown(viewModel.state.value.settings.stickers.first { it.key == 100L }.degrees)

    /**
     * Draws the screen on a photo turned a quarter turn and flipped sideways, with one sticker
     * (key 100) slightly turned off the middle, the Stickers sheet open and the sticker selected.
     */
    private fun showTurnedAndFlipped(): ToolViewModel {
        val viewModel = ToolViewModel(
            Tool.EDIT,
            PHOTO_URI,
            FakePhotoLoader(context, ToolFixtures.gradient(600, 400)),
            ViewModelClearedProbe(),
            workDispatcher = Dispatchers.Unconfined,
        )
        viewModel.load(MAX_EDGE_PX)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        compose.setContent {
            MaterialTheme {
                ToolScreen(viewModel = viewModel, onBack = {}, onDone = {})
            }
        }
        compose.waitForIdle()
        val sticker = Sticker(100L, "img_sticker_apple", 0.45f, 0.55f, 0.25f, 10f, false)
        viewModel.changeSettings(
            EditSettings(quarterTurns = 1, flipHorizontal = true, stickers = listOf(sticker)),
            processorFor,
        )
        compose.waitForIdle()
        compose.onNodeWithTag(editToolTag(EditTool.STICKERS)).performClick()
        compose.waitForIdle()
        viewModel.selectSticker(100L)
        compose.waitForIdle()
        assertEquals(100L, viewModel.state.value.selectedStickerKey)
        return viewModel
    }

    /** Values shared by the tests. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** The size the screen asks for: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3
    }
}
