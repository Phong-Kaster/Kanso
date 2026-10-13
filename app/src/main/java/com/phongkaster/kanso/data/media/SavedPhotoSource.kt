package com.phongkaster.kanso.data.media

/**
 * Where My edits (and Home's Recent edits) get the photos Kanso itself has saved.
 *
 * Every photo Kanso saves goes into the phone's `Pictures/Kanso` album (see `MediaStorePhotoSaver`).
 * This source lists exactly those, so a person sees their own edits and nothing else from the
 * phone. The app reads them through [MediaStoreSavedPhotoSource]; tests pass a fake instead,
 * because a JVM test has no real photo library.
 *
 * The promise every implementation keeps:
 *
 * - The photos come newest first, by the time each file was last changed.
 * - It never asks for a permission. A photo it is not allowed to read is simply not listed, and
 *   a library it cannot read at all gives an empty list, never an error: the screen then shows
 *   its "no edits yet" message.
 * - It only reads; it never adds, changes or removes a photo.
 */
interface SavedPhotoSource {

    /** Returns every readable photo in Kanso's own `Pictures/Kanso` album, newest first. */
    suspend fun savedPhotos(): List<Photo>
}
