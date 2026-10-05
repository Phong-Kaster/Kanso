package com.photo.editor.ui.screen.edit.component.bottombar.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.extraBold10
import com.photo.editor.domain.model.CropRatio
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageSubBottomBarTitle

@Composable
fun CropBottomBar(
    uiState: EditImageUiState,
    modifier: Modifier = Modifier
) {
    val cropState = uiState.cropState
    val cropRatio = cropState.cropRatio

    Column(
        modifier = modifier.background(Color.White),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            items(CropRatio.entries) { ratio ->
                CenterColumn(
                    modifier = Modifier
                        .size(60.dp)
                        .rounded(5.dp)
                        .then(
                            if (cropRatio == ratio) Modifier.background(Color.Black)
                            else Modifier.clickable {
                                cropState.cropRatio = ratio
                            }
                        )
                ) {
                    Icon(
                        painter = painterResource(ratio.iconId),
                        contentDescription = stringResource(ratio.titleId),
                        modifier = Modifier.size(32.dp),
                        tint = if (cropRatio == ratio) SubColor else Color.Black,
                    )

                    Text(
                        text = stringResource(ratio.titleId).uppercase(),
                        style = extraBold10,
                        color = if (cropRatio == ratio) SubColor else Color.Black,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(Int.MAX_VALUE)
                    )
                }
            }
        }

        EditImageSubBottomBarTitle(
            title = stringResource(R.string.crop),
            onConfirmClick = {
                cropState.confirmCropRegion()
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
    CropBottomBar(
        uiState = EditImageUiState()
    )
}