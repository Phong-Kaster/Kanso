package com.phongkaster.kanso.tool

import android.app.Application
import android.content.Context
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.GalleryGridTest
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.save.FakePhotoSaver
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.galleryPhotoTag
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import com.phongkaster.kanso.ui.tool.EDITOR_PANEL_TAG
import com.phongkaster.kanso.ui.tool.EDITOR_TOOLBAR_TAG
import com.phongkaster.kanso.ui.tool.TOOL_BODY_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SAVE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_UNREADABLE_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import com.phongkaster.kanso.ui.tool.editorToolTag
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks the tool screen (the editor) on Android 15 (DoD 8): a photo tagged "turned" is drawn
 * upright and fitted inside the photo area, the image node names the photo's Uri, an unreadable
 * photo gives a plain message, Save is shown but disabled, and the back arrow calls back.
 *
 * It also checks the editor's controls (Run 6, DoD 3 and 4): the floating toolbar holds the five
 * editing tools as icon-only toggles in order, switching between them changes the title, the
 * selected toggle and the panel's message but never the photo; the photo area, the panel and the
 * toolbar are stacked from top to bottom; and Back, Save and every toggle are at least 48 dp.
 * While a save runs the toggles are switched off (DoD 7), so the tool saved with cannot change.
 *
 * One test drives the whole app instead of a hosted screen (DoD 3): Home, the Edit entry and a
 * gallery photo open the editor, whose toolbar then switches between the five tools in place.
 * Another hands the screen the insets of a phone with a 0 dp navigation bar and a bottom gesture
 * zone, and checks that the panel and the toolbar stay above the zone.
 *
 * The screen runs on the same phone size as the pictures (411 x 891 dp, xxhdpi).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ToolScreenTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** A fresh folder for the fixture files, deleted after each test. */
    @get:Rule
    val temp = TemporaryFolder()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * A 400 x 200 JPEG with EXIF orientation 6, through the real loader: the drawn image is
     * taller than wide, lies inside the area below the top bar, and carries the photo's Uri.
     */
    @Test
    fun a_turned_photo_is_drawn_upright_and_fitted() {
        val file = ToolFixtures.writeJpeg(
            temp.newFile("turned.jpg"),
            ToolFixtures.twoHalves(400, 200),
            ExifInterface.ORIENTATION_ROTATE_90,
        )
        val uri = ToolFixtures.serve(context, file, "turned.jpg")
        show(ToolViewModel(Tool.EDIT, uri, PhotoLoader(context), ViewModelClearedProbe(), NoSavePhotoSaver()))
        waitForImage()

        val image = compose.onNodeWithTag(TOOL_IMAGE_TAG).fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithTag(TOOL_BODY_TAG).fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertTrue("The photo is not taller than wide: $image", image.height > image.width)
        assertInside(image, body)
        assertInside(image, root)
        // Fitted: the photo keeps its 1:2 shape and fills the area's width or its height,
        // whichever is tighter, so it is neither stretched, cropped nor shrunk more than needed.
        assertEquals(image.height / 2f, image.width, 2f)
        assertTrue(
            "The photo $image does not fill $body on either side",
            kotlin.math.abs(image.width - body.width) <= 1f ||
                kotlin.math.abs(image.height - body.height) <= 1f,
        )

        compose.onNode(hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, uri))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.tool_photo_description))
            .assertIsDisplayed()
    }

    /** The top bar shows the tool's name, and Save is visible but cannot be tapped yet. */
    @Test
    fun the_top_bar_names_the_tool_and_save_is_visible_and_enabled() {
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        show(ToolViewModel(Tool.RETOUCH, "content://media/external/images/media/7", loader, ViewModelClearedProbe(), NoSavePhotoSaver()))
        waitForImage()

        compose.onNode(
            hasText(context.getString(R.string.tool_retouch)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
        ).assertIsDisplayed()
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsDisplayed().assertIsEnabled()
    }

    /**
     * The screen asks for the photo at twice the phone's longest edge: 891 dp at 3 pixels per dp
     * is 2673 pixels, so 5346; and only once, however often it is drawn.
     */
    @Test
    fun the_photo_is_asked_for_at_twice_the_screen_s_longest_edge() {
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        show(ToolViewModel(Tool.EDIT, "content://media/external/images/media/7", loader, ViewModelClearedProbe(), NoSavePhotoSaver()))
        waitForImage()

        assertEquals(listOf(2 * 891 * 3), loader.requestedEdges.toList())
    }

    /** A photo that cannot be opened gives the plain message, and no image. */
    @Test
    fun an_unreadable_photo_shows_a_plain_message() {
        show(ToolViewModel(Tool.EDIT, ToolFixtures.MISSING_URI, PhotoLoader(context), ViewModelClearedProbe(), NoSavePhotoSaver()))
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(TOOL_UNREADABLE_TAG)).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag(TOOL_UNREADABLE_TAG).assertIsDisplayed()
        compose.onNode(hasText(context.getString(R.string.tool_photo_unreadable))).assertIsDisplayed()
        assertTrue(compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsDisplayed()
    }

    /** The back arrow calls onBack. */
    @Test
    fun the_back_arrow_calls_on_back() {
        var backCalls = 0
        val loader = FakePhotoLoader(context, null)
        show(ToolViewModel(Tool.EDIT, "content://x/1", loader, ViewModelClearedProbe(), NoSavePhotoSaver()), onBack = { backCalls++ })

        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertEquals(1, backCalls)
    }

    /** Asking again for the same size decodes nothing more; clearing the ViewModel tells the probe. */
    @Test
    fun loading_is_done_once_per_size_and_clearing_is_reported() {
        val loader = FakePhotoLoader(context, null)
        val probe = ViewModelClearedProbe()
        val viewModel = ToolViewModel(Tool.EDIT, "content://x/1", loader, probe, NoSavePhotoSaver())
        // A ViewModelStore is what a navigation entry keeps its ViewModels in; clearing it is
        // what happens when the entry leaves the back stack.
        val store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            /** Hands out the ViewModel built above. */
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return viewModel as T
            }
        }
        val stored = ViewModelProvider(store, factory)[ToolViewModel::class.java]

        compose.runOnUiThread {
            stored.load(100)
            stored.load(100)
            stored.load(200)
        }
        compose.waitForIdle()
        assertEquals(listOf(100, 200), loader.requestedEdges.toList())

        assertFalse(probe.wasCleared(ToolViewModel::class))
        compose.runOnUiThread { store.clear() }
        assertTrue(probe.wasCleared(ToolViewModel::class))
    }

    /**
     * DoD 3: the toolbar holds exactly five toggles, from left to right Edit, Remove background,
     * Enhance, Retouch and Artistic, each named by its content description and showing no text,
     * and no Camera. Edit, the key's tool, is on and the rest off. Tapping each other tool in turn
     * leaves only that one on and makes its name the title, on the same photo, decoded only once.
     */
    @Test
    fun the_toolbar_switches_between_the_five_editing_tools() {
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        show(ToolViewModel(Tool.EDIT, PHOTO_URI, loader, ViewModelClearedProbe(), NoSavePhotoSaver()))
        waitForImage()

        val toggles = compose.onAllNodes(isToggleable() and hasAnyAncestor(hasTestTag(EDITOR_TOOLBAR_TAG)))
        toggles.assertCountEquals(EDITING_TOOLS.size)
        val leftToRight = toggles.fetchSemanticsNodes().sortedBy { it.boundsInRoot.left }
        assertEquals(
            EDITING_TOOLS.map { label(it) },
            leftToRight.map { it.config[SemanticsProperties.ContentDescription].single() },
        )
        assertEquals(EDITING_TOOLS.map { editorToolTag(it) }, leftToRight.map { it.config[SemanticsProperties.TestTag] })
        leftToRight.forEach { node ->
            assertFalse("A toggle shows text: ${node.config}", node.config.contains(SemanticsProperties.Text))
        }
        // The camera takes photos instead of editing one, so it has no toggle: nothing in the
        // toolbar is named "Camera", and no node carries the tag a Camera toggle would have. The
        // label and the tag are written out, because Camera is not a tool any more (R3).
        compose.onAllNodes(
            hasContentDescription(context.getString(R.string.tool_camera)) and
                hasAnyAncestor(hasTestTag(EDITOR_TOOLBAR_TAG)),
        ).assertCountEquals(0)
        compose.onAllNodesWithTag(CAMERA_TOGGLE_TAG).assertCountEquals(0)
        assertOnlyOn(Tool.EDIT)

        listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC).forEach { tool ->
            compose.onNodeWithTag(editorToolTag(tool)).performClick()
            compose.waitForIdle()

            assertOnlyOn(tool)
            // The tool's name is shown once, as the title above the photo area.
            val titles = compose.onAllNodes(hasText(label(tool)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)))
            titles.assertCountEquals(1)
            val title = titles.fetchSemanticsNodes().single().boundsInRoot
            val body = compose.onNodeWithTag(TOOL_BODY_TAG).fetchSemanticsNode().boundsInRoot
            assertTrue("The title $title is not above the photo area $body", title.bottom <= body.top + SLACK)
            compose.onNode(hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, PHOTO_URI))
                .assertIsDisplayed()
        }
        // The first tool's name has left the screen with its selection.
        compose.onAllNodes(hasText(label(Tool.EDIT)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)))
            .assertCountEquals(0)
        // Switching tools never asks for the photo again.
        assertEquals(listOf(2 * 891 * 3), loader.requestedEdges.toList())
    }

    /**
     * DoD 4, layout: from top to bottom the photo area, the panel and the toolbar, none
     * overlapping the next, the toolbar inside the window and the photo inside its area.
     */
    @Test
    fun the_photo_the_panel_and_the_toolbar_stack_from_top_to_bottom() {
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        show(ToolViewModel(Tool.EDIT, PHOTO_URI, loader, ViewModelClearedProbe(), NoSavePhotoSaver()))
        waitForImage()

        val image = compose.onNodeWithTag(TOOL_IMAGE_TAG).fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithTag(TOOL_BODY_TAG).fetchSemanticsNode().boundsInRoot
        val panel = compose.onNodeWithTag(EDITOR_PANEL_TAG).fetchSemanticsNode().boundsInRoot
        val toolbar = compose.onNodeWithTag(EDITOR_TOOLBAR_TAG).fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

        assertTrue("The panel $panel is empty", panel.height > 0f && panel.width > 0f)
        assertTrue("The photo area $body runs into the panel $panel", body.bottom <= panel.top + SLACK)
        assertTrue("The panel $panel runs into the toolbar $toolbar", panel.bottom <= toolbar.top + SLACK)
        assertInside(toolbar, root)
        assertInside(image, body)
    }

    /**
     * DoD 4, panel: each of the five tools shows its own message in the panel once selected, a
     * message that ends "coming in a later run.", is no tool's name, and differs from the others;
     * the other tools' messages are not on screen meanwhile.
     */
    @Test
    fun each_tool_shows_its_own_panel_message() {
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        show(ToolViewModel(Tool.EDIT, PHOTO_URI, loader, ViewModelClearedProbe(), NoSavePhotoSaver()))
        waitForImage()
        val labels = Tool.entries.map { label(it) }.toSet()

        val messages = EDITING_TOOLS.map { tool ->
            compose.onNodeWithTag(editorToolTag(tool)).performClick()
            compose.waitForIdle()

            val message = context.getString(PANEL_MESSAGES.getValue(tool))
            compose.onNode(hasText(message) and hasAnyAncestor(hasTestTag(EDITOR_PANEL_TAG))).assertIsDisplayed()
            EDITING_TOOLS.filter { it != tool }.forEach { other ->
                compose.onAllNodes(hasText(context.getString(PANEL_MESSAGES.getValue(other)))).assertCountEquals(0)
            }
            assertTrue("\"$message\" does not end as promised", message.endsWith("coming in a later run."))
            assertFalse("\"$message\" is a tool's name", message in labels)
            message
        }
        assertEquals("Two tools share a panel message: $messages", EDITING_TOOLS.size, messages.toSet().size)
    }

    /**
     * At the phone size of this class (411 x 891 dp, xxhdpi), Back, Save and each of the five
     * toggles are at least 48 dp wide and tall, the touch target size Android recommends.
     */
    @Test
    fun back_save_and_every_toggle_are_at_least_48_dp() {
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        show(ToolViewModel(Tool.EDIT, PHOTO_URI, loader, ViewModelClearedProbe(), NoSavePhotoSaver()))
        waitForImage()
        val minimum = with(compose.density) { 48.dp.toPx() }

        val controls: List<Pair<String, SemanticsNodeInteraction>> =
            listOf(
                "Back" to compose.onNodeWithContentDescription(context.getString(R.string.back)),
                "Save" to compose.onNodeWithTag(TOOL_SAVE_TAG),
            ) + EDITING_TOOLS.map { tool -> label(tool) to compose.onNodeWithTag(editorToolTag(tool)) }

        controls.forEach { (name, node) ->
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue(
                "$name is ${bounds.width} x ${bounds.height} px, under 48 dp ($minimum px)",
                bounds.width >= minimum - SLACK && bounds.height >= minimum - SLACK,
            )
        }
    }

    /**
     * DoD 7: while a save runs, the five toggles are switched off, so the tool the person saved
     * with stays the selected one (the Saved screen's "Edit another" opens that tool's gallery).
     * They stay in the accessibility tree, each still a toggle named by its tool and at least
     * 48 dp, and a tap on one changes nothing. Once the save is done they work again.
     *
     * The test's clock is paused before the screen is shown and moved by hand, because Save shows
     * a moving progress indicator while it saves, and waiting for a still screen could then last
     * forever.
     */
    @Test
    fun the_toggles_are_switched_off_while_a_save_runs() {
        // The gate holds the save open until the test completes it.
        val gate = CompletableDeferred<Unit>()
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        val viewModel = ToolViewModel(Tool.RETOUCH, PHOTO_URI, loader, ViewModelClearedProbe(), FakePhotoSaver(gate = gate))
        compose.mainClock.autoAdvance = false
        show(viewModel)
        advanceUntil { compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isNotEmpty() }
        EDITING_TOOLS.forEach { tool -> compose.onNodeWithTag(editorToolTag(tool)).assertIsEnabled() }

        compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
        val savingText = context.getString(R.string.tool_saving)
        advanceUntil {
            compose.onAllNodes(hasText(savingText), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        val minimum = with(compose.density) { 48.dp.toPx() }
        EDITING_TOOLS.forEach { tool ->
            val toggle = compose.onNodeWithTag(editorToolTag(tool))
                .assertIsNotEnabled()
                .assert(isToggleable())
                .assert(hasContentDescription(label(tool)))
            val bounds = toggle.fetchSemanticsNode().boundsInRoot
            assertTrue(
                "${label(tool)} is ${bounds.width} x ${bounds.height} px while saving, under 48 dp ($minimum px)",
                bounds.width >= minimum - SLACK && bounds.height >= minimum - SLACK,
            )
        }

        // A tap on another tool during the save changes nothing.
        compose.onNodeWithTag(editorToolTag(Tool.ENHANCE)).performClick()
        advanceBy(500)
        assertOnlyOn(Tool.RETOUCH)
        assertEquals(Tool.RETOUCH, viewModel.state.value.tool)
        compose.onAllNodes(hasText(label(Tool.RETOUCH)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)))
            .assertCountEquals(1)

        // The save ends: the toggles are back, and a tap switches the tool again.
        gate.complete(Unit)
        advanceUntil { viewModel.state.value.save is ToolUiState.SaveState.Saved }
        advanceBy(500)
        EDITING_TOOLS.forEach { tool -> compose.onNodeWithTag(editorToolTag(tool)).assertIsEnabled() }
        compose.onNodeWithTag(editorToolTag(Tool.ENHANCE)).performClick()
        advanceBy(500)
        assertOnlyOn(Tool.ENHANCE)
    }

    /**
     * DoD 3, through the whole app rather than a hosted screen. A fresh install with photo access
     * granted, the photo library and the photo decoder swapped for fakes, then Home, the Edit
     * entry and photo 1, so the editor is on top. Its toolbar holds exactly five toggles, from
     * left to right Edit, Remove background, Enhance, Retouch and Artistic, each named by its
     * content description and showing no text, Edit on and the other four off, and no Camera
     * toggle. Tapping each of the other four in turn leaves only that one on and makes its name
     * the title, while the same one editor stays on screen over the same photo, which is decoded
     * once in all. One Back then returns to Edit's gallery, and a second one to Home.
     *
     * A test cannot read the app's back stack, so, like the walks in `SavedNavigationTest`, its
     * size is read from what is on screen (one editor and no gallery after every switch) and from
     * where Back leads: the stack still holds Home, the gallery and the editor, three keys, as it
     * did when the editor opened.
     *
     * The other tests of this class draw the screen inside the empty activity the rule starts.
     * This one starts the app's own activity on top of it; the rule still finds the app's nodes,
     * because it looks at every Compose screen of the test, and the empty activity draws none.
     */
    @Test
    fun the_toolbar_switches_the_five_tools_in_place_without_reloading_the_photo() {
        val app: Application = ApplicationProvider.getApplicationContext()
        // Photo access granted, as after a person answered "Allow" on a fresh install.
        shadowOf(app).grantPermissions(*photoPermissionsFor(35).toTypedArray())
        val photos = GalleryGridTest.manyPhotos(3)
        // Photo 1 is the newest, so it is the first cell of the gallery.
        val photo = photos.first()
        val source = FakePhotoSource(albums = listOf(Album(1L, "A")), allPhotos = photos)
        val loader = FakePhotoLoader(app, ToolFixtures.gradient(60, 40))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Koin lets a later module replace an earlier recipe with the same type, so the
            // gallery and the editor, both built after this line, get the fakes.
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
            assertEquals(1, count(TOOL_SCREEN_TAG))
            assertEquals("The photo was not decoded exactly once: ${loader.requestedEdges}", 1, loader.requestedEdges.size)

            val toggles = compose.onAllNodes(isToggleable() and hasAnyAncestor(hasTestTag(EDITOR_TOOLBAR_TAG)))
            toggles.assertCountEquals(EDITING_TOOLS.size)
            // The toolbar does not scroll, so boundsInRoot is where each toggle really is.
            val leftToRight = toggles.fetchSemanticsNodes().sortedBy { it.boundsInRoot.left }
            assertEquals(
                EDITING_TOOLS.map { label(it) },
                leftToRight.map { it.config[SemanticsProperties.ContentDescription].single() },
            )
            assertEquals(EDITING_TOOLS.map { editorToolTag(it) }, leftToRight.map { it.config[SemanticsProperties.TestTag] })
            leftToRight.forEach { node ->
                assertFalse("A toggle shows text: ${node.config}", node.config.contains(SemanticsProperties.Text))
            }
            // Camera takes photos instead of editing one: no toggle is named after it or tagged for it.
            compose.onAllNodes(
                hasContentDescription(label(Tool.CAMERA)) and hasAnyAncestor(hasTestTag(EDITOR_TOOLBAR_TAG)),
            ).assertCountEquals(0)
            compose.onAllNodesWithTag(CAMERA_TOGGLE_TAG).assertCountEquals(0)
            // The tags were checked in order above, so this is "the first on, the other four off".
            assertOnlyOn(Tool.EDIT)
            assertEquals(1, titleCount(Tool.EDIT))

            EDITING_TOOLS.drop(1).forEach { tool ->
                compose.onNodeWithTag(editorToolTag(tool)).performClick()
                waitFor { titleCount(tool) == 1 }

                assertOnlyOn(tool)
                // The title names the tool just tapped, and no other tool's name is shown as text.
                EDITING_TOOLS.filter { it != tool }.forEach { other -> assertEquals(0, titleCount(other)) }
                compose.onAllNodes(shownPhoto).assertCountEquals(1)
                // Still the one editor over no gallery: the switch opened no screen and closed none.
                assertEquals(1, count(TOOL_SCREEN_TAG))
                assertEquals(0, count(GALLERY_SCREEN_TAG))
            }
            assertEquals("The photo was decoded again: ${loader.requestedEdges}", 1, loader.requestedEdges.size)

            // The back stack is still [Home, Gallery(EDIT), ToolPhoto(EDIT, photo 1)].
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            val galleryTitle = context.getString(R.string.gallery_title, label(Tool.EDIT))
            waitFor { count(TOOL_SCREEN_TAG) == 0 && compose.onAllNodesWithText(galleryTitle).fetchSemanticsNodes().isNotEmpty() }
            assertEquals(1, count(GALLERY_SCREEN_TAG))

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(GALLERY_SCREEN_TAG) == 0 }
            assertEquals(0, count(TOOL_SCREEN_TAG))
            compose.onNodeWithText(label(Tool.CAMERA)).assertExists()
        }
    }

    /**
     * The editor's bottom controls, the panel and the floating toolbar with its five toggles,
     * stay above the bottom gesture zone even when the navigation bar is 0 dp tall (POLICIES.md,
     * the bottom-edge rule).
     *
     * The bottom gesture zone is the strip where a swipe up means "go home"; a tap inside it can
     * bring up the phone's own navigation instead of pressing a toggle. With swipe gestures on and
     * the gesture hint off (as on the owner's Galaxy A13), the navigation bar's inset is 0 while
     * the zone is still there, so controls padded by the navigation bar alone would keep only the
     * toolbar's own 16 dp margin and end inside a zone [GESTURE_ZONE] tall.
     *
     * Robolectric draws no system bars, so the test hands the view the screen is drawn in the
     * window insets such a phone would: the gesture zone at the bottom, and every other inset, the
     * navigation bar's included, 0. It also reads both insets back as the screen sees them: if
     * they never arrived (a change in Robolectric or Compose), the test fails, because without
     * the zone it would prove nothing.
     */
    @Test
    fun the_toolbar_and_the_panel_clear_the_bottom_gesture_zone_when_the_navigation_bar_is_zero_tall() {
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        // Built here, before the screen is drawn: lint forbids building a ViewModel while drawing.
        val viewModel = ToolViewModel(Tool.EDIT, PHOTO_URI, loader, ViewModelClearedProbe(), NoSavePhotoSaver())
        var composeView: View? = null
        var seenZonePx = -1
        var seenNavigationBarPx = -1
        compose.setContent {
            // LocalView is the Android view the screen is drawn in, the one that receives the
            // window insets; both insets are read here just as the screen reads them.
            val view = LocalView.current
            val localDensity = LocalDensity.current
            val zone = WindowInsets.systemGestures.getBottom(localDensity)
            val navigationBar = WindowInsets.navigationBars.getBottom(localDensity)
            // SideEffect hands the values out after each composition that succeeds.
            SideEffect {
                composeView = view
                seenZonePx = zone
                seenNavigationBarPx = navigationBar
            }
            MaterialTheme {
                ToolScreen(viewModel = viewModel, onBack = {}, onSaved = {})
            }
        }
        compose.waitForIdle()
        waitForImage()

        val zonePx = with(compose.density) { GESTURE_ZONE.roundToPx() }
        // Only the gesture zone is set, so every other inset, the navigation bar's included, is 0.
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemGestures(), Insets.of(0, 0, 0, zonePx))
            .build()
        compose.runOnIdle { ViewCompat.dispatchApplyWindowInsets(checkNotNull(composeView), insets) }
        compose.waitForIdle()
        assertEquals("The screen did not see the gesture zone the test sent", zonePx, seenZonePx)
        assertEquals("The screen saw a navigation bar the test did not send", 0, seenNavigationBarPx)

        // The zone's top edge, counted from the top of the screen's root.
        val zoneTop = (compose.onRoot().fetchSemanticsNode().size.height - zonePx).toFloat()
        val controls = listOf(EDITOR_PANEL_TAG, EDITOR_TOOLBAR_TAG) + EDITING_TOOLS.map { editorToolTag(it) }
        controls.forEach { tag ->
            val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
            // positionInRoot plus the size is where the node is laid out, never clipped to the
            // screen as boundsInRoot is, so a control pushed off the bottom cannot look inside.
            val top = node.positionInRoot.y
            val bottom = top + node.size.height
            assertTrue("$tag starts at $top px, above the top of the screen", top >= 0f)
            assertTrue(
                "$tag ends at $bottom px, inside the gesture zone that starts at $zoneTop px",
                bottom <= zoneTop,
            )
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * With the test's clock paused, moves it forward by [millis] and runs whatever the main
     * thread was asked to do meanwhile.
     */
    private fun advanceBy(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * With the test's clock paused, moves it forward 100 ms at a time until [condition] holds.
     * Gives up after about ten seconds of the test's clock.
     */
    private fun advanceUntil(condition: () -> Boolean) {
        repeat(100) {
            if (condition()) return
            advanceBy(100)
        }
        if (!condition()) error("The screen never reached the expected state")
    }

    /** The label of [tool] as the app shows it, such as "Retouch". */
    private fun label(tool: Tool): String = context.getString(tool.labelRes)

    /** Checks that [selected]'s toggle is on and every other editing tool's toggle is off. */
    private fun assertOnlyOn(selected: Tool) {
        EDITING_TOOLS.forEach { tool ->
            val toggle = compose.onNodeWithTag(editorToolTag(tool))
            if (tool == selected) toggle.assertIsOn() else toggle.assertIsOff()
        }
    }

    /** Shows the tool screen for [viewModel] (built by the caller, outside any composable). */
    private fun show(viewModel: ToolViewModel, onBack: () -> Unit = {}) {
        compose.setContent {
            MaterialTheme {
                ToolScreen(viewModel = viewModel, onBack = onBack, onSaved = {})
            }
        }
        compose.waitForIdle()
    }

    /** Waits until the photo is on screen. */
    private fun waitForImage() {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * For the whole-app walk: waits up to ten seconds of real time for [condition], letting the
     * main thread run in between. The app's loads finish on the main thread, and under Robolectric
     * the test runs on that thread itself, so its queue only moves when the test runs it.
     */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Counts the nodes carrying [tag] on every Compose screen of the test. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** Counts the texts on the editor that read [tool]'s name: the title, when it is the selected tool. */
    private fun titleCount(tool: Tool): Int =
        compose.onAllNodes(hasText(label(tool)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)))
            .fetchSemanticsNodes().size

    /**
     * Taps Home's Edit entry, the same way `SavedNavigationTest` does. Home's tool cards are
     * addressed by the tag `home-tool-<TOOL_NAME>`, because the "Edit" group header and the Edit
     * card read the same. While Home still lists its tools as plain rows without that tag, the
     * entry is the one clickable node reading "Edit". Home scrolls, so the entry is scrolled into
     * view before the tap.
     */
    private fun tapHomeEditEntry() {
        val byText = hasText(label(Tool.EDIT)) and hasClickAction()
        waitFor { count(HOME_EDIT_CARD_TAG) > 0 || compose.onAllNodes(byText).fetchSemanticsNodes().isNotEmpty() }
        val entry = if (count(HOME_EDIT_CARD_TAG) > 0) hasTestTag(HOME_EDIT_CARD_TAG) else byText
        compose.onNode(entry).performScrollTo().performClick()
    }

    /** Checks that [inner] lies within [outer], allowing half a pixel for rounding. */
    private fun assertInside(inner: Rect, outer: Rect) {
        val slack = 0.5f
        assertTrue(
            "$inner is not inside $outer",
            inner.left >= outer.left - slack &&
                inner.top >= outer.top - slack &&
                inner.right <= outer.right + slack &&
                inner.bottom <= outer.bottom + slack,
        )
    }

    /** Values the editor's tests share. */
    private companion object {
        /** The photo the fake loader stands in for; nothing is read from it. */
        const val PHOTO_URI = "content://media/external/images/media/7"

        /** Half a pixel, allowed for rounding when two bounds are compared. */
        const val SLACK = 0.5f

        /** The tag a Camera toggle would carry (`editorToolTag` of the old Camera tool); none may exist. */
        const val CAMERA_TOGGLE_TAG = "editor-tool-CAMERA"

        /** The tag of Home's Edit card (`home-tool-<TOOL_NAME>`). */
        const val HOME_EDIT_CARD_TAG = "home-tool-EDIT"

        /**
         * The height of the bottom gesture zone the gesture test hands the screen: more than the
         * toolbar's own 16 dp bottom margin, so a toolbar padded by the navigation bar alone (0 dp
         * in that test) would end inside it.
         */
        val GESTURE_ZONE = 40.dp

        /** The five editing tools in the order the toolbar must show them, left to right. */
        val EDITING_TOOLS = listOf(Tool.EDIT, Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC)

        /** The panel message each editing tool must show. */
        val PANEL_MESSAGES = mapOf(
            Tool.EDIT to R.string.editor_panel_edit,
            Tool.REMOVE_BACKGROUND to R.string.editor_panel_remove_background,
            Tool.ENHANCE to R.string.editor_panel_enhance,
            Tool.RETOUCH to R.string.editor_panel_retouch,
            Tool.ARTISTIC to R.string.editor_panel_artistic,
        )
    }
}
