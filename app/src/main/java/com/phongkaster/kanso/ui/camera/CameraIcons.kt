package com.phongkaster.kanso.ui.camera

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// The app module does not depend on the Material icons library, so the camera's icons are
// drawn here as vectors, on the usual 24 by 24 grid. Their colour is only a stand-in: Material
// 3's `Icon` tints the whole drawing with its content colour, so they follow the theme.

/** Starts an empty 24 dp icon named [name], drawn on a 24 by 24 grid. */
private fun iconBuilder(name: String): ImageVector.Builder = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
)

/**
 * A camera body with its round lens, the same shape as Material's "photo camera" icon. Used on
 * the gallery's top bar to take a new photo instead of picking one.
 */
val CameraIcon: ImageVector = iconBuilder("Camera").apply {
    // The body, with a round hole where the lens goes. EvenOdd fill leaves the hole empty.
    path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
        moveTo(9f, 2f)
        lineTo(7.17f, 4f)
        horizontalLineTo(4f)
        curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
        verticalLineToRelative(12f)
        curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
        horizontalLineToRelative(16f)
        curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
        verticalLineTo(6f)
        curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
        horizontalLineToRelative(-3.17f)
        lineTo(15f, 2f)
        close()
        moveTo(12f, 17f)
        arcToRelative(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -10f)
        arcToRelative(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 10f)
        close()
    }
    // The lens itself, a smaller disc inside the hole.
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 8.8f)
        arcToRelative(3.2f, 3.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 6.4f)
        arcToRelative(3.2f, 3.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -6.4f)
        close()
    }
}.build()

/** A lightning bolt: the flash fires for every photo (Material's "flash on" shape). */
val FlashOnIcon: ImageVector = iconBuilder("FlashOn").apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(7f, 2f)
        verticalLineToRelative(11f)
        horizontalLineToRelative(3f)
        verticalLineToRelative(9f)
        lineToRelative(7f, -12f)
        horizontalLineToRelative(-4f)
        lineToRelative(4f, -8f)
        close()
    }
}.build()

/** A lightning bolt with a small "A" beside it: the camera decides (Material's "flash auto"). */
val FlashAutoIcon: ImageVector = iconBuilder("FlashAuto").apply {
    // The bolt.
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 2f)
        verticalLineToRelative(12f)
        horizontalLineToRelative(3f)
        verticalLineToRelative(9f)
        lineToRelative(7f, -12f)
        horizontalLineTo(9f)
        lineToRelative(4f, -9f)
        close()
    }
    // The letter A, with the hole of its crossbar left empty by EvenOdd fill.
    path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
        moveTo(19f, 2f)
        horizontalLineToRelative(-2f)
        lineToRelative(-3.2f, 9f)
        horizontalLineToRelative(1.9f)
        lineToRelative(0.7f, -2f)
        horizontalLineToRelative(3.2f)
        lineToRelative(0.7f, 2f)
        horizontalLineToRelative(1.9f)
        close()
        moveTo(16.85f, 7.65f)
        lineTo(18f, 4f)
        lineToRelative(1.15f, 3.65f)
        close()
    }
}.build()

/** A lightning bolt crossed out: the flash never fires (Material's "flash off" shape). */
val FlashOffIcon: ImageVector = iconBuilder("FlashOff").apply {
    // The lower half of the bolt and the diagonal stroke through it.
    path(fill = SolidColor(Color.Black)) {
        moveTo(3.27f, 3f)
        lineTo(2f, 4.27f)
        lineToRelative(5f, 5f)
        verticalLineTo(13f)
        horizontalLineToRelative(3f)
        verticalLineToRelative(9f)
        lineToRelative(3.58f, -6.14f)
        lineTo(17.73f, 20f)
        lineTo(19f, 18.73f)
        close()
    }
    // The upper half of the bolt.
    path(fill = SolidColor(Color.Black)) {
        moveTo(17f, 10f)
        horizontalLineToRelative(-4f)
        lineToRelative(4f, -8f)
        horizontalLineTo(7f)
        verticalLineToRelative(2.18f)
        lineToRelative(8.46f, 8.46f)
        close()
    }
}.build()

/**
 * Two curved arrows chasing each other round a small lens: swap between the back and front
 * camera. Drawn with lines (a stroke) rather than filled shapes; the tint colours both.
 */
val SwitchLensIcon: ImageVector = iconBuilder("SwitchLens").apply {
    // The upper arrow's curve, over the top from left to right, and the lower one's, under the
    // bottom from right to left: two parts of a circle of radius 7 round the middle.
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(5.42f, 9.61f)
        arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 18.58f, y1 = 9.61f)
        moveTo(18.58f, 14.39f)
        arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 5.42f, y1 = 14.39f)
    }
    // The two arrow heads: one points down at the right end, one points up at the left end.
    path(fill = SolidColor(Color.Black)) {
        moveTo(15.9f, 9f)
        horizontalLineTo(21.3f)
        lineTo(18.6f, 12.6f)
        close()
        moveTo(2.7f, 15f)
        horizontalLineTo(8.1f)
        lineTo(5.4f, 11.4f)
        close()
    }
    // The lens in the middle.
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 10f)
        arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = 4f)
        arcToRelative(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 0f, dy1 = -4f)
        close()
    }
}.build()
