package com.phongkaster.kanso.camera

import android.Manifest
import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.MainActivity
import com.phongkaster.kanso.R
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.gallery.RecordingRegistry
import com.phongkaster.kanso.navigation.KANSO_CAMERA_BUTTON_TAG
import com.phongkaster.kanso.navigation.tapCameraButton
import com.phongkaster.kanso.ui.camera.CAMERA_REFUSED_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SCREEN_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SETTINGS_BUTTON_TAG
import com.phongkaster.kanso.ui.camera.CAMERA_SHUTTER_TAG
import com.phongkaster.kanso.ui.camera.CameraController
import com.phongkaster.kanso.ui.camera.CameraScreen
import com.phongkaster.kanso.ui.camera.CameraViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * DoD 20: the camera asks for camera access the first time it opens, never at launch, and a
 * refusal is handled like the gallery's: an explanation, a button to the app's system settings,
 * and no second request by itself, not even after the activity is re-created.
 *
 * The first test drives the real app (MainActivity) and reads the request Robolectric recorded.
 * The others host the camera screen in a plain ComponentActivity with a [RecordingRegistry], which
 * records each request instead of showing the system dialog and lets the test answer "denied".
 * Nothing is granted unless a test grants it.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CameraPermissionTest {

    /** Finds Compose nodes in whatever activity the test starts. */
    @get:org.junit.Rule
    val compose = createEmptyComposeRule()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Records the permission requests of the hosted camera screen and lets the test answer. */
    private val registry = RecordingRegistry()

    /** The running activity hosting the camera screen, closed after each test. */
    private var scenario: ActivityScenario<ComponentActivity>? = null

    /** Sets the camera screen as the content of every ComponentActivity created while registered. */
    private val contentSetter = object : Application.ActivityLifecycleCallbacks {
        /** Shows the camera screen, with the recording registry, in the newly created activity. */
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            // MainActivity is a ComponentActivity too; only the plain host gets this content.
            if (activity::class != ComponentActivity::class) return
            // Built outside the composable: lint forbids constructing a ViewModel while drawing.
            val viewModel = CameraViewModel(FakeCameraController(), ViewModelClearedProbe())
            (activity as ComponentActivity).setContent {
                val owner = object : ActivityResultRegistryOwner {
                    override val activityResultRegistry: ActivityResultRegistry = registry
                }
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                    MaterialTheme {
                        CameraScreen(viewModel = viewModel, onBack = {}, onKept = {})
                    }
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

    /**
     * Closes the activity and removes the callback. KansoApplication starts Koin for every
     * Robolectric test, and Koin's state outlives the test inside the same JVM, so it is stopped.
     */
    @After
    fun tearDown() {
        scenario?.close()
        app.unregisterActivityLifecycleCallbacks(contentSetter)
        stopKoin()
    }

    /**
     * Launching the app requests nothing; tapping the round Camera button beside the floating tab
     * bar (found by its tag, `nav-camera`) requests CAMERA only.
     */
    @Test
    fun launch_asks_for_nothing_and_opening_the_camera_asks_for_camera() {
        ActivityScenario.launch(MainActivity::class.java).use { main ->
            loadKoinModules(module { factory<CameraController> { FakeCameraController() } })
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag(KANSO_CAMERA_BUTTON_TAG).fetchSemanticsNodes().isNotEmpty()
            }
            main.onActivity { assertNull(shadowOf(it).lastRequestedPermission) }

            tapCameraButton(compose)
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithTag(CAMERA_SCREEN_TAG).fetchSemanticsNodes().isNotEmpty()
            }

            main.onActivity { activity ->
                val request = shadowOf(activity).lastRequestedPermission
                assertEquals(
                    listOf(Manifest.permission.CAMERA),
                    request?.requestedPermissions?.toList(),
                )
            }
        }
    }

    /** The hosted camera screen's first show requests exactly CAMERA, once. */
    @Test
    fun first_show_requests_camera_once() {
        launchCamera()
        compose.onNodeWithTag(CAMERA_SCREEN_TAG).assertIsDisplayed()
        assertEquals(listOf(listOf(Manifest.permission.CAMERA)), registry.requests)
    }

    /**
     * Denied: the refused panel, its explanation and its settings button show, the button opens
     * the app's page in the system settings, and neither re-creating the activity nor pausing
     * and resuming it asks again.
     */
    @Test
    fun refused_explains_opens_settings_and_does_not_ask_again() {
        launchCamera()
        assertEquals(1, registry.requests.size)
        registry.answerLatest(mapOf(Manifest.permission.CAMERA to false))
        compose.waitForIdle()

        assertRefusedShown()

        compose.onNodeWithTag(CAMERA_SETTINGS_BUTTON_TAG).performClick()
        compose.waitForIdle()
        val started = shadowOf(app).nextStartedActivity
        assertNotNull("The settings button started nothing", started)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, started.action)
        assertEquals("package:com.phongkaster.kanso", started.data.toString())

        scenario!!.recreate()
        compose.waitForIdle()
        assertEquals(1, registry.requests.size)
        assertRefusedShown()

        scenario!!.moveToState(Lifecycle.State.STARTED)
        scenario!!.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
        assertEquals(1, registry.requests.size)
        assertRefusedShown()
    }

    /** Granted in the dialog: the shutter shows and the refused panel does not. */
    @Test
    fun granted_shows_the_shutter() {
        launchCamera()
        shadowOf(app).grantPermissions(Manifest.permission.CAMERA)
        registry.answerLatest(mapOf(Manifest.permission.CAMERA to true))
        compose.waitForIdle()

        compose.onNodeWithTag(CAMERA_SHUTTER_TAG).assertExists()
        assertEquals(0, compose.onAllNodesWithTag(CAMERA_REFUSED_TAG).fetchSemanticsNodes().size)
    }

    /** Checks that the refused panel, its explanation and the settings button are shown. */
    private fun assertRefusedShown() {
        compose.onNodeWithTag(CAMERA_REFUSED_TAG).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.camera_refused_title)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.camera_refused_message)).assertIsDisplayed()
        compose.onNodeWithTag(CAMERA_SETTINGS_BUTTON_TAG).assertIsDisplayed()
        assertEquals(0, compose.onAllNodesWithTag(CAMERA_SHUTTER_TAG).fetchSemanticsNodes().size)
    }

    /** Hosts the camera screen in a plain activity and waits until it is drawn. */
    private fun launchCamera() {
        app.registerActivityLifecycleCallbacks(contentSetter)
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        compose.waitForIdle()
    }
}
