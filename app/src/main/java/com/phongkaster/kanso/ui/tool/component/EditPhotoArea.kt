package com.phongkaster.kanso.ui.tool.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme
import com.phongkaster.kanso.ui.tool.ToolUiState
import com.phongkaster.kanso.ui.tool.component.editbottombar.EditBottomBar
import com.phongkaster.kanso.ui.tool.component.editbottombar.edittoolrow.EditToolRow

/**
 * The Edit screen's photo area, stateless: the photo (or, while Crop is open, the cropper) filling
 * all the space between the top bar and the bottom bar. It draws [state] and reports nothing: Undo
 * sits in the top bar beside Done, and Compare is pinned at the right of the tool row
 * ([EditToolRow]) in the bottom bar (the Stickers sheet replaces that row while it is open).
 *
 * **Why the photo never changes size.** The row of tools and the open tool's panel live in the
 * screen's bottom bar ([EditBottomBar]). `Scaffold` measures that bar first and hands this area
 * what is left. The bar is one fixed height whether a tool is open or not and whichever tool it
 * is, so the photo keeps one size while the person moves between tools. Nothing ever covers or
 * dims it.
 *
 * [photo] draws the photo (the screen passes its photo card); it is a slot, so this function knows
 * nothing of bitmaps or processors and a preview can pass a plain sample.
 *
 * [cropper], when given, takes the photo's place while the Crop panel is open (the screen passes
 * [EditCropper]); while Compare is on, or with no cropper given, [photo] is drawn as usual.
 */
@Composable
fun EditPhotoArea(
    state: ToolUiState,
    photo: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    cropper: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // While Crop is open the photo card becomes the cropper (handles, drag, pinch); while
        // comparing, the plain photo is shown instead, so the original can be seen.
        if (state.openTool == EditTool.CROP && !state.comparing && cropper != null) {
            cropper()
        } else {
            photo()
        }
    }
}

/**
 * A sample Edit screen for the previews, laid out as the real one: a `Scaffold` whose bottom bar
 * is [EditBottomBar], with [EditPhotoArea] above it. Plain coloured boxes stand in for the photo
 * and the cropper, and a line of text for the panel, so nothing touches a ViewModel or decodes a
 * photo.
 */
@Composable
private fun SampleEditToolContent(state: ToolUiState) {
    KansoTheme(themeMode = ThemeMode.System) {
        Scaffold(
            bottomBar = {
                EditBottomBar(
                    state = state,
                    onOpenTool = {},
                    sheet = { tool -> Text(text = tool.name) },
                )
            },
        ) { innerPadding ->
            EditPhotoArea(
                state = state,
                photo = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    )
                },
                // A box of another colour stands in for the cropper, so Crop's swap is visible.
                cropper = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    )
                },
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
    }
}

/**
 * The Edit screen with Adjust's panel open above the row of tools and an edit to undo, in light
 * and in dark: the sample settings raise brightness, so Adjust's entry carries the "changed" dot.
 */
@PreviewLightDark
@Composable
private fun EditToolContentPreview() {
    SampleEditToolContent(
        ToolUiState(
            tool = Tool.EDIT,
            photoUri = "content://sample/photo/1",
            canUndo = true,
            openTool = EditTool.ADJUST,
            settings = EditSettings(brightness = 0.3f),
        ),
    )
}

/**
 * The Edit screen with no tool open, in light and in dark: the row of tools sits alone at the
 * bottom and the photo takes all the space above it.
 */
@PreviewLightDark
@Composable
private fun EditToolContentClosedPreview() {
    SampleEditToolContent(ToolUiState(tool = Tool.EDIT, photoUri = "content://sample/photo/1"))
}

/**
 * The Edit screen with Crop open, in light and in dark: the cropper (here a plain stand-in box)
 * takes the photo's place, and Crop's panel above the row of tools.
 */
@PreviewLightDark
@Composable
private fun EditToolContentCropPreview() {
    SampleEditToolContent(
        ToolUiState(
            tool = Tool.EDIT,
            photoUri = "content://sample/photo/1",
            openTool = EditTool.CROP,
        ),
    )
}

/** The Edit screen while Compare is on, in light and in dark: the row and Undo are faint. */
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
