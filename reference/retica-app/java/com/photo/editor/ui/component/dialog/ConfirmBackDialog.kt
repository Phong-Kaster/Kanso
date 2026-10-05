package com.photo.editor.ui.component.dialog

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.extraBold16
import com.photo.editor.common.ui.theme.medium14
import com.photo.editor.common.ui.theme.regular14
import com.photo.editor.ui.component.button.SimpleButton

@Composable
fun ConfirmBackDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmBack: () -> Unit,
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismissRequest,
        ) {
            ConfirmBackDialogContent(
                onContinueClick = onDismissRequest,
                onExitClick = onConfirmBack,
            )
        }
    }
}

@Composable
private fun ConfirmBackDialogContent(
    onContinueClick: () -> Unit,
    onExitClick: () -> Unit,
) {
    CenterColumn(
        modifier = Modifier
            .fillMaxWidth()
            .rounded(16.dp)
            .background(Color.White)
            .padding(24.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            modifier = Modifier.size(45.dp),
            tint = Color.Black,
        )

        Text(
            text = stringResource(R.string.confirm_back_title),
            style = extraBold16,
            color = Color.Black,
            modifier = Modifier.padding(top = 24.dp)
        )


        Text(
            text = stringResource(R.string.confirm_back_description),
            style = regular14,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )

        SimpleButton(
            title = stringResource(R.string.continue_to_edit),
            onClick = onContinueClick,
            modifier = Modifier.run {
                padding(top = 24.dp)
                    .fillMaxWidth()
            }
        )

        CenterBox(
            modifier = Modifier
                .padding(top = 4.dp)
                .height(40.dp)
                .fillMaxWidth()
                .rounded(10.dp)
                .clickable { onExitClick() }
        ) {
            Text(
                text = stringResource(R.string.yes_discard),
                style = medium14,
                color = Color(0xFF808080),
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ConfirmBackDialogContent(
        onContinueClick = {},
        onExitClick = {},
    )
}