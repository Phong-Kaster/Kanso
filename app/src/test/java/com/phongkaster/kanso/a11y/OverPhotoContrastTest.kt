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
 *   the blurred photo, so the darkest or lightest pixel there is the shade and the other the ink.
 *   Because the shade fades from row to row, the title's ink is also compared, row by row, with
 *   that row's own background read just past the title's end, and the lowest of those ratios
 *   must reach [TEXT_MIN_CONTRAST] too ([titleRowsContrast]; a self-check test proves that
 *   measure fails ink fading into its background);
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

    // --- The title's measure checks itself -------------------------------------------------

    /**
     * Proves the title's row-by-row measure can fail, on a drawn-by-hand picture of the very case
     * it exists for: a pale "letter" (a bar of #E0E0E0) whose top half sits on near-black
     * (#101010) and whose bottom half sits on a pale grey (#C8C8C8), as a dark title would if the
     * shade faded to a white photo behind its baseline. The lightest-against-darkest measure
     * passes it (about 13), the row measure must not (about 1.3); and over near-black alone, the
     * row measure must pass, so the failure comes from the pale rows and not from the measure.
     */
    @Test
    fun title_measure_catches_ink_that_fades_into_its_background() {
        val ink = 0xFFE0E0E0.toInt()
        val dark = 0xFF101010.toInt()
        val pale = 0xFFC8C8C8.toInt()

        /** Draws the 100 x 40 test picture: background [lower] below row 20, the bar at x 10 to 19. */
        fun picture(lower: Int): Bitmap = Bitmap.createBitmap(100, 40, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until 40) {
                for (x in 0 until 100) setPixel(x, y, if (y < 20) dark else lower)
                for (x in 10 until 20) setPixel(x, y, ink)
            }
        }

        val fading = picture(lower = pale)
        val plain = picture(lower = dark)
        try {
            val box = Rect(0f, 0f, 30f, 40f)
            val extremes = textExtremesContrast(fading, box).getOrThrow()
            val fadingRows = rowsContrast(fading, left = 0, top = 0, right = 30, bottom = 40, backgroundX = 60)
            val plainRows = rowsContrast(plain, left = 0, top = 0, right = 30, bottom = 40, backgroundX = 60)
            println("contrast self-check: extremes $extremes, rows over a fading background $fadingRows, rows over plain $plainRows")
            assertTrue("the extremes measure should pass the fading picture (it is the gap), got $extremes", extremes >= TEXT_MIN_CONTRAST)
            assertTrue("the row measure must fail ink fading into its background, got $fadingRows", fadingRows < TEXT_MIN_CONTRAST)
            assertTrue("the row measure must pass ink over a plain dark background, got $plainRows", plainRows >= TEXT_MIN_CONTRAST)
        } finally {
            fading.recycle()
            plain.recycle()
        }
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
                    val title = nodeRect(SAVED_TITLE_TAG)
                    add(Measure("title, lightest against darkest pixel", textExtremesContrast(bitmap, title), TEXT_MIN_CONTRAST))
                    add(Measure("title's ink against each row's own background", titleRowsContrast(bitmap, title), TEXT_MIN_CONTRAST))
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
     * Returns the lowest contrast between the title's ink and the background of any pixel row the
     * ink is drawn on, the title laid out at [text].
     *
     * Why rows. The title sits on the top shade, which holds its strength at the top and then
     * fades towards the photo, so the background behind the title changes from row to row: in
     * dark mode over a white photo the shade is dark at the top of the letters and paler at their
     * baseline. The lightest and darkest pixels of the whole box ([textExtremesContrast]) can then
     * be the ink and the darkest shade at the top, and pass, while the bottom of the letters
     * barely shows against the paler shade. So each row's background is read on its own, just
     * past the end of the title ([TITLE_BACKGROUND_GAP] beyond its bounds, where nothing else is
     * drawn and the shade and the plain photo are the same as behind the letters), and the ink is
     * compared with every row's.
     */
    private fun titleRowsContrast(bitmap: Bitmap, text: Rect): Result<Double> = runCatching {
        rowsContrast(
            bitmap = bitmap,
            left = text.left.roundToInt(),
            top = text.top.roundToInt(),
            right = text.right.roundToInt(),
            bottom = text.bottom.roundToInt(),
            backgroundX = (text.right + px(TITLE_BACKGROUND_GAP)).roundToInt(),
        )
    }

    /**
     * Returns the lowest contrast between the ink drawn in the box [left] to [right] (exclusive),
     * [top] to [bottom] (exclusive), of [bitmap] and the background of each row that ink is on.
     * A row's background is its pixel at [backgroundX], outside the box. Throws when the box or
     * the background column lies outside the bitmap or the box holds too few rows of ink.
     *
     * 1. The ink's colour: the pixel of the box that differs most from its own row's background.
     *    Text is drawn in one colour, and only where a glyph covers a pixel fully is it pure ink;
     *    the softened edges blend it with the background, so they differ less.
     * 2. The rows the ink is on: each row holding a pixel within [INK_MATCH] per channel of the
     *    ink. A row whose background has itself come close to the ink counts too: there the
     *    letters are lost, which is exactly what must fail.
     * 3. The ratio of the ink against each of those rows' background; the lowest is returned.
     */
    private fun rowsContrast(bitmap: Bitmap, left: Int, top: Int, right: Int, bottom: Int, backgroundX: Int): Double {
        check(left in 0 until right && right <= bitmap.width && top in 0 until bottom && bottom <= bitmap.height) {
            "the box ($left, $top) to ($right, $bottom) does not lie inside the drawn screen of ${bitmap.width} x ${bitmap.height}"
        }
        check(backgroundX in right until bitmap.width) {
            "the background column $backgroundX does not lie between the box's end $right and the screen's edge ${bitmap.width}"
        }
        val backgrounds = IntArray(bottom - top) { row -> bitmap.getPixel(backgroundX, top + row) }

        var ink = 0
        var inkRatio = -1.0
        for (y in top until bottom) {
            val background = relativeLuminance(backgrounds[y - top])
            for (x in left until right) {
                val argb = bitmap.getPixel(x, y)
                val r = ratio(relativeLuminance(argb), background)
                if (r > inkRatio) {
                    inkRatio = r
                    ink = argb
                }
            }
        }
        val inkLuminance = relativeLuminance(ink)

        var lowest = Double.MAX_VALUE
        var inkRows = 0
        for (y in top until bottom) {
            val holdsInk = (left until right).any { x -> isNearColour(bitmap.getPixel(x, y), ink, INK_MATCH) }
            if (holdsInk) {
                inkRows++
                lowest = min(lowest, ratio(inkLuminance, relativeLuminance(backgrounds[y - top])))
            }
        }
        check(inkRows >= MIN_INK_ROWS) { "only $inkRows row(s) of the box hold its ink, at least $MIN_INK_ROWS expected" }
        return lowest
    }

    /** Tells whether the ARGB colours [first] and [second] differ by at most [tolerance] in each of red, green and blue. */
    private fun isNearColour(first: Int, second: Int, tolerance: Int): Boolean =
        listOf(16, 8, 0).all { shift -> kotlin.math.abs(((first shr shift) and 0xFF) - ((second shr shift) and 0xFF)) <= tolerance }

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

        /**
         * How far past the end of Saved's title each row's background is read: clear of the last
         * letter's softened edge, on the shade over the photo, where nothing else is drawn.
         */
        val TITLE_BACKGROUND_GAP = 4.dp

        /** How far per channel (0 to 255) a pixel may be from the title's ink and still count as ink. */
        const val INK_MATCH = 24

        /** The fewest rows of ink the title's box must hold for its measure to mean anything. */
        const val MIN_INK_ROWS = 3
    }
}

/**
 * The Robolectric screen every state is drawn on, the same as the screenshot tests': 411 dp wide,
 * 891 dp tall, at xxhdpi, the size of a typical modern phone.
 */
private const val OVER_PHOTO_PHONE = "w411dp-h891dp-xxhdpi"
