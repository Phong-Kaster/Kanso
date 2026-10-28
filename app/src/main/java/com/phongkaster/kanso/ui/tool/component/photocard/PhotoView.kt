package com.phongkaster.kanso.ui.tool.component.photocard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.LivePreview
import com.phongkaster.kanso.data.edit.face.FaceFocus
import com.phongkaster.kanso.data.edit.face.WarpMesh
import kotlin.math.roundToInt
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY
import com.phongkaster.kanso.ui.tool.model.PhotoZoom

/**
 * Draws the decoded photo [image] as large as fits in the card, keeping its shape, and marks the
 * image node with [photoUri] so tests can tell which photo is shown.
 *
 * **Zoom.** The photo is drawn zoomed by [zoom] (the whole photo with [PhotoZoom.NONE]), inside its
 * own frame. A change of [zoom] glides there over [ZOOM_MILLIS] ms instead of jumping, with
 * `animateFloatAsState`, Compose's own animation API: each frame it hands back a value a little
 * closer to the target, and `graphicsLayer` redraws the photo scaled and slid by it without
 * measuring or laying anything out again. A double tap switches between [zoom] and the whole
 * photo, so the person can step back and see everything; that choice is the view's own
 * (`rememberSaveable`), since only how the photo is looked at changes, never the photo.
 *
 * **Live look.** While a slider is dragged, [live] is set: the photo is drawn as it is, then the
 * live look's region (the faces) is cut out of it once (`remember`) and drawn on top through a
 * [WarpMesh] with `Canvas.drawBitmapMesh`, so the graphics chip stretches it between about a
 * thousand moved grid points. A new frame costs almost nothing and the face follows the finger,
 * as in Meitu. When the live look holds a smoothed copy, the same region of it is drawn through
 * the same mesh on top, as opaque as the smoothing slider, which mixes the two as the saved
 * picture does. The Image node itself stays, for its size, its description and the tests;
 * `drawWithContent` adds to what it paints.
 */
@Composable
internal fun PhotoView(image: ImageBitmap, photoUri: String, zoom: PhotoZoom, live: LivePreview?) {
    val description = stringResource(R.string.tool_photo_description)
    var wholePhoto by rememberSaveable(init = { mutableStateOf(false) })
    val target = if (wholePhoto) PhotoZoom.NONE else zoom
    val motion = tween<Float>(durationMillis = ZOOM_MILLIS, easing = FastOutSlowInEasing)
    val scale by animateFloatAsState(targetValue = target.scale, animationSpec = motion, label = "photo-zoom-scale")
    val shiftX by animateFloatAsState(targetValue = target.shiftX, animationSpec = motion, label = "photo-zoom-x")
    val shiftY by animateFloatAsState(targetValue = target.shiftY, animationSpec = motion, label = "photo-zoom-y")
    val mesh = remember(
        live,
        calculation = {
            live?.let(block = { WarpMesh.build(it.pushes, it.region.left, it.region.top, it.region.right, it.region.bottom) })
        },
    )
    // The live region cut out of the photo, and of its smoothed copy, once per region.
    val photoPiece = remember(image, live?.region, calculation = { live?.let(block = { cut(image.asAndroidBitmap(), it.region) }) })
    val smoothedPiece = remember(
        live?.smoothed,
        live?.region,
        calculation = { live?.let(block = { look -> look.smoothed?.let(block = { cut(it, look.region) }) }) },
    )
    // Smooth scaling for the stretched photo, and a second paint whose opacity is the smoothing.
    val photoPaint = remember(calculation = { Paint(Paint.FILTER_BITMAP_FLAG) })
    val overlayPaint = remember(calculation = { Paint(Paint.FILTER_BITMAP_FLAG) })
    Image(
        bitmap = image,
        contentDescription = description,
        // Fit scales the photo down (or up) until it fits whole: never cropped, never stretched.
        contentScale = ContentScale.Fit,
        // aspectRatio sizes the image node itself to the photo's shape, the largest that fits
        // the card, so the node's bounds are exactly where the photo is drawn: it fills the card
        // in one direction, and the blurred backdrop shows in the space left in the other.
        modifier = Modifier
            .aspectRatio(image.width.toFloat() / image.height.toFloat())
            // The zoomed photo is cut to its own frame, so it never spills over the backdrop.
            .clipToBounds()
            .graphicsLayer(
                block = {
                    // Scaling happens around the frame's centre (graphicsLayer's default origin);
                    // the shift is a fraction of the frame, so it is turned into pixels here.
                    scaleX = scale
                    scaleY = scale
                    translationX = shiftX * size.width
                    translationY = shiftY * size.height
                },
            )
            .pointerInput(
                key1 = Unit,
                block = { detectTapGestures(onDoubleTap = { wholePhoto = !wholePhoto }) },
            )
            .drawWithContent(
                onDraw = {
                    // The photo as it is; a live look only adds its region on top.
                    drawContent()
                    if (live == null || mesh == null || photoPiece == null) return@drawWithContent
                    drawIntoCanvas(
                        block = { canvas ->
                            val native = canvas.nativeCanvas
                            // The mesh is in the photo's own pixels; scale them to this node's size.
                            native.save()
                            native.scale(size.width / image.width, size.height / image.height)
                            native.drawBitmapMesh(photoPiece, mesh.columns, mesh.rows, mesh.vertices, 0, null, 0, photoPaint)
                            if (smoothedPiece != null && live.smoothing > 0f) {
                                overlayPaint.alpha = (live.smoothing * OPAQUE).roundToInt().coerceIn(0, OPAQUE)
                                native.drawBitmapMesh(smoothedPiece, mesh.columns, mesh.rows, mesh.vertices, 0, null, 0, overlayPaint)
                            }
                            native.restore()
                        },
                    )
                },
            )
            .testTag(TOOL_IMAGE_TAG)
            .semantics { this[TOOL_PHOTO_URI_KEY] = photoUri },
    )
}

/** How long the photo takes to glide to a new zoom, in milliseconds. */
private const val ZOOM_MILLIS = 450

/** A paint's alpha for fully opaque. */
private const val OPAQUE = 255

/** The part of [bitmap] inside [region], as its own bitmap, for drawing through a mesh. */
private fun cut(bitmap: Bitmap, region: Rect): Bitmap =
    Bitmap.createBitmap(bitmap, region.left, region.top, region.width(), region.height())

/** The photo in light and dark, drawn from the code-made sample picture. */
@PreviewLightDark
@Composable
private fun PhotoViewPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        PhotoView(image = samplePhoto().asImageBitmap(), photoUri = SAMPLE_PHOTO_URI, zoom = PhotoZoom.NONE, live = null)
    }
}

/** The photo zoomed in on its upper left quarter, as on a face there, in light and dark. */
@PreviewLightDark
@Composable
private fun PhotoViewZoomedPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        PhotoView(
            image = samplePhoto().asImageBitmap(),
            photoUri = SAMPLE_PHOTO_URI,
            zoom = PhotoZoom.toward(FaceFocus(left = 0.1f, top = 0.1f, right = 0.4f, bottom = 0.45f)),
            live = null,
        )
    }
}

/** The Uri text the preview pretends its photo came from. */
private const val SAMPLE_PHOTO_URI = "content://media/external/images/media/42"

/** A made-up 640 x 480 photo for the preview, drawn in code: a sky-to-sea gradient. */
private fun samplePhoto(): Bitmap {
    val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, 480f, 0xFFFFB74D.toInt(), 0xFF1565C0.toInt(), Shader.TileMode.CLAMP)
    }
    Canvas(bitmap).drawRect(0f, 0f, 640f, 480f, paint)
    return bitmap
}
