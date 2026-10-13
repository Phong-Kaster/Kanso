package com.phongkaster.kanso.data.media

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lists the photos Kanso has saved, by asking Android's MediaStore for the rows of Kanso's own
 * `Pictures/Kanso` album, newest first.
 *
 * MediaStore is the phone's shared database of photos (`MediaStorePhotoSource` explains it). A
 * query names which rows it wants with a *selection*, a small SQL condition such as
 * `relative_path = ?` whose `?` is filled in from a separate list of arguments (so a folder name
 * can never be read as SQL), and the order it wants them in with a *sort order*, here
 * `date_modified DESC`: the most recently changed file first, the same field the gallery sorts by.
 *
 * How a saved photo is found depends on the Android version, because the saver stores it
 * differently (see `MediaStorePhotoSaver`):
 *
 * - Android 10 and later (API 29+): every row has a `RELATIVE_PATH`, the folder inside shared
 *   storage. Kanso's rows have exactly `Pictures/Kanso/`, so the selection asks for that. Reading
 *   the app's own rows needs no permission.
 * - Android 8.1 and 9 (API 27 and 28): there is no `RELATIVE_PATH` column. The row's `DATA` column
 *   holds the file's full path instead (for example `/storage/emulated/0/Pictures/Kanso/x.jpg`),
 *   so the selection asks for paths containing `/Pictures/Kanso/`. Reading MediaStore at all
 *   needs the photo-read permission there, and this class never asks for it: without it Android
 *   throws a SecurityException, which simply means "nothing readable".
 *
 * Any failure (no permission, no MediaStore, a broken row) gives an empty list, so My edits shows
 * its "no edits yet" message instead of crashing. The class only queries: it never writes.
 *
 * [context] gives the ContentResolver. [sdkInt] is the phone's Android version; tests pass one
 * to check each branch, the app keeps the default, the version of the phone it runs on.
 */
class MediaStoreSavedPhotoSource(
    context: Context,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) : SavedPhotoSource {

    /** The application context: it lives as long as the app, so holding it leaks no screen. */
    private val appContext = context.applicationContext

    /**
     * Returns Kanso's saved photos, newest first, read on `Dispatchers.IO` (the pool of
     * background threads meant for slow reads) so the screen keeps drawing meanwhile. Any error
     * gives an empty list; a cancelled read stays cancelled.
     */
    override suspend fun savedPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        try {
            query()
        } catch (cancelled: CancellationException) {
            // Cancellation must travel on, so the coroutine that asked can stop.
            throw cancelled
        } catch (_: Exception) {
            // A SecurityException (no read permission on Android 8.1 and 9) lands here too:
            // nothing is readable, so nothing is listed.
            emptyList()
        }
    }

    /**
     * Asks MediaStore for the rows of `Pictures/Kanso`, newest first, and turns each into a
     * [Photo] whose Uri is the row's `content://` address.
     *
     * The InlinedApi warning is silenced on purpose: `RELATIVE_PATH` is a text constant copied
     * into this code when it is compiled, and it is only used on Android 10 and later, where the
     * column exists.
     */
    @SuppressLint("InlinedApi")
    private fun query(): List<Photo> {
        // The collection of every image on the phone's shared storage.
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val (selection, arguments) = if (sdkInt >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ?" to arrayOf(KANSO_RELATIVE_PATH)
        } else {
            // DATA (the file path) is deprecated from Android 10, where RELATIVE_PATH replaces
            // it; on Android 8.1 and 9 it is the only way to tell which folder a photo is in.
            // In LIKE, `%` stands for any run of characters.
            @Suppress("DEPRECATION")
            val pathColumn = MediaStore.MediaColumns.DATA
            "$pathColumn LIKE ?" to arrayOf("%/$KANSO_RELATIVE_PATH%")
        }
        val projection = arrayOf(
            BaseColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            BUCKET_ID_COLUMN,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        val cursor = appContext.contentResolver
            .query(collection, projection, selection, arguments, sortOrder)
            ?: return emptyList()
        // `use` closes the cursor when the block ends, even after an error: an open cursor holds
        // database memory.
        return cursor.use {
            val idIndex = it.getColumnIndexOrThrow(BaseColumns._ID)
            val nameIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val bucketIndex = it.getColumnIndexOrThrow(BUCKET_ID_COLUMN)
            val modifiedIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            buildList {
                while (it.moveToNext()) {
                    val id = it.getLong(idIndex)
                    add(
                        Photo(
                            id = id,
                            // The row's own address, e.g. content://media/external/images/media/42.
                            uri = ContentUris.withAppendedId(collection, id).toString(),
                            name = it.getString(nameIndex).orEmpty(),
                            albumId = it.getLong(bucketIndex),
                            modifiedSeconds = it.getLong(modifiedIndex),
                        ),
                    )
                }
            }
        }
    }

    /** The folder Kanso saves into, as MediaStore writes it. */
    private companion object {
        /**
         * Kanso's folder inside shared storage, with the trailing slash MediaStore always adds.
         * It is the same text as the saver's own constant, which is private to the saver.
         */
        const val KANSO_RELATIVE_PATH = "Pictures/Kanso/"

        /**
         * MediaStore's album column ("bucket" is MediaStore's word for a folder). It is written
         * out because its constant moved between Android versions (ImageColumns before Android
         * 10, MediaColumns from then on); the column itself has always had this name.
         */
        const val BUCKET_ID_COLUMN = "bucket_id"
    }
}
