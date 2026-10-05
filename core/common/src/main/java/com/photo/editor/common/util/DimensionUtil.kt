package com.photo.editor.common.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import kotlin.math.max

@Composable
fun Size.toDpSize(): DpSize = with(LocalDensity.current) { DpSize(width.toDp(), height.toDp()) }

fun computeFillMaxDimension(srcWidth: Float, srcHeight: Float, dstWidth: Float, dstHeight: Float): Float {
    val widthScale = computeFillWidth(srcWidth, dstWidth)
    val heightScale = computeFillHeight(srcHeight, dstHeight)
    return max(widthScale, heightScale)
}

private fun computeFillWidth(srcSize: Size, dstSize: Size): Float = dstSize.width.toFloat() / srcSize.width
private fun computeFillHeight(srcSize: Size, dstSize: Size): Float = dstSize.height.toFloat() / srcSize.height

private fun computeFillWidth(srcWidth: Float, dstWidth: Float): Float = dstWidth / srcWidth
private fun computeFillHeight(srcHeight: Float, dstHeight: Float): Float = dstHeight / srcHeight