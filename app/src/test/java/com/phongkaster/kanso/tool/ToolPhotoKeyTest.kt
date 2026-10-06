package com.phongkaster.kanso.tool

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.di.appModule
import com.phongkaster.kanso.navigation.Home
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.navigation.ToolPhoto
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.TOOL_SCREEN_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolViewModel
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks that the chosen tool and photo survive rotation and process death because they travel
 * in the navigation key `ToolPhoto(tool, photoUri)` (DoD 10).
 *
 * The test hosts a small copy of the app's navigation in a plain ComponentActivity: a back stack
 * made with rememberNavBackStack, starting at Home, and a NavDisplay that shows the real tool
 * screen (its ViewModel built by Koin from the key, as in the app) for a `ToolPhoto` key. The
 * test pushes `ToolPhoto(EDIT, uri)` itself. Since every new activity starts again from Home, the
 * tool screen can only come back if the saved back stack is restored.
 *
 * The photo is a real EXIF-6 JPEG served under a content Uri, decoded by the real loader.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ToolPhotoKeyTest {

    /** Finds Compose nodes in whatever activity the test starts. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** A fresh folder for the fixture file, deleted after each test. */
    @get:Rule
    val temp = TemporaryFolder()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** The back stack of the most recently composed host, set from inside the composition. */
    private var backStack: MutableList<NavKey>? = null

    /** Sets the small navigation host as the content of every ComponentActivity created. */
    private val contentSetter = object : Application.ActivityLifecycleCallbacks {
        /** Shows the host in the newly created activity. */
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            (activity as? ComponentActivity)?.setContent {
                MaterialTheme {
                    // Home is only the starting key; the restored stack replaces it if saved.
                    val stack = rememberNavBackStack(Home)
                    SideEffect { backStack = stack }
                    NavDisplay(
                        backStack = stack,
                        onBack = { stack.removeLastOrNull() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        entryProvider = entryProvider {
                            entry<Home> { Text("home") }
                            entry<ToolPhoto> { key ->
                                // Built by Koin from the key, exactly as the app's host does.
                                val viewModel = koinViewModel<ToolViewModel>(
                                    parameters = { parametersOf(key.tool, key.photoUri) },
                                )
                                ToolScreen(viewModel = viewModel, onBack = {})
                            }
                        },
                    )
                }
            }
        }

        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    /** Registers the callback that hosts the navigation. */
    @Before
    fun registerContent() {
        app.registerActivityLifecycleCallbacks(contentSetter)
    }

    /**
     * Removes the callback and stops Koin: KansoApplication starts it for every Robolectric
     * test, and its state outlives the test inside the same JVM.
     */
    @After
    fun tearDown() {
        app.unregisterActivityLifecycleCallbacks(contentSetter)
        stopKoin()
    }

    /** The key turns into JSON and back unchanged, the way the back stack is saved. */
    @Test
    fun the_key_round_trips_through_json() {
        val key = ToolPhoto(Tool.EDIT, "content://media/external/images/media/42")

        val json = Json.encodeToString(ToolPhoto.serializer(), key)
        val back = Json.decodeFromString(ToolPhoto.serializer(), json)

        assertEquals(key, back)
        assertTrue("The Uri is not in the saved text: $json", json.contains(key.photoUri))
        assertTrue("The tool is not in the saved text: $json", json.contains("EDIT"))
    }

    /** Rotation (recreating the activity) shows the same tool on the same photo. */
    @Test
    fun recreate_keeps_the_tool_and_the_photo() {
        val key = ToolPhoto(Tool.EDIT, servedFixture())
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            compose.waitForIdle()
            openToolScreen(key)
            assertToolScreenShows(key)

            backStack = null
            scenario.recreate()
            compose.waitForIdle()

            assertEquals(listOf(Home, key), backStack?.toList())
            assertToolScreenShows(key)
        }
    }

    /**
     * Process death: the activity's saved state is captured and passed through a Parcel (as
     * Android stores it outside the app), the activity is destroyed, Koin is started afresh as a
     * new process would, and a new activity restored from that state shows the same tool and
     * photo.
     */
    @Test
    fun process_death_keeps_the_tool_and_the_photo() {
        val key = ToolPhoto(Tool.EDIT, servedFixture())

        val first = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        compose.waitForIdle()
        openToolScreen(key)
        assertToolScreenShows(key)

        // What Android keeps when it kills a background app: the saved-state Bundle, flattened.
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

        // A new process: every singleton, the photo loader included, is built again.
        stopKoin()
        startKoin {
            androidContext(app)
            modules(appModule)
        }

        backStack = null
        val second = Robolectric.buildActivity(ComponentActivity::class.java).setup(restored)
        try {
            compose.waitForIdle()
            assertEquals(listOf(Home, key), backStack?.toList())
            assertToolScreenShows(key)
        } finally {
            second.pause().stop().destroy()
        }
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Writes a 400 x 200 EXIF-6 JPEG and serves it, returning its content Uri as text. */
    private fun servedFixture(): String {
        val file = ToolFixtures.writeJpeg(
            temp.newFile("kept.jpg"),
            ToolFixtures.twoHalves(400, 200),
            ExifInterface.ORIENTATION_ROTATE_90,
        )
        val context: Context = app
        return ToolFixtures.serve(context, file, "kept.jpg")
    }

    /** Pushes [key] onto the hosted back stack, as choosing a photo in the gallery does. */
    private fun openToolScreen(key: ToolPhoto) {
        val stack = backStack
        assertNotNull("The host has not been composed", stack)
        compose.runOnUiThread { stack!!.add(key) }
        compose.waitForIdle()
    }

    /**
     * Waits for the tool screen, then checks it names [key]'s tool in its top bar and draws the
     * photo with [key]'s Uri, upright (taller than wide), each exactly once.
     */
    private fun assertToolScreenShows(key: ToolPhoto) {
        val image = hasTestTag(TOOL_IMAGE_TAG) and SemanticsMatcher.expectValue(TOOL_PHOTO_URI_KEY, key.photoUri)
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(image).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(image).assertCountEquals(1)
        compose.onAllNodes(
            hasText(app.getString(key.tool.labelRes)) and hasAnyAncestor(hasTestTag(TOOL_SCREEN_TAG)),
        ).assertCountEquals(1)
        compose.onAllNodes(hasText("home")).assertCountEquals(0)

        val bounds = compose.onAllNodes(image).fetchSemanticsNodes().single().boundsInRoot
        assertTrue("The photo is not upright: $bounds", bounds.height > bounds.width)
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
}
