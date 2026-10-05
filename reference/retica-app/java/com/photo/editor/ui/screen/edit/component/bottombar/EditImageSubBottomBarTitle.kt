package com.photo.editor.ui.screen.edit.component.bottombar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import com.photo.editor.common.ui.foundation.CenterBox
import com.photo.editor.common.ui.modifier.rippleClickable
import com.photo.editor.common.ui.theme.extraBold12

@Composable
fun EditImageSubBottomBarTitle(
    title: String,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.White)
    ) {
        CenterBox(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(50.dp)
                .rippleClickable(25.dp) { onCancelClick() }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.close),
                tint = Color.Black,
                modifier = Modifier.size(32.dp),
            )
        }

        Text(
            text = title.uppercase(),
            style = extraBold12,
            modifier = Modifier.align(Alignment.Center)
        )

        CenterBox(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp, 0.dp))
                .background(Color(0xFFEBEDF0))
                .clickable { onConfirmClick() }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.confirm),
                tint = Color.Black,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    EditImageSubBottomBarTitle(
        title = stringResource(R.string.app_name),
        onConfirmClick = {},
        onCancelClick = {}
    )
}