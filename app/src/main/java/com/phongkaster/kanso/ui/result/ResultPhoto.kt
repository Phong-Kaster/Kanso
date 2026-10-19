package com.phongkaster.kanso.ui.result

import android.graphics.Bitmap

/**
 * What the Result screen's photo area shows: nothing yet, the picture, nothing because the photo
 * cannot be read, or nothing because no picture was handed over.
 */
sealed interface ResultPhoto {

    /** The photo is still being decoded (only the My edits viewer decodes one). */
    data object Loading : ResultPhoto

    /** The picture is ready, upright, as [bitmap]. */
    data class Ready(val bitmap: Bitmap) : ResultPhoto

    /** The viewer's photo could not be opened or decoded. */
    data object Unreadable : ResultPhoto

    /**
     * No rendered picture was waiting in the hand-off store, for example after Android killed the
     * app while it was in the background: the screen says "Nothing to show".
     */
    data object Empty : ResultPhoto
}
