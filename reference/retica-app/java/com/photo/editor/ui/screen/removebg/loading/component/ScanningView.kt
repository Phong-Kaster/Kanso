package com.photo.editor.ui.screen.removebg.loading.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.photo.editor.common.ui.modifier.circle
import com.photo.editor.common.ui.theme.PrimaryColor
import com.photo.editor.common.ui.theme.PrimaryRed
import com.photo.editor.common.ui.theme.PrimaryRedLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun forwardScanGradient(
    startY: Float,
    endY: Float,
) = Brush.verticalGradient(
    listOf(PrimaryRedLight, PrimaryRed),
    startY = startY,
    endY = endY,
)

private fun backwardScanGradient(
    startY: Float,
    endY: Float,
) = Brush.verticalGradient(
    listOf(PrimaryRed, PrimaryRedLight),
    startY = startY,
    endY = endY,
)

@Composable
fun ScanningView(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var forwardScanning: Boolean? by remember { mutableStateOf(null) }
    var forwardScanProgress by remember { mutableFloatStateOf(0f) }
    var forwardTrailSize by remember { mutableFloatStateOf(0f) }
    var backwardScanProgress by remember { mutableFloatStateOf(0f) }
    var backwardTrailSize by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            forwardScanning = true

            val forwardJob1 = launch {
                animate(0f, 1f, animationSpec = tween(1500, easing = LinearEasing)) { value, _ ->
                    forwardScanProgress = value
                }
            }
            val forwardJob2 = launch {
                animate(0f, 0.5f, animationSpec = tween(500, easing = LinearEasing)) { value, _ ->
                    forwardTrailSize = value
                }
                animate(0.5f, 0.5f, animationSpec = tween(1000, easing = LinearEasing)) { value, _ ->
                    forwardTrailSize = value
                }
                animate(0.5f, 0f, animationSpec = tween(400, easing = LinearEasing)) { value, _ ->
                    forwardTrailSize = value
                }
            }

            forwardJob1.join()
            forwardJob2.join()

            forwardScanning = null

            delay(500)

            forwardScanning = false
            val backwardJob1 = launch {
                animate(0f, 1f, animationSpec = tween(1500, easing = LinearEasing)) { value, _ ->
                    backwardScanProgress = value
                }
            }
            val backwardJob2 = launch {
                animate(0f, 0.5f, animationSpec = tween(500, easing = LinearEasing)) { value, _ ->
                    backwardTrailSize = value
                }
                animate(0.5f, 0.5f, animationSpec = tween(1000, easing = LinearEasing)) { value, _ ->
                    backwardTrailSize = value
                }
                animate(0.5f, 0f, animationSpec = tween(400, easing = LinearEasing)) { value, _ ->
                    backwardTrailSize = value
                }
            }

            backwardJob1.join()
            backwardJob2.join()

            forwardScanning = null

            delay(500)
        }
    }

    Box(modifier = modifier) {
        Box(modifier = Modifier.padding(horizontal = 10.dp)) {
            content()
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        brush = forwardScanGradient(
                            startY = size.height * (forwardScanProgress - forwardTrailSize),
                            endY = size.height,
                        ),
                        topLeft = Offset(10.dp.toPx(), size.height * (forwardScanProgress - forwardTrailSize)),
                        size = Size(width = size.width - 20.dp.toPx(), size.height * forwardTrailSize)
                    )

                    drawRect(
                        brush = backwardScanGradient(
                            startY = size.height * (1f - backwardScanProgress),
                            endY = size.height * (1f - backwardScanProgress + backwardTrailSize),
                        ),
                        topLeft = Offset(10.dp.toPx(), size.height * (1f - backwardScanProgress)),
                        size = Size(width = size.width - 20.dp.toPx(), size.height * backwardTrailSize),
                    )
                }
        ) {
            if (forwardScanning == true) {
                Spacer(
                    modifier = Modifier
                        .align(BiasAlignment(0f, (forwardScanProgress - 0.5f) * 2))
                        .fillMaxWidth()
                        .height(2.dp)
                        .circle()
                        .background(PrimaryRed)
                        .alpha(forwardScanProgress)
                )
            }

            if (forwardScanning == false) {
                Spacer(
                    modifier = Modifier
                        .align(BiasAlignment(0f, (-backwardScanProgress + 0.5f) * 2))
                        .fillMaxWidth()
                        .height(2.dp)
                        .circle()
                        .background(PrimaryRed)
                        .alpha(backwardScanProgress)
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ScanningView() {
        Spacer(
            modifier = Modifier
                .size(200.dp)
                .background(Color.White)
        )
    }
}