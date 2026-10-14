package com.phongkaster.kanso.ui.tool

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

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

/** Test tag of the free crop's width range (its left and right edges). */
const val EDIT_FREE_WIDTH_TAG = "edit-free-width"

/** Test tag of the free crop's height range (its top and bottom edges). */
const val EDIT_FREE_HEIGHT_TAG = "edit-free-height"

/** Test tag of the free crop's "Crop" button, which applies the rectangle. */
const val EDIT_FREE_APPLY_TAG = "edit-free-apply"

/** The tallest the Edit panel grows; beyond it, its controls scroll, so the photo stays large. */
private val PANEL_MAX_HEIGHT = 260.dp

/** The width of a slider's label, so the four sliders line up. */
private val LABEL_WIDTH = 96.dp

/** The narrowest a free crop may get, as a fraction of the photo's width or height. */
private const val MIN_FREE_SPAN = 0.05f

/** Returns the test tag of [ratio]'s crop chip, such as `edit-ratio-SQUARE`. */
fun editRatioTag(ratio: CropRatio): String = "edit-ratio-${ratio.name}"

/** Returns the test tag of [preset]'s filter chip, such as `edit-filter-KELVIN`. */
fun editFilterTag(preset: FilterPreset): String = "edit-filter-${preset.name}"

/**
 * The Edit tool's controls, for the options strip of the Edit screen: crop shapes, quarter turns
 * and flips, four colour sliders, and filters with an intensity. Each change is handed to
 * [onApply] as an [EditProcessor], which the screen's ViewModel runs on the photo shown.
 *
 * **Why each change is applied on its own.** The ViewModel runs every processor on the photo as
 * it is shown now, which already holds every earlier edit, and keeps each result as one step Undo
 * can take back. So this panel never hands over its whole state, which would apply the earlier
 * changes a second time, but only the one change just made:
 *
 * - a crop shape, a turn or a flip is applied the moment it is tapped (a turn is a change by
 *   nature: two taps make two quarter turns);
 * - a colour slider is applied when the person lets go of it, and then springs back to the
 *   middle, ready for the next change: so dragging brightness up and letting go twice brightens
 *   twice, and each is one Undo step. A live, while-dragging picture is not possible through the
 *   ViewModel, which runs one edit at a time and drops a request made while one runs;
 * - a filter is applied when its chip is tapped, at the intensity the slider under the chips
 *   shows at that moment;
 * - Free shows two range sliders for the rectangle to keep; "Crop" applies it.
 *
 * [filterSource] finds each filter's look-up image; when null, as in the app, it is read from the
 * app's assets (the engine's bundled images, see [FilterPreset]); a test passes its own. This
 * function keeps the panel's state (what the controls show) with `remember`; the drawing itself
 * is [EditPanelContent].
 */
@Composable
fun EditPanel(
    onApply: (ToolProcessor) -> Unit,
    modifier: Modifier = Modifier,
    filterSource: FilterLut.Source? = null,
) {
    // The real look-up source reads the assets through the application's context, made once.
    val context = LocalContext.current
    val assetFilters = remember(context) {
        val appContext = context.applicationContext
        FilterLut.fromAssets { path -> appContext.assets.open(path) }
    }
    val filters = filterSource ?: assetFilters
    // What the controls show: the free crop rectangle, slider positions and filter intensity.
    // Reading `settings` inside a callback reads its value at that moment, never a stale copy.
    var settings by remember { mutableStateOf(EditSettings()) }
    val commit: (EditSettings) -> Unit = { change -> onApply(EditProcessor(change, filters)) }

    EditPanelContent(
        settings = settings,
        onRatio = { ratio ->
            if (ratio == CropRatio.FREE) {
                // Free opens (or closes) the rectangle controls; nothing is cropped yet.
                val open = settings.cropRatio != CropRatio.FREE
                settings = settings.copy(
                    cropRatio = if (open) CropRatio.FREE else null,
                    freeCrop = EditSettings.CropRect.FULL,
                )
            } else {
                settings = settings.copy(cropRatio = null)
                commit(EditSettings(cropRatio = ratio))
            }
        },
        onFreeCropChange = { rect -> settings = settings.copy(freeCrop = rect) },
        onFreeCropApply = {
            val rect = settings.freeCrop
            if (rect != EditSettings.CropRect.FULL) {
                commit(EditSettings(cropRatio = CropRatio.FREE, freeCrop = rect))
            }
            settings = settings.copy(cropRatio = null, freeCrop = EditSettings.CropRect.FULL)
        },
        onTurn = { turns -> commit(EditSettings(quarterTurns = turns)) },
        onFlip = { horizontal ->
            commit(if (horizontal) EditSettings(flipHorizontal = true) else EditSettings(flipVertical = true))
        },
        onSettingsChange = { changed -> settings = changed },
        onAdjustFinished = {
            val current = settings
            if (current.hasColourChange) {
                commit(
                    EditSettings(
                        brightness = current.brightness,
                        contrast = current.contrast,
                        saturation = current.saturation,
                        warmth = current.warmth,
                    ),
                )
            }
            // Back to the middle: the change now lives in the photo, not in the sliders.
            settings = current.copy(
                brightness = EditSettings.MIDDLE,
                contrast = EditSettings.MIDDLE,
                saturation = EditSettings.MIDDLE,
                warmth = EditSettings.MIDDLE,
            )
        },
        onFilter = { preset ->
            if (preset != FilterPreset.NONE) {
                commit(EditSettings(filter = preset, filterIntensity = settings.filterIntensity))
            }
        },
        modifier = modifier,
    )
}

/**
 * The stateless part of the Edit panel (see [EditPanel] for what each control does): draws
 * [settings] and reports every touch through a callback, changing nothing itself.
 *
 * From top to bottom: the crop chips, with the free crop's two range sliders under them while
 * Free is chosen; the turn and flip buttons; the brightness, contrast, saturation and warmth
 * sliders (the middle is "no change"); the filter chips and the intensity slider. Taller than
 * [PANEL_MAX_HEIGHT], it scrolls. Chips, buttons and sliders all keep Material's 48 dp minimum
 * touch target.
 *
 * [onRatio] is a crop chip tapped; [onFreeCropChange] a new free rectangle and [onFreeCropApply]
 * its "Crop" button; [onTurn] a quarter turn (+1 right, -1 left); [onFlip] a flip (true: left to
 * right); [onSettingsChange] a slider moved, with the new settings; [onAdjustFinished] a colour
 * slider let go; [onFilter] a filter chip tapped.
 */
@Composable
fun EditPanelContent(
    settings: EditSettings,
    onRatio: (CropRatio) -> Unit,
    onFreeCropChange: (EditSettings.CropRect) -> Unit,
    onFreeCropApply: () -> Unit,
    onTurn: (Int) -> Unit,
    onFlip: (Boolean) -> Unit,
    onSettingsChange: (EditSettings) -> Unit,
    onAdjustFinished: () -> Unit,
    onFilter: (FilterPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .testTag(EDIT_PANEL_TAG)
            .fillMaxWidth()
            // The height limit comes before the scroll, so the scroll knows how much it shows.
            .heightIn(max = PANEL_MAX_HEIGHT)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CropChips(freeOpen = settings.cropRatio == CropRatio.FREE, onRatio = onRatio)
        // AnimatedVisibility grows the free crop controls in and shrinks them out (Compose's own
        // animation API), instead of making the panel jump.
        AnimatedVisibility(visible = settings.cropRatio == CropRatio.FREE) {
            FreeCropControls(rect = settings.freeCrop, onChange = onFreeCropChange, onApply = onFreeCropApply)
        }
        TurnButtons(onTurn = onTurn, onFlip = onFlip)
        LabelledSlider(
            label = R.string.edit_tool_brightness,
            value = settings.brightness,
            onValueChange = { onSettingsChange(settings.copy(brightness = it)) },
            tag = EDIT_BRIGHTNESS_TAG,
            onValueChangeFinished = onAdjustFinished,
        )
        LabelledSlider(
            label = R.string.edit_tool_contrast,
            value = settings.contrast,
            onValueChange = { onSettingsChange(settings.copy(contrast = it)) },
            tag = EDIT_CONTRAST_TAG,
            onValueChangeFinished = onAdjustFinished,
        )
        LabelledSlider(
            label = R.string.edit_tool_saturation,
            value = settings.saturation,
            onValueChange = { onSettingsChange(settings.copy(saturation = it)) },
            tag = EDIT_SATURATION_TAG,
            onValueChangeFinished = onAdjustFinished,
        )
        LabelledSlider(
            label = R.string.edit_tool_warmth,
            value = settings.warmth,
            onValueChange = { onSettingsChange(settings.copy(warmth = it)) },
            tag = EDIT_WARMTH_TAG,
            onValueChangeFinished = onAdjustFinished,
        )
        FilterChips(onFilter = onFilter)
        LabelledSlider(
            label = R.string.edit_tool_filter_intensity,
            value = settings.filterIntensity,
            onValueChange = { onSettingsChange(settings.copy(filterIntensity = it)) },
            tag = EDIT_INTENSITY_TAG,
            valueRange = 0f..1f,
        )
    }
}

/**
 * One row of crop chips, scrolling sideways if the screen is narrow. Free is a filter chip,
 * marked selected while its rectangle controls are open ([freeOpen]); each fixed shape is an
 * action chip that crops at once. A tap calls [onRatio] with the chip's shape.
 */
@Composable
private fun CropChips(freeOpen: Boolean, onRatio: (CropRatio) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CropRatio.entries.forEach { ratio ->
            val label = stringResource(ratioLabel(ratio))
            if (ratio == CropRatio.FREE) {
                FilterChip(
                    selected = freeOpen,
                    onClick = { onRatio(ratio) },
                    label = { Text(text = label, maxLines = 1) },
                    modifier = Modifier.testTag(editRatioTag(ratio)),
                )
            } else {
                AssistChip(
                    onClick = { onRatio(ratio) },
                    label = { Text(text = label, maxLines = 1) },
                    modifier = Modifier.testTag(editRatioTag(ratio)),
                )
            }
        }
    }
}

/**
 * The free crop's controls: a range slider for the left and right edges, one for the top and
 * bottom edges, each as a fraction of the photo, and a "Crop" button that applies the rectangle
 * ([onApply]). A range slider has two thumbs, one per edge. A move that would make the rectangle
 * narrower than [MIN_FREE_SPAN] is ignored; any other calls [onChange] with the new [rect].
 *
 * The range slider is still marked experimental in some Material 3 versions, hence the opt-in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FreeCropControls(
    rect: EditSettings.CropRect,
    onChange: (EditSettings.CropRect) -> Unit,
    onApply: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LabelledRow(label = R.string.edit_tool_free_width) {
            RangeSlider(
                value = rect.left..rect.right,
                onValueChange = { range ->
                    if (range.endInclusive - range.start >= MIN_FREE_SPAN) {
                        onChange(rect.copy(left = range.start, right = range.endInclusive))
                    }
                },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth().testTag(EDIT_FREE_WIDTH_TAG),
            )
        }
        LabelledRow(label = R.string.edit_tool_free_height) {
            RangeSlider(
                value = rect.top..rect.bottom,
                onValueChange = { range ->
                    if (range.endInclusive - range.start >= MIN_FREE_SPAN) {
                        onChange(rect.copy(top = range.start, bottom = range.endInclusive))
                    }
                },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth().testTag(EDIT_FREE_HEIGHT_TAG),
            )
        }
        FilledTonalButton(
            onClick = onApply,
            modifier = Modifier
                .align(Alignment.End)
                .testTag(EDIT_FREE_APPLY_TAG),
        ) {
            Text(text = stringResource(R.string.edit_tool_free_apply))
        }
    }
}

/**
 * The turn and flip buttons in one row, scrolling sideways if the screen is narrow: "Turn left"
 * and "Turn right" call [onTurn] with -1 and +1 quarter turns, the two flips call [onFlip] (true
 * for left to right, false for top to bottom).
 */
@Composable
private fun TurnButtons(onTurn: (Int) -> Unit, onFlip: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = { onTurn(-1) }, modifier = Modifier.testTag(EDIT_ROTATE_LEFT_TAG)) {
            Text(text = stringResource(R.string.edit_tool_rotate_left), maxLines = 1)
        }
        OutlinedButton(onClick = { onTurn(1) }, modifier = Modifier.testTag(EDIT_ROTATE_RIGHT_TAG)) {
            Text(text = stringResource(R.string.edit_tool_rotate_right), maxLines = 1)
        }
        OutlinedButton(onClick = { onFlip(true) }, modifier = Modifier.testTag(EDIT_FLIP_HORIZONTAL_TAG)) {
            Text(text = stringResource(R.string.edit_tool_flip_horizontal), maxLines = 1)
        }
        OutlinedButton(onClick = { onFlip(false) }, modifier = Modifier.testTag(EDIT_FLIP_VERTICAL_TAG)) {
            Text(text = stringResource(R.string.edit_tool_flip_vertical), maxLines = 1)
        }
    }
}

/**
 * One row of filter chips, scrolling sideways: one action chip per filter of [FilterPreset]
 * except None (taking a filter back is Undo's job). A tap calls [onFilter] with the chip's filter.
 */
@Composable
private fun FilterChips(onFilter: (FilterPreset) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterPreset.entries.filter { it != FilterPreset.NONE }.forEach { preset ->
            AssistChip(
                onClick = { onFilter(preset) },
                label = { Text(text = stringResource(filterLabel(preset)), maxLines = 1) },
                modifier = Modifier.testTag(editFilterTag(preset)),
            )
        }
    }
}

/**
 * A slider with its [label] on the left, at least 48 dp tall: shows [value] within [valueRange]
 * (by default the colour sliders' range, whose middle means no change), calls [onValueChange]
 * while it moves and [onValueChangeFinished] when the person lets go. [tag] is its test tag. The
 * slider also carries the label as its description, so a screen reader names it.
 */
@Composable
private fun LabelledSlider(
    @StringRes label: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
    tag: String,
    valueRange: ClosedFloatingPointRange<Float> = EditSettings.MIN..EditSettings.MAX,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val text = stringResource(label)
    LabelledRow(label = label) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
                .semantics { contentDescription = text },
        )
    }
}

/**
 * A row at least 48 dp tall with [label] in a fixed-width column on the left and [control]
 * filling the rest, so labelled controls line up one under another.
 */
@Composable
private fun LabelledRow(@StringRes label: Int, control: @Composable () -> Unit) {
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
 * The Edit panel as it looks mid-edit, in light and in dark: brightness pushed up a little and the
 * filter intensity at 70%, with the free crop closed. The sample data is a plain [EditSettings];
 * nothing is read from assets and every callback does nothing. The outer Box fills the preview
 * first, because KansoTheme would otherwise stretch its single child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun EditPanelPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                EditPanelContent(
                    settings = EditSettings(brightness = 0.3f, filterIntensity = 0.7f),
                    onRatio = {},
                    onFreeCropChange = {},
                    onFreeCropApply = {},
                    onTurn = {},
                    onFlip = {},
                    onSettingsChange = {},
                    onAdjustFinished = {},
                    onFilter = {},
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/**
 * The Edit panel with Free chosen, in light and in dark: the two range sliders show a sample
 * rectangle that keeps the middle of the photo, with the "Crop" button under them.
 */
@PreviewLightDark
@Composable
private fun EditPanelFreeCropPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                EditPanelContent(
                    settings = EditSettings(
                        cropRatio = CropRatio.FREE,
                        freeCrop = EditSettings.CropRect(0.1f, 0.2f, 0.9f, 0.8f),
                    ),
                    onRatio = {},
                    onFreeCropChange = {},
                    onFreeCropApply = {},
                    onTurn = {},
                    onFlip = {},
                    onSettingsChange = {},
                    onAdjustFinished = {},
                    onFilter = {},
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
