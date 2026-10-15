package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the Enhance panel's button that runs Enhance on the photo. */
const val ENHANCE_RUN_TAG = "enhance-run"

/**
 * The Enhance tool's controls, shown in the options strip under the photo.
 *
 * It holds a button that runs Enhance ([onRun]: the caller runs the Enhance processor through
 * the tool screen's ViewModel, which shows the progress on the photo card) and, once there is an
 * [original] and an [enhanced] photo to compare, the before/after comparison ([EnhanceCompare]).
 * It is stateless: it draws what it is given and reports taps, so the tool screen decides what
 * the two photos are and a preview or test can hand it sample bitmaps.
 */
@Composable
fun EnhancePanel(
    onRun: () -> Unit,
    original: Bitmap?,
    enhanced: Bitmap?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ENHANCE_PANEL_TAG)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (original != null && enhanced != null) {
            EnhanceCompare(
                original = original,
                enhanced = enhanced,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            onClick = onRun,
            modifier = Modifier
                .fillMaxWidth()
                // 48 dp is the smallest touch target Android's accessibility guidance allows.
                .heightIn(min = 48.dp)
                .testTag(ENHANCE_RUN_TAG),
        ) {
            Text(stringResource(R.string.enhance_run))
        }
    }
}

/**
 * Draws a 480 x 320 vertical gradient from [top] to [bottom], a stand-in photo for the previews.
 */
private fun sampleEnhancePhoto(top: Int, bottom: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(480, 320, Bitmap.Config.ARGB_8888)
    val paint = Paint()
    paint.shader = LinearGradient(0f, 0f, 0f, 320f, top, bottom, Shader.TileMode.CLAMP)
    Canvas(bitmap).drawRect(0f, 0f, 480f, 320f, paint)
    return bitmap
}

/**
 * The Enhance panel before Enhance has run: the button alone, drawn once in light and once in
 * dark. The outer Box fills the preview first, because KansoTheme would otherwise stretch its
 * single child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun EnhancePanelPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                EnhancePanel(onRun = {}, original = null, enhanced = null)
            }
        }
    }
}

/**
 * The Enhance panel after Enhance has run: the before/after comparison above the button, from two
 * sample photos drawn in code (a dim one and a brighter one), in light and in dark.
 */
@PreviewLightDark
@Composable
private fun EnhancePanelComparePreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                EnhancePanel(
                    onRun = {},
                    original = sampleEnhancePhoto(0xFF3E2723.toInt(), 0xFF1A237E.toInt()),
                    enhanced = sampleEnhancePhoto(0xFFFFB74D.toInt(), 0xFF42A5F5.toInt()),
                )
            }
        }
    }
}
