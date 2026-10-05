package com.photo.editor.ui.screen.removebg.edit.component

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
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.bold12

@Composable
fun RemoveBgEditMainButtons(
    onSolidColorClick: () -> Unit,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        itemSpacing = 10.dp,
    ) {
        CenterRow(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .rounded(5.dp)
                .background(Color.White)
                .clickable { onSolidColorClick() },
            itemSpacing = 8.dp
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_remove_bg_add_color),
                contentDescription = stringResource(R.string.add_color),
                modifier = Modifier.size(24.dp),
                tint = Color.Black,
            )

            Text(
                text = stringResource(R.string.add_color).uppercase(),
                style = bold12,
                color = Color.Black,
            )
        }

        CenterRow(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .rounded(5.dp)
                .background(Color.Black)
                .clickable { onImageClick() },
            itemSpacing = 8.dp
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_remove_bg_image),
                contentDescription = stringResource(R.string.image),
                modifier = Modifier.size(24.dp),
                tint = Color.White,
            )

            Text(
                text = stringResource(R.string.add_image).uppercase(),
                style = bold12,
                color = Color.White,
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    RemoveBgEditMainButtons({}, {})
}