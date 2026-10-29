package com.phongkaster.kanso.ui.tool.component.toolpanel.beautifypanel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.component.editbottombar.EditSheet
import com.phongkaster.kanso.ui.tool.component.toolpanel.BEAUTIFY_PANEL_TAG
import com.phongkaster.kanso.ui.tool.model.BeautifyGroup
import com.phongkaster.kanso.ui.tool.model.BeautifyTool

/** Test tag of the Beautify panel's smoothing slider, shown while Smooth is open. */
const val BEAUTIFY_STRENGTH_TAG = "beautify-strength"

/** Test tag of the Beautify panel's face width slider, shown while Face width is open. */
const val BEAUTIFY_WIDTH_TAG = "beautify-width"

/** Test tag of the Beautify panel's face lift slider, shown while Face lift is open. */
const val BEAUTIFY_LIFT_TAG = "beautify-lift"

/**
 * The Beautify tool's controls, shown in the options strip under the photo, laid out like the Edit
 * screen: a row of tools on two levels ([BeautifyToolRow]: Face and Smooth on top, Face opening its
 * own row of face tools with a Back entry), and above it the slider of the tool that is open.
 *
 * **Laid out as the Edit screen's bottom bar.** The screen puts this panel in its `Scaffold`'s
 * bottom bar, so the photo gets all the height above it. Like Edit's bar it is the lowest thing on
 * screen (the screen keeps it clear of the navigation bar and the gesture area): the row of tools
 * at the very bottom, and above it Edit's own fixed-height panel area ([EditSheet]), which shows the open tool's
 * slider on a rounded surface, or "Pick a tool to start editing" while none is open. The bar never
 * changes height, so the photo never jumps when a tool opens.
 *
 * - **Two levels, as in Meitu.** Tapping Face shows Face's tools (Width and Lift, then Temple
 *   once it exists) and Back, and opens the group's first tool at once, so its slider is ready;
 *   Back returns to the top row and closes the slider. Smooth, a tool of its own, opens its slider
 *   straight from the top row.
 * - Tapping a tool opens its slider; tapping the open tool closes it. The open tool is highlighted
 *   and the panel area cross-fades to its slider with `AnimatedContent` (Compose's own animation
 *   API). The sliders keep their values while the person moves between the rows.
 * - **Smooth** is a 0..100 slider (0, no smoothing, by default); **Face width** a -100..+100
 *   slider (0, the face as it is, by default; negative is wider, positive narrower, as in Meitu), drawn
 *   two-sided: coloured from 0 toward the thumb; **Face lift** a 0..100 slider (0, the face as it
 *   is, by default).
 * - **Live while dragging, as in Meitu.** Every move of a slider calls [onPreview] with all three
 *   values, and the screen shows a quick frame of the edit on the photo as shown, so the face
 *   follows the finger. Letting go calls [onApply] with all three values, and the photo is redone from
 *   the original at full quality (several seconds on a phone), which then replaces the frame.
 * - A tool whose value is not its default carries the Edit row's small dot, and so does its
 *   group in the top row, so the person sees which ones hold a change.
 *
 * The entries are the Edit screen's own ([EditToolEntry]) and each slider is a [BeautifySlider]
 * (its name on a line of its own, the slider the panel's full width under it), so both screens
 * look and behave alike. The open group, the open tool and the three values are the panel's own state, kept with
 * `rememberSaveable`, so they survive a rotation. [enabled] false (while an edit runs) switches
 * every control off.
 */
@Composable
fun BeautifyPanel(
    onApply: (strength: Float, width: Float, lift: Float) -> Unit,
    onPreview: (strength: Float, width: Float, lift: Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // `by` reads and writes each state's value directly; a change redraws the panel.
    var openGroup by rememberSaveable(init = { mutableStateOf<BeautifyGroup?>(null) })
    var openTool by rememberSaveable(init = { mutableStateOf<BeautifyTool?>(null) })
    var strength by rememberSaveable(init = { mutableFloatStateOf(DEFAULT_STRENGTH) })
    var width by rememberSaveable(init = { mutableFloatStateOf(DEFAULT_WIDTH) })
    var lift by rememberSaveable(init = { mutableFloatStateOf(DEFAULT_LIFT) })
    Column(
        modifier = modifier
            .fillMaxWidth()
            // The screen's bottom bar keeps it clear of the navigation bar and the gesture area.
            .testTag(BEAUTIFY_PANEL_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            // Edit's panel area: the open tool's slider on a rounded surface, or the hint.
            EditSheet(
                openTool = openTool,
                modifier = Modifier.padding(horizontal = 16.dp),
                sheet = { tool ->
                    when (tool) {
                        BeautifyTool.SMOOTH -> BeautifySlider(
                            label = R.string.beautify_strength_label,
                            value = strength,
                            onValueChange = {
                                strength = it.coerceIn(0f, 1f)
                                onPreview(strength, width, lift)
                            },
                            onValueChangeFinished = { onApply(strength, width, lift) },
                            tag = BEAUTIFY_STRENGTH_TAG,
                            enabled = enabled,
                            valueRange = 0f..1f,
                        )
                        BeautifyTool.FACE_WIDTH -> BeautifySlider(
                            label = R.string.beautify_width_label,
                            value = width,
                            onValueChange = {
                                width = it.coerceIn(-1f, 1f)
                                onPreview(strength, width, lift)
                            },
                            onValueChangeFinished = { onApply(strength, width, lift) },
                            tag = BEAUTIFY_WIDTH_TAG,
                            enabled = enabled,
                            valueRange = -1f..1f,
                        )
                        BeautifyTool.FACE_LIFT -> BeautifySlider(
                            label = R.string.beautify_lift_label,
                            value = lift,
                            onValueChange = {
                                lift = it.coerceIn(0f, 1f)
                                onPreview(strength, width, lift)
                            },
                            onValueChangeFinished = { onApply(strength, width, lift) },
                            tag = BEAUTIFY_LIFT_TAG,
                            enabled = enabled,
                            valueRange = 0f..1f,
                        )
                    }
                },
            )
            BeautifyToolRow(
                openGroup = openGroup,
                openTool = openTool,
                changed = { tool ->
                    when (tool) {
                        BeautifyTool.SMOOTH -> strength != DEFAULT_STRENGTH
                        BeautifyTool.FACE_WIDTH -> width != DEFAULT_WIDTH
                        BeautifyTool.FACE_LIFT -> lift != DEFAULT_LIFT
                    }
                },
                enabled = enabled,
                // Into a group: its row, with its first tool's slider open and ready.
                onOpenGroup = { group ->
                    openGroup = group
                    openTool = group.tools.firstOrNull()
                },
                onTool = { tool -> openTool = if (openTool == tool) null else tool },
                // Back to the top row, with no slider open.
                onBack = {
                    openGroup = null
                    openTool = null
                },
            )
        },
    )
}

/** The smoothing slider's starting value: no smoothing, so Face width alone changes only the shape. */
private const val DEFAULT_STRENGTH = 0f

/** The face width slider's starting value: the face as it is. */
private const val DEFAULT_WIDTH = 0f

/** The face lift slider's starting value: the face as it is. */
private const val DEFAULT_LIFT = 0f

/**
 * The Beautify panel with no tool open, in light and in dark. The outer Box fills the preview
 * first, because KansoTheme would otherwise stretch its single child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun BeautifyPanelPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                BeautifyPanel(onApply = { _, _, _ -> }, onPreview = { _, _, _ -> })
            }
        }
    }
}

/** The Beautify panel switched off, as while an edit runs, in light and in dark. */
@PreviewLightDark
@Composable
private fun BeautifyPanelDisabledPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                BeautifyPanel(onApply = { _, _, _ -> }, onPreview = { _, _, _ -> }, enabled = false)
            }
        }
    }
}
