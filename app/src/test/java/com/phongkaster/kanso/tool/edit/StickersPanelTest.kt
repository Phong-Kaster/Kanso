package com.phongkaster.kanso.tool.edit

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.StickerCatalog
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.editToolTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.STICKERS_GRID_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.STICKER_CHIPS_TAG
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.stickerChipTag
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.stickerspanel.stickerTileTag
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
 * Checks the Stickers sheet's chips and grid on Android 15 (DoD 3), driven from the sheet just
 * opened with a tap on the Stickers entry of the real tool screen:
 *
 * - the chip row scrolls sideways (a `LazyRow`) and the grid scrolls up and down (a
 *   `LazyVerticalGrid`) with exactly three columns: the first three tiles share one row, the
 *   fourth starts the next;
 * - tapping each chip in turn marks it chosen (the chip reports `selected`, the others do not) and
 *   the ViewModel's category follows;
 * - scrolling through the whole grid, the tiles met (found by their tags) are exactly that
 *   category's layers: 17 food, 17 mood, 6 quote, 20 emoji.
 *
 * The photo is decoded before the screen is drawn, so the loading indicator (which animates
 * forever) never appears.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class StickersPanelTest {

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
     * The chip row is a lazy row (it can scroll sideways and not up and down), the grid is a lazy
     * vertical grid (it can scroll up and down and not sideways), and the grid is three columns
     * wide: tiles 1 to 3 sit on one y and increase in x, tile 4 is lower and back at the first
     * column's x.
     */
    @Test
    fun the_chips_are_a_lazy_row_and_the_grid_has_exactly_three_columns() {
        showStickersSheet()

        val chips = compose.onNodeWithTag(STICKER_CHIPS_TAG).fetchSemanticsNode().config
        assertNotNull("The chip row does not scroll sideways", chips.getOrNull(SemanticsProperties.HorizontalScrollAxisRange))
        assertNull("The chip row scrolls up and down", chips.getOrNull(SemanticsProperties.VerticalScrollAxisRange))

        val grid = compose.onNodeWithTag(STICKERS_GRID_TAG).fetchSemanticsNode().config
        assertNotNull("The grid does not scroll up and down", grid.getOrNull(SemanticsProperties.VerticalScrollAxisRange))
        assertNull("The grid scrolls sideways", grid.getOrNull(SemanticsProperties.HorizontalScrollAxisRange))

        val first = StickerCatalog.inCategory(StickerCategory.FOOD).take(4)
        val positions = first.map { compose.onNodeWithTag(stickerTileTag(it.pictureId)).fetchSemanticsNode().positionInRoot }
        assertEquals("tiles 1 and 2 are not on one row", positions[0].y, positions[1].y, 0.5f)
        assertEquals("tiles 2 and 3 are not on one row", positions[1].y, positions[2].y, 0.5f)
        assertTrue("tile 2 is not right of tile 1", positions[1].x > positions[0].x)
        assertTrue("tile 3 is not right of tile 2", positions[2].x > positions[1].x)
        assertTrue("tile 4 did not start a new row", positions[3].y > positions[0].y + 1f)
        assertEquals("tile 4 is not back at the first column", positions[0].x, positions[3].x, 0.5f)
    }

    /**
     * Tapping each chip in turn (Food, Mood, Quote, Emoji and back to Food) makes that chip the
     * selected one and the ViewModel's chosen category; no other chip on screen is selected.
     */
    @Test
    fun tapping_each_chip_marks_it_chosen() {
        val viewModel = showStickersSheet()
        assertEquals(StickerCategory.FOOD, viewModel.state.value.stickerCategory)
        compose.onNodeWithTag(stickerChipTag(StickerCategory.FOOD)).assertIsSelected()

        (StickerCategory.entries.drop(1) + StickerCategory.entries.first()).forEach { category ->
            tapChip(category)
            assertEquals(category, viewModel.state.value.stickerCategory)
            compose.onNodeWithTag(stickerChipTag(category)).assertIsSelected()
            StickerCategory.entries.filter { it != category }.forEach { other ->
                // A chip scrolled out of the row is not composed, so only the ones on screen are read.
                compose.onAllNodesWithTag(stickerChipTag(other)).fetchSemanticsNodes().forEach { node ->
                    assertFalse("${other.name} is still selected after ${category.name}", node.config.getOrNull(SemanticsProperties.Selected) == true)
                }
            }
        }
    }

    /**
     * Scrolling through each category's whole grid, the sticker tiles met are exactly that
     * category's layers, counted by tag: 17 food, 17 mood, 6 quote and 20 emoji.
     */
    @Test
    fun scrolling_the_grid_meets_every_sticker_of_the_category() {
        showStickersSheet()
        val expectedCounts = mapOf(
            StickerCategory.FOOD to 17,
            StickerCategory.MOOD to 17,
            StickerCategory.QUOTE to 6,
            StickerCategory.EMOJI to 20,
        )
        StickerCategory.entries.forEach { category ->
            tapChip(category)
            compose.onNodeWithTag(STICKERS_GRID_TAG).performScrollToIndex(0)
            compose.waitForIdle()
            val met = linkedSetOf<String>()
            met += tileTagsOnScreen()
            // 40 dp steps are smaller than one 56 dp row, so no row can be jumped over; extra
            // steps past the end change nothing.
            repeat(25) {
                scrollGridBy(40f)
                met += tileTagsOnScreen()
            }
            assertEquals("tiles met in ${category.name}", expectedCounts.getValue(category), met.size)
            val expected = StickerCatalog.inCategory(category).map { stickerTileTag(it.pictureId) }.toSet()
            assertEquals("tiles of ${category.name}", expected, met.toSet())
        }
        assertEquals(60, expectedCounts.values.sum())
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Taps the chip of [category], scrolling the chip row to it first (it may sit off screen). */
    private fun tapChip(category: StickerCategory) {
        compose.onNodeWithTag(STICKER_CHIPS_TAG).performScrollToNode(hasTestTag(stickerChipTag(category)))
        compose.onNodeWithTag(stickerChipTag(category)).performClick()
        compose.waitForIdle()
    }

    /** Scrolls the grid down by [dp] density-independent pixels with its own scroll action (no fling). */
    private fun scrollGridBy(dp: Float) {
        val pixels = with(compose.density) { dp.dp.toPx() }
        compose.onNodeWithTag(STICKERS_GRID_TAG).performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
            scrollBy(0f, pixels)
        }
        compose.waitForIdle()
    }

    /** The tags of the sticker tiles that are composed right now. */
    private fun tileTagsOnScreen(): List<String> =
        compose.onAllNodes(SemanticsMatcher("is a sticker tile") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(TILE_PREFIX) == true
        }).fetchSemanticsNodes().mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }

    /**
     * Draws the real tool screen for a real [ToolViewModel] on a fake photo, then taps the
     * Stickers entry and waits until its sheet is open.
     */
    private fun showStickersSheet(): ToolViewModel {
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
        compose.onNodeWithTag(editToolTag(EditTool.STICKERS)).performClick()
        compose.waitForIdle()
        assertEquals(EditTool.STICKERS, viewModel.state.value.openTool)
        return viewModel
    }

    /** Values shared by the tests. */
    private companion object {
        /** A content Uri no real photo has; the fake loader ignores it. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** The size the screen asks for: twice 891 dp at 3 pixels per dp. */
        const val MAX_EDGE_PX = 2 * 891 * 3

        /** What every sticker tile's test tag starts with (see `stickerTileTag`). */
        const val TILE_PREFIX = "sticker-tile-"
    }
}
