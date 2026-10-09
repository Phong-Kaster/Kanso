package com.phongkaster.kanso.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.data.settings.createSettingsDataStore
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.theme.seedColorScheme
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.robolectric.annotation.Config

/**
 * Checks that a fresh install of Kanso opens in Kanso blue on every Android version the app
 * supports, in light and in dark, through Material 3 Expressive (DoD 1 and the motion check of
 * DoD 10):
 *
 * - (a) an empty settings file reads as the Kanso blue choice at API 27, 30, 31 and 35;
 * - (b) launched on Android 8.1, 11, 12 and 15 (API 27, 30, 31, 35), each from an empty settings
 *   file with the theme mode left at System and the phone forced into day ("notnight") or night
 *   ("night"), the app hands every screen the scheme generated from KANSO_BLUE_SEED;
 * - (c) the seed is the agreed blue and the generated primary colours really are blue;
 * - (d) the window background shown before Compose draws matches Kanso blue's background;
 * - (e) KansoTheme hands out Material's expressive (springy) motion, not the standard one;
 * - (f) KansoTheme hands out emphasized text styles that really are bolder than the regular ones,
 *   while the regular ones stay Material's defaults.
 *
 * Robolectric gives every test a new, empty app storage, so each launch is a fresh install. The
 * Android version and the night setting come from each test's `@Config`, which replaces the
 * class's; each launch test first checks that it really runs with them.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "notnight")
class KansoBlueThemeTest {

    /** Finds Compose nodes in whatever activity a test launches. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** Gives each test its own empty folder for the settings files of part (a). */
    @get:Rule
    val folder = TemporaryFolder()

    /** The app context, used to read resources as the app does. */
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

    /** (a) An empty settings file reads as Kanso blue on Android 8.1, 11, 12 and 15. */
    @Test
    fun an_empty_settings_file_reads_kanso_blue_on_every_android_version() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.IO)
        try {
            SDK_LEVELS.forEach { sdkInt ->
                // A file of its own for each level, so every read starts from nothing saved.
                val file = File(folder.root, "settings-$sdkInt.preferences_pb")
                val repository = SettingsRepository(createSettingsDataStore(file, scope), sdkInt = sdkInt)
                assertEquals(
                    "Theme colour of an empty settings file at API $sdkInt",
                    ThemeColour.KansoBlue,
                    repository.themeColour.first(),
                )
            }
        } finally {
            // Shuts the DataStores down so their files are released before the folder is deleted.
            scope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    /** (b) A fresh install on Android 8.1 in day mode opens in Kanso blue's light scheme. */
    @Test
    @Config(sdk = [27], qualifiers = "notnight")
    fun fresh_install_opens_in_kanso_blue_light_on_android_8_1() {
        assertFreshInstallIsKansoBlue(sdk = 27, dark = false)
    }

    /** (b) A fresh install on Android 8.1 in night mode opens in Kanso blue's dark scheme. */
    @Test
    @Config(sdk = [27], qualifiers = "night")
    fun fresh_install_opens_in_kanso_blue_dark_on_android_8_1() {
        assertFreshInstallIsKansoBlue(sdk = 27, dark = true)
    }

    /** (b) A fresh install on Android 11 in day mode opens in Kanso blue's light scheme. */
    @Test
    @Config(sdk = [30], qualifiers = "notnight")
    fun fresh_install_opens_in_kanso_blue_light_on_android_11() {
        assertFreshInstallIsKansoBlue(sdk = 30, dark = false)
    }

    /** (b) A fresh install on Android 11 in night mode opens in Kanso blue's dark scheme. */
    @Test
    @Config(sdk = [30], qualifiers = "night")
    fun fresh_install_opens_in_kanso_blue_dark_on_android_11() {
        assertFreshInstallIsKansoBlue(sdk = 30, dark = true)
    }

    /**
     * (b) A fresh install on Android 12, the first version with wallpaper colours, opens in
     * Kanso blue's light scheme in day mode, not in the wallpaper's.
     */
    @Test
    @Config(sdk = [31], qualifiers = "notnight")
    fun fresh_install_opens_in_kanso_blue_light_on_android_12() {
        assertFreshInstallIsKansoBlue(sdk = 31, dark = false)
    }

    /** (b) A fresh install on Android 12 in night mode opens in Kanso blue's dark scheme. */
    @Test
    @Config(sdk = [31], qualifiers = "night")
    fun fresh_install_opens_in_kanso_blue_dark_on_android_12() {
        assertFreshInstallIsKansoBlue(sdk = 31, dark = true)
    }

    /** (b) A fresh install on Android 15 in day mode opens in Kanso blue's light scheme. */
    @Test
    @Config(sdk = [35], qualifiers = "notnight")
    fun fresh_install_opens_in_kanso_blue_light_on_android_15() {
        assertFreshInstallIsKansoBlue(sdk = 35, dark = false)
    }

    /** (b) A fresh install on Android 15 in night mode opens in Kanso blue's dark scheme. */
    @Test
    @Config(sdk = [35], qualifiers = "night")
    fun fresh_install_opens_in_kanso_blue_dark_on_android_15() {
        assertFreshInstallIsKansoBlue(sdk = 35, dark = true)
    }

    /**
     * (c) The seed is the agreed blue, and the light and dark `primary` and `primaryContainer`
     * generated from it are blue: a hue between 195 and 255 degrees, with blue the largest of
     * the three channels.
     */
    @Test
    fun the_seed_is_kanso_blue_and_its_primary_colours_are_blue() {
        assertEquals(Color(0xFF1A73E8), KANSO_BLUE_SEED)

        val problems = listOf(false, true).flatMap { dark ->
            val scheme = seedColorScheme(KANSO_BLUE_SEED, dark)
            listOf("primary" to scheme.primary, "primaryContainer" to scheme.primaryContainer)
                .mapNotNull { (role, colour) -> notBlueReason("$role (dark = $dark)", colour) }
        }
        assertTrue(
            "Kanso blue's primary colours must be blue:\n" + problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    /**
     * (d) The window background (`R.color.window_background`), which the window shows before
     * Compose draws its first frame, equals Kanso blue's light background in `res/values` and
     * its dark background in `res/values-night`, within 1 per channel, so there is no flash of
     * another colour at start-up. On failure the message names both values as hex.
     */
    @Test
    fun the_window_background_matches_kanso_blue_in_light_and_dark() {
        val looks = listOf(false, true).map { dark ->
            val resourceDir = if (dark) "res/values-night" else "res/values"
            val look = if (dark) "dark" else "light"
            Triple(
                "$resourceDir ($look)",
                windowBackground(night = dark),
                seedColorScheme(KANSO_BLUE_SEED, dark).background,
            )
        }
        val mismatches = looks.filterNot { (_, resource, expected) ->
            withinOnePerChannel(resource, expected)
        }
        assertTrue(
            "R.color.window_background must equal Kanso blue's background:\n" +
                looks.joinToString("\n") { (where, resource, expected) ->
                    "$where: window_background is ${hex(resource)}, Kanso blue's background is ${hex(expected)}"
                },
            mismatches.isEmpty(),
        )
    }

    /**
     * (e) Inside KansoTheme, `MaterialTheme.motionScheme.defaultSpatialSpec()` is the spring that
     * `MotionScheme.expressive()` makes, with the same stiffness and damping, and not the
     * standard scheme's spring.
     */
    @Test
    fun kanso_theme_hands_out_the_expressive_spring_motion() {
        var themed: FiniteAnimationSpec<Float>? = null
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    KansoTheme(themeMode = ThemeMode.System) {
                        themed = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
                    }
                }
            }
            compose.waitForIdle()
        }

        val inTheme = themed
        assertTrue("KansoTheme's content was never composed", inTheme != null)
        assertTrue("Expected a spring from the motion scheme, got $inTheme", inTheme is SpringSpec<*>)
        val expressive = MotionScheme.expressive().defaultSpatialSpec<Float>()
        val standard = MotionScheme.standard().defaultSpatialSpec<Float>()
        assertEquals("KansoTheme must hand out the expressive spatial spring", expressive, inTheme)
        assertNotEquals("KansoTheme must not hand out the standard spatial spring", standard, inTheme)

        // The same check on the spring's two settings, in case SpringSpec ever compares by identity.
        val themedSpring = inTheme as SpringSpec<*>
        val expressiveSpring = expressive as SpringSpec<*>
        val standardSpring = standard as SpringSpec<*>
        assertEquals("Spring stiffness", expressiveSpring.stiffness, themedSpring.stiffness, 0f)
        assertEquals("Spring damping ratio", expressiveSpring.dampingRatio, themedSpring.dampingRatio, 0f)
        assertTrue(
            "The expressive spring must differ from the standard one " +
                "(standard: stiffness ${standardSpring.stiffness}, damping ${standardSpring.dampingRatio})",
            standardSpring.stiffness != themedSpring.stiffness ||
                standardSpring.dampingRatio != themedSpring.dampingRatio,
        )
    }

    /**
     * (f) Inside KansoTheme, `MaterialTheme.typography` gives `titleLargeEmphasized` and
     * `headlineSmallEmphasized` a font weight different from `titleLarge` and `headlineSmall`
     * (the title styles the screens use), so an emphasized title really looks bolder. A type
     * scale built with `Typography()` and no arguments would fail this: it copies each regular
     * style into its emphasized twin. The regular styles must still equal Material's defaults,
     * so passing a type scale changes no regular text in the app.
     */
    @Test
    fun kanso_theme_hands_out_emphasized_styles_bolder_than_the_regular_ones() {
        var themed: Typography? = null
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    KansoTheme(themeMode = ThemeMode.System) {
                        themed = MaterialTheme.typography
                    }
                }
            }
            compose.waitForIdle()
        }

        val typography = checkNotNull(themed) { "KansoTheme's content was never composed" }
        assertNotEquals(
            "titleLargeEmphasized must not have titleLarge's font weight",
            typography.titleLarge.fontWeight,
            typography.titleLargeEmphasized.fontWeight,
        )
        assertNotEquals(
            "headlineSmallEmphasized must not have headlineSmall's font weight",
            typography.headlineSmall.fontWeight,
            typography.headlineSmallEmphasized.fontWeight,
        )

        // Material's default regular styles, for comparison.
        val defaults = Typography()
        assertEquals("titleLarge must stay Material's default", defaults.titleLarge, typography.titleLarge)
        assertEquals("headlineSmall must stay Material's default", defaults.headlineSmall, typography.headlineSmall)
    }

    /**
     * Launches the real app as a fresh install, with the phone at API level [sdk] in the [dark]
     * or day setting, and checks that it saved nothing yet (theme mode System, colour Kanso blue)
     * and that the scheme KansoTheme hands to every screen is Kanso blue in that look.
     */
    private fun assertFreshInstallIsKansoBlue(sdk: Int, dark: Boolean) {
        // Controls: the test really runs on the Android version and night setting it names.
        assertEquals("Android API level", sdk, Build.VERSION.SDK_INT)
        val nightBits = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val expectedNightBits = if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        assertEquals("Phone night setting", expectedNightBits, nightBits)

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag(KANSO_THEME_TAG).fetchSemanticsNodes().isNotEmpty()
            }

            // (a) on the app's own repository, which takes this phone's API level.
            val repository: SettingsRepository = KoinPlatform.getKoin().get()
            assertEquals("Theme mode of a fresh install", ThemeMode.System, runBlocking { repository.themeMode.first() })
            assertEquals(
                "Theme colour of a fresh install at API $sdk",
                ThemeColour.KansoBlue,
                runBlocking { repository.themeColour.first() },
            )

            // (b) the scheme every screen is drawn with.
            val applied = compose.onNodeWithTag(KANSO_THEME_TAG).fetchSemanticsNode()
                .config[KansoColorSchemeKey]
            val expected = seedColorScheme(KANSO_BLUE_SEED, dark)
            rolesOf(expected).zip(rolesOf(applied)).forEach { (want, got) ->
                assertEquals("${want.first} at API $sdk, dark = $dark", want.second, got.second)
            }
        }
    }

    /** Returns the colour roles DoD 1 compares, each with its name, in a fixed order. */
    private fun rolesOf(scheme: ColorScheme): List<Pair<String, Color>> = listOf(
        "primary" to scheme.primary,
        "onPrimary" to scheme.onPrimary,
        "primaryContainer" to scheme.primaryContainer,
        "secondary" to scheme.secondary,
        "tertiary" to scheme.tertiary,
        "background" to scheme.background,
        "surface" to scheme.surface,
    )

    /**
     * Reads `R.color.window_background` as the app does with the phone in the [night] or day
     * setting: a context whose configuration says night picks the value in `res/values-night`.
     */
    private fun windowBackground(night: Boolean): Color {
        val configuration = Configuration(context.resources.configuration)
        val nightBits = if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        configuration.uiMode = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightBits
        return Color(context.createConfigurationContext(configuration).getColor(R.color.window_background))
    }

    /**
     * Returns why [colour] (named [name] in the message) is not blue, or null when it is: its hue
     * must lie between [MIN_BLUE_HUE] and [MAX_BLUE_HUE] degrees and its blue channel must be
     * the largest of the three.
     */
    private fun notBlueReason(name: String, colour: Color): String? {
        val (red, green, blue) = channels(colour)
        val hue = hueDegrees(red, green, blue)
        return when {
            hue < MIN_BLUE_HUE || hue > MAX_BLUE_HUE ->
                "$name ${hex(colour)} has hue $hue, outside $MIN_BLUE_HUE..$MAX_BLUE_HUE"
            blue <= red || blue <= green ->
                "$name ${hex(colour)}: blue ($blue) is not the largest channel (red $red, green $green)"
            else -> null
        }
    }

    /**
     * Returns the HSV hue, in degrees from 0 to 360, of the 8-bit channels [red], [green] and
     * [blue]: where the colour sits on the colour wheel (red 0, green 120, blue 240).
     */
    private fun hueDegrees(red: Int, green: Int, blue: Int): Double {
        val max = maxOf(red, green, blue)
        val min = minOf(red, green, blue)
        val delta = (max - min).toDouble()
        // A grey has no hue; 0 keeps it outside the blue range.
        if (delta == 0.0) return 0.0
        val hue = when (max) {
            red -> 60.0 * (((green - blue) / delta) % 6.0)
            green -> 60.0 * ((blue - red) / delta + 2.0)
            else -> 60.0 * ((red - green) / delta + 4.0)
        }
        return if (hue < 0.0) hue + 360.0 else hue
    }

    /** Tells whether [a] and [b] differ by at most 1 in each of red, green and blue (0 to 255). */
    private fun withinOnePerChannel(a: Color, b: Color): Boolean =
        channels(a).zip(channels(b)).all { (x, y) -> abs(x - y) <= 1 }

    /** Returns the red, green and blue channels of [colour] as 8-bit numbers (0 to 255). */
    private fun channels(colour: Color): List<Int> {
        val argb = colour.toArgb()
        return listOf((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
    }

    /** Writes [colour] as `#AARRGGBB`, the way colours are written in `colors.xml`. */
    private fun hex(colour: Color): String = "#%08X".format(colour.toArgb())

    /** Values the tests use. */
    private companion object {
        /** The Android API levels a fresh install is checked on: 8.1, 11, 12 and 15. */
        val SDK_LEVELS = listOf(27, 30, 31, 35)

        /** The lowest hue, in degrees, that still counts as blue. */
        const val MIN_BLUE_HUE = 195.0

        /** The highest hue, in degrees, that still counts as blue. */
        const val MAX_BLUE_HUE = 255.0
    }
}
