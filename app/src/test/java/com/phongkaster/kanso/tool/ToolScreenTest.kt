package com.phongkaster.kanso.tool

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.EditFailure
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.tool.ARTISTIC_PANEL_TAG
import com.phongkaster.kanso.ui.tool.CUTOUT_PANEL_TAG
import com.phongkaster.kanso.ui.tool.EDITOR_PANEL_TAG
import com.phongkaster.kanso.ui.tool.EDIT_PANEL_TAG
import com.phongkaster.kanso.ui.tool.ENHANCE_PANEL_TAG
import com.phongkaster.kanso.ui.tool.RETOUCH_PANEL_TAG
import com.phongkaster.kanso.ui.tool.TOOL_BACK_TAG
import com.phongkaster.kanso.ui.tool.TOOL_BODY_TAG
import com.phongkaster.kanso.ui.tool.TOOL_CARD_TAG
import com.phongkaster.kanso.ui.tool.TOOL_FAILURE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_PROGRESS_TAG
import com.phongkaster.kanso.ui.tool.TOOL_REPLACE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SAVE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_UNDO_TAG
import com.phongkaster.kanso.ui.tool.TOOL_UNREADABLE_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolPanel
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.ToolViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks the tool screen on Android 15 (DoD 8): a photo tagged "turned" is drawn upright and
 * fitted inside the photo card, the image node names the photo's Uri, an unreadable photo gives a
 * plain message with Save switched off, and Back calls back.
 *
 * Run 7, DoD 10: each of the five tools has a screen of its own, with no way to change tools on
 * it. For each tool, with the 600 x 400 gradient photo: a round Back button at the top left; the
 * tool's name centred at the top, the only text on the screen equal to a tool's name; a full-width
 * rounded card holding the photo, which fills the card in one direction with its own shape; a
 * "Replace" chip inside the card near its bottom; the tool's own options strip under the card;
 * one full-width Save button under that; no toggle anywhere; the photo decoded exactly once.
 * Another test hands the screen the insets of a phone with a 0 dp navigation bar and a bottom
 * gesture zone, and checks that Save stays above the zone.
 *
 * Run 8: each tool's options strip holds that tool's own panel, and no text promises a tool "in
 * a later run"; Undo works only when there is an edit to take back; while an edit runs a progress
 * ring shows and Save is off; and each kind of failed edit shows its plain message over the photo.
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
     * taller than wide, lies inside the photo card, and carries the photo's Uri.
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

    /** The top bar shows the tool's name, and Save is visible and can be tapped once the photo is shown. */
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

    /** A photo that cannot be opened gives the plain message, no image, and Save switched off. */
    @Test
    fun an_unreadable_photo_shows_a_plain_message() {
        show(ToolViewModel(Tool.EDIT, ToolFixtures.MISSING_URI, PhotoLoader(context), ViewModelClearedProbe(), NoSavePhotoSaver()))
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(hasTestTag(TOOL_UNREADABLE_TAG)).fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag(TOOL_UNREADABLE_TAG).assertIsDisplayed()
        compose.onNode(hasText(context.getString(R.string.tool_photo_unreadable))).assertIsDisplayed()
        assertTrue(compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsDisplayed().assertIsNotEnabled()
    }

    /** The Back button calls onBack. */
    @Test
    fun the_back_arrow_calls_on_back() {
        var backCalls = 0
        val loader = FakePhotoLoader(context, null)
        show(ToolViewModel(Tool.EDIT, "content://x/1", loader, ViewModelClearedProbe(), NoSavePhotoSaver()), onBack = { backCalls++ })

        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        compose.waitForIdle()
        assertEquals(1, backCalls)
    }

    /** The "Replace" chip calls onReplace, once per tap. */
    @Test
    fun the_replace_chip_calls_on_replace() {
        var replaceCalls = 0
        val loader = FakePhotoLoader(context, ToolFixtures.gradient(600, 400))
        show(
            ToolViewModel(Tool.ENHANCE, PHOTO_URI, loader, ViewModelClearedProbe(), NoSavePhotoSaver()),
            onReplace = { replaceCalls++ },
        )
        waitForImage()

        compose.onNodeWithTag(TOOL_REPLACE_TAG).assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals(1, replaceCalls)
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
     * DoD 10, once per tool: each tool's own screen, opened on the 600 x 400 gradient photo, has
     * the round Back button at the top left, the tool's name centred and alone among the tools'
     * names, the full-width rounded card holding the fitted photo, the Replace chip near the
     * card's bottom, the tool's own options strip under the card and the full-width Save button
     * under that. Nothing on it is a toggle, its state's tool stays the key's tool, and its photo
     * is decoded exactly once.
     *
     * One screen is drawn at a time; the test hands it each tool's ViewModel in turn, as opening
     * each tool's screen from its own key would.
     */
    @Test
    fun each_tool_has_its_own_screen_with_back_its_name_the_photo_card_replace_its_strip_and_save() {
        var backCalls = 0
        val dp24 = with(compose.density) { 24.dp.toPx() }
        val dp48 = with(compose.density) { 48.dp.toPx() }
        showEachTool(onBack = { backCalls++ }) { index, tool, viewModel, loader ->
            val root = compose.onRoot().fetchSemanticsNode().boundsInRoot

            // Back: round, named "Back", near the left edge, and it calls back.
            val back = compose.onNodeWithTag(TOOL_BACK_TAG)
                .assert(hasContentDescription(context.getString(R.string.back)))
                .fetchSemanticsNode().boundsInRoot
            assertTrue("$tool: Back $back is not at the top left", back.left - root.left <= dp24)
            assertTrue("$tool: Back $back is under 48 dp", back.width >= dp48 - SLACK && back.height >= dp48 - SLACK)
            assertEquals("$tool: Back is not round", back.width, back.height, 1f)
            compose.onNodeWithTag(TOOL_BACK_TAG).performClick()
            compose.waitForIdle()
            assertEquals(index + 1, backCalls)

            // The name: this tool's alone, once, centred.
            Tool.entries.forEach { other ->
                compose.onAllNodes(hasText(label(other)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)))
                    .assertCountEquals(if (other == tool) 1 else 0)
            }
            val title = compose.onNode(hasText(label(tool)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)))
                .fetchSemanticsNode().boundsInRoot
            assertEquals("$tool: the name $title is not centred", root.center.x, title.center.x, 2f)
            assertTrue("$tool: the name $title is not above the card", title.bottom <= cardBounds().top + SLACK)

            // The card: full width, holding the photo, which fills it one way with its own shape.
            val card = cardBounds()
            assertTrue("$tool: the card $card is not full width", card.left - root.left <= dp24 && root.right - card.right <= dp24)
            val image = compose.onNodeWithTag(TOOL_IMAGE_TAG).fetchSemanticsNode().boundsInRoot
            assertInside(image, card)
            assertTrue(
                "$tool: the photo $image does not fill the card $card on either side",
                kotlin.math.abs(image.width - card.width) <= 1f || kotlin.math.abs(image.height - card.height) <= 1f,
            )
            assertEquals("$tool: the photo's shape changed", 1.5f, image.width / image.height, 0.015f)
            compose.onNode(hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, PHOTO_URI))
                .assertIsDisplayed()

            // Replace: inside the card, its bottom near the card's bottom.
            val replace = compose.onNodeWithTag(TOOL_REPLACE_TAG)
                .assert(hasText(context.getString(R.string.tool_screen_replace)))
                .fetchSemanticsNode().boundsInRoot
            assertInside(replace, card)
            assertTrue("$tool: Replace $replace is not near the card's bottom ${card.bottom}", card.bottom - replace.bottom <= dp24)

            // The options strip: under the card, holding this tool's own panel.
            val panel = compose.onNodeWithTag(EDITOR_PANEL_TAG).fetchSemanticsNode().boundsInRoot
            // An empty slot is placed nowhere and measures nothing, so the position is only
            // checked once a tool's own controls fill the strip.
            assertTrue("$tool: the strip $panel is not under the card $card", panel.isEmpty || panel.top >= card.bottom - SLACK)
            // The panel may still be an empty slot (zero tall), so it is checked to exist in the
            // strip rather than to be displayed.
            compose.onNode(hasTestTag(PANEL_TAGS.getValue(tool)) and hasAnyAncestor(hasTestTag(EDITOR_PANEL_TAG)))
                .assertExists()

            // Save: full width, under the strip.
            val save = compose.onNodeWithTag(TOOL_SAVE_TAG)
                .assert(hasText(context.getString(R.string.tool_save)))
                .assertIsEnabled()
                .fetchSemanticsNode().boundsInRoot
            assertTrue("$tool: Save $save is not full width", save.left - root.left <= dp24 && root.right - save.right <= dp24)
            assertTrue("$tool: Save $save is not under the strip $panel", panel.isEmpty || save.top >= panel.bottom - SLACK)

            // No way to change tools: nothing is a toggle, and the tool is still the key's.
            compose.onAllNodes(isToggleable()).assertCountEquals(0)
            assertEquals(tool, viewModel.state.value.tool)
            assertEquals("$tool: the photo was not decoded exactly once", 1, loader.requestedEdges.size)
        }
    }

    /**
     * Run 8: each of the five tools' screens (the stateless content, with the tool's own panel in
     * the slot) shows that tool's panel inside the options strip and no other tool's, and no text
     * on it still promises the tool is "coming in a later run".
     */
    @Test
    fun each_tool_shows_its_own_panel_and_no_later_run_message() {
        val photo = ToolFixtures.gradient(600, 400)
        var state by mutableStateOf(readyState(Tool.entries.first(), photo))
        compose.setContent {
            MaterialTheme {
                ToolContent(state = state, onBack = {}, onSave = {}, panel = { ToolPanel(state.tool) })
            }
        }
        compose.waitForIdle()
        Tool.entries.forEach { tool ->
            compose.runOnUiThread { state = readyState(tool, photo) }
            compose.waitForIdle()

            compose.onNode(hasTestTag(PANEL_TAGS.getValue(tool)) and hasAnyAncestor(hasTestTag(EDITOR_PANEL_TAG)))
                .assertExists()
            Tool.entries.filter { it != tool }.forEach { other ->
                compose.onAllNodes(hasTestTag(PANEL_TAGS.getValue(other))).assertCountEquals(0)
            }
            compose.onAllNodes(hasText("coming in a later run", substring = true, ignoreCase = true))
                .assertCountEquals(0)
        }
    }

    /**
     * Undo (`tool-undo`) is switched off while there is nothing to take back, and switched on,
     * calling onUndo once per tap, once there is.
     */
    @Test
    fun undo_is_enabled_only_when_there_is_an_edit_to_take_back() {
        val photo = ToolFixtures.gradient(600, 400)
        var undoCalls = 0
        var state by mutableStateOf(readyState(Tool.EDIT, photo))
        compose.setContent {
            MaterialTheme {
                ToolContent(state = state, onBack = {}, onSave = {}, onUndo = { undoCalls++ })
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(TOOL_UNDO_TAG).assertIsDisplayed().assertIsNotEnabled()

        compose.runOnUiThread { state = state.copy(edited = ToolFixtures.gradient(300, 200), canUndo = true) }
        compose.waitForIdle()
        compose.onNodeWithTag(TOOL_UNDO_TAG).assertIsEnabled().performClick()
        compose.waitForIdle()
        assertEquals(1, undoCalls)
    }

    /**
     * While an edit runs, the progress ring (`tool-progress`) is in the photo card at the value
     * the state reports, and Save and Undo are switched off. The clock is held still from the
     * start, as the Constraints ask of any test that shows a progress indicator.
     */
    @Test
    fun while_an_edit_runs_save_is_off_and_the_progress_ring_is_shown() {
        val state = readyState(Tool.REMOVE_BACKGROUND, ToolFixtures.gradient(600, 400)).copy(
            canUndo = true,
            processing = ToolUiState.Processing.Running(0.4f),
        )
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                ToolContent(state = state, onBack = {}, onSave = {}, panel = { ToolPanel(state.tool) })
            }
        }
        compose.mainClock.advanceTimeBy(500)

        compose.onNode(hasTestTag(TOOL_PROGRESS_TAG) and hasAnyAncestor(hasTestTag(TOOL_BODY_TAG)))
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher("shows progress 0.4") { node ->
                    val current = node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.current
                    current != null && kotlin.math.abs(current - 0.4f) < 0.01f
                },
            )
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag(TOOL_UNDO_TAG).assertIsNotEnabled()
        // The photo stays on screen under the ring.
        compose.onNodeWithTag(TOOL_IMAGE_TAG).assertIsDisplayed()
    }

    /**
     * Each kind of failed edit shows its own plain message under `tool-failure`, the photo stays
     * on screen under it, no progress ring is left, and a tap on the message calls
     * onDismissFailure.
     */
    @Test
    fun each_failure_shows_its_plain_message_over_the_unchanged_photo() {
        val photo = ToolFixtures.gradient(600, 400)
        var dismissCalls = 0
        var state by mutableStateOf(readyState(Tool.RETOUCH, photo))
        compose.setContent {
            MaterialTheme {
                ToolContent(state = state, onBack = {}, onSave = {}, onDismissFailure = { dismissCalls++ })
            }
        }
        compose.waitForIdle()
        compose.onAllNodes(hasTestTag(TOOL_FAILURE_TAG)).assertCountEquals(0)

        val messages = mutableSetOf<String>()
        FAILURE_MESSAGES.entries.forEachIndexed { index, (failure, messageRes) ->
            compose.runOnUiThread { state = readyState(Tool.RETOUCH, photo).copy(failure = failure) }
            compose.waitForIdle()
            val message = context.getString(messageRes)
            assertTrue("$failure: the message is empty", message.isNotBlank())
            compose.onNodeWithTag(TOOL_FAILURE_TAG).assertIsDisplayed().assert(hasText(message))
            compose.onNode(hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, PHOTO_URI))
                .assertIsDisplayed()
            compose.onAllNodes(hasTestTag(TOOL_PROGRESS_TAG)).assertCountEquals(0)
            compose.onNodeWithTag(TOOL_FAILURE_TAG).performClick()
            compose.waitForIdle()
            assertEquals("$failure: the tap did not dismiss", index + 1, dismissCalls)
            messages += message
        }
        assertEquals("Two failures share a message: $messages", FAILURE_MESSAGES.size, messages.size)
    }

    /**
     * Save stays above the bottom gesture zone even when the navigation bar is 0 dp tall
     * (POLICIES.md, the bottom-edge rule), and so does the options strip above it.
     *
     * The bottom gesture zone is the strip where a swipe up means "go home"; a tap inside it can
     * bring up the phone's own navigation instead of pressing Save. With swipe gestures on and the
     * gesture hint off (as on the owner's Galaxy A13), the navigation bar's inset is 0 while the
     * zone is still there, so a button padded by the navigation bar alone would end inside a zone
     * [GESTURE_ZONE] tall.
     *
     * Robolectric draws no system bars, so the test hands the view the screen is drawn in the
     * window insets such a phone would: the gesture zone at the bottom, and every other inset, the
     * navigation bar's included, 0. It also reads both insets back as the screen sees them: if
     * they never arrived (a change in Robolectric or Compose), the test fails, because without
     * the zone it would prove nothing.
     */
    @Test
    fun save_clears_the_bottom_gesture_zone_when_the_navigation_bar_is_zero_tall() {
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
        listOf(EDITOR_PANEL_TAG, TOOL_SAVE_TAG).forEach { tag ->
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

    /** The label of [tool] as the app shows it, such as "Retouch". */
    private fun label(tool: Tool): String = context.getString(tool.labelRes)

    /**
     * Shows each tool's own screen in turn, on the 600 x 400 gradient photo, and runs [check] on
     * each once its photo is shown and its title names it, with the tool's position in
     * `Tool.entries`, the tool, its ViewModel and its loader. Back calls [onBack].
     *
     * One screen is drawn at a time: the test hands it each tool's ViewModel in turn, as opening
     * each tool's screen from its own key would. The ViewModels are built here, before the screen
     * is drawn, because lint forbids building a ViewModel while drawing.
     */
    private fun showEachTool(
        onBack: () -> Unit = {},
        check: (index: Int, tool: Tool, viewModel: ToolViewModel, loader: FakePhotoLoader) -> Unit,
    ) {
        val loaders = Tool.entries.associateWith { FakePhotoLoader(context, ToolFixtures.gradient(600, 400)) }
        val viewModels = Tool.entries.associateWith { tool ->
            ToolViewModel(tool, PHOTO_URI, loaders.getValue(tool), ViewModelClearedProbe(), NoSavePhotoSaver())
        }
        var shown by mutableStateOf(viewModels.getValue(Tool.entries.first()))
        compose.setContent {
            MaterialTheme {
                ToolScreen(viewModel = shown, onBack = onBack, onSaved = {})
            }
        }
        compose.waitForIdle()
        Tool.entries.forEachIndexed { index, tool ->
            compose.runOnUiThread { shown = viewModels.getValue(tool) }
            compose.waitForIdle()
            val title = hasText(label(tool)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG))
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodes(title).fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isNotEmpty()
            }
            compose.waitForIdle()
            check(index, tool, viewModels.getValue(tool), loaders.getValue(tool))
        }
    }

    /** A snapshot of [tool]'s screen with [photo] decoded and shown, nothing edited or saved. */
    private fun readyState(tool: Tool, photo: Bitmap): ToolUiState =
        ToolUiState(tool = tool, photoUri = PHOTO_URI, photo = ToolUiState.PhotoState.Ready(photo))

    /** Where the photo card is, in the root's pixels. */
    private fun cardBounds(): Rect = compose.onNodeWithTag(TOOL_CARD_TAG).fetchSemanticsNode().boundsInRoot

    /** Shows the tool screen for [viewModel] (built by the caller, outside any composable). */
    private fun show(viewModel: ToolViewModel, onBack: () -> Unit = {}, onReplace: () -> Unit = {}) {
        compose.setContent {
            MaterialTheme {
                ToolScreen(viewModel = viewModel, onBack = onBack, onSaved = {}, onReplace = onReplace)
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

    /** Values the tool screen's tests share. */
    private companion object {
        /** The photo the fake loader stands in for; nothing is read from it. */
        const val PHOTO_URI = "content://media/external/images/media/7"

        /** Half a pixel, allowed for rounding when two bounds are compared. */
        const val SLACK = 0.5f

        /**
         * The height of the bottom gesture zone the gesture test hands the screen: more than
         * Save's own 12 dp bottom margin, so a button padded by the navigation bar alone (0 dp in
         * that test) would end inside it.
         */
        val GESTURE_ZONE = 40.dp

        /** The test tag of each tool's own panel, drawn in the options strip. */
        val PANEL_TAGS = mapOf(
            Tool.EDIT to EDIT_PANEL_TAG,
            Tool.REMOVE_BACKGROUND to CUTOUT_PANEL_TAG,
            Tool.ENHANCE to ENHANCE_PANEL_TAG,
            Tool.RETOUCH to RETOUCH_PANEL_TAG,
            Tool.ARTISTIC to ARTISTIC_PANEL_TAG,
        )

        /** The plain message each kind of failed edit must show. */
        val FAILURE_MESSAGES = mapOf(
            EditFailure.OutOfMemory to R.string.tool_failed_out_of_memory,
            EditFailure.NoSubject to R.string.tool_failed_no_subject,
            EditFailure.NoFace to R.string.tool_failed_no_face,
            EditFailure.ModelUnavailable to R.string.tool_failed_model_unavailable,
            EditFailure.Other to R.string.tool_failed_other,
        )
    }
}
