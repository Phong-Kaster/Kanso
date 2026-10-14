package com.phongkaster.kanso.tool.artistic

import android.graphics.Bitmap
import android.graphics.Color
import com.phongkaster.kanso.data.edit.artistic.ArtisticStyle
import com.phongkaster.kanso.data.edit.artistic.Stylizer

/**
 * A stand-in for the style-transfer engine (which cannot load on the JVM): it "paints" a photo by
 * recolouring every pixel in a fixed way that depends on the style, so a test can compute the
 * expected picture exactly. It counts its calls and remembers the size of each photo it received.
 */
class RecolouringStylizer : Stylizer {

    /** How many times [stylize] was called. */
    var calls = 0
        private set

    /** Width and height of each photo handed to [stylize], in order. */
    val received = mutableListOf<Pair<Int, Int>>()

    /** Returns a new opaque copy of [photo] with each pixel recoloured by [recolour] for [style]. */
    override suspend fun stylize(photo: Bitmap, style: ArtisticStyle): Bitmap {
        calls++
        received += photo.width to photo.height
        val pixels = IntArray(photo.width * photo.height)
        photo.getPixels(pixels, 0, photo.width, 0, 0, photo.width, photo.height)
        for (index in pixels.indices) pixels[index] = recolour(pixels[index], style)
        val result = Bitmap.createBitmap(photo.width, photo.height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, photo.width, 0, 0, photo.width, photo.height)
        return result
    }

    /** The fixed recolouring of each style. */
    companion object {

        /** Returns [pixel] recoloured for [style], always fully opaque. */
        fun recolour(pixel: Int, style: ArtisticStyle): Int {
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            return when (style) {
                ArtisticStyle.WARM_WASH -> Color.rgb(minOf(255, red + 80), green, blue / 2)
                ArtisticStyle.COOL_WASH -> Color.rgb(red / 2, green, minOf(255, blue + 80))
                ArtisticStyle.MOSAIC -> Color.rgb(255 - red, 255 - green, 255 - blue)
                ArtisticStyle.INK -> {
                    val grey = (red + green + blue) / 6
                    Color.rgb(grey, grey, grey)
                }
            }
        }
    }
}
