package com.phongkaster.kanso.data.edit.edit

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The one translation between what the user SEES while placing stickers and where a sticker
 * really lives.
 *
 * The user sees the photo after the crop, the quarter turns and the flips (the "shown picture").
 * A [Sticker] is stored as fractions of the ORIGINAL photo, so a later crop, turn or flip carries
 * it along. Done ([EditProcessor]) applies the steps in this order: crop, then turn, then flip,
 * and draws each sticker before them. This class is exactly the inverse of that, so a sticker put
 * at a spot of the shown picture is drawn by Done at that same spot.
 *
 * Positions are [Point]s from 0 to 1 across the picture, y growing downward. Turning and flipping
 * the whole picture also turns the sticker's look, so the angle and the mirror flag are mapped
 * too. A flip is a mirror: it reverses the sense of every angle and toggles [Sticker.mirrored].
 *
 * It is plain Kotlin with no Android types, so it can be tested on the JVM.
 *
 * @param originalWidth the original photo's width in pixels (pixels, so aspect ratios are right).
 * @param originalHeight the original photo's height in pixels.
 * @param settings the edit whose crop, quarter turns and flips define the shown picture.
 */
class StickerMapping(
    originalWidth: Int,
    originalHeight: Int,
    settings: EditSettings,
) {
    /** A position as fractions of a picture: x from 0 (left) to 1 (right), y from 0 (top) to 1 (bottom). */
    data class Point(val x: Float, val y: Float)

    private val photoWidth = originalWidth.toFloat()
    private val photoHeight = originalHeight.toFloat()

    // Clockwise quarter turns reduced to 0..3 (-1 is the same as 3), as Done does.
    private val turns = ((settings.quarterTurns % 4) + 4) % 4
    private val flipH = settings.flipHorizontal
    private val flipV = settings.flipVertical

    // The kept rectangle in original pixels, rounded and clamped the way Done does it.
    private val cropLeft: Int
    private val cropTop: Int
    private val cropWidth: Int
    private val cropHeight: Int

    init {
        val rect = settings.crop
        val w = originalWidth
        val h = originalHeight
        val left = (minOf(rect.left, rect.right).coerceIn(0f, 1f) * w).roundToInt().coerceIn(0, w - 1)
        val top = (minOf(rect.top, rect.bottom).coerceIn(0f, 1f) * h).roundToInt().coerceIn(0, h - 1)
        val right = (maxOf(rect.left, rect.right).coerceIn(0f, 1f) * w).roundToInt().coerceIn(left + 1, w)
        val bottom = (maxOf(rect.top, rect.bottom).coerceIn(0f, 1f) * h).roundToInt().coerceIn(top + 1, h)
        cropLeft = left
        cropTop = top
        cropWidth = right - left
        cropHeight = bottom - top
    }

    // The shown picture's width in pixels: an odd number of quarter turns swaps width and height.
    private val shownWidthPx = if (turns % 2 == 1) cropHeight.toFloat() else cropWidth.toFloat()

    // Whether the whole picture is mirrored (one flip, not two): the turn is then reversed.
    private val reversing = flipH != flipV

    /**
     * The angle in degrees (clockwise, y down) of the picture's left-to-right direction after the
     * turns and flips: where the original's "right" ends up. Used to carry a sticker's angle.
     */
    private val axisDegrees: Float

    init {
        // The direction (1, 0) turned clockwise by [turns] quarters, then flipped.
        val radians = Math.toRadians(90.0 * turns)
        var x = cos(radians)
        var y = sin(radians)
        if (flipH) x = -x
        if (flipV) y = -y
        axisDegrees = Math.toDegrees(atan2(y, x)).toFloat()
    }

    /** Turns a shown-picture position into the original photo's fractions (undoing flips, then turns, then the crop). */
    fun shownToOriginal(p: Point): Point {
        // Undo the flips (they were done last).
        val u = if (flipH) 1f - p.x else p.x
        val v = if (flipV) 1f - p.y else p.y
        // Undo the turns: (a, b) is the position inside the cropped picture.
        val a: Float
        val b: Float
        when (turns) {
            1 -> { a = v; b = 1f - u }
            2 -> { a = 1f - u; b = 1f - v }
            3 -> { a = 1f - v; b = u }
            else -> { a = u; b = v }
        }
        // Undo the crop: the cropped picture sits inside the original at the rounded rectangle.
        return Point((cropLeft + a * cropWidth) / photoWidth, (cropTop + b * cropHeight) / photoHeight)
    }

    /** Turns an original-photo position into the shown picture's fractions (the crop, then the turns, then the flips). */
    fun originalToShown(p: Point): Point {
        // The position inside the cropped picture.
        val a = (p.x * photoWidth - cropLeft) / cropWidth
        val b = (p.y * photoHeight - cropTop) / cropHeight
        // Turn clockwise by quarters, as Done's turn step moves each pixel.
        var u: Float
        var v: Float
        when (turns) {
            1 -> { u = 1f - b; v = a }
            2 -> { u = 1f - a; v = 1f - b }
            3 -> { u = b; v = 1f - a }
            else -> { u = a; v = b }
        }
        if (flipH) u = 1f - u
        if (flipV) v = 1f - v
        return Point(u, v)
    }

    /** Maps a sticker angle seen on the shown picture to the angle stored on the original. */
    fun shownAngleToOriginal(degrees: Float): Float =
        // Without a mirror the angle just loses the turn; with one it is reflected about the axis.
        normalise(if (reversing) axisDegrees + 180f - degrees else degrees - axisDegrees)

    /** Maps a sticker angle stored on the original to the angle seen on the shown picture. */
    fun originalAngleToShown(degrees: Float): Float =
        normalise(if (reversing) axisDegrees + 180f - degrees else degrees + axisDegrees)

    /** A mirrored picture shows a sticker with its mirror flag the other way round. */
    fun shownMirrorToOriginal(mirrored: Boolean): Boolean = if (reversing) !mirrored else mirrored

    /** The inverse of [shownMirrorToOriginal] (the same toggle, since a mirror undoes itself). */
    fun originalMirrorToShown(mirrored: Boolean): Boolean = if (reversing) !mirrored else mirrored

    /** A width as a fraction of the shown picture's width, as a fraction of the original's width. */
    fun shownWidthToOriginal(width: Float): Float = width * shownWidthPx / photoWidth

    /** A width as a fraction of the original's width, as a fraction of the shown picture's width. */
    fun originalWidthToShown(width: Float): Float = width * photoWidth / shownWidthPx

    /**
     * A new sticker of [pictureId] with identity [key], at the shown picture's centre, a third of
     * its width, upright and not mirrored, already converted to the original's fractions.
     */
    fun centredSticker(key: Long, pictureId: String): Sticker {
        val centre = shownToOriginal(Point(0.5f, 0.5f))
        return Sticker(
            key = key,
            pictureId = pictureId,
            centreX = centre.x,
            centreY = centre.y,
            width = shownWidthToOriginal(1f / 3f),
            degrees = shownAngleToOriginal(0f),
            mirrored = shownMirrorToOriginal(false),
        )
    }

    /** Brings [degrees] into the range 0 (included) to 360 (excluded). */
    private fun normalise(degrees: Float): Float {
        val r = degrees % 360f
        return if (r < 0f) r + 360f else r
    }
}
