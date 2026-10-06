package com.phongkaster.kanso.save

import android.Manifest
import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.photo.FullSizeDecoder
import com.phongkaster.kanso.data.photo.MediaStorePhotoSaver
import com.phongkaster.kanso.data.photo.SaveResult
import com.phongkaster.kanso.tool.ToolFixtures
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * Checks Save on Android 9 and 8.1 (DoD 12 and 13), where there is no scoped storage: the saver
 * writes the file itself into `<Pictures>/Kanso/`, never over an existing file, asks the media
 * scanner to add it to the gallery, and needs the storage-write permission until it is granted.
 *
 * Robolectric gives shared storage a temporary folder of its own; the test reads the same
 * folder through Environment, exactly as the saver does.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [28])
class LegacySaverTest {

    /** A fresh folder for the fixture photos. */
    @get:Rule
    val temp = TemporaryFolder()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** Mounts shared storage and empties the Kanso album, so each test starts clean. */
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

    /** Android 9: a turned photo lands upright in `<Pictures>/Kanso/` and is scanned. */
    @Test
    fun a_turned_photo_is_written_upright_and_scanned_on_28() {
        assertTurnedPhotoWrittenAndScanned()
    }

    /** Android 8.1: the same. */
    @Test
    @Config(sdk = [27])
    fun a_turned_photo_is_written_upright_and_scanned_on_27() {
        assertTurnedPhotoWrittenAndScanned()
    }

    /** Android 9: two saves in the same second give two files; the first is not overwritten. */
    @Test
    fun two_saves_in_the_same_second_make_two_files_on_28() {
        val first = ToolFixtures.writeJpeg(temp.newFile("first.jpg"), ToolFixtures.gradient(300, 200))
        val second = ToolFixtures.writeJpeg(temp.newFile("second.jpg"), ToolFixtures.twoHalves(300, 200))
        val saver = MediaStorePhotoSaver(app, now = { FIXED_TIME_MILLIS })

        val firstResult = runBlocking { saver.save(ToolFixtures.serve(app, first, "first.jpg")) }
        val secondResult = runBlocking { saver.save(ToolFixtures.serve(app, second, "second.jpg")) }

        assertTrue(firstResult is SaveResult.Saved && secondResult is SaveResult.Saved)
        val files = legacyKansoFiles()
        assertEquals(2, files.size)
        files.forEach { assertTrue("Bad name ${it.name}", SAVED_NAME.matches(it.name)) }
        val plain = files.single { !it.name.removeSuffix(".jpg").matches(Regex(""".*_\d{6}_\d+""")) }
        val numbered = files.single { it !== plain }
        assertEquals(plain.name.removeSuffix(".jpg") + "_1.jpg", numbered.name)
        // Each file holds its own photo: the second save did not write over the first.
        assertArrayEquals(first.readBytes(), plain.readBytes())
        assertArrayEquals(second.readBytes(), numbered.readBytes())
    }

    /** Android 9: the too-large fallback copies the photo as it is, EXIF turn included. */
    @Test
    fun a_decode_out_of_memory_falls_back_to_a_verbatim_copy_on_28() {
        val source = ToolFixtures.writeJpeg(
            temp.newFile("huge.jpg"),
            ToolFixtures.twoHalves(400, 200),
            ExifInterface.ORIENTATION_ROTATE_90,
        )
        val saver = MediaStorePhotoSaver(app, decoder = FullSizeDecoder { throw OutOfMemoryError("test") })

        val result = runBlocking { saver.save(ToolFixtures.serve(app, source, "huge.jpg")) }

        val file = legacyKansoFiles().single()
        assertEquals(SaveResult.Saved(Uri.fromFile(file).toString(), keptOriginalOrientation = true), result)
        assertArrayEquals(source.readBytes(), file.readBytes())
    }

    /** A photo that cannot be read gives Failed and leaves no file behind. */
    @Test
    fun an_unreadable_photo_fails_and_leaves_nothing_on_28() {
        val result = runBlocking { MediaStorePhotoSaver(app).save(ToolFixtures.MISSING_URI) }

        assertEquals(SaveResult.Failed, result)
        assertTrue(legacyKansoFiles().isEmpty())
    }

    /** Android 9 needs the storage-write permission until it is granted. */
    @Test
    fun write_permission_is_needed_until_granted_on_28() {
        assertWritePermissionNeededUntilGranted()
    }

    /** Android 8.1 needs the storage-write permission until it is granted. */
    @Test
    @Config(sdk = [27])
    fun write_permission_is_needed_until_granted_on_27() {
        assertWritePermissionNeededUntilGranted()
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Saves a 400 x 200 photo tagged "turn a quarter" and checks the file: in `<Pictures>/Kanso`,
     * well named, 200 x 400 with no turn left, handed to the media scanner, and the original
     * unchanged.
     */
    private fun assertTurnedPhotoWrittenAndScanned() {
        val source = ToolFixtures.writeJpeg(
            temp.newFile("turned.jpg"),
            ToolFixtures.twoHalves(400, 200),
            ExifInterface.ORIENTATION_ROTATE_90,
        )
        val before = sha256(source)

        val result = runBlocking {
            MediaStorePhotoSaver(app).save(ToolFixtures.serve(app, source, "turned.jpg"))
        }

        val file = legacyKansoFiles().single()
        assertEquals(SaveResult.Saved(Uri.fromFile(file).toString(), false), result)
        assertEquals("Kanso", file.parentFile!!.name)
        assertEquals(legacyKansoFolder().absoluteFile, file.parentFile!!.absoluteFile)
        assertTrue("Bad name ${file.name}", SAVED_NAME.matches(file.name))
        assertTrue(looksLikeJpeg(file.readBytes()))
        assertEquals(200 to 400, jpegSize(file))
        assertTrue(exifOrientation(file) in setOf(ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL))
        assertTrue(
            "The media scanner was not asked: ${ShadowMediaScannerConnection.getSavedPaths()}",
            file.absolutePath in ShadowMediaScannerConnection.getSavedPaths(),
        )
        assertEquals(before, sha256(source))
    }

    /** Checks needsWritePermission before and after WRITE_EXTERNAL_STORAGE is granted. */
    private fun assertWritePermissionNeededUntilGranted() {
        val saver = MediaStorePhotoSaver(app)
        assertTrue(saver.needsWritePermission())
        shadowOf(app).grantPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        assertFalse(saver.needsWritePermission())
    }
}
