package com.phongkaster.kanso.screenshots

import android.app.Application
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.myedits.MY_EDITS_EMPTY_TAG
import com.phongkaster.kanso.ui.myedits.MY_EDITS_GRID_TAG
import com.phongkaster.kanso.ui.myedits.MyEditsContent
import com.phongkaster.kanso.ui.myedits.MyEditsUiState
import com.phongkaster.kanso.ui.myedits.myEditsSkeletonTag
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Takes the reference pictures of the My edits tab (pictures 28 to 33): twelve saved photos, the
 * "no edits yet" message, and the skeleton tiles shown while the photos load, each in light and
 * in dark.
 *
 * The screen's stateless part is hosted directly in KansoTheme with the mode forced, on the same
 * phone screen as [ScreenshotTest]. The photos and the empty message are drawn on Android 11; the
 * loading state on Android 15, with the test clock paused before anything is drawn and moved on
 * by exactly [FROZEN_AT_MS], so every recording catches the same frame. The skeletons stand still
 * anyway (only the stateful screen makes them pulse), so the loading pictures show their still
 * frame.
 *
 * The photos' pictures are drawn by Coil, as in the app, through the gallery's thumbnail. Coil is
 * given an image loader that knows the test-only `fake` scheme ([SolidColourFetcherFactory]): for
 * such a Uri it makes a small bitmap of that colour, so no file is read. The loader runs every
 * step on the calling thread, so the pictures are ready as soon as the screen is idle.
 *
 * Each Android version's pictures are separate tests (DoD 35).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = MY_EDITS_PHONE)
class MyEditsScreenshotTest {

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

    /** Picture 28: twelve saved photos, in light mode. */
    @Test
    @Config(sdk = [30])
    fun my_edits_light() {
        showPhotos(ThemeMode.Light)
        capture("28_my_edits_light.png")
    }

    /** Picture 29: twelve saved photos, in dark mode. */
    @Test
    @Config(sdk = [30])
    fun my_edits_dark() {
        showPhotos(ThemeMode.Dark)
        capture("29_my_edits_dark.png")
    }

    /** Picture 30: no saved photo yet, in light mode. */
    @Test
    @Config(sdk = [30])
    fun my_edits_empty_light() {
        showEmpty(ThemeMode.Light)
        capture("30_my_edits_empty_light.png")
    }

    /** Picture 31: no saved photo yet, in dark mode. */
    @Test
    @Config(sdk = [30])
    fun my_edits_empty_dark() {
        showEmpty(ThemeMode.Dark)
        capture("31_my_edits_empty_dark.png")
    }

    /** Picture 32: the skeleton tiles while the photos load, in light mode. */
    @Test
    @Config(sdk = [35])
    fun my_edits_loading_light() {
        showLoading(ThemeMode.Light)
        capture("32_my_edits_loading_light.png")
    }

    /** Picture 33: the skeleton tiles while the photos load, in dark mode. */
    @Test
    @Config(sdk = [35])
    fun my_edits_loading_dark() {
        showLoading(ThemeMode.Dark)
        capture("33_my_edits_loading_dark.png")
    }

    /**
     * Installs the solid-colour image loader, shows twelve saved photos in [mode] and waits until
     * every picture is loaded.
     */
    private fun showPhotos(mode: ThemeMode) {
        val fetchers = SolidColourFetcherFactory()
        Coil.setImageLoader(
            ImageLoader.Builder(app)
                .components { add(fetchers) }
                .dispatcher(Dispatchers.Unconfined)
                .build(),
        )
        val state = MyEditsUiState(photos = twelvePhotos(), loading = false)
        compose.setContent {
            KansoTheme(themeMode = mode) {
                MyEditsContent(state = state, onPhotoClick = {})
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(MY_EDITS_GRID_TAG).assertIsDisplayed()
        compose.waitUntil(LOAD_TIMEOUT_MS) { fetchers.fetched.get() >= PHOTO_COUNT }
        compose.waitForIdle()
    }

    /** Shows the screen with no saved photo in [mode] and checks the message is on screen. */
    private fun showEmpty(mode: ThemeMode) {
        compose.setContent {
            KansoTheme(themeMode = mode) {
                MyEditsContent(state = MyEditsUiState(photos = emptyList(), loading = false), onPhotoClick = {})
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(MY_EDITS_EMPTY_TAG).assertIsDisplayed()
    }

    /**
     * Pauses the test clock, shows the screen still loading in [mode], moves the clock on to
     * [FROZEN_AT_MS] and checks the first skeleton tile is on screen.
     */
    private fun showLoading(mode: ThemeMode) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = mode) {
                MyEditsContent(state = MyEditsUiState(photos = emptyList(), loading = true), onPhotoClick = {})
            }
        }
        compose.mainClock.advanceTimeBy(FROZEN_AT_MS)
        compose.onNodeWithTag(myEditsSkeletonTag(0)).assertIsDisplayed()
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

        /** How many saved photos the photo pictures show. */
        const val PHOTO_COUNT = 12

        /**
         * Twelve saved photos, newest first, one hour apart, each with its own colour: the hues go
         * round the colour wheel so neighbouring tiles are easy to tell apart.
         */
        fun twelvePhotos(): List<Photo> = (0 until PHOTO_COUNT).map { i ->
            val colour = Color.HSVToColor(floatArrayOf(i * 30f, 0.45f, 0.85f))
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

/**
 * The Robolectric screen the pictures are drawn on, the same as [ScreenshotTest]'s: 411 dp wide,
 * 891 dp tall, at xxhdpi, the size of a typical modern phone.
 */
private const val MY_EDITS_PHONE = "w411dp-h891dp-xxhdpi"
