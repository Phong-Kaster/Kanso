package com.photo.editor.common.geometry

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Because we use many library for edit image,
 * each library has their own implementation of size
 * This class is used to represent size in our app
 * and it has extensions to convert to other sizes back and forth
 **/
data class UnifySize(
    val width: Float,
    val height: Float,
) {
    companion object {
        val Zero = UnifySize(0f, 0f)
    }

    constructor(width: Int, height: Int) : this(width.toFloat(), height.toFloat())
    constructor(width: Double, height: Double) : this(width.toFloat(), height.toFloat())

    /** The lesser of the magnitudes of the [width] and the [height]. */
    val minDimension: Float
        get() = min(width, height)

    /** The greater of the magnitudes of the [width] and the [height]. */
    val maxDimension: Float
        get() = max(width, height)

    val center: UnifyOffset
        get() = UnifyOffset(width / 2f, height / 2f)
}

