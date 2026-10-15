package com.phongkaster.kanso.tool

import android.graphics.Bitmap
import android.graphics.Color
import com.phongkaster.kanso.data.edit.EditResult
import com.phongkaster.kanso.data.edit.ToolProcessor

/**
 * A stand-in for a real engine in picture tests: it returns a copy of the photo with a warm
 * tint laid over it, instantly, so a picture can show a tool screen after an edit without
 * running a model.
 */
class BrightenedProcessor : ToolProcessor {

    /** Copies [input] and blends a warm colour into every pixel; never fails. */
    override suspend fun process(input: Bitmap, onProgress: (Float) -> Unit): EditResult {
        val out = input.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(out.width * out.height)
        out.getPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        for (i in pixels.indices) {
            val p = pixels[i]
            pixels[i] = Color.rgb(
                (Color.red(p) + 255) / 2,
                (Color.green(p) * 3 + 255) / 4,
                Color.blue(p) / 2,
            )
        }
        out.setPixels(pixels, 0, out.width, 0, 0, out.width, out.height)
        return EditResult.Edited(out)
    }
}
