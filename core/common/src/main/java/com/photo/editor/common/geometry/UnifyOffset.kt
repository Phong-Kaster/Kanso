package com.photo.editor.common.geometry

data class UnifyOffset(
    val x: Float,
    val y: Float,
) {
    companion object {
        val Zero = UnifyOffset(0f, 0f)
    }
}
