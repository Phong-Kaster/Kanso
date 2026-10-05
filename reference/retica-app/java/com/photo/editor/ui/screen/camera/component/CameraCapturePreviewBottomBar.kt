package com.photo.editor.ui.screen.camera.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.regular12

@Composable
fun CameraCapturePreviewBottomBar(
    onConfirmCapturedImage: () -> Unit,
    onDismissCapturedImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterRow(
        modifier = modifier
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
                    .clickable { onDismissCapturedImage() }
                    .padding(4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.cancel),
                    modifier = Modifier
                        .size(32.dp),
                    tint = Color.White,
                )

                Text(
                    text = stringResource(R.string.cancel),
                    style = regular12,
                    color = Color.White,
                )
            }
        }

        CenterBox(
            modifier = Modifier
                .weight(1f)
        ) {

        }

        CenterBox(
            modifier = Modifier
                .weight(1f)
        ) {
            CenterColumn(
                modifier = Modifier
                    .rounded(5.dp)
                    .clickable { onConfirmCapturedImage() }
                    .padding(4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.confirm),
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp),
                    tint = Color.White,
                )

                Text(
                    text = stringResource(R.string.confirm),
                    style = regular12,
                    color = Color.White,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    CameraCapturePreviewBottomBar(
        onConfirmCapturedImage = { },
        onDismissCapturedImage = {},
    )
}