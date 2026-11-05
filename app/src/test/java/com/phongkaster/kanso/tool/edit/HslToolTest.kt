package com.phongkaster.kanso.tool.edit

import android.app.Application
import android.graphics.Bitmap
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.HslAdjustments
import com.phongkaster.kanso.data.edit.edit.HslChannel
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks the HSL tool through the Edit screen's ViewModel, with the same rules as RGB and Adjust:
 *
 * 1. the first time HSL opens, Red and Hue are chosen; reopened, it shows the pair chosen last;
 * 2. choosing a range or a channel never changes the photo and adds nothing to Undo;
 * 3. one drag of the slider (previews, then let go) is one Undo step, and Redo brings it back;
 * 4. the HSL entry's "changed" dot is lit while any of the 27 values is away from 0, and only then,
 *    and no other tool's dot is lit by HSL;
 * 5. Compare keeps the HSL change and renders nothing; Back after an HSL change asks "Discard your
 *    changes?".
 *
 * The ViewModel is built by hand, as in `RgbToolTest`, on a generated photo; no screen is drawn.
 * (That the HSL sheet has no reset button is checked on the drawn sheet, in `HslSheetTest`.)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class HslToolTest {

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** The real Edit processor with no filters: the HSL tests choose none. */
    private val processorFor: (EditSettings) -> ToolProcessor = { settings ->
        EditProcessor(settings, FilterLut.Source({ null }))
    }

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** The first time HSL opens, the Red circle and the Hue chip are the chosen ones. */
    @Test
    fun red_and_hue_are_chosen_the_first_time() {
        val viewModel = loadedEditViewModel()

        assertEquals(ColourRange.RED, viewModel.state.value.hslRange)
        assertEquals(HslChannel.HUE, viewModel.state.value.hslChannel)
        viewModel.openTool(EditTool.HSL)
        assertEquals(EditTool.HSL, viewModel.state.value.openTool)
        assertEquals(ColourRange.RED, viewModel.state.value.hslRange)
        assertEquals(HslChannel.HUE, viewModel.state.value.hslChannel)
    }

    /** A chosen Navy blue and Lightness are still chosen after HSL is closed and opened again. */
    @Test
    fun the_last_range_and_channel_are_remembered() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.HSL)
        viewModel.chooseHslRange(ColourRange.NAVY_BLUE)
        viewModel.chooseHslChannel(HslChannel.LIGHTNESS)
        viewModel.openTool(EditTool.HSL) // tapping the open tool again closes its sheet
        assertNull(viewModel.state.value.openTool)
        viewModel.openTool(EditTool.HSL)

        assertEquals(EditTool.HSL, viewModel.state.value.openTool)
        assertEquals(ColourRange.NAVY_BLUE, viewModel.state.value.hslRange)
        assertEquals(HslChannel.LIGHTNESS, viewModel.state.value.hslChannel)
    }

    /** Choosing a range or a channel changes no setting and makes no Undo step. */
    @Test
    fun choosing_a_range_or_channel_changes_nothing_and_adds_no_undo_step() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.HSL)

        ColourRange.entries.forEach(action = { range -> viewModel.chooseHslRange(range) })
        HslChannel.entries.forEach(action = { channel -> viewModel.chooseHslChannel(channel) })
        runMainThread()

        assertEquals("a choice changed the photo", EditSettings(), viewModel.state.value.settings)
        assertFalse("a choice made an Undo step", viewModel.state.value.canUndo)
        assertTrue(viewModel.state.value.settings.isNeutral(EditTool.HSL))
    }

    /**
     * One drag of several steps, then the finger let go, is one Undo step: Undo takes back the whole
     * drag at once and Redo brings the final value back.
     */
    @Test
    fun one_drag_is_one_undo_step_and_redo_brings_it_back() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.HSL)

        drag(viewModel, hslWith(ColourRange.GREEN, HslChannel.SATURATION, 0.1f), hslWith(ColourRange.GREEN, HslChannel.SATURATION, 0.3f), hslWith(ColourRange.GREEN, HslChannel.SATURATION, 0.6f))
        val afterDrag = viewModel.state.value.settings
        assertEquals(0.6f, afterDrag.hsl[ColourRange.GREEN, HslChannel.SATURATION], 0f)

        viewModel.undo()
        runMainThread()
        assertEquals("one Undo took back only part of the drag", EditSettings(), viewModel.state.value.settings)
        assertFalse("the drag was more than one step", viewModel.state.value.canUndo)

        viewModel.redo()
        runMainThread()
        assertEquals(afterDrag, viewModel.state.value.settings)
    }

    /**
     * The HSL dot is lit while any of the 27 values is away from 0 and only then: for the first,
     * a middle and the last of the 27, and not for the 26 others; it goes out when the value
     * returns to 0; and Adjust, RGB and Filters stay unlit throughout.
     */
    @Test
    fun the_dot_is_lit_while_any_of_the_27_values_is_away_from_0_and_only_then() {
        listOf(
            Triple(ColourRange.RED, HslChannel.HUE, 0.5f),
            Triple(ColourRange.LIGHT_BLUE, HslChannel.SATURATION, -0.3f),
            Triple(ColourRange.PINK, HslChannel.LIGHTNESS, 1f),
        ).forEach(
            action = { (range, channel, value) ->
                val viewModel = loadedEditViewModel()
                viewModel.openTool(EditTool.HSL)
                assertTrue("a dot before any change", viewModel.state.value.settings.isNeutral(EditTool.HSL))

                drag(viewModel, hslWith(range, channel, value))
                val settings = viewModel.state.value.settings
                assertFalse("no dot with $range $channel at $value", settings.isNeutral(EditTool.HSL))
                EditTool.entries.filter(predicate = { it != EditTool.HSL }).forEach(
                    action = { other -> assertTrue("HSL lit the dot of $other", settings.isNeutral(other)) },
                )

                drag(viewModel, hslWith(range, channel, 0f))
                assertTrue("the dot stayed with $range $channel back at 0", viewModel.state.value.settings.isNeutral(EditTool.HSL))
            },
        )
    }

    /**
     * Compare keeps the HSL change and renders nothing in full while on, so the photo shown is the
     * one without the change; with Compare off again the change is still in the settings.
     */
    @Test
    fun compare_shows_the_photo_without_the_hsl_change() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.HSL)
        drag(viewModel, hslWith(ColourRange.RED, HslChannel.HUE, -0.6f))
        val edited = viewModel.state.value.settings

        viewModel.setComparing(true)
        runMainThread()
        assertTrue(viewModel.state.value.comparing)
        assertEquals("comparing lost the HSL change", edited, viewModel.state.value.settings)
        assertNull("comparing rendered the edit", viewModel.state.value.edited)

        viewModel.setComparing(false)
        runMainThread()
        assertFalse(viewModel.state.value.comparing)
        assertEquals(edited, viewModel.state.value.settings)
    }

    /**
     * Back first closes the HSL sheet; Back again, with only an HSL change made, asks "Discard your
     * changes?" instead of leaving.
     */
    @Test
    fun back_with_an_hsl_change_asks_to_discard() {
        val viewModel = loadedEditViewModel()
        viewModel.openTool(EditTool.HSL)
        drag(viewModel, hslWith(ColourRange.PURPLE, HslChannel.LIGHTNESS, 0.5f))

        assertTrue(viewModel.onBackPressed())
        assertNull(viewModel.state.value.openTool)
        assertTrue("Back left the screen with an HSL change", viewModel.onBackPressed())
        assertTrue(viewModel.state.value.discardPrompt)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Settings with every HSL value 0 except [channel] of [range], which is [value]. */
    private fun hslWith(range: ColourRange, channel: HslChannel, value: Float): EditSettings =
        EditSettings(hsl = HslAdjustments.NEUTRAL.with(range, channel, value))

    /**
     * One slider drag: each of [steps] handed over while the finger moves, then the finger let go,
     * as the HSL sheet's slider does.
     */
    private fun drag(viewModel: ToolViewModel, vararg steps: EditSettings) {
        steps.forEach(action = { step -> viewModel.previewSettings(step, processorFor) })
        viewModel.commitSettings()
        runMainThread()
    }

    /** Builds an Edit screen ViewModel for a generated photo by hand and loads it. */
    private fun loadedEditViewModel(): ToolViewModel {
        val photo: Bitmap = ToolFixtures.gradient(600, 400)
        val viewModel = ToolViewModel(
            tool = Tool.EDIT,
            photoUri = PHOTO_URI,
            loader = FakePhotoLoader(app, photo),
            probe = ViewModelClearedProbe(),
            workDispatcher = Dispatchers.Unconfined,
        )
        viewModel.load(MAX_EDGE_PX)
        runMainThread()
        assertTrue(viewModel.state.value.photo is ToolUiState.PhotoState.Ready)
        return viewModel
    }

    /** Runs everything waiting in the main thread's queue, where the ViewModel's jobs end. */
    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Values shared by the tests. */
    private companion object {
        /** The photo the ViewModel works on; the fake loader reads nothing from it. */
        const val PHOTO_URI = "content://media/external/images/media/7"

        /** The size the screen asks for: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3
    }
}
