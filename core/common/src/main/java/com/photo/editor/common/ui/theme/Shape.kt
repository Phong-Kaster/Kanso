@file:Suppress("FunctionName")

package com.photo.editor.common.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Shape

fun VerticalClipRect(
    startClipPercent: Float = 0f,
    endClipPercent: Float = 1f,
): Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val top = 0f
    val left = w * startClipPercent
    val bottom = h
    val right = w * endClipPercent

    moveTo(left, top)
    lineTo(right, top)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}

fun HorizontalClipRect(
    startClipPercent: Float = 0f,
    endClipPercent: Float = 1f,
): Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val top = h * startClipPercent
    val left = 0f
    val bottom = h * endClipPercent
    val right = w

    moveTo(left, top)
    lineTo(right, top)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}

fun ClipRect(
    verticalStartClipPercent: Float = 0f,
    verticalEndClipPercent: Float = 1f,
    horizontalStartClipPercent: Float = 0f,
    horizontalEndClipPercent: Float = 1f,
): Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val top = h * verticalStartClipPercent
    val left = w * horizontalStartClipPercent
    val bottom = h * verticalEndClipPercent
    val right = w * horizontalEndClipPercent

    moveTo(left, top)
    lineTo(right, top)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}