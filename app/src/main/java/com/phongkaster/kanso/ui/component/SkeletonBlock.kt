package com.phongkaster.kanso.ui.component

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/**
 * How long the band of light takes to cross a skeleton block once, in milliseconds: slow enough
 * to read as "waiting", not as an alarm.
 */
private const val PULSE_MILLIS = 1_200

/** How wide the moving band is, as a share of the block's width. */
private const val BAND_WIDTH_FRACTION = 0.6f

/**
 * Where the moving band of every [SkeletonBlock] below is, as a reader of a number from 0 (the
 * band is just off the block's left edge) to 1 (just off its right edge).
 *
 * What a CompositionLocal is. Most values reach a composable as parameters. A CompositionLocal
 * is a value that is handed down the whole tree instead, without every composable in between
 * passing it on: a composable reads it with `.current`, and an ancestor sets it for everything
 * inside with `CompositionLocalProvider`. Material's own theme reaches every button this way.
 * Here it lets one place, [ProvideSkeletonPulse], drive the band of every skeleton block below it.
 *
 * The value is a reader (a function returning the number), not the number itself, so a block can
 * call it while it *draws* rather than while it is composed: a moving band then only repaints the
 * blocks, frame by frame, and never rebuilds the screen. "Static" means a new value rebuilds
 * everything below, which is fine because the reader itself never changes.
 *
 * The default is a still frame (always 0, the band out of sight), so a preview, a test that draws
 * a screen's stateless part, or anything outside [ProvideSkeletonPulse] never animates: an
 * animation that never ends would keep a preview or a test waiting forever.
 */
val LocalSkeletonPhase = staticCompositionLocalOf<() -> Float> { { 0f } }

/**
 * A placeholder block drawn where content will appear once it has loaded (a "skeleton" of the
 * screen): a calm area of the theme's `surfaceContainerHigh` cut to [shape], with a soft band of
 * the lighter `surfaceContainerHighest` passing across it while [ProvideSkeletonPulse] is active.
 * Its size comes from [modifier], so it can stand in for a photo tile, a card or a line of text.
 *
 * The band's position is read from [LocalSkeletonPhase] inside `drawBehind`, which runs in the
 * draw phase, the last of Compose's three phases (composition decides what is on screen, layout
 * where, drawing paints it). Reading the moving value there means each new frame only repaints
 * this block; nothing is composed or measured again. The band fades the same colour role in and
 * out (from fully clear to solid and back), so no colour outside the theme is ever painted.
 *
 * A skeleton has no text and no meaning of its own, so it adds nothing for screen readers: the
 * screen that shows skeletons says once, for all of them, that its content is loading.
 */
@Composable
fun SkeletonBlock(shape: Shape, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val band = MaterialTheme.colorScheme.surfaceContainerHighest
    val phase = LocalSkeletonPhase.current
    Spacer(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(color = base)
                // Read here, in the draw phase: a new phase repaints this block and nothing else.
                val bandWidth = size.width * BAND_WIDTH_FRACTION
                val bandStart = -bandWidth + (size.width + bandWidth) * phase()
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to band.copy(alpha = 0f),
                        0.5f to band,
                        1f to band.copy(alpha = 0f),
                        startX = bandStart,
                        endX = bandStart + bandWidth,
                    ),
                )
            },
    )
}

/**
 * Makes the band of every [SkeletonBlock] inside [content] move, over and over, while [pulsing]
 * is true; with `pulsing = false` the blocks keep the still frame of [LocalSkeletonPhase].
 *
 * The motion comes from `rememberInfiniteTransition`, Compose's animation for something that
 * repeats for as long as it is on screen: `animateFloat` runs a number from 0 to 1 in
 * [PULSE_MILLIS] at an even speed (`LinearEasing`), then starts again (`infiniteRepeatable`). It
 * stops by itself when this composable leaves the screen, which is as soon as the content has
 * loaded and the skeletons give way to it.
 *
 * Only a screen's stateful part calls this (My edits wraps its screen in it); previews pass
 * `pulsing = false`, because a preview is drawn with no test clock and an endless animation would
 * keep it from ever finishing. When the phone's "Remove animations" setting is on, Android sets
 * the animator duration scale (read from the global settings) to 0 and the blocks stay still too.
 */
@Composable
fun ProvideSkeletonPulse(pulsing: Boolean = true, content: @Composable () -> Unit) {
    // Asked once: the setting rarely changes while a skeleton is on screen. "Remove animations"
    // sets the animator duration scale to 0; an unset scale means normal speed (1).
    val context = LocalContext.current
    val animatorsOn = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    if (pulsing && animatorsOn) {
        val transition = rememberInfiniteTransition(label = "skeletonPulse")
        val phase = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(durationMillis = PULSE_MILLIS, easing = LinearEasing)),
            label = "skeletonPhase",
        )
        // One reader for the whole life of the pulse: it reads the animated value when called,
        // so the blocks follow the animation without this local ever changing.
        val reader = remember(phase) { { phase.value } }
        CompositionLocalProvider(LocalSkeletonPhase provides reader, content = content)
    } else {
        content()
    }
}

/**
 * Skeleton blocks in light and dark: a rounded tile and a round one at the still frame (what
 * previews, tests and recorded pictures show), then the same pair with the band frozen half-way
 * across, to show what the moving band looks like. Nothing here animates.
 */
@PreviewLightDark
@Composable
private fun SkeletonBlockPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface {
            ProvideSkeletonPulse(pulsing = false) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBlock(shape = MaterialTheme.shapes.large, modifier = Modifier.size(96.dp))
                    SkeletonBlock(shape = CircleShape, modifier = Modifier.size(48.dp))
                    // A fixed phase, provided by hand: the band stands still half-way across.
                    CompositionLocalProvider(LocalSkeletonPhase provides { 0.5f }) {
                        SkeletonBlock(shape = MaterialTheme.shapes.large, modifier = Modifier.size(96.dp))
                        SkeletonBlock(shape = CircleShape, modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}
