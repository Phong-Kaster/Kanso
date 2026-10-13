package com.phongkaster.kanso.navigation

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.GalleryGridTest
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.galleryPhotoTag
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_REPLACE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * DoD 11, through the app's real navigation: "Replace" on a tool screen goes back to the photo
 * gallery of the same tool, and the photo picked there opens in the same tool.
 *
 * Case (a): from Home, Retouch's gallery, photo 1, then Replace. The gallery titled "Retouch:
 * choose a photo" is on top again, with no tool screen left; photo 2 there opens the Retouch
 * screen on photo 2; Replace once more, and one Back from the gallery reaches Home, so Replace
 * never piled a screen up. A test cannot read the app's back stack, so its shape is read from what
 * is on screen and from where Back leads.
 *
 * Photo and camera access are granted, and the photo library and the photo decoder are swapped
 * for fakes before the app starts (Home's ViewModel keeps the sources it was built with).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ReplaceNavigationTest {

    /** Finds Compose nodes in the activity the test starts. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Stops Koin: its global state outlives the test inside the same JVM. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * Case (a): Retouch gallery, photo 1, Replace: the Retouch gallery is on top; photo 2 gives
     * the Retouch screen on photo 2; Replace again, and one Back reaches Home.
     */
    @Test
    fun replace_on_the_retouch_screen_returns_to_the_retouch_gallery() {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA, *photoPermissionsFor(35).toTypedArray())
        val photos = GalleryGridTest.manyPhotos(3)
        // Photo 1 is the newest, so it is the first cell of the gallery; photo 2 the second.
        val (photo1, photo2) = photos
        loadKoinModules(
            module {
                single<PhotoSource> { FakePhotoSource(albums = listOf(Album(1L, "A")), allPhotos = photos) }
                single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
            },
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openToolGallery(compose, Tool.RETOUCH)
            waitForRetouchGallery(photo1.id)
            compose.onNodeWithTag(galleryPhotoTag(photo1.id)).performClick()
            waitForRetouchScreenOn(photo1.uri)

            // Replace: back to the Retouch gallery, the tool screen gone.
            compose.onNodeWithTag(TOOL_REPLACE_TAG).performClick()
            waitForRetouchGallery(photo2.id)
            assertEquals(0, count(TOOL_SCREEN_TAG))
            assertEquals(1, count(GALLERY_SCREEN_TAG))

            // The photo picked there opens in the same tool.
            compose.onNodeWithTag(galleryPhotoTag(photo2.id)).performClick()
            waitForRetouchScreenOn(photo2.uri)

            // Replace again, then one Back from the gallery reaches Home.
            compose.onNodeWithTag(TOOL_REPLACE_TAG).performClick()
            waitForRetouchGallery(photo1.id)
            assertEquals(0, count(TOOL_SCREEN_TAG))
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(GALLERY_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    /**
     * Waits until the gallery titled "Retouch: choose a photo" is the only screen of its kind,
     * no tool screen is left, and the photo [photoId] is among its cells.
     */
    private fun waitForRetouchGallery(photoId: Long) {
        val title = app.getString(R.string.gallery_title, app.getString(R.string.tool_retouch))
        val titled = hasText(title) and hasAnyAncestor(hasTestTag(GALLERY_SCREEN_TAG))
        waitFor {
            count(TOOL_SCREEN_TAG) == 0 &&
                compose.onAllNodes(titled).fetchSemanticsNodes().isNotEmpty() &&
                count(galleryPhotoTag(photoId)) > 0
        }
    }

    /**
     * Waits until the Retouch screen shows the photo [photoUri] with no gallery left, and checks
     * that its title names Retouch.
     */
    private fun waitForRetouchScreenOn(photoUri: String) {
        val shownPhoto = hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, photoUri)
        waitFor { count(GALLERY_SCREEN_TAG) == 0 && compose.onAllNodes(shownPhoto).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, count(TOOL_SCREEN_TAG))
        val title = hasText(app.getString(R.string.tool_retouch)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG))
        assertEquals(1, compose.onAllNodes(title).fetchSemanticsNodes().size)
    }

    /**
     * Waits up to ten seconds of real time for [condition], letting the main thread run in
     * between: the gallery's read and the tool screen's decode post their results there, and
     * under Robolectric the test runs on that thread itself.
     */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size
}
