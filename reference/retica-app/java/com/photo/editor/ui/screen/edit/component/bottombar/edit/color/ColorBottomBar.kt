package com.photo.editor.ui.screen.edit.component.bottombar.edit.color

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.ui.theme.extraBold10
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageSubBottomBarTitle
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageValueSlider
import com.photo.editor.ui.screen.edit.enums.EditImageColorOption

@Composable
fun ColorBottomBar(
    uiState: EditImageUiState,
    modifier: Modifier = Modifier
) {
    var selectedOption: EditImageColorOption? by rememberSaveable { mutableStateOf(null) }

    Column(modifier = modifier) {
        AnimatedContent(
            selectedOption,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxWidth()
        ) { option ->
            when (option) {
                EditImageColorOption.Curve -> {
                    ColorCurveEditor(
                        colorState = uiState.colorState,
                    )
                }

                EditImageColorOption.HSL -> {
                    ColorHSLEditor(
                        colorState = uiState.colorState,
                    )
                }

                EditImageColorOption.RGB -> {
                    ColorRGBEditor(
                        colorState = uiState.colorState,
                    )
                }

                else -> {
                    Spacer(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Column(
            modifier = Modifier.background(Color.White),
        ) {
            CenterRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                EditImageColorOption.entries.forEach { option ->
                    CenterColumn(
                        modifier = Modifier
                            .size(60.dp)
                            .rounded(5.dp)
                            .background(if (selectedOption == option) Color.Black else Color.Transparent)
                            .clickable {
                                if (selectedOption == option) selectedOption = null
                                else selectedOption = option
                            }
                    ) {
                        Image(
                            painter = painterResource(option.iconId),
                            contentDescription = stringResource(option.titleId),
                            modifier = Modifier.size(32.dp),
                            colorFilter = if (selectedOption == option) ColorFilter.tint(SubColor) else null,
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
                title = stringResource(R.string.color),
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
    ColorBottomBar(
        uiState = EditImageUiState()
    )
}