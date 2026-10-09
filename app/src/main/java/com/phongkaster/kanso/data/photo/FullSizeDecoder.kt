package com.phongkaster.kanso.data.photo

import android.graphics.Bitmap
import java.io.InputStream

/**
 * Decodes a whole photo at full size, every pixel of it, from a stream.
 *
 * It is a small interface so that a test can hand the saver a decoder that runs out of memory,
 * the way a huge photo would on a real phone, and check that the saver still saves something.
 */
fun interface FullSizeDecoder {

    /** Decodes [input] at full size, or returns null when the bytes are not a picture. */
    fun decode(input: InputStream): Bitmap?
}
