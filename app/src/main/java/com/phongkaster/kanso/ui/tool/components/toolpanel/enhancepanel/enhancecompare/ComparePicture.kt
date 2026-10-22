package com.phongkaster.kanso.ui.tool.components.toolpanel.enhancepanel.enhancecompare

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.TOOL_PHOTO_URI_KEY

/**
 * A semantics property carrying the bitmap the comparison picture is showing right now. Like
 * `TOOL_PHOTO_URI_KEY`, it is a Kanso-only fact for tests, invisible to people and to screen
 * readers: a test can check which picture is shown without comparing pixels.
 */
val ENHANCE_SHOWN_BITMAP_KEY = SemanticsPropertyKey<Bitmap>("EnhanceShownBitmap")

/**
 * One of the two comparison pictures: [bitmap] fitted whole into a strip 120 dp tall, tagged
 * [tag] and carrying [bitmap] in [ENHANCE_SHOWN_BITMAP_KEY] for tests.
 */
@Composable
internal fun ComparePicture(bitmap: Bitmap, description: String, tag: String) {
    // Turning a Bitmap into the ImageBitmap Compose draws is cheap, but remembering it avoids
    // doing it on every recomposition.
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
        bitmap = image,
        contentDescription = description,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .testTag(tag)
            .semantics { this[ENHANCE_SHOWN_BITMAP_KEY] = bitmap },
    )
}

/** One picture of the compare pair, drawn from the code-made sample photo, in light and in dark. */
@PreviewLightDark
@Composable
private fun ComparePicturePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        ComparePicture(bitmap = samplePhoto(), description = "Sample photo", tag = "sample-picture")
    }
}

/** A made-up 640 x 480 photo for the preview, drawn in code: a sky-to-sea gradient. */
private fun samplePhoto(): Bitmap {
    val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        shader = LinearGradient(0f, 0f, 0f, 480f, 0xFFFFB74D.toInt(), 0xFF1565C0.toInt(), Shader.TileMode.CLAMP)
    }
    Canvas(bitmap).drawRect(0f, 0f, 640f, 480f, paint)
    return bitmap
}
