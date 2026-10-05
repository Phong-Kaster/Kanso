package com.photo.editor.util

import org.opencv.core.Size
import kotlin.math.max
import kotlin.math.min


fun computeFillMaxDimension(srcSize: Size, dstSize: Size): Double {
    val widthScale = computeFillWidth(srcSize, dstSize)
    val heightScale = computeFillHeight(srcSize, dstSize)
    return max(widthScale, heightScale)
}

fun computeFillMaxDimension(
    srcWidth: Double,
    srcHeight: Double,
    dstWidth: Double,
    dstHeight: Double
): Double {
    val widthScale = computeFillWidth(Size(srcWidth, srcHeight), Size(dstWidth, dstHeight))
    val heightScale = computeFillHeight(Size(srcWidth, srcHeight), Size(dstWidth, dstHeight))
    return max(widthScale, heightScale)
}

fun computeFillMinDimension(srcSize: Size, dstSize: Size): Double {
    val widthScale = computeFillWidth(srcSize, dstSize)
    val heightScale = computeFillHeight(srcSize, dstSize)
    return min(widthScale, heightScale)
}

fun computeFillMinDimension(
    srcWidth: Double,
    srcHeight: Double,
    dstWidth: Double,
    dstHeight: Double
): Double {
    val widthScale = computeFillWidth(Size(srcWidth, srcHeight), Size(dstWidth, dstHeight))
    val heightScale = computeFillHeight(Size(srcWidth, srcHeight), Size(dstWidth, dstHeight))
    return min(widthScale, heightScale)
}

private fun computeFillWidth(srcSize: Size, dstSize: Size): Double =
    dstSize.width / srcSize.width

private fun computeFillHeight(srcSize: Size, dstSize: Size): Double =
    dstSize.height / srcSize.height