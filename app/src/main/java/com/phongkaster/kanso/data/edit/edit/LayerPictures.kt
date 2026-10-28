package com.phongkaster.kanso.data.edit.edit

import android.graphics.Bitmap

/**
 * Finds the picture of a layer, so [EditProcessor] (and the cropper, and the live preview) can
 * draw it.
 *
 * Whoever draws a layer knows only its [LayerContent]; this is the one place that turns a content
 * into pixels. The app passes [AppLayerPictures], a test passes one that makes a small bitmap,
 * and the default draws nothing. It is a `fun interface` (an interface with one function), so a
 * lambda is enough to make one, the same shape as [FilterLut.Source].
 */
fun interface LayerPictures {

    /**
     * Returns the picture of [content] at about [widthPx] pixels wide (a little more or less is
     * fine, the caller scales to the exact size), or null when there is no such picture, in which
     * case that layer draws nothing. [widthPx] is never more than the photo's longest edge,
     * however big the layer is.
     *
     * **Ownership:** the implementation owns every bitmap it returns. It may cache it, hand the
     * same one out again, and recycle it whenever it likes; the caller only reads it while it
     * draws that one layer and never recycles it. So an implementation must not recycle a
     * bitmap while a render may still be using it, and must not return a bitmap the caller
     * would have to free.
     */
    fun picture(content: LayerContent, widthPx: Int): Bitmap?
}
