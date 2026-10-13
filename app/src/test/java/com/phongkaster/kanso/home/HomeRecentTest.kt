package com.phongkaster.kanso.home

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ImageLoader
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.media.Album
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.media.SavedPhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.data.photo.SaveResult
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.myedits.FakeSavedPhotoSource
import com.phongkaster.kanso.navigation.KansoTab
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.navigation.kansoTabTag
import com.phongkaster.kanso.navigation.openToolGallery
import com.phongkaster.kanso.navigation.waitForHome
import com.phongkaster.kanso.save.FakePhotoSaver
import com.phongkaster.kanso.screenshots.FAKE_SCHEME
import com.phongkaster.kanso.screenshots.SolidColourFetcherFactory
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.galleryPhotoTag
import com.phongkaster.kanso.ui.home.HOME_CAROUSEL_TAG
import com.phongkaster.kanso.ui.home.HOME_RECENT_CARD_TAG_PREFIX
import com.phongkaster.kanso.ui.home.HOME_RECENT_EMPTY_TAG
import com.phongkaster.kanso.ui.home.HOME_RECENT_MORE_TAG
import com.phongkaster.kanso.ui.home.HOME_RECENT_SKELETON_TAG_PREFIX
import com.phongkaster.kanso.ui.home.HOME_RECENT_TAG
import com.phongkaster.kanso.ui.home.HOME_RECENT_URI_KEY
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.home.HomeViewModel
import com.phongkaster.kanso.ui.home.homeRecentTag
import com.phongkaster.kanso.ui.myedits.MY_EDITS_SCREEN_TAG
import com.phongkaster.kanso.ui.myedits.savedAtText
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import com.phongkaster.kanso.ui.saved.SAVED_CLOSE_TAG
import com.phongkaster.kanso.ui.saved.SAVED_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SAVE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import kotlin.math.abs
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * DoD 7, through the real app (MainActivity on Android 15, phone-sized): Home's "Recent edits"
 * strip shows Kanso's saved photos, newest first, at most ten, as tall rounded cards with spoken
 * names that scroll sideways without moving Home; a card opens its photo in the Edit tool and one
 * Back returns Home; the arrow selects the My edits tab; a fresh install shows the short message
 * and no card or placeholder; and a photo saved a moment ago is the first card when the person
 * comes back to Home.
 *
 * The edits source and the photo library are fakes, loaded before the app starts
 * (`createEmptyComposeRule`, `loadKoinModules`, then `ActivityScenario.launch`): Home's ViewModel
 * is built when Home first draws and keeps the source it was given, so a later swap would never
 * reach it. The photos' Uris use the test-only `fake` scheme, and Coil is given an image loader
 * that turns such a Uri into a small bitmap of the colour it names ([SolidColourFetcherFactory]),
 * so the cards show real pictures without reading a file.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class HomeRecentTest {

    /** Finds Compose nodes in the activity the test starts. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Coil's maker of solid-colour pictures for `fake` Uris; counts the pictures it has made. */
    private val fetchers = SolidColourFetcherFactory()

    /**
     * Gives Coil back a plain image loader and stops Koin after each test: both are kept in
     * JVM-wide singletons that outlive the test.
     */
    @After
    fun tearDown() {
        Coil.setImageLoader(ImageLoader.Builder(app).build())
        stopKoin()
    }

    /**
     * (a) Twelve photos of distinct dates, given to the source out of order: the cards follow the
     * newest-first order, card i showing photo i (its Uri and its spoken name, "Edited photo" and
     * the date), and the strip stops at ten (card 9 is the tenth newest, there is no card 10).
     * Each card is taller than it is wide and rounded: 1 px inside its top-left corner the screen
     * shows Home's background, not the card's picture, which fills its centre.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun twelve_photos_show_newest_first_up_to_ten_as_tall_rounded_cards() {
        val photos = photos(12)
        launch(edits = FakeSavedPhotoSource(photos.shuffled(java.util.Random(12)))) { scenario ->
            waitFor { count(homeRecentTag(0)) > 0 }

            val shown = cards().sortedBy { it.positionInRoot.x }
            assertTrue("At least two cards should be composed, found ${shown.size}", shown.size >= 2)
            shown.forEachIndexed { index, card ->
                assertEquals(homeRecentTag(index), card.config.getOrNull(SemanticsProperties.TestTag))
                assertEquals("Card $index's photo", photos[index].uri, card.config.getOrNull(HOME_RECENT_URI_KEY))
                assertEquals(listOf(expectedName(photos[index])), card.config.getOrNull(SemanticsProperties.ContentDescription))
                assertTrue(
                    "Card $index must be taller than wide: ${card.size}",
                    card.size.height > card.size.width,
                )
            }

            // The rounded corner, read from the drawn screen once the cards' pictures are made.
            waitFor { fetchers.fetched.get() >= shown.size }
            compose.waitForIdle()
            val first = compose.onNodeWithTag(homeRecentTag(0)).fetchSemanticsNode()
            val left = first.positionInRoot.x
            val top = first.positionInRoot.y
            val cornerX = ceil(left).toInt() + 1
            val cornerY = ceil(top).toInt() + 1
            val centreX = (left + first.size.width / 2f).toInt()
            val centreY = (top + first.size.height / 2f).toInt()
            val outsideX = (left - OUTSIDE_PX).toInt()
            val (corner, centre, outside) = drawScreen(scenario) { bitmap ->
                Triple(bitmap.getPixel(cornerX, cornerY), bitmap.getPixel(centreX, centreY), bitmap.getPixel(outsideX, centreY))
            }
            assertTrue("The centre of card 0 must show its picture, was ${hex(centre)}", near(centre, colourOf(0)))
            assertFalse("The corner of card 0 must not show its picture, was ${hex(corner)}", near(corner, colourOf(0)))
            assertTrue("The corner of card 0 must show Home's background ${hex(outside)}, was ${hex(corner)}", near(corner, outside))

            // The strip's limit: card 9 is the tenth newest photo, and there is no card 10.
            compose.onNodeWithTag(HOME_RECENT_TAG).performScrollToIndex(HomeViewModel.RECENT_LIMIT - 1)
            compose.waitForIdle()
            val last = compose.onNodeWithTag(homeRecentTag(9)).fetchSemanticsNode()
            assertEquals(photos[9].uri, last.config.getOrNull(HOME_RECENT_URI_KEY))
            compose.onNodeWithTag(homeRecentTag(9)).assertContentDescriptionEquals(expectedName(photos[9]))
            assertEquals(0, count(homeRecentTag(10)))
            assertEquals(0, count(homeRecentTag(11)))
        }
    }

    /**
     * (a) A left swipe on the strip moves the first card's left edge to the left, while Home's own
     * column stays scrolled to the top (its scroll value stays 0): the strip scrolls sideways on
     * its own.
     */
    @Test
    fun a_left_swipe_moves_the_cards_while_home_stays_put() {
        launch(edits = FakeSavedPhotoSource(photos(12))) {
            waitFor { count(homeRecentTag(0)) > 0 }
            assertEquals(0f, homeScroll(), 0.5f)
            val before = compose.onNodeWithTag(homeRecentTag(0)).fetchSemanticsNode().positionInRoot.x

            // A slow drag, so the strip follows the finger instead of flinging far away.
            compose.onNodeWithTag(HOME_RECENT_TAG).performTouchInput {
                swipeLeft(startX = width * 0.8f, endX = width * 0.4f, durationMillis = 1_000)
            }
            compose.waitForIdle()

            val after = compose.onAllNodesWithTag(homeRecentTag(0)).fetchSemanticsNodes()
                .singleOrNull()?.positionInRoot?.x ?: Float.NEGATIVE_INFINITY
            assertTrue("Card 0's left edge must move left: $before, then $after", after < before - 1f)
            assertEquals("Home's column must not scroll", 0f, homeScroll(), 0.5f)
        }
    }

    /** (b) A tap on the newest card opens the Edit tool on that photo; one Back returns Home. */
    @Test
    fun tapping_the_newest_card_opens_the_edit_tool_on_it_and_back_returns_home() {
        val photos = photos(5)
        launch(edits = FakeSavedPhotoSource(photos)) { scenario ->
            waitFor { count(homeRecentTag(0)) > 0 }
            compose.onNodeWithTag(homeRecentTag(0)).performClick()

            val shownPhoto = hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, photos[0].uri)
            waitFor { count(HOME_SCREEN_TAG) == 0 && compose.onAllNodes(shownPhoto).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(
                hasText(app.getString(R.string.tool_edit)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
            ).assertExists()

            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            waitFor { count(TOOL_SCREEN_TAG) == 0 && count(HOME_SCREEN_TAG) > 0 }
            compose.onNodeWithTag(HOME_SCREEN_TAG).assertIsDisplayed()
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }

    /** (c) The round arrow beside "Recent edits" opens the My edits tab, selected on the bar. */
    @Test
    fun the_arrow_selects_my_edits() {
        launch(edits = FakeSavedPhotoSource(photos(3))) {
            waitFor { count(HOME_RECENT_MORE_TAG) > 0 }
            compose.onNodeWithTag(HOME_RECENT_MORE_TAG).performClick()
            waitFor { count(MY_EDITS_SCREEN_TAG) > 0 && count(HOME_SCREEN_TAG) == 0 }
            compose.onNodeWithTag(kansoTabTag(KansoTab.MY_EDITS)).assertIsSelected()
        }
    }

    /**
     * (d) With nothing saved, `home-recent-empty` shows the empty message (exact text), and there is
     * no card and no placeholder.
     */
    @Test
    fun an_empty_source_shows_the_message_and_no_card_or_placeholder() {
        launch(edits = FakeSavedPhotoSource()) {
            waitFor { count(HOME_RECENT_EMPTY_TAG) > 0 }
            compose.onNodeWithTag(HOME_RECENT_EMPTY_TAG)
                .assertIsDisplayed()
                .assertTextEquals(app.getString(R.string.home_hero_recent_empty))
            compose.onAllNodes(isCard).assertCountEquals(0)
            compose.onAllNodes(isSkeleton).assertCountEquals(0)
        }
    }

    /**
     * (e) Edit's shortcut, photo 1 in its gallery, Save (the saver answers Uri X and the edits
     * source then holds X as its newest photo), the Saved screen, its close button: within ten
     * seconds of real time Home's first card is X.
     */
    @Test
    fun a_photo_just_saved_is_first_when_home_returns() {
        val edits = FakeSavedPhotoSource(photos(4))
        val savedUri = "$FAKE_SCHEME://color/FF4CAF50"
        val library = FakePhotoSource(albums = listOf(Album(1L, "Camera")), allPhotos = libraryPhotos())
        launch(edits = edits, library = library, saver = FakePhotoSaver(result = SaveResult.Saved(savedUri))) {
            waitFor { count(homeRecentTag(0)) > 0 }
            assertEquals(photos(4)[0].uri, card0Uri())

            openToolGallery(compose, Tool.EDIT)
            waitFor { count(GALLERY_SCREEN_TAG) > 0 && count(galleryPhotoTag(1L)) > 0 }
            compose.onNodeWithTag(galleryPhotoTag(1L)).performClick()
            waitFor { count(TOOL_SCREEN_TAG) > 0 }
            waitFor { compose.onAllNodes(hasTestTag(TOOL_SAVE_TAG) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
            waitFor { count(SAVED_SCREEN_TAG) > 0 }

            // The phone's MediaStore would now list the new photo first: so does the fake.
            edits.photos = listOf(photo(id = 99L, uri = savedUri, modifiedSeconds = NEWEST_SECONDS + 600L)) + edits.photos

            compose.onNodeWithTag(SAVED_CLOSE_TAG).performClick()
            waitFor { count(SAVED_SCREEN_TAG) == 0 && count(HOME_SCREEN_TAG) > 0 }
            waitFor { card0Uri() == savedUri }
            assertEquals(savedUri, card0Uri())
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Grants photo access, installs the solid-colour image loader, loads the fakes ([edits] for
     * the saved photos, [library] for the gallery, [saver] for Save, the fake decoder), starts the
     * app, waits for Home and runs [body] with the scenario.
     */
    private fun launch(
        edits: FakeSavedPhotoSource,
        library: FakePhotoSource = FakePhotoSource(albums = emptyList(), allPhotos = emptyList()),
        saver: FakePhotoSaver = FakePhotoSaver(),
        body: (ActivityScenario<MainActivity>) -> Unit,
    ) {
        shadowOf(app).grantPermissions(*photoPermissionsFor(35).toTypedArray(), Manifest.permission.CAMERA)
        Coil.setImageLoader(
            ImageLoader.Builder(app)
                .components { add(fetchers) }
                .dispatcher(Dispatchers.Unconfined)
                .build(),
        )
        loadKoinModules(
            module {
                single<SavedPhotoSource> { edits }
                single<PhotoSource> { library }
                single<PhotoSaver> { saver }
                single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
            },
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForHome(compose)
            body(scenario)
        }
    }

    /** Returns the photo cards composed in the strip, whatever their place. */
    private fun cards(): List<SemanticsNode> = compose.onAllNodes(isCard).fetchSemanticsNodes()

    /** Returns the Uri the first card of the strip stands for, or null while there is none. */
    private fun card0Uri(): String? = compose.onAllNodesWithTag(homeRecentTag(0)).fetchSemanticsNodes()
        .singleOrNull()?.config?.getOrNull(HOME_RECENT_URI_KEY)

    /**
     * Returns how far Home's column is scrolled, in pixels: the value of the vertical scroll of
     * the one scrolling node that holds the carousel.
     */
    private fun homeScroll(): Float {
        val column = compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
                hasAnyDescendant(hasTestTag(HOME_CAROUSEL_TAG)),
        ).fetchSemanticsNode()
        return column.config[SemanticsProperties.VerticalScrollAxisRange].value()
    }

    /** Returns the spoken name Home should give [photo]'s card. */
    private fun expectedName(photo: Photo): String =
        app.getString(R.string.my_edits_photo_description, savedAtText(photo.modifiedSeconds))

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

    /** Waits up to ten seconds of real time for [condition], letting the main thread run in between. */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** Values, photos and matchers the tests share. */
    private companion object {
        /** The time of the newest sample photo, in seconds since 1970 (October 2026). */
        const val NEWEST_SECONDS = 1_791_800_000L

        /** The largest difference per colour channel still counted as the same colour. */
        const val COLOUR_TOLERANCE = 8

        /** How far left of card 0, in pixels, Home's background is sampled. */
        const val OUTSIDE_PX = 6f

        /** Returns the solid colour (ARGB) of the edited photo at [index] of [photos]. */
        fun colourOf(index: Int): Int =
            android.graphics.Color.HSVToColor(floatArrayOf(index * 29f, 0.6f, 0.8f))

        /**
         * Returns [count] saved photos, newest first (photo 0 the newest), one day apart so their
         * dates differ, each a solid colour through the `fake` scheme.
         */
        fun photos(count: Int): List<Photo> = (0 until count).map { i ->
            photo(
                id = (i + 1).toLong(),
                uri = "$FAKE_SCHEME://color/${"%08X".format(colourOf(i))}",
                modifiedSeconds = NEWEST_SECONDS - i * 86_400L,
            )
        }

        /** Returns one saved photo with [id] and [uri], saved at [modifiedSeconds]. */
        fun photo(id: Long, uri: String, modifiedSeconds: Long): Photo = Photo(
            id = id,
            uri = uri,
            name = "Kanso_$id.jpg",
            albumId = 1L,
            modifiedSeconds = modifiedSeconds,
        )

        /** The phone's photo library for the save walk: three photos in one album, photo 1 the newest. */
        fun libraryPhotos(): List<Photo> = (1..3).map { i ->
            Photo(
                id = i.toLong(),
                uri = "content://media/external/images/media/${500 + i}",
                name = "IMG_$i.jpg",
                albumId = 1L,
                modifiedSeconds = NEWEST_SECONDS - i * 60L,
            )
        }

        /** Matches every photo card of the strip (`home-recent-<digits>`), whatever its place. */
        val isCard = SemanticsMatcher("is a recent photo card") { node ->
            val tag = node.config.getOrNull(SemanticsProperties.TestTag)
            tag != null && tag.startsWith(HOME_RECENT_CARD_TAG_PREFIX) &&
                tag.removePrefix(HOME_RECENT_CARD_TAG_PREFIX).all { it.isDigit() } &&
                tag.length > HOME_RECENT_CARD_TAG_PREFIX.length
        }

        /** Matches every placeholder card of the strip, whatever its place. */
        val isSkeleton = SemanticsMatcher("is a recent placeholder card") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(HOME_RECENT_SKELETON_TAG_PREFIX) == true
        }
    }
}
