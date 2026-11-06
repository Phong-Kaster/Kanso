package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.phongkaster.kanso.ui.tool.TOOL_CARD_TAG
import com.phongkaster.kanso.ui.tool.component.editbottombar.EDIT_SHEET_TAG
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.TextFont
import com.phongkaster.kanso.data.edit.edit.TextLayerLayout
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_DELETE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_DUPLICATE_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_EDIT_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_FLIP_TAG
import com.phongkaster.kanso.ui.tool.component.photocard.layeroverlay.LAYER_RESIZE_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LAYER_OPACITY_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LAYER_SHEET_CONFIRM_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LAYER_SIZE_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TEXT_COLOURS_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TEXT_COLOUR_BACK_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TEXT_COLOUR_PANE_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TEXT_CUSTOM_COLOUR_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TEXT_FIELD_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TEXT_FONTS_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.textFontTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.textSwatchTag
import com.phongkaster.kanso.ui.tool.model.TextSwatch
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.layerTabTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LAYER_TABS_TAG
import com.phongkaster.kanso.ui.tool.model.TextTab
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.textBackgroundTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TEXT_BACKGROUND_NONE_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LAYER_SHEET_CANCEL_TAG

/**
 * Proves the Text sheet on the real Edit screen (Run 11, Android 15, 411 x 891 dp): the Text
 * entry opens a sheet with a text area, and while the keyboard is asked for only the text area
 * shows; with the keyboard down the font row, colour row and both sliders come back; typing,
 * a font chip and a colour circle change the selected text; the colour picker takes the rows'
 * place until its back arrow; and a selected text's frame has an edit button where a sticker's
 * has its flip button.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class TextSheetTest {

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
     * Bug found on the Galaxy A13: with the keyboard asked for but not on screen, the sheet filled
     * the whole screen and hid its rows. Now the tabs stay until a keyboard really shows, the sheet
     * is never taller than the row and panel area it replaces (292 dp here: the 220 dp panel area and the 72 dp row), the photo stays in sight
     * above it, and typing still puts the words on the photo. (Robolectric shows no keyboard, which
     * is exactly that case.)
     */
    @Test
    fun without_a_keyboard_on_screen_the_sheet_keeps_its_rows_and_size() {
        val viewModel = openText()
        assertTrue(viewModel.state.value.textEditing)
        compose.onNodeWithTag(TEXT_FIELD_TAG).assertExists()
        listOf(LAYER_TABS_TAG, LAYER_SHEET_CANCEL_TAG, LAYER_SHEET_CONFIRM_TAG).forEach { compose.onNodeWithTag(it).assertExists() }
        TextTab.entries.forEach { compose.onNodeWithTag(layerTabTag(it)).assertExists() }
        val sheet = compose.onNodeWithTag(EDIT_SHEET_TAG).fetchSemanticsNode().boundsInRoot
        val photo = compose.onNodeWithTag(TOOL_CARD_TAG).fetchSemanticsNode().boundsInRoot
        val maxSheet = with(compose.density) { 292.dp.toPx() }
        assertTrue("the sheet is ${sheet.height} px tall, more than 292 dp", sheet.height <= maxSheet + 1f)
        assertTrue("the photo is hidden (${photo.height} px)", photo.height > maxSheet)

        compose.onNodeWithTag(TEXT_FIELD_TAG).performTextInput("Xin chào")
        compose.waitForIdle()
        val text = viewModel.state.value.settings.layers.single().content as LayerContent.Text
        assertEquals("Xin chào", text.text)
    }

    /**
     * With the keyboard down, each tab shows its one control under the pinned top bar: the Font
     * tab's chips, the Colour tab's circles and the Background tab's circles style the text.
     */
    @Test
    fun with_the_keyboard_down_each_tab_styles_the_text() {
        val viewModel = openText()
        compose.onNodeWithTag(TEXT_FIELD_TAG).performTextInput("Hello")
        viewModel.setTextEditing(false)
        compose.waitForIdle()

        compose.onNodeWithTag(layerTabTag(TextTab.FONT)).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onAllNodesWithTag(TEXT_FIELD_TAG).assertCountEquals(0)
        compose.onNodeWithTag(textFontTag(TextFont.PATRICK_HAND)).performScrollTo().performClick()
        compose.onNodeWithTag(layerTabTag(TextTab.COLOUR)).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onAllNodesWithTag(TEXT_FONTS_TAG).assertCountEquals(0)
        compose.onNodeWithTag(textSwatchTag(TextSwatch.BLUE)).performScrollTo().performClick()
        compose.onNodeWithTag(layerTabTag(TextTab.BACKGROUND)).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(textBackgroundTag(TextSwatch.YELLOW)).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(LAYER_SHEET_CONFIRM_TAG).assertExists()

        val text = viewModel.state.value.settings.layers.single().content as LayerContent.Text
        assertEquals(TextFont.PATRICK_HAND, text.font)
        assertEquals(TextSwatch.BLUE.argb, text.colorArgb)
        assertEquals(TextSwatch.YELLOW.argb, text.boxArgb)

        compose.onNodeWithTag(TEXT_BACKGROUND_NONE_TAG).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(null, (viewModel.state.value.settings.layers.single().content as LayerContent.Text).boxArgb)
    }

    /** The Colour tab's rainbow circle swaps the tabs for the picker; its back arrow brings them back. */
    @Test
    fun the_rainbow_circle_opens_the_picker_in_the_sheet() {
        val viewModel = openText()
        compose.onNodeWithTag(TEXT_FIELD_TAG).performTextInput("Hello")
        viewModel.setTextEditing(false)
        compose.waitForIdle()

        compose.onNodeWithTag(layerTabTag(TextTab.COLOUR)).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(TEXT_CUSTOM_COLOUR_TAG).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(TEXT_COLOUR_PANE_TAG).assertExists()
        compose.onAllNodesWithTag(LAYER_TABS_TAG).assertCountEquals(0)
        // The top bar stays pinned above the picker.
        compose.onNodeWithTag(LAYER_SHEET_CONFIRM_TAG).assertExists()

        compose.onNodeWithTag(TEXT_COLOUR_BACK_TAG).performClick()
        compose.waitForIdle()
        compose.onAllNodesWithTag(TEXT_COLOUR_PANE_TAG).assertCountEquals(0)
        compose.onNodeWithTag(LAYER_TABS_TAG).assertExists()
        compose.onNodeWithTag(TEXT_COLOURS_TAG).assertExists()
    }

    /** A selected text's frame has delete, edit, duplicate and resize; there is no flip button. */
    @Test
    fun a_texts_frame_has_an_edit_button_instead_of_flip() {
        val viewModel = openText()
        compose.onNodeWithTag(TEXT_FIELD_TAG).performTextInput("Hello")
        viewModel.setTextEditing(false)
        compose.waitForIdle()
        listOf(LAYER_DELETE_TAG, LAYER_EDIT_TAG, LAYER_DUPLICATE_TAG, LAYER_RESIZE_TAG).forEach {
            compose.onNodeWithTag(it).assertExists()
        }
        compose.onAllNodesWithTag(LAYER_FLIP_TAG).assertCountEquals(0)

        compose.onNodeWithTag(LAYER_EDIT_TAG).performClick()
        compose.waitForIdle()
        assertTrue("the edit button did not open the keyboard", viewModel.state.value.textEditing)
    }

    /** The tick keeps the text and closes the sheet with nothing selected. */
    @Test
    fun the_tick_keeps_the_text() {
        val viewModel = openText()
        compose.onNodeWithTag(TEXT_FIELD_TAG).performTextInput("Hello")
        viewModel.setTextEditing(false)
        compose.waitForIdle()
        compose.onNodeWithTag(LAYER_SHEET_CONFIRM_TAG).performClick()
        compose.waitForIdle()
        assertNull(viewModel.state.value.openTool)
        assertEquals(1, viewModel.state.value.settings.layers.size)
    }

    /** Draws the screen on the fake photo, scrolls the row to Text and taps it. */
    private fun openText(): ToolViewModel {
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
        compose.setContent {
            MaterialTheme {
                ToolScreen(viewModel = viewModel, onBack = {}, onDone = {})
            }
        }
        compose.waitForIdle()
        // The row is lazy and Text sits past its right edge: scroll the row itself to it first.
        compose.scrollToolRowTo(EditTool.TEXT)
        compose.onNodeWithTag(editToolTag(EditTool.TEXT)).performClick()
        compose.waitForIdle()
        assertEquals(EditTool.TEXT, viewModel.state.value.openTool)
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
