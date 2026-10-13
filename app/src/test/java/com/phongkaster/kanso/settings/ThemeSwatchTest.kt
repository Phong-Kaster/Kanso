package com.phongkaster.kanso.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.openTab
import com.phongkaster.kanso.navigation.scrollAboveBar
import com.phongkaster.kanso.navigation.tapAboveBar
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_SWATCH_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_KANSO_BLUE_SWATCH_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_KANSO_BLUE_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_WALLPAPER_SWATCH_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_WALLPAPER_TAG
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
import com.phongkaster.kanso.ui.theme.seedColorScheme
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Drives the real app to Settings on a fresh install and checks the colour rows' swatches
 * (DoD 2): on Android 15 the rows read Kanso blue, From wallpaper, Custom colour, top to bottom,
 * with Kanso blue selected, and each row holds a swatch whose middle pixel is exactly its
 * choice's colour; tapping each row re-themes the app in that choice's scheme. On Android 11
 * there is no wallpaper row, so only Kanso blue and Custom colour are offered.
 *
 * The colours the swatches must show:
 * - Kanso blue: the light `primary` of the Kanso blue scheme, `seedColorScheme(KANSO_BLUE_SEED,
 *   dark = false).primary`;
 * - From wallpaper: the light `primary` the system builds from the wallpaper,
 *   `dynamicLightColorScheme(context).primary`;
 * - Custom colour: the last custom seed, which on a fresh install is Kanso blue's seed itself,
 *   and once a colour was picked stays that colour while Kanso blue or the wallpaper is chosen.
 *
 * The pixels are read by drawing the activity's content view into a bitmap, as `ScreenPixels.kt`
 * does (Compose's own `captureToImage` times out under Robolectric here), with Robolectric's real
 * drawing code switched on (`GraphicsMode.Mode.NATIVE`). A row is scrolled into view before its
 * swatch is read, since only what is on screen is drawn.
 *
 * Settings is opened with the floating tab bar's Settings tab (`TabTaps.openTab`), and rows are
 * tapped with `TabTaps.tapAboveBar`, because a row scrolled into view on this 320 x 470 dp screen
 * can rest under the bar.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "notnight")
class ThemeSwatchTest {

    /** Launches MainActivity before each test and lets the test find and tap its Compose nodes. */
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** The app context, used to read the same string resources and system colours the app reads. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test. Koin's state outlives the test's Application inside the same
     * JVM, and starting it twice fails.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * On Android 15 a fresh install offers three colour rows, Kanso blue, From wallpaper and
     * Custom colour, one below the other in that order, with Kanso blue selected; each row's
     * swatch shows its own colour in its middle pixel.
     */
    @Test
    fun android_15_shows_three_colour_rows_each_with_a_swatch_of_its_colour() {
        waitForHome()
        openSettings()

        assertRowsInOrder(THEME_COLOUR_KANSO_BLUE_TAG, THEME_COLOUR_WALLPAPER_TAG, THEME_COLOUR_CUSTOM_TAG)
        compose.onNodeWithTag(THEME_COLOUR_KANSO_BLUE_TAG).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(THEME_COLOUR_WALLPAPER_TAG).assertIsNotSelected()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).assertIsNotSelected()

        assertSwatch(
            rowTag = THEME_COLOUR_KANSO_BLUE_TAG,
            swatchTag = THEME_COLOUR_KANSO_BLUE_SWATCH_TAG,
            expected = kansoBluePrimary(),
            what = "Kanso blue's light primary",
        )
        assertSwatch(
            rowTag = THEME_COLOUR_WALLPAPER_TAG,
            swatchTag = THEME_COLOUR_WALLPAPER_SWATCH_TAG,
            expected = dynamicLightColorScheme(context).primary,
            what = "the wallpaper's light primary",
        )
        assertSwatch(
            rowTag = THEME_COLOUR_CUSTOM_TAG,
            swatchTag = THEME_COLOUR_CUSTOM_SWATCH_TAG,
            expected = KANSO_BLUE_SEED,
            what = "Kanso blue's seed, the custom seed of a fresh install",
        )
    }

    /**
     * On Android 15, tapping each colour row saves that choice and re-themes the app in its
     * scheme within ten seconds of real time counted from the tap. The rows are tapped so that each tap moves the app
     * away from the scheme it showed before (the custom scheme of a fresh install grows from
     * Kanso blue's seed, so it looks like Kanso blue and is tapped after the wallpaper instead):
     * From wallpaper, Kanso blue, From wallpaper again, then Custom colour.
     */
    @Test
    fun tapping_each_colour_row_applies_its_scheme() {
        // What KansoTheme shows for From wallpaper on Android 12 and later, in the light look.
        val wallpaper = dynamicLightColorScheme(context)
        val kansoBlue = kansoBlueScheme()
        // KansoTheme builds the custom scheme from the saved seed, an ARGB number.
        val custom = seedColorScheme(Color(KANSO_BLUE_SEED.toArgb()), dark = false)
        waitForHome()
        openSettings()

        choose(THEME_COLOUR_WALLPAPER_TAG, saved = ThemeColour.Wallpaper, scheme = wallpaper)
        choose(THEME_COLOUR_KANSO_BLUE_TAG, saved = ThemeColour.KansoBlue, scheme = kansoBlue)
        choose(THEME_COLOUR_WALLPAPER_TAG, saved = ThemeColour.Wallpaper, scheme = wallpaper)
        choose(
            THEME_COLOUR_CUSTOM_TAG,
            saved = ThemeColour.Custom(KANSO_BLUE_SEED.toArgb()),
            scheme = custom,
        )
    }

    /**
     * On Android 15 the Custom row's swatch shows the last custom colour picked while another
     * colour choice is selected, not Kanso blue's seed (DoD 2: "the colour of the last custom
     * seed"). Custom is tapped and its colour set to green #2E7D32; then Kanso blue and From
     * wallpaper are tapped in turn, and each time the Custom swatch's middle pixel is still that
     * green. Tapping Custom again re-themes the app in green, the colour the swatch showed.
     *
     * The green is written through the app's settings repository, which is what the colour
     * picker's save does once a drag settles; the engine's picker itself is not dragged (its
     * positions cannot be aimed at an exact colour) and its hex field is not typed into (it reads
     * the digits as a decimal number).
     */
    @Test
    fun the_custom_swatch_shows_the_last_custom_colour_while_another_choice_is_selected() {
        // KansoTheme builds the custom scheme from the saved seed, an ARGB number.
        val green = seedColorScheme(Color(CUSTOM_GREEN.toArgb()), dark = false)
        waitForHome()
        openSettings()

        // Tapped as a person would: on a fresh install Custom starts from Kanso blue's seed.
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).tapAboveBar()
        awaitSavedThemeColour(ThemeColour.Custom(KANSO_BLUE_SEED.toArgb()))
        // What the picker's save writes once a drag towards green has settled.
        runBlocking { repository().setThemeColour(ThemeColour.Custom(CUSTOM_GREEN.toArgb())) }
        awaitSavedThemeColour(ThemeColour.Custom(CUSTOM_GREEN.toArgb()))
        compose.waitUntil(timeoutMillis = 10_000) {
            // The new colour reaches the screen through the main thread's queue; run it.
            shadowOf(Looper.getMainLooper()).idle()
            appliedScheme().primary == green.primary
        }

        choose(THEME_COLOUR_KANSO_BLUE_TAG, saved = ThemeColour.KansoBlue, scheme = kansoBlueScheme())
        assertCustomSwatchIsGreen(selection = "Kanso blue")

        choose(
            THEME_COLOUR_WALLPAPER_TAG,
            saved = ThemeColour.Wallpaper,
            // What KansoTheme shows for From wallpaper on Android 12 and later, in the light look.
            scheme = dynamicLightColorScheme(context),
        )
        assertCustomSwatchIsGreen(selection = "From wallpaper")

        // The swatch promised green: tapping Custom brings exactly that colour's scheme back.
        choose(
            THEME_COLOUR_CUSTOM_TAG,
            saved = ThemeColour.Custom(CUSTOM_GREEN.toArgb()),
            scheme = green,
        )
    }

    /**
     * On Android 11 there are no wallpaper colours: two colour rows, Kanso blue then Custom
     * colour, Kanso blue selected, no wallpaper row and no wallpaper swatch; the two swatches
     * still show their own colours.
     */
    @Test
    @Config(sdk = [30])
    fun android_11_shows_two_colour_rows_with_swatches_and_no_wallpaper_row() {
        waitForHome()
        openSettings()

        assertRowsInOrder(THEME_COLOUR_KANSO_BLUE_TAG, THEME_COLOUR_CUSTOM_TAG)
        assertTrue(
            "The wallpaper row must not be offered below Android 12",
            compose.onAllNodesWithTag(THEME_COLOUR_WALLPAPER_TAG).fetchSemanticsNodes().isEmpty(),
        )
        assertTrue(
            "No wallpaper swatch may be drawn below Android 12",
            compose.onAllNodesWithTag(THEME_COLOUR_WALLPAPER_SWATCH_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isEmpty(),
        )
        compose.onNodeWithTag(THEME_COLOUR_KANSO_BLUE_TAG).performScrollTo().assertIsSelected()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).assertIsNotSelected()

        assertSwatch(
            rowTag = THEME_COLOUR_KANSO_BLUE_TAG,
            swatchTag = THEME_COLOUR_KANSO_BLUE_SWATCH_TAG,
            expected = kansoBluePrimary(),
            what = "Kanso blue's light primary",
        )
        assertSwatch(
            rowTag = THEME_COLOUR_CUSTOM_TAG,
            swatchTag = THEME_COLOUR_CUSTOM_SWATCH_TAG,
            expected = KANSO_BLUE_SEED,
            what = "Kanso blue's seed, the custom seed of a fresh install",
        )
    }

    /**
     * Taps the colour row tagged [rowTag] and fails unless, within ten seconds of real time
     * counted from the tap, the settings file holds [saved] and the scheme the app hands to every
     * screen matches [scheme], the choice's own light scheme (DoD 2); then only that row may be
     * selected. The ten seconds are one budget for the whole tap: the save uses some of it, and
     * the recolouring gets only what is left, so a slow save cannot buy the recolouring more time.
     *
     * "Matches" means equal in the seven colour roles DoD 1 compares (see [rolesOf]): primary,
     * onPrimary, primaryContainer, secondary, tertiary, background and surface. Comparing seven
     * roles, not only `primary`, catches a tap that brought the right `primary` with another
     * seed's scheme. Before the tap the app must show another `primary`, so the wait proves the
     * tap changed the scheme.
     *
     * The seven roles are waited for together and then checked one by one, so a failure names the
     * role that differs.
     */
    private fun choose(rowTag: String, saved: ThemeColour, scheme: ColorScheme) {
        assertNotEquals(
            "Before $rowTag is tapped the app must show another scheme, or the tap proves nothing",
            scheme.primary,
            appliedScheme().primary,
        )
        val row = compose.onNodeWithTag(rowTag).performScrollTo()
        // One deadline on the wall clock for the whole tap, taken just before it: the save and
        // the recolouring below share these ten seconds instead of getting ten seconds each.
        val deadline = System.currentTimeMillis() + TAP_TO_SCHEME_MILLIS
        // Tapped above the floating bar (the row is already in view, so its scroll is a no-op).
        row.tapAboveBar()
        awaitSavedThemeColour(saved, deadline)
        val expected = rolesOf(scheme)
        // runCatching holds on to a failed wait instead of ending the test at once: the checks
        // below then name the role that is still wrong, and getOrThrow() fails the test anyway if
        // every role only matched after the deadline.
        val waited = runCatching {
            // Only what is left of the ten seconds; at least 1 ms, so the time is never negative.
            val remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(1L)
            compose.waitUntil(timeoutMillis = remaining) { rolesOf(appliedScheme()) == expected }
            // waitUntil looks at the scheme once before it reads its own clock, so a scheme that
            // arrived only after the deadline would still get through it; this check fails it.
            assertTrue(
                "Tapping $rowTag must recolour the app within ten seconds of the tap",
                System.currentTimeMillis() <= deadline,
            )
        }
        expected.zip(rolesOf(appliedScheme())).forEach { (want, got) ->
            assertEquals("${want.first} of the scheme after tapping $rowTag", want.second, got.second)
        }
        waited.getOrThrow()
        compose.onNodeWithTag(rowTag).assertIsSelected()
        COLOUR_ROW_TAGS.filter { it != rowTag && rowExists(it) }.forEach { other ->
            compose.onNodeWithTag(other).assertIsNotSelected()
        }
    }

    /**
     * Scrolls the row tagged [rowTag] into view above the floating bar, then fails unless it holds the swatch tagged
     * [swatchTag], at least 24 dp across, whose middle pixel equals [expected] within
     * [TOLERANCE] per channel; [what] names that colour in the failure message.
     *
     * The swatch is found in the unmerged tree (its row merges it for screen readers), and its
     * middle is worked out from `positionInRoot` and its size, never from `boundsInRoot`, which
     * Compose clips to the visible part of the screen.
     */
    private fun assertSwatch(rowTag: String, swatchTag: String, expected: Color, what: String) {
        // Above the floating bar, not merely into view: a row left under the bar would be read
        // through the bar's pixels.
        compose.onNodeWithTag(rowTag).scrollAboveBar(compose)
        compose.waitForIdle()
        val swatch = compose.onNode(
            hasTestTag(swatchTag) and hasAnyAncestor(hasTestTag(rowTag)),
            useUnmergedTree = true,
        ).fetchSemanticsNode()

        val minimum = with(compose.density) { SWATCH_MIN_SIZE.toPx() }
        assertTrue(
            "The swatch $swatchTag must be at least $SWATCH_MIN_SIZE across, was ${swatch.size}",
            swatch.size.width >= minimum && swatch.size.height >= minimum,
        )
        val centreX = (swatch.positionInRoot.x + swatch.size.width / 2f).roundToInt()
        val centreY = (swatch.positionInRoot.y + swatch.size.height / 2f).roundToInt()
        assertColourNear("The middle of $swatchTag should be $what", expected, drawnPixelAt(centreX, centreY))
    }

    /**
     * Lets the press ripples of the taps before it fade out, then fails unless the Custom row's
     * swatch shows [CUSTOM_GREEN] in its middle pixel; [selection] names the colour choice that
     * is selected, for the failure message.
     *
     * A tap on a row draws a ripple over it, the Custom row's swatch included, and that ripple is
     * an Android drawable that fades on Robolectric's clock, not on Compose's, so both clocks are
     * moved on (see [advanceBothClocks]) before the pixel is read.
     */
    private fun assertCustomSwatchIsGreen(selection: String) {
        advanceBothClocks(RIPPLE_SETTLE_MILLIS)
        assertSwatch(
            rowTag = THEME_COLOUR_CUSTOM_TAG,
            swatchTag = THEME_COLOUR_CUSTOM_SWATCH_TAG,
            expected = CUSTOM_GREEN,
            what = "the last custom colour, green, while $selection is selected (not Kanso blue's seed)",
        )
    }

    /**
     * Moves Compose's test clock and Robolectric's main-thread clock on by [millis] together:
     * Compose's animations run on the first, Android's own drawables (such as a press ripple) on
     * the second.
     */
    private fun advanceBothClocks(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
    }

    /**
     * Draws the activity's content view into a fresh bitmap and returns the colour of the pixel
     * at ([x], [y]). The Compose root fills the content view from its top-left corner, so a
     * node's position in the root is its position in the bitmap. Runs on the main thread (the
     * test thread under Robolectric).
     */
    private fun drawnPixelAt(x: Int, y: Int): Color {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        return try {
            view.draw(Canvas(bitmap))
            check(x in 0 until bitmap.width && y in 0 until bitmap.height) {
                "($x, $y) lies outside the drawn screen of ${bitmap.width} x ${bitmap.height}"
            }
            Color(bitmap.getPixel(x, y))
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Fails with [message] unless [actual] equals [expected] within [TOLERANCE] in each of its
     * alpha, red, green and blue channels (0 to 255).
     */
    private fun assertColourNear(message: String, expected: Color, actual: Color) {
        val wanted = expected.toArgb()
        val drawn = actual.toArgb()
        val close = listOf(24, 16, 8, 0).all { shift ->
            abs(((wanted shr shift) and 0xFF) - ((drawn shr shift) and 0xFF)) <= TOLERANCE
        }
        assertTrue("$message: expected ${hex(wanted)}, drawn ${hex(drawn)}", close)
    }

    /** Writes an ARGB colour number as `#AARRGGBB`, for failure messages. */
    private fun hex(argb: Int): String = "#%08X".format(argb)

    /**
     * Fails unless each of [tags] names exactly one selectable colour row, and the rows lie
     * strictly one below the other in the order given (their tops in the root's coordinates).
     *
     * The tops are read with `positionInRoot`, where the row is laid out, and not with
     * `boundsInRoot`: Compose clips `boundsInRoot` to the part of the screen that is visible, so
     * a row scrolled out of view reports a top of 0 and the order would look wrong.
     */
    private fun assertRowsInOrder(vararg tags: String) {
        val tops = tags.map { tag ->
            val row = compose.onNodeWithTag(tag).fetchSemanticsNode()
            // A selectable row (a radio-button row) carries the Selected property, on or off.
            assertTrue("Row $tag must be selectable", SemanticsProperties.Selected in row.config)
            tag to row.positionInRoot.y
        }
        tops.zipWithNext().forEach { (upper, lower) ->
            assertTrue(
                "Row ${upper.first} (top ${upper.second}) must lie above ${lower.first} " +
                    "(top ${lower.second}); order found: $tops",
                upper.second < lower.second,
            )
        }
        val shown = COLOUR_ROW_TAGS.sumOf { compose.onAllNodesWithTag(it).fetchSemanticsNodes().size }
        assertEquals("Expected exactly the colour rows ${tags.toList()}", tags.size, shown)
    }

    /** Tells whether a row tagged [tag] is on the Settings screen at all. */
    private fun rowExists(tag: String): Boolean =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /**
     * Returns Kanso blue's light scheme, the one KansoTheme grows from Kanso blue's seed when the
     * phone is in day mode, as it is in these tests.
     */
    private fun kansoBlueScheme(): ColorScheme = seedColorScheme(KANSO_BLUE_SEED, dark = false)

    /** Returns the light `primary` of Kanso blue, the scheme grown from Kanso blue's seed. */
    private fun kansoBluePrimary(): Color = kansoBlueScheme().primary

    /** Returns the colour scheme KansoTheme currently hands to every screen. */
    private fun appliedScheme(): ColorScheme =
        compose.onNodeWithTag(KANSO_THEME_TAG).fetchSemanticsNode().config[KansoColorSchemeKey]

    /**
     * Returns the seven colour roles DoD 1 compares, each with its name, in a fixed order, as
     * `KansoBlueThemeTest` does: two schemes that agree in all seven are taken to be the same.
     */
    private fun rolesOf(scheme: ColorScheme): List<Pair<String, Color>> = listOf(
        "primary" to scheme.primary,
        "onPrimary" to scheme.onPrimary,
        "primaryContainer" to scheme.primaryContainer,
        "secondary" to scheme.secondary,
        "tertiary" to scheme.tertiary,
        "background" to scheme.background,
        "surface" to scheme.surface,
    )

    /** Reads the theme colour saved in the settings file right now. */
    private fun savedThemeColour(): ThemeColour = runBlocking { repository().themeColour.first() }

    /**
     * Waits, in real time, until the saved theme colour is [expected], and fails unless it is by
     * then. The write runs on a background thread, which Compose's test clock does not wait for.
     *
     * [deadline] is the moment on the wall clock (`System.currentTimeMillis()`) at which the wait
     * gives up: by default ten seconds from the call, while [choose] passes the deadline it took at
     * its tap, so the save only spends part of the tap's ten seconds.
     */
    private fun awaitSavedThemeColour(
        expected: ThemeColour,
        // A default value is worked out afresh at every call, so each call gets its own ten seconds.
        deadline: Long = System.currentTimeMillis() + 10_000,
    ) {
        while (savedThemeColour() != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            compose.waitForIdle()
        }
        assertEquals(expected, savedThemeColour())
    }

    /** Returns the app's one settings repository from Koin. */
    private fun repository(): SettingsRepository = KoinPlatform.getKoin().get()

    /** Taps the Settings tab on the floating bar and waits until the Settings screen is shown. */
    private fun openSettings() {
        openTab(compose, KansoTab.SETTINGS)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home is on screen, found by its own tag. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Values the tests share. */
    private companion object {
        /** How far, per colour channel (0 to 255), a drawn pixel may be from the expected colour. */
        const val TOLERANCE = 2

        /** The smallest size a swatch may have across, so its colour is easy to see. */
        val SWATCH_MIN_SIZE = 24.dp

        /**
         * The custom colour the Custom swatch test picks: green #2E7D32, the DoD's custom colour,
         * far from Kanso blue's seed #1A73E8, so a swatch showing the seed cannot pass for it.
         */
        val CUSTOM_GREEN = Color(0xFF2E7D32)

        /** How long both clocks are moved on before a swatch is read, so every ripple has faded. */
        const val RIPPLE_SETTLE_MILLIS = 1_000L

        /**
         * How long, in real milliseconds counted from the tap, a tap on a colour row may take to
         * save the choice and recolour the app (DoD 2: "within 10 s of real time").
         */
        const val TAP_TO_SCHEME_MILLIS = 10_000L

        /** The tags of the three colour rows, in the order the screen lists them. */
        val COLOUR_ROW_TAGS = listOf(
            THEME_COLOUR_KANSO_BLUE_TAG,
            THEME_COLOUR_WALLPAPER_TAG,
            THEME_COLOUR_CUSTOM_TAG,
        )
    }
}
