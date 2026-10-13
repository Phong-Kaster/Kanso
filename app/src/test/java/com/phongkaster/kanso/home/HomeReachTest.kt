package com.phongkaster.kanso.home

import android.Manifest
import android.app.Application
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.a11y.TouchTargetCheck
import com.phongkaster.kanso.camera.FakeCameraController
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.media.SavedPhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.myedits.FakeSavedPhotoSource
import com.phongkaster.kanso.navigation.KANSO_CAMERA_BUTTON_TAG
import com.phongkaster.kanso.navigation.KANSO_TAB_BAR_TAG
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.navigation.kansoTabTag
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.camera.CAMERA_SCREEN_TAG
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.home.HOME_CAROUSEL_TAG
import com.phongkaster.kanso.ui.home.HOME_RECENT_EMPTY_TAG
import com.phongkaster.kanso.ui.home.HOME_RECENT_MORE_TAG
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.home.homeCarouselDotTag
import com.phongkaster.kanso.ui.home.homeRecentTag
import com.phongkaster.kanso.ui.home.homeShortcutTag
import com.phongkaster.kanso.ui.home.homeTryTag
import com.phongkaster.kanso.ui.myedits.MY_EDITS_SCREEN_TAG
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import kotlin.math.abs
import org.junit.After
import org.junit.Assert.assertEquals
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
 * DoD 6, through the real app (MainActivity on Android 15, portrait, font scale 1, Kanso blue,
 * light): right after launch, with nothing scrolled and no swipe, every way out of Home that the
 * owner wants one tap away is wholly on screen and above the floating bar, and one tap on it leads
 * where it should.
 *
 * The targets are the five tool shortcuts, the first banner's "Try now" (Remove background), the
 * Camera button, the recent-edits arrow, the three tabs, and the newest recent photo (with five
 * saved photos) or the strip's empty message (on a fresh install). Each is checked on the small
 * phone (360 x 780 dp) and on the phone (411 x 891 dp), each with a 24 dp status bar and a 48 dp
 * three-button navigation bar, and again with no navigation bar and a 32 dp bottom gesture area.
 * Robolectric draws no system bars, so the test sends those insets to the app's Compose view
 * itself, as the system would ([sendInsets]).
 *
 * "Wholly on screen" is measured from each node's unclipped box, its position in the root plus its
 * size, which must lie inside the window and below the status bar; its `boundsInRoot` must equal
 * that box within 1 px. Compose cuts `boundsInRoot` down to what is visible, so a half-hidden node
 * would pass a check of `boundsInRoot` alone; the comparison catches it. A node outside the bar
 * must also end at or above the tops of the bar and the Camera button.
 *
 * The one-tap walk starts the app afresh for each target and calls `performClick()` once, a real
 * touch at the node's centre (so a node lying under the bar would send the touch to the bar), then
 * waits for the screen it leads to. It prints the gestures it used per target, one row each; every
 * row reads "1 tap", none a swipe or a scroll.
 *
 * The fakes are loaded before the app starts (`createEmptyComposeRule`, `loadKoinModules`, then
 * `ActivityScenario.launch`): Home's ViewModel keeps the source it was built with.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class HomeReachTest {

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

    /** The small phone on a fresh install: every target, and the empty message, on screen. */
    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun small_phone_fresh_install_has_every_target_on_screen() {
        assertEveryTargetOnScreen(edits = emptyList())
    }

    /** The small phone with five saved photos: every target, the newest photo included. */
    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun small_phone_with_five_edits_has_every_target_on_screen() {
        assertEveryTargetOnScreen(edits = photos(5))
    }

    /** The phone on a fresh install. */
    @Test
    @Config(qualifiers = PHONE)
    fun phone_fresh_install_has_every_target_on_screen() {
        assertEveryTargetOnScreen(edits = emptyList())
    }

    /** The phone with five saved photos. */
    @Test
    @Config(qualifiers = PHONE)
    fun phone_with_five_edits_has_every_target_on_screen() {
        assertEveryTargetOnScreen(edits = photos(5))
    }

    /**
     * On the small phone with five saved photos and the three-button navigation bar: from a fresh
     * launch for each target, exactly one `performClick()` leads where it should, and the printed
     * table reads "1 tap" on every row.
     */
    @Test
    @Config(qualifiers = SMALL_PHONE)
    fun each_target_is_one_tap_from_a_fresh_launch_on_the_small_phone() {
        val edits = photos(5)
        val table = mutableListOf("| Target | Gestures used |", "|---|---|")
        loadFakes(edits)
        targets(edits).forEach { target ->
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                waitForHome()
                sendInsets(scenario, NAVIGATION_BAR)
                assertNothingScrolled()

                compose.onNodeWithTag(target.tag).performClick()
                waitFor { target.arrived() }
                table += "| ${target.name} (`${target.tag}`) | 1 tap |"
            }
        }
        println("Home reach, gestures per target (small phone, 48 dp navigation bar, five edits):")
        table.forEach { println(it) }
        val toolRows = table.filter { row -> Tool.entries.any { "`${homeShortcutTag(it)}`" in row } }
        assertEquals(Tool.entries.size, toolRows.size)
        assertTrue("Every tool row must read 1 tap: $toolRows", toolRows.all { it.endsWith("| 1 tap |") })
        assertTrue("No row may use a swipe or a scroll", table.none { "swipe" in it.lowercase() || "scroll" in it.lowercase() })
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Starts the app with [edits] as Kanso's saved photos and, for each inset set, checks every
     * target (see the class comment) with nothing scrolled.
     */
    private fun assertEveryTargetOnScreen(edits: List<Photo>) {
        loadFakes(edits)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome()
            listOf(NAVIGATION_BAR, GESTURES).forEach { insets ->
                sendInsets(scenario, insets)
                assertNothingScrolled()
                val statusBarPx = with(compose.density) { STATUS_BAR.toPx() }
                val barTop = listOf(KANSO_TAB_BAR_TAG, KANSO_CAMERA_BUTTON_TAG).minOf { box(node(it)).top }
                val root = compose.onRoot().fetchSemanticsNode().size

                targets(edits).forEach { target ->
                    val node = node(target.tag)
                    val unclipped = box(node)
                    val where = "${target.name} with ${insets.name}: $unclipped"
                    assertTrue("$where must have a click action", SemanticsActions.OnClick in node.config)
                    compose.onNodeWithTag(target.tag).assertTouchAtLeast48()
                    assertTrue(
                        "$where must lie wholly inside the ${root.width} x ${root.height} px window, below the status bar",
                        unclipped.left >= -EPSILON && unclipped.right <= root.width + EPSILON &&
                            unclipped.top >= statusBarPx - EPSILON && unclipped.bottom <= root.height + EPSILON,
                    )
                    assertSameRect("$where must not be clipped", unclipped, node.boundsInRoot)
                    if (!target.inBar) {
                        assertTrue("$where must end above the bar (top $barTop)", unclipped.bottom <= barTop + EPSILON)
                    }
                }

                // The empty message on a fresh install, like the targets: on screen and above the bar.
                if (edits.isEmpty()) {
                    val empty = node(HOME_RECENT_EMPTY_TAG)
                    val unclipped = box(empty)
                    assertTrue("The empty message with ${insets.name} must end above the bar: $unclipped", unclipped.bottom <= barTop + EPSILON)
                    assertSameRect("The empty message must not be clipped", unclipped, empty.boundsInRoot)
                }

                // The shortcuts are inside no sideways-scrolling container.
                Tool.entries.forEach { tool ->
                    compose.onNodeWithTag(homeShortcutTag(tool)).assert(!hasAnyAncestor(hasHorizontalScroll))
                }
                println("Home reach with ${insets.name}: bar top $barTop px, window ${root.height} px")
            }
        }
    }

    /**
     * The one-tap targets, each with its tag, whether it lies inside the floating bar's row, and
     * the check that its screen has arrived after a tap. With [edits] the newest photo is a target;
     * on a fresh install the strip has no card, so the arrow and the tabs stand alone.
     */
    private fun targets(edits: List<Photo>): List<Target> {
        val shortcuts = Tool.entries.map { tool ->
            Target("${label(tool)} shortcut", homeShortcutTag(tool), inBar = false) { galleryOf(tool) }
        }
        val others = listOf(
            Target("Remove background's Try now", homeTryTag(Tool.REMOVE_BACKGROUND), inBar = false) {
                galleryOf(Tool.REMOVE_BACKGROUND)
            },
            Target("Camera", KANSO_CAMERA_BUTTON_TAG, inBar = true) { count(CAMERA_SCREEN_TAG) > 0 },
            Target("Recent edits arrow", HOME_RECENT_MORE_TAG, inBar = false) { myEditsSelected() },
            Target("Home tab", kansoTabTag(KansoTab.HOME), inBar = true) { count(HOME_SCREEN_TAG) > 0 },
            Target("My edits tab", kansoTabTag(KansoTab.MY_EDITS), inBar = true) { myEditsSelected() },
            Target("Settings tab", kansoTabTag(KansoTab.SETTINGS), inBar = true) {
                count(SETTINGS_SCREEN_TAG) > 0 && count(HOME_SCREEN_TAG) == 0
            },
        )
        val newest = edits.firstOrNull()?.let { photo ->
            Target("Newest recent photo", homeRecentTag(0), inBar = false) {
                count(HOME_SCREEN_TAG) == 0 && compose.onAllNodes(
                    hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, photo.uri),
                ).fetchSemanticsNodes().isNotEmpty() && compose.onAllNodes(
                    hasText(app.getString(R.string.tool_edit)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
                ).fetchSemanticsNodes().isNotEmpty()
            }
        }
        return shortcuts + others + listOfNotNull(newest)
    }

    /**
     * Grants photo access, then loads the fakes: [edits] as the saved photos, an empty photo
     * library, the fake camera and the fake decoder.
     */
    private fun loadFakes(edits: List<Photo>) {
        shadowOf(app).grantPermissions(*photoPermissionsFor(35).toTypedArray(), Manifest.permission.CAMERA)
        loadKoinModules(
            module {
                single<SavedPhotoSource> { FakeSavedPhotoSource(edits) }
                single<PhotoSource> { FakePhotoSource(albums = emptyList(), allPhotos = emptyList()) }
                single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
                factory<CameraController> { FakeCameraController() }
            },
        )
    }

    /**
     * Sends [insets] (a 24 dp status bar, and a navigation bar or a bottom gesture area) to the
     * app's Compose view, as the system would, and waits until Home has laid out again.
     */
    private fun sendInsets(scenario: ActivityScenario<MainActivity>, insets: InsetSet) {
        val density = compose.density
        val status = with(density) { STATUS_BAR.roundToPx() }
        val navigation = with(density) { insets.navigationBar.roundToPx() }
        val gestures = with(density) { insets.gestures.roundToPx() }
        val built = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, status, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, navigation))
            .setInsets(WindowInsetsCompat.Type.systemGestures(), Insets.of(0, 0, 0, gestures))
            .build()
        compose.waitForIdle()
        scenario.onActivity { activity ->
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val view = composeViewIn(content) ?: error("The app has no Compose view to send insets to")
            ViewCompat.dispatchApplyWindowInsets(view, built)
        }
        waitFor { true }
        compose.waitForIdle()
    }

    /**
     * Returns the view Compose draws the app in (the one `LocalView` names inside the app, an
     * `AndroidComposeView`), found by walking down from [view]: insets sent to it reach the
     * `WindowInsets` the screens read, as in the hosted tests that send insets to `LocalView`.
     */
    private fun composeViewIn(view: View): View? {
        if (view.javaClass.simpleName == "AndroidComposeView") return view
        if (view !is ViewGroup) return null
        for (index in 0 until view.childCount) {
            composeViewIn(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    /** Fails unless Home's column is scrolled to the top and the carousel shows banner 1. */
    private fun assertNothingScrolled() {
        val column = compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
                hasAnyDescendant(hasTestTag(HOME_CAROUSEL_TAG)),
        ).fetchSemanticsNode()
        assertEquals("Home must not be scrolled", 0f, column.config[SemanticsProperties.VerticalScrollAxisRange].value(), 0.5f)
        val firstDot = node(homeCarouselDotTag(0))
        assertTrue(
            "The carousel must show banner 1",
            firstDot.config.getOrNull(SemanticsProperties.StateDescription) != null,
        )
    }

    /** Fails unless this node's touch target is at least 48 dp wide and tall. */
    private fun SemanticsNodeInteraction.assertTouchAtLeast48() {
        val node = fetchSemanticsNode()
        val min = with(compose.density) { MIN_TOUCH.toPx() }
        // The larger of the node's size and the area Compose widens its touch to.
        val (width, height) = TouchTargetCheck.measuredSizePx(node)
        assertTrue(
            "${node.config.getOrNull(SemanticsProperties.TestTag)}'s touch target $width x $height px is under 48 dp",
            width >= min - EPSILON && height >= min - EPSILON,
        )
    }

    /** Fails unless [a] and [b] are the same rectangle within 1 px on every side. */
    private fun assertSameRect(message: String, a: Rect, b: Rect) {
        assertTrue(
            "$message: laid out $a, visible $b",
            abs(a.left - b.left) <= EPSILON && abs(a.top - b.top) <= EPSILON &&
                abs(a.right - b.right) <= EPSILON && abs(a.bottom - b.bottom) <= EPSILON,
        )
    }

    /** Tells whether [tool]'s gallery, titled "<tool>: choose a photo", is on screen. */
    private fun galleryOf(tool: Tool): Boolean = compose.onAllNodes(
        hasText(app.getString(R.string.gallery_title, label(tool))) and hasAnyAncestor(hasTestTag(GALLERY_SCREEN_TAG)),
    ).fetchSemanticsNodes().isNotEmpty()

    /** Tells whether the My edits tab is on screen and selected on the bar. */
    private fun myEditsSelected(): Boolean {
        if (count(MY_EDITS_SCREEN_TAG) == 0 || count(HOME_SCREEN_TAG) > 0) return false
        return node(kansoTabTag(KansoTab.MY_EDITS)).config.getOrNull(SemanticsProperties.Selected) == true
    }

    /** Returns [tool]'s name as Home shows it. */
    private fun label(tool: Tool): String = app.getString(tool.labelRes)

    /** Returns the laid-out box of [node], in pixels, from its position and size (not clipped). */
    private fun box(node: SemanticsNode): Rect = Rect(node.positionInRoot, node.size.toSize())

    /** Returns the one node tagged [tag]. */
    private fun node(tag: String): SemanticsNode = compose.onNodeWithTag(tag).fetchSemanticsNode()

    /** Waits until Home is on screen with its first shortcut, letting the main thread run. */
    private fun waitForHome() {
        waitFor { count(HOME_SCREEN_TAG) > 0 && count(homeShortcutTag(Tool.EDIT)) > 0 && count(KANSO_TAB_BAR_TAG) > 0 }
        compose.waitForIdle()
    }

    /** Waits up to ten seconds of real time for [condition], letting the main thread run in between. */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /**
     * One way out of Home: its [name] for the printed table, its [tag], whether it lies [inBar]
     * (the bar's row: the tabs and the Camera button), and [arrived], which tells whether the
     * screen it leads to is shown.
     */
    private class Target(val name: String, val tag: String, val inBar: Boolean, val arrived: () -> Boolean)

    /** A set of bottom insets the test sends: a navigation bar's height and a gesture area's. */
    private class InsetSet(val name: String, val navigationBar: Dp, val gestures: Dp)

    /** Values, photos and matchers the tests share. */
    private companion object {
        /** How far, in pixels, two edges may differ and still count as the same. */
        const val EPSILON = 1f

        /** The status bar's height every inset set includes. */
        val STATUS_BAR = 24.dp

        /** The smallest touch target allowed. */
        val MIN_TOUCH = 48.dp

        /** A 48 dp three-button navigation bar (its gesture area is the bar itself). */
        val NAVIGATION_BAR = InsetSet("a 48 dp navigation bar", navigationBar = 48.dp, gestures = 48.dp)

        /** Gesture navigation with the hint hidden: no navigation bar, a 32 dp bottom gesture area. */
        val GESTURES = InsetSet("a 32 dp gesture area", navigationBar = 0.dp, gestures = 32.dp)

        /** Matches a node that scrolls sideways. */
        val hasHorizontalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)

        /** Returns [count] saved photos, newest first, one hour apart. */
        fun photos(count: Int): List<Photo> = (1..count).map { i ->
            Photo(
                id = i.toLong(),
                uri = "content://media/external/images/media/${900 + i}",
                name = "Kanso_$i.jpg",
                albumId = 1L,
                modifiedSeconds = 1_791_800_000L - i * 3_600L,
            )
        }
    }
}

/** The small phone of DoD 6: 360 x 780 dp at xxhdpi. */
private const val SMALL_PHONE = "w360dp-h780dp-xxhdpi"

/** The phone: 411 x 891 dp at xxhdpi, the size the other whole-app layout tests use. */
private const val PHONE = "w411dp-h891dp-xxhdpi"
