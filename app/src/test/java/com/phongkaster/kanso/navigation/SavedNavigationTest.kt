package com.phongkaster.kanso.navigation

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.camera.FakeCameraController
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.GalleryGridTest
import com.phongkaster.kanso.save.FakePhotoSaver
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CAMERA_KEEP_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.gallery.GALLERY_CAMERA_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.galleryPhotoTag
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import com.phongkaster.kanso.ui.saved.SAVED_EDIT_ANOTHER_TAG
import com.phongkaster.kanso.ui.saved.SAVED_HOME_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.EDITOR_TOOLBAR_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SAVE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.editorToolTag
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
 * DoD 6 (gallery path) and DoD 15 (back stack), through the app's real navigation: from Home,
 * Retouch opens its gallery; the gallery's camera button opens the camera; Keep opens the
 * Retouch tool screen; Save replaces everything above Home with the Saved screen; Edit another
 * leaves the Retouch gallery above Home; Home and system Back both leave only Home.
 *
 * Run 6, DoD 7: the tool the person switched to in the editor is the one that counts after
 * saving. Home's Camera tile opens the editor on Edit; the toolbar switches it to Retouch; Save,
 * then Edit another, opens the Retouch gallery with only Home below it, and Home leaves only Home.
 * Like the walks above, the back stack is read from what is on screen and from where Back leads.
 *
 * Run 6, DoD 3: the editor opened from Home's Edit entry and a gallery photo switches between the
 * five editing tools in place, on the same photo, and Back still leads to the gallery, then Home.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SavedNavigationTest {

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

    /** Gallery camera button, Keep: the Retouch tool screen opens, not the Edit one. */
    @Test
    fun gallery_camera_keep_opens_the_galleries_own_tool() {
        launchToSaved(finish = false) {
            compose.onNode(
                hasText(app.getString(R.string.tool_retouch)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
            ).assertExists()
        }
    }

    /** Edit another leaves [Home, Gallery(Retouch)]: the gallery shows and Back reaches Home. */
    @Test
    fun edit_another_leaves_home_and_the_tools_gallery() {
        launchToSaved { scenario ->
            compose.onNodeWithTag(SAVED_EDIT_ANOTHER_TAG).performClick()
            waitFor { count(GALLERY_SCREEN_TAG) > 0 && count(SAVED_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(GALLERY_SCREEN_TAG) == 0 }
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
        }
    }

    /** Home leaves [Home]: no Saved, tool or gallery screen below it. */
    @Test
    fun home_leaves_only_home() {
        launchToSaved {
            compose.onNodeWithTag(SAVED_HOME_TAG).performClick()
            waitFor { count(SAVED_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            assertEquals(0, count(GALLERY_SCREEN_TAG))
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
        }
    }

    /** System Back from Saved goes to Home, not to the tool screen. */
    @Test
    fun back_from_saved_goes_home() {
        launchToSaved { scenario ->
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(SAVED_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
        }
    }

    /**
     * DoD 7: editor opened on Edit, switched to Retouch, saved; Edit another opens the gallery
     * titled "Retouch: choose a photo" with no tool or Saved screen left, and Back from it reaches
     * Home, so the back stack was [Home, Gallery(Retouch)].
     */
    @Test
    fun edit_another_after_switching_tools_opens_the_switched_tools_gallery() {
        saveAfterSwitchingTo(Tool.RETOUCH) { scenario ->
            compose.onNodeWithTag(SAVED_EDIT_ANOTHER_TAG).performClick()
            val title = app.getString(R.string.gallery_title, app.getString(R.string.tool_retouch))
            waitFor { compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty() && count(SAVED_SCREEN_TAG) == 0 }
            assertEquals(1, count(GALLERY_SCREEN_TAG))
            assertEquals(0, count(TOOL_SCREEN_TAG))
            // The editor opened on Edit; its gallery must not be the one shown.
            val editTitle = app.getString(R.string.gallery_title, app.getString(R.string.tool_edit))
            assertEquals(0, compose.onAllNodesWithText(editTitle).fetchSemanticsNodes().size)

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(GALLERY_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            assertEquals(0, count(SAVED_SCREEN_TAG))
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
        }
    }

    /** DoD 7: after saving a switched tool, Home leaves only Home: no Saved, tool or gallery screen. */
    @Test
    fun home_after_switching_tools_leaves_only_home() {
        saveAfterSwitchingTo(Tool.RETOUCH) {
            compose.onNodeWithTag(SAVED_HOME_TAG).performClick()
            waitFor { count(SAVED_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            assertEquals(0, count(GALLERY_SCREEN_TAG))
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
        }
    }

    /**
     * DoD 3, through the app's real navigation from a fresh install with photo access granted:
     * Home, the Edit entry, photo 1, so the editor is on top. Its toolbar holds exactly five
     * toggles, from left to right Edit, Remove background, Enhance, Retouch and Artistic, each
     * named only by its content description, Edit on and the other four off, and no Camera
     * toggle. Tapping each of the other four in turn leaves only that one on and makes its name
     * the title, on the same photo, which is decoded once in all. The back stack kept its size:
     * one Back returns to the gallery titled "Edit: choose a photo", a second one to Home.
     */
    @Test
    fun the_editor_switches_tools_in_place_on_the_chosen_photo() {
        shadowOf(app).grantPermissions(*photoPermissionsFor(35).toTypedArray())
        val photos = GalleryGridTest.manyPhotos(3)
        // Photo 1 is the newest, so it is the first cell of the gallery.
        val photo = photos.first()
        val source = FakePhotoSource(albums = listOf(Album(1L, "A")), allPhotos = photos)
        val loader = FakePhotoLoader(app, ToolFixtures.gradient(60, 40))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Koin lets a later module replace an earlier recipe with the same type.
            loadKoinModules(
                module {
                    single<PhotoSource> { source }
                    single<PhotoLoader> { loader }
                },
            )
            tapHomeEditEntry()
            waitFor { count(galleryPhotoTag(photo.id)) > 0 }
            compose.onNodeWithTag(galleryPhotoTag(photo.id)).performClick()
            // The photo marker: the image node names the Uri of the photo it shows.
            val shownPhoto = hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, photo.uri)
            waitFor { count(GALLERY_SCREEN_TAG) == 0 && compose.onAllNodes(shownPhoto).fetchSemanticsNodes().isNotEmpty() }

            val toggles = compose.onAllNodes(isToggleable() and hasAnyAncestor(hasTestTag(EDITOR_TOOLBAR_TAG)))
            toggles.assertCountEquals(EDITING_TOOLS.size)
            val leftToRight = toggles.fetchSemanticsNodes().sortedBy { it.boundsInRoot.left }
            assertEquals(
                EDITING_TOOLS.map { app.getString(it.labelRes) },
                leftToRight.map { it.config[SemanticsProperties.ContentDescription].single() },
            )
            assertEquals(EDITING_TOOLS.map { editorToolTag(it) }, leftToRight.map { it.config[SemanticsProperties.TestTag] })
            leftToRight.forEach { node ->
                assertFalse("A toggle shows text: ${node.config}", node.config.contains(SemanticsProperties.Text))
            }
            // Camera is not an editing tool: no toggle is named after it or tagged for it.
            compose.onAllNodes(
                hasContentDescription(app.getString(R.string.tool_camera)) and
                    hasAnyAncestor(hasTestTag(EDITOR_TOOLBAR_TAG)),
            ).assertCountEquals(0)
            assertEquals(0, count(CAMERA_TOGGLE_TAG))
            assertOnlyOn(Tool.EDIT)
            assertEquals(1, titleCount(Tool.EDIT))

            EDITING_TOOLS.drop(1).forEach { tool ->
                compose.onNodeWithTag(editorToolTag(tool)).performClick()
                waitFor { titleCount(tool) == 1 }

                assertOnlyOn(tool)
                EDITING_TOOLS.filter { it != tool }.forEach { other -> assertEquals(0, titleCount(other)) }
                compose.onAllNodes(shownPhoto).assertCountEquals(1)
                // Still the one editor: the switch opened no screen and closed none.
                assertEquals(1, count(TOOL_SCREEN_TAG))
                assertEquals(0, count(GALLERY_SCREEN_TAG))
            }
            assertEquals("The photo was decoded again: ${loader.requestedEdges}", 1, loader.requestedEdges.size)

            // The back stack is still [Home, Gallery(EDIT), ToolPhoto(EDIT, photo 1)].
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            val galleryTitle = app.getString(R.string.gallery_title, app.getString(R.string.tool_edit))
            waitFor { count(TOOL_SCREEN_TAG) == 0 && compose.onAllNodesWithText(galleryTitle).fetchSemanticsNodes().isNotEmpty() }
            assertEquals(1, count(GALLERY_SCREEN_TAG))

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(GALLERY_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertExists()
        }
    }

    /**
     * Taps Home's Edit entry. Home's tool cards are addressed by the tag `home-tool-<TOOL_NAME>`,
     * because the "Edit" group header and the Edit card read the same. While Home still lists its
     * tools as plain rows without that tag, the entry is the one clickable node reading "Edit".
     */
    private fun tapHomeEditEntry() {
        val byText = hasText(app.getString(R.string.tool_edit)) and hasClickAction()
        waitFor { count(HOME_EDIT_CARD_TAG) > 0 || compose.onAllNodes(byText).fetchSemanticsNodes().isNotEmpty() }
        val entry = if (count(HOME_EDIT_CARD_TAG) > 0) hasTestTag(HOME_EDIT_CARD_TAG) else byText
        compose.onNode(entry).tapAboveBar()
    }

    /** Checks that [selected]'s toolbar toggle is on and every other editing tool's toggle is off. */
    private fun assertOnlyOn(selected: Tool) {
        EDITING_TOOLS.forEach { tool ->
            val toggle = compose.onNodeWithTag(editorToolTag(tool))
            if (tool == selected) toggle.assertIsOn() else toggle.assertIsOff()
        }
    }

    /**
     * Runs the walk Home, Camera tile, shutter, Keep (which opens the editor on Edit), then
     * switches the editor to [tool] with its toolbar toggle and taps Save, and calls [body] with
     * the scenario once the Saved screen is shown.
     */
    private fun saveAfterSwitchingTo(tool: Tool, body: (ActivityScenario<MainActivity>) -> Unit) {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA, *photoPermissionsFor(35).toTypedArray())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadKoinModules(
                module {
                    factory<CameraController> { FakeCameraController() }
                    single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                    single<PhotoSource> { FakePhotoSource(albums = emptyList(), allPhotos = emptyList()) }
                    single<PhotoSaver> { FakePhotoSaver() }
                },
            )
            // The floating bar's Camera button opens the camera for the Edit tool.
            waitFor { count(KANSO_CAMERA_BUTTON_TAG) > 0 }
            tapCameraButton(compose)
            waitFor {
                compose.onAllNodes(hasTestTag(CAMERA_SHUTTER_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag(CAMERA_SHUTTER_TAG).performClick()
            waitFor { count(CAMERA_KEEP_TAG) > 0 }
            compose.onNodeWithTag(CAMERA_KEEP_TAG).performClick()
            waitFor { count(TOOL_SCREEN_TAG) > 0 && count(editorToolTag(tool)) > 0 }
            waitFor { titleCount(Tool.EDIT) == 1 }

            compose.onNodeWithTag(editorToolTag(tool)).performClick()
            waitFor { titleCount(tool) == 1 && titleCount(Tool.EDIT) == 0 }

            waitFor {
                compose.onAllNodes(hasTestTag(TOOL_SAVE_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
            waitFor { count(SAVED_SCREEN_TAG) > 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            body(scenario)
        }
    }

    /** Counts the texts on the tool screen that read [tool]'s name (the editor's title). */
    private fun titleCount(tool: Tool): Int =
        compose.onAllNodes(hasText(app.getString(tool.labelRes)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)))
            .fetchSemanticsNodes().size

    /**
     * Runs the walk Home, Retouch, gallery camera button, shutter, Keep, then (when [finish]) Save,
     * and calls [body] with the scenario at the end of it.
     */
    private fun launchToSaved(finish: Boolean = true, body: (ActivityScenario<MainActivity>) -> Unit) {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA, *photoPermissionsFor(35).toTypedArray())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadKoinModules(
                module {
                    factory<CameraController> { FakeCameraController() }
                    single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                    single<PhotoSource> { FakePhotoSource(albums = emptyList(), allPhotos = emptyList()) }
                    single<PhotoSaver> { FakePhotoSaver() }
                },
            )
            val label = app.getString(R.string.tool_retouch)
            waitFor { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(label).tapAboveBar()
            waitFor { count(GALLERY_CAMERA_TAG) > 0 }
            compose.onNodeWithTag(GALLERY_CAMERA_TAG).performClick()
            waitFor {
                compose.onAllNodes(hasTestTag(CAMERA_SHUTTER_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag(CAMERA_SHUTTER_TAG).performClick()
            waitFor { count(CAMERA_KEEP_TAG) > 0 }
            compose.onNodeWithTag(CAMERA_KEEP_TAG).performClick()
            waitFor { count(TOOL_SCREEN_TAG) > 0 }
            if (finish) {
                waitFor {
                    compose.onAllNodes(hasTestTag(TOOL_SAVE_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
                waitFor { count(SAVED_SCREEN_TAG) > 0 }
                assertEquals(0, count(TOOL_SCREEN_TAG))
            }
            body(scenario)
        }
    }

    /** Waits up to ten seconds for [condition], letting the main thread run in between. */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** Values the walks share. */
    private companion object {
        /** The five editing tools in the order the editor's toolbar must show them, left to right. */
        val EDITING_TOOLS = listOf(Tool.EDIT, Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC)

        /** The tag of Home's Edit card (`home-tool-<TOOL_NAME>`). */
        const val HOME_EDIT_CARD_TAG = "home-tool-EDIT"

        /** The tag a Camera toggle would carry in the editor's toolbar; none may exist. */
        const val CAMERA_TOGGLE_TAG = "editor-tool-CAMERA"
    }
}
