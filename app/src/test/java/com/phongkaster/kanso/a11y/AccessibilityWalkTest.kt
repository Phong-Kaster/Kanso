package com.phongkaster.kanso.a11y

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import com.phongkaster.kanso.R
import com.phongkaster.kanso.camera.FakeCameraController
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.settings.ThemeColour
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.gallery.RecordingRegistry
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.screenshots.FAKE_SCHEME
import com.phongkaster.kanso.screenshots.SolidColourFetcherFactory
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CAMERA_REFUSED_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SETTINGS_BUTTON_TAG
import com.phongkaster.kanso.ui.camera.CameraContent
import com.phongkaster.kanso.ui.camera.CameraFrame
import com.phongkaster.kanso.ui.camera.CameraScreen
import com.phongkaster.kanso.ui.camera.CameraState
import com.phongkaster.kanso.ui.camera.CameraStatus
import com.phongkaster.kanso.ui.camera.CameraUiState
import com.phongkaster.kanso.ui.camera.CameraViewModel
import com.phongkaster.kanso.ui.camera.FlashMode
import com.phongkaster.kanso.ui.gallery.GalleryContent
import com.phongkaster.kanso.ui.gallery.GalleryUiState
import com.phongkaster.kanso.ui.home.HomeContent
import com.phongkaster.kanso.ui.home.HomeUiState
import com.phongkaster.kanso.ui.permission.PermissionAccess
import com.phongkaster.kanso.ui.saved.SavedContent
import com.phongkaster.kanso.ui.saved.SavedPhoto
import com.phongkaster.kanso.ui.saved.SavedUiState
import com.phongkaster.kanso.ui.settings.SettingsContent
import com.phongkaster.kanso.ui.settings.SettingsUiState
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolUiState
import java.io.File
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Walks every screen state of Kanso and checks that each control in it has a spoken name and is
 * at least 48 dp wide and tall (DoD 18), with [TouchTargetCheck].
 *
 * How a state is checked. Each test draws one state in its own window, inside KansoTheme in
 * light mode: the screen's stateless part (the `...Content` composable, or the nearest public or
 * internal part where that is private) fed a UiState built here from sample values, or, for the
 * refused camera, the real screen (see below). No Koin, no permission dialog and no camera is
 * involved, and no ViewModel apart from the refused camera's. Then the test reads the
 * merged semantics tree, the one TalkBack reads, takes every node that can be tapped, toggled or
 * selected, and hands them to [TouchTargetCheck]. It prints one line per state,
 * `a11y <state>: <n> nodes checked`, and fails when n is 0 (a state with no control would prove
 * nothing) or when any control fails; the failure names the state and, for each failing
 * control, its test tag, role, text, description and measured size.
 *
 * The states: Home; the gallery full, partially shared, refused, empty and loading; the editor
 * with each of the five tools selected, with the photo unreadable and while saving; Saved; the
 * camera live, after a shot, refused and unavailable; Settings with Custom selected (the
 * engine's colour picker is then on screen, and [TouchTargetCheck] leaves out what is inside
 * it, DoD 18's named exception).
 *
 * Where a stand-in replaces something the computer cannot provide:
 * - the camera's live picture and its photo just taken are slots of `CameraContent`; the real
 *   ones need a camera and a file, so a plain box and a drawn photo (with the real photo's
 *   description) take their places. Neither is a control, so the check sees the same controls;
 * - the refused camera is the one state drawn from the real screen, `CameraScreen`, because its
 *   refused panel (`CameraRefusedPanel`) is private and a hand-drawn copy would miss a control
 *   later added to it. The screen reaches the state by itself, as in
 *   `ThemeFollowTest.checkCameraRefused`: its ViewModel holds a [FakeCameraController] (the
 *   computer has no camera, and with access refused the camera is never connected, so the fake
 *   is never reached), and its one permission request goes to a [RecordingRegistry], which shows
 *   no system dialog and answers "denied";
 * - the gallery's thumbnails are the real ones, loaded by Coil from made-up `fake://` addresses
 *   that [SolidColourFetcherFactory] turns into coloured squares, so each photo cell keeps the
 *   spoken name the app gives it.
 *
 * Every state is drawn with the test clock paused (`mainClock.autoAdvance = false`, set before
 * anything is drawn) and moved on by hand: the loading states show Material's loading
 * indicator, which never stops moving, and with the clock running by itself every wait for the
 * screen to settle would wait for it forever. Nothing is pressed: the refused camera's "denied"
 * comes from the registry, and the clock is moved a frame at a time until the screen shows it.
 *
 * Two more tests check the check itself on a sample 40 dp icon button with no description (the
 * DoD's "its own control"): it must be reported.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = WALK_PHONE)
class AccessibilityWalkTest {

    /** Starts an empty activity to host each state. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Whether this test gave Coil its own image loader, which [tearDown] must then replace. */
    private var coilReplaced = false

    /**
     * Gives Coil back a plain image loader if a test replaced it, and stops Koin: both are kept
     * in JVM-wide singletons that outlive the test (KansoApplication starts Koin for every test).
     */
    @After
    fun tearDown() {
        if (coilReplaced) Coil.setImageLoader(ImageLoader.Builder(app).build())
        stopKoin()
    }

    // --- Home ------------------------------------------------------------------------------

    /** Home: "Open a photo", the Camera shortcut, the five tool cards and Settings. */
    @Test
    fun home() {
        walk("home") {
            HomeContent(
                state = HOME_STATE,
                onOpenPhoto = {},
                onToolClick = {},
            )
        }
    }

    // --- Gallery ---------------------------------------------------------------------------

    /** The gallery with every photo shared: back, camera, the album chips and the photo cells. */
    @Test
    fun gallery_full() {
        useSolidColourThumbnails()
        walk(
            "gallery full",
            galleryScreen(
                state = GalleryUiState(tool = Tool.EDIT, albums = ALBUMS, photos = photos(), loading = false),
                access = PermissionAccess.Full,
            ),
        )
    }

    /**
     * The gallery after "Select photos" (Android 14 and later): the notice with its "share more"
     * button above the one album and the few photos that were shared.
     */
    @Test
    fun gallery_partially_shared() {
        useSolidColourThumbnails()
        val album = ALBUMS.first()
        walk(
            "gallery partially shared",
            galleryScreen(
                state = GalleryUiState(
                    tool = Tool.EDIT,
                    albums = listOf(album),
                    photos = photos().filter { it.albumId == album.id },
                    loading = false,
                ),
                access = PermissionAccess.Partial,
            ),
        )
    }

    /** The gallery after photo access was refused: back, camera and the button to the settings. */
    @Test
    fun gallery_refused() {
        walk(
            "gallery refused",
            galleryScreen(
                state = GalleryUiState(tool = Tool.EDIT, loading = false),
                access = PermissionAccess.None,
            ),
        )
    }

    /** The gallery with access but no photo on the phone: back, camera and the empty message. */
    @Test
    fun gallery_empty() {
        walk(
            "gallery empty",
            galleryScreen(
                state = GalleryUiState(tool = Tool.EDIT, loading = false),
                access = PermissionAccess.Full,
            ),
        )
    }

    /** The gallery while the photos are read: back, camera and the loading indicator. */
    @Test
    fun gallery_loading() {
        walk(
            "gallery loading",
            galleryScreen(
                state = GalleryUiState(tool = Tool.EDIT, loading = true),
                access = PermissionAccess.Full,
            ),
        )
    }

    // --- Editor ----------------------------------------------------------------------------

    /**
     * Each of the five editor tests below covers one tool; this one fails if a tool is ever added
     * to [Tool] without a state of its own here.
     */
    @Test
    fun every_tool_has_its_own_editor_state() {
        assertEquals(
            "Each tool needs an editor test of its own in this class",
            Tool.entries.toSet(),
            EDITOR_TOOLS_WALKED.toSet(),
        )
    }

    /** The editor on Edit with a photo: back, Save and the five toolbar toggles. */
    @Test
    fun editor_with_edit_selected() {
        walkEditorWith(Tool.EDIT)
    }

    /** The editor on Remove background with a photo. */
    @Test
    fun editor_with_remove_background_selected() {
        walkEditorWith(Tool.REMOVE_BACKGROUND)
    }

    /** The editor on Enhance with a photo. */
    @Test
    fun editor_with_enhance_selected() {
        walkEditorWith(Tool.ENHANCE)
    }

    /** The editor on Retouch with a photo. */
    @Test
    fun editor_with_retouch_selected() {
        walkEditorWith(Tool.RETOUCH)
    }

    /** The editor on Artistic with a photo. */
    @Test
    fun editor_with_artistic_selected() {
        walkEditorWith(Tool.ARTISTIC)
    }

    /** The editor when the photo cannot be opened: Save is switched off but still named. */
    @Test
    fun editor_with_the_photo_unreadable() {
        walk(
            "editor photo unreadable",
            editorScreen(
                ToolUiState(
                    tool = Tool.EDIT,
                    photoUri = PHOTO_URI,
                    photo = ToolUiState.PhotoState.Unreadable,
                    save = ToolUiState.SaveState.Idle,
                ),
            ),
        )
    }

    /**
     * The editor while a save runs: Save shows its small loading indicator and "Saving...", and
     * the toolbar's toggles are switched off but still named.
     */
    @Test
    fun editor_while_saving() {
        walk(
            "editor saving",
            editorScreen(
                ToolUiState(
                    tool = Tool.EDIT,
                    photoUri = PHOTO_URI,
                    photo = ToolUiState.PhotoState.Ready(ToolFixtures.gradient(600, 400)),
                    save = ToolUiState.SaveState.Saving,
                ),
            ),
        )
    }

    // --- Saved -----------------------------------------------------------------------------

    /** Saved with its photo: Share, Edit another and Home. */
    @Test
    fun saved() {
        val state = SavedUiState(photoUri = PHOTO_URI, photo = SavedPhoto.Ready(ToolFixtures.gradient(600, 400)))
        walk("saved") {
            SavedContent(state = state, sharing = false, onShare = {}, onEditAnother = {}, onHome = {})
        }
    }

    // --- Camera ----------------------------------------------------------------------------

    /** The camera before a shot: back, the flash button, the shutter and the lens switch. */
    @Test
    fun camera_live() {
        walk("camera live", cameraScreen(CameraUiState(camera = READY_CAMERA), shot = null))
    }

    /** The camera after a shot: back, Retake and Keep, around the photo just taken. */
    @Test
    fun camera_after_a_shot() {
        val state = CameraUiState(camera = READY_CAMERA, shot = File(app.filesDir, "shots/shot.jpg"))
        walk("camera after a shot", cameraScreen(state, shot = ToolFixtures.gradient(600, 400).asImageBitmap()))
    }

    /**
     * The camera after camera access was refused: back and the button to the settings.
     *
     * Unlike the other states, this one is the real camera screen brought to the refused state
     * by itself ([showRefusedCamera]), as `ThemeFollowTest.checkCameraRefused` does: the screen's
     * refused panel is private, and walking the real screen means a control later added to that
     * panel is walked too.
     */
    @Test
    fun camera_refused() {
        showRefusedCamera()
        assertControlsPass("camera refused")
    }

    /** The camera when it cannot be opened: back and the switched-off shutter, still named. */
    @Test
    fun camera_unavailable() {
        val unavailable = CameraState(
            status = CameraStatus.Unavailable,
            canSwitchLens = false,
            flashMode = FlashMode.Off,
            hasFlash = false,
        )
        walk("camera unavailable", cameraScreen(CameraUiState(camera = unavailable), shot = null))
    }

    // --- Settings --------------------------------------------------------------------------

    /**
     * Settings with Custom colour selected, on a phone with wallpaper colours: back, the three
     * theme mode rows and the three colour rows. The colour picker below them is the engine's
     * and is left out by [TouchTargetCheck].
     */
    @Test
    fun settings_with_custom_selected() {
        walk("settings custom selected") {
            SettingsContent(
                state = SettingsUiState(
                    themeMode = ThemeMode.System,
                    themeColour = ThemeColour.Custom(seedArgb = CUSTOM_SEED_ARGB),
                ),
                wallpaperColour = WALLPAPER_COLOUR,
                onThemeModeSelected = {},
                onKansoBlueChosen = {},
                onWallpaperChosen = {},
                onCustomChosen = {},
                onCustomColourChosen = {},
            )
        }
    }

    // --- The check's own control -----------------------------------------------------------

    /**
     * The check fails a sample 40 dp icon button with no content description, drawn the way a
     * screen would draw it: it is reported as a control with no spoken name.
     *
     * Its size is not asserted here. Compose widens the touch area of every small tappable node
     * to the phone's 48 dp minimum by itself and reports that widened area as the node's touch
     * bounds, which DoD 18 counts; the next test shows the size half of the check at work.
     */
    @Test
    fun the_check_reports_a_40_dp_icon_button_without_a_name() {
        showSampleIconButton(widenTouchArea = true)

        val violations = sampleViolations()
        assertEquals("The check must report the sample icon button once, got: $violations", 1, violations.size)
        assertTrue(
            "The sample icon button must be reported as having no name, got: $violations",
            violations.single().contains(TouchTargetCheck.NO_NAME),
        )
    }

    /**
     * The check fails the same 40 dp icon button with no description when its touch area really
     * is 40 dp: it is reported both as having no spoken name and as smaller than 48 dp.
     *
     * To get a touch area of 40 dp, the sample is drawn under a view configuration whose minimum
     * touch target is zero, so Compose does not widen it (see the previous test). That is the
     * only change: the button, its icon and its size are the same.
     */
    @Test
    fun the_check_reports_a_40_dp_icon_button_with_a_40_dp_touch_area_as_unnamed_and_too_small() {
        showSampleIconButton(widenTouchArea = false)

        val violations = sampleViolations()
        assertEquals("The check must report the sample icon button once, got: $violations", 1, violations.size)
        val report = violations.single()
        assertTrue("The sample must be reported as having no name, got: $report", report.contains(TouchTargetCheck.NO_NAME))
        assertTrue("The sample must be reported as too small, got: $report", report.contains(TouchTargetCheck.TOO_SMALL))
    }

    // --- Helpers ---------------------------------------------------------------------------

    /**
     * Draws [content] as the state called [state] (see [show]) and checks every control in it
     * (see [assertControlsPass]).
     */
    private fun walk(state: String, content: @Composable () -> Unit) {
        show(content)
        assertControlsPass(state)
    }

    /**
     * Draws [content] inside KansoTheme in light mode with the test clock paused, then moves the
     * clock on by [SETTLE_MS] and runs the main thread's queue, so the state is laid out and
     * every finite animation has ended. The pause comes first, before anything is drawn, so a
     * loading indicator never gets to run the clock by itself (see the class comment).
     */
    private fun show(content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = ThemeMode.Light) {
                content()
            }
        }
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Reads the merged semantics tree of the state on screen, called [state] in the messages,
     * prints how many controls were checked, and fails if there were none or if any failed.
     */
    private fun assertControlsPass(state: String) {
        val nodes = mergedControls()
        val checked = TouchTargetCheck.controlsToCheck(nodes)
        println("a11y $state: ${checked.size} nodes checked")
        assertTrue(
            "a11y $state: no control was checked. The merged tree has ${nodes.size} node(s) " +
                "that can be tapped, toggled or selected, but none is both placed on screen " +
                "and outside the colour picker",
            checked.isNotEmpty(),
        )
        val violations = TouchTargetCheck.violations(nodes, compose.density)
        assertTrue(
            "a11y $state: ${violations.size} of ${checked.size} controls fail DoD 18 " +
                "(a spoken name, at least ${TouchTargetCheck.MIN_SIZE} each way):\n" +
                violations.joinToString("\n"),
            violations.isEmpty(),
        )
    }

    /**
     * Returns every node of the merged semantics tree (the one TalkBack reads, where a button and
     * what is inside it are one node) that can be tapped, toggled or selected.
     */
    private fun mergedControls(): List<SemanticsNode> =
        compose.onAllNodes(IS_CONTROL, useUnmergedTree = false).fetchSemanticsNodes()

    /** Checks the editor on [tool] with a photo shown and nothing said about saving. */
    private fun walkEditorWith(tool: Tool) {
        walk(
            "editor ${tool.name}",
            editorScreen(
                ToolUiState(
                    tool = tool,
                    photoUri = PHOTO_URI,
                    photo = ToolUiState.PhotoState.Ready(ToolFixtures.gradient(600, 400)),
                    save = ToolUiState.SaveState.Idle,
                ),
            ),
        )
    }

    /**
     * Returns the gallery's stateless part for [state] with the permission answer [access]
     * (the question always answered), every tap doing nothing, and the real thumbnails.
     */
    private fun galleryScreen(state: GalleryUiState, access: PermissionAccess): @Composable () -> Unit = {
        GalleryContent(
            state = state,
            access = access,
            answered = true,
            onBack = {},
            onOpenCamera = {},
            onSelectAlbum = {},
            onPhotoClick = {},
            onShareMore = {},
            onOpenSettings = {},
        )
    }

    /** Returns the editor's stateless part for [state], every tap doing nothing. */
    private fun editorScreen(state: ToolUiState): @Composable () -> Unit = {
        ToolContent(state = state, onBack = {}, onSave = {}, onSelectTool = {})
    }

    /**
     * Returns the camera's top bar around its stateless content for [state]. The live picture is
     * a plain box; when [shot] is given, it is shown as the photo just taken, with the real shot
     * view's description, and the bar offers Keep and Retake.
     */
    private fun cameraScreen(state: CameraUiState, shot: ImageBitmap?): @Composable () -> Unit = {
        val description = stringResource(R.string.camera_shot_description)
        // The declared type tells Kotlin the slot is a composable taking the picture's modifier,
        // as CameraScreen's own code does; null means "show the live picture".
        val shotPicture: (@Composable (Modifier) -> Unit)? = if (shot == null) {
            null
        } else {
            { pictureModifier ->
                Image(
                    bitmap = shot,
                    contentDescription = description,
                    contentScale = ContentScale.Fit,
                    modifier = pictureModifier,
                )
            }
        }
        CameraFrame(onBack = {}) {
            CameraContent(
                state = state,
                onShutter = {},
                onKeep = {},
                onRetake = {},
                onFlash = {},
                onSwitchLens = {},
                onZoom = {},
                livePicture = { pictureModifier -> Box(pictureModifier) },
                shotPicture = shotPicture,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    /**
     * Draws the real camera screen (see [show]) and lets it reach its "camera access is off"
     * state, the way `ThemeFollowTest.checkCameraRefused` does:
     *
     * - the screen's ViewModel is built here around a [FakeCameraController], so no camera and no
     *   Koin is involved. Access is never granted, so the screen never connects the camera and
     *   the fake is never reached;
     * - the screen asks for the camera through `rememberLauncherForActivityResult`, which finds
     *   its registry in `LocalActivityResultRegistryOwner`. Providing a [RecordingRegistry] there
     *   means the request is only recorded (no system dialog), and the test answers it "denied";
     * - the screen learns the answer at once, but only shows it once the clock moves, and the
     *   clock is paused (see the class comment). So the clock is moved one frame at a time, with
     *   the main thread's queue run after each, until the refused panel is there, and then by
     *   [SETTLE_MS], so every finite animation has ended before the walk.
     *
     * It fails if the screen did not ask exactly once, or if the refused panel and its settings
     * button are not on screen, so the walk can never pass on the top bar's back button alone.
     */
    private fun showRefusedCamera() {
        val registry = RecordingRegistry()
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = CameraViewModel(FakeCameraController(), ViewModelClearedProbe())
        show {
            // A CompositionLocalProvider hands a value down to everything drawn inside it; here,
            // the registry the screen's permission request goes to.
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                CameraScreen(viewModel = viewModel, onBack = {}, onKept = {})
            }
        }
        assertEquals("The camera screen must ask for the camera once", 1, registry.requests.size)
        registry.answerLatest(registry.requests.single().associateWith { false })

        compose.waitUntil(timeoutMillis = REFUSED_TIMEOUT_MS) {
            compose.mainClock.advanceTimeByFrame()
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(hasTestTag(CAMERA_REFUSED_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        shadowOf(Looper.getMainLooper()).idle()

        compose.onNodeWithTag(CAMERA_REFUSED_TAG).assertIsDisplayed()
        compose.onNodeWithTag(CAMERA_SETTINGS_BUTTON_TAG).assertIsDisplayed()
    }

    /**
     * Gives Coil an image loader that knows the `fake` scheme ([SolidColourFetcherFactory]) and
     * runs every step on the calling thread, so the gallery's thumbnails load without files.
     */
    private fun useSolidColourThumbnails() {
        Coil.setImageLoader(
            ImageLoader.Builder(app)
                .components { add(SolidColourFetcherFactory()) }
                .dispatcher(Dispatchers.Unconfined)
                .build(),
        )
        coilReplaced = true
    }

    /**
     * Draws the sample icon button: a 40 dp tappable box with a gear icon and no content
     * description, in the middle of the screen. With [widenTouchArea] false it is drawn under a
     * view configuration whose minimum touch target is zero, so Compose leaves its touch area at
     * 40 dp; otherwise under the phone's own configuration.
     *
     * The box sits inside a full-screen box, which hands it loose constraints: KansoTheme passes
     * its own full-screen size down as a minimum, which would otherwise stretch the sample.
     */
    private fun showSampleIconButton(widenTouchArea: Boolean) {
        show {
            val system = LocalViewConfiguration.current
            // Remembered, so the same configuration object is handed down on every frame.
            val configuration: ViewConfiguration = remember(system, widenTouchArea) {
                if (widenTouchArea) {
                    system
                } else {
                    object : ViewConfiguration by system {
                        override val minimumTouchTargetSize: DpSize = DpSize.Zero
                    }
                }
            }
            CompositionLocalProvider(LocalViewConfiguration provides configuration) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(SAMPLE_SIZE)
                            .testTag(SAMPLE_TAG)
                            .clickable(role = Role.Button, onClick = {}),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = null)
                    }
                }
            }
        }
    }

    /**
     * Checks that the sample icon button is laid out at exactly [SAMPLE_SIZE] and is the one
     * control [TouchTargetCheck] checks, prints the count as the walk does, and returns the
     * check's report for it.
     */
    private fun sampleViolations(): List<String> {
        val sample = compose.onNodeWithTag(SAMPLE_TAG).fetchSemanticsNode()
        val samplePx = with(compose.density) { SAMPLE_SIZE.toPx() }
        assertTrue(
            "The sample must be laid out at $SAMPLE_SIZE ($samplePx px), was ${sample.size}",
            abs(sample.size.width - samplePx) <= 0.5f && abs(sample.size.height - samplePx) <= 0.5f,
        )
        val nodes = mergedControls()
        val checked = TouchTargetCheck.controlsToCheck(nodes)
        println("a11y sample 40 dp icon button: ${checked.size} nodes checked")
        assertEquals("The sample must be the one control checked", listOf(sample.id), checked.map { it.id })
        return TouchTargetCheck.violations(nodes, compose.density)
    }

    /** Values the tests share. */
    private companion object {
        /**
         * How far the paused clock is moved on after a state is drawn, in milliseconds: long
         * enough for every finite animation (a colour fade, the Saved mark) to end.
         */
        const val SETTLE_MS = 1_000L

        /**
         * How long, in real milliseconds, the refused camera may take to show its refused panel
         * after the "denied" answer before the test fails (the same ten seconds
         * `ThemeFollowTest` gives the camera); it normally takes a frame or two.
         */
        const val REFUSED_TIMEOUT_MS = 10_000L

        /** The made-up Uri the editor and Saved states say their photo came from. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** The test tag of the sample icon button the check is tried on. */
        const val SAMPLE_TAG = "a11y-sample-icon-button"

        /** The size of the sample icon button: 8 dp under the minimum. */
        val SAMPLE_SIZE = 40.dp

        /** A custom seed colour for the Settings state, a deep green, as an ARGB number. */
        val CUSTOM_SEED_ARGB = 0xFF2E7D32.toInt()

        /** A wallpaper colour for the Settings state, so its wallpaper row is drawn too. */
        val WALLPAPER_COLOUR = Color(0xFF8B5000)

        /** Matches a node that can be tapped, toggled or selected (see [TouchTargetCheck.isControl]). */
        val IS_CONTROL = SemanticsMatcher("can be tapped, toggled or selected") { TouchTargetCheck.isControl(it) }

        /** Home's two groups of tools, as `HomeViewModel` builds them. */
        val HOME_STATE = HomeUiState(
            aiTools = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
            editTools = listOf(Tool.EDIT),
        )

        /** The tools the five editor tests walk, one each; see `every_tool_has_its_own_editor_state`. */
        val EDITOR_TOOLS_WALKED = listOf(Tool.EDIT, Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC)

        /** A camera that is open and ready, with its flash on auto and a second lens. */
        val READY_CAMERA = CameraState(
            status = CameraStatus.Ready,
            canSwitchLens = true,
            flashMode = FlashMode.Auto,
            hasFlash = true,
        )

        /** The three albums of the sample photo library. */
        val ALBUMS = listOf(Album(1L, "Camera"), Album(2L, "Screenshots"), Album(3L, "Travel"))

        /**
         * Twelve sample photos, newest first, spread over [ALBUMS], each with its own colour in a
         * `fake://color/AARRGGBB` address that only [SolidColourFetcherFactory] can open.
         */
        fun photos(): List<Photo> = (0 until 12).map { i ->
            val red = (i * 37 + 60) % 256
            val green = (i * 91 + 40) % 256
            val blue = (i * 53 + 120) % 256
            val argb = (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
            Photo(
                id = (i + 1).toLong(),
                uri = "$FAKE_SCHEME://color/${"%08X".format(argb)}",
                name = "photo_${i + 1}.jpg",
                albumId = ALBUMS[i % ALBUMS.size].id,
                modifiedSeconds = 1_700_000_000L - i * 3_600L,
            )
        }
    }
}

/**
 * The Robolectric screen every state is drawn on, the same as the screenshot tests': 411 dp wide,
 * 891 dp tall, at xxhdpi, the size of a typical modern phone.
 */
private const val WALK_PHONE = "w411dp-h891dp-xxhdpi"
