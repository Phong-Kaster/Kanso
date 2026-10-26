package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.CatalogSticker
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.Sticker
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.edit.edit.StickerPictures
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.settings.sampleScreen
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.component.EDIT_CROPPER_TAG
import com.phongkaster.kanso.ui.tool.component.EditCropper
import com.phongkaster.kanso.ui.tool.model.editProcessorFor
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks on Android 15 that the stickers stay on the photo while Crop is open (Run 10, DoD 20), so
 * the person sees what the crop will cut.
 *
 * - **The cropper alone, one opaque sticker.** The real [EditCropper] is drawn on a blue photo with
 *   one solid red sticker in its middle (a fake picture source gives the red square): the pixel at
 *   the sticker's place in the cropper is red, not the photo's blue. The crop rectangle's handles
 *   and body still work: a drag inside the rectangle reports one finished crop moved to the left.
 * - **The whole screen, the Crop panel opened.** On the real tool screen a sticker (a catalogue
 *   picture opaque in the middle) is added first and the Crop sheet then opened: the pixel at the
 *   middle of the cropper is that picture's colour, not the photo's.
 *
 * The cropper loads its picture on a background thread, so each pixel is read in a loop that
 * keeps the main thread's queue running until the expected colour appears (or the wait runs out
 * and the test fails with the colour it saw). Pixels are read by drawing the activity's content
 * view into a bitmap (`sampleScreen`), never with `captureToImage`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StickerCropperTest {

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
     * One opaque sticker on the cropper alone: the pixel at the middle of the cropper (where the
     * sticker is centred, inside the crop rectangle) is the sticker's red, not the photo's blue,
     * and a drag inside the rectangle still moves the crop: exactly one finished crop is reported
     * and its left edge lies left of where it started.
     */
    @Test
    fun the_sticker_shows_in_its_own_colour_and_the_crop_still_drags() {
        val reported = mutableListOf<EditSettings.CropRect>()
        val photo = Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888)
            .also { it.eraseColor(BLUE) }
        val sticker = Sticker(
            key = 1L,
            pictureId = "red",
            centreX = 0.5f,
            centreY = 0.5f,
            width = 0.5f,
            degrees = 0f,
            mirrored = false,
        )
        val stickers = listOf(sticker)
        val settings = EditSettings(crop = CROP, stickers = stickers)
        // One picture source for the whole composition (the cropper remembers by it).
        val pictures = StickerPictures { _, widthPx ->
            val edge = widthPx.coerceAtLeast(1)
            Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888).also { it.eraseColor(RED) }
        }
        compose.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    EditCropper(
                        bitmap = photo,
                        settings = settings,
                        enabled = true,
                        onCropFinished = { rect -> reported += rect },
                        stickers = stickers,
                        stickerPictures = pictures,
                    )
                }
            }
        }
        compose.waitForIdle()

        val centre = compose.onNodeWithTag(EDIT_CROPPER_TAG).fetchSemanticsNode().boundsInRoot.center
        val seen = waitForColour(centre.x, centre.y, Color(RED))
        assertTrue("the sticker's place shows $seen, not the sticker's red", distance(seen, Color(RED)) <= STRICT)
        assertTrue("the photo's blue shows through the sticker", distance(seen, Color(BLUE)) > STRICT)

        compose.onNodeWithTag(EDIT_CROPPER_TAG).performTouchInput {
            swipe(start = center, end = center - Offset(width / 8f, 0f), durationMillis = DRAG_MILLIS)
        }
        compose.waitForIdle()
        assertEquals("one drag reported ${reported.size} crops", 1, reported.size)
        assertTrue("the drag did not move the crop left: ${reported.single()}", reported.single().left < CROP.left)
    }

    /**
     * DoD 20 through the real tool screen: a sticker added first, then the Crop sheet opened. The
     * cropper's middle pixel is the sticker picture's own middle colour (read from the picture's
     * resource), which the gradient photo does not have there.
     */
    @Test
    fun with_the_crop_sheet_open_the_screens_cropper_shows_the_sticker() {
        val viewModel = showEdit()
        val entry = opaqueSticker()
        val processorFor = editProcessorFor(context)
        val photo = (viewModel.state.value.photo as ToolUiState.PhotoState.Ready).bitmap
        val photoColour = Color(photo.getPixel(photo.width / 2, photo.height / 2))
        val stickerColour = pictureMiddle(entry)
        assertTrue(
            "the test's picture ($stickerColour) is too close to the photo's middle ($photoColour)",
            distance(stickerColour, photoColour) > LOOSE,
        )

        val sticker = Sticker(
            key = 7L,
            pictureId = entry.pictureId,
            centreX = 0.5f,
            centreY = 0.5f,
            width = 0.4f,
            degrees = 0f,
            mirrored = false,
        )
        viewModel.changeSettings(EditSettings(stickers = listOf(sticker)), processorFor)
        viewModel.openTool(EditTool.CROP)
        compose.waitForIdle()

        val centre = compose.onNodeWithTag(EDIT_CROPPER_TAG).fetchSemanticsNode().boundsInRoot.center
        val seen = waitForColour(centre.x, centre.y, stickerColour, LOOSE)
        assertTrue(
            "the cropper's middle shows $seen, not the sticker's $stickerColour (the photo has $photoColour)",
            distance(seen, stickerColour) <= LOOSE,
        )
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Reads the pixel at ([x], [y]) again and again, letting the main thread's queue run and
     * sleeping a little between reads, until it is within [tolerance] of [expected] or
     * [WAIT_TIMEOUT_MS] has passed. Returns the last colour read (the caller asserts on it).
     */
    private fun waitForColour(x: Float, y: Float, expected: Color, tolerance: Float = STRICT): Color {
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.waitForIdle()
            val now = pixelAt(x, y)
            if (distance(now, expected) <= tolerance || System.currentTimeMillis() > deadline) return now
            Thread.sleep(POLL_MS)
        }
    }

    /** The colour of the one screen pixel at ([x], [y]) in the Compose root, read from a drawn bitmap. */
    private fun pixelAt(x: Float, y: Float): Color =
        compose.activity.sampleScreen(Rect(x, y, x + 1f, y + 1f)).ink

    /** The largest difference between a channel of [a] and the same channel of [b], from 0 to 255. */
    private fun distance(a: Color, b: Color): Float = maxOf(
        abs(a.red - b.red),
        abs(a.green - b.green),
        abs(a.blue - b.blue),
    ) * 255f

    /** The colour of the middle pixel of the catalogue picture [entry], decoded from its resource. */
    private fun pictureMiddle(entry: CatalogSticker): Color {
        val bitmap = BitmapFactory.decodeResource(context.resources, entry.res)
        assertNotNull("the picture ${entry.pictureId} did not decode", bitmap)
        val colour = Color(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
        bitmap.recycle()
        return colour
    }

    /** The first catalogue picture whose middle pixel is fully opaque: a sticker that hides what is under it. */
    private fun opaqueSticker(): CatalogSticker {
        val found = StickerCatalog.all.firstOrNull { entry ->
            val bitmap = BitmapFactory.decodeResource(context.resources, entry.res)
            val opaque = bitmap != null && (bitmap.getPixel(bitmap.width / 2, bitmap.height / 2) ushr 24) == 255
            bitmap?.recycle()
            opaque
        }
        if (found == null) fail("no catalogue picture is opaque in the middle")
        return found!!
    }

    /** Draws the real tool screen on a fake 600 x 400 photo and returns its ViewModel; no sheet is open yet. */
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

    /** Values shared by the tests. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** The size the screen asks for: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3

        /** The cropper-alone test's photo size. */
        const val PHOTO_WIDTH = 120
        const val PHOTO_HEIGHT = 80

        /** Opaque red (the sticker) and blue (the photo) as ARGB ints. */
        const val RED = 0xFFFF0000.toInt()
        const val BLUE = 0xFF0000FF.toInt()

        /** The crop of the cropper-alone test: the photo without a tenth on each side. */
        val CROP = EditSettings.CropRect(0.1f, 0.1f, 0.9f, 0.9f)

        /** How far (out of 255) a solid colour may be from the expected one. */
        const val STRICT = 8f

        /** How far (out of 255) a scaled picture's middle may be from the picture's own middle. */
        const val LOOSE = 30f

        /** How long the test's drag lasts, in milliseconds: long enough for several moves. */
        const val DRAG_MILLIS = 400L

        /** How long, in real milliseconds, a pixel wait may take before the test reads what is there. */
        const val WAIT_TIMEOUT_MS = 10_000L

        /** How long, in milliseconds, to sleep between two reads of a wait. */
        const val POLL_MS = 25L
    }
}
