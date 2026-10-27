package com.phongkaster.kanso.ui.tool.component.photocard.stickeroverlay

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * The maths of a sticker on the shown picture, as plain functions on plain numbers.
 *
 * Every function here works in ONE coordinate system: pixels of the overlay, x growing to the
 * right and y growing DOWN, which is how a screen counts. In that system an angle in degrees
 * grows CLOCKWISE (turning from "right" towards "down"), the same sense as a sticker's own
 * degrees and as a layer's rotation. The file knows nothing about Android, the screen or the
 * photo's crop, so a plain JVM test can check every line; the conversion from the photo's
 * fractions to these pixels is done elsewhere, through the one StickerMapping.
 */

/** A sign meaning "the left side" or "the top side" of a sticker rectangle (see [cornerX]). */
const val SIDE_START = -1f

/** A sign meaning "the right side" or "the bottom side" of a sticker rectangle (see [cornerX]). */
const val SIDE_END = 1f

/** The smallest a sticker may be made, as a fraction of the overlay's width (about a twentieth). */
const val MIN_STICKER_WIDTH_FRACTION = 0.05f

/** The largest a sticker may be made, as a fraction of the overlay's width (the whole width). */
const val MAX_STICKER_WIDTH_FRACTION = 1f

/**
 * Rotates the offset ([x], [y]) clockwise by [degrees] about the origin and returns the new x.
 * With y growing downward a clockwise turn is the usual rotation matrix, unchanged.
 */
private fun rotatedX(x: Float, y: Float, degrees: Float): Float {
    val r = Math.toRadians(degrees.toDouble())
    return (x * cos(r) - y * sin(r)).toFloat()
}

/** The y that goes with [rotatedX]: the offset ([x], [y]) turned clockwise by [degrees]. */
private fun rotatedY(x: Float, y: Float, degrees: Float): Float {
    val r = Math.toRadians(degrees.toDouble())
    return (x * sin(r) + y * cos(r)).toFloat()
}

/**
 * Whether the point ([pointX], [pointY]) lies on the sticker whose centre is ([centreX], [centreY]),
 * whose picture is [width] by [height] pixels, turned clockwise by [degrees] about its centre.
 *
 * The sticker is a turned rectangle, so the test turns the POINT back by the same angle about the
 * centre and then asks the easy question: is it within half the width and half the height of the
 * centre? A mirrored sticker covers the same rectangle (a mirror about its own vertical axis maps
 * the rectangle onto itself), so the mirror needs no part in the test. The edge counts as a hit.
 */
fun pointOnSticker(
    pointX: Float,
    pointY: Float,
    centreX: Float,
    centreY: Float,
    width: Float,
    height: Float,
    degrees: Float,
): Boolean {
    val dx = pointX - centreX
    val dy = pointY - centreY
    val localX = rotatedX(dx, dy, -degrees)
    val localY = rotatedY(dx, dy, -degrees)
    return kotlin.math.abs(localX) <= width / 2f && kotlin.math.abs(localY) <= height / 2f
}

/**
 * The x of one corner of the turned sticker rectangle. [sideX] and [sideY] choose the corner:
 * [SIDE_START] or [SIDE_END] for left or right, and top or bottom. The corner of the unturned
 * rectangle is turned clockwise by [degrees] about the centre, so the frame's buttons follow the
 * sticker's turn.
 */
fun cornerX(
    centreX: Float,
    centreY: Float,
    width: Float,
    height: Float,
    degrees: Float,
    sideX: Float,
    sideY: Float,
): Float = centreX + rotatedX(sideX * width / 2f, sideY * height / 2f, degrees)

/** The y that goes with [cornerX]: same arguments, the corner's vertical position in pixels. */
fun cornerY(
    centreX: Float,
    centreY: Float,
    width: Float,
    height: Float,
    degrees: Float,
    sideX: Float,
    sideY: Float,
): Float = centreY + rotatedY(sideX * width / 2f, sideY * height / 2f, degrees)

/** The straight-line distance in pixels from ([fromX], [fromY]) to ([toX], [toY]). */
fun distanceBetween(fromX: Float, fromY: Float, toX: Float, toY: Float): Float =
    hypot(toX - fromX, toY - fromY)

/**
 * The direction in degrees from ([fromX], [fromY]) to ([toX], [toY]): 0 is straight right, 90 is
 * straight down (clockwise on the screen), from -180 to 180.
 */
fun angleBetween(fromX: Float, fromY: Float, toX: Float, toY: Float): Float =
    Math.toDegrees(atan2((toY - fromY).toDouble(), (toX - fromX).toDouble())).toFloat()

/**
 * The turn from the direction [fromDegrees] to the direction [toDegrees], brought into -180 to 180
 * so that crossing the 180 line (170 to -170) is a small turn of 20, not a jump of -340.
 */
fun angleChange(fromDegrees: Float, toDegrees: Float): Float {
    var d = (toDegrees - fromDegrees) % 360f
    if (d > 180f) d -= 360f
    if (d < -180f) d += 360f
    return d
}

/**
 * The sticker's new width when its resize handle's finger moved from [startDistance] to
 * [distance] away from the sticker's centre: the width times the ratio of the two distances, so a
 * finger twice as far makes it twice as wide. The result is kept between [minWidth] and
 * [maxWidth]. A start distance of zero (a finger exactly on the centre) leaves [startWidth].
 */
fun handleWidth(
    startWidth: Float,
    startDistance: Float,
    distance: Float,
    minWidth: Float,
    maxWidth: Float,
): Float {
    if (startDistance <= 0f) return startWidth
    return (startWidth * (distance / startDistance)).coerceIn(minWidth, maxWidth)
}

/**
 * The sticker's new turn when its resize handle's finger moved from the direction [startAngle] to
 * [angle] about the sticker's centre: the starting [startDegrees] plus the change of direction.
 * The picture's shape (its height over width) is not touched by either handle function.
 */
fun handleDegrees(startDegrees: Float, startAngle: Float, angle: Float): Float =
    startDegrees + angleChange(startAngle, angle)

/**
 * Where a sticker's centre goes (x) when a pinch moves from a hand of two fingers centred on
 * ([oldPivotX], [oldPivotY]) to one centred on ([newPivotX], [newPivotY]), scaling by [scale] and
 * turning by [rotation] degrees clockwise. The sticker's offset from the old pivot is scaled and
 * turned and then laid from the new pivot, so the part of the sticker under the fingers stays
 * under the fingers. With one finger, scale 1 and rotation 0, the centre moves by exactly the
 * pivot's move.
 */
fun pinchCentreX(
    centreX: Float,
    centreY: Float,
    oldPivotX: Float,
    oldPivotY: Float,
    newPivotX: Float,
    newPivotY: Float,
    scale: Float,
    rotation: Float,
): Float = newPivotX + scale * rotatedX(centreX - oldPivotX, centreY - oldPivotY, rotation)

/** The y that goes with [pinchCentreX]: same arguments, the new centre's vertical position. */
fun pinchCentreY(
    centreX: Float,
    centreY: Float,
    oldPivotX: Float,
    oldPivotY: Float,
    newPivotX: Float,
    newPivotY: Float,
    scale: Float,
    rotation: Float,
): Float = newPivotY + scale * rotatedY(centreX - oldPivotX, centreY - oldPivotY, rotation)
