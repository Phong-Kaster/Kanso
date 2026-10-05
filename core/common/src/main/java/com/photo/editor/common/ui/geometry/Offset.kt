package com.photo.editor.common.ui.geometry

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.absoluteValue
import kotlin.math.atan
import kotlin.math.roundToInt

fun Offset.roundToIntOffset(): IntOffset = IntOffset(x.roundToInt(), y.roundToInt())

fun Offset.roundToIntSize(): IntSize = IntSize(x.roundToInt(), y.roundToInt())

fun Offset.adjacent(other: Offset, threshold: Float): Boolean {
    return (this.x - other.x).absoluteValue <= threshold && (this.y - other.y).absoluteValue <= threshold
}

/**
 * Calculate angle degrees between three points.
 * The calculated angle is the angle between vector p1p2 and p1p3
 */
fun calculateAngle(p1: Offset, p2: Offset, p3: Offset): Float {
    val numerator = p2.y * (p1.x - p3.x) + p1.y * (p3.x - p2.x) + p3.y * (p2.x - p1.x)
    val denominator = (p2.x - p1.x) * (p1.x - p3.x) + (p2.y - p1.y) * (p1.y - p3.y)
    val ratio = numerator / denominator

    val angleRad = atan(ratio)
    val angleDeg = (angleRad * 180) / Math.PI

    return angleDeg.toFloat()
}

fun Offset.rotate(degrees: Float, pivot: Offset): Offset {
    val radians = Math.toRadians(degrees.toDouble())
    val cos = kotlin.math.cos(radians).toFloat()
    val sin = kotlin.math.sin(radians).toFloat()
    val newX = pivot.x + (x - pivot.x) * cos - (y - pivot.y) * sin
    val newY = pivot.y + (x - pivot.x) * sin + (y - pivot.y) * cos
    return Offset(newX, newY)
}

fun IntSize.toOffset(): Offset = Offset(width.toFloat(), height.toFloat())