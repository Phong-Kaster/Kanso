package com.photo.editor.ui.component.shimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.valentinilk.shimmer.shimmer

@Composable
fun SimpleShimmer(
    content: @Composable BoxScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shimmer()
            .background(Color.LightGray),
    ) {
        content()
    }
}