package com.phongkaster.kanso.ui.saved

import android.content.ClipData
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * The type of a photo Kanso saves as a JPEG, the usual case: every unedited save and every opaque
 * edit. It is also the type assumed when a photo's own type cannot be found out.
 */
const val SHARE_MIME_TYPE = "image/jpeg"

/** The type of a picture Kanso saves as a PNG: an edit with see-through pixels, such as a cut-out. */
const val SHARE_PNG_MIME_TYPE = "image/png"

/** How long [shareableUri] waits for the media scanner before giving up, in milliseconds. */
const val DEFAULT_SCAN_TIMEOUT_MILLIS = 5_000L

/**
 * Returns a Uri for the saved photo [photoUri] that another app may be given, or null when there
 * is none.
 *
 * Content Uri and file Uri, in plain words: a `file://` Uri is a raw path on the phone's
 * storage, such as `file:///storage/emulated/0/Pictures/Kanso/Kanso_1.jpg`. Handing one to
 * another app is refused by Android (since Android 7 it throws FileUriExposedException), because
 * the other app may not be allowed to read that path. A `content://` Uri instead names a row of a
 * content provider, here MediaStore, the phone's shared photo database; the provider opens the
 * file for whoever was granted access, so it can be shared safely.
 *
 * On Android 10 and later the save already gives a content Uri, which is returned as it is. On
 * Android 8.1 and 9 the save gives a file Uri, so MediaStore is asked for the row whose file path
 * (its DATA column) is that file, and the row's content Uri is returned. Right after a save the
 * media scanner may not have added the row yet; then the scanner is asked to scan the file, and
 * the content Uri it reports is used, waiting at most [scanTimeoutMillis]. Any other kind of Uri,
 * or a file MediaStore does not know, gives null: a file Uri is never returned.
 *
 * The work runs on Dispatchers.IO, the background threads meant for files and databases, so a
 * slow query never stops the screen from drawing.
 */
suspend fun shareableUri(
    context: Context,
    photoUri: String,
    scanTimeoutMillis: Long = DEFAULT_SCAN_TIMEOUT_MILLIS,
): Uri? = withContext(Dispatchers.IO) {
    val uri = Uri.parse(photoUri)
    when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> uri
        ContentResolver.SCHEME_FILE -> {
            val path = uri.path
            if (path.isNullOrEmpty()) {
                null
            } else {
                findInMediaStore(context, path) ?: scanForContentUri(context, path, scanTimeoutMillis)
            }
        }
        else -> null
    }
}

/**
 * Returns the content Uri of the MediaStore row whose file is at [path], or null when MediaStore
 * has no such row or cannot be asked.
 *
 * DATA is the column holding each row's file path. It is deprecated from Android 10, where rows
 * no longer need a path, but on Android 8.1 and 9 it is the only link from a file to its row.
 */
@Suppress("DEPRECATION")
private fun findInMediaStore(context: Context, path: String): Uri? = try {
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    context.contentResolver.query(
        collection,
        arrayOf(MediaStore.Images.Media._ID),
        "${MediaStore.Images.Media.DATA} = ?",
        arrayOf(path),
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            // withAppendedId adds the row's number to the collection: .../images/media/42.
            ContentUris.withAppendedId(collection, id)
        } else {
            null
        }
    }
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    // A refused or broken query means "not found"; the scanner is asked next.
    null
}

/**
 * Asks the media scanner to add the file at [path] to MediaStore, and returns the content Uri it
 * reports, or null if it reports none within [timeoutMillis].
 *
 * Scanning a file MediaStore already knows changes nothing, so asking twice is harmless.
 */
private suspend fun scanForContentUri(context: Context, path: String, timeoutMillis: Long): Uri? =
    withTimeoutOrNull(timeoutMillis) {
        suspendCancellableCoroutine<Uri?> { continuation ->
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(path),
                // The scanner is told the file's real type, so a saved PNG is listed as a PNG.
                arrayOf(shareMimeTypeFor(reportedType = null, fileName = path)),
            ) { _, scanned ->
                // Called once, on a scanner thread; a null Uri means the file could not be added.
                if (continuation.isActive) {
                    continuation.resume(scanned?.takeIf { it.scheme == ContentResolver.SCHEME_CONTENT })
                }
            }
        }
    }

/**
 * Returns the MIME type to share a photo under, given the type its content provider
 * [reportedType] (null when it reported none) and the photo's [fileName] or path (null when
 * unknown).
 *
 * A MIME type is the short label, such as `image/png`, that tells the receiving app how to read
 * the bytes; a PNG cut-out sent as `image/jpeg` may be refused or shown without its see-through
 * background. The provider's own answer wins when it is an image type. Otherwise the extension
 * decides: `.png` (any case) gives `image/png`, anything else `image/jpeg`, Kanso's usual format.
 */
internal fun shareMimeTypeFor(reportedType: String?, fileName: String?): String = when {
    reportedType != null && reportedType.startsWith("image/") -> reportedType
    fileName != null && fileName.endsWith(".png", ignoreCase = true) -> SHARE_PNG_MIME_TYPE
    else -> SHARE_MIME_TYPE
}

/**
 * Returns the MIME type of the saved photo at [contentUri], read from the photo itself: first
 * ContentResolver.getType, which asks MediaStore for the type stored with the photo's row; when
 * that gives nothing, the photo's file name (its DISPLAY_NAME column, else the Uri's last part)
 * decides by its extension (see [shareMimeTypeFor]).
 *
 * The work runs on Dispatchers.IO, because asking MediaStore is a call into another process.
 * A refused or failed question falls back to the extension, never throws.
 */
suspend fun shareMimeType(context: Context, contentUri: Uri): String = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val reported = try {
        resolver.getType(contentUri)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
    if (reported != null && reported.startsWith("image/")) return@withContext reported
    shareMimeTypeFor(reportedType = null, fileName = displayName(resolver, contentUri) ?: contentUri.lastPathSegment)
}

/**
 * Returns the file name MediaStore keeps for the row at [uri] (its DISPLAY_NAME column), or null
 * when there is none or the question fails.
 */
private fun displayName(resolver: ContentResolver, uri: Uri): String? = try {
    val column = MediaStore.MediaColumns.DISPLAY_NAME
    resolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(column)) else null
    }
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}

/**
 * Builds the intent that opens Android's share sheet for the photo at [contentUri], with the
 * sheet titled [title], sent as [mimeType].
 *
 * The share sheet (the "chooser") is the system panel listing the apps that can receive a photo:
 * messages, mail, social apps. The app does not pick one itself. It describes what it wants to
 * send, an ACTION_SEND intent of the photo's own type (`image/jpeg`, or `image/png` for a cut-out;
 * see [shareMimeType]) carrying the photo's Uri in EXTRA_STREAM, and wraps it with
 * Intent.createChooser so the person chooses. No package or component is set on either intent,
 * so no app is preferred or targeted. When [mimeType] is not given, the Uri's own extension
 * decides (see [shareMimeTypeFor]); a MediaStore content Uri has none, so the caller should pass
 * the type [shareMimeType] reads.
 *
 * The read grant: the receiving app has no permission to read Kanso's photos. The flag
 * FLAG_GRANT_READ_URI_PERMISSION gives it a one-off right to read exactly this Uri, and only
 * while it handles the share. Android passes that right along through the intent's ClipData (a
 * small list of the Uris the intent carries), so the Uri is put there too, and the flag is set on
 * both the inner intent and the chooser so the grant reaches whichever app is chosen.
 *
 * [contentUri] must be a content Uri (see [shareableUri]); a file Uri would be refused.
 */
fun shareChooserIntent(
    contentUri: Uri,
    title: CharSequence,
    mimeType: String = shareMimeTypeFor(reportedType = null, fileName = contentUri.lastPathSegment),
): Intent {
    val clip = ClipData.newRawUri(null, contentUri)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, contentUri)
        clipData = clip
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, title).apply {
        clipData = clip
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
