package com.phongkaster.kanso.ui.tool

import com.phongkaster.kanso.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.mr0xf00.easycrop.CropState
import com.mr0xf00.easycrop.CropperStyle
import com.mr0xf00.easycrop.CropperStyleGuidelines
import com.mr0xf00.easycrop.LocalCropperStyle
import com.mr0xf00.easycrop.images.ImageBitmapSrc
import com.mr0xf00.easycrop.ui.CropperPreview
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.abs

/** Test tag of the Edit screen's cropper, the photo card while the Crop sheet is open. */
const val EDIT_CROPPER_TAG = "edit-cropper"

/**
 * How far apart two crop rectangles may be, as a fraction of the photo's width or height, and
 * still count as the same: a tenth of a percent. It absorbs the rounding of pixels to fractions
 * and back, so a rectangle the cropper was just given is never reported back as a new action.
 */
private const val SAME_CROP_TOLERANCE = 0.001f

/**
 * The Edit screen's cropper: [bitmap], the original photo in its original orientation, with the
 * crop rectangle of [settings] drawn on it. The person drags a handle to move an edge or a corner,
 * drags inside the rectangle to move it, and pinches to zoom in for precision. The drawing and the
 * touch handling are the `core/imagecrop` module's `CropperPreview` (package
 * `com.mr0xf00.easycrop`); this function connects it to the editing session.
 *
 * **Why the original orientation.** The cropper always shows the photo un-turned and un-flipped
 * (decision A-008): the crop is stored as fractions of the original photo
 * ([EditSettings.CropRect]), and the final render crops first and turns afterwards, so the
 * rectangle the person draws here is exactly the one the render takes.
 *
 * **From the session to the cropper.** The cropper keeps its own rectangle (`CropState.region`,
 * in pixels of the photo). Whenever [settings] carries another crop or crop shape (a ratio chip
 * was tapped, or Undo took a crop back) that rectangle is moved to match, so the handles follow
 * the session. While a fixed shape is chosen the rectangle's shape is locked (`aspectLock`), so a
 * drag on a handle keeps it.
 *
 * **From the cropper to the session.** When the last finger leaves the cropper, the rectangle is
 * turned into fractions of the photo and, if it differs from the session's crop, handed to
 * [onCropFinished] once: one finished gesture is one undo step. A pinch, a tap, or a rectangle the
 * session itself just set changes nothing, so nothing is reported.
 *
 * While [enabled] is false (Compare is on, or Done is rendering the final picture) the cropper ignores every touch.
 *
 * The drawing itself is the stateless [EditCropperContent].
 */
@Composable
fun EditCropper(
    bitmap: Bitmap,
    settings: EditSettings,
    enabled: Boolean,
    onCropFinished: (EditSettings.CropRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One crop state per photo: a new photo starts a new cropper. It starts already showing the
    // session's crop, so the first frame does not jump from the whole photo to the crop.
    val cropState = remember(bitmap) {
        CropState(ImageBitmapSrc(bitmap.asImageBitmap())).also { state ->
            showCrop(state, settings, bitmap.width, bitmap.height)
        }
    }
    // The lambda handed to the pointer handler below lives longer than one composition, so it
    // reads the newest settings and callback through these instead of capturing old ones.
    val currentSettings by rememberUpdatedState(settings)
    val currentOnCropFinished by rememberUpdatedState(onCropFinished)

    // LaunchedEffect runs its block after composition and again whenever one of its keys changes:
    // here, whenever the session's crop or crop shape changes, the cropper is moved to match.
    LaunchedEffect(cropState, settings.crop, settings.cropRatio) {
        showCrop(cropState, settings, bitmap.width, bitmap.height)
    }

    EditCropperContent(
        cropState = cropState,
        enabled = enabled,
        onGestureEnd = {
            val drawn = fractionsOf(cropState.region, bitmap.width, bitmap.height)
            if (!sameCrop(drawn, currentSettings.crop)) currentOnCropFinished(drawn)
        },
        modifier = modifier,
    )
}

/**
 * The stateless part of the cropper (see [EditCropper]): draws [cropState] with the module's
 * `CropperPreview`, in the app's theme colours, filling the space it is given, and calls
 * [onGestureEnd] each time the last finger is lifted from it.
 *
 * While [enabled] is false a transparent layer on top takes every touch, so the handles, the drag
 * and the pinch all stop working, without the cropper changing how it looks.
 */
@Composable
fun EditCropperContent(
    cropState: CropState,
    enabled: Boolean,
    onGestureEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnGestureEnd by rememberUpdatedState(onGestureEnd)
    val colours = MaterialTheme.colorScheme
    // The module draws with a "style": its background, the dimming outside the rectangle, the
    // rectangle's border, its handles and the thirds guidelines. Every colour comes from the theme.
    val style = remember(colours) {
        CropperStyle(
            backgroundColor = colours.surfaceContainerLowest,
            rectColor = colours.primary,
            handleColor = colours.primary,
            guidelines = CropperStyleGuidelines(color = colours.primary.copy(alpha = 0.6f)),
            overlay = colours.scrim.copy(alpha = 0.5f),
            autoZoom = false,
        )
    }
    val photoDescription = stringResource(R.string.tool_photo_description)
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(EDIT_CROPPER_TAG)
            // Screen readers hear the same description the plain photo card carries.
            .semantics { contentDescription = photoDescription }
            // Watches every touch without taking it. The Main pass reaches the cropper inside
            // first, so by the time a lifted finger arrives here the rectangle is final.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.changes.none { it.pressed }) currentOnGestureEnd()
                    }
                }
            },
    ) {
        // CompositionLocalProvider hands a value down to everything drawn inside it: the cropper
        // reads its style from LocalCropperStyle.
        CompositionLocalProvider(LocalCropperStyle provides style) {
            CropperPreview(state = cropState, modifier = Modifier.fillMaxSize())
        }
        if (!enabled) {
            // Drawn on top, this layer is the one the touches reach, and it consumes them all.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    },
            )
        }
    }
}

/**
 * Moves [state]'s rectangle to the crop of [settings] (fractions of a [width] x [height] photo)
 * and locks its shape while a fixed crop shape is chosen.
 *
 * The lock is switched off while the rectangle is set: with it on, the module keeps the OLD
 * rectangle's shape and only scales it, so a newly chosen ratio would never appear. A rectangle
 * already within [SAME_CROP_TOLERANCE] of the crop is left alone, so a crop the cropper itself
 * reported is not set again.
 */
private fun showCrop(state: CropState, settings: EditSettings, width: Int, height: Int) {
    val locked = settings.cropRatio != null && settings.cropRatio != CropRatio.FREE
    if (!sameCrop(fractionsOf(state.region, width, height), settings.crop)) {
        state.aspectLock = false
        state.region = pixelsOf(settings.crop, width, height)
    }
    state.aspectLock = locked
}

/** The rectangle [region], in pixels of a [width] x [height] photo, as fractions kept in 0..1. */
private fun fractionsOf(region: Rect, width: Int, height: Int): EditSettings.CropRect =
    EditSettings.CropRect(
        left = (region.left / width).coerceIn(0f, 1f),
        top = (region.top / height).coerceIn(0f, 1f),
        right = (region.right / width).coerceIn(0f, 1f),
        bottom = (region.bottom / height).coerceIn(0f, 1f),
    )

/** The crop [rect] (fractions) as a rectangle in pixels of a [width] x [height] photo. */
private fun pixelsOf(rect: EditSettings.CropRect, width: Int, height: Int): Rect = Rect(
    left = rect.left * width,
    top = rect.top * height,
    right = rect.right * width,
    bottom = rect.bottom * height,
)

/** True when every edge of [a] is within [SAME_CROP_TOLERANCE] of the same edge of [b]. */
private fun sameCrop(a: EditSettings.CropRect, b: EditSettings.CropRect): Boolean =
    abs(a.left - b.left) <= SAME_CROP_TOLERANCE &&
        abs(a.top - b.top) <= SAME_CROP_TOLERANCE &&
        abs(a.right - b.right) <= SAME_CROP_TOLERANCE &&
        abs(a.bottom - b.bottom) <= SAME_CROP_TOLERANCE

/**
 * A made-up 320 x 240 photo for the previews: a diagonal blend from blue to orange, so the dimmed
 * part outside the rectangle is easy to tell from the part kept. Nothing is decoded from a file.
 */
private fun sampleCropperBitmap(): Bitmap {
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
 * One sample cropper for the previews: the sample photo with the crop [settings] shown, enabled
 * or not, and no callback doing anything. The outer Box fills the preview first, because
 * KansoTheme would otherwise stretch its single child to the whole screen.
 */
@Composable
private fun SampleEditCropper(settings: EditSettings, enabled: Boolean = true) {
    val bitmap = remember { sampleCropperBitmap() }
    val cropState = remember(bitmap) {
        CropState(ImageBitmapSrc(bitmap.asImageBitmap())).also { state ->
            showCrop(state, settings, bitmap.width, bitmap.height)
        }
    }
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            EditCropperContent(cropState = cropState, enabled = enabled, onGestureEnd = {})
        }
    }
}

/**
 * The cropper with a free rectangle keeping the middle of the photo, in light and in dark: the
 * handles on its corners and edges, the outside dimmed.
 */
@PreviewLightDark
@Composable
private fun EditCropperPreview() {
    SampleEditCropper(
        settings = EditSettings(
            cropRatio = CropRatio.FREE,
            crop = EditSettings.CropRect(0.1f, 0.2f, 0.9f, 0.8f),
        ),
    )
}

/**
 * The cropper with the square shape chosen, as a ratio chip leaves it (the largest centred square
 * of the 4:3 sample), while Compare is on, in light and in dark: it looks the same but ignores
 * touches.
 */
@PreviewLightDark
@Composable
private fun EditCropperSquarePreview() {
    SampleEditCropper(
        settings = EditSettings(
            cropRatio = CropRatio.SQUARE,
            crop = EditSettings.CropRect(0.125f, 0f, 0.875f, 1f),
        ),
        enabled = false,
    )
}
