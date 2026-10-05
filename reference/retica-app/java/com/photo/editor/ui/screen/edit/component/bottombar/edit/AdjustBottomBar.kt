package com.photo.editor.ui.screen.edit.component.bottombar.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageSubBottomBarTitle
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageValueSlider
import com.photo.editor.ui.screen.edit.enums.EditImageAdjustOption
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.extraBold10

@Suppress("NAME_SHADOWING")
@Composable
fun AdjustBottomBar(
    uiState: EditImageUiState,
    modifier: Modifier = Modifier
) {
    var selectedOption: EditImageAdjustOption? by rememberSaveable { mutableStateOf(EditImageAdjustOption.Lux) }

    Column(modifier = modifier) {
        AnimatedVisibility(selectedOption != null) {
            val selectedOption = selectedOption ?: return@AnimatedVisibility
            val value = uiState.adjustState.getAdjustValueByOption(selectedOption)

            EditImageValueSlider(
                value = value,
                onValueChange = { uiState.adjustState.setAdjustValueByOption(selectedOption, it) },
                valueRange = -1f..1f,
                title = stringResource(selectedOption.titleId),
                valueString = (value * 100f).toInt().toString()
            )
        }

        Column(
            modifier = Modifier
                .background(Color.White),
        ) {
            LazyRow(
                state = rememberLazyListState(EditImageAdjustOption.entries.indexOf(selectedOption).coerceAtLeast(0)),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(EditImageAdjustOption.entries) { option ->
                    CenterColumn(
                        modifier = Modifier
                            .size(64.dp)
                            .rounded(5.dp)
                            .then(
                                if (selectedOption == option) Modifier.background(Color.Black)
                                else Modifier.clickable {
                                    if (selectedOption == option) selectedOption = null
                                    else selectedOption = option
                                }
                            )
                    ) {
                        Icon(
                            painter = painterResource(option.iconId),
                            contentDescription = stringResource(option.titleId),
                            modifier = Modifier.size(32.dp),
                            tint = if (selectedOption == option) SubColor else Color.Black,
                        )

                        Text(
                            text = stringResource(option.titleId).uppercase(),
                            style = extraBold10,
                            color = if (selectedOption == option) SubColor else Color.Black,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(Int.MAX_VALUE)
                        )
                    }
                }
            }

            EditImageSubBottomBarTitle(
                title = stringResource(R.string.adjust),
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
}

@Preview
@Composable
private fun Preview() {
    AdjustBottomBar(
        uiState = EditImageUiState(),
    )
}