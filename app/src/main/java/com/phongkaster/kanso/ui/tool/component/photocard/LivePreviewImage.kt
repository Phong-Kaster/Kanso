package com.phongkaster.kanso.ui.tool.component.photocard

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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
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
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.pictureId
import com.phongkaster.kanso.data.edit.edit.LayerContent
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

/**
 * What [LivePreviewImage] draws for the sticker under a finger, as stable text for tests to read:
 * `key=3;picture=<id>;drawn=true;` then the same `turns=..;flipH=..;flipV=..;crop=..` text as
 * [LivePreviewGeometry] (the sticker is drawn inside that very crop, turn and flip), then
 * `;matrix=` and the 20 numbers of [LivePreviewMatrix] (the sliders' colours, without the
 * filter), joined by commas. `drawn=false` means the sticker's picture could not be decoded.
 * The property is absent while no sticker is touched.
 */
val LivePreviewTouchedLayer = SemanticsPropertyKey<String>("LivePreviewTouchedSticker")

/** Writes [LivePreviewTouchedLayer] in a `semantics { }` block; reading it back is for tests only. */
var SemanticsPropertyReceiver.livePreviewTouchedLayer by LivePreviewTouchedLayer

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
 * 4. **The sticker under a finger.** The layers not being touched are already inside [bitmap]
 *    (the ViewModel's small copy has them drawn before the filter, so they come out filtered).
 *    The one being moved, pinched or turned cannot wait for a re-render of that copy, so it is
 *    drawn here, every frame, from [touchedLayer] and its decoded [touchedPicture]: inside the
 *    very same crop, turn and flip as the photo (it is placed in the photo's own pixels and goes
 *    through the same transform), with the same colour matrix but without the filter, and
 *    clipped to the part of the photo that is shown, so a sticker that sticks out of the crop is
 *    cut at the photo's edge. Both are null when no sticker is touched.
 *
 * The node carries [LIVE_PREVIEW_TAG], and [LivePreviewMatrix], [LivePreviewGeometry] and
 * [LivePreviewTouchedLayer] say what is applied, for tests.
 */
@Composable
fun LivePreviewImage(
    bitmap: Bitmap,
    settings: EditSettings,
    modifier: Modifier = Modifier,
    touchedLayer: Layer? = null,
    touchedPicture: ImageBitmap? = null,
) {
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
                if (touchedLayer != null) {
                    livePreviewTouchedLayer = "key=${touchedLayer.key};" +
                        "picture=${touchedLayer.pictureId};drawn=${touchedPicture != null};" +
                        "$geometry;matrix=${matrixNumbers.joinToString(",")}"
                }
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
            if (touchedLayer != null && touchedPicture != null) {
                drawTouchedLayer(
                    sticker = touchedLayer,
                    picture = touchedPicture,
                    photoWidth = bitmap.width,
                    photoHeight = bitmap.height,
                    crop = crop,
                    pixelScale = drawWidth.toFloat() / crop.width,
                    drawWidth = drawWidth,
                    drawHeight = drawHeight,
                    colourFilter = colourFilter,
                )
            }
        }
    }
}

/**
 * Draws the touched [sticker] ([picture] is its decoded image) in the already turned and mirrored
 * space of [LivePreviewImage]: the origin is the middle of the shown photo, which is [drawWidth] x
 * [drawHeight] before the turn and the flip. [pixelScale] is how many screen pixels one pixel of the
 * photo takes there.
 *
 * The sticker's centre is a fraction of the ORIGINAL photo, so its place in the shown part is
 * its distance, in photo pixels, from the crop's middle, times [scale]. It is then mirrored,
 * turned by its own angle and moved there (the calls of `withTransform` act on the drawing in
 * reverse order, like in the photo's own transform), and everything is clipped to the shown
 * photo so nothing sticks out of it. [colourFilter] carries the colour sliders (null when they
 * are all in the middle); the filter is not applied to it.
 */
private fun DrawScope.drawTouchedLayer(
    sticker: Layer,
    picture: ImageBitmap,
    photoWidth: Int,
    photoHeight: Int,
    crop: IntRect,
    pixelScale: Float,
    drawWidth: Int,
    drawHeight: Int,
    colourFilter: ColorFilter?,
) {
    val width = sticker.width * photoWidth * pixelScale
    if (!width.isFinite() || width < 1f || picture.width <= 0) return
    val height = width * picture.height / picture.width
    val offsetX = (sticker.centreX * photoWidth - (crop.left + crop.right) / 2f) * pixelScale
    val offsetY = (sticker.centreY * photoHeight - (crop.top + crop.bottom) / 2f) * pixelScale
    if (!offsetX.isFinite() || !offsetY.isFinite() || !sticker.degrees.isFinite()) return
    val w = width.roundToInt().coerceAtLeast(1)
    val h = height.roundToInt().coerceAtLeast(1)
    // Only the shown part of the photo can take a sticker pixel.
    clipRect(-drawWidth / 2f, -drawHeight / 2f, drawWidth / 2f, drawHeight / 2f) {
        withTransform({
            translate(offsetX, offsetY)
            rotate(degrees = sticker.degrees, pivot = Offset.Zero)
            scale(scaleX = if (sticker.mirrored) -1f else 1f, scaleY = 1f, pivot = Offset.Zero)
        }) {
            drawImage(
                image = picture,
                dstOffset = IntOffset(-w / 2, -h / 2),
                dstSize = IntSize(w, h),
                alpha = sticker.opacity.coerceIn(Layer.MIN_OPACITY, 1f),
                colorFilter = colourFilter,
                filterQuality = FilterQuality.Medium,
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

/** A made-up 100 x 60 sticker picture for the previews: an opaque magenta pill. Nothing is decoded. */
private fun sampleStickerPicture(): ImageBitmap {
    val bitmap = Bitmap.createBitmap(100, 60, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply { color = 0xFFD81B60.toInt() }
    android.graphics.Canvas(bitmap).drawRoundRect(0f, 0f, 100f, 60f, 30f, 30f, paint)
    return bitmap.asImageBitmap()
}

/**
 * The same live preview with a sticker under a finger, drawn through the crop, turn, flip and
 * colour of the photo and cut at the photo's edge (it sits half outside the crop), in light and
 * in dark.
 */
@PreviewLightDark
@Composable
private fun LivePreviewImageTouchedStickerPreview() {
    val bitmap = remember { sampleGradientBitmap() }
    val picture = remember { sampleStickerPicture() }
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
                ),
                touchedLayer = Layer(
                    key = 1,
                    content = LayerContent.Picture("sample"),
                    centreX = 0.66f,
                    centreY = 0.4f,
                    width = 0.3f,
                    degrees = 15f,
                    mirrored = false,
                ),
                touchedPicture = picture,
            )
        }
    }
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
