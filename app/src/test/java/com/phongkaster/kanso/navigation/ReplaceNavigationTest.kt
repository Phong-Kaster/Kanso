package com.phongkaster.kanso.navigation

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.camera.FakeCameraController
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.media.SavedPhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.GalleryGridTest
import com.phongkaster.kanso.myedits.FakeSavedPhotoSource
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CAMERA_KEEP_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.galleryPhotoTag
import com.phongkaster.kanso.ui.home.HOME_CAROUSEL_TAG
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.home.homeRecentTag
import com.phongkaster.kanso.ui.home.homeTryTag
import com.phongkaster.kanso.ui.myedits.MY_EDITS_SCREEN_TAG
import com.phongkaster.kanso.ui.myedits.myEditsTileTag
import com.phongkaster.kanso.ui.component.permission.model.photoPermissionsFor
import com.phongkaster.kanso.ui.result.RESULT_EDIT_TAG
import com.phongkaster.kanso.ui.result.RESULT_PHOTO_TAG
import com.phongkaster.kanso.ui.result.RESULT_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.result.RESULT_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_REPLACE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
 * gallery of the same tool, whichever way the tool screen was opened, and the photo picked there
 * opens in the same tool.
 *
 * - (a) Home's carousel swiped to Beautify's banner, "Try now", photo 1, Replace: the gallery titled
 *   "Beautify: choose a photo" is on top, with no tool screen left; photo 2 there opens the Beautify
 *   screen on photo 2; Replace once more, and one Back from the gallery reaches Home.
 * - (b) Home's newest recent photo (`home-recent-0`) opens the Edit screen, which has no Replace;
 *   one Back reaches Home.
 * - (c) The Camera button, shutter, Keep: the Edit screen, with no Replace; one Back reaches Home.
 * - (d) My edits, a tile, the viewer's "Edit": the Edit screen, with no Replace; Back shows the
 *   photo screen (the viewer) again, and another Back shows My edits.
 * - Two taps on Replace (on the Beautify screen) before the screen can change open one gallery,
 *   not two.
 *
 * Run 9 (A-007): Replace was removed from the Edit screen only; (b), (c) and (d) reach nothing
 * but Edit, so they now check that Replace is absent there and where Back leads instead, and the
 * double tap moved to Beautify.
 *
 * A test cannot read the app's back stack, so its shape is read from what is on screen and from
 * where Back leads. Photo and camera access are granted, and the photo library, the saved photos,
 * the photo decoder and the camera are swapped for fakes before the app starts
 * (`createEmptyComposeRule`, `loadKoinModules`, then `ActivityScenario.launch`): Home's and My
 * edits' ViewModels keep the sources they were built with, so a later swap would never reach them.
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
     * Case (a): Beautify's "Try now" in the carousel, photo 1, Replace: the Beautify gallery is on
     * top; photo 2 gives the Beautify screen on photo 2; Replace again, and one Back reaches Home.
     */
    @Test
    fun replace_on_the_beautify_screen_returns_to_the_beautify_gallery() {
        // Photo 1 is the newest, so it is the first cell of the gallery; photo 2 the second.
        val (photo1, photo2) = galleryPhotos
        launchWithFakes { scenario ->
            bringBeautifyBannerToFront()
            // The banner's own click handler, as a screen reader's double tap would call it: the
            // carousel sits in Home's scrolling column, where a touch could land on the tab bar.
            compose.onNodeWithTag(homeTryTag(Tool.BEAUTIFY)).performSemanticsAction(SemanticsActions.OnClick)
            waitForGallery(Tool.BEAUTIFY, photo1.id)
            compose.onNodeWithTag(galleryPhotoTag(photo1.id)).performClick()
            waitForToolScreenOn(Tool.BEAUTIFY, photo1.uri)

            // Replace: back to the Beautify gallery, the tool screen gone.
            compose.onNodeWithTag(TOOL_REPLACE_TAG).performClick()
            waitForGallery(Tool.BEAUTIFY, photo2.id)
            assertEquals(0, count(TOOL_SCREEN_TAG))
            assertEquals(1, count(GALLERY_SCREEN_TAG))

            // The photo picked there opens in the same tool.
            compose.onNodeWithTag(galleryPhotoTag(photo2.id)).performClick()
            waitForToolScreenOn(Tool.BEAUTIFY, photo2.uri)

            // Replace again, then one Back from the gallery reaches Home.
            compose.onNodeWithTag(TOOL_REPLACE_TAG).performClick()
            waitForGallery(Tool.BEAUTIFY, photo1.id)
            assertOneBackReachesHome(scenario)
        }
    }

    /**
     * Case (b): Home's newest recent photo opens the Edit screen on it; the Edit screen has no
     * Replace, and one Back reaches Home.
     *
     * Run 9 (A-007): the Edit screen no longer has a Replace chip, and a recent photo only ever
     * opens Edit, so this case can no longer reach Replace. It now proves the route still opens
     * the Edit screen on the right photo, that Replace is gone from it, and that Back from it
     * reaches Home; Replace itself stays proven on Beautify by case (a) and the double tap.
     */
    @Test
    fun a_recent_photo_opens_the_edit_screen_without_replace_and_back_reaches_home() {
        val newest = editedPhotos.first()
        launchWithFakes { scenario ->
            waitForHome(compose)
            waitFor { count(homeRecentTag(0)) > 0 }
            compose.onNodeWithTag(homeRecentTag(0)).tapAboveBar()
            waitForToolScreenOn(Tool.EDIT, newest.uri)

            assertEquals("The Edit screen must have no Replace", 0, count(TOOL_REPLACE_TAG))
            assertBackFromToolReachesHome(scenario)
        }
    }

    /**
     * Case (c): the Camera button beside the tab bar, the shutter, Keep: the Edit screen, with no
     * Replace; one Back reaches Home (the camera is not left below it).
     *
     * Run 9 (A-007): the Edit screen no longer has a Replace chip and a kept shot only ever opens
     * Edit, so this case now proves the route, that Replace is gone, and that Back skips the
     * camera; Replace itself stays proven on Beautify by case (a) and the double tap.
     */
    @Test
    fun the_camera_opens_the_edit_screen_without_replace_and_back_reaches_home() {
        launchWithFakes { scenario ->
            waitForHome(compose)
            tapCameraButton(compose)
            waitFor {
                compose.onAllNodes(hasTestTag(CAMERA_SHUTTER_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag(CAMERA_SHUTTER_TAG).performClick()
            waitFor { count(CAMERA_KEEP_TAG) > 0 }
            compose.onNodeWithTag(CAMERA_KEEP_TAG).performClick()
            waitFor { count(TOOL_SCREEN_TAG) > 0 && count(CAMERA_KEEP_TAG) == 0 }
            waitFor { count(TOOL_IMAGE_TAG) > 0 }
            assertToolTitle(Tool.EDIT)

            assertEquals("The Edit screen must have no Replace", 0, count(TOOL_REPLACE_TAG))
            assertBackFromToolReachesHome(scenario)
        }
    }

    /**
     * Case (d): My edits, tile 0, the viewer's "Edit": the Edit screen on that photo, with no
     * Replace; Back shows the photo screen on the same photo, and another Back shows My edits, its
     * tab selected.
     *
     * Run 9 (A-007): the Edit screen no longer has a Replace chip and the viewer only ever opens
     * Edit, so the step through the Edit gallery is gone; the test still proves the route, that
     * Replace is absent, and the back stack below the Edit screen (viewer, then My edits).
     */
    @Test
    fun my_edits_opens_the_edit_screen_without_replace_and_back_returns_to_the_viewer() {
        val photo = editedPhotos.first()
        launchWithFakes { scenario ->
            waitForHome(compose)
            openTab(compose, KansoTab.MY_EDITS)
            waitFor { count(MY_EDITS_SCREEN_TAG) > 0 && count(myEditsTileTag(0)) > 0 }
            compose.onNodeWithTag(myEditsTileTag(0)).tapAboveBar()
            waitFor { count(RESULT_SCREEN_TAG) > 0 && count(MY_EDITS_SCREEN_TAG) == 0 }
            waitFor { count(RESULT_EDIT_TAG) > 0 }
            compose.onNodeWithTag(RESULT_EDIT_TAG).performClick()
            waitForToolScreenOn(Tool.EDIT, photo.uri)
            assertEquals("The Edit screen must have no Replace", 0, count(TOOL_REPLACE_TAG))

            // Back: the photo screen the Edit screen was opened from, on the same photo.
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            val viewerPhoto = hasTestTag(RESULT_PHOTO_TAG) and SemanticsMatcher.expectValue(RESULT_PHOTO_URI_KEY, photo.uri)
            waitFor {
                count(TOOL_SCREEN_TAG) == 0 && count(GALLERY_SCREEN_TAG) == 0 && count(RESULT_SCREEN_TAG) > 0 &&
                    compose.onAllNodes(viewerPhoto).fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            assertEquals(0, count(MY_EDITS_SCREEN_TAG))

            // Another Back: My edits, with its tab selected.
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(RESULT_SCREEN_TAG) == 0 && count(MY_EDITS_SCREEN_TAG) > 0 }
            waitFor { count(kansoTabTag(KansoTab.MY_EDITS)) > 0 }
            compose.onNodeWithTag(kansoTabTag(KansoTab.MY_EDITS)).assertIsSelected()
            assertEquals(0, count(HOME_SCREEN_TAG))
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    /**
     * A double tap on Replace opens one gallery. The two taps are sent through Replace's own click
     * handler inside one main-thread task, so the screen cannot change between them: the worst
     * case of a fast double tap. Afterwards exactly one gallery is shown, no tool screen is left,
     * and one Back reaches Home, so the second tap piled nothing up.
     *
     * What this proves, and what it does not: it proves the outcome (one gallery, nothing piled
     * up), not the guard that brings it about. It cannot tell which part of the app ignored the
     * second tap: Replace's own guard, the navigation's handling of a repeated key, or anything
     * else on the way would all pass it. Removing one guard while another still stops the second
     * tap leaves this test green.
     *
     * Run 9 (A-007): the Edit screen has no Replace any more, so the double tap is made on the
     * Beautify screen, reached from Beautify's "Try now" and the gallery's first photo, as in case
     * (a); what it proves is unchanged.
     */
    @Test
    fun a_double_tap_on_replace_opens_one_gallery() {
        val photo1 = galleryPhotos.first()
        launchWithFakes { scenario ->
            bringBeautifyBannerToFront()
            compose.onNodeWithTag(homeTryTag(Tool.BEAUTIFY)).performSemanticsAction(SemanticsActions.OnClick)
            waitForGallery(Tool.BEAUTIFY, photo1.id)
            compose.onNodeWithTag(galleryPhotoTag(photo1.id)).performClick()
            waitForToolScreenOn(Tool.BEAUTIFY, photo1.uri)

            val replace = compose.onNodeWithTag(TOOL_REPLACE_TAG).fetchSemanticsNode()
            val click = checkNotNull(replace.config.getOrNull(SemanticsActions.OnClick)?.action) {
                "Replace has no click action"
            }
            compose.runOnUiThread {
                click()
                click()
            }

            waitForGallery(Tool.BEAUTIFY, photo1.id)
            compose.waitForIdle()
            assertEquals("A double tap on Replace must open one gallery", 1, count(GALLERY_SCREEN_TAG))
            assertOneBackReachesHome(scenario)
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Grants photo and camera access, loads the fakes ([galleryPhotos] as the photo library,
     * [editedPhotos] as Kanso's saved photos, the fake decoder and the fake camera), starts the
     * app and calls [body] with the scenario.
     */
    private fun launchWithFakes(body: (ActivityScenario<MainActivity>) -> Unit) {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA, *photoPermissionsFor(35).toTypedArray())
        loadKoinModules(
            module {
                single<PhotoSource> { FakePhotoSource(albums = listOf(Album(1L, "A")), allPhotos = galleryPhotos) }
                single<SavedPhotoSource> { FakeSavedPhotoSource(editedPhotos) }
                single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                factory<CameraController> { FakeCameraController() }
            },
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario -> body(scenario) }
    }

    /**
     * Swipes Home's carousel to the left, as a finger would, until Beautify's banner (the third,
     * so two swipes from the first) is in front: its "Try now" wholly inside the carousel. Each
     * swipe moves exactly one banner (the carousel's single-advance fling). Two more swipes are
     * allowed in case one of the first two did not register, then the test fails.
     */
    private fun bringBeautifyBannerToFront() {
        waitForHome(compose)
        waitFor { count(HOME_CAROUSEL_TAG) > 0 }
        compose.onNodeWithTag(HOME_CAROUSEL_TAG).performScrollTo()
        repeat(BEAUTIFY_SWIPES) { swipeCarouselLeft() }
        repeat(SPARE_SWIPES) { if (!beautifyTryNowInFront()) swipeCarouselLeft() }
        assertTrue("Beautify's banner should be in front after swiping", beautifyTryNowInFront())
    }

    /** Swipes Home's carousel one banner to the left and lets it settle. */
    private fun swipeCarouselLeft() {
        compose.onNodeWithTag(HOME_CAROUSEL_TAG).performTouchInput {
            swipeLeft(startX = width * 0.85f, endX = width * 0.4f, durationMillis = 1_000)
        }
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Tells whether Beautify's "Try now" is composed and lies wholly inside the carousel, from its
     * laid-out position and size (`positionInRoot`, which is not clipped to what is visible).
     */
    private fun beautifyTryNowInFront(): Boolean {
        val tryNow = compose.onAllNodesWithTag(homeTryTag(Tool.BEAUTIFY)).fetchSemanticsNodes().singleOrNull()
            ?: return false
        val carousel = compose.onNodeWithTag(HOME_CAROUSEL_TAG).fetchSemanticsNode()
        val left = tryNow.positionInRoot.x
        val right = left + tryNow.size.width
        return left >= carousel.positionInRoot.x - 1f && right <= carousel.positionInRoot.x + carousel.size.width + 1f
    }

    /**
     * Waits until the gallery titled "<tool>: choose a photo" is shown, no tool screen is left,
     * and the photo [photoId] is among its cells.
     */
    private fun waitForGallery(tool: Tool, photoId: Long) {
        val title = app.getString(R.string.gallery_title, app.getString(tool.labelRes))
        val titled = hasText(title) and hasAnyAncestor(hasTestTag(GALLERY_SCREEN_TAG))
        waitFor {
            count(TOOL_SCREEN_TAG) == 0 &&
                compose.onAllNodes(titled).fetchSemanticsNodes().isNotEmpty() &&
                count(galleryPhotoTag(photoId)) > 0
        }
    }

    /**
     * Waits until [tool]'s screen shows the photo [photoUri] with no gallery left, and checks that
     * its title names [tool].
     */
    private fun waitForToolScreenOn(tool: Tool, photoUri: String) {
        val shownPhoto = hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, photoUri)
        waitFor { count(GALLERY_SCREEN_TAG) == 0 && compose.onAllNodes(shownPhoto).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(1, count(TOOL_SCREEN_TAG))
        assertToolTitle(tool)
    }

    /** Checks that the tool screen's title is [tool]'s name, shown exactly once. */
    private fun assertToolTitle(tool: Tool) {
        val title = hasText(app.getString(tool.labelRes)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG))
        assertEquals(1, compose.onAllNodes(title).fetchSemanticsNodes().size)
    }

    /**
     * With a gallery on top and no tool screen left, presses Back once and checks that Home is
     * shown, with no gallery or tool screen, and that the app is still open.
     */
    private fun assertOneBackReachesHome(scenario: ActivityScenario<MainActivity>) {
        assertEquals(0, count(TOOL_SCREEN_TAG))
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor { count(GALLERY_SCREEN_TAG) == 0 && count(HOME_SCREEN_TAG) > 0 }
        assertEquals(0, count(TOOL_SCREEN_TAG))
        compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
        scenario.onActivity { assertFalse(it.isFinishing) }
    }

    /**
     * With a tool screen on top, presses Back once and checks that Home is shown, with no tool
     * screen or gallery left, and that the app is still open. Run 9: the Edit screen's routes,
     * which can no longer reach Replace, check their way back with this.
     */
    private fun assertBackFromToolReachesHome(scenario: ActivityScenario<MainActivity>) {
        assertEquals(1, count(TOOL_SCREEN_TAG))
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor { count(TOOL_SCREEN_TAG) == 0 && count(HOME_SCREEN_TAG) > 0 }
        assertEquals(0, count(GALLERY_SCREEN_TAG))
        compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
        scenario.onActivity { assertFalse(it.isFinishing) }
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

    /** The photos and numbers the tests share. */
    private companion object {
        /** Left swipes from the first banner to Beautify's, the third. */
        const val BEAUTIFY_SWIPES = 2

        /** Extra swipes allowed if one did not register. */
        const val SPARE_SWIPES = 2

        /** The photo library: three photos, newest first, with ids 1 to 3. */
        val galleryPhotos: List<Photo> = GalleryGridTest.manyPhotos(3)

        /**
         * The photos Kanso has saved, newest first, as Home's strip and My edits list them. Their
         * Uris differ from the library's, so a screen on one cannot pass for the other.
         */
        val editedPhotos: List<Photo> = (0 until 3).map { i ->
            Photo(
                id = 300L + i,
                uri = "content://media/external/images/media/${300 + i}",
                name = "Kanso_$i.jpg",
                albumId = 2L,
                modifiedSeconds = 1_791_000_000L - i * 3_600L,
            )
        }
    }
}
