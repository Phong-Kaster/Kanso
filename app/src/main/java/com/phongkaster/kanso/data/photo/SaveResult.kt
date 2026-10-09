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
