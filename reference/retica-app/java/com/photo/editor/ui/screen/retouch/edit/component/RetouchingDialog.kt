package com.photo.editor.ui.screen.retouch.edit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.medium16

@Composable
fun RetouchingDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        ) {
            CenterColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .rounded(20.dp)
                    .background(Color.White)
                    .padding(20.dp),
                itemSpacing = 20.dp,
            ) {
                CircularProgressIndicator(color = Color.Black )

                Text(
                    text = stringResource(R.string.retouching),
                    style = medium16,
                    color = Color.Black,
                )
            }
        }
    }
}