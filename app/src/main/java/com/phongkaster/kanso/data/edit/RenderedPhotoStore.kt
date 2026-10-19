package com.phongkaster.kanso.data.edit

import android.graphics.Bitmap

/**
 * The hand-off between a tool screen and the Result screen: holds the one picture Done rendered.
 *
 * A navigation key is saved as text and cannot carry a bitmap, so Done puts the picture here and
 * navigates to Result, which reads it back. The store is a Koin `single`, so it lives as long as the
 * process: it survives rotation, but not the process being killed, after which [peek] is null and
 * Result shows "Nothing to show". Only one picture is held; a newer [put] replaces the older one.
 */
class RenderedPhotoStore {

    /** The held picture, or null when nothing was handed over. */
    @Volatile
    private var bitmap: Bitmap? = null

    /** The photo's own file, when the picture is that photo untouched; null for an edited picture. */
    @Volatile
    private var original: String? = null

    /**
     * Holds [rendered], replacing any earlier picture. [originalUri] is the text of the photo's own
     * Uri when nothing was edited: Save then copies that file exactly, as Save always did for an
     * untouched photo, instead of re-encoding the screen-sized picture.
     */
    fun put(rendered: Bitmap, originalUri: String? = null) {
        bitmap = rendered
        original = originalUri
    }

    /** The untouched photo's Uri, or null when the held picture was edited (or nothing is held). */
    fun originalUri(): String? = original

    /** Returns the held picture without removing it, or null when there is none. */
    fun peek(): Bitmap? = bitmap

    /** Forgets the held picture. */
    fun clear() {
        bitmap = null
        original = null
    }
}
