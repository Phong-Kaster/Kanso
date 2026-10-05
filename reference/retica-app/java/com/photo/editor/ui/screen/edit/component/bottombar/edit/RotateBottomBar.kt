package com.photo.editor.ui.screen.edit.component.bottombar.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.extraBold10
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageSubBottomBarTitle
import com.photo.editor.ui.screen.edit.enums.EditImageRotateOption

@Composable
fun RotateBottomBar(
    uiState: EditImageUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(Color.White),
    ) {
        CenterRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            itemSpacing = 10.dp
        ) {
            EditImageRotateOption.entries.forEach { option ->
                CenterColumn(
                    modifier = Modifier
                        .size(60.dp)
                        .rounded(5.dp)
                        .clickable { uiState.cropState.rotateWithOption(option) }
                ) {
                    Image(
                        painter = painterResource(option.iconId),
                        contentDescription = stringResource(option.titleId),
                        modifier = Modifier.size(32.dp),
                    )

                    Text(
                        text = stringResource(option.titleId).uppercase(),
                        style = extraBold10,
                        color = Color.Black,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(Int.MAX_VALUE)
                    )
                }
            }
        }

        EditImageSubBottomBarTitle(
            title = stringResource(R.string.rotate),
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

@Preview
@Composable
private fun Preview() {
    RotateBottomBar(
        uiState = EditImageUiState()
    )
}