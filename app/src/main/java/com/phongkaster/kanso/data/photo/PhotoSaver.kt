package com.phongkaster.kanso.data.photo

/**
 * Saves a photo as a new JPEG in the phone's `Pictures/Kanso` album.
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
}
