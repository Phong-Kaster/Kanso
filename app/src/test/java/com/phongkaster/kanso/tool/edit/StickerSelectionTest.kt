package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.LayerMapping
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.data.edit.edit.pictureId
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.TOOL_UNDO_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_DELETE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_DUPLICATE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_FLIP_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_OVERLAY_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_RESIZE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.angleBetween
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.distanceBetween
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.layerNodeTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.STICKERS_CANCEL_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.STICKERS_CONFIRM_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.stickerTileTag
import com.phongkaster.kanso.ui.tool.model.editProcessorFor
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
 * Proves which sticker is selected and what selecting does (Run 10, Android 15): the hit rules
 * for overlapping layers (DoD 10), layers that are inert once the sheet is closed (DoD 11),
 * one undo step per action through the screen's Undo (DoD 12), selection that is not an edit and
 * not part of the settings (DoD 12, 21). The Compose tests draw the real `ToolScreen` on a fake
 * 600 x 400 photo; the ViewModel tests need no screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StickerSelectionTest {

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

    // --- DoD 10: which sticker a tap selects --------------------------------------------------

    /** A tap where only the newer of two overlapping layers lies selects the newer, changing no setting. */
    @Test
    fun a_tap_where_only_the_newer_sticker_lies_selects_it() {
        val viewModel = showOverlapping(select = 100L)
        val start = viewModel.state.value.settings

        tapAt(0.70f, 0.5f)

        assertEquals(101L, viewModel.state.value.selectedLayerKey)
        assertEquals(start, viewModel.state.value.settings)
    }

    /** A tap on the overlap of two layers selects the newer one (the one drawn on top). */
    @Test
    fun a_tap_on_the_overlap_selects_the_newer_sticker() {
        val viewModel = showOverlapping(select = 100L)
        val start = viewModel.state.value.settings

        tapAt(0.45f, 0.5f)

        assertEquals(101L, viewModel.state.value.selectedLayerKey)
        assertEquals(start, viewModel.state.value.settings)
    }

    /** The sticker nodes of the overlay come in the layers' own order, oldest first. */
    @Test
    fun the_sticker_nodes_are_in_semantics_order_oldest_first() {
        showOverlapping(select = 100L)

        val overlay = compose.onNodeWithTag(LAYER_OVERLAY_TAG).fetchSemanticsNode()
        val order = overlay.children
            .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
            .filter { it == layerNodeTag(100L) || it == layerNodeTag(101L) }
        assertEquals(listOf(layerNodeTag(100L), layerNodeTag(101L)), order)
    }

    /** A tap on empty photo selects none and changes no setting. */
    @Test
    fun a_tap_on_empty_photo_selects_none() {
        val viewModel = showOverlapping(select = 100L)
        val start = viewModel.state.value.settings

        tapAt(0.05f, 0.05f)

        assertNull(viewModel.state.value.selectedLayerKey)
        assertEquals(start, viewModel.state.value.settings)
    }

    /** A tap on the selected sticker keeps it selected and leaves every picture and setting as they were. */
    @Test
    fun a_tap_on_the_selected_sticker_changes_nothing() {
        val viewModel = showOverlapping(select = 101L)
        val start = viewModel.state.value.settings

        tapAt(0.55f, 0.5f)

        val state = viewModel.state.value
        assertEquals(101L, state.selectedLayerKey)
        assertEquals(start, state.settings)
        assertEquals(start.layers.map { it.pictureId }, state.settings.layers.map { it.pictureId })
    }

    // --- DoD 11: inert once the sheet is closed -------------------------------------------------

    /**
     * A sticker placed and the sheet closed with the tick, then Filters opened: a tap and a swipe
     * on the sticker's shown place leave the settings and the undo history unchanged, select
     * nothing and draw no frame.
     */
    @Test
    fun with_filters_open_a_closed_sticker_takes_no_touch() {
        placeThenCloseAndTouch(openFilters = true)
    }

    /** The same with no tool open at all. */
    @Test
    fun with_no_tool_open_a_closed_sticker_takes_no_touch() {
        placeThenCloseAndTouch(openFilters = false)
    }

    /** With a sticker selected, the tick closes the sheet and leaves none selected. */
    @Test
    fun confirm_leaves_no_sticker_selected() {
        val viewModel = showEdit()
        openStickers(viewModel)
        tapTile(0)
        assertNotNull("the added sticker was not selected", viewModel.state.value.selectedLayerKey)

        compose.onNodeWithTag(STICKERS_CONFIRM_TAG).performClick()
        compose.waitForIdle()

        assertNull(viewModel.state.value.selectedLayerKey)
        assertEquals(1, viewModel.state.value.settings.layers.size)
    }

    /** With a sticker selected, the cross closes the sheet and leaves none selected. */
    @Test
    fun cancel_leaves_no_sticker_selected() {
        val viewModel = showEdit()
        openStickers(viewModel)
        tapTile(0)
        assertNotNull("the added sticker was not selected", viewModel.state.value.selectedLayerKey)

        compose.onNodeWithTag(STICKERS_CANCEL_TAG).performClick()
        compose.waitForIdle()

        assertNull(viewModel.state.value.selectedLayerKey)
        assertTrue("Cancel kept the sticker", viewModel.state.value.settings.layers.isEmpty())
    }

    // --- DoD 12: one undo step per action -------------------------------------------------------

    /**
     * With the sheet open: add a sticker, turn it with the handle, flip it, resize it with the
     * handle, then tap the screen's Undo four times. Each Undo gives the settings from before that
     * action, and the fourth leaves no sticker.
     */
    @Test
    fun four_actions_are_undone_one_by_one_with_the_sheet_open() {
        addTurnFlipResizeThenUndoFourTimes(confirmFirst = false)
    }

    /** The same four actions, with the sheet closed by the tick before the four Undo taps. */
    @Test
    fun four_actions_are_undone_one_by_one_after_the_tick() {
        addTurnFlipResizeThenUndoFourTimes(confirmFirst = true)
    }

    /**
     * Selecting a sticker, selecting another, selecting none and naming a key that does not exist
     * change neither the settings nor Undo: one Undo afterwards still goes back over the add only.
     */
    @Test
    fun selecting_and_deselecting_change_neither_settings_nor_undo() {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.STICKERS)
        viewModel.addSticker(StickerCatalog.inCategory(StickerCategory.FOOD).first(), processorFor)
        val before = viewModel.state.value.settings
        assertTrue(viewModel.state.value.canUndo)

        viewModel.selectLayer(null)
        assertEquals(before, viewModel.state.value.settings)
        assertTrue(viewModel.state.value.canUndo)
        viewModel.selectLayer(before.layers.single().key)
        viewModel.selectLayer(9999L)
        assertNull("an unknown key selected something", viewModel.state.value.selectedLayerKey)
        viewModel.selectLayer(before.layers.single().key)
        assertEquals(before, viewModel.state.value.settings)
        assertTrue(viewModel.state.value.canUndo)

        viewModel.undo()
        assertEquals(EditSettings(), viewModel.state.value.settings)
        assertFalse(viewModel.state.value.canUndo)
    }

    /**
     * One whole drag, 50 previews and one commit, is exactly one undo entry: one Undo puts the
     * sticker back where it was before the drag (and the add below it is still there), a second
     * Undo takes the sticker away.
     */
    @Test
    fun one_whole_drag_is_exactly_one_undo_entry() {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.STICKERS)
        viewModel.addSticker(StickerCatalog.inCategory(StickerCategory.FOOD).first(), processorFor)
        val beforeDrag = viewModel.state.value.settings
        val sticker = beforeDrag.layers.single()

        for (move in 1..50) {
            viewModel.previewLayers(listOf(sticker.copy(centreX = sticker.centreX + move * 0.002f)), processorFor)
        }
        viewModel.commitSettings()
        assertTrue("the drag did not move the sticker", viewModel.state.value.settings != beforeDrag)

        viewModel.undo()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals("the first Undo", beforeDrag, viewModel.state.value.settings)
        assertTrue(viewModel.state.value.canUndo)
        viewModel.undo()
        assertTrue("the second Undo", viewModel.state.value.settings.layers.isEmpty())
        assertFalse(viewModel.state.value.canUndo)
    }

    // --- DoD 21: selection is the screen's, not the photo's -------------------------------------

    /** After Undo takes back the add of the selected sticker, nothing is selected. */
    @Test
    fun undoing_the_add_of_the_selected_sticker_selects_none() {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.STICKERS)
        viewModel.addSticker(StickerCatalog.inCategory(StickerCategory.FOOD).first(), processorFor)
        assertNotNull(viewModel.state.value.selectedLayerKey)

        viewModel.undo()

        assertTrue(viewModel.state.value.settings.layers.isEmpty())
        assertNull(viewModel.state.value.selectedLayerKey)
    }

    /** Neither EditSettings nor Layer has a field about selection: it is the screen's state only. */
    @Test
    fun the_settings_and_the_sticker_have_no_selection_field() {
        listOf(EditSettings::class.java, Layer::class.java).forEach { type ->
            val names = type.declaredFields.map { it.name }
            names.forEach { name ->
                assertFalse("${type.simpleName} has a field called $name", name.contains("select", ignoreCase = true))
            }
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Places a sticker through the grid, closes the sheet with the tick, optionally opens Filters,
     * then taps and swipes on the sticker's shown place. Settings, Undo and the selection stay
     * as they were and no frame button is drawn; one Undo then goes back over the add only.
     */
    private fun placeThenCloseAndTouch(openFilters: Boolean) {
        val viewModel = showEdit()
        openStickers(viewModel)
        tapTile(0)
        compose.onNodeWithTag(STICKERS_CONFIRM_TAG).performClick()
        compose.waitForIdle()
        if (openFilters) {
            compose.onNodeWithTag(editToolTag(EditTool.FILTERS)).performClick()
            compose.waitForIdle()
            assertEquals(EditTool.FILTERS, viewModel.state.value.openTool)
        } else {
            assertNull(viewModel.state.value.openTool)
        }
        val settings = viewModel.state.value.settings
        val canUndo = viewModel.state.value.canUndo
        assertEquals(1, settings.layers.size)
        val centre = shownCentre(viewModel, settings.layers.single())

        compose.onNodeWithTag(LAYER_OVERLAY_TAG).performTouchInput { click(centre) }
        compose.waitForIdle()
        compose.onNodeWithTag(LAYER_OVERLAY_TAG).performTouchInput { swipe(centre, centre + Offset(80f, 50f), durationMillis = 300) }
        compose.waitForIdle()

        val state = viewModel.state.value
        assertEquals("a touch changed the settings", settings, state.settings)
        assertEquals("a touch changed Undo", canUndo, state.canUndo)
        assertNull("a touch selected a sticker", state.selectedLayerKey)
        listOf(LAYER_DELETE_TAG, LAYER_DUPLICATE_TAG, LAYER_FLIP_TAG, LAYER_RESIZE_TAG).forEach {
            compose.onAllNodesWithTag(it).assertCountEquals(0)
        }
        viewModel.undo()
        assertEquals("the history held more than the add", EditSettings(), viewModel.state.value.settings)
        assertFalse(viewModel.state.value.canUndo)
    }

    /** Runs the add, turn, flip, resize sequence and the four Undo taps (see the two tests that call it). */
    private fun addTurnFlipResizeThenUndoFourTimes(confirmFirst: Boolean) {
        val viewModel = showEdit()
        openStickers(viewModel)
        val s0 = viewModel.state.value.settings

        tapTile(0)
        val s1 = viewModel.state.value.settings
        assertEquals(1, s1.layers.size)

        dragHandle(viewModel, turnDegrees = 40f, scale = 1f)
        val s2 = viewModel.state.value.settings
        assertTrue("the turn changed nothing", s2 != s1)

        compose.onNodeWithTag(LAYER_FLIP_TAG).performClick()
        compose.waitForIdle()
        val s3 = viewModel.state.value.settings
        assertTrue("the flip changed nothing", s3 != s2)

        dragHandle(viewModel, turnDegrees = 0f, scale = 1.5f)
        val s4 = viewModel.state.value.settings
        assertTrue("the resize changed nothing", s4 != s3)

        if (confirmFirst) {
            compose.onNodeWithTag(STICKERS_CONFIRM_TAG).performClick()
            compose.waitForIdle()
            assertNull(viewModel.state.value.openTool)
            assertEquals(s4, viewModel.state.value.settings)
        }

        listOf(s3, s2, s1, s0).forEachIndexed { index, expected ->
            compose.onNodeWithTag(TOOL_UNDO_TAG).performClick()
            compose.waitForIdle()
            assertEquals("Undo number ${index + 1}", expected, viewModel.state.value.settings)
        }
        assertTrue("the fourth Undo left a sticker", viewModel.state.value.settings.layers.isEmpty())
    }

    /** Moves the finger on the resize handle so its distance from the centre is times [scale] and its direction turns [turnDegrees]. */
    private fun dragHandle(viewModel: ToolViewModel, turnDegrees: Float, scale: Float) {
        val origin = compose.onNodeWithTag(LAYER_OVERLAY_TAG).fetchSemanticsNode().positionInRoot
        val handleRoot = compose.onNodeWithTag(LAYER_RESIZE_TAG).fetchSemanticsNode().boundsInRoot.center
        val handle = Offset(handleRoot.x - origin.x, handleRoot.y - origin.y)
        val centre = shownCentre(viewModel, viewModel.state.value.settings.layers.single())
        val d = distanceBetween(centre.x, centre.y, handle.x, handle.y)
        val a = angleBetween(centre.x, centre.y, handle.x, handle.y)
        compose.onNodeWithTag(LAYER_OVERLAY_TAG).performTouchInput {
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

    /** The [sticker]'s centre on the shown picture, in the overlay's own pixels. */
    private fun shownCentre(viewModel: ToolViewModel, sticker: Layer): Offset {
        val state = viewModel.state.value
        val photo = (state.photo as ToolUiState.PhotoState.Ready).bitmap
        val p = LayerMapping(photo.width, photo.height, state.settings)
            .originalToShown(LayerMapping.Point(sticker.centreX, sticker.centreY))
        val size = compose.onNodeWithTag(LAYER_OVERLAY_TAG).fetchSemanticsNode().size
        return Offset(p.x * size.width, p.y * size.height)
    }

    /** One tap on the overlay at ([x], [y]) given as fractions of the shown picture. */
    private fun tapAt(x: Float, y: Float) {
        val size = compose.onNodeWithTag(LAYER_OVERLAY_TAG).fetchSemanticsNode().size
        compose.onNodeWithTag(LAYER_OVERLAY_TAG).performTouchInput { click(Offset(x * size.width, y * size.height)) }
        compose.waitForIdle()
    }

    /**
     * Two layers 0.4 wide, the older (key 100) with its centre at 0.35 across and the newer (key
     * 101) at 0.55, so they overlap between 0.35 and 0.55, with the sheet open and [select] selected.
     */
    private fun showOverlapping(select: Long): ToolViewModel {
        val viewModel = showEdit()
        val older = Layer(100L, LayerContent.Picture("img_sticker_apple"), 0.35f, 0.5f, 0.4f, 0f, false)
        val newer = Layer(101L, LayerContent.Picture("img_sticker_apple"), 0.55f, 0.5f, 0.4f, 0f, false)
        viewModel.changeSettings(EditSettings(layers = listOf(older, newer)), processorFor)
        compose.waitForIdle()
        openStickers(viewModel)
        viewModel.selectLayer(select)
        compose.waitForIdle()
        assertEquals(select, viewModel.state.value.selectedLayerKey)
        return viewModel
    }

    /** Taps the Stickers entry and waits until the sheet is open. */
    private fun openStickers(viewModel: ToolViewModel) {
        compose.onNodeWithTag(editToolTag(EditTool.STICKERS)).performClick()
        compose.waitForIdle()
        assertEquals(EditTool.STICKERS, viewModel.state.value.openTool)
    }

    /** Taps the Food grid's tile number [index] (0 is the first). */
    private fun tapTile(index: Int) {
        val id = StickerCatalog.inCategory(StickerCategory.FOOD)[index].pictureId
        compose.onNodeWithTag(stickerTileTag(id)).performClick()
        compose.waitForIdle()
    }

    /** Builds the Edit screen's ViewModel on the fake photo and decodes the photo. */
    private fun loadedViewModel(): ToolViewModel {
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
        return viewModel
    }

    /** Draws the real tool screen for a decoded photo and returns its ViewModel; no sheet is open yet. */
    private fun showEdit(): ToolViewModel {
        val viewModel = loadedViewModel()
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
    }
}
