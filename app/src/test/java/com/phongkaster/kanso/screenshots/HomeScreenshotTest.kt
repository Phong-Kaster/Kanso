package com.phongkaster.kanso.screenshots

import android.app.Application
import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.KANSO_TAB_BAR_TAG
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.KansoTabShell
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.navigation.tabBarClearance
import com.phongkaster.kanso.ui.home.HOME_RECENT_EMPTY_TAG
import com.phongkaster.kanso.ui.home.HomeContent
import com.phongkaster.kanso.ui.home.HomeUiState
import com.phongkaster.kanso.ui.home.homeRecentSkeletonTag
import com.phongkaster.kanso.ui.home.homeRecentTag
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Takes the reference pictures of Home's "Recent edits" section (pictures 34 to 39), each in light
 * and in dark:
 *
 * - 34 and 35: Home on a fresh install, the strip's short message instead of photos, on Android 11.
 * - 36 and 37: Home while the recent photos are read, the placeholder cards, on Android 15, with the
 *   test clock paused before anything is drawn and moved on by exactly [FROZEN_AT_MS], so every
 *   recording catches the same frame (the placeholders stand still anyway: only the stateful
 *   screen makes them pulse).
 * - 38 and 39: Home on the small phone (360 x 780 dp) as the app shows it, inside the tab shell
 *   with the floating bar, a 48 dp navigation bar sent as insets, and three recent photos: the
 *   picture of DoD 6, everything one tap away with nothing scrolled.
 *
 * Home's stateless part is hosted directly in KansoTheme with the mode forced. The recent photos'
 * pictures are drawn by Coil, as in the app, given an image loader that knows the test-only `fake`
 * scheme ([SolidColourFetcherFactory]), so no file is read. Each Android version's pictures are
 * separate tests (DoD 35).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HOME_PHONE)
class HomeScreenshotTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Gives Coil back a plain image loader and stops Koin after each test: both are kept in
     * JVM-wide singletons that outlive the test.
     */
    @After
    fun tearDown() {
        Coil.setImageLoader(ImageLoader.Builder(app).build())
        stopKoin()
    }

    /** Picture 34: Home on a fresh install, in light mode. */
    @Test
    @Config(sdk = [30])
    fun home_empty_light() {
        showEmpty(ThemeMode.Light)
        capture("34_home_empty_light.png")
    }

    /** Picture 35: Home on a fresh install, in dark mode. */
    @Test
    @Config(sdk = [30])
    fun home_empty_dark() {
        showEmpty(ThemeMode.Dark)
        capture("35_home_empty_dark.png")
    }

    /** Picture 36: Home while the recent photos load, in light mode. */
    @Test
    @Config(sdk = [35])
    fun home_loading_light() {
        showLoading(ThemeMode.Light)
        capture("36_home_loading_light.png")
    }

    /** Picture 37: Home while the recent photos load, in dark mode. */
    @Test
    @Config(sdk = [35])
    fun home_loading_dark() {
        showLoading(ThemeMode.Dark)
        capture("37_home_loading_dark.png")
    }

    /** Picture 38: Home on the small phone with the bar and three recent photos, in light mode. */
    @Test
    @Config(sdk = [35], qualifiers = SMALL_PHONE_SHOT)
    fun home_small_phone_light() {
        showSmallPhone(ThemeMode.Light)
        capture("38_home_small_phone_light.png")
    }

    /** Picture 39: Home on the small phone with the bar and three recent photos, in dark mode. */
    @Test
    @Config(sdk = [35], qualifiers = SMALL_PHONE_SHOT)
    fun home_small_phone_dark() {
        showSmallPhone(ThemeMode.Dark)
        capture("39_home_small_phone_dark.png")
    }

    /**
     * Shows Home with no saved photo in [mode] inside the tab shell with the floating bar (Home
     * selected, the bar's clearance at the end of the column) and checks the bar and the message
     * are on screen.
     */
    private fun showEmpty(mode: ThemeMode) {
        compose.setContent {
            KansoTheme(themeMode = mode) {
                KansoTabShell(showBar = true, selected = KansoTab.HOME, onTabSelected = {}, onCamera = {}) {
                    HomeContent(
                        state = homeState(photos = emptyList(), loading = false),
                        onToolClick = {},
                        contentPadding = PaddingValues(bottom = tabBarClearance()),
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(KANSO_TAB_BAR_TAG).assertIsDisplayed()
        compose.onNodeWithTag(HOME_RECENT_EMPTY_TAG).assertExists()
    }

    /**
     * Pauses the test clock, shows Home still reading its recent photos in [mode], moves the clock
     * on to [FROZEN_AT_MS] and checks the first placeholder card is there.
     */
    private fun showLoading(mode: ThemeMode) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = mode) {
                HomeContent(state = homeState(photos = emptyList(), loading = true), onToolClick = {})
            }
        }
        compose.mainClock.advanceTimeBy(FROZEN_AT_MS)
        compose.onNodeWithTag(homeRecentSkeletonTag(0)).assertExists()
    }

    /**
     * Shows Home in [mode] as the app draws it on the small phone: inside the tab shell with the
     * floating bar (Home selected), leaving the bar's clearance at the end of the column, with
     * three recent photos; sends a 48 dp navigation bar as insets and waits until the pictures are
     * loaded.
     */
    private fun showSmallPhone(mode: ThemeMode) {
        val fetchers = SolidColourFetcherFactory()
        Coil.setImageLoader(
            ImageLoader.Builder(app)
                .components { add(fetchers) }
                .dispatcher(Dispatchers.Unconfined)
                .build(),
        )
        var composeView: View? = null
        var seenNavigationBarPx = -1
        compose.setContent {
            // LocalView is the Android view the screen is drawn in, the one that receives the
            // window insets; the navigation bar is read here just as the screens read it.
            val view = LocalView.current
            val navigationBar = WindowInsets.navigationBars.getBottom(LocalDensity.current)
            // SideEffect hands the values out after each composition that succeeds.
            SideEffect {
                composeView = view
                seenNavigationBarPx = navigationBar
            }
            KansoTheme(themeMode = mode) {
                KansoTabShell(showBar = true, selected = KansoTab.HOME, onTabSelected = {}, onCamera = {}) {
                    HomeContent(
                        state = homeState(photos = threePhotos(), loading = false),
                        onToolClick = {},
                        contentPadding = PaddingValues(bottom = tabBarClearance()),
                    )
                }
            }
        }
        compose.waitForIdle()

        val navigationPx = with(compose.density) { NAVIGATION_BAR.roundToPx() }
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navigationPx))
            .setInsets(WindowInsetsCompat.Type.systemGestures(), Insets.of(0, 0, 0, navigationPx))
            .build()
        compose.runOnIdle { ViewCompat.dispatchApplyWindowInsets(checkNotNull(composeView), insets) }
        compose.waitForIdle()
        assertEquals("The screen did not see the navigation bar the test sent", navigationPx, seenNavigationBarPx)

        compose.onNodeWithTag(KANSO_TAB_BAR_TAG).assertIsDisplayed()
        compose.onNodeWithTag(homeRecentTag(0)).assertIsDisplayed()
        compose.waitUntil(LOAD_TIMEOUT_MS) { fetchers.fetched.get() >= PHOTO_COUNT }
        compose.waitForIdle()
    }

    /** Saves a picture of the whole window as `src/test/screenshots/[name]`. */
    private fun capture(name: String) {
        compose.onRoot().captureRoboImage("src/test/screenshots/$name")
    }

    /** Values and photos the tests share. */
    private companion object {
        /** How long to wait for the pictures, in milliseconds. */
        const val LOAD_TIMEOUT_MS = 5_000L

        /** How far the paused clock is moved on before a loading picture is taken, in milliseconds. */
        const val FROZEN_AT_MS = 500L

        /** How many recent photos the small-phone pictures show. */
        const val PHOTO_COUNT = 3

        /** The navigation bar the small-phone pictures are drawn with. */
        val NAVIGATION_BAR = 48.dp

        /** Home's state as its ViewModel builds it, with [photos] in the strip, [loading] or not. */
        fun homeState(photos: List<Photo>, loading: Boolean) = HomeUiState(
            carouselTools = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.BEAUTIFY, Tool.ARTISTIC),
            shortcutTools = Tool.entries.toList(),
            recentPhotos = photos,
            recentLoading = loading,
        )

        /**
         * Three saved photos, newest first, one hour apart, each with its own soft colour through
         * the `fake` scheme.
         */
        fun threePhotos(): List<Photo> = (0 until PHOTO_COUNT).map { i ->
            val colour = Color.HSVToColor(floatArrayOf(200f + i * 50f, 0.45f, 0.85f))
            Photo(
                id = (i + 1).toLong(),
                uri = "$FAKE_SCHEME://color/${"%08X".format(colour)}",
                name = "Kanso_${i + 1}.jpg",
                albumId = 1L,
                modifiedSeconds = 1_791_800_000L - i * 3_600L,
            )
        }
    }
}

/** The Robolectric screen of pictures 34 to 37, the same phone as [ScreenshotTest]'s. */
private const val HOME_PHONE = "w411dp-h891dp-xxhdpi"

/** The small phone of pictures 38 and 39 (DoD 6): 360 x 780 dp at xxhdpi. */
private const val SMALL_PHONE_SHOT = "w360dp-h780dp-xxhdpi"
