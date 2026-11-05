package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.CatalogSticker
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.settings.sampleScreen
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_DELETE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_DUPLICATE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_FLIP_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_OVERLAY_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_RESIZE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.layerNodeTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.stickerTileTag
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
 * Proves Compare hides the layers (Run 10, DoD 14, Android 15): with the Stickers panel open
 * and one opaque sticker selected, Compare on shows the original photo's own pixel where the
 * sticker lies, draws no sticker, no frame and no corner button, and disables the grid's tiles;
 * Compare off brings the sticker, the frame and the tiles back. Pixels are read by drawing the
 * activity's content view into a bitmap (`sampleScreen`), never with `captureToImage`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StickerOverlayTest {

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
     * With Compare on, the pixel where the sticker lies equals the original photo's pixel (read
     * before the sticker was added), no sticker node, frame or corner button exists, the grid's
     * tiles are disabled and a tap on one adds nothing. With Compare off the sticker covers that
     * pixel again, the frame and its four buttons are back and the tiles are enabled.
     */
    @Test
    fun compare_hides_the_sticker_and_its_frame_and_disables_the_grid() {
        val viewModel = showEdit()
        // The Stickers sheet makes the photo shorter, so the photo is measured once it is open.
        // Nine entries no longer fit a phone-sized window: bring Stickers into view first.
        compose.scrollToolRowTo(EditTool.STICKERS)
        compose.onNodeWithTag(editToolTag(EditTool.STICKERS)).performClick()
        compose.waitForIdle()
        val centre = compose.onNodeWithTag(TOOL_IMAGE_TAG).fetchSemanticsNode().boundsInRoot.center
        val original = pixelAt(centre.x, centre.y)
        val opaque = opaqueSticker()
        viewModel.addSticker(opaque, processorFor)
        compose.waitForIdle()
        val key = viewModel.state.value.selectedLayerKey
        assertNotNull("the added sticker was not selected", key)
        val tileTag = stickerTileTag(StickerCatalog.inCategory(viewModel.state.value.stickerCategory).first().pictureId)
        pollPixel(centre.x, centre.y, { "an opaque sticker left the photo's pixel as it was: $it" }) { distance(original, it) > TOLERANCE }
        compose.onNodeWithTag(layerNodeTag(key!!)).assertExists()
        cornerTags.forEach { compose.onNodeWithTag(it).assertExists() }
        compose.onNodeWithTag(tileTag).assertIsEnabled()

        viewModel.setComparing(true)
        compose.waitForIdle()
        pollPixel(centre.x, centre.y, { "Compare shows $it, not the original $original" }) { distance(original, it) <= TOLERANCE }
        compose.onAllNodesWithTag(layerNodeTag(key)).assertCountEquals(0)
        compose.onAllNodesWithTag(LAYER_OVERLAY_TAG).assertCountEquals(0)
        cornerTags.forEach { compose.onAllNodesWithTag(it).assertCountEquals(0) }
        compose.onNodeWithTag(tileTag).assertIsNotEnabled()
        compose.onNodeWithTag(tileTag).performClick()
        compose.waitForIdle()
        assertEquals("a disabled tile added a sticker", 1, viewModel.state.value.settings.layers.size)

        viewModel.setComparing(false)
        compose.waitForIdle()
        pollPixel(centre.x, centre.y, { "the sticker did not come back: $it" }) { distance(original, it) > TOLERANCE }
        compose.onNodeWithTag(layerNodeTag(key)).assertExists()
        cornerTags.forEach { compose.onNodeWithTag(it).assertExists() }
        compose.onNodeWithTag(tileTag).assertIsEnabled()
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** The four corner buttons' tags. */
    private val cornerTags = listOf(LAYER_DELETE_TAG, LAYER_DUPLICATE_TAG, LAYER_FLIP_TAG, LAYER_RESIZE_TAG)

    /** The colour of the one screen pixel at ([x], [y]) in the Compose root, read from a drawn bitmap. */
    private fun pixelAt(x: Float, y: Float): Color =
        compose.activity.sampleScreen(Rect(x, y, x + 1f, y + 1f)).ink

    /**
     * Reads the pixel at ([x], [y]) again and again, in real time and at most [WAIT_TIMEOUT_MS],
     * until [accept] holds for it, running the main queue and the composition between tries. The
     * sticker pictures arrive through the live preview's small copy, drawn on a background thread,
     * so one read right after a change can still show the old picture. Fails with [message] built
     * from the last pixel read when time runs out; returns the accepted pixel.
     */
    private fun pollPixel(x: Float, y: Float, message: (Color) -> String, accept: (Color) -> Boolean): Color {
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS
        while (true) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.waitForIdle()
            val pixel = pixelAt(x, y)
            if (accept(pixel)) return pixel
            if (System.currentTimeMillis() > deadline) fail(message(pixel))
            Thread.sleep(POLL_MS)
        }
    }

    /** The largest difference between a channel of [a] and the same channel of [b], from 0 to 255. */
    private fun distance(a: Color, b: Color): Float = maxOf(
        abs(a.red - b.red),
        abs(a.green - b.green),
        abs(a.blue - b.blue),
    ) * 255f

    /** The first catalogue picture whose middle pixel is fully opaque: a sticker that hides what is under it. */
    private fun opaqueSticker(): CatalogSticker {
        val found = StickerCatalog.all.firstOrNull { entry ->
            val bitmap = BitmapFactory.decodeResource(context.resources, entry.res)
            val opaque = bitmap != null && (bitmap.getPixel(bitmap.width / 2, bitmap.height / 2) ushr 24) == 255
            bitmap?.recycle()
            opaque
        }
        assertNotNull("no catalogue picture is opaque in the middle", found)
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

        /** How far two pixels of the same photo may differ (out of 255) after two ways of drawing it. */
        const val TOLERANCE = 12f

        /** The longest a pixel poll waits, in milliseconds, for the live preview's copy to land. */
        const val WAIT_TIMEOUT_MS = 10_000L

        /** How long, in milliseconds, to sleep between two reads of a poll. */
        const val POLL_MS = 50L
    }
}
