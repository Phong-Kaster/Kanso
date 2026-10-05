package com.photo.editor.ui.screen.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.theme.AlternativeColor

@Composable
fun HomePagerIndicator(
    pageIndex: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    val inactiveItemModifier = Modifier
        .size(5.dp)
        .circle()
        .background(AlternativeColor)

    CenterRow(modifier = modifier, itemSpacing = 5.dp) {
        repeat(pageIndex) {
            Spacer(modifier = inactiveItemModifier)
        }

        Spacer(
            modifier = Modifier
                .width(15.dp)
                .height(5.dp)
                .circle()
                .background(Color.Black)
        )

        repeat(totalPages - pageIndex - 1) {
            Spacer(modifier = inactiveItemModifier)
        }
    }
}

@Preview
@Composable
private fun Preview() {
    HomePagerIndicator(
        pageIndex = 0,
        totalPages = 5,
    )
}