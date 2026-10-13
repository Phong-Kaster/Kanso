package com.phongkaster.kanso.home

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.media.SavedPhotoSource
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.myedits.FakeSavedPhotoSource
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.kansoTabTag
import com.phongkaster.kanso.ui.home.HOME_RECENT_CARD_TAG_PREFIX
import com.phongkaster.kanso.ui.home.HOME_RECENT_EMPTY_TAG
import com.phongkaster.kanso.ui.home.HOME_RECENT_SKELETON_TAG_PREFIX
import com.phongkaster.kanso.ui.home.HOME_RECENT_TAG
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.home.homeRecentSkeletonTag
import com.phongkaster.kanso.ui.home.homeRecentTag
import com.phongkaster.kanso.ui.myedits.MY_EDITS_EMPTY_TAG
import com.phongkaster.kanso.ui.myedits.MY_EDITS_GRID_TAG
import com.phongkaster.kanso.ui.myedits.MY_EDITS_SCREEN_TAG
import com.phongkaster.kanso.ui.myedits.MY_EDITS_SKELETON_TAG_PREFIX
import com.phongkaster.kanso.ui.myedits.MY_EDITS_TILE_TAG_PREFIX
import com.phongkaster.kanso.ui.myedits.myEditsSkeletonTag
import com.phongkaster.kanso.ui.myedits.myEditsTileTag
import kotlin.math.abs
import kotlin.math.ceil
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DoD 8, through the real app (MainActivity on Android 15, phone-sized): while Kanso's saved
 * photos are read, Home's "Recent edits" strip and the My edits grid show soft placeholders in the
 * shape of their cards and tiles, never a spinner; once the photos arrive the placeholders give way
 * to them.
 *
 * The edits source is a fake whose reads wait on a gate ([FakeSavedPhotoSource.holdReads]) until
 * the test opens it. It is loaded before the app starts (`createEmptyComposeRule`,
 * `loadKoinModules`, then `ActivityScenario.launch`), because Home's ViewModel keeps the source it
 * was built with.
 *
 * The placeholders' band is an endless animation while the real screens show them. The test clock
 * is therefore paused before the app starts (`mainClock.autoAdvance = false`), and every wait steps
 * it one frame at a time while letting the main thread run ([waitFor]), so no wait ever hangs on an
 * animation that never ends, and the placeholders are drawn at a known frame.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class EditsLoadingTest {

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
     * Home on its own: with the gate closed the strip shows at least two rounded placeholder cards,
     * the strip alone says the photos are loading, nothing on Home is a progress indicator and the
     * empty message is absent; once the gate opens, within ten seconds no placeholder remains and
     * the cards show, each placeholder as wide and as tall as a real card within 1 dp.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun home_shows_placeholder_cards_until_the_recent_photos_arrive() {
        val source = FakeSavedPhotoSource(photos(5)).apply { holdReads() }
        launch(source) { scenario ->
            waitFor { count(isHomeSkeleton) >= 2 }
            val skeletons = assertHomeLoading(scenario)

            source.releaseReads()
            waitFor { count(isHomeSkeleton) == 0 && count(homeRecentTag(0)) > 0 }
            assertEquals(0, count(HOME_RECENT_EMPTY_TAG))
            assertSameSize("Home's placeholder", skeletons, node(homeRecentTag(0)))
        }
    }

    /**
     * Home and the My edits tab together, with one gate: Home shows its placeholder cards (as in
     * the Home test), then the My edits tab shows at least six rounded placeholder tiles with one
     * spoken name on the grid, no progress indicator and no empty message. The gate opens: the
     * tiles show and no placeholder remains; back on Home the cards show and no placeholder
     * remains. Each placeholder matches the size of a real tile or card within 1 dp.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun home_and_my_edits_show_placeholders_until_the_photos_arrive() {
        val source = FakeSavedPhotoSource(photos(8)).apply { holdReads() }
        launch(source) { scenario ->
            waitFor { count(isHomeSkeleton) >= 2 }
            val cardSkeletons = assertHomeLoading(scenario)

            compose.onNodeWithTag(kansoTabTag(KansoTab.MY_EDITS)).assertExistsAndClick()
            waitFor { count(HOME_SCREEN_TAG) == 0 && count(isMyEditsSkeleton) >= MIN_TILE_SKELETONS }
            val tileSkeletons = compose.onAllNodes(isMyEditsSkeleton).fetchSemanticsNodes()
            assertTrue("At least six placeholder tiles, found ${tileSkeletons.size}", tileSkeletons.size >= MIN_TILE_SKELETONS)
            compose.onNodeWithTag(MY_EDITS_GRID_TAG).assertContentDescriptionEquals(app.getString(R.string.my_edits_loading))
            tileSkeletons.forEach { assertNoOwnName(it) }
            compose.onAllNodes(isProgress and hasAnyAncestor(hasTestTag(MY_EDITS_SCREEN_TAG)), useUnmergedTree = true)
                .assertCountEquals(0)
            compose.onAllNodes(isProgress, useUnmergedTree = true).assertCountEquals(0)
            assertEquals(0, count(MY_EDITS_EMPTY_TAG))
            assertRounded(scenario, node(myEditsSkeletonTag(0)), "My edits placeholder 0")
            val tileSkeleton = node(myEditsSkeletonTag(0))

            source.releaseReads()
            waitFor { count(isMyEditsSkeleton) == 0 && count(myEditsTileTag(0)) > 0 }
            assertTrue(count(isMyEditsTile) > 0)
            assertSameSize("My edits' placeholder", tileSkeleton, node(myEditsTileTag(0)))

            compose.onNodeWithTag(kansoTabTag(KansoTab.HOME)).assertExistsAndClick()
            waitFor { count(MY_EDITS_SCREEN_TAG) == 0 && count(homeRecentTag(0)) > 0 }
            waitFor { count(isHomeSkeleton) == 0 }
            assertEquals(0, count(HOME_RECENT_EMPTY_TAG))
            assertSameSize("Home's placeholder", cardSkeletons, node(homeRecentTag(0)))
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Pauses the test clock, loads [source] as the edits source (and an empty photo library),
     * starts the app, waits for Home and runs [body] with the scenario.
     */
    private fun launch(source: FakeSavedPhotoSource, body: (ActivityScenario<MainActivity>) -> Unit) {
        compose.mainClock.autoAdvance = false
        loadKoinModules(
            module {
                single<SavedPhotoSource> { source }
                single<PhotoSource> { FakePhotoSource(albums = emptyList(), allPhotos = emptyList()) }
            },
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor { count(HOME_SCREEN_TAG) > 0 }
            body(scenario)
        }
    }

    /**
     * The checks of Home while the gate is closed (see the tests); returns the first placeholder
     * card, to compare with a real card once the photos have arrived.
     */
    private fun assertHomeLoading(scenario: ActivityScenario<MainActivity>): SemanticsNode {
        val skeletons = compose.onAllNodes(isHomeSkeleton).fetchSemanticsNodes()
        assertTrue("At least two placeholder cards, found ${skeletons.size}", skeletons.size >= 2)
        compose.onNodeWithTag(HOME_RECENT_TAG).assertContentDescriptionEquals(app.getString(R.string.home_hero_recent_loading))
        skeletons.forEach { assertNoOwnName(it) }
        compose.onAllNodes(isProgress and hasAnyAncestor(hasTestTag(HOME_RECENT_TAG)), useUnmergedTree = true)
            .assertCountEquals(0)
        compose.onAllNodes(isProgress, useUnmergedTree = true).assertCountEquals(0)
        assertEquals(0, count(HOME_RECENT_EMPTY_TAG))
        assertEquals(0, count(isHomeCard))
        val first = node(homeRecentSkeletonTag(0))
        assertRounded(scenario, first, "Home placeholder 0")
        return first
    }

    /** Fails unless [node] has no spoken name of its own, in the unmerged tree. */
    private fun assertNoOwnName(node: SemanticsNode) {
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        val own = compose.onNodeWithTag(checkNotNull(tag), useUnmergedTree = true).fetchSemanticsNode()
        assertNull("$tag must have no spoken name of its own", own.config.getOrNull(SemanticsProperties.ContentDescription))
    }

    /**
     * Fails unless [node] is drawn rounded: 1 px inside its top-left corner the screen shows the
     * background just left of it, while its centre shows something else (the placeholder's fill).
     */
    private fun assertRounded(scenario: ActivityScenario<MainActivity>, node: SemanticsNode, name: String) {
        val left = node.positionInRoot.x
        val top = node.positionInRoot.y
        val cornerX = ceil(left).toInt() + 1
        val cornerY = ceil(top).toInt() + 1
        val centreX = (left + node.size.width / 2f).toInt()
        val centreY = (top + node.size.height / 2f).toInt()
        val outsideX = (left - OUTSIDE_PX).toInt().coerceAtLeast(0)
        val (corner, centre, outside) = drawScreen(scenario) { bitmap ->
            Triple(bitmap.getPixel(cornerX, cornerY), bitmap.getPixel(centreX, centreY), bitmap.getPixel(outsideX, centreY))
        }
        assertFalse("$name's centre must differ from the background ${hex(outside)}, was ${hex(centre)}", near(centre, outside))
        assertTrue("$name's corner must show the background ${hex(outside)}, was ${hex(corner)}", near(corner, outside))
    }

    /** Fails unless [placeholder] and [real] have the same width and height within 1 dp. */
    private fun assertSameSize(name: String, placeholder: SemanticsNode, real: SemanticsNode) {
        val oneDp = with(compose.density) { 1f * density }
        assertTrue(
            "$name is ${placeholder.size}, the real one ${real.size}",
            abs(placeholder.size.width - real.size.width) <= oneDp &&
                abs(placeholder.size.height - real.size.height) <= oneDp,
        )
    }

    /**
     * Clicks this node through its semantics click action, the one a screen reader's double tap
     * calls: with the clock paused, a touch's down and up would need frames between them.
     */
    private fun SemanticsNodeInteraction.assertExistsAndClick() {
        assertExists()
        performSemanticsAction(SemanticsActions.OnClick)
    }

    /**
     * Draws the activity's content view into a fresh bitmap, hands it to [read], then frees it.
     * The Compose root fills the content view from its top-left corner, so a node's position in
     * the root is the bitmap's coordinates.
     */
    private fun <T> drawScreen(scenario: ActivityScenario<MainActivity>, read: (Bitmap) -> T): T {
        var result: T? = null
        scenario.onActivity { activity ->
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val view = content.getChildAt(0) ?: error("The activity has no content view yet")
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(bitmap))
                result = read(bitmap)
            } finally {
                bitmap.recycle()
            }
        }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /** Tells whether the red, green and blue of [a] and [b] each differ by at most [COLOUR_TOLERANCE]. */
    private fun near(a: Int, b: Int): Boolean =
        abs(android.graphics.Color.red(a) - android.graphics.Color.red(b)) <= COLOUR_TOLERANCE &&
            abs(android.graphics.Color.green(a) - android.graphics.Color.green(b)) <= COLOUR_TOLERANCE &&
            abs(android.graphics.Color.blue(a) - android.graphics.Color.blue(b)) <= COLOUR_TOLERANCE

    /** Writes [colour] as `#AARRGGBB`, for failure messages. */
    private fun hex(colour: Int): String = "#%08X".format(colour)

    /**
     * Waits up to ten seconds of real time for [condition]. Each try moves the paused test clock
     * on by one frame and lets the main thread run, where the reads' results and the screens'
     * moves are posted.
     */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.mainClock.advanceTimeByFrame()
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Returns the one node tagged [tag]. */
    private fun node(tag: String): SemanticsNode = compose.onNodeWithTag(tag).fetchSemanticsNode()

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** Counts the nodes [matcher] matches. */
    private fun count(matcher: SemanticsMatcher): Int = compose.onAllNodes(matcher).fetchSemanticsNodes().size

    /** Values, photos and matchers the tests share. */
    private companion object {
        /** How many placeholder tiles My edits must show at least. */
        const val MIN_TILE_SKELETONS = 6

        /** The largest difference per colour channel still counted as the same colour. */
        const val COLOUR_TOLERANCE = 8

        /** How far left of a placeholder, in pixels, the background is sampled. */
        const val OUTSIDE_PX = 6f

        /** The time of the newest sample photo, in seconds since 1970 (October 2026). */
        const val NEWEST_SECONDS = 1_791_800_000L

        /** Returns [count] saved photos, newest first, one hour apart. */
        fun photos(count: Int): List<Photo> = (1..count).map { i ->
            Photo(
                id = i.toLong(),
                uri = "content://media/external/images/media/${700 + i}",
                name = "Kanso_$i.jpg",
                albumId = 1L,
                modifiedSeconds = NEWEST_SECONDS - i * 3_600L,
            )
        }

        /** Matches a node that reports progress, as every spinner and progress bar does. */
        val isProgress = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

        /** Matches every placeholder card of Home's strip. */
        val isHomeSkeleton = SemanticsMatcher("is a Home placeholder card") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(HOME_RECENT_SKELETON_TAG_PREFIX) == true
        }

        /** Matches every photo card of Home's strip (`home-recent-<digits>`). */
        val isHomeCard = SemanticsMatcher("is a Home recent photo card") { node ->
            val tag = node.config.getOrNull(SemanticsProperties.TestTag)
            tag != null && tag.length > HOME_RECENT_CARD_TAG_PREFIX.length &&
                tag.startsWith(HOME_RECENT_CARD_TAG_PREFIX) &&
                tag.removePrefix(HOME_RECENT_CARD_TAG_PREFIX).all { it.isDigit() }
        }

        /** Matches every placeholder tile of the My edits grid. */
        val isMyEditsSkeleton = SemanticsMatcher("is a My edits placeholder tile") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(MY_EDITS_SKELETON_TAG_PREFIX) == true
        }

        /** Matches every photo tile of the My edits grid. */
        val isMyEditsTile = SemanticsMatcher("is a My edits photo tile") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(MY_EDITS_TILE_TAG_PREFIX) == true
        }
    }
}
