package com.phongkaster.kanso.save

import android.graphics.BitmapFactory
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.security.MessageDigest

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
