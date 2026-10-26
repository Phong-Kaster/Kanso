package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.Sticker
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.data.edit.edit.StickerMapping
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.components.editbottombar.EDIT_SHEET_TAG
import com.phongkaster.kanso.ui.tool.components.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.ui.tool.components.STICKER_LIMIT_MESSAGE_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.stickerspanel.STICKERS_CANCEL_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.stickerspanel.STICKERS_CONFIRM_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.stickerspanel.STICKERS_PANEL_TAG
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.stickerspanel.stickerTileTag
import com.phongkaster.kanso.ui.tool.editProcessorFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * Checks adding, keeping and throwing away stickers on the Edit screen on Android 15 (Run 10):
 *
 * - DoD 4: one tap on a grid tile adds one sticker at the middle of the picture the person SEES,
 *   even when the photo is cropped off-centre and turned a quarter turn, about a third of the shown
 *   picture's width, and selects it;
 * - DoD 13: at 20 stickers a tap adds nothing and the limit message is shown;
 * - DoD 42: the tick keeps every change, one undo entry per action, and closes the sheet;
 * - DoD 43: the cross, and the system Back, put settings and undo steps back exactly as they were
 *   when the sheet opened (checked on the ViewModel and through the screen);
 * - DoD 18: the one recipe, `editProcessorFor`, draws a sticker into the processed photo.
 *
 * The Compose tests draw the real tool screen (`ToolScreen`) for a real [ToolViewModel] on a fake
 * 600 x 400 photo that is decoded before the screen is drawn, so no loading indicator is on screen.
 * Edits the screen cannot make yet (moving or flipping a sticker) are made through the ViewModel's
 * `changeSettings`, the call a sticker gesture will make.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StickerAddTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** THE recipe the screen itself uses, so the tests draw stickers the way the app does. */
    private val processorFor by lazy { editProcessorFor(context) }

    /** Stops Koin after each test: KansoApplication starts it for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * From a photo cropped off-centre and turned a quarter turn, one tap on the first Food tile
     * gives one sticker. Seen through [StickerMapping] its centre is the middle of the shown
     * picture (0.5, 0.5, within 0.01), its width is between 0.30 and 0.37 of the shown picture's
     * width, and the screen's selected sticker is that sticker.
     */
    @Test
    fun one_tap_adds_one_sticker_at_the_middle_of_the_shown_picture() {
        val viewModel = showEdit()
        val turned = EditSettings(crop = OFF_CENTRE, quarterTurns = 1)
        viewModel.changeSettings(turned, processorFor)
        compose.waitForIdle()
        openStickers(viewModel)

        tapTile(0)

        val state = viewModel.state.value
        assertEquals("one tap, one sticker", 1, state.settings.stickers.size)
        val sticker = state.settings.stickers.single()
        val photo = (state.photo as ToolUiState.PhotoState.Ready).bitmap
        val mapping = StickerMapping(photo.width, photo.height, state.settings)
        val shown = mapping.originalToShown(StickerMapping.Point(sticker.centreX, sticker.centreY))
        assertEquals("shown centre x", 0.5f, shown.x, 0.01f)
        assertEquals("shown centre y", 0.5f, shown.y, 0.01f)
        val shownWidth = mapping.originalWidthToShown(sticker.width)
        assertTrue("shown width $shownWidth is not between 0.30 and 0.37", shownWidth in 0.30f..0.37f)
        assertEquals("selected sticker", sticker.key, state.selectedStickerKey)
        assertEquals(StickerCatalog.inCategory(StickerCategory.FOOD).first().pictureId, sticker.pictureId)
    }

    /**
     * With 20 stickers on the photo a tap on a tile adds nothing (still 20, and no new undo step),
     * the screen state asks for the limit message and the message card is on screen with its text.
     * The clock is held while the tile is tapped, so the message is read before its own timer
     * would hide it.
     */
    @Test
    fun at_twenty_stickers_a_tap_shows_the_limit_message() {
        val viewModel = showEdit()
        val full = EditSettings(stickers = List(EditSettings.MAX_STICKERS) { index -> sticker(100L + index) })
        viewModel.changeSettings(full, processorFor)
        compose.waitForIdle()
        openStickers(viewModel)
        val canUndoBefore = viewModel.state.value.canUndo

        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag(stickerTileTag(firstFoodId())).performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()

        val state = viewModel.state.value
        assertTrue("the state did not ask for the limit message", state.stickerLimitShown)
        assertEquals("a sticker was added past the limit", 20, state.settings.stickers.size)
        assertEquals(canUndoBefore, state.canUndo)
        assertNull("a refused tap selected something", state.selectedStickerKey)
        compose.onNodeWithTag(STICKER_LIMIT_MESSAGE_TAG).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.stickers_limit_message)).assertIsDisplayed()
    }

    /**
     * From a photo with one earlier sticker and the sheet open: a tap adds a sticker, the earlier
     * one is moved, and the tick is tapped. The settings hold both changes; each action is its own
     * undo entry (the first Undo takes back only the move, the second only the added sticker); the
     * sheet is gone and the five tool entries are on screen again.
     */
    @Test
    fun the_tick_keeps_every_change_and_closes_the_sheet() {
        val viewModel = showEdit()
        val earlier = sticker(100L)
        val start = EditSettings(stickers = listOf(earlier))
        viewModel.changeSettings(start, processorFor)
        compose.waitForIdle()
        openStickers(viewModel)

        tapTile(0)
        val afterAdd = viewModel.state.value.settings
        assertEquals(2, afterAdd.stickers.size)
        val moved = afterAdd.copy(stickers = afterAdd.stickers.map { if (it.key == earlier.key) it.copy(centreX = 0.2f) else it })
        viewModel.changeSettings(moved, processorFor)
        compose.waitForIdle()

        compose.onNodeWithTag(STICKERS_CONFIRM_TAG).performClick()
        compose.waitForIdle()

        val state = viewModel.state.value
        assertEquals("the settings lost a change", moved, state.settings)
        assertEquals(0.2f, state.settings.stickers.first { it.key == earlier.key }.centreX, 1e-4f)
        assertNull(state.openTool)
        assertNull(state.selectedStickerKey)
        assertSheetClosedAndFiveToolsShown()

        // One entry per action: the move, then the added sticker, then the earlier sticker's own.
        viewModel.undo()
        assertEquals("first Undo", afterAdd, viewModel.state.value.settings)
        viewModel.undo()
        assertEquals("second Undo", start, viewModel.state.value.settings)
        viewModel.undo()
        assertEquals("third Undo", EditSettings(), viewModel.state.value.settings)
        assertFalse(viewModel.state.value.canUndo)
    }

    /**
     * On the ViewModel alone: with a crop and one sticker confirmed earlier, open the sheet, add
     * two stickers, flip and drag the earlier one, then cancel. The settings are the ones from the
     * moment the sheet opened (earlier sticker upright and in its old place, the crop kept), Undo is
     * as before, and the undo history is the same list of steps (read by undoing it all).
     */
    @Test
    fun cancel_puts_the_settings_and_the_undo_history_back() {
        val viewModel = loadedViewModel()
        viewModel.changeSettings(EditSettings(crop = OFF_CENTRE), processorFor)
        viewModel.openTool(EditTool.STICKERS)
        viewModel.addSticker(StickerCatalog.inCategory(StickerCategory.FOOD).first(), processorFor)
        viewModel.confirmStickers()
        val opened = viewModel.state.value.settings
        assertEquals(1, opened.stickers.size)
        assertEquals(OFF_CENTRE, opened.crop)
        assertTrue(viewModel.state.value.canUndo)

        viewModel.openTool(EditTool.STICKERS)
        val food = StickerCatalog.inCategory(StickerCategory.FOOD)
        viewModel.addSticker(food[1], processorFor)
        viewModel.addSticker(food[2], processorFor)
        // Flip and drag the earlier sticker (what the sticker gestures will do through changeSettings).
        val earlierKey = opened.stickers.single().key
        val now = viewModel.state.value.settings
        assertEquals(3, now.stickers.size)
        viewModel.changeSettings(
            now.copy(stickers = now.stickers.map { if (it.key == earlierKey) it.copy(mirrored = !it.mirrored) else it }),
            processorFor,
        )
        val flipped = viewModel.state.value.settings
        viewModel.changeSettings(
            flipped.copy(stickers = flipped.stickers.map { if (it.key == earlierKey) it.copy(centreX = 0.1f, centreY = 0.9f) else it }),
            processorFor,
        )
        assertTrue("the edits did not reach the settings", viewModel.state.value.settings != opened)

        viewModel.cancelStickers()
        shadowOf(Looper.getMainLooper()).idle()

        val state = viewModel.state.value
        assertEquals("settings after Cancel", opened, state.settings)
        assertFalse("the earlier sticker is still flipped", state.settings.stickers.single().mirrored)
        assertEquals(OFF_CENTRE, state.settings.crop)
        assertTrue("Undo was lost", state.canUndo)
        assertNull(state.openTool)
        assertNull(state.selectedStickerKey)

        // The history is what it was: the sticker's confirmation, then the crop, then nothing.
        viewModel.undo()
        assertEquals(EditSettings(crop = OFF_CENTRE), viewModel.state.value.settings)
        viewModel.undo()
        assertEquals(EditSettings(), viewModel.state.value.settings)
        assertFalse(viewModel.state.value.canUndo)
    }

    /**
     * The same as the ViewModel test, through the screen: the cross (a tap on [STICKERS_CANCEL_TAG])
     * closes the sheet and restores the settings and Undo, and the five tool entries are back.
     */
    @Test
    fun the_cross_cancels_through_the_screen() {
        cancelThroughTheScreen { compose.onNodeWithTag(STICKERS_CANCEL_TAG).performClick() }
    }

    /**
     * The same again with the system Back (`Espresso.pressBack()`) while the sheet is open: Back is
     * the sheet's Cancel, not "leave the screen".
     */
    @Test
    fun system_back_cancels_through_the_screen() {
        cancelThroughTheScreen { Espresso.pressBack() }
    }

    /**
     * DoD 18, factory clause: a sticker added from the panel survives a crop finished in the
     * cropper because both build their processor from [editProcessorFor]. A real cropper gesture is
     * not drivable in a JVM test, so this checks what the guarantee rests on: that the recipe,
     * given settings with a crop and a sticker, draws the sticker into the real processed photo
     * (the real sticker pictures from the drawables), so the result differs from the same settings
     * without it.
     */
    @Test
    fun the_shared_recipe_draws_the_sticker_into_the_processed_photo() {
        val photo = ToolFixtures.gradient(600, 400)
        val plain = EditSettings(crop = EditSettings.CropRect(0.1f, 0.1f, 0.9f, 0.9f))
        val withSticker = plain.copy(stickers = listOf(sticker(1L)))

        val without = process(plain, photo)
        val with = process(withSticker, photo)

        assertEquals(without.width, with.width)
        assertEquals(without.height, with.height)
        assertFalse("the sticker left no mark on the processed photo", without.sameAs(with))
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Runs one cancel scenario: a crop and an earlier sticker, the Stickers sheet opened, two
     * stickers added by tapping, the earlier one flipped and moved, then [cancel]. Afterwards the
     * settings and Undo are what they were at opening and the sheet is closed.
     */
    private fun cancelThroughTheScreen(cancel: () -> Unit) {
        val viewModel = showEdit()
        val earlier = sticker(100L)
        viewModel.changeSettings(EditSettings(crop = OFF_CENTRE, stickers = listOf(earlier)), processorFor)
        compose.waitForIdle()
        val opened = viewModel.state.value.settings
        val canUndoBefore = viewModel.state.value.canUndo
        openStickers(viewModel)

        tapTile(0)
        tapTile(1)
        val now = viewModel.state.value.settings
        assertEquals(3, now.stickers.size)
        val flipped = now.copy(stickers = now.stickers.map { if (it.key == earlier.key) it.copy(mirrored = true, centreX = 0.15f) else it })
        viewModel.changeSettings(flipped, processorFor)
        compose.waitForIdle()
        assertTrue(viewModel.state.value.settings != opened)

        cancel()
        compose.waitForIdle()

        val state = viewModel.state.value
        assertEquals("settings after Cancel", opened, state.settings)
        assertEquals(OFF_CENTRE, state.settings.crop)
        assertFalse(state.settings.stickers.single().mirrored)
        assertEquals(canUndoBefore, state.canUndo)
        assertNull(state.openTool)
        assertSheetClosedAndFiveToolsShown()
        // Undo goes back one step to the plain photo: the earlier sticker's own step survived.
        viewModel.undo()
        assertEquals(EditSettings(), viewModel.state.value.settings)
    }

    /** The sheet's nodes are gone and each of the five tool entries is on screen. */
    private fun assertSheetClosedAndFiveToolsShown() {
        compose.onAllNodesWithTag(STICKERS_PANEL_TAG).assertCountEquals(0)
        compose.onAllNodesWithTag(EDIT_SHEET_TAG).assertCountEquals(0)
        assertEquals(5, EditTool.entries.size)
        EditTool.entries.forEach { compose.onNodeWithTag(editToolTag(it)).assertIsDisplayed() }
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

    /** The picture id of the first Food sticker. */
    private fun firstFoodId(): String = StickerCatalog.inCategory(StickerCategory.FOOD).first().pictureId

    /** A made-up sticker with [key], upright, at the middle of the original photo. */
    private fun sticker(key: Long): Sticker = Sticker(
        key = key,
        pictureId = "img_sticker_apple",
        centreX = 0.5f,
        centreY = 0.5f,
        width = 0.3f,
        degrees = 0f,
        mirrored = false,
    )

    /** Renders [settings] on [photo] with the app's own recipe and returns the picture. */
    private fun process(settings: EditSettings, photo: Bitmap): Bitmap {
        val result = runBlocking { processorFor(settings).process(photo) { } }
        return (result as EditResult.Edited).bitmap
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

        /** A crop away from the middle of the photo (the lower right part). */
        val OFF_CENTRE = EditSettings.CropRect(0.3f, 0.2f, 0.9f, 0.8f)
    }
}
