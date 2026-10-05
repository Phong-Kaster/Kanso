package com.library.colorpicker

internal inline fun tryWithoutCatch(block: () -> Unit) = try {
    block()
} catch (ex: Exception) {
    ex.printStackTrace()
}

internal inline fun <T> tryOrNull(block: () -> T): T? = try {
    block()
} catch (ex: Exception) {
    ex.printStackTrace()
    null
}