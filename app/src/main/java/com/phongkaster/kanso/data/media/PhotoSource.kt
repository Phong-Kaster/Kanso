package com.phongkaster.kanso.data.media

/**
 * One photo on the phone, as the gallery needs it.
 *
 * [id] is the photo's number in the phone's media database; it never changes for the same photo,
 * which is why the gallery grid uses it as the key of each cell. [uri] is the photo's content URI
 * as text (for example `content://media/external/images/media/42`): it is how any part of the
 * app, and any other app, opens the photo, so it is what travels to the tool screen. [name] is the
 * file name shown to screen readers, [albumId] the album (folder) it is in, and [modifiedSeconds]
 * when it was last changed, in seconds since 1970.
 */
data class Photo(
    val id: Long,
    val uri: String,
    val name: String,
    val albumId: Long,
    val modifiedSeconds: Long,
)

/** One album (a folder of photos, such as "Camera" or "Screenshots"), with its [id] and [name]. */
data class Album(val id: Long, val name: String)

/**
 * Where the gallery gets its photos from.
 *
 * The app reads the phone's photo library through [MediaStorePhotoSource]; tests give the gallery
 * a fake one instead, because a JVM test has no real photo library.
 */
interface PhotoSource {

    /** Returns every album that holds at least one photo. */
    suspend fun albums(): List<Album>

    /**
     * Returns the photos of the album [albumId], or of every album when [albumId] is null,
     * newest first.
     */
    suspend fun photos(albumId: Long?): List<Photo>
}
