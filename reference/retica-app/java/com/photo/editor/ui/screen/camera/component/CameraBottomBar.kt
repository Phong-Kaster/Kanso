package com.photo.editor.ui.screen.camera.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.ui.screen.camera.CameraUiState
import com.photo.editor.common.ui.theme.regular12
import com.photo.gallery.model.GalleryImage

@Composable
fun CameraBottomBar(
    uiState: CameraUiState = CameraUiState(),
    onCaptureClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    lastGalleryImage: GalleryImage? = null,
    modifier: Modifier = Modifier,
) {
    var showFilterBottomBar by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        AnimatedVisibility(
            !showFilterBottomBar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            CenterRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Black.copy(0.3f)),
            ) {
                CenterBox(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    CenterColumn(
                        modifier = Modifier
                            .rounded(5.dp)
                            .clickable { onLibraryClick() }
                            .padding(4.dp)
                    ) {
                        SubcomposeAsyncImage(
                            model = lastGalleryImage?.uri,
                            contentDescription = null,
                            loading = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_collection),
                                    contentDescription = stringResource(R.string.library),
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White,
                                )
                            },
                            error = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_collection),
                                    contentDescription = stringResource(R.string.library),
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White,
                                )
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .rounded(4.dp)
                                .padding(6.dp)
                        )

                        Text(
                            text = stringResource(R.string.library),
                            style = regular12,
                            color = Color.White,
                        )
                    }
                }

                CenterBox(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    CenterBox(
                        modifier = Modifier
                            .size(46.dp)
                            .circle()
                            .background(Color.White)
                            .clickable { onCaptureClick() }
                    ) {
                        Spacer(
                            modifier = Modifier
                                .size(16.dp)
                                .border(1.dp, Color.Black, CircleShape)
                        )
                    }
                }

                CenterBox(
                    modifier = Modifier
                        .weight(1f)
                ) {
                    CenterColumn(
                        modifier = Modifier
                            .rounded(5.dp)
                            .clickable { showFilterBottomBar = true }
                            .padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_filter),
                            contentDescription = stringResource(R.string.filter),
                            modifier = Modifier
                                .size(32.dp),
                            tint = Color.White,
                        )

                        Text(
                            text = stringResource(R.string.filter),
                            style = regular12,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            showFilterBottomBar,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            CameraFilterBottomBar(
                uiState = uiState,
                onCloseClick = { showFilterBottomBar = false },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    CameraBottomBar()
}