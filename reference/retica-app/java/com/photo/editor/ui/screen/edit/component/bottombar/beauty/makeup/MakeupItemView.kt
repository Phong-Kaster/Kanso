package com.photo.editor.ui.screen.edit.component.bottombar.beauty.makeup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.AlternativeColor
import com.photo.editor.common.ui.theme.medium12

@Composable
fun MakeupItemView(
    image: @Composable BoxScope.() -> Unit,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(60.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(60.dp)
                .rounded(5.dp)
                .clickable { onClick() }
        ) {
            image()

            if (isSelected) {
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .background(AlternativeColor.copy(0.7f))
                )

                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.Center),
                    tint = Color.Black,
                )
            }
        }

        Text(
            text = title,
            style = medium12,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(Int.MAX_VALUE)
        )
    }
}

@Composable
fun MakeupNoneItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        CenterColumn(
            modifier = Modifier.width(40.dp),
            itemSpacing = 4.dp
        ) {
            Image(
                painter = painterResource(R.drawable.empty_slash),
                contentDescription = null,
                modifier = Modifier
                    .width(40.dp)
                    .height(60.dp)
                    .rounded(5.dp)
                    .clickable { onClick() }
                    .background(AlternativeColor)
            )

            Text(
                text = stringResource(R.string.none),
                style = medium12,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(Int.MAX_VALUE)
            )
        }

        Spacer(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .height(60.dp)
                .width(1.dp)
                .background(Color(0xFFEAEAEA))
        )
    }
}

@Preview
@Composable
private fun Preview() {
    Row {
        MakeupNoneItem({})

        MakeupItemView(
            image = {
                Image(
                    painter = painterResource(R.drawable.app_icon),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            title = stringResource(R.string.lipstick),
            isSelected = true,
            onClick = {},
        )
    }
}