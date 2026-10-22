package com.phongkaster.kanso.ui.tool.components.photocard

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.phongkaster.kanso.data.edit.edit.EditColourMatrix
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.min
import kotlin.math.roundToInt

/** Test tag of the Edit screen's live preview of the photo ([LivePreviewImage]). */
const val LIVE_PREVIEW_TAG = "live-preview"

/**
 * The colour matrix [LivePreviewImage] applies, as its 20 numbers (4 rows of 5, row after row):
 * exactly the identity matrix when no colour slider has moved. It lives in the node's semantics
 * (the description of the screen that tests and screen readers read), so a test can check which
 * colours are drawn without reading pixels.
 */
val LivePreviewMatrix = SemanticsPropertyKey<List<Float>>("LivePreviewMatrix")

/** Writes [LivePreviewMatrix] in a `semantics { }` block; reading it back is for tests only. */
var SemanticsPropertyReceiver.livePreviewMatrix by LivePreviewMatrix

/**
 * The geometry [LivePreviewImage] applies, as stable text such as
 * `turns=1;flipH=true;flipV=false;crop=0.0,0.0,1.0,1.0` (the quarter turns 0 to 3, the two flips,
 * then the crop's left, top, right and bottom as fractions of the photo), for tests to read.
 */
val LivePreviewGeometry = SemanticsPropertyKey<String>("LivePreviewGeometry")

/** Writes [LivePreviewGeometry] in a `semantics { }` block; reading it back is for tests only. */
var SemanticsPropertyReceiver.livePreviewGeometry by LivePreviewGeometry

/** The identity colour matrix's 20 numbers: ones on the diagonal (places 0, 6, 12, 18). */
private val IDENTITY_MATRIX = List(20) { index -> if (index % 6 == 0) 1f else 0f }

/**
 * Draws [bitmap] with the Edit [settings] applied live, every frame, by Compose itself, filling
 * the space [modifier] gives it (it fills its parent by default).
 *
 * [bitmap] is the original photo, or its small copy with the filter already applied (the
 * ViewModel's `filterPreview`): the filter is the one part Compose cannot draw. Everything else
 * is applied here while drawing, so the photo follows a slider while the finger is still on it
 * and no full-size picture is ever rendered before the edit is finished:
 *
 * 1. **Crop.** Only the rectangle `settings.crop` (fractions of the photo, turned into pixels the
 *    same way the final render does) is taken from [bitmap].
 * 2. **Quarter turns, then flips.** The cropped part is turned clockwise, then mirrored along
 *    the screen's axes, the same order as the final render (crop, turn, flip). It is scaled to
 *    fit inside the box whole, keeping its shape, and centred.
 * 3. **Colour sliders.** Brightness, contrast, saturation and warmth become one colour matrix
 *    ([EditColourMatrix.of], the very numbers the final render uses), handed to the GPU as a
 *    `ColorFilter.colorMatrix`; with every slider in the middle no filter is set at all.
 *
 * The node carries [LIVE_PREVIEW_TAG], and [LivePreviewMatrix] and [LivePreviewGeometry] say
 * what is applied, for tests.
 */
@Composable
fun LivePreviewImage(bitmap: Bitmap, settings: EditSettings, modifier: Modifier = Modifier) {
    // asImageBitmap wraps the Android bitmap for Compose without copying its pixels; remember
    // keeps the wrapper until another bitmap arrives.
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val matrix = remember(settings) { EditColourMatrix.of(settings) }
    val identity = remember(matrix) { EditColourMatrix.isIdentity(matrix) }
    val colourFilter = remember(matrix, identity) {
        if (identity) null else ColorFilter.colorMatrix(ColorMatrix(matrix))
    }
    val matrixNumbers = if (identity) IDENTITY_MATRIX else matrix.toList()
    val turns = ((settings.quarterTurns % 4) + 4) % 4
    val crop = cropPixels(bitmap.width, bitmap.height, settings.crop)
    val geometry = "turns=$turns;flipH=${settings.flipHorizontal};flipV=${settings.flipVertical};" +
        "crop=${settings.crop.left},${settings.crop.top},${settings.crop.right},${settings.crop.bottom}"
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag(LIVE_PREVIEW_TAG)
            .semantics {
                livePreviewMatrix = matrixNumbers
                livePreviewGeometry = geometry
            },
    ) {
        // A quarter or three-quarter turn swaps the picture's width and height on screen.
        val sideways = turns % 2 == 1
        val shownWidth = if (sideways) crop.height else crop.width
        val shownHeight = if (sideways) crop.width else crop.height
        // "Contain": the largest scale at which the whole turned picture fits in the box.
        val fit = min(size.width / shownWidth, size.height / shownHeight)
        val drawWidth = (crop.width * fit).roundToInt()
        val drawHeight = (crop.height * fit).roundToInt()
        val centre = center
        // withTransform applies its steps to the drawing in reverse order of the calls: the
        // picture, drawn centred on (0, 0), is first turned, then mirrored in the screen's axes,
        // then moved to the middle of the box. Both pivots are (0, 0), the picture's centre.
        withTransform({
            translate(centre.x, centre.y)
            scale(
                scaleX = if (settings.flipHorizontal) -1f else 1f,
                scaleY = if (settings.flipVertical) -1f else 1f,
                pivot = Offset.Zero,
            )
            rotate(degrees = 90f * turns, pivot = Offset.Zero)
        }) {
            drawImage(
                image = image,
                srcOffset = IntOffset(crop.left, crop.top),
                srcSize = IntSize(crop.width, crop.height),
                dstOffset = IntOffset(-drawWidth / 2, -drawHeight / 2),
                dstSize = IntSize(drawWidth, drawHeight),
                colorFilter = colourFilter,
            )
        }
    }
}

/**
 * The crop [rect] (fractions of the photo) as pixels of a [width] x [height] picture, rounded the
 * same way the final render rounds it: edges put in order, kept inside the picture, at least
 * 1 x 1. So the preview takes exactly the rectangle Done will keep.
 */
private fun cropPixels(width: Int, height: Int, rect: EditSettings.CropRect): IntRect {
    val left = (min(rect.left, rect.right).coerceIn(0f, 1f) * width).roundToInt().coerceIn(0, width - 1)
    val top = (min(rect.top, rect.bottom).coerceIn(0f, 1f) * height).roundToInt().coerceIn(0, height - 1)
    val right = (maxOf(rect.left, rect.right).coerceIn(0f, 1f) * width).roundToInt().coerceIn(left + 1, width)
    val bottom = (maxOf(rect.top, rect.bottom).coerceIn(0f, 1f) * height).roundToInt().coerceIn(top + 1, height)
    return IntRect(left, top, right, bottom)
}

/**
 * A made-up 320 x 240 photo for the previews: a diagonal blend from blue to orange, so the crop,
 * the turn and the colour change are all visible. Nothing is decoded from a file.
 */
private fun sampleGradientBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 320f, 240f,
            0xFF1A73E8.toInt(), 0xFFF29900.toInt(),
            Shader.TileMode.CLAMP,
        )
    }
    android.graphics.Canvas(bitmap).drawRect(0f, 0f, 320f, 240f, paint)
    return bitmap
}

/**
 * The live preview of a photo cropped to its left two thirds, turned a quarter, mirrored and
 * made warmer and brighter, in light and in dark.
 */
@PreviewLightDark
@Composable
private fun LivePreviewImagePreview() {
    val bitmap = remember { sampleGradientBitmap() }
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            LivePreviewImage(
                bitmap = bitmap,
                settings = EditSettings(
                    crop = EditSettings.CropRect(0f, 0f, 0.66f, 1f),
                    quarterTurns = 1,
                    flipHorizontal = true,
                    brightness = 0.2f,
                    warmth = 0.5f,
                ),
            )
        }
    }
}
