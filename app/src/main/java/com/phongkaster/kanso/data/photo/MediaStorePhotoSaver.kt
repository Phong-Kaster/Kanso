package com.phongkaster.kanso.data.photo

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Saves a photo as a new, upright JPEG in the phone's `Pictures/Kanso` album, or an edited
 * picture as it is: a JPEG when it is opaque, a PNG when it has see-through pixels (a cut-out).
 *
 * MediaStore and scoped storage, in plain words:
 *
 * - MediaStore is the phone's shared database of photos, videos and music. Every picture the
 *   phone's gallery app shows is one row in it, with facts such as its name, its folder and its
 *   type. An app that adds a picture adds a row, then writes the picture's bytes "into" that row
 *   through a stream MediaStore hands back.
 * - Since Android 10 (API 29) storage is "scoped": an app may no longer write wherever it likes in
 *   shared storage. Instead it asks MediaStore for a new row, naming the folder it wants in
 *   `RELATIVE_PATH` (here `Pictures/Kanso/`). No permission is needed for that, because the app
 *   only ever adds its own files. While the row is marked `IS_PENDING = 1` other apps do not see
 *   it, so nobody opens a half-written photo; clearing the mark publishes it.
 * - On Android 8.1 and 9 (API 27 and 28) there is no `RELATIVE_PATH`. The app writes the file
 *   itself into the shared Pictures folder, which needs the WRITE_EXTERNAL_STORAGE permission, and
 *   then asks the media scanner to add the new file to MediaStore so the gallery shows it.
 *
 * Upright: cameras often store pixels as the sensor saw them and add an EXIF orientation tag (see
 * [readExifOrientation]). Some apps ignore that tag, so a photo with one is decoded at full size,
 * turned with a Matrix and written again as a JPEG with no tag. A photo that needs no turning is
 * copied byte for byte, losing nothing. If a photo is too large to turn in memory, it is copied as
 * it is, tag included, and the result says so ([SaveResult.Saved.keptOriginalOrientation]).
 *
 * The original photo is only ever opened for reading; it is never changed. Every save makes a
 * new file named `Kanso_<yyyyMMdd_HHmmss>.jpg` (`.png` for a picture with see-through pixels),
 * with `_1`, `_2` ... added when that name is taken, so an existing picture is never overwritten.
 *
 * [context] gives the ContentResolver; [decoder] decodes a turned photo at full size (tests pass
 * one that runs out of memory); [now] tells the time in milliseconds, used for the file name.
 */
class MediaStorePhotoSaver(
    context: Context,
    private val decoder: FullSizeDecoder = DefaultFullSizeDecoder,
    private val now: () -> Long = System::currentTimeMillis,
) : PhotoSaver {

    /** The application context: it lives as long as the app, so holding it leaks no screen. */
    private val appContext = context.applicationContext

    /** Opens photos and talks to MediaStore. */
    private val resolver
        get() = appContext.contentResolver

    /** True on Android 8.1 and 9 while WRITE_EXTERNAL_STORAGE is not granted. */
    override fun needsWritePermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED

    /**
     * Saves the photo at [sourceUri] on Dispatchers.IO, the pool of background threads meant for
     * files, so the screen keeps drawing. Any failure gives [SaveResult.Failed] and leaves no
     * half-written picture behind.
     */
    override suspend fun save(sourceUri: String): SaveResult = withContext(Dispatchers.IO) {
        try {
            val source = Uri.parse(sourceUri)
            val output = prepare(source) ?: return@withContext SaveResult.Failed
            try {
                val baseName = FILE_PREFIX + SimpleDateFormat(TIME_PATTERN, Locale.US).format(Date(now()))
                // An unedited photo is always a JPEG: its own bytes, or its pixels turned upright.
                val writePhoto: (OutputStream) -> Unit = { write(it, source, output) }
                val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    writeToMediaStore(baseName, JPEG_EXTENSION, JPEG_MIME_TYPE, writePhoto)
                } else {
                    writeToPicturesFolder(baseName, JPEG_EXTENSION, JPEG_MIME_TYPE, writePhoto)
                }
                SaveResult.Saved(saved, output.keptOriginalOrientation)
            } finally {
                output.release()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Missing photo, access taken away, full disk, MediaStore refusing the row: all mean
            // "not saved", which the screen says plainly.
            SaveResult.Failed
        } catch (_: OutOfMemoryError) {
            SaveResult.Failed
        }
    }

    /**
     * Saves [bitmap], the picture the person edited, exactly as it is: no scaling, no turning.
     *
     * The format follows the picture (see [saveFormatFor]): a picture with an alpha channel, such
     * as a cut-out whose background was removed, is written as a PNG, the only common format
     * that keeps see-through pixels; any other picture is written as a JPEG at quality 100
     * ([EDITED_JPEG_QUALITY]), which is far smaller and looks the same to the eye. The file name, its extension (`.jpg` or
     * `.png`) and the type MediaStore and the media scanner are told (`image/jpeg` or
     * `image/png`) all follow that choice, in the same `Pictures/Kanso` album and with the same
     * `Kanso_<yyyyMMdd_HHmmss>` naming as [save] for an unedited photo.
     *
     * The work runs on Dispatchers.IO so the screen keeps drawing. The bitmap is only read: it is
     * not recycled, because the screen is still showing it. The original photo is not touched at
     * all. Any failure gives [SaveResult.Failed], and the half-written row or file is deleted.
     */
    override suspend fun save(bitmap: Bitmap): SaveResult = withContext(Dispatchers.IO) {
        try {
            val format = saveFormatFor(bitmap)
            val extension = extensionFor(format)
            val mimeType = mimeTypeFor(format)
            val baseName = FILE_PREFIX + SimpleDateFormat(TIME_PATTERN, Locale.US).format(Date(now()))
            val writeBitmap: (OutputStream) -> Unit = { stream ->
                // compress encodes the pixels as they are; PNG is lossless and ignores the quality.
                if (!bitmap.compress(format, EDITED_JPEG_QUALITY, stream)) {
                    throw IOException("The $mimeType encoder failed")
                }
            }
            val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeToMediaStore(baseName, extension, mimeType, writeBitmap)
            } else {
                writeToPicturesFolder(baseName, extension, mimeType, writeBitmap)
            }
            SaveResult.Saved(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Full disk, access taken away, MediaStore refusing the row, the encoder failing.
            SaveResult.Failed
        } catch (_: OutOfMemoryError) {
            SaveResult.Failed
        }
    }

    /**
     * Works out what to write for the photo at [source]: its own bytes when it is already
     * upright (or too large to turn), else the turned pixels. Returns null when the photo carries
     * a turn but is not a picture that can be decoded.
     */
    private fun prepare(source: Uri): Output? {
        // Opening the photo also checks that it exists and is readable: if not, this throws.
        val tagStream = resolver.openInputStream(source) ?: return null
        val orientation = tagStream.use(::readExifOrientation)
        val matrix = uprightMatrix(orientation) ?: return Output.Copy(keptOriginalOrientation = false)

        var decoded: Bitmap? = null
        return try {
            val pixelStream = resolver.openInputStream(source) ?: return null
            decoded = pixelStream.use(decoder::decode) ?: return null
            Output.Upright(applyUpright(decoded, matrix))
        } catch (_: OutOfMemoryError) {
            // Too large to hold (twice) in memory: copy the bytes as they are, tag and all
            // (assumption A-001). The person is told some apps may show it sideways.
            decoded?.let { if (!it.isRecycled) it.recycle() }
            Output.Copy(keptOriginalOrientation = true)
        }
    }

    /**
     * Android 10 and later: adds a pending row to MediaStore in `Pictures/Kanso/`, named
     * [baseName] with [extension] and typed [mimeType], lets [writeBytes] write the picture into
     * it, then publishes it. Returns the new row's content Uri as text. If anything fails after
     * the row exists, the row is deleted again and the error is passed on.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeToMediaStore(
        baseName: String,
        extension: String,
        mimeType: String,
        writeBytes: (OutputStream) -> Unit,
    ): String {
        // VOLUME_EXTERNAL_PRIMARY is the phone's main shared storage, where the gallery looks.
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val name = uniqueName(baseName, namesTakenInMediaStore(baseName), extension)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            // Hidden from other apps until the bytes are all written.
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val row = resolver.insert(collection, values) ?: throw IOException("MediaStore made no row")
        try {
            val stream = resolver.openOutputStream(row) ?: throw IOException("No stream for $row")
            stream.use(writeBytes)
            val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            // Zero rows changed means the picture is still hidden: that is a failed save.
            if (resolver.update(row, publish, null, null) == 0) throw IOException("Not published")
        } catch (t: Throwable) {
            // Leave no empty or half-written picture in the gallery.
            runCatching { resolver.delete(row, null, null) }
            throw t
        }
        return row.toString()
    }

    /**
     * Returns the names starting with [baseName] that MediaStore already shows this app, so a
     * new name can avoid them. MediaStore would rename a clash itself, but choosing a free name
     * first keeps the `_1`, `_2` naming the same on every Android version. A failed query
     * counts as "none taken": MediaStore still never overwrites.
     */
    private fun namesTakenInMediaStore(baseName: String): Set<String> = try {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val column = MediaStore.MediaColumns.DISPLAY_NAME
        resolver.query(collection, arrayOf(column), "$column LIKE ?", arrayOf("$baseName%"), null)
            ?.use { cursor ->
                val index = cursor.getColumnIndexOrThrow(column)
                buildSet {
                    while (cursor.moveToNext()) cursor.getString(index)?.let(::add)
                }
            }
            .orEmpty()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptySet()
    }

    /**
     * Android 8.1 and 9: lets [writeBytes] write the picture as a new file in `<Pictures>/Kanso/`
     * named [baseName] with [extension], then asks the media scanner to add it to MediaStore as
     * [mimeType]. Returns the file's Uri as text (the scanner gives the content Uri only later,
     * in the background). A half-written file is deleted again.
     */
    private fun writeToPicturesFolder(
        baseName: String,
        extension: String,
        mimeType: String,
        writeBytes: (OutputStream) -> Unit,
    ): String {
        // Deprecated from Android 10, where scoped storage replaces it; here it is the only way.
        @Suppress("DEPRECATION")
        val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val folder = File(pictures, ALBUM_NAME)
        if (!folder.isDirectory && !folder.mkdirs()) throw IOException("Cannot make $folder")
        val file = createUniqueFile(folder, baseName, extension)
        try {
            FileOutputStream(file).use(writeBytes)
        } catch (t: Throwable) {
            file.delete()
            throw t
        }
        // The scanner reads the new file and adds its row to MediaStore, so the gallery app
        // shows it without waiting for the phone's next full scan.
        MediaScannerConnection.scanFile(
            appContext,
            arrayOf(file.absolutePath),
            arrayOf(mimeType),
            null,
        )
        return Uri.fromFile(file).toString()
    }

    /**
     * Creates and returns a new empty file in [folder] named after [baseName] with [extension],
     * adding `_1`, `_2` ... until the name is free. createNewFile makes the file only if no file
     * of that name exists, in one step, so two saves can never pick the same name.
     */
    private fun createUniqueFile(folder: File, baseName: String, extension: String): File {
        for (suffix in 0..MAX_SUFFIX) {
            val file = File(folder, nameWithSuffix(baseName, suffix, extension))
            if (file.createNewFile()) return file
        }
        throw IOException("No free name for $baseName in $folder")
    }

    /** Writes [output] to [stream]: the bytes of [source] as they are, or the turned pixels. */
    private fun write(stream: OutputStream, source: Uri, output: Output) {
        when (output) {
            is Output.Copy -> {
                val input = resolver.openInputStream(source) ?: throw IOException("Cannot read $source")
                // A plain byte copy: the saved file is exactly the original, EXIF included.
                input.use { it.copyTo(stream) }
            }
            is Output.Upright -> {
                // compress writes a fresh JPEG with no EXIF block, so no orientation tag at all.
                val written = output.bitmap.compress(Bitmap.CompressFormat.JPEG, UPRIGHT_JPEG_QUALITY, stream)
                if (!written) throw IOException("The JPEG encoder failed")
            }
        }
    }

    /** What a save writes: the original bytes, or a turned picture. */
    private sealed interface Output {

        /** True when the photo carries a turn that could not be applied. */
        val keptOriginalOrientation: Boolean

        /** Frees any memory held for the save. */
        fun release() = Unit

        /** Copy the original bytes; [keptOriginalOrientation] tells whether a turn was left undone. */
        class Copy(override val keptOriginalOrientation: Boolean) : Output

        /** Write [bitmap], already upright, as a new JPEG. */
        class Upright(val bitmap: Bitmap) : Output {

            /** An upright picture carries no turn. */
            override val keptOriginalOrientation: Boolean = false

            /** Frees the full-size pixels as soon as they are written. */
            override fun release() = bitmap.recycle()
        }
    }

    /** Names and limits shared by every save. */
    private companion object {
        /** Every saved file's name starts with this. */
        const val FILE_PREFIX = "Kanso_"

        /** The date and time in a file name, e.g. 20261006_143005. */
        const val TIME_PATTERN = "yyyyMMdd_HHmmss"

        /** The album, a folder inside Pictures. */
        const val ALBUM_NAME = "Kanso"

        /** Where MediaStore puts the file on Android 10 and later; it ends with a slash. */
        const val RELATIVE_PATH = "Pictures/Kanso/"

        /**
         * The JPEG quality of an edited picture, out of 100: the highest, so saving adds as little
         * loss as a JPEG can (the owner's decision, 2026-10-28, after a 2K photo came out visibly
         * worse); the file is two to three times larger than at 95, and still several times
         * smaller than a PNG of a photo. A PNG ignores it.
         */
        const val EDITED_JPEG_QUALITY = 100

        /** How many numbered names are tried before giving up. */
        const val MAX_SUFFIX = 9_999
    }
}

/** The type of a saved JPEG, told to MediaStore and the media scanner. */
internal const val JPEG_MIME_TYPE = "image/jpeg"

/** The type of a saved PNG (a picture with see-through pixels, such as a cut-out). */
internal const val PNG_MIME_TYPE = "image/png"

/** The file extension of a saved JPEG, without its dot. */
internal const val JPEG_EXTENSION = "jpg"

/** The file extension of a saved PNG, without its dot. */
internal const val PNG_EXTENSION = "png"

/**
 * Chooses how an edited picture is written: PNG when [hasAlpha] (the picture has an alpha
 * channel, so some pixels may be see-through, as in a cut-out), else JPEG.
 *
 * Bitmap.hasAlpha only says the picture carries an alpha channel, not that a pixel actually uses
 * it; checking every pixel would cost a full pass over a large picture, so a picture with the
 * channel is always kept as a PNG, which loses nothing either way.
 */
internal fun saveFormatFor(hasAlpha: Boolean): Bitmap.CompressFormat =
    if (hasAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

/** Chooses how [bitmap] is written; see the Boolean overload for the rule. */
internal fun saveFormatFor(bitmap: Bitmap): Bitmap.CompressFormat = saveFormatFor(bitmap.hasAlpha())

/** Returns the MIME type of a file written in [format]: `image/png` or `image/jpeg`. */
internal fun mimeTypeFor(format: Bitmap.CompressFormat): String =
    if (format == Bitmap.CompressFormat.PNG) PNG_MIME_TYPE else JPEG_MIME_TYPE

/** Returns the file extension, without its dot, of a file written in [format]: `png` or `jpg`. */
internal fun extensionFor(format: Bitmap.CompressFormat): String =
    if (format == Bitmap.CompressFormat.PNG) PNG_EXTENSION else JPEG_EXTENSION

/**
 * Returns `[baseName].[extension]` for [suffix] 0, else `[baseName]_[suffix].[extension]`.
 * [extension] is `jpg` unless a PNG is being saved.
 */
internal fun nameWithSuffix(baseName: String, suffix: Int, extension: String = JPEG_EXTENSION): String =
    if (suffix == 0) "$baseName.$extension" else "${baseName}_$suffix.$extension"

/**
 * Returns the first name built from [baseName] and [extension] (see [nameWithSuffix]) that is not
 * in [taken].
 */
internal fun uniqueName(baseName: String, taken: Set<String>, extension: String = JPEG_EXTENSION): String =
    generateSequence(0) { it + 1 }.map { nameWithSuffix(baseName, it, extension) }.first { it !in taken }
