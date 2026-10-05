package com.photo.editor.common.geometry

import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * An immutable, 2D, axis-aligned, floating-point rectangle whose coordinates
 * are relative to a given origin.
 */
data class UnifyRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    companion object {
        /** A rectangle with left, top, right, and bottom edges all at zero. */
        val Zero = UnifyRect(0f, 0f, 0f, 0f)
    }

    /** The distance between the left and right edges of this rectangle. */
    val width: Float
        get() = right - left

    /** The distance between the top and bottom edges of this rectangle. */
    val height: Float
        get() = bottom - top

    /** The distance between the upper-left corner and the lower-right corner of this rectangle. */
    val size: UnifySize
        get() = UnifySize(width, height)

    /** The lesser of the magnitudes of the [width] and the [height] of this rectangle. */
    val minDimension: Float
        get() = min(width.absoluteValue, height.absoluteValue)

    /**
     * The greater of the magnitudes of the [width] and the [height] of this
     * rectangle.
     */
    val maxDimension: Float
        get() = max(width.absoluteValue, height.absoluteValue)

    /**
     * The offset to the intersection of the top and left edges of this rectangle.
     */
    val topLeft: UnifyOffset
        get() = UnifyOffset(left, top)

    /**
     * The offset to the center of the top edge of this rectangle.
     */
    val topCenter: UnifyOffset
        get() = UnifyOffset(left + width / 2.0f, top)

    /**
     * The offset to the intersection of the top and right edges of this rectangle.
     */
    val topRight: UnifyOffset
        get() = UnifyOffset(right, top)

    /**
     * The offset to the center of the left edge of this rectangle.
     */
    val centerLeft: UnifyOffset
        get() = UnifyOffset(left, top + height / 2.0f)

    /**
     * The offset to the point halfway between the left and right and the top and
     * bottom edges of this rectangle.
     *
     * See also [UnifySize.center].
     */
    val center: UnifyOffset
        get() = UnifyOffset(left + width / 2.0f, top + height / 2.0f)

    /**
     * The offset to the center of the right edge of this rectangle.
     */
    val centerRight: UnifyOffset
        get() = UnifyOffset(right, top + height / 2.0f)

    /**
     * The offset to the intersection of the bottom and left edges of this rectangle.
     */
    val bottomLeft: UnifyOffset
        get() = UnifyOffset(left, bottom)

    /**
     * The offset to the center of the bottom edge of this rectangle.
     */
    val bottomCenter: UnifyOffset
        get() { return UnifyOffset(left + width / 2.0f, bottom) }

    /**
     * The offset to the intersection of the bottom and right edges of this rectangle.
     */
    val bottomRight: UnifyOffset
        get() { return UnifyOffset(right, bottom) }

    fun contains(p: UnifyPoint): Boolean {
        return left <= p.x && p.x < left + width && top <= p.y && p.y < top + height
    }
}

