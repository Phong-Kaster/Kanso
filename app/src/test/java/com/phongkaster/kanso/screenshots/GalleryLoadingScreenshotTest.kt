package com.phongkaster.kanso.screenshots

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.RecordingRegistry
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.gallery.GALLERY_LOADING_TAG
import com.phongkaster.kanso.ui.gallery.GalleryScreen
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.component.permission.model.photoPermissionsFor
import com.phongkaster.kanso.ui.theme.KansoTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Takes the reference pictures of the gallery while its photos are still loading, in light and in
 * dark (pictures 26 and 27): the top bar, and below it the Expressive loading indicator.
 *
 * The gallery is hosted directly in KansoTheme with the mode forced, on Android 15 and the same
 * phone screen as [ScreenshotTest]. Photo access is granted with Robolectric's grantPermissions,
 * and the photos come from a [FakePhotoSource] whose reads are held back
 * ([FakePhotoSource.holdReads]) and never let go, so the gallery stays in its loading state.
 *
 * The loading indicator animates for as long as it is shown. Two things follow. The test clock is
 * paused before anything is drawn (`mainClock.autoAdvance = false`), because with the clock
 * running by itself every wait for the screen to settle would wait for an animation that never
 * ends. And the clock is then moved on by exactly [FROZEN_AT_MS], so the indicator is drawn at
 * the same moment of its animation on every run; otherwise each recording would catch a different
 * shape and the screenshot check would fail at random.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = GALLERY_LOADING_PHONE)
class GalleryLoadingScreenshotTest {

    /** Starts an empty activity to host the gallery. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** Picture 26: the gallery loading its photos, in light mode. */
    @Test
    fun gallery_loading_light() {
        showLoadingGallery(ThemeMode.Light)
        capture("26_gallery_loading_light.png")
    }

    /** Picture 27: the gallery loading its photos, in dark mode. */
    @Test
    fun gallery_loading_dark() {
        showLoadingGallery(ThemeMode.Dark)
        capture("27_gallery_loading_dark.png")
    }

    /**
     * Grants photo access, pauses the test clock, shows the Edit tool's gallery in [mode] with
     * its reads held back, moves the clock on to [FROZEN_AT_MS] and checks the loading indicator
     * is on screen.
     */
    private fun showLoadingGallery(mode: ThemeMode) {
        shadowOf(app).grantPermissions(*photoPermissionsFor(35).toTypedArray())

        val source = FakePhotoSource(albums = emptyList(), allPhotos = emptyList()).apply { holdReads() }
        val registry = RecordingRegistry()
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = GalleryViewModel(Tool.EDIT, ViewModelClearedProbe(), source)

        // Paused before setContent, so the indicator's first frame is already under the test's
        // control (see the class comment).
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                KansoTheme(themeMode = mode) {
                    GalleryScreen(
                        viewModel = viewModel,
                        onBack = {},
                        onPhotoChosen = { _, _ -> },
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(FROZEN_AT_MS)

        compose.onNodeWithTag(GALLERY_LOADING_TAG).assertIsDisplayed()
        assertTrue("The read should still be held", viewModel.state.value.loading)
        assertTrue("No permission request was expected", registry.requests.isEmpty())
    }

    /**
     * Saves a picture of the whole window as `src/test/screenshots/[name]`. The clock stays
     * paused, so the picture shows the indicator exactly as it was at [FROZEN_AT_MS].
     */
    private fun capture(name: String) {
        compose.onRoot().captureRoboImage("src/test/screenshots/$name")
    }

    /** Values the tests share. */
    private companion object {
        /**
         * How far the paused clock is moved on before the picture is taken, in milliseconds: long
         * enough for the indicator to be drawn mid-animation, and the same on every run.
         */
        const val FROZEN_AT_MS = 500L
    }
}

/**
 * The Robolectric screen the pictures are drawn on, the same as [ScreenshotTest]'s: 411 dp wide,
 * 891 dp tall, at xxhdpi, the size of a typical modern phone.
 */
private const val GALLERY_LOADING_PHONE = "w411dp-h891dp-xxhdpi"
