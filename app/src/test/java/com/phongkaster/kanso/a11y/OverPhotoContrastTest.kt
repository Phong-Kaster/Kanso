package com.phongkaster.kanso.a11y

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.saved.SAVED_CLOSE_TAG
import com.phongkaster.kanso.ui.saved.SAVED_MARK_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SHARE_TAG
import com.phongkaster.kanso.ui.saved.SAVED_TITLE_TAG
import com.phongkaster.kanso.ui.saved.SavedContent
import com.phongkaster.kanso.ui.saved.SavedPhoto
import com.phongkaster.kanso.ui.saved.SavedUiState
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_REPLACE_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolUiState
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks that the text and buttons drawn over a photo stay readable whatever the photo looks like
 * (DoD 24): on an all-white photo and on an all-black one, in light and in dark, with Kanso blue.
 *
 * The screens that draw over a photo are Saved (right after a save), the photo opened from My
 * edits (Saved's viewer form) and the five tool screens. Each test draws one of them in one mode
 * from its stateless `...Content` part, first with an all-white 600 x 800 photo, then with an
 * all-black one (the photo is swapped through Compose state, so the same window is drawn again),
 * and after each draw reads pixels and works out WCAG contrast ratios:
 *
 * - Saved's title ("Saved", or "My edits" in the viewer): the lightest and the darkest pixel
 *   inside its text's bounds, at least [TEXT_MIN_CONTRAST]. The title sits on the top shade over
 *   the blurred photo, so the darkest or lightest pixel there is the shade and the other the ink;
 * - the round close and Share buttons: their icon against the circle it sits on, at least
 *   [ICON_MIN_CONTRAST];
 * - the "Replace" chip on the tool screens: its word against the chip's container, at least
 *   [TEXT_MIN_CONTRAST];
 * - Saved's success mark: its check against the mark, at least [ICON_MIN_CONTRAST].
 *
 * How a contrast is measured. The screen is drawn into a bitmap from the activity's content view,
 * as `ScreenPixels.kt` does (Compose's own captureToImage times out under Robolectric here), with
 * Robolectric's real drawing code on (`GraphicsMode.Mode.NATIVE`). Positions come from the
 * semantics tree, `positionInRoot` and `size`, never `boundsInRoot`. A container's colour is
 * read at one pixel inside the container where nothing else is drawn (below the icon in a
 * circle, just past the word in the chip); the ink's colour is the pixel, among those where the
 * icon or the word is drawn, that differs most from it. An icon or a word that was not drawn
 * at all leaves only container pixels, a ratio near 1, and fails.
 *
 * The WCAG contrast ratio of two colours is (L1 + 0.05) / (L2 + 0.05), L1 the lighter one's
 * relative luminance (0 black, 1 white) and L2 the darker one's: 1 for two equal colours, 21 for
 * black on white. WCAG asks 4.5 for text and 3 for icons and other graphics.
 *
 * Every problem of both photos is listed in one failure, and every ratio is printed as
 * `contrast <screen> <mode> <photo>: <what> <ratio>`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = OVER_PHOTO_PHONE)
class OverPhotoContrastTest {

    /** Starts an empty activity to host each screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Stops Koin, which KansoApplication starts for every test and keeps in a JVM-wide singleton. */
    @After
    fun tearDown() {
        stopKoin()
    }

    // --- Saved -----------------------------------------------------------------------------

    /** Saved in light mode over a white and a black photo: title, close, Share and the mark. */
    @Test
    fun saved_reads_over_white_and_black_photos_in_light() {
        checkSaved(ThemeMode.Light, viewer = false)
    }

    /** Saved in dark mode over a white and a black photo: title, close, Share and the mark. */
    @Test
    fun saved_reads_over_white_and_black_photos_in_dark() {
        checkSaved(ThemeMode.Dark, viewer = false)
    }

    /** The photo opened from My edits in light mode over a white and a black photo: title, close, Share. */
    @Test
    fun photo_opened_from_my_edits_reads_over_white_and_black_photos_in_light() {
        checkSaved(ThemeMode.Light, viewer = true)
    }

    /** The photo opened from My edits in dark mode over a white and a black photo: title, close, Share. */
    @Test
    fun photo_opened_from_my_edits_reads_over_white_and_black_photos_in_dark() {
        checkSaved(ThemeMode.Dark, viewer = true)
    }

    // --- Tool screens ----------------------------------------------------------------------

    /** The Edit screen in light mode: "Replace" reads on its chip over a white and a black photo. */
    @Test
    fun tool_screen_edit_reads_over_white_and_black_photos_in_light() {
        checkToolScreen(ThemeMode.Light, Tool.EDIT)
    }

    /** The Edit screen in dark mode: "Replace" reads on its chip over a white and a black photo. */
    @Test
    fun tool_screen_edit_reads_over_white_and_black_photos_in_dark() {
        checkToolScreen(ThemeMode.Dark, Tool.EDIT)
    }

    /** The Remove background screen in light mode. */
    @Test
    fun tool_screen_remove_background_reads_over_white_and_black_photos_in_light() {
        checkToolScreen(ThemeMode.Light, Tool.REMOVE_BACKGROUND)
    }

    /** The Remove background screen in dark mode. */
    @Test
    fun tool_screen_remove_background_reads_over_white_and_black_photos_in_dark() {
        checkToolScreen(ThemeMode.Dark, Tool.REMOVE_BACKGROUND)
    }

    /** The Enhance screen in light mode. */
    @Test
    fun tool_screen_enhance_reads_over_white_and_black_photos_in_light() {
        checkToolScreen(ThemeMode.Light, Tool.ENHANCE)
    }

    /** The Enhance screen in dark mode. */
    @Test
    fun tool_screen_enhance_reads_over_white_and_black_photos_in_dark() {
        checkToolScreen(ThemeMode.Dark, Tool.ENHANCE)
    }

    /** The Retouch screen in light mode. */
    @Test
    fun tool_screen_retouch_reads_over_white_and_black_photos_in_light() {
        checkToolScreen(ThemeMode.Light, Tool.RETOUCH)
    }

    /** The Retouch screen in dark mode. */
    @Test
    fun tool_screen_retouch_reads_over_white_and_black_photos_in_dark() {
        checkToolScreen(ThemeMode.Dark, Tool.RETOUCH)
    }

    /** The Artistic screen in light mode. */
    @Test
    fun tool_screen_artistic_reads_over_white_and_black_photos_in_light() {
        checkToolScreen(ThemeMode.Light, Tool.ARTISTIC)
    }

    /** The Artistic screen in dark mode. */
    @Test
    fun tool_screen_artistic_reads_over_white_and_black_photos_in_dark() {
        checkToolScreen(ThemeMode.Dark, Tool.ARTISTIC)
    }

    // --- One screen each -------------------------------------------------------------------

    /**
     * Draws Saved in [mode] (its viewer form, the photo opened from My edits, when [viewer]) over
     * each photo and checks its title, its close and Share buttons and, after a save only, its
     * success mark.
     */
    private fun checkSaved(mode: ThemeMode, viewer: Boolean) {
        val screen = if (viewer) "photo opened from my edits" else "saved"
        checkOverPhotos(
            screen = screen,
            mode = mode,
            content = { photo ->
                SavedContent(
                    state = SavedUiState(photoUri = PHOTO_URI, photo = SavedPhoto.Ready(photo), viewer = viewer),
                    sharing = false,
                    onShare = {},
                    onEditAnother = {},
                    onClose = {},
                )
            },
            measure = { bitmap ->
                buildList {
                    add(Measure("title", textExtremesContrast(bitmap, nodeRect(SAVED_TITLE_TAG)), TEXT_MIN_CONTRAST))
                    add(Measure("close icon", roundButtonContrast(bitmap, nodeRect(SAVED_CLOSE_TAG)), ICON_MIN_CONTRAST))
                    add(Measure("share icon", roundButtonContrast(bitmap, nodeRect(SAVED_SHARE_TAG)), ICON_MIN_CONTRAST))
                    if (!viewer) {
                        add(Measure("success mark's check", markContrast(bitmap, nodeRect(SAVED_MARK_TAG)), ICON_MIN_CONTRAST))
                    }
                }
            },
        )
    }

    /** Draws the screen of [tool] in [mode] over each photo and checks the "Replace" chip's word. */
    private fun checkToolScreen(mode: ThemeMode, tool: Tool) {
        val replace = app.getString(R.string.tool_screen_replace)
        checkOverPhotos(
            screen = "tool screen ${tool.name}",
            mode = mode,
            content = { photo ->
                ToolContent(
                    state = ToolUiState(
                        tool = tool,
                        photoUri = PHOTO_URI,
                        photo = ToolUiState.PhotoState.Ready(photo),
                        save = ToolUiState.SaveState.Idle,
                    ),
                    onBack = {},
                    onSave = {},
                    onReplace = {},
                )
            },
            measure = { bitmap ->
                val word = compose.onNode(
                    hasText(replace) and hasAnyAncestor(hasTestTag(TOOL_REPLACE_TAG)),
                    useUnmergedTree = true,
                ).fetchSemanticsNode()
                val wordRect = Rect(word.positionInRoot, word.size.toSize())
                listOf(Measure("\"$replace\" on its chip", chipWordContrast(bitmap, wordRect, nodeRect(TOOL_REPLACE_TAG)), TEXT_MIN_CONTRAST))
            },
        )
    }

    // --- Helpers ---------------------------------------------------------------------------

    /**
     * One contrast the test measured: [what] it is, its [ratio] (null when the test could not
     * read the right pixels; the reason is then in [problem]) and the [minimum] it needs.
     */
    private data class Measure(val what: String, val ratio: Double?, val minimum: Double, val problem: String? = null) {
        /**
         * Builds a measure from a [result] of a contrast function: its ratio, or the reason it
         * could not be measured.
         */
        constructor(what: String, result: Result<Double>, minimum: Double) :
            this(what, result.getOrNull(), minimum, result.exceptionOrNull()?.message)
    }

    /**
     * Draws [content] inside KansoTheme in [mode] with Kanso blue, handing it an all-white photo,
     * then an all-black one, both 600 x 800 pixels; after each, waits until the screen has
     * settled, draws it into a bitmap and lets [measure] read its contrasts. Fails, naming
     * [screen], [mode] and the photo, if any contrast is under its minimum or could not be read.
     *
     * The photo is held in Compose state (`mutableStateOf`): changing it draws the same window
     * again with the new photo, as the app would, so one test covers both photos. Nothing on
     * these screens keeps moving once the photo is shown, so the clock runs by itself.
     */
    private fun checkOverPhotos(
        screen: String,
        mode: ThemeMode,
        content: @Composable (Bitmap) -> Unit,
        measure: (Bitmap) -> List<Measure>,
    ) {
        val photo = mutableStateOf(solidPhoto(PHOTOS.first().second))
        compose.setContent {
            KansoTheme(themeMode = mode) {
                content(photo.value)
            }
        }
        compose.waitForIdle()

        val problems = mutableListOf<String>()
        PHOTOS.forEach { (photoName, argb) ->
            photo.value = solidPhoto(argb)
            compose.waitForIdle()
            val bitmap = drawScreen()
            val measures = try {
                measure(bitmap)
            } finally {
                // A bitmap holds native memory; free it now rather than waiting for the collector.
                bitmap.recycle()
            }
            measures.forEach { m ->
                val shown = m.ratio?.let { String.format(Locale.ROOT, "%.2f", it) } ?: "not measured"
                println("contrast $screen $mode $photoName photo: ${m.what} $shown")
                when {
                    m.ratio == null -> problems += "$photoName photo: the ${m.what} could not be measured: ${m.problem}"
                    m.ratio < m.minimum -> problems += "$photoName photo: the ${m.what} has contrast " +
                        String.format(Locale.ROOT, "%.2f, needs at least %.1f", m.ratio, m.minimum)
                }
            }
        }
        assertTrue(
            "The $screen in $mode is not readable over every photo:\n" + problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    /** Returns a 600 x 800 photo of one colour, [argb]. */
    private fun solidPhoto(argb: Int): Bitmap =
        Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888).apply { eraseColor(argb) }

    /**
     * Draws the activity's content view into a fresh bitmap, which the caller must recycle. The
     * Compose root fills the content view from its top-left corner, so a position in the root is
     * a position in the bitmap. Runs on the main thread (the test thread under Robolectric).
     */
    private fun drawScreen(): Bitmap {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    /**
     * Returns where the node tagged [tag] is laid out, in the root's pixels, from the unmerged
     * tree: from `positionInRoot` and the node's size, never from `boundsInRoot`, which Compose
     * cuts to the visible part of the screen.
     */
    private fun nodeRect(tag: String): Rect {
        val node = compose.onNode(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNode()
        return Rect(node.positionInRoot, node.size.toSize())
    }

    /** Returns [dp] in the screen's pixels. */
    private fun px(dp: Dp): Float = with(compose.density) { dp.toPx() }

    /**
     * Returns the contrast between the lightest and the darkest pixel of [bitmap] inside [text],
     * a text's bounds: the ink against what it is drawn on.
     */
    private fun textExtremesContrast(bitmap: Bitmap, text: Rect): Result<Double> = runCatching {
        var lightest = -1.0
        var darkest = 2.0
        forEachPixel(bitmap, text) { argb ->
            val luminance = relativeLuminance(argb)
            lightest = max(lightest, luminance)
            darkest = min(darkest, luminance)
        }
        check(lightest >= 0.0) { "its bounds $text hold no pixel of the drawn screen" }
        ratio(lightest, darkest)
    }

    /**
     * Returns the contrast of a round icon button's icon against its circle, the button laid out
     * at [button]. The circle's colour is read [ROUND_CONTAINER_OFFSET] below the button's
     * centre, inside the 48 dp circle and below the 24 dp icon; the icon's is the pixel within
     * [ICON_HALF] of the centre that differs most from the circle.
     */
    private fun roundButtonContrast(bitmap: Bitmap, button: Rect): Result<Double> =
        centredInkContrast(bitmap, button, containerOffset = px(ROUND_CONTAINER_OFFSET))

    /**
     * Returns the contrast of the success mark's check against the mark, the mark laid out at
     * [mark]. The mark's colour is read [MARK_CONTAINER_OFFSET] below its centre, inside the
     * 64 dp scalloped shape and below the check; the check's is the pixel within [ICON_HALF] of
     * the centre that differs most from the mark.
     */
    private fun markContrast(bitmap: Bitmap, mark: Rect): Result<Double> =
        centredInkContrast(bitmap, mark, containerOffset = px(MARK_CONTAINER_OFFSET))

    /**
     * Reads the container's colour [containerOffset] pixels below the centre of [node] and
     * returns its contrast with the pixel, within [ICON_HALF] of the centre, that differs most
     * from it: what is drawn in the middle against what it is drawn on.
     */
    private fun centredInkContrast(bitmap: Bitmap, node: Rect, containerOffset: Float): Result<Double> = runCatching {
        check(containerOffset < node.height / 2f) {
            "the container pixel, $containerOffset px below the centre, lies outside the node $node"
        }
        val centre = node.center
        val container = pixelAt(bitmap, centre.x, centre.y + containerOffset)
        val half = px(ICON_HALF)
        val inkArea = Rect(centre.x - half, centre.y - half, centre.x + half, centre.y + half)
        strongestAgainst(bitmap, inkArea, container)
    }

    /**
     * Returns the contrast of the chip's word, laid out at [word], against the chip's container,
     * the chip laid out at [chip]. The container's colour is read [CHIP_CONTAINER_GAP] past the
     * word's end, level with its middle, inside the chip's end padding; the word's is the pixel
     * inside its bounds that differs most from it.
     */
    private fun chipWordContrast(bitmap: Bitmap, word: Rect, chip: Rect): Result<Double> = runCatching {
        val point = Offset(word.right + px(CHIP_CONTAINER_GAP), word.center.y)
        check(chip.contains(point)) { "the container pixel at $point lies outside the chip $chip" }
        val container = pixelAt(bitmap, point.x, point.y)
        strongestAgainst(bitmap, word, container)
    }

    /**
     * Returns the highest contrast between [background], an ARGB colour, and any pixel of
     * [bitmap] inside [area].
     */
    private fun strongestAgainst(bitmap: Bitmap, area: Rect, background: Int): Double {
        val backgroundLuminance = relativeLuminance(background)
        var best = -1.0
        forEachPixel(bitmap, area) { argb ->
            best = max(best, ratio(relativeLuminance(argb), backgroundLuminance))
        }
        check(best >= 0.0) { "the area $area holds no pixel of the drawn screen" }
        return best
    }

    /** Calls [read] with the ARGB colour of every pixel of [bitmap] inside [area]. */
    private inline fun forEachPixel(bitmap: Bitmap, area: Rect, read: (Int) -> Unit) {
        val left = area.left.roundToInt().coerceIn(0, bitmap.width)
        val top = area.top.roundToInt().coerceIn(0, bitmap.height)
        val right = area.right.roundToInt().coerceIn(left, bitmap.width)
        val bottom = area.bottom.roundToInt().coerceIn(top, bitmap.height)
        for (y in top until bottom) {
            for (x in left until right) {
                read(bitmap.getPixel(x, y))
            }
        }
    }

    /** Returns the ARGB colour of the pixel of [bitmap] at ([x], [y]), in the root's pixels. */
    private fun pixelAt(bitmap: Bitmap, x: Float, y: Float): Int {
        val column = x.toInt()
        val row = y.toInt()
        check(column in 0 until bitmap.width && row in 0 until bitmap.height) {
            "the pixel ($column, $row) lies outside the drawn screen of ${bitmap.width} x ${bitmap.height}"
        }
        return bitmap.getPixel(column, row)
    }

    /**
     * Returns the WCAG contrast ratio of two colours of relative luminance [first] and [second],
     * in either order: (lighter + 0.05) / (darker + 0.05).
     */
    private fun ratio(first: Double, second: Double): Double =
        (max(first, second) + 0.05) / (min(first, second) + 0.05)

    /**
     * Returns the WCAG relative luminance of the ARGB colour [argb], from 0 (black) to 1 (white):
     * each 8-bit sRGB channel is turned back into linear light, then the three are weighted by
     * how bright the eye finds red, green and blue (as `ThemeFollowTest` does).
     */
    private fun relativeLuminance(argb: Int): Double {
        val red = linearChannel((argb shr 16) and 0xFF)
        val green = linearChannel((argb shr 8) and 0xFF)
        val blue = linearChannel(argb and 0xFF)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    /**
     * Undoes the sRGB gamma curve on one 8-bit [channel] (0 to 255), giving its linear light
     * from 0 to 1: a straight line for the darkest values, a 2.4 power curve above.
     */
    private fun linearChannel(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    /** Values the tests share. */
    private companion object {
        /** The least contrast text needs (WCAG AA for body text). */
        const val TEXT_MIN_CONTRAST = 4.5

        /** The least contrast an icon or other graphic needs (WCAG AA for non-text). */
        const val ICON_MIN_CONTRAST = 3.0

        /** The made-up Uri the screens say their photo came from. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** The width of the test photos, in pixels. */
        const val PHOTO_WIDTH = 600

        /** The height of the test photos, in pixels: portrait, as most phone photos are. */
        const val PHOTO_HEIGHT = 800

        /** The two photos each screen is drawn over, by name: all white, then all black. */
        val PHOTOS: List<Pair<String, Int>> = listOf(
            "white" to android.graphics.Color.WHITE,
            "black" to android.graphics.Color.BLACK,
        )

        /**
         * Half the side of the square, around a button's or the mark's centre, where its icon is
         * drawn: the 24 dp icons fill it, and the mark's 32 dp check box draws its stroke inside.
         */
        val ICON_HALF = 12.dp

        /** How far below a round button's centre its circle's colour is read: inside its 24 dp radius. */
        val ROUND_CONTAINER_OFFSET = 18.dp

        /**
         * How far below the success mark's centre the mark's colour is read: inside the scallops
         * of the 64 dp cookie (about 24 dp from its centre at their deepest), below its check.
         */
        val MARK_CONTAINER_OFFSET = 20.dp

        /** How far past the end of the chip's word its container's colour is read: inside its end padding. */
        val CHIP_CONTAINER_GAP = 3.dp
    }
}

/**
 * The Robolectric screen every state is drawn on, the same as the screenshot tests': 411 dp wide,
 * 891 dp tall, at xxhdpi, the size of a typical modern phone.
 */
private const val OVER_PHOTO_PHONE = "w411dp-h891dp-xxhdpi"
