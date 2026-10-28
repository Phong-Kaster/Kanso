package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.AppLayerPictures
import com.phongkaster.kanso.data.edit.edit.DateFormat
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.TextFont
import com.phongkaster.kanso.data.edit.edit.TextLayerLayout
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.model.editProcessorFor
import java.time.LocalDateTime
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
 * Proves the Date tool's rules in the ViewModel (ADR-006, Android 15): opening Date sets the next
 * moment to now (to the minute) and adds nothing; a format chip adds a white date at size 20 as
 * one undo step, or rewrites the selected date keeping its letter size; the pickers change the
 * selected date or, with none, the next one; a tap on the selected date moves it to the next
 * format and wraps round; a date counts towards the 20 layers; touching a date opens the Date
 * sheet; and the app draws a date as one line of words. The ViewModel uses the app's real fonts
 * on a fake 600 x 400 photo, and a clock fixed at 28 October 2026, 14:30:45.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class DateToolTest {

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

    /** Opening Date sets the next moment to now without its seconds, adds nothing, makes no step. */
    @Test
    fun opening_date_sets_the_next_moment_to_now_and_adds_nothing() {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.DATE)

        val state = viewModel.state.value
        assertEquals(EditTool.DATE, state.openTool)
        assertEquals(LocalDateTime.of(2026, 10, 28, 14, 30), state.dateMoment)
        assertTrue(state.settings.layers.isEmpty())
        assertFalse(state.canUndo)
        assertTrue(state.settings.isNeutral(EditTool.DATE))
    }

    /** A chip with nothing selected adds one selected date: white, phone's font, size 20, one step. */
    @Test
    fun a_chip_adds_a_white_date_at_size_20_as_one_step() {
        val viewModel = dateSheetOpen()
        viewModel.chooseDateFormat(DateFormat.YEAR, processorFor)

        val state = viewModel.state.value
        val layer = state.settings.layers.single()
        assertEquals(layer.key, state.selectedLayerKey)
        val date = dateOf(layer)
        assertEquals(DateFormat.YEAR, date.format)
        assertEquals("2026", date.words)
        assertEquals(0xFFFFFFFF.toInt(), date.colorArgb)
        assertEquals(TextFont.NONE, date.font)
        assertEquals(1f, layer.opacity)
        assertFalse(layer.mirrored)
        assertEquals("a new date starts at size 20", 20f, viewModel.selectedLayerSize()!!, 0.01f)
        assertFalse(state.settings.isNeutral(EditTool.DATE))

        assertTrue("adding a date made no undo step", state.canUndo)
        viewModel.undo()
        assertTrue(viewModel.state.value.settings.layers.isEmpty())
    }

    /** A chip with a date selected rewrites it: a longer format, a wider layer, the same letter size. */
    @Test
    fun a_chip_rewrites_the_selected_date_keeping_its_letter_size() {
        val viewModel = dateSheetOpen()
        viewModel.chooseDateFormat(DateFormat.YEAR, processorFor)
        val before = viewModel.state.value.settings.layers.single()

        viewModel.chooseDateFormat(DateFormat.WEEKDAY_FULL, processorFor)

        val after = viewModel.state.value.settings.layers.single()
        assertEquals("no second date was added", before.key, after.key)
        assertEquals("Wed, 28 October 2026", dateOf(after).words)
        assertTrue("the longer words did not widen the layer", after.width > before.width * 3f)
        assertEquals("the letters changed size", 20f, viewModel.selectedLayerSize()!!, 0.01f)
        viewModel.undo()
        assertEquals(DateFormat.YEAR, dateOf(viewModel.state.value.settings.layers.single()).format)
    }

    /** With no date selected the pickers change only the next moment, which the next date takes. */
    @Test
    fun the_pickers_without_a_selection_set_the_next_date_moment() {
        val viewModel = dateSheetOpen()
        val picked = LocalDateTime.of(2024, 2, 29, 7, 15, 59)
        viewModel.chooseDateMoment(picked, processorFor)

        assertEquals(LocalDateTime.of(2024, 2, 29, 7, 15), viewModel.state.value.dateMoment)
        assertTrue("picking a moment added a layer", viewModel.state.value.settings.layers.isEmpty())
        assertFalse(viewModel.state.value.canUndo)

        viewModel.chooseDateFormat(DateFormat.WEEKDAY_TIME_12, processorFor)
        assertEquals("Thursday · 7:15 AM", dateOf(viewModel.state.value.settings.layers.single()).words)
    }

    /** With a date selected the pickers change that date, as one undo step. */
    @Test
    fun the_pickers_with_a_selection_change_that_date() {
        val viewModel = dateSheetOpen()
        viewModel.chooseDateFormat(DateFormat.NUMERIC_TIME_24, processorFor)
        viewModel.chooseDateMoment(LocalDateTime.of(2025, 12, 31, 23, 59), processorFor)

        assertEquals("31/12/2025 23:59", dateOf(viewModel.state.value.settings.layers.single()).words)
        viewModel.undo()
        assertEquals("28/10/2026 14:30", dateOf(viewModel.state.value.settings.layers.single()).words)
    }

    /** A tap on the selected date moves it to the next format; after the last comes the first. */
    @Test
    fun a_tap_on_the_selected_date_moves_it_to_the_next_format() {
        val viewModel = dateSheetOpen()
        viewModel.chooseDateFormat(DateFormat.WEEKDAY_FULL, processorFor)
        val key = viewModel.state.value.selectedLayerKey!!

        viewModel.tapLayer(key, processorFor)
        assertEquals(DateFormat.WEEKDAY_ORDINAL, dateOf(viewModel.state.value.settings.layers.single()).format)

        viewModel.chooseDateFormat(DateFormat.entries.last(), processorFor)
        viewModel.tapLayer(key, processorFor)
        assertEquals(DateFormat.entries.first(), dateOf(viewModel.state.value.settings.layers.single()).format)

        viewModel.undo()
        assertEquals(DateFormat.entries.last(), dateOf(viewModel.state.value.settings.layers.single()).format)
    }

    /** A tap on a sticker changes nothing: only a date cycles. */
    @Test
    fun a_tap_on_a_sticker_changes_nothing() {
        val viewModel = loadedViewModel()
        viewModel.changeSettings(EditSettings(layers = listOf(sticker(5L))), processorFor)
        viewModel.selectLayer(5L)
        val before = viewModel.state.value.settings

        viewModel.tapLayer(5L, processorFor)
        assertEquals(before, viewModel.state.value.settings)
    }

    /** At 20 layers a chip adds nothing and asks for the limit message. */
    @Test
    fun a_date_counts_towards_the_twenty_layers() {
        val viewModel = loadedViewModel()
        viewModel.changeSettings(
            EditSettings(layers = (1L..EditSettings.MAX_LAYERS).map { sticker(it) }),
            processorFor,
        )
        viewModel.openTool(EditTool.DATE)
        viewModel.chooseDateFormat(DateFormat.YEAR, processorFor)

        assertEquals(EditSettings.MAX_LAYERS, viewModel.state.value.settings.layers.size)
        assertTrue(viewModel.state.value.layerLimitShown)
    }

    /** Touching a date with the Stickers sheet open opens the Date sheet with it selected. */
    @Test
    fun touching_a_date_opens_the_date_sheet() {
        val viewModel = dateSheetOpen()
        viewModel.chooseDateFormat(DateFormat.YEAR, processorFor)
        val key = viewModel.state.value.selectedLayerKey!!
        viewModel.confirmLayers()
        viewModel.openTool(EditTool.STICKERS)
        assertNull(viewModel.state.value.selectedLayerKey)

        viewModel.selectLayer(key)
        assertEquals(EditTool.DATE, viewModel.state.value.openTool)
        assertEquals(key, viewModel.state.value.selectedLayerKey)
    }

    /** The app draws a date as one line of its words: a picture much wider than tall. */
    @Test
    fun the_app_draws_a_date_as_one_line_of_words() {
        val date = LayerContent.Date(
            moment = LocalDateTime.of(2026, 10, 28, 14, 30),
            format = DateFormat.DAY_MONTH_YEAR_TIME_12,
            colorArgb = 0xFFFFFFFF.toInt(),
            font = TextFont.NONE,
        )
        val picture = AppLayerPictures(context).picture(date, 800)
        assertNotNull("a date drew nothing", picture)
        assertTrue("the date wrapped onto a second line", picture!!.width > picture.height * 6)
        // A date is drawn exactly as the one-line text of its words.
        val block = TextLayerLayout.forApp(context).measure(date.asText())
        assertEquals(1f / block.aspect, picture.width.toFloat() / picture.height, 0.1f)
    }

    /**
     * The Background row: a ready colour puts a solid box behind the words and None takes it away,
     * one undo step each; the letters keep their colour and their size while the layer grows by
     * the box's margin. A see-through colour from the picker keeps its alpha.
     */
    @Test
    fun the_background_row_sets_and_clears_a_box() {
        val viewModel = dateSheetOpen()
        viewModel.chooseDateFormat(DateFormat.YEAR, processorFor)
        val bare = viewModel.state.value.settings.layers.single()

        val yellow = 0xFFFDD835.toInt()
        viewModel.chooseDateBackground(yellow, processorFor)
        val boxed = viewModel.state.value.settings.layers.single()
        assertEquals(yellow, dateOf(boxed).boxArgb)
        assertEquals("the letters changed colour", 0xFFFFFFFF.toInt(), dateOf(boxed).colorArgb)
        assertTrue("the box added no margin", boxed.width > bare.width)
        assertEquals("the letters changed size", 20f, viewModel.selectedLayerSize()!!, 0.01f)

        val seeThrough = 0x801E88E5.toInt()
        viewModel.chooseDateBackground(seeThrough, processorFor, preview = true)
        viewModel.commitSettings()
        assertEquals("the picker's alpha was lost", seeThrough, dateOf(viewModel.state.value.settings.layers.single()).boxArgb)

        viewModel.chooseDateBackground(null, processorFor)
        val again = viewModel.state.value.settings.layers.single()
        assertNull(dateOf(again).boxArgb)
        assertEquals("back to no box, back to the first width", bare.width, again.width, 1e-5f)

        viewModel.undo()
        assertEquals(seeThrough, dateOf(viewModel.state.value.settings.layers.single()).boxArgb)
    }

    /** A date draws through its text with its own letters' colour and its box. */
    @Test
    fun a_date_draws_with_its_letters_colour_and_its_box() {
        val red = 0xFFE53935.toInt()
        val date = LayerContent.Date(LocalDateTime.of(2026, 10, 28, 14, 30), DateFormat.YEAR, red, TextFont.NONE)
        assertEquals(red, date.asText().colorArgb)
        assertNull(date.asText().boxArgb)
        val boxed = date.copy(boxArgb = 0xFF000000.toInt()).asText()
        assertEquals(red, boxed.colorArgb)
        assertEquals(0xFF000000.toInt(), boxed.boxArgb)
    }

    /** A solid box is drawn: the picture's left edge, beside the words, is the box's colour. */
    @Test
    fun the_app_draws_the_box_behind_the_words() {
        val blue = 0xFF1E88E5.toInt()
        val date = LayerContent.Date(LocalDateTime.of(2026, 10, 28, 14, 30), DateFormat.YEAR, 0xFFFFFFFF.toInt(), TextFont.NONE)
        val pictures = AppLayerPictures(context)

        val boxed = pictures.picture(date.copy(boxArgb = blue), 400)!!
        // A point in the left margin, halfway down: inside the box, clear of the letters and corners.
        val inMargin = boxed.getPixel((boxed.width * 0.04f).toInt(), boxed.height / 2)
        assertEquals("the box is not blue there", blue, inMargin)

        val bare = pictures.picture(date, 400)!!
        assertEquals("no box, yet the margin is painted", 0, bare.getPixel(1, bare.height / 2) ushr 24)
    }

    /** The Text sheet's Background row puts a box behind a text too, keeping its letter size. */
    @Test
    fun a_text_takes_a_box_from_the_background_row() {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.TEXT)
        viewModel.typeText("Xin chào", processorFor)
        viewModel.setTextEditing(false)
        val size = viewModel.selectedLayerSize()!!

        viewModel.chooseTextBackground(0xFF43A047.toInt(), processorFor)
        val text = viewModel.state.value.settings.layers.single().content as LayerContent.Text
        assertEquals(0xFF43A047.toInt(), text.boxArgb)
        assertEquals("the letters changed size", size, viewModel.selectedLayerSize()!!, 0.01f)
        viewModel.undo()
        assertNull((viewModel.state.value.settings.layers.single().content as LayerContent.Text).boxArgb)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** A loaded ViewModel with the Date sheet open and nothing on the photo. */
    private fun dateSheetOpen(): ToolViewModel {
        val viewModel = loadedViewModel()
        viewModel.openTool(EditTool.DATE)
        return viewModel
    }

    /** A sticker layer with [key] in the middle of the photo. */
    private fun sticker(key: Long): Layer =
        Layer(key, LayerContent.Picture("img_sticker_apple"), 0.5f, 0.5f, 0.2f, 0f, false)

    /** The date content of [layer]. */
    private fun dateOf(layer: Layer): LayerContent.Date = layer.content as LayerContent.Date

    /** The Edit screen's ViewModel on the fake photo, real fonts, a fixed clock, photo decoded. */
    private fun loadedViewModel(): ToolViewModel {
        val viewModel = ToolViewModel(
            Tool.EDIT,
            PHOTO_URI,
            FakePhotoLoader(context, ToolFixtures.gradient(600, 400)),
            ViewModelClearedProbe(),
            workDispatcher = Dispatchers.Unconfined,
            textLayout = TextLayerLayout.forApp(context),
            clock = { NOW },
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

        /** The fixed clock: 28 October 2026, 14:30:45 (the seconds are dropped). */
        val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 28, 14, 30, 45)
    }
}
