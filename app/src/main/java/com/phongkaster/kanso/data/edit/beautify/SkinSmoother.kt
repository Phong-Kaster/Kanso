package com.phongkaster.kanso.data.edit.beautify

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Smooths the skin of one face, for the Beautify tool.
 *
 * It is an interface, a seam: the app's real smoother ([EngineSkinSmoother]) uses OpenCV, which is
 * native code and cannot load in a JVM test, so tests hand [BeautifyProcessor] a fake smoother.
 * The smoother always smooths fully; how much of it the person sees (the strength slider) is
 * decided afterwards by [BeautifyProcessor], which blends the smoothed copy over the original.
 */
interface SkinSmoother {

    /**
     * Returns a new bitmap the size of [photo] in which only the pixels inside [face] (in pixels
     * of [photo]) are smoothed; every pixel outside [face] is exactly as in [photo]. [photo]
     * itself is never changed.
     */
    fun smooth(photo: Bitmap, face: Rect): Bitmap
}
