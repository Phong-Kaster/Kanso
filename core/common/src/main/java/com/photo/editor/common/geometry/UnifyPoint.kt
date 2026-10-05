package com.photo.editor.common.geometry

data class UnifyPoint(
    val x: Float,
    val y: Float,
) {
    companion object {
        val Zero = UnifyPoint(0f, 0f)
    }

    constructor(): this(0f, 0f)
    constructor(x: Double, y: Double) : this(x.toFloat(), y.toFloat())
    constructor(x: Int, y: Int) : this(x.toFloat(), y.toFloat())

    operator fun plus(other: UnifyPoint): UnifyPoint {
        return UnifyPoint(x + other.x, y + other.y)
    }

    operator fun minus(other: UnifyPoint): UnifyPoint {
        return UnifyPoint(x - other.x, y - other.y)
    }

    operator fun times(number: Float): UnifyPoint {
        return UnifyPoint(x * number, y * number)
    }

    fun dot(p: UnifyPoint): Float {
        return x * p.x + y * p.y
    }
}