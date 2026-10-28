package com.phongkaster.kanso.ui.tool.component.toolpanel.enhancepanel.enhancecompare

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the before/after comparison of the Enhance panel. */
const val ENHANCE_COMPARE_TAG = "enhance-compare"

/** Test tag of the comparison picture while it shows the original photo. */
const val ENHANCE_COMPARE_ORIGINAL_TAG = "enhance-compare-original"

/** Test tag of the comparison picture while it shows the enhanced photo. */
const val ENHANCE_COMPARE_ENHANCED_TAG = "enhance-compare-enhanced"

/** How long the comparison picture takes to fade from one photo to the other, in milliseconds. */
private const val COMPARE_FADE_MILLIS = 150

/**
 * Before and after, in one place: shows the [enhanced] photo, and the [original] while the person
 * holds the "Hold to see the original" control down. Letting go shows the enhanced one again.
 *
 * Only the photo being shown is composed (each under its own test tag), and
 * `AnimatedContent` fades from one to the other. `AnimatedContent` keeps the old content on
 * screen while it fades out and then removes it, so once the fade ends exactly one is left.
 */
@Composable
fun EnhanceCompare(original: Bitmap, enhanced: Bitmap, modifier: Modifier = Modifier) {
    // Whether the original is shown. `remember` keeps it across recompositions; it starts false,
    // so the enhanced photo is what the person sees first.
    var showOriginal by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.testTag(ENHANCE_COMPARE_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedContent(
            targetState = showOriginal,
            transitionSpec = {
                fadeIn(tween(COMPARE_FADE_MILLIS)) togetherWith fadeOut(tween(COMPARE_FADE_MILLIS))
            },
            label = "enhance-compare",
        ) { originalShown ->
            if (originalShown) {
                ComparePicture(
                    bitmap = original,
                    description = stringResource(R.string.enhance_compare_original),
                    tag = ENHANCE_COMPARE_ORIGINAL_TAG,
                )
            } else {
                ComparePicture(
                    bitmap = enhanced,
                    description = stringResource(R.string.enhance_compare_enhanced),
                    tag = ENHANCE_COMPARE_ENHANCED_TAG,
                )
            }
        }
        HoldToCompare(
            held = showOriginal,
            onHeldChange = { showOriginal = it },
        )
    }
}

/**
 * A photo for the comparison previews, drawn in code: a 480 x 320 dim gradient, standing in for
 * the original.
 */
private fun sampleBeforePhoto(): Bitmap = sampleGradient(0xFF3E2723.toInt(), 0xFF1A237E.toInt())

/**
 * A photo for the comparison previews, drawn in code: the same gradient, brighter, standing in
 * for the enhanced copy.
 */
private fun sampleAfterPhoto(): Bitmap = sampleGradient(0xFFFFB74D.toInt(), 0xFF42A5F5.toInt())

/** Draws a 480 x 320 vertical gradient from [top] to [bottom], for the previews. */
private fun sampleGradient(top: Int, bottom: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(480, 320, Bitmap.Config.ARGB_8888)
    val paint = Paint()
    paint.shader = LinearGradient(0f, 0f, 0f, 320f, top, bottom, Shader.TileMode.CLAMP)
    Canvas(bitmap).drawRect(0f, 0f, 480f, 320f, paint)
    return bitmap
}

/**
 * The comparison with the enhanced photo shown, drawn once in light and once in dark, from
 * sample photos drawn in code. The outer Box fills the preview first, because KansoTheme would
 * otherwise stretch its single child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun EnhanceComparePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                EnhanceCompare(
                    original = sampleBeforePhoto(),
                    enhanced = sampleAfterPhoto(),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
