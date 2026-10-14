package com.phongkaster.kanso.save

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.photo.MediaStorePhotoSaver
import com.phongkaster.kanso.data.photo.SaveResult
import com.phongkaster.kanso.data.photo.extensionFor
import com.phongkaster.kanso.data.photo.mimeTypeFor
import com.phongkaster.kanso.data.photo.saveFormatFor
import com.phongkaster.kanso.data.photo.uniqueName
import com.phongkaster.kanso.tool.ToolFixtures
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowEnvironment
import org.robolectric.shadows.ShadowMediaScannerConnection

/**
 * Checks Save of an edited picture (Run 8): `MediaStorePhotoSaver.save(bitmap)` writes the bitmap
 * exactly as it is into `Pictures/Kanso`, as a JPEG named `.jpg` and typed `image/jpeg` when it is
 * opaque, and as a PNG named `.png` and typed `image/png` when it has an alpha channel (a
 * cut-out), at the bitmap's own pixel size. A failing write gives Failed and leaves nothing
 * behind, and the photo the edit came from is never touched.
 *
 * Android 15 goes through MediaStore, replaced by a [FakeMediaProvider]; Android 9 and 8.1 write
 * the file themselves into Robolectric's stand-in Pictures folder and ask the media scanner, one
 * `@Test` per Android version. The format, type, extension and naming rules are also checked on
 * their own, as plain functions.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class SaveEditedTest {

    /** A fresh folder for the fixtures and the fake MediaStore's files. */
    @get:Rule
    val temp = TemporaryFolder()

    /** The application under test. */
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** The fake MediaStore, installed before each test. */
    private lateinit var media: FakeMediaProvider

    /**
     * Puts the fake MediaStore in place, mounts shared storage and empties the Kanso album of the
     * older Androids, so each test starts clean.
     */
    @Before
    fun setUp() {
        media = FakeMediaProvider.install(temp.newFolder("media"))
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        legacyKansoFolder().deleteRecursively()
    }

    /** Stops Koin, which KansoApplication starts for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * Android 15: an opaque 640 x 480 edit is saved as a JPEG named `Kanso_<time>.jpg`, typed
     * `image/jpeg`, in `Pictures/Kanso/`, pending while written and published after, at 640 x 480;
     * the photo file the edit came from keeps its bytes.
     */
    @Test
    fun an_opaque_edit_is_saved_as_a_jpeg_in_pictures_kanso() {
        val source = ToolFixtures.writeJpeg(temp.newFile("source.jpg"), ToolFixtures.gradient(600, 400))
        val before = sha256(source)
        val edited = opaque(640, 480)

        val result = runBlocking { MediaStorePhotoSaver(context, now = { FIXED_TIME_MILLIS }).save(edited) }

        val row = media.rows.values.single()
        assertEquals(SaveResult.Saved(row.uri.toString()), result)
        val inserted = media.inserts.single()
        assertEquals("Pictures/Kanso/", inserted.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
        assertEquals("image/jpeg", inserted.getAsString(MediaStore.MediaColumns.MIME_TYPE))
        assertEquals(1, inserted.getAsInteger(MediaStore.MediaColumns.IS_PENDING))
        val name = inserted.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
        assertTrue("Bad name $name", SAVED_NAME.matches(name))
        assertEquals(0, row.values.getAsInteger(MediaStore.MediaColumns.IS_PENDING))

        assertTrue("Not a JPEG", looksLikeJpeg(row.file.readBytes()))
        assertEquals(640 to 480, jpegSize(row.file))
        assertEquals("The source photo changed", before, sha256(source))
        assertFalse("The edit was recycled", edited.isRecycled)
        assertTrue(media.deletes.isEmpty())
    }

    /**
     * Android 15: an edit with an alpha channel (a cut-out) is saved as a PNG named
     * `Kanso_<time>.png`, typed `image/png`, at its own 300 x 500 size, and its see-through
     * corner stays see-through.
     */
    @Test
    fun an_edit_with_alpha_is_saved_as_a_png_in_pictures_kanso() {
        val edited = seeThrough(300, 500)

        val result = runBlocking { MediaStorePhotoSaver(context, now = { FIXED_TIME_MILLIS }).save(edited) }

        val row = media.rows.values.single()
        assertEquals(SaveResult.Saved(row.uri.toString()), result)
        val inserted = media.inserts.single()
        assertEquals("Pictures/Kanso/", inserted.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
        assertEquals("image/png", inserted.getAsString(MediaStore.MediaColumns.MIME_TYPE))
        val name = inserted.getAsString(MediaStore.MediaColumns.DISPLAY_NAME)
        assertTrue("Bad name $name", SAVED_PNG_NAME.matches(name))

        val bytes = row.file.readBytes()
        assertTrue("Not a PNG", looksLikePng(bytes))
        assertEquals(300 to 500, jpegSize(row.file))
        val decoded = BitmapFactory.decodeFile(row.file.absolutePath)
        assertEquals("The see-through corner was filled in", 0, Color.alpha(decoded.getPixel(0, 0)))
    }

    /**
     * Android 15: when writing the picture's bytes fails (the stream throws, as on a full disk),
     * the result is Failed and the row MediaStore made for it is deleted again: nothing is left.
     */
    @Test
    fun a_failing_stream_gives_failed_and_leaves_no_row() {
        val failing = ThrowingMediaProvider.install()

        val result = runBlocking { MediaStorePhotoSaver(context).save(opaque(64, 48)) }

        assertEquals(SaveResult.Failed, result)
        assertEquals("No row was made, so the stream was never tried", 1, failing.inserted.size)
        assertTrue("A row was left behind", failing.rows.isEmpty())
        assertEquals(failing.inserted, failing.deletes)
    }

    /** Android 9: an opaque edit is written as a `.jpg` in `<Pictures>/Kanso`, scanned as `image/jpeg`. */
    @Test
    @Config(sdk = [28])
    fun an_opaque_edit_is_written_as_a_jpeg_and_scanned_as_jpeg_on_28() {
        assertLegacyJpeg()
    }

    /** Android 8.1: the same. */
    @Test
    @Config(sdk = [27])
    fun an_opaque_edit_is_written_as_a_jpeg_and_scanned_as_jpeg_on_27() {
        assertLegacyJpeg()
    }

    /** Android 9: an edit with alpha is written as a `.png` in `<Pictures>/Kanso`, scanned as `image/png`. */
    @Test
    @Config(sdk = [28])
    fun an_edit_with_alpha_is_written_as_a_png_and_scanned_as_png_on_28() {
        assertLegacyPng()
    }

    /** Android 8.1: the same. */
    @Test
    @Config(sdk = [27])
    fun an_edit_with_alpha_is_written_as_a_png_and_scanned_as_png_on_27() {
        assertLegacyPng()
    }

    /**
     * Android 9: when the picture cannot be written (here the encoder throws on a recycled
     * bitmap, after the file was made), the result is Failed and the half-made file is deleted.
     */
    @Test
    @Config(sdk = [28])
    fun a_failing_write_gives_failed_and_leaves_no_file_on_28() {
        val broken = opaque(64, 48).apply { recycle() }

        val result = runBlocking { MediaStorePhotoSaver(context).save(broken) }

        assertEquals(SaveResult.Failed, result)
        assertTrue("A file was left behind: ${legacyKansoFiles()}", legacyKansoFiles().isEmpty())
    }

    /** The format follows the alpha channel: PNG with one, JPEG without, for a flag or a bitmap. */
    @Test
    fun the_format_follows_the_alpha_channel() {
        assertEquals(Bitmap.CompressFormat.PNG, saveFormatFor(true))
        assertEquals(Bitmap.CompressFormat.JPEG, saveFormatFor(false))
        assertEquals(Bitmap.CompressFormat.PNG, saveFormatFor(seeThrough(10, 10)))
        assertEquals(Bitmap.CompressFormat.JPEG, saveFormatFor(opaque(10, 10)))
    }

    /** The MIME type and the extension follow the format: `image/png` and `png`, else `image/jpeg` and `jpg`. */
    @Test
    fun the_type_and_extension_follow_the_format() {
        assertEquals("image/png", mimeTypeFor(Bitmap.CompressFormat.PNG))
        assertEquals("image/jpeg", mimeTypeFor(Bitmap.CompressFormat.JPEG))
        assertEquals("png", extensionFor(Bitmap.CompressFormat.PNG))
        assertEquals("jpg", extensionFor(Bitmap.CompressFormat.JPEG))
    }

    /**
     * A unique name ends in the given extension (`.jpg` by default), and numbers itself `_1`,
     * `_2` only past names taken with that same extension.
     */
    @Test
    fun a_unique_name_uses_the_extension_it_is_given() {
        val base = "Kanso_20261006_120000"
        assertEquals("$base.jpg", uniqueName(base, emptySet()))
        assertEquals("$base.png", uniqueName(base, emptySet(), "png"))
        assertEquals("${base}_1.png", uniqueName(base, setOf("$base.png"), "png"))
        assertEquals("${base}_2.png", uniqueName(base, setOf("$base.png", "${base}_1.png"), "png"))
        // A JPEG of the same name does not block the PNG, nor the other way round.
        assertEquals("$base.png", uniqueName(base, setOf("$base.jpg"), "png"))
        assertEquals("$base.jpg", uniqueName(base, setOf("$base.png"), "jpg"))
    }

    // --- Helpers -----------------------------------------------------------------------------

    /**
     * Saves an opaque 640 x 480 edit on Android 8.1 or 9 and checks the file: in
     * `<Pictures>/Kanso`, named `.jpg`, a JPEG at 640 x 480, handed to the scanner as `image/jpeg`.
     */
    private fun assertLegacyJpeg() {
        val result = runBlocking { MediaStorePhotoSaver(context).save(opaque(640, 480)) }

        val file = legacyKansoFiles().single()
        assertEquals(SaveResult.Saved(Uri.fromFile(file).toString()), result)
        assertEquals(legacyKansoFolder().absoluteFile, file.parentFile!!.absoluteFile)
        assertTrue("Bad name ${file.name}", SAVED_NAME.matches(file.name))
        assertTrue("Not a JPEG", looksLikeJpeg(file.readBytes()))
        assertEquals(640 to 480, jpegSize(file))
        assertScanned(file, "image/jpeg", notType = "image/png")
    }

    /**
     * Saves a 300 x 500 edit with alpha on Android 8.1 or 9 and checks the file: in
     * `<Pictures>/Kanso`, named `.png`, a PNG at 300 x 500, handed to the scanner as `image/png`.
     */
    private fun assertLegacyPng() {
        val result = runBlocking { MediaStorePhotoSaver(context).save(seeThrough(300, 500)) }

        val file = legacyKansoFiles().single()
        assertEquals(SaveResult.Saved(Uri.fromFile(file).toString()), result)
        assertEquals(legacyKansoFolder().absoluteFile, file.parentFile!!.absoluteFile)
        assertTrue("Bad name ${file.name}", SAVED_PNG_NAME.matches(file.name))
        assertTrue("Not a PNG", looksLikePng(file.readBytes()))
        assertEquals(300 to 500, jpegSize(file))
        assertScanned(file, "image/png", notType = "image/jpeg")
    }

    /** Checks the media scanner was asked to add [file] as [type], and never as [notType]. */
    private fun assertScanned(file: File, type: String, notType: String) {
        val paths = ShadowMediaScannerConnection.getSavedPaths()
        val types = ShadowMediaScannerConnection.getSavedMimeTypes()
        assertTrue("The scanner was not asked for ${file.absolutePath}: $paths", file.absolutePath in paths)
        assertTrue("The scanner was not told $type: $types", type in types)
        assertFalse("The scanner was told $notType: $types", notType in types)
    }

    /** Draws a [width] x [height] picture with no alpha channel, as most edits are. */
    private fun opaque(width: Int, height: Int): Bitmap {
        val bitmap = ToolFixtures.gradient(width, height)
        bitmap.setHasAlpha(false)
        assertFalse("The opaque fixture has alpha", bitmap.hasAlpha())
        return bitmap
    }

    /**
     * Draws a [width] x [height] picture with an alpha channel, as a cut-out is: see-through
     * everywhere except a red disc in its middle.
     */
    private fun seeThrough(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }
        Canvas(bitmap).drawCircle(width / 2f, height / 2f, minOf(width, height) / 4f, paint)
        assertTrue("The see-through fixture has no alpha", bitmap.hasAlpha())
        return bitmap
    }

    /** Tells whether [bytes] start with the PNG signature (89 50 4E 47 0D 0A 1A 0A). */
    private fun looksLikePng(bytes: ByteArray): Boolean =
        bytes.size > 8 && PNG_SIGNATURE.indices.all { bytes[it] == PNG_SIGNATURE[it].toByte() }

    /**
     * A stand-in for MediaStore whose write streams always fail, as a full disk does: it makes a
     * row when asked, hands out a stream for it that throws on the first byte, and records every
     * row made and deleted.
     */
    class ThrowingMediaProvider : ContentProvider() {

        /** The rows that exist now. */
        val rows = mutableListOf<Uri>()

        /** Every row ever made, oldest first. */
        val inserted = mutableListOf<Uri>()

        /** Every row deleted, oldest first. */
        val deletes = mutableListOf<Uri>()

        /** Starts the provider; there is nothing to set up. */
        override fun onCreate(): Boolean = true

        /** Makes a row and registers a stream for it that fails on the first write. */
        override fun insert(uri: Uri, values: ContentValues?): Uri {
            val row = ContentUris.withAppendedId(uri, (inserted.size + 1).toLong())
            rows += row
            inserted += row
            shadowOf(RuntimeEnvironment.getApplication().contentResolver)
                .registerOutputStreamSupplier(row) { FailingStream() }
            return row
        }

        /** Returns no rows, so every name counts as free. */
        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor = MatrixCursor(projection ?: arrayOf(FakeMediaProvider.DISPLAY_NAME_COLUMN))

        /** Changes nothing; a save never gets this far. */
        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        /** Removes the row [uri] names and records the delete. */
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            deletes += uri
            return if (rows.remove(uri)) 1 else 0
        }

        /** Every row would hold an image. */
        override fun getType(uri: Uri): String = "image/jpeg"

        /** A stream that refuses every byte, as one on a full disk does. */
        class FailingStream : OutputStream() {

            /** Throws: nothing can be written. */
            override fun write(b: Int) {
                throw IOException("No space left on the test's disk")
            }
        }

        /** Puts the failing stand-in in place. */
        companion object {
            /** Builds it and registers it as the `media` provider for the rest of the test. */
            fun install(): ThrowingMediaProvider {
                val provider = Robolectric.buildContentProvider(ThrowingMediaProvider::class.java)
                    .create("media")
                    .get()
                ShadowContentResolver.registerProviderInternal("media", provider)
                return provider
            }
        }
    }

    /** Values shared by the tests. */
    private companion object {
        /** The name every saved PNG must have: `Kanso_<yyyyMMdd>_<HHmmss>[_n].png`. */
        val SAVED_PNG_NAME = Regex("""Kanso_\d{8}_\d{6}(_\d+)?\.png""")

        /** The eight bytes every PNG file starts with. */
        val PNG_SIGNATURE = intArrayOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    }
}
