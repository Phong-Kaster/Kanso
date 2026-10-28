package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.TextAlignment
import com.phongkaster.kanso.data.edit.edit.TextFont
import com.phongkaster.kanso.data.edit.edit.TextLayerLayout
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.model.editProcessorFor
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Proves the Text tool's rules in the ViewModel (Run 11, ADR-005, Android 15): opening Text adds
 * an empty text that is no undo step until it has words, an empty text is dropped when the
 * keyboard closes, a run of typing is one undo step, a text keeps its letter size while its words
 * and font change, the alignment cycles, a colour from the picker is one step, the size slider's
 * number round-trips, the 20-layer limit counts texts, and touching a layer opens the sheet of its
 * kind. The ViewModel uses the app's real fonts on a fake 600 x 400 photo.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class TextToolTest {

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

    /** Opening Text adds one empty, white, centred text, selected, keyboard up, and no undo step. */
    @Test
    fun opening_text_adds_an_empty_text_that_is_not_yet_an_action() {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.TEXT)

        val state = viewModel.state.value
        assertEquals(EditTool.TEXT, state.openTool)
        assertTrue("the keyboard was not asked for", state.textEditing)
        val text = state.settings.layers.single()
        assertEquals(text.key, state.selectedLayerKey)
        val content = text.content as LayerContent.Text
        assertEquals("", content.text)
        assertEquals(0xFFFFFFFF.toInt(), content.colorArgb)
        assertEquals(TextFont.NONE, content.font)
        assertEquals(TextAlignment.CENTRE, content.alignment)
        assertEquals("a new text starts at size 20", 20f, viewModel.selectedLayerSize()!!, 0.01f)
        assertFalse("an empty text made an undo step", state.canUndo)
    }

    /** Closing the keyboard on a text with no letters takes it off the photo, leaving no undo step. */
    @Test
    fun an_empty_text_is_dropped_when_the_keyboard_closes() {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.TEXT)
        viewModel.typeText("   ", processorFor)
        viewModel.setTextEditing(false)

        val state = viewModel.state.value
        assertTrue(state.settings.layers.isEmpty())
        assertNull(state.selectedLayerKey)
        assertFalse(state.canUndo)
    }

    /** A run of typing is ONE undo step together with the text's adding: Undo removes the whole text. */
    @Test
    fun a_run_of_typing_is_one_undo_step() {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.TEXT)
        "Happy birthday".indices.forEach { viewModel.typeText("Happy birthday".substring(0, it + 1), processorFor) }
        viewModel.setTextEditing(false)

        assertEquals("Happy birthday", textOf(viewModel.state.value.settings.layers.single()).text)
        assertTrue(viewModel.state.value.canUndo)
        viewModel.undo()
        assertTrue("one undo did not take the whole text back", viewModel.state.value.settings.layers.isEmpty())
        assertFalse(viewModel.state.value.canUndo)
    }

    /** More words make the text wider but keep its letter size (the size number does not move). */
    @Test
    fun typing_keeps_the_letter_size_and_widens_the_text() {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.TEXT)
        viewModel.typeText("Hi", processorFor)
        val short = viewModel.state.value.settings.layers.single()
        val shortSize = viewModel.selectedLayerSize()!!
        viewModel.typeText("Hi there", processorFor)
        val long = viewModel.state.value.settings.layers.single()

        assertTrue("more words did not widen the text", long.width > short.width)
        assertEquals("the letter size changed", shortSize, viewModel.selectedLayerSize()!!, 0.05f)
    }

    /** A font is one undo step on a text with words, and keeps the letter size. */
    @Test
    fun a_font_is_one_undo_step_and_keeps_the_size() {
        val viewModel = typedText("Xin chào")
        val size = viewModel.selectedLayerSize()!!
        val before = viewModel.state.value.settings

        viewModel.chooseTextFont(TextFont.SPACE_MONO, processorFor)
        assertEquals(TextFont.SPACE_MONO, textOf(viewModel.state.value.settings.layers.single()).font)
        assertEquals(size, viewModel.selectedLayerSize()!!, 0.05f)

        viewModel.undo()
        assertEquals(before, viewModel.state.value.settings)
    }

    /** The alignment button cycles centre, left, right and back to centre. */
    @Test
    fun the_alignment_cycles_centre_left_right() {
        val viewModel = typedText("Hello")
        val seen = (1..3).map {
            viewModel.cycleTextAlignment(processorFor)
            textOf(viewModel.state.value.settings.layers.single()).alignment
        }
        assertEquals(listOf(TextAlignment.LEFT, TextAlignment.RIGHT, TextAlignment.CENTRE), seen)
    }

    /** A drag in the colour picker is previewed, and its release (the back arrow) is one undo step. */
    @Test
    fun a_custom_colour_drag_is_one_undo_step() {
        val viewModel = typedText("Hello")
        val before = viewModel.state.value.settings
        listOf(0xFF102030.toInt(), 0x00405060, 0xFF708090.toInt()).forEach {
            viewModel.chooseTextColour(it, processorFor, preview = true)
        }
        viewModel.commitSettings()
        assertEquals(0xFF708090.toInt(), textOf(viewModel.state.value.settings.layers.single()).colorArgb)

        viewModel.undo()
        assertEquals(before, viewModel.state.value.settings)
    }

    /** The size slider at 50 gives a text whose number reads back as 50. */
    @Test
    fun the_size_slider_round_trips_for_a_text() {
        val viewModel = typedText("Hello")
        viewModel.previewLayerSize(50f, processorFor)
        viewModel.commitSettings()
        assertEquals(50f, viewModel.selectedLayerSize()!!, 0.05f)
    }

    /** With 20 layers already on the photo, opening Text adds nothing and shows the limit message. */
    @Test
    fun the_layer_limit_counts_texts_and_stickers_together() {
        val viewModel = loadedViewModel()
        val full = (1..EditSettings.MAX_LAYERS).map { sticker(it.toLong()) }
        viewModel.changeSettings(EditSettings(layers = full), processorFor)
        viewModel.openTool(EditTool.TEXT)

        assertEquals(EditSettings.MAX_LAYERS, viewModel.state.value.settings.layers.size)
        assertTrue(viewModel.state.value.layerLimitShown)
        assertNull(viewModel.state.value.selectedLayerKey)
    }

    /** Touching a text while the Stickers sheet is open switches to the Text sheet with it selected. */
    @Test
    fun touching_a_text_from_the_stickers_sheet_opens_the_text_sheet() {
        val viewModel = typedText("Hello")
        val textKey = viewModel.state.value.settings.layers.single().key
        viewModel.confirmLayers()
        viewModel.changeSettings(
            viewModel.state.value.settings.copy(layers = viewModel.state.value.settings.layers + sticker(500L)),
            processorFor,
        )
        viewModel.openTool(EditTool.STICKERS)
        viewModel.selectLayer(500L)
        assertEquals(EditTool.STICKERS, viewModel.state.value.openTool)

        viewModel.selectLayer(textKey)
        assertEquals(EditTool.TEXT, viewModel.state.value.openTool)
        assertEquals(textKey, viewModel.state.value.selectedLayerKey)
        assertFalse("selecting opened the keyboard", viewModel.state.value.textEditing)
    }

    /** The frame's edit button selects the text and opens the keyboard on it. */
    @Test
    fun the_edit_button_opens_the_keyboard_on_the_text() {
        val viewModel = typedText("Hello")
        val key = viewModel.state.value.settings.layers.single().key
        viewModel.editText(key)
        assertTrue(viewModel.state.value.textEditing)
        assertEquals(key, viewModel.state.value.selectedLayerKey)
    }

    /** Cancel puts the photo back as it was before the sheet opened: the typed text is gone. */
    @Test
    fun cancel_throws_the_texts_away() {
        val viewModel = typedText("Hello")
        viewModel.cancelLayers()
        assertTrue(viewModel.state.value.settings.layers.isEmpty())
        assertNull(viewModel.state.value.openTool)
        assertFalse(viewModel.state.value.canUndo)
    }

    /** A text longer than 200 characters is cut to 200. */
    @Test
    fun a_text_is_cut_at_200_characters() {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.TEXT)
        viewModel.typeText("a".repeat(250), processorFor)
        assertEquals(LayerContent.Text.MAX_LENGTH, textOf(viewModel.state.value.settings.layers.single()).text.length)
    }

    /** The Edit ViewModel with the Text sheet open and [words] typed, the keyboard closed. */
    private fun typedText(words: String): ToolViewModel {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.TEXT)
        viewModel.typeText(words, processorFor)
        viewModel.setTextEditing(false)
        assertNotNull(viewModel.state.value.selectedLayerKey)
        return viewModel
    }

    /** A sticker layer with [key] in the middle of the photo. */
    private fun sticker(key: Long): Layer =
        Layer(key, LayerContent.Picture("img_sticker_apple"), 0.5f, 0.5f, 0.2f, 0f, false)

    /** The text content of [layer]. */
    private fun textOf(layer: Layer): LayerContent.Text = layer.content as LayerContent.Text

    /** The Edit screen's ViewModel on the fake photo, with the app's real fonts, photo decoded. */
    private fun loadedViewModel(): ToolViewModel {
        val viewModel = ToolViewModel(
            Tool.EDIT,
            PHOTO_URI,
            FakePhotoLoader(context, ToolFixtures.gradient(600, 400)),
            ViewModelClearedProbe(),
            workDispatcher = Dispatchers.Unconfined,
            textLayout = TextLayerLayout.forApp(context),
        )
        viewModel.load(MAX_EDGE_PX)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        return viewModel
    }

    /** Values shared by the tests. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** The size the screen asks for. */
        const val MAX_EDGE_PX = 2000
    }
}
