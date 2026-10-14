package com.phongkaster.kanso.tool.cutout

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.CUTOUT_CHECKERBOARD_TAG
import com.phongkaster.kanso.ui.tool.CUTOUT_PANEL_TAG
import com.phongkaster.kanso.ui.tool.CheckerboardTileColours
import com.phongkaster.kanso.ui.tool.CutoutPanel
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolUiState
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 6, the screen half: the tool screen's stateless content with the Remove background panel in
 * its slot shows the photo and a checkerboard whose two tile colours differ, in light and in dark.
 *
 * The colours are checked twice: as the checkerboard reports them in its semantics, and as they
 * are really drawn, by drawing the activity's content view into a bitmap (never captureToImage,
 * which times out here) and reading one light and one dark tile along its top edge, above the
 * panel's button.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class CutoutPreviewTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Stops Koin after each test: the test application starts it for every Robolectric test, and
     * its state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * With the Remove background panel in the slot, the screen shows the photo and the
     * checkerboard, whose two tile colours differ both as reported and as drawn; tapping the
     * panel's button calls its callback.
     */
    @Test
    fun the_remove_background_screen_shows_a_two_tone_checkerboard() {
        val state = ToolUiState(
            tool = Tool.REMOVE_BACKGROUND,
            photoUri = PHOTO_URI,
            photo = ToolUiState.PhotoState.Ready(ToolFixtures.gradient(600, 400)),
        )
        var mode by mutableStateOf(ThemeMode.Light)
        var runs = 0
        compose.setContent {
            KansoTheme(themeMode = mode) {
                ToolContent(
                    state = state,
                    onBack = {},
                    onSave = {},
                    panel = { CutoutPanel(onRun = { runs++ }, showCheckerboard = false) },
                )
            }
        }

        listOf(ThemeMode.Light, ThemeMode.Dark).forEach { shownMode ->
            compose.runOnUiThread { mode = shownMode }
            compose.waitForIdle()

            compose.onNodeWithTag(TOOL_IMAGE_TAG).assertExists()
            val board = compose
                .onNode(hasTestTag(CUTOUT_CHECKERBOARD_TAG) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)))
                .fetchSemanticsNode()
            val (light, dark) = board.config[CheckerboardTileColours]
            assertNotEquals("$shownMode: the two tile colours are the same", light, dark)

            val bounds = board.boundsInRoot
            val tilePx = with(compose.density) { TILE_DP.dp.toPx() }
            // Sample four tiles in from the top-left corner: the photo card is rounded, so its very
            // corner is clipped, and four tiles in the checks (even row, even column) are still
            // above the photo, which is centred in the tall card.
            val y = (bounds.top + tilePx * (INSET_TILES + 0.5f)).toInt()
            val firstX = (bounds.left + tilePx * (INSET_TILES + 0.5f)).toInt()
            val secondX = (bounds.left + tilePx * (INSET_TILES + 1.5f)).toInt()
            drawn { bitmap ->
                val first = bitmap.getPixel(firstX, y)
                val second = bitmap.getPixel(secondX, y)
                assertTrue(
                    "$shownMode: the first tile drew ${Integer.toHexString(first)}, not the light colour",
                    channelDistance(first, light) <= COLOUR_TOLERANCE,
                )
                assertTrue(
                    "$shownMode: the second tile drew ${Integer.toHexString(second)}, not the dark colour",
                    channelDistance(second, dark) <= COLOUR_TOLERANCE,
                )
                assertNotEquals("$shownMode: neighbouring tiles drew the same colour", first, second)
            }
        }

        compose.onNode(hasTestTag(CUTOUT_PANEL_TAG)).assertExists()
        compose.onNode(hasAnyAncestor(hasTestTag(CUTOUT_PANEL_TAG)) and hasClickAction()).performClick()
        compose.waitForIdle()
        assertEquals(1, runs)
    }

    /**
     * Draws the activity's content view into a fresh bitmap, hands it to [read], then frees it.
     * The Compose root fills the content view from its top-left corner, so node bounds in the root
     * are pixel positions in the bitmap.
     */
    private fun drawn(read: (Bitmap) -> Unit) {
        compose.runOnUiThread {
            val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
            val view = checkNotNull(content.getChildAt(0)) { "The activity has no content view yet" }
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            try {
                read(bitmap)
            } finally {
                bitmap.recycle()
            }
        }
    }

    /** The largest difference between the drawn [pixel] and [colour] in red, green or blue. */
    private fun channelDistance(pixel: Int, colour: Color): Int {
        val argb = colour.toArgb()
        return maxOf(
            abs(android.graphics.Color.red(pixel) - android.graphics.Color.red(argb)),
            abs(android.graphics.Color.green(pixel) - android.graphics.Color.green(argb)),
            abs(android.graphics.Color.blue(pixel) - android.graphics.Color.blue(argb)),
        )
    }

    /** Values the test shares. */
    private companion object {
        /** The photo the state stands for; nothing is read from it. */
        const val PHOTO_URI = "content://media/external/images/media/6"

        /** The checkerboard's tile side, in dp (its default). */
        const val TILE_DP = 12

        /** How many tiles in from the checkerboard's top-left corner the sampled tiles lie. */
        const val INSET_TILES = 4

        /** A drawn tile may differ from its colour by this much in a channel (rounding). */
        const val COLOUR_TOLERANCE = 3
    }
}
