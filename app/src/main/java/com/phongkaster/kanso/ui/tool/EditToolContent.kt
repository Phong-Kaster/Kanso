package com.phongkaster.kanso.ui.tool

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the area under the row of tools where the open tool's sheet is drawn. */
const val EDIT_SHEET_TAG = "edit-sheet"

/** The share of the space below the top bar the photo always takes; the controls get the rest. */
private const val PHOTO_SHARE = 0.6f

/**
 * The Edit screen's body below the top bar, stateless: the photo on top, the row of tools and the
 * open tool's sheet under it. It draws [state] and reports touches through callbacks.
 *
 * **Why the photo never changes size.** The body is a column split by weights: the photo area
 * takes 60% of the height ([PHOTO_SHARE]) and the controls area the other 40%, whatever is inside
 * them. Opening or closing a sheet only changes what is drawn inside the 40%, so the photo stays
 * put instead of jumping each time a tool is tapped.
 *
 * **Why the sheet is not a modal bottom sheet.** Material's `ModalBottomSheet` dims everything
 * behind it and swallows touches outside it, so the Undo and Compare pill on the photo could not
 * be tapped while a sheet is open. Here the sheet is an ordinary surface in the controls area:
 * nothing dims the photo and the pill stays in reach.
 *
 * [photo] draws the photo (the screen passes its photo card) and [sheet] draws the controls of the
 * tool it is given; both are slots, so this function knows nothing of bitmaps or processors and a
 * preview can pass plain samples. [onOpenTool] is a tool entry tapped, [onUndo] the pill's Undo
 * and [onCompare] its Compare switch flipped, with the new value.
 *
 * While Compare is on, the row of tools is disabled, and so is Undo; the screen also passes the
 * comparing flag to the sheet's controls, which disable themselves.
 */
@Composable
fun EditToolContent(
    state: ToolUiState,
    onOpenTool: (EditTool) -> Unit,
    onUndo: () -> Unit,
    onCompare: (Boolean) -> Unit,
    photo: @Composable () -> Unit,
    sheet: @Composable (EditTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(PHOTO_SHARE)
                .fillMaxWidth(),
        ) {
            photo()
            // Undo is usable only with an edit to take back, nothing running, and Compare off.
            EditPill(
                undoEnabled = state.canUndo &&
                    state.processing is ToolUiState.Processing.Idle &&
                    !state.comparing,
                comparing = state.comparing,
                onUndo = onUndo,
                onCompare = onCompare,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f - PHOTO_SHARE)
                .fillMaxWidth(),
        ) {
            EditToolRow(openTool = state.openTool, enabled = !state.comparing, onOpen = onOpenTool)
            EditSheetArea(
                openTool = state.openTool,
                sheet = sheet,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

/**
 * The area under the row of tools: empty while [openTool] is null, otherwise a surface with
 * rounded top corners holding [sheet] for that tool. The area itself always exists and carries
 * [EDIT_SHEET_TAG]; the surface, present only while a sheet is open, is described to screen
 * readers as the tool settings.
 *
 * `AnimatedContent` (Compose's own animation API) animates between what it shows for one value
 * and for the next: here the sheet slides up and fades in when a tool opens, and the old content
 * fades out as the new one comes in when the person switches tools or closes the sheet.
 */
@Composable
private fun EditSheetArea(
    openTool: EditTool?,
    sheet: @Composable (EditTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetText = stringResource(R.string.edit_layout_sheet)
    Box(modifier = modifier.testTag(EDIT_SHEET_TAG)) {
        AnimatedContent(
            targetState = openTool,
            transitionSpec = {
                (fadeIn() + slideInVertically { height -> height / 4 }) togetherWith
                    (fadeOut() + slideOutVertically { height -> height / 4 })
            },
            label = "edit-sheet",
            modifier = Modifier.fillMaxSize(),
        ) { tool ->
            if (tool != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = sheetText },
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        sheet(tool)
                    }
                }
            } else {
                // Nothing open: the area stays empty, keeping its height so the photo never moves.
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/**
 * A sample Edit screen body for the previews: a plain coloured box stands in for the photo and a
 * line of text for the sheet, so nothing touches a ViewModel or decodes a photo.
 */
@Composable
private fun SampleEditToolContent(state: ToolUiState) {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize()) {
            EditToolContent(
                state = state,
                onOpenTool = {},
                onUndo = {},
                onCompare = {},
                photo = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    )
                },
                sheet = { tool -> Text(text = tool.name) },
            )
        }
    }
}

/** The Edit screen body with Adjust's sheet open and an edit to undo, in light and in dark. */
@PreviewLightDark
@Composable
private fun EditToolContentPreview() {
    SampleEditToolContent(
        ToolUiState(
            tool = Tool.EDIT,
            photoUri = "content://sample/photo/1",
            canUndo = true,
            openTool = EditTool.ADJUST,
        ),
    )
}

/**
 * The Edit screen body with every sheet closed, in light and in dark: the photo keeps the same
 * size, and the area under the row of tools is empty.
 */
@PreviewLightDark
@Composable
private fun EditToolContentClosedPreview() {
    SampleEditToolContent(ToolUiState(tool = Tool.EDIT, photoUri = "content://sample/photo/1"))
}

/** The Edit screen body while Compare is on, in light and in dark: the row and Undo are faint. */
@PreviewLightDark
@Composable
private fun EditToolContentComparingPreview() {
    SampleEditToolContent(
        ToolUiState(
            tool = Tool.EDIT,
            photoUri = "content://sample/photo/1",
            canUndo = true,
            openTool = EditTool.FILTERS,
            comparing = true,
        ),
    )
}
