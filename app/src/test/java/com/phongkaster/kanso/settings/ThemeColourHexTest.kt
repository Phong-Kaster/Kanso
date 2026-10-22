package com.phongkaster.kanso.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.data.settings.SETTINGS_SCOPE
import com.phongkaster.kanso.data.settings.SettingsRepository
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.createSettingsDataStore
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.openThemeColourScreen
import com.phongkaster.kanso.navigation.openTab
import com.phongkaster.kanso.navigation.scrollAboveBar
import com.phongkaster.kanso.navigation.tapAboveBar
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.CUSTOM_COLOUR_PICKER_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_SWATCH_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_TAG
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.KANSO_THEME_TAG
import com.phongkaster.kanso.ui.theme.KansoColorSchemeKey
import com.phongkaster.kanso.ui.theme.seedColorScheme
import java.io.File
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
 * Drives the real app to Settings on a fresh install and types a colour code into the custom
 * colour picker's hex field (DoD 18):
 *
 * - `2E7D32` saves `Custom(0xFF2E7D32)`, recolours the whole app with the scheme grown from that
 *   green, and the Custom row's swatch shows it, all within ten seconds of real time from the
 *   keystroke;
 * - an unfinished code, `2E7`, then changes nothing, even after more than six times the
 *   ViewModel's 300 ms settle time;
 * - after the activity is recreated, a fresh DataStore opened over the same settings file still
 *   reads the green, so the colour survives a restart.
 *
 * On Android 11 (SDK 30) the first of these steps is checked too.
 *
 * Settings is opened with the floating tab bar's Settings tab (`TabTaps.openTab`) and the Custom
 * row is tapped with `TabTaps.tapAboveBar`, because on this small test screen a row scrolled
 * into view can rest under the bar. The field is the editable node under the picker's tag, in
 * the unmerged tree. Pixels are read by drawing the activity's content view into a bitmap with
 * Robolectric's real drawing code (`GraphicsMode.Mode.NATIVE`), as `ThemeSwatchTest` does.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "notnight")
class ThemeColourHexTest {

    /** Launches MainActivity before each test and lets the test find and type into its nodes. */
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** The app context, whose private files hold the settings file. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** Scopes of the DataStores this test opens itself, shut down after the test. */
    private val scopes = mutableListOf<CoroutineScope>()

    /**
     * Shuts down the DataStores the test opened, then stops Koin: Koin's state outlives the
     * test's Application inside the same JVM, and starting it twice fails.
     */
    @After
    fun stopKoinAfterTest() {
        runBlocking { scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() } }
        stopKoin()
    }

    /**
     * On Android 15: `2E7D32` turns the app green within ten seconds; `2E7` then changes
     * nothing; and the green is still in the settings file after a restart.
     */
    @Test
    fun android_15_a_typed_code_turns_the_app_that_colour_and_a_partial_one_changes_nothing() {
        val green = rolesOf(seedColorScheme(Color(GREEN_ARGB), dark = false, exactPrimary = true))
        openCustomPicker()

        typeGreenAndCheckEverythingTurnsGreen()

        // An unfinished code: nothing may be saved and the scheme may not move.
        field().performTextReplacement("2E7")
        val until = System.currentTimeMillis() + PARTIAL_WAIT_MILLIS
        while (System.currentTimeMillis() < until) {
            Thread.sleep(50)
            compose.waitForIdle()
        }
        // Also move both test clocks on past the settle time, in case the wait above ran on a
        // clock that real time does not move.
        advanceBothClocks(PARTIAL_WAIT_MILLIS)
        compose.waitForIdle()
        assertEquals(
            "After typing 2E7 the stored colour must still be the green",
            ThemeColour.Custom(GREEN_ARGB),
            savedThemeColour(),
        )
        green.zip(rolesOf(appliedScheme())).forEach { (want, got) ->
            assertEquals("${want.first} of the scheme after typing 2E7", want.second, got.second)
        }

        // A restart: the activity is recreated, then the app's DataStore is released and a fresh
        // one is opened over the same file, as a new process would.
        compose.activityRule.scenario.recreate()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(KANSO_THEME_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        val reopened = runBlocking { writeThenReopen { } }
        assertEquals(
            "A fresh store over the same file should read the green",
            ThemeColour.Custom(GREEN_ARGB),
            runBlocking { reopened.themeColour.first() },
        )
    }

    /** On Android 11 (no wallpaper colours), `2E7D32` turns the app green within ten seconds too. */
    @Test
    @Config(sdk = [30])
    fun android_11_a_typed_code_turns_the_app_that_colour() {
        openCustomPicker()

        typeGreenAndCheckEverythingTurnsGreen()
    }

    /**
     * From a fresh install (the stored colour is still the default), opens Settings, taps the
     * Custom colour row and waits for the picker to show, scrolled into view.
     */
    private fun openCustomPicker() {
        waitForHome()
        assertEquals(
            "The test needs a fresh install: the stored colour should be the default",
            repository().defaultThemeColour,
            savedThemeColour(),
        )
        openSettings()
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).tapAboveBar()
        // Choosing Custom on a fresh install starts from Kanso blue's seed, not the green, so
        // the green below can only come from the typed code.
        awaitSavedThemeColour(ThemeColour.Custom(KANSO_BLUE_SEED.toArgb()))
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(CUSTOM_COLOUR_PICKER_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag(CUSTOM_COLOUR_PICKER_TAG).performScrollTo()
        compose.waitForIdle()
    }

    /**
     * Replaces the field's text with `2E7D32`, taking one ten-second deadline at that keystroke,
     * then fails unless by the deadline the stored colour is `Custom(0xFF2E7D32)`, the app's
     * scheme is the one grown from that green in all seven roles, and the Custom row's swatch
     * shows the green in its middle pixel.
     */
    private fun typeGreenAndCheckEverythingTurnsGreen() {
        val expected = rolesOf(seedColorScheme(Color(GREEN_ARGB), dark = false, exactPrimary = true))
        // One deadline on the wall clock for everything that follows the keystroke.
        val deadline = System.currentTimeMillis() + TYPE_TO_SCHEME_MILLIS
        field().performTextReplacement("2E7D32")

        awaitSavedThemeColour(ThemeColour.Custom(GREEN_ARGB), deadline)
        // runCatching keeps a failed wait so the checks below can name the role that is wrong;
        // getOrThrow() still fails the test if the scheme was late.
        val waited = runCatching {
            // What is left of the ten seconds; at least 1 ms, so the time is never negative.
            val remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(1L)
            compose.waitUntil(timeoutMillis = remaining) { rolesOf(appliedScheme()) == expected }
            assertTrue(
                "Typing 2E7D32 must recolour the app within ten seconds",
                System.currentTimeMillis() <= deadline,
            )
        }
        expected.zip(rolesOf(appliedScheme())).forEach { (want, got) ->
            assertEquals("${want.first} of the scheme after typing 2E7D32", want.second, got.second)
        }
        waited.getOrThrow()

        assertCustomSwatchShows(Color(GREEN_ARGB))
        assertTrue(
            "The swatch must show the green within ten seconds of the keystroke",
            System.currentTimeMillis() <= deadline,
        )
    }

    /**
     * Fails unless the middle pixel of the Custom row's swatch equals [expected] within
     * [TOLERANCE] per channel. The middle is the swatch's `positionInRoot` plus half its size
     * (never fixed coordinates, and never `boundsInRoot`, which is clipped to what is visible).
     * The press ripple of the earlier tap is let fade first, on both clocks.
     */
    private fun assertCustomSwatchShows(expected: Color) {
        advanceBothClocks(RIPPLE_SETTLE_MILLIS)
        // Above the floating bar, so the swatch is not read through the bar's pixels.
        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).scrollAboveBar(compose)
        compose.waitForIdle()
        val swatch = compose.onNode(hasTestTag(THEME_COLOUR_CUSTOM_SWATCH_TAG), useUnmergedTree = true)
            .fetchSemanticsNode()
        val centreX = (swatch.positionInRoot.x + swatch.size.width / 2f).roundToInt()
        val centreY = (swatch.positionInRoot.y + swatch.size.height / 2f).roundToInt()
        val drawn = drawnPixelAt(centreX, centreY).toArgb()
        val wanted = expected.toArgb()
        val close = listOf(24, 16, 8, 0).all { shift ->
            abs(((wanted shr shift) and 0xFF) - ((drawn shr shift) and 0xFF)) <= TOLERANCE
        }
        assertTrue("The Custom swatch's middle: expected ${hex(wanted)}, drawn ${hex(drawn)}", close)
    }

    /** Finds the picker's hex field: the node with editable text under the picker's tag. */
    private fun field(): SemanticsNodeInteraction = compose.onNode(
        hasAnyAncestor(hasTestTag(CUSTOM_COLOUR_PICKER_TAG)) and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText),
        useUnmergedTree = true,
    )

    /**
     * Moves Compose's test clock and Robolectric's main-thread clock on by [millis] together:
     * Compose's animations run on the first, Android's own drawables (such as a ripple) on the
     * second.
     */
    private fun advanceBothClocks(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))
    }

    /**
     * Draws the activity's content view into a fresh bitmap and returns the colour of the pixel
     * at ([x], [y]). The Compose root fills the content view from its top-left corner, so a
     * node's position in the root is its position in the bitmap.
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

    /** Writes an ARGB colour number as `#AARRGGBB`, for failure messages. */
    private fun hex(argb: Int): String = "#%08X".format(argb)

    /** Returns the colour scheme KansoTheme currently hands to every screen. */
    private fun appliedScheme(): ColorScheme =
        compose.onNodeWithTag(KANSO_THEME_TAG).fetchSemanticsNode().config[KansoColorSchemeKey]

    /**
     * Returns the seven colour roles DoD 1 compares, each with its name, in a fixed order, as
     * `ThemeSwatchTest` does: two schemes that agree in all seven are taken to be the same.
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

    /** Reads the theme colour saved in the settings file right now, through the app's repository. */
    private fun savedThemeColour(): ThemeColour = runBlocking { repository().themeColour.first() }

    /**
     * Waits, in real time, until the saved theme colour is [expected], and fails unless it is by
     * then. The write runs on a background thread, which Compose's test clock does not wait for.
     * [deadline] is the wall-clock moment the wait gives up (by default ten seconds from the call).
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

    /**
     * Runs [write] against the app's own repository, then shuts the app's DataStore down as an
     * app exit would (cancelling its scope and waiting for it, which releases the file: only one
     * DataStore may have a file open), and returns a repository over a fresh DataStore on the same
     * settings file, which reads it from disk.
     */
    private suspend fun writeThenReopen(write: suspend (SettingsRepository) -> Unit): SettingsRepository {
        write(repository())
        KoinPlatform.getKoin().get<CoroutineScope>(SETTINGS_SCOPE).coroutineContext[Job]!!.cancelAndJoin()
        val scope = CoroutineScope(Job() + Dispatchers.IO).also { scopes += it }
        return SettingsRepository(createSettingsDataStore(settingsFile(), scope), sdkInt = 35)
    }

    /** The app's settings file, where the app's own DataStore keeps it (never renamed). */
    private fun settingsFile(): File = context.filesDir.resolve("datastore/settings.preferences_pb")

    /** Returns the app's one settings repository from Koin. */
    private fun repository(): SettingsRepository = KoinPlatform.getKoin().get()

    /** Opens the Settings tab, then the Theme colour screen from its menu, and waits until it is shown. */
    private fun openSettings() {
        openThemeColourScreen(compose)
    }

    /** Waits until Home is on screen, found by its own tag. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(HOME_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Values the tests share. */
    private companion object {
        /** The DoD's green, #2E7D32, fully opaque, as an ARGB number. */
        val GREEN_ARGB = 0xFF2E7D32.toInt()

        /** How far, per colour channel (0 to 255), the drawn swatch may be from the green. */
        const val TOLERANCE = 2

        /** Real milliseconds from the keystroke within which the app must be green (DoD 18). */
        const val TYPE_TO_SCHEME_MILLIS = 10_000L

        /** How long an unfinished code is watched: more than six times the 300 ms settle time. */
        const val PARTIAL_WAIT_MILLIS = 2_000L

        /** How long both clocks are moved on before the swatch is read, so every ripple has faded. */
        const val RIPPLE_SETTLE_MILLIS = 1_000L
    }
}
