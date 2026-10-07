package com.phongkaster.kanso.save

import android.graphics.BitmapFactory
import android.os.Environment
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.exifinterface.media.ExifInterface
import com.phongkaster.kanso.data.photo.PhotoSaver
import com.phongkaster.kanso.data.photo.SaveResult
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred

/** The name every saved photo must have: `Kanso_<yyyyMMdd>_<HHmmss>[_n].jpg`. */
val SAVED_NAME = Regex("""Kanso_\d{8}_\d{6}(_\d+)?\.jpg""")

/** A fixed moment (2026-10-06, around midday), so two saves fall in the same second. */
const val FIXED_TIME_MILLIS = 1_791_288_000_000L

/** Returns the SHA-256 of [file]'s bytes, as hex text. */
fun sha256(file: File): String =
    MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

/** Returns [file]'s pixel size as (width, height), read from its header only. */
fun jpegSize(file: File): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, options)
    return options.outWidth to options.outHeight
}

/** Returns [file]'s EXIF orientation tag, or 0 ("undefined") when it has none. */
fun exifOrientation(file: File): Int =
    ExifInterface(file.absolutePath).getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_UNDEFINED,
    )

/** Tells whether [bytes] start like a JPEG (the FF D8 FF start-of-image marker). */
fun looksLikeJpeg(bytes: ByteArray): Boolean =
    bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()

/** The `<Pictures>/Kanso` folder Android 8.1 and 9 save into (Robolectric's stand-in folder). */
@Suppress("DEPRECATION")
fun legacyKansoFolder(): File =
    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Kanso")

/** The files now in `<Pictures>/Kanso`, by name. */
fun legacyKansoFiles(): List<File> =
    legacyKansoFolder().listFiles()?.filter { it.isFile }?.sortedBy { it.name }.orEmpty()

/**
 * A [PhotoSaver] that writes nothing: it records each photo it is asked to save and answers
 * [result]. With [gate] set, each save waits until the test completes it, so the screen can be
 * seen mid-save.
 */
class FakePhotoSaver(
    var needsPermission: Boolean = false,
    var result: SaveResult = SaveResult.Saved("content://media/external_primary/images/media/1"),
    val gate: CompletableDeferred<Unit>? = null,
) : PhotoSaver {

    /** The source Uri of every save asked for, oldest first. */
    val calls = CopyOnWriteArrayList<String>()

    /** Answers [needsPermission]. */
    override fun needsWritePermission(): Boolean = needsPermission

    /** Records the call, waits for [gate] if any, and answers [result]. */
    override suspend fun save(sourceUri: String): SaveResult {
        calls += sourceUri
        gate?.await()
        return result
    }
}

/**
 * An ActivityResultRegistry that shows no dialog: it records the permission of each single
 * permission request (`RequestPermission`, whose input is one String) and lets the test answer
 * the latest one with [answerLatest].
 */
class SinglePermissionRegistry : ActivityResultRegistry() {

    /** The permission of every request so far, oldest first. */
    val requests = mutableListOf<String>()

    /** The request code of the latest request, used to deliver its answer. */
    private var latestRequestCode = -1

    /** Records the request instead of showing the system permission dialog. */
    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        requests += input.toString()
        latestRequestCode = requestCode
    }

    /** Delivers [granted] as the answer to the latest request. */
    fun answerLatest(granted: Boolean) {
        check(latestRequestCode != -1) { "No permission request to answer" }
        dispatchResult(latestRequestCode, granted)
    }
}
