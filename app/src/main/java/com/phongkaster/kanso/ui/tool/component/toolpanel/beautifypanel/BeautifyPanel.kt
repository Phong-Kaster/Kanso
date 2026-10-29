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
import com.phongkaster.kanso.ui.tool.model.BeautifyAmounts
import com.phongkaster.kanso.ui.tool.model.BeautifyGroup
import com.phongkaster.kanso.ui.tool.model.BeautifyTool

/** Test tag of the Beautify panel's smoothing slider, shown while Smooth is open. */
const val BEAUTIFY_STRENGTH_TAG = "beautify-strength"

/** Test tag of the Beautify panel's face width slider, shown while Face width is open. */
const val BEAUTIFY_WIDTH_TAG = "beautify-width"

/** Test tag of the Beautify panel's face lift slider, shown while Face lift is open. */
const val BEAUTIFY_LIFT_TAG = "beautify-lift"

/** Test tag of the Beautify panel's waist slider, shown while Body > Waist is open. */
const val BEAUTIFY_WAIST_TAG = "beautify-waist"

/** Test tag of the Beautify panel's arms slider, shown while Body > Arms is open. */
const val BEAUTIFY_ARMS_TAG = "beautify-arms"

/**
 * The Beautify tool's controls, shown in the options strip under the photo, laid out like the Edit
 * screen: a row of tools on two levels ([BeautifyToolRow]: Face, Body and Smooth on top, Face and
 * Body each opening their own row of tools with a Back entry), and above it the slider of the tool
 * that is open.
 *
 * **Laid out as the Edit screen's bottom bar.** The screen puts this panel in its `Scaffold`'s
 * bottom bar, so the photo gets all the height above it. Like Edit's bar it is the lowest thing on
 * screen (the screen keeps it clear of the navigation bar and the gesture area): the row of tools
 * at the very bottom, and above it Edit's own fixed-height panel area ([EditSheet]), which shows the open tool's
 * slider on a rounded surface, or "Pick a tool to start editing" while none is open. The bar never
 * changes height, so the photo never jumps when a tool opens.
 *
 * - **Two levels, as in Meitu.** Tapping Face shows Face's tools (Width and Lift) and Back, and
 *   tapping Body shows Body's (Waist and Arms; Chest and Legs come one at a time); either opens the
 *   group's first tool at once, so its slider is ready. Back returns to the top row and closes the
 *   slider. Smooth, a tool of its own, opens its slider straight from the top row. Each change of
 *   group is told to [onGroupChange], so the screen can show the whole photo while Body is open
 *   (the zoom onto the face would hide the waist).
 * - Tapping a tool opens its slider; tapping the open tool closes it. The open tool is highlighted
 *   and the panel area cross-fades to its slider with `AnimatedContent` (Compose's own animation
 *   API). The sliders keep their values while the person moves between the rows.
 * - **Smooth** is a 0..100 slider (0, no smoothing, by default); **Face width** a -100..+100
 *   slider (0, the face as it is, by default; negative is wider, positive narrower, as in Meitu), drawn
 *   two-sided: coloured from 0 toward the thumb; **Face lift** the same, -100..+100 (negative drops
 *   the jaw, positive lifts it, as in Meitu); **Waist** and **Arms** the same, -100..+100 (negative
 *   wider, positive slimmer, as in Meitu).
 * - **Live while dragging, as in Meitu.** Every move of a slider calls [onPreview] with every
 *   value ([BeautifyAmounts]), and the screen shows a quick frame of the edit on the photo as
 *   shown, so the photo follows the finger. Letting go calls [onApply] with every value, and the photo is redone from
 *   the original at full quality (several seconds on a phone), which then replaces the frame.
 * - A tool whose value is not its default carries the Edit row's small dot, and so does its
 *   group in the top row, so the person sees which ones hold a change.
 *
 * The entries are the Edit screen's own ([EditToolEntry]) and each slider is a [BeautifySlider]
 * (its name on a line of its own, the slider the panel's full width under it), so both screens
 * look and behave alike. The open group, the open tool and every value are the panel's own state, kept with
 * `rememberSaveable`, so they survive a rotation. [enabled] false (while an edit runs) switches
 * every control off.
 */
@Composable
fun BeautifyPanel(
    onApply: (BeautifyAmounts) -> Unit,
    onPreview: (BeautifyAmounts) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onGroupChange: (BeautifyGroup?) -> Unit = {},
) {
    // `by` reads and writes each state's value directly; a change redraws the panel.
    var openGroup by rememberSaveable(init = { mutableStateOf<BeautifyGroup?>(null) })
    var openTool by rememberSaveable(init = { mutableStateOf<BeautifyTool?>(null) })
    var strength by rememberSaveable(init = { mutableFloatStateOf(DEFAULT_STRENGTH) })
    var width by rememberSaveable(init = { mutableFloatStateOf(DEFAULT_WIDTH) })
    var lift by rememberSaveable(init = { mutableFloatStateOf(DEFAULT_LIFT) })
    var waist by rememberSaveable(init = { mutableFloatStateOf(DEFAULT_WAIST) })
    var arms by rememberSaveable(init = { mutableFloatStateOf(DEFAULT_ARMS) })

    /** Every slider's value as it is now, read when a slider moves or is let go. */
    fun amounts() = BeautifyAmounts(smoothing = strength, faceWidth = width, faceLift = lift, waist = waist, arms = arms)
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
                                onPreview(amounts())
                            },
                            onValueChangeFinished = { onApply(amounts()) },
                            tag = BEAUTIFY_STRENGTH_TAG,
                            enabled = enabled,
                            valueRange = 0f..1f,
                        )
                        BeautifyTool.FACE_WIDTH -> BeautifySlider(
                            label = R.string.beautify_width_label,
                            value = width,
                            onValueChange = {
                                width = it.coerceIn(-1f, 1f)
                                onPreview(amounts())
                            },
                            onValueChangeFinished = { onApply(amounts()) },
                            tag = BEAUTIFY_WIDTH_TAG,
                            enabled = enabled,
                            valueRange = -1f..1f,
                        )
                        BeautifyTool.FACE_LIFT -> BeautifySlider(
                            label = R.string.beautify_lift_label,
                            value = lift,
                            onValueChange = {
                                lift = it.coerceIn(-1f, 1f)
                                onPreview(amounts())
                            },
                            onValueChangeFinished = { onApply(amounts()) },
                            tag = BEAUTIFY_LIFT_TAG,
                            enabled = enabled,
                            valueRange = -1f..1f,
                        )
                        BeautifyTool.WAIST -> BeautifySlider(
                            label = R.string.beautify_waist_label,
                            value = waist,
                            onValueChange = {
                                waist = it.coerceIn(-1f, 1f)
                                onPreview(amounts())
                            },
                            onValueChangeFinished = { onApply(amounts()) },
                            tag = BEAUTIFY_WAIST_TAG,
                            enabled = enabled,
                            valueRange = -1f..1f,
                        )
                        BeautifyTool.ARMS -> BeautifySlider(
                            label = R.string.beautify_arms_label,
                            value = arms,
                            onValueChange = {
                                arms = it.coerceIn(-1f, 1f)
                                onPreview(amounts())
                            },
                            onValueChangeFinished = { onApply(amounts()) },
                            tag = BEAUTIFY_ARMS_TAG,
                            enabled = enabled,
                            valueRange = -1f..1f,
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
                        BeautifyTool.WAIST -> waist != DEFAULT_WAIST
                        BeautifyTool.ARMS -> arms != DEFAULT_ARMS
                    }
                },
                enabled = enabled,
                // Into a group: its row, with its first tool's slider open and ready.
                onOpenGroup = { group ->
                    openGroup = group
                    openTool = group.tools.firstOrNull()
                    onGroupChange(group)
                },
                onTool = { tool -> openTool = if (openTool == tool) null else tool },
                // Back to the top row, with no slider open.
                onBack = {
                    openGroup = null
                    openTool = null
                    onGroupChange(null)
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

/** The waist slider's starting value: the body as it is. */
private const val DEFAULT_WAIST = 0f

/** The arms slider's starting value: the arms as they are. */
private const val DEFAULT_ARMS = 0f

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
                BeautifyPanel(onApply = {}, onPreview = {})
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
                BeautifyPanel(onApply = {}, onPreview = {}, enabled = false)
            }
        }
    }
}
