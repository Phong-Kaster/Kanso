package com.phongkaster.kanso.save

import android.content.Context
import android.provider.MediaStore
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks Save on Android 15 (DoD 12): a new, full-size, upright JPEG named `Kanso_<time>.jpg` goes
 * into `Pictures/Kanso/` through MediaStore, hidden while it is written, and the original never
 * changes.
 *
 * MediaStore is replaced by a [FakeMediaProvider] backed by a temporary folder, so the test sees
 * every row the saver adds and the bytes it writes. The source photos are real JPEG files served
 * under content Uris, as in the tool screen's tests.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class MediaStoreSaverTest {

    /** A fresh folder for the fixtures and the fake MediaStore's files. */
    @get:Rule
    val temp = TemporaryFolder()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** The fake MediaStore, installed before each test. */
    private lateinit var media: FakeMediaProvider

    /** Puts the fake MediaStore in place. */
    @Before
    fun installMedia() {
        media = FakeMediaProvider.install(temp.newFolder("media"))
    }

    /** Stops Koin, which KansoApplication starts for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * A 4000 x 3000 photo tagged "turn a quarter" (EXIF 6) is saved as a 3000 x 4000 JPEG with
     * no turn left in it, in a row of `Pictures/Kanso/` that is pending while written and
     * published after; the original's bytes are the same before and after.
     */
    @Test
    fun a_turned_photo_is_saved_upright_at_full_size_in_pictures_kanso() {
        val source = ToolFixtures.writeJpeg(
            temp.newFile("big.jpg"),
            ToolFixtures.gradient(4000, 3000),
            ExifInterface.ORIENTATION_ROTATE_90,
        )
        val before = sha256(source)
        val uri = ToolFixtures.serve(context, source, "big.jpg")

        val result = runBlocking { MediaStorePhotoSaver(context, now = { FIXED_TIME_MILLIS }).save(uri) }

        assertTrue("Not saved: $result", result is SaveResult.Saved)
        result as SaveResult.Saved
        assertFalse(result.keptOriginalOrientation)

        val inserted = media.inserts.single()
        assertEquals("Pictures/Kanso/", inserted.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
        assertEquals("image/jpeg", inserted.getAsString(MediaStore.MediaColumns.MIME_TYPE))
        assertEquals(1, inserted.getAsInteger(MediaStore.MediaColumns.IS_PENDING))
        val name = inserted.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
        assertTrue("Bad name $name", SAVED_NAME.matches(name))

        val row = media.rows.values.single()
        assertEquals(row.uri.toString(), result.uri)
        assertEquals(0, row.values.getAsInteger(MediaStore.MediaColumns.IS_PENDING))
        assertEquals(0, media.updates.last().second.getAsInteger(MediaStore.MediaColumns.IS_PENDING))

        assertTrue(looksLikeJpeg(row.file.readBytes()))
        assertEquals(3000 to 4000, jpegSize(row.file))
        assertTrue(
            "The saved photo still carries a turn",
            exifOrientation(row.file) in setOf(ExifInterface.ORIENTATION_UNDEFINED, ExifInterface.ORIENTATION_NORMAL),
        )
        assertEquals("The original changed", before, sha256(source))
        assertTrue(media.deletes.isEmpty())
    }

    /** A photo that needs no turning is copied byte for byte, and the original is unchanged. */
    @Test
    fun an_upright_photo_is_copied_byte_for_byte() {
        val source = ToolFixtures.writeJpeg(temp.newFile("upright.jpg"), ToolFixtures.gradient(600, 400))
        val before = sha256(source)
        val uri = ToolFixtures.serve(context, source, "upright.jpg")

        val result = runBlocking { MediaStorePhotoSaver(context).save(uri) }

        assertEquals(SaveResult.Saved(media.rows.values.single().uri.toString(), false), result)
        assertArrayEquals(source.readBytes(), media.rows.values.single().file.readBytes())
        assertEquals(before, sha256(source))
    }

    /** A photo tagged "upright" (EXIF 1) is copied byte for byte as well. */
    @Test
    fun a_photo_tagged_normal_is_copied_byte_for_byte() {
        val source = ToolFixtures.writeJpeg(
            temp.newFile("normal.jpg"),
            ToolFixtures.gradient(600, 400),
            ExifInterface.ORIENTATION_NORMAL,
        )
        val uri = ToolFixtures.serve(context, source, "normal.jpg")

        val result = runBlocking { MediaStorePhotoSaver(context).save(uri) }

        assertTrue(result is SaveResult.Saved)
        assertArrayEquals(source.readBytes(), media.rows.values.single().file.readBytes())
    }

    /**
     * When decoding a turned photo runs out of memory, the photo is copied as it is, keeping
     * its EXIF turn (assumption A-001), and the result says so for the screen to explain.
     */
    @Test
    fun a_decode_out_of_memory_falls_back_to_a_verbatim_copy() {
        val source = ToolFixtures.writeJpeg(
            temp.newFile("huge.jpg"),
            ToolFixtures.twoHalves(400, 200),
            ExifInterface.ORIENTATION_ROTATE_90,
        )
        val before = sha256(source)
        val uri = ToolFixtures.serve(context, source, "huge.jpg")
        val runsOutOfMemory = FullSizeDecoder { throw OutOfMemoryError("Too large for the test") }

        val result = runBlocking { MediaStorePhotoSaver(context, decoder = runsOutOfMemory).save(uri) }

        val row = media.rows.values.single()
        assertEquals(SaveResult.Saved(row.uri.toString(), keptOriginalOrientation = true), result)
        assertArrayEquals(source.readBytes(), row.file.readBytes())
        assertEquals(ExifInterface.ORIENTATION_ROTATE_90, exifOrientation(row.file))
        assertEquals(0, row.values.getAsInteger(MediaStore.MediaColumns.IS_PENDING))
        assertEquals(before, sha256(source))
    }

    /** Two saves in the same second make two rows with different names; nothing is replaced. */
    @Test
    fun two_saves_in_the_same_second_make_two_files() {
        val source = ToolFixtures.writeJpeg(temp.newFile("twice.jpg"), ToolFixtures.gradient(300, 200))
        val uri = ToolFixtures.serve(context, source, "twice.jpg")
        val saver = MediaStorePhotoSaver(context, now = { FIXED_TIME_MILLIS })

        val first = runBlocking { saver.save(uri) }
        val second = runBlocking { saver.save(uri) }

        assertTrue(first is SaveResult.Saved && second is SaveResult.Saved)
        assertNotEquals((first as SaveResult.Saved).uri, (second as SaveResult.Saved).uri)
        val names = media.inserts.map { it.getAsString(MediaStore.MediaColumns.DISPLAY_NAME) }
        assertEquals(2, names.size)
        assertEquals(2, names.toSet().size)
        names.forEach { assertTrue("Bad name $it", SAVED_NAME.matches(it)) }
        assertEquals(names[0].removeSuffix(".jpg") + "_1.jpg", names[1])
        assertEquals(2, media.rows.size)
        media.rows.values.forEach { assertArrayEquals(source.readBytes(), it.file.readBytes()) }
        assertTrue(media.deletes.isEmpty())
    }

    /** A photo that cannot be read gives Failed and leaves no row behind. */
    @Test
    fun an_unreadable_photo_fails_and_leaves_nothing() {
        val result = runBlocking { MediaStorePhotoSaver(context).save(ToolFixtures.MISSING_URI) }

        assertEquals(SaveResult.Failed, result)
        assertTrue("A row was left behind", media.rows.isEmpty())
    }

    /** Android 10 and later never need the storage-write permission. */
    @Test
    fun no_write_permission_is_needed() {
        assertFalse(MediaStorePhotoSaver(context).needsWritePermission())
    }

    /** Android 10 needs no storage-write permission either. */
    @Test
    @Config(sdk = [29])
    fun no_write_permission_is_needed_on_29() {
        assertFalse(MediaStorePhotoSaver(context).needsWritePermission())
    }
}
