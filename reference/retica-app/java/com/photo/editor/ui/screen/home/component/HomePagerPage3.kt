package com.photo.editor.ui.screen.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.photo.editor.R
import com.photo.editor.common.ui.theme.HomePagerOverlayGradient
import com.photo.editor.common.ui.theme.appTextStyle
import com.photo.editor.common.ui.theme.extraBold12

@Composable
fun HomePagerPage3(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.home_pager_3),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
        )

        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .aspectRatio(1.9f)
                .background(HomePagerOverlayGradient)
        )
    }
}

@Composable
fun HomePagerPage3Text(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = stringResource(R.string.home_pager_3_1).uppercase(),
            style = extraBold12.copy(
                letterSpacing = 1.sp,
                shadow = Shadow(color = Color.Black.copy(0.5f), blurRadius = 20f)
            ),
            color = Color.White,
        )

        Text(
            text = stringResource(R.string.home_pager_3_2),
            style = appTextStyle(32, 700).copy(shadow = Shadow(color = Color.Black.copy(0.5f), blurRadius = 20f)),
            color = Color.White,
        )
    }
}

@Preview
@Composable
private fun Preview() {
    HomePagerPage3(modifier = Modifier.aspectRatio(0.7f))
    HomePagerPage3Text()
}