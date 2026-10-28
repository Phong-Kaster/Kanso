package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.Layer
import com.phongkaster.kanso.data.edit.edit.LayerContent
import com.phongkaster.kanso.data.edit.edit.LayerSize
import com.phongkaster.kanso.data.edit.edit.TextAlignment
import com.phongkaster.kanso.data.edit.edit.TextFont
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerSheetTopBar
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerSliders

/** Test tag of the whole Text panel. */
const val TEXT_PANEL_TAG = "text-panel"

/**
 * The Text sheet's contents (ADR-005), for the selected text layer [selected] (null when no text
 * is selected: the rows are then greyed out, but the text area still takes words, which start a
 * new text):
 *
 * 1. the top bar: Cancel ([onCancel]), the title "Text" and Confirm ([onConfirm]);
 * 2. the text area with its alignment button ([TextInputRow]);
 * 3. the font row ([TextFontRow]), "None" and the five bundled fonts;
 * 4. the colour row ([TextColourRow]), twelve ready colours and a rainbow circle that swaps the
 *    rows for the colour picker ([TextColourPane]) until its back arrow is tapped;
 * 5. the size and opacity sliders ([LayerSliders]); [size] is the selected text's size number.
 *
 * **While the keyboard is up** ([editing]) only the text area is shown, so the keyboard and the
 * sheet leave the photo in sight and the words appear on it as they are typed; the other rows come
 * back when the keyboard goes down.
 *
 * Every change goes out as a callback and the panel keeps no state of its own about the text:
 * [onWords] is the area's new words, [onEditingChange] the keyboard opening or closing,
 * [onAlignment] the alignment button, [onFont] a font chip, [onColour] a ready colour (one undo
 * step), [onCustomColour] each colour the picker gives (previewed) and [onCustomColourDone] the
 * picker's back arrow (the picker's colours become one undo step). [onSize] and [onOpacity] are the
 * sliders moving and [onSliderDone] their release. The one thing it remembers is whether the
 * colour picker is open, a view of the sheet that no other part of the screen needs.
 */
@Composable
internal fun TextPanel(
    selected: Layer?,
    size: Float,
    editing: Boolean,
    enabled: Boolean,
    onWords: (String) -> Unit,
    onEditingChange: (Boolean) -> Unit,
    onAlignment: () -> Unit,
    onFont: (TextFont) -> Unit,
    onColour: (Int) -> Unit,
    onCustomColour: (Int) -> Unit,
    onCustomColourDone: () -> Unit,
    onSize: (Float) -> Unit,
    onOpacity: (Float) -> Unit,
    onSliderDone: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = selected?.content as? LayerContent.Text
    val canEdit = enabled && text != null
    var picking by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth().testTag(TEXT_PANEL_TAG),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = {
            if (picking && !editing && text != null) {
                TextColourPane(
                    startArgb = text.colorArgb,
                    onColour = onCustomColour,
                    onBack = {
                        picking = false
                        onCustomColourDone()
                    },
                )
                return@Column
            }
            AnimatedVisibility(
                visible = !editing,
                content = {
                    LayerSheetTopBar(
                        title = stringResource(R.string.text_tool_label),
                        cancelDescription = stringResource(R.string.text_sheet_cancel),
                        confirmDescription = stringResource(R.string.text_sheet_confirm),
                        onCancel = onCancel,
                        onConfirm = onConfirm,
                    )
                },
            )
            TextInputRow(
                key = selected?.key,
                words = text?.text.orEmpty(),
                alignment = text?.alignment ?: TextAlignment.CENTRE,
                editing = editing,
                enabled = enabled,
                onWords = onWords,
                onEditingChange = onEditingChange,
                onAlignment = onAlignment,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            AnimatedVisibility(
                visible = !editing,
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        content = {
                            TextFontRow(
                                chosen = text?.font,
                                enabled = canEdit,
                                onChoose = onFont,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            TextColourRow(
                                chosenArgb = text?.colorArgb,
                                enabled = canEdit,
                                onChoose = onColour,
                                onCustom = { picking = true },
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            LayerSliders(
                                size = size,
                                opacity = selected?.opacity ?: 1f,
                                enabled = canEdit,
                                onSizeChange = onSize,
                                onOpacityChange = onOpacity,
                                onChangeFinished = onSliderDone,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        },
                    )
                },
            )
        },
    )
}

/** A sample text layer for the previews: two words in red, Dancing Script, centred. */
private val sampleText = Layer(
    key = 1L,
    content = LayerContent.Text(
        text = "Xin chào",
        colorArgb = 0xFFE53935.toInt(),
        font = TextFont.DANCING_SCRIPT,
        alignment = TextAlignment.CENTRE,
        wrapEms = 8f,
    ),
    centreX = 0.5f,
    centreY = 0.5f,
    width = 0.4f,
    degrees = 0f,
    mirrored = false,
    opacity = 0.9f,
)

/** The whole Text sheet with a text selected and the keyboard down, in light and in dark. */
@PreviewLightDark
@Composable
private fun TextPanelPreview() {
    SampleTextPanel(editing = false)
}

/** The Text sheet while the keyboard is up: only the text area shows, in light and in dark. */
@PreviewLightDark
@Composable
private fun TextPanelEditingPreview() {
    SampleTextPanel(editing = true)
}

/** The panel on a sheet-coloured surface the height of the real sheet. */
@Composable
private fun SampleTextPanel(editing: Boolean) {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = {
                        TextPanel(
                            selected = sampleText,
                            size = LayerSize.TEXT_DEFAULT,
                            editing = editing,
                            enabled = true,
                            onWords = {},
                            onEditingChange = {},
                            onAlignment = {},
                            onFont = {},
                            onColour = {},
                            onCustomColour = {},
                            onCustomColourDone = {},
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
