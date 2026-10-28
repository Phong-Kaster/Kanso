package com.phongkaster.kanso.data.photo

import android.graphics.Bitmap

/**
 * Saves a photo, or the picture the person edited, as a new file in the phone's `Pictures/Kanso`
 * album.
 *
 * It is an interface so the screen and its ViewModel can be tested with a fake that writes
 * nothing; the real one is `MediaStorePhotoSaver`. The original photo is never changed.
 */
interface PhotoSaver {

    /**
     * Tells whether Save must ask the person for the storage-write permission first. That is
     * true only on Android 8.1 and 9 (API 27 and 28), and only while it is not yet granted.
     */
    fun needsWritePermission(): Boolean

    /**
     * Saves the photo named by [sourceUri] (the text of a content or file Uri) and says how it
     * went. It does its work off the main thread and never throws for a bad photo or a full disk.
     */
    suspend fun save(sourceUri: String): SaveResult

    /**
     * Saves [bitmap], the edited picture, exactly as it is (same width and height, no scaling)
     * and says how it went: a PNG when the bitmap has an alpha channel (`bitmap.hasAlpha()`, as
     * a cut-out does), else a JPEG at quality 100. The file's extension and type follow that
     * format. It does its work off the main thread, never changes the original photo, never
     * recycles [bitmap], and never throws for a full disk; a failed save leaves no file behind.
     */
    suspend fun save(bitmap: Bitmap): SaveResult
}
