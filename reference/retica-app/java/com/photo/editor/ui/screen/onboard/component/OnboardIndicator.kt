package com.photo.editor.ui.screen.onboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.bold18
import com.photo.editor.common.ui.theme.medium14
import com.photo.editor.common.ui.theme.medium16

@Composable
fun OnboardIndicator(
    pageIndex: Int,
    totalPage: Int,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    inactiveColor: Color = Color(0xFF808080),
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageIndex) {
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .circle()
                    .background(inactiveColor)
            )
        }

        Spacer(
            modifier = Modifier
                .width(40.dp)
                .height(8.dp)
                .circle()
                .background(Color.Black)
        )

        repeat(totalPage - pageIndex - 1) {
            Spacer(
                modifier = Modifier
                    .size(8.dp)
                    .circle()
                    .background(inactiveColor)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(if (pageIndex == totalPage - 1) R.string.lets_start else R.string.next),
            style = bold18,
            color = Color.Black,
            modifier = Modifier
                .clickable { onNextClick() }
                .rounded(4.dp)
                .padding(4.dp)
        )
    }
}

@Preview
@Composable
private fun Preview() {
    OnboardIndicator(
        pageIndex = 0,
        totalPage = 3,
        onNextClick = {},
    )
}