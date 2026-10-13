package com.phongkaster.kanso.theme

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
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
import com.phongkaster.kanso.ui.camera.CAMERA_PREVIEW_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_REFUSED_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CameraScreen
import com.phongkaster.kanso.ui.camera.CameraViewModel
import com.phongkaster.kanso.ui.gallery.GALLERY_BODY_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_GRID_TAG
import com.phongkaster.kanso.ui.gallery.GalleryContent
import com.phongkaster.kanso.ui.gallery.GalleryUiState
import com.phongkaster.kanso.ui.home.HomeContent
import com.phongkaster.kanso.ui.home.HomeUiState
import com.phongkaster.kanso.ui.permission.PermissionAccess
import com.phongkaster.kanso.ui.saved.SAVED_MARK_TAG
import com.phongkaster.kanso.ui.saved.SavedContent
import com.phongkaster.kanso.ui.saved.SavedPhoto
import com.phongkaster.kanso.ui.saved.SavedUiState
import com.phongkaster.kanso.ui.settings.SETTINGS_GROUP_APPEARANCE_TAG
import com.phongkaster.kanso.ui.settings.SettingsContent
import com.phongkaster.kanso.ui.settings.SettingsUiState
import com.phongkaster.kanso.ui.theme.KANSO_BLUE_SEED
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.theme.seedColorScheme
import com.phongkaster.kanso.ui.tool.TOOL_CARD_TAG
import com.phongkaster.kanso.ui.tool.ToolContent
import com.phongkaster.kanso.ui.tool.ToolUiState
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
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
 * Checks that every screen follows the app's light or dark choice, the editor included, and
 * paints its top bar and its background in the theme's own colours (DoD 16).
 *
 * Each test draws one screen in KansoTheme with the mode forced to Light or Dark and the default
 * colour, Kanso blue, on Android 15: Home, the gallery with photos, the editor with a ready
 * photo, Saved, Settings, and the camera twice, once after camera access was refused and once
 * live with a [FakeCameraController] (the real camera screen, its ViewModel and a camera that
 * reports itself ready at once). The other screens are their stateless `...Content` parts fed a
 * UiState built here, as the previews do.
 *
 * The screen is then drawn into a bitmap, as `ScreenPixels.kt` does (Compose's own
 * captureToImage times out under Robolectric here), with Robolectric's real drawing code on
 * (`GraphicsMode.Mode.NATIVE`), and two pixels are read:
 * - one in the top bar, near its top edge in the middle, where no title or button reaches. It
 *   must be the scheme's `surface`, the colour Material's top bar is filled with. Home has no top
 *   bar (its title is the first item of its scrolling column), so it has no such pixel;
 * - one in the screen's background, outside any photo or camera picture and away from every
 *   text and control. It must be the colour the screen really paints there: the scheme's
 *   `background` (what a Scaffold fills its body with) on every screen but the live camera,
 *   whose sampled background is its bottom bar, a Surface in `surface`.
 * Both must equal the expected colour of `seedColorScheme(KANSO_BLUE_SEED, dark)` within
 * [TOLERANCE] per channel, and the background pixel's WCAG relative luminance must be above
 * [LIGHT_MIN_LUMINANCE] in light and below [DARK_MAX_LUMINANCE] in dark, so a screen that kept
 * a light background in dark mode (or the reverse) fails even if its colour were close.
 *
 * Where each pixel goes is worked out from the screen's own layout (the positions of tagged
 * nodes below the bar) rather than guessed, and the test first checks its own choice: the
 * top-bar pixel must lie above the content, and no pixel may lie on a node that shows text, a
 * picture or a control. Settings' colour swatches and the colour picker's white card are fixed
 * colours by design (the picker card is DoD 16's one named exception), so no pixel is read
 * there; the Saved success mark is the theme's primary colour, so its row is sampled at the
 * screen's left edge, well clear of it.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = THEME_FOLLOW_PHONE)
class ThemeFollowTest {

    /** Starts an empty activity to host each screen. */
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

    /** Home in light mode paints Kanso blue's light background. */
    @Test
    fun home_follows_the_light_theme() {
        checkHome(ThemeMode.Light)
    }

    /** Home in dark mode paints Kanso blue's dark background. */
    @Test
    fun home_follows_the_dark_theme() {
        checkHome(ThemeMode.Dark)
    }

    /** The gallery with photos in light mode: light top bar and background. */
    @Test
    fun gallery_with_photos_follows_the_light_theme() {
        checkGallery(ThemeMode.Light)
    }

    /** The gallery with photos in dark mode: dark top bar and background. */
    @Test
    fun gallery_with_photos_follows_the_dark_theme() {
        checkGallery(ThemeMode.Dark)
    }

    /** The editor with a ready photo in light mode: light top bar and background around the photo. */
    @Test
    fun editor_with_a_photo_follows_the_light_theme() {
        checkEditor(ThemeMode.Light)
    }

    /** The editor with a ready photo in dark mode: dark top bar and background around the photo. */
    @Test
    fun editor_with_a_photo_follows_the_dark_theme() {
        checkEditor(ThemeMode.Dark)
    }

    /** Saved in light mode: light top bar and background. */
    @Test
    fun saved_follows_the_light_theme() {
        checkSaved(ThemeMode.Light)
    }

    /** Saved in dark mode: dark top bar and background. */
    @Test
    fun saved_follows_the_dark_theme() {
        checkSaved(ThemeMode.Dark)
    }

    /** Settings in light mode: light top bar and background. */
    @Test
    fun settings_follows_the_light_theme() {
        checkSettings(ThemeMode.Light)
    }

    /** Settings in dark mode: dark top bar and background. */
    @Test
    fun settings_follows_the_dark_theme() {
        checkSettings(ThemeMode.Dark)
    }

    /** The camera after access was refused, in light mode: light top bar and background. */
    @Test
    fun camera_refused_follows_the_light_theme() {
        checkCameraRefused(ThemeMode.Light)
    }

    /** The camera after access was refused, in dark mode: dark top bar and background. */
    @Test
    fun camera_refused_follows_the_dark_theme() {
        checkCameraRefused(ThemeMode.Dark)
    }

    /** The live camera in light mode: light top bar and bottom bar. */
    @Test
    fun camera_live_follows_the_light_theme() {
        checkCameraLive(ThemeMode.Light)
    }

    /** The live camera in dark mode: dark top bar and bottom bar. */
    @Test
    fun camera_live_follows_the_dark_theme() {
        checkCameraLive(ThemeMode.Dark)
    }

    // --- One screen each -------------------------------------------------------------------

    /**
     * Draws Home in [mode] and checks a background pixel at the screen's left edge, halfway
     * down: Home's column keeps 16 dp from the edges, so nothing but background is there.
     */
    private fun checkHome(mode: ThemeMode) {
        show(mode) {
            HomeContent(
                state = HOME_STATE,
                onOpenPhoto = {},
                onToolClick = {},
            )
        }
        assertFollows(
            screen = "Home",
            mode = mode,
            topBar = null,
            background = Spot(
                place = "background at the left edge, halfway down",
                x = EDGE_X_PX,
                y = rootSize().height / 2,
                expected = scheme(mode).background,
                role = "background",
            ),
            contentTop = null,
        )
    }

    /**
     * Draws the gallery of the Edit tool in [mode] with twelve photos in three albums and checks
     * the top bar and a background pixel in the grid's 8 dp left margin, halfway down the grid,
     * where no photo tile reaches.
     */
    private fun checkGallery(mode: ThemeMode) {
        useSolidColourThumbnails()
        val state = GalleryUiState(tool = Tool.EDIT, albums = ALBUMS, photos = photos(), loading = false)
        show(mode) {
            GalleryContent(
                state = state,
                access = PermissionAccess.Full,
                answered = true,
                onBack = {},
                onOpenCamera = {},
                onSelectAlbum = {},
                onPhotoClick = {},
                onShareMore = {},
                onOpenSettings = {},
            )
        }
        val grid = nodeRect(GALLERY_GRID_TAG)
        assertFollows(
            screen = "Gallery with photos",
            mode = mode,
            topBar = topBarSpot(mode),
            background = Spot(
                place = "background in the grid's left margin, halfway down the grid",
                x = EDGE_X_PX,
                y = grid.center.y.roundToInt(),
                expected = scheme(mode).background,
                role = "background",
            ),
            contentTop = nodeRect(GALLERY_BODY_TAG).top,
        )
    }

    /**
     * Draws the editor on Edit in [mode] with a ready landscape photo and checks the top bar and
     * a background pixel in the photo area above the photo, halfway between the area's top and
     * the photo's. A landscape photo on a portrait screen fills the width and leaves that band of
     * background above and below it; the test checks the band is there before reading it.
     */
    private fun checkEditor(mode: ThemeMode) {
        val state = ToolUiState(
            tool = Tool.EDIT,
            photoUri = PHOTO_URI,
            photo = ToolUiState.PhotoState.Ready(ToolFixtures.gradient(600, 400)),
            save = ToolUiState.SaveState.Idle,
        )
        show(mode) {
            ToolContent(state = state, onBack = {}, onSave = {})
        }
        val card = nodeRect(TOOL_CARD_TAG)
        assertTrue(
            "The card must leave a margin at the screen's left edge (card left ${card.left}), " +
                "or this test reads the wrong place",
            card.left >= MIN_BAND_PX,
        )
        assertFollows(
            screen = "Tool screen with a ready photo",
            mode = mode,
            topBar = topBarSpot(mode),
            background = Spot(
                place = "background left of the photo card, level with its middle",
                x = (card.left / 2f).roundToInt(),
                y = ((card.top + card.bottom) / 2f).roundToInt(),
                expected = scheme(mode).background,
                role = "background",
            ),
            contentTop = card.top,
        )
    }

    /**
     * Draws Saved in [mode] with its photo and checks the top bar and a background pixel at the
     * screen's left edge, level with the success mark: the body keeps 16 dp from the edges and
     * the mark sits in the middle, so that pixel is background and well clear of the mark.
     */
    private fun checkSaved(mode: ThemeMode) {
        val state = SavedUiState(photoUri = PHOTO_URI, photo = SavedPhoto.Ready(ToolFixtures.gradient(600, 400)))
        show(mode) {
            SavedContent(state = state, sharing = false, onShare = {}, onEditAnother = {}, onHome = {})
        }
        val mark = nodeRect(SAVED_MARK_TAG)
        assertFollows(
            screen = "Saved",
            mode = mode,
            topBar = topBarSpot(mode),
            background = Spot(
                place = "background at the left edge, level with the success mark",
                x = EDGE_X_PX,
                y = mark.center.y.roundToInt(),
                expected = scheme(mode).background,
                role = "background",
            ),
            contentTop = mark.top,
        )
    }

    /**
     * Draws Settings in [mode] with that mode and Kanso blue chosen (no wallpaper row, no
     * picker) and checks the top bar and a background pixel at the screen's left edge, level with
     * the theme mode group: the groups keep 16 dp from the edges, so no row and no swatch is
     * there.
     */
    private fun checkSettings(mode: ThemeMode) {
        show(mode) {
            SettingsContent(
                state = SettingsUiState(themeMode = mode, themeColour = ThemeColour.KansoBlue),
                wallpaperColour = null,
                onThemeModeSelected = {},
                onKansoBlueChosen = {},
                onWallpaperChosen = {},
                onCustomChosen = {},
                onCustomColourChosen = {},
            )
        }
        val group = nodeRect(SETTINGS_GROUP_APPEARANCE_TAG)
        assertFollows(
            screen = "Settings",
            mode = mode,
            topBar = topBarSpot(mode),
            background = Spot(
                place = "background at the left edge, level with the theme mode group",
                x = EDGE_X_PX,
                y = group.center.y.roundToInt(),
                expected = scheme(mode).background,
                role = "background",
            ),
            contentTop = group.top,
        )
    }

    /**
     * Draws the real camera screen in [mode], answers its camera permission request "denied"
     * through a [RecordingRegistry] (no system dialog), and checks the top bar and a background
     * pixel at the screen's left edge, level with the middle of the refused panel, which keeps
     * 24 dp from the edges.
     */
    private fun checkCameraRefused(mode: ThemeMode) {
        val registry = RecordingRegistry()
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = CameraViewModel(FakeCameraController(), ViewModelClearedProbe())
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                KansoTheme(themeMode = mode) {
                    CameraScreen(viewModel = viewModel, onBack = {}, onKept = {})
                }
            }
        }
        compose.waitForIdle()
        assertEquals("The camera screen must ask for the camera once", 1, registry.requests.size)
        registry.answerLatest(registry.requests.single().associateWith { false })
        compose.waitForIdle()

        val panel = nodeRect(CAMERA_REFUSED_TAG)
        assertFollows(
            screen = "Camera, access refused",
            mode = mode,
            topBar = topBarSpot(mode),
            background = Spot(
                place = "background at the left edge, level with the middle of the refused panel",
                x = EDGE_X_PX,
                y = panel.center.y.roundToInt(),
                expected = scheme(mode).background,
                role = "background",
            ),
            contentTop = panel.top,
        )
    }

    /**
     * Grants camera access and draws the real camera screen in [mode] with a [FakeCameraController],
     * waits until the fake camera is connected (the shutter can be pressed), and checks the top
     * bar and a pixel of the bottom bar at the screen's left edge, level with the shutter: the
     * bar keeps its buttons 16 dp from the edges, and it is a Surface in the theme's `surface`.
     * The live picture itself is not a theme colour, so no pixel is read there.
     */
    private fun checkCameraLive(mode: ThemeMode) {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val viewModel = CameraViewModel(FakeCameraController(), ViewModelClearedProbe())
        show(mode) {
            CameraScreen(viewModel = viewModel, onBack = {}, onKept = {})
        }
        // The fake camera connects from a coroutine the screen starts; it ends on the main thread.
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(hasTestTag(CAMERA_SHUTTER_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()

        val shutter = nodeRect(CAMERA_SHUTTER_TAG)
        assertFollows(
            screen = "Camera, live",
            mode = mode,
            topBar = topBarSpot(mode),
            background = Spot(
                place = "bottom bar at the left edge, level with the shutter",
                x = EDGE_X_PX,
                y = shutter.center.y.roundToInt(),
                expected = scheme(mode).surface,
                role = "surface",
            ),
            contentTop = nodeRect(CAMERA_PREVIEW_TAG).top,
        )
    }

    // --- Helpers ---------------------------------------------------------------------------

    /**
     * One pixel the test reads: [place] says in words where it is, ([x], [y]) is where, in the
     * screen's pixels from its top-left corner, and [expected] is the theme colour it must show,
     * the scheme's colour called [role].
     */
    private data class Spot(
        val place: String,
        val x: Int,
        val y: Int,
        val expected: Color,
        val role: String,
    )

    /** Returns Kanso blue in [mode]'s look, the scheme KansoTheme hands to every screen. */
    private fun scheme(mode: ThemeMode): ColorScheme = seedColorScheme(KANSO_BLUE_SEED, dark = mode == ThemeMode.Dark)

    /**
     * Returns the top-bar pixel for [mode]: the middle of the screen, [TOP_BAR_Y_PX] below the
     * top edge, inside the bar but above its title and buttons, which are centred in its height.
     * It must be the scheme's `surface`.
     */
    private fun topBarSpot(mode: ThemeMode): Spot = Spot(
        place = "top bar, middle of its top edge",
        x = rootSize().width / 2,
        y = TOP_BAR_Y_PX,
        expected = scheme(mode).surface,
        role = "surface",
    )

    /**
     * Draws [content] inside KansoTheme in [mode], with Kanso blue, and waits until the screen
     * has settled. Nothing the tests draw keeps moving, so the clock runs by itself.
     */
    private fun show(mode: ThemeMode, content: @Composable () -> Unit) {
        compose.setContent {
            KansoTheme(themeMode = mode) {
                content()
            }
        }
        compose.waitForIdle()
    }

    /** Returns the size of the screen's Compose root, in pixels: the size of the drawn bitmap. */
    private fun rootSize(): IntSize = compose.onRoot().fetchSemanticsNode().size

    /**
     * Returns where the node tagged [tag] is laid out, in the root's pixels. It is read from
     * `positionInRoot` and the node's size, never from `boundsInRoot`, which Compose cuts to the
     * visible part of the screen.
     */
    private fun nodeRect(tag: String): Rect {
        val node = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        return Rect(node.positionInRoot, node.size.toSize())
    }

    /**
     * Fails, naming [screen] and [mode], unless the [topBar] pixel (if the screen has a top bar)
     * and the [background] pixel show their expected theme colours within [TOLERANCE] per
     * channel, and the background's relative luminance suits the mode.
     *
     * Before reading, it checks its own choice of pixels: the top-bar pixel must lie above
     * [contentTop], the top of the content below the bar, and no pixel may lie on a node that
     * shows text, a picture or a control. Every problem is listed in one failure.
     */
    private fun assertFollows(screen: String, mode: ThemeMode, topBar: Spot?, background: Spot, contentTop: Float?) {
        val spots = listOfNotNull(topBar, background)
        val problems = mutableListOf<String>()
        if (topBar != null) {
            val top = checkNotNull(contentTop) { "A screen with a top bar needs the top of its content" }
            if (topBar.y >= top) {
                problems += "the ${topBar.place} at y ${topBar.y} is not above the content, which starts at y $top"
            }
        }
        spots.forEach { spot ->
            nodesUnder(spot).forEach { node ->
                problems += "the ${spot.place} at (${spot.x}, ${spot.y}) lies on $node, " +
                    "not on plain background: the test must read another pixel"
            }
        }

        val drawn = drawnColours(spots)
        spots.zip(drawn).forEach { (spot, colour) ->
            if (!isNear(spot.expected, colour)) {
                problems += "the ${spot.place} at (${spot.x}, ${spot.y}) is drawn ${hex(colour)}, " +
                    "expected the theme's ${spot.role} ${hex(spot.expected)} within $TOLERANCE per channel"
            }
        }

        // The background is the last spot read.
        val luminance = relativeLuminance(drawn.last())
        if (mode == ThemeMode.Dark) {
            if (luminance >= DARK_MAX_LUMINANCE) {
                problems += "the ${background.place} has relative luminance %.3f, needs below %.2f in dark"
                    .format(luminance, DARK_MAX_LUMINANCE)
            }
        } else if (luminance <= LIGHT_MIN_LUMINANCE) {
            problems += "the ${background.place} has relative luminance %.3f, needs above %.2f in light"
                .format(luminance, LIGHT_MIN_LUMINANCE)
        }

        assertTrue(
            "$screen in $mode does not follow the theme:\n" + problems.joinToString("\n"),
            problems.isEmpty(),
        )
    }

    /**
     * Returns a description of every node placed on screen that shows text, a picture (a content
     * description) or a control and covers [spot], from the unmerged tree, where every text and
     * icon is a node of its own.
     */
    private fun nodesUnder(spot: Spot): List<String> {
        val point = Offset(spot.x + 0.5f, spot.y + 0.5f)
        return compose.onAllNodes(SHOWS_SOMETHING, useUnmergedTree = true).fetchSemanticsNodes()
            .filter { node -> isPlaced(node) && Rect(node.positionInRoot, node.size.toSize()).contains(point) }
            .map { node -> describe(node) }
    }

    /**
     * Tells whether [node]'s layout, and every layout around it up to the root, has been placed:
     * a layout only measured, never placed, is not on the screen and covers nothing.
     */
    private fun isPlaced(node: SemanticsNode): Boolean {
        var info: LayoutInfo? = node.layoutInfo
        while (info != null) {
            if (!info.isPlaced) return false
            info = info.parentInfo
        }
        return true
    }

    /** Describes [node] for a failure message by its test tag, texts and content descriptions. */
    private fun describe(node: SemanticsNode): String {
        val config = node.config
        val tag = config.getOrNull(SemanticsProperties.TestTag) ?: "no tag"
        val texts = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        val descriptions = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
        return "a node (tag: $tag, text: $texts, description: $descriptions)"
    }

    /**
     * Draws the activity's content view into a fresh bitmap and returns the colour of the pixel
     * of each of [spots], in order. The Compose root fills the content view from its top-left
     * corner, so a position in the root is a position in the bitmap. Runs on the main thread
     * (the test thread under Robolectric).
     */
    private fun drawnColours(spots: List<Spot>): List<Color> {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            return spots.map { spot ->
                check(spot.x in 0 until bitmap.width && spot.y in 0 until bitmap.height) {
                    "The ${spot.place} at (${spot.x}, ${spot.y}) lies outside the drawn screen " +
                        "of ${bitmap.width} x ${bitmap.height}"
                }
                Color(bitmap.getPixel(spot.x, spot.y))
            }
        } finally {
            // A bitmap holds native memory; free it now rather than waiting for the collector.
            bitmap.recycle()
        }
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
     * Tells whether [actual] equals [expected] within [TOLERANCE] in each of its alpha, red,
     * green and blue channels (0 to 255). An undrawn, see-through pixel never passes.
     */
    private fun isNear(expected: Color, actual: Color): Boolean {
        val wanted = expected.toArgb()
        val drawn = actual.toArgb()
        return listOf(24, 16, 8, 0).all { shift ->
            abs(((wanted shr shift) and 0xFF) - ((drawn shr shift) and 0xFF)) <= TOLERANCE
        }
    }

    /** Writes [colour] as `#AARRGGBB`, for failure messages. */
    private fun hex(colour: Color): String = "#%08X".format(colour.toArgb())

    /**
     * Returns the WCAG relative luminance of [colour], from 0 (black) to 1 (white), as
     * `KansoBlueContrastTest` works it out: each 8-bit sRGB channel is turned back into linear
     * light, then the three are weighted by how bright the eye finds red, green and blue.
     */
    private fun relativeLuminance(colour: Color): Double {
        val argb = colour.toArgb()
        val red = linearChannel((argb shr 16) and 0xFF)
        val green = linearChannel((argb shr 8) and 0xFF)
        val blue = linearChannel(argb and 0xFF)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    /**
     * Undoes the sRGB gamma curve on one 8-bit [channel] (0 to 255), giving its linear light
     * from 0 to 1: a straight line for the darkest values, a 2.4 power curve above.
     */
    private fun linearChannel(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }

    /** Values the tests share. */
    private companion object {
        /** How far, per colour channel (0 to 255), a drawn pixel may be from the expected colour. */
        const val TOLERANCE = 2

        /** The lowest relative luminance a light background may have. */
        const val LIGHT_MIN_LUMINANCE = 0.6

        /** The highest relative luminance a dark background may have. */
        const val DARK_MAX_LUMINANCE = 0.15

        /**
         * How far below the top of the screen the top-bar pixel is, in pixels: 1 dp at xxhdpi,
         * inside the 64 dp bar and well above its 48 dp buttons, which are centred in it.
         */
        const val TOP_BAR_Y_PX = 3

        /**
         * How far from the screen's left edge the background pixels are, in pixels: under 1 dp,
         * inside the margin every screen keeps between its edge and its content (8 dp at least).
         */
        const val EDGE_X_PX = 2

        /**
         * The least height, in pixels, the band of background above the editor's photo must
         * have for the test to read a pixel in its middle (4 dp at xxhdpi).
         */
        const val MIN_BAND_PX = 12f

        /** The made-up Uri the editor and Saved say their photo came from. */
        const val PHOTO_URI = "content://media/external/images/media/42"

        /** Matches a node that shows text, a picture (it has a description) or a control. */
        val SHOWS_SOMETHING = SemanticsMatcher("shows text, a picture or a control") { node ->
            val config = node.config
            SemanticsProperties.Text in config ||
                SemanticsProperties.ContentDescription in config ||
                SemanticsActions.OnClick in config ||
                SemanticsProperties.ToggleableState in config ||
                SemanticsProperties.Selected in config
        }

        /** Home's two groups of tools, as `HomeViewModel` builds them. */
        val HOME_STATE = HomeUiState(
            aiTools = listOf(Tool.REMOVE_BACKGROUND, Tool.ENHANCE, Tool.RETOUCH, Tool.ARTISTIC),
            editTools = listOf(Tool.EDIT),
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
 * The Robolectric screen the screens are drawn on, the same as the screenshot tests': 411 dp
 * wide, 891 dp tall, at xxhdpi, the size of a typical modern phone.
 */
private const val THEME_FOLLOW_PHONE = "w411dp-h891dp-xxhdpi"
