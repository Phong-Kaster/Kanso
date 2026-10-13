package com.phongkaster.kanso.myedits

import android.Manifest
import android.app.Application
import android.content.ContentUris
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phongkaster.kanso.data.media.MediaStoreSavedPhotoSource
import com.phongkaster.kanso.data.media.Photo
import com.phongkaster.kanso.data.photo.MediaStorePhotoSaver
import com.phongkaster.kanso.data.photo.SaveResult
import com.phongkaster.kanso.tool.ToolFixtures
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Checks where My edits' photos come from (DoD 32): [MediaStoreSavedPhotoSource] lists exactly the
 * photos in Kanso's own `Pictures/Kanso` folder, newest first, as `content://` Uris, on Android
 * 8.1, 11 and 15, and never writes anything while reading.
 *
 * MediaStore is replaced by a [RecordingMediaProvider], an in-memory table that applies the
 * selection and the sort order it is given, so a wrong folder filter or order shows up as a wrong
 * answer. Each test puts five photos in `Pictures/Kanso/` and three elsewhere (the camera folder,
 * screenshots and a look-alike `Pictures/KansoOld/`), with the dates mixed so that "newest first"
 * is not the order they were added in.
 *
 * Each Android version is its own test (DoD 35), so every version's result is reported on its own.
 */
@RunWith(AndroidJUnit4::class)
class EditsSourceTest {

    /** A fresh folder for the photo the save test saves. */
    @get:Rule
    val temp = TemporaryFolder()

    /** The application under test. */
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    /** The recording MediaStore, installed before each test. */
    private lateinit var media: RecordingMediaProvider

    /** The ids of the five photos in `Pictures/Kanso`, newest first. */
    private lateinit var kansoIdsNewestFirst: List<Long>

    /** Installs the recording MediaStore and fills it with the eight photos. */
    @Before
    fun fillMedia() {
        media = RecordingMediaProvider.install()
        // (name, folder, time): the Kanso photos' times are mixed up with the others'.
        val rows = listOf(
            Triple("Kanso_20261001_100000.jpg", KANSO_FOLDER, BASE_SECONDS + 300L),
            Triple("IMG_0001.jpg", "DCIM/Camera/", BASE_SECONDS + 900L),
            Triple("Kanso_20261003_100000.jpg", KANSO_FOLDER, BASE_SECONDS + 700L),
            Triple("Screenshot_1.png", "Pictures/Screenshots/", BASE_SECONDS + 100L),
            Triple("Kanso_20261002_100000.jpg", KANSO_FOLDER, BASE_SECONDS + 500L),
            Triple("Kanso_20261005_100000.jpg", KANSO_FOLDER, BASE_SECONDS + 1_100L),
            Triple("Kanso_old.jpg", "Pictures/KansoOld/", BASE_SECONDS + 1_300L),
            Triple("Kanso_20261004_100000.jpg", KANSO_FOLDER, BASE_SECONDS + 200L),
        )
        val ids = rows.map { (name, folder, time) -> Triple(media.addPhoto(name, folder, time), folder, time) }
        kansoIdsNewestFirst = ids.filter { it.second == KANSO_FOLDER }.sortedByDescending { it.third }.map { it.first }
    }

    /** Stops Koin, which KansoApplication starts for every Robolectric test. */
    @After
    fun stopKoinAfterTest() {
        stopKoin()
    }

    /**
     * Android 8.1 with photo access granted: exactly the five Kanso photos, newest first, found by
     * the file path (`_data`), since the table has no `relative_path` column before Android 10.
     */
    @Test
    @Config(sdk = [27])
    fun android_8_1_lists_the_kanso_photos_newest_first_by_their_file_path() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)

        val photos = readSavedPhotos(sdkInt = 27)

        assertKansoPhotosNewestFirst(photos)
        val query = media.queries.single()
        assertTrue("The query should filter on the file path: ${query.selection}", query.selection.orEmpty().contains("_data LIKE"))
        assertFalse("relative_path does not exist on Android 8.1", query.selection.orEmpty().contains("relative_path"))
        assertEquals(listOf("%/Pictures/Kanso/%"), query.selectionArgs)
        assertNothingWritten()
    }

    /**
     * Android 8.1 without photo access: MediaStore refuses the read, and the source answers with
     * an empty list instead of an error (it never asks for the permission itself).
     */
    @Test
    @Config(sdk = [27])
    fun android_8_1_without_photo_access_gives_an_empty_list() {
        val photos = readSavedPhotos(sdkInt = 27)

        assertEquals(emptyList<Photo>(), photos)
        assertEquals("The source should have tried exactly once", 1, media.queries.size)
        assertNothingWritten()
    }

    /** Android 11: exactly the five Kanso photos, newest first, found by `relative_path`. */
    @Test
    @Config(sdk = [30])
    fun android_11_lists_the_kanso_photos_newest_first_by_their_folder() {
        val photos = readSavedPhotos(sdkInt = 30)

        assertKansoPhotosNewestFirst(photos)
        val query = media.queries.single()
        assertTrue("The query should filter on the folder: ${query.selection}", query.selection.orEmpty().contains("relative_path = ?"))
        assertEquals(listOf(KANSO_FOLDER), query.selectionArgs)
        assertEquals("date_modified DESC", query.sortOrder)
        assertNothingWritten()
    }

    /** Android 15: exactly the five Kanso photos, newest first, found by `relative_path`. */
    @Test
    @Config(sdk = [35])
    fun android_15_lists_the_kanso_photos_newest_first_by_their_folder() {
        val photos = readSavedPhotos(sdkInt = 35)

        assertKansoPhotosNewestFirst(photos)
        assertEquals(listOf(KANSO_FOLDER), media.queries.single().selectionArgs)
        assertNothingWritten()
    }

    /**
     * Android 15: a photo saved by Kanso's own saver lands in `Pictures/Kanso/` and is listed
     * first by the next read, ahead of the five older ones.
     */
    @Test
    @Config(sdk = [35])
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun android_15_lists_a_photo_just_saved_by_kanso_first() {
        val source = ToolFixtures.writeJpeg(temp.newFile("photo.jpg"), ToolFixtures.gradient(64, 48))
        val sourceUri = ToolFixtures.serve(app, source, "photo.jpg")

        val result = runBlocking { MediaStorePhotoSaver(app).save(sourceUri) }
        assertTrue("Not saved: $result", result is SaveResult.Saved)
        val savedId = ContentUris.parseId(Uri.parse((result as SaveResult.Saved).uri))
        val insertsBySaver = media.inserts.size
        val updatesBySaver = media.updates.size

        val photos = readSavedPhotos(sdkInt = 35)

        assertEquals(6, photos.size)
        assertEquals(savedId, photos.first().id)
        assertTrue("Bad name ${photos.first().name}", photos.first().name.startsWith("Kanso_"))
        assertEquals(kansoIdsNewestFirst, photos.drop(1).map { it.id })
        // The read itself wrote nothing: only the saver's own insert and update are recorded.
        assertEquals(insertsBySaver, media.inserts.size)
        assertEquals(updatesBySaver, media.updates.size)
        assertTrue(media.deletes.isEmpty())
    }

    // --- Helpers -----------------------------------------------------------------------------

    /** Reads the saved photos through the real source, told it runs on [sdkInt]. */
    private fun readSavedPhotos(sdkInt: Int): List<Photo> =
        runBlocking { MediaStoreSavedPhotoSource(app, sdkInt = sdkInt).savedPhotos() }

    /**
     * Fails unless [photos] are exactly the five Kanso photos, newest first, each with the
     * `content://` Uri of its MediaStore row and a time that never goes up down the list.
     */
    private fun assertKansoPhotosNewestFirst(photos: List<Photo>) {
        assertEquals(kansoIdsNewestFirst, photos.map { it.id })
        photos.forEach { photo ->
            assertTrue("Not a content Uri: ${photo.uri}", photo.uri.startsWith("content://media/"))
            assertEquals(photo.id, ContentUris.parseId(Uri.parse(photo.uri)))
            assertTrue("Not a Kanso photo: ${photo.name}", photo.name.startsWith("Kanso_2026"))
        }
        assertEquals(photos.sortedByDescending { it.modifiedSeconds }, photos)
    }

    /** Fails if anything was inserted, changed or deleted in MediaStore. */
    private fun assertNothingWritten() {
        assertTrue("Inserts: ${media.inserts}", media.inserts.isEmpty())
        assertTrue("Updates: ${media.updates}", media.updates.isEmpty())
        assertTrue("Deletes: ${media.deletes}", media.deletes.isEmpty())
    }

    /** Values the tests share. */
    private companion object {
        /** Kanso's own folder, as MediaStore writes it. */
        const val KANSO_FOLDER = "Pictures/Kanso/"

        /**
         * The earliest sample time, in seconds since 1970 (November 2023): well before the moment
         * the save test runs, so a photo saved now is always the newest.
         */
        const val BASE_SECONDS = 1_700_000_000L
    }
}
