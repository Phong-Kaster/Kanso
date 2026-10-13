package com.phongkaster.kanso.a11y

import android.Manifest
import android.app.Application
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.camera.FakeCameraController
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.media.SavedPhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.gallery.GalleryGridTest
import com.phongkaster.kanso.myedits.FakeSavedPhotoSource
import com.phongkaster.kanso.navigation.KANSO_CAMERA_BUTTON_TAG
import com.phongkaster.kanso.navigation.KANSO_TAB_BAR_TAG
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.navigation.openTab
import com.phongkaster.kanso.navigation.openToolGallery
import com.phongkaster.kanso.navigation.tapAboveBar
import com.phongkaster.kanso.navigation.tapCameraButton
import com.phongkaster.kanso.navigation.waitForHome
import com.phongkaster.kanso.save.FakePhotoSaver
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.galleryPhotoTag
import com.phongkaster.kanso.ui.home.HOME_RECENT_TAG
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.home.homeRecentTag
import com.phongkaster.kanso.ui.myedits.MY_EDITS_GRID_TAG
import com.phongkaster.kanso.ui.myedits.MY_EDITS_SCREEN_TAG
import com.phongkaster.kanso.ui.myedits.myEditsTileTag
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import com.phongkaster.kanso.ui.saved.SAVED_EDIT_ANOTHER_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SCREEN_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SHARE_TAG
import com.phongkaster.kanso.ui.settings.CUSTOM_COLOUR_PICKER_TAG
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_COLOUR_CUSTOM_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SAVE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import kotlin.math.max
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * DoD 26: nothing sits under the floating bar or in the phone's gesture area.
 *
 * The whole app is walked (MainActivity on Android 15) on the phone (411 x 891 dp) and on the
 * small phone (360 x 780 dp), each with (i) no navigation bar and a 32 dp bottom gesture area
 * (swipe gestures with the hint hidden) and (ii) a 48 dp three-button navigation bar. Robolectric
 * draws no system bars, so the test sends those insets to the app's Compose view itself, as the
 * system would ([sendInsets]), with `ViewCompat.dispatchApplyWindowInsets` on the view `LocalView`
 * names inside the app. They are sent again on every screen the walk reaches, and each time the test
 * checks the screen received them: one of its bottom controls must move up by the inset sent.
 *
 * Two kinds of check:
 * - The bottom controls end at least the larger of the two insets above the window's bottom, so no
 *   tap lands where the system listens for "go home": the floating bar (`nav-bar`) and its Camera
 *   button (`nav-camera`) on Home, My edits and Settings; Save (`tool-save`) on every tool's
 *   screen; Share and Edit (`saved-share`, `saved-edit-another`) on the Saved screen and on the
 *   photo opened from My edits; the live camera's shutter.
 * - Scrolled to its end, a tab screen's last item ends at or above the top of the floating bar:
 *   Home's last recent card, My edits' 40th tile (with 40 saved photos), and Settings' colour
 *   picker card (with Custom chosen).
 *
 * Positions are read from `positionInRoot` plus the node's size, never `boundsInRoot`, which
 * Compose clips to the visible part of a scrolled node. The fakes are loaded before the app starts
 * (`createEmptyComposeRule`, `loadKoinModules`, then `ActivityScenario.launch`): Home's and My
 * edits' ViewModels keep the sources they were built with.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class BottomClearanceTest {

    /** Finds Compose nodes in the activity the test starts. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** The navigation bar's height the current walk sends. */
    private var navigationBar: Dp = 0.dp

    /** The bottom gesture area's height the current walk sends. */
    private var gestures: Dp = 0.dp

    /** Stops Koin: its global state outlives the test inside the same JVM. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /** The phone, no navigation bar and a 32 dp bottom gesture area. */
    @Test
    @Config(qualifiers = PHONE)
    fun phone_with_a_gesture_area_keeps_everything_clear() {
        walk(navigationBar = 0.dp, gestures = GESTURE_AREA)
    }

    /** The phone, a 48 dp navigation bar. */
    @Test
    @Config(qualifiers = PHONE)
    fun phone_with_a_navigation_bar_keeps_everything_clear() {
        walk(navigationBar = NAVIGATION_BAR, gestures = NAVIGATION_BAR)
    }

    /** The small phone, no navigation bar and a 32 dp bottom gesture area. */
    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun small_phone_with_a_gesture_area_keeps_everything_clear() {
        walk(navigationBar = 0.dp, gestures = GESTURE_AREA)
    }

    /** The small phone, a 48 dp navigation bar. */
    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun small_phone_with_a_navigation_bar_keeps_everything_clear() {
        walk(navigationBar = NAVIGATION_BAR, gestures = NAVIGATION_BAR)
    }

    // --- The walk ------------------------------------------------------------------------------

    /**
     * Starts the app with the fakes, sends a [navigationBar] tall navigation bar and a [gestures]
     * tall bottom gesture area, and checks Home, My edits and its photo viewer, Settings, the live
     * camera, every tool screen and the Saved screen (see the class comment).
     */
    private fun walk(navigationBar: Dp, gestures: Dp) {
        this.navigationBar = navigationBar
        this.gestures = gestures
        loadFakes()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            checkHome(scenario)
            checkMyEditsAndViewer(scenario)
            checkSettings(scenario)
            checkCamera(scenario)
            checkToolsAndSaved(scenario)
        }
    }

    /** Home: the bar and Camera button clear the insets; the last recent card scrolls above the bar. */
    private fun checkHome(scenario: ActivityScenario<MainActivity>) {
        waitForHome(compose)
        sendInsets(scenario, probeTag = KANSO_TAB_BAR_TAG)
        assertBarClearsInsets("Home")

        val last = HomeViewModel.RECENT_LIMIT - 1
        waitFor { count(homeRecentTag(0)) > 0 }
        compose.onNodeWithTag(HOME_RECENT_TAG).performScrollTo()
        compose.onNodeWithTag(HOME_RECENT_TAG).performScrollToIndex(last)
        compose.waitForIdle()
        scrollToEnd("Home", HOME_RECENT_TAG)
        assertAboveBar("Home's last recent card", homeRecentTag(last))
    }

    /**
     * My edits: the bar and Camera button clear the insets; the 40th tile scrolls above the bar.
     * The tile then opens the photo viewer, whose Share and Edit clear the insets; Back returns.
     */
    private fun checkMyEditsAndViewer(scenario: ActivityScenario<MainActivity>) {
        openTab(compose, KansoTab.MY_EDITS)
        waitFor { count(MY_EDITS_SCREEN_TAG) > 0 && count(myEditsTileTag(0)) > 0 }
        sendInsets(scenario, probeTag = KANSO_TAB_BAR_TAG)
        assertBarClearsInsets("My edits")

        val last = EDIT_COUNT - 1
        compose.onNodeWithTag(MY_EDITS_GRID_TAG).performScrollToIndex(last)
        compose.waitForIdle()
        scrollToEnd("My edits", MY_EDITS_GRID_TAG)
        assertAboveBar("My edits' 40th tile", myEditsTileTag(last))

        compose.onNodeWithTag(myEditsTileTag(last)).tapAboveBar()
        waitFor { count(SAVED_SCREEN_TAG) > 0 && count(KANSO_TAB_BAR_TAG) == 0 && count(SAVED_SHARE_TAG) > 0 }
        sendInsets(scenario, probeTag = SAVED_SHARE_TAG)
        assertClearsInsets("The viewer's Share", SAVED_SHARE_TAG)
        assertClearsInsets("The viewer's Edit", SAVED_EDIT_ANOTHER_TAG)

        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor { count(SAVED_SCREEN_TAG) == 0 && count(MY_EDITS_SCREEN_TAG) > 0 }
    }

    /**
     * Settings: the bar and Camera button clear the insets; with Custom chosen, the colour picker
     * card scrolls above the bar.
     */
    private fun checkSettings(scenario: ActivityScenario<MainActivity>) {
        openTab(compose, KansoTab.SETTINGS)
        waitFor { count(SETTINGS_SCREEN_TAG) > 0 && count(MY_EDITS_SCREEN_TAG) == 0 }
        sendInsets(scenario, probeTag = KANSO_TAB_BAR_TAG)
        assertBarClearsInsets("Settings")

        compose.onNodeWithTag(THEME_COLOUR_CUSTOM_TAG).tapAboveBar()
        waitFor { count(CUSTOM_COLOUR_PICKER_TAG) > 0 }
        compose.waitForIdle()
        scrollToEnd("Settings", CUSTOM_COLOUR_PICKER_TAG)
        assertAboveBar("Settings' colour picker card", CUSTOM_COLOUR_PICKER_TAG)
    }

    /** The live camera, opened with the Camera button: its shutter clears the insets. */
    private fun checkCamera(scenario: ActivityScenario<MainActivity>) {
        tapCameraButton(compose)
        waitFor {
            compose.onAllNodes(hasTestTag(CAMERA_SHUTTER_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        sendInsets(scenario, probeTag = CAMERA_SHUTTER_TAG)
        assertClearsInsets("The camera's shutter", CAMERA_SHUTTER_TAG)

        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor { count(CAMERA_SHUTTER_TAG) == 0 && count(KANSO_TAB_BAR_TAG) > 0 }
    }

    /**
     * Every tool's screen, each opened from Home through its gallery on photo 1: Save clears the
     * insets. On [SAVE_TOOL] Save is pressed, and the Saved screen's Share and "Edit another"
     * clear the insets too. Each screen is left with Back, back to Home.
     */
    private fun checkToolsAndSaved(scenario: ActivityScenario<MainActivity>) {
        openTab(compose, KansoTab.HOME)
        Tool.entries.forEach { tool ->
            openToolGallery(compose, tool)
            waitFor { count(GALLERY_SCREEN_TAG) > 0 && count(galleryPhotoTag(galleryPhotos.first().id)) > 0 }
            compose.onNodeWithTag(galleryPhotoTag(galleryPhotos.first().id)).performClick()
            waitFor { count(GALLERY_SCREEN_TAG) == 0 && count(TOOL_IMAGE_TAG) > 0 && count(TOOL_SAVE_TAG) > 0 }
            sendInsets(scenario, probeTag = TOOL_SAVE_TAG)
            assertClearsInsets("${tool.name}'s Save", TOOL_SAVE_TAG)

            if (tool == SAVE_TOOL) {
                waitFor { compose.onAllNodes(hasTestTag(TOOL_SAVE_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
                waitFor { count(SAVED_SCREEN_TAG) > 0 && count(TOOL_SCREEN_TAG) == 0 && count(SAVED_SHARE_TAG) > 0 }
                sendInsets(scenario, probeTag = SAVED_SHARE_TAG)
                assertClearsInsets("The Saved screen's Share", SAVED_SHARE_TAG)
                assertClearsInsets("The Saved screen's Edit another", SAVED_EDIT_ANOTHER_TAG)
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                waitFor { count(SAVED_SCREEN_TAG) == 0 && count(HOME_SCREEN_TAG) > 0 }
            } else {
                // Back to the gallery, then back to Home.
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                waitFor { count(TOOL_SCREEN_TAG) == 0 && count(GALLERY_SCREEN_TAG) > 0 }
                scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                waitFor { count(GALLERY_SCREEN_TAG) == 0 && count(HOME_SCREEN_TAG) > 0 }
            }
        }
    }

    // --- Checks --------------------------------------------------------------------------------

    /** Checks that the floating bar and the Camera button beside it both clear the insets on [screen]. */
    private fun assertBarClearsInsets(screen: String) {
        waitFor { count(KANSO_TAB_BAR_TAG) > 0 && count(KANSO_CAMERA_BUTTON_TAG) > 0 }
        assertClearsInsets("$screen's floating bar", KANSO_TAB_BAR_TAG)
        assertClearsInsets("$screen's Camera button", KANSO_CAMERA_BUTTON_TAG)
    }

    /**
     * Checks that the node tagged [tag] ends at least the larger of the navigation bar and the
     * bottom gesture area above the window's bottom edge.
     */
    private fun assertClearsInsets(what: String, tag: String) {
        val windowBottom = compose.onRoot().fetchSemanticsNode().size.height.toFloat()
        val inset = with(compose.density) { max(navigationBar.toPx(), gestures.toPx()) }
        val bottom = bottomOf(tag)
        assertTrue(
            "$what ends at $bottom px, less than $inset px above the window's bottom ($windowBottom px) " +
                "with ${describeInsets()}",
            windowBottom - bottom >= inset - EPSILON,
        )
    }

    /** Checks that the node tagged [tag] ends at or above the top of the floating bar. */
    private fun assertAboveBar(what: String, tag: String) {
        waitFor { count(tag) > 0 }
        val barTop = compose.onNodeWithTag(KANSO_TAB_BAR_TAG).fetchSemanticsNode().positionInRoot.y
        val bottom = bottomOf(tag)
        assertTrue(
            "$what, scrolled to the end, ends at $bottom px, below the bar's top ($barTop px) with ${describeInsets()}",
            bottom <= barTop + EPSILON,
        )
    }

    /** Returns the bottom edge, in pixels from the window's top, of the node tagged [tag], unclipped. */
    private fun bottomOf(tag: String): Float {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        return node.positionInRoot.y + node.size.height
    }

    /**
     * Scrolls the innermost vertically scrolling container that holds (or is) the node tagged
     * [tag] to its very end, through its semantics scroll action, as far as it goes. A lazy grid
     * only estimates its length, so the scroll is repeated until the container reports it can go
     * no further.
     */
    private fun scrollToEnd(screen: String, tag: String) {
        val scroller = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
            SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollBy) and
            (hasTestTag(tag) or hasAnyDescendant(hasTestTag(tag)))
        val windowHeight = compose.onRoot().fetchSemanticsNode().size.height.toFloat()
        repeat(MAX_SCROLLS) {
            // Ancestors come before descendants in the list, so the last is the innermost.
            val node = compose.onAllNodes(scroller).fetchSemanticsNodes().lastOrNull()
                ?: error("$screen has no vertical scroller around $tag")
            val range = node.config[SemanticsProperties.VerticalScrollAxisRange]
            val left = range.maxValue() - range.value()
            if (left <= EPSILON) return
            val scrollBy = checkNotNull(node.config[SemanticsActions.ScrollBy].action)
            compose.runOnUiThread { scrollBy(0f, max(left, windowHeight)) }
            compose.waitForIdle()
            shadowOf(Looper.getMainLooper()).idle()
        }
        fail("$screen did not reach the end of its scrolling after $MAX_SCROLLS scrolls")
    }

    // --- Set-up --------------------------------------------------------------------------------

    /**
     * Grants photo and camera access, then loads the fakes: [EDIT_COUNT] saved photos, a library
     * of three photos in one album, the fake decoder, saver and camera.
     */
    private fun loadFakes() {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA, *photoPermissionsFor(35).toTypedArray())
        loadKoinModules(
            module {
                single<SavedPhotoSource> { FakeSavedPhotoSource(editedPhotos) }
                single<PhotoSource> { FakePhotoSource(albums = listOf(Album(1L, "A")), allPhotos = galleryPhotos) }
                single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                single<PhotoSaver> { FakePhotoSaver() }
                factory<CameraController> { FakeCameraController() }
            },
        )
    }

    /**
     * Sends the walk's insets (a 24 dp status bar, the navigation bar and the bottom gesture area)
     * to the app's Compose view, as the system would, waits until the screen has laid out again,
     * and checks that the screen received them.
     *
     * How receipt is checked. Robolectric draws no system bars, so nothing would complain if the
     * insets went nowhere, and a control that happened to sit high enough would pass the
     * clearance checks all the same. So the insets are sent twice: first with no navigation bar
     * and no gesture area, then with the walk's. Between the two, the node tagged [probeTag], one
     * of the controls about to be checked, which lifts itself by the taller of the two insets,
     * must have moved up by at least that much. A screen that never got the second set, or whose
     * control ignores it, leaves the probe where it was and fails here.
     */
    private fun sendInsets(scenario: ActivityScenario<MainActivity>, probeTag: String) {
        dispatchInsets(scenario, navigationBarPx = 0, gesturesPx = 0)
        waitFor { count(probeTag) > 0 }
        val bottomWithout = bottomOf(probeTag)

        val density = compose.density
        dispatchInsets(
            scenario,
            navigationBarPx = with(density) { navigationBar.roundToPx() },
            gesturesPx = with(density) { gestures.roundToPx() },
        )
        val bottomWith = bottomOf(probeTag)
        val inset = with(density) { max(navigationBar.toPx(), gestures.toPx()) }
        val moved = bottomWithout - bottomWith
        println("insets $probeTag with ${describeInsets()}: moved up $moved px for an inset of $inset px")
        assertTrue(
            "The screen did not receive the insets sent: $probeTag ends at $bottomWithout px with none and at " +
                "$bottomWith px with ${describeInsets()}, so it moved up $moved px, not the $inset px sent",
            moved >= inset - EPSILON,
        )
    }

    /**
     * Sends a 24 dp status bar, a [navigationBarPx] tall navigation bar and a [gesturesPx] tall
     * bottom gesture area (both in pixels) to the app's Compose view, as the system would, and
     * waits until the screen has laid out again.
     */
    private fun dispatchInsets(scenario: ActivityScenario<MainActivity>, navigationBarPx: Int, gesturesPx: Int) {
        val density = compose.density
        val status = with(density) { STATUS_BAR.roundToPx() }
        val navigation = navigationBarPx
        val gesture = gesturesPx
        val built = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, status, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navigation))
            .setInsets(WindowInsetsCompat.Type.systemGestures(), Insets.of(0, 0, 0, gesture))
            .build()
        compose.waitForIdle()
        scenario.onActivity { activity ->
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val view = composeViewIn(content) ?: error("The app has no Compose view to send insets to")
            ViewCompat.dispatchApplyWindowInsets(view, built)
        }
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    /**
     * Returns the view Compose draws the app in (the one `LocalView` names inside the app, an
     * `AndroidComposeView`), found by walking down from [view]: insets sent to it reach the
     * `WindowInsets` the screens read.
     */
    private fun composeViewIn(view: View): View? {
        if (view.javaClass.simpleName == "AndroidComposeView") return view
        if (view !is ViewGroup) return null
        for (index in 0 until view.childCount) {
            composeViewIn(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    /** Names the insets of the current walk, for failure messages. */
    private fun describeInsets(): String = "a $navigationBar navigation bar and a $gestures gesture area"

    /** Waits up to ten seconds for [condition], letting the main thread run in between. */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** The screens, insets, photos and numbers the tests share. */
    private companion object {
        /** The tool whose screen is saved, to reach the Saved screen (as SavedNavigationTest does). */
        val SAVE_TOOL = Tool.RETOUCH

        /** How many photos Kanso has saved: My edits' 40th tile is the last. */
        const val EDIT_COUNT = 40

        /** Slack for rounding, in pixels. */
        const val EPSILON = 1f

        /** How many scrolls [scrollToEnd] tries before it fails. */
        const val MAX_SCROLLS = 20

        /** The status bar's height sent with every inset set. */
        val STATUS_BAR = 24.dp

        /** The three-button navigation bar's height; the gesture area matches it in that mode. */
        val NAVIGATION_BAR = 48.dp

        /** The bottom gesture area's height with swipe gestures and no navigation bar. */
        val GESTURE_AREA = 32.dp

        /** The photo library: three photos, newest first, with ids 1 to 3. */
        val galleryPhotos: List<Photo> = GalleryGridTest.manyPhotos(3)

        /** Kanso's saved photos, newest first, as Home's strip and My edits list them. */
        val editedPhotos: List<Photo> = (0 until EDIT_COUNT).map { i ->
            Photo(
                id = 700L + i,
                uri = "content://media/external/images/media/${700 + i}",
                name = "Kanso_$i.jpg",
                albumId = 2L,
                modifiedSeconds = 1_791_000_000L - i * 3_600L,
            )
        }
    }
}

/** The phone of DoD 26: 411 x 891 dp. */
private const val PHONE = "w411dp-h891dp-xxhdpi"

/** The small phone of DoD 26: 360 x 780 dp. */
private const val SMALL_PHONE = "w360dp-h780dp-xxhdpi"
