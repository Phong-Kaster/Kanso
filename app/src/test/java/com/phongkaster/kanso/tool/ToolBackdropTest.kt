package com.phongkaster.kanso.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.common.BACKDROP_BLUR_RADIUS
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_CARD_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolViewModel
import kotlin.math.abs
import kotlin.math.max
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 27, the machine pre-check: beside a narrow photo on a tool screen, the card's empty space is
 * filled with a soft, blurred copy of the photo, on Android 15 and on Android 11 alike.
 *
 * The Edit screen shows [ToolFixtures.narrowStripes], a 300 x 800 photo whose upper half is
 * red-and-white vertical stripes 8 px wide and whose lower half blue-and-white stripes, in Kanso
 * blue, light and then dark. In the space between the card's left edge and the photo's:
 * - at least 90 % of the sampled pixels differ from the card's own colour by more than 8 in some
 *   channel (the space is filled, not left as the bare card);
 * - the mean colour of the upper half is reddish and of the lower half bluish (it comes from the
 *   photo);
 * - horizontally adjacent pixels differ by at most 12 in every channel (it is soft: no stripe
 *   survives);
 * and inside the photo, pixels of adjacent stripes differ by at least 100 in some channel (the
 *   photo itself stays sharp).
 *
 * Android 11 is the fallback, where Compose's blur draws nothing: all four checks always hold
 * there, carried by the stretched tiny copy alone. On Android 15 a control comes first: a striped
 * box drawn with the same blur. If this machine does not soften it (Robolectric's drawing may
 * ignore the blur), the Android 15 pass prints "blur not driven by machine on this host" and only
 * checks that the space is filled; a person checks the real blur on the phone.
 *
 * Pixels are read by drawing the activity's content view into a bitmap (never captureToImage,
 * which times out here). Each Android version is a test of its own.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ToolBackdropTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Android 11, where `blur` draws nothing: the stretched tiny copy passes all four checks. */
    @Test
    @Config(sdk = [30])
    fun the_space_beside_a_narrow_photo_is_a_soft_copy_of_it_on_android_11() {
        checkBackdrop(runControl = false)
    }

    /**
     * Android 15: the control decides whether this machine applies the blur; the four checks run
     * if it does, the "filled" check alone if it does not.
     */
    @Test
    @Config(sdk = [35])
    fun the_space_beside_a_narrow_photo_is_a_soft_copy_of_it_on_android_15() {
        checkBackdrop(runControl = true)
    }

    /**
     * Draws the control first when [runControl], then the Edit screen on the striped photo in
     * light and in dark, and checks the backdrop each time.
     */
    private fun checkBackdrop(runControl: Boolean) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val stripes = ToolFixtures.narrowStripes()
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = ToolViewModel(
            tool = Tool.EDIT,
            photoUri = PHOTO_URI,
            loader = FakePhotoLoader(context, stripes),
            probe = ViewModelClearedProbe(),
        )
        var showControl by mutableStateOf(runControl)
        var mode by mutableStateOf(ThemeMode.Light)
        var cardColour = 0
        compose.setContent {
            KansoTheme(themeMode = mode) {
                if (showControl) {
                    BlurControl(stripes)
                } else {
                    // The colour of the card itself, as the theme gives it to a card.
                    val colour = CardDefaults.cardColors().containerColor.toArgb()
                    SideEffect { cardColour = colour }
                    ToolScreen(viewModel = viewModel, onBack = {}, onDone = {})
                }
            }
        }
        compose.waitForIdle()

        val blurDriven = if (runControl) {
            val box = compose.onNodeWithTag(CONTROL_TAG).fetchSemanticsNode().boundsInRoot
            val largest = drawn { bitmap -> largestAdjacentStep(bitmap, box.deflate(box.width / 8f)) }
            val driven = largest <= SOFT_STEP
            if (!driven) println("blur not driven by machine on this host")
            showControl = false
            driven
        } else {
            // Android 11: the fallback is always asserted in full.
            true
        }

        listOf(ThemeMode.Light, ThemeMode.Dark).forEach { shownMode ->
            mode = shownMode
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitForIdle()
            checkScreen(shownMode, cardColour, fullChecks = blurDriven)
        }
    }

    /**
     * Reads the drawn tool screen in [mode] and runs the checks of the class description against
     * [cardColour]; only the "filled" check unless [fullChecks].
     */
    private fun checkScreen(mode: ThemeMode, cardColour: Int, fullChecks: Boolean) {
        val card = compose.onNodeWithTag(TOOL_CARD_TAG).fetchSemanticsNode().boundsInRoot
        val image = compose.onNodeWithTag(TOOL_IMAGE_TAG).fetchSemanticsNode().boundsInRoot
        val gapLeft = card.left + EDGE_MARGIN_PX
        val gapRight = image.left - EDGE_MARGIN_PX
        assertTrue("$mode: no space beside the photo $image in the card $card", gapRight - gapLeft > 20f)
        // Clear of the card's rounded corners at the top and bottom, and of the photo's middle,
        // where its red half meets its blue half.
        val corner = CORNER_MARGIN_PX
        val middle = (card.top + card.bottom) / 2f
        val upper = Rect(gapLeft, card.top + corner, gapRight, middle - MIDDLE_MARGIN_PX)
        val lower = Rect(gapLeft, middle + MIDDLE_MARGIN_PX, gapRight, card.bottom - corner)

        drawn { bitmap ->
            val samples = sample(bitmap, upper) + sample(bitmap, lower)
            val filled = samples.count { channelDistance(it, cardColour) > FILLED_DISTANCE }
            assertTrue(
                "$mode: only $filled of ${samples.size} pixels beside the photo differ from the card's colour",
                filled >= samples.size * 0.9,
            )
            if (!fullChecks) return@drawn

            val upperMean = meanOf(sample(bitmap, upper))
            val lowerMean = meanOf(sample(bitmap, lower))
            assertTrue(
                "$mode: the upper space is not reddish: mean RGB ${upperMean.toList()}",
                upperMean[0] > upperMean[2] + MEAN_LEAD,
            )
            assertTrue(
                "$mode: the lower space is not bluish: mean RGB ${lowerMean.toList()}",
                lowerMean[2] > lowerMean[0] + MEAN_LEAD,
            )

            val step = max(largestAdjacentStep(bitmap, upper), largestAdjacentStep(bitmap, lower))
            assertTrue("$mode: the space beside the photo is not soft: adjacent pixels differ by $step", step <= SOFT_STEP)

            // Inside the photo: the centres of neighbouring stripes on a row of the upper half.
            val scale = image.width / PHOTO_WIDTH_PX
            val y = (image.top + image.height / 4f).toInt()
            val weakest = (0 until STRIPE_PAIRS).minOf { i ->
                val x1 = (image.left + (i * ToolFixtures.STRIPE_PX + ToolFixtures.STRIPE_PX / 2f) * scale).toInt()
                val x2 = (image.left + ((i + 1) * ToolFixtures.STRIPE_PX + ToolFixtures.STRIPE_PX / 2f) * scale).toInt()
                channelDistance(bitmap.getPixel(x1, y), bitmap.getPixel(x2, y))
            }
            assertTrue("$mode: the photo's stripes are not sharp: neighbours differ by only $weakest", weakest >= SHARP_STEP)
        }
    }

    /**
     * Draws the activity's content view into a fresh bitmap, hands it to [read], then frees it.
     * The Compose root fills the content view from its top-left corner, so node bounds in the
     * root are pixel positions in the bitmap.
     */
    private fun <T> drawn(read: (Bitmap) -> T): T {
        var result: T? = null
        compose.runOnUiThread {
            val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
            val view = checkNotNull(content.getChildAt(0)) { "The activity has no content view yet" }
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            try {
                result = read(bitmap)
            } finally {
                bitmap.recycle()
            }
        }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /** The pixels of [area] in [bitmap], every [SAMPLE_STEP_PX] pixels each way. */
    private fun sample(bitmap: Bitmap, area: Rect): List<Int> {
        val pixels = mutableListOf<Int>()
        var y = area.top.toInt()
        while (y < area.bottom.toInt()) {
            var x = area.left.toInt()
            while (x < area.right.toInt()) {
                pixels += bitmap.getPixel(x, y)
                x += SAMPLE_STEP_PX
            }
            y += SAMPLE_STEP_PX
        }
        check(pixels.isNotEmpty()) { "No pixel to sample in $area" }
        return pixels
    }

    /**
     * The largest difference, in any channel, between two horizontally adjacent pixels of [area],
     * on every [SAMPLE_STEP_PX]-th row.
     */
    private fun largestAdjacentStep(bitmap: Bitmap, area: Rect): Int {
        var largest = 0
        var y = area.top.toInt()
        while (y < area.bottom.toInt()) {
            for (x in area.left.toInt() until area.right.toInt() - 1) {
                largest = max(largest, channelDistance(bitmap.getPixel(x, y), bitmap.getPixel(x + 1, y)))
            }
            y += SAMPLE_STEP_PX
        }
        return largest
    }

    /** The mean red, green and blue of [pixels]. */
    private fun meanOf(pixels: List<Int>): DoubleArray = doubleArrayOf(
        pixels.map { android.graphics.Color.red(it) }.average(),
        pixels.map { android.graphics.Color.green(it) }.average(),
        pixels.map { android.graphics.Color.blue(it) }.average(),
    )

    /** The largest difference between [a] and [b] in any of red, green and blue. */
    private fun channelDistance(a: Int, b: Int): Int = maxOf(
        abs(android.graphics.Color.red(a) - android.graphics.Color.red(b)),
        abs(android.graphics.Color.green(a) - android.graphics.Color.green(b)),
        abs(android.graphics.Color.blue(a) - android.graphics.Color.blue(b)),
    )

    /**
     * The blur control: [stripes] stretched over a 200 dp square with the very blur the backdrop
     * uses, so the test can see whether this machine softens it.
     */
    @Composable
    private fun BlurControl(stripes: Bitmap) {
        val image = remember(stripes) { stripes.asImageBitmap() }
        Box(
            modifier = Modifier
                .size(200.dp)
                .testTag(CONTROL_TAG),
        ) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(BACKDROP_BLUR_RADIUS),
            )
        }
    }

    /** Values the checks share. */
    private companion object {
        /** The photo the fake loader stands in for; nothing is read from it. */
        const val PHOTO_URI = "content://media/external/images/media/27"

        /** The width of [ToolFixtures.narrowStripes], in pixels. */
        const val PHOTO_WIDTH_PX = 300f

        /** The test tag on the blur control. */
        const val CONTROL_TAG = "blur-control"

        /** A pixel beside the photo counts as filled when it differs from the card by more than this. */
        const val FILLED_DISTANCE = 8

        /** Adjacent pixels that differ by at most this in every channel read as soft. */
        const val SOFT_STEP = 12

        /** Neighbouring stripes of the photo must differ by at least this in some channel. */
        const val SHARP_STEP = 100

        /** How far the reddish (or bluish) channel's mean must lead the other one. */
        const val MEAN_LEAD = 40.0

        /** How many pairs of neighbouring stripes are compared inside the photo. */
        const val STRIPE_PAIRS = 10

        /** Distance kept from the card's straight edges and the photo's edge, in pixels. */
        const val EDGE_MARGIN_PX = 4f

        /** Distance kept from the card's top and bottom, clear of its rounded corners, in pixels. */
        const val CORNER_MARGIN_PX = 120f

        /** Distance kept from the card's middle, where red meets blue, in pixels. */
        const val MIDDLE_MARGIN_PX = 80f

        /** The spacing of the sampled pixels, in pixels. */
        const val SAMPLE_STEP_PX = 4
    }
}
