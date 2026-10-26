package com.apero.pinity.ui.screen.edit_image.decal

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.apero.pinity.R
import com.apero.pinity.ui.component.modifier.circle
import com.apero.pinity.ui.screen.edit_image.decal.model.DecalImageOverlay
import com.apero.pinity.ui.screen.edit_image.decal.model.DecalTextOverlay
import com.apero.pinity.ui.theme.PrimaryColor
import com.apero.pinity.ui.theme.regular12
import com.apero.pinity.util.resourceToUri

private const val TAG = "StickerOverlay"

@Composable
fun DecalOverlayPreview(
    overlayState: DecalOverlayState,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    editing: Boolean = false,
    onActive: (Boolean) -> Unit = {},
    onDelete: () -> Unit = {},
    onOpenTextEditor: () -> Unit = {},
    onChangeNextDecal: () -> Unit = {},
) {
    val context = LocalContext.current
    val closeIcon = painterResource(R.drawable.ic_close_circle)
    val resizeIcon = painterResource(R.drawable.ic_resize_circle)
    val rotateIcon = painterResource(R.drawable.ic_rotate_circle)
    var transforming by remember { mutableStateOf(false) }


    Box {
        Canvas(
            modifier = modifier
                .onSizeChanged { overlayState.canvasSize = it }
                .stickerPointer(
                    overlayState = overlayState,
                    active = active,
                    density = LocalDensity.current,
                    onActive = onActive,
                    onTransforming = { transforming = it },
                    onDelete = onDelete,
                    onClick = {
                        onChangeNextDecal()
                    }
                )
        ) {
            translate(
                left = size.width / 2 - overlayState.size.width / 2 + overlayState.centerOffset.x,
                top = size.height / 2 - overlayState.size.height / 2 + overlayState.centerOffset.y,
            ) {
                rotate(
                    degrees = overlayState.rotationDegrees,
                    pivot = (overlayState.size.toOffset()) / 2f
                ) {
                    // Draw sticker image
                    drawImage(
                        image = overlayState.bitmap,
                        dstSize = overlayState.size,
                    )

                    // Draw transformation lines, dots
                    if (!active) return@translate

                    drawRect(
                        color = Color.White,
                        topLeft = Offset.Zero,
                        size = overlayState.size.toSize(),
                        style = Stroke(2.dp.toPx())
                    )

                    if (transforming) return@translate
                    val iconSize = 24.dp.toPx()

                    translate(
                        left = overlayState.size.width.toFloat() - iconSize / 2,
                        top = -iconSize / 2,
                    ) {
                        with(closeIcon) {
                            draw(Size(iconSize, height = iconSize))
                        }
                    }

                    translate(
                        left = -iconSize / 2,
                        top = overlayState.size.height.toFloat() - iconSize / 2,
                    ) {
                        with(rotateIcon) {
                            draw(Size(iconSize, height = iconSize))
                        }
                    }

                    translate(
                        left = overlayState.size.width.toFloat() - iconSize / 2,
                        top = overlayState.size.height.toFloat() - iconSize / 2,
                    ) {
                        with(resizeIcon) {
                            draw(Size(iconSize, height = iconSize))
                        }
                    }
                }

                translate(
                    left = overlayState.size.width.toFloat(),
                ) {

                }
            }
        }

        /**
         * DO NOT REMOVE
         * this button is used to open the text editor for DecalTextOverlay.
         * it is not visible in the final product.
         * */
        AnimatedVisibility(
            visible = false,
            content = {
                if (overlayState.overlay is DecalTextOverlay && active && !editing) {
                    Text(
                        text = stringResource(R.string.edit),
                        style = regular12,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                translationX = overlayState.centerOffset.x.toFloat()
                                translationY =
                                    overlayState.size.height / 2 + overlayState.centerOffset.y + size.height / 2 + 6.dp.toPx()
                            }
                            .circle()
                            .clickable { onOpenTextEditor() }
                            .background(PrimaryColor)
                            .padding(vertical = 6.dp, horizontal = 12.dp)
                    )
                }
            }
        )
    }
}

@Preview(
    widthDp = 480,
    heightDp = 720,
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
)
@Composable
private fun Preview() {
    val context = LocalContext.current
    val overlay1 = remember { DecalImageOverlay(uri = resourceToUri(context.resources, R.drawable.filter_sx7000)) }
    val overlay2 = remember { DecalImageOverlay(uri = resourceToUri(context.resources, R.drawable.img_lifestyle_2_bg)) }

    var overlayState: DecalOverlayState? by remember {
        mutableStateOf(
            DecalDefaultOverlayState(
                context = context,
                overlay = overlay1,
                inspectionMode = true,
            )
        )
    }
    var overlayState2: DecalOverlayState? by remember {
        mutableStateOf(
            DecalDefaultOverlayState(
                context = context,
                overlay = overlay2,
                inspectionMode = true,
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        overlayState?.let {
            DecalOverlayPreview(
                overlayState = it,
                active = true,
                onDelete = { overlayState = null },
                modifier = Modifier.fillMaxSize()
            )
        }
        overlayState2?.let {
            DecalOverlayPreview(
                overlayState = it,
                active = true,
                onDelete = { overlayState2 = null },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(50.dp)
            )
        }
    }
}