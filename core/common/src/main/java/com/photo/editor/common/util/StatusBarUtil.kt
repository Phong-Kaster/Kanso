package com.photo.editor.common.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private var maximumStatusBarHeight by mutableStateOf(0.dp)

@Composable
fun Modifier.staticStatusBarPadding(): Modifier = this.composed {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var backgroundColor by remember { mutableStateOf(Color.Transparent) }

    // When status bar height changes, if it's invisible,
    // delay for a moment before setting its color to Black to avoid blinking when setting it immediately
    LaunchedEffect(statusBarHeight) {
        if (statusBarHeight == 0.dp) {
            delay(300)
            backgroundColor = Color.Black
        } else {
            backgroundColor = Color.Transparent
        }
    }

    LaunchedEffect(statusBarHeight) {
        if (statusBarHeight > maximumStatusBarHeight) maximumStatusBarHeight = statusBarHeight
    }

    Modifier
        .background(backgroundColor)
        .padding(top = maximumStatusBarHeight)
}