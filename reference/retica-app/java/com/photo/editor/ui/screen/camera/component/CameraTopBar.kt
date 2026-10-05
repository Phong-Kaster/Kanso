package com.photo.editor.ui.screen.camera.component

import androidx.camera.core.CameraSelector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import com.photo.editor.domain.model.AspectRatio
import com.photo.editor.domain.model.FlashMode
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.ui.screen.camera.CameraInteraction
import com.photo.editor.ui.screen.camera.CameraUiState
import com.photo.editor.common.ui.theme.regular14
import com.photo.editor.ui.component.topbar.BaseTopBar

@Composable
fun CameraTopBar(
    uiState: CameraUiState,
    interaction: CameraInteraction,
    modifier: Modifier = Modifier
) {
    Column {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Black.copy(0.3f))
        ) {
            BaseTopBar {  }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TopBarButton(
                    iconId = R.drawable.ic_back_2,
                    description = stringResource(R.string.back),
                    onClick = interaction.onBackClick,
                )

                Spacer(modifier = Modifier.weight(1f))

                TopBarButton(
                    iconId = uiState.aspectRatio.iconId,
                    description = stringResource(R.string.aspect_ratio),
                    onClick = {
                        uiState.aspectRatio = AspectRatio.entries[
                            (AspectRatio.entries.indexOf(uiState.aspectRatio) + 1) % AspectRatio.entries.size
                        ]
                    },
                )

                AnimatedVisibility (uiState.cameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                    TopBarButton(
                        iconId = uiState.flashMode.iconId,
                        description = stringResource(R.string.flash),
                        onClick = {
                            uiState.flashMode = FlashMode.entries[
                                (FlashMode.entries.indexOf(uiState.flashMode) + 1) % FlashMode.entries.size
                            ]
                        },
                    )
                }

                TopBarButton(
                    iconId = R.drawable.ic_switch_camera,
                    description = stringResource(R.string.switch_camera),
                    onClick = interaction.onSwitchCameraClick,
                )
            }
        }

        CenterBox(
            modifier = Modifier
                .padding(12.dp)
                .heightIn(32.dp)
                .widthIn(32.dp)
                .circle()
                .clickable {
                    var nextZoom = uiState.zoomRatio.toInt() + 1f
                    if (nextZoom > uiState.maximumZoomRatio) nextZoom = uiState.minZoomRatio
                    interaction.onCameraZoomChange(nextZoom)
                }
                .background(Color.Black)
        ) {
            Text(
                text = "%.01f".format(uiState.zoomRatio),
                style = regular14,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun TopBarButton(
    iconId: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterBox(
        modifier = modifier
            .size(48.dp)
            .circle()
            .clickable { onClick() }
    ) {
        Icon(
            painter = painterResource(iconId),
            contentDescription = description,
            modifier = Modifier.size(24.dp),
            tint = Color.White
        )
    }
}

@Preview
@Composable
private fun Preview() {
    CameraTopBar(
        uiState = CameraUiState(),
        interaction = CameraInteraction(),
    )
}