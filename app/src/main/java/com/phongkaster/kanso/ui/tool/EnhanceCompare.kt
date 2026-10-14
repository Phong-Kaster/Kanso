package com.phongkaster.kanso.ui.tool

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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the before/after comparison of the Enhance panel. */
const val ENHANCE_COMPARE_TAG = "enhance-compare"

/** Test tag of the control the person holds down to see the original photo. */
const val ENHANCE_COMPARE_HOLD_TAG = "enhance-compare-hold"

/** Test tag of the comparison picture while it shows the original photo. */
const val ENHANCE_COMPARE_ORIGINAL_TAG = "enhance-compare-original"

/** Test tag of the comparison picture while it shows the enhanced photo. */
const val ENHANCE_COMPARE_ENHANCED_TAG = "enhance-compare-enhanced"

/**
 * A semantics property carrying the bitmap the comparison picture is showing right now. Like
 * `TOOL_PHOTO_URI_KEY`, it is a Kanso-only fact for tests, invisible to people and to screen
 * readers: a test can check which picture is shown without comparing pixels.
 */
val ENHANCE_SHOWN_BITMAP_KEY = SemanticsPropertyKey<Bitmap>("EnhanceShownBitmap")

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
 * One of the two comparison pictures: [bitmap] fitted whole into a strip 120 dp tall, tagged
 * [tag] and carrying [bitmap] in [ENHANCE_SHOWN_BITMAP_KEY] for tests.
 */
@Composable
private fun ComparePicture(bitmap: Bitmap, description: String, tag: String) {
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

/**
 * The hold-to-compare control, a pill at least 48 dp tall. [onHeldChange] gets true the moment a
 * finger touches it and false when the finger lifts (or slides off and is cancelled).
 *
 * `pointerInput` reads raw touches; `detectTapGestures(onPress = …)` calls its block on each
 * press, and `tryAwaitRelease()` suspends until that finger lets go. For a screen-reader user,
 * who cannot hold, a double tap (the semantics `onClick`) flips between the two photos instead.
 * [held] colours the pill while the original is shown.
 */
@Composable
private fun HoldToCompare(held: Boolean, onHeldChange: (Boolean) -> Unit) {
    val label = stringResource(R.string.enhance_hold)
    // pointerInput(Unit) starts once and keeps running, so it reads the newest callback through
    // this State instead of the one it captured at the start.
    val currentOnHeldChange by rememberUpdatedState(onHeldChange)
    val currentHeld by rememberUpdatedState(held)
    val container = if (held) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = if (held) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(container)
            .testTag(ENHANCE_COMPARE_HOLD_TAG)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                onClick(label = label) {
                    currentOnHeldChange(!currentHeld)
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        currentOnHeldChange(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            currentOnHeldChange(false)
                        }
                    },
                )
            },
    ) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
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
