package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.EditColourMatrix
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.Sticker
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.components.photocard.LIVE_PREVIEW_TAG
import com.phongkaster.kanso.ui.tool.components.photocard.LivePreviewGeometry
import com.phongkaster.kanso.ui.tool.components.photocard.LivePreviewImage
import com.phongkaster.kanso.ui.tool.components.photocard.LivePreviewMatrix
import com.phongkaster.kanso.ui.tool.components.photocard.LivePreviewTouchedSticker
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Checks on Android 15 how stickers show in the Edit screen's live picture (Run 10, DoD 19).
 *
 * **The finger and the filtered copy.** The untouched stickers are drawn into the small copy the
 * ViewModel filters; the one under a finger is drawn live by `LivePreviewImage` instead. The first
 * test drives the real tool screen for a real [ToolViewModel] from a filter, a raised brightness, a
 * crop, a turn, a flip and two stickers, with a recording processor standing in for the recipe, and
 * checks the whole life of one touch:
 *
 * - before the touch both stickers are in the request that was rendered, and the live picture
 *   reports no touched sticker;
 * - between finger-down (the first changed sticker list, `previewStickers`) and finger-up
 *   (`commitSettings`) the touched sticker is left out of the request, the live picture reports it
 *   (`LivePreviewTouchedSticker`) with exactly the picture's own crop, turn and flip
 *   (`LivePreviewGeometry`) and its colour matrix (`LivePreviewMatrix`, the sliders' numbers), and
 *   further drag steps send no new request;
 * - after finger-up the next request includes it at its new place, and the live picture reports
 *   no touched sticker again.
 *
 * **The cut at the edge.** The second test hosts the live picture alone, with a solid red sticker
 * under a finger lying half outside the crop on a blue photo, on a white backdrop, and reads the
 * pixels (drawing the activity's content view into a bitmap, never `captureToImage`): no red pixel
 * lies outside the shown photo's bounds, while red is drawn inside them.
 *
 * The photo is decoded before the screen is drawn, so no loading indicator is on screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StickerLivePreviewTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * One touch, start to end: see the class comment. The request that reaches the renderer is read
     * from the settings each rendering processor was built for; the live picture is read from its
     * semantics.
     */
    @Test
    fun the_touched_sticker_is_left_out_of_the_request_and_joins_it_on_finger_lift() {
        val first = sticker(100L, 0.3f)
        val second = sticker(101L, 0.7f)
        val base = EditSettings(
            filter = FilterPreset.KELVIN,
            filterIntensity = 0.8f,
            brightness = 0.5f,
            quarterTurns = 1,
            flipHorizontal = true,
            crop = EditSettings.CropRect(0.1f, 0.1f, 0.9f, 0.9f),
            stickers = listOf(first, second),
        )
        val recorder = RequestRecorder()
        val viewModel = showEdit()

        viewModel.changeSettings(base, recorder.processorFor)
        waitUntil("both stickers in the first request") { recorder.requests.any { it.stickers == listOf(first, second) } }
        viewModel.openTool(EditTool.STICKERS)
        compose.waitForIdle()
        assertNull("a sticker is reported touched before any touch", touchedReport())
        assertNull(viewModel.state.value.touchedStickerKey)

        // Finger down and a first move: the first sticker is touched.
        val moved = first.copy(centreX = 0.35f)
        val requestsBefore = recorder.requests.size
        viewModel.previewStickers(listOf(moved, second), recorder.processorFor)
        waitUntil("a request without the touched sticker") { recorder.requests.size > requestsBefore }
        assertEquals("the touched sticker is not the first", 100L, viewModel.state.value.touchedStickerKey)
        assertEquals(
            "the request still holds the touched sticker",
            listOf(second),
            recorder.requests.last().stickers,
        )
        compose.waitForIdle()
        val geometry = shownGeometry()
        assertEquals("turns=1;flipH=true;flipV=false;crop=0.1,0.1,0.9,0.9", geometry)
        val matrix = shownMatrix()
        assertEquals(EditColourMatrix.of(base).toList(), matrix)
        assertEquals(
            "the live picture does not report the touched sticker as expected",
            "key=100;picture=${first.pictureId};drawn=true;$geometry;matrix=${matrix.joinToString(",")}",
            touchedReport(),
        )

        // More drag steps: the request does not change, so nothing is rendered again.
        val afterTouch = recorder.requests.size
        val last = first.copy(centreX = 0.45f, centreY = 0.4f)
        viewModel.previewStickers(listOf(first.copy(centreX = 0.4f), second), recorder.processorFor)
        viewModel.previewStickers(listOf(last, second), recorder.processorFor)
        pumpFor(PUMP_MS)
        compose.waitForIdle()
        assertEquals("a drag step re-rendered the filtered copy", afterTouch, recorder.requests.size)
        assertTrue(
            "the drag step did not reach the report",
            touchedReport()!!.startsWith("key=100;picture=${first.pictureId};"),
        )
        assertEquals(geometry, shownGeometry())

        // Finger up: the touched sticker joins the next request, at its new place.
        viewModel.commitSettings()
        waitUntil("a request with the lifted sticker") { recorder.requests.last().stickers == listOf(last, second) }
        waitUntil("the touched report to go") { touchedReport() == null }
        assertNull(viewModel.state.value.touchedStickerKey)
        assertEquals(listOf(last, second), viewModel.state.value.settings.stickers)
    }

    /**
     * A sticker under a finger that lies half outside the crop is cut at the shown photo's edge: on
     * a blue 120 x 80 photo cropped to its middle, with a white backdrop, the solid red sticker is
     * centred on the crop's top-left corner, so a quarter of it would spill above the picture. No
     * red pixel lies outside the shown photo's bounds (one pixel of slack for rounding), and red is
     * drawn inside them.
     */
    @Test
    fun a_sticker_partly_outside_the_crop_is_cut_at_the_edge_of_the_picture() {
        val photo = solidBitmap(120, 80, BLUE)
        val picture = solidBitmap(40, 40, RED).asImageBitmap()
        val settings = EditSettings(crop = EditSettings.CropRect(0.25f, 0.25f, 0.75f, 0.75f))
        val touched = Sticker(
            key = 1L,
            pictureId = "red",
            centreX = 0.25f,
            centreY = 0.25f,
            width = 0.4f,
            degrees = 0f,
            mirrored = false,
        )
        compose.setContent {
            Box(Modifier.fillMaxSize().background(ComposeColor.White)) {
                Box(Modifier.width(BOX_DP.dp).height(BOX_DP.dp)) {
                    LivePreviewImage(
                        bitmap = photo,
                        settings = settings,
                        touchedSticker = touched,
                        touchedPicture = picture,
                    )
                }
            }
        }
        compose.waitForIdle()

        // The shown photo: the crop (60 x 40 of the photo) fitted into the square box and centred.
        val box = compose.onNodeWithTag(LIVE_PREVIEW_TAG).fetchSemanticsNode().boundsInRoot
        val fit = minOf(box.width / 60f, box.height / 40f)
        val shownWidth = 60f * fit
        val shownHeight = 40f * fit
        val left = box.center.x - shownWidth / 2f
        val top = box.center.y - shownHeight / 2f
        val right = left + shownWidth
        val bottom = top + shownHeight

        val screen = drawnScreen()
        var outside = 0
        var inside = 0
        for (y in box.top.toInt() until box.bottom.toInt()) {
            for (x in box.left.toInt() until box.right.toInt()) {
                if (!isRed(screen.getPixel(x, y))) continue
                val within = x >= left - 1f && x <= right + 1f && y >= top - 1f && y <= bottom + 1f
                if (within) inside++ else outside++
            }
        }
        screen.recycle()
        assertTrue("the sticker was not drawn inside the picture", inside > MIN_RED_PIXELS)
        assertEquals("sticker pixels were drawn outside the picture", 0, outside)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Stands in for the recipe that turns settings into a processor: each processor it builds
     * records the settings it was built for (the settings of one preview request) and answers with
     * a plain picture the size of its input.
     */
    private class RequestRecorder {

        /** The settings of every request that reached a processor, oldest first. */
        val requests = CopyOnWriteArrayList<EditSettings>()

        /** Builds the processor for one request. */
        val processorFor: (EditSettings) -> ToolProcessor = { settings ->
            object : ToolProcessor {
                /** Records [settings] and answers a picture of [input]'s size. */
                override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult {
                    requests += settings
                    val picture = Bitmap.createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
                    picture.eraseColor(android.graphics.Color.rgb(200, 40, 40))
                    return EditResult.Edited(picture)
                }
            }
        }
    }

    /** A real catalogue picture as a sticker with [key], upright, at the given horizontal place, in the photo's middle row. */
    private fun sticker(key: Long, centreX: Float): Sticker = Sticker(
        key = key,
        pictureId = "img_sticker_apple",
        centreX = centreX,
        centreY = 0.5f,
        width = 0.2f,
        degrees = 0f,
        mirrored = false,
    )

    /** The live picture's report of the touched sticker, or null when it reports none. */
    private fun touchedReport(): String? {
        val config = compose.onNodeWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).fetchSemanticsNode().config
        return if (LivePreviewTouchedSticker in config) config[LivePreviewTouchedSticker] else null
    }

    /** The crop, turn and flip the live picture says it applies. */
    private fun shownGeometry(): String =
        compose.onNodeWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).fetchSemanticsNode().config[LivePreviewGeometry]

    /** The colour matrix the live picture says it applies. */
    private fun shownMatrix(): List<Float> =
        compose.onNodeWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).fetchSemanticsNode().config[LivePreviewMatrix]

    /** Builds the Edit screen's ViewModel on a fake 600 x 400 photo, decodes it, and draws the real tool screen. */
    private fun showEdit(): ToolViewModel {
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
        return viewModel
    }

    /**
     * Waits, in real time and at most [WAIT_TIMEOUT_MS], until [condition] holds, running the main
     * thread's queue and the composition between checks (where the ViewModel's results land);
     * fails naming [what]. The filtered copy is drawn on a real background thread, so a virtual
     * clock would not do.
     */
    private fun waitUntil(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.waitForIdle()
            if (condition()) return
            if (System.currentTimeMillis() > deadline) fail("Timed out waiting for $what")
            Thread.sleep(POLL_MS)
        }
    }

    /** Lets background work and the main thread's queue run for about [millis] real milliseconds. */
    private fun pumpFor(millis: Long) {
        val deadline = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(POLL_MS)
        }
    }

    /**
     * Draws the activity's content view into a fresh bitmap and returns it (the caller recycles
     * it). The content view is drawn itself instead of using Compose's captureToImage, whose wait
     * for the window to redraw times out under Robolectric here (see `settings/ScreenPixels.kt`).
     */
    private fun drawnScreen(): Bitmap {
        compose.waitForIdle()
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = checkNotNull(content.getChildAt(0)) { "The activity has no content view yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    /** True for a clearly red pixel: neither the white backdrop, nor the blue photo, nor a blend of the two. */
    private fun isRed(pixel: Int): Boolean {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        return red > 200 && green < 60 && blue < 60
    }

    /** A bitmap of [width] x [height] filled with one opaque [colour]. */
    private fun solidBitmap(width: Int, height: Int, colour: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { it.eraseColor(colour) }

    /** Values shared by the tests. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** The size the screen asks for: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3

        /** The side, in dp, of the square the live picture is drawn in. */
        const val BOX_DP = 200

        /** Opaque red and blue as ARGB ints. */
        const val RED = 0xFFFF0000.toInt()
        const val BLUE = 0xFF0000FF.toInt()

        /** How many red pixels inside the picture make the sticker count as drawn. */
        const val MIN_RED_PIXELS = 100

        /** How long, in real milliseconds, a wait may take before the test fails. */
        const val WAIT_TIMEOUT_MS = 10_000L

        /** How long, in milliseconds, to sleep between two checks of a wait. */
        const val POLL_MS = 10L

        /** How long, in real milliseconds, to let the drag steps' effects (there should be none) settle. */
        const val PUMP_MS = 300L
    }
}
