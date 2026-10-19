package com.phongkaster.kanso.ui.tool

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import kotlin.math.roundToInt

/** Test tag of the Edit panel's brightness slider. */
const val EDIT_BRIGHTNESS_TAG = "edit-brightness"

/** Test tag of the Edit panel's contrast slider. */
const val EDIT_CONTRAST_TAG = "edit-contrast"

/** Test tag of the Edit panel's saturation slider. */
const val EDIT_SATURATION_TAG = "edit-saturation"

/** Test tag of the Edit panel's warmth slider. */
const val EDIT_WARMTH_TAG = "edit-warmth"

/** Test tag of the Edit panel's filter intensity slider. */
const val EDIT_INTENSITY_TAG = "edit-filter-intensity"

/** Test tag of the Edit panel's "Turn left" button (a quarter turn anticlockwise). */
const val EDIT_ROTATE_LEFT_TAG = "edit-rotate-left"

/** Test tag of the Edit panel's "Turn right" button (a quarter turn clockwise). */
const val EDIT_ROTATE_RIGHT_TAG = "edit-rotate-right"

/** Test tag of the Edit panel's button that mirrors the photo left to right. */
const val EDIT_FLIP_HORIZONTAL_TAG = "edit-flip-horizontal"

/** Test tag of the Edit panel's button that mirrors the photo top to bottom. */
const val EDIT_FLIP_VERTICAL_TAG = "edit-flip-vertical"

/**
 * The ending of the test tag of the number shown beside a slider's name: the brightness slider's
 * number is tagged `edit-brightness-value` (see [editSliderValueTag]).
 */
const val EDIT_SLIDER_VALUE = "-value"

/** The width of a slider's name, so the sliders line up one under another. */
private val LABEL_WIDTH = 96.dp

/** The width of the number beside a slider's name, room for "-100". */
private val VALUE_WIDTH = 44.dp

/** A slider value of 1 is shown as this number: the -1..1 settings are shown as -100..100. */
private const val SHOWN_SCALE = 100f

/** Returns the test tag of [ratio]'s crop chip, such as `edit-ratio-SQUARE`. */
fun editRatioTag(ratio: CropRatio): String = "edit-ratio-${ratio.name}"

/** Returns the test tag of [preset]'s filter chip, such as `edit-filter-KELVIN`. */
fun editFilterTag(preset: FilterPreset): String = "edit-filter-${preset.name}"

/**
 * Returns the test tag of the number beside the slider tagged [sliderTag], such as
 * `edit-brightness-value`.
 */
fun editSliderValueTag(sliderTag: String): String = sliderTag + EDIT_SLIDER_VALUE

/**
 * The Edit tool's controls for one sheet of the Edit screen: the group of [openTool] only, which
 * is crop shapes (Crop), quarter turns and flips (Rotate), four colour sliders (Adjust), or
 * filters with an intensity (Filters).
 *
 * **No state of its own.** Everything drawn here comes from [settings], the editing session's
 * current settings, which the screen's ViewModel keeps. So when the sheet is drawn afresh (the
 * sheet area's `AnimatedContent` builds a new one each time a tool opens) nothing is lost:
 * brightness 70 is still 70 when Adjust is opened again, and a slider stays where it was let go.
 *
 * **Whole settings, never a change.** Every touch hands over a complete new [EditSettings] (made
 * from [settings] with `copy`), which the ViewModel renders against the original photo:
 *
 * - [onChange] is one action, one Undo step: a crop chip, a turn, a flip, a filter chip;
 * - [onPreview] is a slider being dragged: the photo follows the finger, but no Undo step is
 *   made yet, and [onCommit] is the finger let go, which makes the whole drag one Undo step;
 * - [onRatio] is a crop shape chosen; the ViewModel measures the original photo itself to fit
 *   the shape's rectangle in it.
 *
 * Each callback that changes the picture also carries a `processorFor` function, which builds the
 * [EditProcessor] that draws a set of settings. This composable builds it, because it owns the
 * filters' look-up images: [filterSource] finds them; when null, as in the app, they are read from
 * the app's assets (the engine's bundled images, see [FilterPreset]); a test passes its own. So
 * the screen simply forwards each callback to the ViewModel.
 *
 * While [enabled] is false (the Compare switch is on, or Done is rendering the final picture)
 * every control is disabled. An ordinary preview render does not disable them: a slider switched
 * off mid-drag would flicker and lose the finger.
 *
 * The drawing itself is the stateless [EditPanelContent].
 */
@Composable
fun EditPanel(
    openTool: EditTool,
    settings: EditSettings,
    onChange: (EditSettings, (EditSettings) -> ToolProcessor) -> Unit,
    onPreview: (EditSettings, (EditSettings) -> ToolProcessor) -> Unit,
    onCommit: () -> Unit,
    onRatio: (CropRatio, (EditSettings) -> ToolProcessor) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filterSource: FilterLut.Source? = null,
) {
    // The real look-up source reads the assets through the application's context, made once.
    val context = LocalContext.current
    val assetFilters = remember(context) {
        val appContext = context.applicationContext
        FilterLut.fromAssets { path -> appContext.assets.open(path) }
    }
    val filters = filterSource ?: assetFilters
    // One function, remembered per source, that turns any settings into the processor drawing them.
    val processorFor: (EditSettings) -> ToolProcessor = remember(filters) {
        { wanted: EditSettings -> EditProcessor(wanted, filters) }
    }

    EditPanelContent(
        openTool = openTool,
        settings = settings,
        onChange = { changed -> onChange(changed, processorFor) },
        onPreview = { changed -> onPreview(changed, processorFor) },
        onCommit = onCommit,
        onRatio = { ratio -> onRatio(ratio, processorFor) },
        enabled = enabled,
        modifier = modifier,
    )
}

/**
 * The stateless part of the Edit panel (see [EditPanel] for what each callback means): draws
 * [settings] and reports every touch with the new settings, changing nothing itself.
 *
 * It draws only the group of [openTool]:
 *
 * - Crop: one chip per crop shape, the chosen one ([EditSettings.cropRatio]) marked, and a line
 *   saying how to crop; a fixed shape locks the crop to it, Free unlocks it. The rectangle itself
 *   is dragged on the photo, where the screen shows the cropper ([EditCropper]) instead;
 * - Rotate: turn left, turn right, flip across and flip upside down, each one action;
 * - Adjust: brightness, contrast, saturation and warmth, each from -100 through 0 (no change, the
 *   centre) to +100, with its number beside its name;
 * - Filters: None and the seven presets, one at a time (the chosen one is marked), and the
 *   intensity from 0 to 100, which is only usable while a filter is chosen.
 *
 * A slider calls [onPreview] while it moves and [onCommit] when let go; a chip or button calls
 * [onChange]; a crop chip calls [onRatio]. Its height is given by the sheet around it; when the
 * group is taller than that, it scrolls. Chips, buttons and sliders keep Material's 48 dp minimum
 * touch target, and each is disabled while [enabled] is false.
 */
@Composable
fun EditPanelContent(
    openTool: EditTool,
    settings: EditSettings,
    onChange: (EditSettings) -> Unit,
    onPreview: (EditSettings) -> Unit,
    onCommit: () -> Unit,
    onRatio: (CropRatio) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .testTag(EDIT_PANEL_TAG)
            .fillMaxWidth()
            // The sheet around the panel sets its height; the scroll reaches whatever does not fit.
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (openTool) {
            EditTool.CROP -> {
                CropChips(chosen = settings.cropRatio, enabled = enabled, onRatio = onRatio)
                // The rectangle itself is moved on the photo (the cropper); this line says how.
                Text(
                    text = stringResource(R.string.edit_crop_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            EditTool.ROTATE -> TurnButtons(
                enabled = enabled,
                onTurn = { turns -> onChange(settings.copy(quarterTurns = (settings.quarterTurns + turns).mod(4))) },
                onFlip = { horizontal ->
                    onChange(
                        if (horizontal) {
                            settings.copy(flipHorizontal = !settings.flipHorizontal)
                        } else {
                            settings.copy(flipVertical = !settings.flipVertical)
                        },
                    )
                },
            )
            EditTool.ADJUST -> {
                LabelledSlider(
                    label = R.string.edit_tool_brightness,
                    value = settings.brightness,
                    onValueChange = { onPreview(settings.copy(brightness = it)) },
                    onValueChangeFinished = onCommit,
                    tag = EDIT_BRIGHTNESS_TAG,
                    enabled = enabled,
                )
                LabelledSlider(
                    label = R.string.edit_tool_contrast,
                    value = settings.contrast,
                    onValueChange = { onPreview(settings.copy(contrast = it)) },
                    onValueChangeFinished = onCommit,
                    tag = EDIT_CONTRAST_TAG,
                    enabled = enabled,
                )
                LabelledSlider(
                    label = R.string.edit_tool_saturation,
                    value = settings.saturation,
                    onValueChange = { onPreview(settings.copy(saturation = it)) },
                    onValueChangeFinished = onCommit,
                    tag = EDIT_SATURATION_TAG,
                    enabled = enabled,
                )
                LabelledSlider(
                    label = R.string.edit_tool_warmth,
                    value = settings.warmth,
                    onValueChange = { onPreview(settings.copy(warmth = it)) },
                    onValueChangeFinished = onCommit,
                    tag = EDIT_WARMTH_TAG,
                    enabled = enabled,
                )
            }
            EditTool.FILTERS -> {
                FilterChips(
                    chosen = settings.filter,
                    enabled = enabled,
                    onFilter = { preset -> onChange(withFilter(settings, preset)) },
                )
                LabelledSlider(
                    label = R.string.edit_tool_filter_intensity,
                    value = settings.filterIntensity,
                    onValueChange = { onPreview(settings.copy(filterIntensity = it)) },
                    onValueChangeFinished = onCommit,
                    tag = EDIT_INTENSITY_TAG,
                    // With no filter chosen, an intensity would change nothing the person can see.
                    enabled = enabled && settings.filter != FilterPreset.NONE,
                    valueRange = 0f..1f,
                )
            }
        }
    }
}

/**
 * The settings after choosing filter [preset] on top of [settings]: the filter REPLACES any earlier
 * one (filters never pile up). Coming from None, the intensity starts at full, so the filter shows;
 * moving from one filter to another keeps the intensity the person set. None puts the intensity
 * back to its default too, so "no filter" is exactly the starting state and asks nothing on Back.
 */
private fun withFilter(settings: EditSettings, preset: FilterPreset): EditSettings = when {
    preset == FilterPreset.NONE -> settings.copy(filter = FilterPreset.NONE, filterIntensity = 1f)
    settings.filter == FilterPreset.NONE -> settings.copy(filter = preset, filterIntensity = 1f)
    else -> settings.copy(filter = preset)
}

/**
 * One row of crop chips, scrolling sideways if the screen is narrow. Each is a filter chip marked
 * selected while it is the [chosen] shape: a fixed shape crops at once to the largest centred
 * rectangle of that shape and locks the cropper's rectangle to it; Free unlocks it, so its edges
 * move freely. A tap calls [onRatio] with the chip's shape; while [enabled] is false every chip is
 * disabled.
 */
@Composable
private fun CropChips(chosen: CropRatio?, enabled: Boolean, onRatio: (CropRatio) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CropRatio.entries.forEach { ratio ->
            FilterChip(
                selected = ratio == chosen,
                onClick = { onRatio(ratio) },
                label = { Text(text = stringResource(ratioLabel(ratio)), maxLines = 1) },
                enabled = enabled,
                modifier = Modifier.testTag(editRatioTag(ratio)),
            )
        }
    }
}

/**
 * The turn and flip buttons in one row, scrolling sideways if the screen is narrow: "Turn left"
 * and "Turn right" call [onTurn] with -1 and +1 quarter turns, "Flip across" and "Flip upside
 * down" call [onFlip] (true for left to right, false for top to bottom; a second tap flips back).
 * While [enabled] is false all four are disabled.
 */
@Composable
private fun TurnButtons(enabled: Boolean, onTurn: (Int) -> Unit, onFlip: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = { onTurn(-1) },
            enabled = enabled,
            modifier = Modifier.testTag(EDIT_ROTATE_LEFT_TAG),
        ) {
            Text(text = stringResource(R.string.edit_tool_rotate_left), maxLines = 1)
        }
        OutlinedButton(
            onClick = { onTurn(1) },
            enabled = enabled,
            modifier = Modifier.testTag(EDIT_ROTATE_RIGHT_TAG),
        ) {
            Text(text = stringResource(R.string.edit_tool_rotate_right), maxLines = 1)
        }
        OutlinedButton(
            onClick = { onFlip(true) },
            enabled = enabled,
            modifier = Modifier.testTag(EDIT_FLIP_HORIZONTAL_TAG),
        ) {
            Text(text = stringResource(R.string.edit_tool_flip_horizontal), maxLines = 1)
        }
        OutlinedButton(
            onClick = { onFlip(false) },
            enabled = enabled,
            modifier = Modifier.testTag(EDIT_FLIP_VERTICAL_TAG),
        ) {
            Text(text = stringResource(R.string.edit_tool_flip_vertical), maxLines = 1)
        }
    }
}

/**
 * One row of filter chips, scrolling sideways: None and then one chip per preset of
 * [FilterPreset], the [chosen] one marked selected, so only one is ever on. A tap calls [onFilter]
 * with the chip's filter; while [enabled] is false every chip is disabled.
 */
@Composable
private fun FilterChips(chosen: FilterPreset, enabled: Boolean, onFilter: (FilterPreset) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // NONE is the enum's first entry, so None leads the row.
        FilterPreset.entries.forEach { preset ->
            FilterChip(
                selected = preset == chosen,
                onClick = { onFilter(preset) },
                label = { Text(text = stringResource(filterLabel(preset)), maxLines = 1) },
                enabled = enabled,
                modifier = Modifier.testTag(editFilterTag(preset)),
            )
        }
    }
}

/**
 * A slider with its [label] and its number on the left, at least 48 dp tall: shows [value] within
 * [valueRange] (by default the colour sliders' -1..1, whose centre means no change), calls
 * [onValueChange] while it moves and [onValueChangeFinished] when the person lets go. The slider
 * only ever shows [value], which the caller takes from the session's settings, so it stays where
 * it was let go instead of springing back.
 *
 * The number is [value] times 100, rounded: -100..100 for a colour slider, 0..100 for the
 * intensity; it carries the test tag [editSliderValueTag] of [tag]. While [enabled] is false the
 * slider is disabled. The slider also carries the label as its description, so a screen reader
 * names it.
 */
@Composable
private fun LabelledSlider(
    @StringRes label: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    tag: String,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float> = EditSettings.MIN..EditSettings.MAX,
) {
    val text = stringResource(label)
    val shown = stringResource(R.string.edit_session_slider_value, (value * SHOWN_SCALE).roundToInt())
    LabelledRow(label = label, value = shown, valueTag = editSliderValueTag(tag)) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
                .semantics { contentDescription = text },
        )
    }
}

/**
 * A row at least 48 dp tall with [label] in a fixed-width column on the left, then, when given,
 * [value] (a slider's number, tagged [valueTag]) right beside it, and [control] filling the rest,
 * so labelled controls line up one under another.
 */
@Composable
private fun LabelledRow(
    @StringRes label: Int,
    value: String? = null,
    valueTag: String? = null,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            modifier = Modifier.width(LABEL_WIDTH),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier
                    .width(VALUE_WIDTH)
                    .padding(end = 8.dp)
                    .then(if (valueTag != null) Modifier.testTag(valueTag) else Modifier),
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            control()
        }
    }
}

/** The string resource naming crop shape [ratio] on its chip. */
@StringRes
private fun ratioLabel(ratio: CropRatio): Int = when (ratio) {
    CropRatio.FREE -> R.string.edit_tool_ratio_free
    CropRatio.SQUARE -> R.string.edit_tool_ratio_square
    CropRatio.FOUR_THREE -> R.string.edit_tool_ratio_4_3
    CropRatio.THREE_FOUR -> R.string.edit_tool_ratio_3_4
    CropRatio.SIXTEEN_NINE -> R.string.edit_tool_ratio_16_9
    CropRatio.NINE_SIXTEEN -> R.string.edit_tool_ratio_9_16
}

/** The string resource naming filter [preset] on its chip. */
@StringRes
private fun filterLabel(preset: FilterPreset): Int = when (preset) {
    FilterPreset.NONE -> R.string.edit_tool_filter_none
    FilterPreset.LIGHT -> R.string.edit_tool_filter_light
    FilterPreset.SEVENTY_SEVEN -> R.string.edit_tool_filter_1977
    FilterPreset.KELVIN -> R.string.edit_tool_filter_kelvin
    FilterPreset.NASHVILLE -> R.string.edit_tool_filter_nashville
    FilterPreset.WALDEN -> R.string.edit_tool_filter_walden
    FilterPreset.HUDSON -> R.string.edit_tool_filter_hudson
    FilterPreset.AMARO -> R.string.edit_tool_filter_amaro
}

/**
 * One sample sheet for the previews: the panel showing [openTool]'s group for the sample
 * [settings], enabled or not, on the sheet's colour. Nothing is read from assets and every
 * callback does nothing. The outer Box fills the preview first, because KansoTheme would
 * otherwise stretch its single child to the whole screen.
 */
@Composable
private fun SampleEditPanel(openTool: EditTool, settings: EditSettings, enabled: Boolean = true) {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                EditPanelContent(
                    openTool = openTool,
                    settings = settings,
                    onChange = {},
                    onPreview = {},
                    onCommit = {},
                    onRatio = {},
                    enabled = enabled,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/**
 * The Adjust sheet as it looks mid-edit, in light and in dark: brightness at 30 and warmth at -20
 * (each number beside its name), contrast and saturation at the centre.
 */
@PreviewLightDark
@Composable
private fun EditPanelPreview() {
    SampleEditPanel(openTool = EditTool.ADJUST, settings = EditSettings(brightness = 0.3f, warmth = -0.2f))
}

/**
 * The Crop sheet with Free chosen, in light and in dark: the shape chips with Free marked, and the
 * line saying how to move the rectangle on the photo.
 */
@PreviewLightDark
@Composable
private fun EditPanelFreeCropPreview() {
    SampleEditPanel(
        openTool = EditTool.CROP,
        settings = EditSettings(
            cropRatio = CropRatio.FREE,
            crop = EditSettings.CropRect(0.1f, 0.2f, 0.9f, 0.8f),
        ),
    )
}

/** The Rotate sheet, in light and in dark: the two turn buttons and the two flips. */
@PreviewLightDark
@Composable
private fun EditPanelRotatePreview() {
    SampleEditPanel(openTool = EditTool.ROTATE, settings = EditSettings())
}

/**
 * The Filters sheet, in light and in dark: Kelvin chosen (its chip marked, None not) at an
 * intensity of 70.
 */
@PreviewLightDark
@Composable
private fun EditPanelFiltersPreview() {
    SampleEditPanel(
        openTool = EditTool.FILTERS,
        settings = EditSettings(filter = FilterPreset.KELVIN, filterIntensity = 0.7f),
    )
}

/**
 * The Crop sheet while Compare is on, in light and in dark: every chip is disabled and faint.
 */
@PreviewLightDark
@Composable
private fun EditPanelDisabledPreview() {
    SampleEditPanel(openTool = EditTool.CROP, settings = EditSettings(), enabled = false)
}
