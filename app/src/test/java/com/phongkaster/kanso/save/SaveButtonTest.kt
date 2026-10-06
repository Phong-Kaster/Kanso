package com.phongkaster.kanso.save

import android.Manifest
import android.app.Application
import android.os.Environment
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.photo.MediaStorePhotoSaver
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.data.photo.SaveResult
import com.phongkaster.kanso.di.ViewModelClearedProbe
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.tool.FakePhotoLoader
import com.phongkaster.kanso.tool.ToolFixtures
import com.phongkaster.kanso.ui.tool.SAVE_MESSAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SAVE_PROGRESS_TAG
import com.phongkaster.kanso.ui.tool.TOOL_SAVE_TAG
import com.phongkaster.kanso.ui.tool.ToolScreen
import com.phongkaster.kanso.ui.tool.ToolViewModel
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowEnvironment
import org.robolectric.shadows.ShadowMediaScannerConnection

/**
 * Checks the Save action on the tool screen (DoD 12 and 13): it calls the saver once the photo
 * is shown, hands the new picture to `onSaved` once, asks for the storage-write permission on
 * Android 8.1 and 9 only, and explains a refusal, a failure and a photo kept sideways.
 *
 * The permission dialog cannot be shown in a JVM test, so the screen gets a
 * [SinglePermissionRegistry] that records each request and lets the test answer it. The photo
 * on screen comes from a [FakePhotoLoader]; the saved bytes come from a real fixture file.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class SaveButtonTest {

    /** Starts an empty activity to host the screen. */
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** A fresh folder for the fixture photo and the fake MediaStore's files. */
    @get:Rule
    val temp = TemporaryFolder()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Records permission requests instead of showing the system dialog. */
    private val registry = SinglePermissionRegistry()

    /** The Uri of every picture handed to onSaved, oldest first. */
    private val saved = CopyOnWriteArrayList<String>()

    /** Mounts shared storage and empties the Kanso album used on Android 8.1 and 9. */
    @Before
    fun cleanAlbum() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        legacyKansoFolder().deleteRecursively()
    }

    /** Stops Koin, which KansoApplication starts for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    // --- With a fake saver -------------------------------------------------------------------

    /** Tapping Save calls the saver with the photo, asks for nothing, and calls onSaved once. */
    @Test
    fun save_calls_the_saver_and_hands_the_picture_on_once() {
        val saver = FakePhotoSaver(result = SaveResult.Saved("content://media/external_primary/images/media/9"))
        show(saver, PHOTO_URI)

        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsDisplayed().assertIsEnabled().performClick()
        waitUntil { saved.isNotEmpty() }

        assertEquals(listOf(PHOTO_URI), saver.calls.toList())
        assertEquals(listOf("content://media/external_primary/images/media/9"), saved.toList())
        assertTrue("A permission was requested: ${registry.requests}", registry.requests.isEmpty())
        assertTrue(compose.onAllNodes(hasTestTag(SAVE_MESSAGE_TAG)).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsEnabled()
    }

    /** While a save runs, Save is disabled and shows a spinner and "Saving…"; then it is back. */
    @Test
    fun save_is_disabled_and_shows_progress_while_saving() {
        val gate = CompletableDeferred<Unit>()
        val saver = FakePhotoSaver(gate = gate)
        show(saver, PHOTO_URI)

        compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
        waitUntil { compose.onAllNodes(hasTestTag(TOOL_SAVE_PROGRESS_TAG)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsNotEnabled()
        compose.onNode(hasText(app.getString(R.string.tool_saving)), useUnmergedTree = true).assertExists()

        gate.complete(Unit)
        waitUntil { saved.isNotEmpty() }
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsEnabled()
        assertEquals(1, saver.calls.size)
    }

    /** A photo too large to turn is saved as it is, and the screen says so. */
    @Test
    fun a_photo_kept_sideways_is_explained() {
        val saver = FakePhotoSaver(result = SaveResult.Saved("content://media/x/1", keptOriginalOrientation = true))
        show(saver, PHOTO_URI)

        compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
        waitForMessage()

        compose.onNodeWithTag(SAVE_MESSAGE_TAG).assertIsDisplayed()
        compose.onNode(hasText(app.getString(R.string.save_kept_orientation))).assertIsDisplayed()
        assertEquals(listOf("content://media/x/1"), saved.toList())
    }

    /** A failed save shows the failure message and hands nothing on. */
    @Test
    fun a_failed_save_is_explained() {
        show(FakePhotoSaver(result = SaveResult.Failed), PHOTO_URI)

        compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
        waitForMessage()

        compose.onNode(hasText(app.getString(R.string.save_failed))).assertIsDisplayed()
        assertTrue(saved.isEmpty())
    }

    /** Save cannot be tapped while the photo is not shown (here: it cannot be opened). */
    @Test
    fun save_is_disabled_without_a_photo() {
        val saver = FakePhotoSaver()
        val viewModel = ToolViewModel(Tool.EDIT, PHOTO_URI, FakePhotoLoader(app, null), ViewModelClearedProbe(), saver)
        host(viewModel)

        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsDisplayed().assertIsNotEnabled()
    }

    // --- With the real saver: which Android versions ask --------------------------------------

    /** Android 15: Save asks for nothing and writes a row into the (fake) MediaStore. */
    @Test
    fun android_15_saves_without_asking() {
        assertSavesWithoutAsking()
    }

    /** Android 10: Save asks for nothing and writes a row into the (fake) MediaStore. */
    @Test
    @Config(sdk = [29])
    fun android_10_saves_without_asking() {
        assertSavesWithoutAsking()
    }

    /** Android 9: Save asks for WRITE_EXTERNAL_STORAGE; granting it writes the file and scans it. */
    @Test
    @Config(sdk = [28])
    fun android_9_asks_and_saves_when_granted() {
        assertAsksThenSavesWhenGranted()
    }

    /** Android 8.1: the same. */
    @Test
    @Config(sdk = [27])
    fun android_8_1_asks_and_saves_when_granted() {
        assertAsksThenSavesWhenGranted()
    }

    /** Android 9: refusing shows the message and writes nothing. */
    @Test
    @Config(sdk = [28])
    fun android_9_refused_shows_a_message_and_writes_nothing() {
        assertRefusalWritesNothing()
    }

    /** Android 8.1: refusing shows the message and writes nothing. */
    @Test
    @Config(sdk = [27])
    fun android_8_1_refused_shows_a_message_and_writes_nothing() {
        assertRefusalWritesNothing()
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Saves through the real saver on Android 10+ and checks no permission was requested. */
    private fun assertSavesWithoutAsking() {
        val media = FakeMediaProvider.install(temp.newFolder("media"))
        show(MediaStorePhotoSaver(app), servedPhoto())

        compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
        waitUntil { saved.isNotEmpty() }

        assertTrue("A permission was requested: ${registry.requests}", registry.requests.isEmpty())
        val row = media.rows.values.single()
        assertEquals(listOf(row.uri.toString()), saved.toList())
        assertTrue(looksLikeJpeg(row.file.readBytes()))
    }

    /** Taps Save without the permission, checks the request, grants it and checks the file. */
    private fun assertAsksThenSavesWhenGranted() {
        show(MediaStorePhotoSaver(app), servedPhoto())

        compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
        compose.waitForIdle()
        assertEquals(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), registry.requests.toList())
        assertTrue("Wrote before the answer", legacyKansoFiles().isEmpty())

        shadowOf(app).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        compose.runOnUiThread { registry.answerLatest(true) }
        waitUntil { saved.isNotEmpty() }

        val file = legacyKansoFiles().single()
        assertTrue("Bad name ${file.name}", SAVED_NAME.matches(file.name))
        assertEquals(200 to 400, jpegSize(file))
        assertTrue(file.absolutePath in ShadowMediaScannerConnection.getSavedPaths())
        assertEquals(1, saved.size)
        assertEquals(1, registry.requests.size)
    }

    /** Taps Save without the permission, refuses it, and checks the message and the empty album. */
    private fun assertRefusalWritesNothing() {
        show(MediaStorePhotoSaver(app), servedPhoto())

        compose.onNodeWithTag(TOOL_SAVE_TAG).performClick()
        compose.waitForIdle()
        assertEquals(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), registry.requests.toList())

        compose.runOnUiThread { registry.answerLatest(false) }
        waitForMessage()

        compose.onNodeWithTag(SAVE_MESSAGE_TAG).assertIsDisplayed()
        compose.onNode(hasText(app.getString(R.string.save_permission_denied))).assertIsDisplayed()
        assertTrue("A file was written", legacyKansoFiles().isEmpty())
        assertTrue(saved.isEmpty())
        // The screen is still there and Save can be tried again.
        compose.onNodeWithTag(TOOL_SAVE_TAG).assertIsEnabled()
    }

    /** Writes a 400 x 200 photo tagged "turn a quarter" and serves it; returns its Uri. */
    private fun servedPhoto(): String {
        val file = ToolFixtures.writeJpeg(
            temp.newFile("photo.jpg"),
            ToolFixtures.twoHalves(400, 200),
            ExifInterface.ORIENTATION_ROTATE_90,
        )
        return ToolFixtures.serve(app, file, "photo.jpg")
    }

    /** Shows the tool screen on [photoUri] with [saver], and waits until the photo is drawn. */
    private fun show(saver: PhotoSaver, photoUri: String) {
        // Built outside the composable: lint forbids constructing a ViewModel while drawing.
        val loader = FakePhotoLoader(app, ToolFixtures.gradient(600, 400))
        host(ToolViewModel(Tool.EDIT, photoUri, loader, ViewModelClearedProbe(), saver))
        waitUntil { compose.onAllNodes(hasTestTag(TOOL_IMAGE_TAG)).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Sets the tool screen for [viewModel] as the content, with the recording registry. */
    private fun host(viewModel: ToolViewModel) {
        compose.setContent {
            val owner = object : ActivityResultRegistryOwner {
                override val activityResultRegistry: ActivityResultRegistry = registry
            }
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                MaterialTheme {
                    ToolScreen(viewModel = viewModel, onBack = {}, onSaved = { saved += it })
                }
            }
        }
        compose.waitForIdle()
    }

    /** Waits until the save message is on screen. */
    private fun waitForMessage() {
        waitUntil { compose.onAllNodes(hasTestTag(SAVE_MESSAGE_TAG)).fetchSemanticsNodes().isNotEmpty() }
    }

    /**
     * Waits until [condition] holds. The save finishes on a background thread and comes back
     * to the main thread, so the main looper is run on each check.
     */
    private fun waitUntil(condition: () -> Boolean) {
        compose.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    /** Values the tests share. */
    private companion object {
        /** The photo the fake-saver tests show; nothing is read from it. */
        const val PHOTO_URI = "content://media/external/images/media/7"
    }
}
