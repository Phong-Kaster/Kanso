package com.phongkaster.kanso.tool.edit

import android.graphics.Bitmap
import com.phongkaster.kanso.data.edit.edit.StickerPictures

/**
 * A fake [StickerPictures] for the processor tests: it makes its pictures out of nothing, so no
 * sticker file is needed.
 *
 * Every id gives a square picture [widthPx] wide and tall in [colour], except:
 * - [CHECKER], a one-pixel checkerboard of [CHECKER_A] and [CHECKER_B] (top-left is [CHECKER_A]);
 * - [QUAD], four opaque quadrants: [QUAD_TOP_LEFT], [QUAD_TOP_RIGHT], [QUAD_BOTTOM_LEFT] and
 *   [QUAD_BOTTOM_RIGHT], so a turn or a mirror can be told apart by where each colour lands;
 * - [HALF], a solid [HALF_COLOUR] that is half see-through (alpha 128).
 *
 * The picture is exactly as wide as the processor asked, so a sticker is drawn at scale 1. Every
 * request is written to [requestedWidths], so a test can see how wide a picture was asked for.
 */
class SolidStickerPictures(private val colour: Int = SOLID) : StickerPictures {

    /** The widths asked for so far, in order. */
    val requestedWidths = mutableListOf<Int>()

    /** Builds the picture for [pictureId] at [widthPx] x [widthPx] pixels. */
    override fun picture(pictureId: String, widthPx: Int): Bitmap? {
        requestedWidths += widthPx
        val data = IntArray(widthPx * widthPx) { index ->
            val x = index % widthPx
            val y = index / widthPx
            when (pictureId) {
                CHECKER -> if ((x + y) % 2 == 0) CHECKER_A else CHECKER_B
                QUAD -> when {
                    y < widthPx / 2 -> if (x < widthPx / 2) QUAD_TOP_LEFT else QUAD_TOP_RIGHT
                    else -> if (x < widthPx / 2) QUAD_BOTTOM_LEFT else QUAD_BOTTOM_RIGHT
                }
                HALF -> HALF_COLOUR
                else -> colour
            }
        }
        val bitmap = Bitmap.createBitmap(widthPx, widthPx, Bitmap.Config.ARGB_8888)
        // setPixels takes plain (non-premultiplied) ARGB and the bitmap converts it itself.
        bitmap.setPixels(data, 0, widthPx, 0, 0, widthPx, widthPx)
        return bitmap
    }

    /** The ids and colours the tests agree on. */
    companion object {
        /** The id that asks for the checkerboard. */
        const val CHECKER = "checker"

        /** The id that asks for the four-quadrant picture. */
        const val QUAD = "quad"

        /** The id that asks for the half see-through picture. */
        const val HALF = "half"

        /** The default solid colour: a strong red, opaque. */
        const val SOLID = 0xFFC83232.toInt()

        /** The checkerboard's first colour (a red), opaque. */
        const val CHECKER_A = 0xFFE61414.toInt()

        /** The checkerboard's second colour (a blue), opaque. */
        const val CHECKER_B = 0xFF1414E6.toInt()

        /** The top-left quadrant of [QUAD]: red. */
        const val QUAD_TOP_LEFT = 0xFFFF0000.toInt()

        /** The top-right quadrant of [QUAD]: green. */
        const val QUAD_TOP_RIGHT = 0xFF00FF00.toInt()

        /** The bottom-left quadrant of [QUAD]: blue. */
        const val QUAD_BOTTOM_LEFT = 0xFF0000FF.toInt()

        /** The bottom-right quadrant of [QUAD]: yellow. */
        const val QUAD_BOTTOM_RIGHT = 0xFFFFFF00.toInt()

        /**
         * The colour of [HALF]: red at alpha 128. Its channels are 0 or 255 so converting to the
         * bitmap's premultiplied storage and back loses nothing.
         */
        const val HALF_COLOUR = 0x80FF0000.toInt()
    }
}
