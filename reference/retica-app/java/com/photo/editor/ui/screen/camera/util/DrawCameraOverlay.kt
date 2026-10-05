package com.photo.editor.ui.screen.camera.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.photo.editor.domain.model.AspectRatio

internal fun Modifier.drawCameraOverlay(
    aspectRatio: AspectRatio,
    overlayColor: Color = Color.Black.copy(0.55f),
    drawGrid: Boolean = false
) = composed {
    var maxDimenValue by rememberSaveable { mutableFloatStateOf(1f) }
    val maxDimenAnim by animateFloatAsState(maxDimenValue, label = "")

    drawWithCache {
        val minDimen = size.minDimension
        maxDimenValue = if (aspectRatio == AspectRatio.RatioFull) size.maxDimension
        else minDimen / (aspectRatio.numerator.toFloat() / aspectRatio.denominator)
        val isLandscape = size.width > size.height

        val center = Offset(size.width / 2f, size.height / 2f)
        val left = if (isLandscape) center.x - maxDimenAnim / 2f else center.x - minDimen / 2
        val right = if (isLandscape) center.x + maxDimenAnim / 2f else center.x + minDimen / 2
        val top = if (isLandscape) center.y - minDimen / 2f else center.y - maxDimenAnim / 2f
        val bottom = if (isLandscape) center.y + minDimen / 2f else center.y + maxDimenAnim / 2f

        val noOverlayWidth = right - left
        val noOverlayHeight = bottom - top

        val gridLineWidth = 1.dp.toPx()

        onDrawBehind {
            clipRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                clipOp = ClipOp.Difference,
            ) {
                drawRect(overlayColor)
            }

            clipRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
            ) {
                // Draw grid
                if (drawGrid) {
                    // Vertical 1
                    drawLine(
                        color = Color.White.copy(0.3f),
                        start = Offset(left + noOverlayWidth / 3, top),
                        end = Offset(left + noOverlayWidth / 3, bottom),
                        strokeWidth = gridLineWidth,
                    )

                    // Vertical 2
                    drawLine(
                        color = Color.White.copy(0.3f),
                        start = Offset(left + noOverlayWidth * 2 / 3, top),
                        end = Offset(left + noOverlayWidth * 2 / 3, bottom),
                        strokeWidth = gridLineWidth,
                    )

                    // Horizontal 1
                    drawLine(
                        color = Color.White.copy(0.3f),
                        start = Offset(left, top + noOverlayHeight / 3),
                        end = Offset(right, top + noOverlayHeight / 3),
                        strokeWidth = gridLineWidth,
                    )

                    // Horizontal 2
                    drawLine(
                        color = Color.White.copy(0.3f),
                        start = Offset(left, top + noOverlayHeight * 2 / 3),
                        end = Offset(right, top + noOverlayHeight * 2 / 3),
                        strokeWidth = gridLineWidth,
                    )
                }
            }
        }
    }
}