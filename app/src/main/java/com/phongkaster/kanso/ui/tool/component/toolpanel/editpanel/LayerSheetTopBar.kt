package com.phongkaster.kanso.ui.tool.component.toolpanel.editpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the cancel (X) button of a layer sheet (Stickers or Text). */
const val LAYER_SHEET_CANCEL_TAG = "layer-sheet-cancel"

/** Test tag of the confirm (check) button of a layer sheet (Stickers or Text). */
const val LAYER_SHEET_CONFIRM_TAG = "layer-sheet-confirm"

/**
 * The top row of a layer sheet (Stickers or Text): a cross at the start that throws the sheet's
 * changes away ([onCancel]), the sheet's [title] in the middle, and a check at the end that
 * keeps them ([onConfirm]). [cancelDescription] and [confirmDescription] are what a screen
 * reader says for the two buttons. Only one sheet is open at a time, so both sheets share the
 * two test tags.
 *
 * Each button is an [IconButton], whose 48 dp square is the minimum comfortable touch target. The
 * icons are Material Symbols drawables drawn with [Icon], which tints them with the content
 * colour (the files paint white, so they must not be drawn as a plain image).
 */
@Composable
internal fun LayerSheetTopBar(
    title: String,
    cancelDescription: String,
    confirmDescription: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        // Space between pushes the cross to the start, the title to the middle and the check to the end.
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(48.dp).testTag(LAYER_SHEET_CANCEL_TAG),
                content = {
                    Icon(painter = painterResource(R.drawable.ic_close), contentDescription = cancelDescription)
                },
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                // A heading lets a screen reader jump straight to the sheet.
                modifier = Modifier.semantics { heading() },
            )
            IconButton(
                onClick = onConfirm,
                modifier = Modifier.size(48.dp).testTag(LAYER_SHEET_CONFIRM_TAG),
                content = {
                    Icon(painter = painterResource(R.drawable.ic_check), contentDescription = confirmDescription)
                },
            )
        },
    )
}

/**
 * The Stickers sheet's top bar, in light and in dark. The sample sits in a full-size Box because
 * `KansoTheme` stretches its direct child to the whole screen.
 */
@PreviewLightDark
@Composable
private fun LayerSheetTopBarPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = {
                LayerSheetTopBar(
                    title = stringResource(R.string.stickers_tool_label),
                    cancelDescription = stringResource(R.string.stickers_sheet_cancel),
                    confirmDescription = stringResource(R.string.stickers_sheet_confirm),
                    onCancel = {},
                    onConfirm = {},
                )
            },
        )
    }
}
