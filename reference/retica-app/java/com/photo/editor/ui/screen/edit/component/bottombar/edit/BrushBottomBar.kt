package com.photo.editor.ui.screen.edit.component.bottombar.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.bottomsheet.ColorPickerBottomSheet
import com.photo.editor.common.ui.foundation.BaseSlider
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.MintColor
import com.photo.editor.common.ui.theme.PrimaryColor
import com.photo.editor.common.ui.theme.SemiTransparent
import com.photo.editor.common.ui.theme.TextDefaultColors
import com.photo.editor.common.ui.theme.medium14
import com.photo.editor.common.ui.theme.regular12
import com.photo.editor.common.util.getAssetAbsoluteFilePath
import com.photo.editor.overlaybrush.brush.BrushPathProperties
import com.photo.editor.overlaybrush.brush.ImageBrushPattern
import com.photo.editor.overlaybrush.brush.LineBrushPattern
import com.photo.editor.overlaybrush.brush.LineBrushType
import com.photo.editor.overlaybrush.brush.TextBrushPattern
import com.photo.editor.overlaybrush.brush.textbrusheditor.TextBrushEditorDialog
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageSubBottomBarTitle

@Composable
fun BrushBottomBar(
    uiState: EditImageUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state = uiState.brushDrawingState
    var showEditTextBrushDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {


        CenterRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(0.7f))
                .padding(vertical = 10.dp, horizontal = 16.dp),
            itemSpacing = 12.dp
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_undo),
                contentDescription = stringResource(R.string.undo),
                modifier = Modifier
                    .size(32.dp)
                    .rounded(5.dp)
                    .background(SemiTransparent)
                    .rippleClickable(enabled = state.paths.isNotEmpty()) { state.undo() }
                    .padding(4.dp),
                tint = if (state.paths.isNotEmpty()) Color.White else Color.DarkGray,
            )

            Icon(
                painter = painterResource(R.drawable.ic_redo),
                contentDescription = stringResource(R.string.redo),
                modifier = Modifier
                    .size(32.dp)
                    .rounded(5.dp)
                    .background(SemiTransparent)
                    .rippleClickable(enabled = state.pathsUndone.isNotEmpty()) { state.redo() }
                    .padding(4.dp),
                tint = if (state.pathsUndone.isNotEmpty()) Color.White else Color.DarkGray,
            )

            BaseSlider(
                value = state.currentPathProperty.size,
                onValueChange = { state.setPatternSize(size = it) },
                valueRange = 5f..50f,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painter = painterResource(R.drawable.ic_erase),
                contentDescription = stringResource(R.string.erase),
                modifier = Modifier
                    .size(32.dp)
                    .rounded(5.dp)
                    .background(if (state.currentPathProperty.eraseMode) Color.White else SemiTransparent)
                    .rippleClickable { state.toggleEraser() }
                    .padding(4.dp),
                tint = if (state.currentPathProperty.eraseMode) Color.Black else Color.White,
            )
        }

        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(top = 12.dp),
        ) {
            AnimatedVisibility(state.currentPathProperty.pattern is TextBrushPattern) {
                CenterRow(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                        .fillMaxWidth()
                        .rounded(10.dp)
                        .background(MintColor)
                        .clickable {
                            showEditTextBrushDialog = true
                        }
                        .padding(10.dp)
                ) {
                    Text(
                        text = (state.currentPathProperty.pattern as? TextBrushPattern)?.text.orEmpty().ifEmpty {
                            stringResource(R.string.click_to_edit)
                        },
                        style = medium14,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            AnimatedVisibility((state.currentPathProperty.pattern as? LineBrushPattern)?.type?.allowColor == true) {
                val pattern = (state.currentPathProperty.pattern as? LineBrushPattern) ?: return@AnimatedVisibility

                if (pattern.type.allowShadow) {
                    Column(
                        modifier = Modifier.padding(bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CenterRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            itemSpacing = 8.dp
                        ) {
                            Box {
                                Text(
                                    text = stringResource(R.string.color),
                                    style = regular12,
                                    color = Color.Black,
                                    modifier = Modifier
                                )
                                Text(
                                    text = stringResource(R.string.shadow),
                                    style = regular12,
                                    color = Color.Transparent,
                                    modifier = Modifier
                                )
                            }

                            BrushColorPicker(
                                selectedColor = pattern.color,
                                onColorSelected = {
                                    state.setLineBrushColor(it)
                                },
                                modifier = Modifier.weight(1f),
                                contentPaddingValues = PaddingValues(),
                            )
                        }

                        CenterRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            itemSpacing = 8.dp
                        ) {
                            Box {
                                Text(
                                    text = stringResource(R.string.color),
                                    style = regular12,
                                    color = Color.Transparent,
                                    modifier = Modifier
                                )
                                Text(
                                    text = stringResource(R.string.shadow),
                                    style = regular12,
                                    color = Color.Black,
                                    modifier = Modifier
                                )
                            }

                            BrushColorPicker(
                                selectedColor = pattern.shadowColor,
                                onColorSelected = {
                                    state.setLineShadowColor(it)
                                },
                                modifier = Modifier.weight(1f),
                                contentPaddingValues = PaddingValues(),
                            )
                        }
                    }
                } else if (pattern.type.allowColor) {
                    BrushColorPicker(
                        selectedColor = pattern.color,
                        onColorSelected = {
                            state.setLineBrushColor(it)
                        },
                        contentPaddingValues = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 5.dp)
            ) {
                item {
                    CenterBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .rounded(10.dp)
                            .border(
                                1.dp,
                                if (state.currentPathProperty.pattern is TextBrushPattern && !state.currentPathProperty.eraseMode) PrimaryColor
                                else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                state.currentPathProperty = state.currentPathProperty.copy(
                                    pattern = TextBrushPattern(context.getString(R.string.edit_text)),
                                    // Disable erase after confirm new text (Improve UX)
                                    eraseMode = false
                                )
                            }
                    ) {
                        Image(
                            painter = painterResource(com.photo.editor.overlaybrush.R.drawable.brush_text_pattern_preview),
                            contentDescription = stringResource(R.string.text),
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                    }
                }

                items(ImageBrushPattern.entries) {
                    CenterBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .rounded(10.dp)
                            .border(
                                1.dp,
                                if (state.currentPathProperty.pattern == it && !state.currentPathProperty.eraseMode) PrimaryColor
                                else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                state.setImagePattern(pattern = it)
                                // Disable erase after confirm new text (Improve UX)
                                state.currentPathProperty = state.currentPathProperty.copy(eraseMode = false)
                            }
                    ) {
                        AsyncImage(
                            model = getAssetAbsoluteFilePath(it.previewImageAsset),
                            contentDescription = it.name,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                    }
                }

                items(LineBrushType.entries) { lineBrushType ->
                    CenterBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .rounded(10.dp)
                            .border(
                                1.dp,
                                if ((state.currentPathProperty.pattern as? LineBrushPattern)?.type == lineBrushType
                                    && !state.currentPathProperty.eraseMode
                                ) PrimaryColor
                                else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                state.setLineBrushType(lineBrushType)
                                // Disable erase after confirm new text (Improve UX)
                                state.currentPathProperty = state.currentPathProperty.copy(eraseMode = false)
                            }
                    ) {

                        Image(
                            painter = painterResource(lineBrushType.previewId),
                            contentDescription = lineBrushType.name,
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                    }
                }
            }

            EditImageSubBottomBarTitle(
                title = stringResource(R.string.brush),
                onConfirmClick = {
                    uiState.saveState()
                    uiState.selectedEditOption = null
                },
                onCancelClick = {
                    uiState.restoreLastState()
                    uiState.selectedEditOption = null
                },
            )
        }
    }

    TextBrushEditorDialog(
        showDialog = showEditTextBrushDialog,
        onDismissRequest = { showEditTextBrushDialog = false },
        state = state,
    )
}

private val DefaultColors = listOf(
    0xFFFFFFFF, 0xFF000000, 0xFFEF4444, 0xFFF97316, 0xFFFACC15, 0xFF4ADE80, 0xFF2DD4BF, 0xFF3B82F6,
    0xFF6366F1, 0xFFEC4899, 0xFFF43F5E, 0xFFD946EF, 0xFF8B5CF6, 0xFF0EA5E9, 0xFF10B981, 0xFF84CC16,
)

@Composable
private fun BrushColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentPaddingValues: PaddingValues = PaddingValues(vertical = 14.dp, horizontal = 15.dp)
) {
    var showColorPickerDialog by remember { mutableStateOf(false) }

    LazyRow(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = contentPaddingValues
    ) {
        item {
            Image(
                painter = painterResource(com.photo.editor.overlaybrush.R.drawable.ic_color_picker),
                contentDescription = stringResource(R.string.color),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .size(20.dp)
                    .circle()
                    .then(
                        if (!TextDefaultColors.contains(selectedColor) && selectedColor != 0L)
                            Modifier.background(Color(selectedColor))
                        else Modifier
                    )
                    .clickable {
                        showColorPickerDialog = true
                    }
            )
        }

        items(DefaultColors) {
            CenterBox(
                modifier = Modifier
                    .size(20.dp)
                    .circle()
                    .background(Color(it))
                    .then(
                        if (it == 0xFFFFFFFF) Modifier.border(1.dp, Color.Black, CircleShape)
                        else Modifier
                    )
                    .clickable {
                        onColorSelected(it)
                    }
            ) {
                if (selectedColor == it) {
                    Spacer(
                        modifier = Modifier
                            .size(8.dp)
                            .circle()
                            .background(
                                if (it == 0xFF000000) Color.White
                                else if (it == 0xFFFFFFFF) Color.Black
                                else Color.White
                            )
                    )
                }
            }
        }
    }

    ColorPickerBottomSheet(
        showBottomSheet = showColorPickerDialog,
        onDismissRequest = { showColorPickerDialog = false },
        initialColor = Color(selectedColor),
        onColorChanged = {
            onColorSelected(it.toArgb().toLong())
        }
    )
}

@Preview
@Composable
private fun Preview() {
    BrushBottomBar(
        uiState = EditImageUiState()
    )
}

@Preview
@Composable
private fun Preview2() {
    BrushBottomBar(
        uiState = remember {
            EditImageUiState().apply {
                brushDrawingState.currentPathProperty = BrushPathProperties(pattern = TextBrushPattern("Hello World!"))
            }
        },
    )
}

@Preview
@Composable
private fun Preview3() {
    BrushBottomBar(
        uiState = remember {
            EditImageUiState().apply {
                brushDrawingState.currentPathProperty =
                    BrushPathProperties(
                        pattern = LineBrushPattern(
                            type = LineBrushType.SimpleSolid,
                            color = 0xFFFFFFFF
                        )
                    )
            }
        },
    )
}

@Preview
@Composable
private fun Preview4() {
    BrushBottomBar(
        uiState = remember {
            EditImageUiState().apply {
                brushDrawingState.currentPathProperty = BrushPathProperties(
                    pattern = LineBrushPattern(
                        type = LineBrushType.Neon,
                        color = 0xFFFFFFFF
                    )
                )
            }
        },
    )
}