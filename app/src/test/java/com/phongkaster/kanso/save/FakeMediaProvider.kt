package com.phongkaster.kanso.save

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver

/**
 * A stand-in for Android's MediaStore, the `media` content provider, backed by a temporary
 * folder. Robolectric has no MediaStore of its own, so this one records what the saver asks of
 * it: every inserted row (with its values as given), every update and every delete, and keeps
 * each row's bytes in a file of [root].
 *
 * Queries ignore their selection and return every row; the saver filters names itself.
 */
class FakeMediaProvider : ContentProvider() {

    /** One row of the fake MediaStore: its Uri, its current values and the file of its bytes. */
    class Row(val uri: Uri, val values: ContentValues, val file: File)

    /** The folder the rows' bytes are kept in; set by [install]. */
    lateinit var root: File

    /** The rows that exist now, by id. */
    val rows = LinkedHashMap<Long, Row>()

    /** The values of every insert, exactly as the saver gave them, oldest first. */
    val inserts = mutableListOf<ContentValues>()

    /** Every update, as (row Uri, values given), oldest first. */
    val updates = mutableListOf<Pair<Uri, ContentValues>>()

    /** The Uris of every deleted row, oldest first. */
    val deletes = mutableListOf<Uri>()

    /** The id the next inserted row gets. */
    private var nextId = 1L

    /** Starts the provider; there is nothing to set up. */
    override fun onCreate(): Boolean = true

    /** Adds a row, makes its empty file, and lets the app's resolver write into that file. */
    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val id = nextId++
        val rowUri = ContentUris.withAppendedId(uri, id)
        val file = File(root, "row_$id.bin").apply { createNewFile() }
        val given = ContentValues(values ?: ContentValues())
        inserts += ContentValues(given)
        rows[id] = Row(rowUri, given, file)
        // Robolectric's ContentResolver hands out output streams from its own table, not from
        // the provider, so the new row's file is registered there.
        shadowOf(RuntimeEnvironment.getApplication().contentResolver)
            .registerOutputStreamSupplier(rowUri) { FileOutputStream(file) }
        return rowUri
    }

    /** Returns every row, with the columns asked for (all values as text or numbers). */
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val columns = projection ?: arrayOf(DISPLAY_NAME_COLUMN)
        val cursor = MatrixCursor(columns)
        rows.values.forEach { row -> cursor.addRow(columns.map { row.values.get(it) }) }
        return cursor
    }

    /** Changes the values of the row [uri] names and records the change. */
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        val given = ContentValues(values ?: ContentValues())
        updates += uri to given
        val row = rows[ContentUris.parseId(uri)] ?: return 0
        row.values.putAll(given)
        return 1
    }

    /** Removes the row [uri] names, with its file, and records the delete. */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        deletes += uri
        val row = rows.remove(ContentUris.parseId(uri)) ?: return 0
        row.file.delete()
        return 1
    }

    /** Every row of this fake holds a JPEG. */
    override fun getType(uri: Uri): String = "image/jpeg"

    /** Opens the row's file, in case the resolver asks the provider directly for a stream. */
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val row = rows[ContentUris.parseId(uri)] ?: throw java.io.FileNotFoundException("$uri")
        return ParcelFileDescriptor.open(row.file, ParcelFileDescriptor.parseMode(mode))
    }

    /** Helpers to put the fake in place. */
    companion object {
        /** The DISPLAY_NAME column of MediaStore. */
        const val DISPLAY_NAME_COLUMN = "_display_name"

        /**
         * Builds the fake, keeps its bytes in [root], and registers it as the `media` provider
         * of the app's ContentResolver for the rest of the test.
         */
        fun install(root: File): FakeMediaProvider {
            val provider = Robolectric.buildContentProvider(FakeMediaProvider::class.java)
                .create("media")
                .get()
            provider.root = root
            ShadowContentResolver.registerProviderInternal("media", provider)
            return provider
        }
    }
}
