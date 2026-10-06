package com.phongkaster.kanso.gallery

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.gallery.GALLERY_BODY_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_PARTIAL_NOTICE_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_REFUSED_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_SCREEN_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_SETTINGS_BUTTON_TAG
import com.phongkaster.kanso.ui.gallery.GALLERY_SHARE_MORE_TAG
import com.phongkaster.kanso.ui.gallery.GalleryScreen
import com.phongkaster.kanso.ui.gallery.GalleryViewModel
import com.phongkaster.kanso.ui.permission.photoPermissionsFor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Checks how the gallery asks for photo access: which permissions it requests on each Android
 * version, that it asks once and never again by itself, and what it shows when access is
 * granted, partly granted or refused.
 *
 * The system permission dialog cannot be shown in a JVM test, so the gallery is given a
 * [RecordingRegistry] through LocalActivityResultRegistryOwner: it records every request instead
 * of showing a dialog, and the test answers it. What checkSelfPermission reports is set with
 * Robolectric's grantPermissions (nothing is granted until a test grants it).
 *
 * The gallery is hosted in a plain ComponentActivity started with ActivityScenario. Its content
 * is set from an activity-lifecycle callback, so it is set again when the activity is re-created,
 * the way a real activity sets its content in onCreate.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class GalleryPermissionTest {

    /** Finds Compose nodes in whatever activity the test starts. */
    @get:Rule
    val compose = createEmptyComposeRule()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Records the permission requests and lets the test answer them. */
    private val registry = RecordingRegistry()

    /** The running activity, closed after each test. */
    private var scenario: ActivityScenario<ComponentActivity>? = null

    /** Sets the gallery as the content of every ComponentActivity created during the test. */
    private val contentSetter = object : Application.ActivityLifecycleCallbacks {
        /** Shows the gallery, with the recording registry, in the newly created activity. */
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            // Built outside the composable: lint forbids constructing a ViewModel while drawing.
            val viewModel = GalleryViewModel(Tool.EDIT, ViewModelClearedProbe())
            (activity as? ComponentActivity)?.setContent {
                val owner = object : ActivityResultRegistryOwner {
                    override val activityResultRegistry: ActivityResultRegistry = registry
                }
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                    MaterialTheme {
                        GalleryScreen(
                            viewModel = viewModel,
                            onBack = {},
                        )
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

    /** Registers the callback that hosts the gallery. */
    @Before
    fun registerContent() {
        app.registerActivityLifecycleCallbacks(contentSetter)
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

    // --- Which permissions fit which Android version ---------------------------------------

    /** Android 8.1 and 12L ask for READ_EXTERNAL_STORAGE only. */
    @Test
    fun old_versions_use_read_external_storage() {
        assertEquals(listOf(Manifest.permission.READ_EXTERNAL_STORAGE), photoPermissionsFor(27))
        assertEquals(listOf(Manifest.permission.READ_EXTERNAL_STORAGE), photoPermissionsFor(32))
    }

    /** Android 13 asks for READ_MEDIA_IMAGES only. */
    @Test
    fun android_13_uses_read_media_images() {
        assertEquals(listOf(Manifest.permission.READ_MEDIA_IMAGES), photoPermissionsFor(33))
    }

    /** Android 14 and 15 ask for READ_MEDIA_IMAGES and READ_MEDIA_VISUAL_USER_SELECTED. */
    @Test
    fun android_14_and_later_add_user_selected() {
        assertEquals(BOTH, photoPermissionsFor(34))
        assertEquals(BOTH, photoPermissionsFor(35))
    }

    // --- The first request -------------------------------------------------------------------

    /** On Android 14 the first show requests both media permissions. */
    @Test
    @Config(sdk = [34])
    fun first_show_on_34_requests_both() {
        assertFirstRequest(BOTH)
    }

    /** On Android 15 the first show requests both media permissions. */
    @Test
    @Config(sdk = [35])
    fun first_show_on_35_requests_both() {
        assertFirstRequest(BOTH)
    }

    /** On Android 13 the first show requests READ_MEDIA_IMAGES only. */
    @Test
    @Config(sdk = [33])
    fun first_show_on_33_requests_media_images() {
        assertFirstRequest(listOf(Manifest.permission.READ_MEDIA_IMAGES))
    }

    /** On Android 12L the first show requests READ_EXTERNAL_STORAGE only. */
    @Test
    @Config(sdk = [32])
    fun first_show_on_32_requests_external_storage() {
        assertFirstRequest(listOf(Manifest.permission.READ_EXTERNAL_STORAGE))
    }

    /** On Android 8.1 the first show requests READ_EXTERNAL_STORAGE only. */
    @Test
    @Config(sdk = [27])
    fun first_show_on_27_requests_external_storage() {
        assertFirstRequest(listOf(Manifest.permission.READ_EXTERNAL_STORAGE))
    }

    // --- Refused -----------------------------------------------------------------------------

    /** Android 15, refused: the explanation and the settings button work, and no second ask. */
    @Test
    @Config(sdk = [35])
    fun refused_on_35_explains_and_opens_settings() {
        assertRefusedBehaviour()
    }

    /** Android 11, refused: the explanation and the settings button work, and no second ask. */
    @Test
    @Config(sdk = [30])
    fun refused_on_30_explains_and_opens_settings() {
        assertRefusedBehaviour()
    }

    // --- Partial and full access --------------------------------------------------------------

    /** Android 14, only some photos shared: the notice shows and "share more" asks again. */
    @Test
    @Config(sdk = [34])
    fun partial_access_on_34_shows_notice_and_share_more_asks_again() {
        launchGallery()
        assertEquals(1, registry.requests.size)

        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        answer(
            mapOf(
                Manifest.permission.READ_MEDIA_IMAGES to false,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED to true,
            ),
        )

        compose.onNodeWithTag(GALLERY_PARTIAL_NOTICE_TAG).assertIsDisplayed()
        compose.onNodeWithTag(GALLERY_BODY_TAG).assertIsDisplayed()

        compose.onNodeWithTag(GALLERY_SHARE_MORE_TAG).performClick()
        compose.waitForIdle()
        assertEquals(2, registry.requests.size)
        assertEquals(BOTH, registry.requests.last())
    }

    /** Android 15, all photos allowed: the photo area shows and there is no partial notice. */
    @Test
    @Config(sdk = [35])
    fun full_access_shows_the_body_without_notice() {
        launchGallery()
        shadowOf(app).grantPermissions(*BOTH.toTypedArray())
        answer(BOTH.associateWith { true })

        compose.onNodeWithTag(GALLERY_BODY_TAG).assertIsDisplayed()
        assertEquals(0, nodeCount(GALLERY_PARTIAL_NOTICE_TAG))
        assertEquals(0, nodeCount(GALLERY_REFUSED_TAG))
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Opens the gallery with nothing granted and checks the one request asks for [expected]. */
    private fun assertFirstRequest(expected: List<String>) {
        launchGallery()
        compose.onNodeWithTag(GALLERY_SCREEN_TAG).assertIsDisplayed()
        assertEquals(1, registry.requests.size)
        assertEquals(expected, registry.requests.single())
    }

    /**
     * Opens the gallery, refuses every permission, then checks the refused panel, its settings
     * button, and that neither re-creating the activity nor pausing and resuming it asks again.
     */
    private fun assertRefusedBehaviour() {
        launchGallery()
        assertEquals(1, registry.requests.size)
        answer(registry.requests.single().associateWith { false })

        assertRefusedShown()

        compose.onNodeWithTag(GALLERY_SETTINGS_BUTTON_TAG).performClick()
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

    /** Checks that the refused panel, its explanation and the settings button are shown. */
    private fun assertRefusedShown() {
        val context: Context = app
        compose.onNodeWithTag(GALLERY_REFUSED_TAG).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.gallery_refused_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.gallery_refused_message))
            .assertIsDisplayed()
        compose.onNodeWithTag(GALLERY_SETTINGS_BUTTON_TAG).assertIsDisplayed()
    }

    /** Starts the activity that shows the gallery and waits until it is drawn. */
    private fun launchGallery() {
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        compose.waitForIdle()
    }

    /** Answers the latest permission request with [result] and waits for the screen to update. */
    private fun answer(result: Map<String, Boolean>) {
        registry.answerLatest(result)
        compose.waitForIdle()
    }

    /** Counts the nodes carrying [tag]. */
    private fun nodeCount(tag: String): Int =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** Values the tests share. */
    private companion object {
        /** The two permissions Android 14 and later ask for, in the order the gallery asks. */
        val BOTH = listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
    }
}

/**
 * An ActivityResultRegistry that shows no dialog: it records the permissions of each request
 * and lets the test answer the latest one with [answerLatest].
 */
class RecordingRegistry : ActivityResultRegistry() {

    /** The permissions of every request so far, oldest first. */
    val requests = mutableListOf<List<String>>()

    /** The request code of the latest request, used to deliver its answer. */
    private var latestRequestCode = -1

    /** Records the request instead of showing the system permission dialog. */
    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        @Suppress("UNCHECKED_CAST")
        requests += (input as Array<String>).toList()
        latestRequestCode = requestCode
    }

    /** Delivers [result] as the answer to the latest request. */
    fun answerLatest(result: Map<String, Boolean>) {
        check(latestRequestCode != -1) { "No permission request to answer" }
        dispatchResult(latestRequestCode, result)
    }
}
