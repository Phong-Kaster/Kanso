package com.photo.editor.common.ui.transformableimage

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size

/**
 * Class hold transformation information: scale, translate
 * The class also hold surface size on which transformation was applied
 **/
@Stable
class TransformState {
    var surfaceSize: Size by mutableStateOf(Size.Zero)

    var scale: Float by mutableFloatStateOf(1f)
        private set

    var translateX: Float by mutableFloatStateOf(0f)
        private set

    var translateY: Float by mutableFloatStateOf(0f)
        private set

    fun scaleAndTranslate(scale: Float, translateX: Float, translateY: Float) {
        if (surfaceSize.width == 0f || surfaceSize.height == 0f) {
            this.scale = 1f
            this.translateX = 0f
            this.translateY = 0f
            return
        }

        var targetScale = this.scale * scale
        var targetTranslateX = this.translateX + translateX
        var targetTranslateY = this.translateY + translateY

        targetScale = targetScale.coerceIn(0.5f, 10f)

        val width = surfaceSize.width
        val height = surfaceSize.height

        if (targetScale > 1) {
            val maxOffsetX = ((width * targetScale / 2 - width / 2)).coerceAtLeast(0f)
            val maxOffsetY = ((height * targetScale / 2 - height / 2)).coerceAtLeast(0f)
            targetTranslateX = targetTranslateX.coerceIn(-maxOffsetX, maxOffsetX)
            targetTranslateY = targetTranslateY.coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            targetTranslateX = 0f
            targetTranslateY = 0f
        }


        this.scale = targetScale
        this.translateX = targetTranslateX
        this.translateY = targetTranslateY
    }

    fun reset() {
        this.scale = 1f
        this.translateX = 0f
        this.translateY = 0f
    }
}