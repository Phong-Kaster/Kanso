package com.phongkaster.kanso.data.edit

import android.graphics.Bitmap
import android.graphics.Rect
import com.phongkaster.kanso.data.edit.face.WarpPush

/**
 * What the screen draws, live, while a slider tool's slider is dragged: not a finished picture
 * (making one takes far too long to repeat many times a second) but a recipe the screen draws on
 * the graphics chip each frame, on top of the photo as shown.
 *
 * - [region] is the part of the photo the live look changes, in its pixels: every face box and
 *   every push's circle. Outside it the photo is drawn as it is; inside it the screen draws that
 *   part of the photo through a `WarpMesh`, an evenly spaced grid moved by [pushes].
 * - [pushes] reshape the photo (Face width), in pixels of the photo as shown; empty for none.
 * - [smoothed], when set, is the photo as shown fully smoothed inside the faces; the screen draws
 *   it through the same mesh on top of the photo, [smoothing] opaque (0 to 1), which mixes the two
 *   as the saved picture's blend does.
 */
data class LivePreview(
    val region: Rect,
    val pushes: List<WarpPush>,
    val smoothed: Bitmap?,
    val smoothing: Float,
)
