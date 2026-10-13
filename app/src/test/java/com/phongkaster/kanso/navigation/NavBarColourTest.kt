package com.phongkaster.kanso.navigation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.abs
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
 * DoD 22: the floating bar is dark in light mode and light in dark mode, in the app's own colours,
 * for each of the six schemes (Kanso blue, From wallpaper and Custom colour #2E7D32, each light
 * and dark), and its selected pill, labels and icons stand out.
 *
 * The bar is hosted inside KansoTheme with each scheme over the theme's background, first with
 * Home selected and then with My edits selected, and read from the drawn pixels (Robolectric's
 * real drawing code, `GraphicsMode.Mode.NATIVE`) and from the scheme KansoTheme publishes:
 * - a pixel of `nav-bar` between two tabs equals `inverseSurface` within 2, with a relative
 *   luminance below 0.15 in light and above 0.6 in dark;
 * - a pixel inside the selected tab's pill, beside its icon, equals `inversePrimary` within 2,
 *   with a contrast of at least 3.0 against `inverseSurface`;
 * - the selected icon's role (`inverseSurface`) reaches 4.5:1 on the pill, and the selected label
 *   (`inversePrimary`) and the other icons and labels (`inverseOnSurface`) reach 4.5:1 on the
 *   bar; inside each icon's bounds, the most frequent colour other than the ground is its role
 *   within 8;
 * - the Camera button's container pixel is its container role (`primary`) within 2, and its
 *   icon's role (`onPrimary`) reaches 3:1 on it.
 *
 * The contrast ratio is WCAG's: (L1 + 0.05) / (L2 + 0.05), the lighter colour's relative
 * luminance over the darker's.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = PHONE)
class NavBarColourTest {

    /** Starts an empty activity to host the bar. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Stops Koin: the test does not use it, but KansoApplication starts it for every Robolectric
     * test, and its state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Kanso blue, light: a dark bar with a standing-out pill, labels and icons. */
    @Test
    fun kanso_blue_light() {
        checkScheme(ThemeMode.Light, ThemeColour.KansoBlue)
    }

    /** Kanso blue, dark: a light bar with a standing-out pill, labels and icons. */
    @Test
    fun kanso_blue_dark() {
        checkScheme(ThemeMode.Dark, ThemeColour.KansoBlue)
    }

    /** From wallpaper (Android 15 has wallpaper colours), light. */
    @Test
    fun wallpaper_light() {
        checkScheme(ThemeMode.Light, ThemeColour.Wallpaper)
    }

    /** From wallpaper, dark. */
    @Test
    fun wallpaper_dark() {
        checkScheme(ThemeMode.Dark, ThemeColour.Wallpaper)
    }

    /** Custom colour #2E7D32, light. */
    @Test
    fun custom_green_light() {
        checkScheme(ThemeMode.Light, CUSTOM_GREEN)
    }

    /** Custom colour #2E7D32, dark. */
    @Test
    fun custom_green_dark() {
        checkScheme(ThemeMode.Dark, CUSTOM_GREEN)
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Hosts the bar in KansoTheme with [mode] and [colour] over the theme's background, checks it
     * with Home selected, then selects My edits and checks it again.
     */
    private fun checkScheme(mode: ThemeMode, colour: ThemeColour) {
        val selected = mutableStateOf(KansoTab.HOME)
        compose.setContent {
            KansoTheme(themeMode = mode, themeColour = colour) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    KansoTabBar(
                        selected = selected.value,
                        onTabSelected = {},
                        onCamera = {},
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
        compose.waitForIdle()
        val look = "$mode, $colour"
        checkBar(KansoTab.HOME, mode == ThemeMode.Dark, look)

        compose.runOnIdle { selected.value = KansoTab.MY_EDITS }
        compose.waitForIdle()
        checkBar(KansoTab.MY_EDITS, mode == ThemeMode.Dark, look)
    }

    /**
     * Checks the drawn bar and button against the scheme KansoTheme applied, with [selected]
     * marked, in the [dark] or light look; [look] names the scheme in failure messages.
     */
    private fun checkBar(selected: KansoTab, dark: Boolean, look: String) {
        val scheme = scheme()
        val bitmap = drawScreen()
        try {
            // The bar between the first two tabs: the inverse surface, dark in light, light in dark.
            val bar = bounds(KANSO_TAB_BAR_TAG)
            val boundary = bounds(kansoTabTag(KansoTab.HOME)).right
            val ground = pixel(bitmap, boundary, bar.center.y)
            assertNear("$look: the bar between two tabs", scheme.inverseSurface, ground, 2)
            if (dark) {
                assertTrue("$look: the bar must be light, luminance ${ground.luminance()}", ground.luminance() > 0.6f)
            } else {
                assertTrue("$look: the bar must be dark, luminance ${ground.luminance()}", ground.luminance() < 0.15f)
            }

            // The selected tab's pill, beside its icon.
            val selectedIcon = iconBounds(selected)
            val pill = pixel(bitmap, selectedIcon.left - px(8.dp), selectedIcon.center.y)
            assertNear("$look: the pill of ${selected.name}", scheme.inversePrimary, pill, 2)
            assertContrast("$look: pill on the bar", scheme.inversePrimary, scheme.inverseSurface, 3.0)

            // The roles of the icons and labels against what they are drawn on.
            assertContrast("$look: selected icon on the pill", scheme.inverseSurface, scheme.inversePrimary, 4.5)
            assertContrast("$look: selected label on the bar", scheme.inversePrimary, scheme.inverseSurface, 4.5)
            assertContrast("$look: other icons and labels on the bar", scheme.inverseOnSurface, scheme.inverseSurface, 4.5)

            // Each icon is drawn in its role: the most frequent colour inside it, ground aside.
            KansoTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val iconGround = if (isSelected) scheme.inversePrimary else scheme.inverseSurface
                val role = if (isSelected) scheme.inverseSurface else scheme.inverseOnSurface
                val ink = mostFrequentInk(bitmap, iconBounds(tab), iconGround)
                assertNear("$look: the ${tab.name} icon (selected: $isSelected)", role, ink, 8)
            }

            // The Camera button: its container is primary, and onPrimary reads on it.
            val button = bounds(KANSO_CAMERA_BUTTON_TAG)
            val container = pixel(bitmap, button.left + px(4.dp), button.center.y)
            assertNear("$look: the Camera button's container", scheme.primary, container, 2)
            assertContrast("$look: the Camera icon on its button", scheme.onPrimary, scheme.primary, 3.0)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Returns the most frequent colour inside [area] of [bitmap] among the pixels that are not
     * within 8 of [ground] in every channel: the colour an icon is drawn in, its smooth edges aside.
     */
    private fun mostFrequentInk(bitmap: Bitmap, area: Rect, ground: Color): Color {
        val counts = HashMap<Int, Int>()
        val left = area.left.roundToInt().coerceIn(0, bitmap.width - 1)
        val right = area.right.roundToInt().coerceIn(left + 1, bitmap.width)
        val top = area.top.roundToInt().coerceIn(0, bitmap.height - 1)
        val bottom = area.bottom.roundToInt().coerceIn(top + 1, bitmap.height)
        for (y in top until bottom) {
            for (x in left until right) {
                val argb = bitmap.getPixel(x, y)
                if (!channelsWithin(Color(argb), ground, 8)) counts[argb] = (counts[argb] ?: 0) + 1
            }
        }
        val best = counts.maxByOrNull { it.value } ?: error("Nothing but the ground was drawn inside $area")
        return Color(best.key)
    }

    /** Returns the bounds of [tab]'s icon, from the unmerged tree, in the root's pixels. */
    private fun iconBounds(tab: KansoTab): Rect = compose.onNode(
        hasTestTag(kansoTabIconTag(tab)) and hasAnyAncestor(hasTestTag(kansoTabTag(tab))),
        useUnmergedTree = true,
    ).fetchSemanticsNode().boundsInRoot

    /** Returns the bounds of the one node tagged [tag], in the root's pixels. */
    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    /** Returns the colour scheme KansoTheme hands to the bar. */
    private fun scheme(): ColorScheme =
        compose.onNodeWithTag(KANSO_THEME_TAG).fetchSemanticsNode().config[KansoColorSchemeKey]

    /** Returns [dp] in pixels at the test screen's density. */
    private fun px(dp: Dp): Float = with(compose.density) { dp.toPx() }

    /** Draws the activity's content view into a fresh bitmap; the caller recycles it. */
    private fun drawScreen(): Bitmap {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    /** Returns the colour of [bitmap]'s pixel nearest to ([x], [y]). */
    private fun pixel(bitmap: Bitmap, x: Float, y: Float): Color = Color(
        bitmap.getPixel(x.roundToInt().coerceIn(0, bitmap.width - 1), y.roundToInt().coerceIn(0, bitmap.height - 1)),
    )

    /** Fails unless [actual] equals [expected] within [tolerance] in each 0-255 colour channel. */
    private fun assertNear(what: String, expected: Color, actual: Color, tolerance: Int) {
        assertTrue("$what: expected ${hex(expected)}, drawn ${hex(actual)}", channelsWithin(expected, actual, tolerance))
    }

    /** Fails unless [foreground] on [background] reaches the contrast ratio [minimum]. */
    private fun assertContrast(what: String, foreground: Color, background: Color, minimum: Double) {
        val ratio = contrast(foreground, background)
        assertTrue("$what: %.2f:1, needs %.1f:1".format(ratio, minimum), ratio >= minimum)
    }

    /** Returns the WCAG contrast ratio of [a] and [b]. */
    private fun contrast(a: Color, b: Color): Double {
        val first = a.luminance().toDouble()
        val second = b.luminance().toDouble()
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }

    /** Tells whether [a] and [b] are within [tolerance] of each other in red, green and blue. */
    private fun channelsWithin(a: Color, b: Color, tolerance: Int): Boolean {
        val first = a.toArgb()
        val second = b.toArgb()
        return listOf(16, 8, 0).all { shift ->
            abs(((first shr shift) and 0xFF) - ((second shr shift) and 0xFF)) <= tolerance
        }
    }

    /** Writes [colour] as `#AARRGGBB`, for failure messages. */
    private fun hex(colour: Color): String = "#%08X".format(colour.toArgb())

    /** Values the tests share. */
    private companion object {
        /** The custom colour of the DoD: green #2E7D32. */
        val CUSTOM_GREEN = ThemeColour.Custom(seedArgb = 0xFF2E7D32.toInt())
    }
}

/** The phone screen the hosted bar is drawn on: 411 x 891 dp at xxhdpi, as in the pictures. */
private const val PHONE = "w411dp-h891dp-xxhdpi"
