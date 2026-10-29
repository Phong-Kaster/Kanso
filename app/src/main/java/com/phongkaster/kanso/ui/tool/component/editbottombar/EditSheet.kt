package com.phongkaster.kanso.ui.tool.component.editbottombar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the open tool's panel, which appears just above the row of tools while a tool is open. */
const val EDIT_SHEET_TAG = "edit-sheet"

/**
 * The fixed height of the panel area above the row of tools, open or not. It fits the tallest
 * panel, Adjust's or Filters' (a row of chips with a 48 dp touch target, plus one 48 dp slider
 * row), with the surface's padding and the gap above the row.
 */
private val SHEET_HEIGHT = 136.dp

/**
 * The open tool's panel, just above the row of tools, in an area of one fixed height
 * ([SHEET_HEIGHT]) whether a tool is open or not and whichever tool it is. That is what keeps the
 * photo one size: the bottom bar never changes height, so the space `Scaffold` leaves the photo
 * never changes either. Every tool's controls are made to fit it (Adjust shows one slider at a
 * time, picked by chips); with very large text they scroll inside it rather than grow it.
 *
 * With [openTool] null the area shows a short hint to pick a tool. Otherwise it is a rounded
 * surface holding [sheet] for that tool, tagged [EDIT_SHEET_TAG] and described to screen readers
 * as the tool settings, its controls centred in it.
 *
 * `AnimatedContent` (Compose's own animation API) cross-fades between what it shows for one value
 * and the next: the hint and a tool's panel, or one tool's controls and another's. Only what is
 * inside the area fades; the area itself never moves.
 *
 * It is generic in [T], the kind of tool: the Edit screen passes its `EditTool`s and the Beautify
 * screen its `BeautifyTool`s, so both screens share one panel area that looks and moves alike.
 */
@Composable
internal fun <T : Any> EditSheet(
    openTool: T?,
    sheet: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetText = stringResource(R.string.edit_layout_sheet)
    AnimatedContent(
        targetState = openTool,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "edit-sheet",
        modifier = modifier
            .fillMaxWidth()
            .height(SHEET_HEIGHT)
            .padding(bottom = 8.dp),
    ) { tool ->
        if (tool != null) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(EDIT_SHEET_TAG)
                    .semantics { contentDescription = sheetText },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    sheet(tool)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.edit_sheet_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The panel area with Filters open, in light and in dark; a line of text stands in for the controls. */
@PreviewLightDark
@Composable
private fun EditSheetPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        EditSheet(openTool = EditTool.FILTERS, sheet = { tool -> Text(text = tool.name) })
    }
}
