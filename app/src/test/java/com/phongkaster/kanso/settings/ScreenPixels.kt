package com.phongkaster.kanso.settings

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import org.junit.Assert.assertTrue

/**
 * What one drawing of the screen shows: the [background] colour near the bottom-left corner
 * (plain background on Home and Settings, never text) and the [ink] colour, the pixel inside a
 * text's bounds that differs most in luminance from that background, i.e. the text itself.
 */
internal class ScreenSample(val background: Color, val ink: Color)

/**
 * Draws this activity's Compose content into a fresh bitmap and returns the colour of a pixel
 * near its bottom-left corner, which on Home and Settings is plain background, never text.
 *
 * It draws the content view itself (as Roborazzi does) instead of using Compose's
 * captureToImage, whose wait for the window to redraw times out under Robolectric here. The
 * content view is drawn without the system bar backgrounds, so only the app's own pixels count.
 * Call it on the main thread (the test thread under Robolectric).
 */
internal fun Activity.drawnColorNearBottomLeft(): Color = drawContent { bitmap ->
    backgroundOf(bitmap)
}

/**
 * Draws this activity's Compose content once and samples both the background and the text
 * inside [textBounds] (a text node's bounds in the Compose root, in pixels). The Compose root
 * fills the content view from its top-left corner, so the two share coordinates.
 */
internal fun Activity.sampleScreen(textBounds: Rect): ScreenSample = drawContent { bitmap ->
    val background = backgroundOf(bitmap)
    val left = textBounds.left.toInt().coerceIn(0, bitmap.width - 1)
    val top = textBounds.top.toInt().coerceIn(0, bitmap.height - 1)
    val right = textBounds.right.toInt().coerceIn(left + 1, bitmap.width)
    val bottom = textBounds.bottom.toInt().coerceIn(top + 1, bitmap.height)
    val backgroundLuminance = background.luminance()
    var ink = background
    var bestDistance = -1f
    for (y in top until bottom) {
        for (x in left until right) {
            val color = Color(bitmap.getPixel(x, y))
            val distance = abs(color.luminance() - backgroundLuminance)
            if (distance > bestDistance) {
                bestDistance = distance
                ink = color
            }
        }
    }
    ScreenSample(background = background, ink = ink)
}

/** Draws the activity's content view into a bitmap, hands it to [read], then frees it. */
private fun <T> Activity.drawContent(read: (Bitmap) -> T): T {
    val content = findViewById<ViewGroup>(android.R.id.content)
    val view = content.getChildAt(0) ?: error("The activity has no content view yet")
    check(view.width > 0 && view.height > 0) { "The content view is not laid out yet" }
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    return try {
        read(bitmap)
    } finally {
        bitmap.recycle()
    }
}

/** Returns the colour of the pixel near the bottom-left corner of [bitmap]. */
private fun backgroundOf(bitmap: Bitmap): Color = Color(bitmap.getPixel(2, bitmap.height - 3))

/**
 * Tells whether [color] is light (relative luminance above one half). Only an opaque pixel
 * counts, so an empty, never-drawn bitmap is neither light nor dark.
 */
internal fun isLight(color: Color): Boolean = color.alpha > 0.99f && color.luminance() > 0.5f

/**
 * Tells whether [color] is dark (relative luminance below one half). Only an opaque pixel
 * counts: a never-drawn bitmap is transparent black and must not pass as dark.
 */
internal fun isDark(color: Color): Boolean = color.alpha > 0.99f && color.luminance() < 0.5f

/** Fails unless [color] is an opaque light pixel. */
internal fun assertLight(color: Color) {
    assertTrue("Expected a light screen, got $color", isLight(color))
}

/** Fails unless [color] is an opaque dark pixel. */
internal fun assertDark(color: Color) {
    assertTrue("Expected a dark screen, got $color", isDark(color))
}

/**
 * Fails unless [sample] shows the dark theme drawn through: a dark background and light text on
 * it, so a screen that only painted its own background dark would not pass.
 */
internal fun assertDrawnDark(sample: ScreenSample) {
    assertDark(sample.background)
    assertTrue(
        "Expected light text on the dark background, highest-contrast text pixel was ${sample.ink}",
        isLight(sample.ink),
    )
}

/**
 * Fails unless [sample] shows the light theme drawn through: a light background and dark text
 * on it.
 */
internal fun assertDrawnLight(sample: ScreenSample) {
    assertLight(sample.background)
    assertTrue(
        "Expected dark text on the light background, highest-contrast text pixel was ${sample.ink}",
        isDark(sample.ink),
    )
}
