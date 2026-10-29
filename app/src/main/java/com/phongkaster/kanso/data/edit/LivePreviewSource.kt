package com.phongkaster.kanso.data.edit

import android.graphics.Bitmap

/**
 * A tool processor that can show its edit live while a slider is being dragged, as Meitu does,
 * before the full-quality render that starts when the finger lifts.
 *
 * A live look must be quick (a phone draws a new frame many times a second while a finger moves),
 * so it is not a finished picture but a [LivePreview] recipe the screen draws on the graphics chip
 * over the photo as shown, and the processor keeps whatever it can between frames (found faces, a
 * smoothed copy). It uses the settings the processor holds at that moment, the same ones the full
 * render would use.
 *
 * The tool screen's ViewModel checks for this interface (`processor as? LivePreviewSource`), so any
 * slider tool can offer live looks by implementing it.
 */
interface LivePreviewSource {

    /**
     * Returns how to draw [photo] (the unedited photo as shown on screen) with the processor's
     * current settings, or null when there is nothing to show (no face found, or the look could
     * not be made). The first call for a photo may take a moment (finding the faces, smoothing
     * once); later ones are nearly instant. [photo] is not changed. A `CancellationException` is
     * passed on; nothing else is thrown.
     */
    suspend fun livePreview(photo: Bitmap): LivePreview?

    /** Forgets everything kept between frames, so a photo that left the screen is not held on to. */
    fun releasePreview()
}
