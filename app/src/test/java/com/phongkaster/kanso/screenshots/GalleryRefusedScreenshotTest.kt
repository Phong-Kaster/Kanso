package com.phongkaster.kanso.screenshots

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.RecordingRegistry
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.gallery.GALLERY_REFUSED_TAG
import com.phongkaster.kanso.ui.gallery.GalleryScreen
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.theme.KansoTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Takes the reference pictures of the gallery's "photo access is off" state, in light and in
 * dark (pictures 12 and 13).
 *
 * The gallery is hosted directly in KansoTheme with the mode forced, with the baseline colours,
 * on Android 11 and the same phone screen as [ScreenshotTest]. Photo access is refused by
 * answering the gallery's one permission request "denied" through a [RecordingRegistry], so no
 * system dialog is involved.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = GALLERY_PHONE)
class GalleryRefusedScreenshotTest {

    /** Starts an empty activity to host the gallery. */
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

    /** Picture 12: the refused state in light mode. */
    @Test
    fun gallery_refused_light() {
        showRefusedGallery(ThemeMode.Light)
        capture("12_gallery_refused_light.png")
    }

    /** Picture 13: the refused state in dark mode. */
    @Test
    fun gallery_refused_dark() {
        showRefusedGallery(ThemeMode.Dark)
        capture("13_gallery_refused_dark.png")
    }

    /**
     * Shows the Edit tool's gallery in [mode], refuses the photo permission it asks for, and
     * waits until the refused panel is on screen.
     */
    private fun showRefusedGallery(mode: ThemeMode) {
        val registry = RecordingRegistry()
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = GalleryViewModel(Tool.EDIT, ViewModelClearedProbe(), FakePhotoSource(emptyList(), emptyList()))
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
        compose.waitForIdle()
        assertEquals(1, registry.requests.size)
        registry.answerLatest(registry.requests.single().associateWith { false })
        compose.waitForIdle()
        compose.onNodeWithTag(GALLERY_REFUSED_TAG).assertIsDisplayed()
    }

    /** Saves a picture of the whole window as `src/test/screenshots/[name]`. */
    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name")
    }
}

/**
 * The Robolectric screen the pictures are drawn on, the same as [ScreenshotTest]'s: 411 dp wide,
 * 891 dp tall, at xxhdpi, the size of a typical modern phone.
 */
private const val GALLERY_PHONE = "w411dp-h891dp-xxhdpi"
