package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.a11y.TouchTargetCheck
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.Sticker
import com.phongkaster.kanso.data.edit.edit.StickerMapping
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.STICKER_DELETE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.STICKER_DUPLICATE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.STICKER_FLIP_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.STICKER_OVERLAY_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.STICKER_RESIZE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.angleBetween
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.angleChange
import com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay.distanceBetween
import com.phongkaster.kanso.ui.tool.model.editProcessorFor
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Proves the selected sticker's frame on the real Edit screen (Run 10, Android 15): the delete,
 * duplicate and flip corner buttons (DoD 5, 6, 7), the resize handle's drag (DoD 8) and the
 * buttons' spoken names and 48 dp touch targets (DoD 22). Every test draws the real
 * `ToolScreen` for a real [ToolViewModel] on a fake 600 x 400 photo with the Stickers sheet open.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StickerFrameTest {

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
     * DoD 5: from two stickers with the second selected, a tap on the delete corner leaves only
     * the first sticker, and nothing is selected.
     */
    @Test
    fun delete_removes_the_selected_sticker_and_selects_none() {
        val viewModel = showWith(EditSettings(stickers = listOf(sticker(100L, 0.3f), sticker(101L, 0.7f))), select = 101L)

        compose.onNodeWithTag(STICKER_DELETE_TAG).performClick()
        compose.waitForIdle()

        val state = viewModel.state.value
        assertEquals(listOf(100L), state.settings.stickers.map { it.key })
        assertNull("a deleted sticker stayed selected", state.selectedStickerKey)
    }

    /**
     * DoD 6: from one selected sticker turned 30 degrees and mirrored, on a photo turned a quarter
     * turn, a tap on the duplicate corner gives two stickers. The copy has the same picture, size,
     * turn and mirror; on the SHOWN picture its centre is right of and below the original's by more
     * than 0 and at most 10 percent of the shown width on each axis; the copy is the newest and is
     * selected.
     */
    @Test
    fun duplicate_copies_the_sticker_a_little_right_and_down_on_the_shown_picture() {
        val original = sticker(100L, 0.5f, degrees = 30f, mirrored = true)
        val settings = EditSettings(quarterTurns = 1, stickers = listOf(original))
        val viewModel = showWith(settings, select = 100L)

        compose.onNodeWithTag(STICKER_DUPLICATE_TAG).performClick()
        compose.waitForIdle()

        val state = viewModel.state.value
        assertEquals(2, state.settings.stickers.size)
        val first = state.settings.stickers.first()
        val copy = state.settings.stickers.last()
        assertEquals(original, first)
        assertTrue("the copy reuses the key", copy.key != original.key)
        assertEquals(original.pictureId, copy.pictureId)
        assertEquals(original.width, copy.width, 1e-4f)
        assertEquals(original.degrees, copy.degrees, 1e-3f)
        assertEquals(original.mirrored, copy.mirrored)
        assertEquals("the copy is the selected one", copy.key, state.selectedStickerKey)

        val photo = (state.photo as ToolUiState.PhotoState.Ready).bitmap
        val mapping = StickerMapping(photo.width, photo.height, state.settings)
        val a = mapping.originalToShown(StickerMapping.Point(first.centreX, first.centreY))
        val b = mapping.originalToShown(StickerMapping.Point(copy.centreX, copy.centreY))
        // The quarter turn swaps the photo's sides: the shown picture is 400 x 600 pixels.
        val shownWidthPx = 400f
        val shownHeightPx = 600f
        val dx = (b.x - a.x) * shownWidthPx
        val dy = (b.y - a.y) * shownHeightPx
        assertTrue("the copy is not right of the original ($dx px)", dx > 0f && dx <= 0.1f * shownWidthPx)
        assertTrue("the copy is not below the original ($dy px)", dy > 0f && dy <= 0.1f * shownWidthPx)
    }

    /**
     * DoD 7: from one selected unmirrored sticker one tap on flip mirrors it, and a second tap
     * gives settings equal to the starting settings.
     */
    @Test
    fun flip_twice_gives_back_the_starting_settings() {
        val start = EditSettings(stickers = listOf(sticker(100L, 0.5f)))
        val viewModel = showWith(start, select = 100L)

        compose.onNodeWithTag(STICKER_FLIP_TAG).performClick()
        compose.waitForIdle()
        assertTrue("the first tap did not mirror", viewModel.state.value.settings.stickers.single().mirrored)

        compose.onNodeWithTag(STICKER_FLIP_TAG).performClick()
        compose.waitForIdle()
        assertEquals(start, viewModel.state.value.settings)
    }

    /**
     * DoD 8: dragging the resize handle of a selected sticker from distance d at angle a to
     * distance 2d at angle a + 90 degrees doubles its shown width (within 5 percent) and turns it
     * 90 degrees (within 2), keeps its centre and its picture's shape, and makes exactly one undo
     * entry. The photo is not turned.
     */
    @Test
    fun dragging_the_handle_doubles_and_turns_the_sticker() {
        resizeAndTurn(EditSettings(stickers = listOf(sticker(100L, 0.5f, width = 0.2f))))
    }

    /** The same drag as above on a photo turned a quarter turn, where shown and original differ. */
    @Test
    fun dragging_the_handle_works_on_a_turned_photo() {
        resizeAndTurn(EditSettings(quarterTurns = 1, stickers = listOf(sticker(100L, 0.5f, width = 0.2f))))
    }

    /**
     * DoD 22: each of the four corner buttons has the spoken name from the run's strings and a
     * touch target of at least 48 x 48 dp (measured as the accessibility walk does).
     */
    @Test
    fun the_four_corner_buttons_have_names_and_48_dp_targets() {
        showWith(EditSettings(stickers = listOf(sticker(100L, 0.5f))), select = 100L)
        val density = Density(context)
        val expected = mapOf(
            STICKER_DELETE_TAG to context.getString(R.string.sticker_frame_delete),
            STICKER_DUPLICATE_TAG to context.getString(R.string.sticker_frame_duplicate),
            STICKER_FLIP_TAG to context.getString(R.string.sticker_frame_flip),
            STICKER_RESIZE_TAG to context.getString(R.string.sticker_frame_resize),
        )
        val minPx = with(density) { TouchTargetCheck.MIN_SIZE.toPx() }
        expected.forEach { (tag, description) ->
            assertTrue("the description of $tag is blank", description.isNotBlank())
            compose.onNodeWithTag(tag).assertContentDescriptionEquals(description)
            val (width, height) = TouchTargetCheck.measuredSizePx(compose.onNodeWithTag(tag).fetchSemanticsNode())
            assertTrue("$tag is $width x $height px, under 48 dp ($minPx px)", width + 0.5f >= minPx && height + 0.5f >= minPx)
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Shows [settings], drags the handle of the sticker 100 from distance d at angle a to 2d at a
     * plus 90 degrees, and checks the shown width, turn, centre, shape and the one undo entry.
     */
    private fun resizeAndTurn(settings: EditSettings) {
        val viewModel = showWith(settings, select = 100L)
        val before = viewModel.state.value.settings
        val widthBefore = shownWidth(viewModel)
        val degreesBefore = shownDegrees(viewModel)
        val centreBefore = shownCentre(viewModel)
        val shapeBefore = nodeShape()

        dragHandle(viewModel, turnDegrees = 90f, scale = 2f)

        assertEquals("shown width", 2f, shownWidth(viewModel) / widthBefore, 0.1f)
        assertEquals("shown turn", 90f, angleChange(degreesBefore, shownDegrees(viewModel)), 2f)
        val centreAfter = shownCentre(viewModel)
        assertEquals("centre x", centreBefore.x, centreAfter.x, 2f)
        assertEquals("centre y", centreBefore.y, centreAfter.y, 2f)
        assertEquals("width over height", shapeBefore, nodeShape(), shapeBefore * 0.02f)
        // The drag was committed on its own: Undo is available before any Undo tap.
        assertTrue("the drag made an undo entry", viewModel.state.value.canUndo)
        viewModel.undo()
        shadowOf(Looper.getMainLooper()).idle()
        // The setup's own step is still below the drag's one.
        assertTrue("only one entry was taken", viewModel.state.value.canUndo)
        assertEquals("one drag is one undo entry", before, viewModel.state.value.settings)
    }

    /** The sticker node's own width over its height (its layout size, unaffected by the turn). */
    private fun nodeShape(): Float {
        val size = compose.onNodeWithTag("sticker-100").fetchSemanticsNode().size
        return size.width.toFloat() / size.height
    }

    /**
     * Presses the resize handle and moves the finger in 12 steps so that its distance from the
     * sticker's centre is multiplied by [scale] and its direction turns by [turnDegrees], then
     * lifts. Everything is in the overlay's own pixels, which stand still during the drag.
     */
    private fun dragHandle(viewModel: ToolViewModel, turnDegrees: Float, scale: Float) {
        val origin = compose.onNodeWithTag(STICKER_OVERLAY_TAG).fetchSemanticsNode().positionInRoot
        val handleNode = compose.onNodeWithTag(STICKER_RESIZE_TAG).fetchSemanticsNode()
        // Position plus half the size: the bounds rectangle would be clipped to the visible area.
        val handleRoot = handleNode.positionInRoot +
            Offset(handleNode.size.width / 2f, handleNode.size.height / 2f)
        val handle = Offset(handleRoot.x - origin.x, handleRoot.y - origin.y)
        val centre = shownCentre(viewModel)
        val d = distanceBetween(centre.x, centre.y, handle.x, handle.y)
        val a = angleBetween(centre.x, centre.y, handle.x, handle.y)
        compose.onNodeWithTag(STICKER_OVERLAY_TAG).performTouchInput {
            down(handle)
            val steps = 12
            for (i in 1..steps) {
                val f = i / steps.toFloat()
                val distance = d * (1f + (scale - 1f) * f)
                val angle = Math.toRadians((a + turnDegrees * f).toDouble())
                moveTo(Offset(centre.x + distance * cos(angle).toFloat(), centre.y + distance * sin(angle).toFloat()))
            }
            up()
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

    /** A made-up sticker with [key], its centre at [x] across and the middle down the original. */
    private fun sticker(
        key: Long,
        x: Float,
        width: Float = 0.3f,
        degrees: Float = 0f,
        mirrored: Boolean = false,
    ): Sticker = Sticker(key, "img_sticker_apple", x, 0.5f, width, degrees, mirrored)

    /** Draws the screen, puts [settings] on the photo, opens the Stickers sheet and selects [select]. */
    private fun showWith(settings: EditSettings, select: Long): ToolViewModel {
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
        viewModel.changeSettings(settings, processorFor)
        compose.waitForIdle()
        compose.onNodeWithTag(editToolTag(EditTool.STICKERS)).performClick()
        compose.waitForIdle()
        assertEquals(EditTool.STICKERS, viewModel.state.value.openTool)
        viewModel.selectSticker(select)
        compose.waitForIdle()
        assertEquals(select, viewModel.state.value.selectedStickerKey)
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
