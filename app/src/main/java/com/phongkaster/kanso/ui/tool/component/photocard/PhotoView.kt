package com.phongkaster.kanso.ui.tool.component.photocard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_IMAGE_TAG
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY

/**
 * Draws the decoded photo [image] as large as fits in the card, keeping its shape, and marks the
 * image node with [photoUri] so tests can tell which photo is shown.
 */
@Composable
internal fun PhotoView(image: ImageBitmap, photoUri: String) {
    val description = stringResource(R.string.tool_photo_description)
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
            .testTag(TOOL_IMAGE_TAG)
            .semantics { this[TOOL_PHOTO_URI_KEY] = photoUri },
    )
}

/** The photo in light and dark, drawn from the code-made sample picture. */
@PreviewLightDark
@Composable
private fun PhotoViewPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        PhotoView(image = samplePhoto().asImageBitmap(), photoUri = SAMPLE_PHOTO_URI)
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
