package com.photo.editor.ui.screen.edit.component.bottombar.beauty

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.photo.editor.R
import com.photo.editor.common.util.logEvent
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageSubBottomBarTitle
import com.photo.editor.ui.screen.edit.component.bottombar.EditImageValueSlider
import com.photo.editor.ui.screen.edit.component.bottombar.beauty.makeup.MakeupBottomBar
import com.photo.editor.ui.screen.edit.enums.EditImageBeautyOption

@Composable
fun BeautyBottomBar(
    uiState: EditImageUiState,
    onAdjustClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val beautyState = uiState.beautyState

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedContent(
            targetState = uiState.selectedBeautyOption,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { option ->
            when (option) {
                null -> {
                    BeautyMainBottomBar(
                        // Avoid highlighting make up option because we show make up as separate bottom bar
                        selectedOption = if (uiState.selectedBeautyOption == EditImageBeautyOption.Makeup) null else uiState.selectedBeautyOption,
                        onOptionSelected = {
                            val eventSuffix = it.name.lowercase()
                            logEvent("edit_beauty_scr_${eventSuffix}_click")

                            if (uiState.selectedBeautyOption == it) uiState.selectedBeautyOption = null
                            else uiState.selectedBeautyOption = it
                        },
                        onAdjustClick = onAdjustClick,
                    )
                }

                EditImageBeautyOption.Makeup -> {
                    MakeupBottomBar(
                        beautyState = beautyState,
                        onConfirmClick = {
                            uiState.saveState()
                            uiState.selectedBeautyOption = null
                        },
                        onCancelClick = {
                            uiState.restoreLastState()
                            uiState.selectedBeautyOption = null
                        }
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val selectedOption = uiState.selectedBeautyOption ?: return@Column
                        val value = beautyState.getBeautyValueByOption(selectedOption)

                        EditImageValueSlider(
                            value = value,
                            onValueChange = {
                                beautyState.setBeautyValueByOption(selectedOption, it)
                            },
                            title = stringResource(R.string.intensity),
                            valueString = (value * 100f).toInt().toString()
                        )

                        EditImageSubBottomBarTitle(
                            title = stringResource(selectedOption.titleId),
                            onConfirmClick = {
                                uiState.saveState()
                                uiState.selectedBeautyOption = null
                            },
                            onCancelClick = {
                                uiState.restoreLastState()
                                uiState.selectedBeautyOption = null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    BeautyBottomBar(
        uiState = EditImageUiState(),
        onAdjustClick = {},
    )
}