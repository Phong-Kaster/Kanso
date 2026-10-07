package com.phongkaster.kanso.data.photo

/**
 * What came of one attempt to save a photo into the phone's own gallery.
 */
sealed interface SaveResult {

    /**
     * The photo was saved. [uri] is the text of the new picture's content Uri.
     *
     * [keptOriginalOrientation] is true when the photo was too large to turn upright in memory,
     * so its bytes were copied as they are and the picture still carries its rotation tag; the
     * screen tells the person, because some apps ignore that tag.
     */
    data class Saved(val uri: String, val keptOriginalOrientation: Boolean = false) : SaveResult

    /** The photo could not be saved, and nothing was written. */
    data object Failed : SaveResult
}

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
