package com.phongkaster.kanso.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** The diameter of [ClassicSlider]'s round thumb. */
private val CLASSIC_THUMB_SIZE = 20.dp

/** How faint a disabled slider is drawn: Material's usual opacity for disabled content. */
private const val DISABLED_ALPHA = 0.38f

/**
 * Kanso's slider, in the classic Material 2 look: a thin rounded track and a round thumb that sits
 * on it, with a small dot at every step when [steps] is above 0 (none when it is 0, the continuous
 * slider). Material 3's own slider draws a tall pill thumb, so this one keeps the behaviour
 * (dragging, keyboard and screen reader support) of `Slider` but replaces how it looks, through
 * its `thumb` and `track` slots. Every slider in the app uses it, so they all look alike.
 *
 * It shows [value] within [valueRange], calls [onValueChange] while the thumb moves and
 * [onValueChangeFinished] when the person lets go. While [enabled] is false it is drawn faint and
 * ignores touches.
 *
 * Each slot is a composable `Slider` calls to draw that part. The track slot receives the
 * `SliderState`, which holds the current value, the range and the step count, so the track can
 * colour the part before the thumb. The thumb's centre travels between half a thumb in from each
 * end of the slider, so the track's coloured end is placed at the same spot.
 *
 * The coloured part and the thumb use the theme's `primary`; the rest of the track and the dots
 * on it use `primary` at 24% opacity, as Material 2 did, and the dots on the coloured part use
 * `onPrimary`, so they stay visible.
 */
@Composable
fun ClassicSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val inactive = primary.copy(alpha = 0.24f)
    // The slots exist only on the Slider that takes a SliderState (the holder of value, range and
    // steps), so the value is mirrored into one: the state follows [value] when the caller changes
    // it, and Slider reports every drag back through [onValueChange].
    val sliderState = remember(valueRange, steps) {
        SliderState(value = value, steps = steps, trackRange = valueRange)
    }
    LaunchedEffect(value) { if (sliderState.value != value) sliderState.value = value }
    Slider(
        state = sliderState,
        // This Slider does not move the thumb itself: the caller sets the state's value.
        onValueChange = {
            sliderState.value = it
            onValueChange(it)
        },
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        modifier = modifier.alpha(if (enabled) 1f else DISABLED_ALPHA),
        thumb = {
            Box(
                Modifier
                    .size(CLASSIC_THUMB_SIZE)
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .background(primary, CircleShape),
            )
        },
        track = { state ->
            Canvas(Modifier.fillMaxWidth().height(CLASSIC_THUMB_SIZE)) {
                val trackHeight = 4.dp.toPx()
                val dotRadius = 1.dp.toPx()
                val inset = CLASSIC_THUMB_SIZE.toPx() / 2f
                val centreY = size.height / 2f
                val thumbX = inset + state.coercedValueAsFraction * (size.width - 2 * inset)
                // The whole track, then the part up to the thumb over it.
                drawLine(inactive, Offset(inset, centreY), Offset(size.width - inset, centreY), trackHeight, StrokeCap.Round)
                drawLine(primary, Offset(inset, centreY), Offset(thumbX, centreY), trackHeight, StrokeCap.Round)
                // One dot per stop, the two ends included; a continuous slider (no steps) has none.
                if (state.steps > 0) {
                    for (i in 0..state.steps + 1) {
                        val x = inset + i / (state.steps + 1f) * (size.width - 2 * inset)
                        drawCircle(if (x <= thumbX) onPrimary else primary, dotRadius, Offset(x, centreY))
                    }
                }
            }
        },
    )
}

/** The slider in light and in dark: a continuous one at 40% and a stepped one at 50%. */
@PreviewLightDark
@Composable
private fun ClassicSliderPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(Modifier.padding(16.dp)) {
                androidx.compose.foundation.layout.Column {
                    ClassicSlider(value = 0.4f, onValueChange = {})
                    ClassicSlider(value = 0.5f, onValueChange = {}, steps = 4)
                }
            }
        }
    }
}
