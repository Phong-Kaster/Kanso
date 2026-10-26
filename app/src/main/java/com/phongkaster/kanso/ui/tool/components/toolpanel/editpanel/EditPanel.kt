package com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.ToolProcessor
import com.phongkaster.kanso.data.edit.edit.CatalogSticker
import com.phongkaster.kanso.data.edit.edit.CropRatio
import com.phongkaster.kanso.data.edit.edit.StickerCategory
import com.phongkaster.kanso.ui.tool.components.toolpanel.editpanel.stickerspanel.StickersPanel
import com.phongkaster.kanso.data.edit.edit.DrawableStickerPictures
import com.phongkaster.kanso.data.edit.edit.EditProcessor
import com.phongkaster.kanso.ui.tool.editProcessorFor
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.edit.edit.FilterLut
import com.phongkaster.kanso.data.edit.edit.FilterPreset
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.AdjustSlider
import com.phongkaster.kanso.ui.tool.components.EditCropper
import com.phongkaster.kanso.ui.tool.components.toolpanel.EDIT_PANEL_TAG

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
 * **Whole settings, never a change.** A turn, a flip, a filter chip or a slider move hands over a
 * complete new [EditSettings] (made from [settings] with `copy`), which the ViewModel renders
 * against the original photo:
 *
 * - [onChange] is one action, one Undo step: a turn, a flip, a filter chip;
 * - [onPreview] is a slider being dragged: the photo follows the finger, but no Undo step is
 *   made yet, and [onCommit] (which carries nothing) is the finger let go, which makes the whole
 *   drag one Undo step;
 * - [onRatio] is a crop shape chip chosen, handed over as a [CropRatio], not as settings; the ViewModel measures the original photo itself to fit
 *   the shape's rectangle in it.
 *
 * Each callback that changes the picture also carries a `processorFor` function, which builds the
 * [EditProcessor] that draws a set of settings. It comes from the app's one recipe,
 * [editProcessorFor] (filter look-up images from the assets, see [FilterPreset], and the sticker
 * pictures from the drawables), so every place that draws settings draws stickers too. When
 * [filterSource] is not null (a test) it supplies the filters instead. So
 * the screen simply forwards each callback to the ViewModel.
 *
 * While [enabled] is false (the Compare switch is on, or Done is rendering the final picture)
 * every control is disabled. An ordinary preview render does not disable them: a slider switched
 * off mid-drag would flicker and lose the finger.
 *
 * [adjustSlider] is Adjust's chosen sub-tool (kept in the screen's state) and [onAdjustSlider] a
 * sub-tool chip tapped.
 *
 * Stickers (the fifth tool): [stickerCategory] is the chosen chip, [onStickerCategoryChosen] a chip
 * tapped, [onStickerChosen] a grid tile tapped, [onStickersCancel] and [onStickersConfirm] the
 * sheet's own Cancel and Confirm buttons; they are forwarded to the stateless content untouched,
 * as stickers need no processor built here.
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
    adjustSlider: AdjustSlider = AdjustSlider.BRIGHTNESS,
    onAdjustSlider: (AdjustSlider) -> Unit = {},
    stickerCategory: StickerCategory = StickerCategory.FOOD,
    onStickerCategoryChosen: (StickerCategory) -> Unit = {},
    onStickerChosen: (CatalogSticker) -> Unit = {},
    onStickersCancel: () -> Unit = {},
    onStickersConfirm: () -> Unit = {},
) {
    val context = LocalContext.current
    // One function, remembered, that turns any settings into the processor drawing them. The app's
    // one recipe ([editProcessorFor]) reads filters from assets and sticker pictures from drawables;
    // a test that passes its own [filterSource] keeps the sticker pictures but swaps the filters.
    val processorFor: (EditSettings) -> ToolProcessor = remember(context, filterSource) {
        if (filterSource == null) {
            editProcessorFor(context)
        } else {
            val stickers = DrawableStickerPictures(context.applicationContext)
            ({ wanted: EditSettings -> EditProcessor(wanted, filterSource, stickers) })
        }
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
        adjustSlider = adjustSlider,
        onAdjustSlider = onAdjustSlider,
        stickerCategory = stickerCategory,
        onStickerCategoryChosen = onStickerCategoryChosen,
        onStickerChosen = onStickerChosen,
        onStickersCancel = onStickersCancel,
        onStickersConfirm = onStickersConfirm,
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
 * - Adjust: a row of four sub-tool chips (brightness, contrast, saturation, warmth) and ONE
 *   slider, for the chosen chip ([adjustSlider]), from -100 through 0 (no change, the centre) to
 *   +100, with its number beside its name. A chip tap calls [onAdjustSlider] and changes nothing
 *   in the photo. One slider at a time keeps Adjust as short as the other tools, so the panel
 *   area never has to grow;
 * - Filters: None and the seven presets, one at a time (the chosen one is marked), and the
 *   intensity from 0 to 100, which is only usable while a filter is chosen.
 *
 * A slider calls [onPreview] while it moves and [onCommit] when let go; a chip or button calls
 * [onChange]; a crop chip calls [onRatio]. Its height is given by the sheet around it; when the
 * group is taller than that, it scrolls. Chips, buttons and sliders keep Material's 48 dp minimum
 * touch target, and each is disabled while [enabled] is false.
 *
 * Stickers draws [StickersPanel]: category chips and a grid that scrolls on its own, so this
 * composable does not scroll for that tool.
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
    adjustSlider: AdjustSlider = AdjustSlider.BRIGHTNESS,
    onAdjustSlider: (AdjustSlider) -> Unit = {},
    stickerCategory: StickerCategory = StickerCategory.FOOD,
    onStickerCategoryChosen: (StickerCategory) -> Unit = {},
    onStickerChosen: (CatalogSticker) -> Unit = {},
    onStickersCancel: () -> Unit = {},
    onStickersConfirm: () -> Unit = {},
) {
    // The other tools scroll when too tall for the sheet. Stickers does not: its grid is a lazy
    // list that scrolls itself, and a lazy list inside a scrolling column has no height to fill.
    val scrolling = if (openTool == EditTool.STICKERS) Modifier else Modifier.verticalScroll(rememberScrollState())
    Column(
        modifier = modifier
            .testTag(EDIT_PANEL_TAG)
            .fillMaxWidth()
            // The sheet around the panel sets its height; the scroll reaches whatever does not fit.
            .then(scrolling),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (openTool) {
            EditTool.STICKERS -> StickersPanel(
                category = stickerCategory,
                enabled = enabled,
                onCategoryChosen = onStickerCategoryChosen,
                onStickerChosen = onStickerChosen,
                onCancel = onStickersCancel,
                onConfirm = onStickersConfirm,
            )
            EditTool.CROP -> {
                CropChips(chosen = settings.cropRatio, cropIsFull = settings.crop == EditSettings.CropRect.FULL, enabled = enabled, onRatio = onRatio)
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
                AdjustChips(chosen = adjustSlider, enabled = enabled, onChoose = onAdjustSlider)
                // One slider, for the chosen sub-tool only, so Adjust is no taller than the others.
                LabelledSlider(
                    label = adjustSlider.labelRes,
                    value = adjustValue(settings, adjustSlider),
                    onValueChange = { onPreview(withAdjust(settings, adjustSlider, it)) },
                    onValueChangeFinished = onCommit,
                    tag = adjustSlider.sliderTag,
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

/** The value of [slider]'s setting in [settings], from -1 to 1 (0 is no change). */
private fun adjustValue(settings: EditSettings, slider: AdjustSlider): Float = when (slider) {
    AdjustSlider.BRIGHTNESS -> settings.brightness
    AdjustSlider.CONTRAST -> settings.contrast
    AdjustSlider.SATURATION -> settings.saturation
    AdjustSlider.WARMTH -> settings.warmth
}

/** [settings] with [slider]'s setting moved to [value], every other setting left as it was. */
private fun withAdjust(settings: EditSettings, slider: AdjustSlider, value: Float): EditSettings = when (slider) {
    AdjustSlider.BRIGHTNESS -> settings.copy(brightness = value)
    AdjustSlider.CONTRAST -> settings.copy(contrast = value)
    AdjustSlider.SATURATION -> settings.copy(saturation = value)
    AdjustSlider.WARMTH -> settings.copy(warmth = value)
}

/**
 * One sample sheet for the previews: the panel showing [openTool]'s group for the sample
 * [settings], enabled or not, on the sheet's colour. Nothing is read from assets and every
 * callback does nothing. The outer Box fills the preview first, because KansoTheme would
 * otherwise stretch its single child to the whole screen.
 */
@Composable
private fun SampleEditPanel(
    openTool: EditTool,
    settings: EditSettings,
    enabled: Boolean = true,
    adjustSlider: AdjustSlider = AdjustSlider.BRIGHTNESS,
) {
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
                    adjustSlider = adjustSlider,
                )
            }
        }
    }
}

/**
 * The Adjust sheet as it looks mid-edit, in light and in dark: the Brightness chip chosen, and
 * its one slider at 30 with the number beside its name.
 */
@PreviewLightDark
@Composable
private fun EditPanelPreview() {
    SampleEditPanel(openTool = EditTool.ADJUST, settings = EditSettings(brightness = 0.3f, warmth = -0.2f))
}

/**
 * The Adjust sheet with the Warmth chip chosen, in light and in dark: the one slider now shows
 * warmth, at -20.
 */
@PreviewLightDark
@Composable
private fun EditPanelWarmthPreview() {
    SampleEditPanel(
        openTool = EditTool.ADJUST,
        settings = EditSettings(brightness = 0.3f, warmth = -0.2f),
        adjustSlider = AdjustSlider.WARMTH,
    )
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

/** The Stickers sheet in a sheet-sized surface, in light and in dark: Food chosen, its tiles in a grid. */
@PreviewLightDark
@Composable
private fun EditPanelStickersPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxWidth().height(200.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                EditPanelContent(
                    openTool = EditTool.STICKERS,
                    settings = EditSettings(),
                    onChange = {},
                    onPreview = {},
                    onCommit = {},
                    onRatio = {},
                )
            }
        }
    }
}

/**
 * The Crop sheet while Compare is on, in light and in dark: every chip is disabled and faint.
 */
@PreviewLightDark
@Composable
private fun EditPanelDisabledPreview() {
    SampleEditPanel(openTool = EditTool.CROP, settings = EditSettings(), enabled = false)
}
