package com.photo.editor.ui.screen.edit.component.bottombar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.theme.medium18
import com.photo.editor.common.util.logEvent
import com.photo.editor.monetization.AdsProvider
import com.photo.editor.monetization.BaseBannerAdContent
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import com.photo.editor.ui.screen.edit.component.bottombar.beauty.BeautyBottomBar
import com.photo.editor.ui.screen.edit.component.bottombar.edit.EditBottomBar
import com.photo.editor.ui.screen.edit.enums.EditImageEditOption

@Composable
fun EditImageBottomBar(
    uiState: EditImageUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                uiState.showOriginalImageFlow.collectAsState().value,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = stringResource(R.string.original),
                    style = medium18.copy(shadow = Shadow(Color.Black.copy(0.5f), Offset.Zero, 4f)),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (uiState.selectedEditOption != EditImageEditOption.Crop) {
                CenterBox(
                    modifier = Modifier
                        .size(32.dp)
                        .circle()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                uiState.showOriginalImageFlow.value = true

                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })

                                uiState.showOriginalImageFlow.value = false
                            }
                        }
                        .background(Color(0x80252525))
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_show_original),
                        contentDescription = stringResource(R.string.show_original),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        AnimatedContent(
            targetState = uiState.isSelectedEditOption,
            modifier = Modifier,
        ) { isEdit ->
            if (isEdit) {
                LaunchedEffect(Unit) {
                    logEvent("edit_adjust_scr")
                }

                EditBottomBar(
                    uiState = uiState,
                    onBeautyClick = {
                        uiState.isSelectedEditOption = false
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    logEvent("edit_beauty_scr")
                }

                BeautyBottomBar(
                    uiState = uiState,
                    onAdjustClick = { uiState.isSelectedEditOption = true }
                )
            }
        }

        BaseBannerAdContent(AdsProvider.bannerAll)
    }
}

@Preview
@Composable
private fun Preview() {
    EditImageBottomBar(
        uiState = EditImageUiState(),
        modifier = Modifier
    )
}