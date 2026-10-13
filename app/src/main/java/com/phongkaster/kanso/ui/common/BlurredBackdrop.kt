package com.phongkaster.kanso.ui.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/**
 * How far [BlurredBackdrop]'s `blur` spreads each pixel. Public so a test can draw a control with
 * exactly the same blur and see whether the machine it runs on applies it at all.
 */
val BACKDROP_BLUR_RADIUS = 24.dp

/**
 * The longest side, in pixels, of the tiny copy [BlurredBackdrop] stretches over its box. Small
 * enough that, stretched back up to a whole card, every detail has melted into soft colour.
 */
const val BACKDROP_MAX_SIDE_PX = 64

/**
 * A soft, blurred copy of [photo] that fills its whole box: the backdrop behind a photo that does
 * not fill its card (beside a narrow photo, above and below a wide one), so the empty space takes
 * the photo's own colours instead of a flat grey. The tool screens put the sharp photo on top of
 * it, and the Saved screen reuses it the same way.
 *
 * How the blur is made, in two layers, so it works on every phone Kanso runs on:
 * 1. A tiny copy. The photo is halved again and again (each step averaging neighbouring pixels,
 *    see [tinyCopy]) until neither side is longer than [BACKDROP_MAX_SIDE_PX] pixels, then drawn
 *    stretched to fill the box (`ContentScale.Crop`, so no edge is left empty) with bitmap
 *    filtering on, which blends smoothly between its few pixels. Stretching 64 pixels across a
 *    whole card is itself a strong blur: every edge and stripe has been averaged away, only the
 *    colours remain. This layer alone is what older phones show.
 * 2. Compose's `Modifier.blur` on top. `blur` asks the graphics system to soften what this
 *    element draws by [BACKDROP_BLUR_RADIUS]. It is built on Android's RenderEffect, which exists
 *    only from Android 12 (API 31): on Android 12 and later it smooths away the last traces of
 *    the tiny copy's pixels; on Android 8.1 to 11 it draws nothing at all, so the stretched copy is
 *    the backdrop there. Either way the result is a soft wash of the photo's colours, never a
 *    sharp duplicate.
 *
 * The tiny copy is made once per photo (`remember(photo)`), not on every frame. The backdrop is
 * decoration, so it has no content description: the photo on top of it is the one that is named.
 */
@Composable
fun BlurredBackdrop(photo: ImageBitmap, modifier: Modifier = Modifier) {
    val tiny = remember(photo) { tinyCopy(photo.asAndroidBitmap()).asImageBitmap() }
    Image(
        bitmap = tiny,
        contentDescription = null,
        // Crop scales the tiny copy up until it covers the whole box, cutting off what spills over.
        contentScale = ContentScale.Crop,
        // Filtering blends between neighbouring pixels while stretching, instead of drawing each
        // pixel as a hard square.
        filterQuality = FilterQuality.High,
        modifier = modifier
            .fillMaxSize()
            .blur(BACKDROP_BLUR_RADIUS),
    )
}

/**
 * Returns a copy of [photo] no longer than [BACKDROP_MAX_SIDE_PX] pixels on either side, made by
 * halving it step by step with filtering on.
 *
 * Why halve in steps instead of shrinking once: a single big shrink with filtering only blends
 * each new pixel from its nearest few old ones, skipping the pixels in between, so thin stripes
 * could survive as hard edges. Halving blends every 2 x 2 block into one pixel, so each step
 * averages everything, and after a few steps the copy holds only the photo's broad colours.
 *
 * A `HARDWARE` bitmap lives in graphics memory only and cannot be read to be scaled, so it is
 * first copied to an ordinary one. A photo that is already small enough is returned as it is.
 */
internal fun tinyCopy(photo: Bitmap): Bitmap {
    var current = if (photo.config == Bitmap.Config.HARDWARE) {
        photo.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        photo
    }
    while (current.width > BACKDROP_MAX_SIDE_PX || current.height > BACKDROP_MAX_SIDE_PX) {
        current = Bitmap.createScaledBitmap(
            current,
            (current.width / 2).coerceAtLeast(1),
            (current.height / 2).coerceAtLeast(1),
            // filter = true: each new pixel blends the old ones it covers.
            true,
        )
    }
    return current
}

/**
 * A narrow sample photo for the preview, drawn in code: 300 x 800 pixels, red-and-white stripes
 * on the upper half and blue-and-white stripes on the lower half, the same pattern the backdrop's
 * test uses, so the preview shows how sharp stripes turn into a soft wash.
 */
private fun sampleStripedPhoto(): Bitmap {
    val bitmap = Bitmap.createBitmap(300, 800, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    canvas.drawColor(android.graphics.Color.WHITE)
    for (left in 0 until 300 step 16) {
        paint.color = android.graphics.Color.RED
        canvas.drawRect(left.toFloat(), 0f, left + 8f, 400f, paint)
        paint.color = android.graphics.Color.BLUE
        canvas.drawRect(left.toFloat(), 400f, left + 8f, 800f, paint)
    }
    return bitmap
}

/**
 * The backdrop of the striped sample photo in a rounded square, in light and in dark: a soft red
 * wash above a soft blue one, with no stripe left. (In the preview pane, as on Android 8.1 to 11,
 * only the stretched tiny copy may show, if the pane does not apply `blur`.)
 */
@PreviewLightDark
@Composable
private fun BlurredBackdropPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface {
            val photo = remember { sampleStripedPhoto().asImageBitmap() }
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .size(200.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
            ) {
                BlurredBackdrop(photo = photo)
            }
        }
    }
}
