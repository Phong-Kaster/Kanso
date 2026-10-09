package com.phongkaster.kanso.navigation

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.camera.FakeCameraController
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CAMERA_KEEP_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SCREEN_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.home.HOME_OPEN_PHOTO_TAG
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.home.homeToolTag
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.editorToolTag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Drives the real app (MainActivity, started on the JVM by Robolectric) through its navigation:
 * Home shows "Open a photo", the Camera shortcut, the five tool cards in their two groups and
 * Settings; each card opens its tool's gallery and Back returns to Home; the Camera shortcut opens
 * the camera, whose kept shot opens the editor on Edit right above Home; Back on Home closes the
 * app; rotation keeps the current screen; each screen's ViewModel lives exactly as long as its
 * back stack entry; and no permission is asked for before the screen that needs it.
 *
 * Home's cards are always found by their tag (`homeToolTag`), never by their text alone: the
 * "Edit" header and the Edit card read the same word.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NavigationTest {

    /** Lets the test find and check Compose nodes in whatever activity the test launches. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** The app context, used to read the same string resources the screens show. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** The application under test, used to grant permissions before a launch. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test. KansoApplication starts Koin for every test's fresh
     * Application, but Koin's global state outlives it inside the same JVM, and starting it
     * twice fails.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * DoD 5: inside Home's one scrolling column there are the title "Kanso" exactly once, a
     * clickable "Open a photo", a clickable "Camera" exactly once (and no Camera card), the
     * headers "AI tools" and "Edit", five clickable tool cards each showing its label, and a
     * clickable "Settings" exactly once. The four AI tool cards lie between the two headers and
     * the Edit card below the "Edit" header (assumption A-001).
     */
    @Test
    fun home_lists_open_a_photo_camera_five_tool_cards_and_settings() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            compose.onAllNodesWithText("Kanso").assertCountEquals(1)
            compose.onNodeWithText("Kanso").assert(insideTheColumn)

            val openPhoto = context.getString(R.string.home_open_photo)
            compose.onAllNodesWithText(openPhoto).assertCountEquals(1)
            compose.onNodeWithTag(HOME_OPEN_PHOTO_TAG)
                .assert(hasText(openPhoto))
                .assert(insideTheColumn)
                .assertHasClickAction()

            val camera = context.getString(R.string.tool_camera)
            compose.onAllNodesWithText(camera).assertCountEquals(1)
            compose.onNodeWithText(camera)
                .assert(insideTheColumn)
                .assert(!isToolCard)
                .assertHasClickAction()
            compose.onAllNodesWithTag(HOME_CARD_TAG_PREFIX + "CAMERA").assertCountEquals(0)

            val settings = context.getString(R.string.settings)
            compose.onAllNodesWithText(settings).assertCountEquals(1)
            compose.onNodeWithText(settings)
                .assert(insideTheColumn)
                .assertHasClickAction()

            val aiHeader = header(R.string.home_header_ai_tools)
            val editHeader = header(R.string.home_header_edit)
            compose.onAllNodes(aiHeader).assertCountEquals(1)
            compose.onAllNodes(editHeader).assertCountEquals(1)
            compose.onNode(aiHeader).assert(insideTheColumn)
            compose.onNode(editHeader).assert(insideTheColumn)

            compose.onAllNodes(isToolCard).assertCountEquals(5)
            Tool.editingTools.forEach { tool ->
                compose.onNodeWithTag(homeToolTag(tool))
                    .assert(hasText(label(tool)))
                    .assert(insideTheColumn)
                    .assertHasClickAction()
            }

            // positionInRoot, not boundsInRoot: the column is taller than the screen, and
            // boundsInRoot is clipped to what is visible (a row scrolled out of view reads 0).
            val aiHeaderTop = top(aiHeader)
            val editHeaderTop = top(editHeader)
            assertTrue("The AI tools header must come before the Edit header", aiHeaderTop < editHeaderTop)
            AI_TOOLS.forEach { tool ->
                val cardTop = top(hasTestTag(homeToolTag(tool)))
                assertTrue("${tool.name} must lie below the AI tools header", cardTop > aiHeaderTop)
                assertTrue("${tool.name} must lie above the Edit header", cardTop < editHeaderTop)
            }
            assertTrue(
                "The Edit card must lie below the Edit header",
                top(hasTestTag(homeToolTag(Tool.EDIT))) > editHeaderTop,
            )
        }
    }

    /**
     * DoD 1 and 5: each of the five tool cards opens the photo gallery titled
     * "<tool>: choose a photo", and Back returns to Home each time.
     */
    @Test
    fun each_tool_opens_its_screen_and_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            Tool.editingTools.forEach { tool ->
                tapCard(tool)
                waitForGallery()
                compose.onNodeWithTag(GALLERY_SCREEN_TAG).assertIsDisplayed()
                // The gallery's title names the tool, so a card opening another tool's gallery
                // would fail here.
                compose.onNode(
                    hasText(galleryTitle(tool)) and hasAnyAncestor(hasTestTag(GALLERY_SCREEN_TAG)),
                ).assertIsDisplayed()

                Espresso.pressBack()
                waitForHomeOnly()
                assertEquals(Lifecycle.State.RESUMED, scenario.state)
                scenario.onActivity { activity -> assertFalse(activity.isFinishing) }
            }
        }
    }

    /**
     * DoD 15 (and DoD 17 of an earlier run): launching the app asks for no permission; the first
     * tap on a tool card (Edit) asks for the two photo permissions Android 15 uses, and nothing
     * else.
     */
    @Test
    fun launch_asks_for_nothing_and_the_gallery_asks_for_photo_access() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            scenario.onActivity { assertNull(shadowOf(it).lastRequestedPermission) }

            tapCard(Tool.EDIT)
            waitForGallery()

            assertLastRequest(scenario, PHOTO_PERMISSIONS)
        }
    }

    /**
     * DoD 15: launching asks for nothing; tapping "Open a photo" asks for the two photo
     * permissions, and nothing else.
     */
    @Test
    fun open_a_photo_asks_only_for_photo_access() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            scenario.onActivity { assertNull(shadowOf(it).lastRequestedPermission) }

            compose.onNodeWithTag(HOME_OPEN_PHOTO_TAG).performScrollTo().performClick()
            waitForGallery()

            assertLastRequest(scenario, PHOTO_PERMISSIONS)
            // Robolectric keeps only the latest request, so "only" also rests on this: the camera
            // screen, the one place that asks for CAMERA, was never opened.
            assertEquals(0, count(CAMERA_SCREEN_TAG))
        }
    }

    /**
     * DoD 15: launching asks for nothing; tapping the Camera shortcut asks for the camera
     * permission only. The camera is swapped for a fake, because Robolectric cannot run CameraX.
     */
    @Test
    fun the_camera_shortcut_asks_only_for_camera() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Koin lets a later module replace an earlier recipe with the same type.
            loadKoinModules(module { factory<CameraController> { FakeCameraController() } })
            waitForHome()
            scenario.onActivity { assertNull(shadowOf(it).lastRequestedPermission) }

            tapCamera()
            waitFor { count(CAMERA_SCREEN_TAG) > 0 }

            assertLastRequest(scenario, listOf(Manifest.permission.CAMERA))
            // Robolectric keeps only the latest request, so "only" also rests on this: the
            // gallery, the one place that asks for photos, was never opened.
            assertEquals(0, count(GALLERY_SCREEN_TAG))
        }
    }

    /**
     * DoD 5: with camera access granted, the Camera shortcut opens the camera; the shutter and
     * then Keep open the editor on the shot with Edit on and the other four tools off, with no
     * gallery in between. A test cannot read the back stack, so it is read from where Back leads:
     * one Back from the editor reaches Home, so the stack was [Home, ToolPhoto(EDIT, shot)].
     */
    @Test
    fun the_camera_shortcut_opens_the_camera_and_keep_opens_the_editor_on_edit() {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            loadKoinModules(
                module {
                    factory<CameraController> { FakeCameraController() }
                    single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                },
            )
            waitForHome()
            tapCamera()
            waitFor {
                compose.onAllNodes(hasTestTag(CAMERA_SHUTTER_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag(CAMERA_SHUTTER_TAG).performClick()
            waitFor { count(CAMERA_KEEP_TAG) > 0 }
            compose.onNodeWithTag(CAMERA_KEEP_TAG).performClick()
            // The photo is shown once the editor has decoded the shot.
            waitFor { count(CAMERA_SCREEN_TAG) == 0 && count(TOOL_IMAGE_TAG) > 0 }

            assertEquals(1, count(TOOL_SCREEN_TAG))
            assertEquals(0, count(GALLERY_SCREEN_TAG))
            Tool.editingTools.forEach { tool ->
                val toggle = compose.onNodeWithTag(editorToolTag(tool))
                if (tool == Tool.EDIT) toggle.assertIsOn() else toggle.assertIsOff()
            }

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(TOOL_SCREEN_TAG) == 0 }
            assertEquals(0, count(GALLERY_SCREEN_TAG))
            assertEquals(0, count(CAMERA_SCREEN_TAG))
            compose.onNodeWithTag(HOME_OPEN_PHOTO_TAG).assertExists()
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    /** DoD 6: the Settings row opens Settings, and Back returns to Home. */
    @Test
    fun settings_opens_and_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            compose.onNodeWithText(context.getString(R.string.settings))
                .performScrollTo()
                .performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(
                hasText(context.getString(R.string.settings)) and
                    hasAnyAncestor(hasTestTag(SETTINGS_SCREEN_TAG)),
            ).assertIsDisplayed()

            Espresso.pressBack()
            waitForHomeOnly()
            scenario.onActivity { assertFalse(it.isFinishing) }
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    /** DoD 7: Back on Home closes the app: the activity is destroyed. */
    @Test
    fun back_on_home_closes_the_app() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            var finishing = false
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            // finish() marks the activity as finishing at once; the teardown follows later.
            scenario.onActivity { finishing = it.isFinishing }
            assertTrue("Back on Home should finish the activity", finishing)
        }
    }

    /** DoD 8: rotating (recreating the activity) on the Edit gallery keeps it on screen. */
    @Test
    fun gallery_survives_recreation_and_one_back_returns_home() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            tapCard(Tool.EDIT)
            waitForGallery()

            scenario.recreate()
            waitForGallery()
            compose.onNodeWithTag(GALLERY_SCREEN_TAG).assertIsDisplayed()

            Espresso.pressBack()
            waitForHomeOnly()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    /**
     * DoD 10: a gallery's ViewModel is cleared when the gallery is popped, while Home's
     * ViewModel, whose entry stays on the back stack, is kept.
     */
    @Test
    fun popping_a_gallery_clears_its_view_model_but_not_homes() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForHome()
            val probe = KoinPlatform.getKoin().get<ViewModelClearedProbe>()

            tapCard(Tool.EDIT)
            waitForGallery()
            assertFalse(probe.wasCleared(GalleryViewModel::class))

            Espresso.pressBack()
            waitForHomeOnly()
            compose.waitUntil(timeoutMillis = 10_000) {
                probe.wasCleared(GalleryViewModel::class)
            }
            assertTrue(probe.wasCleared(GalleryViewModel::class))
            assertFalse(probe.wasCleared(HomeViewModel::class))
        }
    }

    /** Returns the label Home shows for [tool]. */
    private fun label(tool: Tool): String = context.getString(tool.labelRes)

    /** Returns the title of [tool]'s gallery, such as "Retouch: choose a photo". */
    private fun galleryTitle(tool: Tool): String = context.getString(R.string.gallery_title, label(tool))

    /**
     * Matches the group header reading the string [textRes]: a text marked as a heading, which
     * the Edit card (same word, but a button) is not.
     */
    private fun header(textRes: Int): SemanticsMatcher =
        hasText(context.getString(textRes)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    /** Returns the top of the one node matching [matcher], where it is laid out, in pixels. */
    private fun top(matcher: SemanticsMatcher): Float = compose.onNode(matcher).fetchSemanticsNode().positionInRoot.y

    /** Scrolls Home to [tool]'s card, found by its tag, and taps it. */
    private fun tapCard(tool: Tool) {
        compose.onNodeWithTag(homeToolTag(tool)).performScrollTo().performClick()
    }

    /** Scrolls Home to the Camera shortcut, found by its text "Camera", and taps it. */
    private fun tapCamera() {
        compose.onNodeWithText(context.getString(R.string.tool_camera)).performScrollTo().performClick()
    }

    /**
     * Fails unless the last permission request Robolectric recorded for [scenario]'s activity
     * asked for exactly [permissions], in that order.
     */
    private fun assertLastRequest(scenario: ActivityScenario<MainActivity>, permissions: List<String>) {
        scenario.onActivity { activity ->
            val request = shadowOf(activity).lastRequestedPermission
            assertEquals(permissions, request?.requestedPermissions?.toList())
        }
    }

    /** Waits until the gallery screen is on screen. */
    private fun waitForGallery() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag(GALLERY_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Waits until Home's title is on screen. */
    private fun waitForHome() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Waits until Home is shown and no gallery or Settings screen is left on screen. Home is
     * checked by its own tag, not by its title: the title is the first item of Home's scrolling
     * column, and Home keeps its scroll position, so after a test scrolled down to "Settings" and
     * came back the title can be scrolled out of view while Home is on screen.
     */
    private fun waitForHomeOnly() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Kanso").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag(GALLERY_SCREEN_TAG).fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodesWithTag(SETTINGS_SCREEN_TAG).fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag(HOME_SCREEN_TAG).assertIsDisplayed()
    }

    /**
     * Waits up to ten seconds of real time for [condition], letting the main thread run in
     * between: the camera and the editor finish their work on the main thread, and under
     * Robolectric the test runs on that thread itself, so its queue only moves when it is run.
     */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** Values and matchers the tests share. */
    private companion object {
        /** The four tools of the "AI tools" group, in the order Home lists them (assumption A-001). */
        val AI_TOOLS = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC)

        /** The two photo permissions a gallery asks for on Android 15. */
        val PHOTO_PERMISSIONS = listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        /** The start of every Home card's tag, `home-tool-`, taken from `homeToolTag` itself. */
        val HOME_CARD_TAG_PREFIX = homeToolTag(Tool.EDIT).removeSuffix(Tool.EDIT.name)

        /** Matches every tool card on Home, whatever its tool. */
        val isToolCard = SemanticsMatcher("is a Home tool card") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(HOME_CARD_TAG_PREFIX) == true
        }

        /** Matches a node inside a scrolling container: on Home, its one scrolling column. */
        val insideTheColumn = hasAnyAncestor(hasScrollAction())
    }
}
