package com.phongkaster.kanso.screenshots

import android.app.Application
import android.graphics.Color
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
import coil.Coil
import coil.ImageLoader
import com.github.takahirom.roborazzi.captureRoboImage
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.RecordingRegistry
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.gallery.GALLERY_GRID_TAG
import com.phongkaster.kanso.ui.gallery.GalleryScreen
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.component.permission.model.photoPermissionsFor
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlinx.coroutines.Dispatchers
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
 * Takes the reference pictures of the gallery showing photos, in light and in dark (pictures 10
 * and 11).
 *
 * The gallery is hosted directly in KansoTheme with the mode forced, with the baseline colours,
 * on Android 11 and the same phone screen as [ScreenshotTest]. Photo access is granted with
 * Robolectric's grantPermissions, and the photos come from a [FakePhotoSource]: eighteen photos
 * in three albums whose URIs look like `fake://color/FF3366AA`.
 *
 * The thumbnails are drawn by Coil, as in the app. Coil is given an image loader that knows the
 * `fake` scheme ([SolidColourFetcherFactory]): for such a URI it makes a small bitmap filled with
 * that colour, so no file and no network is involved. The loader runs every step on the calling
 * thread (Dispatchers.Unconfined), so the thumbnails are ready as soon as the screen is idle.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = GALLERY_GRID_PHONE)
class GalleryGridScreenshotTest {

    /** Starts an empty activity to host the gallery. */
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

    /** Picture 10: the gallery with photos in light mode. */
    @Test
    fun gallery_light() {
        showGallery(ThemeMode.Light)
        capture("10_gallery_light.png")
    }

    /** Picture 11: the gallery with photos in dark mode. */
    @Test
    fun gallery_dark() {
        showGallery(ThemeMode.Dark)
        capture("11_gallery_dark.png")
    }

    /**
     * Grants photo access, installs the solid-colour image loader, shows the Edit tool's gallery
     * in [mode], and waits until the photos and their thumbnails are on screen.
     */
    private fun showGallery(mode: ThemeMode) {
        shadowOf(app).grantPermissions(*photoPermissionsFor(30).toTypedArray())

        val fetchers = SolidColourFetcherFactory()
        Coil.setImageLoader(
            ImageLoader.Builder(app)
                .components { add(fetchers) }
                .dispatcher(Dispatchers.Unconfined)
                .build(),
        )

        val registry = RecordingRegistry()
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = GalleryViewModel(Tool.EDIT, ViewModelClearedProbe(), fakeLibrary())
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
        // The library is read on a background thread, which the rule does not track by itself.
        compose.waitUntil(LOAD_TIMEOUT_MS) { !viewModel.state.value.loading }
        compose.waitForIdle()
        compose.onNodeWithTag(GALLERY_GRID_TAG).assertIsDisplayed()
        compose.waitUntil(LOAD_TIMEOUT_MS) { fetchers.fetched.get() >= MIN_THUMBNAILS }
        compose.waitForIdle()
        assertTrue("No permission request was expected", registry.requests.isEmpty())
    }

    /** Saves a picture of the whole window as `src/test/screenshots/[name]`. */
    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name")
    }

    /** Values the tests share. */
    private companion object {
        /** How long to wait for the fake library and the thumbnails, in milliseconds. */
        const val LOAD_TIMEOUT_MS = 5_000L

        /** The fewest thumbnails that must be loaded before the picture is taken. */
        const val MIN_THUMBNAILS = 9

        /**
         * Eighteen photos in three albums, newest first, each with its own colour: the hues go
         * round the colour wheel so neighbouring cells are easy to tell apart.
         */
        fun fakeLibrary(): FakePhotoSource {
            val albums = listOf(Album(1L, "Camera"), Album(2L, "Screenshots"), Album(3L, "Travel"))
            val photos = (0 until 18).map { i ->
                val colour = Color.HSVToColor(floatArrayOf(i * 20f, 0.45f, 0.85f))
                Photo(
                    id = (i + 1).toLong(),
                    uri = "$FAKE_SCHEME://color/${"%08X".format(colour)}",
                    name = "photo_${i + 1}.jpg",
                    albumId = albums[i % 3].id,
                    modifiedSeconds = 1_700_000_000L - i * 3_600L,
                )
            }
            return FakePhotoSource(albums = albums, allPhotos = photos)
        }
    }
}

/**
 * The URI scheme of the fake photos; only [SolidColourFetcherFactory] can open it. It is internal,
 * not private, because the factory lives in its own file and reads it from this package.
 */
internal const val FAKE_SCHEME = "fake"

/**
 * The Robolectric screen the pictures are drawn on, the same as [ScreenshotTest]'s: 411 dp wide,
 * 891 dp tall, at xxhdpi, the size of a typical modern phone.
 */
private const val GALLERY_GRID_PHONE = "w411dp-h891dp-xxhdpi"
