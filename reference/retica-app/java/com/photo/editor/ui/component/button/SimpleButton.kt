package com.photo.editor.ui.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.photo.editor.common.ui.foundation.CenterRow
import com.photo.editor.common.ui.modifier.rounded
import com.photo.editor.common.ui.theme.extraBold14

@Composable
fun SimpleButton(
    title: String,
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    height: Dp = 42.dp,
    backgroundBrush: Brush = SolidColor(Color.Black),
    shape: Shape = rounded(10.dp),
) {
    CenterRow(
        modifier = modifier
            .height(height)
            .clip(shape)
            .clickable(enabled) { onClick() }
            .background(backgroundBrush)
            .padding(horizontal = 16.dp)
    ) {
        Text(text = title, style = extraBold14, color = Color.White)
    }
}

@Preview
@Composable
private fun Preview() {
    Column {
        SimpleButton(title = "Hello World!", modifier = Modifier.fillMaxWidth(), onClick = {})
    }
}