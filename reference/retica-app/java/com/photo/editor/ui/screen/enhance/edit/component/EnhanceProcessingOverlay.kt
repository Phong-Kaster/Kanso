package com.photo.editor.ui.screen.enhance.edit.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.R
import com.photo.editor.common.ui.foundation.CenterColumn
import com.photo.editor.common.ui.modifier.silentClickable
import com.photo.editor.common.ui.theme.extraBold14
import com.photo.editor.common.ui.theme.regular12

@Composable
fun EnhanceProcessingOverlay(
    progress: Int,
    modifier: Modifier = Modifier,
) {
    CenterColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.7f))
            .silentClickable { }
    ) {
        Image(
            painter = painterResource(R.drawable.enhance_loading),
            contentDescription = stringResource(R.string.enhancing),
            modifier = Modifier.size(40.dp),
        )

        Text(
            text = stringResource(R.string.ai_loading_title),
            style = extraBold14,
            color = Color.White,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = stringResource(R.string.ai_loading_subtitle),
            style = regular12,
            color = Color.White,
            modifier = Modifier
                .padding(top = 4.dp)
                .widthIn(max = 200.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun Preview() {
    EnhanceProcessingOverlay(20)
}