package com.photo.editor.ui.lockscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.ui.base.BaseScreen

@Composable
fun LockscreenBaseScreen(
    content: @Composable () -> Unit,
) {
    BaseScreen(
        backgroundColor = Color.Unspecified,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            DigitalClock(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(BiasAlignment(0f, -0.8f))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
            ) {
                content()
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    LockscreenBaseScreen({})
}