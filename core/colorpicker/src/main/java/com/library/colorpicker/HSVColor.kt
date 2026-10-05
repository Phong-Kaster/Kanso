package com.library.colorpicker

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.graphics.Color
import com.github.ajalt.colormath.RenderCondition
import com.github.ajalt.colormath.model.HSV
import com.github.ajalt.colormath.model.RGB

/**
 * https://github.com/godaddy/compose-color-picker/blob/main/color-picker/src/commonMain/kotlin/com/godaddy/android/colorpicker/HsvColor.kt
 * A representation of Color in Hue, Saturation and Value form.
 */
internal data class HSVColor(
    // from = 0.0, to = 360.0
    val hue: Float,

    // from = 0.0, to = 1.0
    val saturation: Float,

    // from = 0.0, to = 1.0
    val value: Float,

    // from = 0.0, to = 1.0
    val alpha: Float
) {
    fun toColor(): Color {
        val hsv = HSV(hue, saturation, value, alpha)
        val rgb = hsv.toSRGB()
        return Color(rgb.r, rgb.g, rgb.b, rgb.alpha)
    }

    fun toHex(): String {
        val hsv = HSV(hue, saturation, value, alpha)
        val rgb = hsv.toSRGB()
        return rgb.toHex(withNumberSign = false, renderAlpha = RenderCondition.NEVER)
    }

    companion object {
        val DEFAULT = HSVColor(360f, 1.0f, 1.0f, 1.0f)

        /**
         *  the color math hsv to local hsv color
         */
        private fun HSV.toColor(): HSVColor {
            return HSVColor(
                hue = if (this.h.isNaN()) 0f else this.h,
                saturation = this.s,
                value = this.v,
                alpha = this.alpha
            )
        }

        fun from(color: Color): HSVColor {
            return RGB(color.red, color.green, color.blue, color.alpha).toHSV().toColor()
        }

        val Saver: Saver<HSVColor, *> = listSaver(
            save = { listOf(it.hue, it.saturation, it.value, it.alpha) },
            restore = { HSVColor(it[0], it[1], it[2], it[3]) }
        )
    }
}