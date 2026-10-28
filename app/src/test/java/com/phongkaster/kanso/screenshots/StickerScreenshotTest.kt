package com.phongkaster.kanso.screenshots

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.edit.edit.CatalogSticker
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.settings.sampleScreen
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_DELETE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_DUPLICATE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_FLIP_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_RESIZE_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.STICKER_CHIPS_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.stickerChipTag
import com.phongkaster.kanso.ui.tool.model.editProcessorFor
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import org.junit.After
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
 * Takes the reference pictures of the Edit screen's layers (Run 10, DoD 27), each in light and
 * in dark, numbered 69 to 82:
 *
 * - 69 and 70: the row of tools after a sticker was added and the sheet closed, the dot showing
 *   on Stickers;
 * - 71 to 78: the Stickers sheet on each category tab, Food, Mood, Quote and Emoji;
 * - 79 and 80: a sticker just added and selected, its four corner buttons around it;
 * - 81 and 82: the photo with layers after a crop through one and a filter.
 *
 * Stickers reach the live picture through a small copy drawn on a background thread, so a picture
 * is never taken right after a change: the test polls a screen pixel in real time until the
 * sticker has landed (and, for the filter, until the filtered copy exists and the picture stops
 * changing). The photo is drawn in code and handed over by a [FakePhotoLoader], on Android 11 and
 * the same phone screen as [ToolScreenshotTest].
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = STICKER_PHONE)
class StickerScreenshotTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** The shared recipe that turns settings into the real processor, as the screen builds it. */
    private val processorFor by lazy { editProcessorFor(context) }

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Picture 69: the row of tools with Stickers used, its dot showing, light. */
    @Test
    fun edit_stickers_row_light() {
        val viewModel = showEdit(ThemeMode.Light)
        addStickerAndClosePanel(viewModel)
        capture("69_edit_stickers_row_light.png")
    }

    /** Picture 70: the row of tools with Stickers used, its dot showing, dark. */
    @Test
    fun edit_stickers_row_dark() {
        val viewModel = showEdit(ThemeMode.Dark)
        addStickerAndClosePanel(viewModel)
        capture("70_edit_stickers_row_dark.png")
    }

    /** Picture 71: the Stickers sheet on the Food tab, light. */
    @Test
    fun edit_stickers_food_light() {
        showEdit(ThemeMode.Light)
        openStickers()
        chooseCategory(StickerCategory.FOOD)
        capture("71_edit_stickers_food_light.png")
    }

    /** Picture 72: the Stickers sheet on the Food tab, dark. */
    @Test
    fun edit_stickers_food_dark() {
        showEdit(ThemeMode.Dark)
        openStickers()
        chooseCategory(StickerCategory.FOOD)
        capture("72_edit_stickers_food_dark.png")
    }

    /** Picture 73: the Stickers sheet on the Mood tab, light. */
    @Test
    fun edit_stickers_mood_light() {
        showEdit(ThemeMode.Light)
        openStickers()
        chooseCategory(StickerCategory.MOOD)
        capture("73_edit_stickers_mood_light.png")
    }

    /** Picture 74: the Stickers sheet on the Mood tab, dark. */
    @Test
    fun edit_stickers_mood_dark() {
        showEdit(ThemeMode.Dark)
        openStickers()
        chooseCategory(StickerCategory.MOOD)
        capture("74_edit_stickers_mood_dark.png")
    }

    /** Picture 75: the Stickers sheet on the Quote tab, light. */
    @Test
    fun edit_stickers_quote_light() {
        showEdit(ThemeMode.Light)
        openStickers()
        chooseCategory(StickerCategory.QUOTE)
        capture("75_edit_stickers_quote_light.png")
    }

    /** Picture 76: the Stickers sheet on the Quote tab, dark. */
    @Test
    fun edit_stickers_quote_dark() {
        showEdit(ThemeMode.Dark)
        openStickers()
        chooseCategory(StickerCategory.QUOTE)
        capture("76_edit_stickers_quote_dark.png")
    }

    /** Picture 77: the Stickers sheet on the Emoji tab, light. */
    @Test
    fun edit_stickers_emoji_light() {
        showEdit(ThemeMode.Light)
        openStickers()
        chooseCategory(StickerCategory.EMOJI)
        capture("77_edit_stickers_emoji_light.png")
    }

    /** Picture 78: the Stickers sheet on the Emoji tab, dark. */
    @Test
    fun edit_stickers_emoji_dark() {
        showEdit(ThemeMode.Dark)
        openStickers()
        chooseCategory(StickerCategory.EMOJI)
        capture("78_edit_stickers_emoji_dark.png")
    }

    /** Picture 79: a sticker just added and selected, with its four corner buttons, light. */
    @Test
    fun edit_sticker_selected_light() {
        val viewModel = showEdit(ThemeMode.Light)
        addSelectedSticker(viewModel)
        capture("79_edit_sticker_selected_light.png")
    }

    /** Picture 80: a sticker just added and selected, with its four corner buttons, dark. */
    @Test
    fun edit_sticker_selected_dark() {
        val viewModel = showEdit(ThemeMode.Dark)
        addSelectedSticker(viewModel)
        capture("80_edit_sticker_selected_dark.png")
    }

    /** Picture 81: the photo with a sticker after a crop through it and a filter, light. */
    @Test
    fun edit_stickers_crop_filter_light() {
        val viewModel = showEdit(ThemeMode.Light)
        cropAndFilterWithSticker(viewModel)
        capture("81_edit_stickers_crop_filter_light.png")
    }

    /** Picture 82: the photo with a sticker after a crop through it and a filter, dark. */
    @Test
    fun edit_stickers_crop_filter_dark() {
        val viewModel = showEdit(ThemeMode.Dark)
        cropAndFilterWithSticker(viewModel)
        capture("82_edit_stickers_crop_filter_dark.png")
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Taps the Stickers entry in the row of tools so its sheet opens. */
    private fun openStickers() {
        compose.onNodeWithTag(editToolTag(EditTool.STICKERS)).performClick()
        compose.waitForIdle()
    }

    /** Taps the chip of [category], scrolling the chip row to it first (it may sit off screen). */
    private fun chooseCategory(category: StickerCategory) {
        compose.onNodeWithTag(STICKER_CHIPS_TAG).performScrollToNode(hasTestTag(stickerChipTag(category)))
        compose.onNodeWithTag(stickerChipTag(category)).performClick()
        compose.waitForIdle()
    }

    /**
     * Opens the Stickers sheet, adds the first catalogue picture that is opaque in its middle (so
     * it certainly hides the photo under it), and waits until that picture has really landed on
     * the photo: the pixel at the photo's middle must stop being the photo's own colour. It also
     * checks the four corner buttons are drawn.
     */
    private fun addSelectedSticker(viewModel: ToolViewModel) {
        val centre = compose.onNodeWithTag(TOOL_IMAGE_TAG).fetchSemanticsNode().boundsInRoot.center
        val original = pixelAt(centre.x, centre.y)
        openStickers()
        viewModel.addSticker(opaqueSticker(), processorFor)
        compose.waitForIdle()
        assertNotNull("the added sticker was not selected", viewModel.state.value.selectedLayerKey)
        pollPixel(centre.x, centre.y, "the sticker never covered the photo's middle") { distance(original, it) > TOLERANCE }
        listOf(LAYER_DELETE_TAG, LAYER_DUPLICATE_TAG, LAYER_FLIP_TAG, LAYER_RESIZE_TAG)
            .forEach { compose.onNodeWithTag(it).assertExists() }
    }

    /**
     * Adds one sticker (waiting until it has landed on the photo), then ticks the sheet closed so
     * the bottom row of tools is back with the dot on Stickers, and waits until the picture is
     * still.
     */
    private fun addStickerAndClosePanel(viewModel: ToolViewModel) {
        addSelectedSticker(viewModel)
        viewModel.confirmLayers()
        compose.waitForIdle()
        waitUntilStill()
    }

    /**
     * Adds a sticker, ticks the sheet closed, then crops to a rectangle that still holds the
     * sticker's middle and chooses the 1977 filter. It waits for the filtered copy (which holds the
     * layers) to exist and then for the picture to stop changing, so the render has landed.
     */
    private fun cropAndFilterWithSticker(viewModel: ToolViewModel) {
        addSelectedSticker(viewModel)
        viewModel.confirmLayers()
        compose.waitForIdle()
        val settings = viewModel.state.value.settings.copy(
            crop = EditSettings.CropRect(0.15f, 0.15f, 0.85f, 0.85f),
            filter = FilterPreset.SEVENTY_SEVEN,
        )
        viewModel.changeSettings(settings, processorFor)
        waitUntil("the filtered copy") { viewModel.state.value.filterPreview != null }
        waitUntilStill()
    }

    /** The colour of the one screen pixel at ([x], [y]) in the Compose root, read from a drawn bitmap. */
    private fun pixelAt(x: Float, y: Float): Color =
        compose.activity.sampleScreen(Rect(x, y, x + 1f, y + 1f)).ink

    /**
     * Reads the pixel at ([x], [y]) again and again, in real time and at most [WAIT_TIMEOUT_MS],
     * running the main queue and the composition between tries, until [accept] holds for it.
     * Fails with [message] when time runs out.
     */
    private fun pollPixel(x: Float, y: Float, message: String, accept: (Color) -> Boolean) {
        waitUntil(message) { accept(pixelAt(x, y)) }
    }

    /**
     * Waits in real time, at most [WAIT_TIMEOUT_MS], until [condition] holds, running the main
     * thread's queue and the composition between checks (where background results land). Fails
     * naming [what] when time runs out. No fixed sleep stands in for the render.
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

    /**
     * Waits until the whole picture has stopped changing: several pixels of the photo read the
     * same on [STILL_READS] checks in a row, [POLL_MS] apart. A render that is still landing moves
     * at least one of them.
     */
    private fun waitUntilStill() {
        val bounds = compose.onNodeWithTag(TOOL_IMAGE_TAG).fetchSemanticsNode().boundsInRoot
        val points = listOf(0.3f, 0.5f, 0.7f).flatMap { fx ->
            listOf(0.3f, 0.5f, 0.7f).map { fy -> bounds.left + bounds.width * fx to bounds.top + bounds.height * fy }
        }
        var last: List<Color>? = null
        var same = 0
        waitUntil("the picture to stop changing") {
            val now = points.map { (x, y) -> pixelAt(x, y) }
            same = if (last != null && now.indices.all { distance(now[it], last!![it]) <= 1f }) same + 1 else 0
            last = now
            same >= STILL_READS
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

    /**
     * Draws the real tool screen for a real [ToolViewModel] on a 600 x 400 gradient photo in [mode]
     * and returns the ViewModel. The photo is decoded first, so no loading indicator (which
     * animates forever) is ever on screen. No sheet is open yet.
     */
    private fun showEdit(mode: ThemeMode): ToolViewModel {
        val viewModel = ToolViewModel(
            tool = Tool.EDIT,
            photoUri = PHOTO_URI,
            loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400)),
            probe = ViewModelClearedProbe(),
            workDispatcher = Dispatchers.Unconfined,
        )
        viewModel.load(MAX_EDGE_PX)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        compose.setContent {
            KansoTheme(themeMode = mode) {
                ToolScreen(viewModel = viewModel, onBack = {}, onDone = {})
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(TOOL_IMAGE_TAG).assertExists()
        return viewModel
    }

    /** Saves a picture of the whole window as `src/test/screenshots/[name]`. */
    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name")
    }

    /** Values shared by the tests. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** The size the screen asks for: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3

        /** How far two pixels may differ (out of 255) and still count as the same. */
        const val TOLERANCE = 12f

        /** The longest a wait takes, in real milliseconds, before the test fails. */
        const val WAIT_TIMEOUT_MS = 10_000L

        /** Real milliseconds between two checks of a wait. */
        const val POLL_MS = 50L

        /** How many checks in a row must read the same picture before it counts as still. */
        const val STILL_READS = 4
    }
}

/**
 * The Robolectric screen the pictures are drawn on, the same as [ToolScreenshotTest]'s: 411 dp
 * wide, 891 dp tall, at xxhdpi.
 */
private const val STICKER_PHONE = "w411dp-h891dp-xxhdpi"
