package com.phongkaster.kanso.result

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowContentResolver

/**
 * A stand-in for MediaStore's image table on Android 8.1 and 9, where each row still records its
 * file's path in the DATA column. Robolectric has no MediaStore, so this one knows only the
 * [files] a test gives it (path to row id) and answers the one question the share code asks:
 * "which row has DATA = this path?".
 *
 * Run 9: moved from the `saved` test package to `result`, because the Result screen replaced the
 * Saved screen; the viewer still shares a saved photo through it.
 */
class FakeImagesProvider : ContentProvider() {

    /** The rows MediaStore knows: file path to row id. */
    val files = LinkedHashMap<String, Long>()

    /** Every query, as (Uri, selection, first selection argument), oldest first. */
    val queries = mutableListOf<Triple<Uri, String?, String?>>()

    /** Starts the provider; there is nothing to set up. */
    override fun onCreate(): Boolean = true

    /**
     * Returns the `_id` of the row whose path is the first selection argument, when the
     * selection asks for DATA; otherwise no rows.
     */
    @Suppress("DEPRECATION")
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val path = selectionArgs?.firstOrNull()
        queries += Triple(uri, selection, path)
        val cursor = MatrixCursor(arrayOf(MediaStore.Images.Media._ID))
        if (selection == "${MediaStore.Images.Media.DATA} = ?") {
            path?.let { files[it] }?.let { id -> cursor.addRow(arrayOf<Any>(id)) }
        }
        return cursor
    }

    /** Not used by the share code. */
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    /** Not used by the share code. */
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    /** Not used by the share code. */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    /** Every row holds a JPEG. */
    override fun getType(uri: Uri): String = "image/jpeg"

    /** Helpers to put the fake in place. */
    companion object {
        /** Builds the fake and registers it as the `media` provider for the rest of the test. */
        fun install(): FakeImagesProvider {
            val provider = Robolectric.buildContentProvider(FakeImagesProvider::class.java)
                .create("media")
                .get()
            ShadowContentResolver.registerProviderInternal("media", provider)
            return provider
        }
    }
}
