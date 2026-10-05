package com.photo.editor.ui.screen.edit.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.theme.SemiTransparent
import com.photo.editor.common.ui.theme.bold14
import com.photo.editor.ui.component.topbar.BaseTopBar
import com.photo.editor.ui.screen.edit.EditImageInteraction
import com.photo.editor.ui.screen.edit.common.EditImageUiState

@Composable
fun EditImageTopBar(
    uiState: EditImageUiState = EditImageUiState(),
    interaction: EditImageInteraction = EditImageInteraction(),
    modifier: Modifier = Modifier
) = BaseTopBar(modifier = modifier) {
    // Do not show top bar if user is using an edit function
    val isUsingEdit = (uiState.isSelectedEditOption && uiState.selectedEditOption != null)
    // Do not show top bar if user is using a beauty function
    val isUsingBeauty = (!uiState.isSelectedEditOption && uiState.selectedBeautyOption != null)
    // Do not show top bar if user is editing overlay
    val isEditingOverlay = uiState.overlayState.focusedOverlay != null

    AnimatedVisibility(
        !isUsingEdit && !isUsingBeauty && !isEditingOverlay,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            CenterBox(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(32.dp)
                    .circle()
                    .clickable { interaction.onBackClick() }
                    .background(SemiTransparent)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back_2),
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(20.dp),
                    tint = Color.White,
                )
            }

            CenterRow(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(32.dp)
                    .circle()
                    .animateContentSize(),
                itemSpacing = 6.dp
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = stringResource(R.string.undo),
                    modifier = Modifier
                        .size(32.dp)
                        .background(SemiTransparent)
                        .rippleClickable(enabled = uiState.canUndo) { uiState.undo() }
                        .padding(4.dp),
                    tint = if (uiState.canUndo) Color.White else Color.DarkGray,
                )

                Icon(
                    painter = painterResource(R.drawable.ic_redo),
                    contentDescription = stringResource(R.string.redo),
                    modifier = Modifier
                        .size(32.dp)
                        .background(SemiTransparent)
                        .rippleClickable(enabled = uiState.canRedo) { uiState.redo() }
                        .padding(4.dp),
                    tint = if (uiState.canRedo) Color.White else Color.DarkGray,
                )

                CenterBox(
                    modifier = Modifier
                        .height(32.dp)
                        .background(SemiTransparent)
                        .clickable { interaction.onSaveClick() }
                        .padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        style = bold14,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    EditImageTopBar()
}