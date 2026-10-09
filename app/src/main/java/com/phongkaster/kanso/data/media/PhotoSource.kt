package com.phongkaster.kanso.data.media

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
