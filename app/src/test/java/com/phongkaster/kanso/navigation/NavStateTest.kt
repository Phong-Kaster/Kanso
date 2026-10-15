package com.phongkaster.kanso.navigation

import android.Manifest
import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.os.Parcel
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.media.PhotoSource
import com.phongkaster.kanso.data.media.SavedPhotoSource
import com.phongkaster.kanso.data.photo.PhotoLoader
import com.phongkaster.kanso.data.settings.SETTINGS_SCOPE
import com.phongkaster.kanso.di.appModule
import com.phongkaster.kanso.gallery.FakePhotoSource
import com.phongkaster.kanso.myedits.FakeSavedPhotoSource
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.home.HOME_SCREEN_TAG
import com.phongkaster.kanso.ui.myedits.MY_EDITS_SCREEN_TAG
import com.phongkaster.kanso.ui.myedits.myEditsTileTag
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_TAG
import com.phongkaster.kanso.ui.saved.SAVED_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.settings.SETTINGS_SCREEN_TAG
import com.phongkaster.kanso.ui.settings.THEME_MODE_SYSTEM_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * DoD 33, the open tab: with My edits (or Settings) selected, turning the phone (the activity is
 * recreated) and Android closing the app in the background (process death) both bring the app back
 * on that same tab, selected on the floating bar, with its content. The new navigation keys
 * ([MyEdits], [EditedPhoto]) are `@Serializable` and survive a round trip through JSON, which is
 * how `rememberNavBackStack` saves them.
 *
 * Why the tab survives at all. The open tab is not a separate setting: it is the key on top of the
 * back stack (`[Home, MyEdits]`), and the back stack is saved with the activity's state. Every new
 * activity starts again from `[Home]`, so if the tab comes back, the saved stack was restored.
 *
 * "With its first tile": on My edits the first tile is checked by opening it, so the photo viewer
 * must show the newest saved photo. On Settings the first row (System, of the mode choice) is
 * checked to be there.
 *
 * Process death is simulated as `ToolPhotoKeyTest` does it: the activity's saved state is captured
 * and passed through a Parcel, the activity is destroyed, the settings file is released and Koin
 * is started afresh (every singleton built anew, as in a new process), and a new activity is
 * created from that state. The fakes are loaded before the first activity starts, and again into
 * the new Koin.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class NavStateTest {

    /** Finds Compose nodes in whatever activity the test starts. */
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

    /** My edits selected, then the activity is recreated: My edits is back, selected, with its first tile. */
    @Test
    fun my_edits_survives_recreate() {
        loadKoinModules(fakes())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openAndCheck(KansoTab.MY_EDITS)
            scenario.recreate()
            compose.waitForIdle()
            assertTabRestored(KansoTab.MY_EDITS)
            assertFirstTileOpensNewestPhoto()
        }
    }

    /** My edits selected, then process death: My edits is back, selected, with its first tile. */
    @Test
    fun my_edits_survives_process_death() {
        loadKoinModules(fakes())
        val first = Robolectric.buildActivity(MainActivity::class.java).setup()
        openAndCheck(KansoTab.MY_EDITS)
        killAndRestore(first) {
            assertTabRestored(KansoTab.MY_EDITS)
            assertFirstTileOpensNewestPhoto()
        }
    }

    /** Settings selected, then the activity is recreated: Settings is back, selected, with its first row. */
    @Test
    fun settings_survives_recreate() {
        loadKoinModules(fakes())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openAndCheck(KansoTab.SETTINGS)
            scenario.recreate()
            compose.waitForIdle()
            assertTabRestored(KansoTab.SETTINGS)
            waitFor { count(THEME_MODE_SYSTEM_TAG) > 0 }
        }
    }

    /** Settings selected, then process death: Settings is back, selected, with its first row. */
    @Test
    fun settings_survives_process_death() {
        loadKoinModules(fakes())
        val first = Robolectric.buildActivity(MainActivity::class.java).setup()
        openAndCheck(KansoTab.SETTINGS)
        killAndRestore(first) {
            assertTabRestored(KansoTab.SETTINGS)
            waitFor { count(THEME_MODE_SYSTEM_TAG) > 0 }
        }
    }

    /**
     * The new keys turn into JSON and back unchanged. `serializer()` only exists for a class marked
     * `@Serializable`, so this test would not compile if either key lost the mark.
     */
    @Test
    fun the_new_keys_round_trip_through_json() {
        val myEditsJson = Json.encodeToString(MyEdits.serializer(), MyEdits)
        assertSame(MyEdits, Json.decodeFromString(MyEdits.serializer(), myEditsJson))

        val photo = EditedPhoto("content://media/external/images/media/42")
        val photoJson = Json.encodeToString(EditedPhoto.serializer(), photo)
        assertEquals(photo, Json.decodeFromString(EditedPhoto.serializer(), photoJson))
        assertTrue("The Uri is not in the saved text: $photoJson", photoJson.contains(photo.photoUri))
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Grants photo access and returns the fakes: [editedPhotos] as Kanso's saved photos, an empty
     * photo library and the fake decoder (the viewer decodes the photo it shows).
     */
    private fun fakes(): Module {
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA, *photoPermissionsFor(35).toTypedArray())
        return module {
            single<SavedPhotoSource> { FakeSavedPhotoSource(editedPhotos) }
            single<PhotoSource> { FakePhotoSource(albums = emptyList(), allPhotos = emptyList()) }
            single<PhotoLoader> { FakePhotoLoader(app, ToolFixtures.gradient(60, 40)) }
        }
    }

    /** From Home, opens [tab] through the floating bar and checks it is shown and selected. */
    private fun openAndCheck(tab: KansoTab) {
        waitForHome(compose)
        openTab(compose, tab)
        assertTabRestored(tab)
        compose.waitForIdle()
    }

    /**
     * Waits until [tab]'s screen is shown with Home gone, then checks that [tab] is the one
     * selected tab on the floating bar.
     */
    private fun assertTabRestored(tab: KansoTab) {
        val screenTag = if (tab == KansoTab.MY_EDITS) MY_EDITS_SCREEN_TAG else SETTINGS_SCREEN_TAG
        waitFor { count(screenTag) > 0 && count(HOME_SCREEN_TAG) == 0 && count(kansoTabTag(tab)) > 0 }
        compose.onNodeWithTag(kansoTabTag(tab)).assertIsSelected()
        KansoTab.entries.filter { it != tab }.forEach { other ->
            compose.onNodeWithTag(kansoTabTag(other)).assertIsNotSelected()
        }
    }

    /**
     * Waits for My edits' first tile, opens it, and checks that the viewer shows the newest saved
     * photo: the first tile is the one it should be, not a placeholder.
     */
    private fun assertFirstTileOpensNewestPhoto() {
        waitFor { count(myEditsTileTag(0)) > 0 }
        compose.onNodeWithTag(myEditsTileTag(0)).tapAboveBar()
        val newest = hasTestTag(SAVED_PHOTO_TAG) and SemanticsMatcher.expectValue(SAVED_PHOTO_URI_KEY, editedPhotos.first().uri)
        waitFor { compose.onAllNodes(newest).fetchSemanticsNodes().isNotEmpty() }
    }

    /**
     * Simulates process death for the activity driven by [first], then runs [checkRestored]
     * against the activity Android would bring back.
     *
     * The saved-state Bundle is captured and passed through a Parcel (as Android stores it outside
     * the app), the activity is destroyed and its window removed. The settings DataStore's scope
     * is cancelled and joined, which releases the settings file (only one DataStore may hold it),
     * then Koin is started afresh with the app's module and the fakes, as a new process would. A
     * new activity is created from the state; it is destroyed after [checkRestored], whether the
     * checks pass or not.
     */
    private fun killAndRestore(first: ActivityController<MainActivity>, checkRestored: () -> Unit) {
        val saved = Bundle()
        first.pause().stop()
        first.saveInstanceState(saved)
        first.destroy()
        // Make sure the old window is gone, so only the new activity's screen can be found.
        try {
            first.get().windowManager.removeViewImmediate(first.get().window.decorView)
        } catch (e: RuntimeException) {
            // Already removed by the destroy above.
        }
        val restored = throughParcel(saved)

        runBlocking {
            KoinPlatform.getKoin().get<CoroutineScope>(SETTINGS_SCOPE).coroutineContext[Job]!!.cancelAndJoin()
        }
        stopKoin()
        startKoin {
            androidContext(app)
            // Later modules win, as in TestKansoApplication: the fakes replace the real sources.
            modules(appModule, fakes())
        }

        val second = Robolectric.buildActivity(MainActivity::class.java).setup(restored)
        try {
            compose.waitForIdle()
            checkRestored()
        } finally {
            second.pause().stop().destroy()
        }
    }

    /** Writes [bundle] into a Parcel and reads it back, as Android does with saved state. */
    private fun throughParcel(bundle: Bundle): Bundle {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(bundle)
            parcel.setDataPosition(0)
            val copy = parcel.readBundle(javaClass.classLoader)
            assertNotNull("The saved state did not survive the Parcel", copy)
            return copy!!
        } finally {
            parcel.recycle()
        }
    }

    /** Waits up to ten seconds for [condition], letting the main thread run in between. */
    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Counts the nodes carrying [tag]. */
    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** The photos the tests share. */
    private companion object {
        /** Kanso's saved photos, newest first, as My edits lists them: tile 0 is photo 0. */
        val editedPhotos: List<Photo> = (0 until 4).map { i ->
            Photo(
                id = 500L + i,
                uri = "content://media/external/images/media/${500 + i}",
                name = "Kanso_$i.jpg",
                albumId = 1L,
                modifiedSeconds = 1_791_000_000L - i * 3_600L,
            )
        }
    }
}
