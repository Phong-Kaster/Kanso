package com.photo.editor.ui.screen.retouch.edit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.BaseSlider
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.SemiTransparent
import com.photo.editor.common.ui.theme.bold12
import com.photo.editor.common.ui.theme.regular14
import com.photo.editor.common.util.logEvent
import com.photo.editor.ui.screen.retouch.common.RetouchEditInteraction
import com.photo.editor.ui.screen.retouch.common.RetouchUiState
import com.photo.editor.ui.screen.retouch.model.RetouchMode

@Composable
fun RetouchEditMainButtons(
    uiState: RetouchUiState,
    modifier: Modifier = Modifier,
    interaction: RetouchEditInteraction = RetouchEditInteraction(),
) {
    Column(
        modifier = modifier
    ) {
        CenterRow(
            modifier = Modifier
                .background(Color.Black.copy(0.7f))
                .padding(horizontal = 10.dp),
            itemSpacing = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        vertical = 5.dp
                    ), verticalArrangement = Arrangement.Center
            ) {
                if (uiState.retouchMode == RetouchMode.Draw) {
                    Text(
                        text = stringResource(R.string.brush_size),
                        style = regular14,
                        color = Color.White,
                    )
                }

                CenterRow(itemSpacing = 5.dp) {
                    if (uiState.retouchMode == RetouchMode.Draw) {
                        BaseSlider(
                            value = uiState.brushSize,
                            onValueChange = {
                                interaction.onBrushSizeChanged(it)
                            },
                            valueRange = 5f..80f,
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.retouch_touch_tutorial),
                            style = regular14,
                            color = Color.White,
                            modifier = Modifier
                                .weight(1f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            CenterRow(itemSpacing = 12.dp) {

                Icon(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = stringResource(R.string.undo),
                    modifier = Modifier
                        .size(32.dp)
                        .rounded(5.dp)
                        .background(SemiTransparent)
                        .rippleClickable(enabled = uiState.canUndo) { interaction.onUndoClick() }
                        .padding(4.dp),
                    tint = if (uiState.canUndo) Color.White else Color.Black,
                )

                Icon(
                    painter = painterResource(R.drawable.ic_redo),
                    contentDescription = stringResource(R.string.redo),
                    modifier = Modifier
                        .size(32.dp)
                        .rounded(5.dp)
                        .background(SemiTransparent)
                        .rippleClickable(enabled = uiState.canRedo) { interaction.onRedoClick() }
                        .padding(4.dp),
                    tint = if (uiState.canRedo) Color.White else Color.Black,
                )
            }
        }


        CenterRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 10.dp),
            itemSpacing = 10.dp
        ) {
            CenterRow(
                modifier = Modifier
                    .height(54.dp)
                    .weight(1f)
                    .rounded(10.dp)
                    .background(if (uiState.retouchMode == RetouchMode.Draw) Color.Black else Color.White)
                    .clickable {
                        logEvent("ai_retouch_adjust_scr_brush_click")
                        uiState.retouchMode = RetouchMode.Draw
                    },
                itemSpacing = 2.dp
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_brush),
                    contentDescription = stringResource(R.string.brush),
                    modifier = Modifier.size(32.dp),
                    tint = if (uiState.retouchMode == RetouchMode.Draw) Color.White else Color.Black
                )

                Text(
                    text = stringResource(R.string.brush).uppercase(),
                    style = bold12,
                    color = if (uiState.retouchMode == RetouchMode.Draw) Color.White else Color.Black
                )
            }

            CenterRow(
                modifier = Modifier
                    .height(54.dp)
                    .weight(1f)
                    .rounded(10.dp)
                    .background(if (uiState.retouchMode == RetouchMode.Touch) Color.Black else Color.White)
                    .clickable {
                        logEvent("ai_retouch_adjust_scr_restore_click")
                        uiState.retouchMode = RetouchMode.Touch
                    }
            ) {
                Icon(
                    painter = painterResource(
                        R.drawable.ic_brush
                    ),
                    contentDescription = stringResource(R.string.touch),
                    modifier = Modifier.size(32.dp),
                    tint = if (uiState.retouchMode == RetouchMode.Touch) Color.White else Color.Black
                )
                Text(
                    text = stringResource(R.string.touch).uppercase(),
                    style = bold12,
                    color = if (uiState.retouchMode == RetouchMode.Touch) Color.White else Color.Black
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    RetouchEditMainButtons(uiState = RetouchUiState())
}