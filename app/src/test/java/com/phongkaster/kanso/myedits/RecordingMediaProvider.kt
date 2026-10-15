package com.phongkaster.kanso.myedits

import android.Manifest
import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.util.concurrent.CopyOnWriteArrayList
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver

/**
 * A stand-in for Android's MediaStore (the `media` content provider) that really applies the
 * selection and the sort order it is given, and records every query, insert, update and delete.
 *
 * Robolectric has no MediaStore of its own. The older [com.phongkaster.kanso.save.FakeMediaProvider]
 * ignores a query's selection, which is fine for the saver but would prove nothing about a reader
 * that depends on its selection. This one keeps its rows in an in-memory SQLite table (the same
 * database engine the real MediaStore uses) and hands each query's selection, arguments and sort
 * order to SQLite as they are, so a wrong folder filter or a wrong order gives a wrong answer.
 *
 * It copies two facts of the real provider that matter to Kanso:
 *
 * - Before Android 10 the table has no `relative_path` column (only the file path in `_data`),
 *   so a query that names `relative_path` there fails, as it would on the phone.
 * - Before Android 10 reading needs the photo-read permission: without it a query throws a
 *   SecurityException. Robolectric starts with every permission refused; a test grants it with
 *   `shadowOf(application).grantPermissions(...)`.
 *
 * Inserts are accepted (so [com.phongkaster.kanso.data.photo.MediaStorePhotoSaver] can save into
 * it): a new row gets `date_modified` = now when the saver gives none, as MediaStore does, and its
 * bytes go to an in-memory stream. Every Uri of the `media` authority reaches the same one table.
 */
class RecordingMediaProvider : ContentProvider() {

    /** One query as the provider received it. */
    class Query(
        val uri: Uri,
        val projection: List<String>?,
        val selection: String?,
        val selectionArgs: List<String>?,
        val sortOrder: String?,
    )

    /** Every query, oldest first. */
    val queries = CopyOnWriteArrayList<Query>()

    /** The values of every insert, exactly as given, oldest first. */
    val inserts = CopyOnWriteArrayList<ContentValues>()

    /** Every update, as (row Uri, values given), oldest first. */
    val updates = CopyOnWriteArrayList<Pair<Uri, ContentValues>>()

    /** The Uris of every delete, oldest first. */
    val deletes = CopyOnWriteArrayList<Uri>()

    /** The in-memory table of images. */
    private lateinit var database: SQLiteDatabase

    /** True when the table has MediaStore's Android 10 column `relative_path`. */
    private val hasRelativePath: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /** Makes the empty in-memory table, with the columns of the running Android version. */
    override fun onCreate(): Boolean {
        database = SQLiteDatabase.create(null)
        val relativePath = if (hasRelativePath) ", $RELATIVE_PATH TEXT" else ""
        database.execSQL(
            "CREATE TABLE $TABLE (" +
                "$ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$DISPLAY_NAME TEXT, $MIME_TYPE TEXT, $DATA TEXT, $BUCKET_ID INTEGER, " +
                "$DATE_MODIFIED INTEGER, $IS_PENDING INTEGER DEFAULT 0$relativePath)",
        )
        return true
    }

    /**
     * Puts a photo named [name] in [folder] (for example `Pictures/Kanso/`), changed at
     * [modifiedSeconds], straight into the table, as if another app or an earlier save had made
     * it. It is not recorded as an insert. Returns the new row's id.
     */
    fun addPhoto(name: String, folder: String, modifiedSeconds: Long): Long {
        val values = ContentValues().apply {
            put(DISPLAY_NAME, name)
            put(MIME_TYPE, "image/jpeg")
            put(DATA, "$STORAGE_ROOT/$folder$name")
            put(BUCKET_ID, folder.hashCode().toLong())
            put(DATE_MODIFIED, modifiedSeconds)
            if (hasRelativePath) put(RELATIVE_PATH, folder)
        }
        return database.insertOrThrow(TABLE, null, values)
    }

    /**
     * Records the query, refuses it before Android 10 without the photo-read permission, and
     * otherwise runs it on the table with the given selection, arguments and sort order.
     */
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        queries += Query(uri, projection?.toList(), selection, selectionArgs?.toList(), sortOrder)
        val app = RuntimeEnvironment.getApplication()
        if (!hasRelativePath &&
            app.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Reading media needs READ_EXTERNAL_STORAGE before Android 10")
        }
        return database.query(
            TABLE,
            projection?.map { it }?.toTypedArray(),
            selection,
            selectionArgs?.map { it }?.toTypedArray(),
            null,
            null,
            sortOrder,
        )
    }

    /** Records the insert, adds the row and lets the app's resolver write the row's bytes. */
    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val given = ContentValues(values ?: ContentValues())
        inserts += ContentValues(given)
        val row = ContentValues(given)
        // MediaStore stamps a new row with the time it was made.
        if (!row.containsKey(DATE_MODIFIED)) row.put(DATE_MODIFIED, System.currentTimeMillis() / 1_000L)
        val id = database.insertOrThrow(TABLE, null, row)
        val rowUri = ContentUris.withAppendedId(uri, id)
        // Robolectric's ContentResolver hands out output streams from its own table, not from the
        // provider, so the new row's stream is registered there.
        shadowOf(RuntimeEnvironment.getApplication().contentResolver)
            .registerOutputStreamSupplier(rowUri) { ByteArrayOutputStream() }
        return rowUri
    }

    /** Records the update and changes the values of the row [uri] names. */
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        val given = ContentValues(values ?: ContentValues())
        updates += uri to given
        return database.update(TABLE, given, "$ID = ?", arrayOf(ContentUris.parseId(uri).toString()))
    }

    /** Records the delete and removes the row [uri] names. */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        deletes += uri
        return database.delete(TABLE, "$ID = ?", arrayOf(ContentUris.parseId(uri).toString()))
    }

    /** Every row of this provider holds a JPEG. */
    override fun getType(uri: Uri): String = "image/jpeg"

    /** The table's name and columns (MediaStore's own column names) and the helper to install it. */
    companion object {
        /** The one table every Uri reaches. */
        private const val TABLE = "images"

        /** The row id column. */
        const val ID = "_id"

        /** The file name column. */
        const val DISPLAY_NAME = "_display_name"

        /** The file type column. */
        const val MIME_TYPE = "mime_type"

        /** The full file path column (the only folder information before Android 10). */
        const val DATA = "_data"

        /** The album (bucket) column. */
        const val BUCKET_ID = "bucket_id"

        /** The last-changed time column, in seconds since 1970. */
        const val DATE_MODIFIED = "date_modified"

        /** The "still being written" mark, Android 10 and later. */
        const val IS_PENDING = "is_pending"

        /** The folder column, Android 10 and later. */
        const val RELATIVE_PATH = "relative_path"

        /** Where shared storage starts on a phone, the start of every `_data` path. */
        const val STORAGE_ROOT = "/storage/emulated/0"

        /**
         * Builds the provider and registers it as the `media` provider of the app's
         * ContentResolver for the rest of the test.
         */
        fun install(): RecordingMediaProvider {
            val provider = Robolectric.buildContentProvider(RecordingMediaProvider::class.java)
                .create("media")
                .get()
            ShadowContentResolver.registerProviderInternal("media", provider)
            return provider
        }
    }
}
