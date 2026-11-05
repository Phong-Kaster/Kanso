package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.hslpanel

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.ColourRange
import com.phongkaster.kanso.data.edit.edit.HslChannel
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.component.SliderWithValue
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt

/** A stored value of 1 is shown as 100: the -1..1 HSL settings read -100..100 on the slider. */
private const val SHOWN_SCALE = 100f

/** How far, in degrees, the Hue slider's gradient reaches to each side of the range's centre. */
private const val HUE_REACH_DEGREES = 20f

/** The saturation used for every colour the HSL controls draw, so they look alike. */
private const val DISPLAY_SATURATION = 0.75f

/** The lightness used for the colour the HSL controls draw. */
private const val DISPLAY_LIGHTNESS = 0.5f

/** The test tag of the HSL slider, and (with `-value`) of its number. */
const val HSL_SLIDER_TAG = "hsl-slider"

/** The test tag of the slider's 32 dp row, the room the slider takes in the sheet's layout. */
const val HSL_SLIDER_ROW_TAG = "hsl-slider-row"

/** The height of the slider's row in the sheet's layout. */
private val ROW_HEIGHT = 32.dp

/** The height of the slider's touch area: Android's recommended 48 dp touch target. */
private val TOUCH_HEIGHT = 48.dp

/**
 * Returns the colour of the hue [degrees] on the wheel, at the saturation and lightness every HSL
 * control shares. The degrees are wrapped into 0..360 first, since a gradient reaches below 0 or
 * above 360 for the ranges at the ends of the wheel.
 */
private fun hueColour(degrees: Float, saturation: Float = DISPLAY_SATURATION, lightness: Float = DISPLAY_LIGHTNESS): Color {
    val wrapped = ((degrees % 360f) + 360f) % 360f
    return Color.hsl(hue = wrapped, saturation = saturation, lightness = lightness)
}

/**
 * The colour that stands for this range on screen: its centre hue at the shared saturation and
 * lightness. The HSL slider's track and the range circles both use it, so a range looks the same
 * everywhere.
 */
internal fun ColourRange.displayColour(): Color = hueColour(centreDegrees)

/** The string naming this range, such as "Light green". */
@StringRes
internal fun ColourRange.nameRes(): Int = when (this) {
    ColourRange.RED -> R.string.edit_hsl_range_red
    ColourRange.ORANGE -> R.string.edit_hsl_range_orange
    ColourRange.LIGHT_GREEN -> R.string.edit_hsl_range_light_green
    ColourRange.GREEN -> R.string.edit_hsl_range_green
    ColourRange.LIGHT_BLUE -> R.string.edit_hsl_range_light_blue
    ColourRange.BLUE -> R.string.edit_hsl_range_blue
    ColourRange.NAVY_BLUE -> R.string.edit_hsl_range_navy_blue
    ColourRange.PURPLE -> R.string.edit_hsl_range_purple
    ColourRange.PINK -> R.string.edit_hsl_range_pink
}

/** The string naming this channel: "Hue", "Saturation" or "Lightness". */
@StringRes
internal fun HslChannel.labelRes(): Int = when (this) {
    HslChannel.HUE -> R.string.edit_hsl_channel_hue
    HslChannel.SATURATION -> R.string.edit_hsl_channel_saturation
    HslChannel.LIGHTNESS -> R.string.edit_hsl_channel_lightness
}

/**
 * The gradient the slider's track shows for [channel] of [range], so the person sees what dragging
 * does: Hue runs from 20 degrees before the range's centre to 20 degrees after it, Saturation from
 * grey to the range's full colour, Lightness from a dark to a light version of it.
 */
private fun trackBrushFor(range: ColourRange, channel: HslChannel): Brush {
    val centre = range.centreDegrees
    val colours = when (channel) {
        HslChannel.HUE -> listOf(hueColour(centre - HUE_REACH_DEGREES), hueColour(centre + HUE_REACH_DEGREES))
        HslChannel.SATURATION -> listOf(hueColour(centre, saturation = 0f), range.displayColour())
        HslChannel.LIGHTNESS -> listOf(hueColour(centre, lightness = 0.15f), hueColour(centre, lightness = 0.85f))
    }
    return Brush.horizontalGradient(colours)
}

/**
 * The one slider of the HSL sheet: it changes [channel] of [range]. [value] is the stored setting,
 * -1..1 where 0 means no change; the slider shows it as -100..100 with the number at its right end.
 * The track is a colour gradient made from the range's colour (see [trackBrushFor]). [onValueChange]
 * is called while the thumb moves, so the photo follows the finger, and [onValueChangeFinished]
 * when the person lets go.
 *
 * The row is 32 dp tall in the layout, because the sheet has room for three 32 dp rows only, but
 * the slider asks for exactly 48 dp of height with `requiredHeight`, which ignores the 32 dp the
 * row allows: Compose centres the taller slider on the row, so its touch area is 48 dp tall (and
 * as wide as the row less the number) and overflows the row by 8 dp above and below without
 * moving anything else. The track and thumb stay drawn in the middle. A screen reader hears the
 * channel and the range, for example "Hue, Light green". While [enabled] is false the slider
 * cannot be moved.
 */
@Composable
internal fun HslSlider(
    range: ColourRange,
    channel: HslChannel,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val description = stringResource(channel.labelRes()) + ", " + stringResource(range.nameRes())
    val shown = stringResource(R.string.edit_session_slider_value, (value * SHOWN_SCALE).roundToInt())
    Box(modifier = modifier.fillMaxWidth().height(ROW_HEIGHT).testTag(HSL_SLIDER_ROW_TAG)) {
        SliderWithValue(
            value = value,
            onValueChange = onValueChange,
            valueText = shown,
            valueTag = "$HSL_SLIDER_TAG-value",
            valueRange = -1f..1f,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            color = range.displayColour(),
            trackBrush = trackBrushFor(range, channel),
            // The slider itself is 48 dp tall (Material's Slider is as tall as its track slot).
            touchHeight = TOUCH_HEIGHT,
            sliderModifier = Modifier
                // 48 dp although the row allows only 32 dp: the slider overflows the row, centred on it.
                .requiredHeight(TOUCH_HEIGHT)
                .testTag(HSL_SLIDER_TAG)
                .semantics { contentDescription = description },
        )
    }
}

/** The slider for each channel of Light green, in light and in dark. */
@PreviewLightDark
@Composable
private fun HslSliderPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp)) {
                HslSlider(ColourRange.LIGHT_GREEN, HslChannel.HUE, 0.3f, {}, {})
                HslSlider(ColourRange.LIGHT_GREEN, HslChannel.SATURATION, 0f, {}, {})
                HslSlider(ColourRange.RED, HslChannel.LIGHTNESS, -0.5f, {}, {})
            }
        }
    }
}
