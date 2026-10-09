package com.phongkaster.kanso.ui.saved

import android.graphics.Bitmap

/** What the photo area shows: nothing yet, the decoded photo, or nothing because it cannot be read. */
sealed interface SavedPhoto {

    /** The photo is still being decoded. */
    data object Loading : SavedPhoto

    /** The photo is decoded, upright, as [bitmap]. */
    data class Ready(val bitmap: Bitmap) : SavedPhoto

    /** The photo could not be opened or decoded. */
    data object Unreadable : SavedPhoto
}
