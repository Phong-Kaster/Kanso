package com.phongkaster.kanso.ui.tool.component.editbottombar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.phongkaster.kanso.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EditToolRow

/** The height of the panel area above the row of tools (the same 136 dp as the fixed sheet in EditSheet). */
private val SHEET_AREA_HEIGHT = 136.dp

/** The row of tools' height when it has not been measured yet: a 64 dp entry plus 4 dp above and below. */
private val DEFAULT_ROW_HEIGHT = 72.dp

/**
 * The height of a layer sheet (Stickers, Text), which takes the place of the row of tools. It is
 * taller than the panel area and the row together, to hold its top bar, the size and opacity
 * sliders and its own controls, so the photo above gets a little shorter while one is open.
 */
private val LAYER_SHEET_HEIGHT = 320.dp

/**
 * The Edit screen's bottom bar, the lowest thing on screen: one row of Crop, Rotate, Adjust,
 * Filters, Stickers, Text and Compare, and just above it the fixed-height panel area, which holds
 * the open tool's panel ([sheet]) or a hint when none is open; while Stickers or Text is open its
 * sheet takes
 * the place of both (see below). It is passed to `Scaffold`'s `bottomBar` slot, so
 * the photo area above always ends where this bar begins, and since the bar never changes height
 * neither does the photo.
 *
 * The bar keeps clear of the navigation bar and of the bottom gesture area: with swipe gestures
 * the navigation bar can be 0 dp tall while a swipe up from the edge still means "go home".
 *
 * **Why the panel is not a modal bottom sheet.** Material's `ModalBottomSheet` dims everything
 * behind it and swallows touches outside it, so the entries of the row could not
 * be tapped while a panel is open. Here the panel is an ordinary surface in the bar: nothing dims
 * the photo and the row stays in reach.
 *
 * [onOpenTool] is a tool entry tapped (the ViewModel opens that tool, or closes it if it was
 * open). While Compare is on, or Done is rendering, the row is disabled; the screen also passes
 * that flag to the panel's controls, which disable themselves.
 *
 * **A layer sheet replaces the row.** While Stickers or Text is open its sheet (which has its own
 * Cancel and Confirm) is shown instead of the row of tools, [LAYER_SHEET_HEIGHT] tall: taller than
 * the panel area and the row together, because it also holds the selected layer's size and
 * opacity sliders, so the photo gets a little shorter while it is open. While the Text sheet's
 * keyboard is up the sheet shrinks to its text area, and the whole bar rides on top of the
 * keyboard (the keyboard's inset is part of the bar's padding). The row's height is
 * measured with `onSizeChanged` while the row is on screen and remembered for that moment. While
 * Compare is on, or Done is rendering, the ViewModel ignores the sheet's Cancel, Confirm and the
 * system Back, so the sheet cannot change what Done is drawing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditBottomBar(
    state: ToolUiState,
    onOpenTool: (EditTool) -> Unit,
    sheet: @Composable (EditTool) -> Unit,
    modifier: Modifier = Modifier,
    onCompare: (Boolean) -> Unit = {},
) {
    // The row's last measured height in pixels (0 until it was drawn once).
    var rowHeightPx by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                // The keyboard (Text sheet) counts too: while it is up the bar sits on top of it.
                WindowInsets.navigationBars
                    .union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom))
                    .union(WindowInsets.ime),
            )
            .padding(bottom = 8.dp),
    ) {
        val layerSheet = state.openTool == EditTool.STICKERS || state.openTool == EditTool.TEXT
        if (layerSheet) {
            // A layer sheet (Stickers, Text) takes the place of the row of tools. While the Text
            // sheet's keyboard is up it is only as tall as its text area, so the photo stays in
            // sight above the keyboard.
            val sheetText = stringResource(R.string.edit_layout_sheet)
            val density = LocalDensity.current
            val rowHeight = if (rowHeightPx > 0) with(density) { rowHeightPx.toDp() } else DEFAULT_ROW_HEIGHT
            // Compact only while the keyboard is really on screen: a keyboard that was asked for
            // but never came (or was hidden by the system) must not leave the sheet without its
            // rows. Compact means "as tall as the text area": the sheet wraps its content then,
            // and must never fill the bar's whole height (the screen).
            val compact = state.openTool == EditTool.TEXT && state.textEditing && WindowInsets.isImeVisible
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .then(
                        if (compact) {
                            Modifier.wrapContentHeight()
                        } else {
                            Modifier.height(maxOf(LAYER_SHEET_HEIGHT, SHEET_AREA_HEIGHT + rowHeight))
                        },
                    )
                    .padding(bottom = 8.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .then(if (compact) Modifier.fillMaxWidth() else Modifier.fillMaxSize())
                        .testTag(EDIT_SHEET_TAG)
                        .semantics { contentDescription = sheetText },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    sheet(state.openTool ?: EditTool.STICKERS)
                }
            }
        } else {
            EditSheet(
                openTool = state.openTool,
                sheet = sheet,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            // The session's settings tell the row which tools hold a change (their dot).
            EditToolRow(
                openTool = state.openTool,
                enabled = !state.comparing && !state.finishing,
                onOpen = onOpenTool,
                settings = state.settings,
                comparing = state.comparing,
                // Compare is the way back from comparing, so only Done's render switches it off.
                compareEnabled = !state.finishing,
                onCompare = onCompare,
                // Remember how tall the row is: a layer sheet is never shorter than the row and the panel area.
                modifier = Modifier.onSizeChanged { rowHeightPx = it.height },
            )
        }
    }
}

/**
 * The bottom bar with Adjust's panel open, in light and in dark, pinned to the bottom of a
 * full-size box as the Scaffold pins it. A line of text stands in for the panel's controls.
 */
@PreviewLightDark
@Composable
private fun EditBottomBarPreview() {
    KansoTheme(themeMode = ThemeMode.System) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            EditBottomBar(
                state = ToolUiState(
                    tool = Tool.EDIT,
                    photoUri = "content://sample/photo/1",
                    openTool = EditTool.ADJUST,
                ),
                onOpenTool = {},
                sheet = { tool -> Text(text = tool.name) },
            )
        }
    }
}
