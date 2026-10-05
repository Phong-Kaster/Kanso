package com.photo.editor.common.util

inline fun tryWithoutCatch(block: () -> Unit) = try {
    block()
} catch (ex: Exception) {
    ex.printStackTrace()
}

inline fun <T> tryOrNull(block: () -> T): T? = try {
    block()
} catch (ex: Exception) {
    ex.printStackTrace()
    null
}

data class Five<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)

fun min(value1: Float, value2: Float, value3: Float, value4: Float): Float {
    return kotlin.math.min(kotlin.math.min(value1.toDouble(), value2.toDouble()), kotlin.math.min(value3.toDouble(), value4.toDouble())).toFloat()
}

fun max(value1: Float, value2: Float, value3: Float, value4: Float): Float {
    return kotlin.math.max(kotlin.math.max(value1.toDouble(), value2.toDouble()), kotlin.math.max(value3.toDouble(), value4.toDouble())).toFloat()
}