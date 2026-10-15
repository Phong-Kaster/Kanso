package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.NoSavePhotoSaver
import com.phongkaster.kanso.ui.tool.EDIT_BRIGHTNESS_TAG
import com.phongkaster.kanso.ui.tool.EDIT_COMPARE_TAG
import com.phongkaster.kanso.ui.tool.EDIT_CONTRAST_TAG
import com.phongkaster.kanso.ui.tool.EDIT_INTENSITY_TAG
import com.phongkaster.kanso.ui.tool.EDIT_PILL_TAG
import com.phongkaster.kanso.ui.tool.EDIT_ROTATE_LEFT_TAG
import com.phongkaster.kanso.ui.tool.EDIT_SATURATION_TAG
import com.phongkaster.kanso.ui.tool.EDIT_WARMTH_TAG
import com.phongkaster.kanso.ui.tool.TOOL_CARD_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_UNDO_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolPanel
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.editFilterTag
import com.phongkaster.kanso.ui.tool.editRatioTag
import com.phongkaster.kanso.ui.tool.editToolTag
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks on Android 15 the Edit screen's Compare switch and the pill that holds it (DoD 8, 21):
 *
 * - with an edit made, switching Compare on shows the untouched original photo, and switching it
 *   off shows the same edit again (the very same bitmap, never redone);
 * - while Compare is on every editing control is disabled at once: the four tool entries, Undo,
 *   and the controls of each sheet (the four colour sliders, the crop shapes, the turn buttons,
 *   the filters and their intensity), while Compare itself stays usable; the ViewModel also
 *   refuses an edit and an undo; switching Compare off enables them all again;
 * - the pill holds Undo and Compare, floats in the bottom-right quarter of the photo card, and
 *   both stay shown and reachable while a sheet is open.
 *
 * The test draws the tool screen's stateless part, `ToolContent`, for a real [ToolViewModel] on a
 * fake 120 x 80 photo, with the Edit sheet slot filled as `ToolScreen` fills it (`ToolPanel`,
 * disabled while Compare is on). The edit is real: the brightness slider pushed to its maximum and
 * let go hands the real Edit processor to the ViewModel's `apply`. What the photo shows is read
 * from pixels, drawing the activity's content view into a bitmap.
 *
 * The photo is decoded before the screen is drawn, so the loading indicator (which animates
 * forever) is never on screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class CompareTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * With a brightening edit made, Compare on draws exactly the pixels the photo had before the
     * edit, and Compare off draws exactly the edited pixels again, from the same edited bitmap.
     * The middle of the image node is compared, clear of the pill at the card's bottom right.
     */
    @Test
    fun compare_shows_the_original_and_switching_it_off_shows_the_edit_again() {
        val viewModel = showEdit(EditTool.ADJUST)
        val image = middleOf(bounds(TOOL_IMAGE_TAG))
        val originalPixels = pixelsIn(image)

        val edited = makeEdit(viewModel)
        val editedPixels = pixelsIn(image)
        assertFalse("the edit did not change the shown photo", originalPixels.contentEquals(editedPixels))

        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        assertTrue(viewModel.state.value.comparing)
        compose.onNodeWithTag(EDIT_COMPARE_TAG).assertIsOn()
        assertArrayEquals("Compare on must show the original", originalPixels, pixelsIn(image))
        assertSame("comparing must keep the edit", edited, viewModel.state.value.edited)

        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        assertFalse(viewModel.state.value.comparing)
        compose.onNodeWithTag(EDIT_COMPARE_TAG).assertIsOff()
        assertSame("Compare off must bring back the same edit", edited, viewModel.state.value.edited)
        assertArrayEquals("Compare off must show the edit again", editedPixels, pixelsIn(image))
    }

    /**
     * While Compare is on, every editing control is disabled at once and Compare stays enabled:
     * the four colour sliders, the four tool entries (a tap on one opens nothing) and Undo; the
     * controls of the other three sheets too (each opened through the ViewModel, since the row is
     * disabled). The ViewModel refuses an edit and an undo meanwhile. Switching Compare off
     * enables the sliders, the entries and Undo again.
     */
    @Test
    fun compare_disables_every_editing_control_and_switching_it_off_enables_them() {
        val viewModel = showEdit(EditTool.ADJUST)
        val edited = makeEdit(viewModel)
        assertEditingEnabled(true)

        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        assertTrue(viewModel.state.value.comparing)
        assertEditingEnabled(false)
        compose.onNodeWithTag(EDIT_COMPARE_TAG).assertIsEnabled()

        // A tap on a disabled entry changes nothing.
        compose.onNodeWithTag(editToolTag(EditTool.CROP)).performClick()
        compose.waitForIdle()
        assertEquals(EditTool.ADJUST, viewModel.state.value.openTool)

        // Every other sheet's controls are disabled too.
        mapOf(
            EditTool.CROP to listOf(editRatioTag(CropRatio.SQUARE)),
            EditTool.ROTATE to listOf(EDIT_ROTATE_LEFT_TAG),
            EditTool.FILTERS to listOf(editFilterTag(FilterPreset.KELVIN), EDIT_INTENSITY_TAG),
        ).forEach { (tool, tags) ->
            viewModel.openTool(tool)
            compose.waitForIdle()
            assertEquals(tool, viewModel.state.value.openTool)
            tags.forEach { tag -> compose.onNodeWithTag(tag).assertIsNotEnabled() }
        }
        viewModel.openTool(EditTool.ADJUST)
        compose.waitForIdle()

        // The ViewModel refuses an edit and an undo while comparing.
        viewModel.apply(EditProcessor(EditSettings(brightness = EditSettings.MIN), FilterLut.Source { null }))
        viewModel.undo()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        val during = viewModel.state.value
        assertSame("an edit or an undo went through while comparing", edited, during.edited)
        assertTrue(during.canUndo)
        assertTrue(during.processing is ToolUiState.Processing.Idle)

        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        assertFalse(viewModel.state.value.comparing)
        assertEditingEnabled(true)
        assertSame(edited, viewModel.state.value.edited)
    }

    /**
     * The pill holds both Undo and Compare and floats in the bottom-right quarter of the photo
     * card, near its corner, with no sheet open and with each sheet open; Undo is the screen's
     * only Undo. With a sheet open, a real tap on Compare reaches it and switches it on.
     */
    @Test
    fun the_pill_holds_undo_and_compare_at_the_photos_bottom_right() {
        val viewModel = showEdit(null)
        (listOf<EditTool?>(null) + EditTool.entries).forEach { open ->
            if (open == null) viewModel.closeTool() else if (viewModel.state.value.openTool != open) viewModel.openTool(open)
            compose.waitForIdle()
            assertEquals(open, viewModel.state.value.openTool)

            compose.onAllNodesWithTag(TOOL_UNDO_TAG).assertCountEquals(1)
            compose.onNode(hasTestTag(TOOL_UNDO_TAG) and hasAnyAncestor(hasTestTag(EDIT_PILL_TAG))).assertIsDisplayed()
            compose.onNode(hasTestTag(EDIT_COMPARE_TAG) and hasAnyAncestor(hasTestTag(EDIT_PILL_TAG))).assertIsDisplayed()

            val card = bounds(TOOL_CARD_TAG)
            val pill = bounds(EDIT_PILL_TAG)
            assertTrue("pill $pill not in the right half of $card with $open open", pill.left >= card.center.x)
            assertTrue("pill $pill not in the lower half of $card with $open open", pill.top >= card.center.y)
            assertTrue("pill $pill sticks out of $card with $open open", pill.right <= card.right && pill.bottom <= card.bottom)
            assertTrue("pill $pill far from the right edge with $open open", card.right - pill.right <= dp(24f))
            assertTrue("pill $pill far from the bottom edge with $open open", card.bottom - pill.bottom <= dp(24f))
        }

        viewModel.openTool(EditTool.ADJUST)
        compose.waitForIdle()
        compose.onNodeWithTag(EDIT_COMPARE_TAG).performClick()
        compose.waitForIdle()
        assertTrue("a tap on Compare did not reach it with a sheet open", viewModel.state.value.comparing)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the Edit screen's ViewModel on the fake photo, decodes the photo, opens [open]'s
     * sheet (none for null), and draws `ToolContent` with the Edit sheet slot filled as
     * `ToolScreen` fills it: the panel is disabled while Compare is on or an edit runs. Returns
     * the ViewModel.
     */
    private fun showEdit(open: EditTool?): ToolViewModel {
        val viewModel = ToolViewModel(
            Tool.EDIT,
            PHOTO_URI,
            FakePhotoLoader(context, photo()),
            ViewModelClearedProbe(),
            NoSavePhotoSaver(),
            workDispatcher = Dispatchers.Unconfined,
        )
        // The fake loader answers at once; idling the main thread lets the result land.
        viewModel.load(1000)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        if (open != null) viewModel.openTool(open)
        assertEquals(open, viewModel.state.value.openTool)
        compose.setContent {
            MaterialTheme {
                // collectAsState redraws the screen whenever the ViewModel's state changes.
                val state by viewModel.state.collectAsState()
                ToolContent(
                    state = state,
                    onBack = {},
                    onSave = {},
                    onUndo = viewModel::undo,
                    onOpenTool = viewModel::openTool,
                    onCompare = viewModel::setComparing,
                    editSheet = { tool ->
                        ToolPanel(
                            tool = Tool.EDIT,
                            state = state,
                            openTool = tool,
                            enabled = !state.comparing && state.processing is ToolUiState.Processing.Idle,
                            onApply = viewModel::apply,
                        )
                    },
                )
            }
        }
        compose.waitForIdle()
        return viewModel
    }

    /**
     * Makes one real edit through the Adjust sheet (brightness pushed to its maximum and let go),
     * waits (at most 5 s of real time) until it has landed and none is running, and returns the
     * edited bitmap.
     */
    private fun makeEdit(viewModel: ToolViewModel): Bitmap {
        compose.onNodeWithTag(EDIT_BRIGHTNESS_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(EditSettings.MAX) }
        compose.waitUntil(timeoutMillis = 5_000) {
            // The processor works on a real background thread; its result comes back through the
            // main looper, which a Robolectric test must let run itself.
            shadowOf(Looper.getMainLooper()).idle()
            val state = viewModel.state.value
            state.edited != null && state.processing is ToolUiState.Processing.Idle
        }
        compose.waitForIdle()
        assertTrue(viewModel.state.value.canUndo)
        return checkNotNull(viewModel.state.value.edited)
    }

    /**
     * Fails unless the Adjust sheet's four sliders, the four tool entries and Undo are all
     * enabled ([enabled] true) or all disabled ([enabled] false).
     */
    private fun assertEditingEnabled(enabled: Boolean) {
        val tags = listOf(EDIT_BRIGHTNESS_TAG, EDIT_CONTRAST_TAG, EDIT_SATURATION_TAG, EDIT_WARMTH_TAG, TOOL_UNDO_TAG) +
            EditTool.entries.map(::editToolTag)
        tags.forEach { tag ->
            val node = compose.onNodeWithTag(tag)
            if (enabled) node.assertIsEnabled() else node.assertIsNotEnabled()
        }
    }

    /**
     * The bounds of the node tagged [tag] in the root's pixels, from its `positionInRoot` and its
     * size (never clipped to what is visible).
     */
    private fun bounds(tag: String): Rect {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        val position = node.positionInRoot
        return Rect(position.x, position.y, position.x + node.size.width, position.y + node.size.height)
    }

    /** The middle half of [area] each way: a quarter of its width and height in from each side. */
    private fun middleOf(area: Rect): Rect = Rect(
        area.left + area.width / 4,
        area.top + area.height / 4,
        area.right - area.width / 4,
        area.bottom - area.height / 4,
    )

    /** [value] dp in this screen's pixels. */
    private fun dp(value: Float): Float = with(compose.density) { value.dp.toPx() }

    /**
     * Draws the activity's content view into a fresh bitmap and returns the pixels inside [area]
     * (in the root's pixels), row after row.
     *
     * It draws the content view itself instead of using Compose's captureToImage, whose wait for
     * the window to redraw times out under Robolectric here (see `settings/ScreenPixels.kt`). The
     * Compose root fills the content view from its top-left corner, so the two share coordinates.
     */
    private fun pixelsIn(area: Rect): IntArray {
        compose.waitForIdle()
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = checkNotNull(content.getChildAt(0)) { "The activity has no content view yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return try {
            val left = area.left.toInt().coerceIn(0, bitmap.width - 1)
            val top = area.top.toInt().coerceIn(0, bitmap.height - 1)
            val right = area.right.toInt().coerceIn(left + 1, bitmap.width)
            val bottom = area.bottom.toInt().coerceIn(top + 1, bitmap.height)
            val width = right - left
            val height = bottom - top
            val data = IntArray(width * height)
            bitmap.getPixels(data, 0, width, left, top, width, height)
            data
        } finally {
            bitmap.recycle()
        }
    }

    /** The fake 120 x 80 photo: mid-grey on the left, a moderate orange on the right. */
    private fun photo(): Bitmap {
        val width = 120
        val height = 80
        val data = IntArray(width * height) { index ->
            if (index % width < width / 2) rgb(128, 128, 128) else rgb(180, 120, 80)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(data, 0, width, 0, 0, width, height)
        return bitmap
    }

    /** An opaque ARGB colour from its three channels. */
    private fun rgb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    /** The made-up Uri of the fake photo. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"
    }
}
