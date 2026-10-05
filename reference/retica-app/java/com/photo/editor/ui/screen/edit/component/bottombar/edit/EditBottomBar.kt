package com.photo.editor.ui.screen.edit.component.bottombar.edit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.modifier.silentClickable
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.appTextStyle
import com.photo.editor.common.ui.theme.medium12
import com.photo.editor.common.util.logEvent
import com.photo.editor.ui.screen.edit.common.EditImageUiState
import com.photo.editor.ui.screen.edit.component.bottombar.edit.color.ColorBottomBar
import com.photo.editor.ui.screen.edit.enums.EditImageEditOption

@Composable
fun EditBottomBar(
    uiState: EditImageUiState,
    onBeautyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    BackHandler(uiState.selectedEditOption != null) {
        uiState.selectedEditOption = null
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            uiState.selectedEditOption == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(bottom = 12.dp, top = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .horizontalScroll(scrollState),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(modifier = Modifier)

                    EditImageEditOption.entries.forEach { option ->
                        CenterColumn(
                            modifier = Modifier
                                .size(64.dp)
                                .rounded(5.dp)
                                .clickable {
                                    val eventSuffix = option.name.lowercase()
                                    logEvent("edit_adjust_scr_${eventSuffix}_click")

                                    when (option) {
                                        EditImageEditOption.Text -> uiState.overlayState.addTextOverlay()
                                        else -> uiState.selectedEditOption = option
                                    }
                                }
                        ) {
                            Icon(
                                painter = painterResource(option.iconId),
                                contentDescription = stringResource(option.titleId),
                                modifier = Modifier.size(32.dp),
                                tint = Color.Black,
                            )

                            Text(
                                text = stringResource(option.titleId).uppercase(),
                                style = appTextStyle(8, 700),
                                color = Color.Black,
                                modifier =Modifier.basicMarquee(Int.MAX_VALUE)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                }


                CenterColumn(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp, 0.dp, 0.dp, 10.dp))
                        .background(AlternativeColor)
                        .clickable { onBeautyClick() }
                        .padding(start = 2.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_beauty),
                        contentDescription = stringResource(R.string.beauty),
                        modifier = Modifier.size(32.dp),
                        tint = Color.Black,
                    )

                    Text(
                        text = stringResource(R.string.beauty),
                        style = medium12,
                        color = Color.Black,
                        modifier =Modifier.basicMarquee(Int.MAX_VALUE)

                    )
                }
            }
        }

        AnimatedContent(
            targetState = uiState.selectedEditOption,
            transitionSpec = {
                fadeIn() + expandVertically() togetherWith fadeOut()
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) { option ->
            when (option) {
                EditImageEditOption.Filter -> {
                    FilterBottomBar(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .silentClickable { }
                    )
                }

                EditImageEditOption.Adjust -> {
                    AdjustBottomBar(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .silentClickable { }
                    )
                }

                EditImageEditOption.Crop -> {
                    CropBottomBar(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .silentClickable { }
                    )
                }

                EditImageEditOption.Rotate -> {
                    RotateBottomBar(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .silentClickable { }
                    )
                }

                EditImageEditOption.Color -> {
                    ColorBottomBar(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .silentClickable { }
                    )
                }

                EditImageEditOption.Sticker -> {
                    StickerBottomBar(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .silentClickable { }
                    )
                }

                EditImageEditOption.Brush -> {
                    BrushBottomBar(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .silentClickable { }
                    )
                }

                EditImageEditOption.Frame -> {
                    FrameBottomBar(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .silentClickable { }
                    )
                }

                else -> {
                    Spacer(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.fillMaxSize()) {
        EditBottomBar(
            uiState = EditImageUiState(),
            onBeautyClick = {},
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}