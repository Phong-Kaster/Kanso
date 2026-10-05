package com.photo.editor.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.photo.editor.common.ui.modifier.silentClickable
import com.photo.editor.common.ui.theme.SubColor
import com.photo.editor.common.util.tryWithoutCatch

@Composable
fun BaseScreen(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    backgroundColor: Color = Color.White,
    contentWindowInsets: WindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    background: @Composable (BoxScope.() -> Unit) = {},
    content: @Composable BoxScope.() -> Unit,
) {
    BaseScreenBrush(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        backgroundBrush = SolidColor(backgroundColor),
        contentWindowInsets = contentWindowInsets,
        background = background,
        content = content
    )
}

@Composable
fun BaseScreenBrush(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    backgroundBrush: Brush = SolidColor(Color.White),
    contentWindowInsets: WindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    background: @Composable (BoxScope.() -> Unit) = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.background(backgroundBrush)) {
        background()

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { tryWithoutCatch { focusManager.clearFocus() } }
                ),
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            containerColor = Color.Unspecified,
            contentWindowInsets = contentWindowInsets,
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                content()
            }
        }
    }
}