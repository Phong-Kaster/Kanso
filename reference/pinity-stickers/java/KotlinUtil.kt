package com.apero.pinity.util

inline fun tryWithoutCatch(block: () -> Unit) = try {
    block()
} catch (ex: Exception) {
    ex.printStackTrace()
}

data class Five<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)