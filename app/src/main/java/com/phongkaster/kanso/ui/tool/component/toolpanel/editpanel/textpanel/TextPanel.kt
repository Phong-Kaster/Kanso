package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.textpanel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerOpacitySlider
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerSizeSlider
import com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel.LayerTabRow
import com.phongkaster.kanso.ui.tool.model.TextTab

/** Test tag of the whole Text panel. */
const val TEXT_PANEL_TAG = "text-panel"

/**
 * Where the background picker starts when there is no box yet: white at half strength, so the
 * transparency bar is visibly in play from the first touch.
 */
internal const val DEFAULT_PICKER_BOX = 0x80FFFFFF.toInt()

/**
 * The Text sheet's contents (ADR-005), for the selected text layer [selected] (null when no text
 * is selected), in three parts (the owner's decision, 2026-10-28):
 *
 * 1. the top bar: Cancel ([onCancel]), the title "Text" and Confirm ([onConfirm]). It is pinned:
 *    it never scrolls, and it stays on screen even while the keyboard is up;
 * 2. the row of tabs ([LayerTabRow], [TextTab]): Text, Font, Size, Colour, Background, Opacity;
 * 3. the chosen tab's ONE control: the text area with its alignment button ([TextInputRow]), the
 *    font chips ([TextFontRow]), the size slider ([LayerSizeSlider]; [size] is the selected text's
 *    size number), the letters' colours ([TextColourRow]), the box's colours
 *    ([TextBackgroundRow]: none, a ready colour, or any colour with transparency) or the opacity
 *    slider ([LayerOpacitySlider]). It scrolls if it is taller than the room left.
 *
 * The sheet opens on the Text tab. The chosen tab stays while the sheet is open, also when another
 * text is selected; with no text selected the styling tabs are greyed out and the Text tab shows.
 * The keyboard always brings the Text tab back. A rainbow circle swaps the tabs and the control for
 * the colour picker ([TextColourPane]) until its back arrow is tapped: opaque for the letters, with
 * its transparency bar for the box.
 *
 * **While the keyboard is up** ([editing], and the keyboard really on screen) only the top bar and
 * the text area are shown, so the keyboard and the sheet leave as much of the photo in sight as
 * they can.
 *
 * Every change goes out as a callback and the panel keeps no state of its own about the text:
 * [onWords] is the area's new words, [onEditingChange] the keyboard opening or closing,
 * [onAlignment] the alignment button, [onFont] a font chip, [onColour] a ready colour (one undo
 * step), [onCustomColour] each colour the picker gives (previewed) and [onCustomColourDone] the
 * picker's back arrow (the picker's colours become one undo step); [onBackground],
 * [onCustomBackground] and [onCustomBackgroundDone] are the same for the box (null is no box).
 * [onSize] and [onOpacity] are the sliders moving and [onSliderDone] their release. What it
 * remembers is a view of the sheet no other part of the screen needs: the chosen tab and which
 * picker is open.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    val text = selected?.content as? LayerContent.Text
    val canEdit = enabled && text != null
    var chosenTab by rememberSaveable { mutableStateOf(TextTab.TEXT) }
    var pickingColour by rememberSaveable { mutableStateOf(false) }
    var pickingBackground by rememberSaveable { mutableStateOf(false) }
    // The keyboard opening (the text area, or the frame's edit button) brings the Text tab back.
    LaunchedEffect(editing, block = { if (editing) chosenTab = TextTab.TEXT })
    // A styling tab needs a selected text; without one the sheet shows its first tab.
    val tab = if (text == null && chosenTab.needsSelection) TextTab.TEXT else chosenTab
    // The tabs hide only while the keyboard is really on screen, not merely asked for: if it never
    // comes (or the system hides it), they stay in reach.
    val keyboardUp = editing && WindowInsets.isImeVisible
    Column(
        modifier = modifier.fillMaxWidth().testTag(TEXT_PANEL_TAG),
        content = {
            // Pinned: above everything that changes or scrolls, so it never moves.
            LayerSheetTopBar(
                title = stringResource(R.string.text_tool_label),
                cancelDescription = stringResource(R.string.text_sheet_cancel),
                confirmDescription = stringResource(R.string.text_sheet_confirm),
                onCancel = onCancel,
                onConfirm = onConfirm,
            )
            if (pickingColour && !keyboardUp && text != null) {
                TextColourPane(
                    title = stringResource(R.string.text_colour_title),
                    startArgb = text.colorArgb,
                    onColour = onCustomColour,
                    onBack = {
                        pickingColour = false
                        onCustomColourDone()
                    },
                    modifier = Modifier.weight(1f, fill = false),
                )
                return@Column
            }
            if (pickingBackground && !keyboardUp && text != null) {
                TextColourPane(
                    title = stringResource(R.string.text_background_title),
                    startArgb = text.boxArgb ?: DEFAULT_PICKER_BOX,
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
            AnimatedVisibility(
                visible = !keyboardUp,
                content = {
                    LayerTabRow(
                        tabs = TextTab.entries,
                        chosen = tab,
                        label = { it.labelRes },
                        usable = { !it.needsSelection || text != null },
                        enabled = enabled,
                        onChoose = { chosenTab = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                },
            )
            // weight(fill = false): the control gets at most the room the bars leave, and no more
            // than it needs, so the keyboard's compact sheet is only as tall as the text area.
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 4.dp),
                contentAlignment = Alignment.Center,
                content = {
                    when (tab) {
                        TextTab.TEXT -> TextInputRow(
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
                        TextTab.FONT -> TextFontRow(
                            chosen = text?.font,
                            enabled = canEdit,
                            onChoose = onFont,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        TextTab.SIZE -> LayerSizeSlider(
                            size = size,
                            enabled = canEdit,
                            onSizeChange = onSize,
                            onChangeFinished = onSliderDone,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        TextTab.COLOUR -> TextColourRow(
                            chosenArgb = text?.colorArgb,
                            enabled = canEdit,
                            onChoose = onColour,
                            onCustom = { pickingColour = true },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        TextTab.BACKGROUND -> TextBackgroundRow(
                            chosenArgb = text?.boxArgb,
                            enabled = canEdit,
                            onChoose = onBackground,
                            onCustom = { pickingBackground = true },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        TextTab.OPACITY -> LayerOpacitySlider(
                            opacity = selected?.opacity ?: 1f,
                            enabled = canEdit,
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

/** A sample text layer for the previews: two words in red on a white box, Dancing Script, centred. */
private val sampleText = Layer(
    key = 1L,
    content = LayerContent.Text(
        text = "Xin chào",
        colorArgb = 0xFFE53935.toInt(),
        font = TextFont.DANCING_SCRIPT,
        alignment = TextAlignment.CENTRE,
        wrapEms = 8f,
        boxArgb = 0xFFFFFFFF.toInt(),
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
                    modifier = Modifier.fillMaxWidth().height(208.dp),
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
