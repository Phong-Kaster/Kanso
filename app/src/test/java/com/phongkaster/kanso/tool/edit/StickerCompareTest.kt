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
import com.phongkaster.kanso.ui.tool.component.photocard.LIVE_PREVIEW_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_OVERLAY_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.layerNodeTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.stickerTileTag
import com.phongkaster.kanso.ui.tool.model.editProcessorFor
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

/**
 * Checks on Android 15 that Compare shows the untouched original with no layers on it, and that
 * the sticker controls do nothing while it is on (Run 10, DoD 14).
 *
 * On the real tool screen, with the Stickers sheet open and two opaque layers added (the first
 * landing in the middle of the photo), Compare is switched on and the test checks that:
 *
 * - the pixel at the layers' place is the original photo's own pixel (read before any sticker
 *   existed), the live picture (which carries the filtered copy with the layers baked in) is not
 *   drawn, and neither is the layers' overlay;
 * - the grid's tiles are disabled and a tap on one adds nothing;
 * - the ViewModel refuses every sticker action: a drag step, selecting, duplicating, flipping and
 *   deleting change neither the settings nor the selection;
 *
 * and that with Compare off the layers are back (the pixel is no longer the original's) and the
 * tiles are enabled again.
 *
 * The layers reach the live picture through a copy drawn on a background thread, so the pixel
 * waits are loops that keep the main thread's queue running. Pixels are read by drawing the
 * activity's content view into a bitmap (`sampleScreen`), never with `captureToImage`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StickerCompareTest {

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

    /** See the class comment: Compare on, then off, with two layers on the photo. */
    @Test
    fun compare_shows_the_original_without_stickers_and_the_sticker_controls_do_nothing() {
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
        viewModel.addSticker(opaque, processorFor)
        compose.waitForIdle()
        val layers = viewModel.state.value.settings.layers
        assertEquals(2, layers.size)
        val selected = viewModel.state.value.selectedLayerKey
        assertNotNull("the added sticker was not selected", selected)
        val tileTag = stickerTileTag(StickerCatalog.inCategory(viewModel.state.value.stickerCategory).first().pictureId)

        waitUntil("the stickers to show") { distance(original, pixelAt(centre.x, centre.y)) > TOLERANCE }
        compose.onNodeWithTag(tileTag).assertIsEnabled()

        viewModel.setComparing(true)
        waitUntil("the original to show") { distance(original, pixelAt(centre.x, centre.y)) <= TOLERANCE }
        // Stays the original for a moment longer: nothing draws a sticker late.
        pumpFor(PUMP_MS)
        val comparing = pixelAt(centre.x, centre.y)
        assertTrue("Compare shows $comparing, not the original $original", distance(original, comparing) <= TOLERANCE)
        compose.onAllNodesWithTag(LIVE_PREVIEW_TAG, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag(LAYER_OVERLAY_TAG).assertCountEquals(0)
        layers.forEach { compose.onAllNodesWithTag(layerNodeTag(it.key)).assertCountEquals(0) }

        // The controls do nothing: the tiles are disabled and a tap adds nothing...
        compose.onNodeWithTag(tileTag).assertIsNotEnabled()
        compose.onNodeWithTag(tileTag).performClick()
        compose.waitForIdle()
        assertEquals("a disabled tile added a sticker", 2, viewModel.state.value.settings.layers.size)

        // ...and the ViewModel refuses each sticker action.
        val before = viewModel.state.value
        val target = layers.first().key
        viewModel.previewLayers(layers.map { it.copy(centreX = 0.1f) }, processorFor)
        viewModel.selectLayer(null)
        viewModel.duplicateLayer(target, processorFor)
        viewModel.flipLayer(target, processorFor)
        viewModel.deleteLayer(target, processorFor)
        compose.waitForIdle()
        val after = viewModel.state.value
        assertEquals("a sticker action changed the settings while comparing", before.settings, after.settings)
        assertEquals("a sticker action changed the selection while comparing", before.selectedLayerKey, after.selectedLayerKey)
        assertEquals(before.canUndo, after.canUndo)
        assertNull("a drag step touched a sticker while comparing", after.touchedLayerKey)

        viewModel.setComparing(false)
        waitUntil("the stickers to come back") { distance(original, pixelAt(centre.x, centre.y)) > TOLERANCE }
        compose.onNodeWithTag(tileTag).assertIsEnabled()
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Waits, in real time and at most [WAIT_TIMEOUT_MS], until [condition] holds, running the main
     * thread's queue and the composition between checks; fails naming [what].
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
        compose.waitForIdle()
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

        /** How far two pixels of the same photo may differ (out of 255) after two ways of drawing it. */
        const val TOLERANCE = 12f

        /** How long, in real milliseconds, a wait may take before the test fails. */
        const val WAIT_TIMEOUT_MS = 10_000L

        /** How long, in milliseconds, to sleep between two checks of a wait. */
        const val POLL_MS = 20L

        /** How long, in real milliseconds, to let a late sticker show up (there should be none). */
        const val PUMP_MS = 300L
    }
}
