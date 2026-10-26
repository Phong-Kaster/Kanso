package com.phongkaster.kanso.data.edit.edit

import android.graphics.Bitmap

/**
 * Finds the picture of a sticker, so [EditProcessor] can draw it into the saved photo.
 *
 * The processor knows only a sticker's `pictureId`; this is the one place that turns the id into
 * pixels. The app passes a source that decodes the real sticker files, a test passes one that
 * makes a small bitmap, and the default draws nothing. It is a `fun interface` (an interface with
 * one function), so a lambda is enough to make one, the same shape as [FilterLut.Source].
 */
fun interface StickerPictures {

    /**
     * Returns the picture [pictureId] decoded at about [widthPx] pixels wide (a little more or
     * less is fine, the processor scales to the exact size), or null when there is no such
     * picture, in which case that sticker draws nothing. [widthPx] is never more than the photo's
     * longest edge, however big the sticker is.
     *
     * **Ownership:** the implementation owns every bitmap it returns. It may cache it, hand the
     * same one out again, and recycle it whenever it likes; the processor only reads it while it
     * draws that one sticker and never recycles it. So an implementation must not recycle a
     * bitmap while a render may still be using it, and must not return a bitmap the caller
     * would have to free.
     */
    fun picture(pictureId: String, widthPx: Int): Bitmap?
}
