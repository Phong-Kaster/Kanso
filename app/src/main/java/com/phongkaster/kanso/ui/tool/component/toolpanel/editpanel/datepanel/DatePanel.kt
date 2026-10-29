package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.datepanel

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.DateFormat
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.LayerSize
import com.phongkaster.kanso.data.edit.edit.TextFont
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerSheetTopBar
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerOpacitySlider
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerSizeSlider
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerTabRow
import com.phongkaster.kanso.ui.tool.model.DateTab
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.datepanel.datemomentrow.DateMomentRow
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.DEFAULT_PICKER_BOX
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TextBackgroundRow
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TextColourPane
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TextColourRow
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel.TextFontRow
import java.time.LocalDateTime

/** Test tag of the whole Date panel. */
const val DATE_PANEL_TAG = "date-panel"

/**
 * The Date sheet's contents (ADR-006), for the selected date layer [selected] (null when no date
 * is selected), in three parts (the owner's decision, 2026-10-28):
 *
 * 1. the top bar: Cancel ([onCancel]), the title "Date" and Confirm ([onConfirm]). It is pinned:
 *    it never scrolls;
 * 2. the row of tabs ([LayerTabRow], [DateTab]): Format, Day & time, Font, Size, Colour,
 *    Background, Opacity;
 * 3. the chosen tab's ONE control: the 20 format chips ([DateFormatRow]), each written with the
 *    moment shown; the day and time buttons ([DateMomentRow]), which open the pickers; the font
 *    chips, the letters' colours and the box's colours, the same as the Text sheet's
 *    ([TextFontRow], [TextColourRow], [TextBackgroundRow]); or the size or opacity slider
 *    ([LayerSizeSlider], [LayerOpacitySlider]; [size] is the selected date's size number).
 *
 * The sheet opens on the Format tab, and the chosen tab stays while it is open. Format and
 * Day & time work with no date selected (a format chip adds a date; the day and time set the
 * moment the next one gets); the styling tabs are greyed out then and the Format tab shows. A
 * rainbow circle swaps the tabs and the control for the colour picker ([TextColourPane]) until its
 * back arrow: opaque for the letters, with its transparency bar for the box.
 *
 * **Which moment is shown.** The selected date's own moment, or with none selected [nextMoment],
 * the moment the next added date gets.
 *
 * Every change goes out as a callback and the panel keeps no state about the dates: [onMoment] a
 * picked moment, [onFormat] a format chip, [onFont] a font chip, [onColour] a ready colour (one
 * undo step), [onCustomColour] each colour the picker gives (previewed) and [onCustomColourDone]
 * the picker's back arrow (one undo step); [onBackground], [onCustomBackground] and
 * [onCustomBackgroundDone] the same for the box (null is no box). [onSize] and [onOpacity] are the
 * sliders moving and [onSliderDone] their release. What it remembers is the chosen tab and which
 * picker is open.
 */
@Composable
internal fun DatePanel(
    selected: Layer?,
    nextMoment: LocalDateTime,
    size: Float,
    enabled: Boolean,
    onMoment: (LocalDateTime) -> Unit,
    onFormat: (DateFormat) -> Unit,
    onFont: (TextFont) -> Unit,
    onColour: (Int) -> Unit,
    onCustomColour: (Int) -> Unit,
    onCustomColourDone: () -> Unit,
    onBackground: (Int?) -> Unit,
    onCustomBackground: (Int) -> Unit,
    onCustomBackgroundDone: () -> Unit,
    onSize: (Float) -> Unit,
    onOpacity: (Float) -> Unit,
    onSliderDone: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = selected?.content as? LayerContent.Date
    val canStyle = enabled && date != null
    val moment = date?.moment ?: nextMoment
    var chosenTab by rememberSaveable { mutableStateOf(DateTab.FORMAT) }
    var pickingColour by rememberSaveable { mutableStateOf(false) }
    var pickingBackground by rememberSaveable { mutableStateOf(false) }
    // A styling tab needs a selected date; without one the sheet shows its first tab.
    val tab = if (date == null && chosenTab.needsSelection) DateTab.FORMAT else chosenTab
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DATE_PANEL_TAG),
        content = {
            // Pinned: above everything that changes or scrolls, so it never moves.
            LayerSheetTopBar(
                title = stringResource(R.string.date_tool_label),
                cancelDescription = stringResource(R.string.date_sheet_cancel),
                confirmDescription = stringResource(R.string.date_sheet_confirm),
                onCancel = onCancel,
                onConfirm = onConfirm,
            )
            if (pickingColour && date != null) {
                TextColourPane(
                    title = stringResource(R.string.text_colour_title),
                    startArgb = date.colorArgb,
                    onColour = onCustomColour,
                    onBack = {
                        pickingColour = false
                        onCustomColourDone()
                    },
                    modifier = Modifier.weight(1f, fill = false),
                )
                return@Column
            }
            if (pickingBackground && date != null) {
                TextColourPane(
                    title = stringResource(R.string.text_background_title),
                    startArgb = date.boxArgb ?: DEFAULT_PICKER_BOX,
                    onColour = onCustomBackground,
                    onBack = {
                        pickingBackground = false
                        onCustomBackgroundDone()
                    },
                    modifier = Modifier.weight(1f, fill = false),
                    showAlpha = true,
                )
                return@Column
            }
            LayerTabRow(
                tabs = DateTab.entries,
                chosen = tab,
                label = { it.labelRes },
                usable = { !it.needsSelection || date != null },
                enabled = enabled,
                onChoose = { chosenTab = it },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            // The chosen tab's control, in the room the bars leave; it scrolls if taller.
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center,
                content = {
                    when (tab) {
                        DateTab.FORMAT -> DateFormatRow(
                            moment = moment,
                            chosen = date?.format,
                            enabled = enabled,
                            onChoose = onFormat,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        DateTab.MOMENT -> DateMomentRow(
                            moment = moment,
                            enabled = enabled,
                            onMoment = onMoment,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        DateTab.FONT -> TextFontRow(
                            chosen = date?.font,
                            enabled = canStyle,
                            onChoose = onFont,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        DateTab.SIZE -> LayerSizeSlider(
                            size = size,
                            enabled = canStyle,
                            onSizeChange = onSize,
                            onChangeFinished = onSliderDone,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        DateTab.COLOUR -> TextColourRow(
                            chosenArgb = date?.colorArgb,
                            enabled = canStyle,
                            onChoose = onColour,
                            onCustom = { pickingColour = true },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        DateTab.BACKGROUND -> TextBackgroundRow(
                            chosenArgb = date?.boxArgb,
                            enabled = canStyle,
                            onChoose = onBackground,
                            onCustom = { pickingBackground = true },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        DateTab.OPACITY -> LayerOpacitySlider(
                            opacity = selected?.opacity ?: 1f,
                            enabled = canStyle,
                            onOpacityChange = onOpacity,
                            onChangeFinished = onSliderDone,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                },
            )
        },
    )
}

/** A sample date layer for the previews: "Wed, 28 October 2026" in orange on a dark box, Space Mono. */
private val sampleDate = Layer(
    key = 1L,
    content = LayerContent.Date(
        moment = LocalDateTime.of(2026, 10, 28, 14, 30),
        format = DateFormat.WEEKDAY_FULL,
        colorArgb = 0xFFFB8C00.toInt(),
        font = TextFont.SPACE_MONO,
        boxArgb = 0xFF212121.toInt(),
    ),
    centreX = 0.5f,
    centreY = 0.5f,
    width = 0.5f,
    degrees = 0f,
    mirrored = false,
)

/** The whole Date sheet with a date selected, in light and in dark. */
@PreviewLightDark
@Composable
private fun DatePanelPreview() {
    SampleDatePanel(selected = sampleDate)
}

/** The Date sheet with no date selected: chips and pickers work, styling is greyed out. */
@PreviewLightDark
@Composable
private fun DatePanelNothingSelectedPreview() {
    SampleDatePanel(selected = null)
}

/** The panel on a sheet-coloured surface the height of the real sheet. */
@Composable
private fun SampleDatePanel(selected: Layer?) {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(208.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = {
                        DatePanel(
                            selected = selected,
                            nextMoment = LocalDateTime.of(2026, 10, 28, 14, 30),
                            size = LayerSize.TEXT_DEFAULT,
                            enabled = true,
                            onMoment = {},
                            onFormat = {},
                            onFont = {},
                            onColour = {},
                            onCustomColour = {},
                            onCustomColourDone = {},
                            onBackground = {},
                            onCustomBackground = {},
                            onCustomBackgroundDone = {},
                            onSize = {},
                            onOpacity = {},
                            onSliderDone = {},
                            onCancel = {},
                            onConfirm = {},
                        )
                    },
                )
            },
        )
    }
}
