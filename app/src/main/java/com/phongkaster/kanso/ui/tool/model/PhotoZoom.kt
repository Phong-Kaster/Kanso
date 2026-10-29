package com.phongkaster.kanso.ui.tool.model

import com.phongkaster.kanso.data.edit.face.FaceFocus
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

/**
 * How far the photo card is zoomed: [scale] times larger, then slid by [shiftX] and [shiftY], each
 * a fraction of the photo's shown width and height (0.25 slides it a quarter of its width to the
 * right). [NONE] is the whole photo.
 *
 * The photo is zoomed around its own centre, inside its own frame, so a point at fraction `p` of
 * the photo lands at `0.5 + scale × (p − 0.5) + shift` of the frame.
 */
data class PhotoZoom(val scale: Float, val shiftX: Float, val shiftY: Float) {

    /** The zoom levels and the rule that picks one for a face. */
    companion object {

        /** No zoom: the whole photo, as it fits the card. */
        val NONE = PhotoZoom(scale = 1f, shiftX = 0f, shiftY = 0f)

        /**
         * The zoom that shows the face at [focus] large and centred, as Meitu does:
         * 1. the scale makes the face fill [FILL] of the frame in its tighter direction, between
         *    1 (no zoom, for a face already that large) and [MAX_SCALE];
         * 2. the shift brings the face's centre to the frame's centre: from the rule above,
         *    `0.5 + scale × (centre − 0.5) + shift = 0.5`, so `shift = scale × (0.5 − centre)`;
         * 3. the shift is then held to `(scale − 1) / 2` each way, the most the zoomed photo can
         *    slide before its edge comes into the frame, so a face near an edge never shows empty
         *    space beside the photo. Such a face ends up off centre, as close as it can get.
         */
        fun toward(focus: FaceFocus): PhotoZoom {
            if (focus.width <= 0f || focus.height <= 0f) return NONE
            val scale = (min(1f / focus.width, 1f / focus.height) * FILL).coerceIn(1f, MAX_SCALE)
            val limit = (scale - 1f) / 2f
            return PhotoZoom(
                scale = scale,
                shiftX = held(scale * (0.5f - focus.centerX), limit),
                shiftY = held(scale * (0.5f - focus.centerY), limit),
            )
        }

        /** [shift] held to at most [limit] either way. */
        private fun held(shift: Float, limit: Float): Float = if (abs(shift) <= limit) shift else sign(shift) * limit

        /** The share of the frame the zoomed face fills, in its tighter direction. */
        private const val FILL = 0.6f

        /** The closest zoom, so a tiny face in a large photo does not turn into a few big pixels. */
        private const val MAX_SCALE = 4f
    }
}
