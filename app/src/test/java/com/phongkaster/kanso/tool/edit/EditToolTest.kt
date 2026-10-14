package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.NoSavePhotoSaver
import com.phongkaster.kanso.ui.tool.EDIT_BRIGHTNESS_TAG
import com.phongkaster.kanso.ui.tool.EditPanel
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.editFilterTag
import com.phongkaster.kanso.ui.tool.editRatioTag
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Checks on Android 15 that the person sees each Edit change on the photo as they make it (DoD 5).
 *
 * The test draws the tool screen's stateless part, `ToolContent`, for a real [ToolViewModel] on a
 * fake 120 x 80 photo, with the real Edit panel in its panel slot handing the real Edit processor
 * to the ViewModel's `apply`. The photo the screen shows (the image node [TOOL_IMAGE_TAG]) is the
 * state's `edited` bitmap once an edit is made, so that is the bitmap read here. Within 5 s of
 * real time:
 *
 * - brightness pushed to its maximum and let go makes the shown photo brighter on average;
 * - the 1:1 chip makes the shown photo square;
 * - a filter chip changes the shown photo's colours. The filter's look-up comes from a generated
 *   inverting strip handed to the panel, so this test checks the panel's wiring, not the assets
 *   (`EditFilterTest` checks those).
 *
 * The photo is decoded before the screen is drawn, so the loading indicator (which animates
 * forever) is never on screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class EditToolTest {

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

    /** Brightness at its maximum, let go, makes the shown photo brighter on average. */
    @Test
    fun brightness_brightens_the_shown_photo() {
        val viewModel = show()
        val before = meanLuminance(shown(viewModel))
        compose.onNodeWithTag(EDIT_BRIGHTNESS_TAG)
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(EditSettings.MAX) }
        val after = waitForEdit(viewModel, previous = null)
        assertTrue("brightness: $before -> ${meanLuminance(after)}", meanLuminance(after) > before)
    }

    /** The 1:1 chip makes the shown 120 x 80 photo square. */
    @Test
    fun a_crop_ratio_changes_the_shape_of_the_shown_photo() {
        val viewModel = show()
        val before = shown(viewModel)
        assertNotEquals(before.width, before.height)
        compose.onNodeWithTag(editRatioTag(CropRatio.SQUARE)).performSemanticsAction(SemanticsActions.OnClick)
        val after = waitForEdit(viewModel, previous = null)
        assertEquals(after.height, after.width)
        assertEquals(before.height, after.height)
    }

    /** A filter chip changes the colours of the shown photo. */
    @Test
    fun a_filter_changes_the_colours_of_the_shown_photo() {
        val viewModel = show()
        val before = channelMeans(shown(viewModel))
        compose.onNodeWithTag(editFilterTag(FilterPreset.KELVIN)).performSemanticsAction(SemanticsActions.OnClick)
        val after = channelMeans(waitForEdit(viewModel, previous = null))
        val change = (0..2).maxOf { abs(after[it] - before[it]) }
        assertTrue("the filter barely changed the colours ($change)", change > 3.0)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Builds the Edit screen's ViewModel on the fake photo, decodes the photo, and draws
     * `ToolContent` with the Edit panel in its slot. Returns the ViewModel.
     */
    private fun show(): ToolViewModel {
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
        val filters = invertingSource()
        compose.setContent {
            MaterialTheme {
                // collectAsState redraws the screen whenever the ViewModel's state changes.
                val state by viewModel.state.collectAsState()
                ToolContent(
                    state = state,
                    onBack = {},
                    onSave = {},
                    panel = { EditPanel(onApply = viewModel::apply, filterSource = filters) },
                    onUndo = viewModel::undo,
                )
            }
        }
        compose.waitForIdle()
        return viewModel
    }

    /**
     * Waits (at most 5 s of real time) until an edit has landed and none is running, and returns
     * the photo now shown. [previous] is the `edited` bitmap before the action (null for none).
     */
    private fun waitForEdit(viewModel: ToolViewModel, previous: Bitmap?): Bitmap {
        compose.waitUntil(timeoutMillis = 5_000) {
            // The processor works on a real background thread; its result comes back through the
            // main looper, which a Robolectric test must let run itself.
            shadowOf(Looper.getMainLooper()).idle()
            val state = viewModel.state.value
            state.edited != null && state.edited !== previous &&
                state.processing is ToolUiState.Processing.Idle
        }
        compose.waitForIdle()
        return checkNotNull(viewModel.state.value.edited)
    }

    /** The photo the screen shows now: the edited one, or the original before any edit. */
    private fun shown(viewModel: ToolViewModel): Bitmap {
        val state = viewModel.state.value
        return state.edited ?: (state.photo as ToolUiState.PhotoState.Ready).bitmap
    }

    /** A filter source whose every filter inverts the colours, from a generated 256 x 3 strip. */
    private fun invertingSource(): FilterLut.Source {
        val data = IntArray(256 * 3) { index -> (255 - index % 256).let { rgb(it, it, it) } }
        val strip = Bitmap.createBitmap(256, 3, Bitmap.Config.ARGB_8888)
        strip.setPixels(data, 0, 256, 0, 0, 256, 3)
        val lut = FilterLut(strip)
        return FilterLut.Source { lut }
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

    /** Every pixel of [bitmap], row after row. */
    private fun pixels(bitmap: Bitmap): IntArray {
        val data = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(data, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return data
    }

    /** The mean luminance of [bitmap] (Rec. 601 weights), 0 to 255. */
    private fun meanLuminance(bitmap: Bitmap): Double = pixels(bitmap).map { color ->
        0.299 * ((color shr 16) and 0xFF) + 0.587 * ((color shr 8) and 0xFF) + 0.114 * (color and 0xFF)
    }.average()

    /** The mean red, green and blue of [bitmap], in that order. */
    private fun channelMeans(bitmap: Bitmap): DoubleArray {
        val data = pixels(bitmap)
        return doubleArrayOf(
            data.map { (it shr 16) and 0xFF }.average(),
            data.map { (it shr 8) and 0xFF }.average(),
            data.map { it and 0xFF }.average(),
        )
    }

    /** The made-up Uri of the fake photo. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"
    }
}
