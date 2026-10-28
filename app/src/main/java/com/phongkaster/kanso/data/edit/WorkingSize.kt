package com.phongkaster.kanso.data.edit

import android.graphics.Bitmap
import com.phongkaster.kanso.navigation.Tool
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The largest picture each tool works on, measured as its longest edge in pixels.
 *
 * A phone photo can be 4000 x 3000 pixels or more, and the heavy tools need several copies of it
 * in memory at once; Enhance even makes its output four times wider and taller. So before a tool
 * runs, the photo is scaled down to a smaller working copy (keeping its shape) instead of letting
 * the app run out of memory and crash on a very large photo.
 */
object WorkingSize {

    /** Longest edge, in pixels, that Remove background works on. */
    const val REMOVE_BACKGROUND = 2000

    /**
     * Longest edge, in pixels, that Enhance works on. It is small because Enhance upscales four
     * times: a 512-pixel input already becomes a 2048-pixel result.
     */
    const val ENHANCE = 512

    /** Longest edge, in pixels, that Retouch works on. */
    const val RETOUCH = 2000

    /** Longest edge, in pixels, that the Artistic styles work on. */
    const val ARTISTIC = 1024

    /**
     * Longest edge, in pixels, that the general Edit tool works on, and so the largest an edited
     * photo is saved. 3000 keeps a 2K or 2560 px photo at its own size (the owner found a 2K photo
     * saved at 2000 px looking worse, 2026-10-28), while a Done render of a 3000 x 2250 photo
     * (three copies of about 27 MB) stays well inside the 192 MB the app gets on the owner's
     * Galaxy A13. A bigger camera photo is shrunk to it.
     */
    const val EDIT = 3000

    /** Returns the longest edge, in pixels, that [tool] works on. */
    fun maxEdgeFor(tool: Tool): Int = when (tool) {
        Tool.EDIT -> EDIT
        Tool.REMOVE_BACKGROUND -> REMOVE_BACKGROUND
        Tool.ENHANCE -> ENHANCE
        Tool.RETOUCH -> RETOUCH
        Tool.ARTISTIC -> ARTISTIC
    }

    /**
     * Returns [bitmap] scaled down so its longest edge is at most [maxEdgePx], keeping its aspect
     * ratio. When it already fits, the same bitmap comes back and no copy is made, so a small
     * photo costs no extra memory. The scaled copy is filtered (smoothed) so it does not look
     * jagged. The original [bitmap] is never changed.
     */
    fun fit(bitmap: Bitmap, maxEdgePx: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdgePx) return bitmap
        val scale = maxEdgePx.toFloat() / longest
        // At least one pixel on each side, so a very long thin picture still gives a valid bitmap.
        val width = (bitmap.width * scale).roundToInt().coerceIn(1, maxEdgePx)
        val height = (bitmap.height * scale).roundToInt().coerceIn(1, maxEdgePx)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}
