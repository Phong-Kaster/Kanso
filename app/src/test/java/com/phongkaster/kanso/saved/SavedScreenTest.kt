package com.phongkaster.kanso.saved

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.saved.SAVED_BACKDROP_TAG
import com.phongkaster.kanso.ui.saved.SAVED_CLOSE_TAG
import com.phongkaster.kanso.ui.saved.SAVED_EDIT_ANOTHER_ICON_TAG
import com.phongkaster.kanso.ui.saved.SAVED_EDIT_ANOTHER_TAG
import com.phongkaster.kanso.ui.saved.SAVED_LOADING_TAG
import com.phongkaster.kanso.ui.saved.SAVED_MARK_TAG
import com.phongkaster.kanso.ui.saved.SAVED_MESSAGE_TAG
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_TAG
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.saved.SAVED_SCREEN_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SCRIM_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SHARE_TAG
import com.phongkaster.kanso.ui.saved.SAVED_TITLE_TAG
import com.phongkaster.kanso.ui.saved.SavedContent
import com.phongkaster.kanso.ui.saved.SavedPhoto
import com.phongkaster.kanso.ui.saved.SavedScreen
import com.phongkaster.kanso.ui.saved.SavedUiState
import com.phongkaster.kanso.ui.saved.SavedViewModel
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks the Saved screen (DoD 15), in its two uses: right after a save, and as the photo viewer
 * opened from My edits.
 *
 * The layout: a blurred copy of the photo covers the whole window and the photo sits whole inside
 * it, as large as fits; at the top a shade, a round close button and the title ("Saved", or
 * "My edits" in the viewer); at the bottom a round, icon-only Share button beside one wide button
 * ("Edit another", or "Edit" in the viewer); after a save only, a success mark centred in the top
 * quarter that plays once and then rests, and the line "Your photo is saved in Pictures/Kanso.".
 * The shade is checked in pixels: over a photo that contrasts with the mode it is the theme's
 * `surface` laid over the photo at 85 %, and it reads clearly apart from the bare photo.
 *
 * The behaviour: Share opens the system share sheet for the photo with a read grant and no chosen
 * app (on Android 9 with the photo's content Uri, never its file Uri), and Close, Share and the
 * wide button call back. The loading indicator shows while the photo is decoded (DoD 9).
 *
 * The layout checks run on Android 11 and on Android 15, each version a test of its own, since the
 * blurred backdrop draws differently on the two (see `BlurredBackdrop`). They show the stateless
 * [SavedContent] with the photo already decoded and the test clock paused (see
 * [showContentWithPausedClock]), so the success mark is pictured at exact moments and nothing
 * moves while the layout is measured. The behaviour checks show the full [SavedScreen], whose
 * [SavedViewModel] is built with a [FakePhotoLoader] holding a picture drawn in code, so nothing
 * depends on a file. Every test runs on the same phone size as the pictures.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SavedScreenTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /**
     * Stops Koin after each test: KansoApplication starts it for every Robolectric test, and its
     * state outlives the test inside the same JVM.
     */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    // --- Layout: backdrop and photo -------------------------------------------------------------

    /** Android 11: the backdrop covers the window and each photo fits inside it (see [checkBackdropAndPhoto]). */
    @Test
    @Config(sdk = [30])
    fun the_backdrop_covers_the_window_and_the_photo_fits_inside_it_on_android_11() {
        checkBackdropAndPhoto()
    }

    /** Android 15: the backdrop covers the window and each photo fits inside it (see [checkBackdropAndPhoto]). */
    @Test
    @Config(sdk = [35])
    fun the_backdrop_covers_the_window_and_the_photo_fits_inside_it_on_android_15() {
        checkBackdropAndPhoto()
    }

    // --- Layout: the top row and the shade ------------------------------------------------------

    /** Android 11: close button, title and shade are in place (see [checkTopRowAndShadeGeometry]). */
    @Test
    @Config(sdk = [30])
    fun the_close_button_title_and_shade_are_in_place_on_android_11() {
        checkTopRowAndShadeGeometry()
    }

    /** Android 15: close button, title and shade are in place (see [checkTopRowAndShadeGeometry]). */
    @Test
    @Config(sdk = [35])
    fun the_close_button_title_and_shade_are_in_place_on_android_15() {
        checkTopRowAndShadeGeometry()
    }

    /** Android 11: the shade is `surface` at 85 % over the photo, in light and dark (see [checkShadeColour]). */
    @Test
    @Config(sdk = [30])
    fun the_shade_is_the_surface_colour_over_a_contrasting_photo_on_android_11() {
        checkShadeColour()
    }

    /** Android 15: the shade is `surface` at 85 % over the photo, in light and dark (see [checkShadeColour]). */
    @Test
    @Config(sdk = [35])
    fun the_shade_is_the_surface_colour_over_a_contrasting_photo_on_android_15() {
        checkShadeColour()
    }

    // --- Layout: the bottom row -----------------------------------------------------------------

    /** Android 11: Share and the wide button share one row across the width (see [checkBottomRow]). */
    @Test
    @Config(sdk = [30])
    fun share_and_the_wide_button_share_one_row_across_the_width_on_android_11() {
        checkBottomRowAfterSaveAndInViewer()
    }

    /** Android 15: Share and the wide button share one row across the width (see [checkBottomRow]). */
    @Test
    @Config(sdk = [35])
    fun share_and_the_wide_button_share_one_row_across_the_width_on_android_15() {
        checkBottomRowAfterSaveAndInViewer()
    }

    /**
     * The wide button shows an icon as well as its label, the icon inside the button and to the
     * left of the label.
     *
     * The icon has no content description (the label beside it names the action), and a button
     * merges everything inside it into one node of the semantics tree, so an icon leaves no trace
     * in the merged tree. It therefore carries a test tag of its own, and this test reads the
     * unmerged tree (`useUnmergedTree = true`), where every tagged node inside a button is still a
     * node of its own. It checks that the icon's node exists, is displayed, sits under the button,
     * is not the label's text node, lies inside the button's bounds and ends before the label's
     * text starts.
     */
    @Test
    fun the_wide_button_shows_an_icon_left_of_its_label() {
        show(CONTENT_URI)

        val button = compose.onNodeWithTag(SAVED_EDIT_ANOTHER_TAG)
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        // The icon, found by its own tag in the unmerged tree. hasAnyAncestor puts it under the
        // button; keyNotDefined(Text) makes sure the tag is not on the label instead.
        val icon = compose.onNodeWithTag(SAVED_EDIT_ANOTHER_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
            .assert(hasAnyAncestor(hasTestTag(SAVED_EDIT_ANOTHER_TAG)))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
            .fetchSemanticsNode()
            .boundsInRoot

        // The label's own text node inside the same button, also from the unmerged tree.
        val text = compose.onNode(
            hasText(app.getString(R.string.saved_edit_another)) and hasAnyAncestor(hasTestTag(SAVED_EDIT_ANOTHER_TAG)),
            useUnmergedTree = true,
        )
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue(
            "The icon at $icon is not inside its button at $button",
            icon.left >= button.left && icon.right <= button.right &&
                icon.top >= button.top && icon.bottom <= button.bottom,
        )
        assertTrue("The icon ends at ${icon.right} px, after its label starts at ${text.left} px", icon.right <= text.left)
    }

    /**
     * Share and the wide button stay above the bottom gesture zone even when the navigation bar
     * is 0 dp tall (POLICIES.md, the bottom-edge rule).
     *
     * The bottom gesture zone is the strip where a swipe up means "go home"; a tap inside it can
     * bring up the phone's own navigation instead of pressing the button. With the navigation bar
     * hidden, or with swipe gestures on and the gesture hint off (as on the owner's Galaxy A13),
     * the navigation bar's inset is 0 while the gesture zone is still there, so a row padded by
     * the navigation bar alone keeps only its own margin and sits inside the zone.
     *
     * Robolectric draws no system bars, so the test hands the view the screen is drawn in the
     * window insets such a phone would: a gesture zone [GESTURE_ZONE] tall at the bottom and every
     * other inset, the navigation bar's included, 0. Each button's bottom edge must then be at or
     * above the zone's top edge.
     *
     * The screen's own reading of the gesture zone is taken as well. If the insets never reached
     * it (a change in Robolectric or Compose), the test fails: without the zone it would prove
     * nothing, and a skipped test would only look like a pass.
     */
    @Test
    fun the_actions_clear_the_bottom_gesture_zone_when_the_navigation_bar_is_zero_tall() {
        var composeView: View? = null
        var seenZonePx = -1
        show(
            CONTENT_URI,
            probe = {
                // LocalView is the Android view the screen is drawn in, the one that receives the
                // window insets; the gesture zone is read here just as the screen reads it.
                val view = LocalView.current
                val zone = WindowInsets.systemGestures.getBottom(LocalDensity.current)
                // SideEffect hands the values out after each composition that succeeds.
                SideEffect {
                    composeView = view
                    seenZonePx = zone
                }
            },
        )

        val zonePx = with(compose.density) { GESTURE_ZONE.roundToPx() }
        // Only the gesture zone is set, so every other inset, the navigation bar's included, is 0.
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.systemGestures(), Insets.of(0, 0, 0, zonePx))
            .build()
        compose.runOnIdle { ViewCompat.dispatchApplyWindowInsets(checkNotNull(composeView), insets) }
        compose.waitForIdle()
        assertTrue(
            "The screen saw a gesture zone of $seenZonePx px, not the $zonePx px the test sent",
            seenZonePx == zonePx,
        )

        val zoneTop = bounds(SAVED_SCREEN_TAG).bottom - zonePx
        listOf(SAVED_SHARE_TAG, SAVED_EDIT_ANOTHER_TAG).forEach { tag ->
            val buttonBottom = bounds(tag).bottom
            assertTrue(
                "The button $tag ends at $buttonBottom px, inside the gesture zone that starts at $zoneTop px",
                buttonBottom <= zoneTop,
            )
        }
    }

    // --- The success mark and the "saved" line --------------------------------------------------

    /** Android 11: the mark's place, and its absence in the viewer (see [checkMarkPosition]). */
    @Test
    @Config(sdk = [30])
    fun the_success_mark_is_centred_in_the_top_quarter_and_never_in_the_viewer_on_android_11() {
        checkMarkPosition()
    }

    /** Android 15: the mark's place, and its absence in the viewer (see [checkMarkPosition]). */
    @Test
    @Config(sdk = [35])
    fun the_success_mark_is_centred_in_the_top_quarter_and_never_in_the_viewer_on_android_15() {
        checkMarkPosition()
    }

    /** Android 11: the success mark plays once and then rests (see [checkMarkPlaysOnce]). */
    @Test
    @Config(sdk = [30])
    fun the_success_mark_plays_once_and_then_rests_on_android_11() {
        checkMarkPlaysOnce()
    }

    /** Android 15: the success mark plays once and then rests (see [checkMarkPlaysOnce]). */
    @Test
    @Config(sdk = [35])
    fun the_success_mark_plays_once_and_then_rests_on_android_15() {
        checkMarkPlaysOnce()
    }

    /**
     * The line "Your photo is saved in Pictures/Kanso." shows right after a save, and never in the
     * photo viewer, where nothing was just saved.
     */
    @Test
    fun the_saved_line_shows_only_right_after_a_save() {
        val photo = SavedPhoto.Ready(solidPhoto(600, 800, Color.GRAY))
        var viewer by mutableStateOf(false)
        showContentWithPausedClock(state = { SavedUiState(photoUri = CONTENT_URI, photo = photo, viewer = viewer) })
        advance(SETTLE_MS)

        // The tagged node is the container of the line; its text is a child.
        compose.onNodeWithTag(SAVED_MESSAGE_TAG).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.saved_message)).assertIsDisplayed()

        update { viewer = true }

        compose.onAllNodesWithTag(SAVED_MESSAGE_TAG).assertCountEquals(0)
        compose.onAllNodes(hasText(app.getString(R.string.saved_message))).assertCountEquals(0)
    }

    /** The photo, named by its Uri and described for screen readers, the title and the message show. */
    @Test
    fun the_saved_photo_title_and_message_are_shown() {
        show(CONTENT_URI)
        waitForPhoto()

        compose.onNode(hasTestTag(SAVED_PHOTO_TAG) and SemanticsMatcher.expectValue(SAVED_PHOTO_URI_KEY, CONTENT_URI))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(app.getString(R.string.saved_photo_description)).assertIsDisplayed()
        compose.onNode(hasText(app.getString(R.string.saved_title))).assertIsDisplayed()
        compose.onNode(hasText(app.getString(R.string.saved_message))).assertIsDisplayed()
    }

    /**
     * DoD 9: while the saved photo is being decoded, the screen shows the Expressive loading
     * indicator, named for screen readers, and no photo; once the photo is decoded the indicator
     * is gone and the photo is shown.
     *
     * The indicator animates for as long as it is shown, so the test clock is paused before the
     * screen is shown: with the clock running by itself, every wait for the screen to settle would
     * wait for an animation that never ends. The stateless screen is fed a photo that the test
     * switches from loading to ready, as the screen's ViewModel does once its decode ends.
     */
    @Test
    fun a_loading_indicator_shows_while_the_photo_decodes_and_goes_once_it_is_ready() {
        var photo by mutableStateOf<SavedPhoto>(SavedPhoto.Loading)
        showContentWithPausedClock(state = { SavedUiState(photoUri = CONTENT_URI, photo = photo) })
        advance(500)

        val indicator = compose.onNodeWithTag(SAVED_LOADING_TAG).assertIsDisplayed()
        val names = indicator.fetchSemanticsNode().config
            .getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
        assertTrue("The loading indicator has no spoken name: $names", names.any { it.isNotBlank() })
        assertTrue(
            "The loading indicator is not named \"${app.getString(R.string.saved_loading)}\": $names",
            app.getString(R.string.saved_loading) in names,
        )
        compose.onAllNodesWithTag(SAVED_PHOTO_TAG).assertCountEquals(0)

        compose.runOnUiThread {
            photo = SavedPhoto.Ready(ToolFixtures.gradient(600, 400))
            // Tells Compose about the change at once, so the next frames draw the new state.
            Snapshot.sendApplyNotifications()
        }
        advance(500)

        compose.onAllNodesWithTag(SAVED_LOADING_TAG).assertCountEquals(0)
        compose.onNodeWithTag(SAVED_PHOTO_TAG).assertIsDisplayed()
    }

    // --- Share and the callbacks ----------------------------------------------------------------

    /**
     * Share starts the system chooser wrapping an image/jpeg ACTION_SEND that carries the photo
     * with a read grant, targets no app, and then calls onShare once.
     */
    @Test
    fun share_opens_the_chooser_for_the_photo() {
        var shares = 0
        show(CONTENT_URI, onShare = { shares++ })

        compose.onNodeWithTag(SAVED_SHARE_TAG).performClick()
        val chooser = waitForStartedActivity()

        assertChooserFor(chooser, Uri.parse(CONTENT_URI))
        compose.waitForIdle()
        compose.waitUntil(timeoutMillis = 5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            shares >= 1
        }
        assertEquals(1, shares)
    }

    /** On Android 9 the saved file's Uri never leaves the app: the share carries its content Uri. */
    @Test
    @Config(sdk = [28])
    fun share_on_android_9_sends_the_content_uri_of_the_saved_file() {
        val provider = FakeImagesProvider.install()
        provider.files[SAVED_PATH] = 42L
        show("file://$SAVED_PATH")

        compose.onNodeWithTag(SAVED_SHARE_TAG).performClick()
        val chooser = waitForStartedActivity()

        assertChooserFor(chooser, Uri.parse("content://media/external/images/media/42"))
    }

    /** Edit another calls onEditAnother, and nothing else. */
    @Test
    fun edit_another_calls_back() {
        var edits = 0
        var closes = 0
        show(CONTENT_URI, onEditAnother = { edits++ }, onClose = { closes++ })

        compose.onNodeWithTag(SAVED_EDIT_ANOTHER_TAG).performClick()
        compose.waitForIdle()

        assertEquals(1, edits)
        assertEquals(0, closes)
        assertTrue(startedIntents().none { it.action == Intent.ACTION_CHOOSER })
    }

    /** The round close button at the top left calls onClose, and nothing else. */
    @Test
    fun the_close_button_calls_on_close_and_nothing_else() {
        var edits = 0
        var closes = 0
        var shares = 0
        show(CONTENT_URI, onShare = { shares++ }, onEditAnother = { edits++ }, onClose = { closes++ })

        compose.onNodeWithTag(SAVED_CLOSE_TAG).performClick()
        compose.waitForIdle()

        assertEquals(1, closes)
        assertEquals(0, edits)
        assertEquals(0, shares)
        assertTrue(startedIntents().none { it.action == Intent.ACTION_CHOOSER })
    }

    // --- The checks each Android version runs ----------------------------------------------------

    /**
     * Shows a portrait 600 x 800 photo, then a landscape 600 x 400 one, both built here, and checks
     * for each: the blurred backdrop covers the whole window; the photo lies inside the window;
     * it touches two opposite edges of the window (fills its width or its height, within 1 dp) and
     * keeps its own shape (its width over its height is the bitmap's, within 1 %).
     */
    private fun checkBackdropAndPhoto() {
        var photo by mutableStateOf<SavedPhoto>(SavedPhoto.Ready(solidPhoto(600, 800, Color.GRAY)))
        showContentWithPausedClock(state = { SavedUiState(photoUri = CONTENT_URI, photo = photo) })
        advance(SETTLE_MS)
        checkPhotoFits(600, 800)

        update { photo = SavedPhoto.Ready(solidPhoto(600, 400, Color.GRAY)) }
        checkPhotoFits(600, 400)
    }

    /** The checks of [checkBackdropAndPhoto] for the photo of [width] by [height] pixels on screen now. */
    private fun checkPhotoFits(width: Int, height: Int) {
        val tolerance = px(1.dp)
        val screen = bounds(SAVED_SCREEN_TAG)
        val backdrop = bounds(SAVED_BACKDROP_TAG)
        val photo = bounds(SAVED_PHOTO_TAG)
        val label = "${width}x$height"

        assertTrue(
            "$label: the backdrop $backdrop does not cover the window $screen",
            backdrop.left <= screen.left + tolerance && backdrop.top <= screen.top + tolerance &&
                backdrop.right >= screen.right - tolerance && backdrop.bottom >= screen.bottom - tolerance,
        )
        assertTrue(
            "$label: the photo $photo is not inside the window $screen",
            photo.left >= screen.left - tolerance && photo.top >= screen.top - tolerance &&
                photo.right <= screen.right + tolerance && photo.bottom <= screen.bottom + tolerance,
        )
        val fillsWidth = abs(photo.width - screen.width) <= tolerance
        val fillsHeight = abs(photo.height - screen.height) <= tolerance
        assertTrue("$label: the photo $photo touches neither pair of edges of the window $screen", fillsWidth || fillsHeight)

        val shown = photo.width / photo.height
        val own = width.toFloat() / height.toFloat()
        assertTrue("$label: the photo is drawn at $shown, not at its own shape $own", abs(shown - own) <= own * 0.01f)
    }

    /**
     * After a save, then in the viewer: the round close button is shown, named "Close", at least
     * 48 dp each way and left of the title; the title reads "Saved" after a save and "My edits" in
     * the viewer; the shade starts at the window's top, spans its width and is 120 dp tall plus
     * the status bar (whose height the screen reads from the window insets, read here the same way).
     */
    private fun checkTopRowAndShadeGeometry() {
        var viewer by mutableStateOf(false)
        var statusBarPx = -1
        val photo = SavedPhoto.Ready(solidPhoto(600, 800, Color.GRAY))
        showContentWithPausedClock(
            state = { SavedUiState(photoUri = CONTENT_URI, photo = photo, viewer = viewer) },
            probe = {
                val top = WindowInsets.statusBars.getTop(LocalDensity.current)
                SideEffect { statusBarPx = top }
            },
        )
        advance(SETTLE_MS)
        assertTrue("The status bar's height was never read", statusBarPx >= 0)

        checkTopRow(app.getString(R.string.saved_title))
        update { viewer = true }
        checkTopRow(app.getString(R.string.saved_layout_viewer_title))

        val tolerance = px(1.dp)
        val screen = bounds(SAVED_SCREEN_TAG)
        val scrim = bounds(SAVED_SCRIM_TAG)
        val expectedHeight = px(SCRIM_HEIGHT) + statusBarPx
        assertTrue("The shade starts at ${scrim.top} px, not at the window's top ${screen.top} px", abs(scrim.top - screen.top) <= tolerance)
        assertTrue(
            "The shade $scrim does not span the window's width $screen",
            abs(scrim.left - screen.left) <= tolerance && abs(scrim.right - screen.right) <= tolerance,
        )
        assertTrue(
            "The shade is ${scrim.height} px tall, not 120 dp plus the status bar ($expectedHeight px)",
            abs(scrim.height - expectedHeight) <= tolerance,
        )
    }

    /** Checks the close button and that the title on screen now reads [title] (see [checkTopRowAndShadeGeometry]). */
    private fun checkTopRow(title: String) {
        val close = compose.onNodeWithTag(SAVED_CLOSE_TAG)
            .assertIsDisplayed()
            .assert(hasContentDescription(app.getString(R.string.saved_layout_close)))
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .fetchSemanticsNode()
            .boundsInRoot
        val titleBounds = compose.onNodeWithTag(SAVED_TITLE_TAG)
            .assertIsDisplayed()
            .assert(hasText(title))
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue("The close button $close is not left of the title $titleBounds", close.right <= titleBounds.left)
        val screen = bounds(SAVED_SCREEN_TAG)
        assertTrue("The close button $close is not in the top quarter of the window", close.bottom <= screen.top + screen.height / 4f)
    }

    /**
     * The shade, in pixels, in light and then in dark, in the viewer (so no success mark is drawn
     * near the sampled column). Each mode gets a tall photo (300 x 900, narrower in shape than the
     * window, so it fills the window's height and lies under the shade's top) in the colour that
     * contrasts with the mode: black in light, white in dark.
     *
     * In the window's middle column, the pixel just below the shade's bottom edge is the bare
     * photo. The pixel [SHADE_SAMPLE_DEPTH] below the window's top must equal the theme's `surface`
     * laid over that photo pixel at alpha [SHADE_ALPHA], within [SHADE_TOLERANCE] in each channel,
     * and its relative luminance must differ from the bare photo pixel's by at least
     * [MIN_LUMINANCE_GAP], so the title and the status bar read on the shade.
     */
    private fun checkShadeColour() {
        var mode by mutableStateOf(ThemeMode.Light)
        var photo by mutableStateOf<SavedPhoto>(SavedPhoto.Ready(solidPhoto(300, 900, Color.BLACK)))
        var surface = 0
        showContentWithPausedClock(
            state = { SavedUiState(photoUri = CONTENT_URI, photo = photo, viewer = true) },
            mode = { mode },
            probe = {
                val colour = MaterialTheme.colorScheme.surface.toArgb()
                SideEffect { surface = colour }
            },
        )
        advance(SETTLE_MS)
        checkShadeOver(ThemeMode.Light, surface, Color.BLACK)

        update {
            mode = ThemeMode.Dark
            photo = SavedPhoto.Ready(solidPhoto(300, 900, Color.WHITE))
        }
        checkShadeOver(ThemeMode.Dark, surface, Color.WHITE)
    }

    /** The pixel checks of [checkShadeColour] in [mode], with the theme's [surface] over a photo of [photoColour]. */
    private fun checkShadeOver(mode: ThemeMode, surface: Int, photoColour: Int) {
        val screen = bounds(SAVED_SCREEN_TAG)
        val scrim = bounds(SAVED_SCRIM_TAG)
        val photoBounds = bounds(SAVED_PHOTO_TAG)
        assertTrue("$mode: the photo $photoBounds does not reach the window's top", photoBounds.top <= screen.top + px(1.dp))
        val x = screen.center.x.toInt()
        val shadedY = (screen.top + px(SHADE_SAMPLE_DEPTH)).toInt()

        drawn { bitmap ->
            // The first row below the shade: the bare photo.
            val belowY = scrim.bottom.roundToInt().coerceAtMost(bitmap.height - 1)
            val bare = bitmap.getPixel(x, belowY)
            val shaded = bitmap.getPixel(x, shadedY)
            assertTrue(
                "$mode: below the shade the photo is ${hex(bare)}, not its own colour ${hex(photoColour)}",
                channelDistance(bare, photoColour) <= SHADE_TOLERANCE,
            )
            val expected = blend(surface, bare, SHADE_ALPHA)
            assertTrue(
                "$mode: the shade is ${hex(shaded)}, not surface ${hex(surface)} at $SHADE_ALPHA over ${hex(bare)} = ${hex(expected)}",
                channelDistance(shaded, expected) <= SHADE_TOLERANCE,
            )
            val gap = abs(relativeLuminance(shaded) - relativeLuminance(bare))
            assertTrue("$mode: the shade's luminance differs from the photo's by only $gap", gap >= MIN_LUMINANCE_GAP)
        }
    }

    /** Checks the bottom row after a save ("Edit another"), then in the viewer ("Edit"); see [checkBottomRow]. */
    private fun checkBottomRowAfterSaveAndInViewer() {
        var viewer by mutableStateOf(false)
        val photo = SavedPhoto.Ready(solidPhoto(600, 800, Color.GRAY))
        showContentWithPausedClock(state = { SavedUiState(photoUri = CONTENT_URI, photo = photo, viewer = viewer) })
        advance(SETTLE_MS)
        checkBottomRow(app.getString(R.string.saved_edit_another))

        update { viewer = true }
        checkBottomRow(app.getString(R.string.saved_layout_viewer_edit))
    }

    /**
     * The bottom row on screen now: Share is round (as wide as tall, at least 48 dp), shows only an
     * icon (no text) and is named "Share"; the wide button reads [wideLabel], is at least 48 dp
     * tall and at least twice Share's width; Share is left of it without overlapping; their
     * vertical centres are within 1 dp; and together they span the window's width less a 16 dp
     * margin at each side (within 1 dp), never leaving it.
     */
    private fun checkBottomRow(wideLabel: String) {
        val tolerance = px(1.dp)
        val screen = bounds(SAVED_SCREEN_TAG)
        val share = compose.onNodeWithTag(SAVED_SHARE_TAG)
            .assertIsDisplayed()
            .assert(hasContentDescription(app.getString(R.string.saved_share)))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
            .assertHeightIsAtLeast(48.dp)
            .fetchSemanticsNode()
            .boundsInRoot
        val wide = compose.onNodeWithTag(SAVED_EDIT_ANOTHER_TAG)
            .assertIsDisplayed()
            .assert(hasText(wideLabel))
            .assertHeightIsAtLeast(48.dp)
            .fetchSemanticsNode()
            .boundsInRoot

        assertTrue("Share $share is not round: ${share.width} x ${share.height} px", abs(share.width - share.height) <= tolerance)
        assertTrue("Share $share does not end before the wide button $wide starts", share.right <= wide.left)
        assertTrue(
            "The wide button is ${wide.width} px wide, less than twice Share's ${share.width} px",
            wide.width >= share.width * 2f,
        )
        val spread = abs(share.center.y - wide.center.y)
        assertTrue("Share's and the wide button's centres are $spread px apart, more than 1 dp", spread <= tolerance)

        assertTrue("The row leaves the window: $share, $wide in $screen", share.left >= screen.left && wide.right <= screen.right)
        val span = wide.right - share.left
        val expectedSpan = screen.width - 2 * px(ROW_MARGIN)
        assertTrue("The row spans $span px, not the window's width less its margins ($expectedSpan px)", abs(span - expectedSpan) <= tolerance)
    }

    /**
     * After a save, with a portrait 600 x 800 photo and then a landscape 600 x 400 one: the
     * success mark is shown, named "Photo saved", its centre within 1 dp of the window's middle
     * column and in the top quarter of the window. Then in the viewer there is no mark at all.
     */
    private fun checkMarkPosition() {
        var photo by mutableStateOf<SavedPhoto>(SavedPhoto.Ready(solidPhoto(600, 800, Color.GRAY)))
        var viewer by mutableStateOf(false)
        showContentWithPausedClock(state = { SavedUiState(photoUri = CONTENT_URI, photo = photo, viewer = viewer) })
        advance(SETTLE_MS)
        checkMarkPlace("600x800")

        update { photo = SavedPhoto.Ready(solidPhoto(600, 400, Color.GRAY)) }
        checkMarkPlace("600x400")

        update { viewer = true }
        compose.onAllNodesWithTag(SAVED_MARK_TAG).assertCountEquals(0)
        compose.onAllNodes(hasContentDescription(app.getString(R.string.saved_mark_description))).assertCountEquals(0)
    }

    /** The place checks of [checkMarkPosition] for the photo named [label]. */
    private fun checkMarkPlace(label: String) {
        val screen = bounds(SAVED_SCREEN_TAG)
        val mark = compose.onNodeWithTag(SAVED_MARK_TAG)
            .assertIsDisplayed()
            .assert(hasContentDescription(app.getString(R.string.saved_mark_description)))
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(
            "$label: the mark's centre is at x ${mark.center.x} px, not the window's middle ${screen.center.x} px",
            abs(mark.center.x - screen.center.x) <= px(1.dp),
        )
        assertTrue("$label: the mark $mark starts above the window", mark.top >= screen.top)
        assertTrue(
            "$label: the mark's centre is at y ${mark.center.y} px, below the window's top quarter",
            mark.center.y - screen.top <= screen.height / 4f,
        )
    }

    /**
     * DoD 7: the success mark plays once when the screen appears, and then rests.
     *
     * The stateless screen is shown with its photo already decoded, so nothing else changes in
     * the mark's area, and the test clock is paused, so time moves only when the test moves it.
     * The node tagged [SAVED_MARK_TAG] must carry its content description. It is drawn into a
     * bitmap at five moments: at 0 ms (its first frame, where an animation still has its starting
     * value), at 250 ms, at 1 000 ms, at 1 500 ms and at 3 000 ms. The pictures at 0, 250 and
     * 1 000 ms must all differ from one another (it moves), and the pictures at 1 500 and 3 000 ms
     * must be identical, pixel for pixel (it has settled and does not loop).
     *
     * With the app's expressive motion the mark grows for about 0.3 s and then changes shape for
     * about 0.45 s, so at 0 ms nothing is drawn yet, at 250 ms it is a circle close to its full
     * size, and from about 0.8 s on it is the finished cookie, which no longer moves.
     */
    private fun checkMarkPlaysOnce() {
        // Made once, outside the screen, so every frame shows the very same photo.
        val photo = SavedPhoto.Ready(ToolFixtures.gradient(600, 400))
        showContentWithPausedClock(state = { SavedUiState(photoUri = CONTENT_URI, photo = photo) })

        compose.onNodeWithTag(SAVED_MARK_TAG)
            .assertIsDisplayed()
            .assert(hasContentDescription(app.getString(R.string.saved_mark_description)))
        // The photo is ready, so the screen holds the photo and no loading indicator.
        compose.onAllNodesWithTag(SAVED_LOADING_TAG).assertCountEquals(0)

        val atStart = pictureOf(SAVED_MARK_TAG)
        advance(250)
        val at250 = pictureOf(SAVED_MARK_TAG)
        advance(750)
        val at1000 = pictureOf(SAVED_MARK_TAG)
        advance(500)
        val at1500 = pictureOf(SAVED_MARK_TAG)
        advance(1_500)
        val at3000 = pictureOf(SAVED_MARK_TAG)

        assertFalse("The mark looks the same at 0 ms and at 250 ms", atStart.contentEquals(at250))
        assertFalse("The mark looks the same at 250 ms and at 1 000 ms", at250.contentEquals(at1000))
        assertFalse("The mark looks the same at 0 ms and at 1 000 ms", atStart.contentEquals(at1000))
        assertTrue("The mark was still changing between 1 500 ms and 3 000 ms", at1500.contentEquals(at3000))
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Shows the Saved screen for [photoUri]. Its ViewModel is built here, before the screen is
     * drawn (lint forbids building a ViewModel while drawing), with a fake loader that hands
     * back a 600 x 400 gradient for any Uri. [probe] is composed next to the screen, for a test
     * that needs to read something only a composable can (it draws nothing).
     */
    private fun show(
        photoUri: String,
        onShare: () -> Unit = {},
        onEditAnother: () -> Unit = {},
        onClose: () -> Unit = {},
        probe: @Composable () -> Unit = {},
    ) {
        val viewModel = SavedViewModel(
            photoUri = photoUri,
            loader = FakePhotoLoader(app, ToolFixtures.gradient(600, 400)),
            probe = ViewModelClearedProbe(),
        )
        compose.setContent {
            probe()
            MaterialTheme {
                SavedScreen(
                    viewModel = viewModel,
                    onShare = onShare,
                    onEditAnother = onEditAnother,
                    onClose = onClose,
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(SAVED_SCREEN_TAG).assertExists()
    }

    /**
     * Pauses the test clock, then shows the stateless Saved screen in KansoTheme, in the mode
     * [mode] returns (light unless a test says otherwise), drawing the state [state] returns. Both
     * are read while the screen is drawn, so a test that returns Compose state from them can change
     * the screen later (see [update]). [probe] is composed inside the theme, next to the screen,
     * for a test that needs to read something only a composable can, such as a theme colour (it
     * draws nothing). No ViewModel and no loader are involved; the taps do nothing.
     *
     * The clock is paused before anything is shown, so time moves only through [advance]: the
     * loading indicator never stops by itself, and the success mark must be pictured at exact
     * moments. Then exactly one frame is run, so the screen is composed and laid out while the
     * mark is still at the very start of its animation (an animation's first frame draws its
     * starting value).
     */
    private fun showContentWithPausedClock(
        state: () -> SavedUiState,
        mode: () -> ThemeMode = { ThemeMode.Light },
        probe: @Composable () -> Unit = {},
    ) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KansoTheme(themeMode = mode()) {
                probe()
                SavedContent(
                    state = state(),
                    sharing = false,
                    onShare = {},
                    onEditAnother = {},
                    onClose = {},
                )
            }
        }
        compose.mainClock.advanceTimeByFrame()
        shadowOf(Looper.getMainLooper()).idle()
        compose.onNodeWithTag(SAVED_SCREEN_TAG).assertExists()
    }

    /**
     * Runs [change] (which sets Compose state the shown screen reads) on the main thread, tells
     * Compose about it at once, then moves the paused clock on by [SETTLE_MS] so the screen is
     * drawn again with the new state and any animation it starts has come to rest.
     */
    private fun update(change: () -> Unit) {
        compose.runOnUiThread {
            change()
            // Tells Compose about the change at once, so the next frames draw the new state.
            Snapshot.sendApplyNotifications()
        }
        advance(SETTLE_MS)
    }

    /**
     * Moves the paused clock forward by [millis], one frame after another, and runs whatever the
     * main thread was asked to do meanwhile.
     */
    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** The bounds, in the root's pixels, of the node tagged [tag] in the merged semantics tree. */
    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    /** [value] in pixels at the test's screen density. */
    private fun px(value: Dp): Float = with(compose.density) { value.toPx() }

    /**
     * Draws the activity's content view into a fresh bitmap, hands it to [read], then frees it.
     * The Compose root fills the content view from its top-left corner, so node bounds in the root
     * are pixel positions in the bitmap.
     *
     * It draws the content view itself, as `ScreenPixels.kt` and `ToolBackdropTest` do, because
     * Compose's captureToImage waits for the window to redraw and that wait times out under
     * Robolectric here. Under Robolectric the test thread is the main thread, so drawing here is
     * allowed.
     */
    private fun <T> drawn(read: (Bitmap) -> T): T {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        val view = content.getChildAt(0) ?: error("The activity has no content view yet")
        check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        try {
            view.draw(Canvas(bitmap))
            return read(bitmap)
        } finally {
            // A bitmap holds native memory; free it now rather than waiting for the collector.
            bitmap.recycle()
        }
    }

    /**
     * Returns the drawn pixels inside the node tagged [tag], row by row. The node is not in a
     * scrolling list, so its bounds are never clipped; they are kept inside the bitmap all the same.
     */
    private fun pictureOf(tag: String): IntArray {
        val area = bounds(tag)
        return drawn { bitmap ->
            val left = area.left.toInt().coerceIn(0, bitmap.width - 1)
            val top = area.top.toInt().coerceIn(0, bitmap.height - 1)
            val right = area.right.toInt().coerceIn(left + 1, bitmap.width)
            val bottom = area.bottom.toInt().coerceIn(top + 1, bitmap.height)
            val width = right - left
            val height = bottom - top
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, left, top, width, height)
            pixels
        }
    }

    /** A photo [width] by [height] pixels painted all in one [colour] (an ARGB int). */
    private fun solidPhoto(width: Int, height: Int, colour: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(colour) }

    /**
     * The colour [top] laid over the opaque colour [bottom] at opacity [alpha], channel by channel,
     * as the screen's canvas blends a translucent paint over what is already drawn.
     */
    private fun blend(top: Int, bottom: Int, alpha: Double): Int = Color.rgb(
        (Color.red(top) * alpha + Color.red(bottom) * (1 - alpha)).roundToInt(),
        (Color.green(top) * alpha + Color.green(bottom) * (1 - alpha)).roundToInt(),
        (Color.blue(top) * alpha + Color.blue(bottom) * (1 - alpha)).roundToInt(),
    )

    /** The largest difference between [a] and [b] in any of red, green and blue. */
    private fun channelDistance(a: Int, b: Int): Int = maxOf(
        abs(Color.red(a) - Color.red(b)),
        abs(Color.green(a) - Color.green(b)),
        abs(Color.blue(a) - Color.blue(b)),
    )

    /**
     * The relative luminance of the colour [argb], as WCAG defines it: how bright the colour looks,
     * from 0 (black) to 1 (white), weighting green most and blue least, as the eye does.
     */
    private fun relativeLuminance(argb: Int): Double =
        0.2126 * linear(Color.red(argb)) + 0.7152 * linear(Color.green(argb)) + 0.0722 * linear(Color.blue(argb))

    /**
     * Turns one sRGB [channel] (0 to 255) into linear light (0 to 1): screen values are stored
     * gamma-encoded, and luminance is a sum of linear light.
     */
    private fun linear(channel: Int): Double {
        val s = channel / 255.0
        return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    /** [argb] as `#RRGGBB`, for failure messages. */
    private fun hex(argb: Int): String = String.format("#%06X", argb and 0xFFFFFF)

    /**
     * Waits until the photo is on screen. The ViewModel's load ends on the main thread, so the
     * main thread's queue is run while waiting.
     */
    private fun waitForPhoto() {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodes(hasTestTag(SAVED_PHOTO_TAG)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Waits until the screen starts the chooser, and returns its intent. The share Uri is looked
     * up on a background thread, so the main thread's queue is run while waiting. Any other
     * started activity (such as the host activity itself) is skipped.
     */
    private fun waitForStartedActivity(): Intent {
        var chooser: Intent? = null
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            chooser = startedIntents().firstOrNull { it.action == Intent.ACTION_CHOOSER }
            chooser != null
        }
        return chooser!!
    }

    /** Takes every activity start recorded so far, oldest first. */
    private fun startedIntents(): List<Intent> =
        generateSequence { shadowOf(app).nextStartedActivity }.toList()

    /**
     * Checks that [chooser] is the system chooser wrapping an image/jpeg ACTION_SEND of [uri],
     * with the read grant on both intents and the Uri in their ClipData, and no app chosen.
     */
    private fun assertChooserFor(chooser: Intent, uri: Uri) {
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertNull(chooser.`package`)
        assertNull(chooser.component)
        assertTrue(chooser.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(app.getString(R.string.saved_share_chooser), chooser.getCharSequenceExtra(Intent.EXTRA_TITLE)?.toString())

        val send = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)
        assertNotNull("The chooser wraps no intent", send)
        send!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("image/jpeg", send.type)
        assertEquals(uri, IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java))
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertNull(send.`package`)
        assertNull(send.component)
        assertEquals(uri, send.clipData?.getItemAt(0)?.uri)
        assertEquals(uri, chooser.clipData?.getItemAt(0)?.uri)
    }

    /** Values shared by the tests. */
    private companion object {
        /** A saved photo as Android 10 and later return it. */
        const val CONTENT_URI = "content://media/external/images/media/7"

        /** Where Android 9's Save puts a photo. */
        const val SAVED_PATH = "/storage/emulated/0/Pictures/Kanso/Kanso_20261007_120000.jpg"

        /**
         * The height of the bottom gesture zone the gesture test hands the screen: more than the
         * bottom row's own margin, so a row padded by the navigation bar alone would end inside it.
         */
        val GESTURE_ZONE = 40.dp

        /** How long a layout test moves the paused clock on, so every animation (the mark's) rests. */
        const val SETTLE_MS = 2_000L

        /** The shade's height below the status bar, as the DoD gives it. */
        val SCRIM_HEIGHT = 120.dp

        /** The margin the bottom row keeps from each side of the window. */
        val ROW_MARGIN = 16.dp

        /** How far below the window's top the shade's colour is sampled: inside its full-strength band. */
        val SHADE_SAMPLE_DEPTH = 4.dp

        /** The shade's opacity at its top, as the DoD gives it. */
        const val SHADE_ALPHA = 0.85

        /** How far, in any channel, a drawn pixel may be from the expected colour. */
        const val SHADE_TOLERANCE = 3

        /** The least difference in relative luminance between the shade and the bare photo. */
        const val MIN_LUMINANCE_GAP = 0.2
    }
}
